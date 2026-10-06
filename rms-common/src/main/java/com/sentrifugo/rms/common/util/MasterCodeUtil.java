package com.sentrifugo.rms.common.util;

import com.sentrifugo.rms.common.exception.CommonException;

import java.util.Locale;
import java.util.Set;

/** 3-character uppercase codes for department / location (used in requisition codes). */
public final class MasterCodeUtil {

    private MasterCodeUtil() {
    }

    public static String normalizeRequired(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new CommonException(label + " code is required.");
        }
        String code = raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (!code.matches("^[A-Z0-9]{3}$")) {
            throw new CommonException(label + " code must be exactly 3 letters/digits (A–Z, 0–9).");
        }
        return code;
    }

    /** Derive a unique 3-char code from a display name, avoiding {@code used} (uppercase). */
    public static String suggestFromName(String name, Set<String> used) {
        String letters = name == null ? "" : name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        String base;
        if (letters.length() >= 3) {
            base = letters.substring(0, 3);
        } else if (letters.isEmpty()) {
            base = "XXX";
        } else {
            base = (letters + "XXX").substring(0, 3);
        }
        if (!used.contains(base)) {
            return base;
        }
        // Vary 3rd char, then 2nd+3rd via digit suffix.
        char a = base.charAt(0);
        char b = base.charAt(1);
        for (char c = 'A'; c <= 'Z'; c++) {
            String candidate = "" + a + b + c;
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        for (int i = 0; i < 1000; i++) {
            String candidate = String.format("%c%02d", a, i);
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        throw new CommonException("Unable to generate a unique 3-character code.");
    }
}
