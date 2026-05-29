package com.faultlens.processor.config;

import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.common.dto.LogEventDto;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {
    /**
     * Creates raw log consumer factory.
     */
    @Bean
    public ConsumerFactory<String, LogEventDto> logEventConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.faultlens.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, LogEventDto.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates listener container for raw logs.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogEventDto> logEventKafkaListenerContainerFactory(
            ConsumerFactory<String, LogEventDto> logEventConsumerFactory,
            DefaultErrorHandler logEventErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, LogEventDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(logEventConsumerFactory);
        factory.setConcurrency(3);
        factory.setBatchListener(true);
        factory.setCommonErrorHandler(logEventErrorHandler);
        return factory;
    }

    /**
     * Creates producer factory for raw log DLQ messages.
     */
    @Bean
    public ProducerFactory<String, LogEventDto> processorLogEventProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Creates Kafka template for raw log DLQ messages.
     */
    @Bean
    public KafkaTemplate<String, LogEventDto> processorLogEventKafkaTemplate(
            @Qualifier("processorLogEventProducerFactory") ProducerFactory<String, LogEventDto> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Sends failed raw log records to log-raw-dlq after retries are exhausted.
     */
    @Bean
    public DefaultErrorHandler logEventErrorHandler(
            @Qualifier("processorLogEventKafkaTemplate") KafkaTemplate<String, LogEventDto> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition("log-raw-dlq", 0));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }

    /**
     * Creates producer factory for error log DTOs.
     */
    @Bean
    public ProducerFactory<String, LogEntryDto> logEntryProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Creates Kafka template for error log DTOs.
     */
    @Bean
    public KafkaTemplate<String, LogEntryDto> logEntryKafkaTemplate(ProducerFactory<String, LogEntryDto> logEntryProducerFactory) {
        return new KafkaTemplate<>(logEntryProducerFactory);
    }
}
