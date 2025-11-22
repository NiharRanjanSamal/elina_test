package com.elina.projects.dto.resource;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Generic resource option entry for dropdowns.
 */
@Data
public class ResourceOptionDTO {
    private Long id;
    private String name;
    private String category;
    private BigDecimal ratePerDay;
    private String metadata;
}

