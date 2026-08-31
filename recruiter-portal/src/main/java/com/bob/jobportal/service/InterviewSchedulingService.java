package com.bob.jobportal.service;

import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.commonutil.exception.SchedulingConflictException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.service.MeetSchedulerService;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.db.dto.*;
import com.bob.db.entity.*;
import com.bob.db.mapper.*;
import com.bob.db.enums.*;
import com.bob.db.repository.*;
import com.bob.jobportal.model.*;
import com.bob.commonutil.util.AppConstants;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InterviewSchedulingService {

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @Autowired
    private InterviewCommitteeRepository interviewCommitteeRepository;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private MeetSchedulerService meetSchedulerService;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private MailSenderHelper mailSenderHelper;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;
    @Autowired
    private InterviewPanelsRepository interviewPanelsRepository;

    @Autowired
    private InterviewScheduleMapper interviewScheduleMapper;
    @Autowired
    private InterviewCentresMapper interviewCentresMapper;

    @Autowired
    private InterviewPanelsMapper interviewPanelsMapper;

    @Autowired
    private CandidateApplicationsMapper candidateApplicationsMapper;

    @Autowired
    private PositionPanelMapper positionPanelMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewScheduleStagingRepository interviewScheduleStagingRepository;

    @Autowired
    private InterviewScheduleStagingMapper interviewScheduleStagingMapper;

    @Autowired
    private InterviewPanelScheduleConfigurationRepository interviewPanelScheduleConfigurationRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private JobPositionsMapper jobPositionsMapper;

    @Autowired
    private MasterPositionsMapper masterPositionsMapper;


    private static final LocalTime MORNING_SLOT1 = LocalTime.of(10, 0);
    private static final LocalTime MORNING_SLOT2 = LocalTime.of(13, 0);
    private static final LocalTime EVENING_SLOT1 = LocalTime.of(14, 0);
    private static final LocalTime EVENING_SLOT2 = LocalTime.of(17, 0);

//    public ExcelTemplateFile generateExcelTemplate(UUID positionId){
//        try{
//            setExcelFilterContext(positionId);
//            ExcelTemplateFile excelTemplateFile = excelTemplateService.generateExcelTemplate(SchedulePanelExcelModel.class);
//            return excelTemplateFile;
//        }catch (Exception e){
//            log.error("Error generating excel template for positionId {}: {}", positionId, e.getMessage());
//            throw new CommonException("Error generating excel template");
//        }
//        finally{
//                ExcelFilterContext.clear();
//        }
//    }

//    private void setExcelFilterContext(UUID positionId){
//        UUID committeeId=interviewCommitteeRepository.findByCommitteeName(AppConstants.INTERVIEW_COMMITEE_NAME).getId();
////        List<PositionPanelEntity> positionPanelEntities=positionPanelRepository.findByJobPositionId(positionId);
////        TODO: once frontend finish integrating approval process uncomment below line
//        List<PositionPanelEntity> positionPanelEntities=positionPanelRepository.findByJobPositionIdAndPositionPanelStatus(positionId, PositionPanelStatus.APPROVED);
//        Set<UUID> panelIds=positionPanelEntities.stream().map(p->p.getInterviewPanel().getId()).collect(Collectors.toSet());
//        ExcelFilterContext.set("isActive",true);
//        ExcelFilterContext.set("committeeId", committeeId);
//        ExcelFilterContext.set("panelIds", panelIds);
//    }


//    @Transactional
//    public String resendFailureMails() {
//        List<InterviewScheduleEntity> interviewScheduleEntities=interviewScheduleRepository.findByInterviewStatusAndMailSendStatus(
//                InterviewSchedulingStatus.SCHEDULED,
//                MailSendStatus.FAILED
//        );
//        if(interviewScheduleEntities.isEmpty()){
//            log.info("No pending mails found");
//            throw new ResourceNotFoundException("No pending mails found");
//        }
//        List<UUID> applicationIds=interviewScheduleEntities.stream().map(InterviewScheduleEntity::getApplicationId).toList();
//
//        List<CandidateApplicationsEntity> candidateApplicationsEntityList=candidateApplicationsRepository.findAllById(applicationIds);
//
//        mailSenderHelper.sendMailToCandidateAndRecruiters(interviewScheduleEntities,candidateApplicationsEntityList);
//        return "Mails sent successfully for"+applicationIds.size()+" Applications";
//    }

    @Transactional
    public List<PositionPanelDTO> getInterviewPanels(List<UUID> positionIds) {
        LocalDate today=LocalDate.now();
        // TODO:Give approved panels when UI is completed
        List<PositionPanelEntity> positionPanels= positionPanelRepository.findByJobPosition_IdInAndEndDateGreaterThanEqualAndPositionPanelStatus(positionIds,today,PositionPanelStatus.APPROVED);
//        List<PositionPanelEntity> positionPanels= positionPanelRepository.findByJobPosition_IdInAndEndDateGreaterThanEqual(positionIds,today);
        InterviewCommitteeEntity interviewCommitteeEntity=interviewCommitteeRepository.findByCommitteeName(AppConstants.INTERVIEW_COMMITEE_NAME);
        Map<UUID,JobPositionsEntity> positionsEntityMap=positionsRepository.findAllByIdIn(positionIds)
                .stream().collect(Collectors.toMap(p->p.getId(),p->p));
        List<UUID> masterPositionIds=positionsEntityMap.values().stream().map(JobPositionsEntity::getMasterPositionId).toList();
        Map<UUID,MasterPositionsEntity> masterPositionsEntityMap=masterPositionsRepository.findAllById(masterPositionIds)
                .stream().collect(Collectors.toMap(mp->mp.getId(),mp->mp));
        List<PositionPanelEntity> positionPanelsForInterview=positionPanels.stream()
                .filter(positionPanelEntity -> positionPanelEntity.getInterviewPanel().getCommittee().getId().equals(interviewCommitteeEntity.getId()))
                .toList();
        Set<UUID> allUserIds = positionPanelsForInterview.stream()
                .map(PositionPanelEntity::getInterviewPanel)
                .filter(Objects::nonNull)
                .flatMap(panel -> panel.getPanelMembers().stream())
                .map(InterviewPanelMembersEntity::getPanelMember)
                .filter(Objects::nonNull)
                .map(UserEntity::getId)
                .collect(Collectors.toSet());

        Map<UUID, UserEntity> userMap = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));

        Set<UUID> allCommitteeIds = positionPanelsForInterview.stream()
                .map(PositionPanelEntity::getInterviewPanel)
                .filter(Objects::nonNull)
                .map(InterviewPanelsEntity::getCommittee)
                .filter(Objects::nonNull)
                .map(InterviewCommitteeEntity::getId)
                .collect(Collectors.toSet());

        Map<UUID, InterviewCommitteeEntity> committeeMap = interviewCommitteeRepository.findAllById(allCommitteeIds).stream()
                .collect(Collectors.toMap(InterviewCommitteeEntity::getId, committee -> committee));



//        return positionPanelsForInterview.stream().map(
//                positionPanelsInter->positionPanelMapper.toDto(positionPanelsInter,userMap,committeeMap)).toList();
        return positionPanelsForInterview.stream().map(positionPanelEntity -> {
            PositionPanelDTO dto = positionPanelMapper.toDto(positionPanelEntity, userMap, committeeMap);
            JobPositionsEntity jobPosition = positionsEntityMap.get(positionPanelEntity.getJobPosition().getId());
            MasterPositionsEntity masterPosition = masterPositionsEntityMap.get(jobPosition.getMasterPositionId());
            dto.setPositionName(masterPosition.getPositionName());
            return dto;
        }).toList();
    }

    public List<PanelAvailabilityModel> getScheduledSlots(PanelAvailabilityRequestModel model) {
        UUID panelId = model.getPanelId();
        LocalDate startDate = model.getPanelStartDate(), endDate = model.getPanelEndDate();

        Specification<InterviewScheduleEntity> mainspecification = buildSpecificationForPanelAvailability(panelId, startDate, endDate);
        Specification<InterviewScheduleStagingEntity> stagingSpecification = buildSpecificationForPanelAvailability(panelId, startDate, endDate);

        List<InterviewScheduleEntity> scheduleEntityList = interviewScheduleRepository.findAll(mainspecification);
        List<InterviewScheduleStagingEntity> scheduleStagingEntities = interviewScheduleStagingRepository.findAll(stagingSpecification)
                .stream()
                .filter(staging -> staging.getInterviewSchedulingApprovalStatus() == InterviewSchedulingApprovalStatus.L1_PENDING || staging.getInterviewSchedulingApprovalStatus() == InterviewSchedulingApprovalStatus.PENDING)
                .toList();

        // 1. Fetch all required metadata in bulk to avoid N+1 queries
        Set<UUID> appIds = scheduleEntityList.stream().map(InterviewScheduleEntity::getApplicationId).collect(Collectors.toSet());
        appIds.addAll(scheduleStagingEntities.stream().map(i -> i.getApplication().getId()).collect(Collectors.toSet()));

        Map<UUID, CandidateApplicationsEntity> applicationsEntityMap = candidateApplicationsRepository.findAllById(appIds)
                .stream().collect(Collectors.toMap(CandidateApplicationsEntity::getId, c -> c));

        List<UUID> positionIds = applicationsEntityMap.values().stream().map(CandidateApplicationsEntity::getPositionId).toList();
        Map<UUID, JobPositionsEntity> jobPositionsEntityMap = positionsRepository.findAllById(positionIds).stream()
                .collect(Collectors.toMap(JobPositionsEntity::getId, p -> p));

        List<UUID> masterPositionIds = jobPositionsEntityMap.values().stream().map(JobPositionsEntity::getMasterPositionId).toList();
        Map<UUID, MasterPositionsEntity> masterPositionsEntityMap = masterPositionsRepository.findAllById(masterPositionIds).stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, mp -> mp));

        // 2. Map all entities to their PositionPanelAvailableModel representations grouped by Date
        Map<LocalDate, List<PanelAvailabilityModel.PositionPanelAvailableModel>> combinedSchedulesPerDate = new LinkedHashMap<>();

        // Process Main Schedules
        for (InterviewScheduleEntity schedule : scheduleEntityList) {
            LocalDate date = schedule.getInterviewStartAt().toLocalDate();
            CandidateApplicationsEntity application = applicationsEntityMap.get(schedule.getApplicationId());
            String positionName = getPositionName(application, jobPositionsEntityMap, masterPositionsEntityMap);

            PanelAvailabilityModel.PositionPanelAvailableModel availableModel = PanelAvailabilityModel.PositionPanelAvailableModel.builder()
                    .positionName(positionName)
                    .startTime(schedule.getInterviewStartAt().toLocalTime())
                    .endTime(schedule.getInterviewEndAt().toLocalTime())
                    .build();

            combinedSchedulesPerDate.computeIfAbsent(date, k -> new ArrayList<>()).add(availableModel);
        }

        // Process Staging Schedules
        for (InterviewScheduleStagingEntity schedule : scheduleStagingEntities) {
            LocalDate date = schedule.getInterviewStartAt().toLocalDate();
            CandidateApplicationsEntity application = applicationsEntityMap.get(schedule.getApplication().getId());
            String positionName = getPositionName(application, jobPositionsEntityMap, masterPositionsEntityMap);

            PanelAvailabilityModel.PositionPanelAvailableModel availableModel = PanelAvailabilityModel.PositionPanelAvailableModel.builder()
                    .positionName(positionName)
                    .startTime(schedule.getInterviewStartAt().toLocalTime())
                    .endTime(schedule.getInterviewEndAt().toLocalTime())
                    .build();

            combinedSchedulesPerDate.computeIfAbsent(date, k -> new ArrayList<>()).add(availableModel);
        }

        // 3. Build the final unified response list
        return combinedSchedulesPerDate.entrySet().stream()
                .map(entry -> PanelAvailabilityModel.builder()
                        .panelDate(entry.getKey())
                        .panelAvailableModels(entry.getValue())
                        .build())
                .toList();
    }

    // Helper method to keep code DRY and clean up nested lookups
    private String getPositionName(CandidateApplicationsEntity application,
                                   Map<UUID, JobPositionsEntity> jobPositionsEntityMap,
                                   Map<UUID, MasterPositionsEntity> masterPositionsEntityMap) {
        if (application == null) return "Unknown Position";
        JobPositionsEntity jobPosition = jobPositionsEntityMap.get(application.getPositionId());
        if (jobPosition == null) return "Unknown Position";
        MasterPositionsEntity masterPosition = masterPositionsEntityMap.get(jobPosition.getMasterPositionId());
        return masterPosition != null ? masterPosition.getPositionName() : "Unknown Position";
    }

    private Specification buildSpecificationForPanelAvailability(UUID panelId,LocalDate start,LocalDate end){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDateTime startDateTime=start.atStartOfDay();
            LocalDateTime endDateTime=end.atTime(LocalTime.MAX);
            // Check for panelid
            predicates.add(criteriaBuilder.equal(root.get("panelId"),panelId));
            //Check overlap schedules
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("interviewStartAt"), endDateTime));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("interviewEndAt"), startDateTime));
            query.orderBy(
                    criteriaBuilder.asc(
                            root.get("interviewStartAt")
                    )
            );
            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0]));
        };
    }


//
//    @Builder
//    public record InterviewCandidateAllocator(UUID applicationId, UUID candidateId
//            , UUID panelId, LocalDateTime startTime,LocalDateTime endTime,String meetingLink,UUID zonalOfficeId){}
//
//
//    @Data
//    class PanelState{
//        final UUID id;
//        final String name;
//        LocalDateTime nextAvailableTime;
//        int remainingSlots;
//        PanelState(SchedulePanelExcelModel m) {
//            this.id = m.getPanelId();
//            this.name = "Panel-" + m.getPanelId(); // For logging
//            this.nextAvailableTime = LocalDateTime.of(m.getPanelDate(), m.getStartTime());
//            this.remainingSlots = m.getInterviewPerDay();
//        }
//
//    }
//
//    private LocalDateTime getValidTimeSlot(LocalDateTime current) {
//        LocalTime time = current.toLocalTime();
//        LocalDate date = current.toLocalDate();
//        int duration = AppConstants.INTERVIEW_SCHEDULING_TIMINGS;
//        //1st Case:Before 10 then move to
//        // 10(Don't happen in our case as we are taking the start time from excel only, but just for safety)
////        if (time.isBefore(MORNING_SLOT1)) {
////            return LocalDateTime.of(date, MORNING_SLOT1);
////        }
//
//        // 2nd case:If in lunch (13:00–14:00) → move to 14:00
//        if (!time.isBefore(MORNING_SLOT2) && time.isBefore(EVENING_SLOT1)) {
//            return LocalDateTime.of(date, EVENING_SLOT1);
//        }
//
//        //3rd Case: If interview crosses from range 1-2PM
//        // Example: start at 12:50 PM with 15-minute duration → ends at 1:05 PM,
//        // then shift the slot to 2:00 PM (start of afternoon session)
//        if (time.isBefore(MORNING_SLOT2) &&
//                !time.plusMinutes(duration).isBefore(MORNING_SLOT2)) {
//            return LocalDateTime.of(date, EVENING_SLOT1);
//        }
//
//        //4th Case: If crossing 5 PM then stop
//        //Example:If start at 4:50 PM with 15-minute duration then ends at 5:05 PM,
//        //  then this slot is not valid as it crosses the allowed interview time
//        if (!time.plusMinutes(duration).isBefore(EVENING_SLOT2)) {
//            return null;
//        }
//
//        return current;
//    }
//    @Transactional
//    public String bulkInterviewSchedule(MultipartFile file, SchedulingPanelModel schedulingPanelModel) throws IOException {
//        List<UUID> applicationIds=schedulingPanelModel.getApplicationIds();
//        //Get the data from excel
//        Map<UUID,UUID> applicationToCandidateMap=candidateApplicationsRepository.findAllById(applicationIds)
//                                                .stream()
//                                                .filter(a -> a.getId() != null && a.getCandidateId() != null)
//                                                .collect(Collectors.toMap(
//                                                            CandidateApplicationsEntity::getId,
//                                                            CandidateApplicationsEntity::getCandidateId
//                                                ));
//            try {
//                setExcelFilterContext(schedulingPanelModel.getPositionId());
//                List<SchedulePanelExcelModel> modelList = excelTemplateService.excelToDto(file.getInputStream(), SchedulePanelExcelModel.class);
//                ExcelFilterContext.clear();
//                Set<UUID> panelIds = modelList.stream().map((model) -> model.getPanelId()).collect(Collectors.toSet());
//                Set<LocalDate> interviewDates = modelList.stream().map((model) -> model.getPanelDate()).collect(Collectors.toSet());
//                LocalDateTime startDateTime = interviewDates.stream().map(LocalDate::atStartOfDay).min(LocalDateTime::compareTo).get();
//                LocalDateTime endDateTime = interviewDates.stream().map(localDate -> localDate.atTime(LocalTime.MAX)).max(LocalDateTime::compareTo).get();
//                //Level-1 Validation
//                List<String> validations = checkValidations(modelList, applicationIds, schedulingPanelModel.getPositionId());
//                //If any validations there then throw error
//                if (!validations.isEmpty()) {
//                    throw new ExcelValidationException(validations);
//                }
//                //Get all existing interviews
//                List<InterviewScheduleEntity> existingSchedules = interviewScheduleRepository.findPanelSchedulings(
//                        panelIds,
//                        startDateTime,
//                        endDateTime
//                );
//                List<PanelState> panelStates = modelList.stream()
//                        .map(PanelState::new)
//                        .collect(Collectors.toList());
//
//                SchedulingResult schedules = roundRobinScheduling(applicationIds, panelIds, startDateTime, endDateTime, panelStates, existingSchedules, applicationToCandidateMap);
//                List<InterviewScheduleEntity> interviewScheduleEntities=schedules.interviewScheduleEntities();
//                List<CandidateApplicationsEntity> candidateApplicationsEntityList=schedules.applicationsEntities();
//                if (interviewScheduleEntities.size() < applicationIds.size()) {
//                    log.error("Only {} out of {} applications were scheduled due to limited panel availability.", interviewScheduleEntities.size(), applicationIds.size());
//                    int missingCount = applicationIds.size() - interviewScheduleEntities.size();
//                    String detailedError = "Couldn't schedule interviews for " + missingCount + " due to limited panel availability. Please consider increasing the interview capacity or adjusting the panel schedules.";
//                    throw new ExcelValidationException(List.of(detailedError));
//                }
//                List<InterviewScheduleEntity> savedEntities = interviewScheduleRepository.saveAllWithAudit(null, interviewScheduleEntities);
//                candidateApplicationsRepository.saveAllWithWorkflow(candidateApplicationsEntityList);
//                mailSenderHelper.sendMailToCandidateAndRecruiters(savedEntities, candidateApplicationsEntityList);
//                return "Successfully scheduled " + savedEntities.size() +
//                        " out of " + applicationIds.size() + " interviews.";
//            }finally {
//                ExcelFilterContext.clear();
//            }
//    }
//
//
//    private SchedulingResult roundRobinScheduling(List<UUID> applicationIds, Set<UUID> panelIds, LocalDateTime startDateTime, LocalDateTime endDateTime, List<PanelState> panelStates, List<InterviewScheduleEntity> existingSchedules, Map<UUID,UUID> applicationToCandidateMap){
//        List<InterviewScheduleEntity> interviewScheduleEntities = new ArrayList<>();
//        int pointer = 0;
//
//        Map<UUID, CandidateApplicationsEntity> candidateApplicationsEntityMap = candidateApplicationsRepository.findAllById(applicationIds)
//                .stream()
//                .collect(Collectors.toMap(CandidateApplicationsEntity::getId, a -> a));
//
//        // Get all candidates
//        List<UUID> candidateIds = candidateApplicationsEntityMap.values().stream().map(CandidateApplicationsEntity::getCandidateId).toList();
//        //Get the positions for mail sending
//        List<UUID> positionIds = candidateApplicationsEntityMap.values().stream().map(CandidateApplicationsEntity::getPositionId).toList();
//
//        Map<UUID, JobPositionsEntity> jobPositionsEntityMap = positionsRepository.findAllById(positionIds)
//                .stream().collect(Collectors.toMap(JobPositionsEntity::getId, j -> j));
//        //Get zonalIds based on applicationId
//        List<CandidateLocationPreferenceEntity> allPreferences = candidateLocationPreferencesRepository
//                .findByCandidateAndPosition(candidateIds, positionIds);
////        log.info("Preference:{}",allPreferences);
//        Map<String, UUID> preferenceMap = allPreferences.stream()
//                .collect(Collectors.toMap(
//                        p -> p.getCandidateId() + "_" + p.getPositionId(),
//                        p -> p.getInterviewCenter(),
//                        (existing, replacement) -> existing
//                ));
//        log.info("Preference:{}", preferenceMap);
//        // Fetch all meeting links in ONE DB call
//        List<Object[]> results = interviewScheduleRepository.findMeetingLinksBulk(
//                panelIds,
//                startDateTime,
//                endDateTime
//        );
//
//        // Convert to Map (panelId_date → link)
//        Map<String, String> meetingMap = new HashMap<>();
//
//        for (Object[] row : results) {
//            UUID panelId = (UUID) row[0];
//            Object dateObj = row[1];
//            LocalDate date;
//            //Convert the object to localdate
//            if (dateObj instanceof java.sql.Date sqlDate) {
//                date = sqlDate.toLocalDate();
//            } else if (dateObj instanceof LocalDate localDate) {
//                date = localDate;
//            } else {
//                throw new IllegalStateException("Unexpected date type: " + dateObj.getClass());
//            }
//            String link = (String) row[2];
//            //Create a key with panelId with date
//            String key = panelId + "_" + date;
//
//            meetingMap.putIfAbsent(key, link);
//        }
//
//        //Take a list for the cand app and performing round robin scheduling
//        List<CandidateApplicationsEntity> candidateApplicationsEntityList = new ArrayList<>();
//        for (int i = 0; i < applicationIds.size(); i++) {
//            if (panelStates.isEmpty()) break;
//            pointer = pointer % panelStates.size();
//            PanelState currAssignedPanel = panelStates.get(pointer);
//            LocalDateTime assignedStartDateTime = getValidTimeSlot(currAssignedPanel.nextAvailableTime);
//            //Time validity check, if null means no more slots available for the panel
//            if (assignedStartDateTime == null) {
//                //No more slots available for the day for this panel, so remove the panel and continue
//                panelStates.remove(pointer);
//                i--;
//                continue;
//            }
//            LocalDateTime assignedEndDateTime = assignedStartDateTime.plusMinutes(15);
//
//            //Overflo check
//            if (!assignedStartDateTime.toLocalDate().equals(assignedEndDateTime.toLocalDate())) {
//                panelStates.remove(pointer);
//                i--;
//                continue;
//            }
//
//            //Check whether panel is available or not
//            boolean checkIsPanelAvailable = checkPanelAvailability(interviewScheduleEntities, existingSchedules, currAssignedPanel, assignedStartDateTime, assignedEndDateTime);
//            if (!checkIsPanelAvailable) {
//                currAssignedPanel.setNextAvailableTime(assignedEndDateTime);
//                i--;
//                pointer++;
//                continue;
//            }
//            String meetingSubject = AppConstants.MEETING_SUBJECT;
//            UUID panelId=currAssignedPanel.getId();
//            LocalDate interviewDate = assignedStartDateTime.toLocalDate();
//            String key=panelId+"_"+interviewDate;
//            String meetingUrl = meetingMap.get(key);
//            if (meetingUrl == null) {
//                LocalDateTime dayStart = interviewDate.atStartOfDay();
//                LocalDateTime dayEnd = interviewDate.atTime(LocalTime.MAX);
//                meetingUrl = meetSchedulerService.createMeetingUrl(
//                        meetingSubject,
//                        dayStart.toString(),
//                        dayEnd.toString()
//                );
//                //Keep it for using for other candidates
//                meetingMap.put(key, meetingUrl);
//            }
//
//            CandidateApplicationsEntity candidateApplications = candidateApplicationsEntityMap.get(applicationIds.get(i));
//            UUID zonalOfficeId = preferenceMap.get(candidateApplications.getCandidateId() + "_" + candidateApplications.getPositionId());
//            log.info("Zonal Office:{}", String.valueOf(zonalOfficeId));
//            InterviewCandidateAllocator interviewCandidateAllocator = InterviewCandidateAllocator.builder()
//                    .applicationId(applicationIds.get(i))
//                    .candidateId(applicationToCandidateMap.get(applicationIds.get(i)))
//                    .panelId(currAssignedPanel.getId())
//                    .startTime(assignedStartDateTime)
//                    .endTime(assignedEndDateTime)
//                    .meetingLink(meetingUrl)
//                    .zonalOfficeId(zonalOfficeId)
//                    .build();
//            //Create the entity
//            InterviewScheduleEntity interviewScheduleEntity = createEntity(interviewCandidateAllocator);
//            interviewScheduleEntities.add(interviewScheduleEntity);
//            //Update the status in candidate Applications entity
//            candidateApplications.setApplicationStatus(CandidateApplicationStatus.SCHEDULED);
//            candidateApplicationsEntityList.add(candidateApplications);
//            log.info("ASSIGNED: App {} -> {} | Time: {} - {}",
//                    applicationIds.get(i), currAssignedPanel.getName(), assignedStartDateTime, assignedEndDateTime);
//            currAssignedPanel.setNextAvailableTime(assignedEndDateTime);
//            currAssignedPanel.setRemainingSlots(currAssignedPanel.getRemainingSlots() - 1);
//
//            //Check the availability of the panel Slot
//            //If the slots are set to zero,then remove the panel
//            if (currAssignedPanel.getRemainingSlots() <= 0) {
//                panelStates.remove(pointer);
//            } else {
//                pointer++;
//            }
//
//        }
//        return new SchedulingResult(interviewScheduleEntities,candidateApplicationsEntityList);
//    }
//
//    private boolean checkPanelAvailability(List<InterviewScheduleEntity>newSchedules,List<InterviewScheduleEntity> existingSchedules,PanelState currAssignedPanel, LocalDateTime assignedStartDateTime, LocalDateTime assignedEndDateTime) {
//        //Check in DB
//        boolean checkInDb = existingSchedules.stream()
//                .filter(s -> s.getPanelId().equals(currAssignedPanel.getId()))
//                .anyMatch(s -> assignedStartDateTime.isBefore(s.getInterviewEndAt()) &&
//                        assignedEndDateTime.isAfter(s.getInterviewStartAt()));
//
//        if (checkInDb) {
//            return false;
//        }
//
//        //Check in the current new Schedules,whether panelId is assigned for that particular date or not
//        boolean checkInNewList = newSchedules.stream()
//                .filter(s -> s.getPanelId().equals(currAssignedPanel.getId()))
//                .anyMatch(s -> assignedStartDateTime.isBefore(s.getInterviewEndAt()) &&
//                        assignedEndDateTime.isAfter(s.getInterviewStartAt()));
//
//        return !checkInNewList;
//    }
//
//
//    public InterviewScheduleEntity createEntity(InterviewCandidateAllocator allocator){
//        //Use builder to create entity
//        InterviewScheduleEntity entity=InterviewScheduleEntity.builder()
//                .applicationId(allocator.applicationId())
//                .candidateId(allocator.candidateId())
//                .panelId(allocator.panelId())
//                .interviewStartAt(allocator.startTime())
//                .interviewEndAt(allocator.endTime())
//                .interviewDurationMinutes(15)
//                .meetingLink(allocator.meetingLink())
//                .zonalOfficeId(allocator.zonalOfficeId())
//                .build();
//        return entity;
//
//    }
//
//    public List<String> checkValidations(List<SchedulePanelExcelModel> modelList,List<UUID> applicationIds,UUID positionId){
//        List<String> validations=new ArrayList<>();
//        Integer sumOfInterviews=modelList.stream().mapToInt(SchedulePanelExcelModel::getInterviewPerDay).sum();
//        if(sumOfInterviews<applicationIds.size()){
//            validations.add("Not enough interview slots to schedule all applications. Please increase the interview capacity.");
//        }
//        List<PositionPanelEntity> positionPanelEntities = positionPanelRepository.findByJobPositionId(positionId);
//        Map<UUID, PositionPanelEntity> positionPanelEntityMap = positionPanelEntities.stream()
//                .collect(Collectors.toMap(p -> p.getInterviewPanel().getId(), p -> p));
//
//        Map<UUID,InterviewPanelsEntity> interviewPanelsEntityMap=interviewPanelsRepository.findAllById(
//                modelList.stream().map(SchedulePanelExcelModel::getPanelId).collect(Collectors.toSet())
//        ).stream().collect(Collectors.toMap(InterviewPanelsEntity::getId, i -> i));
//
//        for (SchedulePanelExcelModel model : modelList) {
//            PositionPanelEntity panelMapping = positionPanelEntityMap.get(model.getPanelId());
//
//            // Check if Panel belongs to Position
//            if (panelMapping == null) {
//                validations.add("Panel" + interviewPanelsEntityMap.get(model.getPanelId()).getPanelName()+ " is not associated with this position.");
//                continue;
//            }
//            // check less than 0 case
//            if (model.getInterviewPerDay() <= 0) {
//                validations.add("Interview per day must be greater 0 for panel " + panelMapping.getInterviewPanel().getPanelName());
//            }
//            // Check if Panel Date is within allowed range
//            LocalDate panelDate = model.getPanelDate();
//            //Check panel Date allowed for future dates only
//            if (panelDate.isBefore(LocalDate.now())) {
//                validations.add("Panel date " + model.getPanelDate() + " cannot be in the past for panel " + panelMapping.getInterviewPanel().getPanelName());
//            }
//            if (panelDate.isBefore(panelMapping.getStartDate()) || panelDate.isAfter(panelMapping.getEndDate())) {
//                validations.add("Panel date " + panelDate + " is outside the allowed range ("
//                        + panelMapping.getStartDate() + " to " + panelMapping.getEndDate() + ") for panel " + panelMapping.getInterviewPanel().getPanelName());
//            }
//            // Restrict the user to enter only the start time as betwween the slots of 10:00 - 13:00 and 14:00 - 17:00
//            LocalTime startTime = model.getStartTime();
//            String panelName = interviewPanelsEntityMap
//                    .get(model.getPanelId())
//                    .getPanelName();
//
//            boolean isValidTimeSlot = false;
//
//            // Morning slot: morning any time– 13:00
//            if (startTime.isBefore(MORNING_SLOT2)) {
//                isValidTimeSlot = true;
//            }
//            // Afternoon slot: 14:00 – 17:00
//            else if (!startTime.isBefore(EVENING_SLOT1) &&
//                    startTime.isBefore(EVENING_SLOT2)) {
//
//                isValidTimeSlot = true;
//            }
//
//            if (!isValidTimeSlot) {
//                validations.add("Invalid time " + startTime + " for panel '" + panelName + "'. "
//                        + "Allowed time slots: 12:00 AM–1:00 PM and 2:00 PM–5:00 PM.");
//            }
//
//        }
//
//        return validations;
//    }
// ── Immutable slot

    public record Slot(UUID panelId, LocalDate date, LocalDateTime start, LocalDateTime end, UUID candidateId, UUID applicationId, UUID zoneId, boolean allocated, boolean isPanelAvailable) {
        public Slot assign(UUID candidateId, UUID applicationId, UUID zoneId) {
            return new Slot(panelId, date, start, end, candidateId, applicationId, zoneId, true, isPanelAvailable);
        }
    }
        private List<String> validateSchedulingRequest(List<UUID> applicationIds, List<UUID> positionIds, List<SchedulePanelExcelModel> scheduleModels) {
            List<String> errors = new ArrayList<>();

            List<CandidateApplicationsEntity> applications = candidateApplicationsRepository.findAllById(applicationIds);
            List<UUID> applicationPositionIds = applications.stream().map(CandidateApplicationsEntity::getPositionId).distinct().toList();
            Map<UUID, JobPositionsEntity> jobPositionsMap = positionsRepository.findAllById(positionIds).stream()
                    .collect(Collectors.toMap(JobPositionsEntity::getId, p -> p));

            Set<UUID> masterPositionIds = jobPositionsMap.values().stream()
                    .map(JobPositionsEntity::getMasterPositionId)
                    .collect(Collectors.toSet());

            Map<UUID, MasterPositionsEntity> masterPositionsMap = masterPositionsRepository.findAllById(masterPositionIds).stream()
                    .collect(Collectors.toMap(MasterPositionsEntity::getId, mp -> mp));

            Map<UUID, String> positionIdToNameMap = new HashMap<>();
            jobPositionsMap.forEach((posId, jobPos) -> {
                MasterPositionsEntity mp = masterPositionsMap.get(jobPos.getMasterPositionId());
                positionIdToNameMap.put(posId, mp != null ? mp.getPositionName() : "Unknown Position");
            });
            for (UUID applicationPositionId : applicationPositionIds) {
                if (!positionIds.contains(applicationPositionId)) {
                    errors.add("Position" + positionIdToNameMap.getOrDefault(applicationPositionId, applicationPositionId.toString()) + " exists in selected applications but is not included in the scheduling request.");
                }
            }
            // Check if panel mappings exist for the requested positions
            Map<UUID,List<PositionPanelEntity>> posToPanelMap=positionPanelRepository.findByJobPosition_IdInAndEndDateGreaterThanEqualAndPositionPanelStatus(positionIds,LocalDate.now(),PositionPanelStatus.APPROVED)
                    .stream().collect(Collectors.groupingBy(p->p.getJobPosition().getId()));

            Set<UUID> panelIds = scheduleModels.stream().map(SchedulePanelExcelModel::getPanelId).collect(Collectors.toSet());
            List<PositionPanelEntity> positionPanels = positionPanelRepository.findByInterviewPanel_IdIn(panelIds);

            Map<UUID, List<PositionPanelEntity>> positionPanelMap = positionPanels.stream()
                    .collect(Collectors.groupingBy(pos -> pos.getJobPosition().getId()));

            for (UUID positionId : positionIds) {
                List<PositionPanelEntity> mappedPanels=posToPanelMap.get(positionId);
                if(mappedPanels==null || mappedPanels.isEmpty()){
                    errors.add("There are no panels mapped for this position"+positionIdToNameMap.getOrDefault(positionId,positionId.toString()));
                }
                List<PositionPanelEntity> scheduledPanels = positionPanelMap.get(positionId);
                if (scheduledPanels == null || scheduledPanels.isEmpty()) {
                    errors.add("Please select atleast one panel for Position"+positionIdToNameMap.getOrDefault(positionId, positionId.toString()));
                }
            }

    //        for (PositionPanelEntity mapping : positionPanels) {
    //            UUID mappedPositionId = mapping.getJobPosition().getId();
    //            if (!positionIds.contains(mappedPositionId)) {
    //                errors.add("Selected panel '" + mapping.getInterviewPanel().getPanelName() + "' is not mapped to the requested positions.");
    //            }
    //        }
            return errors;
        }

    public record TimeRange(LocalDateTime start, LocalDateTime end) {}
    @Transactional(readOnly = true)
    public List<InterviewAllocatedRequestModel> allocateInterview(InterviewSchedulingRequestModel request) {
        List<UUID> applicationIds = request.getSchedulingPanelModel().getApplicationIds();
        List<UUID> positionIds = request.getSchedulingPanelModel().getPositionIds();
        List<SchedulePanelExcelModel> scheduleModels = request.getPanelScheduleModelList();

        List<String> errors = validateSchedulingRequest(applicationIds, positionIds, scheduleModels);
        if (!errors.isEmpty()) {
            throw new ExcelValidationException(errors);
        }
        List<CandidateApplicationsEntity> allApps = candidateApplicationsRepository.findAllById(applicationIds);

        Map<UUID, List<CandidateApplicationsEntity>> appsByPosition = allApps.stream()
                .collect(Collectors.groupingBy(CandidateApplicationsEntity::getPositionId));

        Map<UUID, List<InterviewPanelsEntity>> panelsByPosition = positionPanelRepository
                .findByJobPosition_IdInAndEndDateGreaterThanEqualAndPositionPanelStatus(
                        positionIds, LocalDate.now(), PositionPanelStatus.APPROVED)
                .stream()
                .collect(Collectors.groupingBy(
                        p -> p.getJobPosition().getId(),
                        Collectors.mapping(PositionPanelEntity::getInterviewPanel, Collectors.toList())
                ));

        Map<UUID, CandidateApplicationsEntity> appMap =
                allApps.stream().collect(Collectors.toMap(CandidateApplicationsEntity::getId, a -> a));

        List<UUID> candidateIds = allApps.stream()
                .map(CandidateApplicationsEntity::getCandidateId).toList();

        Map<UUID, CandidateProfileEntity> candidateMap =
                candidateProfileRepository.findAllByCandidateIdIn(candidateIds).stream()
                        .collect(Collectors.toMap(
                                CandidateProfileEntity::getCandidateId,
                                c -> c,
                                (a, b) -> a
                        ));

        Map<UUID, InterviewPanelsEntity> panelMap =
                panelsByPosition.values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toMap(
                                InterviewPanelsEntity::getId,
                                p -> p,
                                (a, b) -> a
                        ));

        // Fetch ALL interview centers up front to serve as a master reference map
        List<InterviewCentresEntity> allCentres = interviewCentresRepository.findAll();
        Map<UUID, InterviewCentresEntity> centerMap = allCentres.stream()
                .collect(Collectors.toMap(InterviewCentresEntity::getId, c -> c));

        Map<UUID, String> zoneNamesMap = allCentres.stream()
                .collect(Collectors.toMap(InterviewCentresEntity::getId, InterviewCentresEntity::getDisplayName, (a, b) -> a));

        //Fetch all preferences first
        List<CandidateLocationPreferenceEntity> allLocationPreferences = candidateLocationPreferencesRepository
                .findByCandidateAndPosition(candidateIds, positionIds);

        // Group preferences by a composite key of "candidateId_positionId"
        Map<String, UUID> globalPreferenceMap = allLocationPreferences.stream()
                .collect(Collectors.toMap(
                        pref -> pref.getCandidateId() + "_" + pref.getPositionId(),
                        CandidateLocationPreferenceEntity::getInterviewCenter,
                        (existing, replacement) -> existing
                ));

        Map<UUID, List<TimeRange>> panelBusyMap = new HashMap<>();
        Map<UUID, List<TimeRange>> zoneBusyMap = new HashMap<>();
        Map<UUID, List<TimeRange>> memberBusyMap = new HashMap<>();
        Map<UUID, List<TimeRange>> applicationBusyMap = new HashMap<>();

        LocalDateTime minStart = scheduleModels.stream()
                .map(m -> LocalDateTime.of(m.getPanelDate(), m.getStartTime()))
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime maxEnd = scheduleModels.stream()
                .map(m -> LocalDateTime.of(m.getPanelDate(), m.getEndTime()))
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().plusDays(1));

        List<InterviewScheduleEntity> finalized = interviewScheduleRepository.findExistingSchedulesInRangeAndApplicationIdNotIn(minStart, maxEnd, applicationIds);
        List<InterviewSchedulingApprovalStatus> statusList = List.of(InterviewSchedulingApprovalStatus.L1_PENDING, InterviewSchedulingApprovalStatus.PENDING);
        List<InterviewScheduleStagingEntity> staging = interviewScheduleStagingRepository.findExistingSchedulesInRangeAndApplicationIdNotIn(minStart, maxEnd, applicationIds, statusList);

        finalized.forEach(db ->
                seedBusyMaps(
                        db.getPanelId(),
                        db.getZonalOfficeId(),
                        db.getApplicationId(),
                        db.getInterviewStartAt(),
                        db.getInterviewEndAt(),
                        panelMap,
                        panelBusyMap,
                        zoneBusyMap,
                        memberBusyMap,
                        applicationBusyMap
                )
        );

        staging.forEach(st ->
                seedBusyMaps(
                        st.getPanelId(),
                        st.getZonalOfficeId(),
                        st.getApplication().getId(),
                        st.getInterviewStartAt(),
                        st.getInterviewEndAt(),
                        panelMap,
                        panelBusyMap,
                        zoneBusyMap,
                        memberBusyMap,
                        applicationBusyMap
                )
        );

        Map<UUID, JobPositionsEntity> jobPositionsMap = positionsRepository.findAllById(positionIds).stream()
                .collect(Collectors.toMap(JobPositionsEntity::getId, p -> p));
        Set<UUID> masterPositionIds = jobPositionsMap.values().stream()
                .map(JobPositionsEntity::getMasterPositionId).collect(Collectors.toSet());
        Map<UUID, MasterPositionsEntity> masterPositionsMap = masterPositionsRepository.findAllById(masterPositionIds).stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, mp -> mp));
        Map<UUID, String> positionIdToNameMap = new HashMap<>();
        jobPositionsMap.forEach((posId, jobPos) -> {
            MasterPositionsEntity mp = masterPositionsMap.get(jobPos.getMasterPositionId());
            positionIdToNameMap.put(posId, mp != null ? mp.getPositionName() : "Unknown Position");
        });

        List<InterviewAllocatedRequestModel> result = new ArrayList<>();
        Map<UUID, Integer> positionOrderMap = new HashMap<>();

        for (int i = 0; i < positionIds.size(); i++) {
            positionOrderMap.put(positionIds.get(i), i);
        }
        Map<UUID, List<PositionPanelEntity>> positionPanelEntityMap =
                positionPanelRepository
                        .findByJobPosition_IdInAndEndDateGreaterThanEqualAndPositionPanelStatus(
                                positionIds,
                                LocalDate.now(),
                                PositionPanelStatus.APPROVED
                        )
                        .stream()
                        .collect(Collectors.groupingBy(p -> p.getJobPosition().getId()));

        Map<UUID, List<SchedulePanelExcelModel>> positionScheduleMap = new HashMap<>();

        for (Map.Entry<UUID, List<PositionPanelEntity>> entry : positionPanelEntityMap.entrySet()) {
            UUID positionId = entry.getKey();
            List<PositionPanelEntity> mappings = entry.getValue();

            List<SchedulePanelExcelModel> validSchedules = scheduleModels.stream()
                    .filter(schedule -> mappings.stream().anyMatch(mapping ->
                                    mapping.getInterviewPanel().getId().equals(schedule.getPanelId())
                                            && !schedule.getPanelDate().isBefore(mapping.getStartDate())
                                            && !schedule.getPanelDate().isAfter(mapping.getEndDate())
                            )
                    ).toList();

            positionScheduleMap.put(positionId, validSchedules);
        }

        for (UUID posId : positionIds) {
            List<CandidateApplicationsEntity> posApps = appsByPosition.get(posId);
            List<InterviewPanelsEntity> posPanels = panelsByPosition.get(posId);
            List<SchedulePanelExcelModel> schedulesForPosition = positionScheduleMap.get(posId);
            if (posApps != null && posPanels != null) {
                String positionName = positionIdToNameMap.getOrDefault(posId, "Unknown Position");

                // Pass the pre-computed globalPreferenceMap and zoneNamesMap directly into the assigner
                result.addAll(assignCandidatesToPanelSlots(posId, posApps, posPanels, schedulesForPosition,
                                candidateMap, panelMap, panelBusyMap, zoneBusyMap, memberBusyMap,
                                applicationBusyMap, request.getZonalChangeMap(), globalPreferenceMap, zoneNamesMap
                        )
                                .stream()
                                .filter(s -> s.applicationId() != null)
                                .map(s -> toDTO(s, appMap, candidateMap, panelMap, centerMap, positionName))
                                .toList()
                );
            }
        }

        result = result.stream()
                .sorted(Comparator
                        .comparingInt((InterviewAllocatedRequestModel r) ->
                                positionOrderMap.getOrDefault(r.getApplication().getPositionId(), Integer.MIN_VALUE))
                        .thenComparing(
                                r -> r.getInterviewScheduleStaging().getInterviewStartAt(),
                                Comparator.nullsLast(LocalDateTime::compareTo)
                        )
                ).toList();

        if (result.size() != applicationIds.size()) {
            Set<UUID> allocatedAppIds = result.stream()
                    .map(r -> r.getInterviewScheduleStaging().getApplication().getId())
                    .collect(Collectors.toSet());
            List<UUID> notAllocatedAppIds = applicationIds.stream()
                    .filter(id -> !allocatedAppIds.contains(id))
                    .toList();
            List<String> appToReason = new ArrayList<>();
            for (UUID appId : notAllocatedAppIds) {
                CandidateApplicationsEntity canApp = appMap.get(appId);
                UUID positionId = canApp.getPositionId();
                List<InterviewPanelsEntity> posPanels = panelsByPosition.get(positionId);
                if (posPanels == null || posPanels.isEmpty()) {
                    appToReason.add(String.format("Application %s: No panels available.", canApp.getApplicationNo()));
                } else {
                    List<UUID> panelIds = posPanels.stream().map(InterviewPanelsEntity::getId).toList();
                    List<SchedulePanelExcelModel> panelSchedules = scheduleModels.stream()
                            .filter(m -> panelIds.contains(m.getPanelId()))
                            .toList();
                    String panelNames = panelSchedules.stream()
                            .map(m -> panelMap.get(m.getPanelId()).getPanelName())
                            .distinct()
                            .collect(Collectors.joining(", "));
                    appToReason.add(String.format("Application %s could not be allocated. All slots are full in panels: %s.", canApp.getApplicationNo(), panelNames));
                }
            }
            throw new SchedulingConflictException(String.format("Scheduling failed. Only %s of %s applications were allocated..", result.size(), applicationIds.size()), appToReason);
        }
        return result;
    }


    private Map<UUID, List<UUID>> buildPriorityMap(List<UUID> sortedZones, List<SchedulePanelExcelModel> scheduleModels) {
        Map<UUID, List<UUID>> panelPriority = new LinkedHashMap<>();
        if (sortedZones == null || sortedZones.isEmpty()) {
            return panelPriority;
        }

        List<UUID> uniquePanelIds = scheduleModels.stream()
                .map(SchedulePanelExcelModel::getPanelId)
                .distinct()
                .toList();

        if (uniquePanelIds.isEmpty()) {
            return panelPriority;
        }

        for (int i = 0; i < sortedZones.size(); i++) {
            UUID zoneId = sortedZones.get(i);
            List<UUID> orderedPanelsForThisZone = new ArrayList<>();

            if (i < uniquePanelIds.size()) {
                UUID primaryPanel = uniquePanelIds.get(i);
                orderedPanelsForThisZone.add(primaryPanel);

                for (UUID panelId : uniquePanelIds) {
                    if (!panelId.equals(primaryPanel)) {
                        orderedPanelsForThisZone.add(panelId);
                    }
                }
            } else {
                orderedPanelsForThisZone.addAll(uniquePanelIds);
            }
            panelPriority.put(zoneId, orderedPanelsForThisZone);
        }
        return panelPriority;
    }

    private List<Slot> assignCandidatesToPanelSlots(UUID positionId, List<CandidateApplicationsEntity> applications,
                                                    List<InterviewPanelsEntity> panels, List<SchedulePanelExcelModel> scheduleModels,
                                                    Map<UUID, CandidateProfileEntity> candidateMap, Map<UUID, InterviewPanelsEntity> panelMap,
                                                    Map<UUID, List<TimeRange>> panelBusyMap, Map<UUID, List<TimeRange>> zoneBusyMap,
                                                    Map<UUID, List<TimeRange>> memberBusyMap, Map<UUID, List<TimeRange>> applicationBusyMap,
                                                    Map<UUID, UUID> zonalChangeMap,
                                                    Map<String, UUID> globalPreferenceMap,
                                                    Map<UUID, String> zoneNamesMap) {

        List<UUID> candidateIds = applications.stream().map(CandidateApplicationsEntity::getCandidateId).toList();
        Map<UUID, UUID> candidateToZoneMap = new HashMap<>();

        for (UUID candidateId : candidateIds) {
            UUID candidatePrefZone = globalPreferenceMap.get(candidateId + "_" + positionId);

            if (zonalChangeMap != null && zonalChangeMap.containsKey(candidatePrefZone)) {
                candidateToZoneMap.put(candidateId, zonalChangeMap.get(candidatePrefZone));
            } else {
                candidateToZoneMap.put(candidateId, candidatePrefZone);
            }
        }

        applications.sort(Comparator
                .comparing((CandidateApplicationsEntity app) -> zoneNamesMap.get(candidateToZoneMap.get(app.getCandidateId())), Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(app -> {
                    CandidateProfileEntity profile = candidateMap.get(app.getCandidateId());
                    return (profile != null && Boolean.TRUE.equals(profile.getDisability())) ? 0 : 1;
                })
                .thenComparing(CandidateApplicationsEntity::getCreatedDate)
        );

        List<UUID> sortedZones = applications.stream()
                .map(app -> candidateToZoneMap.get(app.getCandidateId()))
                .distinct()
                .sorted(Comparator.comparing(zoneId -> zoneNamesMap.getOrDefault(zoneId, ""), String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<LocalDate> allLocalDates = scheduleModels.stream().map(SchedulePanelExcelModel::getPanelDate).distinct().sorted().toList();
        List<UUID> uniquePanelIds = scheduleModels.stream().map(SchedulePanelExcelModel::getPanelId).distinct().toList();

        Map<UUID, List<UUID>> panelPriority = buildPriorityMap(sortedZones, scheduleModels);

        Map<LocalDate, List<UUID>> dateByPanelZone = scheduleModels.stream()
                .collect(Collectors.groupingBy(SchedulePanelExcelModel::getPanelDate, TreeMap::new, Collectors.mapping(SchedulePanelExcelModel::getPanelId, Collectors.toList())));

        Map<LocalDate, Map<UUID, List<Slot>>> slotsCache = new HashMap<>();
        for (LocalDate lDate : allLocalDates) {
            List<UUID> panelsOnDate = dateByPanelZone.getOrDefault(lDate, Collections.emptyList());
            Map<UUID, List<Slot>> panelMapForDate = new LinkedHashMap<>();

            for (UUID panelId : panelsOnDate) {
                List<Slot> baselineSlots = new ArrayList<>(generateSlotsForPanelAndDate(panelId, lDate, scheduleModels));
                panelMapForDate.put(panelId, baselineSlots);
            }
            slotsCache.put(lDate, panelMapForDate);
        }

        Set<UUID> allocatedAppIds = new HashSet<>();
        List<Slot> allocated = new ArrayList<>();

        for (LocalDate lDate : allLocalDates) {
            List<UUID> panelsOnThisDate = dateByPanelZone.get(lDate);
            if (panelsOnThisDate == null || panelsOnThisDate.isEmpty()) continue;

            Map<UUID, List<Slot>> panelSlotsOnDateMap = slotsCache.get(lDate);
            boolean isLockedDate = !lDate.isAfter(LocalDate.now());

            for (CandidateApplicationsEntity app : applications) {
                if (allocatedAppIds.contains(app.getId())) continue;

                UUID candidateId = app.getCandidateId();
                UUID zoneId = candidateToZoneMap.get(candidateId);
                List<UUID> priorityPanelsForZone = panelPriority.getOrDefault(zoneId, Collections.emptyList());

                boolean appScheduledForToday = false;

                for (UUID panelId : priorityPanelsForZone) {
                    if (appScheduledForToday || !panelsOnThisDate.contains(panelId)) continue;

                    List<Slot> generatedSlots = panelSlotsOnDateMap.get(panelId);
                    if (generatedSlots == null) continue;

                    for (int sIdx = 0; sIdx < generatedSlots.size(); sIdx++) {
                        Slot slot = generatedSlots.get(sIdx);
                        if (slot.allocated() || !slot.isPanelAvailable()) continue;

                        if ((isFree(panelBusyMap.get(panelId), slot) &&
                                isFree(zoneBusyMap.get(zoneId), slot) &&
                                membersFree(panelId, panelMap, memberBusyMap, slot) &&
                                isFree(applicationBusyMap.get(app.getId()), slot))) {

                            Slot assignedSlot = slot.assign(candidateId, app.getId(), zoneId);

                            seedBusyMaps(panelId, zoneId, app.getId(), assignedSlot.start(), assignedSlot.end(),
                                    panelMap, panelBusyMap, zoneBusyMap, memberBusyMap, applicationBusyMap);

                            generatedSlots.set(sIdx, assignedSlot);
                            allocated.add(assignedSlot);
                            allocatedAppIds.add(app.getId());
                            appScheduledForToday = true;
                            break;
                        }
                    }
                }
            }

            if (!isLockedDate) {
                for (UUID panelId : uniquePanelIds) {
                    if (!panelsOnThisDate.contains(panelId)) continue; // Ensure panel is active on this day

                    List<Slot> generatedSlots = panelSlotsOnDateMap.get(panelId);
                    if (generatedSlots == null) continue;

                    for (int sIdx = 0; sIdx < generatedSlots.size(); sIdx++) {
                        Slot slot = generatedSlots.get(sIdx);
                        if (slot.allocated() || !slot.isPanelAvailable()) continue;

                        for (CandidateApplicationsEntity app : applications) {
                            if (allocatedAppIds.contains(app.getId())) continue;

                            UUID candidateId = app.getCandidateId();
                            UUID candidateZoneId = candidateToZoneMap.get(candidateId);

                            if (isFree(panelBusyMap.get(panelId), slot) &&
                                    isFree(zoneBusyMap.get(candidateZoneId), slot) &&
                                    membersFree(panelId, panelMap, memberBusyMap, slot) &&
                                    isFree(applicationBusyMap.get(app.getId()), slot)) {

                                Slot assignedSlot = slot.assign(candidateId, app.getId(), candidateZoneId);

                                seedBusyMaps(panelId, candidateZoneId, app.getId(), assignedSlot.start(), assignedSlot.end(),
                                        panelMap, panelBusyMap, zoneBusyMap, memberBusyMap, applicationBusyMap);

                                generatedSlots.set(sIdx, assignedSlot);
                                allocated.add(assignedSlot);
                                allocatedAppIds.add(app.getId());
                                break;
                            }
                        }
                    }
                }
            }
        }
        return allocated;
    }

    private List<Slot> generateSlotsForPanelAndDate(UUID panelId, LocalDate date, List<SchedulePanelExcelModel> scheduleModels) {
        List<Slot> generatedSlots = new ArrayList<>();

        LocalTime lunchStart = LocalTime.of(13, 0);
        LocalTime lunchEnd = LocalTime.of(14, 0);

        List<SchedulePanelExcelModel> matchingConfigs = scheduleModels.stream()
                .filter(m -> m.getPanelId().equals(panelId) && m.getPanelDate().equals(date))
                .toList();

        for (SchedulePanelExcelModel schedule : matchingConfigs) {
            LocalDateTime currentTime = LocalDateTime.of(schedule.getPanelDate(), schedule.getStartTime());
            LocalDateTime panelEndTime = LocalDateTime.of(schedule.getPanelDate(), schedule.getEndTime());

            int maxInterviews = schedule.getInterviewPerDay();
            int duration = schedule.getDurationInMinutes();
            int generatedCount = 0;

            while (currentTime.isBefore(panelEndTime) && generatedCount < maxInterviews) {
                LocalTime currentLocalTime = currentTime.toLocalTime();

                if (!currentLocalTime.isBefore(lunchStart) && currentLocalTime.isBefore(lunchEnd)) {
                    currentTime = currentTime.with(lunchEnd);
                    continue;
                }

                LocalDateTime nextEndTime = currentTime.plusMinutes(duration);

                if (nextEndTime.isAfter(panelEndTime)) {
                    break;
                }

                if (currentTime.toLocalTime().isBefore(lunchStart) && nextEndTime.toLocalTime().isAfter(lunchStart)) {
                    currentTime = currentTime.with(lunchEnd);
                    continue;
                }

                generatedSlots.add(new Slot(
                        schedule.getPanelId(),
                        schedule.getPanelDate(),
                        currentTime,
                        nextEndTime,
                        null, null, null, false, true
                ));

                generatedCount++;
                currentTime = nextEndTime;
            }
        }

        return generatedSlots.stream()
                .sorted(Comparator.comparing(Slot::start))
                .toList();
    }

    private void seedBusyMaps(UUID panelId, UUID zoneId, UUID applicationId,
                              LocalDateTime startTime, LocalDateTime endTime,
                              Map<UUID, InterviewPanelsEntity> panelMap,
                              Map<UUID, List<TimeRange>> panelBusyMap,
                              Map<UUID, List<TimeRange>> zoneBusyMap,
                              Map<UUID, List<TimeRange>> memberBusyMap,
                              Map<UUID, List<TimeRange>> applicationBusyMap) {

        TimeRange timeRange = new TimeRange(startTime, endTime);

        // 1. Block the Panel
        if (panelId != null) {
            panelBusyMap.computeIfAbsent(panelId, k -> new ArrayList<>()).add(timeRange);
        }

        // 2. Block the Zonal Office / Interview Centre
        if (zoneId != null) {
            zoneBusyMap.computeIfAbsent(zoneId, k -> new ArrayList<>()).add(timeRange);
        }

        // 3. Block the Application (prevents scheduling conflicts for the same applicant)
        if (applicationId != null) {
            applicationBusyMap.computeIfAbsent(applicationId, k -> new ArrayList<>()).add(timeRange);
        }

        // 4. Cascade the block to all individual panel members assigned to this panel
        if (panelId != null) {
            InterviewPanelsEntity panel = panelMap.get(panelId);
            if (panel != null && panel.getPanelMembers() != null) {
                panel.getPanelMembers().forEach(member -> {
                    if (member.getPanelMember() != null && member.getPanelMember().getId() != null) {
                        UUID memberId = member.getPanelMember().getId();
                        memberBusyMap.computeIfAbsent(memberId, k -> new ArrayList<>()).add(timeRange);
                    }
                });
            }
        }
    }



    private InterviewAllocatedRequestModel toDTO(Slot s, Map<UUID, CandidateApplicationsEntity> am, Map<UUID, CandidateProfileEntity> cm, Map<UUID, InterviewPanelsEntity> pm, Map<UUID, InterviewCentresEntity> zm, String positionName) {
        return InterviewAllocatedRequestModel.builder()
                .fullName(commonUtilityProvider.buildFullName(cm.get(s.candidateId())))
                .application(candidateApplicationsMapper.toDTO(am.get(s.applicationId())))
                .interviewPanels(interviewPanelsMapper.toDtoWithoutChildren(pm.get(s.panelId())))
                .interviewCentres(interviewCentresMapper.toDto(zm.get(s.zoneId())))
                .interviewScheduleStaging(InterviewScheduleStagingDTO.builder()
                        .application(candidateApplicationsMapper.toDTO(am.get(s.applicationId())))
                        .candidateId(s.candidateId())
                        .panelId(s.panelId())
                        .zonalOfficeId(s.zoneId())
                        .interviewStartAt(s.start())
                        .interviewEndAt(s.end())
                        .build())
                .build();
    }



    private boolean isFree(List<TimeRange> busyTimeRanges, Slot slot) {
        if (busyTimeRanges == null || busyTimeRanges.isEmpty()) {
            return true;
        }

        // A slot is free ONLY if it does not overlap with any busy intervals
        // Overlap formula: slot.start < range.end AND slot.end > range.start
        return busyTimeRanges.stream()
                .noneMatch(range ->
                        slot.start().isBefore(range.end()) &&
                                slot.end().isAfter(range.start())
                );
    }
    private boolean membersFree(UUID panelId, Map<UUID, InterviewPanelsEntity> panelMap,
                                Map<UUID, List<TimeRange>> memberBusyMap, Slot slot) {

        InterviewPanelsEntity panel = panelMap.get(panelId);

        if (panel == null || panel.getPanelMembers() == null) {
            return true;
        }
        // Ensure EVERY single member assigned to this panel is free during the slot window
        return panel.getPanelMembers().stream()
                .allMatch(member -> {
                    if (member.getPanelMember() == null || member.getPanelMember().getId() == null) {
                        return true;
                    }
                    UUID memberId = member.getPanelMember().getId();
                    List<TimeRange> busyTimes = memberBusyMap.get(memberId);
                    return isFree(busyTimes, slot);
                });
    }



    @Transactional
    public String scheduleInterview(SchedulePanelModel schedulePanelModel) {

        List<InterviewScheduleStagingEntity> stagingEntities = interviewScheduleStagingMapper.toEntityList(schedulePanelModel.getAllocatedRequestModelList().stream()
                                .map(InterviewAllocatedRequestModel::getInterviewScheduleStaging)
                                .toList()
                );

        List<SchedulePanelExcelModel> panelExcelModelList=schedulePanelModel.getPanelExcelModelList();

        // 1. Validate conflicts
        List<SchedulingConflictResponseModel> errorModel = validateSchedulingConflicts(stagingEntities);

        if (!errorModel.isEmpty()) {
            throw new SchedulingConflictException("Cannot schedule Interviews",errorModel);
        }

        // 2. Fetch Applications
        List<UUID> applicationIds = schedulePanelModel.getAllocatedRequestModelList().stream().map(r -> r.getInterviewScheduleStaging().getApplication().getId()).toList();
        List<CandidateApplicationsEntity> applications = candidateApplicationsRepository.findAllById(applicationIds);
        List<UUID> positionIds=applications.stream().map(CandidateApplicationsEntity::getPositionId).toList();
        List<InterviewScheduleEntity> oldInterviewScheduleEntities=interviewScheduleRepository.findByApplicationIdIn(applicationIds);

        List<InterviewScheduleEntity> newInterviewScheduleEntities=interviewScheduleRepository.findByApplicationIdIn(applicationIds);
        Map<UUID,CandidateApplicationsEntity> applicationsEntityMap=applications.stream().collect(Collectors.toMap(CandidateApplicationsEntity::getId,c->c));
        Map<UUID, List<PositionPanelEntity>> positionPanelEntityMap =
                positionPanelRepository.findByJobPositionIdIn(positionIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                p -> p.getJobPosition().getId()
                        ));
        // 3. Update Application Status
        applications.forEach(app ->
                app.setApplicationStatus(CandidateApplicationStatus.SCHEDULE_PENDING)
        );
        newInterviewScheduleEntities.forEach(is->{
            is.setInterviewStatus(InterviewSchedulingStatus.RESCHEDULE_PENDING);
            CandidateApplicationsEntity rescheduleAppli=applicationsEntityMap.get(is.getApplicationId());
            rescheduleAppli.setApplicationStatus(CandidateApplicationStatus.RESCHEDULE_PENDING);
        });

        Map<UUID,InterviewScheduleStagingEntity> existingStagingEntities=interviewScheduleStagingRepository.findByApplicationIdIn(applicationIds)
                .stream().collect(Collectors.toMap(
                s -> s.getApplication().getId(),
                s -> s,
                (a, b) -> a
        ));
        //Position
        List<InterviewPanelScheduleConfigurationEntity> panelScheduleConfigurationEntities=new ArrayList<>();
        Map<UUID,List<InterviewPanelScheduleConfigurationEntity>> presentConfigMap=interviewPanelScheduleConfigurationRepository.findByApplicationIdIn(applicationIds)
                                                                    .stream().
                collect(Collectors.groupingBy(
                        InterviewPanelScheduleConfigurationEntity::getApplicationId
                ));
        stagingEntities.forEach(stageEntity->{
            UUID applicationId=stageEntity.getApplication().getId();
            InterviewScheduleStagingEntity existingEntity = existingStagingEntities.get(applicationId);
            if (existingEntity != null) {
                stageEntity.setId(existingEntity.getId());
            }
            int durationInMinutes=(int)Duration.between(stageEntity.getInterviewStartAt(),stageEntity.getInterviewEndAt()).toMinutes();
            stageEntity.setInterviewSchedulingApprovalStatus(InterviewSchedulingApprovalStatus.PENDING);
            stageEntity.setInterviewDurationMinutes(durationInMinutes);
            panelScheduleConfigurationEntities.addAll(createPanelConfiguration(stageEntity,panelExcelModelList,applicationsEntityMap,positionPanelEntityMap));
        });
        interviewPanelScheduleConfigurationRepository.saveAll(panelScheduleConfigurationEntities);
        interviewPanelScheduleConfigurationRepository.deleteAll(
                presentConfigMap.values()
                        .stream()
                        .flatMap(List::stream)
                        .toList()
        );
        // 5. Save everything
        candidateApplicationsRepository.saveAllWithWorkflow(applications);
        interviewScheduleStagingRepository.saveAll(stagingEntities);
        interviewScheduleRepository.saveAllWithAudit(oldInterviewScheduleEntities,newInterviewScheduleEntities);
        return "Successfully scheduled " + stagingEntities.size() + " interviews.";
    }

    private List<InterviewPanelScheduleConfigurationEntity> createPanelConfiguration(InterviewScheduleStagingEntity staging
                                                                                ,List<SchedulePanelExcelModel> schedulePanelExcelModels
                                                                            ,Map<UUID,CandidateApplicationsEntity> appMap,Map<UUID,List<PositionPanelEntity>> positionPanelMap) {
        UUID positionId=appMap.get(staging.getApplication().getId()).getPositionId();
        List<InterviewPanelScheduleConfigurationEntity> configurationEntities=new ArrayList<>();
        List<UUID> panelIds = positionPanelMap.get(positionId)
                .stream()
                .map(p -> p.getInterviewPanel().getId())
                .toList();
        for(SchedulePanelExcelModel model: schedulePanelExcelModels){
            if(panelIds.contains(model.getPanelId())){
                LocalDateTime startDateTime =
                        LocalDateTime.of(
                                model.getPanelDate(),
                                model.getStartTime()
                        );

                LocalDateTime endDateTime =
                        LocalDateTime.of(
                                model.getPanelDate(),
                                model.getEndTime()
                        );

                configurationEntities.add(
                        InterviewPanelScheduleConfigurationEntity.builder()
                                .applicationId(staging.getApplication().getId())
                                .panelId(model.getPanelId())
                                .startDatetime(startDateTime)
                                .endDatetime(endDateTime)
                                .interviewsPerDay(model.getInterviewPerDay())
                                .durationMinutes(model.getDurationInMinutes())
                                .build()
                );
            }
        }
        return configurationEntities;
    }

    @Transactional(readOnly = true)
    public List<SchedulingConflictResponseModel> validateSchedulingConflicts(List<InterviewScheduleStagingEntity> stagingList) {

        Map<String, SchedulingConflictResponseModel> conflictMap = new LinkedHashMap<>();
        // 1. Calculate boundaries from the schedulings
        LocalDateTime minStart = stagingList.stream().
                map(InterviewScheduleStagingEntity::getInterviewStartAt)
                .min(LocalDateTime::compareTo).orElse(LocalDateTime.now());

        LocalDateTime maxEnd = stagingList.stream()
                .map(InterviewScheduleStagingEntity::getInterviewEndAt)
                .max(LocalDateTime::compareTo).orElse(LocalDateTime.now().plusDays(1));

        // Fetch panel and its members
        Set<UUID> allPanelIds = new HashSet<>();
        stagingList.forEach(s -> allPanelIds.add(s.getPanelId()));

        // Get appId data
        List<UUID> allApplicationIds=stagingList.stream().map(ias->ias.getApplication().getId()).toList();

        // Fetch DB data
        List<InterviewScheduleEntity> finalizedDb = interviewScheduleRepository.findExistingSchedulesInRangeAndApplicationIdNotIn(minStart, maxEnd,allApplicationIds);
//    List<InterviewScheduleStagingEntity> stagingDb = interviewScheduleStagingRepository.findExistingSchedulesInRange(minStart, maxEnd);

        // Collect Panel IDs from DB results to get their members too
        finalizedDb.forEach(db -> allPanelIds.add(db.getPanelId()));
//    stagingDb.forEach(st -> allPanelIds.add(st.getPanelId()));

        Map<UUID, Set<UUID>> panelMembersMap = interviewPanelsRepository.findAllById(allPanelIds).stream().collect(Collectors.toMap(InterviewPanelsEntity::getId, p -> p.getPanelMembers().stream().map(m -> m.getPanelMember().getId()).collect(Collectors.toSet())));
        Set<UUID> applicationIds = finalizedDb.stream()
                .map(InterviewScheduleEntity::getApplicationId)
                .collect(Collectors.toSet());

        Map<UUID, String> applicationNoMap =
                candidateApplicationsRepository.findAllById(applicationIds)
                        .stream()
                        .collect(Collectors.toMap(
                                CandidateApplicationsEntity::getId,
                                CandidateApplicationsEntity::getApplicationNo
                        ));
        // 3. Validation Loop
        for (int i = 0; i < stagingList.size(); i++) {
            InterviewScheduleStagingEntity current = stagingList.get(i);
            LocalDateTime curS = current.getInterviewStartAt();
            LocalDateTime curE = current.getInterviewEndAt();
            Set<UUID> curMembers = panelMembersMap.getOrDefault(current.getPanelId(), Collections.emptySet());
            // Check layer 1: Check with itself
            for (int j = i + 1; j < stagingList.size(); j++) {
                InterviewScheduleStagingEntity other = stagingList.get(j);
                // Skip same object
                if (current == other) {
                    continue;
                }
                LocalDateTime otherS = other.getInterviewStartAt();
                LocalDateTime otherE = other.getInterviewEndAt();
                // Check overlap
                if (isOverlap(curS, curE, otherS, otherE)) {
                    Set<UUID> otherMembers = panelMembersMap.getOrDefault(other.getPanelId(), Collections.emptySet());
                    performResourceCheck(current, other.getApplication().getApplicationNo(), other.getPanelId(), other.getZonalOfficeId(), otherMembers, curMembers, conflictMap);
                }
            }

            // --- Check Layer 2: Finalized Schedules ---
            for (InterviewScheduleEntity db : finalizedDb) {
                if (isOverlap(curS, curE, db.getInterviewStartAt(), db.getInterviewEndAt())) {
                    performResourceCheck(current, applicationNoMap.get(db.getApplicationId()), db.getPanelId(), db.getZonalOfficeId(), panelMembersMap.getOrDefault(db.getPanelId(), Collections.emptySet()), curMembers, conflictMap);
                }
            }
        }
        return new ArrayList<>(conflictMap.values());
    }


    private void performResourceCheck(InterviewScheduleStagingEntity current, String conflictingApplicationNo,
            UUID otherPanelId, UUID otherZoneId, Set<UUID> otherMembers, Set<UUID> currentMembers,
            Map<String, SchedulingConflictResponseModel> conflictMap) {

        String appNo = current.getApplication().getApplicationNo();

        boolean panelConflict = Objects.equals(current.getPanelId(), otherPanelId);

        boolean zoneConflict = Objects.equals(current.getZonalOfficeId(), otherZoneId);

        boolean memberConflict =
                !panelConflict &&
                        otherMembers != null &&
                        currentMembers.stream().anyMatch(otherMembers::contains);

        // IMPORTANT:
        // Do not create conflict object if no actual conflict exists
        if (!panelConflict && !zoneConflict && !memberConflict) {
            return;
        }

        SchedulingConflictResponseModel response =
                conflictMap.computeIfAbsent(appNo, k ->
                        SchedulingConflictResponseModel.builder()
                                .applicationNo(appNo)
                                .conflictingApplicationNo(conflictingApplicationNo)
                                .startTime(current.getInterviewStartAt())
                                .endTime(current.getInterviewEndAt())
                                .panelConflict(false)
                                .zoneConflict(false)
                                .memberConflict(false)
                                .build());

        response.setPanelConflict(panelConflict);
        response.setZoneConflict(zoneConflict);
        response.setMemberConflict(memberConflict);

        response.setMessage(buildConflictMessage(response));
    }


    private String buildConflictMessage(SchedulingConflictResponseModel response) {

        String applicationNo = response.getApplicationNo();

        boolean panelConflict = response.isPanelConflict();

        boolean zoneConflict = response.isZoneConflict();

        boolean memberConflict =
                response.isMemberConflict();

        // all three conflicts
        if (panelConflict && zoneConflict && memberConflict) {
            return String.format("Application %s cannot be scheduled. " + "Panel, zone, and interviewers are unavailable.", applicationNo);
        }
        //Panel zone conflict
        if (panelConflict && zoneConflict) {
            return String.format("Application %s cannot be scheduled. " + "Panel and zone are unavailable.", applicationNo);
        }
        //Panel and member conflict
        if (panelConflict && memberConflict) {
            return String.format("Application %s cannot be scheduled. " + "Panel and interviewers are unavailable.", applicationNo);
        }
        //Zone and member conflict
        if (zoneConflict && memberConflict) {
            return String.format("Application %s cannot be scheduled. " + "Zone and interviewers are unavailable.", applicationNo);
        }
        //Panel conflict only
        if (panelConflict) {
            return String.format("Application %s cannot be scheduled. " + "Panel is unavailable.", applicationNo);
        }
        //Zone conflict only
        if (zoneConflict) {
            return String.format("Application %s cannot be scheduled. " + "Zone is unavailable.", applicationNo);
        }

        if (memberConflict) {
            return String.format("Application %s cannot be scheduled. " + "Interviewers are unavailable.", applicationNo);
        }

        return String.format("Application %s cannot be scheduled.", applicationNo);
    }

    private boolean isOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}
