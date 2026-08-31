package com.bob.candidateportal.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class OfferLetterModel {
    private String offerLetterUrl;
    private LocalDate acceptBeforeDate;
}
