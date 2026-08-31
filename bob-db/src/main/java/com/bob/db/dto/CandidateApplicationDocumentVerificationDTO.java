package com.bob.db.dto;

import com.bob.db.enums.DocumentScreeningStatus;
import com.bob.db.enums.DocumentZonalVerificationStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateApplicationDocumentVerificationDTO extends BaseDTO {

    @JsonProperty("verificationId")
    @JsonAlias("id")
    private UUID id;

    private UUID candidateDocumentId; //

    @NotNull(message = "Candidate Id is required")
    private UUID candidateId;

    @NotNull(message = "Application Id is required")
    private UUID applicationId;

    private DocumentScreeningStatus docScreeningStatus = DocumentScreeningStatus.PENDING;

    private String docScreeningComments;

    private UUID lastScreenedByUserId;

    private DocumentZonalVerificationStatus zonalHrDocStatus = DocumentZonalVerificationStatus.PENDING;

    private String zonalHrDocComments;


    private UUID documentId;

    private String fileUrl;

    private String displayName;
    
    private Boolean isValidationPending;

    private String documentNumber;

    private List<String> pendingChecks;

    private Boolean isDigilocker;

    @JsonProperty("id")
    public UUID getIdAlias() {
        return id;
    }
}

