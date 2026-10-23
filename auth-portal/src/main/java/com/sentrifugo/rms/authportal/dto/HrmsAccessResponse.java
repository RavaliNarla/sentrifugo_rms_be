package com.sentrifugo.rms.authportal.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HrmsAccessResponse {
    private boolean listed;
    private String role;
    /** l1_approver, l2_approver, or null. Not used to show the Recruitment link. */
    private String privileges;
    private boolean hasUpcomingInterviews;
}
