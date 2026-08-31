//package com.bob.masterdata.validators;
//
//import com.bob.db.dto.LocationDTO;
//import com.bob.db.util.excel.RegexValidate;
//import com.bob.commonutil.exception.ExcelValidationException;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.TypedQuery;
//import org.springframework.stereotype.Component;
//
//import java.lang.reflect.Field;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Component
//public class LocationValidator {
//
//    private final EntityManager entityManager;
//
//    public LocationValidator(EntityManager entityManager) {
//        this.entityManager = entityManager;
//    }
//
//
//    public List<String> collectValidationErrors(List<LocationDTO> dtos) {
//
//        if(dtos.isEmpty()){
//            throw new ExcelValidationException(List.of("Excel file is empty. Please upload a file with data."));
//        }
//
//        Map<Integer, List<String>> errorMap = new LinkedHashMap<>();
//
//        Map<UUID, String> cityNameMap = loadCityNames(dtos);
//
//        Set<String> seenExcelKeys = new HashSet<>();
//
//        Set<String> existingDbKeys = loadExistingDbKeys();
//
//        for (int i = 0; i < dtos.size(); i++) {
//            LocationDTO dto = dtos.get(i);
//            int rowNum = i + 2;
//            validateRegexForDto(dto, rowNum, errorMap);
//
//            String locationName = dto.getLocationName();
//            UUID cityId = dto.getCityId();
//
//            if (locationName == null || locationName.isBlank()) {
//                addError(errorMap, rowNum, "Location Name is required");
//                continue;
//            }
//
//            if (cityId == null) {
//                addError(errorMap, rowNum, "City must be selected");
//                continue;
//            }
//
//            String normalizedName = normalize(locationName);
//
//            String compositeKey = normalizedName + "|" + cityId;
//
//            String cityName = cityNameMap.getOrDefault(cityId, "Unknown City");
//            String readableLabel = locationName + " (" + cityName + ")";
//
//            if (!seenExcelKeys.add(compositeKey)) {
//                addError(errorMap, rowNum, "Duplicate Location '" + readableLabel + "' found in Excel file");
//            }
//
//            if (existingDbKeys.contains(compositeKey)) {
//                addError(errorMap, rowNum, "Location '" + readableLabel + "' already exists in database");
//            }
//        }
//
//        return formatErrors(errorMap);
//    }
//
//
//    private Set<String> loadExistingDbKeys() {
//
//        String jpql = "SELECT LOWER(l.locationName), l.cityId FROM LocationEntity l WHERE l.isActive = true";
//
//        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
//        List<Object[]> results = query.getResultList();
//
//        Set<String> dbKeys = new HashSet<>();
//        for (Object[] row : results) {
//            String name = (String) row[0];
//            UUID cityId = (UUID) row[1];
//
//            if (name != null && cityId != null) {
//                dbKeys.add(name.trim() + "|" + cityId);
//            }
//        }
//        return dbKeys;
//    }
//
//    private Map<UUID, String> loadCityNames(List<LocationDTO> dtos) {
//        Set<UUID> cityIds = dtos.stream()
//                .map(LocationDTO::getCityId)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//
//        if (cityIds.isEmpty()) return Collections.emptyMap();
//
//        return entityManager.createQuery(
//                        "SELECT c.id, c.cityName FROM CityEntity c WHERE c.id IN :ids", Object[].class)
//                .setParameter("ids", cityIds)
//                .getResultList()
//                .stream()
//                .collect(Collectors.toMap(
//                        row -> (UUID) row[0],
//                        row -> (String) row[1]
//                ));
//    }
//
//
//    private void addError(Map<Integer, List<String>> map, int row, String msg) {
//        map.computeIfAbsent(row, k -> new ArrayList<>()).add(msg);
//    }
//
//    private String normalize(String value) {
//        return value == null ? "" : value.trim().toLowerCase();
//    }
//
//    private List<String> formatErrors(Map<Integer, List<String>> errorMap) {
//        List<String> resultErrors = new ArrayList<>();
//
//        if (!errorMap.isEmpty()) {
//            List<Integer> sortedRows = new ArrayList<>(errorMap.keySet());
//            Collections.sort(sortedRows);
//
//            for (Integer rowNum : sortedRows) {
//                // Combine multiple errors for the same row with " | "
//                String combinedMessage = String.join(" | ", errorMap.get(rowNum));
//                resultErrors.add("Row " + rowNum + ": " + combinedMessage);
//            }
//        }
//        return resultErrors;
//    }
//
//    private void validateRegexForDto(Object dto, int rowNum, Map<Integer, List<String>> errorMap) {
//        Field[] fields = dto.getClass().getDeclaredFields();
//        for (Field field : fields) {
//            if (field.isAnnotationPresent(RegexValidate.class)) {
//                RegexValidate annotation = field.getAnnotation(RegexValidate.class);
//                field.setAccessible(true);
//                try {
//                    Object value = field.get(dto);
//                    if (value != null && !value.toString().isBlank()) {
//                        String stringValue = value.toString();
//                        // Access the pattern from your Enum
//                        String pattern = annotation.regex().getRegex();
//
//                        if (!stringValue.matches(pattern)) {
//                            addError(errorMap, rowNum, annotation.message());
//                        }
//                    }
//                } catch (IllegalAccessException e) {
//                    // Fail silently or log if field access is restricted
//                }
//            }
//        }
//    }
//}