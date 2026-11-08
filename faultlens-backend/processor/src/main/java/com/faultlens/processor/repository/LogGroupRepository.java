package com.faultlens.processor.repository;

import com.faultlens.common.enums.GroupStatus;
import com.faultlens.common.enums.Severity;
import java.util.Collection;
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
     * Groups in the given statuses, optionally of one severity and assignee (P10 / 2.1, 2.2).
     * Merged groups are left out: their entries go to the target. Sorted by the pageable.
     */
    @Query("select g from LogGroup g where g.status in :statuses and g.mergedIntoId is null"
            + " and (:severity is null or g.severity = :severity)"
            + " and (:assignee is null or g.assignee = :assignee)")
    Page<LogGroup> search(@Param("statuses") Collection<GroupStatus> statuses, @Param("severity") Severity severity,
                          @Param("assignee") String assignee, Pageable pageable);
}
