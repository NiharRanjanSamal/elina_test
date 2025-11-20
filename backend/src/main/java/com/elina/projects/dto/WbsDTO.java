package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for WBS entity with hierarchical support.
 * Used for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WbsDTO {
    private Long wbsId;
    private Long tenantId;
    private Long projectId;
    private Long parentWbsId;
    private String wbsCode;
    private String wbsName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer level;
    private String workCenter;
    private String costCenter;
    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private String status;
    private Boolean isConfirmed;
    private LocalDateTime confirmedOn;
    private Long confirmedBy;
    private Boolean isLocked;
    private LocalDate lockDate;
    private Boolean activateFlag;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
    
    // For hierarchical display
    private List<WbsDTO> children = new ArrayList<>();
}

