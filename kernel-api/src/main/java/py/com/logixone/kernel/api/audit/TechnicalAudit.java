package py.com.logixone.kernel.api.audit;

/** Kernel-owned append-only audit boundary available to plugin adapters. */
@FunctionalInterface
public interface TechnicalAudit {

    void record(TechnicalAuditEvent event);
}
