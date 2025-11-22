package com.elina.projects.dto.resource;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for equipment allocation records.
 */
@Data
public class EquipmentAllocationResponseDTO {

    private Long allocationId;
    private Long wbsId;
    private String wbsCode;
    private Long equipmentId;
    private String equipmentName;
    private String equipmentType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long durationDays;
    private BigDecimal hoursPerDay;
    private BigDecimal ratePerDay;
    private BigDecimal totalCost;
    private String remarks;
    private Boolean activateFlag;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
}

