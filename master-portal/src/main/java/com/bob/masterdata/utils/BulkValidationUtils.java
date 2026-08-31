package com.bob.masterdata.utils;


import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BulkValidationUtils {

    private BulkValidationUtils() {
        // Private constructor to prevent instantiation
    }

    public static <T> List<String> collectDuplicateErrors(
            List<T> dtos,
            Function<T, String> valueExtractor,
            EntityManager entityManager,
            Class<?> entityClass,
            String entityField,
            String entityName,
            String columnLabel
    ) {
        // 1. Internal Map to accumulate errors (Row Number -> List of Errors)
        Map<Integer, List<String>> errorMap = new LinkedHashMap<>();

        // 2. Validate internal Excel duplicates
        validateDuplicatesInFile(dtos, valueExtractor, columnLabel, errorMap);


        // 3. Validate Database duplicates
        validateAgainstDatabase(dtos, valueExtractor, entityManager, entityClass, entityField, columnLabel, errorMap);

        // 4. Format the output into a clean List<String>
        List<String> resultErrors = new ArrayList<>();

        if (!errorMap.isEmpty()) {
            // Sort keys so the user sees errors in order: Row 2, Row 3, Row 4...
            List<Integer> sortedRows = new ArrayList<>(errorMap.keySet());
            Collections.sort(sortedRows);

            for (Integer rowNum : sortedRows) {
                // Combine multiple errors for the same row with a separator
                String combinedMessage = String.join(" | ", errorMap.get(rowNum));
                resultErrors.add("Row " + rowNum + ": " + combinedMessage);
            }
        }

        return resultErrors;
    }

    /* =========================================================================
       PRIVATE HELPER METHODS
       ========================================================================= */

    private static <T> void validateDuplicatesInFile(
            List<T> dtos,
            Function<T, String> extractor,
            String columnLabel,
            Map<Integer, List<String>> errorMap
    ) {
        Set<String> seenValues = new HashSet<>();

        for (int i = 0; i < dtos.size(); i++) {
            T dto = dtos.get(i);
            int rowNum = i + 2; // Excel Header is Row 1, Data starts at Row 2

            String value = extractor.apply(dto);

            // 1. Check for Empty Values
            if (value == null || value.isBlank()) {
                addError(errorMap, rowNum, columnLabel + " cannot be empty");
                continue;
            }

            // 2. Check for Duplicates inside the file
            String normalized = value.trim().toLowerCase();
            if (!seenValues.add(normalized)) {
                // Example: "Duplicate Department Name 'Data-1' found in Excel file"
                addError(errorMap, rowNum, "Duplicate " + columnLabel + " '" + value + "' found in Excel file");
            }
        }
    }

    private static <T> void validateAgainstDatabase(
            List<T> dtos,
            Function<T, String> extractor,
            EntityManager entityManager,
            Class<?> entityClass,
            String entityField,
            String columnLabel,
            Map<Integer, List<String>> errorMap
    ) {
        // 1. Collect all non-empty values to check in one batch
        Set<String> valuesToCheck = dtos.stream()
                .map(extractor)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase())
                .collect(Collectors.toSet());

        if (valuesToCheck.isEmpty()) {
            return;
        }

        // 2. Batch Query to find existing items
        // SELECT LOWER(e.name) FROM Entity e WHERE LOWER(e.name) IN :values
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<?> root = query.from(entityClass);
        Expression<String> fieldExpr = cb.lower(root.get(entityField));
        query.select(fieldExpr)
                .where(
                        cb.and(
                                fieldExpr.in(valuesToCheck),
                                cb.isTrue(root.get("isActive"))
                        )
                );

        List<String> existingList = entityManager.createQuery(query).getResultList();
        Set<String> existingSet = new HashSet<>(existingList);

        // 3. Map database matches back to specific Excel rows
        for (int i = 0; i < dtos.size(); i++) {
            T dto = dtos.get(i);
            String val = extractor.apply(dto);

            if (val != null && existingSet.contains(val.trim().toLowerCase())) {
                // Example: "Department Name 'IT' already exists in database"
                addError(errorMap, i + 2, columnLabel + " '" + val + "' already exists in database");
            }
        }
    }

    private static void addError(Map<Integer, List<String>> map, int row, String msg) {
        map.computeIfAbsent(row, k -> new ArrayList<>()).add(msg);
    }
}