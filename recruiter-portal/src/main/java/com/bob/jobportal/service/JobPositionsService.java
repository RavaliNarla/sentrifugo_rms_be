package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.model.ExcelTemplateFile;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.commonutil.service.FileService;
import com.bob.commonutil.util.SecurityUtils;
import com.bob.db.dto.*;
import com.bob.db.entity.*;
import com.bob.db.enums.PositionPanelStatus;
import com.bob.db.enums.PositionStatus;
import com.bob.db.enums.RequisitionEditStatus;
import com.bob.db.enums.RequisitionStatus;
import com.bob.db.enums.UserRole;
import com.bob.db.mapper.JobPositionExclusionsMapper;
import com.bob.db.mapper.JobPositionsMapper;
import com.bob.db.mapper.MasterPositionsMapper;
import com.bob.db.model.JobPositionsExcelModel; // Updated import
import com.bob.db.repository.*;
import com.bob.jobportal.model.JobPositionResponseModel;
import com.bob.jobportal.model.PositionVacancyBreakdownModel;
import com.bob.commonutil.util.AppConstants;
import com.bob.jobportal.util.ExcelDuplicationUtil;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

@Service
@Slf4j
public class JobPositionsService {
    private static final List<RequisitionEditStatus> ACTIVE_DRAFT_STATUSES = List.of(
            RequisitionEditStatus.DRAFT,
            RequisitionEditStatus.L1_PENDING,
            RequisitionEditStatus.L2_PENDING,
            RequisitionEditStatus.L1_REJECTED,
            RequisitionEditStatus.L2_REJECTED,
            RequisitionEditStatus.APPROVED
    );

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private PositionStateDistributionRepository positionStateDistributionRepository;

    @Autowired
    private PositionCategoryDistributionRepository positionCategoryDistributionRepository;

    @Autowired
    private PositionCategoryNationalDistributionRepository positionCategoryNationalDistributionRepository;

    @Autowired
    private JobRequisitionsRepository jobRequisitionsRepository;

    @Autowired
    private JobRequisitionEditRequestRepository jobRequisitionEditRequestRepository;

    @Autowired
    private JobPositionsMapper jobPositionsMapper;

    @Autowired
    private UserRepository userRepository;


    @Value("${indent.upload.dir}")
    private String indentUploadDir;

    @Value("${indent.http.url}")
    private String indentUploadUrl;

    @Autowired
    private FileService fs;

    @Autowired
    private ExcelTemplateService excelTemplateService;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private MasterPositionsMapper masterPositionsMapper;

    @Autowired
    private PositionPanelRepository positionPanelRepository;

    @Autowired
    private InterviewPanelMembersRepository interviewPanelMembersRepository;

    @Autowired
    private DepartmentsRepository departmentsRepository;

    @Autowired
    private EmployementTypesRepository employementTypesRepository;

    @Autowired
    private JobGradeRepository jobGradeRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Transactional
    public JobPositionsDTO createJobPosition(MultipartFile indentFile, JobPositionsDTO jobPositionsDTO){
        // Handle indent file upload
        if (indentFile != null && !indentFile.isEmpty()) {
            try {
                String fileName = AppConstants.INDENT_FILE_PREFIX + UUID.randomUUID();
                String uploadedPath = fs.uploadFile(indentFile, fileName, indentUploadDir);
                jobPositionsDTO.setIndentPath(indentUploadDir + "/" + uploadedPath);
                jobPositionsDTO.setIndentName(indentFile.getOriginalFilename());
                log.info("Indent file uploaded for job position: {}", uploadedPath);
            } catch (Exception e) {
                log.error("Error occurred during indent file save", e);
                throw new CommonException("Error occured during indent file save");
            }
        }
        JobPositionsEntity entity = jobPositionsMapper.toEntity(jobPositionsDTO);
        entity.setPositionStatus(PositionStatus.NEW);
        return jobPositionsMapper.toDto(positionsRepository.save(entity));
    }


    @Transactional
    public JobPositionsDTO updateJobPosition(MultipartFile indentFile, JobPositionsDTO jobPositionsDTO){
        if (jobPositionsDTO.getId() == null) {
            throw new IllegalArgumentException("Job Position ID is required for update");
        }
        JobPositionsEntity entity = positionsRepository.findById(jobPositionsDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job Position not found"));

        // Handle indent file upload
        if (indentFile != null && !indentFile.isEmpty()) {
            try {
                String fileName = AppConstants.INDENT_FILE_PREFIX + UUID.randomUUID();
                String uploadedPath = fs.uploadFile(indentFile, fileName, indentUploadDir);
                jobPositionsDTO.setIndentPath(indentUploadDir + "/" + uploadedPath);
                jobPositionsDTO.setIndentName(indentFile.getOriginalFilename());
                log.info("Indent file uploaded for job position: {}", uploadedPath);
            } catch (Exception e) {
                log.error("Error occurred during indent file save", e);
                throw new CommonException("Error occured during indent file save");
            }
        }

        // Map new data and save to execute INSERT statements
        jobPositionsMapper.updateEntityFromDto(jobPositionsDTO, entity);
        entity.setPositionStatus(PositionStatus.NEW);

        entity = positionsRepository.save(entity);

        // If the parent requisition was rejected, reset it to NEW so the recruiter
        // can resubmit for approval after editing the position.
        if (entity.getRequisitionId() != null) {
            jobRequisitionsRepository.findById(entity.getRequisitionId()).ifPresent(req -> {
                if (req.getRequisitionStatus() == RequisitionStatus.L1_REJECTED
                        || req.getRequisitionStatus() == RequisitionStatus.L2_REJECTED) {
                    req.setRequisitionStatus(RequisitionStatus.NEW);
                    jobRequisitionsRepository.save(req);
                }
            });
        }

       return jobPositionsMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public JobPositionsDTO getPositionById(UUID id) {
        return jobPositionsMapper.toDto(positionsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job Position not found")));
    }

    @Transactional(readOnly = true)
    public List<JobPositionsDTO> getPositionsByRequisitionId(UUID requisitionId) {
        List<JobPositionsEntity> entities = positionsRepository.findAllByRequisitionId(requisitionId);
        return entities.stream()
                .map(jobPositionsMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobPositionsDTO> getDraftPositionsByRequisitionId(UUID requisitionId) {
        JobRequisitionEditRequestEntity draft = jobRequisitionEditRequestRepository
                .findTopByParentRequisitionIdAndIsActiveTrueAndRequisitionStatusInOrderByCreatedDateDesc(requisitionId, ACTIVE_DRAFT_STATUSES)
                .orElseThrow(() -> new ResourceNotFoundException("Active requisition draft not found"));

        if (draft.getPositionEditRequests() == null) {
            return Collections.emptyList();
        }

        return draft.getPositionEditRequests().stream()
                .map(this::toDraftPositionDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePositionById(UUID id) {
        positionsRepository.deleteById(id);
    }

    public List<JobPositionsDTO> bulkSave(UUID requisitionId,MultipartFile file) {
        try {
            List<JobPositionsExcelModel> dtos = excelTemplateService.excelToDto(file.getInputStream(), JobPositionsExcelModel.class);

            // DO NOT REMOVE keep this commented code until confirmation
           /* List<String> approvedByEmails = dtos.stream()
                    .map(JobPositionsExcelModel::getApprovedByEmailId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, UUID> approvedByUserIds = userRepository.findByEmailIgnoreCaseIn(approvedByEmails)
                    .stream()
                    .collect(Collectors.toMap(UserEntity::getEmail, UserEntity::getId));*/

            List<JobPositionsEntity> jobPositionsEntities = new ArrayList<>();

            Set<String> dbKeys = new HashSet<>(positionsRepository.findAllCompositeKeysAsString(requisitionId));

            Function<JobPositionsExcelModel, String> dbKeyExtractor = dto ->
                    dto.getMasterPositionId() + "||" +
                            dto.getDeptId() + "||" +
                            requisitionId;

            List<String> errors = ExcelDuplicationUtil.validateAll(
                    dtos,
                    dbKeys,
                    dbKeyExtractor,
                    JobPositionsExcelModel::getMasterPositionId,
                    JobPositionsExcelModel::getDeptId
            );

            if(!errors.isEmpty()){
                throw new ExcelValidationException(errors);
            }

            for (JobPositionsExcelModel xls : dtos) {
                // DO NOT REMOVE keep this commented code until confirmation
                /*UUID approvedBy = approvedByUserIds.get(xls.getApprovedByEmailId());
                if (approvedBy == null) {
                    errors.add("Approved By Email ID not found: " + xls.getApprovedByEmailId());
                    continue;
                }*/

                JobPositionsEntity entity = jobPositionsMapper.toEntity(xls, PositionStatus.DRAFT);
                entity.setIsLocationWise(false);
                entity.setRequisitionId(requisitionId);
                entity.setIsMedicalRequired(true);
                jobPositionsEntities.add(entity);
            }



            return jobPositionsMapper.toDtoList(positionsRepository.saveAll(jobPositionsEntities));
        } catch (IOException e) {
            throw new CommonException("fail to store excel data.");
        }
    }

    public ExcelTemplateFile generateExcelTemplate() {
        return excelTemplateService.generateExcelTemplate(JobPositionsExcelModel.class);
    }


    @Transactional(readOnly = true)
    public List<JobPositionResponseModel> getActivePosByReqId(UUID requisitionId, String searchText) {
        List<JobPositionsEntity> entities;
        LocalDate today= LocalDate.now();
        boolean isCommitteeMember=securityUtils.getCurrentUserRole().equals(UserRole.COMMITTEE_MEMBER.getValue());
        Set<UUID> allowedPositionIds = null;
        if(isCommitteeMember){
            UUID currentUserId=securityUtils.getCurrentUserId();
            Set<UUID> panelIds = interviewPanelMembersRepository.findAllByPanelMember_Id(currentUserId).stream()
                    .map(member -> member.getPanel().getId())
                    .collect(Collectors.toSet());

            List<PositionPanelEntity> activePanels = positionPanelRepository.findActivePanels(panelIds, today,List.of(PositionPanelStatus.APPROVED));
            activePanels=activePanels.stream().filter(p->!p.getInterviewPanel().getCommittee().getCommitteeName().equals(AppConstants.INTERVIEW_COMMITTEE_NAME)).toList();
            allowedPositionIds = activePanels.stream()
                    .map(panel -> panel.getJobPosition().getId())
                    .collect(Collectors.toSet());

            // If no positions available for committee
            if (allowedPositionIds.isEmpty()) {
                return Collections.emptyList();
            }

        }
        Specification<JobPositionsEntity> spec = buildSpecification(searchText, requisitionId, PositionStatus.ACTIVE, allowedPositionIds);
        entities=positionsRepository.findAll(spec);
        Map<UUID, MasterPositionsDTO> positionsDTOMap=masterPositionsRepository.findAllById(entities.stream()
                .map(JobPositionsEntity::getMasterPositionId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(MasterPositionsEntity::getId, masterPositionsMapper::toDTO));
        return entities.stream()
                .map((entity)->{
                    JobPositionResponseModel responseModel = JobPositionResponseModel.builder()
                            .jobPositions(jobPositionsMapper.toDto(entity))
                            .masterPositions(positionsDTOMap.get(entity.getMasterPositionId()))
                            .build();
                    return responseModel;

                })
                .collect(Collectors.toList());
    }

    public Specification<JobPositionsEntity> buildSpecification(String searchFilter, UUID requisitionId, PositionStatus positionStatus, Set<UUID> allowedPositionIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Validate status
            predicates.add(cb.equal(root.get("positionStatus"), positionStatus));
            //Select by requisitionId
            predicates.add(cb.equal(root.get("requisitionId"), requisitionId));
            //Only allow some positions if user is committee member
            if (allowedPositionIds != null && !allowedPositionIds.isEmpty()) {
                predicates.add(root.get("id").in(allowedPositionIds));
            }
            // Cross join for position and master position
            Root<MasterPositionsEntity> masterRoot = query.from(MasterPositionsEntity.class);
            //Add a filter to remove remaining cross join results by matching master position id
            predicates.add(cb.equal(root.get("masterPositionId"), masterRoot.get("id")));
            // Search filter according to positionName
            if (searchFilter != null && !searchFilter.trim().isEmpty()) {
                String searchLower = "%" + searchFilter.trim().toLowerCase() + "%";
                //Filter by positionName
                predicates.add(cb.like(cb.lower(masterRoot.get("positionName")), searchLower));
            }
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    //job position excel download

    @Transactional(readOnly = true)
    public byte[] generateJobPositionExcelTemplate() {

        Workbook wb = new XSSFWorkbook();

        Sheet mainSheet = wb.createSheet("JobPositions");

        Sheet deptPositionHiddenSheet = wb.createSheet("DeptPositionHidden");
        Sheet deptHiddenSheet = wb.createSheet("DepartmentHidden");
        Sheet positonHiddenSheet = wb.createSheet("PositionHidden");
        Sheet empTypeHiddenSheet = wb.createSheet("EmploymentTypeHidden");
        Sheet gradeHiddenSheet = wb.createSheet("GradeHidden");

        createHeader(mainSheet);


        List<DepartmentsEntity> departments = departmentsRepository.findAll();
        List<MasterPositionsEntity> masterPositions = masterPositionsRepository.findAll();
        List<EmployementTypesEntity> empTypes = employementTypesRepository.findAll();
        List<JobGradeEntity> grades = jobGradeRepository.findAll();


        Map<UUID, String> deptIdToName = departments.stream()
                .collect(Collectors.toMap(DepartmentsEntity::getId, DepartmentsEntity::getDepartmentName));


        Map<String,UUID>  empTypeMap = empTypes.stream()
                .collect(Collectors.toMap(EmployementTypesEntity::getTypeName, EmployementTypesEntity::getId));
        Map<String,UUID> jobGradeMap = grades.stream()
                .collect(Collectors.toMap(JobGradeEntity::getJobGradeCode, JobGradeEntity::getId));

        Map<String, UUID> deptNameToId = new LinkedHashMap<>();
        Map<String, String> deptSafeMap = new HashMap<>();
        Map<String, List<String>> deptPositionMap = new LinkedHashMap<>();
        Map<String, UUID> positionMap = new HashMap<>();

        for (DepartmentsEntity d : departments) {
            String name = d.getDepartmentName();
            deptNameToId.put(name, d.getId());

            String safe = name.replaceAll("[^A-Za-z0-9]", "_");
            if (!Character.isLetter(safe.charAt(0)) && safe.charAt(0) != '_') {
                safe = "_" + safe;
            }
            deptSafeMap.put(name, safe);
        }

        for (MasterPositionsEntity masterPosition : masterPositions) {
            String deptName = deptIdToName.get(masterPosition.getDeptId());
            String positionDisplay = masterPosition.getPositionName() + "-" + masterPosition.getPositionCode();

            if (deptName == null) {
                continue;
            }

            deptPositionMap
                    .computeIfAbsent(deptName, k -> new ArrayList<>())
                    .add(positionDisplay);

            positionMap.put(positionDisplay, masterPosition.getId());
        }


        // 3. Populate Hidden Sheets
        populateDeptPositionHidden(wb, deptPositionHiddenSheet, deptPositionMap);
        populateDepartmentHidden(deptHiddenSheet, deptNameToId, deptSafeMap);
        populateSimpleHidden(positonHiddenSheet, positionMap);

        populateSimpleHidden(empTypeHiddenSheet, empTypeMap);

        populateSimpleHidden(gradeHiddenSheet, jobGradeMap);


        // 4. Create Dropdowns & Validations

        createDepartmentDropdown(mainSheet, deptHiddenSheet, deptNameToId.size());
        createPositionDropdown(mainSheet);

        createSimpleDropdown(mainSheet, empTypeHiddenSheet, 5,empTypeMap.size()); // Employment Type column index
        createSimpleDropdown(mainSheet, gradeHiddenSheet, 6, jobGradeMap.size());   // Grade column index

        // Yes/No dropdown for Enable Location Preferences (Column 7)
        createYesNoDropdown(mainSheet, 7);


        // 2=Total Vacancies, 3=Eligibility Age Min, 4=Eligibility Age Max
        // 8=Mandatory Exp Months, 9=Preferred Exp Monts, 13=Contract Years
        createWholeNumberValidation(mainSheet, 2, 3, 4, 8,9,13);





        // 5. Hide Sheets

        wb.setSheetHidden(wb.getSheetIndex(deptPositionHiddenSheet), true);
        wb.setSheetHidden(wb.getSheetIndex(deptHiddenSheet), true);
        wb.setSheetHidden(wb.getSheetIndex(positonHiddenSheet), true);
        wb.setSheetHidden(wb.getSheetIndex(empTypeHiddenSheet), true);
        wb.setSheetHidden(wb.getSheetIndex(gradeHiddenSheet), true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            wb.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return outputStream.toByteArray();
    }

    private void createHeader(Sheet sheet) {
        Workbook workbook = sheet.getWorkbook();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        Row row = sheet.createRow(0);


        List<String> headers = AppConstants.JOB_POSITION_EXCEL_HEADERS;

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDepartmentDropdown(Sheet mainSheet, Sheet deptHiddenSheet, int count) {
        DataValidationHelper helper = mainSheet.getDataValidationHelper();
        String formula = "'" + deptHiddenSheet.getSheetName() + "'!$A$2:$A$" + (count + 1);
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        CellRangeAddressList range = new CellRangeAddressList(1, 500, 0, 0);

        DataValidation validation = helper.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Invalid Input", "Please select a valid Department from the dropdown.");


        mainSheet.addValidationData(validation);
    }

    private void createPositionDropdown(Sheet mainSheet) {
        DataValidationHelper helper = mainSheet.getDataValidationHelper();
        String formula = "INDIRECT(VLOOKUP($A2, 'DepartmentHidden'!$A:$C, 3, FALSE))";
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        CellRangeAddressList range = new CellRangeAddressList(1, 500, 1, 1);

        DataValidation validation = helper.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Invalid Input", "Please select a valid Position from the dropdown.");


        mainSheet.addValidationData(validation);
    }

    private void createSimpleDropdown(Sheet mainSheet, Sheet hiddenSheet, int column, int size) {
        DataValidationHelper helper = mainSheet.getDataValidationHelper();

        // Dynamically set the row limit based on the size of the data
        String formula = "'" +  hiddenSheet.getSheetName() + "'!$A$2:$A$" + (size + 1);

        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        CellRangeAddressList range = new CellRangeAddressList(1, 500, column, column);

        DataValidation validation = helper.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Invalid Input", "Please select a valid option from the dropdown.");


        mainSheet.addValidationData(validation);
    }

    private void createYesNoDropdown(Sheet mainSheet, int column) {
        DataValidationHelper helper = mainSheet.getDataValidationHelper();
        // Explicit array for Yes/No dropdown
        DataValidationConstraint constraint = helper.createExplicitListConstraint(new String[]{"Yes", "No"});
        CellRangeAddressList range = new CellRangeAddressList(1, 500, column, column);

        DataValidation validation = helper.createValidation(constraint, range);
        // STOPS manual typing
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Invalid Input", "Please select Yes or No.");

        mainSheet.addValidationData(validation);
    }

    private void createWholeNumberValidation(Sheet sheet,int... columns) {
        DataValidationHelper helper = sheet.getDataValidationHelper();

        DataValidationConstraint constraint = helper.createNumericConstraint(
                DataValidationConstraint.ValidationType.INTEGER,
                DataValidationConstraint.OperatorType.GREATER_OR_EQUAL,
                "0", null);


        for (int col : columns) {
            CellRangeAddressList range = new CellRangeAddressList(1, 500, col, col);
            DataValidation validation = helper.createValidation(constraint, range);

            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox("Invalid Input", "Please enter a whole number.");
            sheet.addValidationData(validation);
        }
    }

    private void populateSimpleHidden(Sheet hiddenSheet, Map<String, UUID> map) {
        Row header = hiddenSheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Id");

        int rowIndex = 1;
        for (Map.Entry<String, UUID> e : map.entrySet()) {
            Row row = hiddenSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue().toString());
        }
    }

    private void populateDeptPositionHidden(
            Workbook workbook,
            Sheet deptPositionHiddenSheet,
            Map<String, List<String>> deptPositionMap) {

        Row header = deptPositionHiddenSheet.createRow(0);
        int colIndex = 0;

        for (Map.Entry<String, List<String>> entry : deptPositionMap.entrySet()) {
            String deptName = entry.getKey();
            List<String> positions = entry.getValue();

            // Header = Department Name
            header.createCell(colIndex).setCellValue(deptName);

            // Fill positions vertically
            for (int i = 0; i < positions.size(); i++) {
                Row row = deptPositionHiddenSheet.getRow(i + 1);
                if (row == null) row = deptPositionHiddenSheet.createRow(i + 1);

                row.createCell(colIndex).setCellValue(positions.get(i));
            }

            // Create named range for this department
            createNamedRange(workbook, deptPositionHiddenSheet, deptName, positions.size(), colIndex);

            colIndex++;
        }
    }

    private void populateDepartmentHidden(
            Sheet deptHiddenSheet,
            Map<String, UUID> deptNameToId,
            Map<String, String> deptSafeMap) {

        Row header = deptHiddenSheet.createRow(0);
        header.createCell(0).setCellValue("DepartmentName");
        header.createCell(1).setCellValue("DepartmentId");
        header.createCell(2).setCellValue("SafeName");

        int rowIndex = 1;

        for (Map.Entry<String, UUID> entry : deptNameToId.entrySet()) {
            String deptName = entry.getKey();
            UUID deptId = entry.getValue();

            Row row = deptHiddenSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(deptName);
            row.createCell(1).setCellValue(deptId.toString());
            row.createCell(2).setCellValue(deptSafeMap.get(deptName));
        }
    }


    private void createNamedRange(
            Workbook workbook,
            Sheet sheet,
            String deptName,
            int size,
            int colIndex) {

        Name namedRange = workbook.createName();

        // Convert to safe Excel name
        String safeName = deptName.replaceAll("[^A-Za-z0-9]", "_");

        if (!Character.isLetter(safeName.charAt(0)) && safeName.charAt(0) != '_') {
            safeName = "_" + safeName;
        }

        namedRange.setNameName(safeName);

        String colLetter = CellReference.convertNumToColString(colIndex);


        String formula = "'" + sheet.getSheetName() + "'!$" + colLetter + "$2:$" + colLetter + "$" + (size + 1);

        namedRange.setRefersToFormula(formula);
    }
    //job position excel download end


    //to read from uploaded excel and save to db
    @Transactional
    public List<JobPositionsDTO> bulkSaveFromExcel(UUID requisitionId,MultipartFile file) {

        List<JobPositionsEntity> alreadyExistingPositions = positionsRepository.findAllByRequisitionId(requisitionId);
        Set<String> existingKeys = alreadyExistingPositions.stream()
                .map((position)->position.getDeptId()+"||"+position.getMasterPositionId())
                .collect(Collectors.toSet());
        List<JobPositionsExcelModel> jobPositionsExcelModels = readFromUploadedExcel(existingKeys,file);

        List<JobPositionsEntity> entities = jobPositionsExcelModels.stream()
                .map(model -> {
                    JobPositionsEntity entity = jobPositionsMapper.toEntity(model, PositionStatus.DRAFT);
                    entity.setRequisitionId(requisitionId);
                    entity.setIsLocationWise(false);
                    entity.setRequisitionId(requisitionId);
                    entity.setIsMedicalRequired(true);
                    return  entity;
                }).collect(Collectors.toList());

               positionsRepository.saveAll(entities);

               return jobPositionsMapper.toDtoList(entities);
    }

    public List<JobPositionsExcelModel> readFromUploadedExcel(Set<String> existingKeys,MultipartFile file) {

          Workbook workbook = null;
           try{
               workbook = new XSSFWorkbook(file.getInputStream());
           }catch (Exception e) {
               throw new ExcelValidationException(List.of("Failed to read excel file. Please ensure the file is a valid .xlsx format."));
           }
            Sheet templateSheet = workbook.getSheet("JobPositions");
            if (templateSheet == null) {
                throw new ExcelValidationException(List.of("Invalid excel sheet format. " +
                        "Please download the template and upload the file in the correct format."));
            }
            validateHeaders(templateSheet.getRow(0));
          Sheet deptHiddenSheet = workbook.getSheet("DepartmentHidden");
          Sheet positonHiddenSheet = workbook.getSheet("PositionHidden");
          Sheet empTypeHiddenSheet = workbook.getSheet("EmploymentTypeHidden");
          Sheet gradeHiddenSheet = workbook.getSheet("GradeHidden");

          Map<String,UUID> departmentLookUp = buildSimpleLookup(deptHiddenSheet);
          Map<String,UUID> positionLookUp = buildSimpleLookup(positonHiddenSheet);
          Map<String,UUID> empTypeLookUp = buildSimpleLookup(empTypeHiddenSheet);
          Map<String,UUID> gradeLookUp = buildSimpleLookup(gradeHiddenSheet);
          Map<UUID,Set<UUID>> masterPositionToDeptValidationMap = masterPositionsRepository.findAll().stream()
                  .collect(Collectors.groupingBy(
                          MasterPositionsEntity::getId,
                          Collectors.mapping(MasterPositionsEntity::getDeptId, Collectors.toSet())
                  ));

        List<String> errors = new ArrayList<>();
        List<JobPositionsExcelModel> rows = new ArrayList<>();
        Set<String> seenInFileKeys = new HashSet<>();

          for(int i=1;i<=templateSheet.getLastRowNum();i++){
              Row row = templateSheet.getRow(i);
              if (row == null) continue;

              int rowNum = i + 1;
              Set<String> missingFields = new HashSet<>();
              List<String> invalidFields = new ArrayList<>();

              String deptName = getCellValue(row.getCell(0));
              String masterPosDisplay = getCellValue(row.getCell(1));
              String vacanciesStr = getCellValue(row.getCell(2));
              String ageMinStr = getCellValue(row.getCell(3));
              String ageMaxStr = getCellValue(row.getCell(4));
              String empTypeName = getCellValue(row.getCell(5));
              String gradeCode = getCellValue(row.getCell(6));
              String locationPrefStr = getCellValue(row.getCell(7));
              String manExpMonthsStr = getCellValue(row.getCell(8));
              String prefExpMonthsStr = getCellValue(row.getCell(9)); // Optional
              String manExpText = getCellValue(row.getCell(10));
              String prefExpText = getCellValue(row.getCell(11));     // Optional
              String rolesResp = getCellValue(row.getCell(12));
              String contractYearsStr = getCellValue(row.getCell(13)); // Optional

              // --- Mandatory Checks ---
              if (deptName == null || deptName.isBlank()) missingFields.add("Department Name");
              if (masterPosDisplay == null || masterPosDisplay.isBlank()) missingFields.add("Master Position Code");
              if(vacanciesStr == null || vacanciesStr.isBlank()) missingFields.add("Total Vacancies");
              if(ageMinStr == null || ageMinStr.isBlank()) missingFields.add("Eligibility Age Min");
              if(ageMaxStr == null || ageMaxStr.isBlank()) missingFields.add("Eligibility Age Max");
              if (empTypeName == null || empTypeName.isBlank()) missingFields.add("Employment Type");
              if (gradeCode == null || gradeCode.isBlank()) missingFields.add("Job Grade Code");
              if (locationPrefStr == null || locationPrefStr.isBlank()) missingFields.add("Enable Location Preferences");
              if (manExpMonthsStr == null || manExpMonthsStr.isBlank()) missingFields.add("Mandatory Experience Months");
              if (manExpText == null || manExpText.isBlank()) missingFields.add("Mandatory Experience Text");
              if (rolesResp == null || rolesResp.isBlank()) missingFields.add("Roles and Responsibilities");

              UUID deptId = null;
              UUID masterPosId = null;
              UUID empTypeId =  null;
              UUID gradeId = null;
              if(!missingFields.contains("Department Name")){
                  deptId = departmentLookUp.getOrDefault(deptName,null);
                  if (deptId == null) {
                      invalidFields.add("Department Name (No match found)");
                  }
              }

              if(!missingFields.contains("Master Position Code")){
                  masterPosId = positionLookUp.getOrDefault(masterPosDisplay,null);
                  if (masterPosId == null) {
                      invalidFields.add("Master Position Code (No match found)");
                  }
              }

              if(deptId!=null && masterPosId!=null){
                  String compositeKey = deptId.toString() + "||" + masterPosId.toString();
                  Set<UUID> validDeptsForPosition = masterPositionToDeptValidationMap.get(masterPosId);
                  if(existingKeys.contains(compositeKey)){
                      invalidFields.add("Job Position already exists for "+deptName+" department "+masterPosDisplay);
                  }
                  else if(seenInFileKeys.contains(compositeKey)){
                      invalidFields.add("Duplicate entry in file for "+deptName+" department "+masterPosDisplay);
                  }else if(validDeptsForPosition != null && !validDeptsForPosition.contains(deptId)){
                      invalidFields.add("Master Position : "+masterPosDisplay+" is not present in Department :"+deptName);
                  } else{
                      seenInFileKeys.add(compositeKey);
                  }
              }

              if(!missingFields.contains("Employment Type")){
                  empTypeId = empTypeLookUp.getOrDefault(empTypeName,null);
                  if (empTypeId == null) {
                      invalidFields.add("Employment Type (No match found)");
                  }
              }

              if(!missingFields.contains("Job Grade Code")){
                  gradeId = gradeLookUp.getOrDefault(gradeCode,null);
                  if (gradeId == null) {
                      invalidFields.add("Job Grade Code (No match found)");
                  }
              }

              Boolean locationPref = null;
              if(locationPrefStr!=null) {
                  if(locationPrefStr.equalsIgnoreCase("Yes")) {
                      locationPref = true;
                  }else{
                      locationPref = false;
                  }
              }
              Integer vacancies = vacanciesStr!=null ?   (int) Double.parseDouble(vacanciesStr) : null;
              Integer minAge = ageMinStr!=null ? (int) Double.parseDouble(ageMinStr) : null;
              Integer maxAge = ageMaxStr !=null ? (int)Double.parseDouble(ageMaxStr) : null;
              Integer mandExpMonths = manExpMonthsStr!=null ? (int)Double.parseDouble(manExpMonthsStr) : null;
              Integer prefExpMonths = prefExpMonthsStr!=null ? (int)Double.parseDouble(prefExpMonthsStr) : null;
              Integer contractYears = contractYearsStr!=null ? (int)Double.parseDouble(contractYearsStr) : null;

              if(vacancies != null && vacancies <= 0){
                  invalidFields.add("Total Vacancies (Must be greater than 0)");
              }

              if(minAge !=null && maxAge !=null && minAge > maxAge){
                  invalidFields.add("Eligibility Age (Min age cannot be greater than Max age)");
              }



              if(empTypeName!=null && empTypeName.equalsIgnoreCase("contract") && (contractYears==null || contractYears <= 0)){
                    missingFields.add("Contract Years required when employment type is contract and must be greater than 0");
              }

              if(!missingFields.isEmpty()){
                  errors.add("Row "+rowNum+": Missing mandatory fields - " + String.join(", ", missingFields));
              }

              if(!invalidFields.isEmpty()){
                  errors.add("Row "+rowNum+": Invalid field values - " + String.join(", ", invalidFields));
              }

              if(errors.isEmpty()){
                  JobPositionsExcelModel model = JobPositionsExcelModel.builder()
                          .deptId(deptId)
                          .masterPositionId(masterPosId)
                          .totalVacancies(vacancies)
                          .eligibilityAgeMin(minAge)
                          .eligibilityAgeMax(maxAge)
                          .employmentType(empTypeId)
                          .gradeId(gradeId)
                          .isLocationPreferenceEnabled(locationPref)
                          .mandatoryExperienceMonths(mandExpMonths)
                          .preferredExperienceMonths(prefExpMonths)
                          .mandatoryExperience(manExpText)
                          .preferredExperience(prefExpText)
                          .rolesResponsibilities(rolesResp)
                          .contractYears(contractYears)
                          .build();
                  rows.add(model);
              }

          }



          if(!errors.isEmpty()){
              throw new ExcelValidationException(errors);
          }

          if(rows.isEmpty()){
              throw new ExcelValidationException(List.of("File is empty.No data rows found"));
          }




          return rows;

    }

    private void validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new ExcelValidationException(List.of("Header row is missing"));
        }

        List<String> expectedHeaders = AppConstants.JOB_POSITION_EXCEL_HEADERS;

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

    private Map<String, UUID> buildSimpleLookup(Sheet sheet) {
        Map<String, UUID> map = new HashMap<>();
        if (sheet == null) return map;


        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String name = getCellValue(row.getCell(0));
            String idStr = getCellValue(row.getCell(1));
            if(name == null || idStr == null) continue;
            map.put(name, UUID.fromString(idStr));
        }
        return map;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BLANK -> null;
            default -> cell.toString();
        };
    }
    // to read from uploaded excel and save to db end

    private JobPositionsDTO toDraftPositionDto(JobPositionEditRequestEntity source) {
        JobPositionsDTO dto = new JobPositionsDTO();
        dto.setId(source.getId());
        dto.setParentPositionId(source.getParentPositionId());
        dto.setRequisitionId(source.getJobEditRequisition().getParentRequisitionId());
        dto.setMasterPositionId(source.getMasterPositionId());
        dto.setDeptId(source.getDeptId());
        dto.setTotalVacancies(source.getTotalVacancies());
        dto.setEligibilityAgeMin(source.getEligibilityAgeMin());
        dto.setEligibilityAgeMax(source.getEligibilityAgeMax());
        dto.setEmploymentType(source.getEmploymentType());
        dto.setCibilScore(source.getCibilScore());
        dto.setGradeId(source.getGradeId());
        dto.setMandatoryEducation(source.getMandatoryEducation());
        dto.setPreferredEducation(source.getPreferredEducation());
        dto.setMandatoryExperience(source.getMandatoryExperience());
        dto.setPreferredExperience(source.getPreferredExperience());
        dto.setRolesResponsibilities(source.getRolesResponsibilities());
        dto.setPositionStatus(source.getPositionStatus());
        dto.setContractYears(source.getContractYears());
        dto.setIsLocationPreferenceEnabled(source.getIsLocationPreferenceEnabled());
        dto.setIsLocationWise(source.getIsLocationWise());
        dto.setTotalExperience(source.getTotalExperience());
        dto.setMandatoryExperienceMonths(source.getMandatoryExperienceMonths());
        dto.setPreferredExperienceMonths(source.getPreferredExperienceMonths());
        dto.setIndentPath(source.getIndentPath());
        dto.setApprovedBy(source.getApprovedBy());
        dto.setApprovedOn(source.getApprovedOn());
        dto.setIsMedicalRequired(source.getIsMedicalRequired());
        dto.setMandatoryEduRulesJson(source.getMandatoryEduRulesJson());
        dto.setPreferredEduRulesJson(source.getPreferredEduRulesJson());
        dto.setIndentName(source.getIndentName());
        dto.setIndentOthers(source.getIndentOthers());
        dto.setCutoffDate(source.getCutoffDate());
        dto.setIsMandatoryExpMonthsEduWise(source.getIsMandatoryExpMonthsEduWise());
        dto.setIsPreferredExpMonthsEduWise(source.getIsPreferredExpMonthsEduWise());
        dto.setMandatoryExpMonthsEduWise(source.getMandatoryExpMonthsEduWise());
        dto.setPreferredExpMonthsEduWise(source.getPreferredExpMonthsEduWise());
        dto.setIsProficientInLocalLanguage(source.getIsProficientInLocalLanguage());
        dto.setIsAgeRelWdsWomen(source.getIsAgeRelWdsWomen());
        dto.setIsAgeRelRiotVictimFamily(source.getIsAgeRelRiotVictimFamily());
        dto.setCreatedDate(source.getCreatedDate());
        dto.setModifiedDate(source.getModifiedDate());
        dto.setCreatedBy(source.getCreatedBy());
        dto.setModifiedBy(source.getModifiedBy());
        dto.setIsActive(source.getIsActive());
        dto.setIsIntermediateRequired(source.getIsIntermediateRequired());
        dto.setDynamicFields(source.getDynamicFields());

        List<PositionStateDistributionDTO> stateDistributions = source.getPositionStateDistributionEditRequests() == null
                ? Collections.emptyList()
                : source.getPositionStateDistributionEditRequests().stream().map(state -> {
                    PositionStateDistributionDTO stateDto = new PositionStateDistributionDTO();
                    stateDto.setId(state.getId());
                    stateDto.setPositionId(source.getId());
                    stateDto.setStateId(state.getStateId());
                    stateDto.setCityId(state.getCityId());
                    stateDto.setTotalVacancies(state.getTotalVacancies());
                    stateDto.setLocalLanguage(state.getLocalLanguage());

                    List<PositionCategoryDistributionDTO> categories = state.getPositionCategoryDistributionEditRequests() == null
                            ? Collections.emptyList()
                            : state.getPositionCategoryDistributionEditRequests().stream().map(category -> {
                                PositionCategoryDistributionDTO categoryDto = new PositionCategoryDistributionDTO();
                                categoryDto.setId(category.getId());
                                categoryDto.setStateDistributionId(state.getId());
                                categoryDto.setReservationCategoryId(category.getReservationCategoryId());
                                categoryDto.setDisabilityCategoryId(category.getDisabilityCategoryId());
                                categoryDto.setVacancyCount(category.getVacancyCount());
                                categoryDto.setIsDisability(category.getIsDisability());
                                return categoryDto;
                            }).collect(Collectors.toList());
                    stateDto.setPositionCategoryDistributions(categories);
                    return stateDto;
                }).collect(Collectors.toList());
        dto.setPositionStateDistributions(stateDistributions);

        List<PositionCategoryNationalDistributionDTO> nationalDistributions = source.getPositionCategoryNationalDistributionEditRequests() == null
                ? Collections.emptyList()
                : source.getPositionCategoryNationalDistributionEditRequests().stream().map(category -> {
                    PositionCategoryNationalDistributionDTO categoryDto = new PositionCategoryNationalDistributionDTO();
                    categoryDto.setId(category.getId());
                    categoryDto.setJobPositionId(source.getId());
                    categoryDto.setReservationCategoryId(category.getReservationCategoryId());
                    categoryDto.setDisabilityCategoryId(category.getDisabilityCategoryId());
                    categoryDto.setVacancyCount(category.getVacancyCount());
                    categoryDto.setIsDisability(category.getIsDisability());
                    return categoryDto;
                }).collect(Collectors.toList());
        dto.setPositionCategoryNationalDistributions(nationalDistributions);

        List<JobPositionExclusionsDTO> jobPositionExclusion = source.getJobPositionExclusionsEditRequestEntity() == null
                ? Collections.emptyList()
                : source.getJobPositionExclusionsEditRequestEntity().stream().map(exclusion -> {
                    JobPositionExclusionsDTO exclusionDto = new JobPositionExclusionsDTO();
                    exclusionDto.setId(exclusion.getId());
                    exclusionDto.setPositionId(source.getId());
                    exclusionDto.setExclusionId(exclusion.getExclusionId());
                    exclusionDto.setIsExcluded(exclusion.getIsExcluded());
                    return exclusionDto;
                }).collect(Collectors.toList());
        dto.setJobPositionExclusion(jobPositionExclusion);
        return dto;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Vacancy breakdown APIs
    // ──────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PositionVacancyBreakdownModel> getVacancyBreakdownByRequisition(UUID requisitionId) {
        List<JobPositionsEntity> positions = positionsRepository.findAllByRequisitionId(requisitionId);
        return positions.stream()
                .map(this::buildBreakdown)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PositionVacancyBreakdownModel getVacancyBreakdownByPosition(UUID positionId) {
        JobPositionsEntity position = positionsRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + positionId));
        return buildBreakdown(position);
    }

    private PositionVacancyBreakdownModel buildPositionHeader(JobPositionsEntity position) {
        return PositionVacancyBreakdownModel.builder()
                .positionId(position.getId())
                .masterPositionId(position.getMasterPositionId())
                .deptId(position.getDeptId())
                .employmentType(position.getEmploymentType())
                .contractYears(position.getContractYears())
                .eligibilityAgeMin(position.getEligibilityAgeMin())
                .eligibilityAgeMax(position.getEligibilityAgeMax())
                .mandatoryExperienceMonths(position.getMandatoryExperienceMonths())
                .isMandatoryExpMonthsEduWise(position.getIsMandatoryExpMonthsEduWise())
                .mandatoryExpMonthsEduWise(
                        Boolean.TRUE.equals(position.getIsMandatoryExpMonthsEduWise())
                                ? position.getMandatoryExpMonthsEduWise() : null)
                .isLocationWise(position.getIsLocationWise())
                .totalVacancies(position.getTotalVacancies())
                .remainingTotalVacancies(position.getRemainingTotalVacancies())
                .onboardedCount(PositionVacancyBreakdownModel.calcOnboarded(
                        position.getTotalVacancies(), position.getRemainingTotalVacancies()))
                .offersSent(position.getOffersSent())
                .offersAccepted(position.getOffersAccepted())
                .build();
    }

    private PositionVacancyBreakdownModel buildBreakdown(JobPositionsEntity position) {
        int posOnboarded = PositionVacancyBreakdownModel.calcOnboarded(
                position.getTotalVacancies(), position.getRemainingTotalVacancies());

        if (Boolean.TRUE.equals(position.getIsLocationWise())) {
            List<PositionStateDistributionEntity> stateDists =
                    positionStateDistributionRepository.findByJobPositionIdIn(List.of(position.getId()));

            List<UUID> stateDistIds = stateDists.stream().map(PositionStateDistributionEntity::getId).collect(Collectors.toList());

            List<PositionCategoryDistributionEntity> allCats = stateDistIds.isEmpty()
                    ? Collections.emptyList()
                    : positionCategoryDistributionRepository.findAllByStateDistributionIdInWithFetch(stateDistIds);

            Map<UUID, List<PositionCategoryDistributionEntity>> catsByStateDist = allCats.stream()
                    .collect(Collectors.groupingBy(c -> c.getPositionStateDistribution().getId()));

            List<PositionVacancyBreakdownModel.StateDistributionBreakdown> stateBreakdown = stateDists.stream()
                    .map(sd -> {
                        int sdOnboarded = PositionVacancyBreakdownModel.calcOnboarded(
                                sd.getTotalVacancies(), sd.getRemainingTotalVacancies());
                        List<PositionVacancyBreakdownModel.CategoryBreakdown> catBreakdowns =
                                catsByStateDist.getOrDefault(sd.getId(), Collections.emptyList()).stream()
                                        .map(c -> PositionVacancyBreakdownModel.CategoryBreakdown.builder()
                                                .reservationCategoryId(c.getReservationCategoryId())
                                                .disabilityCategoryId(c.getDisabilityCategoryId())
                                                .isDisability(c.getIsDisability())
                                                .vacancyCount(c.getVacancyCount())
                                                .remainingVacancyCount(c.getRemainingVacancyCount())
                                                .onboardedCount(PositionVacancyBreakdownModel.calcOnboarded(c.getVacancyCount(), c.getRemainingVacancyCount()))
                                                .offersSent(c.getOffersSent())
                                                .offersAccepted(c.getOffersAccepted())
                                                .build())
                                        .collect(Collectors.toList());
                        return PositionVacancyBreakdownModel.StateDistributionBreakdown.builder()
                                .stateId(sd.getStateId())
                                .cityId(sd.getCityId())
                                .totalVacancies(sd.getTotalVacancies())
                                .remainingTotalVacancies(sd.getRemainingTotalVacancies())
                                .onboardedCount(sdOnboarded)
                                .offersSent(sd.getOffersSent())
                                .offersAccepted(sd.getOffersAccepted())
                                .categories(catBreakdowns)
                                .build();
                    })
                    .collect(Collectors.toList());

            PositionVacancyBreakdownModel result = buildPositionHeader(position);
            result.setStateBreakdown(stateBreakdown);
            return result;

        } else {
            List<PositionCategoryNationalDistributionEntity> nationalRows =
                    positionCategoryNationalDistributionRepository.findAllByPositionId(position.getId());

            List<PositionVacancyBreakdownModel.NationalCategoryBreakdown> nationalBreakdown = nationalRows.stream()
                    .map(n -> PositionVacancyBreakdownModel.NationalCategoryBreakdown.builder()
                            .reservationCategoryId(n.getReservationCategoryId())
                            .disabilityCategoryId(n.getDisabilityCategoryId())
                            .isDisability(n.getIsDisability())
                            .vacancyCount(n.getVacancyCount())
                            .remainingVacancyCount(n.getRemainingVacancyCount())
                            .onboardedCount(PositionVacancyBreakdownModel.calcOnboarded(n.getVacancyCount(), n.getRemainingVacancyCount()))
                            .offersSent(n.getOffersSent())
                            .offersAccepted(n.getOffersAccepted())
                            .build())
                    .collect(Collectors.toList());

            PositionVacancyBreakdownModel result = buildPositionHeader(position);
            result.setNationalBreakdown(nationalBreakdown);
            return result;
        }
    }

}

