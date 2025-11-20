package com.elina.authorization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating/updating BusinessRule.
 * Used for API requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRuleCreateDTO {
    
    @NotNull(message = "Rule number is required")
    private Integer ruleNumber;

    @NotBlank(message = "Control point is required")
    @Size(max = 100, message = "Control point must not exceed 100 characters")
    private String controlPoint;

    @NotBlank(message = "Applicability is required")
    @Size(max = 1, message = "Applicability must be Y or N")
    private String applicability;

    @Size(max = 500, message = "Rule value must not exceed 500 characters")
    private String ruleValue;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Boolean activateFlag = true;
}

