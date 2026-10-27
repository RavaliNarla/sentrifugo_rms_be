package com.sentrifugo.rms.db.audit;

public final class AuditHooks {

    private static volatile AuditHook hook;

    private AuditHooks() {
    }

    public static void register(AuditHook auditHook) {
        hook = auditHook;
    }

    public static AuditHook get() {
        return hook;
    }
}
