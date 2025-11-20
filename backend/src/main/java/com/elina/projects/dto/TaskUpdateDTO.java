package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for TaskUpdate entity.
 * Used for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateDTO {
    private Long updateId;
    private Long tenantId;
    private Long taskId;
    private LocalDate updateDate;
    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private BigDecimal dailyUpdateQty;
    private String remarks;
    private Boolean activateFlag;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
}

