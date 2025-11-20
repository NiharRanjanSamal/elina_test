package com.elina.projects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Data Transfer Object for creating/updating Project.
 * Used for API requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreateDTO {
    
    @NotBlank(message = "Project code is required")
    @Size(max = 50, message = "Project code must not exceed 50 characters")
    private String projectCode;

    @NotBlank(message = "Project name is required")
    @Size(max = 200, message = "Project name must not exceed 200 characters")
    private String projectName;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;

    private Boolean activateFlag = true;
}

