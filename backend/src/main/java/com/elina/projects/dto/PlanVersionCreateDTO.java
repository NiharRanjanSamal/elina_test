package com.elina.projects.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object for creating PlanVersion with lines.
 * Used for API requests.
 * 
 * Business rules validated:
 * - Rule 402: PLAN_VERSION_DATE_VALIDATION - versionDate cannot be in future
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanVersionCreateDTO {
    
    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "Version date is required")
    private LocalDate versionDate;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Plan lines are required")
    private List<PlanLineCreateDTO> lines;
}

