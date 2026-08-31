package com.bob.authapp.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CandidateLoginResponse {
//    private Map<String, Object> user;
    private GetCandidateData user;
    private String accessToken;
}
