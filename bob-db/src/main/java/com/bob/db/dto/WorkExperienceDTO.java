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
public class WorkExperienceDTO extends BaseDTO implements Serializable {
    @JsonProperty("workExperienceId")
    private UUID id;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotNull(message = "From date is required")
    @PastOrPresent(message = "From date must be in the past or present")
    private LocalDate fromDate;

    @PastOrPresent(message = "To date must be in the past or present")
    private LocalDate toDate;

    @NotBlank(message = "Organization name is required")
    private String organizationName;

//    @NotBlank(message = "Role is required")
    private String role;

    private String postHeld;

    private Boolean isPresentlyWorking = false;

    private String workDescription;

    @PositiveOrZero(message = "Months of experience must be zero or positive")
    private Short monthsOfExp;

    @PositiveOrZero(message = "Current CTC must be zero or positive")
    private BigDecimal currentCtc;

    private Boolean isValidationPending;
    private List<String> pendingChecks;

}
