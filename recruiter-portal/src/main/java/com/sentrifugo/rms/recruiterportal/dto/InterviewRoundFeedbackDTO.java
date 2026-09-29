package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** One interview round's interviewer markings for the Interview Pool tooltip. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoundFeedbackDTO {
    private Integer round;
    private String roundName;
    private List<PanelMemberScoreViewDTO> scores;
}
