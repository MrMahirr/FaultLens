package com.faultlens.api.repository;

import com.faultlens.api.model.AlarmRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmRuleRepository extends JpaRepository<AlarmRule, Long> {
    List<AlarmRule> findByEnabledTrue();

    List<AlarmRule> findAllByOrderByCreatedAtDesc();
}
