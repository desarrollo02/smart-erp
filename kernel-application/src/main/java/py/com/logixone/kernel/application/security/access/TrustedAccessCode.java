package py.com.logixone.kernel.application.security.access;

/** Stable internal access diagnostic. Web adapters must reduce denials to 401/403. */
public enum TrustedAccessCode {
    USER_ACCESS_DENIED,
    SESSION_ACTOR_MISMATCH,
    COMPANY_ACCESS_DENIED,
    COMPANY_NOT_OPERATIONAL,
    COMPANY_SELECTION_REQUIRED,
    PLUGIN_ACCESS_DENIED,
    PERMISSION_ACCESS_DENIED,
    SCREEN_ACCESS_DENIED,
    SCREEN_COMPOSITION_INVALID
}
