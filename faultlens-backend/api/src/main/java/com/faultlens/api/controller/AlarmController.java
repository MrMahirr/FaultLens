package com.faultlens.api.controller;

import com.faultlens.api.dto.AlarmDto;
import com.faultlens.api.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping
    public ResponseEntity<List<AlarmDto>> getAllAlarms() {
        return ResponseEntity.ok(alarmService.getAllAlarms());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<AlarmDto> resolveAlarm(@PathVariable Long id) {
        return ResponseEntity.ok(alarmService.resolveAlarm(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlarm(@PathVariable Long id) {
        alarmService.deleteAlarm(id);
        return ResponseEntity.noContent().build();
    }
}
