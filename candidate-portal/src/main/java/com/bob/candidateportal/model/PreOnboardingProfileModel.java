package com.bob.candidateportal.model;

import com.bob.commonutil.model.CandidateParentModel;
import com.bob.db.dto.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreOnboardingProfileModel {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CandidateParentModel candidateParentModel;
    private PreOnboardingPersonalDetailsDTO personalDetails;
    private PreOnboardingAddressDTO address;
    private PreOnboardingDomicileDetailsDTO domicileDetails;
    private List<PreOnboardingDisabilityDetailsDTO> disabilityDetails;
    private List<PreOnboardingDocumentDTO> documents;
}
