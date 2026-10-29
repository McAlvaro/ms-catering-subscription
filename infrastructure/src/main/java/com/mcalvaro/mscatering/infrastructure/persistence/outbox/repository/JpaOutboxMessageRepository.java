package com.mcalvaro.mscatering.infrastructure.persistence.outbox.repository;

import com.mcalvaro.mscatering.application.outbox.OutboxMessage;
import com.mcalvaro.mscatering.application.outbox.OutboxMessageRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.outbox.mapper.OutboxMessageMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOutboxMessageRepository implements OutboxMessageRepository {

    private final SpringDataOutboxMessageRepository springDataRepository;
    private final OutboxMessageMapper mapper;

    public JpaOutboxMessageRepository(SpringDataOutboxMessageRepository springDataRepository,
            OutboxMessageMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(OutboxMessage message) {
        springDataRepository
                .save(mapper.toJpaEntity(Objects.requireNonNull(message, "Outbox message must not be null")));
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
}
