package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Section 15 of the requirements doc - Compensation Management fields. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationDetailsRequest {
    private BigDecimal currentCtc;
    private BigDecimal expectedCtc;
    private BigDecimal fixedPay;
    private BigDecimal variablePay;
    private BigDecimal bonus;
    private String compensationComments;
    private BigDecimal agreedCtc;
}
