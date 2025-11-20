package com.elina.authorization.rule;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.repository.BusinessRuleRepository;
import com.elina.authorization.rule.validator.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BusinessRuleEngine.
 */
@ExtendWith(MockitoExtension.class)
class BusinessRuleEngineTest {

    @Mock
    private BusinessRuleRepository businessRuleRepository;

    private BusinessRuleEngine businessRuleEngine;
    private Tenant tenant;
    private BusinessRule rule101;
    private BusinessRule rule201;
    private BusinessRule rule301;
    private BusinessRule rule401;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);

        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantCode("DEFAULT");

        // Rule 101: BACKDATE_ALLOWED_TILL
        rule101 = new BusinessRule();
        rule101.setRuleId(1L);
        rule101.setTenant(tenant);
        rule101.setRuleNumber(101);
        rule101.setControlPoint("TASK_UPDATE");
        rule101.setApplicability("Y");
        rule101.setRuleValue("7");
        rule101.setDescription("Maximum 7 days for backdating");
        rule101.setActivateFlag(true);

        // Rule 201: START_DATE_CANNOT_BE_IN_FUTURE
        rule201 = new BusinessRule();
        rule201.setRuleId(2L);
        rule201.setTenant(tenant);
        rule201.setRuleNumber(201);
        rule201.setControlPoint("TASK");
        rule201.setApplicability("Y");
        rule201.setRuleValue("");
        rule201.setDescription("Task start date cannot be in future");
        rule201.setActivateFlag(true);

        // Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN
        rule301 = new BusinessRule();
        rule301.setRuleId(3L);
        rule301.setTenant(tenant);
        rule301.setRuleNumber(301);
        rule301.setControlPoint("CONFIRMATION");
        rule301.setApplicability("Y");
        rule301.setRuleValue("");
        rule301.setDescription("Cannot modify confirmed entries");
        rule301.setActivateFlag(true);

        // Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY
        rule401 = new BusinessRule();
        rule401.setRuleId(4L);
        rule401.setTenant(tenant);
        rule401.setRuleNumber(401);
        rule401.setControlPoint("TASK_UPDATE");
        rule401.setApplicability("Y");
        rule401.setRuleValue("");
        rule401.setDescription("Daily update cannot exceed planned quantity");
        rule401.setActivateFlag(true);

        // Create validators
        List<BusinessRuleValidator> validators = Arrays.asList(
            new BackdateRuleValidator(),
            new TaskDateRangeRuleValidator(),
            new ConfirmationLockRuleValidator(),
            new PlannedVsActualRuleValidator(),
            new WbsDateRangeRuleValidator(),
            new AllocationDateRuleValidator(),
            new AttendanceEntryRuleValidator(),
            new MaterialUsageRuleValidator()
        );

        businessRuleEngine = new BusinessRuleEngine(businessRuleRepository, validators);
        businessRuleEngine.initialize();
    }

    @Test
    void testIsRuleActive() {
        when(businessRuleRepository.findByRuleNumber(101)).thenReturn(Optional.of(rule101));
        
        assertTrue(businessRuleEngine.isRuleActive(101));
        
        rule101.setActivateFlag(false);
        assertFalse(businessRuleEngine.isRuleActive(101));
    }

    @Test
    void testGetRuleValue() {
        when(businessRuleRepository.findByRuleNumber(101)).thenReturn(Optional.of(rule101));
        
        String value = businessRuleEngine.getRuleValue(101);
        assertEquals("7", value);
    }

    @Test
    void testValidateBackdateRule_Success() {
        when(businessRuleRepository.findByRuleNumber(101)).thenReturn(Optional.of(rule101));
        
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(1L)
            .updateDate(LocalDate.now().minusDays(3)) // 3 days ago (within limit)
            .build();
        
        // Should not throw exception
        assertDoesNotThrow(() -> businessRuleEngine.validate(101, context));
    }

    @Test
    void testValidateBackdateRule_ExceedsLimit() {
        when(businessRuleRepository.findByRuleNumber(101)).thenReturn(Optional.of(rule101));
        
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(1L)
            .updateDate(LocalDate.now().minusDays(10)) // 10 days ago (exceeds 7 day limit)
            .build();
        
        // Should throw exception
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
            () -> businessRuleEngine.validate(101, context));
        
        assertEquals(101, exception.getRuleNumber());
        assertTrue(exception.getMessage().contains("Backdating is only allowed for 7 days"));
    }

    @Test
    void testValidateTaskStartDate_FutureDate() {
        when(businessRuleRepository.findByRuleNumber(201)).thenReturn(Optional.of(rule201));
        
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(1L)
            .taskStartDate(LocalDate.now().plusDays(1)) // Future date
            .build();
        
        // Should throw exception
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
            () -> businessRuleEngine.validate(201, context));
        
        assertEquals(201, exception.getRuleNumber());
        assertTrue(exception.getMessage().contains("cannot be in the future"));
    }

    @Test
    void testValidateConfirmation_OverwriteAttempt() {
        when(businessRuleRepository.findByRuleNumber(301)).thenReturn(Optional.of(rule301));
        
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(1L)
            .entityId(100L)
            .isConfirmed(true) // Entry is confirmed
            .build();
        
        // Should throw exception
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
            () -> businessRuleEngine.validate(301, context));
        
        assertEquals(301, exception.getRuleNumber());
        assertTrue(exception.getMessage().contains("Cannot modify confirmed entry"));
    }

    @Test
    void testValidatePlannedVsActual_ExceedsPlanned() {
        when(businessRuleRepository.findByRuleNumber(401)).thenReturn(Optional.of(rule401));
        
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(1L)
            .plannedQty(new BigDecimal("100.00"))
            .actualQty(new BigDecimal("80.00"))
            .dailyUpdateQty(new BigDecimal("30.00")) // Would make total 110, exceeding 100
            .build();
        
        // Should throw exception
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
            () -> businessRuleEngine.validate(401, context));
        
        assertEquals(401, exception.getRuleNumber());
        assertTrue(exception.getMessage().contains("cannot exceed planned quantity"));
    }

    @Test
    void testValidate_RuleNotActive() {
        rule101.setActivateFlag(false);
        when(businessRuleRepository.findByRuleNumber(101)).thenReturn(Optional.of(rule101));
        
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(1L)
            .updateDate(LocalDate.now().minusDays(10))
            .build();
        
        // Should not throw exception (rule is inactive)
        assertDoesNotThrow(() -> businessRuleEngine.validate(101, context));
    }

    @Test
    void testValidate_RuleNotApplicable() {
        rule101.setApplicability("N");
        when(businessRuleRepository.findByRuleNumber(101)).thenReturn(Optional.of(rule101));
        
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(1L)
            .updateDate(LocalDate.now().minusDays(10))
            .build();
        
        // Should not throw exception (rule is not applicable)
        assertDoesNotThrow(() -> businessRuleEngine.validate(101, context));
    }

    @Test
    void testValidate_RuleNotFound() {
        when(businessRuleRepository.findByRuleNumber(999)).thenReturn(Optional.empty());
        
        BusinessRuleContext context = BusinessRuleContext.builder()
            .tenantId(1L)
            .build();
        
        // Should not throw exception (rule doesn't exist)
        assertDoesNotThrow(() -> businessRuleEngine.validate(999, context));
    }
}

