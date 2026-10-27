package com.sentrifugo.rms.db.audit;

import java.util.Map;

/**
 * Bridge from JPA lifecycle events (rms-db) to the Spring {@code AuditService} (rms-common).
 */
public interface AuditHook {

    void onCreate(Object entity, Map<String, Object> after);

    void onUpdate(Object entity, Map<String, Object> before, Map<String, Object> after);

    void onDelete(Object entity, Map<String, Object> before);
}
