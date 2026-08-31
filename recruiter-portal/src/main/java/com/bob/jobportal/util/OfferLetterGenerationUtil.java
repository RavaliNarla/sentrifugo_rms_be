package com.bob.jobportal.util;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.model.SalaryBreakUpModel;
import com.bob.commonutil.service.AzureBlobStorageService;
import com.bob.commonutil.service.CommonMailService;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.service.PdfConverterService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CandidateLookupUtil;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.CustomMultipartFile;
import com.bob.db.entity.*;
import com.bob.db.repository.*;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OfferLetterGenerationUtil {

    @Autowired
    private CandidateLookupUtil candidateLookupUtil;

    @Autowired
    private AzureBlobStorageService azureBlobStorageService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private PdfConverterService pdfConverterService;

    @Autowired
    private CommonMailService commonMailService;

    @Value("${candidate.offer.upload.path}")
    private String offerUploadPath;

    @Autowired
    private FileService fs;

    @Autowired
    private CandidateOffersRepository candidateOffersRepository;

    @Autowired
    private CandidateMeritListRepository meritListRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CandidateMeritListRepository candidateMeritListRepository;

    @Autowired
    private CandidateCompensationRepository candidateCompensationRepository;



    public List<OfferMailModel> generateOfferLetterForCandidateOffers(List<CandidateOffersEntity> candidateOffers) {
        List<UUID> applicationIds = candidateOffers.stream().map((curr)->curr.getCandidateApplication().getId())
                .toList();
        Map<UUID,CandidateMeritListEntity> meritListEntityMap =  meritListRepository.findByApplicationIdIn(applicationIds)
                .stream()
                .collect(Collectors.toMap(
                        CandidateMeritListEntity::getApplicationId,
                        Function.identity()
                ));
        Map<UUID,CandidateCompensationEntity> candidateCompensationEntityMap = candidateCompensationRepository.findByApplicationIdIn(applicationIds)
                .stream()
                .collect(Collectors.toMap(
                        (curr)->curr.getApplication().getId(),
                        Function.identity()
                ));
        List<UUID> offerStateIds = meritListEntityMap.values().stream().map(CandidateMeritListEntity::getStateId).filter(Objects::nonNull).toList();
        List<UUID> offeredCityIds = meritListEntityMap.values().stream().map(CandidateMeritListEntity::getCityId).filter(Objects::nonNull).toList();

        Map<UUID, MasterPositionsEntity> masterPositionMap = candidateLookupUtil.buildMasterPositionMap(candidateOffers);
        Map<UUID, CandidateProfileEntity> candidateProfileMap = candidateLookupUtil.buildCandidateProfileMap(candidateOffers);
        Map<UUID, ReservationCategoriesEntity> reservationCategoriesMap = candidateLookupUtil.buildReservationCategoryMap(candidateProfileMap,meritListEntityMap);
        Map<UUID, CandidateAddressEntity> candidateAddressMap = candidateLookupUtil.buildCandidateAddressMap(candidateOffers);
        Map<UUID,UserSignatryEntity> signatryEntityMap=candidateLookupUtil.buildSignatryMap(candidateOffers);
        List<UUID> addressStateIds = candidateAddressMap.values().stream().map(CandidateAddressEntity::getStateId).filter(Objects::nonNull).toList();
        List<UUID> allStateIds = new ArrayList<>();
        allStateIds.addAll(offerStateIds);
        allStateIds.addAll(addressStateIds);
        Map<UUID,StateEntity> candidateStateMap = stateRepository.findAllById(allStateIds).stream()
                .collect(Collectors.toMap(
                        StateEntity::getId,
                        Function.identity()
                ));
        Map<UUID,CityEntity> candidateCityMap = cityRepository.findAllById(offeredCityIds).stream()
                .collect(Collectors.toMap(
                        CityEntity::getId,
                        Function.identity()

                ));
        Map<UUID,DistrictEntity> candidateDistrictMap = candidateLookupUtil.buildCandidateDistrictMap(candidateAddressMap);
        Map<UUID,JobPositionsEntity> jobPositionsMap = candidateLookupUtil.buildJobPositionMap(candidateOffers);
        Map<UUID,JobRequisitionsEntity> jobRequisitionsMap = candidateLookupUtil.buildJobRequistionMap(jobPositionsMap);
        Map<UUID,DepartmentsEntity> departmentsMap = candidateLookupUtil.buildDepartmentsMap(jobPositionsMap);
        Map<UUID,JobGradeEntity> jobGradeEntityMap = candidateLookupUtil.buildJobGradeMap(jobPositionsMap);
        Map<UUID,EmployementTypesEntity> employementTypesEntityMap = candidateLookupUtil.buildJobEmpTypeMap(jobPositionsMap);
        Map<UUID,InterviewCentresEntity> medicalCentresMap = candidateLookupUtil.buildMedicalCentreMap(candidateOffers);
        Map<UUID,ZonalStatesEntity> zonalStateMap = candidateLookupUtil.buildZonalStateMap(medicalCentresMap);
        Map<UUID,TemplatesEntity> templatesEntityMap = candidateLookupUtil.buildTemplatesMap(candidateOffers);

        List<OfferMailModel> offerMailModels = new ArrayList<>();
        for(CandidateOffersEntity entity:candidateOffers){
            CandidatesEntity candidate = entity.getCandidate();
            CandidateApplicationsEntity application = entity.getCandidateApplication();
            CandidateProfileEntity candidateProfile = resolveCandidateProfile(candidate.getId(),candidateProfileMap);
            CandidateMeritListEntity meritListEntity = resolverMeritListEntity(application.getId(),meritListEntityMap);
            CandidateAddressEntity candidateAddress = resolveCandidateAddress(candidate.getId(),candidateAddressMap);
            CandidateCompensationEntity compensationEntity = resolveCandidateCompensation(application.getId(),candidateCompensationEntityMap);
            JobPositionsEntity jobPosition = resolveJobPosition(entity,jobPositionsMap);
            JobRequisitionsEntity jobRequisition = resolveJobRequisition(jobPosition,jobRequisitionsMap);

            InterviewCentresEntity medicalCentre = resolveMedicalCentreName(entity,medicalCentresMap);

            String empType = resolveEmpType(jobPosition,employementTypesEntityMap);
            String categoryCode = resolveReservationCategoryCode(candidateProfile,reservationCategoriesMap);
            String allottedCategoryCode = resolveAllocatedCategoryCode(meritListEntity,reservationCategoriesMap);
            String medicalCentreStateName = resolveZonalStateName(medicalCentre,zonalStateMap);
            String jobLocationCityName = resolveOfferedCityName(meritListEntity,candidateCityMap);
            String jobLocationStateName = resolveOfferedStateName(meritListEntity,candidateStateMap);
            String stateName = resolveCandidateStateName(candidateAddress,candidateStateMap);
            String districtName = resolveCandidateDistrictName(candidateAddress,candidateDistrictMap);
            String jobGradeCode = resolveJobGradeCode(jobPosition,jobGradeEntityMap);
            BigDecimal jobGradeMinSalary = resolveJobGradeAmount(jobPosition,jobGradeEntityMap);
            String departmentName = resolveDepartmentName(jobPosition,departmentsMap);


            String designationName = resolveDesignationName(entity.getDesignation(), masterPositionMap);
            String medicalCentreName = medicalCentre != null ? medicalCentre.getDisplayName() : null;
            String address1 =  candidateAddress != null  && candidateAddress.getAddressLine1() != null ? candidateAddress.getAddressLine1() : null;
            String address2 =  candidateAddress !=null && candidateAddress.getAddressLine2() != null ? candidateAddress.getAddressLine2() : null;
            String city =  candidateAddress!=null && candidateAddress.getCity() != null ? candidateAddress.getCity() : null;
            String pincode = candidateAddress != null && candidateAddress.getPincode() != null ? candidateAddress.getPincode() : null;
            String email = candidateProfile!=null && candidateProfile.getEmail() != null ? candidateProfile.getEmail() : null;
            String contactNo = candidateProfile!=null && candidateProfile.getContactNo() != null ? candidateProfile.getContactNo() : null;
            String candidateRegistrationNo = candidateProfile != null && candidateProfile.getRegistrationNo()!=null ? candidateProfile.getRegistrationNo() : null;
            String requisitionTitle = jobRequisition != null && jobRequisition.getRequisitionTitle() != null ? jobRequisition.getRequisitionTitle() : null;
            String requisitionStartDate = jobRequisition != null && jobRequisition.getStartDate() != null ? jobRequisition.getStartDate().format(AppConstants.DD_MM_YYYY) : null;
            String requisitionCutoffDate = jobRequisition != null && jobRequisition.getCutoffDate() != null ? jobRequisition.getCutoffDate().format(AppConstants.DD_MM_YYYY) : null;
            String joiningDate = resolveJoiningDate(entity);
            String medicalTestDate = resolveMedicalTestDate(entity);
//            String userSignatryName=resolveSignatryName(entity,signatryEntityMap);
            String userSignatoryName=resolveSignatoryName(entity);
            String userSignatoryDesignation=resolveSignatoryDesignation(entity);
            Context context = new Context();

            // 1. LETTER IDENTIFIERS & METADATA

            context.setVariable(AppConstants.LETTER_NO, entity.getLetterNumber());
            context.setVariable(AppConstants.APPLICATION_NO, application.getApplicationNo());
            context.setVariable(AppConstants.REGISTRATION_NO, candidateRegistrationNo);
            context.setVariable(AppConstants.ACCEPT_BEFORE_DATE, entity.getAcceptBeforeDate().format(AppConstants.DD_MM_YYYY));


            // 2. CANDIDATE PROFILE DETAILS

            context.setVariable(AppConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(candidateProfile));
            context.setVariable(AppConstants.ADDRESS_LINE_1, address1);
            context.setVariable(AppConstants.ADDRESS_LINE_2, address2);
            context.setVariable(AppConstants.CITY, city);
            context.setVariable(AppConstants.DISTRICT_NAME, districtName);
            context.setVariable(AppConstants.STATE_NAME, stateName);
            context.setVariable(AppConstants.PINCODE, pincode);
            context.setVariable(AppConstants.EMAIL, commonUtilityProvider.maskEmail(email));
            context.setVariable(AppConstants.CANDIDATE_CONTACT_NUMBER, commonUtilityProvider.maskMobileNumber(contactNo));
            context.setVariable(AppConstants.CANDIDATE_CATEGORY_CODE, categoryCode);
            context.setVariable(AppConstants.ALLOTED_CATEGORY, allottedCategoryCode);


            // 3. POSITION & GRADING METADATA (BILINGUAL)

            context.setVariable(AppConstants.POSITION_NAME, designationName);
            context.setVariable(AppConstants.POSITION_HINDI_NAME, "Position Name In Hindi");
            context.setVariable(AppConstants.JOB_GRADE_CODE, jobGradeCode);
            context.setVariable(AppConstants.JOB_GRADE_HINDI_CODE,"Job Grade Code In Hindi");
            context.setVariable(AppConstants.JOB_GRADE_MIN_SALARY,commonUtilityProvider.formatToIndianCurrency(jobGradeMinSalary));


            // 4. FUNCTIONAL DEPARTMENTS & CAMPAIGNS

            context.setVariable(AppConstants.DEPARTMENT, departmentName);
            context.setVariable(AppConstants.DEPARTMENT_HINDI_NAME,"Department Name in Hindi");
            context.setVariable(AppConstants.REQUISITION_TITLE, requisitionTitle);
            context.setVariable(AppConstants.REQUISITION_START_DATE, requisitionStartDate);
            context.setVariable(AppConstants.REQUISITION_CUTOFF_DATE, requisitionCutoffDate);


            // 5. OPERATIONAL LOCATIONS & SCHEDULING

            context.setVariable(AppConstants.MEDICAL_TEST_DATE, medicalTestDate);
            context.setVariable(AppConstants.CANDIDATE_MEDICAL_CENTRE, medicalCentreName);
            context.setVariable(AppConstants.CANDIDATE_MEDICAL_STATE, medicalCentreStateName);
            context.setVariable(AppConstants.JOINING_DATE, joiningDate);
            context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_CENTRE, jobLocationCityName);
            context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_STATE, jobLocationStateName);
            context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_HINDI_CENTRE, "Job Location Name in Hindi");
            context.setVariable(AppConstants.CANDIDATE_JOB_LOCATION_HINDI_STATE, "Job Location State in Hindi");


            // 6. COMPENSATION COMPONENTS (ANNEXURE B)
            if ("contract".equalsIgnoreCase(empType) && compensationEntity != null) {
                SalaryBreakUpModel model = commonUtilityProvider.calculateCompensationBreakdown(compensationEntity, jobLocationCityName);

                context.setVariable(AppConstants.ANNUAL_BASIC,
                        commonUtilityProvider.formatToIndianCurrency(model.getAnnualBasic()));
                context.setVariable(AppConstants.MONTHLY_BASIC,
                        commonUtilityProvider.formatToIndianCurrency(model.getMonthlyBasic()));

                context.setVariable(AppConstants.ANNUAL_HRA,
                        commonUtilityProvider.formatToIndianCurrency(model.getAnnualHra()));
                context.setVariable(AppConstants.MONTHLY_HRA,
                        commonUtilityProvider.formatToIndianCurrency(model.getMonthlyHra()));

                context.setVariable(AppConstants.ANNUAL_SUPPLEMENTARY,
                        commonUtilityProvider.formatToIndianCurrency(model.getAnnualSupplementary()));
                context.setVariable(AppConstants.MONTHLY_SUPPLEMENTARY,
                        commonUtilityProvider.formatToIndianCurrency(model.getMonthlySupplementary()));

                context.setVariable(AppConstants.ANNUAL_MEDICAL,
                        commonUtilityProvider.formatToIndianCurrency(model.getAnnualMedical()));
                context.setVariable(AppConstants.MONTHLY_MEDICAL,
                        commonUtilityProvider.formatToIndianCurrency(model.getMonthlyMedical()));

                context.setVariable(AppConstants.ANNUAL_ENTERTAINMENT,
                        commonUtilityProvider.formatToIndianCurrency(model.getAnnualEntertainment()));
                context.setVariable(AppConstants.MONTHLY_ENTERTAINMENT,
                        commonUtilityProvider.formatToIndianCurrency(model.getMonthlyEntertainment()));

                context.setVariable(AppConstants.ANNUAL_TOTAL_FIXED,
                        commonUtilityProvider.formatToIndianCurrency(model.getAnnualTotalFixed()));
                context.setVariable(AppConstants.MONTHLY_TOTAL_FIXED,
                        commonUtilityProvider.formatToIndianCurrency(model.getMonthlyTotalFixed()));

                context.setVariable(AppConstants.ANNUAL_FIXED_IN_WORDS,commonUtilityProvider.convertNumberToWordsInIndianSystem(model.getAnnualTotalFixed(),"english"));
                context.setVariable(AppConstants.ANNUAL_FIXED_IN_HINDI_WORDS, commonUtilityProvider.convertNumberToWordsInIndianSystem(model.getAnnualTotalFixed(),"hindi"));

                context.setVariable(AppConstants.VARIABLE_PAY,
                        commonUtilityProvider.formatToIndianCurrency(model.getVariablePay()));

                context.setVariable(AppConstants.ANNUAL_VARIABLE_IN_WORDS,
                        commonUtilityProvider.convertNumberToWordsInIndianSystem(model.getVariablePay(),"english"));
                context.setVariable(AppConstants.ANNUAL_VARIABLE_IN_HINDI_WORDS,
                        commonUtilityProvider.convertNumberToWordsInIndianSystem(model.getAnnualTotalFixed(),"hindi"));
            }



            // ==========================================
            // 7. CORPORATE ISSUING AUTHORITIES
            // ==========================================
            context.setVariable(AppConstants.OFFICER_NAME,userSignatoryName);
            context.setVariable(AppConstants.OFFICER_HINDI_NAME, "OFFICER NAME IN HINDI");
            context.setVariable(AppConstants.OFFICER_ROLE, userSignatoryDesignation);
            context.setVariable(AppConstants.OFFICER_HINDI_ROLE, "OFFICER ROLE IN HINDI");
            context.setVariable(AppConstants.OFFICER_DEPT, "OFFICER DEPARTMENT");
            context.setVariable(AppConstants.OFFICER_HINDI_DEPT, "OFFICER DEPARTMENT IN HINDI");



            //to get template from blob storage
            TemplatesEntity template = templatesEntityMap.get(entity.getTemplateId());
            byte[] offerLetterAttachmentByteContent = azureBlobStorageService.downloadFile(template.getFilePath(),"");
            String offerLetterAttachmentString = new String(offerLetterAttachmentByteContent, StandardCharsets.UTF_8);

            //for html body
            String html = templateEngine.process(AppConstants.OFFER_EMAIL_TEMPLATE, context);

            //for offer letter generation
            String processedOfferLetterAttachment = templateEngine.process(offerLetterAttachmentString, context);
            byte[] pdfBytes = pdfConverterService.convertHtmlStringToPdf(processedOfferLetterAttachment);

            MultipartFile pdfFile = new CustomMultipartFile(pdfBytes, "file", "offer.pdf", "application/pdf");
            try{
                String fileName = "Offer_Letter_"+UUID.randomUUID()+".pdf";
                String uploadedFileUrl = fs.uploadFile(pdfFile,fileName,offerUploadPath);
                entity.setOfferFileUrl(offerUploadPath+"/"+uploadedFileUrl);
                OfferMailModel currOfferMailModel = OfferMailModel.builder().
                        email(candidate.getEmail())
                        .htmlBody(html)
                        .pdfFile(pdfFile)
                        .build();
                offerMailModels.add(currOfferMailModel);
            }catch (IOException e){
                throw new CommonException("Failed to upload offer letter to blob");
            }
        }
        return offerMailModels;
    }

    @Async
    public void sendOfferMails(List<OfferMailModel> offerMailModels){
        try{
            for(OfferMailModel currOfferMailModel:offerMailModels){
                commonMailService.sendEmailSync(currOfferMailModel.getEmail(), AppConstants.OFFER_EMAIL_SUBJECT, currOfferMailModel.getHtmlBody(), currOfferMailModel.getPdfFile());
            }
        }catch (MessagingException | IOException e){
            throw new CommonException("Failed to send offer mail");
        }

    }

    @Async
    public void sendOfferLetterMails(List<CandidateOffersEntity> candidateOffersEntities,Map<UUID,CandidateProfileEntity> candidateProfileEntityMap){
        try{
            for(CandidateOffersEntity candidateOffers:candidateOffersEntities){
                Context context=createContextForOffer(candidateOffers,candidateProfileEntityMap);
                String html = templateEngine.process(AppConstants.OFFER_EMAIL_TEMPLATE, context);
                String email=candidateProfileEntityMap.get(candidateOffers.getCandidate().getId()).getEmail();
                String fullUrl = candidateOffers.getOfferFileUrl();
                String fileName = fullUrl.substring(fullUrl.lastIndexOf('/') + 1);
                String path = fullUrl.contains("/") ? fullUrl.substring(0, fullUrl.lastIndexOf('/')) : "";
                byte[] offerLetterAttachmentByteContent = azureBlobStorageService.downloadFile(fileName, path);

                if (offerLetterAttachmentByteContent == null) {
                    throw new CommonException("Offer letter PDF not found in storage for URL: " + fullUrl);
                }

                MultipartFile pdfFile = new CustomMultipartFile(offerLetterAttachmentByteContent, "file", fileName, "application/pdf");
                commonMailService.sendEmailSync(email, AppConstants.OFFER_EMAIL_SUBJECT, html,pdfFile);
            }
        }catch (MessagingException | IOException e){
            throw new CommonException("Failed to send offer mail");
        }
    }

    private Context createContextForOffer(CandidateOffersEntity candidateOffer,Map<UUID,CandidateProfileEntity> candidateProfileEntityMap){
        Context context = new Context();
        UUID candidateId=candidateOffer.getCandidate().getId();
        CandidateProfileEntity candidateProfile=candidateProfileEntityMap.get(candidateId);
        context.setVariable(AppConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(candidateProfile));
        context.setVariable(AppConstants.ACCEPT_BEFORE_DATE, candidateOffer.getAcceptBeforeDate().format(AppConstants.DD_MM_YYYY));
        context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
        return context;
    }


    private CandidateProfileEntity resolveCandidateProfile(UUID candidateId,Map<UUID,CandidateProfileEntity> candidateProfileMap){
        if(candidateId == null) return null;
        return candidateProfileMap.getOrDefault(candidateId,null);
    }

    private CandidateMeritListEntity resolverMeritListEntity(UUID applicationId,Map<UUID,CandidateMeritListEntity> candidateMeritListEntityMap){
        if(applicationId == null) return null;
        return candidateMeritListEntityMap.getOrDefault(applicationId,null);
    }

    public String resolveEmpType(JobPositionsEntity jobPosition, Map<UUID, EmployementTypesEntity> employementTypesEntityMap) {
        if (jobPosition == null || jobPosition.getEmploymentType() == null) return null;
        EmployementTypesEntity empType = employementTypesEntityMap.get(jobPosition.getEmploymentType());
        return empType != null ? empType.getTypeName() : null;
    }

    public String resolveReservationCategoryCode(CandidateProfileEntity profile, Map<UUID, ReservationCategoriesEntity> reservationCategoryMap) {
        if (profile == null || profile.getReservationCategoryId() == null) return null;
        ReservationCategoriesEntity rc = reservationCategoryMap.get(profile.getReservationCategoryId());
        return rc != null ? rc.getCategoryCode() : null;
    }

    public String resolveAllocatedCategoryCode(CandidateMeritListEntity meritEntity, Map<UUID,ReservationCategoriesEntity> reservationCategoryMap){
        if (meritEntity == null || meritEntity.getSelectedCategoryId() == null) return null;
        ReservationCategoriesEntity rc = reservationCategoryMap.get(meritEntity.getSelectedCategoryId());
        return rc != null ? rc.getCategoryCode() : null;
    }

    public String resolveDesignationName(UUID designationId, Map<UUID, MasterPositionsEntity> masterPositionMap) {
        if (designationId == null) return null;
        MasterPositionsEntity mp = masterPositionMap.get(designationId);
        return mp != null ? mp.getPositionName() : null;
    }

    private CandidateAddressEntity resolveCandidateAddress(UUID candidateId,Map<UUID,CandidateAddressEntity> candidateAddressMap){
        if(candidateId == null) return null;
        return candidateAddressMap.getOrDefault(candidateId,null);
    }

    private CandidateCompensationEntity resolveCandidateCompensation(UUID applicationId,Map<UUID,CandidateCompensationEntity> candidateCompensationEntityMap){
        if(applicationId == null) return null;
        return candidateCompensationEntityMap.getOrDefault(applicationId,null);
    }

    private String resolveCandidateStateName(CandidateAddressEntity address,Map<UUID,StateEntity> candidateStateMap){
        if(address == null || address.getStateId() == null) return null;
        StateEntity state = candidateStateMap.get(address.getStateId());
        return state!=null ? state.getStateName() : null;
    }

    private String resolveOfferedStateName(CandidateMeritListEntity meritEntity,Map<UUID,StateEntity> candidateStateMap){
        if(meritEntity == null || meritEntity.getStateId() == null) return null;
        StateEntity state = candidateStateMap.get(meritEntity.getStateId());
        return state!=null ? state.getStateName() : null;
    }

    private String resolveOfferedCityName(CandidateMeritListEntity meritEntity,Map<UUID,CityEntity> candidateCityMap){
        if(meritEntity == null || meritEntity.getCityId() == null) return null;
        CityEntity city = candidateCityMap.get(meritEntity.getCityId());
        return city!=null ? city.getCityName() : null;
    }

    private String resolveCandidateDistrictName(CandidateAddressEntity address,Map<UUID,DistrictEntity> candidateDistrictMap){
        if(address == null || address.getDistrictId() == null) return null;
        DistrictEntity district = candidateDistrictMap.get(address.getDistrictId());
        return district!=null ? district.getDistrictName() : null;
    }

    private JobPositionsEntity resolveJobPosition(CandidateOffersEntity entity,Map<UUID,JobPositionsEntity> jobPositionMap){
        if(entity == null || entity.getCandidateApplication() == null || entity.getCandidateApplication().getPositionId() == null) return null;
        return jobPositionMap.getOrDefault(entity.getCandidateApplication().getPositionId(),null);

    }

    private JobRequisitionsEntity resolveJobRequisition(JobPositionsEntity jobPosition,Map<UUID,JobRequisitionsEntity> jobRequisitionMap){
        if(jobPosition == null || jobPosition.getRequisitionId() == null) return  null;
        return jobRequisitionMap.getOrDefault(jobPosition.getRequisitionId(),null);
    }

    private String resolveDepartmentName(JobPositionsEntity jobPosition,Map<UUID,DepartmentsEntity> departmentsMap){
        if(jobPosition == null || jobPosition.getDeptId() == null) return  null;
        DepartmentsEntity department = departmentsMap.get(jobPosition.getDeptId());
        return department!=null ? department.getDepartmentName() : null;

    }

    private InterviewCentresEntity resolveMedicalCentreName(CandidateOffersEntity entity,Map<UUID,InterviewCentresEntity> medicalCentreMap){
        if(entity == null || entity.getMedicalCenterId() == null) return  null;
        return medicalCentreMap.getOrDefault(entity.getMedicalCenterId(),null);
    }

    private String resolveZonalStateName(InterviewCentresEntity interviewCentre,Map<UUID,ZonalStatesEntity> zonalStateMap){
        if(interviewCentre == null || interviewCentre.getZonalStateId() == null) return null;
        ZonalStatesEntity zonalState = zonalStateMap.get(interviewCentre.getZonalStateId());
        return zonalState!=null ? zonalState.getStateName() : null;
    }

    private String resolveJobGradeCode(JobPositionsEntity jobPosition,Map<UUID,JobGradeEntity> jobGradeEntityMap){
        if(jobPosition == null || jobPosition.getGradeId() == null) return null;
        JobGradeEntity jobGrade = jobGradeEntityMap.get(jobPosition.getGradeId());
        return jobGrade!=null ? jobGrade.getJobGradeCode() : null;
    }

    private BigDecimal resolveJobGradeAmount(JobPositionsEntity jobPosition, Map<UUID,JobGradeEntity> jobGradeEntityMap){
        if(jobPosition == null || jobPosition.getGradeId() == null) return null;
        JobGradeEntity jobGrade = jobGradeEntityMap.get(jobPosition.getGradeId());
        return jobGrade!=null && jobGrade.getMinSalary() != null ? jobGrade.getMinSalary() : null;
    }


    private String resolveJoiningDate(CandidateOffersEntity entity){
        if(entity == null || entity.getJoiningDate() == null) return null;
        return entity.getJoiningDate().format(AppConstants.DD_MM_YYYY);
    }

    private String resolveMedicalTestDate(CandidateOffersEntity entity){
        if(entity == null || entity.getJoiningDate() == null) return null;
        return entity.getJoiningDate().minusDays(6).format(AppConstants.DD_MM_YYYY);
    }
    private String resolveSignatryName(CandidateOffersEntity entity,Map<UUID,UserSignatryEntity> userSignatryEntityMap){
        if(entity == null || entity.getSignatryId() == null) return null;
        UserSignatryEntity userSignatryEntity=userSignatryEntityMap.get(entity.getSignatryId());
        return userSignatryEntity!=null && userSignatryEntity.getSignatryUserName()!=null? userSignatryEntity.getSignatryUserName():null;
    }
    private String resolveSignatoryName(CandidateOffersEntity entity){
        if(entity == null || entity.getSignatoryName() == null) return null;
        return entity.getSignatoryName();
    }

    private String resolveSignatoryDesignation(CandidateOffersEntity entity){
        if(entity == null || entity.getSignatoryDesignation() == null) return null;
        return entity.getSignatoryDesignation();
    }
}
