package com.elina.projects.service;

import com.elina.authorization.context.TenantContext;
import com.elina.authorization.rule.BusinessRuleEngine;
import com.elina.projects.dto.resource.ManpowerAllocationRequestDTO;
import com.elina.projects.entity.Wbs;
import com.elina.projects.entity.resource.EmployeeEntity;
import com.elina.projects.entity.resource.ManpowerAllocationEntity;
import com.elina.projects.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceAllocationServiceTest {

    @Mock
    private ManpowerAllocationRepository manpowerAllocationRepository;
    @Mock
    private EquipmentAllocationRepository equipmentAllocationRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private WbsRepository wbsRepository;
    @Mock
    private BusinessRuleEngine businessRuleEngine;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ResourceCostCalculator resourceCostCalculator;

    private ResourceAllocationService service;

    @BeforeEach
    void setUp() {
        service = new ResourceAllocationService(
                manpowerAllocationRepository,
                equipmentAllocationRepository,
                employeeRepository,
                equipmentRepository,
                wbsRepository,
                businessRuleEngine,
                auditLogService,
                resourceCostCalculator
        );
        TenantContext.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(99L, null));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void allocateEmployeeToWbs_invokesBusinessRulesAndCalculatesCost() {
        Wbs wbs = new Wbs();
        wbs.setWbsId(5L);
        wbs.setStartDate(LocalDate.of(2025, 1, 1));
        wbs.setEndDate(LocalDate.of(2025, 12, 31));
        com.elina.authorization.entity.Tenant tenant = new com.elina.authorization.entity.Tenant();
        tenant.setId(1L);
        wbs.setTenant(tenant);
        when(wbsRepository.findById(5L)).thenReturn(Optional.of(wbs));

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(7L);
        employee.setRatePerDay(new BigDecimal("800"));
        employee.setActivateFlag(true);
        when(employeeRepository.findByIdForTenant(7L)).thenReturn(Optional.of(employee));

        when(manpowerAllocationRepository.save(any())).thenAnswer(invocation -> {
            ManpowerAllocationEntity entity = invocation.getArgument(0);
            entity.setAllocationId(100L);
            return entity;
        });

        ManpowerAllocationRequestDTO dto = new ManpowerAllocationRequestDTO();
        dto.setWbsId(5L);
        dto.setEmployeeId(7L);
        dto.setStartDate(LocalDate.of(2025, 2, 1));
        dto.setEndDate(LocalDate.of(2025, 2, 5));
        dto.setHoursPerDay(new BigDecimal("8"));
        dto.setRemarks("Testing");

        service.allocateEmployeeToWbs(dto);

        verify(businessRuleEngine).validate(eq(501), any());
        verify(businessRuleEngine).validate(eq(601), any());
        verify(businessRuleEngine).validate(eq(101), any());
        verify(businessRuleEngine).validate(eq(102), any());
        verify(businessRuleEngine).validate(eq(602), any());

        ArgumentCaptor<ManpowerAllocationEntity> captor = ArgumentCaptor.forClass(ManpowerAllocationEntity.class);
        verify(manpowerAllocationRepository).save(captor.capture());
        assertEquals(new BigDecimal("4000"), captor.getValue().getTotalCost());
    }

    @Test
    void allocateEmployeeToWbs_propagatesRuleException() {
        Wbs wbs = new Wbs();
        wbs.setWbsId(5L);
        com.elina.authorization.entity.Tenant tenant = new com.elina.authorization.entity.Tenant();
        tenant.setId(1L);
        wbs.setTenant(tenant);
        when(wbsRepository.findById(anyLong())).thenReturn(Optional.of(wbs));

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(7L);
        employee.setActivateFlag(true);
        when(employeeRepository.findByIdForTenant(anyLong())).thenReturn(Optional.of(employee));

        lenient().doThrow(new com.elina.authorization.rule.BusinessRuleException(601, "error", "fix"))
                .when(businessRuleEngine).validate(eq(601), any());

        ManpowerAllocationRequestDTO dto = new ManpowerAllocationRequestDTO();
        dto.setWbsId(5L);
        dto.setEmployeeId(7L);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(1));

        assertThrows(com.elina.authorization.rule.BusinessRuleException.class, () -> service.allocateEmployeeToWbs(dto));
    }
}

