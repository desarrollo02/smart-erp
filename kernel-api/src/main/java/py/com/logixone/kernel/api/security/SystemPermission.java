package py.com.logixone.kernel.api.security;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Stable permission owned by the kernel and never scoped to a company. */
public record SystemPermission(String value) implements Comparable<SystemPermission> {

    private static final int MAX_LENGTH = 128;
    private static final Pattern VALID_VALUE =
            Pattern.compile("[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*){2,}");

    public static final SystemPermission COMPANY_MANAGE =
            new SystemPermission("kernel.company.manage");
    public static final SystemPermission PLUGIN_MANAGE =
            new SystemPermission("kernel.plugin.manage");
    public static final SystemPermission SECURITY_MANAGE =
            new SystemPermission("kernel.security.manage");
    public static final SystemPermission AUDIT_VIEW =
            new SystemPermission("kernel.audit.view");
    public static final SystemPermission SYSTEM_ADMINISTRATION_MANAGE =
            new SystemPermission("kernel.system_administration.manage");

    private static final Set<SystemPermission> KNOWN_PERMISSIONS = Set.of(
            COMPANY_MANAGE,
            PLUGIN_MANAGE,
            SECURITY_MANAGE,
            AUDIT_VIEW,
            SYSTEM_ADMINISTRATION_MANAGE);

    public SystemPermission {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH || !VALID_VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid system permission: " + value);
        }
    }

    public static Set<SystemPermission> knownPermissions() {
        return KNOWN_PERMISSIONS;
    }

    @Override
    public int compareTo(SystemPermission other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
