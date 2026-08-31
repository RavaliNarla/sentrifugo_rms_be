package com.bob.jobportal.service;

import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.commonutil.service.PdfConverterService;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.entity.*;
import com.bob.db.enums.UserRole;
import com.bob.db.repository.*;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.jobportal.model.*;
import com.bob.commonutil.util.AppConstants;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;


@Service
public class DocumentDownloadService {

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CandidateAddressRepository candidateAddressRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private  InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private InterviewPanelsRepository interviewPanelsRepository;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private TemplateEngine templateEngine;


    @Autowired
    private ExcelTemplateService excelTemplateService;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private PdfConverterService pdfConverterService;

    @Autowired
    private CandidateCompensationService compensationService;

    @Autowired
    private CandidateCompensationRepository candidateCompensationRepository;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private InterviewScheduleStagingRepository interviewScheduleStagingRepository;

    @Autowired
    private CandidateRankingResultsRepository candidateRankingResultsRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public byte[] getUserDetailsBasedOnRequestScreen(CommonScreeningDocumentDownloadRequestModel model){

        List<JobPositionsEntity> jobPositions = positionsRepository.findAllById(model.getPositionIds());
        Map<UUID,JobPositionsEntity> jobPositionMap = jobPositions.stream().collect(Collectors.toMap(JobPositionsEntity::getId, Function.identity()));
        List<UUID> requisitionIds = jobPositions.stream().map(JobPositionsEntity::getRequisitionId).distinct().toList();
        if(requisitionIds.size() != 1){
            throw new IllegalArgumentException("Positions should belong to same requisition");
        }
        JobRequisitionsEntity jobRequisition = jobRequisitionsRepository.findById(requisitionIds.get(0)).orElseThrow(()->new ResourceNotFoundException("Job Requisition Not Found"));
        String requisitionName = jobRequisition.getRequisitionCode()+":"+jobRequisition.getRequisitionTitle();
        List<UUID> masterPositionIds = jobPositions.stream().map(JobPositionsEntity::getMasterPositionId).distinct().toList();
        Map<UUID,String> masterPositionsMap = masterPositionsRepository.findAllById(masterPositionIds).stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, MasterPositionsEntity::getPositionName));



        byte[] bytes = null;

        if(AppConstants.DOWNLOAD_DOC_TYPE_PDF.equalsIgnoreCase(model.getDocumentType())){
            Context context = new Context();
            String html = "";
            context.setVariable("requisitionTitle", requisitionName);
            if(AppConstants.CANDIDATE_POOL.equalsIgnoreCase(model.getScreenName())){
                List<CandidatePoolDownloadModel> candidateScreeningDetails = getAllCandidateScreeningDetails(model,jobPositionMap,masterPositionsMap);
                context.setVariable("candidateDetails", candidateScreeningDetails);
                html = templateEngine.process("CandidatePool", context);
                bytes = pdfConverterService.convertHtmlStringToPdf(html);
            }else if (AppConstants.INTERVIEW_POOL.equalsIgnoreCase(model.getScreenName())) {
                List<InterviewPoolDownloadModel> interviewPoolDetails = getAllInterviewPoolDetails(model,jobPositionMap,masterPositionsMap);
                context.setVariable("interviewPoolDetails", interviewPoolDetails);
                html = templateEngine.process("InterviewPool", context);
                bytes = pdfConverterService.convertHtmlStringToPdf(html);
            }else if(AppConstants.SCHEDULE_POOL.equalsIgnoreCase(model.getScreenName())) {
                List<SchedulePoolDownloadModel> schedulePoolDetails = getAllSchedulePoolDetails(model, jobPositionMap, masterPositionsMap);
                context.setVariable("schedulePoolDetails", schedulePoolDetails);
                html = templateEngine.process("SchedulePool", context);
                bytes = pdfConverterService.convertHtmlStringToPdf(html);
            }
            else if(AppConstants.COMPENSATION_POOL.equalsIgnoreCase(model.getScreenName())){
                List<CompensationPoolDownloadModel> compensationPoolDetails = getAllCompensationPoolDetails(model,jobPositionMap,masterPositionsMap);
                context.setVariable("compensationPoolDetails", compensationPoolDetails);
                html = templateEngine.process("CompensationPool", context);
                bytes = pdfConverterService.convertHtmlStringToPdf(html);
            }
            else{
                throw new IllegalArgumentException("Invalid Screen Name");
            }
        }else if(AppConstants.DOWNLOAD_DOC_TYPE_XLSX.equalsIgnoreCase(model.getDocumentType())){
            if(AppConstants.CANDIDATE_POOL.equalsIgnoreCase(model.getScreenName())){
                List<CandidatePoolDownloadModel> candidateScreeningDetails = getAllCandidateScreeningDetails(model,jobPositionMap,masterPositionsMap);
                ExcelTemplateFile templateFile = excelTemplateService.dtoListToExcelFile(CandidatePoolDownloadModel.class, candidateScreeningDetails,requisitionName);
                bytes = templateFile.getFileContent();
            } else if (AppConstants.INTERVIEW_POOL.equalsIgnoreCase(model.getScreenName())) {
                List<InterviewPoolDownloadModel> interviewPoolDetails = getAllInterviewPoolDetails(model,jobPositionMap,masterPositionsMap);
                ExcelTemplateFile templateFile = excelTemplateService.dtoListToExcelFile(InterviewPoolDownloadModel.class, interviewPoolDetails,requisitionName);
                bytes = templateFile.getFileContent();
            }else if(AppConstants.SCHEDULE_POOL.equalsIgnoreCase(model.getScreenName())){
                List<SchedulePoolDownloadModel> schedulePoolDetails = getAllSchedulePoolDetails(model,jobPositionMap,masterPositionsMap);
                ExcelTemplateFile templateFile = excelTemplateService.dtoListToExcelFile(SchedulePoolDownloadModel.class,schedulePoolDetails,requisitionName);
                bytes = templateFile.getFileContent();
            }
            else if(AppConstants.COMPENSATION_POOL.equalsIgnoreCase(model.getScreenName())){
                List<CompensationPoolDownloadModel> compensationPoolDetails = getAllCompensationPoolDetails(model,jobPositionMap,masterPositionsMap);
                ExcelTemplateFile templateFile = excelTemplateService.dtoListToExcelFile(CompensationPoolDownloadModel.class, compensationPoolDetails,requisitionName);
                bytes = templateFile.getFileContent();
            }
            else{
                throw new IllegalArgumentException("Invalid Screen Name");
            }
        }else{
            throw new IllegalArgumentException("Document Type Not Supported");
        }

        return bytes;
    }

    //commented out compensation pool as it is not need now
    private List<CompensationPoolDownloadModel> getAllCompensationPoolDetails(CommonScreeningDocumentDownloadRequestModel model,Map<UUID,JobPositionsEntity> jobPositionMap,Map<UUID,String> masterPositionMap) {
        Specification<CandidateCompensationEntity> compSpecification=compensationService.fetchSpecificationWithFilters(null,model.getCompensationStatuses(),model.getPositionIds());
        List<CandidateCompensationEntity> candidateCompensationEntities=candidateCompensationRepository.findAll(compSpecification);
        String role=securityUtils.getCurrentUserRole();
        List<CompensationPoolDownloadModel> compensationPoolDownloadModels=candidateCompensationEntities.stream().map(
                (compensationEntity)->{
                    String comments = "-";
                    if(role.equalsIgnoreCase(UserRole.RECRUITER.toString())){
                        comments= compensationEntity.getRecruiterComments();
                    }
                    else if(role.equalsIgnoreCase(UserRole.COMMITTEE_MEMBER.toString())){
                        comments= compensationEntity.getPanelComments();
                    }
                    CandidateApplicationsEntity application=compensationEntity.getApplication();
                    CandidateProfileEntity profile=compensationEntity.getCandidateProfile();
                    return CompensationPoolDownloadModel.builder()
                            .fullName(commonUtilityProvider.buildFullName(profile))
                            .applicationNo(application.getApplicationNo())
                            .currentCtc(compensationEntity.getCurrentCtc()!= null ? String.valueOf(compensationEntity.getCurrentCtc()) : null)
                            .expectedCtc(compensationEntity.getExpectedCtc()!=null ? String.valueOf(compensationEntity.getExpectedCtc()) : null)
                            .agreedCtc(compensationEntity.getAgreedCtc()!=null ?String.valueOf(compensationEntity.getAgreedCtc()) : null)
                            .comments(comments)
                            .hike(compensationEntity.getHike()!=null ? String.valueOf(compensationEntity.getHike()):null)
                            .negotiation(compensationEntity.getCompensationStatus().toString())
                            .build();
                }
        ).toList();
        return compensationPoolDownloadModels;
    }

    public List<CandidatePoolDownloadModel> getAllCandidateScreeningDetails(CommonScreeningDocumentDownloadRequestModel model,Map<UUID,JobPositionsEntity> jobPositionMap,Map<UUID,String> masterPositionMap) {
        List<CandidateApplicationsEntity> candidateApplications = candidateApplicationsRepository.findAllAppliedCandidatesByPositionId(
                model.getPositionIds(),
                model.getCandidateApplicationStatuses(),
                model.getStateId(),
                model.getCategoryId()
        );


        List<UUID> applicationIds = candidateApplications.stream().map(CandidateApplicationsEntity::getId).toList();
        List<UUID> candidateIds = candidateApplications.stream().map(CandidateApplicationsEntity::getCandidateId).toList();

        //get all the details based on candiatedIds
        List<CandidateProfileEntity>   candidateProfiles = candidateProfileRepository.findAllByCandidateIdIn(candidateIds);
        List<CandidateAddressEntity>   candidateAddressEntities = candidateAddressRepository.findAllByCandidateIdIn(candidateIds);
        List<WorkExperienceEntity>     workExperienceEntities = workExperienceRepository.findAllByCandidateIdIn(candidateIds);

        //get all the states and reservation categories based on candidate details
        Set<UUID> candidateCategoryIds = candidateProfiles.stream().map(CandidateProfileEntity::getReservationCategoryId).collect(Collectors.toSet());
        Map<String,CandidateLocationPreferenceEntity> locPrefMap=candidateLocationPreferencesRepository.findByCandidateAndPosition(candidateIds,model.getPositionIds())
                .stream().collect(Collectors.toMap(clp->clp.getCandidateId()+"_"+clp.getPositionId(),Function.identity()));
        Set<UUID> candidateStateIds = locPrefMap.values().stream().map(CandidateLocationPreferenceEntity::getStatePreference1).collect(Collectors.toSet());
        List<StateEntity> states = stateRepository.findAllById(candidateStateIds);
        List<ReservationCategoriesEntity> reservationCategories = reservationCategoriesRepository.findAllById(candidateCategoryIds);

        Map<UUID,CandidateProfileEntity> candidateProfileEntityMap = candidateProfiles.stream()
                .collect(Collectors.toMap((CandidateProfileEntity::getCandidateId), Function.identity()));

        Map<UUID,CandidateRankingResultsEntity> candidateRankingResultsEntityMap = candidateRankingResultsRepository.findByApplicationIdIn(applicationIds)
                .stream()
                .collect(Collectors.toMap(
                        CandidateRankingResultsEntity::getApplicationId,
                        Function.identity()
                ));


        Map<UUID,List<WorkExperienceEntity>> wordExperienceMap = workExperienceEntities.stream()
                .collect(Collectors.groupingBy(WorkExperienceEntity::getCandidateId));

        //state-id,name
        Map<UUID,String> stateMap = states.stream()
                .collect(Collectors.toMap(StateEntity::getId,StateEntity::getStateName));
        //categoryId,name
        Map<UUID,String> reservationCategoryMap = reservationCategories.stream()
                .collect(Collectors.toMap(ReservationCategoriesEntity::getId,ReservationCategoriesEntity::getCategoryName));



        List<CandidatePoolDownloadModel> candidatePoolDownloadModels = candidateApplications.stream().
                map((applicationEntity)->{
                    CandidateProfileEntity profile = candidateProfileEntityMap.get(applicationEntity.getCandidateId()) ;
                    JobPositionsEntity jobPosition = jobPositionMap.get(applicationEntity.getPositionId());
                    int totalMonths = commonUtilityProvider.calculateTotalMonths(wordExperienceMap.get(applicationEntity.getCandidateId()));
                    CandidateRankingResultsEntity rankingResult = candidateRankingResultsEntityMap.getOrDefault(applicationEntity.getId(),null);
                    BigDecimal candidateScore =model.isRank() && rankingResult!=null ? rankingResult.getFinalScore() : null;
                    Float workExpMonths =Math.round((totalMonths/12.0f)*10.0f)/10.0f;
                    return CandidatePoolDownloadModel.builder()
                            .position(masterPositionMap.get(jobPosition.getMasterPositionId()))
                            .fullName(commonUtilityProvider.buildFullName(profile))
                            .applicationNo(applicationEntity.getApplicationNo())
                            .status(applicationEntity.getApplicationStatus().toString())
                            .workExperience(workExpMonths)
                            .location(stateMap.getOrDefault(locPrefMap.get(applicationEntity.getCandidateId()+"_"+applicationEntity.getPositionId()).getStatePreference1(),"-"))
                            .category(reservationCategoryMap.get(profile.getReservationCategoryId()))
                            .finalScore(candidateScore)
                            .build();
                }).sorted(
                Comparator.comparing(
                                CandidatePoolDownloadModel::getPosition,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                        .thenComparing(
                                CandidatePoolDownloadModel::getFinalScore,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .toList();

        if(model.isRank()){
            Map<String,List<CandidatePoolDownloadModel>> groupedCandidatePoolDownloadModel = candidatePoolDownloadModels.stream()
                    .collect(Collectors.groupingBy(
                            CandidatePoolDownloadModel::getPosition
                    ));
            groupedCandidatePoolDownloadModel.forEach((key, models) -> {
                for (int i = 0; i < models.size(); i++) {
                    models.get(i).setRank(i + 1);
                }
            });
        }

        return candidatePoolDownloadModels;
    }


    public List<InterviewPoolDownloadModel> getAllInterviewPoolDetails(CommonScreeningDocumentDownloadRequestModel model,Map<UUID,JobPositionsEntity> jobPositionMap,Map<UUID,String> masterPositionMap){

        Specification<InterviewScheduleEntity> spec = (root, query, criteriaBuilder)->{
            List<Predicate> predicates = new ArrayList<>();

            if(model.getInterviewSchedulingStatuses() != null && !model.getInterviewSchedulingStatuses().isEmpty()){
                predicates.add(root.get("interviewStatus").in(model.getInterviewSchedulingStatuses()));
            }


            Root<CandidateApplicationsEntity> applicactionRoot = query.from(CandidateApplicationsEntity.class);

            predicates.add(criteriaBuilder.equal(root.get("applicationId"),applicactionRoot.get("id")));
            if(model.getPositionIds() != null && !model.getPositionIds().isEmpty()){
                predicates.add(applicactionRoot.get("positionId").in(model.getPositionIds()));
            }
            //order by application no
            query.orderBy(criteriaBuilder.asc(root.get("createdDate")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };

        List<InterviewScheduleEntity> interviewScheduleEntities = interviewScheduleRepository.findAll(spec);

        List<UUID> candidateIds = interviewScheduleEntities.stream().map(InterviewScheduleEntity::getCandidateId).toList();
        List<UUID> applicationIds = interviewScheduleEntities.stream().map(InterviewScheduleEntity::getApplicationId).toList();
        Set<UUID> panelIds = interviewScheduleEntities.stream().map(InterviewScheduleEntity::getPanelId).collect(Collectors.toSet());
        Set<UUID> zonalOfficeIds = interviewScheduleEntities.stream().map(InterviewScheduleEntity::getZonalOfficeId).collect(Collectors.toSet());

        List<CandidateProfileEntity> candidateProfiles = candidateProfileRepository.findAllByCandidateIdIn(candidateIds);
        List<CandidateApplicationsEntity> candidateApplicationsEntities = candidateApplicationsRepository.findAllById(applicationIds);
        List<InterviewPanelsEntity> interviewPanelsEntities = interviewPanelsRepository.findAllById(panelIds);
        List<InterviewCentresEntity> interviewCentresEntities = interviewCentresRepository.findAllById(zonalOfficeIds);

        //candidateId, profileEntity
        Map<UUID,CandidateProfileEntity> candidateProfileMap = candidateProfiles.stream().collect(
                Collectors.toMap(
                        (CandidateProfileEntity::getCandidateId),
                        Function.identity()
                )
        );


        Map<UUID,CandidateApplicationsEntity> applicationMap = candidateApplicationsEntities.stream().collect(
                Collectors.toMap(
                        (CandidateApplicationsEntity::getId),
                        Function.identity()
                )
        );

        //panelId, panelName
        Map<UUID,String> panelMap = interviewPanelsEntities.stream().collect(
                Collectors.toMap(
                        (InterviewPanelsEntity::getId),
                        (InterviewPanelsEntity::getPanelName)
                )
        );

        //zonalOfficeId, zone
        Map<UUID,String> zonalOfficeMap  = interviewCentresEntities.stream().collect(
                Collectors.toMap(
                        (InterviewCentresEntity::getId),
                        (InterviewCentresEntity::getDisplayName)
        ));
        Map<UUID, Integer> positionOrderMap = new HashMap<>();

        if (model.getPositionIds() != null) {

            for (int i = 0; i < model.getPositionIds().size(); i++) {
                positionOrderMap.put(model.getPositionIds().get(i), i);
            }
        }

        interviewScheduleEntities = interviewScheduleEntities.stream()
                .sorted(
                        Comparator
                                .comparingInt((InterviewScheduleEntity schedule) -> {
                                    UUID positionId =
                                            applicationMap.get(schedule.getApplicationId())
                                                    .getPositionId();

                                    return positionOrderMap.getOrDefault(
                                            positionId,
                                            Integer.MAX_VALUE
                                    );
                                })
//                                .thenComparing(schedule ->
//                                                zonalOfficeMap.get(schedule.getZonalOfficeId()),
//                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
//                                )
//                                .thenComparing(schedule->
//                                        panelMap.get(schedule.getPanelId()),
//                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
//                                )
                                .thenComparing(
                                        InterviewScheduleEntity::getInterviewStartAt
                                )
                )
                .toList();

        List<InterviewPoolDownloadModel> interviewPoolDownloadModels = interviewScheduleEntities.stream().map(
                (interviewScheduleEntity) ->{
                    CandidateProfileEntity profile = candidateProfileMap.get(interviewScheduleEntity.getCandidateId());
                    CandidateApplicationsEntity application = applicationMap.get(interviewScheduleEntity.getApplicationId());
                    JobPositionsEntity jobPosition = jobPositionMap.get(application.getPositionId());
                    String interviewTime = interviewScheduleEntity.getInterviewStartAt().toLocalTime().toString()+" - "+interviewScheduleEntity.getInterviewStartAt().toLocalTime().plusMinutes(interviewScheduleEntity.getInterviewDurationMinutes()).toString();
                    return InterviewPoolDownloadModel.builder()
                            .positionName(masterPositionMap.get(jobPosition.getMasterPositionId()))
                            .fullName(commonUtilityProvider.buildFullName(profile))
                            .applicationNo(application.getApplicationNo())
                            .interviewDate(interviewScheduleEntity.getInterviewStartAt().toLocalDate().format(AppConstants.DD_MM_YYYY))
                            .interviewTime(interviewTime)
                            .interviewStatus(interviewScheduleEntity.getInterviewStatus().toString())
                            .zone(zonalOfficeMap.get(interviewScheduleEntity.getZonalOfficeId()))
                            .panelDetails(panelMap.get(interviewScheduleEntity.getPanelId()))
                            .score(interviewScheduleEntity.getFinalScore() != null ?interviewScheduleEntity.getFinalScore() : null)
                            .build();
                }
        ).toList();



        return interviewPoolDownloadModels;
    }

    public List<SchedulePoolDownloadModel> getAllSchedulePoolDetails(CommonScreeningDocumentDownloadRequestModel model, Map<UUID,JobPositionsEntity> jobPositionMap, Map<UUID,String> masterPositionMap) {
        //specification for filter
        Specification<InterviewScheduleStagingEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(model.getPositionIds()!=null && !model.getPositionIds().isEmpty()){

                predicates.add(root.get("application").get("positionId").in(model.getPositionIds()));
            }

            if (model.getInterviewSchedulingApprovalStatuses() != null && !model.getInterviewSchedulingApprovalStatuses().isEmpty()) {
                predicates.add(root.get("interviewSchedulingApprovalStatus").in(model.getInterviewSchedulingApprovalStatuses()));
            }

            return  criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<InterviewScheduleStagingEntity> interviewScheduleStagingEntities = interviewScheduleStagingRepository.findAll(spec);

        List<UUID> candidateIds = interviewScheduleStagingEntities.stream().map(InterviewScheduleStagingEntity::getCandidateId).distinct().toList();
        Set<UUID> panelIds = interviewScheduleStagingEntities.stream().map(InterviewScheduleStagingEntity::getPanelId).collect(Collectors.toSet());
        Set<UUID> zonalOfficeIds = interviewScheduleStagingEntities.stream().map(InterviewScheduleStagingEntity::getZonalOfficeId).collect(Collectors.toSet());

        Map<UUID,CandidateProfileEntity> candidateProfileMap = candidateProfileRepository.findAllByCandidateIdIn(candidateIds)
                .stream().collect(Collectors.toMap(CandidateProfileEntity::getCandidateId, Function.identity()));
        Map<UUID,String> panelNameMap = interviewPanelsRepository.findAllById(panelIds)
                .stream().collect(Collectors.toMap(InterviewPanelsEntity::getId, InterviewPanelsEntity::getPanelName));
        Map<UUID,String> zonalOfficeMap = interviewCentresRepository.findAllById(zonalOfficeIds)
                .stream().collect(Collectors.toMap(InterviewCentresEntity::getId, InterviewCentresEntity::getDisplayName));
        Map<UUID, Integer> positionOrderMap = new HashMap<>();

        if (model.getPositionIds() != null) {

            for (int i = 0; i < model.getPositionIds().size(); i++) {
                positionOrderMap.put(model.getPositionIds().get(i), i);
            }
        }

        interviewScheduleStagingEntities =
                interviewScheduleStagingEntities.stream()
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                (InterviewScheduleStagingEntity schedule) -> {

                                                    UUID positionId =
                                                            schedule.getApplication()
                                                                    .getPositionId();

                                                    return positionOrderMap.getOrDefault(
                                                            positionId,
                                                            Integer.MAX_VALUE
                                                    );
                                                }
                                        )
//                                        .thenComparing(
//                                                schedule ->
//                                                        zonalOfficeMap.get(
//                                                                schedule.getZonalOfficeId()
//                                                        ),
//                                                Comparator.nullsLast(
//                                                        String.CASE_INSENSITIVE_ORDER
//                                                )
//                                        )
//                                        .thenComparing(
//                                                schedule->
//                                                        panelNameMap.get(
//                                                                schedule.getPanelId()
//                                                        ),
//                                                Comparator.nullsLast(
//                                                        String.CASE_INSENSITIVE_ORDER
//                                                )
//                                        )
                                        .thenComparing(
                                                InterviewScheduleStagingEntity::getInterviewStartAt
                                        )
                        )
                        .toList();
        List<SchedulePoolDownloadModel> schedulePoolDownloadModels = interviewScheduleStagingEntities.stream().map(
                (interviewScheduleStagingEntity) -> {
                    CandidateProfileEntity profile = candidateProfileMap.get(interviewScheduleStagingEntity.getCandidateId());
                    JobPositionsEntity jobPosition = jobPositionMap.get(interviewScheduleStagingEntity.getApplication().getPositionId());
                    String interviewTime = interviewScheduleStagingEntity.getInterviewStartAt().toLocalTime().format(AppConstants.HH_mm) + " - " + interviewScheduleStagingEntity.getInterviewEndAt().format(AppConstants.HH_mm);
                    return SchedulePoolDownloadModel.builder()
                            .positionName(masterPositionMap.get(jobPosition.getMasterPositionId()))
                            .fullName(commonUtilityProvider.buildFullName(profile))
                            .applicationNo(interviewScheduleStagingEntity.getApplication().getApplicationNo())
                            .interviewDate(interviewScheduleStagingEntity.getInterviewStartAt().toLocalDate().format(AppConstants.DD_MM_YYYY))
                            .interviewTime(interviewTime)
                            .scheduleStatus(interviewScheduleStagingEntity.getInterviewSchedulingApprovalStatus().toString())
                            .zone(zonalOfficeMap.get(interviewScheduleStagingEntity.getZonalOfficeId()))
                            .panelDetails(panelNameMap.get(interviewScheduleStagingEntity.getPanelId()))
                            .build();
                }
        ).toList();
        return schedulePoolDownloadModels;
    }
}






