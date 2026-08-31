package com.bob.authapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class GetCandidateResponse {

    @JsonProperty("candidate_id")
    private UUID candidateId;
    @JsonProperty("full_name")
    private String fullName;
    private String email;
    private String username;
}
