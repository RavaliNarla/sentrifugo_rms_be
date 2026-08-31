package com.bob.jobportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.commonutil.exception.ManualValidationException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.ExcelTemplateService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.CommonUtilityProvider;
import com.bob.db.entity.*;
import com.bob.db.enums.ExamQualificationStatus;
import com.bob.db.enums.WrittenExamConfigurationStatus;
import com.bob.db.repository.*;
import com.bob.jobportal.model.*;
import com.bob.jobportal.util.SummaryTracker;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CandidateWrittenExamMarksService {

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private MasterPositionsRepository masterPositionsRepository;

    @Autowired
    private CandidateApplicationsRepository applicationsRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private ReservationCategoriesRepository reservationCategoriesRepository;

    @Autowired
    private CandidateLocationPreferencesRepository locationPreferencesRepository;

    @Autowired
    private InterviewCentresRepository interviewCentresRepository;

    @Autowired
    private CandidateWrittenExamMarksRepository writtenExamMarksRepository;

    @Autowired
    private CommonUtilityProvider commonUtilityProvider;

    @Autowired
    private CandidateSectionMarksRepository candidateSectionMarksRepository;

    @Autowired
    private WrittenExamConfigurationRepository writtenExamConfigurationRepository;

    @Autowired
    private ExamSectionPassMarksRepository examSectionPassMarksRepository;

    @Autowired
    private ExamSectionCategoryPassMarksRepository examSectionCategoryPassMarksRepository;

    @Autowired
    private CandidateDisabilityDetailsRepository candidateDisabilityDetailsRepository;

    @Autowired
    private CandidateLocationPreferencesRepository candidateLocationPreferencesRepository;

    @Autowired
    private CandidateConcessionsRepository candidateConcessionsRepository;

    //download start
    @Transactional(readOnly = true)
    public byte[] downloadMarkUploadTemplate(List<UUID> positionIds){
        List<JobPositionsEntity> jobPositions = positionsRepository.findAllById(positionIds);
        if(jobPositions.isEmpty()){
            throw new ResourceNotFoundException("No Job Positions Found");
        }
        Map<UUID, WrittenExamConfigurationEntity> examConfigurationMap = writtenExamConfigurationRepository.findByPositionIdIn(positionIds)
                .stream()
                .collect(Collectors.toMap(
                        WrittenExamConfigurationEntity::getPositionId,
                        Function.identity()
                ));

        if(examConfigurationMap.isEmpty()) throw new ResourceNotFoundException("No Exam Configuration found for all the selected positions");
        Set<UUID> masterPositionIds = jobPositions.stream()
                .map(JobPositionsEntity::getMasterPositionId)
                .collect(Collectors.toSet());

        Map<UUID, String> masterPositionMap = masterPositionsRepository.findAllById(masterPositionIds).stream()
                .collect(Collectors.toMap(
                        MasterPositionsEntity::getId,
                        MasterPositionsEntity::getPositionName
                ));

        Map<UUID, List<CandidateApplicationsEntity>> applicationGroupedByPosition = applicationsRepository.findAllByPositionIdIn(positionIds)
                .stream()
                .collect(Collectors.groupingBy(
                        CandidateApplicationsEntity::getPositionId
                ));

        List<UUID> candidateIds = applicationGroupedByPosition.values()
                .stream()
                .flatMap(List::stream)
                .map(CandidateApplicationsEntity::getCandidateId)
                .distinct()
                .toList();

        Map<UUID, CandidateProfileEntity> candidateProfileMap = candidateProfileRepository.findAllByCandidateIdIn(candidateIds).stream()
                .collect(Collectors.toMap(
                        CandidateProfileEntity::getCandidateId,
                        Function.identity()
                ));

        Map<String, CandidateLocationPreferenceEntity> candidateLocationPreferenceMap = locationPreferencesRepository.findByCandidateAndPosition(candidateIds, positionIds)
                .stream().collect(
                        Collectors.toMap(
                                (currLocPref) -> currLocPref.getCandidateId() + "_" + currLocPref.getPositionId(),
                                Function.identity()
                        )
                );

        Set<UUID> interviewCentreIds = candidateLocationPreferenceMap.values()
                .stream()
                .map(CandidateLocationPreferenceEntity::getInterviewCenter)
                .collect(Collectors.toSet());

        Set<UUID> reservationCategoryIds = candidateProfileMap.values()
                .stream()
                .map(CandidateProfileEntity::getReservationCategoryId)
                .collect(Collectors.toSet());

        Map<UUID, String> interviewCentreMap = interviewCentresRepository.findAllById(interviewCentreIds).stream()
                .collect(Collectors.toMap(
                        InterviewCentresEntity::getId,
                        InterviewCentresEntity::getDisplayName
                ));
        Map<UUID, String> reservationCategoryMap = reservationCategoriesRepository.findAllById(reservationCategoryIds)
                .stream()
                .collect(Collectors.toMap(
                        ReservationCategoriesEntity::getId,
                        ReservationCategoriesEntity::getCategoryCode
                ));

        List<UUID> examConfigIds = examConfigurationMap.values().stream()
                .map(WrittenExamConfigurationEntity::getId)
                .distinct()
                .toList();

        Map<UUID, List<ExamSectionPassMarksEntity>> examSectionsMap = examSectionPassMarksRepository.findByExamConfig_IdIn(examConfigIds)
                .stream()
                .collect(Collectors.groupingBy(
                        (currSection) -> currSection.getExamConfig().getId()
                ));

        Map<UUID,CandidateWrittenExamMarksEntity> writtenExamMarksEntityMap = writtenExamMarksRepository.findByPosition_IdIn(positionIds)
                .stream()
                .collect(Collectors.toMap(
                        (currEntity)->currEntity.getApplication().getId(),
                        Function.identity()
                ));
        List<UUID> writtenExamIds = writtenExamMarksEntityMap.values()
                .stream()
                .map(CandidateWrittenExamMarksEntity::getId)
                .toList();

        Map<String,CandidateSectionMarksEntity> candidateSectionMarksEntityMap = candidateSectionMarksRepository.findByCandidateExam_IdIn(writtenExamIds)
                .stream()
                .collect(Collectors.toMap(
                        (currEntity)->currEntity.getCandidateExam().getId()+"_"+currEntity.getExamSection().getId(),
                        Function.identity()
                ));



        Workbook wb = new XSSFWorkbook();

        Sheet mappingHiddenSheet = wb.createSheet("MappingHidden");
        int mappingRowIdx = 0;

        //for hidden sheet
        Row mappingHeader = mappingHiddenSheet.createRow(mappingRowIdx++);
        mappingHeader.createCell(0).setCellValue("Type");      // e.g., APP, POS, SEC, CONFIG
        mappingHeader.createCell(1).setCellValue("SheetName"); // Context (Visible Sheet Name)
        mappingHeader.createCell(2).setCellValue("DisplayKey");// e.g., Application No or Section Name
        mappingHeader.createCell(3).setCellValue("UUID");


        List<String> errors = new ArrayList<>();

        Set<String> assignedSheetNames = new HashSet<>();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int duplicateCounter = 1;
        for (JobPositionsEntity jobPosition : jobPositions) {
            String positionName = masterPositionMap.get(jobPosition.getMasterPositionId());

            List<CandidateApplicationsEntity> applications = applicationGroupedByPosition.get(jobPosition.getId());

            if(!examConfigurationMap.containsKey(jobPosition.getId())){
                errors.add("Exam Configuration not found for position :"+positionName);
            }

            if(applications == null || applications.isEmpty()){
                errors.add("Applications not found for "+positionName);
            }



            // // Clean up the name string to comply with Excel's 31-character
            if(errors.isEmpty()){
                WrittenExamConfigurationEntity examConfiguration = examConfigurationMap.get(jobPosition.getId());
                String baseSafeName = WorkbookUtil.createSafeSheetName(positionName);
                String safeSheetName = baseSafeName.trim();


                // If the name is already used by another position tab, mutate it safely
                while (assignedSheetNames.contains(safeSheetName)) {
                    String suffix = "_" + duplicateCounter;
                    int maxBaseLength = 31 - suffix.length();

                    String truncatedBase = baseSafeName.length() > maxBaseLength
                            ? baseSafeName.substring(0, maxBaseLength)
                            : baseSafeName;

                    safeSheetName = WorkbookUtil.createSafeSheetName(truncatedBase + suffix);
                    duplicateCounter++;
                }

                // Lock the name in so no subsequent sheets can steal it
                assignedSheetNames.add(safeSheetName);


                Sheet sheet = wb.createSheet(safeSheetName);
                addMappingRow(mappingHiddenSheet, mappingRowIdx++, "POSITION", safeSheetName, positionName, jobPosition.getId().toString());
                addMappingRow(mappingHiddenSheet, mappingRowIdx++, "CONFIG", safeSheetName, examConfiguration.getExamName(), examConfiguration.getId().toString());
                List<ExamSectionPassMarksEntity> sections = examSectionsMap.get(examConfiguration.getId());
                sections.sort(Comparator.comparing(ExamSectionPassMarksEntity::getSectionNumber));
                for (ExamSectionPassMarksEntity section : sections) {
                    addMappingRow(mappingHiddenSheet, mappingRowIdx++, "SECTION", safeSheetName, section.getSectionName(), section.getId().toString());
                }
                List<String> sectionNames = sections.stream()
                        .map(ExamSectionPassMarksEntity::getSectionName)
                        .toList();


                createHeader(sheet,sectionNames);


                int rowIdx = 1,sectionStartCol = 6;
                //for scores
                CellStyle decimalCellStyle = wb.createCellStyle();
                DataFormat decimalFormat = wb.createDataFormat();
                decimalCellStyle.setDataFormat(decimalFormat.getFormat("0.0000"));
                for (CandidateApplicationsEntity application : applications) {
                    Row row = sheet.createRow(rowIdx++);
                    CandidateProfileEntity profile = candidateProfileMap.get(application.getCandidateId());

                    String appNo = application.getApplicationNo();
                    String candidateName = commonUtilityProvider.buildFullName(profile);
                    String locPrefKey = application.getCandidateId() + "_" + application.getPositionId();
                    CandidateLocationPreferenceEntity locPref = candidateLocationPreferenceMap.get(locPrefKey);

                    String zone = interviewCentreMap.get(locPref.getInterviewCenter());
                    String category = reservationCategoryMap.get(profile.getReservationCategoryId());

                    // Map the candidate's unique Application text string to their Application UUID
                    addMappingRow(mappingHiddenSheet, mappingRowIdx++, "APPLICATION", safeSheetName, appNo, application.getId()+"_"+application.getCandidateId());
                    String hasDisability = Boolean.TRUE.equals(profile.getDisability()) ? "YES":"NO";

                    // Write properties to the user visible worksheet grid cells
                    row.createCell(0).setCellValue(positionName);
                    row.createCell(1).setCellValue(candidateName);
                    row.createCell(2).setCellValue(appNo);
                    row.createCell(3).setCellValue(zone);
                    row.createCell(4).setCellValue(category);
                    row.createCell(5).setCellValue(hasDisability);

                    CandidateWrittenExamMarksEntity writtenExamMarksEntity = writtenExamMarksEntityMap.getOrDefault(application.getId(),null);
                    for (int i = 0; i < sections.size(); i++) {
                        int secColIdx = sectionStartCol + i;
                        Cell markCell = row.createCell(secColIdx);
                        Double sectionMarksValue = null;
                        ExamSectionPassMarksEntity sectionEntity = sections.get(i);
                        if(writtenExamMarksEntity!=null){
                            CandidateSectionMarksEntity sectionMarksEntity = candidateSectionMarksEntityMap.getOrDefault(writtenExamMarksEntity.getId()+"_"+sectionEntity.getId(),null);
                            sectionMarksValue = sectionMarksEntity!=null && sectionMarksEntity.getMarksObtained() != null ? sectionMarksEntity.getMarksObtained().doubleValue() : null;
                        }

                        if (sectionMarksValue != null) {
                            markCell.setCellValue(sectionMarksValue);
                        } else {
                            markCell.setBlank();
                        }

                        // This line tells Excel to process numbers typed here as real numeric decimals!
                        markCell.setCellStyle(decimalCellStyle);
                    }
                }
                // Common headers occupy indices 0 to 4
                int totalRows = Math.max(1, applications.size());
                int configTotalMarks = examConfiguration.getTotalMarks();

                applySectionValidations(sheet, sections, sectionStartCol, totalRows, configTotalMarks);

                // Auto-fit dynamic columns layout widths cleanly
                int totalCols = sectionStartCol + sections.size();
                for (int i = 0; i < totalCols; i++) {
                    sheet.autoSizeColumn(i);
                }
            }
        }

        int mappingIdx = wb.getSheetIndex("MappingHidden");
        if (mappingIdx != -1) {
            wb.setSheetVisibility(mappingIdx, SheetVisibility.HIDDEN);
        }

        if (wb.getNumberOfSheets() > 1) {
            // 2. FORCE Excel focus to the first visible position tab (Index 1) instead of hidden index 0
            wb.setActiveSheet(1);
        }

        try {
            wb.write(outputStream);
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException("Error processing multi-tab unified workbook template export payload: " + e.getMessage(), e);
        }

        if(!errors.isEmpty()){
            throw new ExcelValidationException(errors);
        }

        return outputStream.toByteArray();
    }


    private void addMappingRow(Sheet sheet, int rowIdx, String type, String sheetName, String displayKey, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(type);
        row.createCell(1).setCellValue(sheetName);
        row.createCell(2).setCellValue(displayKey);
        row.createCell(3).setCellValue(value);
    }

    private void applySectionValidations(Sheet sheet, List<ExamSectionPassMarksEntity> sections,
                                         int sectionStartCol, int maxRows, double configTotalMarks) {
        if (sections == null || sections.isEmpty() || maxRows < 1) {
            return;
        }

        DataValidationHelper validationHelper = sheet.getDataValidationHelper();

        // Resolve column letters bounding dynamic section blocks horizontally (e.g., "F" and "H")
        String startColStr = CellReference.convertNumToColString(sectionStartCol);
        String endColStr = CellReference.convertNumToColString(sectionStartCol + sections.size() - 1);

        for (int i = 0; i < sections.size(); i++) {
            ExamSectionPassMarksEntity sec = sections.get(i);
            int secColIdx = sectionStartCol + i;

            // Ensure values are evaluated as floating-point doubles
            double maxSectionMarks = sec.getSectionTotalMarks().doubleValue();

            String colLetter = CellReference.convertNumToColString(secColIdx);

            // FIX: Changed precision to %.4f to support up to 4 decimal points safely
            String customValidationFormula = String.format(
                    "AND(%1$s2>=0.0, %1$s2<=%2$.4f, SUM($%3$s2:$%4$s2)<=%5$.4f)",
                    colLetter,          // %1$s -> e.g., "F"
                    maxSectionMarks,    // %2$.4f -> Formats to 4 decimals (e.g., "25.0000")
                    startColStr,        // %3$s -> e.g., "F"
                    endColStr,          // %4$s -> e.g., "H"
                    configTotalMarks    // %5$.4f -> Formats overall max to 4 decimals (e.g., "100.0000")
            );

            DataValidationConstraint constraint = validationHelper.createCustomConstraint(customValidationFormula);
            CellRangeAddressList range = new CellRangeAddressList(1, maxRows, secColIdx, secColIdx);
            DataValidation validation = validationHelper.createValidation(constraint, range);

            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox(
                    "Invalid Marks Entry",
                    "1. Value for " + sec.getSectionName() + " must be a valid number between 0 and " + maxSectionMarks + ".\n" +
                            "2. The total horizontal sum across all sections on this row cannot exceed the Examination Config limit of " + configTotalMarks + " marks."
            );

            sheet.addValidationData(validation);
        }
    }

    private void createHeader(Sheet sheet,List<String> headers) {
        List<String> sheetHeaders = new ArrayList<>(Arrays.asList("Position", "Candidate name", "Application No", "Zone", "Category","Disability"));
        sheetHeaders.addAll(headers);
        Workbook workbook = sheet.getWorkbook();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        Row row = sheet.createRow(0);

        for (int i = 0; i < sheetHeaders.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(sheetHeaders.get(i));
            cell.setCellStyle(headerStyle);
        }
    }
    //download end


    //upload start
    @Transactional
    public void saveExaminationMarks(MultipartFile file){
        if(file == null){
            throw new IllegalArgumentException("Uploaded an empty file");
        }

        if (!ExcelTemplateService.hasExcelFormat(file)) {
            throw new IllegalArgumentException("Please upload an Excel file!");
        }


        List<ExaminationConfigurationMarksUploadExcelModel> modelsWithMarks = readMarksFromExcel(file);



        List<CandidateWrittenExamMarksEntity> writtenExamMarksEntities = modelsWithMarks
                .stream()
                        .map(ExaminationConfigurationMarksUploadExcelModel::getWrittenExamMarksEntity)
                                .toList();
        List<UUID> positionIds = writtenExamMarksEntities.stream().map((curr)->curr.getPosition().getId()).toList();

        List<UUID> configurationIds = writtenExamMarksEntities.stream()
                        .map((curr)->curr.getExamConfig().getId())
                                .distinct()
                                        .toList();
        List<UUID> applicationIds =writtenExamMarksEntities.stream()
                .map((curr)->curr.getApplication().getId())
                .distinct()
                .toList();

        Map<String,CandidateWrittenExamMarksEntity> writtenExamMarksInDb = writtenExamMarksRepository.findAllByApplication_IdInAndExamConfig_IdIn(applicationIds,configurationIds)
                        .stream()
                                .collect(Collectors.toMap(
                                        (curr)->curr.getApplication().getId()+"_"+curr.getExamConfig().getId(),
                                        Function.identity()
                                ));
        List<CandidateWrittenExamMarksEntity> savingWrittenExamEntities = writtenExamMarksEntities.stream()
                        .map((curr)->{
                            String searchKey = curr.getApplication().getId()+"_"+curr.getExamConfig().getId();
                            CandidateWrittenExamMarksEntity writtenExamMarksEntity = writtenExamMarksInDb.getOrDefault(searchKey,null);
                            if(writtenExamMarksEntity != null){
                                writtenExamMarksEntity.setTotalMarksObtained(curr.getTotalMarksObtained());
                                writtenExamMarksEntity.setStatus(ExamQualificationStatus.NOT_MARKED);
                                writtenExamMarksEntity.setIsPassed(false);
                                writtenExamMarksEntity.setRankingMarksObtained(null);
                                return writtenExamMarksEntity;
                            }
                            return curr;
                        }).toList();

        writtenExamMarksRepository.saveAll(savingWrittenExamEntities);

        Map<String,CandidateWrittenExamMarksEntity> savedWrittenExamMap = savingWrittenExamEntities.stream()
                .collect(Collectors.toMap(
                        (curr)->curr.getApplication().getId()+"_"+curr.getExamConfig().getId(),
                        Function.identity()
                ));

        List<UUID> writtenExamIds = savingWrittenExamEntities.stream()
                .map(CandidateWrittenExamMarksEntity::getId)
                .toList();

        Map<String,CandidateSectionMarksEntity> writtenSectionIdDb = candidateSectionMarksRepository.findByCandidateExam_IdIn(writtenExamIds)
                .stream()
                .collect(Collectors.toMap(
                        (curr)->curr.getCandidateExam().getId()+"_"+curr.getExamSection().getId(),
                        Function.identity()
                ));



        List<CandidateSectionMarksEntity> savingSectionEntities =
                modelsWithMarks.stream()
                        .flatMap(model -> {

                            CandidateWrittenExamMarksEntity writtenExam =
                                    savedWrittenExamMap.get(
                                            model.getSearchKey()
                                    );
                            return model.getCandidateSectionMarksEntityList()
                                    .stream()
                                    .map((currSectionMarks)->{
                                        String sectionSearchKey = writtenExam.getId()+"_"+currSectionMarks.getExamSection().getId();
                                        CandidateSectionMarksEntity sectionMarks = writtenSectionIdDb.getOrDefault(sectionSearchKey,null);
                                        if(sectionMarks!=null){
                                            sectionMarks.setMarksObtained(currSectionMarks.getMarksObtained());
                                            return  sectionMarks;
                                        }
                                        currSectionMarks.setCandidateExam(writtenExam);
                                        return currSectionMarks;
                                    });
                        })
                        .toList();




        candidateSectionMarksRepository.saveAll(savingSectionEntities);
    }


    public List<ExaminationConfigurationMarksUploadExcelModel> readMarksFromExcel(MultipartFile file){

        List<ExaminationConfigurationMarksUploadExcelModel> uploadedModels = new ArrayList<>();

        List<String> errors = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            //these maps will have all the sheets details
            Map<String, UUID> positionIdsBySheet = new HashMap<>();
            Map<String, UUID> configIdsBySheet = new HashMap<>();
            Map<String, Map<String, UUID>> sectionIdsBySheet = new HashMap<>();
            Map<String, Map<String, UUID>> applicationIdsBySheet = new HashMap<>();
            Map<String,Map<String,UUID>> candidateIdsBySheet = new HashMap<>();


            Sheet mappingSheet = wb.getSheet("MappingHidden");
            if (mappingSheet == null) {
                throw new CommonException("Invalid template file");
            }



            //to fill the maps with hidden sheet detials

            for (int i = 1; i <= mappingSheet.getLastRowNum(); i++) {
                Row row = mappingSheet.getRow(i);
                if (row == null) continue;

                String type = getCellValueAsString(row.getCell(0));
                String sheetName = getCellValueAsString(row.getCell(1));
                String displayKey = getCellValueAsString(row.getCell(2));
                String uuidStr = getCellValueAsString(row.getCell(3));



                switch (type) {
                    case "POSITION" -> positionIdsBySheet.put(sheetName, UUID.fromString(uuidStr));
                    case "CONFIG" -> configIdsBySheet.put(sheetName, UUID.fromString(uuidStr));
                    case "SECTION" -> sectionIdsBySheet
                            .computeIfAbsent(sheetName, k -> new LinkedHashMap<>())
                            .put(displayKey, UUID.fromString(uuidStr));
                    case "APPLICATION" -> {
                        if (uuidStr.contains("_")) {
                            // Splitting the combined string values back to their individual identities
                            String[] parts = uuidStr.split("_");
                            UUID applicationId = UUID.fromString(parts[0]);
                            UUID candidateId = UUID.fromString(parts[1]);

                            // Map Application Number to both distinct entity IDs instantly in memory lookup maps
                            applicationIdsBySheet
                                    .computeIfAbsent(sheetName, k -> new HashMap<>())
                                    .put(displayKey, applicationId);

                            // 2. Safe nested insertion for Candidate IDs
                            candidateIdsBySheet
                                    .computeIfAbsent(sheetName, k -> new HashMap<>())
                                    .put(displayKey, candidateId);
                        }
                    }
                }
            }





            //sheets loop
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet currentSheet = wb.getSheetAt(i);
                String sheetName = currentSheet.getSheetName();


                if (wb.isSheetHidden(i) || "MappingHidden".equalsIgnoreCase(sheetName)) {
                    continue;
                }

                // positionId,configurationId,sectionForThisSheet,appsForThisSheet for particualr sheet
                UUID positionId = positionIdsBySheet.get(sheetName);
                UUID configurationId = configIdsBySheet.get(sheetName);
                Map<String, UUID> sectionsForThisSheet = sectionIdsBySheet.get(sheetName);
                Map<String, UUID> appsForThisSheet = applicationIdsBySheet.get(sheetName);
                Map<String,UUID> candidateIdsForThisSheet = candidateIdsBySheet.get(sheetName);


                if(!validateSheetHeaders(currentSheet,sectionsForThisSheet.keySet())){
                    errors.add("Invalid headers for"+sheetName);
                }

                if(errors.isEmpty()){
                    Row headerRow = currentSheet.getRow(0);

                    // 4. MAP COLUMNS TO SECTION UUIDs
                    Map<Integer, UUID> columnIndexToSectionIdMap = new HashMap<>();
                    Map<Integer,String> columnIndexToSectionNameMap = new HashMap<>();

                    //to map colindex to Sections
                    for (int col = 6; col < headerRow.getLastCellNum(); col++) {
                        String headerName = getCellValueAsString(headerRow.getCell(col));
                        //get the uuid of section for that particular section
                        UUID sectionId = sectionsForThisSheet.get(headerName);
                        columnIndexToSectionIdMap.put(col, sectionId);
                        columnIndexToSectionNameMap.put(col,headerName);
                    }

                    //sheet row loop
                    for (int r = 1; r <= currentSheet.getLastRowNum(); r++) {
                        Row row = currentSheet.getRow(r);

                        String appNo = getCellValueAsString(row.getCell(2));
                        UUID applicationId = appsForThisSheet.get(appNo);
                        UUID candidateId = candidateIdsForThisSheet.get(appNo);


                        Map<UUID, BigDecimal> sectionWiseMarks = new HashMap<>();
                        for (Map.Entry<Integer, UUID> colMapping : columnIndexToSectionIdMap.entrySet()) {
                            int colIndex = colMapping.getKey();
                            UUID sectionId = colMapping.getValue();
                            Cell markCell = row.getCell(colIndex);
                            String marks = getCellValueAsString(markCell);
                            if(marks == null){
                                errors.add("Row - "+(r+1)+": Marks are missing for :"+columnIndexToSectionNameMap.get(colIndex)+" in -"+sheetName+" sheet");
                            }else{
                                sectionWiseMarks.put(sectionId,new BigDecimal(marks));
                            }

                        }
                        if(errors.isEmpty()){

                            CandidateWrittenExamMarksEntity writtenExamMarksEntity = CandidateWrittenExamMarksEntity.builder()
                                    .examConfig(WrittenExamConfigurationEntity.builder().id(configurationId).build())
                                    .position(JobPositionsEntity.builder().id(positionId).build())
                                    .application(CandidateApplicationsEntity.builder().id(applicationId).build())
                                    .candidate(CandidatesEntity.builder().id(candidateId).build())
                                    .totalMarksObtained(sectionWiseMarks.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add))
                                    .build();

                            List<CandidateSectionMarksEntity> sectionMarksEntities = sectionWiseMarks.entrySet().stream()
                                    .map((curr)->{
                                        UUID key = curr.getKey();
                                        BigDecimal value = curr.getValue();

                                        CandidateSectionMarksEntity sectionMarksEntity = CandidateSectionMarksEntity
                                                .builder()
                                                .examSection(ExamSectionPassMarksEntity.builder().id(key).build())
                                                .marksObtained(value)
                                                .build();

                                        return  sectionMarksEntity;
                                    }).toList();

                                   ExaminationConfigurationMarksUploadExcelModel model = ExaminationConfigurationMarksUploadExcelModel.builder()
                                           .searchKey(applicationId+"_"+configurationId)
                                           .writtenExamMarksEntity(writtenExamMarksEntity)
                                           .candidateSectionMarksEntityList(sectionMarksEntities)
                                           .build();

                                   uploadedModels.add(model);




                        }
                    }
                }

            }

        }catch (Exception e){
            throw new CommonException(e.getMessage());
        }

        if(!errors.isEmpty()){
            throw new ExcelValidationException(errors);
        }
        return uploadedModels;
    }


    public boolean validateSheetHeaders(Sheet sheet,Set<String> sections){


        List<String> expectedHeaders = new ArrayList<>(Arrays.asList("Position", "Candidate name", "Application No", "Zone", "Category","Disability"));
        expectedHeaders.addAll(sections);

        Row row = sheet.getRow(0);
        List<String> actualHeaders = new ArrayList<>();
        for(int j=0;j<row.getLastCellNum();j++){
            Cell currCell= row.getCell(j);
            actualHeaders.add(getCellValueAsString(currCell));
        }

        return expectedHeaders.equals(actualHeaders);
    }


    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double numericVal = cell.getNumericCellValue();
                if (numericVal == (long) numericVal) {
                    return String.valueOf((long) numericVal);
                }
                return String.valueOf(numericVal);
            default:
                return null;
        }
    }
    //upload end



    private SummaryModel createStateSummaryModel(UUID stateId, List<CategoryWiseCountModel> categoryWiseCountModels,int totalAppearedCount,int totalVacancyCount,int totalQualifiedCount){
        return SummaryModel.builder()
                .stateId(stateId)
                .categorySummaries(categoryWiseCountModels)
                .totalAppearedCount(totalAppearedCount)
                .totalVacancyCount(totalVacancyCount)
                .totalQualifiedWithoutRelaxation(totalQualifiedCount)
                .build();
    }


    @Transactional
    public List<ExaminationMarksSummaryModel> getOverallMarksSummary(
            List<UUID> positionIds
    ) {


        List<CandidateWrittenExamMarksEntity> candidateWrittenExamMarks = writtenExamMarksRepository.findAllByPositionIdsWithDetails(positionIds);
        if(candidateWrittenExamMarks.isEmpty()){
            throw new ManualValidationException("No candidate appeared for exam in selected positions");
        }

        // Exams
        List<UUID> examIds = candidateWrittenExamMarks.stream().map(CandidateWrittenExamMarksEntity::getId).toList();

        // Positions
        List<JobPositionsEntity> jobPositions = candidateWrittenExamMarks.stream()
                        .map(CandidateWrittenExamMarksEntity::getPosition)
                        .distinct()
                        .toList();

        // Candidate IDs
        List<UUID> candidateIds = candidateWrittenExamMarks.stream()
                        .map(curr -> curr.getCandidate().getId())
                        .distinct()
                        .toList();

        // Section Marks
        List<CandidateSectionMarksEntity> candidateWrittenExamSections = candidateSectionMarksRepository.findByCandidateExam_IdInWithDetails(examIds);

        Map<UUID, WrittenExamConfigurationEntity> examConfigByPosition =
                candidateWrittenExamMarks.stream()
                        .collect(Collectors.toMap(
                                curr -> curr.getExamConfig().getPositionId(),
                                CandidateWrittenExamMarksEntity::getExamConfig,
                                (existing, curr) -> existing
                        ));

        // Section IDs
        List<UUID> examSectionIds = candidateWrittenExamSections.stream()
                        .map((curr)->curr.getExamSection().getId())
                        .toList();

        // Category Cutoffs
        List<ExamSectionCategoryPassMarksEntity> examSectionCategoryPassMarks = examSectionCategoryPassMarksRepository.findAllByExamSectionIdsWithDetails(examSectionIds);

        // PARTITION POSITIONS
        Map<Boolean, List<JobPositionsEntity>> partitionedPositions = jobPositions.stream().collect(Collectors.partitioningBy(JobPositionsEntity::getIsLocationWise));

        List<JobPositionsEntity> nonStateWisePositions = partitionedPositions.get(false);

        List<JobPositionsEntity> stateWisePositions = partitionedPositions.get(true);

        // Non-state position ids
        Set<UUID> nonStateWisePositionIds = nonStateWisePositions.stream().map(JobPositionsEntity::getId)
                        .collect(Collectors.toSet());

        // PARTITION EXAMS
        Map<Boolean, List<CandidateWrittenExamMarksEntity>> partitionedExams = candidateWrittenExamMarks.stream()
                        .collect(Collectors.partitioningBy(
                                exam -> nonStateWisePositionIds.contains(
                                        exam.getPosition().getId()
                                )
                        ));

        List<CandidateWrittenExamMarksEntity> nonStateWiseExams = partitionedExams.get(true);

        List<CandidateWrittenExamMarksEntity> stateWiseExams = partitionedExams.get(false);

        // Non-state exam ids
        Set<UUID> nonStateWiseExamIds = nonStateWiseExams.stream().map(CandidateWrittenExamMarksEntity::getId)
                        .collect(Collectors.toSet());

        Map<Boolean, List<CandidateSectionMarksEntity>> partitionedSectionMarks = candidateWrittenExamSections.stream()
                        .collect(Collectors.partitioningBy(
                                mark -> nonStateWiseExamIds.contains(mark.getCandidateExam().getId())
                        ));

        Map<UUID, List<CandidateSectionMarksEntity>> nonStateWiseSectionMarksByExam = partitionedSectionMarks.get(true).stream()
                        .collect(Collectors.groupingBy(
                                curr -> curr.getCandidateExam().getId()
                        ));

        Map<UUID, List<CandidateSectionMarksEntity>> stateWiseSectionMarksByExam = partitionedSectionMarks.get(false).stream()
                        .collect(Collectors.groupingBy(
                                curr -> curr.getCandidateExam().getId()
                        ));


        // PARTITION CUTOFFS
        Map<Boolean, List<ExamSectionCategoryPassMarksEntity>> partitionedSections = examSectionCategoryPassMarks.stream()
                        .collect(Collectors.partitioningBy(
                                curr -> curr.getStateId() == null
                        ));

        Map<SectionCategoryKey, ExamSectionCategoryPassMarksEntity>
                nonStateWiseCutOffSections = partitionedSections.get(true)
                        .stream()
                        .collect(Collectors.toMap(
                                curr -> new SectionCategoryKey(
                                        curr.getExamSection().getId(),
                                        curr.getCategory().getId()
                                ),
                                Function.identity()
                        ));

        Map<StateCategoryKey, ExamSectionCategoryPassMarksEntity>
                stateWiseCutoffSections = partitionedSections.get(false).stream()
                        .collect(Collectors.toMap(
                                curr -> new StateCategoryKey(
                                        curr.getExamSection().getId(),
                                        curr.getStateId(),
                                        curr.getCategory().getId()
                                ),
                                Function.identity()
                        ));

        // Profiles
        Map<UUID, CandidateProfileEntity> profileMap = candidateProfileRepository
                        .findAllByCandidateIdIn(candidateIds)
                        .stream()
                        .collect(Collectors.toMap(
                                CandidateProfileEntity::getCandidateId,
                                Function.identity()
                        ));

        // Reservation Categories
        Map<String, UUID> reservationCategoryMap = reservationCategoriesRepository.findByCategoryCodeIn(List.of("GEN", "PWD"))
                        .stream()
                        .collect(Collectors.toMap(
                                ReservationCategoriesEntity::getCategoryCode,
                                ReservationCategoriesEntity::getId
                        ));

        List<ExaminationMarksSummaryModel> summaryModels = new ArrayList<>();
        List<UUID> applicationIds = candidateWrittenExamMarks.stream().map((curr)->curr.getApplication().getId()).toList();
        Map<UUID,CandidateConcessionsEntity> existingConcession = candidateConcessionsRepository.findAllByApplicationIdIn(applicationIds)
                .stream()
                .collect(Collectors.toMap(
                        CandidateConcessionsEntity::getApplicationId,
                        Function.identity()
                ));



        // NON STATE WISE
        if (!nonStateWisePositions.isEmpty()) {

            List<ExaminationMarksSummaryModel> nonStateWiseSummaryModels = buildNonStateWiseMarksSummary(
                    nonStateWisePositions,
                    nonStateWiseExams,
                    nonStateWiseSectionMarksByExam,
                    nonStateWiseCutOffSections,
                    profileMap,
                    examConfigByPosition,
                    reservationCategoryMap
            );
            summaryModels.addAll(nonStateWiseSummaryModels);
        }

        // STATE WISE
        if (!stateWisePositions.isEmpty()) {
            List<ExaminationMarksSummaryModel> stateWiseSummaryModels = buildStateWiseMarksSummary(
                    stateWisePositions,
                    stateWiseExams,
                    stateWiseSectionMarksByExam,
                    stateWiseCutoffSections,
                    profileMap,
                    examConfigByPosition,
                    reservationCategoryMap
            );
            summaryModels.addAll(stateWiseSummaryModels);
        }

        List<CandidateConcessionsEntity> savingConcessions = candidateWrittenExamMarks.stream()
                .filter((curr)->
                        !ExamQualificationStatus.DISQUALIFIED.equals(curr.getStatus()) && !ExamQualificationStatus.NOT_MARKED.equals(curr.getStatus())
                )
                .map((curr)->{
                    UUID applicationId = curr.getApplication().getId();
                    CandidateConcessionsEntity concessionsEntity = existingConcession.getOrDefault(applicationId,null);
                    boolean hasTakenConcession = ExamQualificationStatus.QUALIFIED.equals(curr.getStatus());
                    if(concessionsEntity!=null){
                        concessionsEntity.setExamConcession(hasTakenConcession);
                        return concessionsEntity;
                    }
                    return CandidateConcessionsEntity.builder()
                            .applicationId(applicationId)
                            .examConcession(hasTakenConcession)
                            .build();
                }).toList();

        writtenExamMarksRepository.saveAll(candidateWrittenExamMarks);
        candidateConcessionsRepository.saveAll(savingConcessions);
        return summaryModels;
    }


    public List<ExaminationMarksSummaryModel>
    buildNonStateWiseMarksSummary(
            List<JobPositionsEntity> positions,
            List<CandidateWrittenExamMarksEntity> exams,
            Map<UUID, List<CandidateSectionMarksEntity>> sectionMarksByExam,
            Map<SectionCategoryKey, ExamSectionCategoryPassMarksEntity> categoryCutoffsMap,
            Map<UUID, CandidateProfileEntity> profileMap,
            Map<UUID,WrittenExamConfigurationEntity> examConfigByPosition,
            Map<String, UUID> horizontalCategoryMap
    ) {

        Map<UUID, SummaryTracker> trackerMap = new HashMap<>();
        UUID genCategoryId = horizontalCategoryMap.get("GEN");

        Map<SectionCategoryKey, ExamSectionCategoryPassMarksEntity> urCutOffs =
                categoryCutoffsMap.entrySet()
                        .stream()
                        .filter(entry ->
                                entry.getKey()
                                        .categoryId()
                                        .equals(genCategoryId)
                        )
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ));


        for (JobPositionsEntity position : positions) {

            SummaryTracker tracker = trackerMap.computeIfAbsent(position.getId(), k -> new SummaryTracker());

            for (PositionCategoryNationalDistributionEntity dist : position.getPositionCategoryNationalDistributions()) {

                if (Boolean.TRUE.equals(dist.getIsDisability())
                        && dist.getDisabilityCategoryId() != null) {

                    tracker.addVacancy(horizontalCategoryMap.get("PWD"),dist.getVacancyCount());
                }

                if (dist.getReservationCategoryId() != null) {

                    tracker.addVacancy(dist.getReservationCategoryId(), dist.getVacancyCount());
                }
            }
        }

        // Candidate Processing
        for (CandidateWrittenExamMarksEntity exam : exams) {

            UUID positionId = exam.getPosition().getId();

            UUID candidateId = exam.getCandidate().getId();

            SummaryTracker tracker = trackerMap.get(positionId);


            CandidateProfileEntity profile = profileMap.get(candidateId);



            UUID allocationCategoryId = resolveAllocationCategory(profile, tracker, horizontalCategoryMap,null,null);

            tracker.addAppeared(allocationCategoryId);

            List<CandidateSectionMarksEntity> candidateSections = sectionMarksByExam.getOrDefault(exam.getId(), Collections.emptyList());

            // Non State Wise Qualification Logic
            boolean isQualified = false;
            if(!allocationCategoryId.equals(genCategoryId)){
                isQualified = isQualifiedNonStateWise(candidateSections,categoryCutoffsMap,allocationCategoryId);
            }

            boolean isQualifiedWithoutRelaxation = isQualifiedNonStateWise(candidateSections,urCutOffs,genCategoryId);
            if (isQualified || isQualifiedWithoutRelaxation) {
                tracker.addQualified(allocationCategoryId);
            }

            setQualificationStatusForWrittenExam(candidateSections,isQualified,isQualifiedWithoutRelaxation,exam);
        }
        List<ExaminationMarksSummaryModel> summaries = new ArrayList<>();


        for (JobPositionsEntity position : positions) {
            SummaryTracker tracker = trackerMap.get(position.getId());
            List<CategoryWiseCountModel> categoryWiseCountModel = tracker.getCategorySummaries();
            int totalAppeared = tracker.getTotalAppeared();
            int totalVacanciesCount = tracker.getTotalVacancy(horizontalCategoryMap.get("PWD"));
            int totalQualifiedCount = tracker.getTotalQualified();
            SummaryModel categoryWiseSummaryModel = createStateSummaryModel(null,categoryWiseCountModel,totalAppeared,totalVacanciesCount,totalQualifiedCount);
            WrittenExamConfigurationEntity examConfig = examConfigByPosition.get(position.getId());

            boolean finalized =
                    examConfig.getIsFrozen()
                            && (WrittenExamConfigurationStatus.APPROVED == examConfig.getStatus()
                            || WrittenExamConfigurationStatus.L1_REJECTED == examConfig.getStatus()
                            || WrittenExamConfigurationStatus.L2_REJECTED == examConfig.getStatus()
                            || WrittenExamConfigurationStatus.PENDING == examConfig.getStatus());
            ExaminationMarksSummaryModel summaryModel =ExaminationMarksSummaryModel.
                    builder()
                    .positionId(position.getId())
                    .isStateWise(position.getIsLocationWise())
                    .isFinalized(!finalized)
                    .overallMarksSummary(List.of(categoryWiseSummaryModel))
                    .build();
            summaries.add(summaryModel);

        }

        return summaries;
    }


    public List<ExaminationMarksSummaryModel>
    buildStateWiseMarksSummary(
            List<JobPositionsEntity> positions,
            List<CandidateWrittenExamMarksEntity> exams,
            Map<UUID, List<CandidateSectionMarksEntity>> sectionMarksByExam,
            Map<StateCategoryKey, ExamSectionCategoryPassMarksEntity> categoryCutoffs,
            Map<UUID, CandidateProfileEntity> profileMap,
            Map<UUID,WrittenExamConfigurationEntity> examConfigByPosition,
            Map<String, UUID> horizontalCategoryMap
    ) {

        UUID genCategoryId = horizontalCategoryMap.get("GEN");

        Map<StateCategoryKey, ExamSectionCategoryPassMarksEntity> urCutOffs =
                categoryCutoffs.entrySet()
                        .stream()
                        .filter(entry ->
                                entry.getKey()
                                        .categoryId()
                                        .equals(genCategoryId)
                        )
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ));


        List<UUID> positionIds = positions.stream().map(JobPositionsEntity::getId).toList();

        List<UUID> candidateIds = profileMap.values().stream().map(CandidateProfileEntity::getCandidateId).toList();

        Map<CandidatePositionKey, CandidateLocationPreferenceEntity> locationPrefMap = candidateLocationPreferencesRepository.findAllByCandidateIdInAndPositionIdIn(candidateIds, positionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                curr -> new CandidatePositionKey(curr.getCandidateId(), curr.getPositionId()),
                                Function.identity(),
                                (existing, curr) -> existing
                        ));



        Map<UUID, Map<UUID, SummaryTracker>> trackerMap = new HashMap<>();
        Map<UUID, Map<StateCityCategoryKey,Integer>> stateWiseCityCountMap = new HashMap<>();
        // Vacancy Count
        for (JobPositionsEntity position : positions) {

            UUID positionId = position.getId();

            for (PositionStateDistributionEntity stateDist : position.getPositionStateDistributions()) {

                UUID stateId = stateDist.getStateId();
                UUID cityId = stateDist.getCityId();
                SummaryTracker tracker = trackerMap.computeIfAbsent(positionId, k -> new HashMap<>())
                                .computeIfAbsent(stateId, k -> new SummaryTracker(stateId));

                for (PositionCategoryDistributionEntity catDist : stateDist.getPositionCategoryDistributions()) {

                        if (catDist.getReservationCategoryId() != null) {
                            tracker.addVacancy(catDist.getReservationCategoryId(), catDist.getVacancyCount());
                        }

                        if (Boolean.TRUE.equals(catDist.getIsDisability()) && catDist.getDisabilityCategoryId() != null) {
                            tracker.addVacancy(horizontalCategoryMap.get("PWD"), catDist.getVacancyCount());
                        }
                    if(cityId != null){
                        if (catDist.getReservationCategoryId() != null) {
                            stateWiseCityCountMap.computeIfAbsent(positionId,k->new HashMap<>())
                                    .put(new StateCityCategoryKey(stateId,cityId,catDist.getReservationCategoryId()),catDist.getVacancyCount());
                        }

                        if (Boolean.TRUE.equals(catDist.getIsDisability()) && catDist.getDisabilityCategoryId() != null) {
                            UUID pwdCategoryId = horizontalCategoryMap.get("PWD");
                            stateWiseCityCountMap.computeIfAbsent(positionId,k->new HashMap<>())
                                    .put(new StateCityCategoryKey(stateId,cityId,pwdCategoryId),catDist.getVacancyCount());
                        }
                    }

                }
            }
        }

        // Candidate Processing
        for (CandidateWrittenExamMarksEntity exam : exams) {

            UUID positionId = exam.getPosition().getId();

            UUID candidateId = exam.getCandidate().getId();

            CandidateLocationPreferenceEntity locationPreference= locationPrefMap.get(new CandidatePositionKey(candidateId, positionId));

            Map<StateCityCategoryKey,Integer> positionStateCityCategoryMap = stateWiseCityCountMap.get(positionId);
            Map<UUID, SummaryTracker> stateTrackers = trackerMap.get(positionId);

            SummaryTracker tracker = stateTrackers.get(locationPreference.getStatePreference1());

            CandidateProfileEntity profile = profileMap.get(candidateId);



            UUID allocationCategoryId = resolveAllocationCategory(profile, tracker, horizontalCategoryMap,locationPreference,positionStateCityCategoryMap);

            tracker.addAppeared(allocationCategoryId);

            List<CandidateSectionMarksEntity> candidateSections = sectionMarksByExam.getOrDefault(exam.getId(), Collections.emptyList());

            boolean isQualified = false;
            // State Wise Qualification Logic
            if(!allocationCategoryId.equals(genCategoryId)){
                isQualified = isQualifiedStateWise(candidateSections,categoryCutoffs,locationPreference.getStatePreference1(),allocationCategoryId);
            }

            boolean iQualifiedWithoutRelaxation =isQualifiedStateWise(candidateSections,urCutOffs,locationPreference.getStatePreference1(),genCategoryId);

            if (isQualified || iQualifiedWithoutRelaxation) {
                tracker.addQualified(allocationCategoryId);
            }

            setQualificationStatusForWrittenExam(candidateSections,isQualified,iQualifiedWithoutRelaxation,exam);
        }

        List<ExaminationMarksSummaryModel> summaries = new ArrayList<>();

        for (JobPositionsEntity position : positions) {

            Map<UUID, SummaryTracker> stateTrackerMap = trackerMap.get(position.getId());
            WrittenExamConfigurationEntity examConfig = examConfigByPosition.get(position.getId());


            Collection<SummaryTracker> stateTrackers = stateTrackerMap.values();

            List<SummaryModel> stateSummaries = new ArrayList<>();


            for (SummaryTracker tracker : stateTrackers) {
                SummaryModel summaryModel = createStateSummaryModel(tracker.getStateId(),tracker.getCategorySummaries(),
                        tracker.getTotalAppeared(),tracker.getTotalVacancy(horizontalCategoryMap.get("PWD")),
                        tracker.getTotalQualified());
                stateSummaries.add(summaryModel);
            }
            boolean finalized =  examConfig.getIsFrozen()
                    && (WrittenExamConfigurationStatus.APPROVED == examConfig.getStatus()
                    || WrittenExamConfigurationStatus.L1_REJECTED == examConfig.getStatus()
                    || WrittenExamConfigurationStatus.L2_REJECTED == examConfig.getStatus()
                    || WrittenExamConfigurationStatus.PENDING == examConfig.getStatus());

            summaries.add(
                    ExaminationMarksSummaryModel.builder()
                            .positionId(position.getId())
                            .isStateWise(true)
                            .isFinalized(!finalized)
                            .overallMarksSummary(stateSummaries)
                            .build()
            );
        }
        return summaries;
    }

    record StateCityCategoryKey(UUID stateId,UUID cityId,UUID categoryId){}
    record CandidatePositionKey(UUID candidateId, UUID positionId) {}
    record StateCategoryKey(UUID sectionId, UUID stateId, UUID categoryId) {}
    record SectionCategoryKey(UUID sectionId, UUID categoryId) {}

    private UUID resolveAllocationCategory(CandidateProfileEntity profile, SummaryTracker tracker, Map<String,UUID> horizontalMap,
                                           CandidateLocationPreferenceEntity locationPreference,Map<StateCityCategoryKey,Integer> stateCityCategoryMap) {

        UUID candidateCategoryId = profile.getReservationCategoryId();
        UUID pwdCategoryId = horizontalMap.get("PWD");
        if(stateCityCategoryMap!=null && !stateCityCategoryMap.isEmpty()
                && locationPreference!=null && locationPreference.getLocationPreference1()!=null){
            UUID stateId = locationPreference.getStatePreference1();
            UUID cityId = locationPreference.getLocationPreference1();

            if (Boolean.TRUE.equals(profile.getDisability()) && stateCityCategoryMap.getOrDefault(new StateCityCategoryKey(stateId,cityId,pwdCategoryId),0) > 0) {
                return pwdCategoryId;
            } else if(stateCityCategoryMap.getOrDefault(new StateCityCategoryKey(stateId,cityId,candidateCategoryId),0) <=0){
                 return horizontalMap.get("GEN");
            }

            return candidateCategoryId;
        }

            if (Boolean.TRUE.equals(profile.getDisability()) && tracker.getVacancyCount(pwdCategoryId) > 0) {
                return pwdCategoryId;
            } else if (tracker.getVacancyCount(candidateCategoryId) <= 0) {
                return horizontalMap.get("GEN");
            }

            return candidateCategoryId;


    }

    private boolean isQualifiedNonStateWise(List<CandidateSectionMarksEntity> writtenSections,
                                            Map<SectionCategoryKey,ExamSectionCategoryPassMarksEntity> categoryCutoffsMap,UUID allocationCategoryId){

        return !writtenSections.isEmpty() &&
                writtenSections.stream().allMatch(writtenSec -> {

                    ExamSectionPassMarksEntity currSection = writtenSec.getExamSection();
                    int totalMarksOfSection = currSection.getSectionTotalMarks();
                    ExamSectionCategoryPassMarksEntity cutoffSection = categoryCutoffsMap.get(new SectionCategoryKey(currSection.getId(), allocationCategoryId));

                    double cutoffPercentage = cutoffSection.getPassMark()/100.0d;
                    double passMark = totalMarksOfSection*cutoffPercentage;

                    return writtenSec.getMarksObtained().compareTo(BigDecimal.valueOf(passMark)) >= 0;
                });

    }

    private boolean isQualifiedStateWise(List<CandidateSectionMarksEntity> writtenSections, Map<StateCategoryKey,ExamSectionCategoryPassMarksEntity> categoryCutoffsMap,
                                         UUID stateId,UUID allocationCategoryId){

        return !writtenSections.isEmpty() && writtenSections.stream()
                .allMatch(writtenSec -> {

                    ExamSectionPassMarksEntity currSection = writtenSec.getExamSection();
                    int totalMarksOfSection = currSection.getSectionTotalMarks();

                    ExamSectionCategoryPassMarksEntity cutoffSection = categoryCutoffsMap.get(
                            new StateCategoryKey(currSection.getId(), stateId, allocationCategoryId));

                    double cutoffPercentage = cutoffSection.getPassMark()/100.0d;
                    double passMark = totalMarksOfSection*cutoffPercentage;


                    return writtenSec.getMarksObtained().compareTo(BigDecimal.valueOf(passMark)) >= 0;
                });

    }



    public void setQualificationStatusForWrittenExam(List<CandidateSectionMarksEntity> writtenSectionMarks,boolean isQualified,boolean isQualifiedWithoutRelaxation,CandidateWrittenExamMarksEntity exam){
        BigDecimal rankingObtainedScore = writtenSectionMarks.stream()
                .filter(curr -> Boolean.TRUE.equals(curr.getExamSection().getIsRankingEnabled()))
                .map(CandidateSectionMarksEntity::getMarksObtained)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ExamQualificationStatus status = ExamQualificationStatus.DISQUALIFIED;
        if(isQualified){
            status = ExamQualificationStatus.QUALIFIED;
        }
        if(isQualifiedWithoutRelaxation){
            status = ExamQualificationStatus.QUALIFIED_UNDER_UR;
        }
        exam.setIsPassed(isQualified || isQualifiedWithoutRelaxation);
        exam.setStatus(status);
        exam.setRankingMarksObtained(rankingObtainedScore);
    }





}
