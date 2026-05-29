package com.faultlens.analyzer.repository;

import com.faultlens.analyzer.model.Deployment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    /**
     * Finds deployments before a timestamp, newest first.
     */
    List<Deployment> findByDeployedAtBeforeOrderByDeployedAtDesc(Instant deployedAt, Pageable pageable);

    /**
     * Finds the latest deployment before a timestamp.
     */
    Optional<Deployment> findFirstByDeployedAtBeforeOrderByDeployedAtDesc(Instant before);

    /**
     * Finds deployments newest first.
     */
    Page<Deployment> findAllByOrderByDeployedAtDesc(Pageable pageable);
}
