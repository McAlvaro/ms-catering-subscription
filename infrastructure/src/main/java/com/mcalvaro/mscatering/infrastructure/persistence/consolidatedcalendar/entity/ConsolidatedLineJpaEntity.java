package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "consolidated_lines")
public class ConsolidatedLineJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private ConsolidatedCalendarJpaEntity consolidatedCalendar;

    @Column(name = "subscription_id", nullable = false, length = 64)
    private String subscriptionId;

    @Column(name = "patient_id", nullable = false, length = 64)
    private String patientId;
    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "diet_plan_id", nullable = false, length = 64)
    private String dietPlanId;
    @Column(name = "plan_day_number", nullable = false)
    private int planDayNumber;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;
    @Column(name = "address_id", nullable = false, length = 64)
    private String addressId;
    @Column(name = "address_alias", nullable = false)
    private String addressAlias;

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

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsolidatedLineRecipeJpaEntity> recipes = new ArrayList<>();

    public ConsolidatedLineJpaEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ConsolidatedCalendarJpaEntity getConsolidatedCalendar() {
        return consolidatedCalendar;
    }

    public void setConsolidatedCalendar(ConsolidatedCalendarJpaEntity consolidatedCalendar) {
        this.consolidatedCalendar = consolidatedCalendar;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    public String getPatientName() { return patientName; }
    public void setPatientName(String value) { patientName = value; }

    public String getDietPlanId() {
        return dietPlanId;
    }

    public void setDietPlanId(String dietPlanId) {
        this.dietPlanId = dietPlanId;
    }
    public int getPlanDayNumber() { return planDayNumber; }
    public void setPlanDayNumber(int value) { planDayNumber = value; }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
    public String getAddressId() { return addressId; }
    public void setAddressId(String value) { addressId = value; }
    public String getAddressAlias() { return addressAlias; }
    public void setAddressAlias(String value) { addressAlias = value; }

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
    public List<ConsolidatedLineRecipeJpaEntity> getRecipes() { return recipes; }
    public void setRecipes(List<ConsolidatedLineRecipeJpaEntity> value) {
        recipes.clear();
        value.forEach(recipe -> { recipe.setLine(this); recipes.add(recipe); });
    }
}
