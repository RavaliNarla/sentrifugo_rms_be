package com.bob.jobportal.model;

import com.bob.db.dto.CandidateCompensationDTO;
import com.bob.db.enums.CompensationActionEnum;
import lombok.Data;

@Data
public class CandidateCompensationActionRequestModel {
    private CandidateCompensationDTO compensation;
    private CompensationActionEnum action;
}

