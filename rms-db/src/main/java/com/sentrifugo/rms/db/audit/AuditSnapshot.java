package com.sentrifugo.rms.db.audit;

import com.sentrifugo.rms.db.entity.BaseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Reflective field capture for audit diffs. Skips secrets, collections, and associations. */
public final class AuditSnapshot {

    private static final Set<String> REDACT = Set.of(
            "passwordHash", "password", "resetOtp", "resetOtpExpiresAt"
    );

    private static final Set<String> SKIP = Set.of(
            "hibernateLazyInitializer", "handler", "$jacocoData"
    );

    private static final Set<String> IGNORE_IN_DIFF = Set.of(
            "id", "modifiedDate", "modifiedBy", "createdDate", "createdBy"
    );

    private AuditSnapshot() {
    }

    public static Map<String, Object> capture(Object entity) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (entity == null) {
            return out;
        }
        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                String name = field.getName();
                if (SKIP.contains(name) || name.startsWith("$$")) {
                    continue;
                }
                if (REDACT.contains(name)) {
                    out.put(name, "[redacted]");
                    continue;
                }
                Class<?> ft = field.getType();
                if (Collection.class.isAssignableFrom(ft) || Map.class.isAssignableFrom(ft)) {
                    continue;
                }
                if (BaseEntity.class.isAssignableFrom(ft)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    out.put(name, stringify(field.get(entity)));
                } catch (Exception ignored) {
                    // best-effort
                }
            }
            type = type.getSuperclass();
        }
        return out;
    }

    public static String entityTypeName(Object entity) {
        if (entity == null) {
            return "Unknown";
        }
        String name = entity.getClass().getSimpleName();
        int proxy = name.indexOf('$');
        if (proxy > 0) {
            name = name.substring(0, proxy);
        }
        if (name.endsWith("Entity")) {
            name = name.substring(0, name.length() - "Entity".length());
        }
        return name;
    }

    public static UUID entityId(Object entity) {
        if (entity instanceof BaseEntity<?> base && base.getId() instanceof UUID id) {
            return id;
        }
        try {
            Field id = findField(entity.getClass(), "id");
            if (id != null) {
                id.setAccessible(true);
                Object v = id.get(entity);
                if (v instanceof UUID u) {
                    return u;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static List<Map<String, Object>> diff(Map<String, Object> before, Map<String, Object> after) {
        List<Map<String, Object>> changes = new java.util.ArrayList<>();
        if (before == null) {
            before = Map.of();
        }
        if (after == null) {
            after = Map.of();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.addAll(before.keySet());
        keys.addAll(after.keySet());
        for (String key : keys) {
            if (IGNORE_IN_DIFF.contains(key)) {
                continue;
            }
            Object b = before.get(key);
            Object a = after.get(key);
            if (!Objects.equals(asText(b), asText(a))) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("field", key);
                row.put("from", asText(b));
                row.put("to", asText(a));
                changes.add(row);
            }
        }
        return changes;
    }

    /** CREATE: one entry per non-ignored field present on the new row. */
    public static List<Map<String, Object>> fieldsForCreate(Map<String, Object> after) {
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        if (after == null) {
            return rows;
        }
        for (Map.Entry<String, Object> e : after.entrySet()) {
            if (IGNORE_IN_DIFF.contains(e.getKey())) {
                continue;
            }
            String to = asText(e.getValue());
            if (to == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("field", e.getKey());
            row.put("from", null);
            row.put("to", to);
            rows.add(row);
        }
        return rows;
    }

    /** DELETE: one entry per non-ignored field present on the old row. */
    public static List<Map<String, Object>> fieldsForDelete(Map<String, Object> before) {
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        if (before == null) {
            return rows;
        }
        for (Map.Entry<String, Object> e : before.entrySet()) {
            if (IGNORE_IN_DIFF.contains(e.getKey())) {
                continue;
            }
            String from = asText(e.getValue());
            if (from == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("field", e.getKey());
            row.put("from", from);
            row.put("to", null);
            rows.add(row);
        }
        return rows;
    }

    public static String asText(Object value) {
        Object s = stringify(value);
        return s == null ? null : String.valueOf(s);
    }

    private static Object stringify(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        if (value instanceof UUID || value instanceof Number || value instanceof Boolean || value instanceof CharSequence) {
            return value instanceof CharSequence ? value.toString() : value;
        }
        if (value instanceof Temporal || value instanceof java.util.Date) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> c = type;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}
