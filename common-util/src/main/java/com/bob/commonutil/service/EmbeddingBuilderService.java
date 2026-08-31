package com.bob.commonutil.service;

import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.db.entity.*;
import com.bob.db.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.sql.ast.tree.expression.Collation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmbeddingBuilderService {
    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private JobPositionEmbeddingsRepository jobPositionEmbeddingsRepository;

    @Autowired
    private CandidateEmbeddingsRepository candidateEmbeddingsRepository;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private EducationQualificationsRepository educationQualificationsRepository;

    @Autowired
    private SpecializationMasterRepository specializationRepository;

    @Autowired
    private EducationTypeMasterRepository educationTypeRepository;

    @Autowired
    private CandidateCertificationsRepository candidateCertificationsRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateRankingResultsRepository candidateRankingResultsRepository;

    @Autowired
    private ScoringWeightageRepository scoringWeightageRepositor;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;


    private record TextAndScore(String text, BigDecimal score) {
    }



    @Async
    public void generateEmbeddingForJobPoitions(List<UUID> requisitionIds) {
        List<JobPositionsEntity> positions =
                positionsRepository.findByRequisitionIdIn(requisitionIds);
        generateEmbeddingsForPositions(positions);
    }

    public void generateEmbeddingsForPositions(List<JobPositionsEntity> positions) {
        if (positions == null || positions.isEmpty()) {
            return;
        }

        List<UUID> positionIds = positions.stream()
                .map(JobPositionsEntity::getId)
                .toList();

        List<JobPositionEmbeddingsEntity> existingEmbeddings = jobPositionEmbeddingsRepository.findByPositionIdIn(positionIds);

        Map<UUID, JobPositionEmbeddingsEntity> existingEmbeddingMap =
                existingEmbeddings.stream()
                        .collect(Collectors.toMap(
                                JobPositionEmbeddingsEntity::getPositionId,
                                Function.identity()
                        ));

        List<JobPositionEmbeddingsEntity> embeddingsToSave = new ArrayList<>();

        for (JobPositionsEntity position : positions) {

            String education = String.join("\n",
                    Objects.toString(position.getMandatoryEducation(), ""),
                    Objects.toString(position.getPreferredEducation(), "")
            );

            String experience = String.join("\n",
                    Objects.toString(position.getMandatoryExperience(), ""),
                    Objects.toString(position.getPreferredExperience(), ""),
                    Objects.toString(position.getRolesResponsibilities(), "")
            );

            float[] educationEmbedding =
                    openAIService.generateEmbedding(education);

            float[] experienceEmbedding =
                    openAIService.generateEmbedding(experience);

            JobPositionEmbeddingsEntity embeddingEntity =
                    existingEmbeddingMap.getOrDefault(position.getId(), new JobPositionEmbeddingsEntity());

            embeddingEntity.setPositionId(position.getId());
            embeddingEntity.setEducationEmbedding(educationEmbedding);
            embeddingEntity.setExperienceEmbedding(experienceEmbedding);
            embeddingEntity.setEducationEmbeddingRawText(education);
            embeddingEntity.setExperienceEmbeddingRawText(experience);

            embeddingsToSave.add(embeddingEntity);
        }

        jobPositionEmbeddingsRepository.saveAll(embeddingsToSave);
    }

    /**
     * Same idea as a cron: find paid applications with no ranking row,
     * ensure the job has embeddings, then generate candidate embeddings + score.
     */
    public Map<String, Object> backfillMissingRankingResults() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();

        if (scoringWeightageRepositor.findAll().isEmpty()) {
            result.put("error", "scoring_weightage is empty");
            return result;
        }

        List<CandidateApplicationsEntity> applications = candidateApplicationsRepository.findAll();
        Set<UUID> scoredApplicationIds = candidateRankingResultsRepository.findAll().stream()
                .map(CandidateRankingResultsEntity::getApplicationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<CandidateApplicationsEntity> missing = applications.stream()
                .filter(app -> !scoredApplicationIds.contains(app.getId()))
                .toList();

        List<UUID> positionIds = missing.stream()
                .map(CandidateApplicationsEntity::getPositionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<UUID> positionsWithEmbeddings = positionIds.isEmpty()
                ? Set.of()
                : jobPositionEmbeddingsRepository.findByPositionIdIn(positionIds).stream()
                .map(JobPositionEmbeddingsEntity::getPositionId)
                .collect(Collectors.toSet());

        List<UUID> positionsNeedingEmbeddings = positionIds.stream()
                .filter(id -> !positionsWithEmbeddings.contains(id))
                .toList();

        int jobsGenerated = 0;
        if (!positionsNeedingEmbeddings.isEmpty()) {
            List<JobPositionsEntity> positions = positionsRepository.findAllById(positionsNeedingEmbeddings);
            generateEmbeddingsForPositions(positions);
            jobsGenerated = positions.size();
        }

        int succeeded = 0;
        int failed = 0;
        for (CandidateApplicationsEntity application : missing) {
            generateEmbeddingForCandidate(application.getCandidateId(), application.getId());
            boolean saved = candidateRankingResultsRepository.findByApplicationId(application.getId()).isPresent();
            if (saved) {
                succeeded++;
            } else {
                failed++;
                failures.add(String.valueOf(application.getId()));
            }
        }

        result.put("applicationsMissingScore", missing.size());
        result.put("jobEmbeddingsGenerated", jobsGenerated);
        result.put("scoresCreated", succeeded);
        result.put("failed", failed);
        result.put("failedApplicationIds", failures);
        return result;
    }

    @Async
    public void generateEmbeddingForCandidate(UUID candidateId, UUID applicationId) {
        try{
        // Get education details
        TextAndScore educationText = buildEducationText(candidateId);

        // Get work experience details
        TextAndScore workExperienceText = buildWorkExperienceText(candidateId);

        // Get certifications
        String certificationText = buildCertificationProfileText(candidateId);

        // Build combined education text safely
        String educationAndCertificationText = String.join("\n",
                Objects.toString(educationText.text, ""),
                Objects.toString(certificationText, "")
        );

        // Build experience text safely
        String experienceText = Objects.toString(workExperienceText.text, "");

        // Generate embeddings
        float[] educationEmbedding = openAIService.generateEmbedding(educationAndCertificationText);

        float[] experienceEmbedding = openAIService.generateEmbedding(experienceText);

        // Check existing embedding
        CandidateEmbeddingsEntity candidateEmbeddingsEntity =
                candidateEmbeddingsRepository
                        .findByApplicationId(applicationId)
                        .orElse(new CandidateEmbeddingsEntity());

        // Update entity
        candidateEmbeddingsEntity.setCandidateId(candidateId);
        candidateEmbeddingsEntity.setApplicationId(applicationId);

        candidateEmbeddingsEntity.setEducationEmbedding(educationEmbedding);
        candidateEmbeddingsEntity.setExperienceEmbedding(experienceEmbedding);

        candidateEmbeddingsEntity.setEducationScore(educationText.score);
        candidateEmbeddingsEntity.setExperienceScore(workExperienceText.score);

        candidateEmbeddingsEntity.setEducationEmbeddingRawText(
                educationAndCertificationText
        );

        candidateEmbeddingsEntity.setExperienceEmbeddingRawText(
                experienceText
        );

        candidateEmbeddingsRepository.save(candidateEmbeddingsEntity);

        // Recalculate match score
        calculateJobMatchScore(applicationId);
    }catch (Exception e){
        log.error("Error generating embedding for candidate application. candidateId: {}, applicationId: {}, error: {}",candidateId,applicationId, e.getMessage());
    }
    }

    private TextAndScore buildEducationText(UUID candidateId) {

        List<EducationEntity> educations = educationRepository.findByCandidateId(candidateId);

        List<UUID> educationQualificationsIds = educations.stream().map(EducationEntity::getEducationQualificationsId).toList();
        List<UUID> specializationsIds = educations.stream().map(EducationEntity::getSpecializationId).toList();

        // get education qualifications for candidate
        List<EducationQualificationsEntity> educationQualificationsEntities = educationQualificationsRepository.findAllById(educationQualificationsIds);

        List<UUID> educationTypesIds = educationQualificationsEntities.stream().map(EducationQualificationsEntity::getLevelId).toList();

        List<DocumentTypesEntity> documentTypes = documentTypesRepository.findAllById(educationTypesIds);

        BigDecimal maxValue = documentTypes.stream()
                .map(DocumentTypesEntity::getScore)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(BigDecimal::valueOf)
                .orElse(BigDecimal.ZERO);

        // get specialization for candidate
        List<SpecializationMasterEntity> specializationEntities = specializationRepository.findAllById(specializationsIds);

        Map<UUID, String> qualificationMap = educationQualificationsEntities.stream()
                .collect(Collectors.toMap(
                        EducationQualificationsEntity::getId,
                        EducationQualificationsEntity::getQualificationName,
                        (existing, replacement) -> existing
                ));

        Map<UUID, String> specializationMap = specializationEntities.stream()
                .collect(Collectors
                        .toMap(SpecializationMasterEntity::getId,
                                SpecializationMasterEntity::getSpecializationName,
                                (existing, replacement) -> existing));

        Map<UUID, String> educationTypeMap = documentTypes.stream().
                collect(Collectors.toMap(DocumentTypesEntity::getId,
                        DocumentTypesEntity::getDocumentName,
                        (existing, replacement) -> existing));



        boolean check = false;
        StringBuilder sb = new StringBuilder();

        educations.sort(Comparator.comparing(EducationEntity::getEndDate,Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        for (EducationEntity edu : educations) {
            check = true;
            String qualification = qualificationMap.getOrDefault(
                    edu.getEducationQualificationsId(), "Unknown Qualification");

            String specialization = specializationMap.getOrDefault(
                    edu.getSpecializationId(), "");

            String educationType = educationTypeMap.getOrDefault(
                    edu.getEducationTypeId(), "");

            sb.append(qualification);

            if (specialization != null && !specialization.isBlank()) {
                sb.append(" in ").append(specialization).append(" ");
            }

            //sb.append("\n");

            sb.append(edu.getInstitutionName()).append(" ");

            sb.append("\n");
        }

        return new TextAndScore(check ? sb.toString() : null, maxValue);
    }

    private String buildCertificationProfileText(UUID candidateId) {

        List<CandidateCertificationsEntity> certifications = candidateCertificationsRepository.findByCandidateId(candidateId);
        certifications.sort(Comparator.comparing(CandidateCertificationsEntity::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        boolean check = false;
        StringBuilder sb = new StringBuilder();
        //    sb.append("Certifications:\n\n");

        for (CandidateCertificationsEntity cert : certifications) {

            sb.append(cert.getCertificationName()).append("\n");
            //sb.append("Issued By: ").append(cert.getIssuedBy()).append("\n");
            sb.append(cert.getIssuedBy()).append(" ");
            check = true;

            sb.append("\n");
        }

        return check ? sb.toString() : null;
    }

    private TextAndScore buildWorkExperienceText(UUID candidateId) {
        List<WorkExperienceEntity> experiences = workExperienceRepository.findByCandidateId(candidateId);

        BigDecimal totalExp = BigDecimal.ZERO;
        StringBuilder sb = new StringBuilder();

        boolean check = false;
        //sb.append("Work Experience:\n");
        experiences.sort(Comparator.comparing(WorkExperienceEntity::getFromDate,Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        for (WorkExperienceEntity exp : experiences) {
            if (exp.getMonthsOfExp() != null) {
                totalExp = totalExp.add(BigDecimal.valueOf(exp.getMonthsOfExp()));
            }
            check = true;
            // Role / Post Held
            if (exp.getPostHeld() != null && !exp.getPostHeld().isBlank()) {
                sb.append(exp.getPostHeld()).append(" ");
            }

            // Organization
            sb.append(exp.getOrganizationName()).append(" ");

            //sb.append("\n\n");

            // Role
            sb.append("Role: ").append(exp.getRole()).append(" ");

            // Description
            if (exp.getWorkDescription() != null) {
                sb.append(exp.getWorkDescription()).append(" ");
            }
            sb.append("\n");
        }
        BigDecimal years = totalExp.divide(BigDecimal.valueOf(12), 1, RoundingMode.HALF_UP);

        return new TextAndScore(check ? sb.toString() : null, years);
    }


    public Double calculateJobMatchScore(UUID applicationId) {

        CandidateApplicationsEntity candidateApplications =
                candidateApplicationsRepository.findByApplicationIdIgnoringPaymentStatus(applicationId)
                        .orElseThrow(() -> new RuntimeException("Invalid application id"));

        CandidateEmbeddingsEntity candidateEmbeddings =
                candidateEmbeddingsRepository.findByApplicationId(applicationId)
                        .orElseThrow(() -> new RuntimeException("Invalid application id"));

        JobPositionEmbeddingsEntity jobPositionEmbeddings =
                jobPositionEmbeddingsRepository.findByPositionId(candidateApplications.getPositionId())
                        .orElseThrow(() -> new RuntimeException("Invalid position id"));

        // ⚠ Better to fetch active weight instead of findAll().get(0)
        ScoringWeightageEntity weightAge =
                scoringWeightageRepositor.findAll().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("Weightage not configured"));

        // ---- Similarity (0–1 → convert to 0–100 scale) ----
        BigDecimal educationSimilarity =
                cosineSimilarity(jobPositionEmbeddings.getEducationEmbedding(),
                        candidateEmbeddings.getEducationEmbedding()).multiply(BigDecimal.valueOf(100));

        BigDecimal experienceSimilarity =
                cosineSimilarity(
                        jobPositionEmbeddings.getExperienceEmbedding(),
                        candidateEmbeddings.getExperienceEmbedding()
                ).multiply(BigDecimal.valueOf(100));

        // ---- Null-safe scores ----
        BigDecimal educationScore =
                Optional.ofNullable(candidateEmbeddings.getEducationScore())
                        .orElse(BigDecimal.ZERO);

        BigDecimal experienceScore =
                Optional.ofNullable(candidateEmbeddings.getExperienceScore())
                        .orElse(BigDecimal.ZERO);

        BigDecimal score = calculateExperienceScore(experienceScore, BigDecimal.valueOf(12));

        // ---- Weighted Parts ----
        BigDecimal educationPart =weighted(educationSimilarity, weightAge.getEducationSimilarity());
        BigDecimal experiencePart = weighted(experienceSimilarity, weightAge.getExperienceSimilarity());
        BigDecimal educationScorePart = weighted(educationScore, weightAge.getEducationScore());
        BigDecimal experienceScorePart = weighted(score,weightAge.getExperienceScore());

        if (experiencePart.compareTo(BigDecimal.TEN) < 0) {
            log.info(
                    "Experience part is below threshold. applicationId: {}, experiencePart: {}, experienceSimilarity: {}, weight: {}",
                    applicationId,
                    experiencePart,
                    experienceSimilarity,
                    weightAge.getExperienceSimilarity()
            );
            educationScorePart = BigDecimal.ZERO;
            experienceScorePart = BigDecimal.ZERO;
        }

        // ---- Final Score ----
        BigDecimal finalScore =
                educationPart
                        .add(experiencePart)
                        .add(educationScorePart)
                        .add(experienceScorePart)
                        .setScale(2, RoundingMode.HALF_UP);  // important for numeric(5,2)

        // ---- Upsert Ranking Result ----
        CandidateRankingResultsEntity rankingResult =
                candidateRankingResultsRepository
                        .findByApplicationId(applicationId)
                        .orElse(new CandidateRankingResultsEntity());

        rankingResult.setCandidateId(candidateApplications.getCandidateId());;
        rankingResult.setApplicationId(applicationId);

        rankingResult.setEducationSimilarity(educationPart);
        rankingResult.setExperienceSimilarity(experiencePart);

        rankingResult.setEducationScore(educationScorePart);
        rankingResult.setExperienceScore(experienceScorePart);

        rankingResult.setFinalScore(finalScore);

        candidateRankingResultsRepository.save(rankingResult);

        return finalScore.doubleValue();
    }

    private BigDecimal calculateExperienceScore(BigDecimal candidateYears, BigDecimal maxConsideredYears) {

        if (candidateYears == null || maxConsideredYears == null
                || maxConsideredYears.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Cap experience at max
        BigDecimal effectiveYears =
                candidateYears.min(maxConsideredYears);

        return effectiveYears
                .divide(maxConsideredYears, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal cosineSimilarity(float[] jobEmbedding, float[] candidateEmbedding) {

        if (jobEmbedding == null || candidateEmbedding == null) {
            return BigDecimal.ZERO;
        }
        if (jobEmbedding.length != candidateEmbedding.length) {
            throw new IllegalArgumentException("Embeddings must be same length");
        }

        double dotProduct = 0.0;
        double jobNorm = 0.0;
        double candidateNorm = 0.0;

        for (int i = 0; i < jobEmbedding.length; i++) {
            double jobValue = jobEmbedding[i];
            double candidateValue = candidateEmbedding[i];

            dotProduct += jobValue * candidateValue;
            jobNorm += jobValue * jobValue;
            candidateNorm += candidateValue * candidateValue;
        }

        if (jobNorm == 0 || candidateNorm == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(dotProduct / (Math.sqrt(jobNorm) * Math.sqrt(candidateNorm)));
    }


    private BigDecimal weighted(BigDecimal value, BigDecimal weight) {

        if (value == null  || weight == null) {
            return BigDecimal.ZERO;
        }
        return value.multiply(weight);
    }
}