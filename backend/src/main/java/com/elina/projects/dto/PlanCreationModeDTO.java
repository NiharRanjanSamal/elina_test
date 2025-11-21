package com.elina.projects.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object for creating plan versions with different modes.
 * 
 * Supports 3 creation modes:
 * 1. DAILY_ENTRY - Manual entry of daily plan lines
 * 2. DATE_RANGE_SPLIT - Split a date range with quantity distribution
 * 3. SINGLE_LINE_QUICK - Quick single-line plan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanCreationModeDTO {
    
    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "Version date is required")
    private LocalDate versionDate;

    private String description;

    @NotNull(message = "Creation mode is required")
    private CreationMode mode;

    // For DAILY_ENTRY mode
    private List<DailyPlanLineDTO> dailyLines;

    // For DATE_RANGE_SPLIT mode
    private DateRangeSplitDTO rangeSplit;

    // For SINGLE_LINE_QUICK mode
    private SingleLineQuickDTO singleLine;

    public enum CreationMode {
        DAILY_ENTRY,
        DATE_RANGE_SPLIT,
        SINGLE_LINE_QUICK
    }

    /**
     * Daily entry mode - manual entry of plan lines.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyPlanLineDTO {
        @NotNull(message = "Planned date is required")
        private LocalDate plannedDate;

        @NotNull(message = "Planned quantity is required")
        private BigDecimal plannedQty;

        private String description;
    }

    /**
     * Date range split mode - split a date range with quantity distribution.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRangeSplitDTO {
        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        @NotNull(message = "Total quantity is required")
        private BigDecimal totalQty;

        @NotNull(message = "Split type is required")
        private SplitType splitType;

        private Integer splitCount; // For EQUAL_SPLIT or CUSTOM_SPLIT

        private List<BigDecimal> customQuantities; // For CUSTOM_SPLIT

        public enum SplitType {
            EQUAL_SPLIT,      // Split equally across days
            WEEKLY_SPLIT,     // Split by weeks
            MONTHLY_SPLIT,    // Split by months
            CUSTOM_SPLIT      // Custom quantity per period
        }
    }

    /**
     * Single-line quick plan mode.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleLineQuickDTO {
        @NotNull(message = "Planned date is required")
        private LocalDate plannedDate;

        @NotNull(message = "Planned quantity is required")
        private BigDecimal plannedQty;

        private String description;
    }
}

