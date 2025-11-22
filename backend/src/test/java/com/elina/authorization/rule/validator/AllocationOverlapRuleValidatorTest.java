package com.elina.authorization.rule.validator;

import com.elina.authorization.entity.BusinessRule;
import com.elina.authorization.rule.BusinessRuleContext;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.repository.EquipmentAllocationRepository;
import com.elina.projects.repository.ManpowerAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationOverlapRuleValidatorTest {

    @Mock
    private ManpowerAllocationRepository manpowerAllocationRepository;

    @Mock
    private EquipmentAllocationRepository equipmentAllocationRepository;

    private AllocationOverlapRuleValidator validator;
    private BusinessRule rule;

    @BeforeEach
    void setUp() {
        validator = new AllocationOverlapRuleValidator(manpowerAllocationRepository, equipmentAllocationRepository);
        rule = new BusinessRule();
        rule.setRuleNumber(602);
    }

    @Test
    void shouldThrowWhenManpowerOverlapDetected() {
        when(manpowerAllocationRepository.existsOverlappingAllocation(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(true);

        BusinessRuleContext context = BusinessRuleContext.builder()
                .allocationStartDate(LocalDate.now())
                .allocationEndDate(LocalDate.now().plusDays(5))
                .build()
                .addParam("resourceType", "MANPOWER")
                .addParam("resourceId", 10L)
                .addParam("wbsId", 5L);

        assertThrows(BusinessRuleException.class, () -> validator.validate(rule, context));
    }

    @Test
    void shouldPassWhenNoEquipmentOverlap() {
        when(equipmentAllocationRepository.existsOverlappingAllocation(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(false);

        BusinessRuleContext context = BusinessRuleContext.builder()
                .allocationStartDate(LocalDate.now())
                .allocationEndDate(LocalDate.now().plusDays(2))
                .build()
                .addParam("resourceType", "EQUIPMENT")
                .addParam("resourceId", 20L)
                .addParam("wbsId", 7L);

        assertDoesNotThrow(() -> validator.validate(rule, context));
    }
}

