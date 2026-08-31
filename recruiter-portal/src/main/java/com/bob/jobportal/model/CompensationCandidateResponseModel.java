package com.bob.jobportal.model;

import com.bob.db.dto.CandidateCompensationDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompensationCandidateResponseModel {

    private String resumeUrl;
    private CandidateCompensationDTO candidateCompensation;
}
