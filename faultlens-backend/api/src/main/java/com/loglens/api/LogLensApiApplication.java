package com.loglens.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableKafka
@EnableRetry
@EnableScheduling
@EnableAspectJAutoProxy
@SpringBootApplication(scanBasePackages = "com.loglens")
@EntityScan(basePackages = {
        "com.loglens.collector.model",
        "com.loglens.processor.model",
        "com.loglens.analyzer.model",
        "com.loglens.api.model"
})
@EnableJpaRepositories(basePackages = {
        "com.loglens.collector.repository",
        "com.loglens.processor.repository",
        "com.loglens.analyzer.repository",
        "com.loglens.api.repository"
})
public class LogLensApiApplication {
    /**
     * Starts the LogLens API application.
     */
    public static void main(String[] args) {
        SpringApplication.run(LogLensApiApplication.class, args);
    }
}
