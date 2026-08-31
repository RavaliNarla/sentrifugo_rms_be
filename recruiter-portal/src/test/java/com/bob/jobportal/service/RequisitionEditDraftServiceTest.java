package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.entity.JobPositionEditRequestEntity;
import com.bob.db.entity.JobRequisitionEditRequestEntity;
import com.bob.db.entity.JobRequisitionsEntity;
import com.bob.db.entity.RequisitionApproversEntity;
import com.bob.db.entity.WorkflowApprovalEntity;
import com.bob.db.enums.RequisitionEditStatus;
import com.bob.db.enums.RequisitionStatus;
import com.bob.db.repository.JobPositionEditRequestRepository;
import com.bob.db.repository.JobPositionsHistoryRepository;
import com.bob.db.repository.JobRequisitionEditRequestRepository;
import com.bob.db.repository.JobRequisitionHistoryRepository;
import com.bob.db.repository.JobRequisitionsRepository;
import com.bob.db.repository.PositionCategoryDistributionHistoryRepository;
import com.bob.db.repository.PositionCategoryNationalDistributionHistoryRepository;
import com.bob.db.repository.PositionStateDistributionHistoryRepository;
import com.bob.db.repository.PositionsRepository;
import com.bob.db.repository.RequisitionApproversRepository;
import com.bob.db.repository.WorkflowApprovalEntityRepository;
import com.bob.jobportal.config.RequisitionEditFeatureProperties;
import com.bob.jobportal.model.ApproveRequisitionEditDraftRequestModel;
import com.bob.jobportal.model.CancelRequisitionEditDraftRequestModel;
import com.bob.jobportal.model.RequisitionEditDraftResponseModel;
import com.bob.jobportal.model.SubmitRequisitionEditDraftRequestModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for {@link RequisitionEditDraftService} covering the create, submit (direct/workflow),
 * approve, publish, and cancel paths and the most important guards. Repositories are mocked; no Spring
 * context is started.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequisitionEditDraftServiceTest {

    @Mock private JobRequisitionsRepository jobRequisitionsRepository;
    @Mock private JobRequisitionEditRequestRepository jobRequisitionEditRequestRepository;
    @Mock private JobPositionEditRequestRepository jobPositionEditRequestRepository;
    @Mock private PositionsRepository positionsRepository;
    @Mock private JobRequisitionHistoryRepository jobRequisitionHistoryRepository;
    @Mock private JobPositionsHistoryRepository jobPositionsHistoryRepository;
    @Mock private PositionStateDistributionHistoryRepository positionStateDistributionHistoryRepository;
    @Mock private PositionCategoryDistributionHistoryRepository positionCategoryDistributionHistoryRepository;
    @Mock private PositionCategoryNationalDistributionHistoryRepository positionCategoryNationalDistributionHistoryRepository;
    @Mock private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;
    @Mock private RequisitionApproversRepository requisitionApproversRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private RequisitionEditFeatureProperties featureProperties;

    @InjectMocks
    private RequisitionEditDraftService service;

    private final UUID requisitionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(featureProperties.isEnabled()).thenReturn(true);
        when(featureProperties.isDirectModeEnabled()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(securityUtils.getCurrentUserRole()).thenReturn("RECRUITER");
        when(jobRequisitionEditRequestRepository.save(any(JobRequisitionEditRequestEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jobRequisitionsRepository.save(any(JobRequisitionsEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- createOrGetDraft ----------

    @Test
    void createOrGetDraft_createsNewDraft_whenNoneExists_andSetsEditMode() {
        JobRequisitionsEntity live = liveRequisition(RequisitionStatus.APPROVED, LocalDate.now().plusDays(7), false);
        when(jobRequisitionsRepository.findById(requisitionId)).thenReturn(Optional.of(live));
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.empty());
        when(jobRequisitionHistoryRepository.findLatestVersionNo(requisitionId)).thenReturn(0);

        RequisitionEditDraftResponseModel response = service.createOrGetDraft(requisitionId, null);

        assertThat(response).isNotNull();
        assertThat(response.getRequisitionStatus()).isEqualTo(RequisitionEditStatus.DRAFT);
        assertThat(live.getIsInEditMode()).isTrue();
        verify(jobRequisitionEditRequestRepository, times(1)).save(any(JobRequisitionEditRequestEntity.class));
    }

    @Test
    void createOrGetDraft_returnsExistingDraft_whenActiveDraftExists() {
        JobRequisitionsEntity live = liveRequisition(RequisitionStatus.APPROVED, LocalDate.now().plusDays(7), true);
        JobRequisitionEditRequestEntity existing = draft(RequisitionEditStatus.DRAFT);
        when(jobRequisitionsRepository.findById(requisitionId)).thenReturn(Optional.of(live));
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(existing));

        RequisitionEditDraftResponseModel response = service.createOrGetDraft(requisitionId, null);

        assertThat(response.getDraftId()).isEqualTo(existing.getId());
        verify(jobRequisitionEditRequestRepository, never()).save(any());
        verify(jobRequisitionsRepository, never()).save(any()); // edit mode already set
    }

    @Test
    void createOrGetDraft_throws_whenLiveStatusNotApproved() {
        JobRequisitionsEntity live = liveRequisition(RequisitionStatus.NEW, LocalDate.now().plusDays(7), false);
        when(jobRequisitionsRepository.findById(requisitionId)).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> service.createOrGetDraft(requisitionId, null))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("Only APPROVED");
    }

    @Test
    void createOrGetDraft_throws_whenEndDatePassed() {
        JobRequisitionsEntity live = liveRequisition(RequisitionStatus.APPROVED, LocalDate.now().minusDays(1), false);
        when(jobRequisitionsRepository.findById(requisitionId)).thenReturn(Optional.of(live));

        assertThatThrownBy(() -> service.createOrGetDraft(requisitionId, null))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("end date");
    }

    @Test
    void createOrGetDraft_throws_whenFeatureDisabled() {
        when(featureProperties.isEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.createOrGetDraft(requisitionId, null))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("disabled");
    }

    // ---------- submit (direct) ----------

    @Test
    void submitForApprovalDirect_setsApproved_andRecordsWorkflow() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.DRAFT);
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));

        SubmitRequisitionEditDraftRequestModel request = new SubmitRequisitionEditDraftRequestModel();
        request.setComments("LGTM");
        RequisitionEditDraftResponseModel response = service.submitForApprovalDirect(requisitionId, request);

        assertThat(response.getRequisitionStatus()).isEqualTo(RequisitionEditStatus.APPROVED);
        assertThat(draft.getApprovedAt()).isNotNull();
        verify(workflowApprovalEntityRepository, times(1)).save(any(WorkflowApprovalEntity.class));
    }

    @Test
    void submitForApprovalDirect_stillWorks_whenDirectModeDisabledFlag() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.DRAFT);
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));
        when(featureProperties.isDirectModeEnabled()).thenReturn(false);

        RequisitionEditDraftResponseModel response = service.submitForApprovalDirect(requisitionId, null);

        assertThat(response.getRequisitionStatus()).isEqualTo(RequisitionEditStatus.APPROVED);
        verify(workflowApprovalEntityRepository, times(1)).save(any(WorkflowApprovalEntity.class));
    }

    // ---------- submit (workflow) ----------

    @Test
    void submitForApprovalWorkflow_setsL1Pending() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.DRAFT);
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));

        RequisitionEditDraftResponseModel response = service.submitForApprovalWorkflow(requisitionId, null);

        assertThat(response.getRequisitionStatus()).isEqualTo(RequisitionEditStatus.L1_PENDING);
        assertThat(draft.getSubmittedAt()).isNotNull();
        verify(workflowApprovalEntityRepository, times(1)).save(any(WorkflowApprovalEntity.class));
    }

    // ---------- approve workflow ----------

    @Test
    void approve_l1_setsL1Approved() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.L1_PENDING);
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));
        when(requisitionApproversRepository.findByApproverId(userId)).thenReturn(Optional.of(approver(RequisitionApproversEntity.ApproverRole.L1)));

        ApproveRequisitionEditDraftRequestModel req = new ApproveRequisitionEditDraftRequestModel();
        req.setPostingStatus(RequisitionEditStatus.L2_PENDING);

        RequisitionEditDraftResponseModel response = service.approveDraftWithWorkflow(requisitionId, req);

        assertThat(response.getRequisitionStatus()).isEqualTo(RequisitionEditStatus.L2_PENDING);
    }

    @Test
    void approve_l2_autoPublishesDraft() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.L2_PENDING);
        // Two find() calls: first by approve, second by publish; both return same draft (mutated in-place)
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));
        when(requisitionApproversRepository.findByApproverId(userId)).thenReturn(Optional.of(approver(RequisitionApproversEntity.ApproverRole.L2)));

        JobRequisitionsEntity live = liveRequisition(RequisitionStatus.APPROVED, LocalDate.now().plusDays(7), true);
        live.setId(requisitionId);
        when(jobRequisitionsRepository.findByIdForUpdate(requisitionId)).thenReturn(Optional.of(live));
        when(jobRequisitionHistoryRepository.findLatestVersionNo(requisitionId)).thenReturn(1);
        when(positionsRepository.findAllByRequisitionIdForUpdate(requisitionId)).thenReturn(new ArrayList<>());

        ApproveRequisitionEditDraftRequestModel req = new ApproveRequisitionEditDraftRequestModel();
        req.setPostingStatus(RequisitionEditStatus.APPROVED);

        RequisitionEditDraftResponseModel response = service.approveDraftWithWorkflow(requisitionId, req);

        assertThat(response.getRequisitionStatus()).isEqualTo(RequisitionEditStatus.PUBLISHED);
        assertThat(response.getPublishedBy()).isEqualTo(userId);
        assertThat(response.getPublishedAt()).isNotNull();
        verify(jobRequisitionsRepository, times(1)).findByIdForUpdate(requisitionId);
        verify(positionsRepository, times(1)).findAllByRequisitionIdForUpdate(requisitionId);
    }

    @Test
    void approve_l1_rejectsRequestedNonL1Status() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.L1_PENDING);
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));
        when(requisitionApproversRepository.findByApproverId(userId)).thenReturn(Optional.of(approver(RequisitionApproversEntity.ApproverRole.L1)));

        ApproveRequisitionEditDraftRequestModel req = new ApproveRequisitionEditDraftRequestModel();
        req.setPostingStatus(RequisitionEditStatus.APPROVED);

        assertThatThrownBy(() -> service.approveDraftWithWorkflow(requisitionId, req))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("L1 approver");
    }

    // ---------- publish ----------

    @Test
    void publish_throws_whenStaleBaseVersion() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.APPROVED);
        draft.setBaseVersionNo(1);
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));

        JobRequisitionsEntity live = liveRequisition(RequisitionStatus.APPROVED, LocalDate.now().plusDays(7), true);
        live.setId(requisitionId);
        when(jobRequisitionsRepository.findByIdForUpdate(requisitionId)).thenReturn(Optional.of(live));
        when(jobRequisitionHistoryRepository.findLatestVersionNo(requisitionId)).thenReturn(5); // newer than draft base

        assertThatThrownBy(() -> service.publishApprovedDraft(requisitionId))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("stale");
    }

    @Test
    void publish_throws_whenDraftNotApproved() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.L2_PENDING);
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.publishApprovedDraft(requisitionId))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("APPROVED");
    }

    // ---------- cancel ----------

    @Test
    void cancelDraft_setsCancelled_andResetsEditMode() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.DRAFT);
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));
        JobRequisitionsEntity live = liveRequisition(RequisitionStatus.APPROVED, LocalDate.now().plusDays(7), true);
        live.setId(requisitionId);
        when(jobRequisitionsRepository.findById(requisitionId)).thenReturn(Optional.of(live));

        CancelRequisitionEditDraftRequestModel req = new CancelRequisitionEditDraftRequestModel();
        req.setComments("not needed");

        RequisitionEditDraftResponseModel response = service.cancelDraft(requisitionId, req);

        assertThat(response.getRequisitionStatus()).isEqualTo(RequisitionEditStatus.CANCELLED);
        assertThat(live.getIsInEditMode()).isFalse();
        ArgumentCaptor<JobRequisitionsEntity> liveCaptor = ArgumentCaptor.forClass(JobRequisitionsEntity.class);
        verify(jobRequisitionsRepository, times(1)).save(liveCaptor.capture());
        assertThat(liveCaptor.getValue().getIsInEditMode()).isFalse();
        verify(workflowApprovalEntityRepository, times(1)).save(any(WorkflowApprovalEntity.class));
    }

    @Test
    void cancelDraft_throws_whenAlreadyPublished() {
        JobRequisitionEditRequestEntity draft = draft(RequisitionEditStatus.PUBLISHED);
        when(jobRequisitionEditRequestRepository.findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(
                eq(requisitionId), anyCollection())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.cancelDraft(requisitionId, null))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("Published");
    }

    // ---------- helpers ----------

    private JobRequisitionsEntity liveRequisition(RequisitionStatus status, LocalDate endDate, boolean inEditMode) {
        JobRequisitionsEntity live = JobRequisitionsEntity.builder()
                .requisitionTitle("Test Req")
                .requisitionStatus(status)
                .startDate(LocalDate.now().minusDays(2))
                .endDate(endDate)
                .isInEditMode(inEditMode)
                .build();
        live.setId(requisitionId);
        live.setIsActive(true);
        return live;
    }

    private JobRequisitionEditRequestEntity draft(RequisitionEditStatus status) {
        JobRequisitionEditRequestEntity draft = JobRequisitionEditRequestEntity.builder()
                .parentRequisitionId(requisitionId)
                .requestType("EDIT")
                .requisitionTitle("Test Req")
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().plusDays(7))
                .requisitionStatus(status)
                .baseVersionNo(1)
                .positionEditRequests(new ArrayList<>())
                .build();
        draft.setId(UUID.randomUUID());
        draft.setIsActive(true);
        return draft;
    }

    private RequisitionApproversEntity approver(RequisitionApproversEntity.ApproverRole role) {
        return RequisitionApproversEntity.builder()
                .approverId(userId)
                .approverRole(role)
                .build();
    }
}
