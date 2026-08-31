package com.bob.commonutil.service;

import com.bob.commonutil.model.BasicDetailsModel;
import com.bob.commonutil.model.CandidateParentModel;
import com.bob.commonutil.model.EducationResponseModel;
import com.bob.commonutil.model.WorkExperienceResponseModel;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CandidateValidationUtil;
import com.bob.db.dto.*;
import com.bob.db.entity.*;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.db.mapper.*;
import com.bob.db.model.CandidateCertificationsResponseModel;
import com.bob.db.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CandidateCommonGetService {
    @Autowired
    private CandidateAddressRepository candidateAddressRepository;

    @Autowired
    private CandidateAddressMapper candidateAddressMapper;

    @Autowired
    private CandidateProfileMapper candidateProfileMapper;

    @Autowired
    private LanguagesKnownMapper languagesKnownMapper;

    @Autowired
    private CandidateDisabilityDetailsMapper candidateDisabilityDetailsMapper;

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private LanguagesKnownRepository languagesKnownRepository;

    @Autowired
    private CandidateDisabilityDetailsRepository candidateDisabilityDetailsRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateDocumentStoreMapper candidateDocumentStoreMapper;

    @Autowired
    private EducationMapper educationMapper;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    @Autowired
    private WorkExperienceMapper workExperienceMapper;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferenceRepository;

    @Autowired
    private CandidateLocationPreferenceMapper candidateLocationPreferenceMapper;

    @Autowired
    private CandidateValidationUtil candidateValidationUtil;

    @Autowired
    private CandidateApplicationDocumentVerificationRepository candidateApplicationDocumentVerificationRepository;

    @Autowired
    private CandidateApplicationDocumentVerificationMapper candidateApplicationDocumentVerificationMapper;

    @Autowired
    private CandidateApplicationsRepository applicationsRepository;

    @Autowired
    private CandidateCertificationsRepository candidateCertificationsRepository;

    @Autowired
    private CandidateCertificationsMapper candidateCertificationsMapper;

    // Address Details
    public CandidateAddressDTO getCandidateAddress(UUID candidateId) {

        Optional<CandidateAddressEntity> optional =
                candidateAddressRepository.findByCandidateId(candidateId);
        if (optional.isPresent()) {
            return candidateAddressMapper.toDTO(optional.get());
        } else {
            throw new ResourceNotFoundException("Candidate address not found.");
        }
    }

    // Profile Details
    @Transactional
    public BasicDetailsModel getCandidateProfileById(UUID candidateId) {

        Optional<CandidateProfileEntity> optional =
                candidateProfileRepository.findByCandidateId(candidateId);

        if (optional.isPresent()) {
            CandidateProfileEntity entity = optional.get();
            CandidateProfileDTO profileDTO = candidateProfileMapper.toDTO(entity);
            List<LanguagesKnownEntity> languagesKnownEntities =
                    languagesKnownRepository.findByCandidateId(candidateId);
            List<LanguagesKnownDTO> languagesKnownDTOS =
                    languagesKnownMapper.toDTOList(languagesKnownEntities);

            List<CandidateDisabilityDetailsEntity> disabilityEntities =
                    candidateDisabilityDetailsRepository.findAllByCandidateId(candidateId);
            List<CandidateDisabilityDetailsDTO> disabilityDTOS =
                    candidateDisabilityDetailsMapper.toDTOList(disabilityEntities);
            return BasicDetailsModel.builder()
                    .candidateProfile(profileDTO)
                    .languagesKnown(languagesKnownDTOS)
                    .disabilityDetails(disabilityDTOS)
                    .build();
        } else {
            throw new ResourceNotFoundException("Candidate profile not found.");
        }

    }

    // Education Details
    public List<EducationResponseModel> getCandidateEducationDetails(UUID candidateId) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        List<EducationEntity> educationEntities = educationRepository.findByCandidateId(candidateId);
        List<UUID> educationIds = educationEntities.stream().map(EducationEntity::getId).toList();
        List<CandidateDocumentStoreEntity> candidateDocuments = candidateDocumentStoreRepository.findByCandidateIdAndDocumentIdentifierIn(candidateId,educationIds);

        Map<UUID, CandidateDocumentStoreDTO> documentMap =
                candidateDocuments.stream()
                        .collect(Collectors.toMap(
                                CandidateDocumentStoreEntity::getDocumentIdentifier,
                                candidateDocumentStoreMapper::toDTO,
                                (existing, replacement) -> existing
                        ));
        List<EducationResponseModel> educationResponseModels;

        return  educationEntities.stream()
                .map(entity -> EducationResponseModel.builder()
                        .education(educationMapper.toDTO(entity))
                        .documentStore(documentMap.get(entity.getId()))
                        .build()
                )
                .toList();
    }

    // work experience details
    public List<WorkExperienceResponseModel> getCandidateExperienceDetails(UUID candidateId) {
        candidateValidationUtil.validateCandidateExistence(candidateId);
        List<WorkExperienceEntity> experienceEntities = workExperienceRepository.findByCandidateId(candidateId, Sort.by(Sort.Direction.DESC,"fromDate"));
        List<UUID> experienceIds = experienceEntities.stream().map(WorkExperienceEntity::getId).toList();
        List<CandidateDocumentStoreEntity> candidateDocuments = candidateDocumentStoreRepository.findByCandidateIdAndDocumentIdentifierIn(candidateId, experienceIds);

        List<WorkExperienceDTO> experienceDTOs = workExperienceMapper.toDTOList(experienceEntities);
        Map<UUID, CandidateDocumentStoreDTO> documentMap = candidateDocuments.stream()
                .collect(Collectors.toMap(
                        CandidateDocumentStoreEntity::getDocumentIdentifier,
                        candidateDocumentStoreMapper::toDTO,
                        (existing, replacement) -> existing
                ));


        return experienceEntities.stream()
                .map(entity -> WorkExperienceResponseModel.builder()
                        .workExperience(workExperienceMapper.toDTO(entity))
                        .documentStore(documentMap.get(entity.getId()))
                        .build()
                )
                .toList();
    }

    // Document Details
    public List<CandidateDocumentStoreDTO> getCandidateDocuments(UUID candidateId) {
        List<CandidateDocumentStoreEntity> documentsEntities = candidateDocumentStoreRepository.findAllByCandidateId(candidateId);
        return candidateDocumentStoreMapper.toDTOList(documentsEntities);
    }


    // get all the details
    public CandidateParentModel getCandidateBasicDetails(UUID candidateId,UUID positionId) {

        JobPositionsEntity entity = positionsRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found."));

        // Resolve cutoff date: requisition-level first, fall back to position-level for old records.
        LocalDate cutoffDate = jobRequisitionsRepository.findById(entity.getRequisitionId())
                .map(req -> req.getCutoffDate())
                .orElse(null);
        if (cutoffDate == null) {
            cutoffDate = entity.getCutoffDate();
        }

        BasicDetailsModel basicDetails = getCandidateProfileById(candidateId);
        LocalDate dob = basicDetails.getCandidateProfile().getDateOfBirth();

        String age = null;
        if (cutoffDate != null) {
            Period period = Period.between(dob, cutoffDate.plusDays(AppConstants.ONE));
            age = period.getYears() + " " + AppConstants.YEARS + " " + period.getMonths() + " " + AppConstants.MONTHS + " " + period.getDays() + " " + AppConstants.DAYS;
        }

        return CandidateParentModel.builder()
                .basicDetails(basicDetails)
                .addressDetails(getCandidateAddress(candidateId))
                .educationDetails( getCandidateEducationDetails(candidateId))
                .documentDetails(getCandidateDocuments(candidateId))
                .experienceDetails(getCandidateExperienceDetails(candidateId))
                .locationPreference(getCandidateLocationPreference(candidateId,positionId))
                .appSpecificDocDetails(getCandidateApplicationDocumentVerificationDetails(candidateId,positionId))
                .candidateCertifications(getCandidateCertifications(candidateId))
                .age(age)
                .build();
    }

    //Get Location Prefrerence(For recruiter)
    public CandidateLocationPreferenceDTO getCandidateLocationPreference(UUID candidateId,UUID positionId){
        Optional<CandidateLocationPreferenceEntity> optional =
                candidateLocationPreferenceRepository.findByCandidateIdAndPositionId(candidateId,positionId);
        if(optional.isPresent()){
            return candidateLocationPreferenceMapper.toDTO(optional.get());
        }else {
            return null;
        }
    }

    public List<CandidateApplicationDocumentVerificationDTO> getCandidateApplicationDocumentVerificationDetails(UUID candidateId,UUID positionId) {
        Optional<CandidateApplicationsEntity> applicationOpt = applicationsRepository.findByCandidateIdAndPositionId(candidateId,positionId);
        if(applicationOpt.isEmpty()){
            return null;
        }
        UUID applicationId = applicationOpt.get().getId();
        List<CandidateApplicationDocumentVerificationDTO> verificationDetails = candidateApplicationDocumentVerificationMapper
                .toDtoList(candidateApplicationDocumentVerificationRepository.findByApplicationId(applicationId));
        return verificationDetails;
    }
    public List<CandidateCertificationsResponseModel> getCandidateCertifications(UUID candidateId) {
        candidateValidationUtil.validateCandidateExistence(candidateId);

        List<CandidateCertificationsEntity> certificationEntities =   candidateCertificationsRepository.findByCandidateId(candidateId);

        List<UUID> certificationIds = certificationEntities.stream().map(CandidateCertificationsEntity::getId).toList();

        List<CandidateDocumentStoreEntity> candidateDocuments =  candidateDocumentStoreRepository.findByCandidateIdAndDocumentIdentifierIn(candidateId, certificationIds);

        Map<UUID, CandidateDocumentStoreDTO> documentMap =
                candidateDocuments.stream()
                        .collect(Collectors.toMap(
                                CandidateDocumentStoreEntity::getDocumentIdentifier,
                                candidateDocumentStoreMapper::toDTO,
                                (existing, replacement) -> existing
                        ));

        return certificationEntities.stream()
                .map(entity ->
                        CandidateCertificationsResponseModel.builder()
                                .certifications(candidateCertificationsMapper.toDto(entity))
                                .documentStore(documentMap.get(entity.getId()))
                                .build()
                )
                .toList();
    }

}
