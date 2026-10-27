package com.sentrifugo.rms.db.audit;

import java.util.UUID;

/**
 * Request/job scoped correlation id for audit rows.
 * Set by the HTTP filter in rms-common; readable from JPA listeners in rms-db.
 */
public final class AuditCorrelation {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private AuditCorrelation() {
    }

    public static void set(UUID correlationId) {
        CURRENT.set(correlationId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    /** Returns the current id, or creates one if a mutation runs outside an HTTP filter. */
    public static UUID getOrCreate() {
        UUID existing = CURRENT.get();
        if (existing != null) {
            return existing;
        }
        UUID created = UUID.randomUUID();
        CURRENT.set(created);
        return created;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
