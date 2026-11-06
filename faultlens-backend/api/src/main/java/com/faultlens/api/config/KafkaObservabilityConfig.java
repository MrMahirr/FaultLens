package com.faultlens.api.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.MicrometerProducerListener;

/**
 * Kafka metrics and tracing for every module's hand-made factories (P08).
 *
 * <ul>
 *   <li>Micrometer client metrics on all consumers and producers; among them the consumer lag
 *       ({@code kafka_consumer_fetch_manager_records_lag_max}) per group;</li>
 *   <li>observation on templates and listener containers, so a trace follows a log from the
 *       collector's send through the topics (record listeners; batch listeners start a new
 *       span).</li>
 * </ul>
 * Done here once instead of in each module's configuration.
 */
@Configuration
public class KafkaObservabilityConfig {
    @Bean
    static BeanPostProcessor kafkaObservabilityPostProcessor(ObjectProvider<MeterRegistry> registry) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DefaultKafkaConsumerFactory<?, ?> consumers) {
                    consumers.addListener(new MicrometerConsumerListener<>(registry.getObject()));
                } else if (bean instanceof DefaultKafkaProducerFactory<?, ?> producers) {
                    producers.addListener(new MicrometerProducerListener<>(registry.getObject()));
                } else if (bean instanceof KafkaTemplate<?, ?> template) {
                    template.setObservationEnabled(true);
                } else if (bean instanceof AbstractKafkaListenerContainerFactory<?, ?, ?> containers) {
                    containers.getContainerProperties().setObservationEnabled(true);
                }
                return bean;
            }
        };
    }
}
