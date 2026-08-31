package com.bob.jobportal.util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ExcelDuplicationUtil {

    @SafeVarargs
    public static <T> List<String> validateAll(
            List<T> dtos,
            Set<String> dbKeys,
            Function<T, String> dbKeyExtractor,
            Function<T, ?>... excelKeyExtractors
    ) {
        Map<Integer, List<String>> rowWiseErrors = new HashMap<>();

        //Excel validation
        Map<String, Integer> seen = new HashMap<>();

        for (int i = 0; i < dtos.size(); i++) {
            T dto = dtos.get(i);
            int row = i + 2;

            List<String> values = new ArrayList<>();
            for (Function<T, ?> ex : excelKeyExtractors) {
                Object v = ex.apply(dto);
                values.add(v == null ? "" : v.toString());
            }

            String key = String.join("||", values).toLowerCase();

            if (seen.containsKey(key)) {
                rowWiseErrors
                        .computeIfAbsent(row, k -> new ArrayList<>())
                        .add("Duplicate in Excel for selected Master position and Department");
            } else {
                seen.put(key, row);
            }
        }

        //DB validation
        IntStream.range(0, dtos.size()).forEach(i -> {
            String key = dbKeyExtractor.apply(dtos.get(i));
            if (dbKeys.contains(key)) {
                int row = i + 2;
                rowWiseErrors
                        .computeIfAbsent(row, k -> new ArrayList<>())
                        .add("Duplicate in DB for selected Master position and Department");
            }
        });

        //Merge the errors
        List<String> finalErrors = new ArrayList<>();
        rowWiseErrors.forEach((row, errs) -> {
            finalErrors.add("Row " + row + ": " + String.join(", ", errs));
        });

        return finalErrors;
    }

}
