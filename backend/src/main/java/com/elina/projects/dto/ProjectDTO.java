package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Project entity.
 * Used for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private Long projectId;
    private Long tenantId;
    private String projectCode;
    private String projectName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Boolean activateFlag;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
}

