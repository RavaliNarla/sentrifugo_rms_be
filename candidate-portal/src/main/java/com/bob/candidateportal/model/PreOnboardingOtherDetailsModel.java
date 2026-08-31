package com.bob.candidateportal.model;

import com.bob.commonutil.model.CandidateParentModel;
import com.bob.db.dto.PreOnboardingDocumentDTO;
import com.bob.db.dto.PreOnboardingOtherDetailsDTO;
import com.bob.db.dto.PreOnboardingReferenceDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class PreOnboardingOtherDetailsModel {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CandidateParentModel candidateParentModel;
    private List<PreOnboardingReferenceDTO> preOnboardingReferences;
    private PreOnboardingOtherDetailsDTO preOnboardingOtherDetails;
    private List<PreOnboardingDocumentDTO> preOnboardingDocuments;
}
