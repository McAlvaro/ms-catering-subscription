package com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
class SpringDataSubscriptionRepositoryTest {

    @Autowired
    private SpringDataSubscriptionRepository repository;

    @Test
    @DisplayName("Should return active and paused subscriptions for delivery consolidation only")
    void shouldFindOnlyConsolidatableSubscriptionStatuses() {
        // Arrange
        SubscriptionJpaEntity active = subscription("ACTIVE");
        SubscriptionJpaEntity paused = subscription("PAUSED");
        SubscriptionJpaEntity cancelled = subscription("CANCELLED");
        repository.saveAll(List.of(active, paused, cancelled));

        // Act
        List<SubscriptionJpaEntity> result = repository.findByStatusIn(List.of("ACTIVE", "PAUSED"));

        // Assert
        assertThat(result).extracting(SubscriptionJpaEntity::getId)
                .containsExactlyInAnyOrder(active.getId(), paused.getId())
                .doesNotContain(cancelled.getId());
    }

    private SubscriptionJpaEntity subscription(String status) {
        SubscriptionJpaEntity entity = new SubscriptionJpaEntity();
        entity.setId("SUS-" + UUID.randomUUID());
        entity.setPatientId("PAC-" + UUID.randomUUID());
        entity.setDietPlanId("PLAN-" + UUID.randomUUID());
        entity.setPlanType("BASE");
        entity.setContractCode("NTC-2026-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setStatus(status);
        entity.setContractStartDate(LocalDate.of(2026, 1, 1));
        entity.setContractEndDate(LocalDate.of(2026, 1, 15));
        entity.setContractDurationDays(15);
        entity.setServiceType("FULL");
        entity.setTotalPrice(new BigDecimal("250.00"));
        entity.setAcceptedConditions("Accepted terms");
        entity.setSignedAt(Instant.parse("2025-12-01T10:00:00Z"));
        entity.setPrefStreet("Main Street");
        entity.setPrefCity("Madrid");
        entity.setPrefTimeStart("09:00");
        entity.setPrefTimeEnd("11:00");
        return entity;
    }
}
