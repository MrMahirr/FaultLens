package com.faultlens.api.repository;

import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.common.enums.Severity;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long>, JpaSpecificationExecutor<Alarm> {
    long countByStatus(AlarmStatus status);

    long countByStatusInAndSeverity(Collection<AlarmStatus> statuses, Severity severity);
}
