package com.faultlens.api.mapper;

import com.faultlens.analyzer.model.Analysis;
import com.faultlens.analyzer.model.Deployment;
import com.faultlens.api.dto.DeploymentDto;
import com.faultlens.api.dto.LogSourceDto;
import com.faultlens.collector.model.LogSource;
import com.faultlens.common.dto.AnalysisResultDto;
import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.common.dto.LogGroupDto;
import com.faultlens.processor.model.LogEntry;
import com.faultlens.processor.model.LogGroup;
import org.springframework.stereotype.Component;

@Component
public class ApiDtoMapper {
    /**
     * Converts a log entry entity to API DTO.
     */
    public LogEntryDto toLogEntryDto(LogEntry entry) {
        return LogEntryDto.builder()
                .id(entry.getId())
                .sourceId(entry.getSourceId())
                .groupId(entry.getGroupId())
                .severity(entry.getSeverity())
                .message(entry.getMessage())
                .parsedMessage(entry.getParsedMessage())
                .stackTrace(entry.getStackTrace())
                .namespace(entry.getNamespace())
                .podName(entry.getPodName())
                .containerName(entry.getContainerName())
                .serviceName(entry.getServiceName())
                .cluster(entry.getCluster())
                .timestamp(entry.getTimestamp())
                .build();
    }

    /**
     * Converts a log group entity to API DTO.
     */
    public LogGroupDto toLogGroupDto(LogGroup group) {
        return LogGroupDto.builder()
                .id(group.getId())
                .fingerprint(group.getFingerprint())
                .firstMessage(group.getFirstMessage())
                .severity(group.getSeverity())
                .count(group.getCount())
                .firstSeenAt(group.getFirstSeenAt())
                .lastSeenAt(group.getLastSeenAt())
                .build();
    }

    /**
     * Converts an analysis entity to API DTO.
     */
    public AnalysisResultDto toAnalysisDto(Analysis analysis) {
        return AnalysisResultDto.builder()
                .id(analysis.getId())
                .logGroupId(analysis.getLogGroupId())
                .logEntryId(analysis.getLogEntryId())
                .rootCause(analysis.getRootCause())
                .suggestion(analysis.getSuggestion())
                .affectedDeployment(analysis.getAffectedDeployment())
                .confidenceScore(analysis.getConfidenceScore())
                .engineType(analysis.getEngineType())
                .analyzedAt(analysis.getAnalyzedAt())
                .build();
    }

    /**
     * Converts a source entity to API DTO.
     */
    public LogSourceDto toLogSourceDto(LogSource source) {
        return LogSourceDto.builder()
                .id(source.getId())
                .name(source.getName())
                .type(source.getType())
                .config(source.getConfig())
                .enabled(source.isEnabled())
                .createdAt(source.getCreatedAt())
                .lastSeenAt(source.getLastSeenAt())
                .build();
    }

    /**
     * Converts a deployment entity to API DTO.
     */
    public DeploymentDto toDeploymentDto(Deployment deployment) {
        return DeploymentDto.builder()
                .id(deployment.getId())
                .serviceName(deployment.getServiceName())
                .version(deployment.getVersion())
                .environment(deployment.getEnvironment())
                .deployedAt(deployment.getDeployedAt())
                .deployedBy(deployment.getDeployedBy())
                .status(deployment.getStatus())
                .notes(deployment.getNotes())
                .build();
    }

    /**
     * Converts a deployment DTO to entity.
     */
    public Deployment toDeploymentEntity(DeploymentDto dto) {
        return Deployment.builder()
                .id(dto.getId())
                .serviceName(dto.getServiceName())
                .version(dto.getVersion())
                .environment(dto.getEnvironment())
                .deployedAt(dto.getDeployedAt())
                .deployedBy(dto.getDeployedBy())
                .status(dto.getStatus())
                .notes(dto.getNotes())
                .build();
    }
}
