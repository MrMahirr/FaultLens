package com.faultlens.api.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * {@code faultlens_kafka_consumer_lag{group,topic}} (P08): messages not yet processed per
 * consumer group, as {@code kafka-consumer-groups --describe} reports it (end offset minus
 * committed offset, summed over partitions). The client metric {@code records_lag_max} is NaN
 * while a consumer is idle, so it cannot back a dashboard on its own.
 */
@Slf4j
@Component
public class KafkaLagMonitor {
    static final List<String> GROUPS = List.of("faultlens-processor", "faultlens-analyzer", "faultlens-api-alarms");

    private final KafkaAdmin kafkaAdmin;
    private final MeterRegistry registry;
    private final Map<String, AtomicLong> lags = new ConcurrentHashMap<>();
    private volatile Admin admin;
    /** Groups whose lag could not be read; warned once until it works again. */
    private final java.util.Set<String> failing = ConcurrentHashMap.newKeySet();

    public KafkaLagMonitor(KafkaAdmin kafkaAdmin, MeterRegistry registry) {
        this.kafkaAdmin = kafkaAdmin;
        this.registry = registry;
    }

    private Admin admin() {
        if (admin == null) {
            synchronized (this) {
                if (admin == null) {
                    admin = Admin.create(kafkaAdmin.getConfigurationProperties());
                }
            }
        }
        return admin;
    }

    @Scheduled(initialDelay = 15_000, fixedDelay = 15_000)
    public void refresh() {
        for (String group : GROUPS) {
            try {
                Map<TopicPartition, OffsetAndMetadata> committed = admin().listConsumerGroupOffsets(group)
                        .partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS);
                // Partitions assigned to live members but never committed (e.g. nothing arrived
                // yet) count too: their lag is 0 until a commit exists (auto.offset.reset=latest).
                java.util.Set<TopicPartition> partitions = new java.util.HashSet<>(committed.keySet());
                var description = admin().describeConsumerGroups(List.of(group)).all()
                        .get(10, TimeUnit.SECONDS).get(group);
                if (description != null) {
                    description.members().forEach(member -> partitions.addAll(member.assignment().topicPartitions()));
                }
                if (partitions.isEmpty()) {
                    continue;
                }
                Map<TopicPartition, OffsetSpec> latest = new HashMap<>();
                partitions.forEach(partition -> latest.put(partition, OffsetSpec.latest()));
                var ends = admin().listOffsets(latest).all().get(10, TimeUnit.SECONDS);
                Map<String, Long> byTopic = new HashMap<>();
                for (TopicPartition partition : partitions) {
                    var end = ends.get(partition);
                    OffsetAndMetadata offset = committed.get(partition);
                    long lag = end == null || offset == null ? 0 : Math.max(0, end.offset() - offset.offset());
                    byTopic.merge(partition.topic(), lag, Long::sum);
                }
                byTopic.forEach((topic, lag) -> gauge(group, topic).set(lag));
                failing.remove(group);
            } catch (Exception ex) {
                if (failing.add(group)) {
                    log.warn("Could not read consumer lag of {}", group, ex);
                }
            }
        }
    }

    private AtomicLong gauge(String group, String topic) {
        return lags.computeIfAbsent(group + "/" + topic, key -> {
            AtomicLong value = new AtomicLong();
            Gauge.builder("faultlens.kafka.consumer.lag", value, AtomicLong::get)
                    .description("Messages not yet processed by the consumer group")
                    .tag("group", group)
                    .tag("topic", topic)
                    .register(registry);
            return value;
        });
    }

    @PreDestroy
    public void close() {
        if (admin != null) {
            admin.close(Duration.ofSeconds(2));
        }
    }
}
