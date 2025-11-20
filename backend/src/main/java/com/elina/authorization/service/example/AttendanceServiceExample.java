package com.elina.authorization.service.example;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.authorization.rule.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * EXAMPLE SERVICE: AttendanceService
 * 
 * This is an EXAMPLE showing how to validate business rules
 * when posting attendance entries.
 * 
 * Rule enforced:
 * - Rule 601: ATTENDANCE_DATE_CANNOT_BE_IN_FUTURE - Validates attendance date
 */
@Service
public class AttendanceServiceExample {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceServiceExample.class);

    private final BusinessRuleEngine businessRuleEngine;
    // private final AttendanceRepository attendanceRepository;

    public AttendanceServiceExample(BusinessRuleEngine businessRuleEngine) {
        this.businessRuleEngine = businessRuleEngine;
    }

    /**
     * EXAMPLE: Post attendance with business rule validation.
     * 
     * Validates that attendance date is not in the future.
     */
    @Transactional
    public void postAttendance(Long employeeId, LocalDate attendanceDate) {
        // 1. Build context for rule validation
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(TenantContext.getTenantId())
            .userId(getCurrentUserId())
            .entityType("ATTENDANCE")
            .attendanceDate(attendanceDate)
            .build();

        // 2. Validate Rule 601: ATTENDANCE_DATE_CANNOT_BE_IN_FUTURE
        // This rule ensures attendance can only be recorded for today or past dates
        try {
            businessRuleEngine.validate(601, context);
        } catch (BusinessRuleException e) {
            logger.warn("Business rule violation: Rule {} - {}", e.getRuleNumber(), e.getMessage());
            throw e; // Re-throw for GlobalExceptionHandler
        }

        // 3. Proceed with attendance posting if validation passes
        // Attendance attendance = new Attendance();
        // attendance.setEmployeeId(employeeId);
        // attendance.setAttendanceDate(attendanceDate);
        // attendanceRepository.save(attendance);
        
        logger.info("Attendance posted successfully for employee {} on {}", employeeId, attendanceDate);
    }

    private Long getCurrentUserId() {
        return 1L; // Placeholder
    }
}

