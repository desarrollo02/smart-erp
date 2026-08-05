package py.com.logixone.kernel.domain.security.system;

/** Result of validating a complete desired kernel-wide authority snapshot. */
public enum SystemAuthoritySafetyStatus {
    SAFE,
    INVALID_CONTEXT,
    ADMINISTRATOR_REQUIRED;

    public boolean safe() {
        return this == SAFE;
    }
}
