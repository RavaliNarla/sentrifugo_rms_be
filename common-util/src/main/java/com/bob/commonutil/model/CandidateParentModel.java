package com.bob.commonutil.model;

import com.bob.db.dto.CandidateAddressDTO;
import com.bob.db.dto.CandidateApplicationDocumentVerificationDTO;
import com.bob.db.dto.CandidateDocumentStoreDTO;
import com.bob.db.dto.CandidateLocationPreferenceDTO;
import com.bob.db.model.CandidateCertificationsResponseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CandidateParentModel {
    private BasicDetailsModel basicDetails;
    private CandidateAddressDTO addressDetails;
    private List<EducationResponseModel> educationDetails;
    private List<WorkExperienceResponseModel> experienceDetails;
    private List<CandidateDocumentStoreDTO> documentDetails;
    private CandidateLocationPreferenceDTO locationPreference;
    private List<CandidateApplicationDocumentVerificationDTO> appSpecificDocDetails;
    private List<CandidateCertificationsResponseModel> candidateCertifications;
    private String age;
}
