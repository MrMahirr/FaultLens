package com.faultlens.collector.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.common.dto.LogEventDto;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

class KafkaProducerConfigTest {
    @Test
    void configuresReliableIdempotentProducer() {
        KafkaProducerConfig config = new KafkaProducerConfig();
        KafkaProperties properties = new KafkaProperties();

        ProducerFactory<String, LogEventDto> factory = config.logEventProducerFactory(properties);

        assertThat(factory).isInstanceOf(DefaultKafkaProducerFactory.class);
        Map<String, Object> producerProperties = ((DefaultKafkaProducerFactory<?, ?>) factory).getConfigurationProperties();
        assertThat(producerProperties).containsEntry(ProducerConfig.ACKS_CONFIG, "all");
        assertThat(producerProperties).containsEntry(ProducerConfig.RETRIES_CONFIG, 3);
        assertThat(producerProperties).containsEntry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    }
}
