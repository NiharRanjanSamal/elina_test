package com.elina.authorization.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object passed to business rule validators.
 * Contains all relevant data for rule validation.
 * 
 * This context is populated by the calling service (TaskService, WbsService, etc.)
 * and passed to the BusinessRuleEngine for validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRuleContext {

    // Tenant and User Information
    private Long tenantId;
    private Long userId;

    // Entity Information
    private String entityType; // TASK_UPDATE, WBS, TASK, CONFIRMATION, PLAN_VERSION, ALLOCATION, ATTENDANCE, MATERIAL_USAGE
    private Long entityId;

    // Quantity Information
    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private BigDecimal updateQty;
    private BigDecimal dailyUpdateQty;

    // Date Information
    private LocalDate updateDate;
    private LocalDate confirmationDate;
    private LocalDate lockDate;
    private LocalDate taskStartDate;
    private LocalDate taskEndDate;
    private LocalDate wbsStartDate;
    private LocalDate wbsEndDate;
    private LocalDate allocationStartDate;
    private LocalDate allocationEndDate;
    private LocalDate attendanceDate;
    private LocalDate materialUsageDate;
    private LocalDate planVersionDate;

    // Status Information
    private String taskStatus;
    private String wbsStatus;
    private String confirmationStatus;
    private Boolean isLocked;
    private Boolean isConfirmed;

    // Additional dynamic parameters (for extensibility)
    @Builder.Default
    private Map<String, Object> additionalParams = new HashMap<>();

    /**
     * Add additional parameter for custom rule validation.
     */
    public BusinessRuleContext addParam(String key, Object value) {
        if (additionalParams == null) {
            additionalParams = new HashMap<>();
        }
        additionalParams.put(key, value);
        return this;
    }

    /**
     * Get additional parameter.
     */
    @SuppressWarnings("unchecked")
    public <T> T getParam(String key, Class<T> type) {
        if (additionalParams == null) {
            return null;
        }
        Object value = additionalParams.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Get additional parameter as String.
     */
    public String getParamAsString(String key) {
        Object value = additionalParams != null ? additionalParams.get(key) : null;
        return value != null ? value.toString() : null;
    }
}

