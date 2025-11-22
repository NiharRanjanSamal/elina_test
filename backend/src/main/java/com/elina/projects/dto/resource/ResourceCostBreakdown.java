package com.elina.projects.dto.resource;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Cost breakdown returned by ResourceCostCalculator.
 */
@Data
@AllArgsConstructor
public class ResourceCostBreakdown {
    private long totalDays;
    private BigDecimal ratePerDay;
    private BigDecimal totalCost;
}

