package com.elina.projects.entity;

import com.elina.authorization.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Confirmation entity representing a frozen snapshot of WBS progress for a specific date.
 *
 * Tenant enforcement: All confirmation records belong to a tenant and WBS.
 * Every query must be scoped by tenant_id via TenantAwareRepository.
 */
@Entity
@Table(
    name = "confirmations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_confirmations_entity_date",
            columnNames = {"tenant_id", "entity_type", "entity_id", "confirmation_date"}
        )
    },
    indexes = {
        @Index(name = "idx_confirmations_tenant", columnList = "tenant_id"),
        @Index(name = "idx_confirmations_entity", columnList = "tenant_id,entity_type,entity_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(of = {"confirmationId", "entityType", "entityId", "confirmationDate"})
public class ConfirmationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "confirmation_id")
    private Long confirmationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "confirmation_date", nullable = false)
    private LocalDate confirmationDate;

    @Column(name = "confirmed_by", nullable = false)
    private Long confirmedBy;

    @Column(name = "confirmed_on", nullable = false, updatable = false)
    private LocalDateTime confirmedOn;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) {
            createdOn = LocalDateTime.now();
        }
    }
}


