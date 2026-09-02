package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateOfferRequest {

    @NotEmpty(message = "Select at least one candidate")
    private List<UUID> candidateIds;

    @NotNull(message = "Offer template is required")
    private UUID templateId;

    @NotNull(message = "Accept-before date is required")
    private LocalDate acceptBeforeDate;
}
