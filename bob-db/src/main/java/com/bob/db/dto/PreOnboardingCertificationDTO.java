package com.bob.db.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PreOnboardingCertificationDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private String certificationName;

    private String institutionName;

    private LocalDate passingDate;

    private BigDecimal percentage;

    private UUID candidateCertificateId;

}