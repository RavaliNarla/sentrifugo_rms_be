package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.exception.SchedulingConflictException;
import com.bob.commonutil.model.candidateportal.RequestHistoryModel;
import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.ConversationMessagesDTO;
import com.bob.db.dto.ConversationThreadsDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.enums.ConversationThreadsStatus;
import com.bob.db.enums.SenderTypeEnum;
import com.bob.db.mapper.CandidateApplicationsMapper;
import com.bob.db.mapper.ConversationMessagesMapper;
import com.bob.db.mapper.ConversationThreadsMapper;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import com.bob.jobportal.model.*;
import com.bob.jobportal.util.OfferLetterGenerationUtil;
import com.bob.jobportal.util.OfferMailModel;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageService {

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private ConversationMessagesRepository conversationMessagesRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private ConversationThreadsRepository conversationThreadsRepository;

    @Autowired
    private ConversationThreadsMapper conversationThreadsMapper;

    @Autowired
    private ConversationMessagesMapper conversationMessagesMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private RequisitionApproversRepository requisitionApproversRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private MailSenderHelper mailSenderHelper;

    @Autowired
    private RequestTypesRepository requestTypesRepository;

    @Autowired
    private CandidateScreeningRepository candidateScreeningRepository;

    @Autowired
    private CandidateOffersRepository candidateOffersRepository;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private InterviewScheduleStagingRepository interviewScheduleStagingRepository;

    @Autowired
    private InterviewSchedulingService schedulingService;

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private CandidateApplicationsMapper candidateApplicationsMapper;

    @Autowired
    private OfferLetterGenerationUtil offerLetterGenerationUtil;

    public Page<MessageThreadResponseModel> getAllConversationHistory(MessageThreadRequestModel request, Pageable pageable) {
        List<CandidateApplicationsEntity> candidateApplicationsEntities = candidateApplicationsRepository.findByPositionIdIn(request.getPositionsIds());

        List<UUID> allApplicationIds = candidateApplicationsEntities.stream()
                .map(CandidateApplicationsEntity::getId)
                .distinct()
                .toList();
         ;

        Page<ConversationThreadsEntity> threadsPage = bulidFilterSpecification(allApplicationIds,request.getRequestTypeIds(),request.getStatusList(),request.getSearchText(),pageable);

        List<UUID> filteredApplicationIds = threadsPage.stream()
                .map(ConversationThreadsEntity::getApplicationId).toList();



        List<CandidateApplicationsEntity> filteredApplications =
                candidateApplicationsEntities.stream()
                        .filter(application ->
                                filteredApplicationIds.contains(application.getId()))
                        .toList();
        List<UUID> candidatesIds = filteredApplications.stream().map(CandidateApplicationsEntity::getCandidateId).toList();
        List<CandidateProfileEntity> candidateProfileEntitiesList = candidateProfileRepository.findAllByCandidateIdIn(candidatesIds);

        Map<UUID, CandidateProfileEntity> candidateProfileEntityMap =
                candidateProfileEntitiesList.stream()
                        .collect(Collectors.toMap(
                                CandidateProfileEntity::getCandidateId,
                                profile -> profile,
                                (existing, replacement) -> existing
                        ));

        Map<UUID, CandidateApplicationsEntity> applicationMap =
                filteredApplications.stream()
                        .collect(Collectors.toMap(
                                CandidateApplicationsEntity::getId,
                                application -> application
                        ));



        return threadsPage.map(thread -> {

            CandidateApplicationsEntity application =
                    applicationMap.get(thread.getApplicationId());

            CandidateProfileEntity profile =
                    candidateProfileEntityMap.get(application.getCandidateId());

            return MessageThreadResponseModel.builder()
                    .candidateName(
                            commonUtilityProvider.buildFullName(profile)
                    )
                    .applicationNo(application.getApplicationNo())
                    .positionId(application.getPositionId())
                    .applicationId(thread.getApplicationId())
                    .requestTypeId(thread.getRequestTypeId())
                    .status(thread.getStatus())
                    .createdDate(thread.getCreatedDate())
                    .conversationThreadId(thread.getId())
                    .dateExtension(thread.getDateExtension())
                    .zonalId(thread.getZonalId())
                    .build();
        });


    }

    public List<MessageResponseModel> getAllMessages(UUID threadId) {
        List<ConversationMessagesEntity> messagesEntityList =
                conversationMessagesRepository.findAllByThreadId(
                        threadId,
                        Sort.by(Sort.Direction.ASC, "createdDate")
                );

        List<UUID> userIdList = new ArrayList<>();
        List<UUID> candidateIdList = new ArrayList<>();
        messagesEntityList.forEach(item ->{
            if(item.getSenderType() == SenderTypeEnum.CANDIDATE){
                candidateIdList.add(item.getSenderId());
            }else{
                userIdList.add(item.getSenderId());
            }
        });
        List<CandidateProfileEntity> candidateProfileEntities = candidateProfileRepository.findAllByCandidateIdIn(candidateIdList);
        List<UserEntity> userEntityList = userRepository.findAllById(userIdList);

        Map<UUID,String> userMap = userEntityList.stream().collect(Collectors.toMap( UserEntity::getId,UserEntity::getName));
        candidateProfileEntities.forEach(item ->    userMap.put(item.getCandidateId(),commonUtilityProvider.buildFullName(item)));

        return messagesEntityList.stream().map(m ->
             MessageResponseModel.builder()
                    .conversationMessageId(m.getId())
                    .comments(m.getMessage())
                    .userName(Map.of(m.getSenderId(),userMap.get(m.getSenderId())))
                    .createdDate(m.getCreatedDate())
                    .senderType(m.getSenderType())
                     .attachmentPath(m.getAttachmentPath())
                    .build()
        ).toList();

    }

    public RequestHistoryModel submitForApproval(MessageApprovalRequestModel approvalRequestModel) throws MessagingException, IOException {

        List<UUID> threadIds = approvalRequestModel.getConversationThreadId();

        if (threadIds == null || threadIds.size() != 1) {
            throw new CommonException("Exactly one conversation thread id is required");
        }

        ConversationThreadsEntity conversationThreadsEntity = conversationThreadsRepository.findById(threadIds.get(0))
                .orElseThrow(() ->new CommonException("Conversation thread not found"));

        ConversationThreadsStatus requestedStatus = approvalRequestModel.getStatus();

        ConversationThreadsStatus targetStatus = null;
        ConversationThreadsStatus requiredCurrentStatus = null;

        if(requestedStatus.equals(ConversationThreadsStatus.L1_PENDING)){
            targetStatus = ConversationThreadsStatus.L1_PENDING;
            requiredCurrentStatus = ConversationThreadsStatus.PENDING;
        }else if(requestedStatus.equals(ConversationThreadsStatus.REJECTED)){
            targetStatus = ConversationThreadsStatus.REJECTED;
        }else {
            throw new CommonException(String.format("User cannot perform action '%s'.Allowed: %S, %S ",
                    requestedStatus,ConversationThreadsStatus.PENDING,ConversationThreadsStatus.REJECTED));
        }

        if(requiredCurrentStatus != null && conversationThreadsEntity.getStatus() != requiredCurrentStatus){
            throw new CommonException(String.format("Cannot process the conversation. All requisitions must be in '%s' state. Found a requisition in '%s' state.",
                    requiredCurrentStatus, conversationThreadsEntity.getStatus()));
        }

        conversationThreadsEntity.setStatus(targetStatus);
        WorkflowApprovalEntity workflowApproval = createWorkflowEntityMessages(conversationThreadsEntity,approvalRequestModel.getComments());
        workflowApprovalEntityRepository.save(workflowApproval);
        ConversationMessagesEntity conversationMessage = ConversationMessagesEntity.builder()
                .threadId(conversationThreadsEntity.getId())
                .message(approvalRequestModel.getComments())
                .senderId(workflowApproval.getApproverId())
                .senderType(SenderTypeEnum.RECRUITER)
                .build();
        ConversationMessagesDTO savedConversationMessage = conversationMessagesMapper.toDto(conversationMessagesRepository.save(conversationMessage));
        ConversationThreadsDTO savedConversationThread = conversationThreadsMapper.toDTO(conversationThreadsRepository.save(conversationThreadsEntity));

        if(targetStatus.equals(ConversationThreadsStatus.REJECTED)){
            mailSenderHelper.sendMailsToCandidateUponApprovalAndRejections(List.of(conversationThreadsEntity),false);
        }

        if(targetStatus.equals(ConversationThreadsStatus.L1_PENDING)){
            UUID applicationId = conversationThreadsEntity.getApplicationId();
            mailSenderHelper.sendExtensionApprovalMail(List.of(applicationId), RequisitionApproversEntity.ApproverRole.L1);
        }
        return RequestHistoryModel.builder()
                .conversationThreads(savedConversationThread)
                .conversationMessages(List.of(savedConversationMessage))
                .build();



    }


    // approval for L1 and L2
    public List<ConversationThreadsDTO> submitForApprovalL1AndL2(MessageApprovalRequestModel approvalRequestModel) throws MessagingException, IOException {
        if (approvalRequestModel.getConversationThreadId() == null) {
            throw new CommonException("Conversation thread id cannot be null");
        }
        List<ConversationThreadsEntity> conversationThreadsEntities = conversationThreadsRepository.findAllById(approvalRequestModel.getConversationThreadId());

        if(conversationThreadsEntities.isEmpty()){
            throw new ResourceNotFoundException("No conversation threads found");
        }

        List<WorkflowApprovalEntity> workflowApprovalEntities = new ArrayList<>();
        UUID currentUser = securityUtils.getCurrentUserId();

        RequisitionApproversEntity approversEntity = requisitionApproversRepository.findByApproverId(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Current user is not configured as a requisition approver."));

        RequisitionApproversEntity.ApproverRole role = approversEntity.getApproverRole();
        ConversationThreadsStatus requestedStatus = approvalRequestModel.getStatus();

        if (role != RequisitionApproversEntity.ApproverRole.L1 && role != RequisitionApproversEntity.ApproverRole.L2) {
            throw new IllegalStateException("User has an unsupported approver role: " + role);
        }

        String comments = approvalRequestModel.getComments();

        ConversationThreadsStatus targetStatus = null;
        ConversationThreadsStatus requiredCurrentStatus = null;


        List<String> l1Approved =AppConstants.MESSAGES_L2_APPROVED_REQUESTS;

        RequestTypesEntity requestType = requestTypesRepository
                .findById(conversationThreadsEntities.get(0).getRequestTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Request type not found"));

        if (role.equals(RequisitionApproversEntity.ApproverRole.L1)) {
            if(requestedStatus.equals(ConversationThreadsStatus.L2_PENDING)){
                targetStatus =  l1Approved.contains(requestType.getRequestName())? ConversationThreadsStatus.APPROVED : ConversationThreadsStatus.L2_PENDING;
                requiredCurrentStatus =ConversationThreadsStatus.L1_PENDING;
            } else if (requestedStatus.equals(ConversationThreadsStatus.L1_REJECTED)) {
                targetStatus = ConversationThreadsStatus.L1_REJECTED;
            }else {
                throw new CommonException(String.format("L1 approver cannot perform action '%s'.Allowed: %S, %S ",
                        requestedStatus,ConversationThreadsStatus.L2_PENDING,ConversationThreadsStatus.L1_REJECTED));
            }
        }else if (role.equals(RequisitionApproversEntity.ApproverRole.L2)) {
            if(requestedStatus.equals(ConversationThreadsStatus.APPROVED)){
                targetStatus = ConversationThreadsStatus.APPROVED;
                requiredCurrentStatus = ConversationThreadsStatus.L2_PENDING;
            } else if (requestedStatus.equals(ConversationThreadsStatus.L2_REJECTED)) {
                targetStatus = ConversationThreadsStatus.L2_REJECTED;
            }else {
                throw new CommonException(String.format("L1 approver cannot perform action '%s'.Allowed: %S, %S ",
                        requestedStatus,ConversationThreadsStatus.APPROVED,ConversationThreadsStatus.L2_REJECTED));
            }
        }

        for(ConversationThreadsEntity entity:conversationThreadsEntities ){
            if(requiredCurrentStatus != null && entity.getStatus() != requiredCurrentStatus){
                throw new CommonException(String.format("Cannot process conversation thread. All message must be in '%s' state. Found a message in '%s' state.",
                        requiredCurrentStatus, entity.getStatus()));
            }
        }

        List<ConversationThreadsEntity> savedEntities = new ArrayList<>();
        for(ConversationThreadsEntity entity : conversationThreadsEntities ){
            entity.setStatus(targetStatus);
            WorkflowApprovalEntity workflowApproval = createWorkflowEntityMessages(entity,comments);
            workflowApprovalEntities.add(workflowApproval);
            savedEntities.add(entity);
        }

        workflowApprovalEntityRepository.saveAll(workflowApprovalEntities);

        if(targetStatus.equals(ConversationThreadsStatus.APPROVED)){
            updateDataOnApproval(conversationThreadsEntities);
            mailSenderHelper.sendMailsToCandidateUponApprovalAndRejections(conversationThreadsEntities,true);
        }

        if(targetStatus.equals(ConversationThreadsStatus.L2_PENDING)){
            List<UUID> applicationIds = savedEntities.stream().map(ConversationThreadsEntity::getApplicationId).toList();
            mailSenderHelper.sendExtensionApprovalMail(applicationIds, RequisitionApproversEntity.ApproverRole.L2);
        }

        if(targetStatus.equals(ConversationThreadsStatus.L1_REJECTED) || targetStatus.equals(ConversationThreadsStatus.L2_REJECTED)){
            mailSenderHelper.sendMailsToCandidateUponApprovalAndRejections(conversationThreadsEntities,false);
        }

        return conversationThreadsMapper.toDTOList(conversationThreadsRepository.saveAll(savedEntities));
    }

    public void updateDataOnApproval(List<ConversationThreadsEntity> conversationThreadsEntities){
        List<ConversationThreadsEntity>  discrepencyList = new ArrayList<>();
        List<ConversationThreadsEntity>  offerLetterList = new ArrayList<>();
        List<ConversationThreadsEntity>  zonalOfficeLocationChangeRequest = new ArrayList<>();
        List<ConversationThreadsEntity>  otherTypes = new ArrayList<>();
        List<RequestTypesEntity> requestTypes = requestTypesRepository.findAll();
        Map<UUID, String> requestMap = requestTypes.stream()
                .collect(Collectors.toMap(
                        RequestTypesEntity::getId,
                        RequestTypesEntity::getRequestName
                ));


        conversationThreadsEntities.forEach(item ->{

            String requestTypeName = requestMap.get(item.getRequestTypeId());

            if (requestTypeName == null) {
                throw new ResourceNotFoundException("Request type not found for ID: "+ item.getRequestTypeId());
            }

            if (DBConstants.DESCRANCY.equalsIgnoreCase(requestTypeName)) {
                discrepencyList.add(item);
            } else if (DBConstants.JoiningDateExtension.equalsIgnoreCase(requestTypeName)) {
                offerLetterList.add(item);
            } else if (DBConstants.ZONE_OFFICE_CHANGE_REQUEST.equalsIgnoreCase(requestTypeName)) {
                zonalOfficeLocationChangeRequest.add(item);
            } else {
                otherTypes.add(item);
            }
        });

        updateDiscrepancyData(discrepencyList);
        updateOfferLetterDateExtension(offerLetterList);
        updateZonalOfficeLocationChangeRequest(zonalOfficeLocationChangeRequest);
    }


    public void updateZonalOfficeLocationChangeRequest(List<ConversationThreadsEntity> zonalOfficeLocationChangeRequest){
        if (zonalOfficeLocationChangeRequest == null || zonalOfficeLocationChangeRequest.isEmpty()) return;
        List<String> errors=new ArrayList<>();
        List<UUID> applicationIds = zonalOfficeLocationChangeRequest.stream()
                .map(ConversationThreadsEntity::getApplicationId)
                .toList();
        List<CandidateApplicationsEntity> candidateApplicationsEntities = candidateApplicationsRepository.findAllById(applicationIds);
        Map<UUID,CandidateApplicationsEntity> candidateApplicationsEntityMap=candidateApplicationsEntities.stream().
                collect(Collectors.toMap(c->c.getId(),c->c));
        List<CandidateApplicationsEntity> notScheduled = new ArrayList<>();
        List<CandidateApplicationsEntity> schedulePending = new ArrayList<>();
        List<CandidateApplicationsEntity> scheduled = new ArrayList<>();

        candidateApplicationsEntities.forEach(item ->{
            if (item.getApplicationStatus().equals(CandidateApplicationStatus.SCHEDULE_PENDING)) {
                schedulePending.add(item);
            } else if (item.getApplicationStatus().equals(CandidateApplicationStatus.SCHEDULED)
                    || item.getApplicationStatus().equals(CandidateApplicationStatus.RESCHEDULED)) {
                scheduled.add(item);
            }else{
                notScheduled.add(item);
            }
        });

        updateNotScheduled(notScheduled,zonalOfficeLocationChangeRequest);
        Map<UUID,Boolean> finalPendingSchedulings=updateSchedulePending(schedulePending,zonalOfficeLocationChangeRequest);
        Map<UUID,Boolean> finalSchedulings=updateScheduled(scheduled,zonalOfficeLocationChangeRequest);
        int count=0;
        for(Map.Entry<UUID,Boolean> entry:finalPendingSchedulings.entrySet()){
            boolean isScheduled=entry.getValue();
            if(!isScheduled){
                errors.add("Couldn't schedule the interview for Application:"+candidateApplicationsEntityMap.get(entry.getKey()).getApplicationNo());
            }
            else{
                count++;
            }
        }
        for(Map.Entry<UUID,Boolean> entry:finalSchedulings.entrySet()){
            boolean isScheduled=entry.getValue();
            if(!isScheduled){
                errors.add("Couldn't schedule the interview for Application"+candidateApplicationsEntityMap.get(entry.getKey()).getApplicationNo());
            }
            else{
                count++;
            }
        }

        String message=String.format("Zonal office change request processed for %d applications. %d applications couldn't be scheduled due to conflicts.",
                finalPendingSchedulings.size()+finalSchedulings.size(),errors.size());
        if(!errors.isEmpty()) throw new SchedulingConflictException(message,errors);
    }
    private void updateNotScheduled(List<CandidateApplicationsEntity> notScheduled, List<ConversationThreadsEntity> zonalOfficeLocationChangeRequest){
        List<CandidateLocationPreferenceEntity> result = getCandidateLocationPreferences(notScheduled);
        Map<UUID, ConversationThreadsEntity> conversationThreadsEntityMap =
                zonalOfficeLocationChangeRequest.stream()
                        .collect(Collectors.toMap(
                                ConversationThreadsEntity::getApplicationId,
                                Function.identity()
                        ));
        Map<String, CandidateLocationPreferenceEntity> candidateLocationPreferenceEntityMap =
                result.stream()
                        .collect(Collectors.toMap(
                                item -> item.getCandidateId() + "_" + item.getPositionId(),
                                Function.identity(),
                                (existing, replacement) -> existing
                        ));

        List<CandidateLocationPreferenceEntity> entitiesToUpdate = new ArrayList<>();

        for (CandidateApplicationsEntity application : notScheduled) {

            String key = application.getCandidateId() + "_" + application.getPositionId();

            CandidateLocationPreferenceEntity locationPreference = candidateLocationPreferenceEntityMap.get(key);

            if (locationPreference != null) {

                ConversationThreadsEntity thread =
                        conversationThreadsEntityMap.get(application.getId());

                if (thread != null && thread.getZonalId() != null) {

                    locationPreference.setInterviewCenter(
                            thread.getZonalId()
                    );

                    entitiesToUpdate.add(locationPreference);
                }
            }
        }

        candidateLocationPreferencesRepository.saveAll(entitiesToUpdate);
    }


    private Map<UUID,Boolean> updateSchedulePending(List<CandidateApplicationsEntity> schedulePending, List<ConversationThreadsEntity> zonalOfficeLocationChangeRequest){
        //Get appids,posIds
        List<UUID> appIds= schedulePending.stream().map(CandidateApplicationsEntity::getId).toList();
        List<UUID> posIds=schedulePending.stream().map(CandidateApplicationsEntity::getPositionId).toList();
        List<UUID> candidateIds = schedulePending.stream().map(CandidateApplicationsEntity::getCandidateId).toList();
        //Curr Zone map(cand_pos,zone)
        Map<String,UUID> candSelectedZonalMap=candidateLocationPreferencesRepository.findByCandidateAndPosition(candidateIds,posIds).stream()
                .collect(Collectors.toMap(c->c.getCandidateId()+"_"+c.getPositionId()
                        ,c->c.getInterviewCenter()));
        //Changed zone map(appId,zone)
        Map<UUID,UUID> changedZone = zonalOfficeLocationChangeRequest.stream().collect(Collectors.toMap(ConversationThreadsEntity::getApplicationId,ConversationThreadsEntity::getZonalId));
        //Final change(can selected zone, current zone)
        Map<UUID,Boolean> applicationSchedulingResultMap=new HashMap<>();
        //Map appId to staging
        Map<UUID,InterviewScheduleStagingEntity> stageEntityMap=interviewScheduleStagingRepository.findByApplicationIdIn(appIds).stream()
                .collect(Collectors.toMap(is->is.getApplication().getId(),is->is));
        List<UUID> scheduledAppIds=new ArrayList<>();
        for(CandidateApplicationsEntity candidateApplications: schedulePending){
            Map<UUID,UUID> zoneChangeMap=buildZonalChangeMap(List.of(candidateApplications),changedZone,candSelectedZonalMap);
            InterviewSchedulingRequestModel schedulingRequestModel = buildSchedulingRequestModel(List.of(candidateApplications.getId()), List.of(candidateApplications.getPositionId()));
            schedulingRequestModel.setZonalChangeMap(zoneChangeMap);
            try {
                List<InterviewAllocatedRequestModel> allocated = schedulingService.allocateInterview(schedulingRequestModel);
                if (allocated == null || allocated.isEmpty() || allocated.size()!=1) {
                    applicationSchedulingResultMap.put(candidateApplications.getId(), false);
                    log.error("Scheduling allocation failed for application : {}", candidateApplications.getApplicationNo());
                    continue;
                }
                applicationSchedulingResultMap.put(candidateApplications.getId(),true);
                scheduledAppIds.add(candidateApplications.getId());
                updateInterviewScheduleStaging(allocated.get(0),stageEntityMap);
            } catch (SchedulingConflictException ex) {
                applicationSchedulingResultMap.put(candidateApplications.getId(),false);
            }
        }
        updatePreference(scheduledAppIds,schedulePending,zonalOfficeLocationChangeRequest);
        return applicationSchedulingResultMap;
    }

    private void updatePreference(List<UUID> scheduledAppIds, List<CandidateApplicationsEntity> candidateApplicationsEntities, List<ConversationThreadsEntity> zonalOfficeLocationChangeRequest) {
        List<ConversationThreadsEntity> scheduleThreads= zonalOfficeLocationChangeRequest.stream().filter(c->scheduledAppIds.contains(c.getApplicationId())).toList();
        List<CandidateApplicationsEntity> scheduledApps=candidateApplicationsEntities.stream().filter(c->scheduledAppIds.contains(c.getId())).toList();
        updateNotScheduled(scheduledApps,scheduleThreads);
    }

    private void updateInterviewScheduleStaging(InterviewAllocatedRequestModel allocated,Map<UUID,InterviewScheduleStagingEntity> stagingMap) {
        InterviewScheduleStagingEntity currentStaging=stagingMap.get(allocated.getApplication().getId());
        if(currentStaging!=null){
            currentStaging.setZonalOfficeId(allocated.getInterviewScheduleStaging().getZonalOfficeId());
            currentStaging.setInterviewStartAt(allocated.getInterviewScheduleStaging().getInterviewStartAt());
            currentStaging.setInterviewEndAt(allocated.getInterviewScheduleStaging().getInterviewEndAt());
            currentStaging.setPanelId(allocated.getInterviewPanels().getId());
            currentStaging.setInterviewDurationMinutes(allocated.getInterviewScheduleStaging().getInterviewDurationMinutes());
            interviewScheduleStagingRepository.save(currentStaging);
        }
    }

    private Map<UUID,Boolean> updateScheduled(List<CandidateApplicationsEntity> scheduled, List<ConversationThreadsEntity> zonalOfficeLocationChangeRequest){
        List<UUID> appIds= scheduled.stream().map(CandidateApplicationsEntity::getId).toList();
        List<UUID> posIds=scheduled.stream().map(CandidateApplicationsEntity::getPositionId).toList();
        List<UUID> candidateIds = scheduled.stream().map(CandidateApplicationsEntity::getCandidateId).toList();
        Map<String,UUID> candSelectedZonalMap=candidateLocationPreferencesRepository.findByCandidateAndPosition(candidateIds,posIds).stream()
                .collect(Collectors.toMap(c->c.getCandidateId()+"_"+c.getPositionId()
                        ,c->c.getInterviewCenter()));
        Map<UUID,UUID> currentZonalMap = zonalOfficeLocationChangeRequest.stream().collect(Collectors.toMap(ConversationThreadsEntity::getApplicationId,ConversationThreadsEntity::getZonalId));
        Map<UUID,Boolean> applicationSchedulingResultMap=new HashMap<>();
        Map<UUID,InterviewScheduleEntity> stageEntityMap=interviewScheduleRepository.findByApplicationIdIn(appIds).stream()
                .collect(Collectors.toMap(is->is.getApplicationId(),is->is));
        List<UUID> scheduledAppIds=new ArrayList<>();
        for(CandidateApplicationsEntity candidateApplications: scheduled){
            Map<UUID,UUID> zoneChangeMap=buildZonalChangeMap(List.of(candidateApplications),currentZonalMap,candSelectedZonalMap);
            InterviewSchedulingRequestModel schedulingRequestModel = buildSchedulingRequestModel(List.of(candidateApplications.getId()), List.of(candidateApplications.getPositionId()));
            schedulingRequestModel.setZonalChangeMap(zoneChangeMap);
            try {
                List<InterviewAllocatedRequestModel> allocated = schedulingService.allocateInterview(schedulingRequestModel);
                if (allocated == null || allocated.isEmpty() || allocated.size()!=1) {
                    applicationSchedulingResultMap.put(candidateApplications.getId(), false);
                    log.error("Scheduling allocation failed for application : {}", candidateApplications.getApplicationNo());
                    continue;
                }
                applicationSchedulingResultMap.put(candidateApplications.getId(),true);
                updateInterviewSchedule(allocated.get(0),stageEntityMap);
                scheduledAppIds.add(candidateApplications.getId());
            } catch (SchedulingConflictException ex) {
                applicationSchedulingResultMap.put(candidateApplications.getId(),false);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        updatePreference(scheduledAppIds,scheduled,zonalOfficeLocationChangeRequest);
        return applicationSchedulingResultMap;

    }

    private void updateInterviewSchedule(InterviewAllocatedRequestModel allocated,Map<UUID,InterviewScheduleEntity> scheduleEntityMap) throws MessagingException, IOException {
        InterviewScheduleEntity currentSchedule=scheduleEntityMap.get(allocated.getApplication().getId());
        CandidateApplicationsEntity candidateApplications=candidateApplicationsMapper.toEntity(allocated.getApplication());
        if(currentSchedule!=null){
            LocalDateTime startTime=allocated.getInterviewScheduleStaging().getInterviewStartAt(),endTime=allocated.getInterviewScheduleStaging().getInterviewEndAt();
            int durationInMinutes= (int)Duration.between(startTime,endTime).toMinutes();
            currentSchedule.setZonalOfficeId(allocated.getInterviewScheduleStaging().getZonalOfficeId());
            currentSchedule.setInterviewStartAt(startTime);
            currentSchedule.setInterviewEndAt(endTime);
            currentSchedule.setPanelId(allocated.getInterviewScheduleStaging().getPanelId());
            currentSchedule.setInterviewDurationMinutes(durationInMinutes);
            InterviewScheduleEntity savedSchedule=interviewScheduleRepository.save(currentSchedule);
            mailSenderHelper.sendMailToCandidateAndRecruiters(List.of(savedSchedule),List.of(candidateApplications));
        }

    }


    private Map<UUID, UUID> buildZonalChangeMap(List<CandidateApplicationsEntity> apps, Map<UUID, UUID> changedZoneMap, Map<String, UUID> candSelectedZonalMap) {
        Map<UUID,UUID> zonalChangeMap=new HashMap<>();
        for(CandidateApplicationsEntity app:apps){
            UUID curreZone= changedZoneMap.get(app.getId());
            UUID prevZone=candSelectedZonalMap.get(app.getCandidateId()+"_"+app.getPositionId());
            zonalChangeMap.put(prevZone,curreZone);
        }
        return zonalChangeMap;
    }

    private InterviewSchedulingRequestModel buildSchedulingRequestModel(List<UUID> appIds, List<UUID> posIds) {
        SchedulingPanelModel schedulingPanelModel = SchedulingPanelModel.builder()
                .applicationIds(appIds)
                .positionIds(posIds)
                .build();

        List<PositionPanelEntity> positionPanelEntities = positionPanelRepository.findByJobPositionIdIn(posIds);

        //For preventing duplicates
        Set<String> uniquePanelSlots = new HashSet<>();
        List<SchedulePanelExcelModel> panelExcelModels = new ArrayList<>();
        for (PositionPanelEntity positionPanel : positionPanelEntities) {
            LocalDate startDate = positionPanel.getStartDate();
            LocalDate endDate = positionPanel.getEndDate();
            if (startDate == null || endDate == null) {
                continue;
            }
            UUID panelId = positionPanel.getInterviewPanel().getId();
            //Generate the start and end time
            for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
                //Default time fixing
                LocalTime startTime = LocalTime.of(8, 0);
                LocalTime endTime = LocalTime.of(20, 0);
                // Create a unique key for the panel slot
                String uniqueKey = panelId + "|" + currentDate + "|" + startTime + "|" + endTime;
                //Prevent duplicates
                if (uniquePanelSlots.contains(uniqueKey)) {
                    continue;
                }
                uniquePanelSlots.add(uniqueKey);
                SchedulePanelExcelModel panelModel = SchedulePanelExcelModel.builder()
                        .panelId(panelId)
                        .panelDate(currentDate)
                        .startTime(startTime)
                        .endTime(endTime)
                        .durationInMinutes(15)
                        .interviewPerDay(48)
                        .build();

                panelExcelModels.add(panelModel);
            }
        }

        return InterviewSchedulingRequestModel.builder()
                .schedulingPanelModel(schedulingPanelModel)
                .panelScheduleModelList(panelExcelModels)
                .zonalChangeMap(null)
                .build();
    }

    public void updateOfferLetterDateExtension(List<ConversationThreadsEntity> offerLetterList) {

        if (offerLetterList == null || offerLetterList.isEmpty()) return;

        List<UUID> applicationIds = offerLetterList.stream()
                .map(ConversationThreadsEntity::getApplicationId)
                .toList();

        Map<UUID, ConversationThreadsEntity> conversationThreadMap =
                offerLetterList.stream()
                        .collect(Collectors.toMap(
                                ConversationThreadsEntity::getApplicationId,
                                Function.identity()
                        ));

        List<CandidateOffersEntity> candidateOffersEntityList =
                candidateOffersRepository.findAllByCandidateApplication_IdIn(applicationIds);

        List<CandidateOffersEntity> candidateOffersToUpdate = new ArrayList<>();

        candidateOffersEntityList.forEach(item -> {

            ConversationThreadsEntity thread =
                    conversationThreadMap.get(item.getCandidateApplication().getId());

            if (thread != null && thread.getDateExtension() != null) {

                item.setJoiningDate(thread.getDateExtension().toLocalDate());
                item.setAcceptBeforeDate(thread.getDateExtension().toLocalDate().minusDays(1));

                candidateOffersToUpdate.add(item);
            }
        });
        List<OfferMailModel> offerMailModels = offerLetterGenerationUtil.generateOfferLetterForCandidateOffers(candidateOffersToUpdate);
        candidateOffersRepository.saveAll(candidateOffersToUpdate);
        offerLetterGenerationUtil.sendOfferMails(offerMailModels);

    }

    public void updateDiscrepancyData(List<ConversationThreadsEntity>  discrepencyList){
        if(discrepencyList == null || discrepencyList.isEmpty()) return;
        List<UUID> applicationsIds = discrepencyList.stream().map(ConversationThreadsEntity::getApplicationId).toList();
        List<CandidateApplicationsEntity> candidateApplicationsEntities = candidateApplicationsRepository.findAllById(applicationsIds);

        Map<UUID,ConversationThreadsEntity> conversationThreadMap = discrepencyList.stream()
                .collect(Collectors.toMap(ConversationThreadsEntity::getApplicationId,Function.identity()));

        List<UUID> discrepancyList = candidateApplicationsEntities.stream()
                .filter(application -> application.getApplicationStatus()
                        .equals(CandidateApplicationStatus.DISCREPANCY))
                .map(CandidateApplicationsEntity::getId).toList();

        List<UUID> provisionallyApproved = candidateApplicationsEntities.stream()
                .filter(application -> application.getApplicationStatus()
                        .equals(CandidateApplicationStatus.PROVISIONALLY_APPROVED))
                .map(CandidateApplicationsEntity::getId).toList();

        List<CandidateScreeningEntity> candidateScreeningEntities = candidateScreeningRepository.findAllByApplicationIdIn(discrepancyList);
        List<InterviewScheduleEntity> interviewScheduleEntities = interviewScheduleRepository.findByApplicationIdIn(provisionallyApproved);

        List<CandidateScreeningEntity> candidateScreeningSaveEntity = new ArrayList<>();
        List<InterviewScheduleEntity> interviewScheduleSaveEntity = new ArrayList<>();
        candidateScreeningEntities.forEach(item -> {
            item.setSubmitBeforeDate(
                    conversationThreadMap
                            .get(item.getApplicationId())
                            .getDateExtension()
                            .toLocalDate()
            );
            candidateScreeningSaveEntity.add(item);
        });

        interviewScheduleEntities.forEach(item -> {
            item.setZonalSubmitBeforeDate(
                    conversationThreadMap
                            .get(item.getApplicationId())
                            .getDateExtension()
                            .toLocalDate()
            );
            interviewScheduleSaveEntity.add(item);
        });

        candidateScreeningRepository.saveAll(candidateScreeningSaveEntity);
        interviewScheduleRepository.saveAll(interviewScheduleSaveEntity);
    }

    public Page<MessageThreadResponseModel> getConversationHistoryBasedOnApproverRoles(MessageThreadRequestModel request, int page, int size) {
        UUID approverId = securityUtils.getCurrentUserId();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        RequisitionApproversEntity approver = requisitionApproversRepository.findByApproverId(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user is not configured as a requisition approver."));
        RequisitionApproversEntity.ApproverRole role = approver.getApproverRole();
        if (!(role.equals(RequisitionApproversEntity.ApproverRole.L1)
                || approver.getApproverRole().equals(RequisitionApproversEntity.ApproverRole.L2))) {
            throw new IllegalStateException("User has an unsupported approver role.");
        }

        List<CandidateApplicationsEntity> candidateApplicationsEntities =
                candidateApplicationsRepository.findByPositionIdIn(request.getPositionsIds());

        List<UUID> allApplicationIds = candidateApplicationsEntities.stream()
                .map(CandidateApplicationsEntity::getId)
                .distinct()
                .toList();


        // initially keep L1 status list
        List<ConversationThreadsStatus> statusList = List.of(ConversationThreadsStatus.L1_PENDING, ConversationThreadsStatus.L2_PENDING, ConversationThreadsStatus.L1_REJECTED, ConversationThreadsStatus.APPROVED, ConversationThreadsStatus.L2_REJECTED);

        // if role is L2 then validate the statusList accordingly
        if(role.equals(RequisitionApproversEntity.ApproverRole.L2)){
             statusList= List.of(ConversationThreadsStatus.L2_PENDING, ConversationThreadsStatus.APPROVED, ConversationThreadsStatus.L2_REJECTED);
        }

        if(request.getStatusList()!=null && !request.getStatusList().isEmpty()){
            validateStatusList(statusList,request.getStatusList());
            statusList = request.getStatusList();
        }



        Page<ConversationThreadsEntity> threadsPage = bulidFilterSpecification(allApplicationIds,request.getRequestTypeIds(),statusList,request.getSearchText(),pageable);

        Set<UUID> filteredApplicationIds = threadsPage.stream()
                .map(ConversationThreadsEntity::getApplicationId)
                .collect(Collectors.toSet());

        Map<UUID,CandidateApplicationsEntity> applicationMap =
                candidateApplicationsEntities.stream().
                        filter((currEntity)->filteredApplicationIds.contains(currEntity.getId()))
                        .collect(Collectors.toMap(
                                CandidateApplicationsEntity::getId,
                                Function.identity()
                        ));

        List<UUID> candidatesIds = applicationMap.values().stream().map(CandidateApplicationsEntity::getCandidateId).distinct()
                .toList();

        Map<UUID, CandidateProfileEntity> candidateProfileMap =
                candidateProfileRepository.findAllByCandidateIdIn(candidatesIds)
                        .stream()
                        .collect(Collectors.toMap(
                                CandidateProfileEntity::getCandidateId,
                                profile -> profile,
                                (existing, replacement) -> existing
                        ));



        return threadsPage.map(thread -> {

            CandidateApplicationsEntity application =
                    applicationMap.get(thread.getApplicationId());

            CandidateProfileEntity profile =
                    candidateProfileMap.get(application.getCandidateId());

            return    MessageThreadResponseModel.builder()
                    .candidateName(
                            commonUtilityProvider.buildFullName(profile)
                    )
                    .applicationNo(application.getApplicationNo())
                    .positionId(application.getPositionId())
                    .applicationId(thread.getApplicationId())
                    .requestTypeId(thread.getRequestTypeId())
                    .status(thread.getStatus())
                    .createdDate(thread.getCreatedDate())
                    .conversationThreadId(thread.getId())
                    .dateExtension(thread.getDateExtension())
                    .zonalId(thread.getZonalId())
                    .build();
        });


    }

    public WorkflowApprovalEntity createWorkflowEntityMessages(ConversationThreadsEntity savedEntity, String comments ){
        String approverRole = securityUtils.getCurrentUserRole();
        return WorkflowApprovalEntity.builder()
                .entityId(savedEntity.getId())
                .entityType(ConversationThreadsEntity.ENTITY_TYPE)
                .stepNumber(1) // Assuming step 1 for now
                .approverRole(approverRole)
                .approverId(securityUtils.getCurrentUserId())
                .action(DBConstants.WORKFLOW_ACTION_UPDATE)
                .actionDate(LocalDateTime.now())
                .comments(comments)
                .status(savedEntity.getStatus().toString())
                .build();
    }

    public Page<ConversationThreadsEntity> bulidFilterSpecification(
            List<UUID> applicationIds,
            List<UUID> requestTypeIds,
            List<ConversationThreadsStatus> statusList,
            String searchText,
            Pageable pageable
    ) {

        UUID currentUser = securityUtils.getCurrentUserId();

        // SAFE: no null exception
        RequisitionApproversEntity.ApproverRole role = null;

        Optional<RequisitionApproversEntity> approversEntityOpt =
                requisitionApproversRepository.findByApproverId(currentUser);

        if (approversEntityOpt.isPresent()) {
            role = approversEntityOpt.get().getApproverRole();
        }

        List<UUID> excludedRequestTypeIds = Collections.emptyList();

        if (RequisitionApproversEntity.ApproverRole.L2.equals(role)) {
            excludedRequestTypeIds = requestTypesRepository.findAll()
                    .stream()
                    .filter(requestType ->
                            AppConstants.MESSAGES_L2_APPROVED_REQUESTS
                                    .contains(requestType.getRequestName()))
                    .map(RequestTypesEntity::getId)
                    .toList();
        }

        List<UUID> finalExcludedRequestTypeIds = excludedRequestTypeIds;


        return conversationThreadsRepository.searchThreads(applicationIds, requestTypeIds.isEmpty()?null:requestTypeIds, statusList.isEmpty()?null:statusList, searchText, finalExcludedRequestTypeIds, pageable);
    }
    public void validateStatusList(List<ConversationThreadsStatus> expectedStatusList,List<ConversationThreadsStatus> actualStatusList) {
         for (ConversationThreadsStatus status : actualStatusList) {
             if (!expectedStatusList.contains(status)) {
                 throw new CommonException("Invalid Status List");
             }
         }
     }

    public List<CandidateLocationPreferenceEntity> getCandidateLocationPreferences(List<CandidateApplicationsEntity> requestList) {

        Specification<CandidateLocationPreferenceEntity> spec = null;

        for (CandidateApplicationsEntity dto : requestList) {

            Specification<CandidateLocationPreferenceEntity> current =
                    (root, query, cb) -> cb.and(
                            cb.equal(root.get("candidateId"), dto.getCandidateId()),
                            cb.equal(root.get("positionId"), dto.getPositionId())
                    );

            spec = (spec == null)
                    ? current
                    : spec.or(current);
        }

        return candidateLocationPreferencesRepository.findAll(spec);
    }
}
