package com.bob.commonutil.model;

import com.bob.db.dto.CandidateDisabilityDetailsDTO;
import com.bob.db.dto.CandidateProfileDTO;
import com.bob.db.dto.LanguagesKnownDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BasicDetailsModel {
    @NotNull(message = "Candidate profile is required")
    @Valid
    private CandidateProfileDTO candidateProfile;
    
    @Valid
    private List<LanguagesKnownDTO> languagesKnown;
    
    @Valid
    private List<CandidateDisabilityDetailsDTO> disabilityDetails;
}
