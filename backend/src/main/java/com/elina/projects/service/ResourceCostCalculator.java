package com.elina.projects.service;

import com.elina.projects.dto.resource.ResourceCostBreakdown;
import com.elina.projects.entity.resource.EmployeeEntity;
import com.elina.projects.entity.resource.EquipmentEntity;
import com.elina.projects.exception.NotFoundException;
import com.elina.projects.repository.EmployeeRepository;
import com.elina.projects.repository.EquipmentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Cost calculation engine shared across services and reporting.
 */
@Component
public class ResourceCostCalculator {

    private final EmployeeRepository employeeRepository;
    private final EquipmentRepository equipmentRepository;

    public ResourceCostCalculator(EmployeeRepository employeeRepository,
                                  EquipmentRepository equipmentRepository) {
        this.employeeRepository = employeeRepository;
        this.equipmentRepository = equipmentRepository;
    }

    /**
     * Calculate manpower cost for a date range.
     */
    public ResourceCostBreakdown calculateEmployeeCost(Long employeeId, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        EmployeeEntity employee = employeeRepository.findByIdForTenant(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        BigDecimal rate = employee.getRatePerDay() != null ? employee.getRatePerDay() : BigDecimal.ZERO;
        long days = computeDurationDays(startDate, endDate);
        return new ResourceCostBreakdown(days, rate, rate.multiply(BigDecimal.valueOf(days)));
    }

    /**
     * Calculate equipment cost for a date range.
     */
    public ResourceCostBreakdown calculateEquipmentCost(Long equipmentId, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        EquipmentEntity equipment = equipmentRepository.findByIdForTenant(equipmentId)
                .orElseThrow(() -> new NotFoundException("Equipment not found"));

        BigDecimal rate = equipment.getRatePerDay() != null ? equipment.getRatePerDay() : BigDecimal.ZERO;
        long days = computeDurationDays(startDate, endDate);
        return new ResourceCostBreakdown(days, rate, rate.multiply(BigDecimal.valueOf(days)));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required for cost calculation");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    private long computeDurationDays(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}

