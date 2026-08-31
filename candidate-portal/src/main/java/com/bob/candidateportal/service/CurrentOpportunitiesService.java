package com.bob.candidateportal.service;

import com.bob.candidateportal.model.ActiveJobPositionResponseModel;
import com.bob.candidateportal.model.JobPositionFilterRequestModel;
import com.bob.db.dto.*;
import com.bob.db.entity.*;
import com.bob.db.enums.PositionStatus;
import com.bob.db.enums.RequisitionStatus;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CurrentOpportunitiesService {

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private JobGradeRepository jobGradeRepository;


    @Autowired
    private DepartmentsRepository departmentsRepository;


    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private JobRequisitionMapper jobRequisitionMapper;

    @Autowired
    private JobPositionsMapper jobPositionsMapper;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private MasterPositionsMapper masterPositionsMapper;

    @Autowired
    private PositionStateDistributionRepository positionStateDistributionRepository;


    @Autowired
    private PositionStateDistributionMapper positionStateDistributionMapper;


    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidateApplicationsMapper candidateApplicationMapper;

    public List<JobRequisitionsDTO> getActiveRequisitions() {
//        List<String> statuses = List.of(DBConstants.REQ_APPROVAL_PUBLISHED, DBConstants.REQ_APPROVAL_APPROVED);

        List<RequisitionStatus> statuses=List.of(RequisitionStatus.APPROVED);
        List<JobRequisitionsEntity> jobRequisitions =
                jobRequisitionsRepository.findAllByRequisitionStatusIn(statuses);

        return jobRequisitionMapper.toDTOs(jobRequisitions);
    }

    @Transactional(readOnly = true)
    public Page<ActiveJobPositionResponseModel> getActiveJobPositions(JobPositionFilterRequestModel filter) {

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());

        if (filter.getDeptIds() != null && filter.getDeptIds().isEmpty()) {
            filter.setDeptIds(null);
        }
        if(filter.getStateIds()!=null && filter.getStateIds().isEmpty()){
            filter.setStateIds(null);
        }
        LocalDate currentDate = LocalDate.now();
        List<CandidateApplicationsEntity> appliedApplications = candidateApplicationsRepository.findByCandidateIdIgnoringPaymentStatus(filter.getCandidateId());
        List<UUID> applyedPositions =appliedApplications.stream().map(CandidateApplicationsEntity::getPositionId).toList();
        Page<JobPositionsEntity> positionPage = positionsRepository.findActiveJobPositions(
                PositionStatus.ACTIVE,
                RequisitionStatus.APPROVED,
                filter.getCandidateId(), filter.getDeptIds(),
                filter.getStateIds(), filter.getSearchText(),
                filter.getRequisitionId(),filter.getMonthMinExp(),
                filter.getMonthMaxExp(),currentDate,
                applyedPositions, pageable
        );


        List<UUID> masterIds = positionPage.stream().map(JobPositionsEntity::getMasterPositionId).toList();
        List<UUID> reqIds = positionPage.stream().map(JobPositionsEntity::getRequisitionId).toList();

        Map<UUID, MasterPositionsDTO> masterMap = masterPositionsRepository.findAllById(masterIds).stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, masterPositionsMapper::toDTO));

        Map<UUID, JobRequisitionsDTO> reqMap = jobRequisitionsRepository.findAllById(reqIds).stream()
                .collect(Collectors.toMap(JobRequisitionsEntity::getId, jobRequisitionMapper::toDTO));

        return positionPage.map(entity -> ActiveJobPositionResponseModel.builder()
                .positionsDTO(jobPositionsMapper.toDto(entity))
                .masterPositionsDTO(masterMap.get(entity.getMasterPositionId()))
                .requisitionsDTO(reqMap.get(entity.getRequisitionId()))
                .positionStateDistributions(entity.getPositionStateDistributions().stream()
                        .map(positionStateDistributionMapper::toDTO).toList())
                .build());
    }



}
