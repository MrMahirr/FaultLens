package com.faultlens.api.service;

import com.faultlens.api.dto.AlarmDto;
import com.faultlens.api.model.Alarm;
import com.faultlens.api.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;

    public List<AlarmDto> getAllAlarms() {
        return alarmRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AlarmDto resolveAlarm(Long id) {
        Alarm alarm = alarmRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alarm not found with id: " + id));
        alarm.setStatus("RESOLVED");
        alarm.setResolvedAt(Instant.now());
        Alarm updated = alarmRepository.save(alarm);
        return mapToDto(updated);
    }

    public void deleteAlarm(Long id) {
        alarmRepository.deleteById(id);
    }

    private AlarmDto mapToDto(Alarm alarm) {
        return AlarmDto.builder()
                .id(alarm.getId())
                .ruleId(alarm.getRuleId())
                .ruleName(alarm.getRuleName())
                .sourceId(alarm.getSourceId())
                .severity(alarm.getSeverity())
                .status(alarm.getStatus())
                .message(alarm.getMessage())
                .triggeredAt(alarm.getTriggeredAt())
                .resolvedAt(alarm.getResolvedAt())
                .build();
    }
}
