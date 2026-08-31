package com.bob.candidateportal.service;

import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.candidateportal.model.*;
import com.bob.commonutil.model.BasicDetailsModel;
import com.bob.commonutil.model.CandidateParentModel;
import com.bob.commonutil.model.EducationResponseModel;
import com.bob.commonutil.model.WorkExperienceResponseModel;
import com.bob.commonutil.service.*;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.db.dto.*;
import com.bob.db.entity.*;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.thymeleaf.context.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AppliedJobsService {

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private JobPositionsMapper jobPositionsMapper;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;


    @Autowired
    private CandidateApplicationsMapper candidateApplicationMapper;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private JobRequisitionMapper jobRequisitionMapper;

    @Autowired
    private MasterPositionsMapper masterPositionsMapper;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private GenderMasterRepository genderMasterRepository;

    @Autowired
    private ReligionMasterRepository religionMasterRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private MaritalStatusMasterRepository maritalStatusMasterRepository;

    @Autowired
    private EducationQualificationsRepository educationQualificationsRepository;

    @Autowired
    private SpecializationMasterRepository specializationMasterRepository;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private PincodeRepository pincodeRepository;

    @Autowired
    private ZonalStatesRepository zonalStatesRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private AzureBlobStorageService azureBlobStorageService;

    @Autowired
    private CandidateApplicationDocumentVerificationRepository candidateApplicationDocumentVerificationRepository;

    @Autowired
    private CandidateApplicationDocumentVerificationMapper candidateApplicationDocumentVerificationMapper;
  
    @Autowired
    private PdfConverterService pdfConverterService;


    @Autowired
    private CandidateCommonGetService candidateCommonGetService;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private CandidateProfileMapper candidateProfileMapper;

    @Autowired
    private LanguageMasterRepository languageMasterRepository;

    @Autowired
    private CandidateLocationPreferenceMapper candidateLocationPreferenceMapper;

    @Autowired
    private WrittenExamConfigurationRepository writtenExamConfigurationRepository;


    @Autowired
    private ExservicemanCategoryRepository exservicemanCategoryRepository;

    @Transactional(readOnly = true)
    public Page<AppliedJobResponseModel> getAppliedJobsWithSearch(
            UUID candidateId,
            String searchTerm,
            int page,
            int size
    ) {


        Pageable pageable = PageRequest.of(page, size);

        List<CandidateApplicationsEntity> applications = candidateApplicationsRepository.findByCandidateIdIgnoringPaymentStatus(candidateId);

        if (applications.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<UUID, CandidateApplicationsEntity> applicationMap =
                applications.stream()
                        .collect(Collectors.toMap(
                                CandidateApplicationsEntity::getPositionId,
                                a -> a
                        ));

        List<UUID> positionIds = new ArrayList<>(applicationMap.keySet());

        Page<JobPositionsEntity> positionsPage =
                positionsRepository.findByPositionIdsWithSearch(
                        positionIds,
                        searchTerm,
                        pageable
                );
        List<UUID> fetchedPositionIds = positionsPage.stream().map(JobPositionsEntity::getId).toList();

        Map<UUID, JobRequisitionsDTO> requisitionMap =
                jobRequisitionsRepository.findAllById(
                                positionsPage.stream()
                                        .map(JobPositionsEntity::getRequisitionId)
                                        .collect(Collectors.toSet())
                        )
                        .stream()
                        .map(jobRequisitionMapper::toDTO)
                        .collect(Collectors.toMap(
                                JobRequisitionsDTO::getId,
                                dto -> dto
                        ));

        Map<UUID, MasterPositionsDTO> masterPositionMap =
                masterPositionsRepository.findAllById(
                                positionsPage.stream()
                                        .map(JobPositionsEntity::getMasterPositionId)
                                        .collect(Collectors.toSet())
                        )
                        .stream()
                        .map(masterPositionsMapper::toDTO)
                        .collect(Collectors.toMap(
                                MasterPositionsDTO::getId,
                                dto -> dto
                        ));

        Map<UUID,CandidateLocationPreferenceDTO> locationPreferenceMap=candidateLocationPreferencesRepository.findByCandidateIdAndPositionIdIn(candidateId,positionIds)
                .stream()
                .map(candidateLocationPreferenceMapper::toDTO)
                .collect(Collectors.toMap(
                        CandidateLocationPreferenceDTO::getPositionId,
                        dto->dto
                ));
        List<WrittenExamConfigurationEntity> writtenExamConfigurationEntityList = writtenExamConfigurationRepository.findByPositionIdIn(fetchedPositionIds);
        Map<UUID, WrittenExamConfigurationEntity> writtenExamConfigMap = writtenExamConfigurationEntityList.stream()
                .collect(Collectors.toMap(
                        WrittenExamConfigurationEntity::getPositionId,
                        config -> config
                ));
        return positionsPage.map(position ->
                AppliedJobResponseModel.builder()
                        .positions(jobPositionsMapper.toDto(position))
                        .candidateApplication(
                                candidateApplicationMapper.toDTO(
                                        applicationMap.get(position.getId())
                                )
                        )
                        .requisitions(
                                requisitionMap.get(position.getRequisitionId())
                        )
                        .masterPositions(
                                masterPositionMap.get(position.getMasterPositionId())
                        )
                        .candidateLocationPreference(
                                locationPreferenceMap.get(position.getId())
                        )
                        .isWrittenExamConfigured(writtenExamConfigMap.containsKey(position.getId()))
                        .build()
        );
    }




    @Transactional
    public byte[] generateApplication(UUID candidateId, UUID applicationId){

        CandidateApplicationsEntity candidateApplications=candidateApplicationsRepository.findById(applicationId).orElseThrow(()->new ResourceNotFoundException("Application not found"));
        CandidateParentModel candidateParentModel=candidateCommonGetService.getCandidateBasicDetails(candidateId,candidateApplications.getPositionId());
        UUID genderId=candidateParentModel.getBasicDetails().getCandidateProfile().getGenderId();
        String gender=genderMasterRepository.findById(genderId).orElseThrow(()->new ResourceNotFoundException("Gender not found!")).getGender();
        String religion=religionMasterRepository.findById(candidateParentModel.getBasicDetails().getCandidateProfile().getReligionId()).orElseThrow(()->new ResourceNotFoundException("Religion not Found!")).getReligion();
        String categoryName=reservationCategoriesRepository.findById(candidateParentModel.getBasicDetails().getCandidateProfile().getReservationCategoryId()).orElseThrow(()->new ResourceNotFoundException("Category not found!")).getCategoryName();
        String nationality=countryRepository.findById(candidateParentModel.getBasicDetails().getCandidateProfile().getNationality()).orElseThrow(()->new ResourceNotFoundException("Nationality not found!")).getCountryName();
        String maritalStatus=maritalStatusMasterRepository.findById(candidateParentModel.getBasicDetails().getCandidateProfile().getMaritalStatusId()).orElseThrow(()->new ResourceNotFoundException("Marital Status not found!")).getMaritalStatus();
        String fullName= commonUtilityProvider.buildFullName(candidateProfileMapper.toEntity(candidateParentModel.getBasicDetails().getCandidateProfile()));

        List<UUID> qualificationIds =
                candidateParentModel.getEducationDetails()
                        .stream()
                        .map(EducationResponseModel::getEducation)
                        .map(EducationDTO::getEducationQualificationsId)
                        .distinct()
                        .toList();

        Map<UUID, EducationQualificationsEntity> educationMap =
                educationQualificationsRepository
                        .findAllById(qualificationIds)
                        .stream()
                        .collect(Collectors.toMap(
                                EducationQualificationsEntity::getId,
                                Function.identity()
                        ));
        List<DocumentTypesEntity> documentTypes = documentTypesRepository.findAll();
        Map<UUID,String> educationLevelMap = documentTypes
                .stream().collect(Collectors.toMap(
                        (DocumentTypesEntity::getId),
                        (DocumentTypesEntity::getDocumentName)
                ));

        List<UUID> specializationIds=candidateParentModel.getEducationDetails()
                .stream()
                .map(EducationResponseModel::getEducation)
                .map(EducationDTO::getSpecializationId)
                .distinct()
                .toList();
        Map<UUID, String> specializationMap =
                specializationMasterRepository
                        .findAllById(specializationIds)
                        .stream()
                        .collect(Collectors.toMap(
                                SpecializationMasterEntity::getId,
                                SpecializationMasterEntity::getSpecializationName
                        ));

        Map<UUID, String> exservicemanCategoryMap = exservicemanCategoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        ExservicemanCategoryEntity::getId,
                        ExservicemanCategoryEntity::getExsCategoryName
                ));

        List<EducationModel> educationModels=candidateParentModel.getEducationDetails().stream()
                .map(EducationResponseModel::getEducation)
                .sorted((edu1,edu2)->{
                    if (edu1.getStartDate() == null && edu2.getStartDate() == null) return 0;
                    if (edu1.getStartDate() == null) return 1;
                    if (edu2.getStartDate() == null) return -1;

                    return edu2.getStartDate().compareTo(edu1.getStartDate());
                })
                .map(
                educationDTO -> {
                    EducationModel educationModel=EducationModel.builder()
                            .educationLevel(
                                    educationLevelMap.get(
                                            educationMap.get(
                                                    educationDTO.getEducationQualificationsId()
                                            ).getLevelId()
                                    )
                            )
                            .school(educationDTO.getInstitutionName())
                            .university(educationDTO.getUniversityName())
                            .degree(
                                    educationMap.get(
                                            educationDTO.getEducationQualificationsId()
                                    ).getQualificationName()
                            )
                            .specialization(
                                    specializationMap.get(
                                            educationDTO.getSpecializationId()
                                    )
                            )
                            .fromDate(
                                    educationDTO.getStartDate() != null
                                            ? educationDTO.getStartDate().format(AppConstants.DD_MM_YYYY)
                                            : null
                            )
                            .toDate(
                                    educationDTO.getEndDate() != null
                                            ? educationDTO.getEndDate().format(AppConstants.DD_MM_YYYY)
                                            : null
                            )
                            .percentage(
                                    educationDTO.getPercentage() != null
                                            ? educationDTO.getPercentage().setScale(2, RoundingMode.DOWN).toString()
                                            : null
                            )
                            .build();
                    return educationModel;
                }
        ).toList();



        List<WorkExperienceModel> workExperienceModels=candidateParentModel.getExperienceDetails().stream()
                .map(WorkExperienceResponseModel::getWorkExperience)
                .map(workExperienceDTO -> {
                    WorkExperienceModel workExperienceModel=WorkExperienceModel.builder()
                            .organization(workExperienceDTO.getOrganizationName())
                            .role(workExperienceDTO.getRole())
                            .post(workExperienceDTO.getPostHeld())
                            .fromDate(
                                    workExperienceDTO.getFromDate()
                                            .format(AppConstants.DD_MM_YYYY)
                            )
                            .toDate(
                                    workExperienceDTO.getToDate()!=null?workExperienceDTO.getToDate().format(AppConstants.DD_MM_YYYY):null
                            )
                            .duration(workExperienceDTO.getMonthsOfExp().toString())
                            .description(workExperienceDTO.getWorkDescription())
                            .build();
                    return workExperienceModel;
                }).toList();

        CandidateLocationPreferenceDTO candidateLocationPreferenceDTO = candidateParentModel.getLocationPreference();
        UUID examCenterId = candidateLocationPreferenceDTO != null ? candidateLocationPreferenceDTO.getInterviewCenter() : null;
        UUID statePref1Id = candidateLocationPreferenceDTO != null ? candidateLocationPreferenceDTO.getStatePreference1() : null;
        UUID statePref2Id = candidateLocationPreferenceDTO != null ? candidateLocationPreferenceDTO.getStatePreference2() : null;
        UUID statePref3Id = candidateLocationPreferenceDTO != null ? candidateLocationPreferenceDTO.getStatePreference3() : null;
        UUID locPref1Id = candidateLocationPreferenceDTO != null ? candidateLocationPreferenceDTO.getLocationPreference1() : null;
        UUID locPref2Id =  candidateLocationPreferenceDTO != null ? candidateLocationPreferenceDTO.getLocationPreference2() : null;
        UUID locPref3Id =  candidateLocationPreferenceDTO != null ? candidateLocationPreferenceDTO.getLocationPreference3() : null;
        JsonNode dynamicFields = candidateLocationPreferenceDTO != null ? candidateLocationPreferenceDTO.getDynamicFormData()  :null;

        CandidateAddressDTO candidateAddressDTO = candidateParentModel.getAddressDetails();

        //current address
        StateEntity currStateEntity = stateRepository.findById(candidateAddressDTO.getStateId()).orElseThrow(()->new ResourceNotFoundException("Current State Not found"));
        DistrictEntity currDistrictEntity = districtRepository.findById(candidateAddressDTO.getDistrictId()).orElseThrow(()->new ResourceNotFoundException("Current District Not found"));


        //permanent address
        StateEntity premanentStateEntity = stateRepository.findById(candidateAddressDTO.getPermanentStateId()).orElseThrow(()->new ResourceNotFoundException("Permanent State Not found"));
        DistrictEntity premanentDistrictEntity = districtRepository.findById(candidateAddressDTO.getPermanentDistrictId()).orElseThrow(()->new ResourceNotFoundException("Permanent District Not found"));


        BigDecimal currentCtc= candidateParentModel.getExperienceDetails()!=null && !(candidateParentModel.getExperienceDetails().isEmpty()) ?
                candidateParentModel.getExperienceDetails().get(0).getWorkExperience().getCurrentCtc() : null;
        BigDecimal expectedCtc=candidateLocationPreferenceDTO !=null ? candidateLocationPreferenceDTO.getExpectedCtc() : null;



        String formattedCurrentCtc =  commonUtilityProvider.formatToIndianCurrency(currentCtc);
        String formattedExpectedCtc = commonUtilityProvider.formatToIndianCurrency(expectedCtc) ;

        JobPositionsEntity jobPositionsEntity= positionsRepository.findById(candidateApplications.getPositionId())
                .orElseThrow(()->new ResourceNotFoundException("Position not found!"));
        MasterPositionsEntity masterPositions=masterPositionsRepository.findById(jobPositionsEntity.getMasterPositionId())
                .orElseThrow(()->new ResourceNotFoundException("Master Position not found!"));
        JobRequisitionsEntity jobRequisitionsEntity=jobRequisitionsRepository.findById(jobPositionsEntity.getRequisitionId())
                .orElseThrow(()->new ResourceNotFoundException("Requisition not found!"));
        JsonNode locationPreferenceDynamicFields = jobPositionsEntity.getDynamicFields();

        // get the required zonal state Ids
        Set<UUID> stateIds = new HashSet<>();

            if (candidateLocationPreferenceDTO!= null && candidateLocationPreferenceDTO.getStatePreference1() != null) {
                stateIds.add(candidateLocationPreferenceDTO.getStatePreference1());
            }
            if (candidateLocationPreferenceDTO!= null && candidateLocationPreferenceDTO.getStatePreference2() != null) {
                stateIds.add(candidateLocationPreferenceDTO.getStatePreference2());
            }
            if (candidateLocationPreferenceDTO!= null && candidateLocationPreferenceDTO.getStatePreference3() != null) {
                stateIds.add(candidateLocationPreferenceDTO.getStatePreference3());
            }



        Map<UUID, String> stateMap =
                        stateRepository.findAllById(
                                        stateIds
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                StateEntity::getId,
                                StateEntity::getStateName
                        ));

        //map interviewCentreId,interviewCentreName
        String interviewDisplayName = Optional.ofNullable(examCenterId)
                .flatMap(interviewCentresRepository::findById)
                .map(InterviewCentresEntity::getDisplayName)
                .orElse(null);

        Map<UUID,String> cityMap = cityRepository.findAll()
                .stream().collect(Collectors.toMap(
                        CityEntity::getId,
                        CityEntity::getCityName
                ));

        List<UUID> languagesIds = Optional.ofNullable(candidateParentModel)
                .map(CandidateParentModel::getBasicDetails)
                .map(BasicDetailsModel::getLanguagesKnown)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .map(LanguagesKnownDTO::getLanguageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        // safely add local language
        UUID localLangId = candidateLocationPreferenceDTO != null
                ? candidateLocationPreferenceDTO.getLocalLanguageId()
                : null;

        if (localLangId != null) {
            languagesIds.add(localLangId);
        }

        List<LanguageMasterEntity> languageMasterEntities = languageMasterRepository.findAllById(languagesIds);
        Map<UUID, String> languageMap = languageMasterEntities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        LanguageMasterEntity::getId,
                        LanguageMasterEntity::getLanguageName
                ));


        Map<String,String> additionalFieldsMap = getDynamicFieldsFromJson(locationPreferenceDynamicFields,dynamicFields);

        UUID photoDocId = documentTypes.stream().filter((documentTypesEntity)->AppConstants.PHOTO_DOC_CODE.equals(documentTypesEntity.getDocCode())).findFirst().orElseThrow(()->new RuntimeException("Document type not found")).getId();
        UUID signDocId = documentTypes.stream().filter((documentTypesEntity)->AppConstants.SIGN_DOC_CODE.equals(documentTypesEntity.getDocCode())).findFirst().orElseThrow(()->new RuntimeException("Document type not found")).getId();
        CandidateApplicationDocumentVerificationEntity photoStoreEntity=candidateApplicationDocumentVerificationRepository.findByCandidateIdAndDocumentIdAndApplicationId(candidateApplications.getCandidateId(),photoDocId,candidateApplications.getId()).orElseThrow(()->new RuntimeException("Document not found"));
        CandidateApplicationDocumentVerificationEntity signStoreEntity=candidateApplicationDocumentVerificationRepository.findByCandidateIdAndDocumentIdAndApplicationId(candidateApplications.getCandidateId(),signDocId,candidateApplications.getId()).orElseThrow(()->new RuntimeException("Document not found"));
        String reqDisplay=jobRequisitionsEntity.getRequisitionCode()+" - "+jobRequisitionsEntity.getRequisitionTitle();
        UUID exservicemanCategoryId = candidateParentModel.getBasicDetails().getCandidateProfile().getExServiceman();
            ApplicantModel applicantModel = ApplicantModel.builder()
                    .requisitionName(reqDisplay)
                    .positionName(masterPositions.getPositionName())
                    .applicationNo(candidateApplications.getApplicationNo())
                    .fullName(fullName)
                    .age(candidateParentModel.getAge())
                    .dateOfBirth(candidateParentModel.getBasicDetails().getCandidateProfile().getDateOfBirth().format(AppConstants.DD_MM_YYYY))
                    .email(candidateParentModel.getBasicDetails().getCandidateProfile().getEmail())
                    .currentAddressLine1(candidateAddressDTO.getAddressLine1())
                    .currentAddressLine2(candidateAddressDTO.getAddressLine2())
                    .currentStateName(currStateEntity.getStateName())
                    .currentDistrictName(currDistrictEntity.getDistrictName())
                    .currentCityName(candidateAddressDTO.getCity())
                    .currentPincode(candidateAddressDTO.getPincode())
                    .permanentAddressLine1(candidateAddressDTO.getPermanentAddressLine1())
                    .permanentAddressLine2(candidateAddressDTO.getPermanentAddressLine2())
                    .permanentStateName(premanentStateEntity.getStateName())
                    .permanentDistrictName(premanentDistrictEntity.getDistrictName())
                    .permanentCityName(candidateAddressDTO.getPermanentCity())
                    .permanentPincode(candidateAddressDTO.getPermanentPincode())
                    .mobile(candidateParentModel.getBasicDetails().getCandidateProfile().getContactNo())
                    .motherName(candidateParentModel.getBasicDetails().getCandidateProfile().getMotherName())
                    .fatherName(candidateParentModel.getBasicDetails().getCandidateProfile().getFatherName())
                    .gender(gender)
                    .religion(religion)
                    .category(categoryName)
                    .caste(candidateParentModel.getBasicDetails().getCandidateProfile().getCommunity()) //TODO: Check if caste is same as community or if it needs to be fetched from another source
                    .exServicemen( exservicemanCategoryId == null ? "Not Applicable" : exservicemanCategoryMap.get(exservicemanCategoryId))
                    .physicalDisability(booleanToYesNo(candidateParentModel.getBasicDetails().getCandidateProfile().getDisability()))
                    .nationality(nationality)
                    .maritalStatus(maritalStatus)
                    .spouseName(candidateParentModel.getBasicDetails().getCandidateProfile().getSpouseName())
                    .twinSibling(booleanToYesNo(candidateParentModel.getBasicDetails().getCandidateProfile().getIsTwin()))
                    .twinSiblingName(candidateParentModel.getBasicDetails().getCandidateProfile().getTwinName())
                    .cibilScore(candidateParentModel.getBasicDetails().getCandidateProfile().getCibilScore().toString())
                    .currentCTC(formattedCurrentCtc)
                    .expectedCTC(formattedExpectedCtc)
                    .examCenter(interviewDisplayName)
                    .socialMediaLink(candidateParentModel.getBasicDetails().getCandidateProfile().getSocialMediaProfileLink())
                    .statePreference1(stateMap.get(statePref1Id))
                    .statePreference2(stateMap.get(statePref2Id))
                    .statePreference3(stateMap.get(statePref3Id))
                    .locationPreference1(cityMap.get(locPref1Id))
                    .locationPreference2(cityMap.get(locPref2Id))
                    .locationPreference3(cityMap.get(locPref3Id))
                    .govtEmployment(booleanToYesNo(candidateParentModel.getBasicDetails().getCandidateProfile().getCentralGovtEmployed()))
                    .lowerPost(booleanToYesNo(candidateParentModel.getBasicDetails().getCandidateProfile().getEmployedInLowerPost()))
                    .riotsFamily(booleanToYesNo(candidateParentModel.getBasicDetails().getCandidateProfile().getRiotVictimFamily()))
                    .religiousMinority(booleanToYesNo(candidateParentModel.getBasicDetails().getCandidateProfile().getMinority()))
                    .publicSector(booleanToYesNo(candidateParentModel.getBasicDetails().getCandidateProfile().getIsPublicSectorUndertaking()))
                    .disciplinaryAction(booleanToYesNo(candidateParentModel.getBasicDetails().getCandidateProfile().getAnyDisciplinaryAction()))
                    .disciplinaryDetails("")
                    .educationList(educationModels)
                    .experienceList(workExperienceModels)
                    .isLocalLanguageStudied(jobPositionsEntity.getIsProficientInLocalLanguage()?booleanToYesNo(candidateLocationPreferenceDTO.getIsLocalLanguageStudied()): null)
                    .localLanguage(languageMap.get(localLangId))
                    .languageProficiency(languageProficiencyBuilder(languageMap,candidateParentModel.getBasicDetails().getLanguagesKnown()))
                    .photoUrl(imageUrlToBase64(photoStoreEntity.getFileUrl()))
                    .signatureUrl(imageUrlToBase64(signStoreEntity.getFileUrl()))
                    .additionalFieldsMap(additionalFieldsMap)
                    .build();
            Context context = new Context();
            Map<String, Object> candidateDetails = new HashMap<>();
            candidateDetails.put("applicant", applicantModel);
            context.setVariables(candidateDetails);
            String html = templateEngine.process("ApplicationForm", context);
            return pdfConverterService.convertHtmlStringToPdf(html);

    }
    public static String booleanToYesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? AppConstants.YES : AppConstants.NO;
    }
    public static String calculateAgeOnDate(LocalDate dob, LocalDate cutoff) {
        Period p = Period.between(dob, cutoff);
        return p.getYears() +" " +AppConstants.YEARS + " "+p.getMonths()+ " " + AppConstants.MONTHS +" "+ p.getDays() +" "+AppConstants.DAYS;
    }
    private String imageUrlToBase64(String imageUrl) {
        try {
            String actualUrl = imageUrl;
            
            // For Azure: if imageUrl starts with '/', it's a blob path, generate SAS URL
            // For E2E: imageUrl is already a complete HTTP URL
            if (imageUrl.startsWith("/")) {
                // Azure blob path: /documents/Candidate/candidate_doc/Photo_uuid.jpg
                // Remove leading slash and generate SAS URL for direct blob access
                String blobPath = imageUrl.substring(1);
                actualUrl = azureBlobStorageService.generateReadSasUrl(blobPath);
//                log.info("Generated Azure SAS URL for image: {}", actualUrl);
            }
            
            try (InputStream in = new URL(actualUrl).openStream()) {
                byte[] bytes = in.readAllBytes();
                return Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
//            log.error("Failed to convert image to base64: {}", imageUrl, e);
            return null;
        }
    }

    private String languageProficiencyBuilder(Map<UUID, String> languageMap, List<LanguagesKnownDTO> languagesKnown) {
        if (languagesKnown == null || languagesKnown.isEmpty()) {
            return "";
        }

        return languagesKnown.stream()
                .filter(Objects::nonNull)
                .map(langDTO -> {
                    String languageName = languageMap.get(langDTO.getLanguageId());

                    if (languageName == null) {
                        return null;
                    }

                    return buildLanguageProficiencyString(languageName, langDTO);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    public String buildLanguageProficiencyString(
            String language,
            LanguagesKnownDTO languagesKnownDTO) {

        List<String> skills = new ArrayList<>();

        if (Boolean.TRUE.equals(languagesKnownDTO.getCanRead())) {
            skills.add("Read");
        }

        if (Boolean.TRUE.equals(languagesKnownDTO.getCanWrite())) {
            skills.add("Write");
        }

        if (Boolean.TRUE.equals(languagesKnownDTO.getCanSpeak())) {
            skills.add("Speak");
        }

        return language + " (" + String.join(", ", skills) + ")";
    }

    public ExamCenterModel saveExamCenter( UUID candidateId, ExamCenterModel examCenterModel) {
        if (!candidateId.equals(examCenterModel.getCandidateId())) {
            throw new IllegalArgumentException("Candidate ID mismatch");
        }
        CandidateApplicationsEntity applicationEntity=candidateApplicationsRepository.findById(examCenterModel.getApplicationId())
                .orElseThrow(()->new ResourceNotFoundException("Application not found"));

        applicationEntity.setExamCenterId(examCenterModel.getExamCenterId());
        candidateApplicationsRepository.save(applicationEntity);
        return examCenterModel;
    }


    public ExamCenterModel getExamCenter(UUID candidateId, UUID applicationId) {
        CandidateApplicationsEntity applicationEntity=candidateApplicationsRepository.findById(applicationId)
                .orElseThrow(()->new ResourceNotFoundException("Application not found"));
        return ExamCenterModel.builder()
                .examCenterId(applicationEntity.getExamCenterId())
                .applicationId(applicationEntity.getId())
                .candidateId(applicationEntity.getCandidateId())
                .build();
    }
    public Map<String, String> getDynamicFieldsFromJson(
            JsonNode jobPositionNode,
            JsonNode candidateLocationNode) {

        Map<String, String> dynamicFieldsMap = new LinkedHashMap<>();

        if (jobPositionNode == null || jobPositionNode.isNull()) {
            return dynamicFieldsMap;
        }

        JsonNode fieldsNode = jobPositionNode.path("fields");

        // Return empty map if fields array is missing or empty
        if (!fieldsNode.isArray() || fieldsNode.isEmpty()) {
            return dynamicFieldsMap;
        }

        for (JsonNode field : fieldsNode) {

            String id = field.path("id").asText(null);
            String label = field.path("label").asText(null);
            String type = field.path("type").asText(null);

            if (id == null || label == null) {
                continue;
            }

            String value = null;

            if (candidateLocationNode != null
                    && !candidateLocationNode.isNull()
                    && candidateLocationNode.has(id)
                    && !candidateLocationNode.get(id).isNull()) {

                value = candidateLocationNode.get(id).asText();
                if ("date".equalsIgnoreCase(type) && value != null && !value.isBlank()) {
                    value = LocalDate.parse(value).format(AppConstants.DD_MM_YYYY);
                }
            }

            dynamicFieldsMap.put(label, value);
        }

        return dynamicFieldsMap;
    }

}
