package com.bob.jobportal.service;

import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.exception.SchedulingConflictException;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.db.dto.*;
import com.bob.db.entity.*;
import com.bob.db.enums.*;
import com.bob.db.mapper.*;
import com.bob.db.repository.*;
import com.bob.jobportal.model.*;
import com.bob.jobportal.model.InterviewApprovalResponseModel;
import com.bob.jobportal.model.SchedulePoolModel;
import com.bob.jobportal.model.SchedulePoolRequestFilterModel;
import com.bob.jobportal.model.SchedulingConflictResponseModel;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SchedulePoolService {

    @Autowired
    private InterviewScheduleStagingRepository scheduleStagingRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private InterviewPanelsRepository interviewPanelsRepository;

    @Autowired
    private InterviewScheduleStagingMapper scheduleStagingMapper;

    @Autowired
    private InterviewPanelsMapper interviewPanelsMapper;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private InterviewCentresMapper interviewCentresMapper;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private CandidateApplicationsMapper candidateApplicationsMapper;

    @Autowired
    private InterviewCommitteeRepository interviewCommitteeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private MailSenderHelper mailSenderHelper;

    @Autowired
    private InterviewScheduleMapper scheduleMapper;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private DepartmentsRepository departmentsRepository;

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private InterviewSchedulingService interviewSchedulingService;

    @Autowired
    private JobPositionsMapper jobPositionsMapper;

    @Autowired
    private PositionPanelMapper positionPanelMapper;

    @Autowired
    private InterviewPanelScheduleConfigurationRepository scheduleConfigurationRepository;

    @Autowired
    private InterviewPanelScheduleConfigurationMapper scheduleConfigurationMapper;

    @Autowired
    private InterviewScheduleStagingHistoryMapper interviewScheduleStagingHistoryMapper;

    @Autowired
    private InterviewScheduleStagingHistoryRepository interviewScheduleStagingHistoryRepository;

    @Autowired
    private InterviewScheduleStagingMapper interviewScheduleStagingMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @Transactional(readOnly = true)
    public Page<SchedulePoolModel> getSchedulePoolCandidates(SchedulePoolRequestFilterModel requestModel) {
        Pageable pageable= PageRequest.of(requestModel.getPage(),requestModel.getSize());
        Specification<InterviewScheduleStagingEntity> specification=buildSpecificationWithFilters(requestModel.getSearchText(), requestModel.getPositionIds(),requestModel.getStatusList());
        Page<InterviewScheduleStagingEntity> stagingEntityPage=scheduleStagingRepository.findAll(specification,pageable);
        List<InterviewScheduleStagingEntity> stagingEntities=stagingEntityPage.getContent();
        Set<UUID> panelIds= stagingEntities.stream().map(i->i.getPanelId()).collect(Collectors.toSet());
        Set<UUID> applicationIds=stagingEntities.stream().map(i->i.getApplication().getId()).collect(Collectors.toSet());
        Map<UUID, InterviewPanelsEntity> interviewPanelsEntityMap=interviewPanelsRepository.findAllById(panelIds)
                .stream().collect(Collectors.toMap(InterviewPanelsEntity::getId,i->i));
        Map<UUID, InterviewCentresEntity> interviewCentresEntityMap=interviewCentresRepository.findAllById(stagingEntities.stream().map(i->i.getZonalOfficeId()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(InterviewCentresEntity::getId,i->i));
        Map<UUID,UserEntity> userEntityMap=getAllUsersForPanels(interviewPanelsEntityMap.values().stream().toList());
        Map<UUID,InterviewCommitteeEntity> committeeEntityMap=getAllCommitteesForPanels(interviewPanelsEntityMap.values().stream().toList());
        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode(DocumentCode.RESUME.toString());
        List<UUID> candidateIds=stagingEntities.stream().map(i->i.getCandidateId()).toList();
        List<CandidateDocumentStoreEntity> documentStoreEntities = candidateDocumentStoreRepository.findAllByDocumentIdAndCandidateIdIn(documentTypes.getId(), candidateIds);
        Map<UUID, CandidateDocumentStoreEntity> mappedDocumentStore= documentStoreEntities.stream()
                .collect(Collectors.toMap(CandidateDocumentStoreEntity::getCandidateId, Function.identity()));
        Map<UUID, List<InterviewPanelScheduleConfigurationEntity>> scheduleConfigurationEntityMap =
                scheduleConfigurationRepository.findByApplicationIdIn(applicationIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                InterviewPanelScheduleConfigurationEntity::getApplicationId
                        ));
        return stagingEntityPage.map(entity -> {
            return SchedulePoolModel.builder()
                    .fullName(commonUtilityProvider.buildFullName(entity.getCandidateProfile()))
                    .interviewScheduleStaging(scheduleStagingMapper.toDto(entity))
                    .application(candidateApplicationsMapper.toDTO(entity.getApplication()))
                    .interviewPanels(interviewPanelsMapper.toDto(interviewPanelsEntityMap.get(entity.getPanelId()),userEntityMap,committeeEntityMap))
                    .interviewCentres(interviewCentresMapper.toDto(interviewCentresEntityMap.get(entity.getZonalOfficeId())))
                    .resumeUrl(mappedDocumentStore.get(entity.getCandidateId()).getFileUrl())
                    .panelScheduleConfigurations(scheduleConfigurationMapper.toDtoList(scheduleConfigurationEntityMap.get(entity.getApplication().getId())))
                    .build();
        });
    }
    @Transactional(readOnly = true)
    public List<SchedulePoolModel> getSchedulePoolCandidatesList(SchedulePoolRequestFilterModel requestModel) {
//        Pageable pageable= PageRequest.of(requestModel.getPage(),requestModel.getSize());
        Specification<InterviewScheduleStagingEntity> specification=buildSpecificationWithFilters(requestModel.getSearchText(), requestModel.getPositionIds(),requestModel.getStatusList());
        List<InterviewScheduleStagingEntity> stagingEntities=scheduleStagingRepository.findAll(specification);
        Set<UUID> panelIds= stagingEntities.stream().map(i->i.getPanelId()).collect(Collectors.toSet());
        Set<UUID> applicationIds=stagingEntities.stream().map(i->i.getApplication().getId()).collect(Collectors.toSet());
        Map<UUID, InterviewPanelsEntity> interviewPanelsEntityMap=interviewPanelsRepository.findAllById(panelIds)
                .stream().collect(Collectors.toMap(InterviewPanelsEntity::getId,i->i));
        Map<UUID, InterviewCentresEntity> interviewCentresEntityMap=interviewCentresRepository.findAllById(stagingEntities.stream().map(i->i.getZonalOfficeId()).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(InterviewCentresEntity::getId,i->i));
        Map<UUID,UserEntity> userEntityMap=getAllUsersForPanels(interviewPanelsEntityMap.values().stream().toList());
        Map<UUID,InterviewCommitteeEntity> committeeEntityMap=getAllCommitteesForPanels(interviewPanelsEntityMap.values().stream().toList());
        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode(DocumentCode.RESUME.toString());
        List<UUID> candidateIds=stagingEntities.stream().map(i->i.getCandidateId()).toList();
        List<CandidateDocumentStoreEntity> documentStoreEntities = candidateDocumentStoreRepository.findAllByDocumentIdAndCandidateIdIn(documentTypes.getId(), candidateIds);
        Map<UUID, CandidateDocumentStoreEntity> mappedDocumentStore= documentStoreEntities.stream()
                .collect(Collectors.toMap(CandidateDocumentStoreEntity::getCandidateId, Function.identity()));
        Map<UUID, List<InterviewPanelScheduleConfigurationEntity>> scheduleConfigurationEntityMap =
                scheduleConfigurationRepository.findByApplicationIdIn(applicationIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                InterviewPanelScheduleConfigurationEntity::getApplicationId
                        ));
        Map<UUID, Integer> positionOrderMap = new HashMap<>();
        List<UUID> requestedPositionIds = requestModel.getPositionIds();
        if (requestedPositionIds != null) {
            for (int i = 0; i < requestedPositionIds.size(); i++) {
                positionOrderMap.put(
                        requestedPositionIds.get(i),
                        i
                );
            }
        }
        return stagingEntities.stream().map(entity -> {
            return SchedulePoolModel.builder()
                    .fullName(commonUtilityProvider.buildFullName(entity.getCandidateProfile()))
                    .interviewScheduleStaging(scheduleStagingMapper.toDto(entity))
                    .application(candidateApplicationsMapper.toDTO(entity.getApplication()))
                    .interviewPanels(interviewPanelsMapper.toDto(interviewPanelsEntityMap.get(entity.getPanelId()),userEntityMap,committeeEntityMap))
                    .interviewCentres(interviewCentresMapper.toDto(interviewCentresEntityMap.get(entity.getZonalOfficeId())))
                    .resumeUrl(mappedDocumentStore.get(entity.getCandidateId()).getFileUrl())
                    .panelScheduleConfigurations(scheduleConfigurationMapper.toDtoList(scheduleConfigurationEntityMap.get(entity.getApplication().getId())))
                    .build();
        }).sorted(
                Comparator

                        // 1. requested positionIds order
                        .comparingInt(
                                (SchedulePoolModel r) ->
                                        positionOrderMap.getOrDefault(
                                                r.getApplication().getPositionId(),
                                                Integer.MAX_VALUE
                                        )
                        )
                        // 2. zone name
//                        .thenComparing(
//                                r -> r.getInterviewCentres().getDisplayName(),
//                                Comparator.nullsLast(
//                                        String.CASE_INSENSITIVE_ORDER
//                                )
//                        )
                        //3. Panel name
//                        .thenComparing(
//                                r->r.getInterviewPanels().getPanelName(),
//                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
//                        )
                        // 3. interview start time
                        .thenComparing(
                                r -> r.getInterviewScheduleStaging()
                                        .getInterviewStartAt(),
                                Comparator.nullsLast(
                                        LocalDateTime::compareTo
                                )
                        )
        ).toList();
    }

    public Map<UUID, UserEntity> getAllUsersForPanels(List<InterviewPanelsEntity> interviewPanelsEntities) {
        Set<UUID> allUserIds = interviewPanelsEntities.stream()
                .flatMap(panel -> panel.getPanelMembers().stream())
                .map(InterviewPanelMembersEntity::getPanelMember)
                .filter(Objects::nonNull)
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
        return userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));
    }

    public Map<UUID, InterviewCommitteeEntity> getAllCommitteesForPanels(List<InterviewPanelsEntity> interviewPanelsEntities) {
        Set<UUID> allCommitteeIds = interviewPanelsEntities.stream()
                .map(InterviewPanelsEntity::getCommittee)
                .filter(Objects::nonNull)
                .map(InterviewCommitteeEntity::getId)
                .collect(Collectors.toSet());
        return interviewCommitteeRepository.findAllById(allCommitteeIds).stream()
                .collect(Collectors.toMap(InterviewCommitteeEntity::getId, committee -> committee));
    }

    private Specification buildSpecificationWithFilters(String searchText, List<UUID> positionIds, List<InterviewSchedulingApprovalStatus> interviewSchedulingApprovalStatuses) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            if (interviewSchedulingApprovalStatuses != null && !interviewSchedulingApprovalStatuses.isEmpty()) {
                predicates.add(root.get("interviewSchedulingApprovalStatus").in(interviewSchedulingApprovalStatuses));
            }
            if(positionIds!=null && !positionIds.isEmpty()){
                predicates.add(root.get("application").get("positionId").in(positionIds));
            }
            if (searchText != null && !searchText.trim().isEmpty()) {
                String likePattern = "%" + searchText.toLowerCase() + "%";
                Predicate firstNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("candidateProfile").get("firstName")), likePattern);
                Predicate middleNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("candidateProfile").get("middleName")), likePattern);
                Predicate lastNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("candidateProfile").get("lastName")), likePattern);
                predicates.add(criteriaBuilder.or(firstNamePredicate,middleNamePredicate, lastNamePredicate));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public void createHistory(List<InterviewScheduleStagingEntity> stagingEntities ){
        UUID batchId = UUID.randomUUID();
        List<InterviewScheduleStagingHistoryEntity> historyEntities=stagingEntities.stream().map(entity->{
            return InterviewScheduleStagingHistoryEntity.builder()
                    .applicationId(entity.getApplication().getId())
                    .candidateId(entity.getCandidateId())
                    .panelId(entity.getPanelId())
                    .zonalOfficeId(entity.getZonalOfficeId())
                    .interviewStartAt(entity.getInterviewStartAt())
                    .interviewEndAt(entity.getInterviewEndAt())
                    .interviewDurationMinutes(entity.getInterviewDurationMinutes())
                    .interviewSchedulingApprovalStatus(entity.getInterviewSchedulingApprovalStatus())
                    .remarks(entity.getRemarks())
                    .rescheduled(entity.isRescheduled())
                    .batchId(batchId).build();
        }).toList();
        interviewScheduleStagingHistoryRepository.saveAll(historyEntities);
    }

    @Transactional
    public List<InterviewScheduleDTO> submitForl1Approval(List<UUID> positionIds, InterviewSchedulingApprovalStatus status,String remarks) throws MessagingException, IOException {
        List<CandidateApplicationsEntity> candidateApplicationsEntityList=candidateApplicationsRepository.findByPositionIdIn(positionIds);
        List<UUID> applicationIds=candidateApplicationsEntityList.stream().map(CandidateApplicationsEntity::getId).toList();
        List<InterviewScheduleStagingEntity> stagingEntities=scheduleStagingRepository.findByApplicationIdInAndInterviewSchedulingApprovalStatusIn(applicationIds,List.of(InterviewSchedulingApprovalStatus.L1_PENDING));

        if (status.equals(InterviewSchedulingApprovalStatus.REJECTED)) {

            stagingEntities.forEach(i -> {
                i.setInterviewSchedulingApprovalStatus(
                        InterviewSchedulingApprovalStatus.REJECTED
                );
                i.setRemarks(remarks);
            });

            List<SchedulingConflictResponseModel> errors=interviewSchedulingService.validateSchedulingConflicts(stagingEntities);

            if(!errors.isEmpty()){
                throw new SchedulingConflictException("Failed to approve the Schedulings",errors);
            }

            scheduleStagingRepository.saveAll(stagingEntities);
            createHistory(stagingEntities);
            return null;
        }
        //schedule it with applicationId with schedule
        Map<UUID,InterviewScheduleEntity> scheduleEntityMap=interviewScheduleRepository.findByApplicationIdIn(applicationIds).stream()
                .collect(Collectors.toMap(InterviewScheduleEntity::getApplicationId,i->i));
        Map<UUID,CandidateApplicationsEntity> candidateApplicationsEntityMap=candidateApplicationsRepository.findAllById(applicationIds).stream()
                .collect(Collectors.toMap(CandidateApplicationsEntity::getId,i->i));



        //For audit
        List<InterviewScheduleEntity> scheduleEntities=new ArrayList<>();
        List<CandidateApplicationsEntity> newAppEntities=new ArrayList<>();
        // For saved entities
        List<InterviewScheduleEntity> finalSchedules=new ArrayList<>();
        List<CandidateApplicationsEntity> finalApplications=new ArrayList<>();
        for(InterviewScheduleStagingEntity stagingEntity:stagingEntities){
            InterviewScheduleEntity newSchedEntity=InterviewScheduleEntity.builder()
                    .applicationId(stagingEntity.getApplication().getId())
                    .candidateId(stagingEntity.getCandidateId())
                    .panelId((stagingEntity.getPanelId()))
                    .zonalOfficeId(stagingEntity.getZonalOfficeId())
                    .interviewStartAt(stagingEntity.getInterviewStartAt())
                    .interviewEndAt(stagingEntity.getInterviewEndAt())
                    .interviewDurationMinutes(stagingEntity.getInterviewDurationMinutes())
                    .build();

            CandidateApplicationsEntity newApplications=candidateApplicationsEntityMap.get(stagingEntity.getApplication().getId());
            stagingEntity.setInterviewSchedulingApprovalStatus(InterviewSchedulingApprovalStatus.APPROVED);
            stagingEntity.setRemarks(remarks);
            if(stagingEntity.isRescheduled()){
                newSchedEntity.setInterviewStatus(InterviewSchedulingStatus.RESCHEDULED);
                newApplications.setApplicationStatus(CandidateApplicationStatus.RESCHEDULED);
            }
            else{
                newSchedEntity.setInterviewStatus(InterviewSchedulingStatus.SCHEDULED);
                newApplications.setApplicationStatus(CandidateApplicationStatus.SCHEDULED);
            }
            InterviewScheduleEntity scheduleForApplication=null;
            if(scheduleEntityMap.get(stagingEntity.getApplication().getId())!=null){
                scheduleForApplication=scheduleEntityMap.get(stagingEntity.getApplication().getId());
                newSchedEntity.setId(scheduleForApplication.getId());
            }
            scheduleEntities.add(newSchedEntity);
            newAppEntities.add(newApplications);
            finalSchedules.add(interviewScheduleRepository.saveWithAudit(scheduleForApplication,newSchedEntity));
        }
        scheduleStagingRepository.saveAll(stagingEntities);
        createHistory(stagingEntities);
        finalApplications.addAll(candidateApplicationsRepository.saveAllWithWorkflow(newAppEntities));
        entityManager.flush();
        if(finalSchedules.size()==stagingEntities.size()) {
            mailSenderHelper.sendMailToCandidateAndRecruiters(finalSchedules, finalApplications);
        }
        return scheduleMapper.toDtoList(finalSchedules);
    }

    @Transactional(readOnly = true)
    public List<InterviewSchedulePoolApprovalResponse> getInterviewApproval(UUID requisitionId) {
        List<JobPositionsEntity> jobPositionsEntities = positionsRepository.findAllByRequisitionId(requisitionId);
        List<UUID> positionIds = jobPositionsEntities.stream().map(JobPositionsEntity::getId).toList();
        List<CandidateApplicationsEntity> candidateApplicationsEntities = candidateApplicationsRepository.findByPositionIdIn(positionIds);
        List<UUID> allApplicationIds = candidateApplicationsEntities.stream().map(CandidateApplicationsEntity::getId).toList();
        List<InterviewScheduleStagingEntity> stagingEntities = scheduleStagingRepository.findByApplicationIdIn(allApplicationIds);
        List<UUID> panelIds=stagingEntities.stream().map(InterviewScheduleStagingEntity::getPanelId).toList();
        List<PositionPanelEntity> positionPanelEntities=positionPanelRepository.findByJobPositionIdIn(positionIds)
                .stream().filter(positionPanelEntity -> panelIds.contains(positionPanelEntity.getInterviewPanel().getId()) ).toList();
        Map<UUID, InterviewScheduleStagingEntity> stagingEntityMapWithApplicationId =
                stagingEntities.stream().
                        filter(item -> item.getInterviewSchedulingApprovalStatus().equals(InterviewSchedulingApprovalStatus.L1_PENDING) )
                        .collect(Collectors.toMap(
                                i -> i.getApplication().getId(),
                                i -> i,
                                (existing, replacement) -> existing
                        ));

        Map<UUID, List<UUID>> candidateApplicationsIdsWithPositionIdMap =candidateApplicationsEntities.stream().collect(Collectors.groupingBy(
                CandidateApplicationsEntity::getPositionId,
                Collectors.mapping(CandidateApplicationsEntity::getId,Collectors.toList())));


        /*
         * positionId -> stagingEntity
         */
        Map<UUID, List<InterviewScheduleStagingEntity>> stagingEntityMapWithPositionId =
                candidateApplicationsIdsWithPositionIdMap.entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue()
                                        .stream()
                                        .map(stagingEntityMapWithApplicationId::get)
                                        .filter(Objects::nonNull)
                                        .toList()
                        ));

        Map<UUID, List<UUID>> positionIdWithPanelIds =
                stagingEntityMapWithPositionId.entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue()
                                        .stream()
                                        .map(InterviewScheduleStagingEntity::getPanelId)
                                        .filter(Objects::nonNull)
                                        .distinct()
                                        .toList()
                        ));

        /*
         * positionId -> positionPannel
         *
         */

        Map<UUID,PositionPanelEntity> panelEntityMap=positionPanelEntities.stream().collect(Collectors.toMap(
                i->i.getInterviewPanel().getId(),
                i->i,
                (existing,replacement)->existing));

        /*
         * positionId -> pannelEntity
         */
        Map<UUID, List<InterviewPanelsEntity>> positionInterviewPanelsMap =
                positionPanelEntities.stream()
                        .filter(p->positionIdWithPanelIds.get(p.getJobPosition().getId()).contains(p.getInterviewPanel().getId()))
                        .collect(Collectors.groupingBy(
                                positionPanel -> positionPanel.getJobPosition().getId(),
                                Collectors.mapping(
                                        PositionPanelEntity::getInterviewPanel,
                                        Collectors.toList()
                                )
                        ));

        Map<UUID, InterviewApprovalResponseModel> approvalResponse =
                jobPositionsEntities.stream()
                        .filter(position ->
                                stagingEntityMapWithPositionId.get(position.getId()) != null
                                        && !stagingEntityMapWithPositionId.get(position.getId()).isEmpty()
                        )
                        .collect(Collectors.toMap(
                                JobPositionsEntity::getId,
                                position -> InterviewApprovalResponseModel.builder()
                                        .totalCandidateCount(
                                                stagingEntityMapWithPositionId.get(position.getId()) != null
                                                        ? stagingEntityMapWithPositionId.get(position.getId()).size()
                                                        : 0
                                        )
                                        .status(
                                                stagingEntityMapWithPositionId.get(position.getId()) != null &&
                                                        !stagingEntityMapWithPositionId.get(position.getId()).isEmpty()
                                                        ? stagingEntityMapWithPositionId.get(position.getId())
                                                        .get(0)
                                                        .getInterviewSchedulingApprovalStatus()
                                                        : null
                                        )
                                        .totalPanelCount(
                                                stagingEntityMapWithPositionId.get(position.getId()) != null
                                                        ? stagingEntityMapWithPositionId.get(position.getId())
                                                        .stream()
                                                        .map(InterviewScheduleStagingEntity::getPanelId)
                                                        .filter(Objects::nonNull)
                                                        .collect(Collectors.toSet())
                                                        .size()
                                                        : 0

                                        )
                                        .totalZonalCount(
                                                stagingEntityMapWithPositionId.get(position.getId()) != null
                                                        ? stagingEntityMapWithPositionId.get(position.getId())
                                                        .stream()
                                                        .map(InterviewScheduleStagingEntity::getZonalOfficeId)
                                                        .filter(Objects::nonNull)
                                                        .collect(Collectors.toSet())
                                                        .size()
                                                        : 0
                                        )
                                        .panelData(
                                                buildPanelData(
                                                        positionInterviewPanelsMap.get(position.getId()) != null
                                                                ? positionInterviewPanelsMap.get(position.getId())
                                                                : List.of(),
                                                        panelEntityMap
                                                )
                                        )
                                        .zonalData(
                                                buildZonalData(
                                                        stagingEntityMapWithPositionId.get(position.getId()) != null
                                                                ? stagingEntityMapWithPositionId.get(position.getId())
                                                                : List.of()
                                                )
                                        )
                                        .isHistory(false)
                                        .build()
                        ));

        Map<UUID,List<InterviewApprovalResponseModel>> historyApprovalResponse=getApprovalHistory(requisitionId);

        approvalResponse.entrySet().removeIf(entry -> entry.getValue() == null);

        List<UUID> positionIdsWithData = new ArrayList<>();
        positionIdsWithData.addAll(approvalResponse.keySet());
        positionIdsWithData.addAll(historyApprovalResponse.keySet());

        return jobPositionsEntities.stream()
                .filter(position -> positionIdsWithData.contains(position.getId()))
        .map(jobPositionsEntity -> {
            return InterviewSchedulePoolApprovalResponse.builder()
                    .jobPosition(jobPositionsMapper.toDto(jobPositionsEntity))
                    .interviewApprovalDetails(approvalResponse.get(jobPositionsEntity.getId()))
                    .interviewApprovalHistory(historyApprovalResponse.get(jobPositionsEntity.getId()))
                    .build();
        }).toList();
    }

     private List<InterviewApprovalResponseModel.ZonalData> buildZonalData(List<InterviewScheduleStagingEntity> stagingEntities){
        Map<UUID, List<InterviewScheduleStagingEntity>> zonalOfficeIdToStagingEntitiesMap=stagingEntities.stream()
                .filter(i->i.getZonalOfficeId()!=null)
                .collect(Collectors.groupingBy(InterviewScheduleStagingEntity::getZonalOfficeId));
        return zonalOfficeIdToStagingEntitiesMap.entrySet().stream().map(entry->{
            return InterviewApprovalResponseModel.ZonalData.builder()
                    .zonalId(entry.getKey())
                    .zoneName(interviewCentresRepository.findById(entry.getKey()).map(InterviewCentresEntity::getDisplayName).orElse("Unknown Zone"))
                    .candidateCount(entry.getValue().size())
                    .build();
        }).toList();
     }
     private List<InterviewApprovalResponseModel.PanelData> buildPanelData(List<InterviewPanelsEntity> interviewPanelsEntities,Map<UUID,PositionPanelEntity> panelEntityMap){
         return interviewPanelsEntities.stream().map( entry ->{
                     List<UserDTO> userList = entry.getPanelMembers()
                             .stream()
                             .map(InterviewPanelMembersEntity::getPanelMember)
                             .map(userMapper::toDTO)
                             .toList();
                     PositionPanelEntity positionPanel = panelEntityMap.get(entry.getId());
             return InterviewApprovalResponseModel.PanelData.builder()
                     .panelId(entry.getId())
                     .panelName(entry.getPanelName())
                     .members(userList)
                     .startDate(positionPanel != null ? positionPanel.getStartDate() : null)
                     .endDate(positionPanel != null ? positionPanel.getEndDate() : null)
                     .build();

         }
         ).toList();
     }


    private List<InterviewApprovalResponseModel.ZonalData> buildZonalDataHistory(List<InterviewScheduleStagingHistoryEntity> stagingEntities,Map<UUID,InterviewCentresEntity> zonalMap){
        Map<UUID, List<InterviewScheduleStagingHistoryEntity>> zonalOfficeIdToStagingEntitiesMap=stagingEntities.stream()
                .filter(i->i.getZonalOfficeId()!=null)
                .collect(Collectors.groupingBy(InterviewScheduleStagingHistoryEntity::getZonalOfficeId));
        return zonalOfficeIdToStagingEntitiesMap.entrySet().stream().map(entry->{
            return InterviewApprovalResponseModel.ZonalData.builder()
                    .zonalId(entry.getKey())
                    .zoneName(zonalMap.get(entry.getKey()) != null
                            ? zonalMap.get(entry.getKey()).getDisplayName()
                            : "Unknown Zone")
                    .candidateCount(entry.getValue().size())
                    .build();
        }).toList();
    }
    private List<InterviewApprovalResponseModel.PanelData> buildPanelDataHistory(
            List<InterviewPanelsEntity> interviewPanelsEntities,
            Map<UUID, PositionPanelEntity> panelEntityMap
    ) {

        return interviewPanelsEntities.stream()

                // Remove duplicate panels by panelId
                .collect(Collectors.toMap(
                        InterviewPanelsEntity::getId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ))

                .values()
                .stream()

                .map(entry -> {

                    List<UserDTO> userList = entry.getPanelMembers()
                            .stream()
                            .map(InterviewPanelMembersEntity::getPanelMember)
                            .map(userMapper::toDTO)
                            .toList();

                    PositionPanelEntity positionPanel =
                            panelEntityMap.get(entry.getId());

                    return InterviewApprovalResponseModel.PanelData.builder()
                            .panelId(entry.getId())
                            .panelName(entry.getPanelName())
                            .members(userList)
                            .startDate(positionPanel != null
                                    ? positionPanel.getStartDate()
                                    : null)
                            .endDate(positionPanel != null
                                    ? positionPanel.getEndDate()
                                    : null)
                            .build();
                })

                .toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID,List<InterviewApprovalResponseModel>> getApprovalHistory(UUID requisitionId) {
        List<JobPositionsEntity> jobPositionsEntities = positionsRepository.findAllByRequisitionId(requisitionId);
        List<UUID> positionIds = jobPositionsEntities.stream().map(JobPositionsEntity::getId).toList();
        List<PositionPanelEntity> positionPanelEntities = positionPanelRepository.findByJobPositionIdIn(positionIds);

        List<CandidateApplicationsEntity> candidateApplicationsEntities = candidateApplicationsRepository.findByPositionIdIn(positionIds);

        List<UUID> allApplicationIds = candidateApplicationsEntities.stream()
                .map(CandidateApplicationsEntity::getId)
                .toList();

        List<InterviewScheduleStagingHistoryEntity> stagingHistoryEntities = interviewScheduleStagingHistoryRepository.findByApplicationIdIn(allApplicationIds);


        List<UUID> panelIds=positionPanelEntities.stream().map(i->i.getInterviewPanel().getId()).toList();
        List<UUID> zonalIds = stagingHistoryEntities.stream().map(InterviewScheduleStagingHistoryEntity::getZonalOfficeId).filter(Objects::nonNull).toList();

        List<InterviewCentresEntity> zonalEntitys = interviewCentresRepository.findAllById(zonalIds);

        Map<UUID, InterviewPanelsEntity> panelEntityMap=interviewPanelsRepository.findAllById(panelIds).stream().collect(Collectors.toMap(InterviewPanelsEntity::getId,i->i));
        Map<UUID, InterviewCentresEntity> zonalEntityMap = zonalEntitys.stream().collect(Collectors.toMap(InterviewCentresEntity::getId, i -> i));
        Map<UUID, PositionPanelEntity> positionPanelEntityMap = positionPanelEntities.stream().collect(Collectors.toMap(
                i -> i.getInterviewPanel().getId(),
                i -> i,
                (existing, replacement) -> existing
        ));

        /*
         * batchId -> List(stagingHistory)
         */
        Map<UUID, List<InterviewScheduleStagingHistoryEntity>> stagingHistoryMapWithBatchId=
                stagingHistoryEntities.stream()
                        .collect(Collectors.groupingBy(
                                InterviewScheduleStagingHistoryEntity::getBatchId
                        ));

        /*
         * batchId -> List(pannelEntity)
         */
        Map<UUID, List<InterviewPanelsEntity>> panelMapWithBatchId =
                stagingHistoryEntities.stream()
                        .collect(Collectors.groupingBy(
                                InterviewScheduleStagingHistoryEntity::getBatchId,
                                Collectors.mapping(
                                        history -> panelEntityMap.get(history.getPanelId()),
                                        Collectors.filtering(
                                                Objects::nonNull,
                                                Collectors.toList()
                                        )
                                )
                        ));
        /*
         * batchId -> List(zonal or interviewCenter)
         */
        Map<UUID, List<InterviewCentresEntity>> zonalMapWithBatchId =
                stagingHistoryEntities.stream()
                        .collect(Collectors.groupingBy(
                                InterviewScheduleStagingHistoryEntity::getBatchId,
                                Collectors.mapping(
                                        history -> zonalEntityMap.get(history.getZonalOfficeId()),
                                        Collectors.filtering(
                                                Objects::nonNull,
                                                Collectors.toList()
                                        )
                                )
                        ));

        Map<UUID, JobPositionsDTO> jobPositionsDTOMap = jobPositionsEntities.stream().collect(Collectors.toMap(
                JobPositionsEntity::getId,
                jobPositionsMapper::toDtoWithoutChildren
        ));
        Map<UUID,CandidateApplicationsEntity> candidateApplicationsEntityMap=candidateApplicationsEntities.stream().collect(Collectors.toMap(CandidateApplicationsEntity::getId,i->i));

        Map<UUID, List<InterviewApprovalResponseModel>> responseModelMap = new HashMap<>();

        for (Map.Entry<UUID, List<InterviewScheduleStagingHistoryEntity>> entry
                : stagingHistoryMapWithBatchId.entrySet()) {

            UUID batchId = entry.getKey();

            List<InterviewScheduleStagingHistoryEntity> historyEntities =
                    entry.getValue();

            InterviewScheduleStagingHistoryEntity historyEntity =
                    historyEntities.get(0);

            List<InterviewCentresEntity> zonalList =
                    historyEntities.stream()
                            .map(i -> zonalEntityMap.get(i.getZonalOfficeId()))
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

            List<InterviewPanelsEntity> panelList =
                    historyEntities.stream()
                            .map(i -> panelEntityMap.get(i.getPanelId()))
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

            UUID positionId =
                    candidateApplicationsEntityMap
                            .get(historyEntity.getApplicationId())
                            .getPositionId();

            InterviewApprovalResponseModel responseModel =
                    InterviewApprovalResponseModel.builder()
                            .approvalOn(historyEntity.getCreatedDate())
                            .status(historyEntity.getInterviewSchedulingApprovalStatus())
                            .totalCandidateCount(historyEntities.size())
                            .totalZonalCount(zonalList.size())
                            .totalPanelCount(panelList.size())
                            .zonalData(buildZonalDataHistory(historyEntities,zonalEntityMap))
                            .panelData(buildPanelDataHistory(panelList,positionPanelEntityMap))
                            .isHistory(true)
                            .build();

            responseModelMap
                    .computeIfAbsent(positionId, k -> new ArrayList<>())
                    .add(responseModel);
        }

        responseModelMap.entrySet().removeIf(entry -> entry.getValue() == null);
        return responseModelMap;

    }

    @Transactional
    public List<InterviewScheduleStagingDTO> submitForApproval(List<UUID> positionIds) {
        List<CandidateApplicationsEntity> candidateApplicationsEntityList = candidateApplicationsRepository.findByPositionIdIn(positionIds);
        List<UUID> applicationIds = candidateApplicationsEntityList.stream().map(CandidateApplicationsEntity::getId).toList();
        List<InterviewScheduleStagingEntity> stagingEntities = scheduleStagingRepository.findByApplicationIdInAndInterviewSchedulingApprovalStatusIn(applicationIds,List.of(InterviewSchedulingApprovalStatus.PENDING));
        if (stagingEntities.isEmpty()) {
            return Collections.emptyList();
        }

        List<SchedulingConflictResponseModel> errors = interviewSchedulingService.validateSchedulingConflicts(stagingEntities);

        if (!errors.isEmpty()) {
            throw new SchedulingConflictException(
                    "Failed to approve the schedulings",
                    errors
            );
        }

        stagingEntities.forEach(entity -> {
                    int duration= (int) ChronoUnit.MINUTES.between(entity.getInterviewStartAt(), entity.getInterviewEndAt());
                    entity.setInterviewSchedulingApprovalStatus(InterviewSchedulingApprovalStatus.L1_PENDING);
                    entity.setInterviewStartAt(entity.getInterviewStartAt());
                    entity.setInterviewEndAt(entity.getInterviewEndAt());
                    entity.setInterviewDurationMinutes(duration);
                }
        );

        List<InterviewScheduleStagingEntity> savedEntities =
                scheduleStagingRepository.saveAll(stagingEntities);
        mailSenderHelper.sendApprovalMail(positionIds, RequisitionApproversEntity.ApproverRole.L1,"Interview Schedule","Interview Schedule Approval Request");
        return interviewScheduleStagingMapper.toDtoList(savedEntities);
    }

    @Transactional
    public byte[] getInterviewScheduledCandidatesExcel(UUID positionId) {
        JobPositionsEntity jobPosition = positionsRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Positon not found."));
        JobRequisitionsEntity jobRequisition =jobRequisitionsRepository.findById(jobPosition.getRequisitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Job Requisition not found."));
        MasterPositionsEntity masterPosition = masterPositionsRepository.findById(jobPosition.getMasterPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Master Position not found."));
        DepartmentsEntity departmentsEntity = departmentsRepository.findById(jobPosition.getDeptId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));
        List<CandidateApplicationsEntity> candidateApplicationsEntities = candidateApplicationsRepository.findByPositionId(positionId);
        List<UUID> applicationIds = candidateApplicationsEntities.stream().map(CandidateApplicationsEntity::getId).toList();
        List<InterviewScheduleStagingEntity> stagingEntities = scheduleStagingRepository.findByApplicationIdInAndInterviewSchedulingApprovalStatusIn(applicationIds,List.of(InterviewSchedulingApprovalStatus.L1_PENDING));
        List<UUID> panelIds=stagingEntities.stream().map(InterviewScheduleStagingEntity::getPanelId).toList();
        Map<UUID,InterviewPanelsEntity> interviewPanelsMap=interviewPanelsRepository.findAllById(panelIds).stream()
                .collect(
                        Collectors.toMap(
                                InterviewPanelsEntity::getId,
                                Function.identity(),
                                (existing, replacement) -> existing
                        )
                );

        if(stagingEntities.isEmpty()){
            throw new ManualValidationException("No interviews scheduled for the given position.");
        }
        String requisitionName = jobRequisition.getRequisitionCode() +"-"+ jobRequisition.getRequisitionTitle();
        String masterPositionName = masterPosition.getPositionName();
        String departmentName = departmentsEntity.getDepartmentName();
        List<InterviewScheduledCandidatesDownloadModel> interviewSchedules = stagingEntities.stream()
                 .sorted(
                Comparator.comparing(
                                (InterviewScheduleStagingEntity entity) ->
                                        entity.getInterviewCentre() != null
                                                ? entity.getInterviewCentre().getInterviewCentre()
                                                : "",
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(InterviewScheduleStagingEntity::getInterviewStartAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
        )
                .map(entity -> {
            CandidateApplicationsEntity application = entity.getApplication();
            CandidateProfileEntity profile = entity.getCandidateProfile();
            InterviewCentresEntity interviewCentre = entity.getInterviewCentre();
            InterviewPanelsEntity interviewPanel = interviewPanelsMap.get(entity.getPanelId());
            return InterviewScheduledCandidatesDownloadModel.builder()
                    .requisitionName(requisitionName)
                    .positionName(masterPositionName)
                    .departmentName(departmentName)
                    .applicationNo(application != null ? application.getApplicationNo() : null)
                    .candidateName(commonUtilityProvider.buildFullName(profile))
                    .interviewCentre(interviewCentre != null ? interviewCentre.getInterviewCentre() : null)
                    .panelName(interviewPanel != null ? interviewPanel.getPanelName() : null)
                    .interviewDate(entity.getInterviewStartAt()!= null ? entity.getInterviewStartAt().toLocalDate().format(AppConstants.DD_MM_YYYY) : "")
                    .interviewStartTime(entity.getInterviewStartAt()!= null ? entity.getInterviewStartAt().toLocalTime().format(AppConstants.HH_mm) : "")
                    .interviewEndTime(entity.getInterviewEndAt()!= null ? entity.getInterviewEndAt().toLocalTime().format(AppConstants.HH_mm) : "")
                    .build();

        }).toList();
        ExcelTemplateFile excelFile = excelTemplateService.dtoListToExcelFile(InterviewScheduledCandidatesDownloadModel.class, interviewSchedules, null);
        return excelFile.getFileContent();
    }

}

