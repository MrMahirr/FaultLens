package com.loglens.analyzer.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.loglens.analyzer.model.Deployment;
import com.loglens.analyzer.repository.DeploymentRepository;
import com.loglens.common.dto.LogEntryDto;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeploymentCorrelatorTest {
    private final DeploymentRepository repository = mock(DeploymentRepository.class);
    private final DeploymentCorrelator correlator = new DeploymentCorrelator(repository);

    @Test
    void correlatesDeploymentWithinThirtyMinutesWithoutPenalty() {
        Instant logTime = Instant.parse("2024-01-15T14:30:00Z");
        when(repository.findFirstByDeployedAtBeforeOrderByDeployedAtDesc(logTime))
                .thenReturn(Optional.of(deployment(logTime.minusSeconds(20 * 60))));

        Optional<DeploymentCorrelator.DeploymentCorrelation> result = correlator.correlate(LogEntryDto.builder()
                .timestamp(logTime)
                .build());

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().affectedDeployment()).isEqualTo("payment v1.2.3");
        assertThat(result.orElseThrow().confidencePenalty()).isZero();
    }

    @Test
    void correlatesDeploymentWithinTwoHoursWithPenalty() {
        Instant logTime = Instant.parse("2024-01-15T14:30:00Z");
        when(repository.findFirstByDeployedAtBeforeOrderByDeployedAtDesc(logTime))
                .thenReturn(Optional.of(deployment(logTime.minusSeconds(90 * 60))));

        Optional<DeploymentCorrelator.DeploymentCorrelation> result = correlator.correlate(LogEntryDto.builder()
                .timestamp(logTime)
                .build());

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().confidencePenalty()).isEqualTo(0.3);
    }

    @Test
    void ignoresDeploymentsOlderThanTwoHours() {
        Instant logTime = Instant.parse("2024-01-15T14:30:00Z");
        when(repository.findFirstByDeployedAtBeforeOrderByDeployedAtDesc(logTime))
                .thenReturn(Optional.of(deployment(logTime.minusSeconds(121 * 60))));

        assertThat(correlator.correlate(LogEntryDto.builder().timestamp(logTime).build())).isEmpty();
    }

    private Deployment deployment(Instant deployedAt) {
        return Deployment.builder()
                .serviceName("payment")
                .version("1.2.3")
                .deployedAt(deployedAt)
                .build();
    }
}
