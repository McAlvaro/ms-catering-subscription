package com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "delivery_days")
public class DeliveryDayJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private DeliveryCalendarJpaEntity calendar;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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

    @Column(name = "time_start", nullable = false)
    private String timeStart;

    @Column(name = "time_end", nullable = false)
    private String timeEnd;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "consolidated_at")
    private Instant consolidatedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failure_reason")
    private String failureReason;

    public DeliveryDayJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DeliveryCalendarJpaEntity getCalendar() {
        return calendar;
    }

    public void setCalendar(DeliveryCalendarJpaEntity calendar) {
        this.calendar = calendar;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Instant getConsolidatedAt() {
        return consolidatedAt;
    }

    public void setConsolidatedAt(Instant consolidatedAt) {
        this.consolidatedAt = consolidatedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
