package com.mcalvaro.mscatering.infrastructure.persistence.outbox.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.outbox.entity.OutboxMessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxMessageRepository extends JpaRepository<OutboxMessageJpaEntity, UUID> {

    @Query("""
            select message from OutboxMessageJpaEntity message
            where message.processed = false
              and message.availableAt <= :now
              and (message.lockedUntil is null or message.lockedUntil <= :now)
            order by message.created asc, message.id asc
            """)
    List<OutboxMessageJpaEntity> findPendingAvailable(@Param("now") Instant now, Pageable pageable);
}
