package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for task update summary (plan vs actual vs variance).
 * Used for reporting and dashboard displays.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateSummaryDTO {
    private LocalDate date;
    private BigDecimal planQty;
    private BigDecimal actualQty;
    private BigDecimal variance; // actualQty - planQty
    
    public TaskUpdateSummaryDTO(LocalDate date, BigDecimal planQty, BigDecimal actualQty) {
        this.date = date;
        this.planQty = planQty != null ? planQty : BigDecimal.ZERO;
        this.actualQty = actualQty != null ? actualQty : BigDecimal.ZERO;
        this.variance = this.actualQty.subtract(this.planQty);
    }
}

