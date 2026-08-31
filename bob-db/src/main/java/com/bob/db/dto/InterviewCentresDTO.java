package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InterviewCentresDTO extends BaseDTO {

    @JsonProperty("interviewCentreId")
    private UUID id;

    private String interviewCentre;

//    private String organizationName;

    private String organizationType;

    private String zone;

    private UUID zonalStateId;

    private String alpha;

    private String displayName;

}

