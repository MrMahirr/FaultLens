package com.faultlens.analyzer.kafka;

import com.faultlens.common.dto.AnalysisResultDto;
import com.faultlens.common.dto.LogEntryDto;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class AnalyzerKafkaConfig {
    /**
     * Creates consumer factory for error log DTOs.
     */
    @Bean
    public ConsumerFactory<String, LogEntryDto> logEntryConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.faultlens.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, LogEntryDto.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates listener factory for error log DTOs.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogEntryDto> logEntryKafkaListenerContainerFactory(
            ConsumerFactory<String, LogEntryDto> logEntryConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, LogEntryDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(logEntryConsumerFactory);
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2L)));
        return factory;
    }

    /**
     * Creates producer factory for analysis results.
     */
    @Bean
    public ProducerFactory<String, AnalysisResultDto> analysisProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Creates Kafka template for analysis results.
     */
    @Bean
    public KafkaTemplate<String, AnalysisResultDto> analysisKafkaTemplate(ProducerFactory<String, AnalysisResultDto> analysisProducerFactory) {
        return new KafkaTemplate<>(analysisProducerFactory);
    }
}
