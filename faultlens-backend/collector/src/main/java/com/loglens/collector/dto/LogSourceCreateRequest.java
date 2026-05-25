package com.loglens.collector.dto;

import com.loglens.common.enums.LogSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogSourceCreateRequest {
    @NotBlank
    private String name;

    @NotNull
    private LogSourceType type;

    private String config;

    @Builder.Default
    private boolean enabled = true;
}
