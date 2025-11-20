package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object for Project with aggregated details.
 * Used for API responses showing project summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailsDTO {
    private ProjectDTO project;
    private Integer totalWbsCount;
    private Integer totalTaskCount;
    private BigDecimal totalPlannedQty;
    private BigDecimal totalActualQty;
    private List<WbsDTO> wbsHierarchy; // Root WBS with children
}

