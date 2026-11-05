package com.faultlens.collector.repository;

import com.faultlens.collector.model.LogSource;
import com.faultlens.common.enums.LogSourceType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LogSourceRepository extends JpaRepository<LogSource, Long> {
    /**
     * Finds enabled log sources.
     */
    List<LogSource> findByEnabledTrue();

    /**
     * Finds log sources by source type.
     */
    List<LogSource> findByType(LogSourceType type);

    /**
     * Writes only last_seen_at, and never moves it backwards. Unlike save(entity) this cannot
     * overwrite a concurrent edit of the source's name, config or enabled flag.
     */
    @Modifying
    @Transactional
    @Query("update LogSource s set s.lastSeenAt = :seenAt where s.id = :id and (s.lastSeenAt is null or s.lastSeenAt < :seenAt)")
    int touchLastSeenAt(@Param("id") Long id, @Param("seenAt") Instant seenAt);
}
