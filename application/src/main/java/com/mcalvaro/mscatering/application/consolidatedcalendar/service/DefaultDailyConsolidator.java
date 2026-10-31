package com.mcalvaro.mscatering.application.consolidatedcalendar.service;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.service.DailyConsolidator;
import com.mcalvaro.mscatering.domain.core.id.ConsolidatedCalendarId;
import com.mcalvaro.mscatering.domain.core.id.ConsolidatedLineId;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.dietplan.*;
import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedRecipeSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class DefaultDailyConsolidator implements DailyConsolidator {

    private final ISubscriptionRepository subscriptionRepository;
    private final IDietPlanProjectionRepository dietPlanRepository;
    private final IPatientReferenceRepository patientRepository;

    public DefaultDailyConsolidator(ISubscriptionRepository subscriptionRepository,
            IDietPlanProjectionRepository dietPlanRepository, IPatientReferenceRepository patientRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.dietPlanRepository = dietPlanRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public ConsolidatedCalendar consolidateForDate(LocalDate deliveryDate) {
        ConsolidatedCalendar calendar = ConsolidatedCalendar.create(ConsolidatedCalendarId.generate(), deliveryDate);

        List<Subscription> activeSubscriptions = subscriptionRepository.findConsolidatableSubscriptions();

        List<Resolution> resolutions = new ArrayList<>();
        for (Subscription subscription : activeSubscriptions) {
            List<DeliveryDay> days = subscription.getDeliveryCalendar().getDaysForDate(deliveryDate);

            for (DeliveryDay day : days) {
                if (day.getStatus() == DeliveryDayStatus.SCHEDULED) {
                    resolutions.add(resolve(calendar, subscription, day, deliveryDate));
                }
            }
        }
        // Nothing is mutated until every local projection and snapshot is complete.
        for (Resolution resolution : resolutions) {
            calendar.addLine(resolution.line());
            resolution.day().markAsConsolidated();
            subscriptionRepository.save(resolution.subscription());
        }
        return calendar;
    }

    private Resolution resolve(ConsolidatedCalendar calendar, Subscription subscription, DeliveryDay day,
            LocalDate deliveryDate) {
        DietPlanProjection plan = dietPlanRepository.findById(subscription.getDietPlanId())
                .orElseThrow(() -> failure("PLAN-001", "Diet plan was not found"));
        int ordinal = (int) subscription.getDeliveryCalendar().getDeliveryDays().stream()
                .filter(candidate -> !candidate.getDate().isAfter(deliveryDate))
                .filter(candidate -> candidate.getStatus() != DeliveryDayStatus.PAUSED)
                .count();
        if (ordinal < 1 || ordinal > plan.durationDays()) {
            throw failure("CAL-003", "Resolved plan day is outside the plan duration");
        }
        DietPlanDay planDay = plan.day(ordinal)
                .orElseThrow(() -> failure("PLAN-005", "Diet plan day was not found"));
        String patientName = patientRepository.findById(subscription.getPatientId())
                .map(patient -> patient.patientName())
                .filter(name -> name != null && !name.isBlank())
                .orElseThrow(() -> failure("CAL-004", "Patient snapshot is incomplete"));
        List<ConsolidatedRecipeSnapshot> recipes = planDay.meals().stream()
                .filter(meal -> selected(subscription.getContract().serviceType(), meal.type()))
                .sorted(Comparator.comparing(DietPlanMeal::time))
                .flatMap(meal -> meal.recipes().stream().map(recipe -> new ConsolidatedRecipeSnapshot(recipe.recipeId(),
                        recipe.name(), meal.time(), recipe.portionGrams())))
                .toList();
        if (recipes.isEmpty()) {
            throw failure("CAL-004", "Selected recipe snapshot is incomplete");
        }
        ConsolidatedLine line = new ConsolidatedLine(
                            ConsolidatedLineId.generate(),
                            calendar.getId(),
                            subscription.getId(),
                            subscription.getPatientId(),
                            patientName,
                            subscription.getDietPlanId(),
                            ordinal,
                            subscription.getContract().serviceType(),
                            day.getAddress(),
                            day.getTimeWindow(),
                            day.getInstructions(), recipes);
        return new Resolution(subscription, day, line);
    }

    private boolean selected(ServiceType service, MealType mealType) {
        return service == ServiceType.FULL || service.name().equals(mealType.name());
    }

    private DomainException failure(String code, String message) { return new DomainException(code, message); }
    private record Resolution(Subscription subscription, DeliveryDay day, ConsolidatedLine line) { }
}
