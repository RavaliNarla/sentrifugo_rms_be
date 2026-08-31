package com.bob.db.dto;

import com.bob.db.entity.PreOnboardingDocumentEntity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Data;
import org.hibernate.annotations.Where;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PreOnboardingPersonalDetailsDTO extends BaseDTO implements Serializable {

    private UUID id;

    private UUID preOnboardingId;

    private UUID genderId;

    private LocalDate dateOfBirth;

    private UUID maritalStatusId;

    private BigDecimal cibilScore;

    private LocalDate cibilScoreDate;

    private Boolean isPersonWithDisability = false;

    private String disabilityCertificateBoardName;

    private String disabilityCertificateReferenceNo;

    private LocalDate disabilityCertificateIssueDate;

}