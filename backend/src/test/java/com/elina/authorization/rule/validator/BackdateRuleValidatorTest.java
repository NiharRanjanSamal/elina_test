package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.entity.Tenant;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BackdateRuleValidator.
 */
class BackdateRuleValidatorTest {

    private BackdateRuleValidator validator;
    private BusinessRule rule101;
    private BusinessRule rule102;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        validator = new BackdateRuleValidator();
        
        tenant = new Tenant();
        tenant.setId(1L);

        // Rule 101: BACKDATE_ALLOWED_TILL
        rule101 = new BusinessRule();
        rule101.setRuleId(1L);
        rule101.setTenant(tenant);
        rule101.setRuleNumber(101);
        rule101.setControlPoint("TASK_UPDATE");
        rule101.setApplicability("Y");
        rule101.setRuleValue("7");
        rule101.setActivateFlag(true);

        // Rule 102: BACKDATE_ALLOWED_AFTER_LOCK
        rule102 = new BusinessRule();
        rule102.setRuleId(2L);
        rule102.setTenant(tenant);
        rule102.setRuleNumber(102);
        rule102.setControlPoint("TASK_UPDATE");
        rule102.setApplicability("Y");
        rule102.setRuleValue("N");
        rule102.setActivateFlag(true);
    }

    @Test
    void testValidateBackdateWithinLimit() {
        BusinessRuleContext context = BusinessRuleContext.builder()
            .updateDate(LocalDate.now().minusDays(3))
            .build();

        assertDoesNotThrow(() -> validator.validate(rule101, context));
    }

    @Test
    void testValidateBackdateExceedsLimit() {
        BusinessRuleContext context = BusinessRuleContext.builder()
            .updateDate(LocalDate.now().minusDays(10))
            .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
            () -> validator.validate(rule101, context));

        assertEquals(101, exception.getRuleNumber());
        assertTrue(exception.getMessage().contains("Backdating is only allowed for 7 days"));
    }

    @Test
    void testValidateFutureDate() {
        BusinessRuleContext context = BusinessRuleContext.builder()
            .updateDate(LocalDate.now().plusDays(1))
            .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
            () -> validator.validate(rule101, context));

        assertEquals(101, exception.getRuleNumber());
        assertTrue(exception.getMessage().contains("Cannot backdate to a future date"));
    }

    @Test
    void testValidateBackdateAfterLock_NotAllowed() {
        BusinessRuleContext context = BusinessRuleContext.builder()
            .updateDate(LocalDate.now().minusDays(5))
            .lockDate(LocalDate.now().minusDays(3))
            .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
            () -> validator.validate(rule102, context));

        assertEquals(102, exception.getRuleNumber());
        assertTrue(exception.getMessage().contains("Cannot backdate before lock date"));
    }

    @Test
    void testGetSupportedRuleNumbers() {
        int[] supported = validator.getSupportedRuleNumbers();
        assertEquals(2, supported.length);
        assertTrue(Arrays.stream(supported).anyMatch(n -> n == 101));
        assertTrue(Arrays.stream(supported).anyMatch(n -> n == 102));
    }
}

