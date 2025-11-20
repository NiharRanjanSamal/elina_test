package com.elina.projects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for creating/updating Task.
 * Used for API requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateDTO {
    
    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "WBS ID is required")
    private Long wbsId;

    @NotBlank(message = "Task code is required")
    @Size(max = 50, message = "Task code must not exceed 50 characters")
    private String taskCode;

    @NotBlank(message = "Task name is required")
    @Size(max = 200, message = "Task name must not exceed 200 characters")
    private String taskName;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal plannedQty;

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;

    private Boolean activateFlag = true;
}

