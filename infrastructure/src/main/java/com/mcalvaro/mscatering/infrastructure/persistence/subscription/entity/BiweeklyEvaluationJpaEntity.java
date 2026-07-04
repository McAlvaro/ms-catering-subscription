package com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "biweekly_evaluations")
public class BiweeklyEvaluationJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SubscriptionJpaEntity subscription;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "evaluation_number", nullable = false)
    private Integer evaluationNumber;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    public BiweeklyEvaluationJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SubscriptionJpaEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionJpaEntity subscription) {
        this.subscription = subscription;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public Integer getEvaluationNumber() {
        return evaluationNumber;
    }

    public void setEvaluationNumber(Integer evaluationNumber) {
        this.evaluationNumber = evaluationNumber;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }
}
