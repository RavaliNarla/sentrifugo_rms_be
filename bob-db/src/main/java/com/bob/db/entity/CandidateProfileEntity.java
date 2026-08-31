package com.bob.db.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Entity
@Table(name = "candidate_profile", schema = "candidate")
@Data
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate.candidate_profile SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateProfileEntity extends BaseEntity<UUID> {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "full_name_aadhar", length = 150, nullable = false)
    private String fullNameAadhar;

    @Column(name = "full_name_ssc", length = 150, nullable = false)
    private String fullNameSSC;

    @Column(name = "gender_id", nullable = false)
    private UUID genderId; //Need to check

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "marital_status_id", nullable = false)
    private UUID maritalStatusId;//Need to check

    @Column(name = "nationality", nullable = false)
    private UUID nationality;

    @Column(name = "religion_id", nullable = false)
    private UUID religionId; //Need to check

    @Column(name = "reservation_category_id", nullable = false)
    private UUID reservationCategoryId; //Need to check

    @Column(name = "community", length = 100)
    private String community;

    @Column(name = "state_id")
    private UUID stateId; // Need to check

    @Column(name = "mother_name", length = 100, nullable = false)
    private String motherName;

    @Column(name = "father_name", length = 100, nullable = false)
    private String fatherName;

    @Column(name = "spouse_name", length = 100)
    private String spouseName;

    @Column(name = "contact_no", length = 15, nullable = false)
    private String contactNo;

    @Column(name = "alt_contact_no", length = 15)
    private String altContactNo;

    @Column(name = "scocial_media_profile_link", length = 255)
    private String socialMediaProfileLink;


    // basic details tab 2 - sibling
    @Column(name = "is_twin", nullable = false)
    private Boolean isTwin;

    @Column(name = "twin_name", length = 50)
    private String twinName;

    @Column(name = "twin_gender_id")
    private UUID twinGenderId;


    // basic details tab 3 - disability
    @Column(name = "disability", nullable = false)
    private Boolean disability;


    @Column(name = "is_scribe_requirement")
    private Boolean isScribeRequirement;


    // basic details tab 4 - other details
    @Column(name = "ex_serviceman")
    private UUID exServiceman;

    @Column(name = "service_start_date")
    private LocalDate serviceStartDate;

    @Column(name = "service_end_date")
    private LocalDate serviceEndDate;

    @Column(name = "service_months")
    private Integer serviceMonths;

    @Column(name = "central_govt_employed", nullable = false)
    @Builder.Default
    private Boolean centralGovtEmployed = false;

    @Column(name = "employed_in_lower_post")
    private Boolean employedInLowerPost;


    // basic details tab 5 - additional details
    @Column(name = "riot_victim_family", nullable = false)
    @Builder.Default
    private Boolean riotVictimFamily = false;

    @Column(name = "minority", nullable = false)
    @Builder.Default
    private Boolean minority = false;

    @Column(name = "is_public_sector_undertaking")
    private Boolean isPublicSectorUndertaking;

    @Column(name = "any_disciplinary_action")
    private Boolean anyDisciplinaryAction;


    //extre fields
    @Column(name = "email", length = 150, nullable = false)
    private String email;

    @Column(name = "registration_no",updatable = true,insertable = false, length = 20)
    private String registrationNo;

    // disclaimer check boxes
    @Column(name = "ack_disclaimer_accepted", nullable = false)
    private Boolean ackDisclaimerAccepted;

    @Column(name = "doc_upload_disclaimer_accepted", nullable = false)
    private Boolean docUploadDisclaimerAccepted;

    @Column(name = "work_exp_disclaimer_accepted", nullable = false)
    private Boolean workExpDisclaimerAccepted;

    @Column(name = "is_fresher")
    private Boolean isFresher;

    @Column(name="cibil_score")
    private BigDecimal cibilScore;

    @Column(name = "has_certification")
    private Boolean hasCertification;

    @Column(name = "dob_proof_type")
    private String dobProofType;


}
