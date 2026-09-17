package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelMemberScoreViewDTO {
    private String interviewerName;
    private BigDecimal score;
    private String rationale;
    private String decision;
}
