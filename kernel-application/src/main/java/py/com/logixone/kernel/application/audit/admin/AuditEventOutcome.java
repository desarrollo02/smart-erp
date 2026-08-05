package py.com.logixone.kernel.application.audit.admin;

/** Closed union of the stable outcomes used by kernel audit events. */
public enum AuditEventOutcome {
    CHANGED,
    UNCHANGED,
    REJECTED,
    ALLOWED,
    DENIED,
    SELECTION_REQUIRED
}
