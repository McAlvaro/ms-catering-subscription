package com.mcalvaro.mscatering.infrastructure.persistence.outbox.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.outbox.entity.OutboxMessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OutboxMessageJpaEntity message
            set message.lockedUntil = :leaseUntil, message.lockedBy = :lockToken
            where message.id = :id
              and message.processed = false
              and message.availableAt <= :now
              and (message.lockedUntil is null or message.lockedUntil <= :now)
            """)
    int claimIfAvailable(@Param("id") UUID id, @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil, @Param("lockToken") String lockToken);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OutboxMessageJpaEntity message
            set message.processed = true, message.processedOn = :processedOn,
                message.lockedUntil = null, message.lockedBy = null, message.lastError = null
            where message.id = :id and message.processed = false and message.lockedBy = :lockToken
            """)
    int markProcessedIfOwned(@Param("id") UUID id, @Param("lockToken") String lockToken,
            @Param("processedOn") Instant processedOn);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update OutboxMessageJpaEntity message
            set message.attemptCount = message.attemptCount + 1, message.lastError = :lastError,
                message.availableAt = :availableAt, message.lockedUntil = null, message.lockedBy = null
            where message.id = :id and message.processed = false and message.lockedBy = :lockToken
            """)
    int rescheduleAfterFailureIfOwned(@Param("id") UUID id, @Param("lockToken") String lockToken,
            @Param("availableAt") Instant availableAt, @Param("lastError") String lastError);
}
