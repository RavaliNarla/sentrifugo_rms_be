package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CandidateCertificationsDTO extends BaseDTO {

    @JsonProperty("certificateId")
    private UUID id;
    private UUID candidateId;
    private String certificationName;
    private String issuedBy;
    private LocalDate certificationDate;
    private LocalDate expiryDate;
    private UUID certificationId;
    private Boolean isValidationPending;
    private List<String> pendingChecks;

}

