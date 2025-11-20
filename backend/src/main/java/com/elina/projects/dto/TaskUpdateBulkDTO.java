package com.elina.projects.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object for bulk day-wise task updates.
 * Used for API requests to save multiple day-wise updates at once.
 * 
 * Business rules validated for each entry:
 * - Rule 101: BACKDATE_ALLOWED_TILL
 * - Rule 102: BACKDATE_ALLOWED_AFTER_LOCK
 * - Rule 401: DAILY_UPDATE_CANNOT_EXCEED_PLANNED_QTY
 * - Update date must be within task date range
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateBulkDTO {
    
    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "Updates list is required")
    private List<DayWiseUpdateDTO> updates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayWiseUpdateDTO {
        @NotNull(message = "Update date is required")
        private LocalDate updateDate;

        private BigDecimal planQty;

        @NotNull(message = "Actual quantity is required")
        private BigDecimal actualQty;

        private String remarks;
    }
}

