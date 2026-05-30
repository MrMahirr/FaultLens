package com.faultlens.analyzer.repository;

import com.faultlens.analyzer.model.Analysis;
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
     * Finds the most recent analysis for a group.
     */
    Optional<Analysis> findFirstByLogGroupIdOrderByAnalyzedAtDesc(Long logGroupId);

    /**
     * Finds one analysis by group id.
     */
    Optional<Analysis> findByLogGroupId(Long logGroupId);

    /**
     * Finds analyses after a timestamp.
     */
    List<Analysis> findByAnalyzedAtAfter(Instant after);

    /**
     * Finds analyses by source id.
     */
    org.springframework.data.domain.Page<Analysis> findBySourceId(Long sourceId, org.springframework.data.domain.Pageable pageable);

    /**
     * Deletes analyses by source id.
     */
    void deleteBySourceId(Long sourceId);
}
