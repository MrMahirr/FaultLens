package com.faultlens.processor.event;

import com.faultlens.processor.kafka.LogErrorProducer;
import com.faultlens.processor.realtime.RealtimeLogPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogProcessedEventListener {

    private final LogErrorProducer errorProducer;
    private final RealtimeLogPublisher realtimeLogPublisher;

    /**
     * Listens for log processing events and dispatches them to Kafka/WebSockets.
     * Executes AFTER the current database transaction commits.
     * If no transaction is active, it executes immediately (fallbackExecution = true).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLogProcessed(LogProcessedEvent event) {
        event.toErrorQueue().forEach(errorProducer::send);
        event.toPublish().forEach(realtimeLogPublisher::publish);
    }
}
