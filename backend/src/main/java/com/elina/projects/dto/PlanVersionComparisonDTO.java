package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for comparing two plan versions.
 * Used for version comparison functionality.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanVersionComparisonDTO {
    
    private PlanVersionDTO version1;
    private PlanVersionDTO version2;
    
    private List<ComparisonLineDTO> comparisonLines;
    
    private ComparisonSummaryDTO summary;

    /**
     * Comparison line showing differences for a specific date.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonLineDTO {
        private LocalDate plannedDate;
        private BigDecimal qtyVersion1;
        private BigDecimal qtyVersion2;
        private BigDecimal difference;
        private String status; // SAME, INCREASED, DECREASED, NEW, REMOVED
    }

    /**
     * Summary of comparison.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonSummaryDTO {
        private Integer totalDaysVersion1;
        private Integer totalDaysVersion2;
        private Integer commonDays;
        private Integer newDays;
        private Integer removedDays;
        private BigDecimal totalQtyVersion1;
        private BigDecimal totalQtyVersion2;
        private BigDecimal totalDifference;
        private Map<String, Integer> changeStatistics; // e.g., {"INCREASED": 5, "DECREASED": 3}
    }
}

