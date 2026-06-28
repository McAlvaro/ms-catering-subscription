package com.mcalvaro.mscatering.domain.subscription.entity;

import com.mcalvaro.mscatering.domain.core.Entity;
import com.mcalvaro.mscatering.domain.subscription.enums.EvaluationStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing a bi-weekly body evaluation appointment
 * (EvaluacionQuincenal). Automatically generated when a subscription
 * is created: 1 evaluation for 15-day plans, 2 for 30-day plans.
 */
public class BiweeklyEvaluation extends Entity {

    private final UUID patientId;
    private final int evaluationNumber;
    private final LocalDate scheduledDate;
    private EvaluationStatus status;
    private LocalDate completedDate;

    public BiweeklyEvaluation(UUID id, UUID patientId, int evaluationNumber,
            LocalDate scheduledDate) {
        super(id);
        this.patientId = patientId;
        this.evaluationNumber = evaluationNumber;
        this.scheduledDate = scheduledDate;
        this.status = EvaluationStatus.PENDING;
    }

    public void markCompleted(LocalDate completedDate) {
        this.status = EvaluationStatus.COMPLETED;
        this.completedDate = completedDate;
    }

    public void cancel() {
        this.status = EvaluationStatus.CANCELLED;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public int getEvaluationNumber() {
        return evaluationNumber;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }
}
