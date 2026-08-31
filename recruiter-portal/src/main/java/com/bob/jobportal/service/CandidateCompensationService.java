package com.bob.jobportal.service;

import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.CandidateCompensationDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.*;
import com.bob.db.mapper.CandidateCompensationMapper;
import com.bob.db.mapper.InterviewScheduleMapper;
import com.bob.db.repository.*;
import com.bob.db.enums.CompensationActionEnum;
import com.bob.jobportal.model.CandidateCompensationActionRequestModel;
import com.bob.jobportal.model.CompensationCandidateRequestModel;
import com.bob.jobportal.model.CompensationCandidateResponseModel;
import com.bob.jobportal.model.CompensationSendRequestModel;
import jakarta.mail.MessagingException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CandidateCompensationService {

    @Autowired
    private CandidateCompensationRepository candidateCompensationRepository;

    @Autowired
    private CandidateCompensationMapper candidateCompensationMapper;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;

    @Autowired
    private CandidateDocumentStoreRepository candidateDocumentStoreRepository;

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private InterviewCommitteeRepository interviewCommitteeRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private InterviewScheduleMapper interviewScheduleMapper;

    @Autowired
    private MailSenderHelper mailSenderHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewPanelMembersRepository panelMembersRepository;

    public void sendToCompensationPool(CompensationSendRequestModel requestModel) throws MessagingException, IOException {

        List<InterviewScheduleEntity> interviewScheduleEntities=interviewScheduleMapper.toEntityList(requestModel.getInterviewSchedules());
        LocalDate submitBeforeDate = requestModel.getSubmitBeforeDate();
        List<UUID> applicationIds=interviewScheduleEntities.stream().map(InterviewScheduleEntity::getApplicationId).toList();

        List<CandidateCompensationEntity> compensationEntities=new ArrayList<>();
        Map<UUID,CandidateApplicationsEntity> candidateApplicationsEntityMap=candidateApplicationsRepository.findAllById(applicationIds)
                .stream().collect(Collectors.toMap(CandidateApplicationsEntity::getId, Function.identity()));
        List<CandidateApplicationsEntity> candidateApplicationsEntities=new ArrayList<>();
        for(InterviewScheduleEntity entity:interviewScheduleEntities){
            CandidateApplicationsEntity candidateApplications=candidateApplicationsEntityMap.get(entity.getApplicationId());
            candidateApplications.setApplicationStatus(CandidateApplicationStatus.COMPENSATION_PENDING);
            entity.setInterviewStatus(InterviewSchedulingStatus.COMPENSATION);
            CandidateCompensationEntity compensationEntity=CandidateCompensationEntity.builder()
                    .candidateId(entity.getCandidateId())
                    .application(candidateApplications)
                    .interviewScheduleId(entity.getId())
                    .submitBeforeDate(submitBeforeDate)
                    .build();
            compensationEntities.add(compensationEntity);
            candidateApplicationsEntities.add(candidateApplications);
        }
        candidateCompensationRepository.saveAll(compensationEntities);
        candidateApplicationsRepository.saveAllWithWorkflow(candidateApplicationsEntities);
        interviewScheduleRepository.saveAll(interviewScheduleEntities);
        List<UUID> candidateCompensationIds = compensationEntities.stream().map(CandidateCompensationEntity::getId).toList();
        mailSenderHelper.sendMailToCompensationPoolCandidates(candidateCompensationIds);
    }

    @Transactional
    public Page<CompensationCandidateResponseModel> getCompensationCandidates(CompensationCandidateRequestModel requestModel) {
        List<CompensationStatus> compensationStatuses=requestModel.getStatusList();
        Pageable pageable= PageRequest.of(requestModel.getPage(), requestModel.getSize(), Sort.by("createdDate").descending());
        LocalDate currentDate = LocalDate.now();
        UUID userId=securityUtils.getCurrentUserId();
        String role=securityUtils.getCurrentUserRole();
        Optional<UserEntity> optUser=userRepository.findById(userId);
        List<UUID> positionIds=List.of(requestModel.getPositionId());
        if(!optUser.isPresent()){
            return Page.empty();
        }
        if(role.equals(UserRole.COMMITTEE_MEMBER.toString())){
            List<InterviewPanelMembersEntity> panelMembersEntities=panelMembersRepository.findAllByPanelMember_Id(userId);
            if(panelMembersEntities.isEmpty()){
                return Page.empty();
            }
            List<UUID> panelIds=panelMembersEntities.stream().map(e->e.getPanel().getId()).toList();
            List<PositionPanelEntity> positionPanelEntities=positionPanelRepository.findActivePanels(panelIds,currentDate,List.of(PositionPanelStatus.APPROVED)).stream()
                    .filter(posPanel->AppConstants.COMPENSATION_COMMITTEE_NAME.equals(posPanel.getInterviewPanel().getCommittee().getCommitteeName())).toList();
            List<UUID> assignedPositionIds=positionPanelEntities.stream().map(PositionPanelEntity::getJobPosition).map(JobPositionsEntity::getId).toList();
            positionIds=positionIds.stream().filter(p->assignedPositionIds.contains(p)).toList();
            if(positionIds.isEmpty()){
                return Page.empty();
            }
        }

        Specification<CandidateCompensationEntity> candidateCompensationEntitySpecification=fetchSpecificationWithFilters(requestModel.getSearchText(), compensationStatuses,positionIds);
        Page<CandidateCompensationEntity> compensationEntityPage=candidateCompensationRepository.findAll(candidateCompensationEntitySpecification,pageable);

        List<CandidateCompensationEntity> compensationEntities=compensationEntityPage.getContent();
        List<UUID> candidateIds=compensationEntities.stream().map(c -> c.getCandidateProfile().getCandidateId()).toList();
        DocumentTypesEntity documentTypes = documentTypesRepository.findByDocCode(DocumentCode.RESUME.toString());
        List<CandidateDocumentStoreEntity> documentStoreEntities = candidateDocumentStoreRepository.findAllByDocumentIdAndCandidateIdIn(documentTypes.getId(), candidateIds);
        Map<UUID, CandidateDocumentStoreEntity> mappedDocumentStore= documentStoreEntities.stream()
                .collect(Collectors.toMap(CandidateDocumentStoreEntity::getCandidateId, Function.identity()));
        return compensationEntityPage.map(compensationEntity -> {
            CompensationCandidateResponseModel responseModel = CompensationCandidateResponseModel.builder()
                    .resumeUrl(mappedDocumentStore.get(compensationEntity.getCandidateProfile().getCandidateId()).getFileUrl())
                    .candidateCompensation(candidateCompensationMapper.toDTO(compensationEntity))
                    .build();
            return responseModel;
        });

    }
    public Specification<CandidateCompensationEntity> fetchSpecificationWithFilters(String searchText, List<CompensationStatus> compensationStatuses,List<UUID> positionIds){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            //PositionId filter on candidate application
            if(positionIds!=null || !positionIds.isEmpty()){
                predicates.add(root.get("application").get("positionId").in(positionIds));
            }
            //Compensation statuses filter on compensation status of candidate compensation entity
            if (compensationStatuses != null && !compensationStatuses.isEmpty()) {
                predicates.add(root.get("compensationStatus").in(compensationStatuses));
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

    public void addCompensationDetails(CandidateCompensationActionRequestModel requestModel) {
        String role=securityUtils.getCurrentUserRole();
        UUID userId=securityUtils.getCurrentUserId();
        LocalDate currentDate=LocalDate.now();
        CandidateCompensationDTO compensationDTO=requestModel.getCompensation();
        UUID positionId=requestModel.getCompensation().getApplication().getPositionId();
        CompensationActionEnum action=requestModel.getAction();
        BigDecimal fixedPay=compensationDTO.getFixedPay();
        BigDecimal variablePay=compensationDTO.getVariablePay();
        Specification<PositionPanelEntity> specification=buildSpecification(positionId,userId,AppConstants.COMPENSATION_COMMITTEE_NAME,currentDate);
        List<PositionPanelEntity> positionPanelEntities = positionPanelRepository.findAll(specification);
        Optional<CandidateApplicationsEntity> optionalCandidateApplications=candidateApplicationsRepository.findById(compensationDTO.getApplication().getId());
        List<CompensationActionEnum> allowedActions=new ArrayList<>();
        if(role.equals(UserRole.RECRUITER.toString())) {
            if (!positionPanelEntities.isEmpty()) {
                allowedActions.addAll(List.of(CompensationActionEnum.APPROVE, CompensationActionEnum.REJECT, CompensationActionEnum.RENEGOTIATE));
            }
            else{
                allowedActions.add(CompensationActionEnum.SUBMIT);
            }
            if (!allowedActions.contains(action)) {
                throw new RuntimeException("Invalid action for recruiter");
            }

        }else if(role.equals(UserRole.COMMITTEE_MEMBER.toString())) {
            if (positionPanelEntities.isEmpty()) {
                throw new RuntimeException("Recruiter is not part of the interview panel for this position");
            }
            allowedActions.addAll(List.of(CompensationActionEnum.APPROVE, CompensationActionEnum.REJECT, CompensationActionEnum.RENEGOTIATE));
            if (!allowedActions.contains(action)) {
                throw new RuntimeException("Invalid action for committee member");
            }
        }
        else{
            throw new RuntimeException("User role not authorized to perform this action");
        }
        if(!optionalCandidateApplications.isPresent()){
            throw new RuntimeException("Candidate application not found for the given application id");
        }
        CandidateApplicationsEntity candidateApplications=optionalCandidateApplications.get();
        switch (action) {
            case SUBMIT -> {
                if(compensationDTO.getCompensationStatus()!=CompensationStatus.SUBMITTED && compensationDTO.getCompensationStatus()!=CompensationStatus.RENEGOTIATE){
                    throw new RuntimeException("Only compensation details with SUBMITTED or RENEGOTIATE status can be submitted");
                }
                compensationDTO.setCompensationStatus(CompensationStatus.PENDING);
                break;
            }
            case APPROVE -> {
                compensationDTO.setCompensationStatus(CompensationStatus.APPROVED);
                candidateApplications.setApplicationStatus(CandidateApplicationStatus.COMPENSATION_APPROVED);
                break;
            }
            case REJECT -> {
                compensationDTO.setCompensationStatus(CompensationStatus.REJECTED);
                candidateApplications.setApplicationStatus(CandidateApplicationStatus.COMPENSATION_REJECTED);
                break;
            }
            case RENEGOTIATE -> {
                compensationDTO.setCompensationStatus(CompensationStatus.RENEGOTIATE);
                candidateApplications.setApplicationStatus(CandidateApplicationStatus.COMPENSATION_RENEGOTITATE);
                break;
            }
            default -> {
                throw new RuntimeException("Invalid action for recruiter");
            }
        }
        compensationDTO.setAgreedCtc(calculateCtc(fixedPay,variablePay));
        CandidateCompensationEntity compensationEntity=candidateCompensationMapper.toEntity(compensationDTO);
        candidateCompensationRepository.save(compensationEntity);
        candidateApplicationsRepository.saveWithWorkflow(candidateApplications);
    }
    private BigDecimal calculateCtc(BigDecimal fixedPay, BigDecimal variablePay) {
        if (fixedPay == null) {
            fixedPay = BigDecimal.ZERO;
        }
        if (variablePay == null) {
            variablePay = BigDecimal.ZERO;
        }
        return fixedPay.add(variablePay);
    }

    public static Specification<PositionPanelEntity> buildSpecification(UUID positionId, UUID userId, String committeeName, LocalDate currentDate) {
        return (root, query, cb) -> {

            query.distinct(true);

            // Join PositionPanel with InterviewPanel
            Join<PositionPanelEntity, InterviewPanelsEntity> panelJoin = root.join("interviewPanel", JoinType.INNER);

            // Join InterviewPanel with PanelMembers
            Join<InterviewPanelsEntity, InterviewPanelMembersEntity> membersJoin = panelJoin.join("panelMembers", JoinType.INNER);

            // Join PanelMembers  User (panelMember)
            Join<InterviewPanelMembersEntity, UserEntity> memberJoin = membersJoin.join("panelMember", JoinType.INNER);

            // Join InterviewPanel with InterviewCommittee
            Join<InterviewPanelsEntity, InterviewCommitteeEntity> committeeJoin = panelJoin.join("committee", JoinType.INNER);

            //Build predicates
            Predicate positionPredicate = cb.equal(root.get("jobPosition").get("id"), positionId);
            Predicate userPredicate = cb.equal(memberJoin.get("id"), userId);
            Predicate committeePredicate = cb.equal(committeeJoin.get("committeeName"), committeeName);

            //current date is between start date and end date of position panel
            Predicate datePredicate = cb.and(
                    cb.lessThanOrEqualTo(root.get("startDate"), currentDate),
                    cb.greaterThanOrEqualTo(root.get("endDate"), currentDate)
            );

            return cb.and(positionPredicate, userPredicate, committeePredicate, datePredicate);
        };
    }
}
