package py.com.logixone.kernel.application.security.system.command;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;

/** Exact external declaration for the one-shot first kernel administrator. */
public record BootstrapSystemAuthorityCommand(
        ExternalIdentity externalIdentity,
        Optional<String> userDisplayName,
        SystemRoleCode roleCode,
        String roleDisplayName,
        Set<SystemPermission> permissions) {

    public BootstrapSystemAuthorityCommand {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        userDisplayName = Objects.requireNonNull(userDisplayName, "userDisplayName");
        userDisplayName.ifPresent(displayName -> {
            if (displayName.isBlank()
                    || displayName.length() > 160
                    || !displayName.equals(displayName.strip())
                    || displayName.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException(
                        "userDisplayName must be a valid presentation value");
            }
        });
        Objects.requireNonNull(roleCode, "roleCode");
        Objects.requireNonNull(roleDisplayName, "roleDisplayName");
        if (roleDisplayName.isBlank()
                || roleDisplayName.length() > 160
                || !roleDisplayName.equals(roleDisplayName.strip())
                || roleDisplayName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "roleDisplayName must be a valid presentation value");
        }
        permissions = Collections.unmodifiableSet(
                new TreeSet<>(Objects.requireNonNull(permissions, "permissions")));
        if (!permissions.contains(SystemPermission.SYSTEM_ADMINISTRATION_MANAGE)) {
            throw new IllegalArgumentException(
                    "system bootstrap requires kernel.system_administration.manage");
        }
    }
}
