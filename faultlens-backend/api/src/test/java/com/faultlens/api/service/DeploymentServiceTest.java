package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeploymentServiceTest {
    @Test
    void blankQueryMatchesEveryService() {
        assertThat(DeploymentService.containsPattern(null)).isEqualTo("%");
        assertThat(DeploymentService.containsPattern("  ")).isEqualTo("%");
    }

    @Test
    void queryIsLowerCasedTrimmedAndEscaped() {
        assertThat(DeploymentService.containsPattern(" Pay_Ment ")).isEqualTo("%pay!_ment%");
        assertThat(DeploymentService.containsPattern("100%")).isEqualTo("%100!%%");
        assertThat(DeploymentService.containsPattern("a!b")).isEqualTo("%a!!b%");
    }
}
