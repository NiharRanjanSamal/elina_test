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
 * Task Update entity representing a day-wise update for a task.
 * 
 * Tenant enforcement: Task updates belong to a specific tenant and task.
 * All queries must include tenant_id filter via TenantAwareRepository.
 * 
 * Business rules:
 * - Update date cannot be older than allowed backdate (Rule 101: BACKDATE_ALLOWED_TILL)
 * - Daily update qty cannot exceed planned_qty (Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY)
 * - Update date cannot be after lock date if backdate not allowed (Rule 102)
 * - Update date cannot be in future (Rule 201)
 */
@Entity
@Table(name = "task_updates", indexes = {
    @Index(name = "idx_task_updates_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_task_updates_task_id", columnList = "tenant_id,task_id"),
    @Index(name = "idx_task_updates_date", columnList = "tenant_id,task_id,update_date", unique = true),
    @Index(name = "idx_task_updates_tenant_active", columnList = "tenant_id,activate_flag")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "update_id")
    private Long updateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "update_date", nullable = false)
    private LocalDate updateDate;

    @Column(name = "planned_qty", precision = 18, scale = 2)
    private BigDecimal plannedQty;

    @Column(name = "actual_qty", precision = 18, scale = 2, nullable = false)
    private BigDecimal actualQty = BigDecimal.ZERO;

    @Column(name = "daily_update_qty", precision = 18, scale = 2)
    private BigDecimal dailyUpdateQty; // The increment for this day

    @Column(name = "remarks", length = 1000)
    private String remarks;

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

