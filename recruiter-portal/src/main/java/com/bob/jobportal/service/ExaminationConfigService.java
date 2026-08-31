package com.bob.jobportal.service;

import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.entity.*;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.enums.ExamQualificationStatus;
import com.bob.db.enums.WrittenExamConfigurationStatus;
import com.bob.db.dto.WorkflowApprovalDTO;
import com.bob.db.dto.ExamSectionCategoryPassMarksDTO;
import com.bob.db.dto.ExamSectionPassMarksDTO;
import com.bob.db.dto.WrittenExamConfigurationDTO;
import com.bob.db.mapper.WorkflowApprovalMapper;
import com.bob.db.mapper.ExamSectionCategoryPassMarksMapper;
import com.bob.db.mapper.ExamSectionPassMarksMapper;
import com.bob.db.mapper.WrittenExamConfigurationMapper;
import com.bob.db.repository.*;
import com.bob.db.util.DBConstants;
import com.bob.jobportal.model.ExamConfigApprovalRequestModel;
import com.bob.jobportal.model.ExamConfigSaveRequestModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExaminationConfigService {

    @Autowired
    private WrittenExamConfigurationRepository writtenExamConfigurationRepository;

    @Autowired
    private WrittenExamConfigurationMapper writtenExamConfigurationMapper;

    @Autowired
    private ExamSectionPassMarksRepository examSectionPassMarksRepository;

    @Autowired
    private ExamSectionPassMarksMapper examSectionPassMarksMapper;

    @Autowired
    private ExamSectionCategoryPassMarksRepository examSectionCategoryPassMarksRepository;

    @Autowired
    private ExamSectionCategoryPassMarksMapper examSectionCategoryPassMarksMapper;

    @Autowired
    private RequisitionApproversRepository requisitionApproversRepository;

    @Autowired
    private WorkflowApprovalMapper workflowApprovalMapper;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

   @Autowired
   private MailSenderHelper mailSenderHelper;

   @Autowired
   private CandidateWrittenExamMarksRepository candidateWrittenExamMarksRepository;

   @Autowired
   private CandidateApplicationsRepository candidateApplicationsRepository;

    @Transactional
    public List<WrittenExamConfigurationDTO> saveOrUpdateExamConfig(ExamConfigSaveRequestModel request) {
        return request.getPositionIds().stream()
                .map(positionId -> {
                    WrittenExamConfigurationDTO dto = request.getConfig();
                    WrittenExamConfigurationEntity entity = writtenExamConfigurationRepository.findByPositionId(positionId)
                            .map(existing -> {
                                writtenExamConfigurationMapper.updateEntity(dto, existing);
                                existing.setStatus(WrittenExamConfigurationStatus.PENDING);
                                return existing;
                            })
                            .orElseGet(() -> {
                                WrittenExamConfigurationEntity newEntity = writtenExamConfigurationMapper.toEntity(dto);
                                newEntity.setPositionId(positionId);
                                newEntity.setStatus(WrittenExamConfigurationStatus.PENDING);
                                newEntity.setIsFrozen(false);
                                return newEntity;
                            });

                    if (entity.getWrittenExamWeightage() != null) {
                        entity.setInterviewWeightage(new BigDecimal("100").subtract(entity.getWrittenExamWeightage()));
                    }

                    WrittenExamConfigurationEntity saved = writtenExamConfigurationRepository.save(entity);
                    workflowApprovalEntityRepository.save(buildWorkflowEntity(saved, "Submitted"));

                    List<ExamSectionPassMarksDTO> sections = dto.getSections();
                    if (sections != null && !sections.isEmpty()) {
                        saveOrUpdateSections(sections, saved);
                    }

                    return toResponseDTO(saved);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WrittenExamConfigurationDTO> getAllExamConfigs(List<UUID> requisitionIds) {
        RequisitionApproversEntity approver = requisitionApproversRepository.findByApproverId(securityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user is not configured as an approver."));

        List<WrittenExamConfigurationStatus> allowedStatuses;

        if (approver.getApproverRole() == RequisitionApproversEntity.ApproverRole.L1) {
            allowedStatuses = List.of(
                    WrittenExamConfigurationStatus.L1_PENDING,
                    WrittenExamConfigurationStatus.L2_PENDING,
                    WrittenExamConfigurationStatus.L1_REJECTED,
                    WrittenExamConfigurationStatus.L2_REJECTED,
                    WrittenExamConfigurationStatus.APPROVED,
                    WrittenExamConfigurationStatus.FINALIZED
            );
        } else if (approver.getApproverRole() == RequisitionApproversEntity.ApproverRole.L2) {
            allowedStatuses = List.of(
                    WrittenExamConfigurationStatus.L2_PENDING,
                    WrittenExamConfigurationStatus.L2_REJECTED,
                    WrittenExamConfigurationStatus.APPROVED,
                    WrittenExamConfigurationStatus.FINALIZED
            );
        } else {
            throw new IllegalStateException("User has an unsupported approver role: " + approver.getApproverRole());
        }

        Map<UUID, JobPositionsEntity> jobPositionsMap = positionsRepository.findAllByRequisitionIdIn(requisitionIds)
                .stream()
                .collect(Collectors.toMap(
                        JobPositionsEntity::getId,
                        Function.identity()
                ));

        if (CollectionUtils.isEmpty(jobPositionsMap)) {
            return Collections.emptyList();
        }
        List<UUID> positionIds = jobPositionsMap.values().stream().map(JobPositionsEntity::getId).toList();
        Set<UUID> masterPositionIds = jobPositionsMap.values().stream().map(JobPositionsEntity::getMasterPositionId).collect(Collectors.toSet());
        Map<UUID,String> masterPositionNameMap = masterPositionsRepository.findAllById(masterPositionIds).stream()
                .collect(Collectors.toMap(
                        MasterPositionsEntity::getId,
                        MasterPositionsEntity::getPositionName
                ));

        return writtenExamConfigurationRepository
                .findByPositionIdInAndStatusIn(positionIds, allowedStatuses)
                .stream()
                .map(config -> {

                    WrittenExamConfigurationDTO dto =
                            toResponseDTO(config);

                    UUID masterPositionId =
                            jobPositionsMap.get(config.getPositionId())
                                    .getMasterPositionId();

                    dto.setPositionName(
                            masterPositionNameMap.get(masterPositionId)
                    );

                    return dto;
                })
                .toList();
    }

    @Transactional
    public List<WrittenExamConfigurationDTO> finalizeExamConfigs(List<UUID> positionIds) {
        List<WrittenExamConfigurationEntity> entities = writtenExamConfigurationRepository.findByPositionIdIn(positionIds);

        if (entities.isEmpty()) {
            throw new ResourceNotFoundException("No exam configurations found for given position IDs.");
        }

        entities.forEach(entity -> {
            if (entity.getStatus() != WrittenExamConfigurationStatus.APPROVED) {
                throw new IllegalStateException(
                    String.format("Cannot finalize exam config for position '%s'. Must be in APPROVED state. Found '%s'.",
                        entity.getPositionId(), entity.getStatus()));
            }
            entity.setStatus(WrittenExamConfigurationStatus.FINALIZED);
        });

        List<WrittenExamConfigurationEntity> saved = writtenExamConfigurationRepository.saveAll(entities);
        workflowApprovalEntityRepository.saveAll(
                saved.stream().map(e -> buildWorkflowEntity(e, null)).collect(Collectors.toList())
        );

        return saved.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WrittenExamConfigurationDTO> getExamConfigsByPositionIds(List<UUID> positionIds) {
        return writtenExamConfigurationRepository.findByPositionIdIn(positionIds)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private void saveOrUpdateSections(
            List<ExamSectionPassMarksDTO> sections,
            WrittenExamConfigurationEntity examConfig
    ) {

        List<ExamSectionPassMarksEntity> entitiesToSave = new ArrayList<>();

        for (ExamSectionPassMarksDTO sectionDTO : sections) {

            ExamSectionPassMarksEntity sectionEntity = null;

            // 1. Try by ID
            if (sectionDTO.getId() != null) {

                sectionEntity = examSectionPassMarksRepository
                        .findById(sectionDTO.getId())
                        .orElse(null);
            }

            // 2. Try by unique key (DB)
            if (sectionEntity == null) {

                sectionEntity = examSectionPassMarksRepository
                        .findByExamConfig_IdAndSectionNumber(
                                examConfig.getId(),
                                sectionDTO.getSectionNumber()
                        )
                        .orElse(null);
            }

            // 2b. Try in-memory list to avoid creating duplicates within same request
            if (sectionEntity == null) {
                sectionEntity = entitiesToSave.stream()
                        .filter(e -> Objects.equals(e.getSectionNumber(), sectionDTO.getSectionNumber()))
                        .findFirst()
                        .orElse(null);
            }

            // 3. Create new
            if (sectionEntity == null) {

                sectionEntity = examSectionPassMarksMapper.toEntity(sectionDTO);
                sectionEntity.setExamConfig(examConfig);

            } else {
                examSectionPassMarksMapper.updateEntity(sectionDTO, sectionEntity);
            }

            entitiesToSave.add(sectionEntity);
        }

        List<ExamSectionPassMarksEntity> savedSections =
                examSectionPassMarksRepository.saveAll(entitiesToSave);

        // Save category pass marks
        for (ExamSectionPassMarksEntity savedSection : savedSections) {

            ExamSectionPassMarksDTO matchingDTO = sections.stream()
                    .filter(dto -> dto.getSectionNumber().equals(savedSection.getSectionNumber()))
                    .findFirst()
                    .orElse(null);

            if (matchingDTO != null && matchingDTO.getCategoryPassMarks() != null) {

                saveOrUpdateCategoryPassMarks(matchingDTO.getCategoryPassMarks(), savedSection);
            }
        }
    }

    private void saveOrUpdateCategoryPassMarks(
            List<ExamSectionCategoryPassMarksDTO> categoryPassMarks,
            ExamSectionPassMarksEntity sectionEntity
    ) {

        List<ExamSectionCategoryPassMarksEntity> entitiesToSave = new ArrayList<>();

        for (ExamSectionCategoryPassMarksDTO categoryDTO : categoryPassMarks) {

            ExamSectionCategoryPassMarksEntity categoryEntity = null;

            // 1. Try by ID
            if (categoryDTO.getId() != null) {

                categoryEntity = examSectionCategoryPassMarksRepository
                        .findById(categoryDTO.getId())
                        .orElse(null);
            }

            // 2. Try by business key (DB)
            if (categoryEntity == null) {

                if (categoryDTO.getStateId() != null) {

                    categoryEntity = examSectionCategoryPassMarksRepository
                            .findByExamSection_IdAndCategory_IdAndStateId(
                                    sectionEntity.getId(),
                                    categoryDTO.getCategoryId(),
                                    categoryDTO.getStateId()
                            )
                            .orElse(null);

                } else {

                    categoryEntity = examSectionCategoryPassMarksRepository
                            .findByExamSection_IdAndCategory_IdAndStateIdIsNull(
                                    sectionEntity.getId(),
                                    categoryDTO.getCategoryId()
                            )
                            .orElse(null);
                }
            }

            // 2b. Try in-memory list to avoid creating duplicates within same request
            if (categoryEntity == null) {
                categoryEntity = entitiesToSave.stream()
                        .filter(e -> Objects.equals(e.getExamSection().getId(), sectionEntity.getId())
                                && Objects.equals(e.getCategory().getId(), categoryDTO.getCategoryId())
                                && ((categoryDTO.getStateId() == null && e.getStateId() == null)
                                || (categoryDTO.getStateId() != null && e.getStateId() != null && Objects.equals(e.getStateId(), categoryDTO.getStateId()))))
                        .findFirst()
                        .orElse(null);
            }

            // 3. Create new
            if (categoryEntity == null) {

                categoryEntity = examSectionCategoryPassMarksMapper.toEntity(categoryDTO);
                categoryEntity.setExamSection(sectionEntity);

            } else {

                examSectionCategoryPassMarksMapper.updateEntity(categoryDTO, categoryEntity);
            }

            entitiesToSave.add(categoryEntity);
        }

        examSectionCategoryPassMarksRepository.saveAll(entitiesToSave);
    }

    @Transactional(readOnly = true)
    public List<WorkflowApprovalDTO> getWorkflowHistory(UUID examConfigId) {
        return workflowApprovalMapper.toDTOs(
                workflowApprovalEntityRepository.findByEntityTypeAndEntityIdOrderByActionDateDesc(
                        WrittenExamConfigurationEntity.ENTITY_TYPE, examConfigId
                )
        );
    }

    @Transactional
    public WrittenExamConfigurationDTO approveOrReject(ExamConfigApprovalRequestModel request) {
        WrittenExamConfigurationEntity entity = writtenExamConfigurationRepository.findById(request.getExamConfigId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam configuration not found: " + request.getExamConfigId()));

        RequisitionApproversEntity approver = requisitionApproversRepository.findByApproverId(securityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user is not configured as an approver."));

        RequisitionApproversEntity.ApproverRole role = approver.getApproverRole();
        boolean approved = Boolean.TRUE.equals(request.getApproved());
        WrittenExamConfigurationStatus targetStatus;
        WrittenExamConfigurationStatus requiredCurrentStatus;

        if (role == RequisitionApproversEntity.ApproverRole.L1) {
            requiredCurrentStatus = WrittenExamConfigurationStatus.L1_PENDING;
            targetStatus = approved ? WrittenExamConfigurationStatus.L2_PENDING : WrittenExamConfigurationStatus.L1_REJECTED;
        } else if (role == RequisitionApproversEntity.ApproverRole.L2) {
            requiredCurrentStatus = WrittenExamConfigurationStatus.L2_PENDING;
            targetStatus = approved ? WrittenExamConfigurationStatus.APPROVED : WrittenExamConfigurationStatus.L2_REJECTED;
        } else {
            throw new IllegalStateException("User has an unsupported approver role: " + role);
        }

        if (entity.getStatus() != requiredCurrentStatus) {
            throw new IllegalStateException(
                String.format("Cannot perform action. Config must be in '%s' state. Found '%s'.",
                    requiredCurrentStatus, entity.getStatus()));
        }



        if(targetStatus == WrittenExamConfigurationStatus.APPROVED ) {
            entity.setIsFrozen(true);
            targetStatus = sendNotificationOrUpdateApplicationStatus(entity) != null ? WrittenExamConfigurationStatus.FINALIZED : WrittenExamConfigurationStatus.APPROVED;
        }

        if(targetStatus == WrittenExamConfigurationStatus.L2_PENDING){
            mailSenderHelper.sendApprovalMail(List.of(entity.getPositionId()), RequisitionApproversEntity.ApproverRole.L2,"Exam Configuration","Exam Configuration Approval Request");
        }

        entity.setStatus(targetStatus);
        entity.setComments(request.getComments());
        WrittenExamConfigurationEntity saved = writtenExamConfigurationRepository.save(entity);
        workflowApprovalEntityRepository.save(buildWorkflowEntity(saved, request.getComments()));

        return toResponseDTO(saved);
    }

    public WrittenExamConfigurationStatus sendNotificationOrUpdateApplicationStatus(WrittenExamConfigurationEntity entity) {
        UUID positionId = entity.getPositionId();
        List<CandidateWrittenExamMarksEntity> candidatesToNotify = candidateWrittenExamMarksRepository.findByPosition_Id(positionId);
        List<CandidateApplicationsEntity> applicationsEntitiesList = candidateApplicationsRepository.findByPositionId(positionId);

        if(candidatesToNotify.isEmpty()) {
            mailSenderHelper.sendExamNotification(positionId,applicationsEntitiesList);
        }else{
            Map<UUID,CandidateWrittenExamMarksEntity> applicationsEntityMap = candidatesToNotify.stream()
                    .collect(Collectors.toMap(
                            (curr)->curr.getApplication().getId(),
                            Function.identity()
                    ));

            applicationsEntitiesList.forEach((curr)->{
                CandidateWrittenExamMarksEntity writtenExamMarks = applicationsEntityMap.getOrDefault(curr.getId(),null);
                if (writtenExamMarks == null || (writtenExamMarks.getStatus() == ExamQualificationStatus.NOT_MARKED || writtenExamMarks.getStatus() == ExamQualificationStatus.DISQUALIFIED)) {
                    curr.setApplicationStatus(CandidateApplicationStatus.REJECTED);
                }
            });

            candidateApplicationsRepository.saveAllWithWorkflow(applicationsEntitiesList);
            return WrittenExamConfigurationStatus.FINALIZED;
        }
        return null;
    }


    private WorkflowApprovalEntity buildWorkflowEntity(WrittenExamConfigurationEntity entity, String comments) {
        return WorkflowApprovalEntity.builder()
                .entityId(entity.getId())
                .entityType(WrittenExamConfigurationEntity.ENTITY_TYPE)
                .stepNumber(1)
                .approverRole(securityUtils.getCurrentUserRole())
                .approverId(securityUtils.getCurrentUserId())
                .action(DBConstants.WORKFLOW_ACTION_UPDATE)
                .actionDate(LocalDateTime.now())
                .comments(comments)
                .status(entity.getStatus().toString())
                .build();
    }

    private WrittenExamConfigurationDTO toResponseDTO(WrittenExamConfigurationEntity entity) {
        WrittenExamConfigurationDTO dto = writtenExamConfigurationMapper.toDTO(entity);
        List<ExamSectionPassMarksDTO> sections = examSectionPassMarksRepository
                .findByExamConfig_Id(entity.getId())
                .stream()
                .map(sectionEntity -> {
                    ExamSectionPassMarksDTO sectionDTO = examSectionPassMarksMapper.toDTO(sectionEntity);
                    sectionDTO.setCategoryPassMarks(
                            examSectionCategoryPassMarksMapper.toDTOs(
                                    examSectionCategoryPassMarksRepository.findByExamSection_Id(sectionEntity.getId())
                            )
                    );
                    return sectionDTO;
                })
                .collect(Collectors.toList());
        dto.setSections(sections);
        return dto;
    }

    public List<WrittenExamConfigurationDTO> submitForApproval(List<UUID> positionIds) {
        List<WrittenExamConfigurationEntity> entities = writtenExamConfigurationRepository.findByPositionIdIn(positionIds);

        if (entities.isEmpty()) {
            throw new ResourceNotFoundException("No exam configurations found for given position IDs.");
        }
        Set<WrittenExamConfigurationStatus> allowedStatuses = Set.of(
                WrittenExamConfigurationStatus.PENDING,
                WrittenExamConfigurationStatus.APPROVED
        );

        entities.forEach(entity -> {
            if (!allowedStatuses.contains(entity.getStatus()) ) {
                throw new IllegalStateException(
                    String.format("Cannot submit for approval. Config must be in 'PENDING' state. Found '%s' for position '%s'.",
                        entity.getStatus(), entity.getPositionId()));
            }
            entity.setStatus(WrittenExamConfigurationStatus.L1_PENDING);
        });

        List<WrittenExamConfigurationEntity> saved = writtenExamConfigurationRepository.saveAll(entities);
        workflowApprovalEntityRepository.saveAll(
                saved.stream().map(e -> buildWorkflowEntity(e, "Submitted for approval")).collect(Collectors.toList())
        );
        mailSenderHelper.sendApprovalMail(positionIds, RequisitionApproversEntity.ApproverRole.L1,"Exam Configuration","Exam Configuration Approval Request");
        return writtenExamConfigurationMapper.toDTOs(saved);
    }

    public Boolean validateExamConfiguration(UUID positionId) {
        List<CandidateApplicationStatus> allowedStatuses = List.of(
                CandidateApplicationStatus.APPLIED,
                CandidateApplicationStatus.SHORTLISTED,
                CandidateApplicationStatus.ELIGIBLE,
                CandidateApplicationStatus.DISCREPANCY,
                CandidateApplicationStatus.PENDING,
                CandidateApplicationStatus.REJECTED
        );
        return candidateApplicationsRepository.existsByPositionIdAndApplicationStatusNotIn(positionId,allowedStatuses);
    }

    public void deleteExamSection(UUID sectionId) {

        ExamSectionPassMarksEntity sectionEntity = examSectionPassMarksRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam section not found: " + sectionId));

        List<ExamSectionCategoryPassMarksEntity> categoryPassMarksEntities =
                examSectionCategoryPassMarksRepository.findByExamSection_Id(sectionId);

        examSectionCategoryPassMarksRepository.deleteAll(categoryPassMarksEntities);

        examSectionPassMarksRepository.delete(sectionEntity);
    }
}
