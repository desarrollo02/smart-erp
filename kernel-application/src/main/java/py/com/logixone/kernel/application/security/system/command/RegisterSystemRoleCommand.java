package py.com.logixone.kernel.application.security.system.command;

import java.util.Objects;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;

public record RegisterSystemRoleCommand(
        SystemRoleCode roleCode,
        String displayName) {

    public RegisterSystemRoleCommand {
        Objects.requireNonNull(roleCode, "roleCode");
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()
                || displayName.length() > 160
                || !displayName.equals(displayName.strip())
                || displayName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("displayName must be a valid presentation value");
        }
    }
}
