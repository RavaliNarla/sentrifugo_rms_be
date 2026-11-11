package com.sentrifugo.rms.common.util;

import com.sentrifugo.rms.common.exception.CommonException;

import java.util.Locale;
import java.util.Set;

/** Department / location codes used in requisition numbers — kept as-is from HRMS (variable length). */
public final class MasterCodeUtil {

    public static final int MAX_CODE_LENGTH = 64;

    private MasterCodeUtil() {
    }

    public static String normalizeRequired(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new CommonException(label + " code is required.");
        }
        String code = raw.trim();
        if (code.length() > MAX_CODE_LENGTH) {
            throw new CommonException(label + " code must be at most " + MAX_CODE_LENGTH + " characters.");
        }
        if (code.replaceAll("[A-Za-z0-9_-]", "").length() > 0) {
            // Allow letters, digits, underscore, hyphen; strip nothing — reject odd symbols only.
            String cleaned = code.replaceAll("[^A-Za-z0-9_-]", "");
            if (cleaned.isBlank()) {
                throw new CommonException(label + " code must contain letters or digits.");
            }
            code = cleaned;
        }
        return code;
    }

    /** Fallback when HRMS has no code: unique letters from name (not forced to 3 chars). */
    public static String suggestFromName(String name, Set<String> used) {
        String letters = name == null ? "" : name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        String base;
        if (letters.length() >= 3) {
            base = letters.substring(0, Math.min(8, letters.length()));
        } else if (letters.isEmpty()) {
            base = "XXX";
        } else {
            base = (letters + "XXX").substring(0, 3);
        }
        String key = base.toUpperCase(Locale.ROOT);
        if (!used.contains(key)) {
            return base;
        }
        for (int i = 2; i < 10000; i++) {
            String candidate = base + i;
            if (candidate.length() > MAX_CODE_LENGTH) {
                candidate = candidate.substring(0, MAX_CODE_LENGTH);
            }
            if (!used.contains(candidate.toUpperCase(Locale.ROOT))) {
                return candidate;
            }
        }
        throw new CommonException("Unable to generate a unique code.");
    }
}
