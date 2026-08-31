package com.bob.jobportal.scheduler;

import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.CommonMailService;
import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.service.PdfConverterService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.CustomMultipartFile;
import com.bob.db.entity.*;
import com.bob.db.enums.*;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;



@Service
@Slf4j
public class ReminderScheduler {

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidateScreeningRepository candidateScreeningRepository;

    @Autowired
    private CandidateApplicationDocumentVerificationRepository candidateApplicationDocumentVerificationRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private MailSenderHelper mailSenderHelper;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;


    @Autowired
    private CandidateAddressRepository candidateAddressRepository;


    @Autowired
    private StateRepository stateRepository;


    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private PositionsRepository jobPositionsRepository;


    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;


    @Autowired
    private MasterPositionsRepository masterPositionsRepository;


    @Autowired
    private DepartmentsRepository departmentsRepository;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private PdfConverterService pdfConverterService;

    @Autowired
    private CommonMailService mailService;

    @Scheduled(cron = "0 0 18 * * ?", zone = "Asia/Kolkata")
    public void triggerPendingMails() throws MessagingException, IOException {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime prevDays = today.minusDays(3);
        List<InterviewScheduleEntity> interviewScheduleEntities=interviewScheduleRepository.findByInterviewStatusAndMailSendStatusAndCreatedDateBetween(
                InterviewSchedulingStatus.SCHEDULED,
                MailSendStatus.FAILED,prevDays,today
        );

        if(interviewScheduleEntities.isEmpty()){
            log.info("No pending mails found");
            throw new ResourceNotFoundException("No pending mails found");
        }
        List<UUID> applicationIds=interviewScheduleEntities.stream().map(InterviewScheduleEntity::getApplicationId).toList();
        log.info("Applications:{}",applicationIds);
        List<CandidateApplicationsEntity> candidateApplicationsEntityList=candidateApplicationsRepository.findAllById(applicationIds);
        log.info("Starting to send pending mails for {} interview schedules",interviewScheduleEntities.size());
        mailSenderHelper.sendEmails(interviewScheduleEntities,candidateApplicationsEntityList);
    }


    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Kolkata")
    public void triggerScreeningRemainderMails(){

        List<CandidateApplicationsEntity> applications = candidateApplicationsRepository.findAllByApplicationStatusIn(List.of(
                CandidateApplicationStatus.DISCREPANCY,
                CandidateApplicationStatus.PROVISIONALLY_APPROVED
        ));

        //filter all the discrepancy applicationIds
        List<UUID> discrepancyApplicationIds= applications.stream().
        filter((currApplication)->currApplication.getApplicationStatus() == CandidateApplicationStatus.DISCREPANCY)
        .map(CandidateApplicationsEntity::getId)
                .toList();

        //filter all the provisonally approved applicationIds
        List<UUID> provisionallyApprovedApplicationIds = applications.stream().
                filter((currApplication)->currApplication.getApplicationStatus() == CandidateApplicationStatus.PROVISIONALLY_APPROVED)
                .map(CandidateApplicationsEntity::getId)
                .toList();

        LocalDate today = LocalDate.now();

        //candidate screening entites for screening
        List<CandidateScreeningEntity> candidateScreeningEntities = candidateScreeningRepository.findAllByApplicationIdIn(discrepancyApplicationIds);

        //interview schedule entites for zonal screening
        List<InterviewScheduleEntity> provisionallyApprovedInterviewSchedules = interviewScheduleRepository.findByApplicationIdIn(provisionallyApprovedApplicationIds);

        //for sending screening remainder for those within 3 days of submit date
        List<CandidateScreeningEntity>  candidateScreeningEntitiesToRemind =candidateScreeningEntities.stream().
                filter(
                        (currScreeningEntity)-> (
                            commonUtilityProvider.hasThreeDayGap(today,currScreeningEntity.getSubmitBeforeDate())
                        )
                ).toList();

        //for sending zonal screening remainder for those within in 3 days of zonal submit date
        List<InterviewScheduleEntity> provisionallyApprovedInterviewSchedulesToRemind =provisionallyApprovedInterviewSchedules.stream()
                .filter((currScheduleEntity) -> (
                        commonUtilityProvider.hasThreeDayGap(today,currScheduleEntity.getZonalSubmitBeforeDate()))
                )
                .toList();

        //map to collect all the applications that need to be reminded(screening + zonal screening)
        Map<UUID,CandidateApplicationsEntity> applicationsToRemindMap = new HashMap<>();




        if(!candidateScreeningEntitiesToRemind.isEmpty()){

            Set<UUID> screeningApplicationIdsToRemind = candidateScreeningEntitiesToRemind.stream()
                    .map(CandidateScreeningEntity::getApplicationId)
                    .collect(Collectors.toSet());

            //from all the applications collecting only the applications that need to reminded for screenig
            applications.stream()
                    .filter((currApplication)-> screeningApplicationIdsToRemind.contains(currApplication.getId()))
                    .forEach((currApplication)-> {
                                applicationsToRemindMap.put(currApplication.getId(), currApplication);
                            }
                    );
        }

        if(!provisionallyApprovedInterviewSchedulesToRemind.isEmpty()){

            Set<UUID> provisionallyApprovedApplicationIdsToRemind = provisionallyApprovedInterviewSchedulesToRemind.stream()
                    .map(InterviewScheduleEntity::getApplicationId)
                    .collect(Collectors.toSet());

            //from all the applications collecting only the applications that need to reminded for zonal screenig
            applications.stream()
                    .filter((currApplication) -> provisionallyApprovedApplicationIdsToRemind.contains(currApplication.getId()))
                    .forEach((currApplication)->{
                        applicationsToRemindMap.put(currApplication.getId(), currApplication);
                    });
        }

        //all the disticnt candidateIds from all the applications that need to be reminded(screeing + zonal screening)
        List<UUID> candidateIds = applicationsToRemindMap.values().stream()
                .map(CandidateApplicationsEntity::getCandidateId)
                .distinct()
                .toList();

        //all the profiles of applications that need to reminded(screeing + zonal screening)
        Map<UUID,CandidateProfileEntity> candidateProfileMap = candidateProfileRepository.findAllByCandidateIdIn(candidateIds)
                .stream()
                .collect(Collectors.toMap(CandidateProfileEntity::getCandidateId,
                        Function.identity()
                        ));

        //all the profiles of address that need to reminded(screeing + zonal screening)
        Map<UUID, CandidateAddressEntity> candidateAddressMap = candidateAddressRepository.findAllByCandidateIdIn(candidateIds)
                .stream()
                .collect(Collectors.toMap(
                        CandidateAddressEntity::getCandidateId,
                        Function.identity()
                ));


        Set<UUID> stateIds = candidateAddressMap.values()
                .stream()
                .map(CandidateAddressEntity::getStateId)
                .collect(Collectors.toSet());

        Set<UUID> districtIds = candidateAddressMap.values()
                .stream()
                .map(CandidateAddressEntity::getDistrictId)
                .collect(Collectors.toSet());

        Map<UUID, String> candidateStateMap = stateRepository.findAllById(stateIds)
                .stream().collect(
                        Collectors.toMap(
                                StateEntity::getId,
                                StateEntity::getStateName
                        )
                );

        Map<UUID, String> candidateDistrictMap = districtRepository.findAllById(districtIds)
                .stream().collect(
                        Collectors.toMap(
                                DistrictEntity::getId,
                                DistrictEntity::getDistrictName
                        )
                );

        Set<UUID> jobPositionIds = applicationsToRemindMap.values()
                .stream()
                .map(CandidateApplicationsEntity::getPositionId)
                .collect(Collectors.toSet());

        //all the positions of applications that need to reminded(screeing + zonal screening)
        Map<UUID, JobPositionsEntity> jobPositionsMap = jobPositionsRepository.findAllById(jobPositionIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                JobPositionsEntity::getId,
                                Function.identity()
                        )
                );

        Set<UUID> masterPositionIds = jobPositionsMap
                .values()
                .stream()
                .map(JobPositionsEntity::getMasterPositionId)
                .collect(Collectors.toSet());

        Map<UUID, String> masterPositionNameMap = masterPositionsRepository.findAllById(masterPositionIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                MasterPositionsEntity::getId,
                                MasterPositionsEntity::getPositionName
                        )
                );
        Set<UUID> departmentIds = jobPositionsMap.values()
                .stream()
                .map(JobPositionsEntity::getDeptId)
                .collect(Collectors.toSet());


        Set<UUID> jobRequisitionIds = jobPositionsMap.values()
                .stream()
                .map(JobPositionsEntity::getRequisitionId)
                .collect(Collectors.toSet());


        Map<UUID, String> departmentNameMap = departmentsRepository.findAllById(departmentIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                DepartmentsEntity::getId,
                                DepartmentsEntity::getDepartmentName

                        )
                );

        Map<UUID, LocalDate> requisitionStartDateMap = jobRequisitionsRepository.findAllById(jobRequisitionIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                JobRequisitionsEntity::getId,
                                JobRequisitionsEntity::getStartDate
                        )
                );
        List<CandidateApplicationDocumentVerificationEntity> candidateApplicationDocumentVerificationEntities =
                candidateApplicationDocumentVerificationRepository.findAllByApplicationIdInAndDocScreeningStatusOrZonalHrDocStatus(applicationsToRemindMap.keySet(),DocumentScreeningStatus.REJECTED,
                        DocumentZonalVerificationStatus.REJECTED);

        Set<UUID> candidateDocumentIds = candidateApplicationDocumentVerificationEntities.stream()
                .map(CandidateApplicationDocumentVerificationEntity::getCandidateDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, CandidateDocumentStoreEntity> candidateDocumentStoreMap = candidateDocumentStoreRepository.findAllById(candidateDocumentIds)
                .stream()
                .collect(Collectors.toMap(
                        CandidateDocumentStoreEntity::getId,
                        Function.identity()
                ));
        Set<UUID> documentTypeIds = candidateDocumentStoreMap.values()
                .stream()
                .filter(Objects::nonNull)
                .map(CandidateDocumentStoreEntity::getDocumentId)
                .collect(Collectors.toSet());
        Map<UUID, String> documentNamesMap = documentTypesRepository.findAllById(documentTypeIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                DocumentTypesEntity::getId,
                                DocumentTypesEntity::getDocumentName
                        )
                );

        // Screening-specific rejected docs map:
        // additional required docs do not have candidate_document_id, so use display_name as document name.
        List<CandidateApplicationDocumentVerificationEntity> screeningRejectedDocs = candidateApplicationDocumentVerificationEntities.stream()
                .filter(doc -> DocumentScreeningStatus.REJECTED == doc.getDocScreeningStatus())
                .toList();

        Set<UUID> screeningCandidateDocumentIds = screeningRejectedDocs.stream()
                .map(CandidateApplicationDocumentVerificationEntity::getCandidateDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, CandidateDocumentStoreEntity> screeningCandidateDocumentStoreMap = candidateDocumentStoreRepository.findAllById(screeningCandidateDocumentIds)
                .stream()
                .collect(Collectors.toMap(
                        CandidateDocumentStoreEntity::getId,
                        Function.identity()
                ));

        Set<UUID> screeningDocumentTypeIds = screeningCandidateDocumentStoreMap.values()
                .stream()
                .filter(Objects::nonNull)
                .map(CandidateDocumentStoreEntity::getDocumentId)
                .collect(Collectors.toSet());

        Map<UUID, String> screeningDocumentNamesMap = documentTypesRepository.findAllById(screeningDocumentTypeIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                DocumentTypesEntity::getId,
                                DocumentTypesEntity::getDocumentName
                        )
                );

        Map<UUID, List<Map<String, String>>> screeningRejectedDocsMapWithNamesAndComments =
                screeningRejectedDocs
                        .stream().collect(
                                Collectors.groupingBy(
                                        CandidateApplicationDocumentVerificationEntity::getApplicationId,
                                        Collectors.mapping(doc -> {
                                            CandidateDocumentStoreEntity candidateDocumentStore = screeningCandidateDocumentStoreMap.get(doc.getCandidateDocumentId());
                                            String documentName;
                                            if (candidateDocumentStore != null) {
                                                documentName = screeningDocumentNamesMap.getOrDefault(
                                                        candidateDocumentStore.getDocumentId(),
                                                        AppConstants.UNKNOWN_DOCUMENT
                                                );
                                            } else if (doc.getDisplayName() != null && !doc.getDisplayName().isBlank()) {
                                                documentName = doc.getDisplayName();
                                            } else {
                                                documentName = AppConstants.UNKNOWN_DOCUMENT;
                                            }

                                            Map<String, String> docMap = new HashMap<>();
                                            docMap.put(AppConstants.DOCUMENT_NAME, documentName);
                                            docMap.put(AppConstants.COMMENTS, doc.getDocScreeningComments());
                                            docMap.put(AppConstants.ZONAL_HR_COMMENTS, doc.getZonalHrDocComments());

                                            return docMap;

                                        }, Collectors.toList())
                                )
                        );

        Map<UUID, List<Map<String, String>>> rejectedDocsMapWithNamesAndComments =
                candidateApplicationDocumentVerificationEntities
                        .stream().collect(
                                Collectors.groupingBy(
                                        CandidateApplicationDocumentVerificationEntity::getApplicationId,
                                        Collectors.mapping(doc -> {
                                            CandidateDocumentStoreEntity candidateDocumentStore = candidateDocumentStoreMap.get(doc.getCandidateDocumentId());
                                            String documentName = candidateDocumentStore != null ? documentNamesMap.getOrDefault(
                                                    candidateDocumentStore.getDocumentId(), AppConstants.UNKNOWN_DOCUMENT
                                            ) : AppConstants.UNKNOWN_DOCUMENT;

                                            Map<String, String> docMap = new HashMap<>();
                                            docMap.put(AppConstants.DOCUMENT_NAME, documentName);
                                            docMap.put(AppConstants.COMMENTS, doc.getDocScreeningComments());
                                            docMap.put(AppConstants.ZONAL_HR_COMMENTS, doc.getZonalHrDocComments());

                                            return docMap;

                                        }, Collectors.toList())
                                )
                        );

        Map<UUID, Map<String, String>> discrepancyListPerApplication
                = candidateScreeningEntitiesToRemind.stream()
                .collect(
                        Collectors.toMap(
                                CandidateScreeningEntity::getApplicationId,
                                (candidateScreening) -> {
                                    Map<String, String> discrepancyList = new HashMap<>();
                                    if (candidateScreening.getIsWorkCriteriaMet() == CandidateScreeningCriteriaStatus.DISCREPANCY) {
                                        discrepancyList.put(DBConstants.DESCRANCY_WORK_EXPERIENCE, candidateScreening.getWorkCriteriaRemark());
                                    }
                                    if (candidateScreening.getIsAgeCriteriaMet() == CandidateScreeningCriteriaStatus.DISCREPANCY) {
                                        discrepancyList.put(AppConstants.AGE_DISCREPANCY, candidateScreening.getAgeCriteriaRemark());
                                    }
                                    if (candidateScreening.getIsEducationCriteriaMet() == CandidateScreeningCriteriaStatus.DISCREPANCY) {
                                        discrepancyList.put(AppConstants.EDUCATION_DISCREPANCY, candidateScreening.getEducationCriteriaRemark());
                                    }
                                    return discrepancyList;
                                }
                        )
                );

        // for sending mails
        //screening mails
        for (CandidateScreeningEntity candidateScreening : candidateScreeningEntitiesToRemind) {
            CandidateApplicationsEntity application = applicationsToRemindMap.get(candidateScreening.getApplicationId());
            CandidateProfileEntity profile = candidateProfileMap.get(candidateScreening.getCandidateId());
            JobPositionsEntity jobPosition = jobPositionsMap.get(application.getPositionId());
            CandidateAddressEntity address = candidateAddressMap.get(candidateScreening.getCandidateId());

            long duration = ChronoUnit.DAYS.between(today, candidateScreening.getSubmitBeforeDate());
            Context context = new Context();
            context.setVariable(AppConstants.SCREENING_DISCREPANCY_CRON_REMAINDER, true);
            context.setVariable(AppConstants.SCREENING_UPLOAD_REMAINING_DAYS, duration+1);
            context.setVariable(AppConstants.SCREENING_REMAINDER_COUNT,AppConstants.THREE-duration);
            context.setVariable(DBConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(profile));
            context.setVariable(DBConstants.APPLICATION_NO, application.getApplicationNo());
            context.setVariable(DBConstants.POSITION_NAME, masterPositionNameMap.get(jobPosition.getMasterPositionId()));
            context.setVariable(AppConstants.REJECTED_DOCUMENTS, screeningRejectedDocsMapWithNamesAndComments.get(application.getId()));
            context.setVariable(DBConstants.DESCRANCY_LIST, discrepancyListPerApplication.get(application.getId()));
            context.setVariable(DBConstants.DEADLINE_DATE, candidateScreening.getSubmitBeforeDate());
            context.setVariable(DBConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
            context.setVariable(AppConstants.APPLICATION_DATE, application.getApplicationDate());
            context.setVariable(AppConstants.CANDIDATE_LAST_NAME, profile.getLastName());
            context.setVariable(AppConstants.CANDIDATE_ADDRESS, address);
            context.setVariable(AppConstants.STATE_NAME, candidateStateMap.getOrDefault(address.getStateId(), null));
            context.setVariable(AppConstants.DISTRICT_NAME, candidateDistrictMap.getOrDefault(address.getDistrictId(), null));
            context.setVariable(AppConstants.ADVERTISEMENT_START_DATE, requisitionStartDateMap.get(jobPosition.getRequisitionId()));
            context.setVariable(AppConstants.DEPARTMENT, departmentNameMap.get(jobPosition.getDeptId()));
            String htmlContent = templateEngine.process(DBConstants.DESCRANCY_EMAIL_TEMPLATE, context);
            String pdfContent = templateEngine.process(AppConstants.DISCREPANCY_EMAIL_ATTACHMENT, context);
            byte[] pdfBytes = pdfConverterService.convertHtmlStringToPdf(pdfContent);
            MultipartFile pdfFile = new CustomMultipartFile(pdfBytes, "file", "discrepancy-attachment.pdf", "application/pdf");
            mailService.sendEmailTempleteFile(profile.getEmail(), DBConstants.DESCRANCY_MESSAGE_SUBJECT, htmlContent, pdfFile);
        }

        //zonal screening mails
        for(InterviewScheduleEntity interviewSchedule:provisionallyApprovedInterviewSchedulesToRemind){
            CandidateApplicationsEntity application = applicationsToRemindMap.get(interviewSchedule.getApplicationId());
            CandidateProfileEntity profile = candidateProfileMap.get(interviewSchedule.getCandidateId());
            JobPositionsEntity jobPosition = jobPositionsMap.get(application.getPositionId());
            CandidateAddressEntity address = candidateAddressMap.get(interviewSchedule.getCandidateId());

            long duration = ChronoUnit.DAYS.between(today, interviewSchedule.getZonalSubmitBeforeDate());
            Context context = new Context();
            context.setVariable(AppConstants.SCREENING_DISCREPANCY_CRON_REMAINDER, true);
            context.setVariable(AppConstants.SCREENING_UPLOAD_REMAINING_DAYS, duration + 1);
            context.setVariable(AppConstants.SCREENING_REMAINDER_COUNT, AppConstants.THREE - duration);
            context.setVariable(DBConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(profile));
            context.setVariable(DBConstants.APPLICATION_NO, application.getApplicationNo());
            context.setVariable(DBConstants.POSITION_NAME, masterPositionNameMap.get(jobPosition.getMasterPositionId()));
            context.setVariable(AppConstants.REJECTED_DOCUMENTS, rejectedDocsMapWithNamesAndComments.get(application.getId()));
            context.setVariable(DBConstants.DEADLINE_DATE, interviewSchedule.getZonalSubmitBeforeDate());
            context.setVariable(AppConstants.OVERALL_COMMENTS,interviewSchedule.getZonalHrComments());
            context.setVariable(DBConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
            context.setVariable(AppConstants.APPLICATION_DATE, application.getApplicationDate());
            context.setVariable(AppConstants.CANDIDATE_LAST_NAME, profile.getLastName());
            context.setVariable(AppConstants.CANDIDATE_ADDRESS, address);
            context.setVariable(AppConstants.STATE_NAME, candidateStateMap.getOrDefault(address.getStateId(), null));
            context.setVariable(AppConstants.DISTRICT_NAME, candidateDistrictMap.getOrDefault(address.getDistrictId(), null));
            context.setVariable(AppConstants.ADVERTISEMENT_START_DATE, requisitionStartDateMap.get(jobPosition.getRequisitionId()));
            context.setVariable(AppConstants.DEPARTMENT, departmentNameMap.get(jobPosition.getDeptId()));

            String htmlContent = templateEngine.process(AppConstants.ZONAL_HR_EMAIL_TEMPLATE, context);
            String pdfContent = templateEngine.process(AppConstants.ZONAL_HR_DISCREPANCY_EMAIL_ATTACHMENT,context);

            byte[] pdfBytes = pdfConverterService.convertHtmlStringToPdf(pdfContent);
            MultipartFile pdfFile = new CustomMultipartFile(pdfBytes, "file", "zonal-hr-discrepancy-attachment.pdf", "application/pdf");
            mailService.sendEmailTempleteFile(profile.getEmail(), AppConstants.ZONAL_HR_MESSAGE_SUBJECT, htmlContent, pdfFile);
        }
        //mail sending end



        //for rejecting candidates when not submitted after the submit date(screening)
        Set<UUID> rejectingApplicationIds = candidateScreeningEntities.stream()
                .filter(
                        (currScreeningEntity)-> today.isAfter(currScreeningEntity.getSubmitBeforeDate())
                ).map(
                        CandidateScreeningEntity::getApplicationId
                ).collect(Collectors.toSet());

        if(!rejectingApplicationIds.isEmpty()) {
            List<CandidateApplicationsEntity> rejectingApplicationEntities = applications.stream()
                    .filter((currCandidateApplication) -> rejectingApplicationIds.contains(currCandidateApplication.getId()))
                    .map(
                            (currCandidateApplication) -> {
                                currCandidateApplication.setApplicationStatus(CandidateApplicationStatus.REJECTED);
                                return currCandidateApplication;
                            }
                    ).toList();
            candidateApplicationsRepository.saveAllWithWorkflow(rejectingApplicationEntities);
            log.info("Application status changed from discrepancy to rejected for applications for which discrepancy documents are not submitted after submit date");

        }





        //for zonal rejecting
        List<InterviewScheduleEntity> zonalRejectingInterviewSchedules = provisionallyApprovedInterviewSchedules.stream()
                .filter((currInterviewSchedule)->today.isAfter(currInterviewSchedule.getZonalSubmitBeforeDate()))
                .toList();

        if(!zonalRejectingInterviewSchedules.isEmpty()){
            List<InterviewScheduleEntity> updatingZonalStatusInterviewSchedules = zonalRejectingInterviewSchedules
                    .stream()
                    .map((currSchedule)->{
                        currSchedule.setInterviewStatus(InterviewSchedulingStatus.ZONAL_REJECTED);
                        currSchedule.setZonalVerificationStatus(ZonalVerificationStatus.ZONAL_REJECTED);
                        return currSchedule;
                    }).toList();

            Set<UUID> zonalRejectingApplicationIds = zonalRejectingInterviewSchedules.stream()
                    .map(InterviewScheduleEntity::getApplicationId)
                    .collect(Collectors.toSet());

            List<CandidateApplicationsEntity> zonalRejectingApplications = applications.stream()
                    .filter((currApplication)->zonalRejectingApplicationIds.contains(currApplication.getId()))
                    .map((currApplication)-> {
                        currApplication.setApplicationStatus(CandidateApplicationStatus.ZONAL_REJECTED);
                        return currApplication;
                    })
                    .toList();

            interviewScheduleRepository.saveAllWithAudit(zonalRejectingInterviewSchedules,updatingZonalStatusInterviewSchedules);
            candidateApplicationsRepository.saveAllWithWorkflow(zonalRejectingApplications);
            log.info("Application status changed PROVISIONALLY_APPROVED to ZONAL_REJECTED for applications for which discrepancy documents are not submitted after zonal submit date");
        }



    }
}
