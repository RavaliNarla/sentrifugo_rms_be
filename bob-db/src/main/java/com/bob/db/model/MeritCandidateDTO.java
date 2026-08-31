package com.bob.db.model;

import com.bob.db.enums.CandidateOfferStatus;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeritCandidateDTO {

    private UUID candidateId;

    private UUID applicationId;

    private CandidateOfferStatus offerStatus;

    // Reservation
    private UUID categoryId;

    private String category;

    private List<UUID> pwdCategoryIds;

    @Builder.Default
    private Boolean isPwd =false;

    @Builder.Default
    private Boolean isExServicemen =false;

    // State
    private UUID stateId;

    private UUID cityId;

    // Concessions
    @Builder.Default
    private Boolean usedAgeRelaxation =false;

    @Builder.Default
    private Boolean usedMarksRelaxation =false;

    @Builder.Default
    private Boolean usedInterviewRelaxation =false;

    @Builder.Default
    private Boolean generalEligible =false;

    // Marks
    private BigDecimal writtenScore;

    private BigDecimal interviewScore;

    private BigDecimal interviewWeightedScore;

    private BigDecimal examWeightedScore;

    // Calculated later
    private BigDecimal combinedScore;

    private UUID selectedCategoryId;

    @Builder.Default
    private Boolean selected = false;

    @Builder.Default
    private Boolean selectedAgainstPwd=false;

    private UUID selectedPwdCategoryId;

    private String selectionLabel;

    @Builder.Default
    private Boolean displaced =false;

    @Builder.Default
    private Boolean waitlisted =false;

    private String waitlistLabel;

    @Builder.Default
    private Boolean migratedToGeneral = false;

    // Ranking
    private Integer overallRank;

    private Integer categoryRank;

    @Builder.Default
    private Boolean isEWSSeat = false;

    // Tie breaker
    private LocalDate dob;
}
