package com.elina.projects.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aggregated snapshot displayed on the confirmation landing page.
 */
@Value
@Builder
public class WbsConfirmationSummaryDTO {
    Long wbsId;
    String wbsCode;
    String wbsName;
    LocalDate lastConfirmationDate;
    LocalDate lockDate;
    BigDecimal plannedQty;
    BigDecimal actualQty;
    BigDecimal confirmedQtyToDate;
    BigDecimal variance;
    LocalDate previewDate;
    BigDecimal previewActualQty;
}


