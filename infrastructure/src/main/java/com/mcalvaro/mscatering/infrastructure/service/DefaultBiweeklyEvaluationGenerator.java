package com.mcalvaro.mscatering.infrastructure.service;

import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;
import com.mcalvaro.mscatering.domain.subscription.service.BiweeklyEvaluationGenerator;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultBiweeklyEvaluationGenerator implements BiweeklyEvaluationGenerator {

    @Override
    public List<BiweeklyEvaluation> generate(UUID patientId, LocalDate startDate, PlanDuration duration) {
        List<BiweeklyEvaluation> evaluations = new ArrayList<>();

        if (duration == PlanDuration.BIWEEKLY) {
            // 1 evaluación en el día 15 (endDate)
            LocalDate evalDate = startDate.plusDays(15);
            evaluations.add(new BiweeklyEvaluation(
                    UUID.randomUUID(),
                    patientId,
                    1,
                    adjustToWorkingDay(evalDate)));
        } else if (duration == PlanDuration.MONTHLY) {
            // 2 evaluaciones: en el día 15 y en el día 30 (endDate)
            LocalDate eval1Date = startDate.plusDays(15);
            LocalDate eval2Date = startDate.plusDays(30);

            evaluations.add(new BiweeklyEvaluation(
                    UUID.randomUUID(),
                    patientId,
                    1,
                    adjustToWorkingDay(eval1Date)));
            evaluations.add(new BiweeklyEvaluation(
                    UUID.randomUUID(),
                    patientId,
                    2,
                    adjustToWorkingDay(eval2Date)));
        }

        return evaluations;
    }

    private LocalDate adjustToWorkingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            return date.plusDays(2);
        } else if (dayOfWeek == DayOfWeek.SUNDAY) {
            return date.plusDays(1);
        }
        return date;
    }
}
