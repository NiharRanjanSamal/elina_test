package com.elina.projects.dto.resource;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for manpower allocation create/update.
 */
@Data
public class ManpowerAllocationRequestDTO {

    @NotNull
    private Long wbsId;

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = true)
    private BigDecimal hoursPerDay;

    @Size(max = 500)
    private String remarks;
}

