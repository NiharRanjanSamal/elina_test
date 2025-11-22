package com.elina.projects.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO returned for every confirmation history entry.
 */
@Value
@Builder
public class WbsConfirmationResponse {
    Long confirmationId;
    Long wbsId;
    String wbsCode;
    String wbsName;
    LocalDate confirmationDate;
    BigDecimal confirmedQty;
    String remarks;
    Long createdBy;
    LocalDateTime createdOn;
    LocalDate lockDate;
}


