package com.bob.masterdata.Service;

import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.SalaryBreakUpModel;
import com.bob.commonutil.service.AzureBlobStorageService;
import com.bob.commonutil.service.PdfConverterService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.db.dto.TemplatesDTO;
import com.bob.db.entity.*;
import com.bob.db.mapper.TemplatesMapper;
import com.bob.db.repository.*;
import com.bob.masterdata.Model.OfferLetterTemplateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TemplatesService {

    @Autowired
    private TemplatesRepository templatesRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CandidateAddressRepository candidateAddressRepository;

    @Autowired
    private StateRepository statesRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private PositionsRepository jobPositionRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private DepartmentsRepository departmentsRepository;

    @Autowired
    private CandidateOffersRepository candidateOffersRepository;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private ZonalStatesRepository zonalStateRepository;

    @Autowired
    private TemplatesMapper templatesMapper;

    @Autowired
    private AzureBlobStorageService azureBlobStorageService;


    @Autowired
    private PdfConverterService pdfConverterService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private JobGradeRepository jobGradeRepository;

    @Autowired
    private EmployementTypesRepository employementTypesRepository;

    @Autowired
    private CandidateMeritListRepository candidateMeritListRepository;

    @Autowired
    private CandidateCompensationRepository candidateCompensationRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;


    public List<TemplatesDTO> getAllTemplates() {
        List<TemplatesEntity> templates = templatesRepository.findAll();
        return templatesMapper.toDtoList(templates);
    }


    public OfferLetterTemplateModel getPreviewContent(UUID templateId) {
        TemplatesEntity template = templatesRepository.findById(templateId).orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        byte[] fileContent = azureBlobStorageService.downloadFile(template.getFilePath(),"");
        String htmlContent = new String(fileContent, StandardCharsets.UTF_8);


        Context context = new Context();
        // 3.1. LETTER IDENTIFIERS & METADATA
        context.setVariable(AppConstants.LETTER_NO, AppConstants.OFFER_TEMPLATE_PREVIEW_LETTER_NO_VALUE);
        context.setVariable(AppConstants.APPLICATION_NO, AppConstants.OFFER_TEMPLATE_PREVIEW_APPLICATION_NO_VALUE);
        context.setVariable(AppConstants.REGISTRATION_NO, AppConstants.OFFER_TEMPLATE_PREVIEW_PROFILE_REGISTRATION_NUMBER_VALUE);
        context.setVariable(AppConstants.ACCEPT_BEFORE_DATE, AppConstants.OFFER_TEMPLATE_PREVIEW_ACCEPT_BEFORE_DATE_VALUE);
        context.setVariable(AppConstants.OFFER_RELEASE_DATE, LocalDate.now());
        context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);

        // 3.2. CANDIDATE PROFILE DETAILS
        context.setVariable(AppConstants.CANDIDATE_NAME, AppConstants.OFFER_TEMPLATE_PREVIEW_CANDIDATE_NAME_VALUE);
        context.setVariable(AppConstants.ADDRESS_LINE_1, AppConstants.OFFER_TEMPLATE_PREVIEW_ADDRESS_LINE_1_VALUE);
        context.setVariable(AppConstants.ADDRESS_LINE_2, AppConstants.OFFER_TEMPLATE_PREVIEW_ADDRESS_LINE_2_VALUE);
        context.setVariable(AppConstants.CITY, AppConstants.OFFER_TEMPLATE_PREVIEW_CITY_VALUE);
        context.setVariable(AppConstants.DISTRICT_NAME, AppConstants.OFFER_TEMPLATE_PREVIEW_DISTRICT_NAME_VALUE);
        context.setVariable(AppConstants.STATE_NAME, AppConstants.OFFER_TEMPLATE_PREVIEW_STATE_NAME_VALUE);
        context.setVariable(AppConstants.PINCODE, AppConstants.OFFER_TEMPLATE_PREVIEW_PINCODE_VALUE);
        context.setVariable(AppConstants.EMAIL, AppConstants.EMAIL);
        context.setVariable(AppConstants.CANDIDATE_CONTACT_NUMBER, AppConstants.OFFER_TEMPLATE_PREVIEW_CONTACT_NUMBER_VALUE);
        context.setVariable(AppConstants.CANDIDATE_CATEGORY_CODE, AppConstants.OFFER_TEMPLATE_PREVIEW_CATEGORY_CODE_VALUE);
        context.setVariable(AppConstants.ALLOTED_CATEGORY, AppConstants.OFFER_TEMPLATE_PREVIEW_CATEGORY_CODE_VALUE);

        // 3.3. POSITION & GRADING METADATA (BILINGUAL)
        context.setVariable(AppConstants.POSITION_NAME, AppConstants.OFFER_TEMPLATE_PREVIEW_POSITION_NAME_VALUE);
        context.setVariable(AppConstants.POSITION_HINDI_NAME, AppConstants.OFFER_TEMPLATE_PREVIEW_POSITION_HINDI_NAME_VALUE);
        context.setVariable(AppConstants.JOB_GRADE_CODE, AppConstants.OFFER_TEMPLATE_PREVIEW_JOB_GRADE_CODE_VALUE);
        context.setVariable(AppConstants.JOB_GRADE_HINDI_CODE, AppConstants.OFFER_TEMPLATE_PREVIEW_JOB_GRADE_HINDI_CODE_VALUE);
        context.setVariable(AppConstants.JOB_GRADE_MIN_SALARY, AppConstants.OFFER_TEMPLATE_PREVIEW_JOB_GRADE_MIN_SALARY_VALUE);

        // 3.4. FUNCTIONAL DEPARTMENTS & CAMPAIGNS
        context.setVariable(AppConstants.DEPARTMENT, AppConstants.OFFER_TEMPLATE_PREVIEW_DEPARTMENT_VALUE);
        context.setVariable(AppConstants.DEPARTMENT_HINDI_NAME, AppConstants.OFFER_TEMPLATE_PREVIEW_DEPARTMENT_HINDI_NAME_VALUE);
        context.setVariable(AppConstants.REQUISITION_TITLE, AppConstants.OFFER_TEMPLATE_PREVIEW_REQUISITION_TITLE_VALUE);
        context.setVariable(AppConstants.REQUISITION_START_DATE, AppConstants.OFFER_TEMPLATE_PREVIEW_REQUISITION_START_DATE_VALUE);
        context.setVariable(AppConstants.REQUISITION_CUTOFF_DATE, AppConstants.OFFER_TEMPLATE_PREVIEW_REQUISITION_END_DATE_VALUE);

        // 3.5. OPERATIONAL LOCATIONS & SCHEDULING
        context.setVariable(AppConstants.MEDICAL_TEST_DATE, AppConstants.OFFER_TEMPLATE_PREVIEW_JOINING_DATE_VALUE);
        context.setVariable(AppConstants.CANDIDATE_MEDICAL_CENTRE, AppConstants.OFFER_TEMPLATE_PREVIEW_MEDICAL_CENTRE_VALUE);
        context.setVariable(AppConstants.CANDIDATE_MEDICAL_STATE, AppConstants.OFFER_TEMPLATE_PREVIEW_MEDICAL_STATE_VALUE);
        context.setVariable(AppConstants.JOINING_DATE, AppConstants.OFFER_TEMPLATE_PREVIEW_JOINING_DATE_VALUE);
        context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_CENTRE, AppConstants.OFFER_TEMPLATE_PREVIEW_JOB_LOCATION_CENTRE_VALUE);
        context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_STATE, AppConstants.OFFER_TEMPLATE_PREVIEW_JOB_LOCATION_STATE_VALUE);
        context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_HINDI_CENTRE, AppConstants.OFFER_TEMPLATE_PREVIEW_JOB_LOCATION_HINDI_CENTRE_VALUE);
        context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_HINDI_STATE, AppConstants.OFFER_TEMPLATE_PREVIEW_JOB_LOCATION_HINDI_STATE_VALUE);

        // 3.6. COMPENSATION COMPONENTS
        context.setVariable(AppConstants.ANNUAL_BASIC, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_BASIC_VALUE);
        context.setVariable(AppConstants.MONTHLY_BASIC, AppConstants.OFFER_TEMPLATE_PREVIEW_MONTHLY_BASIC_VALUE);
        context.setVariable(AppConstants.ANNUAL_HRA, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_HRA_VALUE);
        context.setVariable(AppConstants.MONTHLY_HRA, AppConstants.OFFER_TEMPLATE_PREVIEW_MONTHLY_HRA_VALUE);
        context.setVariable(AppConstants.ANNUAL_SUPPLEMENTARY, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_SUPPLEMENTARY_VALUE);
        context.setVariable(AppConstants.MONTHLY_SUPPLEMENTARY, AppConstants.OFFER_TEMPLATE_PREVIEW_MONTHLY_SUPPLEMENTARY_VALUE);
        context.setVariable(AppConstants.ANNUAL_MEDICAL, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_MEDICAL_VALUE);
        context.setVariable(AppConstants.MONTHLY_MEDICAL, AppConstants.OFFER_TEMPLATE_PREVIEW_MONTHLY_MEDICAL_VALUE);
        context.setVariable(AppConstants.ANNUAL_ENTERTAINMENT, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_ENTERTAINMENT_VALUE);
        context.setVariable(AppConstants.MONTHLY_ENTERTAINMENT, AppConstants.OFFER_TEMPLATE_PREVIEW_MONTHLY_ENTERTAINMENT_VALUE);

        context.setVariable(AppConstants.ANNUAL_TOTAL_FIXED, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_TOTAL_FIXED_VALUE);
        context.setVariable(AppConstants.MONTHLY_TOTAL_FIXED, AppConstants.OFFER_TEMPLATE_PREVIEW_MONTHLY_TOTAL_FIXED_VALUE);
        context.setVariable(AppConstants.ANNUAL_FIXED_IN_WORDS, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_FIXED_IN_WORDS_VALUE);
        context.setVariable(AppConstants.ANNUAL_FIXED_IN_HINDI_WORDS, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_FIXED_IN_HINDI_WORDS_VALUE);

        context.setVariable(AppConstants.VARIABLE_PAY, AppConstants.OFFER_TEMPLATE_PREVIEW_VARIABLE_PAY_VALUE);
        context.setVariable(AppConstants.ANNUAL_VARIABLE_IN_WORDS, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_VARIABLE_IN_WORDS_VALUE);
        context.setVariable(AppConstants.ANNUAL_VARIABLE_IN_HINDI_WORDS, AppConstants.OFFER_TEMPLATE_PREVIEW_ANNUAL_VARIABLE_IN_HINDI_WORDS_VALUE);

        context.setVariable(AppConstants.CTC, AppConstants.OFFER_TEMPLATE_PREVIEW_CTC_VALUE);
        context.setVariable(AppConstants.BONUS, AppConstants.OFFER_TEMPLATE_PREVIEW_BONUS_VALUE);

        // 3.7. CORPORATE ISSUING AUTHORITIES
        context.setVariable(AppConstants.OFFICER_NAME, AppConstants.OFFER_TEMPLATE_PREVIEW_OFFICER_NAME_VALUE);
        context.setVariable(AppConstants.OFFICER_HINDI_NAME, AppConstants.OFFER_TEMPLATE_PREVIEW_OFFICER_HINDI_NAME_VALUE);
        context.setVariable(AppConstants.OFFICER_ROLE, AppConstants.OFFER_TEMPLATE_PREVIEW_OFFICER_ROLE_VALUE);
        context.setVariable(AppConstants.OFFICER_HINDI_ROLE, AppConstants.OFFER_TEMPLATE_PREVIEW_OFFICER_HINDI_ROLE_VALUE);
        context.setVariable(AppConstants.OFFICER_DEPT, AppConstants.OFFER_TEMPLATE_PREVIEW_OFFICER_DEPT_VALUE);
        context.setVariable(AppConstants.OFFICER_HINDI_DEPT, AppConstants.OFFER_TEMPLATE_PREVIEW_OFFICER_HINDI_DEPT_VALUE);
        context.setVariable(AppConstants.SELECT_LIST, AppConstants.OFFER_TEMPLATE_PREVIEW_SELECT_LIST_VALUE);
        context.setVariable(AppConstants.WAIT_LIST, AppConstants.OFFER_TEMPLATE_PREVIEW_WAIT_LIST_VALUE);

        String processedHtml = templateEngine.process(htmlContent, context);

        byte[] pdfContent =pdfConverterService.convertHtmlStringToPdf(processedHtml);

        return OfferLetterTemplateModel.builder().
                templateName(template.getTemplateName())
                .previewContent(pdfContent)
                .build();
    }


    public String uploadTemplete(MultipartFile file, String path) throws IOException {
        String fileName = file.getOriginalFilename();

        String pathSa = azureBlobStorageService.uploadFile(file,fileName, path);
        return pathSa;
    }

    public OfferLetterTemplateModel getCandidatePreviewTemplate(UUID templateId, UUID applicationId) {
        // ---------------------------------------------------------
        // 1. FETCH CORE ENTITIES
        // ---------------------------------------------------------
        CandidateOffersEntity candidateOffer = candidateOffersRepository.findByCandidateApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate offer not found"));
        TemplatesEntity template = templatesRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        CandidateApplicationsEntity application = candidateOffer.getCandidateApplication();
        JobPositionsEntity jobPosition = candidateOffer.getJobPosition();

        // ---------------------------------------------------------
        // 2. FETCH DEPENDENT ENTITIES
        // ---------------------------------------------------------
        CandidateProfileEntity candidateProfile = candidateProfileRepository.findByCandidateId(application.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        CandidateAddressEntity candidateAddress = candidateAddressRepository.findByCandidateId(application.getCandidateId())
                .orElse(null);

        CandidateMeritListEntity meritListEntity = candidateMeritListRepository.findByApplicationId(applicationId).orElse(null);
        CandidateCompensationEntity compensationEntity = candidateCompensationRepository.findByApplicationId(applicationId).orElse(null);

        // --- OPTIMIZATION: Batch Fetching Shared Entities (States & Categories) ---

        // 1. Collect State IDs
        Set<UUID> stateIdsToFetch = Stream.of(
                candidateAddress != null ? candidateAddress.getStateId() : null,
                meritListEntity != null ? meritListEntity.getStateId() : null
        ).filter(Objects::nonNull).collect(Collectors.toSet());

        // Fetch States once and create a lookup map
        Map<UUID, String> stateLookupMap = stateIdsToFetch.isEmpty() ? Collections.emptyMap() :
                statesRepository.findAllById(stateIdsToFetch).stream()
                        .collect(Collectors.toMap(StateEntity::getId, StateEntity::getStateName));

        String stateName = candidateAddress != null && candidateAddress.getStateId() != null ?
                stateLookupMap.get(candidateAddress.getStateId()) : null;
        String jobLocationStateName = meritListEntity != null && meritListEntity.getStateId() != null ?
                stateLookupMap.get(meritListEntity.getStateId()) : null;

        // 2. Collect Reservation Category IDs
        Set<UUID> categoryIdsToFetch = Stream.of(
                candidateProfile.getReservationCategoryId(),
                meritListEntity != null ? meritListEntity.getSelectedCategoryId() : null
        ).filter(Objects::nonNull).collect(Collectors.toSet());

        // Fetch Categories once and create a lookup map
        Map<UUID, String> categoryLookupMap = categoryIdsToFetch.isEmpty() ? Collections.emptyMap() :
                reservationCategoriesRepository.findAllById(categoryIdsToFetch).stream()
                        .collect(Collectors.toMap(ReservationCategoriesEntity::getId, ReservationCategoriesEntity::getCategoryCode));

        String categoryCode = categoryLookupMap.get(candidateProfile.getReservationCategoryId());
        String allottedCategoryCode = meritListEntity != null ? categoryLookupMap.get(meritListEntity.getSelectedCategoryId()) : null;

        // ---------------------------------------------------------

        // Lookups for remaining distinct entities (Single calls)
        String districtName = (candidateAddress != null && candidateAddress.getDistrictId() != null) ?
                districtRepository.findById(candidateAddress.getDistrictId()).map(DistrictEntity::getDistrictName).orElse(null) : null;

        JobRequisitionsEntity jobRequisition = jobPosition.getRequisitionId() != null ?
                jobRequisitionRepository.findById(jobPosition.getRequisitionId()).orElse(null) : null;
        MasterPositionsEntity masterPosition = jobPosition.getMasterPositionId() != null ?
                masterPositionsRepository.findById(jobPosition.getMasterPositionId()).orElse(null) : null;
        DepartmentsEntity department = jobPosition.getDeptId() != null ?
                departmentsRepository.findById(jobPosition.getDeptId()).orElse(null) : null;
        JobGradeEntity jobGrade = jobPosition.getGradeId() != null ?
                jobGradeRepository.findById(jobPosition.getGradeId()).orElse(null) : null;
        EmployementTypesEntity empType = jobPosition.getEmploymentType() != null ?
                employementTypesRepository.findById(jobPosition.getEmploymentType()).orElse(null) : null;

        InterviewCentresEntity medicalCentre = candidateOffer.getMedicalCenterId() != null ?
                interviewCentresRepository.findById(candidateOffer.getMedicalCenterId()).orElse(null) : null;

        String medicalCentreStateName = (medicalCentre != null && medicalCentre.getZonalStateId() != null) ?
                zonalStateRepository.findById(medicalCentre.getZonalStateId()).map(ZonalStatesEntity::getStateName).orElse(null) : null;

        String jobLocationCityName = (meritListEntity != null && meritListEntity.getCityId() != null) ?
                cityRepository.findById(meritListEntity.getCityId()).map(CityEntity::getCityName).orElse(null) : null;

        // ---------------------------------------------------------
        // 3. BUILD THYMELEAF CONTEXT
        // ---------------------------------------------------------
        Context context = new Context();

        // 3.1. LETTER IDENTIFIERS & METADATA
        context.setVariable(AppConstants.LETTER_NO, candidateOffer.getLetterNumber());
        context.setVariable(AppConstants.APPLICATION_NO, application.getApplicationNo());
        context.setVariable(AppConstants.REGISTRATION_NO, candidateProfile.getRegistrationNo());
        context.setVariable(AppConstants.ACCEPT_BEFORE_DATE, candidateOffer.getAcceptBeforeDate() != null ? candidateOffer.getAcceptBeforeDate().format(AppConstants.DD_MM_YYYY) : AppConstants.OFFER_TEMPLATE_PREVIEW_ACCEPT_BEFORE_DATE_VALUE);
        context.setVariable(AppConstants.OFFER_RELEASE_DATE, LocalDate.now());
        context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);

        // 3.2. CANDIDATE PROFILE DETAILS
        context.setVariable(AppConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(candidateProfile));
        context.setVariable(AppConstants.ADDRESS_LINE_1, candidateAddress != null ? candidateAddress.getAddressLine1() : null);
        context.setVariable(AppConstants.ADDRESS_LINE_2, candidateAddress != null ? candidateAddress.getAddressLine2() : null);
        context.setVariable(AppConstants.CITY, candidateAddress != null ? candidateAddress.getCity() : null);
        context.setVariable(AppConstants.DISTRICT_NAME, districtName);
        context.setVariable(AppConstants.STATE_NAME, stateName);
        context.setVariable(AppConstants.PINCODE, candidateAddress != null ? candidateAddress.getPincode() : null);
        context.setVariable(AppConstants.EMAIL, commonUtilityProvider.maskEmail(candidateProfile.getEmail()));
        context.setVariable(AppConstants.CANDIDATE_CONTACT_NUMBER, commonUtilityProvider.maskMobileNumber(candidateProfile.getContactNo()));
        context.setVariable(AppConstants.CANDIDATE_CATEGORY_CODE, categoryCode);
        context.setVariable(AppConstants.ALLOTED_CATEGORY, allottedCategoryCode);

        // 3.3. POSITION & GRADING METADATA (BILINGUAL)
        context.setVariable(AppConstants.POSITION_NAME, masterPosition != null ? masterPosition.getPositionName() : null);
        context.setVariable(AppConstants.POSITION_HINDI_NAME, "Position Name In Hindi");
        context.setVariable(AppConstants.JOB_GRADE_CODE, jobGrade != null ? jobGrade.getJobGradeCode() : null);
        context.setVariable(AppConstants.JOB_GRADE_HINDI_CODE, "Job Grade Code In Hindi");
        context.setVariable(AppConstants.JOB_GRADE_MIN_SALARY, jobGrade != null && jobGrade.getMinSalary() != null ? jobGrade.getMinSalary().toString() : null);

        // 3.4. FUNCTIONAL DEPARTMENTS & CAMPAIGNS
        context.setVariable(AppConstants.DEPARTMENT, department != null ? department.getDepartmentName() : null);
        context.setVariable(AppConstants.DEPARTMENT_HINDI_NAME, "Department Name in Hindi");
        context.setVariable(AppConstants.REQUISITION_TITLE, jobRequisition != null ? jobRequisition.getRequisitionCode() + " - " + jobRequisition.getRequisitionTitle() : null);
        context.setVariable(AppConstants.REQUISITION_START_DATE, jobRequisition != null && jobRequisition.getStartDate() != null ? jobRequisition.getStartDate().format(AppConstants.DD_MM_YYYY) : null);
        context.setVariable(AppConstants.REQUISITION_CUTOFF_DATE, jobRequisition != null && jobRequisition.getEndDate() != null ? jobRequisition.getEndDate().format(AppConstants.DD_MM_YYYY) : null);

        // 3.5. OPERATIONAL LOCATIONS & SCHEDULING
        String joiningDate = candidateOffer.getJoiningDate() != null ? candidateOffer.getJoiningDate().format(AppConstants.DD_MM_YYYY) : AppConstants.OFFER_TEMPLATE_PREVIEW_JOINING_DATE_VALUE;
        String medicalTestDate = candidateOffer.getJoiningDate() != null ? candidateOffer.getJoiningDate().minusDays(6).format(AppConstants.DD_MM_YYYY) : null;

        context.setVariable(AppConstants.MEDICAL_TEST_DATE, medicalTestDate);
        context.setVariable(AppConstants.CANDIDATE_MEDICAL_CENTRE, medicalCentre != null ? medicalCentre.getDisplayName() : AppConstants.OFFER_TEMPLATE_PREVIEW_MEDICAL_CENTRE_VALUE);
        context.setVariable(AppConstants.CANDIDATE_MEDICAL_STATE, medicalCentreStateName != null ? medicalCentreStateName : AppConstants.OFFER_TEMPLATE_PREVIEW_MEDICAL_STATE_VALUE);
        context.setVariable(AppConstants.JOINING_DATE, joiningDate);
        context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_CENTRE, jobLocationCityName != null ? jobLocationCityName : AppConstants.OFFER_TEMPLATE_PREVIEW_JOB_LOCATION_CENTRE_VALUE);
        context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_STATE, jobLocationStateName != null ? jobLocationStateName : AppConstants.OFFER_TEMPLATE_PREVIEW_JOB_LOCATION_STATE_VALUE);
        context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_HINDI_CENTRE, "Job Location Name in Hindi");
        context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_HINDI_STATE, "Job Location State in Hindi");

        // 3.6. COMPENSATION COMPONENTS
        String empTypeName = empType != null ? empType.getTypeName() : null;
        if ("contract".equalsIgnoreCase(empTypeName) && compensationEntity != null) {
            SalaryBreakUpModel model = commonUtilityProvider.calculateCompensationBreakdown(compensationEntity, jobLocationCityName);

            context.setVariable(AppConstants.ANNUAL_BASIC, commonUtilityProvider.formatToIndianCurrency(model.getAnnualBasic()));
            context.setVariable(AppConstants.MONTHLY_BASIC, commonUtilityProvider.formatToIndianCurrency(model.getMonthlyBasic()));
            context.setVariable(AppConstants.ANNUAL_HRA, commonUtilityProvider.formatToIndianCurrency(model.getAnnualHra()));
            context.setVariable(AppConstants.MONTHLY_HRA, commonUtilityProvider.formatToIndianCurrency(model.getMonthlyHra()));
            context.setVariable(AppConstants.ANNUAL_SUPPLEMENTARY, commonUtilityProvider.formatToIndianCurrency(model.getAnnualSupplementary()));
            context.setVariable(AppConstants.MONTHLY_SUPPLEMENTARY, commonUtilityProvider.formatToIndianCurrency(model.getMonthlySupplementary()));
            context.setVariable(AppConstants.ANNUAL_MEDICAL, commonUtilityProvider.formatToIndianCurrency(model.getAnnualMedical()));
            context.setVariable(AppConstants.MONTHLY_MEDICAL, commonUtilityProvider.formatToIndianCurrency(model.getMonthlyMedical()));
            context.setVariable(AppConstants.ANNUAL_ENTERTAINMENT, commonUtilityProvider.formatToIndianCurrency(model.getAnnualEntertainment()));
            context.setVariable(AppConstants.MONTHLY_ENTERTAINMENT, commonUtilityProvider.formatToIndianCurrency(model.getMonthlyEntertainment()));
            context.setVariable(AppConstants.ANNUAL_TOTAL_FIXED, commonUtilityProvider.formatToIndianCurrency(model.getAnnualTotalFixed()));
            context.setVariable(AppConstants.MONTHLY_TOTAL_FIXED, commonUtilityProvider.formatToIndianCurrency(model.getMonthlyTotalFixed()));
            context.setVariable(AppConstants.ANNUAL_FIXED_IN_WORDS, commonUtilityProvider.convertNumberToWordsInIndianSystem(model.getAnnualTotalFixed(), "english"));
            context.setVariable(AppConstants.ANNUAL_FIXED_IN_HINDI_WORDS, commonUtilityProvider.convertNumberToWordsInIndianSystem(model.getAnnualTotalFixed(), "hindi"));
            context.setVariable(AppConstants.VARIABLE_PAY, commonUtilityProvider.formatToIndianCurrency(model.getVariablePay()));
            context.setVariable(AppConstants.ANNUAL_VARIABLE_IN_WORDS, commonUtilityProvider.convertNumberToWordsInIndianSystem(model.getVariablePay(), "english"));
            context.setVariable(AppConstants.ANNUAL_VARIABLE_IN_HINDI_WORDS, commonUtilityProvider.convertNumberToWordsInIndianSystem(model.getVariablePay(), "hindi"));
        }

        // 3.7. CORPORATE ISSUING AUTHORITIES
        context.setVariable(AppConstants.OFFICER_NAME, "OFFICER NAME");
        context.setVariable(AppConstants.OFFICER_HINDI_NAME, "OFFICER NAME IN HINDI");
        context.setVariable(AppConstants.OFFICER_ROLE, "OFFICER ROLE");
        context.setVariable(AppConstants.OFFICER_HINDI_ROLE, "OFFICER ROLE IN HINDI");
        context.setVariable(AppConstants.OFFICER_DEPT, "OFFICER DEPARTMENT");
        context.setVariable(AppConstants.OFFICER_HINDI_DEPT, "OFFICER DEPARTMENT IN HINDI");
        context.setVariable(AppConstants.SELECT_LIST, candidateOffer.getSelectList());
        context.setVariable(AppConstants.WAIT_LIST, candidateOffer.getWaitList());

        // ---------------------------------------------------------
        // 4. PROCESS PDF GENERATION
        // ---------------------------------------------------------
        byte[] fileContent = azureBlobStorageService.downloadFile(template.getFilePath(),"");
        String htmlContent = new String(fileContent, StandardCharsets.UTF_8);

        //process html form blob with context
        String processedHtml = templateEngine.process(htmlContent, context);
        byte[] pdfContent =pdfConverterService.convertHtmlStringToPdf(processedHtml);

        return OfferLetterTemplateModel.builder()
                .templateName(template.getTemplateName())
                .previewContent(pdfContent)
                .build();
    }
}
