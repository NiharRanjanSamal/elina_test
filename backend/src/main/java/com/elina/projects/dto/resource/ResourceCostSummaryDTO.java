package com.elina.projects.dto.resource;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Aggregated cost summary per WBS.
 */
@Data
public class ResourceCostSummaryDTO {
    private Long wbsId;
    private BigDecimal manpowerCost;
    private BigDecimal equipmentCost;
    private BigDecimal totalCost;
}

