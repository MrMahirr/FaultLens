package com.loglens.api.service;

import com.loglens.analyzer.model.Analysis;
import com.loglens.analyzer.repository.AnalysisRepository;
import com.loglens.api.dto.LogDetailDto;
import com.loglens.api.mapper.ApiDtoMapper;
import com.loglens.api.redis.CacheService;
import com.loglens.common.dto.AnalysisResultDto;
import com.loglens.common.dto.LogEntryDto;
import com.loglens.common.dto.LogGroupDto;
import com.loglens.common.dto.PagedResponse;
import com.loglens.common.enums.Severity;
import com.loglens.common.exception.ResourceNotFoundException;
import com.loglens.processor.model.LogEntry;
import com.loglens.processor.repository.LogEntryRepository;
import com.loglens.processor.repository.LogGroupRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogQueryService {
    private final LogEntryRepository entryRepository;
    private final LogGroupRepository groupRepository;
    private final AnalysisRepository analysisRepository;
    private final CacheService cacheService;
    private final ApiDtoMapper mapper;

    /**
     * Lists logs with optional filters.
     */
    public PagedResponse<LogEntryDto> list(Severity severity, Long sourceId, Instant from, Instant to, String search, int page, int size) {
        Page<LogEntry> result = entryRepository.findAll(spec(severity, sourceId, from, to, search),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
        return page(result.map(mapper::toLogEntryDto));
    }

    /**
     * Returns a log detail with related analyses.
     */
    public LogDetailDto detail(Long id) {
        LogEntry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Log not found: " + id));
        List<AnalysisResultDto> analyses = entry.getGroupId() == null ? List.of() : analysisRepository.findByLogGroupIdOrderByAnalyzedAtDesc(entry.getGroupId()).stream()
                .map(mapper::toAnalysisDto)
                .toList();
        return LogDetailDto.builder()
                .log(mapper.toLogEntryDto(entry))
                .analyses(analyses)
                .build();
    }

    /**
     * Lists log groups.
     */
    public PagedResponse<LogGroupDto> groups(Severity severity, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastSeenAt"));
        Page<LogGroupDto> result = (severity == null
                ? groupRepository.findAllByOrderByLastSeenAtDesc(pageable)
                : groupRepository.findBySeverityOrderByLastSeenAtDesc(severity, pageable))
                .map(mapper::toLogGroupDto);
        return page(result);
    }

    /**
     * Lists entries for a group.
     */
    public PagedResponse<LogEntryDto> groupEntries(Long groupId, int page, int size) {
        return page(entryRepository.findByGroupId(groupId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))).map(mapper::toLogEntryDto));
    }

    /**
     * Returns severity counts for the requested window.
     */
    public Map<String, Long> stats(int windowMinutes) {
        int normalizedWindow = Math.max(1, windowMinutes);
        return cacheService.getStats(normalizedWindow, () -> {
            Instant after = Instant.now().minus(normalizedWindow, ChronoUnit.MINUTES);
            Map<String, Long> stats = new java.util.LinkedHashMap<>();
            for (Severity severity : Severity.values()) {
                stats.put(severity.name(), 0L);
            }
            for (Object[] row : entryRepository.countBySeverityGroupSince(after)) {
                String severity = String.valueOf(row[0]);
                Number count = (Number) row[1];
                stats.put(severity, count.longValue());
            }
            long total = stats.values().stream().mapToLong(Long::longValue).sum();
            stats.put("total", total);
            return stats;
        });
    }

    private Specification<LogEntry> spec(Severity severity, Long sourceId, Instant from, Instant to, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (sourceId != null) {
                predicates.add(cb.equal(root.get("sourceId"), sourceId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("message")), pattern),
                        cb.like(cb.lower(root.get("rawLine")), pattern)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private <T> PagedResponse<T> page(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
