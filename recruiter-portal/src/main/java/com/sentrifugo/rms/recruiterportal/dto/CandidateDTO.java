package com.sentrifugo.rms.recruiterportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDTO {
    private UUID id;

    @NotNull(message = "Requisition is required")
    private UUID requisitionId;

    @NotNull(message = "Position is required")
    private UUID positionId;
    private String positionTitleName;

    /** Constructed full name (First Middle Last). Kept for list/display compatibility. */
    private String name;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String genderHrmsId;
    private String genderName;

    private LocalDate dateOfBirth;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be a valid 10-digit number")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String resumeUrl;
    private boolean hasResume;

    private String idProofUrl;
    private boolean hasIdProof;

    private String photoUrl;
    private boolean hasPhoto;

    private String status;
    private Integer interviewRound;
    /** Latest offer status when candidate is in the offer pool; used for live labels in Candidate Pool. */
    private String offerStatus;
    private BigDecimal finalScore;

    private BigDecimal salary;

    // Compensation Management fields (Section 15 of the requirements doc)
    private BigDecimal currentCtc;
    private BigDecimal expectedCtc;
    private BigDecimal fixedPay;
    private BigDecimal variablePay;
    private BigDecimal bonus;
    private String compensationComments;
    private BigDecimal agreedCtc;
}
