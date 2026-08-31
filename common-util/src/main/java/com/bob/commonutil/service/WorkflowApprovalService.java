package com.bob.commonutil.service;

import com.bob.commonutil.model.WorkflowApprStatusResponseModel;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.WorkflowApprovalDTO;
import com.bob.db.entity.CandidateApplicationsEntity;
import com.bob.db.entity.ConversationThreadsEntity;
import com.bob.db.entity.JobRequisitionEditRequestEntity;
import com.bob.db.entity.JobRequisitionsEntity;
import com.bob.db.entity.PositionPanelEntity;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.enums.RequisitionEditStatus;
import com.bob.db.mapper.WorkflowApprovalMapper;
import com.bob.db.repository.JobRequisitionEditRequestRepository;
import com.bob.db.repository.PositionsRepository;
import com.bob.db.repository.WorkflowApprovalEntityRepository;
import com.bob.db.util.DBConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class WorkflowApprovalService {

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private WorkflowApprovalMapper workflowApprovalMapper;

    @Autowired
    private JobRequisitionEditRequestRepository jobRequisitionEditRequestRepository;
    public List<com.bob.commonutil.model.WorkflowApprStatusResponseModel> getApplicationStatus(UUID applicationId) {
        List<WorkflowApprovalDTO> sourceData = workflowApprovalMapper.toDTOs(
                workflowApprovalEntityRepository.findByEntityTypeAndEntityIdOrderByActionDateDesc(
                        CandidateApplicationsEntity.ENTITY_TYPE,
                        applicationId
                )
        );

        List<WorkflowApprStatusResponseModel> resultList = new ArrayList<>();
        boolean schedulingGroupProcessed = false;

        for (WorkflowApprovalDTO dto : sourceData) {
                resultList.add(mapToResponse(dto, dto.getStatus()));
        }
        return resultList;
    }



    private WorkflowApprStatusResponseModel mapToResponse(WorkflowApprovalDTO dto, String displayStatus) {
        return WorkflowApprStatusResponseModel.builder()
                .id(dto.getId())
                .actionDate(dto.getActionDate())
                .status(displayStatus)
                .build();
    }

    public List<WorkflowApprovalDTO> getReqApprovalHistoryByRequisitionId(UUID requisitionId) {
        return workflowApprovalMapper.toDTOs(
                workflowApprovalEntityRepository.findByEntityTypeAndEntityIdOrderByActionDateDesc(
                        JobRequisitionsEntity.ENTITY_TYPE,
                        requisitionId
                )
        );
    }
    public List<WorkflowApprovalDTO> getPanelApprovalHistory(UUID panelId) {
        return workflowApprovalMapper.toDTOs(
                workflowApprovalEntityRepository.findByEntityTypeAndEntityIdOrderByActionDateDesc(
                        PositionPanelEntity.ENTITY_TYPE,
                        panelId
                )
        );
    }

    public List<WorkflowApprovalDTO> getConversationThreadApprovalHistory(UUID conversationThreadId) {
        return workflowApprovalMapper.toDTOs(
                workflowApprovalEntityRepository.findByEntityTypeAndEntityIdOrderByActionDateDesc(
                        ConversationThreadsEntity.ENTITY_TYPE,
                        conversationThreadId
                )
        );
    }

    /**
     * Task 1: Approval history for a specific edit draft (by draft id).
     */
    public List<WorkflowApprovalDTO> getDraftRequisitionApprovalHistory(UUID draftId) {
        return workflowApprovalMapper.toDTOs(
                workflowApprovalEntityRepository.findByEntityTypeAndEntityIdOrderByActionDateDesc(
                        JobRequisitionEditRequestEntity.ENTITY_TYPE,
                        draftId
                )
        );
    }

    /**
     * Task 2: Combined approval history for a main requisition including all its published drafts,
     * sorted by action date descending.
     *
     * Includes:
     *  - Main req workflow rows  (entity_type = job_requisitions, entity_id = requisitionId)
     *  - Workflow rows from every PUBLISHED edit draft for that req
     *    (entity_type = job_requisition_edit_requests, entity_id IN [published draft ids])
     */
    public List<WorkflowApprovalDTO> getRequisitionApprovalHistoryIncludingDrafts(UUID requisitionId) {
        List<UUID> publishedDraftIds = jobRequisitionEditRequestRepository
                .findAllByParentRequisitionIdAndRequisitionStatus(requisitionId, RequisitionEditStatus.PUBLISHED)
                .stream()
                .map(d -> d.getId())
                .collect(Collectors.toList());

        List<WorkflowApprovalDTO> mainHistory = workflowApprovalMapper.toDTOs(
                workflowApprovalEntityRepository.findByEntityTypeAndEntityIdOrderByActionDateDesc(
                        JobRequisitionsEntity.ENTITY_TYPE, requisitionId));

        List<WorkflowApprovalDTO> draftHistory = publishedDraftIds.isEmpty()
                ? Collections.emptyList()
                : workflowApprovalMapper.toDTOs(
                        workflowApprovalEntityRepository.findByEntityTypeAndEntityIdInOrderByActionDateDesc(
                                JobRequisitionEditRequestEntity.ENTITY_TYPE, publishedDraftIds));

        return Stream.concat(mainHistory.stream(), draftHistory.stream())
                .sorted(Comparator.comparing(WorkflowApprovalDTO::getActionDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

}
