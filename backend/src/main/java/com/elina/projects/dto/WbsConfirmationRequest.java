package com.elina.projects.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request payload for freezing a WBS on a specific date.
 */
@Data
public class WbsConfirmationRequest {

    @NotNull(message = "Confirmation date is required")
    private LocalDate confirmationDate;

    private String remarks;
}


