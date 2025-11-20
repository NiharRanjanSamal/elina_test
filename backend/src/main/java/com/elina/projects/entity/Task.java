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
 * Task entity representing a task within a WBS.
 * 
 * Tenant enforcement: Tasks belong to a specific tenant, project, and WBS.
 * All queries must include tenant_id filter via TenantAwareRepository.
 * 
 * Business rules:
 * - Task start_date cannot be in future (Rule 201)
 * - Task end_date cannot be before start_date
 * - Task dates must be within parent WBS date range
 * - Daily update cannot exceed planned_qty (Rule 401)
 * - Confirmed tasks cannot be modified (Rule 301)
 */
@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_tasks_wbs_id", columnList = "tenant_id,wbs_id"),
    @Index(name = "idx_tasks_tenant_code", columnList = "tenant_id,task_code", unique = true),
    @Index(name = "idx_tasks_tenant_active", columnList = "tenant_id,activate_flag")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wbs_id", nullable = false)
    private Wbs wbs;

    @Column(name = "task_code", nullable = false, length = 50)
    private String taskCode;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "planned_qty", precision = 18, scale = 2)
    private BigDecimal plannedQty;

    @Column(name = "actual_qty", precision = 18, scale = 2)
    private BigDecimal actualQty = BigDecimal.ZERO;

    @Column(name = "unit", length = 20)
    private String unit; // e.g., HOURS, DAYS, QTY

    @Column(name = "status", length = 50)
    private String status; // PLANNED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "is_confirmed")
    private Boolean isConfirmed = false;

    @Column(name = "confirmed_on")
    private LocalDateTime confirmedOn;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "lock_date")
    private LocalDate lockDate;

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

