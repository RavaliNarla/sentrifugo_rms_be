package com.bob.masterdata.Service;

import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.commonutil.service.MailSenderHelper;
import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.InterviewCommitteeDTO;
import com.bob.db.dto.InterviewPanelMembersDTO;
import com.bob.db.dto.InterviewPanelsDTO;
import com.bob.db.dto.UserDTO;
import com.bob.db.entity.*;
import com.bob.db.enums.UserRole;
import com.bob.db.mapper.UserMapper;
import com.bob.db.repository.*;
import com.bob.db.mapper.InterviewPanelsMapper;
import com.bob.db.mapper.InterviewPanelMembersMapper;

import jakarta.mail.MessagingException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;

import org.apache.poi.ss.util.CellRangeAddressList;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InterviewPanelsService {

    @Autowired
    private InterviewPanelsRepository interviewPanelsRepository;

    @Autowired
    private InterviewPanelsMapper interviewPanelsMapper;

    @Autowired
    private InterviewPanelMembersMapper interviewPanelMembersMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewCommitteeRepository interviewCommitteeRepository;

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private MailSenderHelper mailSenderHelper;

    @Autowired
    private CandidateApplicationsRepository candidateApplicationsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public InterviewPanelsDTO save(InterviewPanelsDTO interviewPanelsDTO) {
        validatePanelMembers(interviewPanelsDTO,null);
        InterviewPanelsEntity interviewPanelsEntity = interviewPanelsMapper.toEntity(interviewPanelsDTO);
        updatePanelMembersInEntity(interviewPanelsDTO, interviewPanelsEntity);
        interviewPanelsEntity = interviewPanelsRepository.save(interviewPanelsEntity);
        return getInterviewPanelsDTOWithChildren(interviewPanelsEntity);
    }

    @Transactional(readOnly = true)
    public InterviewPanelsDTO findById(UUID id) {
        Optional<InterviewPanelsEntity> interviewPanelsEntityOptional = interviewPanelsRepository.findById(id);
        return interviewPanelsEntityOptional
                .map(this::getInterviewPanelsDTOWithChildren)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<InterviewPanelsDTO> findAll() {
        List<InterviewPanelsEntity> interviewPanelsEntities = interviewPanelsRepository.findAll(Sort.by(Sort.Direction.DESC, AppConstants.MASTER_CREATED_DATE));
        return getInterviewPanelsDTOListWithChildren(interviewPanelsEntities);
    }

    @Transactional(readOnly = true)
    public Page<InterviewPanelsDTO> searchInterviewPanels(
            String panelName, String committeeName, String panelMemberName, Pageable pageable) {

        Specification<InterviewPanelsEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (panelName != null && !panelName.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("panelName")), "%" + panelName.toLowerCase() + "%"));
            }

            if (committeeName != null && !committeeName.isEmpty()) {
                Join<InterviewPanelsEntity, InterviewCommitteeEntity> committeeJoin = root.join("committee", JoinType.INNER);
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(committeeJoin.get("committeeName")), "%" + committeeName.toLowerCase() + "%"));
            }

            if (panelMemberName != null && !panelMemberName.isEmpty()) {
                Join<InterviewPanelsEntity, InterviewPanelMembersEntity> panelMembersJoin = root.join("panelMembers", JoinType.INNER);
                Join<InterviewPanelMembersEntity, UserEntity> userJoin = panelMembersJoin.join("panelMember", JoinType.INNER);
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("name")), "%" + panelMemberName.toLowerCase() + "%"));
            }

            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<InterviewPanelsEntity> entitiesPage = interviewPanelsRepository.findAll(spec, pageable);

        Map<UUID, UserEntity> userMap = getAllUsersForPanels(entitiesPage.getContent());
        Map<UUID, InterviewCommitteeEntity> committeeMap = getAllCommitteesForPanels(entitiesPage.getContent());

        return entitiesPage.map(entity -> interviewPanelsMapper.toDto(entity, userMap, committeeMap));
    }

    @Transactional // Added @Transactional here to ensure the entire update operation is within a single transaction
    public InterviewPanelsDTO update(UUID id, InterviewPanelsDTO interviewPanelsDTO) throws MessagingException, IOException {
        InterviewPanelsEntity savedPanel =  updateEntity(id, interviewPanelsDTO);
        if (savedPanel != null){
            sendEmailToPanelMembers(savedPanel);
        }
        return getInterviewPanelsDTOWithChildren(savedPanel);
    }
    public boolean findWhetherScheduledOrNot(UUID panelId){
        LocalDateTime currentTime=LocalDateTime.now();
        Specification<InterviewScheduleEntity> specSchedule= buildSpecificationToGetFutureSchedules(panelId,currentTime);
        List<InterviewScheduleEntity> interviewsWithThisPanel=interviewScheduleRepository.findAll(specSchedule);
        if(!interviewsWithThisPanel.isEmpty()){
            return true;
        }
        return false;
    }
    public Specification buildSpecificationToGetFutureSchedules(UUID panelId, LocalDateTime currentTime){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("panelId"), panelId));
            predicates.add(criteriaBuilder.greaterThan(root.get("interviewStartAt"), currentTime));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public InterviewPanelsEntity updateEntity(UUID id, InterviewPanelsDTO interviewPanelsDTO) {
        validatePanelMembers(interviewPanelsDTO,id);
        //validateWhetherScheduledOrNot(id);
        Optional<InterviewPanelsEntity> existingPanelOptional = interviewPanelsRepository.findByIdWithPanelMembers(id); // Use the new method
        if (existingPanelOptional.isPresent()) {
            InterviewPanelsEntity existingPanel = existingPanelOptional.get();
            // No need for Hibernate.initialize() here as it's fetched eagerly
            interviewPanelsMapper.updateEntityFromDto(interviewPanelsDTO, existingPanel);
            updatePanelMembersInEntity(interviewPanelsDTO, existingPanel);
            return interviewPanelsRepository.save(existingPanel);
        }
        return null;
    }

    public void sendEmailToPanelMembers(InterviewPanelsEntity interviewPanelsEntity) throws MessagingException, IOException {
//        List<InterviewScheduleEntity> existingInterviews = interviewScheduleRepository.findByPanelId(interviewPanelsEntity.getId());
        Specification<InterviewScheduleEntity> specSchedule= buildSpecificationToGetFutureSchedules(interviewPanelsEntity.getId(), LocalDateTime.now());
        List<InterviewScheduleEntity> existingInterviews= interviewScheduleRepository.findAll(specSchedule);
        if (!existingInterviews.isEmpty()) {
            List<CandidateApplicationsEntity> candidateApplicationsEntityList=candidateApplicationsRepository.findAllById(
                    existingInterviews.stream().map(InterviewScheduleEntity::getApplicationId).toList());
            mailSenderHelper.sendMailToCandidateAndRecruiters(existingInterviews,candidateApplicationsEntityList);
        }
    }

    @Transactional
    public boolean delete(UUID id) {
        List<PositionPanelEntity> positionPanelEntities=positionPanelRepository.findByInterviewPanel_Id(id);
        validateWhetherScheduledOrNot(id);
        if (!positionPanelEntities.isEmpty()) {
            throw new ManualValidationException(
                    "Cannot delete the panel as it is assigned to a position"
            );
        }
        Optional<InterviewPanelsEntity> existingPanelOptional = interviewPanelsRepository.findById(id);
        if (existingPanelOptional.isPresent()) {
            interviewPanelsRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private void validatePanelMembers(InterviewPanelsDTO interviewPanelsDTO,UUID panelId) {
        InterviewCommitteeDTO committeeDTO = interviewPanelsDTO.getCommittee();

        List<UUID> userIds =
                interviewPanelsDTO.getPanelMembers()
                        .stream()
                        .map(InterviewPanelMembersDTO::getPanelMember)
                        .map(UserDTO::getId)
                        .toList();

        List<InterviewPanelsEntity> existingPanels =
                interviewPanelsRepository.findByCommittee_Id(committeeDTO.getId());
        Set<UUID> requestedUserSet = new HashSet<>(userIds);
        for (InterviewPanelsEntity existingPanel : existingPanels) {
            if (panelId != null && existingPanel.getId().equals(panelId)) {
                continue;
            }
            Set<UUID> existingUserSet =
                    existingPanel.getPanelMembers()
                            .stream()
                            .map(InterviewPanelMembersEntity::getPanelMember)
                            .map(UserEntity::getId)
                            .collect(Collectors.toSet());
            if (existingUserSet.equals(requestedUserSet)) {
                throw new ManualValidationException(
                        String.format(
                                "A panel '%s' already exists in '%s' committee  with the same members. Use the existing panel",
                                existingPanel.getPanelName(),
                                existingPanel.getCommittee().getCommitteeName()
                        )
                );
            }
        }
        Optional<InterviewPanelsEntity> optionalInterviewPanelsEntity;
        if(panelId==null) {
            optionalInterviewPanelsEntity=interviewPanelsRepository.findByPanelNameAndCommittee_Id(interviewPanelsDTO.getPanelName(), interviewPanelsDTO.getCommittee().getId());
        }
        else{
            optionalInterviewPanelsEntity=interviewPanelsRepository.findByPanelNameAndCommittee_IdAndIdNot(interviewPanelsDTO.getPanelName(), interviewPanelsDTO.getCommittee().getId(),panelId);
        }
        if(optionalInterviewPanelsEntity.isPresent()){
            InterviewPanelsEntity interviewPanelsEntity=optionalInterviewPanelsEntity.get();
            InterviewCommitteeEntity interviewCommitteeEntity=interviewPanelsEntity.getCommittee();
            throw new ManualValidationException(
                    String.format(
                            "Panel %s already exists in %s committee . Please use a different panel name.",
                            interviewPanelsEntity.getPanelName(),
                            interviewCommitteeEntity.getCommitteeName()
                    )
            );
        }

        if (interviewPanelsDTO.getPanelMembers() != null && !interviewPanelsDTO.getPanelMembers().isEmpty()) {
            Set<UUID> memberIds = new HashSet<>();
            for (InterviewPanelMembersDTO member : interviewPanelsDTO.getPanelMembers()) {
                if (member.getPanelMember() != null && member.getPanelMember().getId() != null) {
                    if (!memberIds.add(member.getPanelMember().getId())) {
                        throw new ManualValidationException("Duplicate panel member found: " + member.getPanelMember().getName() + " (ID: " + member.getPanelMember().getId() + ") in the same panel.");
                    }
                }
            }
        }
    }
    public void validateWhetherScheduledOrNot(UUID panelId){
        List<PositionPanelEntity> positionPanelEntities=positionPanelRepository.findByInterviewPanel_Id(panelId);
        Set<JobPositionsEntity> jobPositionsEntities=positionPanelEntities.stream().map(PositionPanelEntity::getJobPosition).collect(Collectors.toSet());
        Set<UUID> positionIds=jobPositionsEntities.stream().map(JobPositionsEntity::getId).collect(Collectors.toSet());
        Set<UUID> masterPositionIds=positionPanelEntities.stream().map(positionPanelEntity -> positionPanelEntity.getJobPosition().getMasterPositionId()).collect(Collectors.toSet());
        Map<UUID,MasterPositionsEntity> masterPositionsEntityMap=masterPositionsRepository.findAllById(masterPositionIds).stream()
                .collect(Collectors.toMap(MasterPositionsEntity::getId, m -> m));
        List<CandidateApplicationsEntity> candidateApplicationsEntityList=candidateApplicationsRepository.findByPositionIdIn(positionIds);
        List<UUID> applicationIds=candidateApplicationsEntityList.stream().map(CandidateApplicationsEntity::getId).toList();
        List<InterviewScheduleEntity> interviewScheduleEntities=interviewScheduleRepository.findByApplicationIdIn(applicationIds);
        List<InterviewScheduleEntity> interviewsWithThisPanel=interviewScheduleEntities.stream()
                .filter(interviewScheduleEntity -> interviewScheduleEntity.getPanelId().equals(panelId))
                .toList();
        //To get position Names for which interviews are scheduled with this panel
        List<UUID> applicationIdsScheduledForPanel=interviewsWithThisPanel.stream().map(InterviewScheduleEntity::getApplicationId).toList();
        List<CandidateApplicationsEntity> candidateApplicationsEntities=candidateApplicationsEntityList.stream().filter(candidateApplications -> applicationIdsScheduledForPanel.contains(candidateApplications.getId())).toList();
        List<UUID> positionIdsScheduledForPanel=candidateApplicationsEntities.stream().map(CandidateApplicationsEntity::getPositionId).toList();
//        log.info("Position IDs for which interviews are scheduled with this panel: {}", positionIdsScheduledForPanel);
        if(!interviewsWithThisPanel.isEmpty()){
            Map<UUID, UUID> jobPositionToMasterPositionMap =
                    jobPositionsEntities.stream()
                            .collect(Collectors.toMap(
                                    JobPositionsEntity::getId,
                                    JobPositionsEntity::getMasterPositionId
                            ));

            String masterPositionNames =
                    positionIdsScheduledForPanel.stream()
                            .map(jobPositionToMasterPositionMap::get)
                            .map(masterPositionsEntityMap::get)
                            .filter(Objects::nonNull)
                            .map(MasterPositionsEntity::getPositionName)
                            .collect(Collectors.joining(", "));
//            log.info("Master Position Names for which interviews are scheduled with this panel: {}", masterPositionNames);
            throw new ManualValidationException(
                    "Panel cannot be updated or deleted because interviews are already scheduled under this panel."
            );
//                            " "+masterPositionNames
//            );
        }
    }


    private void updatePanelMembersInEntity(InterviewPanelsDTO dto, InterviewPanelsEntity entity) {
        if (dto.getPanelMembers() == null || dto.getPanelMembers().isEmpty()) {
            entity.getPanelMembers().clear();
            return;
        }

        Map<UUID, InterviewPanelMembersDTO> dtoMembersMap = dto.getPanelMembers().stream()
                .filter(m -> m.getId() != null)
                .collect(Collectors.toMap(InterviewPanelMembersDTO::getId, m -> m));

        entity.getPanelMembers().removeIf(existingMember ->
                existingMember.getId() == null || !dtoMembersMap.containsKey(existingMember.getId())
        );

        for (InterviewPanelMembersDTO dtoMember : dto.getPanelMembers()) {
            if (dtoMember.getId() == null) {
                InterviewPanelMembersEntity newMember = interviewPanelMembersMapper.toEntity(dtoMember);
                newMember.setPanel(entity);
                entity.getPanelMembers().add(newMember);
            } else {
                entity.getPanelMembers().stream()
                        .filter(existingMember -> existingMember.getId() != null && existingMember.getId().equals(dtoMember.getId()))
                        .findFirst()
                        .ifPresentOrElse(existingMember -> {
                            interviewPanelMembersMapper.updateEntityFromDto(dtoMember, existingMember);
                            existingMember.setPanel(entity);
                        }, () -> {
                            InterviewPanelMembersEntity newMember = interviewPanelMembersMapper.toEntity(dtoMember);
                            newMember.setPanel(entity);
                            entity.getPanelMembers().add(newMember);
                        });
            }
        }
    }

    // Ensure this method runs in a transaction
    private InterviewPanelsDTO getInterviewPanelsDTOWithChildren(InterviewPanelsEntity interviewPanelsEntity) {
        if (interviewPanelsEntity == null) {
            return null;
        }

        Set<UUID> userIds = interviewPanelsEntity.getPanelMembers().stream()
                .map(InterviewPanelMembersEntity::getPanelMember)
                .filter(Objects::nonNull)
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
        Map<UUID, UserEntity> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));

        Map<UUID, InterviewCommitteeEntity> committeeMap = new HashMap<>();
        if (interviewPanelsEntity.getCommittee() != null) {
            // Re-fetch the committee to ensure it's fully managed and initialized within this transaction
            interviewCommitteeRepository.findById(interviewPanelsEntity.getCommittee().getId())
                    .ifPresent(committee -> committeeMap.put(committee.getId(), committee));
        }

        return interviewPanelsMapper.toDto(interviewPanelsEntity, userMap, committeeMap);
    }

    private List<InterviewPanelsDTO> getInterviewPanelsDTOListWithChildren(List<InterviewPanelsEntity> interviewPanelsEntities) {
        if (interviewPanelsEntities == null || interviewPanelsEntities.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, UserEntity> userMap = getAllUsersForPanels(interviewPanelsEntities);
        Map<UUID, InterviewCommitteeEntity> committeeMap = getAllCommitteesForPanels(interviewPanelsEntities);

        return interviewPanelsMapper.toDtoList(interviewPanelsEntities, userMap, committeeMap);
    }

    private Map<UUID, UserEntity> getAllUsersForPanels(List<InterviewPanelsEntity> interviewPanelsEntities) {
        Set<UUID> allUserIds = interviewPanelsEntities.stream()
                .flatMap(panel -> panel.getPanelMembers().stream())
                .map(InterviewPanelMembersEntity::getPanelMember)
                .filter(Objects::nonNull)
                .map(UserEntity::getId)
                .collect(Collectors.toSet());
        return userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));
    }

    private Map<UUID, InterviewCommitteeEntity> getAllCommitteesForPanels(List<InterviewPanelsEntity> interviewPanelsEntities) {
        Set<UUID> allCommitteeIds = interviewPanelsEntities.stream()
                .map(InterviewPanelsEntity::getCommittee)
                .filter(Objects::nonNull)
                .map(InterviewCommitteeEntity::getId)
                .collect(Collectors.toSet());
        return interviewCommitteeRepository.findAllById(allCommitteeIds).stream()
                .collect(Collectors.toMap(InterviewCommitteeEntity::getId, committee -> committee));
    }

    public List<UserDTO> getPanelMembers() {
        List<UserEntity> userEntities=userRepository.findByRoleIn(List.of(UserRole.RECRUITER.getValue(),UserRole.COMMITTEE_MEMBER.getValue()));
        return userMapper.toDTOList(userEntities);
    }

    public Workbook generatePanelExcelTemplate() {

        Workbook workbook = new XSSFWorkbook();

        Sheet panelSheet = createPanelSheet(workbook);
        Sheet memberSheet = createPanelMemberSheet(workbook);

        int committeeSize = createCommitteeLookupSheet(workbook);
        int userSize = createUserLookupSheet(workbook);

        applyDropdown(panelSheet, AppConstants.COMMITTEE_LOOKUP, 2, committeeSize);
        applyDropdown(memberSheet, AppConstants.USER_LOOKUP, 1, userSize);

        workbook.setSheetHidden(workbook.getSheetIndex(AppConstants.COMMITTEE_LOOKUP), true);
        workbook.setSheetHidden(workbook.getSheetIndex(AppConstants.USER_LOOKUP), true);
        for (int i = 0; i < 3; i++) {
            panelSheet.autoSizeColumn(i);
        }
        for (int i = 0; i < 2; i++) {
            memberSheet.autoSizeColumn(i);
        }
        return workbook;
    }

    private Sheet createPanelSheet(Workbook workbook) {

        Sheet sheet = workbook.createSheet("PanelSheet");

        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue("Panel Number");
        header.createCell(1).setCellValue("Panel Name");
        header.createCell(2).setCellValue("Committee Name");

        return sheet;
    }

    private Sheet createPanelMemberSheet(Workbook workbook) {

        Sheet sheet = workbook.createSheet("PanelMemberSheet");

        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue("Panel Number");
        header.createCell(1).setCellValue("User");

        return sheet;
    }

    private int createCommitteeLookupSheet(Workbook workbook) {

        Sheet sheet = workbook.createSheet(AppConstants.COMMITTEE_LOOKUP);

        List<InterviewCommitteeEntity> committees = interviewCommitteeRepository.findAll();

        int rowIndex = 0;

        for (InterviewCommitteeEntity committee : committees) {

            Row row = sheet.createRow(rowIndex);

            row.createCell(0).setCellValue(committee.getCommitteeName());
            row.createCell(1).setCellValue(committee.getId().toString());

            rowIndex++;
        }

        return rowIndex;
    }

    private int createUserLookupSheet(Workbook workbook) {

        Sheet sheet = workbook.createSheet(AppConstants.USER_LOOKUP);

        List<UserEntity> users = userRepository.findByRoleIn(List.of(UserRole.RECRUITER.getValue(),UserRole.COMMITTEE_MEMBER.getValue()));

        int rowIndex = 0;

        for (UserEntity user : users) {

            Row row = sheet.createRow(rowIndex);

            String display = user.getName() + " - " + user.getEmail();

            row.createCell(0).setCellValue(display);
            row.createCell(1).setCellValue(user.getId().toString());

            rowIndex++;
        }

        return rowIndex;
    }

    private void applyDropdown(
            Sheet sheet,
            String hiddenSheet,
            int columnIndex,
            int size) {

        DataValidationHelper helper = sheet.getDataValidationHelper();

        String formula = hiddenSheet + "!$A$1:$A$" + size;

        DataValidationConstraint constraint =
                helper.createFormulaListConstraint(formula);

        CellRangeAddressList address =
                new CellRangeAddressList(1, 1000, columnIndex, columnIndex);

        DataValidation validation =
                helper.createValidation(constraint, address);

        sheet.addValidationData(validation);
    }

    private Map<String, UUID> loadLookupMap(Workbook workbook, String sheetName) {

        Sheet sheet = workbook.getSheet(sheetName);

        Map<String, UUID> map = new HashMap<>();

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);
            if (row == null) continue;

            String display = row.getCell(0).getStringCellValue();
            UUID id = UUID.fromString(row.getCell(1).getStringCellValue());

            map.put(display, id);
        }

        return map;
    }
    @Transactional
    public List<InterviewPanelsDTO> uploadExcel(MultipartFile excelFile) {

        try (Workbook workbook = new XSSFWorkbook(excelFile.getInputStream())) {
            if(workbook.getNumberOfSheets()<2){
                throw new ExcelValidationException(
                        List.of("Invalid file. Please use the provided template."));
            }
            Sheet panelSheet = getRequiredSheet(workbook, "PanelSheet");
            Sheet memberSheet = getRequiredSheet(workbook, "PanelMemberSheet");

            Map<String, UUID> committeeLookup = loadLookupMap(workbook, AppConstants.COMMITTEE_LOOKUP);
            Map<String, UUID> userLookup = loadLookupMap(workbook, AppConstants.USER_LOOKUP);
            List<String> errors=new ArrayList<>();
            Set<Integer> panelSerialNumbers = extractPanelSerialNumbers(panelSheet);

            Map<Integer, List<UUID>> membersByPanel =
                    readPanelMembers(memberSheet, userLookup, errors, panelSerialNumbers);

            List<InterviewPanelsDTO> panelDTOs =
                    readPanels(panelSheet, committeeLookup, membersByPanel,errors);
            if (panelDTOs.isEmpty() && errors.isEmpty()) {
                throw new ExcelValidationException(
                        List.of("The file is empty. Please add some data and try again.")
                );
            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException(errors);
            }
            validateDuplicatesInExcel(panelDTOs);

            validateDatabaseReferences(panelDTOs);

            validatePanelsUsingService(panelDTOs);
            List<InterviewPanelsEntity> savedPanels = savePanels(panelDTOs);

            return getInterviewPanelsDTOListWithChildren(savedPanels);

        } catch (ManualValidationException | ExcelValidationException e){
            throw e;
        }
        catch (Exception e) {
            log.error("Excel upload failed", e);
            throw new ManualValidationException("Excel upload failed");
        }
    }
    private void validateDuplicatesInExcel(List<InterviewPanelsDTO> panelDTOs) {

        List<String> errors = new ArrayList<>();

        // 1. Panel Name + Committee duplicate
        Set<String> panelKeys = new HashSet<>();

        // 2. Same member combination duplicate
        Set<String> memberCombinationKeys = new HashSet<>();

        int rowNumber = 2;
        Map<UUID, InterviewCommitteeEntity> interviewCommitteeMap = interviewCommitteeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        InterviewCommitteeEntity::getId,
                        entity -> entity
                ));

        for (InterviewPanelsDTO dto : panelDTOs) {

            String panelKey = dto.getPanelName().toLowerCase().trim()
                    + "_" + dto.getCommittee().getId();

            if (!panelKeys.add(panelKey)) {
                String committeeName = interviewCommitteeMap.get(dto.getCommittee().getId()).getCommitteeName();
                errors.add("Row " + rowNumber + ": A panel with the name '" + dto.getPanelName() + "' already exists for the '" + committeeName + "' committee. Please use a different name.");
                // Sort member IDs to ensure order-independent comparison
                List<UUID> memberIds = dto.getPanelMembers().stream()
                        .map(m -> m.getPanelMember().getId())
                        .sorted()
                        .toList();

                String memberKey = memberIds.toString();

                if (!memberCombinationKeys.add(memberKey)) {
                    errors.add("Row " + rowNumber + ": A panel with the same members already exists. Please modify the panel members.");
                }

                rowNumber++;
            }

            if (!errors.isEmpty()) {
                throw new ExcelValidationException(errors);
            }
        }
    }

    private Sheet getRequiredSheet(Workbook workbook, String sheetName) {

        Sheet sheet = workbook.getSheet(sheetName);

        if (sheet == null) {
            throw new ExcelValidationException(
                    List.of("Invalid excel sheet format. A required sheet is missing. Please use the provided template."));
        }

        return sheet;
    }
    private Set<Integer> extractPanelSerialNumbers(Sheet panelSheet) {

        Set<Integer> serialNumbers = new HashSet<>();

        for (int i = 1; i <= panelSheet.getLastRowNum(); i++) {

            Row row = panelSheet.getRow(i);
            if (row == null) continue;

            Cell cell = row.getCell(0);

            if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                serialNumbers.add((int) cell.getNumericCellValue());
            }
        }

        return serialNumbers;
    }
    private Map<Integer, List<UUID>> readPanelMembers(
            Sheet memberSheet,
            Map<String, UUID> userLookup,
            List<String> errors,
            Set<Integer> panelSerialNumbers) {

        Map<Integer, List<UUID>> membersByPanel = new HashMap<>();

        for (int i = 1; i <= memberSheet.getLastRowNum(); i++) {

            Row row = memberSheet.getRow(i);
            if (row == null) continue;

            int rowNum = i + 1;

            List<String> rowErrors = new ArrayList<>();

            Integer serialNumber = null;
            String userDisplay = null;

            try {
                Cell serialCell = row.getCell(0);
                Cell userCell = row.getCell(1);

                if (serialCell != null && serialCell.getCellType() == CellType.NUMERIC) {
                    serialNumber = (int) serialCell.getNumericCellValue();
                }

                if (userCell != null) {
                    userDisplay = userCell.getStringCellValue().trim();
                }

            } catch (Exception e) {
                rowErrors.add("Invalid data format");
            }

            // Mandatory
            if (serialNumber == null) rowErrors.add("Panel Number is mandatory");
            if (userDisplay == null || userDisplay.isBlank()) rowErrors.add("User is mandatory");

            // Lookup
            if (userDisplay != null && userLookup.get(userDisplay) == null) {
                rowErrors.add("Invalid user selected");
            }
            if (serialNumber != null && !panelSerialNumbers.contains(serialNumber)) {
                rowErrors.add("Panel Number:"+ serialNumber+ " not found in Panel sheet.");
            }
            //  Add ONE row error
            if (!rowErrors.isEmpty()) {
                errors.add("PanelMember Sheet Row " + rowNum + ": "+ String.join(" | ", rowErrors));
                continue;
            }

            membersByPanel
                    .computeIfAbsent(serialNumber, k -> new ArrayList<>())
                    .add(userLookup.get(userDisplay));
        }
        for (Map.Entry<Integer, List<UUID>> entry : membersByPanel.entrySet()) {

            Set<UUID> uniqueMembers = new HashSet<>(entry.getValue());

            if (uniqueMembers.size() != entry.getValue().size()) {
                errors.add("Duplicate users found in Panel Number " + entry.getKey());
            }
        }

        return membersByPanel;
    }
    private List<InterviewPanelsDTO> readPanels(
            Sheet panelSheet,
            Map<String, UUID> committeeLookup,
            Map<Integer, List<UUID>> membersByPanel,
            List<String> errors) {

        List<InterviewPanelsDTO> panelDTOs = new ArrayList<>();

        for (int i = 1; i <= panelSheet.getLastRowNum(); i++) {

            Row row = panelSheet.getRow(i);
            if (row == null) continue;

            int rowNum = i + 1;

            List<String> rowErrors = new ArrayList<>();

            Integer serialNumber = null;
            String panelName = null;
            String committeeDisplay = null;

            try {
                Cell serialCell = row.getCell(0);
                if (serialCell != null && serialCell.getCellType() == CellType.NUMERIC) {
                    serialNumber = (int) serialCell.getNumericCellValue();
                }

                panelName = row.getCell(1) != null ? row.getCell(1).getStringCellValue().trim() : null;
                committeeDisplay = row.getCell(2) != null ? row.getCell(2).getStringCellValue().trim() : null;

            } catch (Exception e) {
                rowErrors.add("Invalid data format");
            }

            // Mandatory
            if (serialNumber == null) rowErrors.add("Panel Number is mandatory");
            if (panelName == null || panelName.isBlank()) rowErrors.add("Panel Name is mandatory");
            if (committeeDisplay == null || committeeDisplay.isBlank()) rowErrors.add("Committee Name is mandatory");

            // Lookup
            UUID committeeId = committeeLookup.get(committeeDisplay);

            if (committeeDisplay != null && committeeId == null) {
                rowErrors.add("Invalid committee selected");
            }

            // Members validation
            List<UUID> memberIds =
                    membersByPanel.getOrDefault(serialNumber, Collections.emptyList());

            if (serialNumber != null) {

                if (!membersByPanel.containsKey(serialNumber)) {
                    rowErrors.add("No panel members mapped for Panel Number " + serialNumber);
                } else if (membersByPanel.get(serialNumber).isEmpty()) {
                    rowErrors.add("Panel must have at least one member");
                }
            }

            if (!rowErrors.isEmpty()) {
                errors.add("Panel Sheet Row " + rowNum + ": " + String.join(" | ", rowErrors));
                continue;
            }

            // Build DTO
            InterviewPanelsDTO dto = new InterviewPanelsDTO();
            dto.setPanelName(panelName);

            InterviewCommitteeDTO committeeDTO = new InterviewCommitteeDTO();
            committeeDTO.setId(committeeId);
            dto.setCommittee(committeeDTO);

            List<InterviewPanelMembersDTO> memberDTOs = new ArrayList<>();

            for (UUID userId : memberIds) {
                InterviewPanelMembersDTO m = new InterviewPanelMembersDTO();
                UserDTO u = new UserDTO();
                u.setId(userId);
                m.setPanelMember(u);
                memberDTOs.add(m);
            }

            dto.setPanelMembers(memberDTOs);

            panelDTOs.add(dto);
        }

        return panelDTOs;
    }
    private void validateDatabaseReferences(List<InterviewPanelsDTO> panelDTOs) {

        Set<UUID> committeeIds =
                panelDTOs.stream()
                        .map(p -> p.getCommittee().getId())
                        .collect(Collectors.toSet());

        Set<UUID> userIds =
                panelDTOs.stream()
                        .flatMap(p -> p.getPanelMembers().stream())
                        .map(m -> m.getPanelMember().getId())
                        .collect(Collectors.toSet());

        Set<UUID> validCommitteeIds =
                interviewCommitteeRepository.findAllById(committeeIds)
                        .stream()
                        .map(InterviewCommitteeEntity::getId)
                        .collect(Collectors.toSet());

        Set<UUID> validUserIds =
                userRepository.findAllById(userIds)
                        .stream()
                        .map(UserEntity::getId)
                        .collect(Collectors.toSet());

        for (InterviewPanelsDTO dto : panelDTOs) {

            if (!validCommitteeIds.contains(dto.getCommittee().getId())) {
                throw new ExcelValidationException(
                        List.of("Committee does not exist in database"));
            }

            for (InterviewPanelMembersDTO member : dto.getPanelMembers()) {

                if (!validUserIds.contains(member.getPanelMember().getId())) {
                    throw new ExcelValidationException(
                            List.of("User does not exist in database"));
                }
            }
        }
    }

    private void validatePanelsUsingService(List<InterviewPanelsDTO> panelDTOs) {

        List<String> errors = new ArrayList<>();

        int rowNumber = 2;

        for (InterviewPanelsDTO dto : panelDTOs) {

            try {
                validatePanelMembers(dto, null);
            }
            catch (IllegalArgumentException ex) {
                errors.add("Row " + rowNumber + ": " + ex.getMessage());
            }

            rowNumber++;
        }

        if (!errors.isEmpty()) {
            throw new ExcelValidationException(errors);
        }
    }

    private List<InterviewPanelsEntity> savePanels(List<InterviewPanelsDTO> panelDTOs) {

        List<InterviewPanelsEntity> entities =
                interviewPanelsMapper.toEntityList(panelDTOs);

        for (InterviewPanelsEntity panel : entities) {

            if (panel.getPanelMembers() != null) {

                for (InterviewPanelMembersEntity member : panel.getPanelMembers()) {
                    member.setPanel(panel);
                }
            }
        }

        return interviewPanelsRepository.saveAll(entities);
    }
}
