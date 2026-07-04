package com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla `subscriptions`.
 * Representa el Aggregate Root `Subscription` en la base de datos.
 */
@Entity
@Table(name = "subscriptions")
public class SubscriptionJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "diet_plan_id", nullable = false)
    private UUID dietPlanId;

    @Column(name = "contract_code", nullable = false, unique = true, length = 20)
    private String contractCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // Campos del ServiceContract (Embebidos directamente en la tabla)
    @Column(name = "contract_start_date", nullable = false)
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date", nullable = false)
    private LocalDate contractEndDate;

    @Column(name = "contract_duration_days", nullable = false)
    private Integer contractDurationDays;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "accepted_conditions", nullable = false, columnDefinition = "TEXT")
    private String acceptedConditions;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    // Campos de DeliveryPreferences (Embebidos)
    @Column(name = "pref_street", nullable = false)
    private String prefStreet;

    @Column(name = "pref_number")
    private String prefNumber;

    @Column(name = "pref_city", nullable = false)
    private String prefCity;

    @Column(name = "pref_reference")
    private String prefReference;

    @Column(name = "pref_latitude")
    private Double prefLatitude;

    @Column(name = "pref_longitude")
    private Double prefLongitude;

    @Column(name = "pref_phone")
    private String prefPhone;

    @Column(name = "pref_time_start", nullable = false)
    private String prefTimeStart;

    @Column(name = "pref_time_end", nullable = false)
    private String prefTimeEnd;

    @Column(name = "pref_special_instructions")
    private String prefSpecialInstructions;

    // Relaciones
    @OneToOne(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DeliveryCalendarJpaEntity deliveryCalendar;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PauseRequestJpaEntity> pauseRequests = new ArrayList<>();

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BiweeklyEvaluationJpaEntity> evaluations = new ArrayList<>();

    public SubscriptionJpaEntity() {
    }

    // Getters y Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getDietPlanId() {
        return dietPlanId;
    }

    public void setDietPlanId(UUID dietPlanId) {
        this.dietPlanId = dietPlanId;
    }

    public String getContractCode() {
        return contractCode;
    }

    public void setContractCode(String contractCode) {
        this.contractCode = contractCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getContractStartDate() {
        return contractStartDate;
    }

    public void setContractStartDate(LocalDate contractStartDate) {
        this.contractStartDate = contractStartDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(LocalDate contractEndDate) {
        this.contractEndDate = contractEndDate;
    }

    public Integer getContractDurationDays() {
        return contractDurationDays;
    }

    public void setContractDurationDays(Integer contractDurationDays) {
        this.contractDurationDays = contractDurationDays;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getAcceptedConditions() {
        return acceptedConditions;
    }

    public void setAcceptedConditions(String acceptedConditions) {
        this.acceptedConditions = acceptedConditions;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public String getPrefStreet() {
        return prefStreet;
    }

    public void setPrefStreet(String prefStreet) {
        this.prefStreet = prefStreet;
    }

    public String getPrefNumber() {
        return prefNumber;
    }

    public void setPrefNumber(String prefNumber) {
        this.prefNumber = prefNumber;
    }

    public String getPrefCity() {
        return prefCity;
    }

    public void setPrefCity(String prefCity) {
        this.prefCity = prefCity;
    }

    public String getPrefReference() {
        return prefReference;
    }

    public void setPrefReference(String prefReference) {
        this.prefReference = prefReference;
    }

    public Double getPrefLatitude() {
        return prefLatitude;
    }

    public void setPrefLatitude(Double prefLatitude) {
        this.prefLatitude = prefLatitude;
    }

    public Double getPrefLongitude() {
        return prefLongitude;
    }

    public void setPrefLongitude(Double prefLongitude) {
        this.prefLongitude = prefLongitude;
    }

    public String getPrefPhone() {
        return prefPhone;
    }

    public void setPrefPhone(String prefPhone) {
        this.prefPhone = prefPhone;
    }

    public String getPrefTimeStart() {
        return prefTimeStart;
    }

    public void setPrefTimeStart(String prefTimeStart) {
        this.prefTimeStart = prefTimeStart;
    }

    public String getPrefTimeEnd() {
        return prefTimeEnd;
    }

    public void setPrefTimeEnd(String prefTimeEnd) {
        this.prefTimeEnd = prefTimeEnd;
    }

    public String getPrefSpecialInstructions() {
        return prefSpecialInstructions;
    }

    public void setPrefSpecialInstructions(String prefSpecialInstructions) {
        this.prefSpecialInstructions = prefSpecialInstructions;
    }

    public DeliveryCalendarJpaEntity getDeliveryCalendar() {
        return deliveryCalendar;
    }

    public void setDeliveryCalendar(DeliveryCalendarJpaEntity deliveryCalendar) {
        this.deliveryCalendar = deliveryCalendar;
        if (deliveryCalendar != null) {
            deliveryCalendar.setSubscription(this);
        }
    }

    public List<PauseRequestJpaEntity> getPauseRequests() {
        return pauseRequests;
    }

    public void setPauseRequests(List<PauseRequestJpaEntity> pauseRequests) {
        this.pauseRequests.clear();
        if (pauseRequests != null) {
            pauseRequests.forEach(p -> {
                p.setSubscription(this);
                this.pauseRequests.add(p);
            });
        }
    }

    public List<BiweeklyEvaluationJpaEntity> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(List<BiweeklyEvaluationJpaEntity> evaluations) {
        this.evaluations.clear();
        if (evaluations != null) {
            evaluations.forEach(e -> {
                e.setSubscription(this);
                this.evaluations.add(e);
            });
        }
    }
}
