package com.faultlens.api.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    /**
     * Creates the raw log topic.
     */
    @Bean
    public NewTopic logRawTopic() {
        return TopicBuilder.name("log-raw").partitions(3).replicas(1).build();
    }

    /**
     * Creates the raw log DLQ topic.
     */
    @Bean
    public NewTopic logRawDlqTopic() {
        return TopicBuilder.name("log-raw-dlq").partitions(1).replicas(1).build();
    }

    /**
     * Creates the error log topic.
     */
    @Bean
    public NewTopic logErrorsTopic() {
        return TopicBuilder.name("log-errors").partitions(3).replicas(1).build();
    }

    /**
     * Creates the analysis topic.
     */
    @Bean
    public NewTopic logAnalysisTopic() {
        return TopicBuilder.name("log-analysis").partitions(1).replicas(1).build();
    }

    /**
     * Creates the deployment events topic.
     */
    @Bean
    public NewTopic deploymentEventsTopic() {
        return TopicBuilder.name("deployment-events").partitions(1).replicas(1).build();
    }
}
