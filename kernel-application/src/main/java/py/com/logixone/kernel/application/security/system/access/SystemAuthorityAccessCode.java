package py.com.logixone.kernel.application.security.system.access;

/** Internal diagnostics. The web boundary must reduce all values to a generic denial. */
public enum SystemAuthorityAccessCode {
    IDENTITY_NOT_FOUND,
    USER_INACTIVE,
    PERMISSION_DENIED,
    PERMISSION_UNKNOWN,
    CONTEXT_INVALID
}
