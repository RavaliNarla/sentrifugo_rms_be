package com.sentrifugo.rms.recruiterportal.service;

import com.sentrifugo.rms.common.exception.CommonException;
import com.sentrifugo.rms.common.exception.ResourceNotFoundException;
import com.sentrifugo.rms.db.entity.JobPositionEntity;
import com.sentrifugo.rms.db.repository.JobPositionRepository;
import com.sentrifugo.rms.recruiterportal.dto.CandidateBulkImportResult;
import com.sentrifugo.rms.recruiterportal.dto.CandidateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Bulk candidate import via XLSX (Name / Phone / Email only — no resume/photo/ID proof).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateBulkImportService {

    private static final String[] HEADERS = {"Name", "Phone", "Email"};
    private static final int MAX_DATA_ROWS = 500;
    private static final DataFormatter FORMATTER = new DataFormatter();

    private final JobPositionRepository jobPositionRepository;
    private final CandidateService candidateService;

    public byte[] buildTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Candidates");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                sheet.setColumnWidth(i, 22 * 256);
            }

            // Soft Excel validations for data rows (server still validates on import).
            DataValidationHelper helper = sheet.getDataValidationHelper();
            addTextLengthValidation(helper, sheet, 0, 1, 200, "Enter candidate full name (1-200 characters).");
            addTextLengthValidation(helper, sheet, 1, 10, 10, "Enter a 10-digit phone number (digits only).");
            addTextLengthValidation(helper, sheet, 2, 5, 200, "Enter a valid email address.");

            // Sample blank rows so users see the editable area.
            for (int r = 1; r <= 10; r++) {
                sheet.createRow(r);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new CommonException("Failed to generate candidate import template: " + e.getMessage());
        }
    }

    public CandidateBulkImportResult importCandidates(UUID positionId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CommonException("Please upload an Excel file.");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new CommonException("Please upload an Excel file (.xlsx).");
        }

        JobPositionEntity position = jobPositionRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));

        List<String> errors = new ArrayList<>();
        int success = 0;
        int failure = 0;

        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new CommonException("Excel file has no sheets.");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null || !headersMatch(headerRow)) {
                throw new CommonException("Invalid template. Expected columns: Name, Phone, Email (in that order). Download the template and try again.");
            }

            int lastRow = Math.min(sheet.getLastRowNum(), MAX_DATA_ROWS);
            boolean anyData = false;
            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                anyData = true;
                String name = cellText(row.getCell(0)).trim();
                String phone = normalizePhone(cellText(row.getCell(1)));
                String email = cellText(row.getCell(2)).trim();

                try {
                    if (name.isBlank()) {
                        throw new CommonException("Name is required");
                    }
                    CandidateDTO dto = CandidateDTO.builder()
                            .requisitionId(position.getRequisitionId())
                            .positionId(position.getId())
                            .name(name)
                            .phone(phone)
                            .email(email)
                            .build();
                    candidateService.add(dto, null, null, null);
                    success++;
                } catch (Exception ex) {
                    failure++;
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                    errors.add("Row " + (r + 1) + ": " + msg);
                    if (errors.size() >= 25) {
                        errors.add("… further errors omitted");
                        break;
                    }
                }
            }

            if (!anyData) {
                throw new CommonException("No candidate rows found. Add at least one row under the header.");
            }
        } catch (CommonException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Candidate bulk import failed: {}", e.getMessage());
            throw new CommonException("Failed to read Excel file. Use the downloaded .xlsx template.");
        }

        return CandidateBulkImportResult.builder()
                .successCount(success)
                .failureCount(failure)
                .errors(errors)
                .build();
    }

    private static boolean headersMatch(Row headerRow) {
        for (int i = 0; i < HEADERS.length; i++) {
            String value = cellText(headerRow.getCell(i)).trim();
            if (!HEADERS[i].equalsIgnoreCase(value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlankRow(Row row) {
        for (int i = 0; i < HEADERS.length; i++) {
            if (!cellText(row.getCell(i)).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return FORMATTER.formatCellValue(cell);
    }

    private static String normalizePhone(String raw) {
        if (raw == null) {
            return "";
        }
        // Excel may format long numbers with decimals/spaces; keep digits only.
        String digits = raw.replaceAll("[^0-9]", "");
        // Handle values like 9876543210.0 from numeric cells already formatted.
        if (digits.length() > 10 && digits.endsWith("0") && raw.contains(".")) {
            // Already handled by formatter usually; leave as digits.
        }
        return digits;
    }

    private static void addTextLengthValidation(DataValidationHelper helper, Sheet sheet,
                                                int columnIndex, int min, int max, String prompt) {
        CellRangeAddressList range = new CellRangeAddressList(1, MAX_DATA_ROWS, columnIndex, columnIndex);
        DataValidationConstraint constraint = helper.createTextLengthConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                String.valueOf(min),
                String.valueOf(max));
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
        validation.createErrorBox("Invalid value", prompt);
        validation.createPromptBox("Hint", prompt);
        validation.setShowPromptBox(true);
        sheet.addValidationData(validation);
    }
}
