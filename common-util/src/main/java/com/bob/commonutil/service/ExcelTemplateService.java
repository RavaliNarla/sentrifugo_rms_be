package com.bob.commonutil.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.ExcelValidationException;
import com.bob.commonutil.model.ExcelTemplateFile;

import com.bob.db.util.context.ExcelFilterContext;
import com.bob.db.util.DBConstants;
import com.bob.db.util.excel.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class ExcelTemplateService {

    public static final String TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @PersistenceContext
    private EntityManager entityManager;

    private String getEffectiveHeader(Field field) {
        if (field.isAnnotationPresent(ExcelHeader.class)) {
            return field.getAnnotation(ExcelHeader.class).value().trim();
        }
        if (field.isAnnotationPresent(ExcelDropdown.class)) {
            return field.getAnnotation(ExcelDropdown.class).displayField().trim();
        }
        return field.getName();
    }

    private int createHeaderRow(Workbook workbook,Sheet sheet, Class<?> dtoClass,Map<String,Integer> columnMap,Set<String> excludedFields,int startRow) {
        Row headerRow = sheet.createRow(startRow);
        int col = 0;
        for (Field field : dtoClass.getDeclaredFields()) {
            if (isExcludedField(field.getName())) {
                continue;
            }
            if(excludedFields!=null && excludedFields.contains(field.getName())){
                continue;
            }
            // Get label from custom annotation or fallback
            String headerLabel = getEffectiveHeader(field);

            Cell cell = headerRow.createCell(col);
            cell.setCellValue(headerLabel);

            // Styling: Make headers bold
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);

            if(columnMap != null)
                columnMap.put(field.getName(), col);
            col++;
        }
        return col;
    }

    public ExcelTemplateFile generateExcelTemplate(Class<?> dtoClass) {
        return buildTemplate(dtoClass, null,null);
    }

    public ExcelTemplateFile generateExcelTemplateWithData(Class<?> dtoClass, List<?> dtoList,Set<String> excludedFields) {
        if (!dtoList.isEmpty() && !dtoClass.isAssignableFrom(dtoList.get(0).getClass())) {
            throw new IllegalArgumentException("DTO class does not match the list element type.");
        }
        return buildTemplate(dtoClass, dtoList,excludedFields);
    }

    private ExcelTemplateFile buildTemplate(Class<?> dtoClass, List<?> dtoList,Set<String> excludedFields) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(dtoClass.getSimpleName());

            Map<String, Integer> columnMap = new LinkedHashMap<>();

            // 1. BUILD HEADERS
            int col = createHeaderRow(workbook, sheet, dtoClass, columnMap, excludedFields,0);

            // 2. PROCESS DROPDOWNS (Hidden Sheets + Named Ranges)
            processDrowDownAnnotation(dtoClass, workbook, sheet, columnMap);

            // Apply yes/no boolean
            processBooleanColumns(dtoClass, workbook, sheet, columnMap);

            // Apply date validations and formatting
            processDateColumns(dtoClass, workbook, sheet, columnMap);

            // Apply LocalTime validations and formatting
            processTimeValidation(dtoClass, workbook, sheet, columnMap);

            // Apply decimal validations
            processDecimalColumns(dtoClass, workbook, sheet, columnMap);

            // Apply string validations (text length)
            processStringColumns(dtoClass, workbook, sheet, columnMap);

            // 3. Apply other validations (e.g., integer validation)
            applyValidations(sheet, dtoClass, columnMap, 500);

            // 4. WRITE PRE-POPULATED DATA ROWS if provided
            if (dtoList != null && !dtoList.isEmpty()) {
                writeDTOListDataToSheet(workbook, dtoClass, dtoList, sheet, excludedFields,1);
            }

            // 5. AUTO-SIZE COLUMNS
            for (int i = 0; i < col; i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception ignored) {}
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            ExcelTemplateFile file = new ExcelTemplateFile();
            file.setFileName(dtoClass.getSimpleName() + DBConstants.EXCEL_TEMPLATE_SUFFIX);
            file.setFileContent(out.toByteArray());
            return file;

        } catch (Exception e) {
            throw new CommonException("Failed to generate Excel template: " + e.getMessage());
        }
    }

    private void processBooleanColumns(Class<?> dtoClass, Workbook workbook, Sheet sheet, Map<String, Integer> columnMap) {
        boolean booleanOptionsCreated = false;
        final String BOOLEAN_SHEET_NAME = "BooleanOptions";
        final String BOOLEAN_RANGE_NAME = "BooleanList";

        for (Field field : dtoClass.getDeclaredFields()) {
            if (isExcludedField(field.getName())) {
                continue;
            }

            // Check if the field is a Boolean or boolean type
            if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                Integer columnIndex = columnMap.get(field.getName());
                if (columnIndex == null) {
                    continue;
                }

                if (!booleanOptionsCreated) {
                    // Create a hidden sheet for "Yes" and "No" options
                    Sheet booleanSheet = workbook.createSheet(BOOLEAN_SHEET_NAME);
                    workbook.setSheetHidden(workbook.getSheetIndex(booleanSheet), true);

                    // Populate the hidden sheet
                    Row headerRow = booleanSheet.createRow(0);
                    headerRow.createCell(0).setCellValue("Option");
                    booleanSheet.createRow(1).createCell(0).setCellValue("Yes");
                    booleanSheet.createRow(2).createCell(0).setCellValue("No");

                    // Create a named range for "Yes" and "No"
                    Name namedRange = workbook.createName();
                    namedRange.setNameName(BOOLEAN_RANGE_NAME);
                    namedRange.setRefersToFormula(BOOLEAN_SHEET_NAME + "!$A$2:$A$3"); // A2 for Yes, A3 for No
                    booleanOptionsCreated = true;
                }

                // Apply the dropdown to the current boolean column
                applyDropdown(sheet, BOOLEAN_RANGE_NAME, columnIndex, 500);
            }
        }
    }

    private void processDateColumns(Class<?> dtoClass, Workbook workbook, Sheet sheet, Map<String, Integer> columnMap) {
        CellStyle dateCellStyle = createDateCellStyle(workbook);
        for (Field field : dtoClass.getDeclaredFields()) {
            if (isExcludedField(field.getName())) {
                continue;
            }

            // Check if the field is a LocalDate type
            if (field.getType() == LocalDate.class) {
                Integer columnIndex = columnMap.get(field.getName());
                if (columnIndex == null) {
                    continue;
                }
                applyDateValidation(sheet, columnIndex, 500);

                // Apply date cell style to the column
                for (int rowNum = 1; rowNum <= 500; rowNum++) { // Apply to data rows
                    Row row = sheet.getRow(rowNum);
                    if (row == null) {
                        row = sheet.createRow(rowNum);
                    }
                    Cell cell = row.getCell(columnIndex);
                    if (cell == null) {
                        cell = row.createCell(columnIndex);
                    }
                    cell.setCellStyle(dateCellStyle);
                }
            }
        }
    }
    private void processTimeValidation(Class<?> dtoClass, Workbook workbook, Sheet sheet, Map<String, Integer> columnMap) {
        CellStyle timeCellStyle = createTimeCellStyle(workbook);
        for(Field field : dtoClass.getDeclaredFields()){
            if(isExcludedField(field.getName())){
                continue;
            }

            // Check if the field is a LocalTime type
            if (field.getType() == LocalTime.class) {
                Integer columnIndex = columnMap.get(field.getName());
                if (columnIndex == null) {
                    continue;
                }
                applyTimeValidation(sheet, columnIndex, 500);

                // Apply time cell style to the column
                for (int rowNum = 1; rowNum <= 500; rowNum++) { // Apply to data rows
                    Row row = sheet.getRow(rowNum);
                    if (row == null) {
                        row = sheet.createRow(rowNum);
                    }
                    Cell cell = row.getCell(columnIndex);
                    if (cell == null) {
                        cell = row.createCell(columnIndex);
                    }
                    cell.setCellStyle(timeCellStyle);
                }
            }
        }

    }

    private void applyTimeValidation(Sheet sheet, Integer col, int row) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        //Allow the constraints to accept time values between 00:00:00 and 23:59:59
        DataValidationConstraint constraint = helper.createTimeConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                "TIME(0,0,0)",
                "TIME(23,59,59)"
        );
        CellRangeAddressList addressList = new CellRangeAddressList(1, row, col, col);

        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid Time", "Please enter a valid time (HH:mm) in 24-hour format.");
        validation.setEmptyCellAllowed(true); // Allow empty cells for optional dates

        sheet.addValidationData(validation);

    }

    private CellStyle createTimeCellStyle(Workbook workbook) {
        CellStyle timeCellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        timeCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("HH:mm"));
        return timeCellStyle;
    }


    private void processDecimalColumns(Class<?> dtoClass, Workbook workbook, Sheet sheet, Map<String, Integer> columnMap) {
        for (Field field : dtoClass.getDeclaredFields()) {
            if (isExcludedField(field.getName())) {
                continue;
            }

            // Check if the field is a BigDecimal type
            if (field.getType() == BigDecimal.class) {
                Integer columnIndex = columnMap.get(field.getName());
                if (columnIndex == null) {
                    continue;
                }
                applyDecimalValidation(sheet, columnIndex, 500);
            }
        }
    }

    private void processStringColumns(Class<?> dtoClass, Workbook workbook, Sheet sheet, Map<String, Integer> columnMap) {
        for (Field field : dtoClass.getDeclaredFields()) {
            if (isExcludedField(field.getName())) {
                continue;
            }

            // Check if the field is a String type
            if (field.getType() == String.class) {
                Integer columnIndex = columnMap.get(field.getName());
                int min = 0;
                int max = 255;
                if(field.isAnnotationPresent(ExcelTextLength.class)){
                    ExcelTextLength excelTextLength=field.getAnnotation(ExcelTextLength.class);
                    min=excelTextLength.min();
                    max=excelTextLength.max();
                }
                if (columnIndex == null) {
                    continue;
                }
                applyTextLengthValidation(sheet, columnIndex, 500,min,max);
            }
        }
    }

    private void processDrowDownAnnotation(Class<?> dtoClass, Workbook workbook, Sheet sheet, Map<String, Integer> columnMap) throws IllegalAccessException, NoSuchFieldException {
        int dropdownIndex = 1;
        for (Field field : dtoClass.getDeclaredFields()) {
            if (isExcludedField(field.getName())) {
                continue;
            }

            if(field.isAnnotationPresent(ExcelDropdown.class)) {
                ExcelDropdown meta = field.getAnnotation(ExcelDropdown.class);
                List<?> masterData = fetchMasterData(meta.masterClass(), meta.filters());
                if (masterData.isEmpty()) continue;

                // Hidden Sheet for data reference
                String hiddenSheetName = "ref_data_" + dropdownIndex++;
                createHiddenSheet(workbook, hiddenSheetName, masterData, meta.displayField());

                // Named Range (Critical for compatibility)
                String rangeName = "List_" + field.getName();
                Name namedRange = workbook.createName();
                namedRange.setNameName(rangeName);
                namedRange.setRefersToFormula(hiddenSheetName + "!$A$2:$A$" + (masterData.size() + 1));

                // Apply to the specific column index recorded in columnMap
                applyDropdown(sheet, rangeName, columnMap.get(field.getName()), 500);
            }
            if (field.isAnnotationPresent(ExcelEnumDropdown.class)) {
                ExcelEnumDropdown enumMeta = field.getAnnotation(ExcelEnumDropdown.class);
                Class<? extends Enum<?>> enumClass = enumMeta.enumClass();

                List<String> enumValues = new ArrayList<>();
                for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                    if (!enumMeta.displayField().isEmpty()) {
                        Field displayField = enumClass.getDeclaredField(enumMeta.displayField());
                        displayField.setAccessible(true);
                        enumValues.add(displayField.get(enumConstant).toString());
                    } else {
                        enumValues.add(enumConstant.name());
                    }
                }

                String hiddenSheetName = "enum_ref_" + dropdownIndex++;
                createEnumHiddenSheet(workbook, hiddenSheetName, enumValues);

                String rangeName = "EnumList_" + field.getName().replace(".", "_"); // Safe naming
                createNamedRange(workbook, rangeName, hiddenSheetName, enumValues.size());
                applyDropdown(sheet, rangeName, columnMap.get(field.getName()), 500);
            }
        }
    }

    private void createEnumHiddenSheet(Workbook workbook, String sheetName, List<String> values) {
        Sheet sheet = workbook.createSheet(sheetName);
        for (int i = 0; i < values.size(); i++) {
            sheet.createRow(i).createCell(0).setCellValue(values.get(i));
        }
        workbook.setSheetHidden(workbook.getSheetIndex(sheet), true);
    }

    private void createNamedRange(Workbook workbook, String rangeName, String hiddenSheetName, int size) {
        Name namedRange = workbook.createName();
        namedRange.setNameName(rangeName);
        namedRange.setRefersToFormula(hiddenSheetName + "!$A$1:$A$" + size);
    }
    private Map<String, Map<String, UUID>> buildDropdownCache(Class<?> dtoClass) {
        Map<String, Map<String, UUID>> dropdownCache = new HashMap<>();
        for (Field field : dtoClass.getDeclaredFields()) {
            if (isExcludedField(field.getName())) {
                continue;
            }
            //Find the dropdown fields and fetch the master data to build a cache of display value to ID mapping for quick lookup during import validation
            if (field.isAnnotationPresent(ExcelDropdown.class)) {
                ExcelDropdown meta = field.getAnnotation(ExcelDropdown.class);
                List<?> masterData = fetchMasterData(meta.masterClass(), meta.filters());
                if (masterData.isEmpty()) {
                    continue;
                }
                Map<String, UUID> valueMap = new HashMap<>();
                for (Object obj : masterData) {
                    try {
                        // Use reflection to get the display value and ID from the master data object
                        Field displayField = obj.getClass().getDeclaredField(meta.displayField());
                        displayField.setAccessible(true);
                        //Find the id field
                        Field idField = findIdField(obj.getClass());
                        idField.setAccessible(true);
                        String name = String.valueOf(displayField.get(obj));
                        UUID id = (UUID) idField.get(obj);
                        //Map it like String,UUID for quick lookup during import validation
                        valueMap.put(name, id);

                    } catch (Exception e) {
                        throw new CommonException("Error building dropdown cache for field: " + field.getName());
                    }
                }
                String key=getDropdownCacheKey(dtoClass, field);
                dropdownCache.put(key, valueMap);
            }
        }

        return dropdownCache;
    }

    private String getDropdownCacheKey(Class<?> dtoClass, Field field) {
        return dtoClass.getSimpleName() + "." + field.getName();
    }
    public <T> List<T> excelToDto(InputStream is, Class<T> dtoClass) {
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Map<String, Map<String, UUID>> dropdownCache=buildDropdownCache(dtoClass);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // 1. VALIDATE HEADERS (Structural Check)
            // This ensures the uploaded file matches the prescribed template exactly
            List<String> headersInFile = validateHeaders(rows, dtoClass);

            List<T> result = new ArrayList<>();
            List<String> allErrors = new ArrayList<>();

            while (rows.hasNext()) {
                Row row = rows.next();
                int rowNum = row.getRowNum() + 1;

                // 2. SKIP EMPTY ROWS
                // Real-world safety: ignores ghost data or blank rows at the bottom
                if (isRowEmpty(row)) continue;

                T dto = dtoClass.getDeclaredConstructor().newInstance();
                List<String> currentRowErrors = new ArrayList<>();

                for (int i = 0; i < headersInFile.size(); i++) {
                    String headerLabel = headersInFile.get(i);
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                    // Find the DTO field that matches this Excel column header
                    Field matchedField = findFieldByHeader(dtoClass, headerLabel);
                    if (matchedField == null) continue;


                    // 3. MANDATORY VALIDATION
                    ExcelHeader excelHeader=matchedField.getAnnotation(ExcelHeader.class);
                    boolean isMandatory=excelHeader.isMandatory();
                    if (isCellEmpty(cell) && isMandatory) {
                        currentRowErrors.add("'" + headerLabel + "' is mandatory");
                        continue; // Skip Regex if the field is missing
                    }

                    try {
                        Object value = resolveCellValue(cell, matchedField);
                        String strVal = (value != null) ? value.toString().trim() : "";

                        // 4. REGEX VALIDATION
                        if (matchedField.isAnnotationPresent(com.bob.db.util.excel.RegexValidate.class)) {
                            com.bob.db.util.excel.RegexValidate anno =
                                    matchedField.getAnnotation(com.bob.db.util.excel.RegexValidate.class);

                            if (!strVal.matches(anno.regex().getRegex())) {
                                currentRowErrors.add(headerLabel + " '" + strVal + "' - " + anno.message());
                            }
                        }

                        // 5. POPULATE DTO
                        // Only populate if no errors found for THIS ROW so far
                        if (currentRowErrors.isEmpty()) {
                            populateFieldValue(dto, matchedField, cell,dropdownCache,dtoClass);
                        }
                    } catch (Exception e) {
                        currentRowErrors.add(headerLabel + " - Invalid data format");
                    }
                }

                // 6. ERROR AGGREGATION
                if (!currentRowErrors.isEmpty()) {
                    allErrors.add("Row " + rowNum + ": " + String.join(" | ", currentRowErrors));
                } else {
                    result.add(dto);
                }
            }

            // 7. FINAL EXCEPTION THROW
            if (!allErrors.isEmpty()) {
                throw new ExcelValidationException(allErrors);
            }

            if(result.isEmpty()){
                throw new ExcelValidationException(List.of("Excel file contains only empty rows. Please enter valid data."));
            }


            return result;

        } catch (ExcelValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelValidationException(List.of("Critical error parsing file: " + e.getMessage()));
        }
    }

    private boolean isCellEmpty(Cell cell) {
        if(cell==null) return true;
        CellType cellType=cell.getCellType();
        //If the cell type is formula like dropdown.It will return the type
        if(cellType==CellType.FORMULA) cellType=cell.getCachedFormulaResultType();
        //If the cell is blank
        if(cellType==CellType.BLANK) return true;
        //If the cell contain whitespaces
        if(cellType==CellType.STRING && cell.getStringCellValue().trim().isEmpty()) return cell.getStringCellValue().trim().isEmpty();
        return false;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (!isCellEmpty(cell)) return false;
        }
        return true;
    }
    private List<String> validateHeaders(Iterator<Row> rows, Class<?> dtoClass) {

        if (!rows.hasNext()) {
            throw new ExcelValidationException(
                    List.of("Invalid Excel file: Header row is missing")
            );
        }

        Row headerRow = rows.next();

        if (headerRow == null || headerRow.getPhysicalNumberOfCells() == 0) {
            throw new ExcelValidationException(
                    List.of("Invalid Excel file: Header row is empty")
            );
        }

        Set<String> actualHeaders = new LinkedHashSet<>();

        for (Cell cell : headerRow) {
            if (cell.getCellType() != CellType.STRING) {
                throw new ExcelValidationException(
                        List.of("Invalid Excel file: Header contains non-text values")
                );
            }
            actualHeaders.add(cell.getStringCellValue().trim());
        }

        Set<String> expectedHeaders = getExpectedHeaders(dtoClass);

        if (!actualHeaders.equals(expectedHeaders)) {
            throw new ExcelValidationException(
                    List.of(
                            "The uploaded Excel file does not match the required template.",
                            "Expected columns: " + expectedHeaders,
                            "Found columns: " + actualHeaders,
                            "Please download and use the latest Excel template."
                    )
            );
        }

        return new ArrayList<>(actualHeaders);
    }
    private void populateFieldValue(Object dto, Field field, Cell cell,Map<String,Map<String,UUID>> dropdownCache,Class<?> dtoClass) throws IllegalAccessException, NoSuchFieldException {

        Object value = resolveCellValue(cell, field);
        if (value == null) return;

        field.setAccessible(true);

        if (field.isAnnotationPresent(ExcelDropdown.class)) {

//            ExcelDropdown meta = field.getAnnotation(ExcelDropdown.class);
            String key=getDropdownCacheKey(dtoClass, field);
            Map<String,UUID> valueMap=dropdownCache.get(key);

            UUID resolvedId = valueMap.get(value.toString());
            if (resolvedId == null) {
                throw new CommonException("Invalid value '" + value + "' for field " + field.getName());
            }

            field.set(dto, resolvedId);

        }else if(field.isAnnotationPresent(ExcelEnumDropdown.class)){
            ExcelEnumDropdown enumMeta = field.getAnnotation(ExcelEnumDropdown.class);
            boolean isValid = validateEnumValue(enumMeta.enumClass(), value.toString(), enumMeta.displayField());

            if (isValid) {
                field.set(dto, value);
            } else {
                throw new CommonException("Invalid value '" + value + "' for field " + field.getName());
            }

        }else {
            field.set(dto, value);
        }
    }

    private boolean validateEnumValue(Class<? extends Enum<?>> enumClass, String value, String displayField) throws IllegalAccessException, NoSuchFieldException {
        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (displayField.isEmpty()) {
                if (constant.name().equalsIgnoreCase(value)) return true;
            } else {
                Field f = enumClass.getDeclaredField(displayField);
                f.setAccessible(true);
                if (f.get(constant).toString().equalsIgnoreCase(value)) return true;
            }
        }
        return false;
    }
    private Object resolveEnumFromValue(Class<? extends Enum<?>> enumClass, String value, String displayField) throws IllegalAccessException, NoSuchFieldException {
        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (displayField.isEmpty()) {
                if (constant.name().equalsIgnoreCase(value)) return constant;
            } else {
                Field f = enumClass.getDeclaredField(displayField);
                f.setAccessible(true);
                if (f.get(constant).toString().equalsIgnoreCase(value)) return constant;
            }
        }
        return null;
    }
    private Object resolveCellValue(Cell cell, Field field) {

        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (field.isAnnotationPresent(ExcelDropdown.class)) {
            return (cell.getCellType() == CellType.STRING)
                    ? cell.getStringCellValue()
                    : cell.toString();
        }

        return getCellValue(cell, field.getType());
    }
    private boolean matchesHeader(Field field, String header) {

        if (field.isAnnotationPresent(ExcelDropdown.class)) {
            ExcelDropdown meta = field.getAnnotation(ExcelDropdown.class);
            return header.equalsIgnoreCase(meta.displayField())
                    || header.equalsIgnoreCase(field.getName());
        }

        return field.getName().equalsIgnoreCase(header);
    }


    // ---------------- HELPER METHODS ----------------

    private void createHiddenSheet(Workbook workbook, String sheetName, List<?> data, String displayField) throws NoSuchFieldException, IllegalAccessException {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue(displayField);

        int row = 1;
        for (Object obj : data) {
            Field field = obj.getClass().getDeclaredField(displayField);
            field.setAccessible(true);
            Object val = field.get(obj);
            sheet.createRow(row++).createCell(0).setCellValue(val != null ? val.toString() : "");
        }

        workbook.setSheetHidden(workbook.getSheetIndex(sheet), true);
    }

    private void applyDropdown(Sheet sheet, String formulaOrNamedRange, int col, int rows) {
        DataValidationHelper helper = sheet.getDataValidationHelper();

        DataValidationConstraint constraint = helper.createFormulaListConstraint(formulaOrNamedRange);

        CellRangeAddressList addressList = new CellRangeAddressList(1, rows, col, col);

        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid Selection", "Please select a value from the dropdown list.");

        sheet.addValidationData(validation);
    }

    private void applyValidations(Sheet sheet, Class<?> dtoClass, Map<String, Integer> columnMap, int rows) {
        for (Field field : dtoClass.getDeclaredFields()) {
            if (isExcludedField(field.getName())) {
                continue;
            }

            Integer columnIndex = columnMap.get(field.getName());
            if (columnIndex == null) {
                continue;
            }

            // Apply integer validation for Integer/int fields, but only if it's not a dropdown
            if (!field.isAnnotationPresent(ExcelDropdown.class) &&
                    (field.getType() == Integer.class || field.getType() == int.class)) {
                applyIntegerValidation(sheet, columnIndex, rows);
            }
        }
    }

    private void applyIntegerValidation(Sheet sheet, int col, int rows) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        // Allow integers between a large range (e.g., -2^31 to 2^31 - 1)
        DataValidationConstraint constraint = helper.createIntegerConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                String.valueOf(Integer.MIN_VALUE),
                String.valueOf(Integer.MAX_VALUE)
        );

        CellRangeAddressList addressList = new CellRangeAddressList(1, rows, col, col);

        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid Input", "Please enter a whole number.");

        sheet.addValidationData(validation);
    }

    private void applyDecimalValidation(Sheet sheet, int col, int rows) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        // Allow decimals between a large range
        DataValidationConstraint constraint = helper.createDecimalConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                "-99999999999999999999999999999999999999", // A very small number
                "99999999999999999999999999999999999999"  // A very large number
        );

        CellRangeAddressList addressList = new CellRangeAddressList(1, rows, col, col);

        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid Input", "Please enter a valid decimal number.");
        validation.setEmptyCellAllowed(true); // Allow empty cells for optional decimals

        sheet.addValidationData(validation);
    }

    private void applyTextLengthValidation(Sheet sheet, int col, int rows,int min,int max) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        // Allow text length between 0 and 255 characters (a common maximum for Excel cells)
        DataValidationConstraint constraint = helper.createTextLengthConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                String.valueOf(min),
                String.valueOf(max)
        );

        CellRangeAddressList addressList = new CellRangeAddressList(1, rows, col, col);

        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid Text Length", "Text length must be between "+min+" and "+max+" characters.");
        validation.setEmptyCellAllowed(true); // Allow empty cells for optional text fields

        sheet.addValidationData(validation);
    }

    private void applyDateValidation(Sheet sheet, int col, int rows) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        // Allow dates between a reasonable range, e.g., 1900-01-01 to 2100-12-31
        // Excel stores dates as numbers, where 1900-01-01 is 1.
        // Using string formulas for start and end dates.
        DataValidationConstraint constraint = helper.createDateConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                "DATE(1900,1,1)", // Start date
                "DATE(2100,12,31)", // End date
                null // No explicit error message for formula
        );

        CellRangeAddressList addressList = new CellRangeAddressList(1, rows, col, col);

        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.createErrorBox("Invalid Date", "Please enter a valid date in YYYY-MM-DD format.");
        validation.setEmptyCellAllowed(true); // Allow empty cells for optional dates

        sheet.addValidationData(validation);
    }

    private CellStyle createDateCellStyle(Workbook workbook) {
        CellStyle dateCellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        return dateCellStyle;
    }

    private boolean isExcludedField(String field) {
        return List.of(
                "id",
                "createdBy",
                "modifiedBy",
                "createdDate",
                "modifiedDate",
                "isActive",
                "serialVersionUID",
                "displayOrder"
        ).contains(field);
    }

    private List<?> fetchMasterData(Class<?> entityClass, ExcelFilter[] filters) {

        StringBuilder jpql = new StringBuilder(
                "FROM " + entityClass.getSimpleName() + " e"
        );

        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        if (filters != null && filters.length != 0) {
            for (ExcelFilter filter : filters) {
                Object value=ExcelFilterContext.get(filter.param());
                if(value==null){
                    throw new CommonException("Filter parameter '" + filter.param() + "' is not set in ExcelFilterContext.");
                }
                // Basic validation for IN operator
                if(filter.operator().equals(Operator.IN)){
                    if(!(value instanceof Collection)){
                        throw new CommonException(
                                "Filter parameter '" + filter.param() +
                                        "' should be a collection for IN operator."
                        );
                    }
                    if(((Collection<?>) value).isEmpty()){
                        return Collections.emptyList();
                    }
                }
                StringBuilder condition = new StringBuilder("e." + filter.field());
                switch (filter.operator()){
                    case EQUAL -> condition.append(" = :").append(filter.param());

                    case IN -> condition.append(" IN :").append(filter.param());

                    case LIKE -> condition.append(" LIKE :").append(filter.param());

                    case GREATER_THAN -> condition.append(" > :").append(filter.param());

                    case LESS_THAN -> condition.append(" < :").append(filter.param());
                }
                if (!conditions.isEmpty()) {
                    condition.insert(0, filter.logical().name() + " ");
                }

                conditions.add(condition.toString());
                params.put(filter.param(), value);
            }
        }
        if (!conditions.isEmpty()) {
            jpql.append(" WHERE ");
            jpql.append(String.join(" ", conditions));
        }
        var query = entityManager.createQuery(jpql.toString(), entityClass);
        params.forEach(query::setParameter);

//        log.info("Fetching master data for dropdown with JPQL: {}", jpql);
        return query.getResultList();
    }


    private Field findIdField(Class<?> clazz) {
        if(clazz==null) throw new IllegalArgumentException("No class provided.");
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField("id");
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new CommonException("No id field found in " + clazz.getName());
    }
    private Field findFieldByHeader(Class<?> dtoClass, String headerLabel) {
        for (Field field : dtoClass.getDeclaredFields()) {
            if (getEffectiveHeader(field).equalsIgnoreCase(headerLabel)) {
                return field;
            }
        }
        return null;
    }
    public static boolean hasExcelFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType());
    }

    private Object getCellValue(Cell cell, Class<?> targetType) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        if (targetType == String.class) {
            if (cellType == CellType.NUMERIC) {
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            }
            return cell.toString().trim();
        }

        if (targetType == BigDecimal.class) {
            if (cellType == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            return new BigDecimal(cell.toString().trim());
        }

        if (targetType == LocalDate.class) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            return LocalDate.parse(cell.toString().trim());
        }

        if (targetType == UUID.class) {
            return UUID.fromString(cell.toString().trim());
        }
        if (targetType == LocalTime.class) {

            //Case-1:when cell contains number
            if (cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalTime();
            }

            // Case 2 — String value in Excel (e.g., "09:30", "09:30 AM", "18:45")
            String value = cell.toString().trim();

            if (value.isEmpty()) {
                return null;
            }

            try {
                return LocalTime.parse(value); // Supports: 09:30 or 09:30:00
            } catch (Exception e) {
                try {
                    return LocalTime.parse(value, DateTimeFormatter.ofPattern("hh:mm a")); // 09:30 AM
                } catch (Exception ex) {
                    return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm")); // 18:45
                }
            }
        }

        if (targetType == Integer.class || targetType == int.class) {
            if (cellType == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            return Integer.parseInt(cell.toString().trim());
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            if (cellType == CellType.BOOLEAN) {
                return cell.getBooleanCellValue();
            }
            String value = cell.toString().trim().toLowerCase();
            if (value.isEmpty()) return null;
            return value.equals("true") || value.equals("yes") || value.equals("1");
        }

        return null;
    }

    private Set<String> getExpectedHeaders(Class<?> dtoClass) {
        Set<String> expectedHeaders = new LinkedHashSet<>();
        for (Field field : dtoClass.getDeclaredFields()) {
            if (isExcludedField(field.getName())) continue;

            expectedHeaders.add(getEffectiveHeader(field));
        }
        return expectedHeaders;
    }

    public ExcelTemplateFile dtoListToExcelFile(Class<?> dtoClass, List<?> dtoList,String requisitionTitle) {
        if(!dtoList.isEmpty() && !dtoClass.isAssignableFrom(dtoList.get(0).getClass())){
            throw new IllegalArgumentException("DTO class does not match the list element type.");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(dtoClass.getSimpleName());
            int startRow = 0;

            if(requisitionTitle !=null){
                startRow = createRequisitionAndPositionRow(workbook,sheet, requisitionTitle,startRow);
            }

            //to create headers
            int col = createHeaderRow(workbook,sheet,dtoClass,null,null,startRow++);


            //to write data to sheet
            writeDTOListDataToSheet(workbook,dtoClass,dtoList,sheet,null,startRow);

            //to resize columns
            for(int i=0;i<col;i++){
                try {
                    sheet.autoSizeColumn(i);
                } catch (Exception ignored) {
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            ExcelTemplateFile file = new ExcelTemplateFile();
            file.setFileName(dtoClass.getSimpleName() + DBConstants.EXCEL_TEMPLATE_SUFFIX);
            file.setFileContent(out.toByteArray());
            return file;
        } catch (Exception e) {
            throw new CommonException("Failed to generate Excel template");
        }

    }

    private void writeDTOListDataToSheet(Workbook workbook,Class<?> dtoClass, List<?> dtoList, Sheet sheet,Set<String> excludedFields,int startRow) throws IllegalAccessException {
        CellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0"));
        for (Object dto : dtoList) {
            Row row = sheet.createRow(startRow++);
            int colIndex = 0;
            for (Field field : dtoClass.getDeclaredFields()) {
                if (isExcludedField(field.getName())) {
                    continue;
                }
                if(excludedFields!=null && excludedFields.contains(field.getName())){
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(dto);
                Cell cell = row.createCell(colIndex++);
                if (value != null) {
                    if (value instanceof LocalDate) {
                        cell.setCellValue((LocalDate) value);
                    } else if (value instanceof Integer) {
                        cell.setCellValue((Integer) value);
                    } else if (value instanceof Float || value instanceof Double) {
                        // Set the numeric value
                        cell.setCellValue(((Number) value).doubleValue());
                        // Apply the 0.0 format style
                        cell.setCellStyle(decimalStyle);
                    }  else {
                        cell.setCellValue(value.toString());
                    }
                }
            }

        }

    }

    public int createRequisitionAndPositionRow(Workbook workbook,Sheet sheet, String requisitionTitle,int startRow) {
        Row requisitionRow = sheet.createRow(startRow++);
        Cell requisitionCellHeading =  requisitionRow.createCell(0);
        Cell requisitionCellValue = requisitionRow.createCell(1);
        requisitionCellHeading.setCellValue("Requisition Title");
        requisitionCellValue.setCellValue(requisitionTitle);

        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        requisitionCellHeading.setCellStyle(style);

        return startRow;
    }
    public byte[] writeJsonNodeToExcel(JsonNode resultSet, List<String> headers, List<String> displayHeaders) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Data");

            if (!resultSet.isArray() || resultSet.isEmpty()) {
                throw new ExcelValidationException(List.of("No details found"));
            }

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);


            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);


            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < displayHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(displayHeaders.get(i));
                cell.setCellStyle(headerStyle);
            }



            int rowNum = 1;

            for (JsonNode object : resultSet) {

                Row row = sheet.createRow(rowNum++);

                for (int i = 0; i < headers.size(); i++) {

                    JsonNode value = object.path(headers.get(i));

                    Cell cell = row.createCell(i);

                    if (value.isInt() || value.isLong()) {
                        cell.setCellValue(value.asLong());
                    } else if (value.isFloat() || value.isDouble() || value.isBigDecimal()) {
                        cell.setCellValue(value.asDouble());
                    } else if (value.isBoolean()) {
                        cell.setCellValue(value.asBoolean());
                    } else if (value.isNull() || value.isMissingNode()) {
                        cell.setBlank();
                    } else {
                        cell.setCellValue(value.asText());
                    }
                }
            }

            for (int i = 0; i < displayHeaders.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);


            return out.toByteArray();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}