package com.elina.projects.entity;

import com.elina.authorization.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit Log entity for tracking changes to important tables.
 * 
 * Tenant enforcement: Audit logs belong to a specific tenant.
 * All queries must include tenant_id filter via TenantAwareRepository.
 * 
 * This table is populated via database triggers and service-level audit logging.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_audit_logs_table_record", columnList = "tenant_id,table_name,record_id"),
    @Index(name = "idx_audit_logs_changed_on", columnList = "tenant_id,changed_on"),
    @Index(name = "idx_audit_logs_changed_by", columnList = "tenant_id,changed_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName; // projects, wbs, tasks, confirmations, task_updates

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType; // INSERT, UPDATE, DELETE

    @Column(name = "old_data", columnDefinition = "NVARCHAR(MAX)")
    private String oldData; // JSON representation of old values

    @Column(name = "new_data", columnDefinition = "NVARCHAR(MAX)")
    private String newData; // JSON representation of new values

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_on", nullable = false, updatable = false)
    private LocalDateTime changedOn;

    @PrePersist
    protected void onCreate() {
        if (changedOn == null) {
            changedOn = LocalDateTime.now();
        }
    }
}

