package com.elina.projects.entity;

import com.elina.authorization.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Plan Line entity representing a line item in a plan version.
 * 
 * Tenant enforcement: Plan lines belong to a specific tenant, plan version, and task.
 * All queries must include tenant_id filter via TenantAwareRepository.
 */
@Entity
@Table(name = "plan_lines", indexes = {
    @Index(name = "idx_plan_lines_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_plan_lines_version_id", columnList = "tenant_id,version_id"),
    @Index(name = "idx_plan_lines_task_id", columnList = "tenant_id,task_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private PlanVersion planVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Column(name = "planned_qty", precision = 18, scale = 2, nullable = false)
    private BigDecimal plannedQty;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "activate_flag", nullable = false)
    private Boolean activateFlag = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) {
            createdOn = LocalDateTime.now();
        }
        if (updatedOn == null) {
            updatedOn = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedOn = LocalDateTime.now();
    }
}

