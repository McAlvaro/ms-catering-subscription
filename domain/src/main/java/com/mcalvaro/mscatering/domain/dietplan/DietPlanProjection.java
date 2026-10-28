package com.mcalvaro.mscatering.domain.dietplan;

import com.mcalvaro.mscatering.domain.core.DomainException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Immutable, local BC2 plan snapshot. It intentionally contains no BC3-only data. */
public record DietPlanProjection(UUID id, PlanType type, UUID ownerPatientId, boolean active,
        LocalDate effectiveStart, int durationDays, List<DietPlanDay> days) {
    public DietPlanProjection {
        days = days == null ? List.of() : List.copyOf(days);
        if (id == null || type == null || effectiveStart == null || durationDays <= 0 || days.size() != durationDays
                || (type == PlanType.BASE && ownerPatientId != null)
                || (type == PlanType.PERSONALIZED && ownerPatientId == null)
                || days.stream().map(DietPlanDay::id).collect(Collectors.toSet()).size() != days.size()
                || !days.stream().map(DietPlanDay::dayNumber).collect(Collectors.toSet())
                        .equals(IntStream.rangeClosed(1, durationDays).boxed().collect(Collectors.toSet()))) {
            throw new DomainException("PROJ-001", "Diet-plan schedule is invalid");
        }
    }

    public Optional<UUID> ownerPatient() {
        return Optional.ofNullable(ownerPatientId);
    }

    public Optional<DietPlanDay> day(int dayNumber) {
        return days.stream().filter(day -> day.dayNumber() == dayNumber).findFirst();
    }
}
