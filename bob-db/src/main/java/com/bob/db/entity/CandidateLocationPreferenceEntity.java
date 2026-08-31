package com.bob.db.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "candidate_location_preference", schema = "candidate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE candidate.candidate_location_preference SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class CandidateLocationPreferenceEntity extends BaseEntity<UUID> {
    @Column(name = "candidate_id")
    private UUID candidateId;

    @Column(name = "position_id")
    private UUID positionId;

    @Column(name = "country_id")
    private UUID countryId;

    @Column(name = "state_preference1")
    private UUID statePreference1;

    @Column(name = "city_preference1")
    private UUID cityPreference1;

    @Column(name = "location_preference1")
    private UUID locationPreference1;

    @Column(name = "state_preference2")
    private UUID statePreference2;

    @Column(name = "city_preference2")
    private UUID cityPreference2;

    @Column(name = "location_preference2")
    private UUID locationPreference2;

    @Column(name = "state_preference3")
    private UUID statePreference3;

    @Column(name = "city_preference3")
    private UUID cityPreference3;

    @Column(name = "location_preference3")
    private UUID locationPreference3;

    @Column(name = "expected_ctc")
    private BigDecimal expectedCtc;

    @Column(name = "interview_center", length = 255)
    private UUID interviewCenter;

    @Column(name = "local_language_id")
    private UUID localLanguageId;

    @Column(name = "is_local_language_studied")
    @Builder.Default
    private Boolean isLocalLanguageStudied = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dynamic_form_data", columnDefinition = "jsonb")
    private JsonNode dynamicFormData;
}
