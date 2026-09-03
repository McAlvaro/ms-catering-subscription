package com.mcalvaro.mscatering.domain.subscription.service;

import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.PlanDuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBiweeklyEvaluationGeneratorTest {

    @Test
    @DisplayName("Should generate one biweekly evaluation on the fifteenth plan day")
    void shouldGenerateOneEvaluationWhenDurationIsBiweekly() {
        // Arrange
        DefaultBiweeklyEvaluationGenerator generator = new DefaultBiweeklyEvaluationGenerator();
        UUID patientId = UUID.randomUUID();

        // Act
        List<BiweeklyEvaluation> evaluations = generator.generate(patientId, LocalDate.of(2026, 1, 5),
                PlanDuration.BIWEEKLY);

        // Assert
        assertThat(evaluations).singleElement().satisfies(evaluation -> {
            assertThat(evaluation.getPatientId()).isEqualTo(patientId);
            assertThat(evaluation.getEvaluationNumber()).isEqualTo(1);
            assertThat(evaluation.getScheduledDate()).isEqualTo(LocalDate.of(2026, 1, 19));
        });
    }

    @Test
    @DisplayName("Should generate two monthly evaluations on the fifteenth and thirtieth plan days")
    void shouldGenerateTwoEvaluationsWhenDurationIsMonthly() {
        // Arrange
        DefaultBiweeklyEvaluationGenerator generator = new DefaultBiweeklyEvaluationGenerator();

        // Act
        List<BiweeklyEvaluation> evaluations = generator.generate(UUID.randomUUID(), LocalDate.of(2026, 1, 5),
                PlanDuration.MONTHLY);

        // Assert
        assertThat(evaluations).extracting(BiweeklyEvaluation::getEvaluationNumber).containsExactly(1, 2);
        assertThat(evaluations).extracting(BiweeklyEvaluation::getScheduledDate)
                .containsExactly(LocalDate.of(2026, 1, 19), LocalDate.of(2026, 2, 3));
    }

    @Test
    @DisplayName("Should move Saturday evaluation dates to the following Monday")
    void shouldMoveEvaluationToMondayWhenCalculatedDateIsSaturday() {
        // Arrange
        DefaultBiweeklyEvaluationGenerator generator = new DefaultBiweeklyEvaluationGenerator();

        // Act
        List<BiweeklyEvaluation> evaluations = generator.generate(UUID.randomUUID(), LocalDate.of(2026, 2, 21),
                PlanDuration.BIWEEKLY);

        // Assert
        assertThat(evaluations).singleElement()
                .extracting(BiweeklyEvaluation::getScheduledDate)
                .isEqualTo(LocalDate.of(2026, 3, 9));
    }

    @Test
    @DisplayName("Should move Sunday evaluation dates to the following Monday")
    void shouldMoveEvaluationToMondayWhenCalculatedDateIsSunday() {
        // Arrange
        DefaultBiweeklyEvaluationGenerator generator = new DefaultBiweeklyEvaluationGenerator();

        // Act
        List<BiweeklyEvaluation> evaluations = generator.generate(UUID.randomUUID(), LocalDate.of(2026, 2, 22),
                PlanDuration.BIWEEKLY);

        // Assert
        assertThat(evaluations).singleElement()
                .extracting(BiweeklyEvaluation::getScheduledDate)
                .isEqualTo(LocalDate.of(2026, 3, 9));
    }

    @Test
    @DisplayName("Should return no evaluations when plan duration is null")
    void shouldReturnNoEvaluationsWhenPlanDurationIsNull() {
        // Arrange
        DefaultBiweeklyEvaluationGenerator generator = new DefaultBiweeklyEvaluationGenerator();

        // Act
        List<BiweeklyEvaluation> evaluations = generator.generate(UUID.randomUUID(), LocalDate.of(2026, 1, 5), null);

        // Assert
        assertThat(evaluations).isEmpty();
    }
}
