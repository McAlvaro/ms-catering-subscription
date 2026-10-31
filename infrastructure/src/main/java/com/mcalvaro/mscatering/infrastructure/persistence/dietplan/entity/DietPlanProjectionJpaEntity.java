package com.mcalvaro.mscatering.infrastructure.persistence.dietplan.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diet_plan_projections")
public class DietPlanProjectionJpaEntity {
    @Id
    @Column(name = "id", length = 64)
    private String id;
    @Column(name = "plan_type", nullable = false) private String planType;
    @Column(name = "owner_patient_id", length = 64) private String ownerPatientId;
    @Column(nullable = false) private boolean active;
    @Column(name = "effective_start", nullable = false) private LocalDate effectiveStart;
    @Column(name = "duration_days", nullable = false) private int durationDays;
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DietPlanDayJpaEntity> days = new ArrayList<>();
    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getPlanType() { return planType; } public void setPlanType(String value) { planType = value; }
    public String getOwnerPatientId() { return ownerPatientId; } public void setOwnerPatientId(String value) { ownerPatientId = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    public LocalDate getEffectiveStart() { return effectiveStart; } public void setEffectiveStart(LocalDate value) { effectiveStart = value; }
    public int getDurationDays() { return durationDays; } public void setDurationDays(int value) { durationDays = value; }
    public List<DietPlanDayJpaEntity> getDays() { return days; }
    public void setDays(List<DietPlanDayJpaEntity> value) { days.clear(); value.forEach(day -> { day.setPlan(this); days.add(day); }); }
}
