package com.mcalvaro.mscatering.infrastructure.persistence.subscription.query;

import com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails.BiweeklyEvaluationDto;
import com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails.DeliveryDayDto;
import com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails.ISubscriptionQueryService;
import com.mcalvaro.mscatering.application.subscription.GetSubscriptionDetails.SubscriptionDetailsDto;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository.SpringDataSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JpaSubscriptionQueryService implements ISubscriptionQueryService {

    private final SpringDataSubscriptionRepository springRepository;

    public JpaSubscriptionQueryService(SpringDataSubscriptionRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionDetailsDto> getSubscriptionDetails(UUID subscriptionId) {
        return springRepository.findById(subscriptionId).map(this::mapToDetailsDto);
    }

    private SubscriptionDetailsDto mapToDetailsDto(SubscriptionJpaEntity entity) {
        List<DeliveryDayDto> deliveryDays = Optional.ofNullable(entity.getDeliveryCalendar())
                .map(cal -> cal.getDeliveryDays().stream()
                        .map(day -> new DeliveryDayDto(
                                day.getId(),
                                day.getDate(),
                                day.getStatus(),
                                day.getAddressStreet(),
                                day.getAddressNumber(),
                                day.getAddressCity(),
                                day.getAddressReference(),
                                day.getAddressLatitude() != null ? day.getAddressLatitude() : 0.0,
                                day.getAddressLongitude() != null ? day.getAddressLongitude() : 0.0,
                                day.getAddressPhone(),
                                LocalTime.parse(day.getTimeStart()),
                                LocalTime.parse(day.getTimeEnd()),
                                day.getInstructions()))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        List<BiweeklyEvaluationDto> evaluations = entity.getEvaluations().stream()
                .map(eval -> new BiweeklyEvaluationDto(
                        eval.getId(),
                        eval.getScheduledDate(),
                        eval.getStatus(),
                        eval.getCompletedDate()))
                .collect(Collectors.toList());

        return new SubscriptionDetailsDto(
                entity.getId(),
                entity.getPatientId(),
                entity.getDietPlanId(),
                entity.getContractCode(),
                entity.getStatus(),
                entity.getContractStartDate(),
                entity.getContractEndDate(),
                entity.getServiceType(),
                entity.getTotalPrice(),
                deliveryDays,
                evaluations);
    }
}
