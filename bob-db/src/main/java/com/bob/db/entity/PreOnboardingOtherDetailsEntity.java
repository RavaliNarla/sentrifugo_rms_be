package com.bob.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "pre_onboarding_other_details", schema = "candidate")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE candidate.pre_onboarding_other_details SET is_active = false WHERE id = ?")
@Where(clause = "is_active = true")
public class PreOnboardingOtherDetailsEntity extends BaseEntity<UUID> {

    public static final String ENTITY_TYPE = "pre_onboarding_other_details";

    @Column(name = "pre_onboarding_id", nullable = false)
    private UUID preOnboardingId;

    @Column(name = "identification_mark", columnDefinition = "text")
    private String identificationMark;

    @Column(name = "practical_training_details", columnDefinition = "text")
    private String practicalTrainingDetails;

    @Column(name = "publication_details", columnDefinition = "text")
    private String publicationDetails;

    @Column(name = "pursuing_further_course")
    @Builder.Default
    private Boolean pursuingFurtherCourse = false;

    @Column(name = "further_course_details", columnDefinition = "text")
    private String furtherCourseDetails;

    @Column(name = "member_of_professional_body")
    @Builder.Default
    private Boolean memberOfProfessionalBody = false;

    @Column(name = "professional_body_details", columnDefinition = "text")
    private String professionalBodyDetails;

    @Column(name = "member_of_political_body")
    @Builder.Default
    private Boolean memberOfPoliticalBody = false;

    @Column(name = "political_body_details", columnDefinition = "text")
    private String politicalBodyDetails;

    @Column(name = "criminal_case_declared")
    @Builder.Default
    private Boolean criminalCaseDeclared = false;

    @Column(name = "criminal_case_details", columnDefinition = "text")
    private String criminalCaseDetails;

    @Column(name = "additional_information", columnDefinition = "text")
    private String additionalInformation;
}