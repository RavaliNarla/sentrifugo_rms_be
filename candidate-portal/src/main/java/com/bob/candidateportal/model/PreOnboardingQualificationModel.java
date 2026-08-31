package com.bob.candidateportal.model;

import com.bob.commonutil.model.CandidateParentModel;
import com.bob.db.dto.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreOnboardingQualificationModel {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CandidateParentModel candidateParentModel;
    private List<PreOnboardingQualificationDTO> preOnboardingQualifications;
    private List<PreOnboardingCertificationDTO> preOnboardingCertifications;
    private PreOnboardingFamilyDetailsDTO preOnboardingFamilyDetails;
    private PreOnboardingGratuityNominationDTO preOnboardingGratuityNomination;
    private List<PreOnboardingDocumentDTO> preOnboardingDocuments;
}
