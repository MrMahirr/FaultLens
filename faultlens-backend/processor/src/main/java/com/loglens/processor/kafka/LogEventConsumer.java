package com.loglens.processor.kafka;

import com.loglens.common.dto.LogEventDto;
import com.loglens.processor.service.LogProcessorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventConsumer {
    private final LogProcessorService processorService;

    /**
     * Consumes raw logs in batches; failed records are handled by the container DLQ handler.
     */
    @Transactional
    @KafkaListener(topics = "log-raw", groupId = "loglens-processor", containerFactory = "logEventKafkaListenerContainerFactory")
    public void consume(List<LogEventDto> events) {
        log.debug("Processing {} raw log events", events.size());
        processorService.processBatch(events);
    }
}
