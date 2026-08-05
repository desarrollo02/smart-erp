package py.com.logixone.kernel.domain.security;

/** Stable fail-closed diagnostics; public adapters may map several values to one generic denial. */
public enum SecurityDiagnosticCode {
    USER_NOT_REGISTERED,
    USER_INACTIVE,
    MEMBERSHIP_REQUIRED,
    MEMBERSHIP_INACTIVE,
    MEMBERSHIP_CONTEXT_INVALID,
    COMPANY_SELECTION_REQUIRED,
    COMPANY_ACCESS_DENIED,
    ROLE_CONTEXT_INVALID
}
