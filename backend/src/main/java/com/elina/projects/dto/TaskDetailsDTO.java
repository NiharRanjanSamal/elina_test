package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for Task with related details.
 * Used for API responses showing task details with plan versions and updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailsDTO {
    private TaskDTO task;
    private List<PlanVersionDTO> planVersions;
    private PlanVersionDTO currentPlanVersion;
    private List<TaskUpdateDTO> recentUpdates;
    private Integer updateCount;
}

