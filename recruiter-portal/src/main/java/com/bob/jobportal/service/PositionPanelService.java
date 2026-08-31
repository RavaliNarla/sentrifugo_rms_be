package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.PositionPanelDTO;
import com.bob.db.dto.InterviewPanelsDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.ActionEnum;
import com.bob.db.enums.PositionPanelStatus;
import com.bob.db.enums.PositionStatus;
import com.bob.db.enums.RequisitionStatus;
import com.bob.db.mapper.InterviewPanelsMapper;
import com.bob.db.mapper.PositionPanelMapper;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import com.bob.jobportal.model.PositionPanelResponseModel;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.util.CellRangeAddressList;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class PositionPanelService {

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private PositionPanelMapper positionPanelMapper;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private InterviewCommitteeRepository interviewCommitteeRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private InterviewPanelsRepository interviewPanelsRepository;

    @Autowired
    private InterviewPanelMembersRepository interviewPanelMembersRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private RequisitionApproversRepository requisitionApproversRepository;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;


    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;
    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private DepartmentsRepository departmentsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private InterviewPanelsMapper interviewPanelsMapper;

    @Autowired
    private MailSenderHelper mailSenderHelper;

    // Helper record for panel member assignments, simplified to only essential fields for validation
    private record PanelMemberAssignment(UUID userId,UUID panelId,
                                         UUID positionId, LocalDate startDate, LocalDate endDate,boolean fromRequest) {}

    @Transactional
    public void approveCommittee(List<UUID> positionPanelIds, String comments) throws MessagingException, IOException {
        if (positionPanelIds == null || positionPanelIds.isEmpty()) {
            throw new IllegalArgumentException("Position Panel IDs cannot be null or empty.");
        }

        UUID currentUser = securityUtils.getCurrentUserId();
        RequisitionApproversEntity approversEntity = requisitionApproversRepository.findByApproverId(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Current user is not configured as a requisition approver."));

        List<PositionPanelEntity> entities = positionPanelRepository.findAllById(positionPanelIds);

        if (entities.isEmpty()) {
            throw new ResourceNotFoundException("No position panels found.");
        }

        final RequisitionApproversEntity.ApproverRole approverRole = approversEntity.getApproverRole();
        final PositionPanelStatus targetStatus;
        final PositionPanelStatus requiredCurrentStatus;

        if (approverRole.equals(RequisitionApproversEntity.ApproverRole.L2)) {
            targetStatus = PositionPanelStatus.APPROVED;
            requiredCurrentStatus = PositionPanelStatus.L2_PENDING;
        } else if (approverRole.equals(RequisitionApproversEntity.ApproverRole.L1)) {
            targetStatus = PositionPanelStatus.L2_PENDING;
            requiredCurrentStatus = PositionPanelStatus.L1_PENDING;
        } else {
            throw new IllegalStateException("User has an unsupported approver role: " + approverRole);
        }

        for (PositionPanelEntity entity : entities) {
            if (entity.getPositionPanelStatus() != requiredCurrentStatus) {
                throw new IllegalStateException(
                    String.format("Cannot approve panels. All panels must be in '%s' state. Found a panel in '%s' state.",
                    requiredCurrentStatus, entity.getPositionPanelStatus()));
            }
        }

        entities.forEach(entity -> {
            entity.setPositionPanelStatus(targetStatus);
            entity.setComments(comments);

        });


        positionPanelRepository.saveAll(entities);

        List<WorkflowApprovalEntity> workflowApprovalEntityList = new ArrayList<>();
        entities.forEach(ety->{
            workflowApprovalEntityList.add(createWorkflowEntityPositionPanel(ety));
        });

        workflowApprovalEntityRepository.saveAll(workflowApprovalEntityList);

        if(targetStatus.equals(PositionPanelStatus.L2_PENDING)){
            mailSenderHelper.sendPanelApprovalMail(entities, RequisitionApproversEntity.ApproverRole.L2);
        }

        //getting lazy loading exceptions while passing the entity List to async mail method.
        // approvals are also being done for single position
        //converting the panels to  dtos and sending positionId separately as parameter
        if(targetStatus.equals(PositionPanelStatus.APPROVED)) {
            JobPositionsEntity jobPosition = entities.stream().map((currPanel)->currPanel.getJobPosition())
                    .findFirst().orElseThrow(()->new CommonException("Invalid Request"));
            List<PositionPanelDTO> positionPanelDTOs = positionPanelMapper.toDtoListWithoutChildren(entities);
            mailSenderHelper.sendPositionPanelAssignmentEmail(positionPanelDTOs,jobPosition);
        }

    }

    @Transactional
    public void rejectCommittee(List<UUID> positionPanelIds, String comments) {
        if (positionPanelIds == null || positionPanelIds.isEmpty()) {
            throw new IllegalArgumentException("Position Panel IDs cannot be null or empty.");
        }

        UUID currentUser = securityUtils.getCurrentUserId();
        RequisitionApproversEntity approversEntity = requisitionApproversRepository.findByApproverId(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Current user is not configured as a requisition approver."));

        List<PositionPanelEntity> entities = positionPanelRepository.findAllById(positionPanelIds);

        if (entities.isEmpty()) {
            throw new ResourceNotFoundException("No position panels found.");
        }

        final RequisitionApproversEntity.ApproverRole approverRole = approversEntity.getApproverRole();
        final PositionPanelStatus targetStatus;
        final PositionPanelStatus requiredCurrentStatus;

        if (approverRole.equals(RequisitionApproversEntity.ApproverRole.L2)) {
            targetStatus = PositionPanelStatus.L2_REJECTED;
            requiredCurrentStatus = PositionPanelStatus.L2_PENDING;
        } else if (approverRole.equals(RequisitionApproversEntity.ApproverRole.L1)) {
            targetStatus = PositionPanelStatus.L1_REJECTED;
            requiredCurrentStatus = PositionPanelStatus.L1_PENDING;
        } else {
            throw new IllegalStateException("User has an unsupported approver role: " + approverRole);
        }

        for (PositionPanelEntity entity : entities) {
            if (entity.getPositionPanelStatus() != requiredCurrentStatus) {
                throw new IllegalStateException(
                    String.format("Cannot reject panels. All panels must be in '%s' state. Found a panel in '%s' state.",
                    requiredCurrentStatus, entity.getPositionPanelStatus()));
            }
        }

        entities.forEach(entity -> {
            entity.setPositionPanelStatus(targetStatus);
            entity.setComments(comments);
        });

        positionPanelRepository.saveAll(entities);

        List<WorkflowApprovalEntity> workflowApprovalEntityList = new ArrayList<>();
        entities.forEach(ety -> workflowApprovalEntityList.add(createWorkflowEntityPositionPanel(ety)));

        workflowApprovalEntityRepository.saveAll(workflowApprovalEntityList);
    }

    @Transactional
    public boolean saveOrUpdatePositionPanel(PositionPanelResponseModel positionPanelResponseModel, UUID jobPositionId) throws MessagingException, IOException {
        JobPositionsEntity jobPosition = positionsRepository.findById(jobPositionId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Position not found with ID: " + jobPositionId));

        List<String> errors=new ArrayList<>();
        errors.addAll(validateForDuplicateInterviewPanelIds(positionPanelResponseModel));
        errors.addAll(validatePositionPanelDates(positionPanelResponseModel));
//        errors.addAll(validatePanelMemberAvailability(positionPanelResponseModel, jobPositionId));

        if(!errors.isEmpty()){
            throw new ExcelValidationException(errors);
        }

        List<PositionPanelEntity> existingPanels = positionPanelRepository.findByJobPositionId(jobPositionId);
        Map<UUID, PositionPanelEntity> existingPanelsMap = existingPanels.stream()
                .collect(Collectors.toMap(PositionPanelEntity::getId, panel -> panel));

        List<PositionPanelEntity> entitiesToPersist = new ArrayList<>();
        List<PositionPanelEntity> entitiesToDelete = new ArrayList<>();
        Set<UUID> incomingPanelIds = new HashSet<>();

        Stream.of(
                positionPanelResponseModel.getInterviewPanelList(),
                positionPanelResponseModel.getScreeningPanelList(),
                positionPanelResponseModel.getCompensationPanelList()
        )
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .forEach(dto -> {
            PositionPanelEntity entity;
            if (dto.getId() != null && existingPanelsMap.containsKey(dto.getId())) {
                // Update existing entity
                entity = existingPanelsMap.get(dto.getId());
                
                // Handle based on actionEnum
                if (dto.getActionEnum() != null) {
                    switch (dto.getActionEnum()) {
                        case MODIFY:
                            // Update start and end dates
                            entity.setStartDate(dto.getStartDate());
                            entity.setEndDate(dto.getEndDate());
                            entity.setJobPosition(jobPosition);
                            entity.setPositionPanelStatus(PositionPanelStatus.L1_PENDING);
                            entity.setComments("");
                            entitiesToPersist.add(entity);
                            break;
                        case DELETE:
                            // Add to entities to delete list
                            entitiesToDelete.add(entity);
                            break;
                        case ADD:
                            // ADD action is not valid for existing entities
                            throw new ManualValidationException("ADD action is not valid for existing entity with ID: " + dto.getId());
                    }
                }
                incomingPanelIds.add(dto.getId());
            } else {
                // Handle new entity based on actionEnum
                if (dto.getActionEnum() == null || dto.getActionEnum() == ActionEnum.ADD) {
                    // Create new entity
                    entity = positionPanelMapper.toEntity(dto);
                    entity.setInterviewPanel(
                            entityManager.getReference(
                                    InterviewPanelsEntity.class,
                                    dto.getInterviewPanel().getId()
                            )
                    );
                    entity.setJobPosition(jobPosition);
                    entity.setPositionPanelStatus(PositionPanelStatus.L1_PENDING);
                    entity.setComments("");
                    entitiesToPersist.add(entity);
                }
                // For DELETE or MODIFY on non-existing entities, do nothing
            }
        });

        // Identify panels to delete (those in existingPanels but not in incomingPanelIds)
        entitiesToDelete.addAll(existingPanels.stream()
                .filter(panel -> !incomingPanelIds.contains(panel.getId()))
                .collect(Collectors.toList()));

        positionPanelRepository.deleteAll(entitiesToDelete);
        positionPanelRepository.saveAll(entitiesToPersist);

        List<WorkflowApprovalEntity> workflowApprovalEntityList = new ArrayList<>();
        entitiesToPersist.forEach(ety->{
            workflowApprovalEntityList.add(createWorkflowEntityPositionPanel(ety));
        });

        workflowApprovalEntityRepository.saveAll(workflowApprovalEntityList);
        mailSenderHelper.sendPanelApprovalMail(entitiesToPersist, RequisitionApproversEntity.ApproverRole.L1);
        return true;
    }

    private List<String> validateForDuplicateInterviewPanelIds(PositionPanelResponseModel positionPanelResponseModel) {
        Set<UUID> seenInterviewPanelIds = new HashSet<>();
        List<UUID> duplicateInterviewPanelIds = new ArrayList<>();
        List<String> errors=new ArrayList<>();
        Stream.of(
                positionPanelResponseModel.getInterviewPanelList(),
                positionPanelResponseModel.getScreeningPanelList(),
                positionPanelResponseModel.getCompensationPanelList()
        )
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .map(PositionPanelDTO::getInterviewPanel)
        .filter(Objects::nonNull)
        .map(InterviewPanelsDTO::getId)
        .filter(Objects::nonNull)
        .forEach(panelId -> {
            if (!seenInterviewPanelIds.add(panelId)) {
                duplicateInterviewPanelIds.add(panelId);
            }
        });

        Map<UUID,InterviewPanelsEntity> interviewPanelsEntityMap=interviewPanelsRepository.findAllById(duplicateInterviewPanelIds)
                .stream()
                .collect(Collectors.toMap(
                        InterviewPanelsEntity::getId,
                        e -> e
                ));

        if (!duplicateInterviewPanelIds.isEmpty()) {
            String duplicatePanelNames = duplicateInterviewPanelIds.stream()
                    .map(id -> {
                        InterviewPanelsEntity entity = interviewPanelsEntityMap.get(id);
                        return entity != null ? entity.getPanelName() : "";
                    })
                    .collect(Collectors.joining(", "));
            errors.add("Duplicate Interview Panel found in the request: " + duplicatePanelNames);
        }
        return errors;
    }

    private List<String> validatePositionPanelDates(PositionPanelResponseModel positionPanelResponseModel) {
        LocalDate today = LocalDate.now();
        List<String> errors=new ArrayList<>();
        List<PositionPanelDTO> panelDTOS=Stream.of(
                        positionPanelResponseModel.getInterviewPanelList(),
                        positionPanelResponseModel.getScreeningPanelList(),
                        positionPanelResponseModel.getCompensationPanelList()
                )
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull).toList();

        if (panelDTOS.isEmpty()) {
            return errors;
        }

        // Collect panel IDs
        List<UUID> panelIds = panelDTOS.stream()
                .map(PositionPanelDTO::getInterviewPanel)
                .filter(Objects::nonNull)
                .map(InterviewPanelsDTO::getId)
                .filter(Objects::nonNull)
                .toList();

        // Fetch panels
        Map<UUID, InterviewPanelsEntity> panelMap =
                interviewPanelsRepository.findAllById(panelIds)
                        .stream()
                        .collect(Collectors.toMap(
                                InterviewPanelsEntity::getId,
                                e -> e
                        ));


        panelDTOS.
                stream()
            .forEach(dto -> {
                UUID panelId = dto.getInterviewPanel() != null
                        ? dto.getInterviewPanel().getId()
                        : null;

                InterviewPanelsEntity panelEntity = panelMap.get(panelId);

                String panelName = panelEntity != null
                        ? panelEntity.getPanelName()
                        : "Unknown Panel";

                String committeeName = (panelEntity != null && panelEntity.getCommittee() != null)
                        ? panelEntity.getCommittee().getCommitteeName()
                        : AppConstants.UNKNOWN_COMMITTEE;

                String fullPanelName = committeeName + " panel \"" + panelName + "\"";
            LocalDate startDate = dto.getStartDate();
            LocalDate endDate = dto.getEndDate();


            if (startDate == null) {
                errors.add("Start date is required for " + fullPanelName + ".");
            }
            if (endDate == null) {
                errors.add("End date is required for " + fullPanelName + ".");
            }
            if(dto.getId()==null) {
                if (startDate.isBefore(today)) {
                    errors.add(
                            String.format(
                                    "Start date (%s) cannot be in the past for %s.",
                                    startDate,
                                    fullPanelName
                            )
                    );                }
            }
            if (endDate.isBefore(startDate)) {
                errors.add(
                        String.format(
                                "End date (%s) must be after start date (%s) for %s.",
                                endDate,
                                startDate,
                                fullPanelName
                        )
                );
            }
        });
        return errors;
    }
    private List<String> validatePanelMemberAvailability(PositionPanelResponseModel positionPanelResponseModel, UUID currentJobPositionId) {
        List<PanelMemberAssignment> allAssignments = new ArrayList<>();
        Set<UUID> userIdsInCurrentRequest = new HashSet<>();

        List<String> errors=new ArrayList<>();


        // 1. Collect incoming assignments from the current request by fetching InterviewPanelsEntity
        Stream.of(
                positionPanelResponseModel.getInterviewPanelList(),
                positionPanelResponseModel.getScreeningPanelList(),
                positionPanelResponseModel.getCompensationPanelList()
        )
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .filter(Objects::nonNull)
        .forEach(positionPanelDTO -> {
            LocalDate panelStartDate = positionPanelDTO.getStartDate();
            LocalDate panelEndDate = positionPanelDTO.getEndDate();

            if (positionPanelDTO.getInterviewPanel() != null && positionPanelDTO.getInterviewPanel().getId() != null) {
                UUID interviewPanelId = positionPanelDTO.getInterviewPanel().getId();
                interviewPanelsRepository.findById(interviewPanelId).ifPresent(interviewPanelsEntity -> {
                    if (interviewPanelsEntity.getPanelMembers() != null) {
                        interviewPanelsEntity.getPanelMembers().stream()
                            .filter(Objects::nonNull)
                            .filter(memberEntity -> memberEntity.getPanelMember() != null && memberEntity.getPanelMember().getId() != null)
                            .forEach(memberEntity -> {
                                UUID userId = memberEntity.getPanelMember().getId();
                                allAssignments.add(new PanelMemberAssignment(
                                    userId,
                                    positionPanelDTO.getInterviewPanel().getId(),
                                    currentJobPositionId,
                                    panelStartDate,
                                    panelEndDate, true
                                ));
                                userIdsInCurrentRequest.add(userId);
                            });
                    }
                });
            } else {
                // If the InterviewPanel ID is missing, it means we cannot fetch its members from the DB.
                // This indicates an invalid DTO for this validation logic.
                throw new ManualValidationException("Interview Panel ID is missing for a position panel. Cannot validate members.");
            }
        });
//        log.info(" Panel Assigned:{}", allAssignments);

        if (!userIdsInCurrentRequest.isEmpty()) {
            List<PositionPanelEntity> existingPanelsForOtherPositions = positionPanelRepository
                .findByJobPositionIdNotAndInterviewPanel_PanelMembers_PanelMember_IdIn(currentJobPositionId, userIdsInCurrentRequest);

            existingPanelsForOtherPositions.forEach(entity -> {
                LocalDate panelStartDate = entity.getStartDate();
                LocalDate panelEndDate = entity.getEndDate();

                if (entity.getInterviewPanel() != null && entity.getInterviewPanel().getPanelMembers() != null) {
                    entity.getInterviewPanel().getPanelMembers().stream()
                        .filter(Objects::nonNull)
                        .filter(memberEntity -> memberEntity.getPanelMember() != null && userIdsInCurrentRequest.contains(memberEntity.getPanelMember().getId()))
                        .forEach(memberEntity -> {
                            allAssignments.add(new PanelMemberAssignment(
                                memberEntity.getPanelMember().getId(),
                                memberEntity.getPanel().getId(),
                                entity.getJobPosition().getId(),
                                panelStartDate,
                                panelEndDate,
                                    false
                            ));
                        });
                }
            });
        }
//        log.info(" Panel Assigned:{}", allAssignments);
        Map<UUID, UserEntity> userEntityMap =
                userRepository.findAllById(userIdsInCurrentRequest)
                        .stream()
                        .collect(Collectors.toMap(
                                UserEntity::getId,
                                u -> u,
                                (existing, replacement) -> existing   // keep first
                        ));
        Set<UUID> positionIds = allAssignments.stream()
                .map(PanelMemberAssignment::positionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, JobPositionsEntity> jobPositionsMap =
                positionsRepository.findAllById(positionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                JobPositionsEntity::getId,
                                e -> e
                        ));
        Set<UUID> requisitionIds=jobPositionsMap.values().stream()
                .map(JobPositionsEntity::getRequisitionId)
                .collect(Collectors.toSet());

        Map<UUID,JobRequisitionsEntity> requisitionsEntityMap=
                jobRequisitionsRepository.findAllById(requisitionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                JobRequisitionsEntity::getId,
                                e->e
                        ));
        Set<UUID> masterPositionIds = jobPositionsMap.values().stream()
                .map(JobPositionsEntity::getMasterPositionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, MasterPositionsEntity> masterPositionsEntityMap =
                masterPositionsRepository.findAllById(masterPositionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                MasterPositionsEntity::getId,
                                e -> e
                        ));


        Set<UUID> panelIds = allAssignments.stream()
                .map(PanelMemberAssignment::panelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, InterviewPanelsEntity> panelMap =
                interviewPanelsRepository.findAllById(panelIds)
                        .stream()
                        .collect(Collectors.toMap(
                                InterviewPanelsEntity::getId,
                                i->i
                        ));

        // 3. Perform overlap check on all collected assignments
        for (int i = 0; i < allAssignments.size(); i++) {
            PanelMemberAssignment assignment1 = allAssignments.get(i);
            String userName=userEntityMap.get(assignment1.userId()).getName();
            for (int j = i + 1; j < allAssignments.size(); j++) {
                PanelMemberAssignment assignment2 = allAssignments.get(j);
                // Check for overlap only if it's the same user
                if (assignment1.userId().equals(assignment2.userId())) {
                    if (datesOverlap(assignment1.startDate(), assignment1.endDate(), assignment2.startDate(), assignment2.endDate())
                            && checkUserIsFromRequest(assignment1,assignment2)) {
                        String positionName1 = masterPositionsEntityMap.get(jobPositionsMap.get(assignment1.positionId())
                                .getMasterPositionId()).getPositionName();

                        String positionName2 = masterPositionsEntityMap.get(jobPositionsMap.get(assignment2.positionId())
                                .getMasterPositionId()).getPositionName();

                        String requisitionDisplay1=requisitionsEntityMap.get(jobPositionsMap.get(assignment1.positionId()).getRequisitionId()).getRequisitionTitle()
                                +"("+requisitionsEntityMap.get(jobPositionsMap.get(assignment1.positionId()).getRequisitionId()).getRequisitionCode()+")";

                        String requisitionDisplay2=requisitionsEntityMap.get(jobPositionsMap.get(assignment2.positionId()).getRequisitionId()).getRequisitionTitle()
                                +"("+requisitionsEntityMap.get(jobPositionsMap.get(assignment2.positionId()).getRequisitionId()).getRequisitionCode()+")";
                        String panelName1 = panelMap.get(assignment1.panelId()).getPanelName();

                        String panelName2 = panelMap.get(assignment2.panelId()).getPanelName();

                        String committeeName1=panelMap.get(assignment1.panelId()).getCommittee()!=null?
                                panelMap.get(assignment1.panelId()).getCommittee().getCommitteeName():AppConstants.UNKNOWN_COMMITTEE;

                        String committeeName2=panelMap.get(assignment2.panelId()).getCommittee()!=null?
                                panelMap.get(assignment2.panelId()).getCommittee().getCommitteeName():AppConstants.UNKNOWN_COMMITTEE;
                        errors.add(
                                String.format(
                                        "User %s is assigned to overlapping panels.\n\n" +
                                                "Panel 1:\n" +
                                                "• Position: %s\n" +
                                                "• Panel: %s (%s Committee)\n" +
                                                "• Requisition: %s\n" +
                                                "• Dates: %s to %s\n\n" +
                                                "Panel 2:\n" +
                                                "• Position: %s\n" +
                                                "• Panel: %s (%s Committee)\n" +
                                                "• Requisition: %s\n" +
                                                "• Dates: %s to %s\n\n",
                                        userName,
                                        positionName1,
                                        panelName1,
                                        committeeName1,
                                        requisitionDisplay1,
                                        assignment1.startDate(),
                                        assignment1.endDate(),
                                        positionName2,
                                        panelName2,
                                        committeeName2,
                                        requisitionDisplay2,
                                        assignment2.startDate(),
                                        assignment2.endDate()
                                )
                        );
                    }
                }
            }
        }
        return errors;
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    private boolean checkUserIsFromRequest(PanelMemberAssignment p1,PanelMemberAssignment p2){
        return p1.fromRequest() || p2.fromRequest();
    }

    private PositionPanelEntity convertDtoToEntity(PositionPanelDTO positionPanelDTO, JobPositionsEntity jobPosition) {
        if (positionPanelDTO == null) {
            return null;
        }
        PositionPanelEntity positionPanelEntity = positionPanelMapper.toEntity(positionPanelDTO);
        positionPanelEntity.setJobPosition(jobPosition);
        return positionPanelEntity;
    }

    @Transactional(readOnly = true)
    public PositionPanelResponseModel getGroupedPositionPanelsByPositionId(UUID positionId) {
        List<PositionPanelEntity> positionPanelEntities = positionPanelRepository.findByJobPositionId(positionId);

        if (positionPanelEntities.isEmpty()) {
            return new PositionPanelResponseModel();
        }

        Set<UUID> allUserIds = positionPanelEntities.stream()
                .map(PositionPanelEntity::getInterviewPanel)
                .filter(Objects::nonNull)
                .flatMap(panel -> panel.getPanelMembers().stream())
                .map(InterviewPanelMembersEntity::getPanelMember)
                .filter(Objects::nonNull)
                .map(UserEntity::getId)
                .collect(Collectors.toSet());

        Map<UUID, UserEntity> userMap = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));

        Set<UUID> allCommitteeIds = positionPanelEntities.stream()
                .map(PositionPanelEntity::getInterviewPanel)
                .filter(Objects::nonNull)
                .map(InterviewPanelsEntity::getCommittee)
                .filter(Objects::nonNull)
                .map(InterviewCommitteeEntity::getId)
                .collect(Collectors.toSet());

        Map<UUID, InterviewCommitteeEntity> committeeMap = interviewCommitteeRepository.findAllById(allCommitteeIds).stream()
                .collect(Collectors.toMap(InterviewCommitteeEntity::getId, committee -> committee));

        List<PositionPanelDTO> allPositionPanelDTOs = positionPanelEntities.stream()
                .map(entity -> positionPanelMapper.toDto(entity, userMap, committeeMap))
                .collect(Collectors.toList());

        PositionPanelResponseModel responseModel = new PositionPanelResponseModel();
        responseModel.setInterviewPanelList(new ArrayList<>());
        responseModel.setScreeningPanelList(new ArrayList<>());
        responseModel.setCompensationPanelList(new ArrayList<>());

        List<CandidateApplicationsEntity> candidateApplicationsEntities=candidateApplicationsRepository.findByPositionIdIn(Set.of(positionId));
        List<UUID> applicationIds=candidateApplicationsEntities.stream().map(CandidateApplicationsEntity::getId).toList();
        List<InterviewScheduleEntity> interviewScheduleEntities=interviewScheduleRepository.findByApplicationIdIn(applicationIds);
        Set<UUID> scheduledPanelIds=interviewScheduleEntities.stream().map(InterviewScheduleEntity::getPanelId).collect(Collectors.toSet());
        log.info("Scheduled Panels:{}",scheduledPanelIds);
        for (PositionPanelDTO dto : allPositionPanelDTOs) {
            if (dto.getInterviewPanel() != null && dto.getInterviewPanel().getCommittee() != null) {
                String committeeName = dto.getInterviewPanel().getCommittee().getCommitteeName();
                if (committeeName != null) {
                    if (committeeName.equalsIgnoreCase(AppConstants.INTERVIEW_COMMITTEE_NAME)) {
                        boolean flag=scheduledPanelIds.contains(dto.getInterviewPanel().getId())?false:true;
                        if(dto.getEndDate().isBefore(LocalDate.now())){flag = true;}
                        dto.setCanEdit(flag);
                        responseModel.getInterviewPanelList().add(dto);
                    } else if (committeeName.equalsIgnoreCase(AppConstants.SCREENING_COMMITTEE_NAME)) {
                        responseModel.getScreeningPanelList().add(dto);
                    } else if (committeeName.equalsIgnoreCase(AppConstants.COMPENSATION_COMMITTEE_NAME)) {
                        responseModel.getCompensationPanelList().add(dto);
                    }
                }
            }
        }
        return responseModel;
    }

    public WorkflowApprovalEntity createWorkflowEntityPositionPanel(PositionPanelEntity savedEntity){
        String approverRole = securityUtils.getCurrentUserRole();
        return WorkflowApprovalEntity.builder()
                .entityId(savedEntity.getId())
                .entityType(PositionPanelEntity.ENTITY_TYPE)
                .stepNumber(1) // Assuming step 1 for now
                .approverRole(approverRole)
                .approverId(securityUtils.getCurrentUserId())
                .action(DBConstants.WORKFLOW_ACTION_UPDATE)
                .actionDate(LocalDateTime.now())
                .comments(savedEntity.getComments())
                .status(savedEntity.getPositionPanelStatus().toString())
                .build();
    }
    @Transactional
    public Workbook generatePanelAssignmentExcelTemplate() {

        Workbook workbook = new XSSFWorkbook();

        Sheet templateSheet = workbook.createSheet("Panel Assignment");
        Sheet reqPositionHiddenSheet = workbook.createSheet("ReqPositionHidden");
        Sheet panelHiddenSheet = workbook.createSheet("PanelHidden");

        Sheet requisitionIdSheet=workbook.createSheet("RequisitionHidden");
        Sheet positionIdSheet=workbook.createSheet("PositionHidden");


        createHeader(templateSheet);

        List<JobRequisitionsEntity> requisitions =
                jobRequisitionsRepository.findAllByRequisitionStatusIn(
                        List.of(RequisitionStatus.APPROVED));

        List<UUID> reqIds = requisitions.stream()
                .map(JobRequisitionsEntity::getId)
                .toList();

        List<JobPositionsEntity> positions =
                positionsRepository.findAllByRequisitionIdInAndPositionStatus(reqIds,PositionStatus.ACTIVE);

        Map<UUID,String> deptMap =
                departmentsRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                DepartmentsEntity::getId,
                                DepartmentsEntity::getDepartmentName
                        ));

        Map<UUID,String> masterPositionMap =
                masterPositionsRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                MasterPositionsEntity::getId,
                                MasterPositionsEntity::getPositionName
                        ));

        Map<String,List<String>> requisitionPositionMap = new LinkedHashMap<>();
        Map<String,UUID> requisitionMap=new HashMap<>();
        Map<String,UUID> positionMap=new HashMap<>();
        Map<String, String> requisitionSafeNameMap = new HashMap<>();
        for(JobPositionsEntity position : positions){

            JobRequisitionsEntity req =
                    requisitions.stream()
                            .filter(r -> r.getId().equals(position.getRequisitionId()))
                            .findFirst()
                            .orElse(null);

            if(req==null){
                continue;
            }

            String reqDisplay =
                    req.getRequisitionTitle()+"("+req.getRequisitionCode()+")";

            String positionName =
                    masterPositionMap.get(position.getMasterPositionId());

            String deptName =
                    deptMap.get(position.getDeptId());

            String display = positionName + " - " + deptName;
            String safeName = reqDisplay.replaceAll("[^A-Za-z0-9]", "_");
            if (!Character.isLetter(safeName.charAt(0)) && safeName.charAt(0) != '_') {
                safeName = "_" + safeName;
            }

            requisitionPositionMap
                    .computeIfAbsent(reqDisplay,k->new ArrayList<>())
                    .add(display);

            requisitionMap.put(reqDisplay,req.getId());
            requisitionSafeNameMap.put(reqDisplay, safeName);
            String key = reqDisplay + "||" + display;
            positionMap.put(key, position.getId());

        }

        populateReqPositionHiddenSheet(workbook,reqPositionHiddenSheet,requisitionPositionMap);

        populateReqHiddenSheet(requisitionIdSheet,requisitionMap,requisitionSafeNameMap);

        populatePosHiddenSheet(positionIdSheet,positionMap);


        Map<String, UUID> panelNameMap =
                interviewPanelsRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                panel -> {
                                    String committeeName = panel.getCommittee() != null
                                            ? panel.getCommittee().getCommitteeName()
                                            : AppConstants.UNKNOWN_COMMITTEE;

                                    return committeeName + " - " + panel.getPanelName();
                                },
                                InterviewPanelsEntity::getId,
                                (existing, replacement) -> existing,
                                LinkedHashMap::new
                        ));

        populatePanelHiddenSheet(panelHiddenSheet,panelNameMap);

        createRequisitionDropdown(templateSheet,reqPositionHiddenSheet,requisitionPositionMap.size());

        createPositionDropdown(templateSheet);

        createPanelDropdown(templateSheet,panelHiddenSheet,panelNameMap.size());

        workbook.setSheetHidden(workbook.getSheetIndex(reqPositionHiddenSheet),true);
        workbook.setSheetHidden(workbook.getSheetIndex(panelHiddenSheet),true);
        workbook.setSheetHidden(workbook.getSheetIndex(requisitionIdSheet),true);
        workbook.setSheetHidden(workbook.getSheetIndex(positionIdSheet),true);
        for (int i = 0; i < 5; i++) {
            templateSheet.autoSizeColumn(i);
        }
        return workbook;
    }

    private void populatePosHiddenSheet(Sheet sheet, Map<String, UUID> positionMap) {

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue(AppConstants.REQUISITION);
        header.createCell(1).setCellValue(AppConstants.POSITION);
        header.createCell(2).setCellValue("PositionId");

        int rowIndex = 1;

        for (Map.Entry<String, UUID> entry : positionMap.entrySet()) {

            String[] parts = entry.getKey().split("\\|\\|");

            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(parts[0]); // requisition
            row.createCell(1).setCellValue(parts[1]); // position
            row.createCell(2).setCellValue(entry.getValue().toString());
        }
    }

    private void populateReqHiddenSheet(Sheet requisitionIdSheet, Map<String, UUID> requisitionMap,Map<String,String> requisitionSafeMap) {
        Row header = requisitionIdSheet.createRow(0);

        header.createCell(0).setCellValue(AppConstants.REQUISITION);
        header.createCell(1).setCellValue("RequisitionId");
        header.createCell(2).setCellValue("SafeName");

        int rowIndex = 1;

        for(Map.Entry<String,UUID> entry : requisitionMap.entrySet()){

            Row row = requisitionIdSheet.createRow(rowIndex++);

            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().toString());
            row.createCell(2).setCellValue(requisitionSafeMap.get(entry.getKey()));
        }

    }

    private void createHeader(Sheet sheet){

        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue(AppConstants.REQUISITION);
        header.createCell(1).setCellValue(AppConstants.POSITION);
        header.createCell(2).setCellValue(AppConstants.PANEL_NAME);
        header.createCell(3).setCellValue("Start Date(DD-MM-YYYY)");
        header.createCell(4).setCellValue("End Date(DD-MM-YYYY)");
    }
    private void populateReqPositionHiddenSheet(
            Workbook workbook,
            Sheet hiddenSheet,
            Map<String,List<String>> requisitionPositionMap){

        Row headerRow = hiddenSheet.createRow(0);

        int columnIndex = 0;

        for(Map.Entry<String,List<String>> entry : requisitionPositionMap.entrySet()){

            String requisition = entry.getKey();
            List<String> positions = entry.getValue();

            headerRow.createCell(columnIndex).setCellValue(requisition);

            for(int i=0;i<positions.size();i++){

                Row row = hiddenSheet.getRow(i+1);

                if(row==null){
                    row = hiddenSheet.createRow(i+1);
                }

                row.createCell(columnIndex).setCellValue(positions.get(i));
            }

            createNamedRange(workbook,hiddenSheet,requisition,positions.size(),columnIndex);

            columnIndex++;
        }
    }
    private void createNamedRange(
            Workbook workbook,
            Sheet hiddenSheet,
            String requisition,
            int positionCount,
            int columnIndex){

        Name namedRange = workbook.createName();

        String safeName = requisition.replaceAll("[^A-Za-z0-9]","_");

        if(!Character.isLetter(safeName.charAt(0)) && safeName.charAt(0)!='_'){
            safeName = "_"+safeName;
        }

        namedRange.setNameName(safeName);

        String colLetter =
                CellReference.convertNumToColString(columnIndex);

        String formula =
                hiddenSheet.getSheetName()+
                        "!$"+colLetter+"$2:$"+colLetter+"$"+(positionCount+1);

        namedRange.setRefersToFormula(formula);
    }
    private void populatePanelHiddenSheet(
            Sheet panelHiddenSheet,
            Map<String,UUID> panelNameMap){

        Row header = panelHiddenSheet.createRow(0);

        header.createCell(0).setCellValue("PanelName");
        header.createCell(1).setCellValue("PanelId");

        int rowIndex = 1;

        for(Map.Entry<String,UUID> entry : panelNameMap.entrySet()){

            Row row = panelHiddenSheet.createRow(rowIndex++);

            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().toString());
        }
    }
    private void createRequisitionDropdown(
            Sheet templateSheet,
            Sheet hiddenSheet,
            int requisitionCount){

        DataValidationHelper helper =
                templateSheet.getDataValidationHelper();

        String endColumn =
                CellReference.convertNumToColString(requisitionCount-1);

        String formula =
                hiddenSheet.getSheetName()+"!$A$1:$"+endColumn+"$1";

        DataValidationConstraint constraint =
                helper.createFormulaListConstraint(formula);

        CellRangeAddressList addressList =
                new CellRangeAddressList(1,500,0,0);

        DataValidation validation =
                helper.createValidation(constraint,addressList);

        templateSheet.addValidationData(validation);
    }
    private void createPositionDropdown(Sheet templateSheet){

        DataValidationHelper helper = templateSheet.getDataValidationHelper();

        String formula = "INDIRECT(VLOOKUP($A2, RequisitionHidden!$A:$C, 3, FALSE))";

        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);

        CellRangeAddressList addressList =
                new CellRangeAddressList(1,500,1,1);

        DataValidation validation =
                helper.createValidation(constraint,addressList);

        templateSheet.addValidationData(validation);
    }
    private void validateHeader(Row headerRow) {
        if (headerRow == null) {
            throw new ExcelValidationException(List.of("Header row is missing"));
        }

        List<String> expectedHeaders = List.of(
                AppConstants.REQUISITION,
                AppConstants.POSITION,
                AppConstants.PANEL_NAME,
                "Start Date(DD-MM-YYYY)",
                "End Date(DD-MM-YYYY)"
        );

        List<String> actualHeaders = new ArrayList<>();

        for (int i = 0; i < expectedHeaders.size(); i++) {
            Cell cell = headerRow.getCell(i);

            if (cell == null || cell.getCellType() != CellType.STRING) {
                throw new ExcelValidationException(
                        List.of("Invalid header at column " + (i + 1))
                );
            }

            actualHeaders.add(cell.getStringCellValue().trim());
        }

        if (!actualHeaders.equals(expectedHeaders)) {
            throw new ExcelValidationException(List.of(
                    "Invalid Excel template",
                    "Expected: " + expectedHeaders,
                    "Found: " + actualHeaders
            ));
        }
    }
    private void createPanelDropdown(
            Sheet templateSheet,
            Sheet panelHiddenSheet,
            int panelCount){

        DataValidationHelper helper =
                templateSheet.getDataValidationHelper();

        String formula =
                panelHiddenSheet.getSheetName()+"!$A$2:$A$"+(panelCount+1);

        DataValidationConstraint constraint =
                helper.createFormulaListConstraint(formula);

        CellRangeAddressList addressList =
                new CellRangeAddressList(1,200,2,2);

        DataValidation validation =
                helper.createValidation(constraint,addressList);

        templateSheet.addValidationData(validation);
    }
    private record ExcelRowData(
            int rowNumber,
            UUID positionId,
            UUID panelId,
            LocalDate startDate,
            LocalDate endDate
    ){}

    @Transactional
    public List<PositionPanelDTO> uploadPanelAssignments(MultipartFile file) throws IOException, MessagingException {

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet templateSheet = workbook.getSheet("Panel Assignment");
        if (templateSheet == null) {
            throw new ExcelValidationException(List.of("Invalid excel sheet format. Please download the template and upload the file in the correct format."));
        }

        // 2. Header Validation (Immediate Fail if headers don't match)
        validateHeader(templateSheet.getRow(0));
        Sheet positionHidden = workbook.getSheet("PositionHidden");
        Sheet panelHidden = workbook.getSheet("PanelHidden");
        Sheet requisitionIdSheet=workbook.getSheet("RequisitionHidden");


        Map<String, UUID> positionLookup = buildPositionLookup(positionHidden);
        Map<String, UUID> panelLookup = buildPanelOrRequisitionLookup(panelHidden);
        Map<String,UUID> requisitionLookup=buildPanelOrRequisitionLookup(requisitionIdSheet);
        List<String> errors = new ArrayList<>();
        List<ExcelRowData> rows = new ArrayList<>();
        List<UUID> requisitionIds=new ArrayList<>();
        Set<UUID> positionIds = new HashSet<>();
        Set<UUID> panelIds = new HashSet<>();

        Set<String> seenInFile = new HashSet<>();
        for(int i = 1; i <= templateSheet.getLastRowNum(); i++){
            List<String> missingFields = new ArrayList<>();
            List<String> invalidFields = new ArrayList<>();
            Row row = templateSheet.getRow(i);
            if(row == null) continue;

            int rowNum = i + 1;

            String reqDisplay = getCellValue(row.getCell(0));
            String positionDisplay = getCellValue(row.getCell(1));
            String panelName = getCellValue(row.getCell(2));
            LocalDate startDate = null;
            LocalDate endDate = null;
            if (reqDisplay == null || reqDisplay.isBlank()) {
                missingFields.add(AppConstants.REQUISITION);
            }

            if (positionDisplay == null || positionDisplay.isBlank()) {
                missingFields.add(AppConstants.POSITION);
            }

            if (panelName == null || panelName.isBlank()) {
                missingFields.add(AppConstants.PANEL_NAME);
            }


            String key = reqDisplay + "||" + positionDisplay;

            UUID requisitionId = null;
            UUID positionId = null;
            UUID panelId = null;

            // Requisition
            if (!missingFields.contains(AppConstants.REQUISITION)) {
                requisitionId = requisitionLookup.get(reqDisplay);
                if (requisitionId == null) {
                    invalidFields.add(AppConstants.REQUISITION);
                }
            }

            // Position
            if (!missingFields.contains(AppConstants.POSITION) && requisitionId != null) {
                positionId = positionLookup.get(key);
                if (positionId == null) {
                    invalidFields.add(AppConstants.POSITION);
                }
            }

            // Panel
            if (!missingFields.contains(AppConstants.PANEL_NAME)) {
                panelId = panelLookup.get(panelName);
                if (panelId == null) {
                    invalidFields.add(AppConstants.PANEL_NAME);
                }
            }
            requisitionIds.add(requisitionId);
            //Parse the date
            startDate = parseDate(row.getCell(3), rowNum, errors, "Start Date");
            endDate = parseDate(row.getCell(4), rowNum, errors, "End Date");
            if (startDate == null) {
                missingFields.add("Start Date");
            }

            if (endDate == null) {
                missingFields.add("End Date");
            }

            if (!missingFields.isEmpty()) {
                errors.add("Row " + rowNum + ": Missing mandatory fields: " + String.join(", ", missingFields));
            }

            if (startDate != null && endDate != null) {
                LocalDate today = LocalDate.now();

                // 1. Start Date cannot be in the past
                if (startDate.isBefore(today)) {
                    errors.add("Invalid start date. The selected Start Date " + startDate + " is in the past for panel '" + panelName + "'.");
                }
                if(endDate.isBefore(today)){
                    errors.add("Invalid end date. The selected End Date" + endDate + " is in the past for panel '" + panelName + "'.");
                }
            }
            // DATE LOGIC VALIDATION
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                errors.add("Row " + rowNum + ": End date must be after start date");
            }

            // --- NEW DUPLICATE CHECK WITHIN FILE ---
            if (positionId != null && panelId != null) {
                String duplicateKey = positionId + "_" + panelId;
                if (!seenInFile.add(duplicateKey)) {
                    errors.add("Row " + (i + 1) + ": Duplicate entry for Position and Panel combination within the file.");
                    continue; // Skip further processing for this redundant row
                }
            }
            if(positionId != null && panelId != null){
                rows.add(new ExcelRowData(rowNum,positionId,panelId,startDate,endDate));

                positionIds.add(positionId);
                panelIds.add(panelId);
            }
        }
        if (rows.isEmpty() && requisitionIds.isEmpty()) {
            throw new ExcelValidationException(
                    List.of("The file is empty. Please add some data and try again.")
            );
        }

        if(!errors.isEmpty()){
            throw new ExcelValidationException(errors);
        }

        validateIdsExist(rows, positionIds, panelIds, errors);
        validateExistingPositionPanels(rows, errors);
        if(!errors.isEmpty()){
            throw new ExcelValidationException(errors);
        }
        //validateBulkPanelAvailability(rows, errors);

        if(!errors.isEmpty()){
            throw new ExcelValidationException(errors);
        }

        List<PositionPanelEntity> savedPositionPanels =buildEntities(rows);
        mailSenderHelper.sendPanelApprovalMail(savedPositionPanels, RequisitionApproversEntity.ApproverRole.L1);
        return positionPanelMapper.toDtoListWithoutChildren(savedPositionPanels);
    }

    private void validateExistingPositionPanels(
            List<ExcelRowData> rows,
            List<String> errors) {

        Set<UUID> positionIds = rows.stream()
                .map(ExcelRowData::positionId)
                .collect(Collectors.toSet());

        List<PositionPanelEntity> existingPanels =
                positionPanelRepository.findByJobPositionIdIn(positionIds);

        Set<String> existingPairs = existingPanels.stream()
                .map(p -> p.getJobPosition().getId() + "_" +
                        p.getInterviewPanel().getId())
                .collect(Collectors.toSet());

        for (ExcelRowData row : rows) {

            String key = row.positionId() + "_" + row.panelId();

            if (existingPairs.contains(key)) {
                errors.add("Row " + row.rowNumber() + ": Panel already assigned to this position");
            }
        }
    }
    private List<PositionPanelEntity> buildEntities(List<ExcelRowData> rows){

        List<PositionPanelEntity> entities = new ArrayList<>();

        for(ExcelRowData row : rows){

            PositionPanelEntity entity = new PositionPanelEntity();

            entity.setJobPosition(
                    entityManager.getReference(JobPositionsEntity.class,row.positionId()));

            entity.setInterviewPanel(
                    entityManager.getReference(InterviewPanelsEntity.class,row.panelId()));

            entity.setStartDate(row.startDate());
            entity.setEndDate(row.endDate());

            entity.setPositionPanelStatus(PositionPanelStatus.L1_PENDING);

            entities.add(entity);
        }

        return positionPanelRepository.saveAll(entities);
    }
    private LocalDate parseDate(Cell cell, int rowNum, List<String> errors, String fieldName) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }

            if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();

                if (value.isEmpty()) return null;

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                return LocalDate.parse(value, formatter);
            }

        } catch (Exception e) {
            errors.add("Row " + rowNum + ": Invalid " + fieldName + " format (DD-MM-YYYY)");
        }

        return null;
    }
    private void validateIdsExist(
            List<ExcelRowData> rows,
            Set<UUID> positionIds,
            Set<UUID> panelIds,
            List<String> errors){

        Set<UUID> validPositions =
                positionsRepository.findAllById(positionIds)
                        .stream()
                        .map(JobPositionsEntity::getId)
                        .collect(Collectors.toSet());

        Set<UUID> validPanels =
                interviewPanelsRepository.findAllById(panelIds)
                        .stream()
                        .map(InterviewPanelsEntity::getId)
                        .collect(Collectors.toSet());

        for(ExcelRowData row : rows){

            if(!validPositions.contains(row.positionId())){
                errors.add("Row " + row.rowNumber() + ": Position does not exist in system");
            }

            if(!validPanels.contains(row.panelId())){
                errors.add("Row " + row.rowNumber() + ": Panel does not exist in system");
            }
        }
    }

    private String getCellValue(Cell cell){
        if(cell==null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BLANK -> null;
            default -> cell.toString();
        };
    }

    private Map<String, UUID> buildPositionLookup(Sheet sheet){

        Map<String, UUID> map = new HashMap<>();

        for(int i = 1; i <= sheet.getLastRowNum(); i++){

            Row row = sheet.getRow(i);
            if(row == null) continue;

            String req = getCellValue(row.getCell(0));
            String pos = getCellValue(row.getCell(1));
            String id  = getCellValue(row.getCell(2));

            if(req == null || pos == null || id == null) continue;

            map.put(req + "||" + pos, UUID.fromString(id));
        }

        return map;
    }

    private Map<String, UUID> buildPanelOrRequisitionLookup(Sheet sheet){

        Map<String, UUID> map = new HashMap<>();

        for(int i = 1; i <= sheet.getLastRowNum(); i++){

            Row row = sheet.getRow(i);
            if(row == null) continue;

            String name = getCellValue(row.getCell(0));
            String id   = getCellValue(row.getCell(1));

            if(name == null || id == null) continue;

            map.put(name, UUID.fromString(id));
        }

        return map;
    }

    private List<PanelMemberAssignment> buildExcelAssignments(List<ExcelRowData> rows){

        List<PanelMemberAssignment> assignments = new ArrayList<>();

        Set<UUID> panelIds = rows.stream()
                .map(ExcelRowData::panelId)
                .collect(Collectors.toSet());

        Map<UUID, InterviewPanelsEntity> panelMap =
                interviewPanelsRepository.findAllById(panelIds)
                        .stream()
                        .collect(Collectors.toMap(
                                InterviewPanelsEntity::getId,
                                p -> p
                        ));

        for(ExcelRowData row : rows){

            InterviewPanelsEntity panel = panelMap.get(row.panelId());

            if(panel == null) continue;

            for(InterviewPanelMembersEntity member : panel.getPanelMembers()){

                UUID userId = member.getPanelMember().getId();

                assignments.add(new PanelMemberAssignment(
                        userId,
                        row.panelId(),
                        row.positionId(),
                        row.startDate(),
                        row.endDate(),
                        true
                ));
            }
        }

        return assignments;
    }

    private List<PanelMemberAssignment> fetchDbAssignments(Set<UUID> userIds){

        List<PositionPanelEntity> dbPanels =
                positionPanelRepository
                        .findByInterviewPanel_PanelMembers_PanelMember_IdIn(userIds);

        List<PanelMemberAssignment> assignments = new ArrayList<>();

        for(PositionPanelEntity entity : dbPanels){

            for(InterviewPanelMembersEntity member :
                    entity.getInterviewPanel().getPanelMembers()){

                UUID userId = member.getPanelMember().getId();

                if(userIds.contains(userId)){

                    assignments.add(new PanelMemberAssignment(
                            userId,
                            entity.getInterviewPanel().getId(),
                            entity.getJobPosition().getId(),
                            entity.getStartDate(),
                            entity.getEndDate(),
                            false
                    ));
                }
            }
        }

        return assignments;
    }


//    private void validateBulkPanelAvailability(List<ExcelRowData> rows, List<String> errors){
//
//        List<PanelMemberAssignment> excelAssignments = buildExcelAssignments(rows);
//
//        if(excelAssignments.isEmpty()){
//            return;
//        }
//
//        Set<UUID> userIds = excelAssignments.stream()
//                .map(PanelMemberAssignment::userId)
//                .collect(Collectors.toSet());
//
//        List<PanelMemberAssignment> dbAssignments = fetchDbAssignments(userIds);
//
//        List<PanelMemberAssignment> allAssignments = new ArrayList<>();
//        allAssignments.addAll(excelAssignments);
//        allAssignments.addAll(dbAssignments);
//
//        Set<UUID> positionIds = allAssignments.stream()
//                .map(PanelMemberAssignment::positionId)
//                .collect(Collectors.toSet());
//
//        Set<UUID> panelIds = allAssignments.stream()
//                .map(PanelMemberAssignment::panelId)
//                .collect(Collectors.toSet());
//
//        Map<UUID, UserEntity> userMap =
//                userRepository.findAllById(userIds)
//                        .stream()
//                        .collect(Collectors.toMap(UserEntity::getId, u -> u));
//
//        Map<UUID, JobPositionsEntity> positionMap =
//                positionsRepository.findAllById(positionIds)
//                        .stream()
//                        .collect(Collectors.toMap(JobPositionsEntity::getId, p -> p));
//
//        Map<UUID, InterviewPanelsEntity> panelMap =
//                interviewPanelsRepository.findAllById(panelIds)
//                        .stream()
//                        .collect(Collectors.toMap(InterviewPanelsEntity::getId, p -> p));
//
//        Set<UUID> requisitionIds = positionMap.values().stream()
//                .map(JobPositionsEntity::getRequisitionId)
//                .collect(Collectors.toSet());
//
//        Map<UUID, JobRequisitionsEntity> requisitionMap =
//                jobRequisitionsRepository.findAllById(requisitionIds)
//                        .stream()
//                        .collect(Collectors.toMap(JobRequisitionsEntity::getId, r -> r));
//
//        Set<UUID> masterPositionIds = positionMap.values().stream()
//                .map(JobPositionsEntity::getMasterPositionId)
//                .collect(Collectors.toSet());
//
//        Map<UUID, MasterPositionsEntity> masterPositionMap =
//                masterPositionsRepository.findAllById(masterPositionIds)
//                        .stream()
//                        .collect(Collectors.toMap(MasterPositionsEntity::getId, m -> m));
//
//        for(int i=0;i<allAssignments.size();i++){
//
//            PanelMemberAssignment a1 = allAssignments.get(i);
//
//            for(int j=i+1;j<allAssignments.size();j++){
//
//                PanelMemberAssignment a2 = allAssignments.get(j);
//
//                if(!a1.userId().equals(a2.userId())) continue;
//
//                if(datesOverlap(a1.startDate(),a1.endDate(),a2.startDate(),a2.endDate())
//                        && checkUserIsFromRequest(a1,a2)){
//
//                    String userName = userMap.get(a1.userId()).getName();
//
//                    JobPositionsEntity pos1 = positionMap.get(a1.positionId());
//                    JobPositionsEntity pos2 = positionMap.get(a2.positionId());
//
//                    String positionName1 =
//                            masterPositionMap.get(pos1.getMasterPositionId()).getPositionName();
//
//                    String positionName2 =
//                            masterPositionMap.get(pos2.getMasterPositionId()).getPositionName();
//
//                    JobRequisitionsEntity req1 =
//                            requisitionMap.get(pos1.getRequisitionId());
//
//                    JobRequisitionsEntity req2 =
//                            requisitionMap.get(pos2.getRequisitionId());
//
//                    String requisitionDisplay1 =
//                            req1.getRequisitionTitle()+"("+req1.getRequisitionCode()+")";
//
//                    String requisitionDisplay2 =
//                            req2.getRequisitionTitle()+"("+req2.getRequisitionCode()+")";
//
//                    InterviewPanelsEntity panel1 = panelMap.get(a1.panelId());
//                    InterviewPanelsEntity panel2 = panelMap.get(a2.panelId());
//
//                    String panelName1 = panel1.getPanelName();
//                    String panelName2 = panel2.getPanelName();
//
//                    String committee1 =
//                            panel1.getCommittee()!=null
//                                    ? panel1.getCommittee().getCommitteeName()
//                                    : AppConstants.UNKNOWN_COMMITTEE;
//
//                    String committee2 =
//                            panel2.getCommittee()!=null
//                                    ? panel2.getCommittee().getCommitteeName()
//                                    : AppConstants.UNKNOWN_COMMITTEE;
//
//                    errors.add(
//                            String.format(
//                                    "User %s is assigned to overlapping panels.\n\n" +
//                                            "Panel 1:\n" +
//                                            "• Position: %s\n" +
//                                            "• Panel: %s (%s Committee)\n" +
//                                            "• Requisition: %s\n" +
//                                            "• Dates: %s to %s\n\n" +
//                                            "Panel 2:\n" +
//                                            "• Position: %s\n" +
//                                            "• Panel: %s (%s Committee)\n" +
//                                            "• Requisition: %s\n" +
//                                            "• Dates: %s to %s",
//                                    userName,
//                                    positionName1,
//                                    panelName1,
//                                    committee1,
//                                    requisitionDisplay1,
//                                    a1.startDate(),
//                                    a1.endDate(),
//                                    positionName2,
//                                    panelName2,
//                                    committee2,
//                                    requisitionDisplay2,
//                                    a2.startDate(),
//                                    a2.endDate()
//                            )
//                    );
//                }
//            }
//        }
//    }
}
