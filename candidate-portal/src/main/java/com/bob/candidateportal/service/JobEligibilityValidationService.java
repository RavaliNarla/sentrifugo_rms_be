package com.bob.candidateportal.service;

import com.bob.candidateportal.model.JobEligibilityValidationResponse;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import com.bob.db.entity.*;
import com.bob.db.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class JobEligibilityValidationService {

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private EducationQualificationsRepository educationQualificationsRepository;

    @Autowired
    private SpecializationMasterRepository specializationMasterRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CandidateDisabilityDetailsRepository candidateDisabilityDetailsRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private GenderMasterRepository genderMasterRepository;

    @Autowired
    private MaritalStatusMasterRepository maritalStatusMasterRepository;

    @Autowired
    private AgeRelaxationCategoriesRepository ageRelaxationCategoriesRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private AgeRelaxationApplicationRepository ageRelaxationApplicationRepository;

    @Autowired
    private CandidateCertificationsRepository candidateCertificationsRepository;

    @Autowired
    private CertificationMasterRepository certificationMasterRepository;

    @Autowired
    private EducationTypeMasterRepository educationTypeMasterRepository;

    @Autowired
    private LanguagesKnownRepository languagesKnownRepository;

    @Autowired
    private LanguageMasterRepository languageMasterRepository;

//    @Autowired
//    private JobPositionExclusionsRepository jobPositionExclusionsRepository;

    @Autowired
    private ExclusionsMasterRepository exclusionsMasterRepository;

    private static final List<String> EXCLUDED_ROLE_KEYWORDS = Arrays.asList("clerk", "peon", "clerical");

    @Autowired
    private QualificationGroupMappingRepository qualificationGroupMappingRepository;

    @Autowired
    private EducationGroupsRepository educationGroupsRepository;

    /**
     * Validates candidate eligibility against job position requirements
     *
     * @param candidateId The UUID of the candidate
     * @param positionId  The UUID of the job position
     * @return JobEligibilityValidationResponse or String message if requisition conflict
     */
    @Transactional
    public Object validateEligibility(UUID candidateId, UUID positionId) {
        log.info("Starting eligibility validation for candidateId: {} against positionId: {}", candidateId, positionId);

        // Validate job position exists
        JobPositionsEntity position = positionsRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Job position not found."));

        // Resolve effective cutoff date: requisition-level wins; fall back to position-level for old records.
        LocalDate effectiveCutoff = jobRequisitionsRepository.findById(position.getRequisitionId())
                .map(JobRequisitionsEntity::getCutoffDate)
                .orElse(null);
        if (effectiveCutoff == null) {
            effectiveCutoff = position.getCutoffDate();
        }
        position.setCutoffDate(effectiveCutoff);

        // Check if candidate has uploaded at least one ID verification document
        boolean hasIdVerificationDoc = candidateDocumentStoreRepository.existsByCandidateIdAndDocType(
                candidateId, 
                AppConstants.ID_VERIFICATION_DOC_TYPE
        );
        if (!hasIdVerificationDoc) {
            log.warn("Candidate {} has not uploaded any ID verification document", candidateId);
            return "Please upload an ID Proof before applying for a job position.";
        }

        // Check if candidate has already applied to a job in this requisition
        UUID requisitionId = position.getRequisitionId();
        if (requisitionId != null && candidateApplicationsRepository.existsByCandidateIdAndRequisitionId(candidateId, requisitionId)) {
            log.warn("Candidate {} has already applied to a job in requisition {}", candidateId, requisitionId);
            return "Cannot apply for multiple jobs within the same requisition.";
        }

        // Check if there are vacancies available for the candidate's reservation category
        String vacancyCheckMessage = checkVacancyAvailabilityForCandidate(candidateId, position);
        if (vacancyCheckMessage != null) {
            log.warn("Vacancy check failed for candidateId: {} in positionId: {}", candidateId, positionId);
            return vacancyCheckMessage;
        }

        // Build response — education runs FIRST, experience depends on education result
        JobEligibilityValidationResponse.EducationValidation educationValidation = validateEducation(candidateId, position);
        JobEligibilityValidationResponse.ExperienceValidation experienceValidation = validateExperience(candidateId, position, educationValidation);
        JobEligibilityValidationResponse.DocumentValidation documentValidation = validateDocuments(candidateId);
        JobEligibilityValidationResponse.AgeValidation ageValidation = validateAge(candidateId, position);
        
        // Check if all validations passed
        boolean allValidationsPassed = experienceValidation.getPassed() 
                && documentValidation.getPassed() 
                && educationValidation.getPassed() 
                && ageValidation.getPassed();
        
        // If all validations passed, save age relaxation application data
        if (allValidationsPassed) {
            saveAgeRelaxationApplicationIfNeeded(candidateId, position, ageValidation);
        }
        
        return JobEligibilityValidationResponse.builder()
                .eligibilityValidationReferenceDate(position.getCutoffDate())
                .experienceValidation(experienceValidation)
                .documentValidation(documentValidation)
                .educationValidation(educationValidation)
                .ageValidation(ageValidation)
                .build();
    }

    /**
     * Pre-validation gate: checks whether any vacancies exist for the candidate's
     * reservation category (or categories they are eligible to fill).
     *
     * Eligibility rules:
     *   SC  → SC, EWS, GEN
     *   ST  → ST, EWS, GEN
     *   OBC → OBC, EWS, GEN
     *   GEN → EWS, GEN
     *   EWS → EWS, GEN
     *
     * @return null if the check passes; an error message string if no vacancies are found.
     */
    private String checkVacancyAvailabilityForCandidate(UUID candidateId, JobPositionsEntity position) {
        CandidateProfileEntity candidateProfile = candidateProfileRepository.findByCandidateId(candidateId).orElse(null);
        if (candidateProfile == null) {
            log.warn("Candidate profile not found for candidateId: {} during vacancy check — skipping", candidateId);
            return null; // Let the four validations surface the missing-profile issue
        }

        UUID reservationCategoryId = candidateProfile.getReservationCategoryId();
        if (reservationCategoryId == null) {
            log.warn("No reservation category set for candidateId: {} — skipping vacancy check", candidateId);
            return null;
        }

        // Single DB call: load all reservation categories and build id → UPPER(code) map
        List<ReservationCategoriesEntity> allCategories = reservationCategoriesRepository.findAllByIsActiveTrue();
        Map<UUID, String> idToCode = allCategories.stream()
                .collect(Collectors.toMap(BaseEntity::getId, rc -> rc.getCategoryCode().toUpperCase()));

        String candidateCategoryCode = idToCode.get(reservationCategoryId);
        if (candidateCategoryCode == null) {
            log.warn("Reservation category id {} not found in master data — skipping vacancy check", reservationCategoryId);
            return null;
        }

        // Resolve UUIDs of the vacancy categories this candidate is eligible to fill
        Set<String> allowedCodes = resolveEligibleVacancyCategoryCodes(candidateCategoryCode);
        Set<UUID> allowedCategoryIds = allCategories.stream()
                .filter(rc -> allowedCodes.contains(rc.getCategoryCode().toUpperCase()))
                .map(BaseEntity::getId)
                .collect(Collectors.toSet());

        boolean hasVacancy;
        boolean isLocationWise = Boolean.TRUE.equals(position.getIsLocationWise());

        if (!isLocationWise) {
            // National job — check national distributions
            List<PositionCategoryNationalDistributionEntity> nationalDistributions =
                    position.getPositionCategoryNationalDistributions();
            hasVacancy = nationalDistributions != null && nationalDistributions.stream()
                    .anyMatch(dist -> !Boolean.TRUE.equals(dist.getIsDisability())
                            && dist.getVacancyCount() != null && dist.getVacancyCount() > 0
                            && allowedCategoryIds.contains(dist.getReservationCategoryId()));
        } else {
            // State-wise job — passes if at least one state has a matching vacancy
            List<PositionStateDistributionEntity> stateDistributions =
                    position.getPositionStateDistributions();
            hasVacancy = stateDistributions != null && stateDistributions.stream()
                    .filter(sd -> sd.getPositionCategoryDistributions() != null)
                    .flatMap(sd -> sd.getPositionCategoryDistributions().stream())
                    .anyMatch(dist -> !Boolean.TRUE.equals(dist.getIsDisability())
                            && dist.getVacancyCount() != null && dist.getVacancyCount() > 0
                            && allowedCategoryIds.contains(dist.getReservationCategoryId()));
        }

        if (!hasVacancy) {
            log.warn("No vacancies for candidateId: {} (category: {}) in positionId: {}",
                    candidateId, candidateCategoryCode, position.getId());
            return "No vacancies are available for your reservation category for this job position.";
        }

        return null; // Vacancy check passed
    }

    /**
     * Returns the set of vacancy category codes a candidate is eligible to apply against,
     * based on their own reservation category code.
     *
     * GEN and EWS vacancies are always in the allowed set because unreserved seats are open
     * to everyone, and EWS seats are pre-emptively included since unfilled EWS seats may be
     * re-allocated at the offer stage.
     */
    private Set<String> resolveEligibleVacancyCategoryCodes(String candidateCategoryCode) {
        Set<String> allowed = new HashSet<>();
        allowed.add(AppConstants.GENERAL_CATEGORY_CODE.toUpperCase()); // GEN — open to all
        allowed.add(AppConstants.EWS_CATEGORY_CODE);             // EWS — open to all
        switch (candidateCategoryCode.toUpperCase()) {
            case AppConstants.SC_CATEGORY_CODE:
                allowed.add(AppConstants.SC_CATEGORY_CODE);
                break;
            case AppConstants.ST_CATEGORY_CODE:
                allowed.add(AppConstants.ST_CATEGORY_CODE);
                break;
            case AppConstants.OBC_CATEGORY_CODE:
                allowed.add(AppConstants.OBC_CATEGORY_CODE);
                break;
            default:
                // GEN and EWS candidates: only GEN + EWS (already added above)
                break;
        }
        return allowed;
    }

    /**
     * Resolves allowed reservation-category IDs for state-wise vacancy validation.
     * If candidate category cannot be resolved, returns empty set.
     */
    private Set<UUID> resolveEligibleVacancyCategoryIds(UUID candidateReservationCategoryId) {
        if (candidateReservationCategoryId == null) {
            return Collections.emptySet();
        }

        List<ReservationCategoriesEntity> allCategories = reservationCategoriesRepository.findAllByIsActiveTrue();
        Map<UUID, String> idToCode = allCategories.stream()
                .collect(Collectors.toMap(BaseEntity::getId, rc -> rc.getCategoryCode().toUpperCase()));

        String candidateCategoryCode = idToCode.get(candidateReservationCategoryId);
        if (candidateCategoryCode == null) {
            return Collections.emptySet();
        }

        Set<String> allowedCodes = resolveEligibleVacancyCategoryCodes(candidateCategoryCode);
        return allCategories.stream()
                .filter(rc -> allowedCodes.contains(rc.getCategoryCode().toUpperCase()))
                .map(BaseEntity::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Reservation-category vacancy validation for a single state/city distribution.
     * Disability category does not drive this check; reservation category does.
     */
    private boolean hasEligibleReservationVacancyInState(
            PositionStateDistributionEntity stateDistribution,
            Set<UUID> allowedReservationCategoryIds) {

        if (stateDistribution == null
                || stateDistribution.getPositionCategoryDistributions() == null
                || stateDistribution.getPositionCategoryDistributions().isEmpty()
                || allowedReservationCategoryIds == null
                || allowedReservationCategoryIds.isEmpty()) {
            return false;
        }

        return stateDistribution.getPositionCategoryDistributions().stream()
                .anyMatch(dist -> !Boolean.TRUE.equals(dist.getIsDisability())
                        && dist.getVacancyCount() != null
                        && dist.getVacancyCount() > 0
                        && allowedReservationCategoryIds.contains(dist.getReservationCategoryId()));
    }

    /**
     * Helper method to save age relaxation application data if needed
     */
    private void saveAgeRelaxationApplicationIfNeeded(
            UUID candidateId,
            JobPositionsEntity position,
            JobEligibilityValidationResponse.AgeValidation ageValidation) {
        
        try {
            // Extract candidate age from the validation result
            String candidateAgeStr = ageValidation.getCandidateAge();
            if (candidateAgeStr == null) {
                log.warn("Candidate age is null, cannot save age relaxation application");
                return;
            }
            
            // Parse only the years part from the age string, e.g. "35 years 0 months 1 days" -> 35
            int candidateAge = Integer.parseInt(candidateAgeStr.trim().split("\\s+")[0]);
            
            // Get eligibility age limits
            Integer eligibilityAgeMax = position.getEligibilityAgeMax();
            if (eligibilityAgeMax == null) {
                log.warn("Eligibility age max is null, cannot save age relaxation application");
                return;
            }
            
            // Get candidate profile to determine relaxation categories
            CandidateProfileEntity candidateProfile = candidateProfileRepository.findByCandidateId(candidateId)
                    .orElse(null);
            
            if (candidateProfile == null) {
                log.warn("Candidate profile not found, cannot save age relaxation application");
                return;
            }
            
            // Check if job is location-wise (state-wise) or national
            boolean isLocationWise = Boolean.TRUE.equals(position.getIsLocationWise());
            
            if (isLocationWise) {
                // STATE-WISE JOB: Save one record per state
                List<JobEligibilityValidationResponse.StateWiseAgeValidation> stateValidations = 
                        ageValidation.getStateWiseAgeValidations();
                
                if (stateValidations == null || stateValidations.isEmpty()) {
                    log.warn("State-wise job but no state validations found for positionId: {}", position.getId());
                    return;
                }
                
                List<PositionStateDistributionEntity> stateDistributions = position.getPositionStateDistributions();
                if (stateDistributions == null || stateDistributions.isEmpty()) {
                    log.warn("State-wise job but no state distributions found for positionId: {}", position.getId());
                    return;
                }
                
                // Loop through each state and save a separate record
                for (JobEligibilityValidationResponse.StateWiseAgeValidation stateValidation : stateValidations) {
                    try {
                        UUID stateId = UUID.fromString(stateValidation.getStateId());
                        UUID cityId = stateValidation.getCityId() != null ? UUID.fromString(stateValidation.getCityId()) : null;
                        boolean stateAgePassed = stateValidation.getAgeValidationPassed() != null
                                ? stateValidation.getAgeValidationPassed()
                                : Boolean.TRUE.equals(stateValidation.getPassed());
                        boolean stateVacancyPassed = stateValidation.getStateVacancyValidationPassed() != null
                                ? stateValidation.getStateVacancyValidationPassed()
                                : false;
                        
                        // Find the state distribution for this state and city combination
                        Optional<PositionStateDistributionEntity> stateDistribution = 
                                stateDistributions.stream()
                                .filter(dist -> dist.getStateId().equals(stateId) && 
                                        Objects.equals(dist.getCityId(), cityId))
                                .findFirst();
                        
                        if (stateDistribution.isEmpty()) {
                            log.warn("State distribution not found for stateId: {}, cityId: {}", stateId, cityId);
                            continue;
                        }
                        
                        // Determine relaxation categories for this specific state
                        List<String> relaxationCategories = determineRelaxationForSpecificState(
                                candidateProfile, stateDistribution.get());
                        addSpecialRelaxations(candidateProfile, relaxationCategories, position);
                        
                        // Save record for this state and city
                        saveAgeRelaxationApplication(
                                candidateId, 
                                position.getId(), 
                                candidateAge, 
                                eligibilityAgeMax, 
                                relaxationCategories,
                                stateId,
                                cityId,
                                stateAgePassed,
                                stateVacancyPassed
                        );
                        
                    } catch (Exception e) {
                        log.error("Error saving age relaxation for state: {}", stateValidation.getStateId(), e);
                        // Continue with other states
                    }
                }
                
            } else {
                // NATIONAL JOB: Save one record with validationPassed = null and stateId = null (not applicable)
                List<String> relaxationCategories = determineRelaxationForNational(candidateProfile, position);
                addSpecialRelaxations(candidateProfile, relaxationCategories, position);
                
                saveAgeRelaxationApplication(
                        candidateId, 
                        position.getId(), 
                        candidateAge, 
                        eligibilityAgeMax, 
                        relaxationCategories,
                        null,
                        null,
                        null,
                        null
                );
            }
            
        } catch (Exception e) {
            log.error("Error in saveAgeRelaxationApplicationIfNeeded for candidateId: {}, positionId: {}", 
                    candidateId, position.getId(), e);
            // Don't fail the validation if saving fails
        }
    }



    /**
     * Validates candidate's work experience against job requirements.
     * Education must pass first — experience is only checked if education is fulfilled.
     *
     * <p>Three possible outcomes:
     * <ul>
     *   <li>Education not passed → passed=false, message="Education criteria must be passed first..."</li>
     *   <li>Education passed, experience passed → passed=true, message=null</li>
     *   <li>Education passed, experience failed → passed=false, detailed per-group message</li>
     * </ul>
     *
     * @param candidateId The UUID of the candidate
     * @param position    The job position entity
     * @param educationValidation The result of education validation (must be run first)
     * @return ExperienceValidation result
     */
    private JobEligibilityValidationResponse.ExperienceValidation validateExperience(
            UUID candidateId,
            JobPositionsEntity position,
            JobEligibilityValidationResponse.EducationValidation educationValidation) {

        log.debug("Validating experience for candidateId: {}", candidateId);

        // Gate: education must pass first
        if (!Boolean.TRUE.equals(educationValidation.getEducationPassed())) {
            log.info("Experience validation short-circuited — education not passed for candidateId: {}", candidateId);
            return JobEligibilityValidationResponse.ExperienceValidation.builder()
                    .passed(false)
                    .validationMessage("Experience check: Education criteria must be passed first to validate experience further.")
                    .build();
        }

        // Fetch and filter work experiences
        List<WorkExperienceEntity> workExperiences = workExperienceRepository
                .findByCandidateIdOrderByFromDateDesc(candidateId);
        List<WorkExperienceEntity> filteredExperiences = filterEligibleExperiences(workExperiences, position);

        // Parse education groups from position
        JsonNode mandatoryEduRulesJson = position.getMandatoryEduRulesJson();
        if (mandatoryEduRulesJson == null || !mandatoryEduRulesJson.has(AppConstants.JOB_ELIGIBILITY_MANDATORY_EDUCATIONS)) {
            // No education rules → experience passes by default
            return JobEligibilityValidationResponse.ExperienceValidation.builder()
                    .passed(true)
                    .build();
        }

        JsonNode mandatoryEducationsNode = mandatoryEduRulesJson.get(AppConstants.JOB_ELIGIBILITY_MANDATORY_EDUCATIONS);
        if (!mandatoryEducationsNode.isObject() || !mandatoryEducationsNode.has(AppConstants.JOB_ELIGIBILITY_VALIDATION_GROUPS)) {
            // Legacy structure — just do flat experience check
            return validateExperienceFlat(candidateId, position, filteredExperiences);
        }

        JsonNode groupsArray = mandatoryEducationsNode.get(AppConstants.JOB_ELIGIBILITY_VALIDATION_GROUPS);
        if (groupsArray == null || !groupsArray.isArray() || groupsArray.size() == 0) {
            return JobEligibilityValidationResponse.ExperienceValidation.builder()
                    .passed(true)
                    .build();
        }

        // ----- Build education maps (same as validateEducationWithGroups) -----
        List<EducationEntity> candidateEducations = educationRepository.findByCandidateId(candidateId);
        if (candidateEducations.isEmpty()) {
            return JobEligibilityValidationResponse.ExperienceValidation.builder()
                    .passed(false)
                    .validationMessage("No education records found for candidate.")
                    .build();
        }

        Set<UUID> qualificationIds = new HashSet<>();
        Set<UUID> specializationIds = new HashSet<>();
        Set<UUID> educationTypeIds = new HashSet<>();

        Set<UUID> groupIds=new HashSet<>();
        candidateEducations.forEach(edu -> {
            qualificationIds.add(edu.getEducationQualificationsId());
            if (edu.getSpecializationId() != null) specializationIds.add(edu.getSpecializationId());
            if (edu.getEducationTypeId() != null) educationTypeIds.add(edu.getEducationTypeId());
        });

        for (JsonNode group : groupsArray) {
            if (group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) {
                for (JsonNode condition : group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) {
                    if (condition.has(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText()))
                        qualificationIds.add(UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText()));
                    if (condition.has(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText()))
                        specializationIds.add(UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText()));
                    String eduTypeKey = condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE
                            : condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID : null;
                    if (eduTypeKey != null && isValidRequirement(condition.get(eduTypeKey).asText()))
                        educationTypeIds.add(UUID.fromString(condition.get(eduTypeKey).asText()));
                    if(condition.has(AppConstants.JOB_ELIGIBILITY_GROUP)&& isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_GROUP ).asText())){
                        groupIds.add(UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_GROUP ).asText()));
                    }
                }
            }
        }
        Map<UUID,String> educationGroupMap=educationGroupsRepository.findAllById(groupIds).stream().collect(Collectors.toMap(
                EducationGroupsEntity::getId, e->e.getGroupName()));



        List<EducationQualificationsEntity> qualificationEntityList =
                educationQualificationsRepository.findAllById(qualificationIds);

        Map<UUID, String> qualificationMap = qualificationEntityList.stream()
                .collect(Collectors.toMap(EducationQualificationsEntity::getId,
                        EducationQualificationsEntity::getQualificationName, (a, b) -> a));

        Map<UUID, String> qualificationCodeMap = qualificationEntityList.stream()
                .collect(Collectors.toMap(EducationQualificationsEntity::getId,
                        q -> q.getQualificationCode() != null ? q.getQualificationCode() : "", (a, b) -> a));

        Set<UUID> levelIds = qualificationEntityList.stream()
                .map(EducationQualificationsEntity::getLevelId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<UUID, DocumentTypesEntity> levelDocTypeMap = documentTypesRepository.findAllById(levelIds)
                .stream().collect(Collectors.toMap(dt -> dt.getId(), dt -> dt, (a, b) -> a));

        Map<UUID, Integer> qualificationLevelScoreMap = qualificationEntityList.stream()
                .filter(q -> q.getLevelId() != null && levelDocTypeMap.containsKey(q.getLevelId()))
                .collect(Collectors.toMap(EducationQualificationsEntity::getId,
                        q -> {
                            Integer s = levelDocTypeMap.get(q.getLevelId()).getScore();
                            return s != null ? s : 0;
                        }, (a, b) -> a));

        Map<UUID, String> qualificationLevelNameMap = qualificationEntityList.stream()
                .filter(q -> q.getLevelId() != null && levelDocTypeMap.containsKey(q.getLevelId()))
                .collect(Collectors.toMap(EducationQualificationsEntity::getId,
                        q -> {
                            String n = levelDocTypeMap.get(q.getLevelId()).getDocumentName();
                            return n != null ? n : "";
                        }, (a, b) -> a));

        boolean candidateHasDiploma = candidateEducations.stream()
                .anyMatch(edu -> AppConstants.DIPLOMA_DOCUMENT_NAME.equalsIgnoreCase(
                        qualificationLevelNameMap.get(edu.getEducationQualificationsId())));

        Map<UUID, String> specializationMap = specializationMasterRepository.findAllById(specializationIds)
                .stream().collect(Collectors.toMap(SpecializationMasterEntity::getId,
                        SpecializationMasterEntity::getSpecializationName, (a, b) -> a));

        Map<UUID, String> educationTypeMap = educationTypeIds.isEmpty() ? Collections.emptyMap()
                : educationTypeMasterRepository.findAllById(educationTypeIds).stream()
                        .collect(Collectors.toMap(EducationTypeMasterEntity::getId,
                                EducationTypeMasterEntity::getEducationType, (a, b) -> a));

        List<String> masterBoardQualCodes = Arrays.asList(AppConstants.MASTER_BOARD_CODE.split(","));

        // ----- Determine required months -----
        boolean isEduWise = Boolean.TRUE.equals(position.getIsMandatoryExpMonthsEduWise());
        Map<UUID, Integer> eduWiseRequirements = new HashMap<>();
        if (isEduWise && position.getMandatoryExpMonthsEduWise() != null) {
            position.getMandatoryExpMonthsEduWise().fields().forEachRemaining(entry -> {
                try {
                    eduWiseRequirements.put(UUID.fromString(entry.getKey()), entry.getValue().asInt());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID in education experience JSON: {}", entry.getKey());
                }
            });
        }
        int flatRequiredMonths = position.getMandatoryExperienceMonths() != null
                ? position.getMandatoryExperienceMonths() : 0;

        Map<UUID, UUID> qualToLevelMap = qualificationEntityList.stream()
                .filter(q -> q.getLevelId() != null)
                .collect(Collectors.toMap(EducationQualificationsEntity::getId,
                        EducationQualificationsEntity::getLevelId, (a, b) -> a));

        // ----- Evaluate each fulfilled education group for post-qual experience -----
        List<String> fulfilledGroupMessages = new ArrayList<>();
        boolean anyGroupPassed = false;
        int fulfilledGroupCount = 0;

        for (int i = 0; i < groupsArray.size(); i++) {
            JsonNode group = groupsArray.get(i);

            LocalDate lastEndDate = findFulfilledGroupLastEndDate(
                    group, candidateEducations, qualificationMap, qualificationCodeMap,
                    qualificationLevelScoreMap, qualificationLevelNameMap, specializationMap,
                    educationTypeMap, masterBoardQualCodes, candidateHasDiploma);

            if (lastEndDate == null) continue; // Group not fulfilled by education
            fulfilledGroupCount++;

            // Calculate post-qualification experience
            ExperienceResult postQualResult = calculatePostQualExperience(
                    filteredExperiences, lastEndDate, position);

            // Determine required months for this group
            int requiredMonths;
            if (isEduWise && !eduWiseRequirements.isEmpty()) {
                requiredMonths = findMinRequiredMonthsForGroup(group, qualToLevelMap, eduWiseRequirements);
                if (requiredMonths < 0) requiredMonths = flatRequiredMonths;
            } else {
                requiredMonths = flatRequiredMonths;
            }

            if (postQualResult.getTotalCompletedMonths() >= requiredMonths) {
                anyGroupPassed = true;
                log.info("Experience passed for group {} — postQualMonths: {}, requiredMonths: {}",
                        i + 1, postQualResult.getTotalCompletedMonths(), requiredMonths);
                break;
            }

            // Build failure message for this group
            StringBuilder groupMsg = new StringBuilder();
            // List each condition as a separate line using mandatory education format
            if (group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) {
                JsonNode conditions = group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS);
                for (JsonNode condition : conditions) {
                    UUID qualId = condition.has(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText())
                            ? UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText()) : null;
                    UUID specId = condition.has(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText())
                            ? UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText()) : null;
                    String eduTypeKey = condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE
                            : condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID : null;
                    UUID eduTypeId = eduTypeKey != null && isValidRequirement(condition.get(eduTypeKey).asText())
                            ? UUID.fromString(condition.get(eduTypeKey).asText()) : null;

                    UUID groupId=condition.has(AppConstants.JOB_ELIGIBILITY_GROUP)&& isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_GROUP ).asText())
                            ? UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_GROUP ).asText()) : null;

                    String qualName = qualId != null ? qualificationMap.get(qualId) : "Any";
                    String specName = specId != null ? specializationMap.get(specId) : null;
                    String eduTypeName = eduTypeId != null ? educationTypeMap.get(eduTypeId) : null;

                    String duration = condition.has(AppConstants.JOB_ELIGIBILITY_DURATION) ? condition.get(AppConstants.JOB_ELIGIBILITY_DURATION).asText() : null;
                    String gpa = condition.has("gpa") ? condition.get("gpa").asText() : null;
                    String percentage = condition.has(AppConstants.JOB_ELIGIBILITY_PERCENTAGE) ? condition.get(AppConstants.JOB_ELIGIBILITY_PERCENTAGE).asText() : null;

                    groupMsg.append(formatMandatoryEducation(qualName, specName,educationGroupMap.get(groupId), eduTypeName, duration, gpa, percentage));
                    groupMsg.append("\n");
                }
            }

            ExperienceResult requiredResult = convertMonthsToExperience(requiredMonths);
            groupMsg.append("\nRequired post-qualification experience: ").append(requiredResult.formatYearsMonths());
            groupMsg.append("\nCurrent post-qualification experience: ").append(postQualResult.formatFull());

            fulfilledGroupMessages.add(groupMsg.toString());
        }

        if (anyGroupPassed) {
            return JobEligibilityValidationResponse.ExperienceValidation.builder()
                    .passed(true)
                    .build();
        }

        if (fulfilledGroupMessages.isEmpty()) {
            // No fulfilled groups found (shouldn't happen since education passed, but guard)
            return JobEligibilityValidationResponse.ExperienceValidation.builder()
                    .passed(false)
                    .validationMessage("No fulfilled education group found for experience evaluation.")
                    .build();
        }

        // Build detailed failure message
        StringBuilder message = new StringBuilder("Any one education group of candidate is required to have related post-qualification experience.");
        boolean singleGroup = fulfilledGroupMessages.size() == 1;
        for (int i = 0; i < fulfilledGroupMessages.size(); i++) {
            if (singleGroup) {
                message.append("\n\nEducation group fulfilled by candidate:\n");
            } else {
                message.append("\n\nEducation group ").append(i + 1).append(" fulfilled by candidate:\n");
            }
            message.append(fulfilledGroupMessages.get(i));
        }

        log.info("Experience validation failed for candidateId: {} — {} fulfilled groups checked", candidateId, fulfilledGroupCount);

        return JobEligibilityValidationResponse.ExperienceValidation.builder()
                .passed(false)
                .validationMessage(message.toString())
                .build();
    }

    /**
     * Flat experience validation for legacy (non-group-based) positions.
     * Simple total-experience >= required-months check.
     */
    private JobEligibilityValidationResponse.ExperienceValidation validateExperienceFlat(
            UUID candidateId,
            JobPositionsEntity position,
            List<WorkExperienceEntity> filteredExperiences) {

        ExperienceResult candidateExperience = calculateTotalExperience(filteredExperiences, position);
        int requiredMonths = position.getMandatoryExperienceMonths() != null
                ? position.getMandatoryExperienceMonths() : 0;

        boolean passed = candidateExperience.getTotalCompletedMonths() >= requiredMonths;

        if (passed) {
            return JobEligibilityValidationResponse.ExperienceValidation.builder()
                    .passed(true)
                    .build();
        }

        ExperienceResult requiredResult = convertMonthsToExperience(requiredMonths);
        String message = "Required experience: " + requiredResult.formatYearsMonths()
                + ", Candidate's experience: " + candidateExperience.formatFull();

        return JobEligibilityValidationResponse.ExperienceValidation.builder()
                .passed(false)
                .validationMessage(message)
                .build();
    }

    /**
     * Validates candidate's submitted documents against required documents
     *
     * @param candidateId The UUID of the candidate
     * @return DocumentValidation result
     */
    private JobEligibilityValidationResponse.DocumentValidation validateDocuments(UUID candidateId) {
        log.debug("Validating documents for candidateId: {}", candidateId);

        // Get all required documents (is_required = true)
        List<DocumentTypesEntity> requiredDocumentTypes = documentTypesRepository.findByIsRequiredTrue();
        
        // Get all documents submitted by the candidate
        List<CandidateDocumentStoreEntity> candidateDocuments = 
                candidateDocumentStoreRepository.findAllByCandidateId(candidateId);

        // Extract document IDs that the candidate has submitted
        Set<UUID> submittedDocumentIds = candidateDocuments.stream()
                .map(CandidateDocumentStoreEntity::getDocumentId)
                .collect(Collectors.toSet());

        // Extract required document IDs
        Set<UUID> requiredDocumentIds = requiredDocumentTypes.stream()
                .map(DocumentTypesEntity::getId)
                .collect(Collectors.toSet());

        // Get document names for response
        List<String> requiredDocumentNames = requiredDocumentTypes.stream()
                .map(DocumentTypesEntity::getDocumentName)
                .sorted()
                .collect(Collectors.toList());

        // Get submitted document names (only those that are in required list)
        // Group by document name and add numbering for duplicates
        List<String> submittedDocumentNames = candidateDocuments.stream()
                .filter(doc -> requiredDocumentIds.contains(doc.getDocumentId()))
                .map(doc -> {
                    // Find the document type to get the name
                    return requiredDocumentTypes.stream()
                            .filter(type -> type.getId().equals(doc.getDocumentId()))
                            .map(DocumentTypesEntity::getDocumentName)
                            .findFirst()
                            .orElse("Unknown Document");
                })
                .sorted()
                .collect(Collectors.toList());

        // Add numbering for duplicate document names
        List<String> numberedDocumentNames = new java.util.ArrayList<>();
        java.util.Map<String, Integer> documentCountMap = new java.util.HashMap<>();
        
        for (String docName : submittedDocumentNames) {
            int count = documentCountMap.getOrDefault(docName, 0) + 1;
            documentCountMap.put(docName, count);
            
            // Add number only if there are duplicates
            long totalOccurrences = submittedDocumentNames.stream()
                    .filter(name -> name.equals(docName))
                    .count();
            
            if (totalOccurrences > 1) {
                numberedDocumentNames.add(docName + " " + count);
            } else {
                numberedDocumentNames.add(docName);
            }
        }

        // Check if candidate has all required documents
        boolean passed = submittedDocumentIds.containsAll(requiredDocumentIds);

        log.info("Document validation result - Required: {}, Submitted: {}, Passed: {}", 
                requiredDocumentNames.size(), numberedDocumentNames.size(), passed);

        return JobEligibilityValidationResponse.DocumentValidation.builder()
                .passed(passed)
                .requiredDocuments(requiredDocumentNames)
                .submittedDocuments(numberedDocumentNames)
                .build();
    }

    /**
     * Validates candidate's education against mandatory education requirements
     *
     * @param candidateId The UUID of the candidate
     * @param position    The job position entity
     * @return EducationValidation result
     */
    private JobEligibilityValidationResponse.EducationValidation validateEducation(
            UUID candidateId,
            JobPositionsEntity position) {

        log.debug("Validating education for candidateId: {}", candidateId);

        // Get mandatory education rules from JSON
        JsonNode mandatoryEduRulesJson = position.getMandatoryEduRulesJson();
        if (mandatoryEduRulesJson == null || !mandatoryEduRulesJson.has(AppConstants.JOB_ELIGIBILITY_MANDATORY_EDUCATIONS)) {
            log.warn("No mandatory education rules defined for position: {}", position.getId());
            return JobEligibilityValidationResponse.EducationValidation.builder()
                    .passed(true)
                    .educationPassed(true)
                    .certificationPassed(true)
                    .intermediatePassed(true)
                    .candidateEducation(new ArrayList<>())
                    .mandatoryEducation(new ArrayList<>())
                    .candidateCertifications(new ArrayList<>())
                    .mandatoryCertifications(new ArrayList<>())
                    .requiredEducation(new ArrayList<>())
                    .build();
        }

        JsonNode mandatoryEducationsNode = mandatoryEduRulesJson.get(AppConstants.JOB_ELIGIBILITY_MANDATORY_EDUCATIONS);
        
        // Check if it's the new group-based structure
        boolean isGroupBased = mandatoryEducationsNode.isObject() && mandatoryEducationsNode.has(AppConstants.JOB_ELIGIBILITY_VALIDATION_GROUPS);
        
        if (isGroupBased) {
            JsonNode groupsArray = mandatoryEducationsNode.get(AppConstants.JOB_ELIGIBILITY_VALIDATION_GROUPS);
            if (groupsArray == null || !groupsArray.isArray() || groupsArray.size() == 0) {
                log.warn("Mandatory educations groups array is empty for position: {}", position.getId());
                return buildEmptyPassResponse();
            }
            return validateEducationWithGroups(candidateId, mandatoryEduRulesJson, groupsArray, position.getIsIntermediateRequired());
        } else {
            // Legacy array-based structure (backward compatibility)
            if (!mandatoryEducationsNode.isArray() || mandatoryEducationsNode.size() == 0) {
                log.warn("Mandatory educations array is empty for position: {}", position.getId());
                return buildEmptyPassResponse();
            }
            return validateEducationLegacy(candidateId, mandatoryEduRulesJson, mandatoryEducationsNode, position.getIsIntermediateRequired());
        }
    }
    
    /**
     * Builds empty pass response for education validation
     */
    private JobEligibilityValidationResponse.EducationValidation buildEmptyPassResponse() {
        return JobEligibilityValidationResponse.EducationValidation.builder()
                .passed(true)
                .educationPassed(true)
                .certificationPassed(true)
                .intermediatePassed(true)
                .candidateEducation(new ArrayList<>())
                .mandatoryEducation(new ArrayList<>())
                .candidateCertifications(new ArrayList<>())
                .mandatoryCertifications(new ArrayList<>())
                .requiredEducation(new ArrayList<>())
                .build();
    }
    
    /**
     * Validates education using new group-based structure
     */
    private JobEligibilityValidationResponse.EducationValidation validateEducationWithGroups(
            UUID candidateId,
            JsonNode mandatoryEduRulesJson,
            JsonNode groupsArray,
            Boolean isIntermediateRequired) {
        
        // Get candidate's education records
        List<EducationEntity> candidateEducations = educationRepository.findByCandidateId(candidateId);
        Boolean hasIntermediateOrDiploma = false;

        if (isIntermediateRequired) {

            List<DocumentTypesEntity> documentTypes = documentTypesRepository
                    .findByDocCodeIn(List.of("INTER", "DIPLOMA"));

            List<UUID> levelIds = documentTypes.stream()
                    .map(DocumentTypesEntity::getId)
                    .toList();

            List<EducationQualificationsEntity> qualifications =
                    educationQualificationsRepository.findAllByLevelIdIn(levelIds);

            Set<UUID> qualificationIds = qualifications.stream()
                    .map(EducationQualificationsEntity::getId)
                    .collect(Collectors.toSet());

            hasIntermediateOrDiploma = candidateEducations.stream()
                    .map(EducationEntity::getEducationQualificationsId)
                    .anyMatch(qualificationIds::contains);


        }

        // Collect all qualification, specialization, and education type IDs for bulk fetching
        Set<UUID> qualificationIds = new HashSet<>();
        Set<UUID> specializationIds = new HashSet<>();
        Set<UUID> educationTypeIds = new HashSet<>();
        Set<UUID> groupIds = new HashSet<>();

        // From candidate educations
        candidateEducations.forEach(edu -> {
            qualificationIds.add(edu.getEducationQualificationsId());
            if (edu.getSpecializationId() != null) specializationIds.add(edu.getSpecializationId());
            if (edu.getEducationTypeId() != null) educationTypeIds.add(edu.getEducationTypeId());
        });

        // From job requirements (all groups and conditions)
        for (JsonNode group : groupsArray) {
            if (group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) {
                JsonNode conditions = group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS);
                for (JsonNode condition : conditions) {
                    if (condition.has(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText())) {
                        qualificationIds.add(UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText()));
                    }
                    if (condition.has(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText())) {
                        specializationIds.add(UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText()));
                    }
                    String eduTypeKey = condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE
                            : condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID : null;
                    if (condition.has(AppConstants.JOB_ELIGIBILITY_GROUP ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_GROUP ).asText())) {
                        groupIds.add(UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_GROUP ).asText()));
                    }
                    if (eduTypeKey != null && isValidRequirement(condition.get(eduTypeKey).asText())) {
                        educationTypeIds.add(UUID.fromString(condition.get(eduTypeKey).asText()));
                    }
                }
            }
        }

        Map<UUID,String> groupMap=educationGroupsRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(EducationGroupsEntity::getId, EducationGroupsEntity::getGroupName));

        // Bulk fetch qualification entities (need code + levelId for score/name mapping)
        List<EducationQualificationsEntity> qualificationEntityList =
                educationQualificationsRepository.findAllById(qualificationIds);

        Map<UUID, String> qualificationMap = qualificationEntityList.stream()
                .collect(Collectors.toMap(
                        EducationQualificationsEntity::getId,
                        EducationQualificationsEntity::getQualificationName,
                        (a, b) -> a
                ));

        Map<UUID, String> qualificationCodeMap = qualificationEntityList.stream()
                .collect(Collectors.toMap(
                        EducationQualificationsEntity::getId,
                        q -> q.getQualificationCode() != null ? q.getQualificationCode() : "",
                        (a, b) -> a
                ));

        // Build level score + name maps via document_types
        Set<UUID> levelIds = qualificationEntityList.stream()
                .map(EducationQualificationsEntity::getLevelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, DocumentTypesEntity> levelDocTypeMap = documentTypesRepository.findAllById(levelIds)
                .stream()
                .collect(Collectors.toMap(dt -> dt.getId(), dt -> dt, (a, b) -> a));

        // qualId → level score (0 if score column not yet populated)
        Map<UUID, Integer> qualificationLevelScoreMap = qualificationEntityList.stream()
                .filter(q -> q.getLevelId() != null && levelDocTypeMap.containsKey(q.getLevelId()))
                .collect(Collectors.toMap(
                        EducationQualificationsEntity::getId,
                        q -> {
                            Integer s = levelDocTypeMap.get(q.getLevelId()).getScore();
                            return s != null ? s : 0;
                        },
                        (a, b) -> a
                ));

        // qualId → level document name (e.g. "Graduation", "Post-Graduation")
        Map<UUID, String> qualificationLevelNameMap = qualificationEntityList.stream()
                .filter(q -> q.getLevelId() != null && levelDocTypeMap.containsKey(q.getLevelId()))
                .collect(Collectors.toMap(
                        EducationQualificationsEntity::getId,
                        q -> {
                            String n = levelDocTypeMap.get(q.getLevelId()).getDocumentName();
                            return n != null ? n : "";
                        },
                        (a, b) -> a
                ));

        // Diploma check using the level name map (avoids a separate DB round-trip)
        boolean candidateHasDiploma = candidateEducations.stream()
                .anyMatch(edu -> AppConstants.DIPLOMA_DOCUMENT_NAME.equalsIgnoreCase(
                        qualificationLevelNameMap.get(edu.getEducationQualificationsId())));

        Map<UUID, String> specializationMap = specializationMasterRepository.findAllById(specializationIds)
                .stream()
                .collect(Collectors.toMap(
                        SpecializationMasterEntity::getId,
                        SpecializationMasterEntity::getSpecializationName,
                        (a, b) -> a
                ));

        // Education type name map — used for Full Time pass-through check
        Map<UUID, String> educationTypeMap = educationTypeIds.isEmpty()
                ? Collections.emptyMap()
                : educationTypeMasterRepository.findAllById(educationTypeIds)
                        .stream()
                        .collect(Collectors.toMap(
                                EducationTypeMasterEntity::getId,
                                EducationTypeMasterEntity::getEducationType,
                                (a, b) -> a
                        ));
        
        // Build candidate education list with details
        List<String> candidateEducationList = candidateEducations.stream()
                .map(edu -> formatCandidateEducation(
                        qualificationMap.get(edu.getEducationQualificationsId()),
                        edu.getSpecializationId() != null ? specializationMap.get(edu.getSpecializationId()) : null,
                        edu.getEducationTypeId() != null ? educationTypeMap.get(edu.getEducationTypeId()) : null,
                        edu.getStartDate(),
                        edu.getEndDate(),
                        edu.getPercentage()
                ))
                .collect(Collectors.toList());
        
        // Build mandatory education list from groups
        List<String> mandatoryEducationList = new ArrayList<>();
        for (int i = 0; i < groupsArray.size(); i++) {
            JsonNode group = groupsArray.get(i);
            if (group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) {
                JsonNode conditions = group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS);
                for (JsonNode condition : conditions) {
                    UUID qualId = condition.has(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText())
                            ? UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText())
                            : null;
                    UUID specId = condition.has(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText())
                            ? UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText())
                            : null;

                    UUID groupId=condition.has(AppConstants.JOB_ELIGIBILITY_GROUP ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_GROUP ).asText())
                            ? UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_GROUP ).asText())
                            : null;
                    
                    String qualName = qualId != null ? qualificationMap.get(qualId) : null;
                    String specName = specId != null ? specializationMap.get(specId) : null;
                    
                    String duration = condition.has(AppConstants.JOB_ELIGIBILITY_DURATION) ? condition.get(AppConstants.JOB_ELIGIBILITY_DURATION).asText() : null;
                    String gpa = condition.has("gpa") ? condition.get("gpa").asText() : null;
                    String percentage = condition.has(AppConstants.JOB_ELIGIBILITY_PERCENTAGE) ? condition.get(AppConstants.JOB_ELIGIBILITY_PERCENTAGE).asText() : null;
                    
                    // Resolve education type name from condition
                    String eduTypeKey = condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE
                            : condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID : null;
                    UUID eduTypeId = eduTypeKey != null && isValidRequirement(condition.get(eduTypeKey).asText())
                            ? UUID.fromString(condition.get(eduTypeKey).asText()) : null;
                    String eduTypeName = eduTypeId != null ? educationTypeMap.get(eduTypeId) : null;
                    
                    mandatoryEducationList.add(formatMandatoryEducation(qualName, specName,groupMap.get(groupId), eduTypeName, duration, gpa, percentage));
                }
            }
            
            // Add (OR) separator between groups (not after last group)
            if (i < groupsArray.size() - 1) {
                mandatoryEducationList.add("(OR)");
            }
        }
        
        // Get master board codes
        List<String> masterBoardQualCodes = Arrays.asList(AppConstants.MASTER_BOARD_CODE.split(","));
        
        // Validate education groups (OR logic between groups)
        boolean educationPassed = false;
        for (JsonNode group : groupsArray) {
            if (validateEducationGroup(group, candidateEducations, qualificationMap, qualificationCodeMap,
                    qualificationLevelScoreMap, qualificationLevelNameMap, specializationMap,
                    educationTypeMap, masterBoardQualCodes, candidateHasDiploma)) {
                educationPassed = true;
                break; // One group passed, that's enough (OR logic)
            }
        }
        
        // Validate certifications with new group structure
        List<String> candidateCertificationsList = new ArrayList<>();
        List<String> mandatoryCertificationsList = new ArrayList<>();
        boolean certificationsPassed = validateCertificationsWithGroups(
                candidateId,
                mandatoryEduRulesJson,
                candidateCertificationsList,
                mandatoryCertificationsList);

        boolean intermediatePassed = !Boolean.TRUE.equals(isIntermediateRequired) || hasIntermediateOrDiploma;
        // Final result
        boolean finalPassed = educationPassed && certificationsPassed && intermediatePassed;
        
        log.info("Education validation result (Group-based) - Education Passed: {}, Certification Passed: {}, Final Passed: {}",
                educationPassed, certificationsPassed, finalPassed);
        
        return JobEligibilityValidationResponse.EducationValidation.builder()
                .passed(finalPassed)
                .educationPassed(educationPassed && intermediatePassed)
                .certificationPassed(certificationsPassed)
                .intermediatePassed(intermediatePassed)
                .candidateEducation(candidateEducationList)
                .mandatoryEducation(mandatoryEducationList)
                .candidateCertifications(candidateCertificationsList)
                .mandatoryCertifications(mandatoryCertificationsList)
                .requiredEducation(intermediatePassed ? null :List.of(AppConstants.HAS_INTERMEDIATE_OR_DIPLOMA))
                .build();
    }
    
    /**
     * Validates certifications using new group-based structure
     */
    private boolean validateCertificationsWithGroups(
            UUID candidateId,
            JsonNode mandatoryEduRulesJson,
            List<String> candidateCertificationsList,
            List<String> mandatoryCertificationsList) {
        
        // Get candidate's certifications
        List<CandidateCertificationsEntity> candidateCertifications = 
                candidateCertificationsRepository.findByCandidateId(candidateId);
        
        Set<UUID> candidateCertificationIds = candidateCertifications.stream()
                .map(CandidateCertificationsEntity::getCertificationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // Fetch certification names for candidate's certifications
        if (!candidateCertificationIds.isEmpty()) {
            List<CertificationMasterEntity> candidateCertMasters = 
                    certificationMasterRepository.findAllById(candidateCertificationIds);
            candidateCertMasters.forEach(cert -> 
                    candidateCertificationsList.add(cert.getCertificationName()));
        }
        
        // Check if using new group structure
        if (!mandatoryEduRulesJson.has("mandatoryCertifications")) {
            // Try legacy structure
            return validateCertificationsLegacy(mandatoryEduRulesJson, candidateCertificationIds, mandatoryCertificationsList);
        }
        
        JsonNode mandatoryCertificationsNode = mandatoryEduRulesJson.get("mandatoryCertifications");
        
        // Check if it's group-based
        if (!mandatoryCertificationsNode.isObject() || !mandatoryCertificationsNode.has(AppConstants.JOB_ELIGIBILITY_VALIDATION_GROUPS)) {
            return true; // No requirements
        }
        
        JsonNode groupsArray = mandatoryCertificationsNode.get(AppConstants.JOB_ELIGIBILITY_VALIDATION_GROUPS);
        if (groupsArray == null || !groupsArray.isArray() || groupsArray.size() == 0) {
            return true; // Empty groups = pass
        }
        
        // Collect all certification IDs from all groups
        Set<UUID> allRequiredCertIds = new HashSet<>();
        for (JsonNode group : groupsArray) {
            if (group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) {
                JsonNode conditions = group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS);
                for (JsonNode certId : conditions) {
                    if (!certId.isNull()) {
                        allRequiredCertIds.add(UUID.fromString(certId.asText()));
                    }
                }
            }
        }
        
        // Fetch certification names for display
        if (!allRequiredCertIds.isEmpty()) {
            List<CertificationMasterEntity> mandatoryCertMasters = 
                    certificationMasterRepository.findAllById(allRequiredCertIds);
            Map<UUID, String> certNameMap = mandatoryCertMasters.stream()
                    .collect(Collectors.toMap(
                            CertificationMasterEntity::getId,
                            CertificationMasterEntity::getCertificationName
                    ));
            
            // Build display list with (OR) separators
            for (int i = 0; i < groupsArray.size(); i++) {
                JsonNode group = groupsArray.get(i);
                if (group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) {
                    JsonNode conditions = group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS);
                    for (JsonNode certIdNode : conditions) {
                        if (!certIdNode.isNull()) {
                            UUID certId = UUID.fromString(certIdNode.asText());
                            String certName = certNameMap.get(certId);
                            if (certName != null) {
                                mandatoryCertificationsList.add(certName);
                            }
                        }
                    }
                }
                
                // Add (OR) separator between groups
                if (i < groupsArray.size() - 1) {
                    mandatoryCertificationsList.add("(OR)");
                }
            }
        }
        
        // Validate groups (OR logic between groups)
        for (JsonNode group : groupsArray) {
            if (!group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) {
                continue;
            }
            
            JsonNode conditions = group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS);
            if (!conditions.isArray()) {
                continue;
            }
            
            // All certifications in group must be present (AND logic)
            boolean groupPassed = true;
            for (JsonNode certIdNode : conditions) {
                if (certIdNode.isNull()) {
                    continue;
                }
                UUID requiredCertId = UUID.fromString(certIdNode.asText());
                if (!candidateCertificationIds.contains(requiredCertId)) {
                    groupPassed = false;
                    break;
                }
            }
            
            if (groupPassed) {
                return true; // One complete group matched
            }
        }
        
        return false; // No group fully matched
    }
    
    /**
     * Legacy certification validation for backward compatibility
     */
    private boolean validateCertificationsLegacy(
            JsonNode mandatoryEduRulesJson,
            Set<UUID> candidateCertificationIds,
            List<String> mandatoryCertificationsList) {
        
        Set<UUID> mandatoryCertificationIds = new HashSet<>();
        if (mandatoryEduRulesJson.has(AppConstants.JOB_ELIGIBILITY_MANDATORY_CERTIFICATION_IDS)) {
            JsonNode mandatoryCertificationsArray = mandatoryEduRulesJson.get(AppConstants.JOB_ELIGIBILITY_MANDATORY_CERTIFICATION_IDS);
            if (mandatoryCertificationsArray.isArray()) {
                for (JsonNode certId : mandatoryCertificationsArray) {
                    if (!certId.isNull()) {
                        mandatoryCertificationIds.add(UUID.fromString(certId.asText()));
                    }
                }
            }
        }
        
        if (mandatoryCertificationIds.isEmpty()) {
            return true; // No requirements
        }
        
        // Fetch certification names
        List<CertificationMasterEntity> mandatoryCertMasters = 
                certificationMasterRepository.findAllById(mandatoryCertificationIds);
        mandatoryCertMasters.forEach(cert -> 
                mandatoryCertificationsList.add(cert.getCertificationName()));
        
        // Check if candidate has at least one
        return candidateCertificationIds.stream()
                .anyMatch(mandatoryCertificationIds::contains);
    }
    
    /**
     * Legacy validation for backward compatibility with old structure
     */
    private JobEligibilityValidationResponse.EducationValidation validateEducationLegacy(
            UUID candidateId,
            JsonNode mandatoryEduRulesJson,
            JsonNode mandatoryEducationsArray,
            Boolean isIntermediateRequired) {
        // Get candidate's education records
        List<EducationEntity> candidateEducations = educationRepository.findByCandidateId(candidateId);

        boolean hasIntermediateOrDiploma = false;

        if (isIntermediateRequired) {

            List<DocumentTypesEntity> documentTypes = documentTypesRepository
                    .findByDocCodeIn(List.of("INTER", "DIPLOMA"));

            List<UUID> levelIds = documentTypes.stream()
                    .map(DocumentTypesEntity::getId)
                    .toList();

            List<EducationQualificationsEntity> qualifications =
                    educationQualificationsRepository.findAllByLevelIdIn(levelIds);

            Set<UUID> qualificationIds = qualifications.stream()
                    .map(EducationQualificationsEntity::getId)
                    .collect(Collectors.toSet());

            hasIntermediateOrDiploma = candidateEducations.stream()
                    .map(EducationEntity::getEducationQualificationsId)
                    .anyMatch(qualificationIds::contains);
        }

        // Fetch qualification and specialization data in bulk for efficiency
        Set<UUID> qualificationIds = new HashSet<>();
        Set<UUID> specializationIds = new HashSet<>();

        // Collect IDs from candidate educations
        candidateEducations.forEach(edu -> {
            qualificationIds.add(edu.getEducationQualificationsId());
            if (edu.getSpecializationId() != null) {
                specializationIds.add(edu.getSpecializationId());
            }
        });

        // Collect IDs from mandatory educations
        for (JsonNode mandatoryEdu : mandatoryEducationsArray) {
            if (mandatoryEdu.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_QUALIFICATIONS_ID)) {
                qualificationIds.add(UUID.fromString(mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_EDUCATION_QUALIFICATIONS_ID).asText()));
            }
            if (mandatoryEdu.has(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION_ID) && !mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION_ID).isNull()) {
                specializationIds.add(UUID.fromString(mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION_ID).asText()));
            }
        }

        // Fetch all qualifications and specializations in one query each
        Map<UUID, String> qualificationMap = educationQualificationsRepository.findAllById(qualificationIds)
                .stream()
                .collect(Collectors.toMap(
                        EducationQualificationsEntity::getId,
                        EducationQualificationsEntity::getQualificationName
                ));

        Map<UUID, String> specializationMap = specializationMasterRepository.findAllById(specializationIds)
                .stream()
                .collect(Collectors.toMap(
                        SpecializationMasterEntity::getId,
                        SpecializationMasterEntity::getSpecializationName
                ));

        // Build candidate education list
        List<String> candidateEducationList = candidateEducations.stream()
                .map(edu -> formatEducation(
                        qualificationMap.get(edu.getEducationQualificationsId()),
                        edu.getSpecializationId() != null ? specializationMap.get(edu.getSpecializationId()) : null
                ))
                .collect(Collectors.toList());

        // Build mandatory education list
        List<String> mandatoryEducationList = new ArrayList<>();
        for (JsonNode mandatoryEdu : mandatoryEducationsArray) {
            UUID qualificationId = mandatoryEdu.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_QUALIFICATIONS_ID)
                    ? UUID.fromString(mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_EDUCATION_QUALIFICATIONS_ID).asText())
                    : null;

            UUID specializationId = (mandatoryEdu.has(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION_ID) && !mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION_ID).isNull())
                    ? UUID.fromString(mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION_ID).asText())
                    : null;

            String qualificationName = qualificationId != null ? qualificationMap.get(qualificationId) : null;
            String specializationName = specializationId != null ? specializationMap.get(specializationId) : null;

            mandatoryEducationList.add(formatEducation(qualificationName, specializationName));
        }
        
        List<String> qualCodes = Arrays.asList(AppConstants.MASTER_BOARD_CODE.split(","));
        List<UUID> eduQualIds = educationQualificationsRepository.findByQualificationCodeIn(qualCodes)
                .stream()
                .map(EducationQualificationsEntity::getId)
                .toList();
        
        // Extract mandatory certification IDs from JSON (legacy structure)
        Set<UUID> mandatoryCertificationIds = new HashSet<>();
        if (mandatoryEduRulesJson.has(AppConstants.JOB_ELIGIBILITY_MANDATORY_CERTIFICATION_IDS)) {
            JsonNode mandatoryCertificationsArray = mandatoryEduRulesJson.get(AppConstants.JOB_ELIGIBILITY_MANDATORY_CERTIFICATION_IDS);
            if (mandatoryCertificationsArray.isArray()) {
                for (JsonNode certId : mandatoryCertificationsArray) {
                    if (!certId.isNull()) {
                        mandatoryCertificationIds.add(UUID.fromString(certId.asText()));
                    }
                }
            }
        }
        
        // Validate certifications
        List<String> candidateCertificationsList = new ArrayList<>();
        List<String> mandatoryCertificationsList = new ArrayList<>();
        boolean certificationsPassed = validateCertifications(
                candidateId, 
                mandatoryCertificationIds, 
                candidateCertificationsList, 
                mandatoryCertificationsList);
        
        // Check if "Any Graduation" or "Any Post-Graduation" is in mandatory educations
        boolean hasAnyGraduation = false;
        boolean hasAnyPostGraduation = false;
        boolean hasAnyGraduationWithoutGroup = false;
        boolean hasAnyPostGraduationWithoutGroup = false;
        UUID gradGrpId=null;
        UUID postGradGrpId=null;
        for (JsonNode mandatoryEdu : mandatoryEducationsArray) {

            UUID reqQualificationId = mandatoryEdu.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_QUALIFICATIONS_ID)
                    ? UUID.fromString(mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_EDUCATION_QUALIFICATIONS_ID).asText())
                    : null;

            if (reqQualificationId == null) {
                continue;
            }

            String qualName = qualificationMap.get(reqQualificationId);

            UUID groupId = mandatoryEdu.has("group")
                    && isValidRequirement(mandatoryEdu.get("group").asText())
                    ? UUID.fromString(mandatoryEdu.get("group").asText())
                    : null;

            if (AppConstants.ANY_GRADUATION_QUALIFICATION_NAME.equals(qualName)) {
                hasAnyGraduation = true;

                if (groupId == null) {
                    hasAnyGraduationWithoutGroup = true;
                    gradGrpId = null;
                } else if (!hasAnyGraduationWithoutGroup) {
                    gradGrpId = groupId;
                }
            }

            if (AppConstants.ANY_POST_GRADUATION_QUALIFICATION_NAME.equals(qualName)) {
                hasAnyPostGraduation = true;

                if (groupId == null) {
                    hasAnyPostGraduationWithoutGroup = true;
                    postGradGrpId = null;
                } else if (!hasAnyPostGraduationWithoutGroup) {
                    postGradGrpId = groupId;
                }
            }
        }
        
        // If "Any Graduation" is required, check if candidate has any graduation-level education
        if (hasAnyGraduation) {
//            boolean educationPassed = checkCandidateHasGraduationLevel(candidateEducations,gradGrpId);
            boolean educationPassed=checkEducationLevelCheck(candidateEducations, gradGrpId, "POST_GRAD");
            boolean intermediatePassed = !Boolean.TRUE.equals(isIntermediateRequired) || hasIntermediateOrDiploma;
            boolean finalPassed = educationPassed && certificationsPassed && intermediatePassed;
            log.info("Education validation result (Any Graduation) - Candidate has {} educations, Education Passed: {}, Certification Passed: {}, Final Passed: {}",
                    candidateEducationList.size(), educationPassed, certificationsPassed, finalPassed);
            return JobEligibilityValidationResponse.EducationValidation.builder()
                    .passed(finalPassed)
                    .educationPassed(educationPassed)
                    .certificationPassed(certificationsPassed)
                    .intermediatePassed(intermediatePassed)
                    .candidateEducation(candidateEducationList)
                    .mandatoryEducation(mandatoryEducationList)
                    .candidateCertifications(candidateCertificationsList)
                    .mandatoryCertifications(mandatoryCertificationsList)
                    .requiredEducation(intermediatePassed ? null :List.of(AppConstants.HAS_INTERMEDIATE_OR_DIPLOMA))
                    .build();
        }
        
        // If "Any Post-Graduation" is required, check if candidate has any post-graduation-level education
        if (hasAnyPostGraduation) {
//            boolean educationPassed = checkCandidateHasPostGraduationLevel(candidateEducations,postGradGrpId);
            boolean educationPassed=checkEducationLevelCheck(candidateEducations, postGradGrpId, "POST_GRAD");
            boolean intermediatePassed = !Boolean.TRUE.equals(isIntermediateRequired) || hasIntermediateOrDiploma;
            boolean finalPassed = educationPassed && certificationsPassed && intermediatePassed;
            log.info("Education validation result (Any Post-Graduation) - Candidate has {} educations, Education Passed: {}, Certification Passed: {}, Final Passed: {}",
                    candidateEducationList.size(), educationPassed, certificationsPassed, finalPassed);
            return JobEligibilityValidationResponse.EducationValidation.builder()
                    .passed(finalPassed)
                    .educationPassed(educationPassed)
                    .certificationPassed(certificationsPassed)
                    .intermediatePassed(intermediatePassed)
                    .candidateEducation(candidateEducationList)
                    .mandatoryEducation(mandatoryEducationList)
                    .candidateCertifications(candidateCertificationsList)
                    .mandatoryCertifications(mandatoryCertificationsList)
                    .requiredEducation(intermediatePassed ? null :List.of(AppConstants.HAS_INTERMEDIATE_OR_DIPLOMA))
                    .build();
        }
        
        // Regular validation: Check if candidate matches at least one mandatory education
        boolean educationPassed = false;
        for (JsonNode mandatoryEdu : mandatoryEducationsArray) {
            UUID reqQualificationId = mandatoryEdu.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_QUALIFICATIONS_ID)
                    ? UUID.fromString(mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_EDUCATION_QUALIFICATIONS_ID).asText())
                    : null;

            UUID reqSpecializationId = (mandatoryEdu.has(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION_ID) && !mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION_ID).isNull())
                    ? UUID.fromString(mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION_ID).asText())
                    : null;

            UUID reqEducationTypeId = mandatoryEdu.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID)
                    ? UUID.fromString(mandatoryEdu.get(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID).asText())
                    : null;

            // Check if any candidate education matches this requirement
            // For each field: if null in requirement, any value from candidate is acceptable
            boolean matchFound = candidateEducations.stream().anyMatch(candEdu -> {
                // Auto-pass for master board qualifications
                if (eduQualIds.contains(reqQualificationId)) {
                    return true;
                }
                
                boolean qualificationMatch = reqQualificationId == null || 
                        reqQualificationId.equals(candEdu.getEducationQualificationsId());
                
                boolean educationTypeMatch = reqEducationTypeId == null || 
                        reqEducationTypeId.equals(candEdu.getEducationTypeId());

                boolean specializationMatch = reqSpecializationId == null || 
                        reqSpecializationId.equals(candEdu.getSpecializationId());

                return qualificationMatch && educationTypeMatch && specializationMatch;
            });

            if (matchFound) {
                educationPassed = true;
                break;
            }
        }

        boolean intermediatePassed = !Boolean.TRUE.equals(isIntermediateRequired) || hasIntermediateOrDiploma;
        // Final pass status must satisfy both education AND certification requirements
        boolean finalPassed = educationPassed && certificationsPassed && intermediatePassed;

        log.info("Education validation result (Legacy) - Candidate has {} educations, Required: {} options, Education Passed: {}, Certification Passed: {}, Final Passed: {}",
                candidateEducationList.size(), mandatoryEducationList.size(), educationPassed, certificationsPassed, finalPassed);

        return JobEligibilityValidationResponse.EducationValidation.builder()
                .passed(finalPassed)
                .educationPassed(educationPassed)
                .certificationPassed(certificationsPassed)
                .intermediatePassed(intermediatePassed)
                .candidateEducation(candidateEducationList)
                .mandatoryEducation(mandatoryEducationList)
                .candidateCertifications(candidateCertificationsList)
                .mandatoryCertifications(mandatoryCertificationsList)
                .requiredEducation(intermediatePassed ? null :List.of(AppConstants.HAS_INTERMEDIATE_OR_DIPLOMA))
                .build();
    }
    
    /**
     * Checks if candidate has any graduation-level education
     *
     * @param candidateEducations List of candidate's education records
     * @return true if candidate has at least one graduation-level education
     */
    private boolean checkCandidateHasGraduationLevel(List<EducationEntity> candidateEducations) {
        if (candidateEducations == null || candidateEducations.isEmpty()) {
            return false;
        }
        
        // Get all education qualification IDs from candidate's educations
        Set<UUID> candidateQualificationIds = candidateEducations.stream()
                .map(EducationEntity::getEducationQualificationsId)
                .collect(Collectors.toSet());
        
        // Fetch all qualifications to get their level_ids
        List<EducationQualificationsEntity> qualifications = 
                educationQualificationsRepository.findAllById(candidateQualificationIds);
        
        // Get all level_ids from qualifications
        Set<UUID> levelIds = qualifications.stream()
                .map(EducationQualificationsEntity::getLevelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (levelIds.isEmpty()) {
            return false;
        }
        
        // Check if any of these level_ids correspond to "Graduation" in document_types
        List<DocumentTypesEntity> documentTypes = documentTypesRepository.findAllById(levelIds);
        
        boolean hasGraduation = documentTypes.stream()
                .anyMatch(doc -> AppConstants.GRADUATION_DOCUMENT_NAME.equals(doc.getDocumentName()));

        log.debug("Checked {} qualifications with {} level IDs, found graduation: {}", 
                qualifications.size(), levelIds.size(), hasGraduation);
        
        return hasGraduation;
    }
    
    /**
     * Checks if candidate has any post-graduation-level education
     *
     * @param candidateEducations List of candidate's education records
     * @return true if candidate has at least one post-graduation-level education
     */
    private boolean checkCandidateHasPostGraduationLevel(List<EducationEntity> candidateEducations) {
        if (candidateEducations == null || candidateEducations.isEmpty()) {
            return false;
        }
        
        // Get all education qualification IDs from candidate's educations
        Set<UUID> candidateQualificationIds = candidateEducations.stream()
                .map(EducationEntity::getEducationQualificationsId)
                .collect(Collectors.toSet());
        
        // Fetch all qualifications to get their level_ids
        List<EducationQualificationsEntity> qualifications = 
                educationQualificationsRepository.findAllById(candidateQualificationIds);
        
        // Get all level_ids from qualifications
        Set<UUID> levelIds = qualifications.stream()
                .map(EducationQualificationsEntity::getLevelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (levelIds.isEmpty()) {
            return false;
        }
        
        // Check if any of these level_ids correspond to "Post-Graduation" in document_types
        List<DocumentTypesEntity> documentTypes = documentTypesRepository.findAllById(levelIds);
        
        boolean hasPostGraduation = documentTypes.stream()
                .anyMatch(doc -> AppConstants.POST_GRADUATION_DOCUMENT_NAME.equals(doc.getDocumentName()));
        
        log.debug("Checked {} qualifications with {} level IDs, found post-graduation: {}", 
                qualifications.size(), levelIds.size(), hasPostGraduation);
        
        return hasPostGraduation;
    }
    
    /**
     * Validates if candidate has at least one of the mandatory certifications
     *
     * @param candidateId The candidate UUID
     * @param mandatoryCertificationIds Set of mandatory certification UUIDs from job position
     * @param candidateCertificationsList Output list to populate candidate certifications
     * @param mandatoryCertificationsList Output list to populate mandatory certifications
     * @return true if candidate has at least one mandatory certification or if no certifications are required
     */
    private boolean validateCertifications(
            UUID candidateId,
            Set<UUID> mandatoryCertificationIds,
            List<String> candidateCertificationsList,
            List<String> mandatoryCertificationsList) {
        
        log.debug("Validating certifications for candidateId: {}", candidateId);
        
        // Always get candidate's certifications regardless of job requirements
        List<CandidateCertificationsEntity> candidateCertifications = 
                candidateCertificationsRepository.findByCandidateId(candidateId);
        
        // Extract certification IDs from candidate's certifications
        Set<UUID> candidateCertificationIds = candidateCertifications.stream()
                .map(CandidateCertificationsEntity::getCertificationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // Fetch certification names for candidate's certifications
        if (!candidateCertificationIds.isEmpty()) {
            List<CertificationMasterEntity> candidateCertMasters = 
                    certificationMasterRepository.findAllById(candidateCertificationIds);
            candidateCertMasters.forEach(cert -> 
                    candidateCertificationsList.add(cert.getCertificationName()));
        }
        
        // If no mandatory certifications required, pass by default
        if (mandatoryCertificationIds == null || mandatoryCertificationIds.isEmpty()) {
            log.debug("No mandatory certifications required - candidate passes with {} certifications",
                    candidateCertificationsList.size());
            return true;
        }
        
        // Fetch certification names for mandatory certifications
        List<CertificationMasterEntity> mandatoryCertMasters = 
                certificationMasterRepository.findAllById(mandatoryCertificationIds);
        mandatoryCertMasters.forEach(cert -> 
                mandatoryCertificationsList.add(cert.getCertificationName()));
        
        // Check if candidate has at least one of the mandatory certifications
        boolean hasMatch = candidateCertificationIds.stream()
                .anyMatch(mandatoryCertificationIds::contains);
        
        log.info("Certification validation result - Candidate has {} certifications, Required: {} options, Passed: {}",
                candidateCertificationsList.size(), mandatoryCertificationsList.size(), hasMatch);
        
        return hasMatch;
    }
    
    /**
     * Formats education as "Qualification Name - Specialization Name" or just "Qualification Name"
     */
    private String formatEducation(String qualificationName, String specializationName) {
        if (qualificationName == null) {
            return "Unknown Qualification";
        }
        if (specializationName == null || specializationName.trim().isEmpty()) {
            return qualificationName;
        }
        return qualificationName + " - " + specializationName;
    }
    
    /**
     * Formats candidate education with duration and GPA/Percentage details
     */
    private String formatCandidateEducation(
            String qualificationName, 
            String specializationName,
            String educationTypeName,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal percentage) {
        
        StringBuilder formatted = new StringBuilder();
        
        // Add education type as prefix
        if (educationTypeName != null && !educationTypeName.trim().isEmpty()) {
            formatted.append(educationTypeName).append(" - ");
        }
        formatted.append(formatEducation(qualificationName, specializationName));
        
        // Add duration
        if (startDate != null && endDate != null) {
            int years = endDate.getYear() - startDate.getYear();
            formatted.append(" - Duration: ").append(years).append(years == 1 ? AppConstants.JOB_ELIGIBILITY_FORMAT_YEAR : AppConstants.JOB_ELIGIBILITY_FORMAT_YEARS);
        }
        
        // Add percentage (latest rule: candidate marks are percentage only)
        if (percentage != null) {
            formatted.append(" - Percentage: ").append(percentage);
        }
        
        return formatted.toString();
    }
    
    /**
     * Formats mandatory education with duration, GPA, and percentage requirements
     */
    private String formatMandatoryEducation(
            String qualificationName,
            String specializationName,
            String groupName,
            String educationTypeName,
            String duration,
            String gpa,
            String percentage) {
        
        StringBuilder formatted = new StringBuilder();
        
        // Add education type as prefix
        if (educationTypeName != null && !educationTypeName.trim().isEmpty()) {
            formatted.append(educationTypeName).append(" - ");
        }
        formatted.append(formatEducation(qualificationName, specializationName));
        // Add duration if specified
        if (isValidRequirement(groupName)) {
            formatted.append(" (").append(groupName).append(")");
        }

        if (isValidRequirement(duration)) {
            try {
                double durationValue = parseNumericValue(duration);
                int years = (int) durationValue;
                formatted.append(" - Duration: ").append(years).append(years == 1 ? AppConstants.JOB_ELIGIBILITY_FORMAT_YEAR : AppConstants.JOB_ELIGIBILITY_FORMAT_YEARS);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid duration value: {}", duration);
            }
        }
        
        // Add GPA and/or Percentage requirements
        boolean hasGPA = isValidRequirement(gpa);
        boolean hasPercentage = isValidRequirement(percentage);
        
        if (hasGPA || hasPercentage) {
            formatted.append(" - ");
            if (hasGPA) {
                formatted.append("GPA: ").append(gpa);
            }
            if (hasGPA && hasPercentage) {
                formatted.append(" or ");
            }
            if (hasPercentage) {
                formatted.append("Percentage: ").append(percentage);
            }
        }
        
        return formatted.toString();
    }
    
    /**
     * Checks if a requirement string is valid (not null, not empty, not "null")
     */
    private boolean isValidRequirement(String value) {
        return value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim());
    }
    
    /**
     * Parses numeric value from string with validation
     */
    private double parseNumericValue(String value){
        if (!isValidRequirement(value)) {
            throw new IllegalArgumentException("Invalid numeric value: " + value);
        }
        
        value = value.trim();
        
        // Check for invalid characters (only digits, dot, and optional minus allowed)
        if (!value.matches("-?\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("Invalid characters in numeric value: " + value);
        }
        
        return Double.parseDouble(value);
    }
    
    /**
     * Checks if candidate has a diploma-level education
     */
    private boolean candidateHasDiploma(List<EducationEntity> candidateEducations) {
        if (candidateEducations == null || candidateEducations.isEmpty()) {
            return false;
        }
        
        Set<UUID> qualificationIds = candidateEducations.stream()
                .map(EducationEntity::getEducationQualificationsId)
                .collect(Collectors.toSet());
        
        List<EducationQualificationsEntity> qualifications = 
                educationQualificationsRepository.findAllById(qualificationIds);
        
        Set<UUID> levelIds = qualifications.stream()
                .map(EducationQualificationsEntity::getLevelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (levelIds.isEmpty()) {
            return false;
        }
        
        List<DocumentTypesEntity> documentTypes = documentTypesRepository.findAllById(levelIds);
        
        return documentTypes.stream()
                .anyMatch(doc -> AppConstants.DIPLOMA_DOCUMENT_NAME.equalsIgnoreCase(doc.getDocumentName()));
    }
    
    /**
     * Gets the education level name for a qualification ID
     */
    private String getEducationLevelName(UUID qualificationId) {
        if (qualificationId == null) {
            return null;
        }
        
        EducationQualificationsEntity qualification = 
                educationQualificationsRepository.findById(qualificationId).orElse(null);
        
        if (qualification == null || qualification.getLevelId() == null) {
            return null;
        }
        
        DocumentTypesEntity documentType = 
                documentTypesRepository.findById(qualification.getLevelId()).orElse(null);
        
        return documentType != null ? documentType.getDocumentName() : null;
    }
    
    /**
     * Validates duration requirement for an education.
     * Uses the pre-fetched qualificationLevelNameMap to avoid per-call DB queries.
     */
    private boolean validateDuration(
            EducationEntity candidateEdu,
            String requiredDuration,
            boolean candidateHasDiploma,
            UUID reqQualificationId,
            Map<UUID, String> qualificationLevelNameMap) {

        if (!isValidRequirement(requiredDuration)) {
            return true; // No duration requirement
        }

        if (candidateEdu.getStartDate() == null || candidateEdu.getEndDate() == null) {
            return false; // Cannot validate without dates
        }

        try {
            int requiredYears  = (int) parseNumericValue(requiredDuration);
            int candidateYears = candidateEdu.getEndDate().getYear() - candidateEdu.getStartDate().getYear();

            // Diploma exception: diploma holder may have 1 year less for graduation-level requirements
            if (candidateHasDiploma && reqQualificationId != null) {
                String levelName = qualificationLevelNameMap.get(reqQualificationId);
                if (AppConstants.GRADUATION_DOCUMENT_NAME.equalsIgnoreCase(levelName)) {
                    return candidateYears >= requiredYears || candidateYears == (requiredYears - 1);
                }
            }

            // Candidate duration must be equal to or more than required (not less)
            return candidateYears >= requiredYears;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid duration value: {}", requiredDuration, e);
            return false;
        }
    }

    /**
     * Checks whether a candidate's education type satisfies the required type.
     *
     * <p>Rule: if the candidate's education is Full Time it passes any required type
     * (Full Time can substitute for Part Time, Distance, etc.).
     * In every other case an exact UUID match is required.
     */
    private boolean checkEducationTypeMatch(
            EducationEntity candidateEdu,
            UUID reqEducationTypeId,
            Map<UUID, String> educationTypeMap) {

        if (reqEducationTypeId == null) return true; // No type requirement on the position
        if (candidateEdu.getEducationTypeId() == null) return false; // Candidate record has no type

        String candidateTypeName = educationTypeMap.get(candidateEdu.getEducationTypeId());
        // Full Time candidate education satisfies any type requirement
        if (AppConstants.FULL_TIME_EDUCATION_TYPE.equalsIgnoreCase(candidateTypeName)) {
            return true;
        }
        // Otherwise exact UUID match
        return reqEducationTypeId.equals(candidateEdu.getEducationTypeId());
    }

    /**
     * Validates required percentage.
     *
     * Latest rule: only percentage is considered from both candidate and job.
     * GPA input is ignored.
     */
    private boolean validateGPAOrPercentage(
            BigDecimal candidatePercentage,
            String requiredGPA,
            String requiredPercentage) {

        boolean hasPercentageReq = isValidRequirement(requiredPercentage);

        if (!hasPercentageReq) {
            return true; // No percentage requirement
        }

        if (candidatePercentage == null) {
            return false; // Candidate has no percentage data
        }

        try {
            double requiredPercentageValue = parseNumericValue(requiredPercentage);
            return candidatePercentage.doubleValue() >= requiredPercentageValue;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid percentage requirement", e);
            return false;
        }
    }
    
    /**
     * Checks if a candidate education matches a single condition.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Any Graduation / Any Post-Graduation  — candidate must be exactly at that level (no higher, no specialization required)</li>
     *   <li>Master board (SSC/CBSE/ICSE)          — qualification auto-pass, still enforces type + percentage</li>
     *   <li>Regular exact match                   — qualification + specialization + type + duration + percentage</li>
     * </ol>
     * Note: higher-level qualification substitution (e.g. MTech instead of BTech) is not allowed.
     */
    private boolean matchesEducationCondition(
            EducationEntity candidateEdu,
            List<EducationEntity> candidateEdus,
            JsonNode condition,
            Map<UUID, String> qualificationMap,
            Map<UUID, String> qualificationCodeMap,
            Map<UUID, Integer> qualificationLevelScoreMap,
            Map<UUID, String> qualificationLevelNameMap,
            Map<UUID, String> specializationMap,
            Map<UUID, String> educationTypeMap,
            List<String> masterBoardQualCodes,
            boolean candidateHasDiploma) {

        UUID reqQualificationId = condition.has(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText())
                ? UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText())
                : null;

        UUID reqSpecializationId = condition.has(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ) && isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText())
                ? UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_SPECIALIZATION ).asText())
                : null;

        String eduTypeKey = condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE
                : condition.has(AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID) ? AppConstants.JOB_ELIGIBILITY_EDUCATION_TYPE_ID : null;
        UUID reqEducationTypeId = eduTypeKey != null && isValidRequirement(condition.get(eduTypeKey).asText())
                ? UUID.fromString(condition.get(eduTypeKey).asText())
                : null;

        String reqDuration   = condition.has(AppConstants.JOB_ELIGIBILITY_DURATION)    ? condition.get(AppConstants.JOB_ELIGIBILITY_DURATION).asText()    : null;
        String reqGPA        = condition.has("gpa")         ? condition.get("gpa").asText()         : null;
        String reqPercentage = condition.has(AppConstants.JOB_ELIGIBILITY_PERCENTAGE)  ? condition.get(AppConstants.JOB_ELIGIBILITY_PERCENTAGE).asText()  : null;

        // STEP 1 — "Any Graduation" / "Any Post-Graduation": score-based >= check
        if (reqQualificationId != null) {
            String qualName = qualificationMap.get(reqQualificationId);
            boolean isAnyGrad     = AppConstants.ANY_GRADUATION_QUALIFICATION_NAME.equals(qualName);
            boolean isAnyPostGrad = AppConstants.ANY_POST_GRADUATION_QUALIFICATION_NAME.equals(qualName);

            if (isAnyGrad || isAnyPostGrad) {
                UUID groupId = condition.has("group") && isValidRequirement(condition.get("group").asText())
                        ? UUID.fromString(condition.get("group").asText())
                        : null;
//                if (groupId == null) {
//                    return false;
//                }
                boolean educationCheck = isAnyGrad ? checkEducationLevelCheck(candidateEdus, groupId, "GRAD")
                        : checkEducationLevelCheck(candidateEdus, groupId, "POST_GRAD");
                if (!educationCheck) {
                    return false;
                }
                int scoreReq  = qualificationLevelScoreMap.getOrDefault(reqQualificationId, 0);
                int scoreCand = qualificationLevelScoreMap.getOrDefault(candidateEdu.getEducationQualificationsId(), 0);

                boolean levelOk;
                if (scoreReq > 0) {
                    // Candidate must be at exactly the required level (not higher)
                    levelOk = scoreCand == scoreReq;
                } else {
                    // Score not yet configured — fall back to level-name check
                    levelOk = isAnyGrad
                            ? checkEducationLevelCheck(candidateEdus, groupId, "GRAD")
                            : checkEducationLevelCheck(candidateEdus, groupId, "POST_GRAD");
                }
                if (!levelOk) return false;

                if (!checkEducationTypeMatch(candidateEdu, reqEducationTypeId, educationTypeMap)) return false;

                // Duration: enforce when provided (candidate must be at exactly the required level at this point)
                if (!validateDuration(candidateEdu, reqDuration, candidateHasDiploma,
                        reqQualificationId, qualificationLevelNameMap)) return false;

                // No specialization check (per spec)
                return validateGPAOrPercentage(candidateEdu.getPercentage(), reqGPA, reqPercentage);
            }
        }

        // STEP 2 — Master board auto-pass (SSC / CBSE / ICSE treated as equivalent 10th qualifications)
        if (reqQualificationId != null) {
            String reqQualCode = qualificationCodeMap.getOrDefault(reqQualificationId, "");
            if (masterBoardQualCodes.contains(reqQualCode)) {
                // Qualification level accepted automatically; still enforce type + GPA
                if (!checkEducationTypeMatch(candidateEdu, reqEducationTypeId, educationTypeMap)) return false;
                return validateGPAOrPercentage(candidateEdu.getPercentage(), reqGPA, reqPercentage);
            }
        }

        // STEP 3 — Regular exact match
        boolean qualificationMatch = reqQualificationId == null ||
                reqQualificationId.equals(candidateEdu.getEducationQualificationsId());

        boolean specializationMatch = reqSpecializationId == null ||
                reqSpecializationId.equals(candidateEdu.getSpecializationId());

        boolean durationMatch = validateDuration(
                candidateEdu, reqDuration, candidateHasDiploma, reqQualificationId, qualificationLevelNameMap);

        boolean gpaPercentageMatch = validateGPAOrPercentage(candidateEdu.getPercentage(), reqGPA, reqPercentage);

        // Full Time education passes any type requirement; otherwise exact match
        boolean educationTypeMatch = checkEducationTypeMatch(candidateEdu, reqEducationTypeId, educationTypeMap);

        return qualificationMatch && specializationMatch && durationMatch && gpaPercentageMatch && educationTypeMatch;
    }
    private boolean checkEducationLevelCheck(List<EducationEntity> candidateEducations,
                                             UUID groupId,
                                             String documentCode) {

        if (candidateEducations == null || candidateEducations.isEmpty() || documentCode == null) {
            return false;
        }

        DocumentTypesEntity documentType = documentTypesRepository.findByDocCode(documentCode);
        if (documentType == null) {
            return false;
        }

        Set<UUID> qualificationIdsForLevel = educationQualificationsRepository
                .findAllByLevelIdIn(List.of(documentType.getId()))
                .stream()
                .map(EducationQualificationsEntity::getId)
                .collect(Collectors.toSet());

        for (EducationEntity education : candidateEducations) {

            // Candidate qualification should belong to the required level
            if (!qualificationIdsForLevel.contains(education.getEducationQualificationsId())) {
                continue;
            }

            // No group specified -> any qualification at this level is acceptable
            if (groupId == null) {
                return true;
            }

            Optional<QualificationGroupMappingEntity> candidateMapping =
                    qualificationGroupMappingRepository.findMapping(
                            education.getEducationQualificationsId(),
                            education.getSpecializationId());

            if (candidateMapping.isPresent()
                    && groupId.equals(candidateMapping.get().getEducationGroupId())) {
                return true;
            }
        }

        return false;
    }
    /**
     * Validates a single group (AND logic - all conditions in group must match)
     */
    private boolean validateEducationGroup(
            JsonNode group,
            List<EducationEntity> candidateEducations,
            Map<UUID, String> qualificationMap,
            Map<UUID, String> qualificationCodeMap,
            Map<UUID, Integer> qualificationLevelScoreMap,
            Map<UUID, String> qualificationLevelNameMap,
            Map<UUID, String> specializationMap,
            Map<UUID, String> educationTypeMap,
            List<String> masterBoardQualCodes,
            boolean candidateHasDiploma) {

        if (!group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) {
            return false;
        }

        JsonNode conditions = group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS);
        if (!conditions.isArray() || conditions.size() == 0) {
            return false;
        }

        // All conditions in group must be satisfied (AND logic)
        for (JsonNode condition : conditions) {
            boolean conditionMatched = candidateEducations.stream()
                    .anyMatch(candEdu -> matchesEducationCondition(
                            candEdu,candidateEducations, condition, qualificationMap, qualificationCodeMap,
                            qualificationLevelScoreMap, qualificationLevelNameMap,
                            specializationMap, educationTypeMap, masterBoardQualCodes, candidateHasDiploma));

            if (!conditionMatched) {
                return false; // One condition failed, group fails
            }
        }

        return true; // All conditions matched
    }

    /**
     * Validates candidate's age against job position age requirements with relaxation rules
     *
     * @param candidateId The UUID of the candidate
     * @param position    The job position entity
     * @return AgeValidation result
     */
    private JobEligibilityValidationResponse.AgeValidation validateAge(UUID candidateId, JobPositionsEntity position) {
        log.debug("Validating age for candidateId: {}", candidateId);

        // Get candidate profile
        CandidateProfileEntity candidateProfile = candidateProfileRepository.findByCandidateId(candidateId)
                .orElse(null);

        if (candidateProfile == null || candidateProfile.getDateOfBirth() == null) {
            log.warn("Candidate profile or date of birth not found for candidateId: {}", candidateId);
            return JobEligibilityValidationResponse.AgeValidation.builder()
                    .passed(false)
                    .candidateAge(null)
                    .allowedAge(null)
                    .stateWiseAgeValidations(null)
                    .build();
        }

        // Calculate candidate's completed age (no rounding up)
        LocalDate referenceDate = (position.getCutoffDate() != null) ? position.getCutoffDate() : LocalDate.now();
        referenceDate = referenceDate.plusDays(1);  //include the cutoff date also

        LocalDate dateOfBirth = candidateProfile.getDateOfBirth();
        Period agePeriod = Period.between(dateOfBirth, referenceDate.plusDays(1));
        int candidateAge = agePeriod.getYears();

        Integer eligibilityAgeMin = position.getEligibilityAgeMin();
        Integer eligibilityAgeMax = position.getEligibilityAgeMax();

        if (eligibilityAgeMin == null || eligibilityAgeMax == null) {
            log.warn("Age eligibility criteria not defined for position: {}", position.getId());
            return JobEligibilityValidationResponse.AgeValidation.builder()
                    .passed(true)
                    .candidateAge(candidateAge + " " + AppConstants.YEARS + " " + agePeriod.getMonths() + " " + AppConstants.MONTHS + " " + agePeriod.getDays() + " " + AppConstants.DAYS)
                    .allowedAge(null)
                    .stateWiseAgeValidations(null)
                    .build();
        }

        // Check if job is location-wise (state-wise)
        boolean isLocationWise = Boolean.TRUE.equals(position.getIsLocationWise());

        if (isLocationWise) {
            // State-wise job - validate for each state
            return validateAgeForStateWise(candidateProfile, position, dateOfBirth, referenceDate, eligibilityAgeMin, eligibilityAgeMax);
        } else {
            // National job - single validation
            return validateAgeForNational(candidateProfile, position, dateOfBirth, referenceDate, eligibilityAgeMin, eligibilityAgeMax);
        }
    }

    /**
     * Validates age for national (non-location-wise) jobs
     */
    private JobEligibilityValidationResponse.AgeValidation validateAgeForNational(
            CandidateProfileEntity candidateProfile,
            JobPositionsEntity position,
            LocalDate dateOfBirth,
            LocalDate referenceDate,
            int eligibilityAgeMin,
            int eligibilityAgeMax) {

        Period agePeriod = Period.between(dateOfBirth, referenceDate);
        int candidateAge = agePeriod.getYears();

        // Check if candidate is below minimum age
        LocalDate minAgeCutoff = referenceDate.minusYears(eligibilityAgeMin);
        if (dateOfBirth.isAfter(minAgeCutoff)) {
            log.info("Candidate DOB {} is after min age cutoff {} - Failed", dateOfBirth, minAgeCutoff);
            return JobEligibilityValidationResponse.AgeValidation.builder()
                    .passed(false)
                    .candidateAge(candidateAge + " " + AppConstants.YEARS + " " + agePeriod.getMonths() + " " + AppConstants.MONTHS + " " + agePeriod.getDays() + " " + AppConstants.DAYS)
                    .allowedAge(eligibilityAgeMin + " - " + eligibilityAgeMax + " " + AppConstants.YEARS)
                    .stateWiseAgeValidations(null)
                    .build();
        }

        // Check if candidate is within the base age range
        LocalDate maxAgeCutoff = referenceDate.minusYears(eligibilityAgeMax);
        if (!dateOfBirth.isBefore(maxAgeCutoff)) {
            log.info("Candidate DOB {} is not before max age cutoff {} - Passed", dateOfBirth, maxAgeCutoff);
            return JobEligibilityValidationResponse.AgeValidation.builder()
                    .passed(true)
                    .candidateAge(candidateAge + " " + AppConstants.YEARS + " " + agePeriod.getMonths() + " " + AppConstants.MONTHS + " " + agePeriod.getDays() + " " + AppConstants.DAYS)
                    .allowedAge(eligibilityAgeMin + " - " + eligibilityAgeMax + " " + AppConstants.YEARS)
                    .stateWiseAgeValidations(null)
                    .build();
        }

        // Candidate age is above eligibilityAgeMax - check for age relaxation
        log.debug("Candidate DOB {} is before max age cutoff {} - checking relaxation", dateOfBirth, maxAgeCutoff);

        List<String> relaxationCategories = determineRelaxationForNational(candidateProfile, position);
        addSpecialRelaxations(candidateProfile, relaxationCategories, position);

        if (relaxationCategories.isEmpty()) {
            log.info("No age relaxation categories applicable for candidate - Failed");
            return JobEligibilityValidationResponse.AgeValidation.builder()
                    .passed(false)
                    .candidateAge(candidateAge + " " + AppConstants.YEARS + " " + agePeriod.getMonths() + " " + AppConstants.MONTHS + " " + agePeriod.getDays() + " " + AppConstants.DAYS)
                    .allowedAge(eligibilityAgeMin + " - " + eligibilityAgeMax + " " + AppConstants.YEARS)
                    .stateWiseAgeValidations(null)
                    .build();
        }

        int maxRelaxationYears = getMaxRelaxationYears(relaxationCategories);
        int allowedMaxAge = eligibilityAgeMax + maxRelaxationYears;
        LocalDate maxAgeCutoffWithRelaxation = referenceDate.minusYears(allowedMaxAge);
        boolean passed = !dateOfBirth.isBefore(maxAgeCutoffWithRelaxation);

        log.info("Age validation with relaxation - Candidate DOB: {}, Allowed max age cutoff: {}, Relaxation: {} years, Passed: {}",
                dateOfBirth, maxAgeCutoffWithRelaxation, maxRelaxationYears, passed);

        return JobEligibilityValidationResponse.AgeValidation.builder()
                .passed(passed)
                .candidateAge(candidateAge + " "+ AppConstants.YEARS + " " + agePeriod.getMonths() + " " + AppConstants.MONTHS + " " + agePeriod.getDays() + " " + AppConstants.DAYS)
                .allowedAge(eligibilityAgeMin + " - " + allowedMaxAge + " "+AppConstants.YEARS)
                .stateWiseAgeValidations(null)
                .build();
    }

    /**
     * Validates age for state-wise jobs - checks all states and returns state-wise results
     */
    private JobEligibilityValidationResponse.AgeValidation validateAgeForStateWise(
            CandidateProfileEntity candidateProfile,
            JobPositionsEntity position,
            LocalDate dateOfBirth,
            LocalDate referenceDate,
            int eligibilityAgeMin,
            int eligibilityAgeMax) {

        Period agePeriod = Period.between(dateOfBirth, referenceDate);
        int candidateAge = agePeriod.getYears();

        // Check if candidate is below minimum age - automatic fail for all states
        LocalDate minAgeCutoff = referenceDate.minusYears(eligibilityAgeMin);
        if (dateOfBirth.isAfter(minAgeCutoff)) {
            log.info("Candidate DOB {} is after min age cutoff {} - Failed for all states", dateOfBirth, minAgeCutoff);
            return JobEligibilityValidationResponse.AgeValidation.builder()
                    .passed(false)
                    .candidateAge(candidateAge + " "+ AppConstants.YEARS + " " + agePeriod.getMonths() + " " + AppConstants.MONTHS + " " + agePeriod.getDays() + " " + AppConstants.DAYS)
                    .allowedAge(eligibilityAgeMin + " - " + eligibilityAgeMax + " "+ AppConstants.YEARS)
                    .stateWiseAgeValidations(new ArrayList<>())
                    .build();
        }

        List<PositionStateDistributionEntity> stateDistributions = position.getPositionStateDistributions();
        
        if (stateDistributions == null || stateDistributions.isEmpty()) {
            log.warn("No state distributions found for location-wise job: {}", position.getId());
            return JobEligibilityValidationResponse.AgeValidation.builder()
                    .passed(false)
                    .candidateAge(candidateAge + " "+ AppConstants.YEARS + " " + agePeriod.getMonths() + " " + AppConstants.MONTHS + " " + agePeriod.getDays() + " " + AppConstants.DAYS)
                    .allowedAge(eligibilityAgeMin + " - " + eligibilityAgeMax + " "+ AppConstants.YEARS)
                    .stateWiseAgeValidations(new ArrayList<>())
                    .build();
        }

        // Fetch all state names in bulk
        Set<UUID> stateIds = stateDistributions.stream()
                .map(PositionStateDistributionEntity::getStateId)
                .collect(Collectors.toSet());
        
        Map<UUID, String> stateNameMap = stateRepository.findAllById(stateIds)
                .stream()
                .collect(Collectors.toMap(StateEntity::getId, StateEntity::getStateName));

        // Fetch all city names in bulk
        Set<UUID> cityIds = stateDistributions.stream()
                .map(PositionStateDistributionEntity::getCityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<UUID, String> cityNameMap = new HashMap<>();
        if (!cityIds.isEmpty()) {
            cityNameMap = cityRepository.findAllById(cityIds)
                    .stream()
                    .collect(Collectors.toMap(CityEntity::getId, CityEntity::getCityName));
        }

        // Validate for each state
        List<JobEligibilityValidationResponse.StateWiseAgeValidation> stateWiseResults = new ArrayList<>();
        boolean overallPassed = false;
        LocalDate maxAgeCutoff = referenceDate.minusYears(eligibilityAgeMax);
        Set<UUID> allowedReservationCategoryIds =
                resolveEligibleVacancyCategoryIds(candidateProfile.getReservationCategoryId());

        for (PositionStateDistributionEntity stateDistribution : stateDistributions) {
            UUID stateId = stateDistribution.getStateId();
            UUID cityId = stateDistribution.getCityId();
            String stateName = stateNameMap.getOrDefault(stateId, AppConstants.UNKNOWN_STATE);
            String cityName = cityId != null ? cityNameMap.getOrDefault(cityId, "Unknown City") : null;
            boolean vacancyPassedForState =
                    hasEligibleReservationVacancyInState(stateDistribution, allowedReservationCategoryIds);

            // Check if candidate is within base age range (no relaxation needed)
            if (!dateOfBirth.isBefore(maxAgeCutoff)) {
                boolean combinedPassed = vacancyPassedForState;
                stateWiseResults.add(JobEligibilityValidationResponse.StateWiseAgeValidation.builder()
                        .stateId(stateId.toString())
                        .stateName(stateName)
                        .cityId(cityId != null ? cityId.toString() : null)
                        .cityName(cityName)
                        .passed(combinedPassed)
                        .ageValidationPassed(true)
                        .stateVacancyValidationPassed(vacancyPassedForState)
                        .allowedAge(eligibilityAgeMin + " - " + eligibilityAgeMax + " "+ AppConstants.YEARS)
                        .build());
                if (combinedPassed) {
                    overallPassed = true;
                }
                continue;
            }

            // Candidate age exceeds max - check relaxation for this state
            List<String> relaxationCategories = determineRelaxationForSpecificState(
                    candidateProfile, 
                    stateDistribution
            );
            addSpecialRelaxations(candidateProfile, relaxationCategories, position);

            if (relaxationCategories.isEmpty()) {
                // No relaxation applicable for this state
                stateWiseResults.add(JobEligibilityValidationResponse.StateWiseAgeValidation.builder()
                        .stateId(stateId.toString())
                        .stateName(stateName)
                        .cityId(cityId != null ? cityId.toString() : null)
                        .cityName(cityName)
                        .passed(false)
                        .ageValidationPassed(false)
                        .stateVacancyValidationPassed(vacancyPassedForState)
                        .allowedAge(eligibilityAgeMin + " - " + eligibilityAgeMax + " "+ AppConstants.YEARS)
                        .build());
                continue;
            }

            int maxRelaxationYears = getMaxRelaxationYears(relaxationCategories);
            int allowedMaxAge = eligibilityAgeMax + maxRelaxationYears;
            LocalDate maxAgeCutoffWithRelaxation = referenceDate.minusYears(allowedMaxAge);
            boolean agePassedForState = !dateOfBirth.isBefore(maxAgeCutoffWithRelaxation);
            boolean passedForState = agePassedForState && vacancyPassedForState;

            stateWiseResults.add(JobEligibilityValidationResponse.StateWiseAgeValidation.builder()
                    .stateId(stateId.toString())
                    .stateName(stateName)
                    .cityId(cityId != null ? cityId.toString() : null)
                    .cityName(cityName)
                    .passed(passedForState)
                    .ageValidationPassed(agePassedForState)
                    .stateVacancyValidationPassed(vacancyPassedForState)
                    .allowedAge(eligibilityAgeMin + " - " + allowedMaxAge + " "+ AppConstants.YEARS)
                    .build());

            if (passedForState) {
                overallPassed = true;
            }
        }

        log.info("State-wise age validation - Candidate age: {}, Overall passed: {}, States passed: {}/{}", 
                candidateAge, overallPassed, 
                stateWiseResults.stream().filter(JobEligibilityValidationResponse.StateWiseAgeValidation::getPassed).count(),
                stateWiseResults.size());

        return JobEligibilityValidationResponse.AgeValidation.builder()
                .passed(overallPassed)
                .candidateAge(candidateAge + " "+ AppConstants.YEARS + " " + agePeriod.getMonths() + " " + AppConstants.MONTHS + " " + agePeriod.getDays() + " " + AppConstants.DAYS)
                .allowedAge(null) // Not applicable for state-wise, shown per state
                .stateWiseAgeValidations(stateWiseResults)
                .build();
    }

    /**
     * Gets maximum relaxation years from list of relaxation category codes.
     * Total = reservation category years + max special category years.
     */
    private int getMaxRelaxationYears(List<String> relaxationCategoryCodes) {
        RelaxationDetails details = getRelaxationDetails(relaxationCategoryCodes, 0, 0);
        return details != null ? details.getTotalRelaxationYears() : 0;
    }

    /**
     * Gets detailed relaxation information using two-part calculation.
     *
     * The formed list contains reservation category codes (GEN/SC/ST/OBC/EWS) and
     * special category codes (PWD, EX_SERVICEMAN, WIDOWS, DIVORCED_WOMEN, JSW, RIOT_VICTIM_FAMILY).
     *
     * Total relaxation = reservation category years + max(special category years).
     *
     * Example: {SC, EX_SERVICEMAN, PWD} → 5 (SC) + max(5 (EX_SERVICEMAN), 10 (PWD)) = 15 years.
     */
    private RelaxationDetails getRelaxationDetails(List<String> relaxationCategoryCodes, 
                                                     int candidateAge, 
                                                     int eligibilityAgeMax) {
        if (relaxationCategoryCodes.isEmpty()) {
            return RelaxationDetails.builder()
                    .totalRelaxationYears(0)
                    .reservationRelaxationYears(0)
                    .specialRelaxationYears(0)
                    .appliedRelReservationCategoryId(null)
                    .appliedRelSpecialCategoryId(null)
                    .actualAppliedYears(null)
                    .build();
        }

        Set<String> reservationCodeSet = Arrays.stream(AppConstants.RESERVATION_CATEGORY_CODES)
                .collect(Collectors.toSet());

        List<String> resCodes = relaxationCategoryCodes.stream()
                .filter(code -> reservationCodeSet.contains(code.toUpperCase()))
                .collect(Collectors.toList());

        List<String> specialCodes = relaxationCategoryCodes.stream()
                .filter(code -> !reservationCodeSet.contains(code.toUpperCase()))
                .collect(Collectors.toList());

        if (resCodes.size() > 1) {
            log.error("Invalid category data: multiple reservation categories found in relaxation list: {}", resCodes);
            throw new IllegalStateException("Invalid category data: multiple reservation categories found — " + resCodes);
        }

        // Fetch all matching entries from the static age relaxation categories table
        List<AgeRelaxationCategoriesEntity> allRelaxationEntries = 
                ageRelaxationCategoriesRepository.findByAgeRelaxationCategoryCodeIn(relaxationCategoryCodes);

        // 1. Reservation category relaxation
        int reservationYears = 0;
        UUID appliedResId = null;
        if (!resCodes.isEmpty()) {
            String resCode = resCodes.get(0);
            Optional<AgeRelaxationCategoriesEntity> resEntry = allRelaxationEntries.stream()
                    .filter(e -> resCode.equalsIgnoreCase(e.getAgeRelaxationCategoryCode()))
                    .findFirst();
            if (resEntry.isPresent() && resEntry.get().getRelaxationYears() != null) {
                reservationYears = resEntry.get().getRelaxationYears();
                appliedResId = resEntry.get().getId();
            }
        }

        // 2. Special category relaxation — pick the one with max years
        int specialYears = 0;
        UUID appliedSpecialId = null;
        if (!specialCodes.isEmpty()) {
            Optional<AgeRelaxationCategoriesEntity> maxSpecial = allRelaxationEntries.stream()
                    .filter(e -> !reservationCodeSet.contains(e.getAgeRelaxationCategoryCode().toUpperCase()))
                    .filter(e -> e.getRelaxationYears() != null)
                    .max(Comparator.comparing(AgeRelaxationCategoriesEntity::getRelaxationYears));
            if (maxSpecial.isPresent()) {
                specialYears = maxSpecial.get().getRelaxationYears();
                appliedSpecialId = maxSpecial.get().getId();
            }
        }

        int totalRelaxation = reservationYears + specialYears;

        // Calculate actual applied years only if candidateAge and eligibilityAgeMax are provided
        Integer actualAppliedYears = null;
        if (candidateAge > 0 && eligibilityAgeMax > 0 && candidateAge > eligibilityAgeMax) {
            actualAppliedYears = candidateAge - eligibilityAgeMax;
        }

        return RelaxationDetails.builder()
                .totalRelaxationYears(totalRelaxation)
                .reservationRelaxationYears(reservationYears)
                .specialRelaxationYears(specialYears)
                .appliedRelReservationCategoryId(appliedResId)
                .appliedRelSpecialCategoryId(appliedSpecialId)
                .actualAppliedYears(actualAppliedYears)
                .build();
    }

    /**
     * Saves age relaxation application data for a single record (national or one state)
     * 
     * @param candidateId The candidate UUID
     * @param positionId The job position UUID
     * @param candidateAge The candidate's age in years
     * @param eligibilityAgeMax The maximum age allowed for the job
     * @param relaxationCategories List of applicable relaxation categories
     * @param stateId The state UUID (null for national jobs)
     * @param ageValidationPassed Whether the candidate passed age validation for this state (null for national jobs - not applicable)
     * @param vacancyValidationPassed Whether the candidate vacancy passed validation for this state (null for national jobs - not applicable)
     */
    @Transactional
    private void saveAgeRelaxationApplication(
            UUID candidateId,
            UUID positionId,
            int candidateAge,
            int eligibilityAgeMax,
            List<String> relaxationCategories,
            UUID stateId,
            UUID cityId,
            Boolean ageValidationPassed,
            Boolean vacancyValidationPassed) {
        
        try {
            // Check if candidate is within age limits without relaxation
            boolean relaxationApplied = candidateAge > eligibilityAgeMax;
            
            RelaxationDetails relaxationDetails = null;
            if (relaxationApplied && !relaxationCategories.isEmpty()) {
                // Get detailed relaxation information
                relaxationDetails = getRelaxationDetails(relaxationCategories, candidateAge, eligibilityAgeMax);
            }
            
            // Build the entity
            AgeRelaxationApplicationEntity entity = AgeRelaxationApplicationEntity.builder()
                    .candidateId(candidateId)
                    .positionId(positionId)
                    .relaxationApplied(relaxationApplied)
                    .appliedRelReservationCategoryId(relaxationDetails != null ? relaxationDetails.getAppliedRelReservationCategoryId() : null)
                    .appliedRelSpecialCategoryId(relaxationDetails != null ? relaxationDetails.getAppliedRelSpecialCategoryId() : null)
                    .actualAppliedRelaxationYears(relaxationDetails != null ? relaxationDetails.getActualAppliedYears() : null)
                    .ageYearsAtRefDate(candidateAge)
                    .stateId(stateId)
                    .cityId(cityId)
                    .stateAgeValidationPassed(stateId != null ? ageValidationPassed : null)
                    .stateVacancyValidationPassed(stateId != null ? vacancyValidationPassed : null)
                    .build();
            
            // Check if record already exists for this candidate-position-state-city combination
            Optional<AgeRelaxationApplicationEntity> existing = 
                    ageRelaxationApplicationRepository.findByCandidateIdAndPositionIdAndStateIdAndCityId(
                            candidateId, positionId, stateId, cityId);
            
            if (existing.isPresent()) {
                // Update existing record
                AgeRelaxationApplicationEntity existingEntity = existing.get();
                existingEntity.setRelaxationApplied(relaxationApplied);
                existingEntity.setAppliedRelReservationCategoryId(entity.getAppliedRelReservationCategoryId());
                existingEntity.setAppliedRelSpecialCategoryId(entity.getAppliedRelSpecialCategoryId());
                existingEntity.setActualAppliedRelaxationYears(entity.getActualAppliedRelaxationYears());
                existingEntity.setAgeYearsAtRefDate(candidateAge);
                existingEntity.setStateAgeValidationPassed(stateId != null ? ageValidationPassed : null);
                existingEntity.setStateVacancyValidationPassed(stateId != null ? vacancyValidationPassed : null);
                ageRelaxationApplicationRepository.save(existingEntity);
                log.info("Updated age relaxation application for candidateId: {}, positionId: {}, stateId: {}, cityId: {}", 
                        candidateId, positionId, stateId, cityId);
            } else {
                // Create new record
                ageRelaxationApplicationRepository.save(entity);
                log.info("Saved new age relaxation application for candidateId: {}, positionId: {}, stateId: {}, cityId: {}, relaxationApplied: {}", 
                        candidateId, positionId, stateId, cityId, relaxationApplied);
            }
        } catch (Exception e) {
            log.error("Error saving age relaxation application for candidateId: {}, positionId: {}, stateId: {}, cityId: {}", 
                    candidateId, positionId, stateId, cityId, e);
            // Don't fail the validation if saving fails
        }
    }

    /**
     * Determines relaxation categories for a specific state distribution.
     *
     * Builds a list of relaxation category codes:
     * - Candidate's reservation category code (if vacancy exists in this state)
     * - "PWD" (if candidate has a disability AND matching disability vacancy exists in this state)
     */
    private List<String> determineRelaxationForSpecificState(
            CandidateProfileEntity candidateProfile,
            PositionStateDistributionEntity stateDistribution) {

        List<String> relaxationCategories = new ArrayList<>();
        UUID candidateReservationCategoryId = candidateProfile.getReservationCategoryId();

        List<PositionCategoryDistributionEntity> categoryDistributions = 
                stateDistribution.getPositionCategoryDistributions();

        if (categoryDistributions == null || categoryDistributions.isEmpty()) {
            return relaxationCategories;
        }

        // 1. Check reservation category vacancy in this state
        boolean hasReservationVacancy = categoryDistributions.stream()
                .anyMatch(dist -> !Boolean.TRUE.equals(dist.getIsDisability()) &&
                        candidateReservationCategoryId.equals(dist.getReservationCategoryId()) &&
                        dist.getVacancyCount() != null && dist.getVacancyCount() > 0);

        if (hasReservationVacancy) {
            reservationCategoriesRepository.findById(candidateReservationCategoryId)
                    .ifPresent(cat -> relaxationCategories.add(cat.getCategoryCode()));
        }

        // 2. Check PwD vacancy in this state (only if candidate has disabilities)
        List<CandidateDisabilityDetailsEntity> candidateDisabilities = 
                candidateDisabilityDetailsRepository.findAllByCandidateId(candidateProfile.getCandidateId());

        if (!candidateDisabilities.isEmpty()) {
            Set<UUID> candidateDisabilityIds = candidateDisabilities.stream()
                    .filter(disability -> disability.getDisabilityPercentage() != null &&
                            disability.getDisabilityPercentage() >= 40)
                    .map(CandidateDisabilityDetailsEntity::getDisabilityCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            boolean hasDisabilityVacancy = categoryDistributions.stream()
                    .anyMatch(dist -> Boolean.TRUE.equals(dist.getIsDisability()) &&
                            dist.getDisabilityCategoryId() != null &&
                            candidateDisabilityIds.contains(dist.getDisabilityCategoryId()) &&
                            dist.getVacancyCount() != null && dist.getVacancyCount() > 0);

            if (hasDisabilityVacancy) {
                relaxationCategories.add(AppConstants.PWD_CATEGORY);
            }
        }

        return relaxationCategories;
    }

    /**
     * Determines relaxation categories for national (non-location-wise) jobs.
     *
     * Builds a list of relaxation category codes:
     * - Candidate's reservation category code (if vacancy exists for that category)
     * - "PWD" (if candidate has a disability AND matching disability vacancy exists)
     */
    private List<String> determineRelaxationForNational(
            CandidateProfileEntity candidateProfile,
            JobPositionsEntity position) {

        List<String> relaxationCategories = new ArrayList<>();
        UUID candidateReservationCategoryId = candidateProfile.getReservationCategoryId();

        // Get national distributions
        List<PositionCategoryNationalDistributionEntity> nationalDistributions = 
                position.getPositionCategoryNationalDistributions();

        // 1. Check reservation category vacancy
        boolean hasReservationVacancy = nationalDistributions.stream()
                .anyMatch(dist -> !Boolean.TRUE.equals(dist.getIsDisability()) &&
                        candidateReservationCategoryId.equals(dist.getReservationCategoryId()) &&
                        dist.getVacancyCount() != null && dist.getVacancyCount() > 0);

        if (hasReservationVacancy) {
            reservationCategoriesRepository.findById(candidateReservationCategoryId)
                    .ifPresent(cat -> relaxationCategories.add(cat.getCategoryCode()));
        }

        // 2. Check PwD vacancy (only if candidate has disabilities)
        List<CandidateDisabilityDetailsEntity> candidateDisabilities = 
                candidateDisabilityDetailsRepository.findAllByCandidateId(candidateProfile.getCandidateId());

        if (!candidateDisabilities.isEmpty()) {
            Set<UUID> candidateDisabilityIds = candidateDisabilities.stream()
                    .filter(disability -> disability.getDisabilityPercentage() != null &&
                            disability.getDisabilityPercentage() >= 40)
                    .map(CandidateDisabilityDetailsEntity::getDisabilityCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            boolean hasDisabilityVacancy = nationalDistributions.stream()
                    .anyMatch(dist -> Boolean.TRUE.equals(dist.getIsDisability()) &&
                            dist.getDisabilityCategoryId() != null &&
                            candidateDisabilityIds.contains(dist.getDisabilityCategoryId()) &&
                            dist.getVacancyCount() != null && dist.getVacancyCount() > 0);

            if (hasDisabilityVacancy) {
                relaxationCategories.add(AppConstants.PWD_CATEGORY);
            }
        }

        return relaxationCategories;
    }

    /**
     * Adds special relaxation categories based on candidate profile and position flags.
     *
     * - EX_SERVICEMAN: if candidate profile exServiceman = true
     * - WIDOWS / DIVORCED_WOMEN / JSW: if position isAgeRelWdsWomen = true AND candidate is female
     *   with matching marital status
     * - RIOT_VICTIM_FAMILY: if position isAgeRelRiotVictimFamily = true AND candidate profile
     *   riotVictimFamily = true
     */
    private void addSpecialRelaxations(
            CandidateProfileEntity candidateProfile,
            List<String> relaxationCategories,
            JobPositionsEntity position) {

        // Check for ex-serviceman (not position-gated)
        if (Boolean.TRUE.equals(candidateProfile.getExServiceman())) {
            relaxationCategories.add(AppConstants.EX_SERVICEMAN_CATEGORY);
        }

        // Check for widow / divorced / judicially separated women (position-gated)
        if (Boolean.TRUE.equals(position.getIsAgeRelWdsWomen())) {
            UUID genderId = candidateProfile.getGenderId();
            UUID maritalStatusId = candidateProfile.getMaritalStatusId();

            if (genderId != null && maritalStatusId != null) {
                Optional<GenderMasterEntity> gender = genderMasterRepository.findById(genderId);
                Optional<MaritalStatusMasterEntity> maritalStatus = maritalStatusMasterRepository.findById(maritalStatusId);

                if (gender.isPresent() && AppConstants.GENDER_FEMALE.equalsIgnoreCase(gender.get().getGender())) {
                    if (maritalStatus.isPresent()) {
                        String status = maritalStatus.get().getMaritalStatus();
                        if (AppConstants.MARITAL_STATUS_WIDOW.equalsIgnoreCase(status)) {
                            relaxationCategories.add(AppConstants.MARITAL_STATUS_WIDOW_CATEGORY);
                        } else if (AppConstants.MARITAL_STATUS_DIVORCED.equalsIgnoreCase(status)) {
                            relaxationCategories.add(AppConstants.MARITAL_STATUS_DIVORCED_WOMEN_CATEGORY);
                        } else if (AppConstants.MARITAL_STATUS_JUDICIALLY_SEPARATED.equalsIgnoreCase(status)) {
                            relaxationCategories.add(AppConstants.MARITAL_STATUS_JSW_CATEGORY);
                        }
                    }
                }
            }
        }

        // Check for 1984 riot victim family (position-gated)
        if (Boolean.TRUE.equals(position.getIsAgeRelRiotVictimFamily())
                && Boolean.TRUE.equals(candidateProfile.getRiotVictimFamily())) {
            relaxationCategories.add(AppConstants.RIOT_VICTIM_FAMILY_CATEGORY);
        }
    }

    /**
     * Calculates total experience from multiple work experience records using a day-based approach.
     *
     * Algorithm:
     * 1. Sum the total number of days across all eligible experience periods.
     * 2. Convert total days into years, months, and remaining days for display.
     *
     * @param workExperiences List of work experience entities
     * @param position Job position
     * @return ExperienceResult with breakdown
     */
    private ExperienceResult calculateTotalExperience(List<WorkExperienceEntity> workExperiences, JobPositionsEntity position) {
        if (workExperiences == null || workExperiences.isEmpty()) {
            return new ExperienceResult(0, 0, 0, 0);
        }

        LocalDate cutoffDate = position.getCutoffDate();
        long totalMonths = 0;
        long totalDays   = 0;

        for (WorkExperienceEntity exp : workExperiences) {
            LocalDate start = exp.getFromDate();
            LocalDate end   = exp.getToDate();

            if (end == null && Boolean.TRUE.equals(exp.getIsPresentlyWorking())) {
                end = (cutoffDate != null) ? cutoffDate : LocalDate.now();
                log.debug("Experience is ongoing, using {} as end", end);
            }

            if (start == null || end == null || !end.isAfter(start)) continue;

            if (cutoffDate != null && end.isAfter(cutoffDate)) {
                end = cutoffDate;
            }

            if (!end.isAfter(start)) continue;

            // Step 1: decompose each period into calendar months + remaining days
            // end.plusDays(1) makes the end date inclusive (last day worked counts)
            Period period = Period.between(start, end.plusDays(1));
            totalMonths += (long) period.getYears() * 12 + period.getMonths();
            totalDays   += period.getDays();

            log.debug("Experience period: {} to {} (inclusive) = {}y {}m {}d",
                    start, end, period.getYears(), period.getMonths(), period.getDays());
        }

        // Step 3 & 4: normalize accumulated hanging days into months (30 days = 1 month)
        totalMonths += totalDays / 30;
        int remainingDays        = (int) (totalDays % 30);
        int totalCompletedMonths = (int) totalMonths;
        int years                = totalCompletedMonths / 12;
        int months               = totalCompletedMonths % 12;

        log.debug("Experience total — {}m {}d → {}y {}m {}d",
                totalCompletedMonths, remainingDays, years, months, remainingDays);

        return new ExperienceResult(years, months, remainingDays, totalCompletedMonths);
    }

    /**
     * Converts months to experience breakdown (for displaying job requirements)
     *
     * @param months Total months required
     * @return ExperienceResult with breakdown
     */
    private ExperienceResult convertMonthsToExperience(int months) {
        return new ExperienceResult(months / 12, months % 12, 0, months);
    }

    /**
     * Holds the result of an experience calculation.
     *
     * <p>Fields are pre-computed from {@code totalCompletedMonths} and {@code days}:
     * <ul>
     *   <li>{@code years  = totalCompletedMonths / 12}</li>
     *   <li>{@code months = totalCompletedMonths % 12}</li>
     *   <li>{@code days   = remaining days after normalization (display only, never used for comparison)}</li>
     * </ul>
     *
     * <p>Eligibility comparison must always use {@code totalCompletedMonths >= requiredMonths}.
     * Days are a display-only helper.
     */
    @Data
    @AllArgsConstructor
    @Builder
    private static class ExperienceResult {
        private int years;
        private int months;
        private int days;
        private int totalCompletedMonths;

        /**
         * Human-readable breakdown, e.g. "2 years, 3 months, 14 days".
         * Shows only non-zero components; returns "0 months" for zero experience.
         */
        String formatFull() {
            StringBuilder result = new StringBuilder();
            if (years > 0) result.append(years).append(years == 1 ? " year" : " years");
            if (months > 0) {
                if (result.length() > 0) result.append(", ");
                result.append(months).append(months == 1 ? " month" : " months");
            }
            if (days > 0) {
                if (result.length() > 0) result.append(", ");
                result.append(days).append(days == 1 ? " day" : " days");
            }
            return result.length() > 0 ? result.toString() : "0 months";
        }

        /**
         * Years and months only (used for displaying job requirements).
         * e.g. "2 years, 6 months"
         */
        String formatYearsMonths() {
            StringBuilder result = new StringBuilder();
            if (years > 0) result.append(years).append(years == 1 ? " year" : " years");
            if (months > 0) {
                if (result.length() > 0) result.append(", ");
                result.append(months).append(months == 1 ? " month" : " months");
            }
            return result.length() > 0 ? result.toString() : "0 months";
        }
    }

    // ======================== OLD EDUCATION-BASED EXPERIENCE — COMMENTED OUT ========================
    // private JobEligibilityValidationResponse.ExperienceValidation validateEducationBasedExperience(
    //         UUID candidateId, JobPositionsEntity position) { ... }
    // private Set<UUID> getCandidateEducationLevels(UUID candidateId) { ... }
    // private String formatEducationBasedRequirements(Map<UUID, Integer> requirements) { ... }
    // ======================== END OLD EDUCATION-BASED EXPERIENCE ========================

    // ================== Experience filtering & post-qualification validation ==================

    /**
     * Filters out ineligible work experiences:
     * <ul>
     *   <li>Experiences containing "clerk", "peon", or "clerical" in role or postHeld</li>
     *   <li>Non-presently-working experiences with a total duration of 180 days or less</li>
     * </ul>
     */
    private List<WorkExperienceEntity> filterEligibleExperiences(
            List<WorkExperienceEntity> workExperiences,
            JobPositionsEntity position) {

        if (workExperiences == null || workExperiences.isEmpty()) {
            return Collections.emptyList();
        }

        return workExperiences.stream()
                .filter(exp -> {
                    // Exclude experiences with clerk/peon/clerical in role or postHeld
                    if (containsExcludedKeyword(exp.getRole()) || containsExcludedKeyword(exp.getPostHeld())) {
                        log.debug("Excluding experience at {} — excluded role/post keyword",
                                exp.getOrganizationName());
                        return false;
                    }

                    // Exclude non-presently-working experiences whose inclusive duration is 6 months or less.
                    // Inclusive means the end date (last day worked) is counted.
                    // Exclude if end < start + 6 months  (i.e., inclusive duration < 6m + 1d, i.e., ≤ 6m exactly).
                    // Example: Jan 1 → Jun 30 = 6m inclusive → excluded
                    //          Jan 1 → Jul 1  = 6m 1d inclusive → included
                    if (!Boolean.TRUE.equals(exp.getIsPresentlyWorking())) {
                        LocalDate start = exp.getFromDate();
                        LocalDate end = exp.getToDate();
                        if (start != null && end != null) {
                            if (end.isBefore(start.plusMonths(6))) {
                                log.debug("Excluding experience at {} — inclusive duration is 6 months or less",
                                        exp.getOrganizationName());
                                return false;
                            }
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Checks whether a text field contains any excluded keyword (case-insensitive).
     */
    private boolean containsExcludedKeyword(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String lowerText = text.toLowerCase();
        return EXCLUDED_ROLE_KEYWORDS.stream().anyMatch(lowerText::contains);
    }

    /**
     * Post-qualification experience validation.
     *
     * <p>Iterates ALL education groups from the position's mandatoryEduRulesJson.
     * For each fulfilled group, determines the last education end-date among the
     * matched candidate records, then counts only the work experience that falls
     * after that date (already filtered for 6-month and excluded-role rules).
     * The post-qual experience is compared against the job's required months
     * (flat or education-level-wise).
     *
     * @return true if at least one education group + post-qual experience combo satisfies
     *         the requirement, or true if no group-based education rules exist.
     */
    // ======================== OLD POST-QUAL EXPERIENCE (STANDALONE) — COMMENTED OUT ========================
    // private boolean validatePostQualificationExperience(
    //         UUID candidateId, JobPositionsEntity position,
    //         List<WorkExperienceEntity> filteredExperiences) { ... }
    // ======================== END OLD POST-QUAL EXPERIENCE ========================

    /**
     * For a fulfilled education group, returns the latest endDate among the matched
     * candidate education records. For each condition, picks the matching education
     * with the earliest endDate (most favourable to candidate), then takes the max
     * across all conditions.
     *
     * @return the latest endDate, or null if the group is not fulfilled.
     */
    private LocalDate findFulfilledGroupLastEndDate(
            JsonNode group,
            List<EducationEntity> candidateEducations,
            Map<UUID, String> qualificationMap,
            Map<UUID, String> qualificationCodeMap,
            Map<UUID, Integer> qualificationLevelScoreMap,
            Map<UUID, String> qualificationLevelNameMap,
            Map<UUID, String> specializationMap,
            Map<UUID, String> educationTypeMap,
            List<String> masterBoardQualCodes,
            boolean candidateHasDiploma) {

        if (!group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) return null;
        JsonNode conditions = group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS);
        if (!conditions.isArray() || conditions.size() == 0) return null;

        LocalDate latestEndDate = null;

        for (JsonNode condition : conditions) {
            // Pick the matching education with the earliest endDate (most favourable)
            Optional<EducationEntity> matchedEdu = candidateEducations.stream()
                    .filter(candEdu -> matchesEducationCondition(
                            candEdu, candidateEducations,condition, qualificationMap, qualificationCodeMap,
                            qualificationLevelScoreMap, qualificationLevelNameMap,
                            specializationMap, educationTypeMap, masterBoardQualCodes, candidateHasDiploma))
                    .filter(candEdu -> candEdu.getEndDate() != null)
                    .min(Comparator.comparing(EducationEntity::getEndDate));

            if (matchedEdu.isEmpty()) return null; // Condition not met → group not fulfilled

            LocalDate endDate = matchedEdu.get().getEndDate();
            if (latestEndDate == null || endDate.isAfter(latestEndDate)) {
                latestEndDate = endDate;
            }
        }

        return latestEndDate;
    }

    /**
     * Calculates total experience from filtered experiences that fall after a given date.
     * For experiences that started before the date but ended after, only the portion
     * after the date is counted.
     */
    private ExperienceResult calculatePostQualExperience(
            List<WorkExperienceEntity> filteredExperiences,
            LocalDate afterDate,
            JobPositionsEntity position) {

        if (filteredExperiences == null || filteredExperiences.isEmpty() || afterDate == null) {
            return new ExperienceResult(0, 0, 0, 0);
        }

        LocalDate cutoffDate = position.getCutoffDate();
        long totalMonths = 0;
        long totalDays   = 0;

        for (WorkExperienceEntity exp : filteredExperiences) {
            LocalDate start = exp.getFromDate();
            LocalDate end   = exp.getToDate();

            if (end == null && Boolean.TRUE.equals(exp.getIsPresentlyWorking())) {
                end = (cutoffDate != null) ? cutoffDate : LocalDate.now();
            }

            if (start == null || end == null || !end.isAfter(start)) continue;

            if (cutoffDate != null && end.isAfter(cutoffDate)) {
                end = cutoffDate;
            }

            // Clamp start to afterDate so only post-qualification portion is counted
            if (start.isBefore(afterDate)) {
                start = afterDate;
            }

            if (!end.isAfter(start)) continue;

            // Step 1: decompose each period into calendar months + remaining days
            // end.plusDays(1) makes the end date inclusive (last day worked counts)
            Period period = Period.between(start, end.plusDays(1));
            totalMonths += (long) period.getYears() * 12 + period.getMonths();
            totalDays   += period.getDays();
        }

        // Step 3 & 4: normalize accumulated hanging days into months (30 days = 1 month)
        totalMonths += totalDays / 30;
        int remainingDays        = (int) (totalDays % 30);
        int totalCompletedMonths = (int) totalMonths;
        int years                = totalCompletedMonths / 12;
        int months               = totalCompletedMonths % 12;

        return new ExperienceResult(years, months, remainingDays, totalCompletedMonths);
    }

    /**
     * Finds the minimum required experience months for an education group by looking up
     * each condition's qualification level in the edu-wise requirements map.
     *
     * @return the minimum required months among matching levels, or -1 if no match found.
     */
    private int findMinRequiredMonthsForGroup(
            JsonNode group,
            Map<UUID, UUID> qualToLevelMap,
            Map<UUID, Integer> eduWiseRequirements) {

        if (!group.has(AppConstants.JOB_ELIGIBILITY_CONDITIONS)) return -1;
        JsonNode conditions = group.get(AppConstants.JOB_ELIGIBILITY_CONDITIONS);

        int minMonths = Integer.MAX_VALUE;
        boolean foundMatch = false;

        for (JsonNode condition : conditions) {
            if (!condition.has(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ) || !isValidRequirement(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText())) {
                continue;
            }
            UUID qualId = UUID.fromString(condition.get(AppConstants.JOB_ELIGIBILITY_QUALIFICATION ).asText());
            UUID levelId = qualToLevelMap.get(qualId);
            if (levelId != null && eduWiseRequirements.containsKey(levelId)) {
                int months = eduWiseRequirements.get(levelId);
                if (months < minMonths) {
                    minMonths = months;
                }
                foundMatch = true;
            }
        }

        return foundMatch ? minMonths : -1;
    }

    /**
     * Checks whether the current candidate is eligible to apply for a job position in a specific state.
     *
     * Two sequential checks are performed:
     *   1. Age eligibility — state_age_validation_passed from age_relaxation_application
     *   2. Local language proficiency — can_read, can_write, can_speak from languages_known
     *
     * @param candidateId UUID of the logged-in candidate
     * @param positionId  UUID of the job position
     * @param stateId     UUID of the state the candidate wants to apply for
     * @return ApiResponse&lt;Boolean&gt; with data=true on full pass, data=false + message on failure
     */
    @Transactional(readOnly = true)
    public ApiResponse<Boolean> checkStateEligibility(UUID candidateId, UUID positionId, UUID stateId, UUID cityId, UUID languageId) {
        log.info("Checking state eligibility for candidateId={}, positionId={}, stateId={}, cityId={}, languageId={}",
                candidateId, positionId, stateId, cityId, languageId);

        // ── 1. State-level eligibility gate from persisted validation flags ─────────────────────
        Optional<AgeRelaxationApplicationEntity> stateValidationRecord = ageRelaxationApplicationRepository
                .findByCandidateIdAndPositionIdAndStateIdAndCityId(candidateId, positionId, stateId, cityId);

        boolean ageValidationPassed = stateValidationRecord
                .map(AgeRelaxationApplicationEntity::getStateAgeValidationPassed)
                .orElse(Boolean.FALSE);
        boolean vacancyValidationPassed = stateValidationRecord
                .map(AgeRelaxationApplicationEntity::getStateVacancyValidationPassed)
                .orElse(Boolean.FALSE);

        if (!ageValidationPassed || !vacancyValidationPassed) {
            log.info("State eligibility gate failed for candidateId={}, positionId={}, stateId={}, cityId={}, agePassed={}, vacancyPassed={}",
                    candidateId, positionId, stateId, cityId, ageValidationPassed, vacancyValidationPassed);
            return ApiResponse.ok(Boolean.FALSE,
                    "No vacancies available for your reservation category for the selected state.");
        }

        // ── 2. Local language proficiency check ──────────────────────────────────
        JobPositionsEntity position = positionsRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Job position not found: " + positionId));

        // If local-language proficiency is not required for this position,
        // state-age qualification is sufficient to pass this API.
        if (!Boolean.TRUE.equals(position.getIsProficientInLocalLanguage())) {
            log.info("Skipping language proficiency check (isProficientInLocalLanguage=false) for candidateId={}, positionId={}, stateId={}, cityId={}, languageId={} — passing after state-age check",
                    candidateId, positionId, stateId, cityId, languageId);
            return ApiResponse.ok(Boolean.TRUE, null);
        }

        if (languageId == null) {
            log.info("Skipping language proficiency check (languageId not provided) for candidateId={}, positionId={}, stateId={}, cityId={}",
                    candidateId, positionId, stateId, cityId);
            return ApiResponse.ok(Boolean.FALSE,
                    "Language is required to validate local language proficiency for the selected state.");
        }

        String languageName = languageMasterRepository.findById(languageId)
                .map(LanguageMasterEntity::getLanguageName)
                .orElse("required language");

        Optional<LanguagesKnownEntity> languagesKnownOpt =
                languagesKnownRepository.findByCandidateIdAndLanguageId(candidateId, languageId);

        boolean proficient = languagesKnownOpt.isPresent()
                && Boolean.TRUE.equals(languagesKnownOpt.get().getCanRead())
                && Boolean.TRUE.equals(languagesKnownOpt.get().getCanWrite())
                && Boolean.TRUE.equals(languagesKnownOpt.get().getCanSpeak());

        if (proficient) {
            log.info("State eligibility check passed for candidateId={}, positionId={}, stateId={}, cityId={}, language={}",
                    candidateId, positionId, stateId, cityId, languageName);
            return ApiResponse.ok(Boolean.TRUE, null);
        }

        log.info("Language proficiency check failed for candidateId={}, positionId={}, stateId={}, cityId={}, language={}",
                candidateId, positionId, stateId, cityId, languageName);
        return ApiResponse.ok(Boolean.FALSE,
                "Proficiency (read, write and speak) is needed to apply for the selected state's local language: "
                        + languageName);
    }

    /**
     * Inner class to hold age relaxation details (two-part: reservation + special)
     */
    @Data
    @AllArgsConstructor
    @Builder
    private static class RelaxationDetails {
        private int totalRelaxationYears;
        private int reservationRelaxationYears;
        private int specialRelaxationYears;
        private UUID appliedRelReservationCategoryId;
        private UUID appliedRelSpecialCategoryId;
        private Integer actualAppliedYears;
    }
}
