package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for PlanVersion entity.
 * Used for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanVersionDTO {
    private Long planVersionId;
    private Long tenantId;
    private Long taskId;
    private Integer versionNo;
    private LocalDate versionDate;
    private String description;
    private Boolean isActive;
    private Boolean activateFlag;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
}

