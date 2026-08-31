package com.bob.commonutil.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.CustomMultipartFile;
import com.bob.db.dto.*;
import com.bob.db.entity.*;
import com.bob.db.enums.MailSendStatus;
import com.bob.db.repository.*;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MailSenderHelper {

    @Autowired
    private CommonMailService commonMailService;

    @Autowired
    private CandidatesRepository candidatesRepository;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private InterviewPanelsRepository interviewPanelsRepository;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private InterviewPanelMembersRepository interviewPanelMembersRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private PositionsRepository jobPositionsRepository;

    @Autowired
    private JobRequisitionsRepository requisitionsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private DepartmentsRepository departmentsRepository;

    @Autowired
    private EmployementTypesRepository employementTypesRepository;

    @Autowired
    private ZonalStatesRepository zonalStatesRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private RequisitionApproversRepository requisitionApproversRepository;

    @Autowired
    private PdfConverterService pdfConverterService;

    @Autowired
    private CandidateCompensationRepository candidateCompensationRepository;

    @Autowired
    private RequestTypesRepository requestTypesRepository;

    @Autowired
    private CandidateWrittenExamMarksRepository candidateWrittenExamMarksRepository;

    @Async // Runs the whole loop in the background
    public void sendMailToCandidateAndRecruiters(List<InterviewScheduleEntity> savedEntities, List<CandidateApplicationsEntity> candidateApplicationsEntityList) throws MessagingException, IOException {
        sendEmails(savedEntities,candidateApplicationsEntityList);
    }

    public void sendEmails(List<InterviewScheduleEntity> savedEntities, List<CandidateApplicationsEntity> candidateApplicationsEntityList) throws MessagingException, IOException {
        log.info("Starting background batch mail process for {} records", savedEntities.size());

        try {
            // 1. Bulk Load Metadata
//            Map<UUID, CandidatesEntity> candidatesEntityMap = candidatesRepository.findAllById(
//                    savedEntities.stream().map(InterviewScheduleEntity::getCandidateId).toList()
//            ).stream().collect(Collectors.toMap(CandidatesEntity::getId, c -> c));

            Map<UUID, CandidateProfileEntity> candidateProfileEntityMap = candidateProfileRepository.findAllByCandidateIdIn(
                    savedEntities.stream().map(InterviewScheduleEntity::getCandidateId).toList()
            ).stream().collect(Collectors.toMap(CandidateProfileEntity::getCandidateId, Function.identity()));

            Map<UUID, InterviewCentresEntity> interviewCentresEntityMap = interviewCentresRepository.findAllById(
                    savedEntities.stream().map(InterviewScheduleEntity::getZonalOfficeId).toList()
            ).stream().collect(Collectors.toMap(InterviewCentresEntity::getId, i -> i));

            Map<UUID,String> zonalStateNameMap = zonalStatesRepository.findAllById(
                    interviewCentresEntityMap.values().stream().map(InterviewCentresEntity::getZonalStateId).collect(Collectors.toSet())
            ).stream().collect(Collectors.toMap(
                    ZonalStatesEntity::getId,
                    ZonalStatesEntity::getStateName
            ));


            Map<UUID, InterviewPanelsEntity> interviewPanelsEntityMap = interviewPanelsRepository.findAllById(
                    savedEntities.stream().map(InterviewScheduleEntity::getPanelId).toList()
            ).stream().collect(Collectors.toMap(InterviewPanelsEntity::getId, c -> c));

            Map<UUID, CandidateApplicationsEntity> applicationsMap = candidateApplicationsEntityList.stream()
                    .collect(Collectors.toMap(CandidateApplicationsEntity::getId, a -> a));

            //every application will have the same position
            UUID jobPositionId = candidateApplicationsEntityList.get(0).getPositionId();
            JobPositionsEntity jobPosition = jobPositionsRepository.findById(jobPositionId).orElseThrow(()->new ResourceNotFoundException("Job Position not Found"));
            MasterPositionsEntity masterPosition = masterPositionsRepository.findById(jobPosition.getMasterPositionId()).orElseThrow(()->new ResourceNotFoundException("Master Position not Found"));
            JobRequisitionsEntity jobRequisition = requisitionsRepository.findById(jobPosition.getRequisitionId()).orElseThrow(()->new ResourceNotFoundException("Requisition not Found"));
            DepartmentsEntity department = departmentsRepository.findById(jobPosition.getDeptId()).orElseThrow(()->new ResourceNotFoundException("Department not found"));
            EmployementTypesEntity employmentType = employementTypesRepository.findById(jobPosition.getEmploymentType()).orElseThrow(()->new ResourceNotFoundException("Employment Type not found"));


            // 2. Loop through each assignment
            for (InterviewScheduleEntity schedule : savedEntities) {
                StringBuilder errorSummary = new StringBuilder();
                boolean overallSuccess = true;

                CandidateApplicationsEntity application = applicationsMap.get(schedule.getApplicationId());
                try {
//                    CandidatesEntity candidate = candidatesEntityMap.get(schedule.getCandidateId());
                    CandidateProfileEntity candidateProfile = candidateProfileEntityMap.get(schedule.getCandidateId());
                    InterviewCentresEntity centre = interviewCentresEntityMap.get(schedule.getZonalOfficeId());
                    String zonalStateName = zonalStateNameMap.get(centre.getZonalStateId());
                    InterviewPanelsEntity panel = interviewPanelsEntityMap.get(schedule.getPanelId());

                    // Send to Candidate
                    try {
                        sendMailToCandidates(schedule, candidateProfile, centre, panel,
                                jobPosition,application.getApplicationNo(),
                                zonalStateName,masterPosition.getPositionName(),
                                department.getDepartmentName(), employmentType.getTypeName(),
                                jobRequisition.getStartDate(),jobRequisition.getEndDate());
                    } catch (Exception e) {
                        overallSuccess = false;
                        errorSummary.append("Candidate Mail Error: ").append(e.getMessage()).append("; ");
                        log.error("Candidate mail failed to schedule {}: {}", schedule.getId(), e.getMessage());
                        throw e;
                    }

                    // Send to Interviewers
//                    try {
//                        sendMailToInterviewers(schedule, candidateProfile, application, centre, panel,zonalStateName);
//                    } catch (Exception e) {
//                        overallSuccess = false;
//                        errorSummary.append("Interviewer Mail Error: ").append(e.getMessage()).append("; ");
//                        log.error("Interviewer mail failed to schedule {}: {}", schedule.getId(), e.getMessage());
//                    }
//
//                    try {
//                        sendMailToZonalHr(schedule,candidateProfile, application, centre, panel,zonalStateName);
//                    } catch (Exception e) {
//                        overallSuccess = false;
//                        errorSummary.append("ZonalHR Mail Error: ").append(e.getMessage()).append("; ");
//                        log.error("Couldn't send mail to ZonalHr {}: {}", schedule.getId(), e.getMessage());
//                    }
//                    if (overallSuccess) {
//                        updateMailStatus(schedule, MailSendStatus.SENT, "Notification sent successfully.");
//                    } else {
//                        updateMailStatus(schedule, MailSendStatus.FAILED, errorSummary.toString());
//                    }

                } catch (Exception e) {
                    log.error("General error processing schedule {}: {}", schedule.getId(), e.getMessage());
                    updateMailStatus(schedule, MailSendStatus.FAILED, "System error: " + e.getMessage());
                    throw e;
                }

            }
            sendMailToRecruitersNew(savedEntities,candidateApplicationsEntityList);
            sendMailToZonalHrNew(savedEntities,candidateApplicationsEntityList);
            //interviewScheduleRepository.saveAll(savedEntities);
            log.info("Finished background batch mail process for {} records", savedEntities.size());
        } catch (Exception e) {

            log.error("Fatal error in batch mail process: {}", e.getMessage());
            throw e;
        }
    }

    private void sendMailToCandidates(InterviewScheduleEntity schedule, CandidateProfileEntity profile,
                                      InterviewCentresEntity centre, InterviewPanelsEntity panel,
                                      JobPositionsEntity jobPosition,String applicationNo,
                                      String zonalStateName,String positionName,
                                      String deptName, String empTypeName,
                                      LocalDate requisitionStartDate,LocalDate requisitionEndDate
                                      ) throws IOException, MessagingException {
        if (profile == null || profile.getEmail() == null) {
            throw new ResourceNotFoundException("Candidate details or email missing.");
        }

        LocalTime reportingTime = commonUtilityProvider.getReportingTime(schedule.getInterviewStartAt().toLocalTime());



        Context context = new Context();
        context.setVariable(AppConstants.CANDIDATE_NAME, commonUtilityProvider.buildFullName(profile));
        context.setVariable(AppConstants.INTERVIEW_CENTER_ADDRESS, buildInterviewCentreAddress(centre, zonalStateName));
        context.setVariable(AppConstants.MEETING_LINK, schedule.getMeetingLink());
        context.setVariable(AppConstants.INTERVIEW_PANEL_NAME, panel != null ? panel.getPanelName() : "N/A");
        context.setVariable(AppConstants.INTERVIEW_START_DATE, schedule.getInterviewStartAt());
        context.setVariable(AppConstants.INTERVIEW_END_DATE, schedule.getInterviewEndAt());
        context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);

        context.setVariable(AppConstants.INTERVIEW_REPORTING_TIME,reportingTime);
        context.setVariable(AppConstants.APPLICATION_NO,applicationNo);
        context.setVariable(AppConstants.CANDIDATE_CONTACT_NUMBER,profile.getContactNo());
        context.setVariable(AppConstants.ADVERTISEMENT_START_DATE,requisitionStartDate);
        context.setVariable(AppConstants.MONTH_START_DATE,LocalDate.of(requisitionStartDate.getYear(),requisitionStartDate.getMonth(),AppConstants.ONE));
        context.setVariable(AppConstants.CASTE_CERTIFICATE_VALIDITY_DATE,LocalDate.of(requisitionEndDate.getYear()-1,requisitionEndDate.getMonth(),requisitionEndDate.getDayOfMonth()));
        context.setVariable(AppConstants.POSITION_NAME,positionName);
        context.setVariable(AppConstants.DEPARTMENT,deptName);
        context.setVariable(AppConstants.EMPLOYMENT_TYPE,empTypeName);
        context.setVariable(AppConstants.EDUCATION_QUALIFICATIONS,jobPosition.getMandatoryEducation());
        context.setVariable(AppConstants.EXPERIENCE,jobPosition.getMandatoryExperience());
        context.setVariable(AppConstants.MIN_AGE,jobPosition.getEligibilityAgeMin());
        context.setVariable(AppConstants.MAX_AGE,jobPosition.getEligibilityAgeMax());


        String pdf = templateEngine.process("CandidateScheduledAttachment.html",context);
        String html = templateEngine.process(AppConstants.CANDIDATE_SCHEDULED_MAIL_TEMPLATE, context);
        byte pdfBytes[] = pdfConverterService.convertHtmlStringToPdf(pdf);
        MultipartFile pdfFile = new CustomMultipartFile(pdfBytes, "file", "interview-schedule-attachment.pdf", "application/pdf");
        // Calling it synchronously
        commonMailService.sendEmailSync(profile.getEmail(), AppConstants.INTERVIEW_SCHEDULED, html, pdfFile);
    }




    private void updateMailStatus(InterviewScheduleEntity schedule, MailSendStatus status, String comment) {
        try {
            schedule.setMailSendStatus(status);
            if(status==MailSendStatus.SENT) schedule.setMailSentOn(LocalDateTime.now());
            schedule.setMailComments(comment);
        } catch (Exception e) {
            log.error("Failed to save mail status to DB for schedule {}: {}", schedule.getId(), e.getMessage());
        }
    }

    private String buildInterviewCentreAddress(InterviewCentresEntity centre, String zonalStateName) {
        if (centre == null) return "N/A";
        StringBuilder address = new StringBuilder(centre.getDisplayName());
        if (zonalStateName != null) {
            address.append(", ").append(zonalStateName);
        }
        return address.toString();
    }

    public void sendMailToZonalHrNew (List<InterviewScheduleEntity> interviewSchedules,List<CandidateApplicationsEntity> applications) {
        //get zonal hr based on id and grouping them
        List<UUID> zonalCentreIds = interviewSchedules.stream()
                .map(InterviewScheduleEntity::getZonalOfficeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<UserEntity> zonalHrList = userRepository.findByRoleAndInterviewCenterIdIn(AppConstants.ZONAL_HR_ROLE, zonalCentreIds);
        Map<UUID, InterviewCentresEntity> interviewCentresMap = interviewCentresRepository.findAllById(zonalCentreIds).stream()
                .collect(Collectors.toMap(InterviewCentresEntity::getId, Function.identity()));

        //getting job postion,requistion and master position d

        List<UUID> jobPositionIds = applications.stream()
                .map(CandidateApplicationsEntity::getPositionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        Map<UUID, JobPositionsEntity> jobPositionsMap = jobPositionsRepository.findAllById(jobPositionIds).stream()
                .collect(Collectors.toMap(JobPositionsEntity::getId, Function.identity()));

        Set<UUID> masterPositionIds = jobPositionsMap.values().stream()
                .map(JobPositionsEntity::getMasterPositionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<UUID> requisitionIds = jobPositionsMap.values().stream()
                .map(JobPositionsEntity::getRequisitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> requisitionNameMap = requisitionsRepository.findAllById(requisitionIds).stream()
                .collect(Collectors.toMap(
                        JobRequisitionsEntity::getId,
                        currentRequisition ->
                                currentRequisition.getRequisitionCode() + "-" + currentRequisition.getRequisitionTitle()
                ));
        Map<UUID, String> positionNameMap = masterPositionsRepository.findAllById(masterPositionIds).stream()
                .collect(Collectors.toMap(
                        MasterPositionsEntity::getId,
                        MasterPositionsEntity::getPositionName));


        Map<UUID, List<InterviewScheduleEntity>> schedulesByCentre = interviewSchedules.stream()
                .filter(s -> s.getZonalOfficeId() != null)
                .collect(Collectors.groupingBy(InterviewScheduleEntity::getZonalOfficeId));

        Map<UUID, CandidateApplicationsEntity> candidateApplicationsMap = applications.stream()
                .collect(Collectors.toMap(CandidateApplicationsEntity::getId, Function.identity()));

        for (UserEntity zonalHr : zonalHrList) {
            List<InterviewScheduleEntity> schedulesForCentre = schedulesByCentre.get(zonalHr.getInterviewCenterId());
            if (schedulesForCentre == null || schedulesForCentre.isEmpty()) {
                continue;
            }

            Map<String, List<InterviewScheduleEntity>> groupedData =
                    schedulesForCentre.stream()
                            .map(schedule -> {

                                CandidateApplicationsEntity application =
                                        candidateApplicationsMap.get(
                                                schedule.getApplicationId()
                                        );



                                JobPositionsEntity jobPosition =
                                        jobPositionsMap.get(
                                                application.getPositionId()
                                        );


                                String key =
                                        jobPosition.getRequisitionId()+"_"+jobPosition.getMasterPositionId();

                                return Map.entry(key, schedule);

                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(
                                    Map.Entry::getKey,
                                    Collectors.mapping(
                                            Map.Entry::getValue,
                                            Collectors.toList()
                                    )
                            ));

            List<Map<String, String>> emailData = groupedData.entrySet()
                    .stream()
                    .map(entry -> {

                        String[] values = entry.getKey().split("_");
                        UUID requisitionId = UUID.fromString(values[0]);
                        UUID masterPositionId = UUID.fromString(values[1]);

                        List<InterviewScheduleEntity> schedules =
                                entry.getValue();

                        LocalDateTime slotStartTime = schedules.stream()
                                .map(InterviewScheduleEntity::getInterviewStartAt)
                                .filter(Objects::nonNull)
                                .min(LocalDateTime::compareTo)
                                .orElse(null);

                        LocalDateTime slotEndTime = schedules.stream()
                                .map(InterviewScheduleEntity::getInterviewEndAt)
                                .filter(Objects::nonNull)
                                .max(LocalDateTime::compareTo)
                                .orElse(null);

                        Map<String, String> row = new HashMap<>();

                        row.put("requisitionName", requisitionNameMap.get(requisitionId));
                        row.put("positionName", positionNameMap.get(masterPositionId));
                        row.put("candidateCount", Integer.toString(schedules.size()));
                        row.put("startDate", slotStartTime != null ? slotStartTime.toLocalDate().format(AppConstants.DD_MM_YYYY) : null);
                        row.put("slotStartTime", slotStartTime != null ? slotStartTime.toLocalTime().format(AppConstants.HH_mm) : null);
                        row.put("slotEndDate", slotEndTime != null ? slotEndTime.toLocalDate().format(AppConstants.DD_MM_YYYY) : null);
                        row.put("slotEndTime", slotEndTime != null ? slotEndTime.toLocalTime().format(AppConstants.HH_mm) : null);


                        return row;

                    })
                    .toList();

            Context context = new Context();
            context.setVariable("zonalHrName", zonalHr.getName());
            context.setVariable("interviewCentre", interviewCentresMap.get(zonalHr.getInterviewCenterId()).getDisplayName());
            context.setVariable("zonalHrData", emailData);
            context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
            String html = templateEngine.process("ZonalHrInterviewSchduledMail.html", context);
            commonMailService.sendEmailTempleteFile(zonalHr.getEmail(), "Interview Scheduling Details", html, null);
        }




    }

    public void sendMailToRecruitersNew(List<InterviewScheduleEntity> interviewSchedules,List<CandidateApplicationsEntity> applications){
        List<UUID> panelIds = interviewSchedules.stream()
                .map(InterviewScheduleEntity::getPanelId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        List<InterviewPanelsEntity> panels = interviewPanelsRepository.findAllWithMembers(panelIds).stream()
                .toList();



        Map<UUID,List<InterviewScheduleEntity>> interviewSchedulesByPanel = interviewSchedules.stream()
                .collect(Collectors.groupingBy(InterviewScheduleEntity::getPanelId));

        //to get job position related details
        List<UUID> jobPositionIds = applications.stream()
                .map(CandidateApplicationsEntity::getPositionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, JobPositionsEntity> jobPositionsMap = jobPositionsRepository.findAllById(jobPositionIds).stream()
                .collect(Collectors.toMap(JobPositionsEntity::getId, Function.identity()));

        Set<UUID> masterPositionIds = jobPositionsMap.values().stream()
                .map(JobPositionsEntity::getMasterPositionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> requisitionIds = jobPositionsMap.values().stream()
                .map(JobPositionsEntity::getRequisitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> requisitionNameMap = requisitionsRepository.findAllById(requisitionIds).stream()
                .collect(Collectors.toMap(
                        JobRequisitionsEntity::getId,
                        currentRequisition ->
                                currentRequisition.getRequisitionCode() + "-" + currentRequisition.getRequisitionTitle()
                ));
        Map<UUID, String> positionNameMap = masterPositionsRepository.findAllById(masterPositionIds).stream()
                .collect(Collectors.toMap(
                        MasterPositionsEntity::getId,
                        MasterPositionsEntity::getPositionName));

        //Lis
        List<UUID> zonalCentreIds = interviewSchedules.stream()
                .map(InterviewScheduleEntity::getZonalOfficeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> interviewCentresMap = interviewCentresRepository.findAllById(zonalCentreIds).stream()
                .collect(Collectors.toMap(InterviewCentresEntity::getId, InterviewCentresEntity::getDisplayName));

        Map<UUID, CandidateApplicationsEntity> candidateApplicationsMap = applications.stream()
                .collect(Collectors.toMap(CandidateApplicationsEntity::getId, Function.identity()));

        for(InterviewPanelsEntity panel : panels){
            List<InterviewScheduleEntity> schedulesForPanel = interviewSchedulesByPanel.get(panel.getId());
            if(schedulesForPanel==null || schedulesForPanel.isEmpty()) continue;
            Map<String, List<InterviewScheduleEntity>> groupedData =
                    schedulesForPanel.stream()
                            .map(schedule -> {

                                CandidateApplicationsEntity application =
                                        candidateApplicationsMap.get(
                                                schedule.getApplicationId()
                                        );



                                JobPositionsEntity jobPosition =
                                        jobPositionsMap.get(
                                                application.getPositionId()
                                        );


                                String key =
                                        jobPosition.getRequisitionId() + "_" +
                                                jobPosition.getMasterPositionId() + "_" +
                                                schedule.getZonalOfficeId();

                                return Map.entry(key, schedule);

                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(
                                    Map.Entry::getKey,
                                    Collectors.mapping(
                                            Map.Entry::getValue,
                                            Collectors.toList()
                                    )
                            ));

            List<Map<String, String>> emailData = groupedData.entrySet()
                    .stream()
                    .map(entry -> {

                        String[] values = entry.getKey().split("_");
                        UUID requisitionId = UUID.fromString(values[0]);
                        UUID masterPositionId = UUID.fromString(values[1]);
                        UUID zonalOfficeId = UUID.fromString(values[2]);

                        List<InterviewScheduleEntity> schedules =
                                entry.getValue();

                        LocalDateTime slotStartTime = schedules.stream()
                                .map(InterviewScheduleEntity::getInterviewStartAt)
                                .filter(Objects::nonNull)
                                .min(LocalDateTime::compareTo)
                                .orElse(null);

                        LocalDateTime slotEndTime = schedules.stream()
                                .map(InterviewScheduleEntity::getInterviewEndAt)
                                .filter(Objects::nonNull)
                                .max(LocalDateTime::compareTo)
                                .orElse(null);

                        Map<String, String> row = new HashMap<>();

                        row.put("requisitionName", requisitionNameMap.get(requisitionId));
                        row.put("positionName", positionNameMap.get(masterPositionId));
                        row.put("interviewCentre", interviewCentresMap.get(zonalOfficeId));
                        row.put("candidateCount", Integer.toString(schedules.size()));
                        row.put("startDate", slotStartTime != null ? slotStartTime.toLocalDate().format(AppConstants.DD_MM_YYYY) : null);
                        row.put("slotStartTime", slotStartTime != null ? slotStartTime.toLocalTime().format(AppConstants.HH_mm) : null);
                        row.put("slotEndDate", slotEndTime != null ? slotEndTime.toLocalDate().format(AppConstants.DD_MM_YYYY) : null);
                        row.put("slotEndTime", slotEndTime != null ? slotEndTime.toLocalTime().format(AppConstants.HH_mm) : null);


                        return row;

                    })
                    .toList();
            String panelMemberNames = panel.getPanelMembers().stream()
                    .map((currPanelMember) -> (currPanelMember.getPanelMember().getName()))
                    .collect(Collectors.joining(", "));

            for(InterviewPanelMembersEntity panelMember : panel.getPanelMembers()){
                UserEntity member = panelMember.getPanelMember();


                Context context = new Context();
                context.setVariable("recruiterName", member.getName());
                context.setVariable("panelName", panel.getPanelName());
                context.setVariable("interviewData",emailData);
                context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
                context.setVariable("panelMembers", panelMemberNames);
                String html = templateEngine.process("RecruiterInterviewScheduledMail.html", context);
                commonMailService.sendEmailTempleteFile(member.getEmail(), "Interview Scheduling Details", html, null);
            }

        }




    }

    @Async
    public void sendPositionPanelAssignmentEmail(List<PositionPanelDTO> positionPanels,JobPositionsEntity jobPosition) throws MessagingException, IOException {

        JobRequisitionsEntity jobRequisition = requisitionsRepository.findById(jobPosition.getRequisitionId())
                .orElseThrow(()->new ResourceNotFoundException("Job Requisition not found"));
        MasterPositionsEntity masterPosition = masterPositionsRepository.findById(jobPosition.getMasterPositionId())
                .orElseThrow(()->new ResourceNotFoundException("Master Position not"));
        String positionName = masterPosition.getPositionName();
        String requisitionName = jobRequisition.getRequisitionCode()+"-"+jobRequisition.getRequisitionTitle();

       for(PositionPanelDTO positionPanel : positionPanels){

           InterviewPanelsDTO interviewPanel = positionPanel.getInterviewPanel();
           InterviewCommitteeDTO interviewCommittee = interviewPanel.getCommittee();
           List<InterviewPanelMembersDTO> panelMembers = interviewPanel.getPanelMembers();
           String panelMemberNames = panelMembers.stream()
                   .map((currPanelMember)->(currPanelMember.getPanelMember().getName()))
                   .collect(Collectors.joining(", "));

           for(InterviewPanelMembersDTO panelMember : panelMembers){
               Context context = new Context();
               String email = panelMember.getPanelMember().getEmail();
               context.setVariable(AppConstants.RECRUITER_NAME,panelMember.getPanelMember().getName());
               context.setVariable(AppConstants.REQUISITION_TITLE,requisitionName);
               context.setVariable(AppConstants.POSITION_NAME,positionName);
               context.setVariable(AppConstants.COMMITTEE_NAME,interviewCommittee.getCommitteeName());
               context.setVariable(AppConstants.CONTEXT_PANEL_NAME,interviewPanel.getPanelName());
               context.setVariable(AppConstants.PANEL_START_DATE,positionPanel.getStartDate().format(AppConstants.DD_MM_YYYY));
               context.setVariable(AppConstants.PANEL_END_DATE,positionPanel.getEndDate().format(AppConstants.DD_MM_YYYY));
               context.setVariable(AppConstants.PANEL_MEMBERS,panelMemberNames);
               context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
               String html = templateEngine.process(AppConstants.PANEL_ASSIGNMENT_TEMPLATE,context);
               commonMailService.sendEmailSync(email,"Panel Assignment Details",html,null);
           }
       }


    }

    @Async
    public void sendPanelApprovalMail(List<PositionPanelEntity> pendingPositionPanels, RequisitionApproversEntity.ApproverRole role) throws MessagingException, IOException {

        List<JobPositionsEntity> jobPositions = pendingPositionPanels.stream().
                map(PositionPanelEntity::getJobPosition).collect(Collectors.toList());
        Set<UUID> requisitionIds = jobPositions.stream().map(JobPositionsEntity::getRequisitionId).collect(Collectors.toSet());
        Set<UUID> masterPositionIds = jobPositions.stream().map(JobPositionsEntity::getMasterPositionId).collect(Collectors.toSet());
        Map<UUID, String> requisitionNameMap = requisitionsRepository.findAllById(
                        requisitionIds
                ).stream()
                .collect(Collectors.toMap(
                        JobRequisitionsEntity::getId,
                        (currentReq) -> currentReq.getRequisitionCode() + "-" + currentReq.getRequisitionTitle()
                ));

        Map<UUID, String> masterPositionMap = masterPositionsRepository.findAllById(
                        masterPositionIds
                ).stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, MasterPositionsEntity::getPositionName));

        Map<String, List<String>> requisitionNameToPositionNamesMap = groupPositionsByRequisition(jobPositions, requisitionNameMap, masterPositionMap);

        UserEntity approver = getApprover(role, null);

        String html = getApprovalHtml(approver.getName(), requisitionNameToPositionNamesMap, "Panel");
        commonMailService.sendEmailSync(approver.getEmail(), "Panel Approval Request", html, null);
    }

    @Async
    public void sendRequisitionsApprovalMail(List<JobRequisitionsEntity> pendingRequisitions, RequisitionApproversEntity.ApproverRole role) throws MessagingException, IOException {

        Set<UUID> requisitionIds = pendingRequisitions.stream().map(JobRequisitionsEntity::getId).collect(Collectors.toSet());
        List<JobPositionsEntity> jobPositions = jobPositionsRepository.findAllByRequisitionIdIn(requisitionIds);

        Set<UUID> masterPositionIds = jobPositions.stream().map(JobPositionsEntity::getMasterPositionId).collect(Collectors.toSet());

        Map<UUID, String> requisitionNameMap = pendingRequisitions.stream()
                .collect(Collectors.toMap(
                        JobRequisitionsEntity::getId,
                        (currentReq)->currentReq.getRequisitionCode() + "-" + currentReq.getRequisitionTitle()
                ));

        Map<UUID,String> masterPositionMap = masterPositionsRepository.findAllById(masterPositionIds)
                .stream().collect(Collectors.toMap(MasterPositionsEntity::getId, MasterPositionsEntity::getPositionName));

        Map<String,List<String>> requisitionNameToPositionNamesMap =  groupPositionsByRequisition(jobPositions,requisitionNameMap,masterPositionMap);

        UserEntity approver = getApprover(role,null);
        String html = getApprovalHtml(approver.getName(),requisitionNameToPositionNamesMap,"Requisition");
        commonMailService.sendEmailSync(approver.getEmail(),"Requisition Approval Pending",html,null);
    }


    @Async
    public void sendExtensionApprovalMail(List<UUID> applicationIds, RequisitionApproversEntity.ApproverRole role) throws MessagingException, IOException {

        List<CandidateApplicationsEntity> applications = candidateApplicationsRepository.findAllById(applicationIds);

        Set<UUID> jobPositionIds = applications.stream().
                map(CandidateApplicationsEntity::getPositionId)
                .collect(Collectors.toSet());
        if(jobPositionIds.size()>1){
            throw new CommonException("Invalid request");
        }
        UUID jobPositionId = jobPositionIds.iterator().next();
        JobPositionsEntity jobPosition = jobPositionsRepository.findById(jobPositionId).orElseThrow(()->new ResourceNotFoundException("Job Position not found"));
        JobRequisitionsEntity jobRequisition = requisitionsRepository.findById(jobPosition.getRequisitionId())
                .orElseThrow(()->new ResourceNotFoundException("Job Requisition not found"));
        MasterPositionsEntity masterPosition = masterPositionsRepository.findById(jobPosition.getMasterPositionId())
                .orElseThrow(()->new ResourceNotFoundException("Master Position not"));
        String positionName = masterPosition.getPositionName();
        String requisitionName = jobRequisition.getRequisitionCode()+"-"+jobRequisition.getRequisitionTitle();


        Map<String, List<String>> requisitionNameToPositionNamesMap = Map.of(requisitionName, List.of(positionName));

        UserEntity approver = getApprover(role,jobPosition);

        String html = getApprovalHtml(approver.getName(),requisitionNameToPositionNamesMap,"Extension");
        commonMailService.sendEmailSync(approver.getEmail(),"Extension Approval Pending",html,null);
    }

    @Async
    public void sendApprovalMail(List<UUID> positionIds,RequisitionApproversEntity.ApproverRole role,String requestType,String subject){

        List<JobPositionsEntity> jobPositions = jobPositionsRepository.findAllById(positionIds);
        Set<UUID> requisitionIds = jobPositions.stream().map(JobPositionsEntity::getRequisitionId).collect(Collectors.toSet());
        Set<UUID> masterPositionIds = jobPositions.stream().map(JobPositionsEntity::getMasterPositionId).collect(Collectors.toSet());
        Map<UUID, String> requisitionNameMap = requisitionsRepository.findAllById(
                        requisitionIds
                ).stream()
                .collect(Collectors.toMap(
                        JobRequisitionsEntity::getId,
                        (currentReq) -> currentReq.getRequisitionCode() + "-" + currentReq.getRequisitionTitle()
                ));

        Map<UUID, String> masterPositionMap = masterPositionsRepository.findAllById(
                        masterPositionIds
                ).stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, MasterPositionsEntity::getPositionName));

        Map<String, List<String>> requisitionNameToPositionNamesMap = groupPositionsByRequisition(jobPositions, requisitionNameMap, masterPositionMap);

        UserEntity approver = getApprover(role, null);

        String html = getApprovalHtml(approver.getName(), requisitionNameToPositionNamesMap, requestType);

        try{
            commonMailService.sendEmailSync(approver.getEmail(),subject,html,null);
        }catch (MessagingException | IOException e){
            throw new RuntimeException(e);
        }
    }

    @Async
    public void sendMailToCompensationPoolCandidates(List<UUID> candidateCompensationIds){
        List<CandidateCompensationEntity> candidateCompensationEntities = candidateCompensationRepository.findAllWithCandidateProfileAndApplication(candidateCompensationIds);
        Set<UUID> positonIds = candidateCompensationEntities.stream().map((curr)->curr.getApplication().getPositionId()).collect(Collectors.toSet());
        Map<UUID,JobPositionsEntity> jobPositionsMap =jobPositionsRepository.findAllById(positonIds).stream()
                .collect(Collectors.toMap(
                        JobPositionsEntity::getId,
                        Function.identity()
                ));
        Set<UUID> requisitionIds = jobPositionsMap.values().stream().map(JobPositionsEntity::getRequisitionId).collect(Collectors.toSet());
        Set<UUID> masterPositionIds = jobPositionsMap.values().stream().map(JobPositionsEntity::getMasterPositionId).collect(Collectors.toSet());
        Map<UUID, String> requisitionNameMap = requisitionsRepository.findAllById(requisitionIds).stream()
                .collect(Collectors.toMap(
                        JobRequisitionsEntity::getId,
                        (currentReq)->currentReq.getRequisitionCode() + "-" + currentReq.getRequisitionTitle()
                ));

        Map<UUID,String> masterPositionMap = masterPositionsRepository.findAllById(masterPositionIds)
                .stream().collect(Collectors.toMap(MasterPositionsEntity::getId, MasterPositionsEntity::getPositionName));

        for(CandidateCompensationEntity compensationEntity : candidateCompensationEntities){
            CandidateProfileEntity profile = compensationEntity.getCandidateProfile();
            CandidateApplicationsEntity application = compensationEntity.getApplication();
            JobPositionsEntity jobPosition = jobPositionsMap.get(application.getPositionId());
            Context context = new Context();
            context.setVariable(AppConstants.CANDIDATE_NAME,commonUtilityProvider.buildFullName(profile));
            context.setVariable(AppConstants.APPLICATION_NO,application.getApplicationNo());
            context.setVariable(AppConstants.REQUISITION_TITLE,requisitionNameMap.get(jobPosition.getRequisitionId()));
            context.setVariable(AppConstants.POSITION_NAME,masterPositionMap.get(jobPosition.getMasterPositionId()));
            context.setVariable(AppConstants.DEADLINE_DATE,compensationEntity.getSubmitBeforeDate());
            context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
            String html = templateEngine.process("candidate-compensation-request.html",context);
            try{
                commonMailService.sendEmailSync(profile.getEmail(),"Request to fill Compensation details",html,null);
            }catch (MessagingException | IOException e){
                throw new RuntimeException(e);
            }

        }

    }



    @Async
    public void senMailToRecruiterAboutCandidateCompensationDetails(UUID compensationId){
        List<CandidateCompensationEntity> compensationEntityList = candidateCompensationRepository.findAllWithCandidateProfileAndApplication(List.of(compensationId));

        if(compensationEntityList.size() != 1) throw new CommonException("Invalid Request");
        CandidateCompensationEntity compensationEntity = compensationEntityList.get(0);
        UserEntity user = userRepository.findById(compensationEntity.getCreatedBy()).orElseThrow(()->new ResourceNotFoundException("User not found"));
        CandidateApplicationsEntity application = compensationEntity.getApplication();
        JobPositionsEntity jobPosition = jobPositionsRepository.findById(application.getPositionId()).orElseThrow(()->new ResourceNotFoundException("Job Position not found"));
        JobRequisitionsEntity jobRequisition = requisitionsRepository.findById(jobPosition.getRequisitionId()).orElseThrow(()->new ResourceNotFoundException("Job Requisition not found"));
        MasterPositionsEntity masterPosition = masterPositionsRepository.findById(jobPosition.getMasterPositionId()).orElseThrow(()->new ResourceNotFoundException("Job Position not found"));

        Context context = new Context();
        context.setVariable(AppConstants.RECRUITER_NAME,user.getName());
        context.setVariable(AppConstants.APPLICATION_NO,application.getApplicationNo());
        context.setVariable(AppConstants.REQUISITION_TITLE,jobRequisition.getRequisitionCode()+"_"+jobRequisition.getRequisitionTitle());
        context.setVariable(AppConstants.POSITION_NAME,masterPosition.getPositionName());
        context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
        String html = templateEngine.process("recruiter_compensation_verification_request.html",context);
        try{
            commonMailService.sendEmailSync(user.getEmail(),"Candidate Compensation Request",html,null);
        }catch (MessagingException | IOException e){
            throw new RuntimeException(e);
        }

    }

    @Async
    public void sendMailsToCandidateUponApprovalAndRejections(List<ConversationThreadsEntity> conversationThreadsEntities,boolean status){
        List<UUID> applicationIds = conversationThreadsEntities.stream().map(ConversationThreadsEntity::getApplicationId).toList();
        Set<UUID> requestTypeIds = conversationThreadsEntities.stream().map(ConversationThreadsEntity::getRequestTypeId).collect(Collectors.toSet());
        Map<UUID,String>  requestTypeNameMap = requestTypesRepository.findAllById(requestTypeIds).stream()
                .collect(Collectors.toMap(
                        RequestTypesEntity::getId,
                        RequestTypesEntity::getRequestName
                ));
        Map<UUID,CandidateApplicationsEntity> applicationMap= candidateApplicationsRepository.findAllById(applicationIds).stream()
                .collect(Collectors.toMap(
                        CandidateApplicationsEntity::getId,
                        Function.identity()
                ));
        List<UUID> candidateIds = applicationMap.values().stream().map(CandidateApplicationsEntity::getCandidateId).distinct().toList();
        Set<UUID> positonIds = applicationMap.values().stream().map(CandidateApplicationsEntity::getPositionId).collect(Collectors.toSet());
        Map<UUID,JobPositionsEntity> jobPositionsMap =jobPositionsRepository.findAllById(positonIds).stream()
                .collect(Collectors.toMap(
                        JobPositionsEntity::getId,
                        Function.identity()
                ));
        Set<UUID> requisitionIds = jobPositionsMap.values().stream().map(JobPositionsEntity::getRequisitionId).collect(Collectors.toSet());
        Set<UUID> masterPositionIds = jobPositionsMap.values().stream().map(JobPositionsEntity::getMasterPositionId).collect(Collectors.toSet());
        Map<UUID, String> requisitionNameMap = requisitionsRepository.findAllById(requisitionIds).stream()
                .collect(Collectors.toMap(
                        JobRequisitionsEntity::getId,
                        (currentReq)->currentReq.getRequisitionCode() + "-" + currentReq.getRequisitionTitle()
                ));

        Map<UUID,String> masterPositionMap = masterPositionsRepository.findAllById(masterPositionIds)
                .stream().collect(Collectors.toMap(MasterPositionsEntity::getId, MasterPositionsEntity::getPositionName));

        Map<UUID,CandidateProfileEntity> candidateProfileMap = candidateProfileRepository.findAllByCandidateIdIn(candidateIds).stream()
                .collect(Collectors.toMap(
                        CandidateProfileEntity::getCandidateId,
                        Function.identity()
                ));
        for(ConversationThreadsEntity conversationThreadsEntity:conversationThreadsEntities){
            CandidateApplicationsEntity application = applicationMap.get(conversationThreadsEntity.getApplicationId());
            CandidateProfileEntity profile = candidateProfileMap.get(application.getCandidateId());
            JobPositionsEntity jobPosition = jobPositionsMap.get(application.getPositionId());
            String requisitionTitle = requisitionNameMap.get(jobPosition.getRequisitionId());
            String masterPositonName = masterPositionMap.get(jobPosition.getMasterPositionId());
            String requestType = requestTypeNameMap.get(conversationThreadsEntity.getRequestTypeId());
            Context context = new Context();
            context.setVariable(AppConstants.CANDIDATE_NAME,commonUtilityProvider.buildFullName(profile));
            context.setVariable(AppConstants.APPLICATION_NO,application.getApplicationNo());
            context.setVariable(AppConstants.REQUISITION_TITLE,requisitionTitle);
            context.setVariable(AppConstants.POSITION_NAME,masterPositonName);
            context.setVariable(AppConstants.APPROVAL_REQUEST_TYPE,requestType);
            context.setVariable(AppConstants.REQUEST_RAISED_DATE,conversationThreadsEntity.getCreatedDate().toLocalDate().format(AppConstants.DD_MM_YYYY));
            context.setVariable(AppConstants.REQUEST_APPROVAL_STATUS,status);
            context.setVariable(AppConstants.COMPANY_NAME,AppConstants.BOB_RECRUITMENT);
            String html = templateEngine.process("candidate_request_approval_update.html",context);
            try{
                commonMailService.sendEmailSync(profile.getEmail(),requestType+" Update",html,null);
            }catch (MessagingException | IOException e){
                throw new RuntimeException(e);
            }

        }

    }

    @Async
    public void sendExamNotification(UUID positionId , List<CandidateApplicationsEntity> applicationsEntitiesList) {

        JobPositionsEntity jobPosition = jobPositionsRepository.findById(positionId).orElseThrow(()->new ResourceNotFoundException("Position not found"));
        JobRequisitionsEntity jobRequisition = requisitionsRepository.findById(jobPosition.getRequisitionId()).orElseThrow(()->new ResourceNotFoundException("Requisition not found"));
        MasterPositionsEntity masterPositionsEntity = masterPositionsRepository.findById(jobPosition.getMasterPositionId()).orElseThrow(()->new ResourceNotFoundException("Requisition not found"));
        String requisitionName = jobRequisition.getRequisitionCode()+"-"+jobRequisition.getRequisitionTitle();
        List<UUID> candidatesToNotifyIds = applicationsEntitiesList.stream().map(CandidateApplicationsEntity::getCandidateId).toList();
        List<CandidateProfileEntity> candidateProfiles = candidateProfileRepository.findAllByCandidateIdIn(candidatesToNotifyIds);
        candidateProfiles.forEach((currProfile)->{
            Context context = new Context();
            context.setVariable(AppConstants.CANDIDATE_NAME,commonUtilityProvider.buildFullName(currProfile));
            context.setVariable(AppConstants.REQUISITION_TITLE,requisitionName);
            context.setVariable(AppConstants.POSITION_NAME,masterPositionsEntity.getPositionName());
            context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
            String html = templateEngine.process("ExamNotification.html",context);
            try{
                commonMailService.sendEmailSync(currProfile.getEmail(),"Exam Notification",html,null);
            }catch (MessagingException | IOException e){
                throw new CommonException("Some error occurred while send exam notification");
            }
        });
    }




    //used in recruiter approval mails
    private Map<String,List<String>> groupPositionsByRequisition(List<JobPositionsEntity> jobPositions,Map<UUID,String> requisitionNameMap, Map<UUID,String> masterPositionMap){


        return jobPositions.stream()
                .collect(Collectors.groupingBy(
                        (jobPosition) -> requisitionNameMap.get(jobPosition.getRequisitionId()),
                        Collectors.mapping(
                                (jobPosition) -> masterPositionMap.get(jobPosition.getMasterPositionId()),
                                Collectors.toList()
                        )
                ));
    }

    public UserEntity  getApprover(RequisitionApproversEntity.ApproverRole role,JobPositionsEntity jobPosition){
        UUID userId = null;
        if(role!=null){
            RequisitionApproversEntity approver = requisitionApproversRepository.findByApproverRole(role)
               .orElseThrow(()->new ResourceNotFoundException("Approver not found"));

            userId = approver.getApproverId();
       }else{
            if (jobPosition==null) {
                throw new CommonException("Invalid request");
            }
            userId = jobPosition.getCreatedBy();
        }
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

    }

    private String getApprovalHtml(String approverName,Map<String,List<String>> requisitionNameToPositionNamesMap,String requestType){
        Context context = new Context();
        context.setVariable(AppConstants.RECRUITER_NAME,approverName);
        context.setVariable(AppConstants.REQUISITION_POSITION_MAP,requisitionNameToPositionNamesMap);
        context.setVariable(AppConstants.APPROVAL_REQUEST_TYPE,requestType);
        context.setVariable(AppConstants.COMPANY_NAME, AppConstants.BOB_RECRUITMENT);
        String html = templateEngine.process(AppConstants.APPROVAL_TEMPALTE,context);
        return html;
    }
}

