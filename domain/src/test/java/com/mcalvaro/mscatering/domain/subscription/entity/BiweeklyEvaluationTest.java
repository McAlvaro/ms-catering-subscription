package com.mcalvaro.mscatering.domain.subscription.entity;

import com.mcalvaro.mscatering.domain.core.id.EvaluationId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.subscription.enums.EvaluationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BiweeklyEvaluationTest {

    @Test
    @DisplayName("Should create a pending biweekly evaluation with its scheduled details")
    void shouldCreatePendingBiweeklyEvaluationWhenDetailsAreProvided() {
        // Arrange
        EvaluationId id = EvaluationId.generate();
        PatientId patientId = PatientId.generate();
        LocalDate scheduledDate = LocalDate.of(2026, 2, 15);

        // Act
        BiweeklyEvaluation evaluation = new BiweeklyEvaluation(id, patientId, 2, scheduledDate);

        // Assert
        assertThat(evaluation.getId()).isEqualTo(id);
        assertThat(evaluation.getPatientId()).isEqualTo(patientId);
        assertThat(evaluation.getEvaluationNumber()).isEqualTo(2);
        assertThat(evaluation.getScheduledDate()).isEqualTo(scheduledDate);
        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.PENDING);
        assertThat(evaluation.getCompletedDate()).isNull();
    }

    @Test
    @DisplayName("Should mark the evaluation as completed with the provided completion date")
    void shouldMarkEvaluationCompletedWhenCompletionDateIsProvided() {
        // Arrange
        BiweeklyEvaluation evaluation = new BiweeklyEvaluation(
                EvaluationId.generate(), PatientId.generate(), 1, LocalDate.of(2026, 2, 15));
        LocalDate completedDate = LocalDate.of(2026, 2, 16);

        // Act
        evaluation.markCompleted(completedDate);

        // Assert
        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
        assertThat(evaluation.getCompletedDate()).isEqualTo(completedDate);
    }

    @Test
    @DisplayName("Should cancel a pending evaluation")
    void shouldCancelEvaluationWhenItIsPending() {
        // Arrange
        BiweeklyEvaluation evaluation = new BiweeklyEvaluation(
                EvaluationId.generate(), PatientId.generate(), 1, LocalDate.of(2026, 2, 15));

        // Act
        evaluation.cancel();

        // Assert
        assertThat(evaluation.getStatus()).isEqualTo(EvaluationStatus.CANCELLED);
        assertThat(evaluation.getCompletedDate()).isNull();
    }

    @Test
    @DisplayName("Should reject a null identifier through the inherited entity validation")
    void shouldThrowNullPointerExceptionWhenIdIsNull() {
        // Arrange
        PatientId patientId = PatientId.generate();
        LocalDate scheduledDate = LocalDate.of(2026, 2, 15);

        // Act & Assert
        assertThatThrownBy(() -> new BiweeklyEvaluation(null, patientId, 1, scheduledDate))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Entity id must not be null");
    }
}
