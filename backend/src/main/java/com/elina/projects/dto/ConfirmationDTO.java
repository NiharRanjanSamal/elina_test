package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Confirmation entity.
 * Used for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationDTO {
    private Long confirmationId;
    private Long tenantId;
    private String entityType; // WBS, TASK
    private Long entityId;
    private LocalDate confirmationDate;
    private Long confirmedBy;
    private LocalDateTime confirmedOn;
    private String remarks;
    private Long createdBy;
    private LocalDateTime createdOn;
}

