package com.bob.commonutil.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryBreakUpModel {

    private BigDecimal totalCtc;
    private BigDecimal variablePay;
    private BigDecimal annualTotalFixed;
    private BigDecimal monthlyTotalFixed;
    private BigDecimal annualBasic;
    private BigDecimal monthlyBasic;
    private BigDecimal annualHra;
    private BigDecimal monthlyHra;
    private BigDecimal annualSupplementary;
    private BigDecimal monthlySupplementary;
    private BigDecimal annualMedical;
    private BigDecimal monthlyMedical;
    private BigDecimal annualEntertainment;
    private BigDecimal monthlyEntertainment;
}
