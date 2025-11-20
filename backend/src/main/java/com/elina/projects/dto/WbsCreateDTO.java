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
 * Data Transfer Object for creating/updating WBS.
 * Used for API requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WbsCreateDTO {
    
    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long parentWbsId;

    @NotBlank(message = "WBS code is required")
    @Size(max = 50, message = "WBS code must not exceed 50 characters")
    private String wbsCode;

    @NotBlank(message = "WBS name is required")
    @Size(max = 200, message = "WBS name must not exceed 200 characters")
    private String wbsName;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private String workCenter;

    private String costCenter;

    private BigDecimal plannedQty;

    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;

    private Boolean activateFlag = true;
}

