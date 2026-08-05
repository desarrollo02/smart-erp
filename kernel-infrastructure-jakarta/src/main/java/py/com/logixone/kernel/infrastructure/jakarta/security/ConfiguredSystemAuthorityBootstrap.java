package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.SecurityOperationStatus;
import py.com.logixone.kernel.application.security.system.SystemAuthorityBootstrapState;
import py.com.logixone.kernel.application.security.system.command.BootstrapSystemAuthorityCommand;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityBootstrapPort;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;

/** Closed opt-in declaration for the first kernel-wide administrator. */
@ApplicationScoped
public class ConfiguredSystemAuthorityBootstrap {

    private static final Logger LOGGER =
            System.getLogger(ConfiguredSystemAuthorityBootstrap.class.getName());
    private static final String ENABLED = "LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_ENABLED";

    @Inject
    Instance<SystemAuthorityBootstrapPort> bootstrapPorts;

    void initialize(
            @Observes @Priority(3100) @Initialized(ApplicationScoped.class)
            Object initializationEvent) {
        Objects.requireNonNull(initializationEvent, "initializationEvent");

        BootstrapDeclaration declaration;
        try {
            declaration = BootstrapDeclaration.from(System.getenv());
        } catch (RuntimeException failure) {
            LOGGER.log(
                    Level.ERROR,
                    "event=system_authority_bootstrap_failed type=InvalidConfiguration");
            throw new IllegalStateException(
                    "System authority bootstrap configuration is invalid", failure);
        }

        if (!declaration.enabled()) {
            LOGGER.log(
                    Level.INFO,
                    "event=system_authority_bootstrap_skipped reason=Disabled");
            return;
        }
        if (bootstrapPorts.isUnsatisfied() || bootstrapPorts.isAmbiguous()) {
            LOGGER.log(
                    Level.ERROR,
                    "event=system_authority_bootstrap_failed type=AdapterUnavailable");
            throw new IllegalStateException(
                    "System authority bootstrap adapter is unavailable");
        }

        SecurityOperationResult<SystemAuthorityBootstrapState> result;
        try {
            result = bootstrapPorts.get().bootstrap(declaration.command());
        } catch (RuntimeException failure) {
            LOGGER.log(
                    Level.ERROR,
                    "event=system_authority_bootstrap_failed type="
                            + failure.getClass().getSimpleName());
            throw new IllegalStateException("System authority bootstrap failed", failure);
        }

        if (result.status() == SecurityOperationStatus.REJECTED) {
            String code = result.failure().orElseThrow().name();
            LOGGER.log(
                    Level.ERROR,
                    "event=system_authority_bootstrap_rejected code=" + code);
            throw new IllegalStateException(
                    "System authority bootstrap was rejected: " + code);
        }

        SystemAuthorityBootstrapState state = result.value().orElseThrow();
        LOGGER.log(Level.INFO, () -> String.join(" ",
                "event=system_authority_bootstrap_completed",
                "status=" + result.status(),
                "subject_user_id=" + state.user().id(),
                "system_role_id=" + state.role().id(),
                "permission_count=" + state.grants().size()));
    }

    record BootstrapDeclaration(
            boolean enabled,
            BootstrapSystemAuthorityCommand command) {

        BootstrapDeclaration {
            if (enabled) {
                Objects.requireNonNull(command, "command");
            } else if (command != null) {
                throw new IllegalArgumentException(
                        "a disabled bootstrap cannot contain a command");
            }
        }

        static BootstrapDeclaration from(Map<String, String> environment) {
            Objects.requireNonNull(environment, "environment");
            String configured = environment.getOrDefault(ENABLED, "false");
            if ("false".equals(configured)) {
                return new BootstrapDeclaration(false, null);
            }
            if (!"true".equals(configured)) {
                throw new IllegalArgumentException(ENABLED + " must be true or false");
            }

            String displayName = environment.get(
                    "LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_DISPLAY_NAME");
            Optional<String> optionalDisplayName = displayName == null || displayName.isBlank()
                    ? Optional.empty()
                    : Optional.of(displayName);

            BootstrapSystemAuthorityCommand command = new BootstrapSystemAuthorityCommand(
                    new ExternalIdentity(
                            required(environment, "LOGIXONE_OIDC_PROVIDER_URL"),
                            required(environment, "LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_SUBJECT")),
                    optionalDisplayName,
                    new SystemRoleCode(required(
                            environment, "LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_ROLE_CODE")),
                    required(environment, "LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_ROLE_NAME"),
                    permissions(required(
                            environment, "LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_PERMISSIONS")));
            return new BootstrapDeclaration(true, command);
        }

        private static String required(Map<String, String> environment, String name) {
            String value = environment.get(name);
            if (value == null || value.isBlank() || !value.equals(value.strip())) {
                throw new IllegalArgumentException(
                        name + " must contain an exact non-blank value");
            }
            return value;
        }

        private static Set<SystemPermission> permissions(String value) {
            String[] entries = value.split(",", -1);
            if (Arrays.stream(entries).anyMatch(String::isBlank)) {
                throw new IllegalArgumentException(
                        "LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_PERMISSIONS"
                                + " contains an empty permission");
            }
            Set<SystemPermission> result = new TreeSet<>();
            for (String entry : entries) {
                SystemPermission permission = new SystemPermission(entry.strip());
                if (!SystemPermission.knownPermissions().contains(permission)) {
                    throw new IllegalArgumentException(
                            "System authority bootstrap contains an unknown permission");
                }
                if (!result.add(permission)) {
                    throw new IllegalArgumentException(
                            "System authority bootstrap contains a duplicate permission");
                }
            }
            return result;
        }
    }
}
