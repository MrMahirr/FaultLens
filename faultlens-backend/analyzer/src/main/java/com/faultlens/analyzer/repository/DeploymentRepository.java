package com.faultlens.analyzer.repository;

import com.faultlens.analyzer.model.Deployment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Deployment history page, newest first, filtered in the database (not on one page in the
     * browser). Both filters are always bound to non-null values, so PostgreSQL never has to
     * guess the type of a null parameter: {@code servicePattern} is a lower-case LIKE pattern
     * with {@code !} as escape ("%" = any service), {@code environment} "" means any.
     */
    @Query(value = """
            select d from Deployment d
            where lower(d.serviceName) like :servicePattern escape '!'
              and (:environment = '' or d.environment = :environment)
            order by d.deployedAt desc, d.id desc""",
            countQuery = """
            select count(d) from Deployment d
            where lower(d.serviceName) like :servicePattern escape '!'
              and (:environment = '' or d.environment = :environment)""")
    Page<Deployment> search(@Param("servicePattern") String servicePattern,
                            @Param("environment") String environment,
                            Pageable pageable);
}
