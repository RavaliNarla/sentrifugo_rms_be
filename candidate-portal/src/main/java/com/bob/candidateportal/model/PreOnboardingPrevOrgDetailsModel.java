package com.bob.candidateportal.model;

import com.bob.commonutil.model.CandidateParentModel;
import com.bob.db.dto.PreOnboardingDocumentDTO;
import com.bob.db.dto.PreOnboardingFamilyDetailsDTO;
import com.bob.db.dto.PreOnboardingPreviousOrganisationDTO;
import com.bob.db.dto.PreOnboardingResidentialHistoryDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreOnboardingPrevOrgDetailsModel {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CandidateParentModel candidateParentModel;
    private List<PreOnboardingResidentialHistoryDTO> residentialHistory;
    private List<PreOnboardingPreviousOrganisationDTO> previousOrganisations;
    private List<PreOnboardingDocumentDTO> documents;
}
