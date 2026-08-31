package com.bob.commonutil.model;

import com.bob.db.dto.CandidateDocumentStoreDTO;
import com.bob.db.dto.WorkExperienceDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkExperienceResponseModel {
    private WorkExperienceDTO workExperience;
    private CandidateDocumentStoreDTO documentStore;
}
