package com.webknot.kpi.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "kpi_definitions",
        schema = "dev",
        uniqueConstraints = @UniqueConstraint(
                name = "kpi_definitions_band_stream_kpi_name_key",
                columnNames = {"band", "stream", "kpi_name"}
        )
)
public class KpiDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "band", nullable = false, columnDefinition = "dev.current_band")
    private CurrentBand band;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "stream", nullable = false, columnDefinition = "dev.current_stream")
    private CurrentStream stream;

    @NotBlank
    @Column(name = "kpi_name", nullable = false, length = 255)
    private String kpiName;

    @NotNull
    @DecimalMin(value = "0.0000001", inclusive = true)
    @DecimalMax(value = "1.00", inclusive = true)
    @Column(name = "weightage", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightage;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public KpiDefinition() {
    }

    public KpiDefinition(Long id,
                         CurrentBand band,
                         CurrentStream stream,
                         String kpiName,
                         BigDecimal weightage,
                         String description,
                         LocalDateTime createdAt) {
        this.id = id;
        this.band = band;
        this.stream = stream;
        this.kpiName = kpiName;
        this.weightage = weightage;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CurrentBand getBand() { return band; }
    public void setBand(CurrentBand band) { this.band = band; }

    public CurrentStream getStream() { return stream; }
    public void setStream(CurrentStream stream) { this.stream = stream; }

    public String getKpiName() { return kpiName; }
    public void setKpiName(String kpiName) { this.kpiName = kpiName; }

    public BigDecimal getWeightage() { return weightage; }
    public void setWeightage(BigDecimal weightage) { this.weightage = weightage; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "KpiDefinition{" +
                "id=" + id +
                ", band=" + band +
                ", stream=" + stream +
                ", kpiName='" + kpiName + '\'' +
                ", weightage=" + weightage +
                '}';
    }
}