package com.elina.projects.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for creating/updating TaskUpdate.
 * Used for API requests.
 * 
 * Business rules validated:
 * - Rule 101: BACKDATE_ALLOWED_TILL - updateDate cannot be older than allowed backdate
 * - Rule 201: START_DATE_CANNOT_BE_IN_FUTURE - updateDate cannot be in future
 * - Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY - dailyUpdateQty cannot exceed plannedQty
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateCreateDTO {
    
    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "Update date is required")
    private LocalDate updateDate;

    private BigDecimal plannedQty;

    @NotNull(message = "Actual quantity is required")
    private BigDecimal actualQty;

    private BigDecimal dailyUpdateQty; // The increment for this day

    private String remarks;
}

