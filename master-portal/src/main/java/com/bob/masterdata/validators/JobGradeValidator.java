package com.bob.masterdata.validators;

import com.bob.db.dto.JobGradeDTO;
import com.bob.masterdata.Model.JobGradeExcelModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class JobGradeValidator {

    public List<String> collectValidationErrors(List<JobGradeExcelModel> dtos) {

        // 1. Map to accumulate errors per row
        Map<Integer, List<String>> errorMap = new LinkedHashMap<>();

        for (int i = 0; i < dtos.size(); i++) {
            JobGradeExcelModel dto = dtos.get(i);
            int rowNum = i + 2; // Excel header is Row 1, Data starts Row 2

            BigDecimal minSalary= new BigDecimal(dto.getMinSalary());
            BigDecimal maxSalary= new BigDecimal(dto.getMaxSalary());

            //  Salary Negative Validation
            if (minSalary != null && minSalary.compareTo(BigDecimal.ZERO) < 0) {
                addError(errorMap, rowNum, "Minimum salary cannot be negative");
            }

            if (maxSalary != null && maxSalary.compareTo(BigDecimal.ZERO) < 0) {
                addError(errorMap, rowNum, "Maximum salary cannot be negative");
            }

            if (minSalary != null && maxSalary != null && maxSalary.compareTo(BigDecimal.ZERO)!=0) {
                if (minSalary.compareTo(maxSalary) > 0) {
                    addError(errorMap, rowNum, "Minimum salary cannot be greater than Maximum salary");
                }
            }
        }

        // 4. Format errors to standard list (e.g., "Row 2: Error 1 | Error 2")
        return formatErrors(errorMap);
    }

    /* =====================================================
       HELPERS
       ===================================================== */

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