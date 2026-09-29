package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelMemberScoreViewDTO {
    private String interviewerName;
    private BigDecimal score;
    private String rationale;
    private String decision;
    /** Optional competency ratings (1–5), keyed like TECHNICAL_KNOWLEDGE. */
    private Map<String, Integer> competencyRatings;
    private String keyObservations;
}
