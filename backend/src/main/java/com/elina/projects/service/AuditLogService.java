package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.TenantRepository;
import com.elina.projects.entity.AuditLog;
import com.elina.projects.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for audit logging.
 * 
 * Tenant enforcement: All audit logs are scoped to tenant.
 * This service writes structured audit entries for complex operations.
 * Database triggers also write audit logs automatically for simple CRUD operations.
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, 
                          TenantRepository tenantRepository,
                          ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.tenantRepository = tenantRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Get current user ID from SecurityContext.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Write audit log entry.
     * 
     * @param tableName Name of the table (e.g., "projects", "wbs", "tasks")
     * @param recordId ID of the record being changed
     * @param actionType Action type (INSERT, UPDATE, DELETE)
     * @param oldData Map of old values (can be null for INSERT)
     * @param newData Map of new values (can be null for DELETE)
     */
    @Transactional
    public void writeAuditLog(String tableName, Long recordId, String actionType, 
                             Map<String, Object> oldData, Map<String, Object> newData) {
        try {
            Long tenantId = TenantContext.getTenantId();
            Long userId = getCurrentUserId();

            if (tenantId == null) {
                logger.warn("TenantContext not set, skipping audit log");
                return;
            }

            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new IllegalStateException("Tenant not found"));

            AuditLog auditLog = new AuditLog();
            auditLog.setTenant(tenant);
            auditLog.setTableName(tableName);
            auditLog.setRecordId(recordId);
            auditLog.setActionType(actionType);
            auditLog.setChangedBy(userId != null ? userId : 0L);
            auditLog.setChangedOn(LocalDateTime.now());

            // Convert old/new data to JSON
            if (oldData != null && !oldData.isEmpty()) {
                auditLog.setOldData(objectMapper.writeValueAsString(oldData));
            }
            if (newData != null && !newData.isEmpty()) {
                auditLog.setNewData(objectMapper.writeValueAsString(newData));
            }

            auditLogRepository.save(auditLog);
            logger.debug("Audit log written: {} {} on {}.{}", actionType, recordId, tableName, recordId);

        } catch (Exception e) {
            // Don't fail the transaction if audit logging fails
            logger.error("Failed to write audit log: {} {} on {}.{}", 
                actionType, recordId, tableName, recordId, e);
        }
    }

    /**
     * Write audit log entry with object data.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void writeAuditLog(String tableName, Long recordId, String actionType, 
                             Object oldData, Object newData) {
        try {
            Map<String, Object> oldMap = oldData != null ? (Map<String, Object>) objectMapper.convertValue(oldData, Map.class) : null;
            Map<String, Object> newMap = newData != null ? (Map<String, Object>) objectMapper.convertValue(newData, Map.class) : null;
            writeAuditLog(tableName, recordId, actionType, oldMap, newMap);
        } catch (Exception e) {
            logger.error("Failed to convert audit data to Map", e);
        }
    }
}

