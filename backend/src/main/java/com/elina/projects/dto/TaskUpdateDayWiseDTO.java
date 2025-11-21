package com.elina.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for unified day-wise update display.
 * Merges plan lines (from active plan version) with existing task updates.
 * Used by frontend DayWiseGrid component.
 * 
 * For date gaps: plan_qty from plan_lines, actual_qty = 0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateDayWiseDTO {
    private LocalDate updateDate;
    private BigDecimal planQty; // From plan_lines (active plan version)
    private BigDecimal actualQty; // From task_updates (0 if no update exists)
    private BigDecimal variance; // actualQty - planQty
    private Long updateId; // null if no update exists yet
    private String remarks;
    private Boolean isLocked; // true if confirmation lock covers this date
    private Boolean canEdit; // false if locked or outside allowed date range
    
    public TaskUpdateDayWiseDTO(LocalDate updateDate, BigDecimal planQty, BigDecimal actualQty) {
        this.updateDate = updateDate;
        this.planQty = planQty != null ? planQty : BigDecimal.ZERO;
        this.actualQty = actualQty != null ? actualQty : BigDecimal.ZERO;
        this.variance = this.actualQty.subtract(this.planQty);
        this.isLocked = false;
        this.canEdit = true;
    }
}

