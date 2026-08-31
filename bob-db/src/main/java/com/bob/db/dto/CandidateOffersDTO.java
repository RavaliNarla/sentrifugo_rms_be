package com.bob.db.dto;

import com.bob.db.enums.CandidateOfferStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CandidateOffersDTO extends BaseDTO implements Serializable {

    @JsonProperty("candidateOfferId")
    private UUID id;

    private LocalDate offerReleaseDate;

    private LocalDate joiningDate;

    private UUID designation;

    private BigDecimal ctc;

    private BigDecimal bonus;

    private CandidateOfferStatus status;

    private String selectList;

    private String waitList;

    private LocalDate acceptBeforeDate;

    private UUID templateId;

    @NotNull(message = "Application ID is required")
    private UUID applicationId;

    @NotNull(message = "Position ID is required")
    private UUID positionId;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotNull(message = "Location ID is required")
    private UUID locationId;

    private String offerFileUrl;

    private UUID medicalCenterId;

    private String candidateComments;

    private boolean isQualified;

    private UUID signatryId;

    private String signatoryName;

    private String signatoryDesignation;

    private String letterNumber;
}
