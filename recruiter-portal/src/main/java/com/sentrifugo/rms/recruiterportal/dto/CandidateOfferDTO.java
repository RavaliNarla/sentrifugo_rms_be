package com.sentrifugo.rms.recruiterportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateOfferDTO {
    private UUID id;
    private UUID candidateId;
    private String candidateName;
    private LocalDate acceptBeforeDate;
    private String offerFileUrl;
    private String status;
}
