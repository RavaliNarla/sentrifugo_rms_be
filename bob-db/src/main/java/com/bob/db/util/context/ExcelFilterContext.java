package com.bob.db.util.context;

import java.util.HashMap;
import java.util.Map;

public class ExcelFilterContext {

    // ThreadLocal ensures data is isolated to the current request thread
    private static final ThreadLocal<Map<String, Object>> CONTEXT =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * Store a runtime value to be used by the @ExcelDropdown filter.
     * @param key The placeholder used in the annotation (e.g., "committeeId")
     * @param value The actual UUID or Object fetched from the DB
     */
    public static void set(String key, Object value) {
        CONTEXT.get().put(key, value);
    }

    /**
     * Retrieve a value for dynamic JPQL binding.
     */
    public static Object get(String key) {
        return CONTEXT.get().get(key);
    }

    /**
     * Critical: Prevents memory leaks and cross-request data contamination
     */
    public static void clear() {
        CONTEXT.remove();
    }
}