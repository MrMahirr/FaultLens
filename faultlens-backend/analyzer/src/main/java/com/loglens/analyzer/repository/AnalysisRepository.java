package com.loglens.analyzer.repository;

import com.loglens.analyzer.model.Analysis;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    /**
     * Finds analyses by group id.
     */
    List<Analysis> findByLogGroupIdOrderByAnalyzedAtDesc(Long logGroupId);

    /**
     * Finds one analysis by group id.
     */
    Optional<Analysis> findByLogGroupId(Long logGroupId);

    /**
     * Finds analyses after a timestamp.
     */
    List<Analysis> findByAnalyzedAtAfter(Instant after);
}
