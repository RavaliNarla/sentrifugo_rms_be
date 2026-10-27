package com.sentrifugo.rms.db.audit;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * JPA lifecycle hooks for every {@link com.sentrifugo.rms.db.entity.BaseEntity}.
 * Forwards to the Spring {@link AuditHook} registered by rms-common.
 */
public class AuditEntityListener {

    private static final Map<Object, Map<String, Object>> LOADED =
            Collections.synchronizedMap(new WeakHashMap<>());

    @PostLoad
    public void onLoad(Object entity) {
        LOADED.put(entity, AuditSnapshot.capture(entity));
    }

    @PostPersist
    public void onCreate(Object entity) {
        AuditHook hook = AuditHooks.get();
        if (hook == null) {
            return;
        }
        Map<String, Object> after = AuditSnapshot.capture(entity);
        hook.onCreate(entity, after);
        LOADED.put(entity, after);
    }

    @PreUpdate
    public void onUpdate(Object entity) {
        AuditHook hook = AuditHooks.get();
        if (hook == null) {
            return;
        }
        Map<String, Object> before = LOADED.get(entity);
        if (before == null) {
            before = Map.of();
        }
        Map<String, Object> after = AuditSnapshot.capture(entity);
        hook.onUpdate(entity, before, after);
        LOADED.put(entity, after);
    }

    @PreRemove
    public void onDelete(Object entity) {
        AuditHook hook = AuditHooks.get();
        if (hook == null) {
            return;
        }
        Map<String, Object> before = LOADED.getOrDefault(entity, AuditSnapshot.capture(entity));
        hook.onDelete(entity, before);
        LOADED.remove(entity);
    }
}
