package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class EducationDTO extends BaseDTO implements Serializable {

    @JsonProperty("educationId")
    private UUID id;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotNull(message = "Education qualification is required")
    private UUID educationQualificationsId;

    @NotBlank(message = "Institution name is required")
    private String institutionName;

    private UUID specializationId;

    @NotNull(message = "Education type is required")
    private UUID educationTypeId;


    @PastOrPresent(message = "Start date must be in the past or present")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @PastOrPresent(message = "End date must be in the past or present")
    private LocalDate endDate;

    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0", message = "Percentage must be at least 0")
    @DecimalMax(value = "100", message = "Percentage cannot exceed 100")
    private BigDecimal percentage;

    private String universityName;

    private String otherQualification;
    private String otherSpecialization;

    private Boolean isValidationPending;
    private List<String> pendingChecks;
    private Boolean isSubmitted;
}