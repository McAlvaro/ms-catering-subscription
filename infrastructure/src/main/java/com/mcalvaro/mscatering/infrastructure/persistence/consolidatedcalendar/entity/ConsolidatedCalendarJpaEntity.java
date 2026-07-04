package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "consolidated_calendars")
public class ConsolidatedCalendarJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_deliveries", nullable = false)
    private int totalDeliveries;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private String closedBy;

    @OneToMany(mappedBy = "consolidatedCalendar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsolidatedLineJpaEntity> lines = new ArrayList<>();

    public ConsolidatedCalendarJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public int getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(int totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public List<ConsolidatedLineJpaEntity> getLines() {
        return lines;
    }

    public void setLines(List<ConsolidatedLineJpaEntity> lines) {
        this.lines.clear();
        if (lines != null) {
            lines.forEach(l -> {
                l.setConsolidatedCalendar(this);
                this.lines.add(l);
            });
        }
    }
}
