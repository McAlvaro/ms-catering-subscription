package com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository;

import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

    @Query("SELECT COUNT(s) FROM SubscriptionJpaEntity s WHERE YEAR(s.contractStartDate) = :year")
    int countSubscriptionsByYear(@Param("year") int year);

    boolean existsByPatientIdAndStatusIn(UUID patientId, List<String> statuses);
}
