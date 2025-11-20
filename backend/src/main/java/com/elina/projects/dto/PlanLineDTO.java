package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for PlanLine entity.
 * Used for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanLineDTO {
    private Long planLineId;
    private Long tenantId;
    private Long planVersionId;
    private Long taskId;
    private Integer lineNumber;
    private LocalDate workDate;
    private BigDecimal plannedQty;
    private String description;
    private Boolean activateFlag;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
}

