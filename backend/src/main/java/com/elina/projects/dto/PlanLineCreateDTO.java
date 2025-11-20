package com.elina.projects.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for creating PlanLine.
 * Used for API requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanLineCreateDTO {
    
    @NotNull(message = "Line number is required")
    private Integer lineNumber;

    @NotNull(message = "Planned date is required")
    private LocalDate plannedDate;

    @NotNull(message = "Planned quantity is required")
    private BigDecimal plannedQty;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

