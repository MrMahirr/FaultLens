package com.faultlens.processor.repository;

import com.faultlens.common.enums.Severity;
import com.faultlens.processor.model.LogGroup;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogGroupRepository extends JpaRepository<LogGroup, Long> {
    /**
     * Finds a group by fingerprint.
     */
    Optional<LogGroup> findByFingerprint(String fingerprint);

    /**
     * Finds a group by fingerprint and locks it for update.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select groupEntity from LogGroup groupEntity where groupEntity.fingerprint = :fingerprint")
    Optional<LogGroup> findWithLockByFingerprint(@Param("fingerprint") String fingerprint);

    /**
     * Finds recent groups by severity.
     */
    Page<LogGroup> findBySeverityOrderByLastSeenAtDesc(Severity severity, Pageable pageable);

    /**
     * Finds recent groups.
     */
    Page<LogGroup> findAllByOrderByLastSeenAtDesc(Pageable pageable);
}
