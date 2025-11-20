package com.elina.authorization.service.example;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * EXAMPLE SERVICE: TaskUpdateService
 * 
 * This is an EXAMPLE showing how to integrate Business Rule Engine
 * into your service classes. This file is for reference only.
 * 
 * Key Points:
 * 1. Inject BusinessRuleEngine
 * 2. Build BusinessRuleContext with all relevant data
 * 3. Call businessRuleEngine.validate() before proceeding
 * 4. Handle BusinessRuleException (it will be caught by GlobalExceptionHandler)
 * 
 * Rules enforced in this example:
 * - Rule 101: BACKDATE_ALLOWED_TILL - Validates update date is within allowed backdate limit
 * - Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY - Validates actual quantity doesn't exceed planned
 * - Rule 201: START_DATE_CANNOT_BE_IN_FUTURE - Validates task start date is not in future
 */
@Service
public class TaskUpdateServiceExample {

    private static final Logger logger = LoggerFactory.getLogger(TaskUpdateServiceExample.class);

    private final BusinessRuleEngine businessRuleEngine;
    // private final TaskRepository taskRepository;
    // private final TaskUpdateRepository taskUpdateRepository;

    public TaskUpdateServiceExample(BusinessRuleEngine businessRuleEngine) {
        this.businessRuleEngine = businessRuleEngine;
    }

    /**
     * EXAMPLE: Create day-wise update with business rule validation.
     * 
     * This method demonstrates:
     * 1. Building BusinessRuleContext with all relevant data
     * 2. Validating multiple rules before proceeding
     * 3. Rules are enforced - if validation fails, BusinessRuleException is thrown
     */
    @Transactional
    public void createDayWiseUpdate(Long taskId, LocalDate updateDate, BigDecimal updateQty) {
        // 1. Fetch task and existing data
        // Task task = taskRepository.findById(taskId)
        //     .orElseThrow(() -> new RuntimeException("Task not found"));
        
        // 2. Build BusinessRuleContext with all relevant data for validation
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId()) // Get from SecurityContext
            .entityType("TASK_UPDATE")
            .entityId(taskId)
            .updateDate(updateDate)
            .plannedQty(new BigDecimal("100.00")) // task.getPlannedQty()
            .actualQty(new BigDecimal("80.00")) // task.getActualQty()
            .dailyUpdateQty(updateQty)
            .taskStartDate(LocalDate.now().minusDays(5)) // task.getStartDate()
            .lockDate(LocalDate.now().minusDays(10)) // task.getLockDate()
            .isConfirmed(false) // task.getIsConfirmed()
            .build();

        // 3. Validate business rules BEFORE proceeding with update
        // If any rule is violated, BusinessRuleException is thrown
        // The GlobalExceptionHandler will convert it to a proper HTTP response
        try {
            businessRuleEngine.validateAll(
                Arrays.asList(101, 401, 201), // Rules to validate
                context
            );
        } catch (BusinessRuleException e) {
            // Log the violation
            logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
            // Re-throw - GlobalExceptionHandler will handle it
            throw e;
        }

        // 4. If we get here, all rules passed - proceed with update
        // TaskUpdate update = new TaskUpdate();
        // update.setTask(task);
        // update.setUpdateDate(updateDate);
        // update.setQuantity(updateQty);
        // taskUpdateRepository.save(update);
        
        logger.info("Day-wise update created successfully for task {}", taskId);
    }

    /**
     * Helper method to get current user ID from SecurityContext.
     */
    private Long getCurrentUserId() {
        // Implementation depends on your SecurityContext setup
        // Example:
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // if (authentication != null && authentication.getPrincipal() instanceof Long) {
        //     return (Long) authentication.getPrincipal();
        // }
        return 1L; // Placeholder
    }
}

