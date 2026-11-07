package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.processor.service.LogProcessorService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * P08 regression: parallel consumers processing batches that share groups. The old
 * lock-then-insert grouping deadlocked here and the aborted transactions sent whole batches to
 * the DLQ; the batch upsert must store every entry and count every one of them in its group.
 */
class GroupingConcurrencyIT extends AbstractIntegrationTest {
    @Autowired
    private LogProcessorService processor;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void parallelBatchesWithOverlappingGroupsNeitherDeadlockNorLoseEntries() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
        int threads = 6;
        int batchesPerThread = 15;
        int batchSize = 100;
        int shapes = 25;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Integer>> results = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            long seed = t;
            results.add(pool.submit((Callable<Integer>) () -> {
                Random random = new Random(seed);
                int stored = 0;
                for (int b = 0; b < batchesPerThread; b++) {
                    List<LogEventDto> batch = new ArrayList<>();
                    for (int i = 0; i < batchSize; i++) {
                        int shape = random.nextInt(shapes);
                        batch.add(LogEventDto.builder()
                                .severity(com.faultlens.common.enums.Severity.ERROR)
                                // same shape per `shape`, different numbers -> one group each
                                .message("grp-" + run + "-s" + shapeName(shape) + " failed after " + random.nextInt(5000) + "ms")
                                .serviceName("it-" + run)
                                .build());
                    }
                    Collections.shuffle(batch, random); // groups touched in a different order per batch
                    stored += processor.processBatch(batch).size();
                }
                return stored;
            }));
        }
        int stored = 0;
        for (Future<Integer> result : results) {
            stored += result.get(); // rethrows a deadlock, if any
        }
        pool.shutdown();

        int expected = threads * batchesPerThread * batchSize;
        assertThat(stored).isEqualTo(expected);
        assertThat(jdbc.queryForObject("select count(*) from log_entries where service_name = ?", Long.class, "it-" + run))
                .isEqualTo(expected);
        assertThat(jdbc.queryForObject(
                "select count(*) from log_groups where first_message like ?", Long.class, "grp-" + run + "-%"))
                .isEqualTo(shapes);
        assertThat(jdbc.queryForObject(
                "select sum(count) from log_groups where first_message like ?", Long.class, "grp-" + run + "-%"))
                .isEqualTo(expected);
    }

    /** Letters only, so the normalizer keeps shapes apart (digits would all become one shape). */
    private static String shapeName(int shape) {
        return String.valueOf((char) ('a' + shape % 26)) + (char) ('a' + shape / 26);
    }
}
