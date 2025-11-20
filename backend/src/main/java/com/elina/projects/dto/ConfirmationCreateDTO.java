package com.elina.projects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Data Transfer Object for creating Confirmation.
 * Used for API requests.
 * 
 * Business rules validated:
 * - Rule 301: CONFIRMATION_CANNOT_BE_OVERWRITTEN - entity must not be already confirmed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationCreateDTO {
    
    @NotBlank(message = "Entity type is required")
    private String entityType; // WBS, TASK

    @NotNull(message = "Entity ID is required")
    private Long entityId;

    @NotNull(message = "Confirmation date is required")
    private LocalDate confirmationDate;

    private String remarks;
}

