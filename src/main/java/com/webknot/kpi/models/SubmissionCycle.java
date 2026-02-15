package com.webknot.kpi.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "submission_cycles", schema = "dev")
public class SubmissionCycle {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cycle_key", nullable = false, unique = true, length = 7)
    private String cycleKey; // YYYY-MM

    @Column(nullable = false, length = 64)
    private String timezone; // e.g. Asia/Kolkata

    @Column(name = "window_start_at", nullable = false)
    private OffsetDateTime windowStartAt;

    @Column(name = "window_end_at")
    private OffsetDateTime windowEndAt; // nullable

    @Column(name = "manual_closed", nullable = false)
    private boolean manualClosed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy; // FK -> dev.employees(employee_id), nullable

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // getters/setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCycleKey() { return cycleKey; }
    public void setCycleKey(String cycleKey) { this.cycleKey = cycleKey; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public OffsetDateTime getWindowStartAt() { return windowStartAt; }
    public void setWindowStartAt(OffsetDateTime windowStartAt) { this.windowStartAt = windowStartAt; }

    public OffsetDateTime getWindowEndAt() { return windowEndAt; }
    public void setWindowEndAt(OffsetDateTime windowEndAt) { this.windowEndAt = windowEndAt; }

    public boolean isManualClosed() { return manualClosed; }
    public void setManualClosed(boolean manualClosed) { this.manualClosed = manualClosed; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}