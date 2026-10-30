package com.mcalvaro.mscatering.infrastructure.persistence.outbox.repository;

import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.mapper.OutboxMessageMapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOutboxMessageRepository implements OutboxMessageRepository {

    private final EntityManager entityManager;
    private final SpringDataOutboxMessageRepository springDataRepository;
    private final OutboxMessageMapper mapper;

    public JpaOutboxMessageRepository(EntityManager entityManager,
            SpringDataOutboxMessageRepository springDataRepository,
            OutboxMessageMapper mapper) {
        this.entityManager = entityManager;
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(OutboxMessage message) {
        entityManager.persist(mapper.toJpaEntity(Objects.requireNonNull(message, "Outbox message must not be null")));
        entityManager.flush();
    }

    @Override
    public Optional<OutboxMessage> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toApplication);
    }

    @Override
    public List<OutboxMessage> findPendingAvailable(Instant now, int limit) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        return List.copyOf(springDataRepository.findPendingAvailable(now, PageRequest.of(0, limit)).stream()
                .map(mapper::toApplication)
                .toList());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<OutboxMessage> tryClaim(UUID id, Instant now, Instant leaseUntil, String lockToken) {
        requireId(id);
        Objects.requireNonNull(now, "Current time must not be null");
        Objects.requireNonNull(leaseUntil, "Lease expiry must not be null");
        requireNonBlank(lockToken, "Lock token must not be blank");
        if (!leaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("Lease expiry must be after current time");
        }
        if (springDataRepository.claimIfAvailable(id, now, leaseUntil, lockToken) != 1) {
            return Optional.empty();
        }
        return springDataRepository.findById(id).map(mapper::toApplication);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessedIfOwned(UUID id, String lockToken, Instant processedOn) {
        requireId(id);
        requireNonBlank(lockToken, "Lock token must not be blank");
        Objects.requireNonNull(processedOn, "Processed time must not be null");
        return springDataRepository.markProcessedIfOwned(id, lockToken, processedOn) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean rescheduleAfterFailureIfOwned(UUID id, String lockToken, Instant availableAt, String lastError) {
        requireId(id);
        requireNonBlank(lockToken, "Lock token must not be blank");
        Objects.requireNonNull(availableAt, "Availability time must not be null");
        requireNonBlank(lastError, "Last error must not be blank");
        return springDataRepository.rescheduleAfterFailureIfOwned(id, lockToken, availableAt, lastError) == 1;
    }

    private static void requireId(UUID id) {
        Objects.requireNonNull(id, "Outbox message id must not be null");
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
