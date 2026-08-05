package py.com.logixone.web.admin;

import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.kernel.domain.security.RoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

/** Strict parsing of browser-supplied technical candidates before application use. */
final class AdminTechnicalInput {

    private AdminTechnicalInput() {
    }

    static CompanyId companyId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("company id is required");
        }
        return CompanyId.parse(value);
    }

    static PluginId pluginId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("plugin id is required");
        }
        return new PluginId(value);
    }

    static AppUserId userId(String value) {
        return AppUserId.parse(required(value, "user id"));
    }

    static RoleId roleId(String value) {
        return RoleId.parse(required(value, "role id"));
    }

    static RoleCode roleCode(String value) {
        return new RoleCode(required(value, "role code"));
    }

    static ContributionId permissionId(String value) {
        return new ContributionId(required(value, "permission id"));
    }

    static SystemRoleId systemRoleId(String value) {
        return SystemRoleId.parse(required(value, "system role id"));
    }

    static SystemRoleCode systemRoleCode(String value) {
        return new SystemRoleCode(required(value, "system role code"));
    }

    static SystemPermission systemPermission(String value) {
        SystemPermission permission = new SystemPermission(required(value, "system permission"));
        if (!SystemPermission.knownPermissions().contains(permission)) {
            throw new IllegalArgumentException("unknown system permission");
        }
        return permission;
    }

    static String requiredText(String value, String name) {
        return required(value, name);
    }

    static long version(String value) {
        String candidate = required(value, "version");
        try {
            long parsed = Long.parseLong(candidate);
            if (parsed < 0) {
                throw new IllegalArgumentException("version must not be negative");
            }
            return parsed;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("version must be a non-negative integer", invalid);
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
