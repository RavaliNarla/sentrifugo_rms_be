package com.bob.candidateportal.model;

import com.bob.db.dto.CandidateApplicationDocumentVerificationDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ScreeningAndZonalRejectedDocuments {
    List<CandidateApplicationDocumentVerificationDTO> RejectedDocuments;
    private LocalDate submitBeforeDate;

}
