package com.bob.authapp.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class GetCandidateData {
    private String name;
    private String email;
    private Boolean isProfileCompleted;
    private Short currentStep;
    private UUID id;
}
