package com.bob.candidateportal.model;

import com.bob.db.enums.CandidateOfferStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOfferDecisionModel {

    private UUID applicationId;

    private CandidateOfferStatus status;

    private String comments;

    private UUID medicalCentreId;

}
