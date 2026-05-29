package com.faultlens.api;

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
@SpringBootApplication(scanBasePackages = "com.faultlens")
@EntityScan(basePackages = {
        "com.faultlens.collector.model",
        "com.faultlens.processor.model",
        "com.faultlens.analyzer.model",
        "com.faultlens.api.model"
})
@EnableJpaRepositories(basePackages = {
        "com.faultlens.collector.repository",
        "com.faultlens.processor.repository",
        "com.faultlens.analyzer.repository",
        "com.faultlens.api.repository"
})
public class FaultLensApiApplication {
    /**
     * Starts the faultlens API application.
     */
    public static void main(String[] args) {
        SpringApplication.run(FaultLensApiApplication.class, args);
    }
}
