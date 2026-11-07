package com.faultlens.api.repository;

import com.faultlens.api.model.ApiKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByPrefix(String prefix);

    List<ApiKey> findAllByOrderByCreatedAtDesc();

    /** Usage stamp without loading the entity (called from the auth filter). */
    @Transactional
    @Modifying
    @Query("update ApiKey k set k.lastUsedAt = :at, k.lastUsedIp = :ip where k.id = :id")
    int markUsed(@Param("id") Long id, @Param("at") Instant at, @Param("ip") String ip);
}
