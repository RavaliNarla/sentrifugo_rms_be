package com.bob.candidateportal.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CustomLocalDateDeserializer extends JsonDeserializer<LocalDate> {
    private static final String[] PATTERNS = {
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "MM-dd-yyyy",
            "MM/dd/yyyy"
    };

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        JsonNode node = p.getCodec().readTree(p);
        String dateStr = node.asText();

        if (dateStr == null || dateStr.isBlank())
            return null;

        // Ignore AI placeholders
        if (dateStr.equalsIgnoreCase("Present") ||
                dateStr.equalsIgnoreCase("N/A") ||
                dateStr.equalsIgnoreCase("Unknown") ||
                dateStr.contains("YYYY"))
            return null;

        // Try multiple date formats
        for (String pattern : PATTERNS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {}
        }

        return null; // If all fail, return null safely
    }
}
