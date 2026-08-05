package py.com.logixone.kernel.application.security.command;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.domain.security.ExternalIdentity;

public record RegisterAppUserCommand(
        ExternalIdentity externalIdentity,
        Optional<String> displayName) {

    public RegisterAppUserCommand {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        displayName = Objects.requireNonNull(displayName, "displayName");
    }
}
