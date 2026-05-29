package com.faultlens.processor.repository;

import com.faultlens.common.enums.Severity;
import com.faultlens.processor.model.LogEntry;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {
    /**
     * Finds entries by severity and time range.
     */
    List<LogEntry> findBySeverityAndTimestampBetween(Severity severity, Instant from, Instant to);

    /**
     * Finds entries by severities and time range.
     */
    Page<LogEntry> findBySeverityInAndTimestampBetween(List<Severity> severities, Instant from, Instant to, Pageable pageable);

    /**
     * Finds entries belonging to a group.
     */
    Page<LogEntry> findByGroupId(Long groupId, Pageable pageable);

    /**
     * Finds entries by source id.
     */
    Page<LogEntry> findBySourceId(Long sourceId, Pageable pageable);

    /**
     * Deletes all entries for a specific source id.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM LogEntry e WHERE e.sourceId = :sourceId")
    void deleteBySourceId(@Param("sourceId") Long sourceId);

    /**
     * Counts entries by severity after a point in time.
     */
    long countBySeverityAndTimestampAfter(Severity severity, Instant timestamp);

    /**
     * Counts entries by severity since a timestamp.
     */
    @Query(value = "SELECT severity, COUNT(*) as count FROM log_entries WHERE timestamp > :since GROUP BY severity", nativeQuery = true)
    List<Object[]> countBySeverityGroupSince(@Param("since") Instant since);
}
