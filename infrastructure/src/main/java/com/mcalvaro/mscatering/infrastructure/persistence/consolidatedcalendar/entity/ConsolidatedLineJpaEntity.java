package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

import java.util.UUID;

@Entity
@Table(name = "consolidated_lines")
public class ConsolidatedLineJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ConsolidatedCalendarJpaEntity consolidatedCalendar;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "diet_plan_id", nullable = false)
    private UUID dietPlanId;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;

    // DeliveryAddress
    @Column(name = "address_street", nullable = false)
    private String addressStreet;

    @Column(name = "address_number")
    private String addressNumber;

    @Column(name = "address_city", nullable = false)
    private String addressCity;

    @Column(name = "address_reference")
    private String addressReference;

    @Column(name = "address_latitude")
    private Double addressLatitude;

    @Column(name = "address_longitude")
    private Double addressLongitude;

    @Column(name = "address_phone")
    private String addressPhone;

    // TimeWindow
    @Column(name = "time_start", nullable = false)
    private String timeStart;

    @Column(name = "time_end", nullable = false)
    private String timeEnd;

    @Column(name = "instructions")
    private String instructions;

    public ConsolidatedLineJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConsolidatedCalendarJpaEntity getConsolidatedCalendar() {
        return consolidatedCalendar;
    }

    public void setConsolidatedCalendar(ConsolidatedCalendarJpaEntity consolidatedCalendar) {
        this.consolidatedCalendar = consolidatedCalendar;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getAddressStreet() {
        return addressStreet;
    }

    public void setAddressStreet(String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public String getAddressNumber() {
        return addressNumber;
    }

    public void setAddressNumber(String addressNumber) {
        this.addressNumber = addressNumber;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressReference() {
        return addressReference;
    }

    public void setAddressReference(String addressReference) {
        this.addressReference = addressReference;
    }

    public Double getAddressLatitude() {
        return addressLatitude;
    }

    public void setAddressLatitude(Double addressLatitude) {
        this.addressLatitude = addressLatitude;
    }

    public Double getAddressLongitude() {
        return addressLongitude;
    }

    public void setAddressLongitude(Double addressLongitude) {
        this.addressLongitude = addressLongitude;
    }

    public String getAddressPhone() {
        return addressPhone;
    }

    public void setAddressPhone(String addressPhone) {
        this.addressPhone = addressPhone;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
