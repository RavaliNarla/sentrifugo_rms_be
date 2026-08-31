package com.bob.jobportal.service;

import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.service.VacancyStatisticsService;
import com.bob.commonutil.util.CandidateLookupUtil;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.OfferApprovalHistoryDTO;
import com.bob.db.dto.WorkflowApprovalDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.*;
import com.bob.db.mapper.OfferApprovalHistoryMapper;
import com.bob.db.mapper.OfferApprovalsMapper;
import com.bob.db.mapper.WorkflowApprovalMapper;
import com.bob.db.repository.*;
import com.bob.jobportal.model.ApproveRejectModel;
import com.bob.jobportal.model.OfferApprovalHistoryModel;
import com.bob.jobportal.model.OfferApprovalRequestModel;
import com.bob.jobportal.util.OfferLetterGenerationUtil;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OfferApprovalService {

    @Autowired
    private OfferApprovalHistoryRepository offerApprovalHistoryRepository;

    @Autowired
    private OfferApprovalRepository offerApprovalRepository;

    @Autowired
    private OfferApprovalHistoryMapper offerApprovalHistoryMapper;

    @Autowired
    private OfferApprovalsMapper offerApprovalsMapper;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CandidateOffersRepository candidateOffersRepository;

    @Autowired
    private RequisitionApproversRepository requisitionApproversRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private OfferLetterGenerationUtil offerLetterGenerationUtil;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private WorkflowApprovalEntityRepository workflowApprovalEntityRepository;

    @Autowired
    private WorkflowApprovalMapper workflowApprovalMapper;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private CandidateLookupUtil candidateLookupUtil;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CandidateMeritListRepository candidateMeritListRepository;

    @Autowired
    private VacancyStatisticsService vacancyStatisticsService;

//
//    @Transactional
//    public Page<OfferApprovalHistoryModel> fetchPendingApprovals(OfferApprovalRequestModel requestModel) {
//        Pageable pageable = PageRequest.of(requestModel.getPage(), requestModel.getSize(), Sort.by("createdDate").descending());
//        //Identify user role
//        UUID currentUserId = securityUtils.getCurrentUserId();
//        RequisitionApproversEntity reqApprover = requisitionApproversRepository.findByApproverId(currentUserId)
//                .orElseThrow(() -> new ManualValidationException("Access denied: User is not an authorized approver."));
//        //Define the global statuses that L1 and L2 can see
//        List<OfferApprovalStatus> globalAllowedStatuses;
//
//        if (reqApprover.getApproverRole() == RequisitionApproversEntity.ApproverRole.L1) {
//            globalAllowedStatuses = List.of(OfferApprovalStatus.L1_PENDING, OfferApprovalStatus.L2_PENDING,
//                    OfferApprovalStatus.L1_REJECTED, OfferApprovalStatus.APPROVED, OfferApprovalStatus.L2_REJECTED);
//        } else if (reqApprover.getApproverRole() == RequisitionApproversEntity.ApproverRole.L2) {
//            globalAllowedStatuses = List.of(OfferApprovalStatus.L2_PENDING, OfferApprovalStatus.APPROVED, OfferApprovalStatus.L2_REJECTED);
//        } else {
//            throw new ManualValidationException("Action denied: Invalid approver role.");
//        }
//        //Filter out the statuses that only that user can see
//        List<OfferApprovalStatus> secureStatusesList;
//        if (requestModel.getStatusList() == null || requestModel.getStatusList().isEmpty()) {
//            secureStatusesList = globalAllowedStatuses;
//        } else {
//            secureStatusesList = requestModel.getStatusList().stream()
//                    .filter(globalAllowedStatuses::contains)
//                    .collect(Collectors.toList());
//        }
//        //Create specification to fetch those candidates
//        Specification<OfferApprovalHistoryEntity> offerApprovalSpecification = createSpecificationWithFilters(requestModel.getPositionIds(), secureStatusesList, requestModel.getSearchText());
//        //Get result and convert it to dto
//        Page<OfferApprovalHistoryEntity> offerApprovalHistoryEntities = offerApprovalHistoryRepository.findAll(offerApprovalSpecification, pageable);
//        List<UUID> candidateOfferIds=offerApprovalHistoryEntities.stream().map(oHE ->oHE.getOfferId()).toList();
//        Map<UUID,CandidateOffersEntity> candidateOffersEntityMap=candidateOffersRepository.findAllById(candidateOfferIds).stream().collect(Collectors.toMap(
//                CandidateOffersEntity::getId,
//                Function.identity()
//        ));
//        List<CandidateOffersEntity> candidateOffersEntities=candidateOffersEntityMap.values().stream().toList();
//        Map<UUID, CandidateProfileEntity> candidateProfileMap = candidateLookupUtil.buildCandidateProfileMap(candidateOffersEntities);
//        Map<UUID,CandidateMeritListEntity> candidateMeritListEntityMap = candidateMeritListRepository.findByPositionIdIn(requestModel.getPositionIds()).stream()
//                .collect(Collectors.toMap(
//                        CandidateMeritListEntity::getApplicationId,
//                        Function.identity()
//                ));
//        List<UUID> stateIds = offerApprovalHistoryEntities.stream().map(oHE ->oHE.getStateId()).toList();
//        List<UUID> cityIds = offerApprovalHistoryEntities.stream().map(oHE ->oHE.getCityId()).toList();
//        Map<UUID,String> stateNameMap = stateRepository.findAllById(stateIds).stream()
//                .collect(Collectors.toMap(
//                        StateEntity::getId,
//                        StateEntity::getStateName
//                ));
//        Map<UUID,String> cityNameMap = cityRepository.findAllById(cityIds).stream()
//                .collect(Collectors.toMap(
//                        CityEntity::getId,
//                        CityEntity::getCityName
//                ));
//        return offerApprovalHistoryEntities.map(offerApprovalHistoryEntity -> {
//            CandidateMeritListEntity candidateMeritListEntity=candidateMeritListEntityMap.get(offerApprovalHistoryEntity.getCandidateApplication().getId());
//            // 3. Map everything to your return model
//            OfferApprovalHistoryModel response = OfferApprovalHistoryModel.builder()
//                    .offerApprovalHistory(offerApprovalHistoryMapper.toDTO(offerApprovalHistoryEntity))
//                    .combinedScore(candidateMeritListEntity.getCombinedScore())
//                    .state(stateNameMap.get(offerApprovalHistoryEntity.getStateId()))
//                    .city(cityNameMap.get(offerApprovalHistoryEntity.getCityId()))
//                    .fullName(commonUtilityProvider.buildFullName(candidateProfileMap.get(offerApprovalHistoryEntity.getCandidateId())))
//                    .build();
//
//
//            return response;
//        });
//    }

    @Transactional(readOnly = true)
    public Page<OfferApprovalHistoryModel> fetchPendingApprovals(OfferApprovalRequestModel requestModel) {
        Pageable pageable = PageRequest.of(requestModel.getPage(), requestModel.getSize(), Sort.by("modifiedDate").descending());

        // 1. Identify user role
        UUID currentUserId = securityUtils.getCurrentUserId();
        RequisitionApproversEntity reqApprover = requisitionApproversRepository.findByApproverId(currentUserId)
                .orElseThrow(() -> new ManualValidationException("Access denied: User is not an authorized approver."));

        // 2. Define the global statuses
        List<OfferApprovalStatus> globalAllowedStatuses;
        if (reqApprover.getApproverRole() == RequisitionApproversEntity.ApproverRole.L1) {
            globalAllowedStatuses = List.of(OfferApprovalStatus.L1_PENDING, OfferApprovalStatus.L2_PENDING,
                    OfferApprovalStatus.L1_REJECTED, OfferApprovalStatus.APPROVED, OfferApprovalStatus.L2_REJECTED);
        } else if (reqApprover.getApproverRole() == RequisitionApproversEntity.ApproverRole.L2) {
            globalAllowedStatuses = List.of(OfferApprovalStatus.L2_PENDING, OfferApprovalStatus.APPROVED, OfferApprovalStatus.L2_REJECTED);
        } else {
            throw new ManualValidationException("Action denied: Invalid approver role.");
        }

        // 3. Filter statuses
        List<OfferApprovalStatus> secureStatusesList;
        if (requestModel.getStatusList() == null || requestModel.getStatusList().isEmpty()) {
            secureStatusesList = globalAllowedStatuses;
        } else {
            secureStatusesList = requestModel.getStatusList().stream()
                    .filter(globalAllowedStatuses::contains)
                    .collect(Collectors.toList());
        }

        // 4. CRITICAL CHANGE: Create specification for the ACTIVE table, not the history table
        Specification<OfferApprovalsEntity> offerApprovalSpecification = createSpecificationWithFilters(
                requestModel.getPositionIds(), secureStatusesList, requestModel.getSearchText());

        // 5. CRITICAL CHANGE: Fetch from offerApprovalRepository (which only holds the latest state)
        Page<OfferApprovalsEntity> activeApprovalsPage = offerApprovalRepository.findAll(offerApprovalSpecification, pageable);

        if (activeApprovalsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // --- Bulk Data Fetching ---
        List<UUID> candidateOfferIds = activeApprovalsPage.stream().map(OfferApprovalsEntity::getOfferId).toList();
        List<CandidateOffersEntity> candidateOffersEntities = candidateOffersRepository.findAllById(candidateOfferIds);
        Map<UUID,CandidateOffersEntity> offersEntityMap=candidateOffersEntities.stream()
                .collect(Collectors.toMap(ca->ca.getId(),Function.identity()));
        Map<UUID, CandidateProfileEntity> candidateProfileMap = candidateLookupUtil.buildCandidateProfileMap(candidateOffersEntities);

        // OPTIMIZATION: Fetch Merit List by applicationIds in this specific page, NOT positionIds.
        List<UUID> applicationIds = activeApprovalsPage.stream()
                .map(app -> app.getCandidateApplication().getId())
                .toList();

        Map<UUID, CandidateMeritListEntity> meritMap = candidateMeritListRepository.findByApplicationIdIn(applicationIds).stream()
                .collect(Collectors.toMap(CandidateMeritListEntity::getApplicationId, Function.identity()));

        List<UUID> stateIds = meritMap.values().stream().map(CandidateMeritListEntity::getStateId).filter(Objects::nonNull).distinct().toList();
        List<UUID> cityIds = meritMap.values().stream().map(CandidateMeritListEntity::getCityId).filter(Objects::nonNull).distinct().toList();

        Map<UUID, String> stateNameMap = stateRepository.findAllById(stateIds).stream()
                .collect(Collectors.toMap(StateEntity::getId, StateEntity::getStateName));
        Map<UUID, String> cityNameMap = cityRepository.findAllById(cityIds).stream()
                .collect(Collectors.toMap(CityEntity::getId, CityEntity::getCityName));

        // 6. Map to Final Response
        return activeApprovalsPage.map(approvalEntity -> {
            CandidateMeritListEntity merit = meritMap.get(approvalEntity.getCandidateApplication().getId());

            UUID stateId = merit != null ? merit.getStateId() : null;
            UUID cityId = merit != null ? merit.getCityId() : null;
            String letterNo=offersEntityMap.get(approvalEntity.getOfferId()).getLetterNumber();
            // Ensure your return model is OfferApprovalModel (representing the active state)
            return OfferApprovalHistoryModel.builder()
                    .offerApproval(offerApprovalsMapper.toDTO(approvalEntity)) // Map the active entity
                    .combinedScore(merit != null ? merit.getCombinedScore() : null)
                    .state(stateId != null ? stateNameMap.get(stateId) : null)
                    .city(cityId != null ? cityNameMap.get(cityId) : null)
                    .fullName(commonUtilityProvider.buildFullName(candidateProfileMap.get(approvalEntity.getCandidateId())))
                    .letterNumber(letterNo)
                    .build();
        });
    }


    private Specification createSpecificationWithFilters(List<UUID> positionIds, List<OfferApprovalStatus> offerApprovalList,String searchText){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            //PositionId filter on candidate application
            if(positionIds!=null || !positionIds.isEmpty()){
                predicates.add(root.get("candidateApplication").get("positionId").in(positionIds));
            }
            //Compensation statuses filter on compensation status of candidate compensation entity
            if (offerApprovalList != null && !offerApprovalList.isEmpty()) {
                predicates.add(root.get("approvalStatus").in(offerApprovalList));
            }
            //Search text filter on first name,middle name and last name of candidate profile
            if (searchText != null && !searchText.trim().isEmpty()) {
                String likePattern = "%" + searchText.toLowerCase() + "%";
                Predicate firstNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("candidateProfile").get("firstName")), likePattern);
                Predicate middleNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("candidateProfile").get("middleName")), likePattern);
                Predicate lastNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("candidateProfile").get("lastName")), likePattern);
                predicates.add(criteriaBuilder.or(firstNamePredicate,middleNamePredicate, lastNamePredicate));
            }

            query.orderBy(criteriaBuilder.desc(root.get("createdDate")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

//    @Transactional
//    public void approveOrRejectCandidate(ApproveRejectModel approveRejectModel) {
//        List<UUID> offerApprovalIds=approveRejectModel.getOfferApprovalHistoryIds();
//        List<OfferApprovalHistoryEntity> offerApprovalHistoryEntities=offerApprovalHistoryRepository.findAllById(offerApprovalIds);
//        UUID currentUserId=securityUtils.getCurrentUserId();
//        Optional<RequisitionApproversEntity> reqApproverOpt=requisitionApproversRepository.findByApproverId(currentUserId);
//        if(!reqApproverOpt.isPresent()){
//            throw new ManualValidationException("Action failed: Approver details not found for this request.");
//        }
//        RequisitionApproversEntity reqApprover=reqApproverOpt.get();
//        if(reqApprover.getApproverRole() == RequisitionApproversEntity.ApproverRole.L1){
//            approveOrRejectL1Candidate(offerApprovalHistoryEntities,approveRejectModel);
//        }
//        else if(reqApprover.getApproverRole() == RequisitionApproversEntity.ApproverRole.L2){
//            approveOrRejectL2Candidate(offerApprovalHistoryEntities,approveRejectModel);
//        }
//        else{
//            throw new ManualValidationException("Action denied: Invalid approver role.");
//        }
//    }
//    private void approveOrRejectL1Candidate(List<OfferApprovalHistoryEntity> offerApprovalHistoryEntities,ApproveRejectModel approveRejectModel){
//        Set<UUID> offerApprovalIds=offerApprovalHistoryEntities.stream().map(OfferApprovalHistoryEntity::getOfferApprovalId).collect(Collectors.toSet());
//        Set<UUID> offerIds=offerApprovalHistoryEntities.stream().map(OfferApprovalHistoryEntity::getOfferId).collect(Collectors.toSet());
//        Map<UUID, OfferApprovalsEntity> offerApprovalsMap = offerApprovalRepository.findAllById(offerApprovalIds)
//                .stream()
//                .collect(Collectors.toMap(
//                        OfferApprovalsEntity::getId,
//                        Function.identity()
//                ));
//        Map<UUID,CandidateOffersEntity> offersEntities=candidateOffersRepository.findAllById(offerIds).stream()
//                .collect(Collectors.toMap(
//                        CandidateOffersEntity::getId,
//                        Function.identity()
//                ));
//
//        List<OfferApprovalHistoryEntity> offerApprovalHistToSave=new ArrayList<>();
//        List<OfferApprovalsEntity> offerApprovalsToSave=new ArrayList<>();
//        List<CandidateOffersEntity> candidateOfferToSave=new ArrayList<>();
//        for(OfferApprovalHistoryEntity offerApprovalHistoryEntity:offerApprovalHistoryEntities){
//            OfferApprovalStatus currentOfferApprovalStatus=OfferApprovalStatus.L1_PENDING;
//            OfferApprovalsEntity currentOfferApprovalEntity=offerApprovalsMap.get(offerApprovalHistoryEntity.getOfferApprovalId());
//            CandidateOffersEntity candidateOffers=offersEntities.get(offerApprovalHistoryEntity.getOfferId());
//            if(offerApprovalHistoryEntity.getApprovalStatus()==currentOfferApprovalStatus){
//                if(approveRejectModel.getAction()==ButtonActionEnum.APPROVE){
//                    offerApprovalHistoryEntity.setApprovalStatus(OfferApprovalStatus.L2_PENDING);
//                    currentOfferApprovalEntity.setApprovalStatus(OfferApprovalStatus.L2_PENDING);
//                }
//                else if(approveRejectModel.getAction()==ButtonActionEnum.REJECT){
//                    offerApprovalHistoryEntity.setApprovalStatus(OfferApprovalStatus.L1_REJECTED);
//                    currentOfferApprovalEntity.setApprovalStatus(OfferApprovalStatus.L1_REJECTED);
//                    candidateOffers.setStatus(CandidateOfferStatus.APPROVAL_REJECTED);
//                    candidateOfferToSave.add(candidateOffers);
//                }else{
//                    throw new ManualValidationException("Invalid button action!");
//                }
//                offerApprovalHistToSave.add(offerApprovalHistoryEntity);
//                offerApprovalsToSave.add(currentOfferApprovalEntity);
//            }else{
//                continue;
//            }
//        }
//        offerApprovalHistoryRepository.saveAllWithWorkflow(offerApprovalHistToSave,approveRejectModel.getComments());
//        offerApprovalRepository.saveAll(offerApprovalsToSave);
//        if(!candidateOfferToSave.isEmpty()){
//            candidateOffersRepository.saveAll(candidateOfferToSave);
//        }
//    }
//
//    private void approveOrRejectL2Candidate(List<OfferApprovalHistoryEntity> offerApprovalHistoryEntities, ApproveRejectModel approveRejectModel) {
//        Set<UUID> offerApprovalIds = offerApprovalHistoryEntities.stream().map(OfferApprovalHistoryEntity::getOfferApprovalId).collect(Collectors.toSet());
//        Set<UUID> offerIds = offerApprovalHistoryEntities.stream().map(OfferApprovalHistoryEntity::getOfferId).collect(Collectors.toSet());
//        Set<UUID> applicationIds=offerApprovalHistoryEntities.stream().map(o->o.getCandidateApplication().getId()).collect(Collectors.toSet());
//        Set<UUID> candidateIds=offerApprovalHistoryEntities.stream().map(o->o.getCandidateId()).collect(Collectors.toSet());
//        Map<UUID, OfferApprovalsEntity> offerApprovalsMap = offerApprovalRepository.findAllById(offerApprovalIds)
//                .stream()
//                .collect(Collectors.toMap(
//                        OfferApprovalsEntity::getId,
//                        Function.identity()
//                ));
//
//        Map<UUID, CandidateOffersEntity> offersEntities = candidateOffersRepository.findAllById(offerIds).stream()
//                .collect(Collectors.toMap(
//                        CandidateOffersEntity::getId,
//                        Function.identity()
//                ));
//        Map<UUID, CandidateApplicationsEntity> candidateApplicationsEntityMap=candidateApplicationsRepository.findAllById(applicationIds).stream()
//                .collect(Collectors.toMap(
//                        CandidateApplicationsEntity::getId,
//                        Function.identity()
//                ));
//
//        Map<UUID,CandidateProfileEntity> candidateProfileEntityMap=candidateProfileRepository.findByCandidateIdIn(candidateIds).stream()
//                .collect(Collectors.toMap(
//                        CandidateProfileEntity::getCandidateId,
//                        Function.identity()
//                ));
//        List<OfferApprovalHistoryEntity> offerApprovalHistToSave = new ArrayList<>();
//        List<OfferApprovalsEntity> offerApprovalsToSave = new ArrayList<>();
//        List<CandidateOffersEntity> candidateOfferToSave = new ArrayList<>();
//        List<CandidateApplicationsEntity> candAppsToSave=new ArrayList<>();
//        for (OfferApprovalHistoryEntity offerApprovalHistoryEntity : offerApprovalHistoryEntities) {
//            OfferApprovalStatus currentOfferApprovalStatus = OfferApprovalStatus.L2_PENDING;
//            OfferApprovalsEntity currentOfferApprovalEntity = offerApprovalsMap.get(offerApprovalHistoryEntity.getOfferApprovalId());
//            CandidateOffersEntity candidateOffers = offersEntities.get(offerApprovalHistoryEntity.getOfferId());
//            CandidateApplicationsEntity candidateApplications=candidateApplicationsEntityMap.get(offerApprovalHistoryEntity.getCandidateApplication().getId());
//            if (offerApprovalHistoryEntity.getApprovalStatus() == currentOfferApprovalStatus) {
//                if (approveRejectModel.getAction() == ButtonActionEnum.APPROVE) {
//                    offerApprovalHistoryEntity.setApprovalStatus(OfferApprovalStatus.APPROVED);
//                    currentOfferApprovalEntity.setApprovalStatus(OfferApprovalStatus.APPROVED);
//                    candidateOffers.setStatus(CandidateOfferStatus.OFFER_SENT);
//                    candidateApplications.setApplicationStatus(CandidateApplicationStatus.OFFERED);
//                    candidateOfferToSave.add(candidateOffers);
//                    candAppsToSave.add(candidateApplications);
//                }
//                else if (approveRejectModel.getAction() == ButtonActionEnum.REJECT) {
//                    offerApprovalHistoryEntity.setApprovalStatus(OfferApprovalStatus.L2_REJECTED);
//                    currentOfferApprovalEntity.setApprovalStatus(OfferApprovalStatus.L2_REJECTED);
//                    candidateOffers.setStatus(CandidateOfferStatus.APPROVAL_REJECTED);
//                    candidateOfferToSave.add(candidateOffers);
//                }
//                else {
//                    throw new ManualValidationException("Invalid button action!");
//                }
//                offerApprovalHistToSave.add(offerApprovalHistoryEntity);
//                offerApprovalsToSave.add(currentOfferApprovalEntity);
//
//            } else {
//                continue;
//            }
//        }
//        offerApprovalHistoryRepository.saveAllWithWorkflow(offerApprovalHistToSave,approveRejectModel.getComments());
//        offerApprovalRepository.saveAll(offerApprovalsToSave);
//        if (!candidateOfferToSave.isEmpty()) {
//            candidateOfferToSave=candidateOffersRepository.saveAll(candidateOfferToSave);
//        }
//        if(!candAppsToSave.isEmpty()){
//            candidateApplicationsRepository.saveAllWithWorkflow(candAppsToSave);
//        }
//        entityManager.flush();
//        offerLetterGenerationUtil.sendOfferLetterMails(candidateOfferToSave,candidateProfileEntityMap);
//    }
        @Transactional
        public void approveOrRejectCandidate(ApproveRejectModel approveRejectModel) {
            // 1. Fetch the active approval records directly
            List<UUID> offerApprovalIds = approveRejectModel.getOfferApprovalIds();
            List<OfferApprovalsEntity> activeApprovals = offerApprovalRepository.findAllById(offerApprovalIds);

            UUID currentUserId = securityUtils.getCurrentUserId();
            RequisitionApproversEntity reqApprover = requisitionApproversRepository.findByApproverId(currentUserId)
                    .orElseThrow(() -> new ManualValidationException("Action failed: Approver details not found."));

            // 2. Route based on L1 / L2 role
            if (reqApprover.getApproverRole() == RequisitionApproversEntity.ApproverRole.L1) {
                approveOrRejectL1Candidate(activeApprovals, approveRejectModel);
            } else if (reqApprover.getApproverRole() == RequisitionApproversEntity.ApproverRole.L2) {
                approveOrRejectL2Candidate(activeApprovals, approveRejectModel);
            } else {
                throw new ManualValidationException("Action denied: Invalid approver role.");
            }
        }

        private void approveOrRejectL1Candidate(List<OfferApprovalsEntity> activeApprovals, ApproveRejectModel approveRejectModel) {
            Set<UUID> offerIds = activeApprovals.stream().map(OfferApprovalsEntity::getOfferId).collect(Collectors.toSet());

            Map<UUID, CandidateOffersEntity> offersMap = candidateOffersRepository.findAllById(offerIds).stream()
                    .collect(Collectors.toMap(CandidateOffersEntity::getId, Function.identity()));

            List<OfferApprovalsEntity> approvalsToSave = new ArrayList<>();
            List<CandidateOffersEntity> offersToSave = new ArrayList<>();

            for (OfferApprovalsEntity approval : activeApprovals) {
                // Skip if this record isn't currently pending L1 action
                if (approval.getApprovalStatus() != OfferApprovalStatus.L1_PENDING) continue;
                CandidateOffersEntity offer = offersMap.get(approval.getOfferId());
                OfferApprovalStatus newStatus;

                if (approveRejectModel.getAction() == ButtonActionEnum.APPROVE) {
                    newStatus = OfferApprovalStatus.L2_PENDING;
                    offer.setStatus(CandidateOfferStatus.L2_PENDING);
                } else if (approveRejectModel.getAction() == ButtonActionEnum.REJECT) {
                    newStatus = OfferApprovalStatus.L1_REJECTED;
                    offer.setStatus(CandidateOfferStatus.L1_REJECTED);
                    offersToSave.add(offer);
                } else {
                    throw new ManualValidationException("Invalid button action!");
                }

                // Update the active record
                approval.setApprovalStatus(newStatus);
                approvalsToSave.add(approval);
            }

            offerApprovalRepository.saveAll(approvalsToSave);
            saveWorkflow(approvalsToSave, securityUtils.getCurrentUserId(), RequisitionApproversEntity.ApproverRole.L1, approveRejectModel.getComments());
            if (!offersToSave.isEmpty()) candidateOffersRepository.saveAll(offersToSave);
        }

        private void approveOrRejectL2Candidate(List<OfferApprovalsEntity> activeApprovals, ApproveRejectModel approveRejectModel) {
            Set<UUID> offerIds = activeApprovals.stream().map(OfferApprovalsEntity::getOfferId).collect(Collectors.toSet());
            Set<UUID> applicationIds = activeApprovals.stream().map(a -> a.getCandidateApplication().getId()).collect(Collectors.toSet());
            Set<UUID> candidateIds = activeApprovals.stream().map(OfferApprovalsEntity::getCandidateId).collect(Collectors.toSet());

            Map<UUID, CandidateOffersEntity> offersMap = candidateOffersRepository.findAllById(offerIds).stream()
                    .collect(Collectors.toMap(CandidateOffersEntity::getId, Function.identity()));

            Map<UUID, CandidateApplicationsEntity> appsMap = candidateApplicationsRepository.findAllById(applicationIds).stream()
                    .collect(Collectors.toMap(CandidateApplicationsEntity::getId, Function.identity()));

            Map<UUID, CandidateProfileEntity> profilesMap = candidateProfileRepository.findByCandidateIdIn(candidateIds).stream()
                    .collect(Collectors.toMap(CandidateProfileEntity::getCandidateId, Function.identity()));

            List<OfferApprovalsEntity> approvalsToSave = new ArrayList<>();
            List<CandidateOffersEntity> offersToSave = new ArrayList<>();
            List<CandidateApplicationsEntity> appsToSave = new ArrayList<>();

            // Track which ones actually got approved so we only email them
            List<CandidateOffersEntity> approvedOffersForEmail = new ArrayList<>();

            for (OfferApprovalsEntity approval : activeApprovals) {
                // Skip if this record isn't currently pending L2 action
                if (approval.getApprovalStatus() != OfferApprovalStatus.L2_PENDING) continue;

                CandidateOffersEntity offer = offersMap.get(approval.getOfferId());
                CandidateApplicationsEntity application = appsMap.get(approval.getCandidateApplication().getId());
                OfferApprovalStatus newStatus;

                if (approveRejectModel.getAction() == ButtonActionEnum.APPROVE) {
                    newStatus = OfferApprovalStatus.APPROVED;
                    offer.setStatus(CandidateOfferStatus.OFFER_SENT);
                    application.setApplicationStatus(CandidateApplicationStatus.OFFERED);

                    offersToSave.add(offer);
                    appsToSave.add(application);
                    approvedOffersForEmail.add(offer); // Stage for mailing

                } else if (approveRejectModel.getAction() == ButtonActionEnum.REJECT) {
                    newStatus = OfferApprovalStatus.L2_REJECTED;
                    offer.setStatus(CandidateOfferStatus.L2_REJECTED);
                    offersToSave.add(offer);
                } else {
                    throw new ManualValidationException("Invalid button action!");
                }

                // Update active record
                approval.setApprovalStatus(newStatus);
                approvalsToSave.add(approval);
            }

            offerApprovalRepository.saveAll(approvalsToSave);
            saveWorkflow(approvalsToSave, securityUtils.getCurrentUserId(), RequisitionApproversEntity.ApproverRole.L2, approveRejectModel.getComments());
            List<CandidateApplicationsEntity> savedApplications=new ArrayList<>();
            if (!appsToSave.isEmpty()) savedApplications=candidateApplicationsRepository.saveAllWithWorkflow(appsToSave);
            if (!offersToSave.isEmpty()) candidateOffersRepository.saveAll(offersToSave);

            // Flush to DB before triggering emails
            entityManager.flush();

            // Trigger emails only for newly approved candidates
            if (!approvedOffersForEmail.isEmpty()) {
                offerLetterGenerationUtil.sendOfferLetterMails(approvedOffersForEmail, profilesMap);
            }
            if(savedApplications.size()>0){
                savedApplications.forEach(app -> {
                    try {
                        vacancyStatisticsService.incrementOffersSent(app.getId());
                    } catch (Exception e) {
                        log.warn("Failed to increment offers_sent for applicationId {}: {}", app.getId(), e.getMessage());
                    }
                });
            }
        }

    private void saveWorkflow(List<OfferApprovalsEntity> approvals,
                              UUID approverId,
                              RequisitionApproversEntity.ApproverRole approverRole,
                              String comments) {

        List<WorkflowApprovalEntity> workflows = approvals.stream()
                .map(approval -> WorkflowApprovalEntity.builder()
                        .entityId(approval.getId())
                        .entityType(OfferApprovalsEntity.ENTITY_TYPE)
                        .stepNumber(1)
                        .approverRole(approverRole.toString())
                        .approverId(approverId)
                        .action(approval.getApprovalStatus().toString())
                        .status(approval.getApprovalStatus().toString())
                        .comments(comments)
                        .actionDate(LocalDateTime.now())
                        .build())
                .toList();

        workflowApprovalEntityRepository.saveAll(workflows);
    }
    @Transactional(readOnly = true)
    public List<WorkflowApprovalDTO> getWorkflowHistory(UUID historyId) {
        return workflowApprovalMapper.toDTOs(
                workflowApprovalEntityRepository.findByEntityTypeAndEntityIdOrderByActionDateDesc(
                        OfferApprovalsEntity.ENTITY_TYPE, historyId
                )
        );
    }
}
