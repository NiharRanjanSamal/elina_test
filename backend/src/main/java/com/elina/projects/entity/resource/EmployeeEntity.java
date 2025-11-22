package com.elina.projects.entity.resource;

import com.elina.authorization.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Employee master data for resource allocations.
 *
 * <p>Tenant enforcement: Each employee belongs to a tenant. All repository
 * queries must filter by {@code tenant_id} via {@link com.elina.authorization.repository.TenantAwareRepository}.</p>
 */
@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_employees_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_employees_tenant_active", columnList = "tenant_id,activate_flag")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "skill_level", length = 100)
    private String skillLevel;

    @Column(name = "rate_per_day", precision = 18, scale = 2, nullable = false)
    private BigDecimal ratePerDay;

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

