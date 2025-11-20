package com.elina.authorization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for BusinessRule entity.
 * Used for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRuleDTO {
    private Long ruleId;
    private Long tenantId;
    private Integer ruleNumber;
    private String controlPoint;
    private String applicability;
    private String ruleValue;
    private String description;
    private Boolean activateFlag;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
}

