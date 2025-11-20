package com.elina.authorization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating/updating MasterCode.
 * Used for API requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterCodeCreateDTO {
    
    @NotBlank(message = "Code type is required")
    @Size(max = 100, message = "Code type must not exceed 100 characters")
    private String codeType;

    @NotBlank(message = "Code value is required")
    @Size(max = 255, message = "Code value must not exceed 255 characters")
    private String codeValue;

    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;

    private String longDescription;

    private Boolean activateFlag = true;
}

