package com.elina.projects.dto.resource;

import lombok.Data;

import java.time.LocalDate;

/**
 * Timeline response row for visualization.
 */
@Data
public class AllocationTimelineItemDTO {
    private String resourceName;
    private String resourceType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long durationDays;
}

