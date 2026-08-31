package com.bob.db.model;

import com.bob.db.dto.CandidateCertificationsDTO;
import com.bob.db.dto.CandidateDocumentStoreDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateCertificationsResponseModel {
    private CandidateCertificationsDTO certifications;
    private CandidateDocumentStoreDTO documentStore;
}
