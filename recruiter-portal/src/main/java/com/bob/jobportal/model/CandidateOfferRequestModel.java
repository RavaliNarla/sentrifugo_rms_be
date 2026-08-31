package com.bob.jobportal.model;

import com.bob.db.dto.CandidateOffersDTO;
import com.bob.db.enums.InterviewSchedulingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CandidateOfferRequestModel {

    CandidateOffersDTO candidateOffersDTO;

    private BigDecimal finalScore;

    private InterviewSchedulingStatus interviewSchedulingStatus;

    private String candidateFullName;

    private String regNo;

    private String location;

    private String state;

    private String reservationCategory;

    private String designationName;

    private UUID offerApprovalId;

    private String candidateDob;

    private String age;

    private boolean hasAgeConcession;
    private BigDecimal examMarks;
    private BigDecimal interviewMarks;
}
