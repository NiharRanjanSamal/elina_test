package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Task entity.
 * Used for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private Long taskId;
    private Long tenantId;
    private Long projectId;
    private Long wbsId;
    private String taskCode;
    private String taskName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private String unit;
    private String status;
    private Boolean isConfirmed;
    private LocalDateTime confirmedOn;
    private Long confirmedBy;
    private Boolean isLocked;
    private LocalDate lockDate;
    private Boolean activateFlag;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
}

