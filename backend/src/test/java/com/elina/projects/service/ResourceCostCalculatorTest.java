package com.elina.projects.service;

import com.elina.projects.dto.resource.ResourceCostBreakdown;
import com.elina.projects.entity.resource.EmployeeEntity;
import com.elina.projects.entity.resource.EquipmentEntity;
import com.elina.projects.exception.NotFoundException;
import com.elina.projects.repository.EmployeeRepository;
import com.elina.projects.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceCostCalculatorTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    private ResourceCostCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ResourceCostCalculator(employeeRepository, equipmentRepository);
    }

    @Test
    void calculateEmployeeCost_shouldMultiplyRateAndDuration() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setRatePerDay(new BigDecimal("800"));
        when(employeeRepository.findByIdForTenant(1L)).thenReturn(Optional.of(employee));

        ResourceCostBreakdown breakdown = calculator.calculateEmployeeCost(
                1L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10));

        assertEquals(10, breakdown.getTotalDays());
        assertEquals(new BigDecimal("800"), breakdown.getRatePerDay());
        assertEquals(new BigDecimal("8000"), breakdown.getTotalCost());
    }

    @Test
    void calculateEquipmentCost_shouldThrowWhenEquipmentMissing() {
        when(equipmentRepository.findByIdForTenant(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () ->
                calculator.calculateEquipmentCost(99L, LocalDate.now(), LocalDate.now().plusDays(1)));
    }
}

