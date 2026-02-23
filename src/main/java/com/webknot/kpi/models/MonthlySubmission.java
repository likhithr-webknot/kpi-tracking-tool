package com.webknot.kpi.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "monthly_submissions",
        schema = "dev",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "monthly_submissions_employee_month_type_key",
                        columnNames = {"employee_id", "month", "submission_type"}
                )
        }
)
public class MonthlySubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "month", nullable = false, length = 7)
    private String month;

    @Column(name = "submission_type", nullable = false, length = 64)
    private String submissionType;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "review_status", length = 64)
    private String reviewStatus;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(name = "manager_review_json", columnDefinition = "text")
    private String managerReviewJson;

    @Column(name = "admin_review_json", columnDefinition = "text")
    private String adminReviewJson;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "manager_submitted_at")
    private LocalDateTime managerSubmittedAt;

    @Column(name = "admin_submitted_at")
    private LocalDateTime adminSubmittedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getManagerReviewJson() {
        return managerReviewJson;
    }

    public void setManagerReviewJson(String managerReviewJson) {
        this.managerReviewJson = managerReviewJson;
    }

    public String getAdminReviewJson() {
        return adminReviewJson;
    }

    public void setAdminReviewJson(String adminReviewJson) {
        this.adminReviewJson = adminReviewJson;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getManagerSubmittedAt() {
        return managerSubmittedAt;
    }

    public void setManagerSubmittedAt(LocalDateTime managerSubmittedAt) {
        this.managerSubmittedAt = managerSubmittedAt;
    }

    public LocalDateTime getAdminSubmittedAt() {
        return adminSubmittedAt;
    }

    public void setAdminSubmittedAt(LocalDateTime adminSubmittedAt) {
        this.adminSubmittedAt = adminSubmittedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
