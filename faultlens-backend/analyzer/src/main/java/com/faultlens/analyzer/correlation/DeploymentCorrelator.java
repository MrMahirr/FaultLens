package com.faultlens.analyzer.correlation;

import com.faultlens.analyzer.model.Deployment;
import com.faultlens.analyzer.repository.DeploymentRepository;
import com.faultlens.common.dto.LogEntryDto;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeploymentCorrelator {
    private final DeploymentRepository repository;

    /**
     * Finds the latest deployment before the log timestamp and computes confidence adjustment.
     */
    public Optional<DeploymentCorrelation> correlate(LogEntryDto entry) {
        if (entry.getTimestamp() == null) {
            return Optional.empty();
        }
        return repository.findFirstByDeployedAtBeforeOrderByDeployedAtDesc(entry.getTimestamp())
                .flatMap(deployment -> toCorrelation(deployment, entry));
    }

    private Optional<DeploymentCorrelation> toCorrelation(Deployment deployment, LogEntryDto entry) {
        long minutes = Duration.between(deployment.getDeployedAt(), entry.getTimestamp()).toMinutes();
        if (minutes < 0 || minutes > 120) {
            return Optional.empty();
        }
        double confidencePenalty = minutes <= 30 ? 0.0 : 0.3;
        return Optional.of(new DeploymentCorrelation(describe(deployment), confidencePenalty));
    }

    private String describe(Deployment deployment) {
        return deployment.getServiceName() + " v" + deployment.getVersion();
    }

    public record DeploymentCorrelation(String affectedDeployment, double confidencePenalty) {
    }
}
