package com.bob.db.dto;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
public class CandidateConcessionsDTO extends BaseDTO{

    private UUID applicationId;


    private Boolean examConcession;


    private Boolean ageConcession;

    private Boolean interviewConcession;
}
