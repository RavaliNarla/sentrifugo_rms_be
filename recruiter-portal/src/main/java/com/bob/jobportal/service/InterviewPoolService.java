package com.bob.jobportal.service;

import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.db.dto.PanelMembersScoreDTO;
import com.bob.db.dto.UserDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.CandidateApplicationStatus;
import com.bob.db.enums.DocumentCode;
import com.bob.db.enums.InterviewSchedulingStatus;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import com.bob.jobportal.model.InterviewedCandidateRequestModel;
import com.bob.jobportal.model.InterviewedCandidateResponseModel;
import com.bob.jobportal.model.PanelMemberScoreResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class InterviewPoolService {

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private InterviewScheduleMapper interviewScheduleMapper;

    @Autowired
    private PanelMembersScoreRepository panelMembersScoreRepository;

    @Autowired
    private InterviewPanelMembersRepository interviewPanelMembersRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PanelMembersScoreMapper panelMembersScoreMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CandidateApplicationsMapper candidateApplicationsMapper;

    @Autowired
    private InterviewPanelsRepository interviewPanelsRepository;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private InterviewPanelsMapper interviewPanelsMapper;

    @Autowired
    private InterviewCentresMapper interviewCentresMapper;

    @Autowired
    private InterviewPanelScheduleConfigurationRepository interviewPanelScheduleConfigurationRepository;

    @Autowired
    private InterviewPanelScheduleConfigurationMapper interviewPanelScheduleConfigurationMapper;

    @Transactional
    public Page<InterviewedCandidateResponseModel> getInterviewedCandidates(InterviewedCandidateRequestModel requestModel){
        Pageable pageable= PageRequest.of(requestModel.getPage(),requestModel.getSize());
        Page<InterviewScheduleEntity> interviewScheduleEntityPage=interviewScheduleRepository.
                findInterviewSchedulingsWithFilters(requestModel.getSearchText(),
                                                    requestModel.getStatusList(),
                                                    requestModel.getPositionIds(),
                                                    pageable);
        List<UUID> candidateIds = interviewScheduleEntityPage.getContent().stream().map(InterviewScheduleEntity::getCandidateId).toList();
        List<UUID> applicationIds = interviewScheduleEntityPage.getContent().stream().map(InterviewScheduleEntity::getApplicationId).toList();
        List<UUID> panelIds=interviewScheduleEntityPage.getContent().stream().map(InterviewScheduleEntity::getPanelId).toList();
        List<UUID> centerIds=interviewScheduleEntityPage.getContent().stream().map(InterviewScheduleEntity::getZonalOfficeId).toList();

        //Map for cand
        Map<UUID, CandidateProfileEntity> profileMap = candidateProfileRepository.findAllByCandidateIdIn(candidateIds)
                .stream().collect(Collectors.toMap(CandidateProfileEntity::getCandidateId, p -> p));

        //Map for CandApp(id) and CandApp
        Map<UUID, CandidateApplicationsEntity> appMap = candidateApplicationsRepository.findAllByIdIn(applicationIds)
                .stream().collect(Collectors.toMap(CandidateApplicationsEntity::getId, a -> a));

        //Map panelId to panel
        Map<UUID,InterviewPanelsEntity> panelMap=interviewPanelsRepository.findAllById(panelIds).
                                stream().collect(Collectors.toMap(InterviewPanelsEntity::getId, a -> a));

        //Map centerId to centers
        Map<UUID,InterviewCentresEntity> centresEntityMap=interviewCentresRepository.findAllById(centerIds).
                stream().collect(Collectors.toMap(InterviewCentresEntity::getId, a -> a));
        //Map appId with config
        Map<UUID, List<InterviewPanelScheduleConfigurationEntity>> scheduleConfigurationEntityMap =
                interviewPanelScheduleConfigurationRepository.findByApplicationIdIn(applicationIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                InterviewPanelScheduleConfigurationEntity::getApplicationId
                        ));
        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode(DocumentCode.RESUME.toString());

        List<CandidateDocumentStoreEntity> documentStoreEntities = candidateDocumentStoreRepository.findAllByDocumentIdAndCandidateIdIn(documentTypes.getId(), candidateIds);

        Map<UUID, CandidateDocumentStoreEntity> mappedDocumentStore= documentStoreEntities.stream()
                .collect(Collectors.toMap(CandidateDocumentStoreEntity::getCandidateId, Function.identity()));


        return interviewScheduleEntityPage.map(interviewScheduleEntity -> {
            InterviewedCandidateResponseModel responseModel=InterviewedCandidateResponseModel.builder()
                    .application(candidateApplicationsMapper.toDTO(appMap.get(interviewScheduleEntity.getApplicationId())))
                    .fullName(commonUtilityProvider.buildFullName(profileMap.get(interviewScheduleEntity.getCandidateId())))
                    .interviewSchedules(interviewScheduleMapper.toDto(interviewScheduleEntity))
                    .resumeUrl(mappedDocumentStore.get(interviewScheduleEntity.getCandidateId()).getFileUrl())
                    .panel(interviewPanelsMapper.toDtoWithoutChildren(panelMap.get(interviewScheduleEntity.getPanelId())))
                    .center(interviewCentresMapper.toDto(centresEntityMap.get(interviewScheduleEntity.getZonalOfficeId())))
                    .panelScheduleConfiguration(interviewPanelScheduleConfigurationMapper.toDtoList(scheduleConfigurationEntityMap
                                    .get(interviewScheduleEntity.getApplicationId())))
                    .build();
            return responseModel;
        });

    }


    @Transactional
    public List<PanelMemberScoreResponseModel> getIndividualPanelScores(UUID scheduledInterviewId) {
        List<PanelMembersScoreEntity> panelMembersScoreEntities=panelMembersScoreRepository.findAllByScheduledInterviewIdIn(List.of(scheduledInterviewId));
        List<UUID> panelMemberIds=panelMembersScoreEntities.stream().map(PanelMembersScoreEntity::getPanelMemberId).toList();
        Map<UUID,UserEntity> userEntityMap=userRepository.findAllById(panelMemberIds).stream().collect(Collectors.toMap(UserEntity::getId,u->u));
        List<PanelMemberScoreResponseModel> responseModels=panelMembersScoreEntities
                .stream().map(panelMembersScoreEntity -> {
            PanelMemberScoreResponseModel panelMemberScoreResponseModel=PanelMemberScoreResponseModel.builder()
                    .panelMembersScore(panelMembersScoreMapper.toDto(panelMembersScoreEntity))
                    .user(userMapper.toDTO(userEntityMap.get(panelMembersScoreEntity.getPanelMemberId())))
                    .build();
            return panelMemberScoreResponseModel;
    }).toList();
        return responseModels;
     }
}
