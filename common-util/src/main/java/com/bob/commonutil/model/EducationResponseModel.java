package com.bob.commonutil.model;

import com.bob.db.dto.CandidateDocumentStoreDTO;
import com.bob.db.dto.EducationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EducationResponseModel {
    public EducationDTO education;
    public CandidateDocumentStoreDTO documentStore;
}
