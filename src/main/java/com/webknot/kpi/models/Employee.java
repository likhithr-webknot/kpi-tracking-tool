package com.webknot.kpi.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Entity
@Table(name = "employees", schema = "dev")
public class Employee {

    @Id
    @Column(name = "employee_id", nullable = false, length = 255)
    private String employeeId;

    @Column(name = "employee_name", length = 255)
    private String employeeName;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "emp_role", columnDefinition = "dev.employee_role")
    private EmployeeRole empRole = EmployeeRole.Employee;

    @Column(name = "stream", nullable = false)
    private String stream;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "band", columnDefinition = "dev.current_band", nullable = false)
    private CurrentBand band = CurrentBand.B8;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Employee updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Employee() {
    }

    public Employee(String employeeId,
                    String employeeName,
                    String email,
                    EmployeeRole empRole,
                    String stream,
                    CurrentBand band,
                    Employee manager,
                    Employee updatedBy,
                    LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.email = email;
        this.empRole = empRole;
        this.stream = stream;
        this.band = band;
        this.manager = manager;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public EmployeeRole getEmpRole() {
        return empRole;
    }

    public void setEmpRole(EmployeeRole empRole) {
        this.empRole = empRole;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public CurrentBand getBand() {
        return band;
    }

    public void setBand(CurrentBand band) {
        this.band = band;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    public Employee getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Employee updatedBy) {
        this.updatedBy = updatedBy;
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
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Employee{" +
                "employeeId='" + employeeId + '\'' +
                ", employeeName='" + employeeName + '\'' +
                ", email='" + email + '\'' +
                ", empRole=" + empRole +
                ", stream='" + stream + '\'' +
                ", band=" + band +
                ", managerId=" + (manager != null ? manager.getEmployeeId() : null) +
                ", updatedById=" + (updatedBy != null ? updatedBy.getEmployeeId() : null) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
