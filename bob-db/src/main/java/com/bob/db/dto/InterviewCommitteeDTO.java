package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class InterviewCommitteeDTO  extends BaseDTO implements Serializable {
    @JsonProperty("interviewCommitteeId")
    private UUID id;
    private String committeeName;

    private String committeeDesc;

}
