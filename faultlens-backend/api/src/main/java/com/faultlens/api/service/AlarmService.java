package com.faultlens.api.service;

import com.faultlens.api.dto.AlarmDto;
import com.faultlens.api.dto.AlarmStatsDto;
import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.repository.AlarmRepository;
import com.faultlens.common.dto.PagedResponse;
import com.faultlens.common.exception.FaultLensException;
import com.faultlens.common.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.faultlens.common.enums.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
public class AlarmService {

    private final AlarmRepository alarmRepository;

    /**
     * Lists alarms newest first, optionally filtered by status and severity.
     */
    public PagedResponse<AlarmDto> list(AlarmStatus status, String severity, int page, int size) {
        Page<Alarm> result = alarmRepository.findAll(filter(status, severity),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "triggeredAt")));
        return PagedResponse.<AlarmDto>builder()
                .content(result.getContent().stream().map(this::mapToDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * Counts alarms per state, independent of list pagination.
     */
    public AlarmStatsDto stats() {
        return new AlarmStatsDto(
                alarmRepository.countByStatus(AlarmStatus.ACTIVE),
                alarmRepository.countByStatus(AlarmStatus.ACKNOWLEDGED),
                alarmRepository.countByStatus(AlarmStatus.RESOLVED),
                alarmRepository.countByStatusInAndSeverity(
                        List.of(AlarmStatus.ACTIVE, AlarmStatus.ACKNOWLEDGED), Severity.CRITICAL));
    }

    /**
     * Marks an alarm as acknowledged. Resolved alarms cannot be acknowledged (400).
     */
    @Transactional
    public AlarmDto acknowledge(Long id) {
        Alarm alarm = find(id);
        if (alarm.getStatus() == AlarmStatus.RESOLVED) {
            throw new FaultLensException("Alarm is already resolved: " + id, "ALARM_ALREADY_RESOLVED");
        }
        alarm.setStatus(AlarmStatus.ACKNOWLEDGED);
        return mapToDto(alarmRepository.save(alarm));
    }

    /**
     * Marks an alarm as resolved; resolving twice keeps the first resolution time.
     */
    @Transactional
    public AlarmDto resolveAlarm(Long id) {
        Alarm alarm = find(id);
        if (alarm.getStatus() != AlarmStatus.RESOLVED) {
            alarm.setStatus(AlarmStatus.RESOLVED);
            // PostgreSQL timestamptz keeps microseconds; truncating keeps this response equal
            // to what later reads return.
            alarm.setResolvedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
            alarm = alarmRepository.save(alarm);
        }
        return mapToDto(alarm);
    }

    /**
     * Deletes an alarm (404 when it does not exist).
     */
    @Transactional
    public void deleteAlarm(Long id) {
        alarmRepository.delete(find(id));
    }

    private Alarm find(Long id) {
        return alarmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alarm not found: " + id));
    }

    private static Specification<Alarm> filter(AlarmStatus status, String severity) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (severity != null && !severity.isBlank()) {
                predicates.add(cb.equal(root.get("severity"), parseSeverity(severity)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Severity parseSeverity(String value) {
        try {
            return Severity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new FaultLensException("Unknown severity: " + value, "INVALID_SEVERITY");
        }
    }

    private AlarmDto mapToDto(Alarm alarm) {
        return AlarmDto.builder()
                .id(alarm.getId())
                .ruleId(alarm.getRuleId())
                .ruleName(alarm.getRuleName())
                .sourceId(alarm.getSourceId())
                .severity(alarm.getSeverity() == null ? null : alarm.getSeverity().name())
                .alarmRuleId(alarm.getAlarmRuleId())
                .status(alarm.getStatus())
                .message(alarm.getMessage())
                .triggeredAt(alarm.getTriggeredAt())
                .resolvedAt(alarm.getResolvedAt())
                .build();
    }
}
