package com.elina.projects.repository;

import com.elina.authorization.repository.TenantAwareRepository;
import com.elina.projects.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity with tenant-aware queries.
 * 
 * Tenant enforcement: All queries automatically filter by tenant_id from TenantContext.
 */
@Repository
public interface AuditLogRepository extends TenantAwareRepository<AuditLog, Long> {
    
    /**
     * Find audit logs for a specific table and record.
     */
    @Query("SELECT al FROM AuditLog al WHERE al.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND al.tableName = :tableName AND al.recordId = :recordId " +
           "ORDER BY al.changedOn DESC")
    List<AuditLog> findByTableNameAndRecordId(
        @Param("tableName") String tableName,
        @Param("recordId") Long recordId
    );

    /**
     * Find audit logs with filtering and pagination.
     */
    @Query("SELECT al FROM AuditLog al WHERE al.tenant.id = :#{T(com.elina.authorization.context.TenantContext).getTenantId()} " +
           "AND (:tableName IS NULL OR al.tableName = :tableName) " +
           "AND (:recordId IS NULL OR al.recordId = :recordId) " +
           "AND (:actionType IS NULL OR al.actionType = :actionType) " +
           "AND (:startDate IS NULL OR al.changedOn >= :startDate) " +
           "AND (:endDate IS NULL OR al.changedOn <= :endDate) " +
           "ORDER BY al.changedOn DESC")
    Page<AuditLog> findWithFilters(
        @Param("tableName") String tableName,
        @Param("recordId") Long recordId,
        @Param("actionType") String actionType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}

