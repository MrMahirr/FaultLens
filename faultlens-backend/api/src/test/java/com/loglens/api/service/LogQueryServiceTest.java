package com.loglens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.loglens.analyzer.repository.AnalysisRepository;
import com.loglens.api.mapper.ApiDtoMapper;
import com.loglens.api.redis.CacheService;
import com.loglens.processor.repository.LogEntryRepository;
import com.loglens.processor.repository.LogGroupRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class LogQueryServiceTest {
    @Test
    void statsReturnsAllSeveritiesAndTotalForRequestedWindow() {
        LogEntryRepository entryRepository = mock(LogEntryRepository.class);
        CacheService cacheService = mock(CacheService.class);
        when(entryRepository.countBySeverityGroupSince(any())).thenReturn(List.of(
                new Object[] {"ERROR", 2L},
                new Object[] {"WARN", 3L}
        ));
        when(cacheService.getStats(org.mockito.ArgumentMatchers.eq(30), any())).thenAnswer(invocation -> {
            Supplier<Map<String, Long>> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        LogQueryService service = new LogQueryService(
                entryRepository,
                mock(LogGroupRepository.class),
                mock(AnalysisRepository.class),
                cacheService,
                new ApiDtoMapper());

        Map<String, Long> stats = service.stats(30);

        assertThat(stats).containsEntry("ERROR", 2L);
        assertThat(stats).containsEntry("WARN", 3L);
        assertThat(stats).containsEntry("INFO", 0L);
        assertThat(stats).containsEntry("total", 5L);
    }
}
