package com.bob.masterdata.validators;

import com.bob.masterdata.Model.MasterPositionExcelModel;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MasterPositionValidator {

    // Database safety limits
    private static final int MAX_DESC_LENGTH = 2000;

    public List<String> collectValidationErrorsFromExcel(List<MasterPositionExcelModel> models) {

        // 1. Map to accumulate errors per row
        Map<Integer, List<String>> errorMap = new LinkedHashMap<>();

        for (int i = 0; i < models.size(); i++) {
            MasterPositionExcelModel m = models.get(i);
            int rowNum = i + 2; // Excel data starts at Row 2

            // Note: Position Name uniqueness is handled by BulkValidationUtils in the Service

            // --- A. Mandatory Dropdowns ---
//            if (m.getDeptId() == null) {
//                addError(errorMap, rowNum, "Department must be selected");
//            }

            if (m.getGradeId() == null) {
                addError(errorMap, rowNum, "Job grade must be selected");
            }

            // --- B. Mandatory Text Fields ---
            if (m.getMandatoryExperience() == null || m.getMandatoryExperience().isBlank()) {
                addError(errorMap, rowNum, "Mandatory experience is required");
            }

//            if (m.getMandatoryEducation() == null || m.getMandatoryEducation().isBlank()) {
//                addError(errorMap, rowNum, "Mandatory education is required");
//            }

            // --- C. Age Logic ---
            Integer minAge = Integer.parseInt(m.getEligibilityAgeMin());
            Integer maxAge = Integer.parseInt(m.getEligibilityAgeMax());

            // 1. Negative Checks
            if (minAge != null && minAge < 0) {
                addError(errorMap, rowNum, "Minimum age cannot be negative");
            }
            if (maxAge != null && maxAge < 0) {
                addError(errorMap, rowNum, "Maximum age cannot be negative");
            }

            // 2. Range Check (Min cannot be > Max)
            if (minAge != null && maxAge != null && minAge >= 0 && maxAge >= 0) {
                if (minAge > maxAge) {
                    addError(errorMap, rowNum, "Minimum age cannot be greater than Maximum age");
                }
            }

            // --- D. Length Safety Check ---
            if (m.getRolesResponsibilities() != null && m.getRolesResponsibilities().length() > MAX_DESC_LENGTH) {
                addError(errorMap, rowNum, "Roles & Responsibilities exceeds maximum length (" + MAX_DESC_LENGTH + " chars)");
            }
        }

        // 2. Format errors to standard list
        return formatErrors(errorMap);
    }
    

    private void addError(Map<Integer, List<String>> map, int row, String msg) {
        map.computeIfAbsent(row, k -> new ArrayList<>()).add(msg);
    }

    private List<String> formatErrors(Map<Integer, List<String>> errorMap) {
        List<String> resultErrors = new ArrayList<>();

        if (!errorMap.isEmpty()) {
            List<Integer> sortedRows = new ArrayList<>(errorMap.keySet());
            Collections.sort(sortedRows);

            for (Integer rowNum : sortedRows) {
                String combinedMessage = String.join(" | ", errorMap.get(rowNum));
                resultErrors.add("Row " + rowNum + ": " + combinedMessage);
            }
        }
        return resultErrors;
    }
}