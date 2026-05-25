package com.loglens.collector.repository;

import com.loglens.collector.model.LogSource;
import com.loglens.common.enums.LogSourceType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogSourceRepository extends JpaRepository<LogSource, Long> {
    /**
     * Finds enabled log sources.
     */
    List<LogSource> findByEnabledTrue();

    /**
     * Finds log sources by source type.
     */
    List<LogSource> findByType(LogSourceType type);
}
