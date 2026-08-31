package com.bob.candidateportal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class ShortlistCandidateModel  {

    private UUID candidateId;

    private UUID positionId;

}
