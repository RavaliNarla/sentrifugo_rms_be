package com.bob.db.dto;

import com.bob.db.entity.LanguagesKnownEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Data
public class CandidateProfileDTO extends BaseDTO implements Serializable {
    @JsonProperty("candidateProfileId")
    private UUID id;

    @NotNull(message = "Candidate ID is required")
    private UUID candidateId;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    private String lastName;

    @NotBlank(message = "Full name as per Aadhar is required")
    private String fullNameAadhar;

    @NotBlank(message = "Full name as per SSC is required")
    private String fullNameSSC;

    @NotNull(message = "Gender is required")
    private UUID genderId;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private UUID maritalStatusId;

    private UUID nationality;

    private UUID religionId;

    private UUID reservationCategoryId;

    private String community;

    private CandidateDisabilityDetailsDTO candidateDisabilityDetailsDTO;

    private UUID stateId;

    private String motherName;

    private String fatherName;

    private String spouseName;

    private String contactNo;

    private String altContactNo;

    private String socialMediaProfileLink;


    // basic details tab 2 - sibling
    private Boolean isTwin;

    private String twinName;

    private UUID twinGenderId;


    // basic details tab 3 - disability
    private Boolean disability;
    
    private Boolean isScribeRequirement;


    // basic details tab 4 - other details
    private UUID exServiceman;

    private LocalDate serviceStartDate;

    private LocalDate serviceEndDate;

    private Integer serviceMonths;

    private Boolean centralGovtEmployed = false;

    private Boolean employedInLowerPost;


    // basic details tab 5 - additional details
    private Boolean riotVictimFamily = false;

    private Boolean minority = false;

    private Boolean isPublicSectorUndertaking;

    private Boolean anyDisciplinaryAction;


    //extre fields
    private String email;

    private String registrationNo;

    // disclaimer check boxes
    private Boolean ackDisclaimerAccepted =false;

    private Boolean docUploadDisclaimerAccepted = false;

    private Boolean workExpDisclaimerAccepted=false;

    private Boolean isFresher =false;
    private BigDecimal cibilScore;

    private Boolean hasCertification =false;

    private String dobProofType;
}
