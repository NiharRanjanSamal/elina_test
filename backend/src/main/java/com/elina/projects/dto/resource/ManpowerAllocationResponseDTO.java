package com.elina.projects.dto.resource;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO representing a manpower allocation record.
 */
@Data
public class ManpowerAllocationResponseDTO {

    private Long allocationId;
    private Long wbsId;
    private String wbsCode;
    private Long employeeId;
    private String employeeName;
    private String skillLevel;
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

