package com.webknot.kpi.models;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "employee_submission_window_overrides", schema = "dev")
public class EmployeeSubmissionWindowOverride {

    @Id
    @Column(name = "employee_id", nullable = false, length = 255)
    private String employeeId;

    @Column(name = "force_open", nullable = false)
    private boolean forceOpen;

    @Column(name = "force_closed", nullable = false)
    private boolean forceClosed;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public boolean isForceOpen() {
        return forceOpen;
    }

    public void setForceOpen(boolean forceOpen) {
        this.forceOpen = forceOpen;
    }

    public boolean isForceClosed() {
        return forceClosed;
    }

    public void setForceClosed(boolean forceClosed) {
        this.forceClosed = forceClosed;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @PrePersist
    public void prePersist() {
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
