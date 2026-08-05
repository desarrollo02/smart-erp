package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.SecurityOperationStatus;
import py.com.logixone.kernel.application.security.command.BootstrapSecurityCommand;
import py.com.logixone.kernel.application.security.port.SecurityBootstrapPort;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.RoleCode;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

/** Executes the closed, idempotent security bootstrap from external configuration. */
@ApplicationScoped
public class ConfiguredSecurityBootstrap {

    private static final Logger LOGGER = System.getLogger(ConfiguredSecurityBootstrap.class.getName());
    private static final String ENABLED = "LOGIXONE_SECURITY_BOOTSTRAP_ENABLED";

    @Inject
    SecurityBootstrapPort bootstrapPort;

    void initialize(@Observes @Initialized(ApplicationScoped.class) Object initializationEvent) {
        Objects.requireNonNull(initializationEvent, "initializationEvent");

        BootstrapDeclaration declaration;
        try {
            declaration = BootstrapDeclaration.from(System.getenv());
        } catch (RuntimeException failure) {
            LOGGER.log(Level.ERROR, "event=security_bootstrap_failed type=InvalidConfiguration");
            throw new IllegalStateException("Security bootstrap configuration is invalid", failure);
        }

        if (!declaration.enabled()) {
            LOGGER.log(Level.INFO, "event=security_bootstrap_skipped reason=Disabled");
            return;
        }

        SecurityOperationResult<?> result;
        try {
            result = bootstrapPort.bootstrap(declaration.command());
        } catch (RuntimeException failure) {
            LOGGER.log(
                    Level.ERROR,
                    "event=security_bootstrap_failed type=" + failure.getClass().getSimpleName());
            throw new IllegalStateException("Security bootstrap failed", failure);
        }

        if (result.status() == SecurityOperationStatus.REJECTED) {
            String code = result.failure().orElseThrow().name();
            LOGGER.log(Level.ERROR, "event=security_bootstrap_rejected code=" + code);
            throw new IllegalStateException("Security bootstrap was rejected: " + code);
        }

        BootstrapSecurityCommand command = declaration.command();
        LOGGER.log(
                Level.INFO,
                "event=security_bootstrap_completed status=" + result.status()
                        + " company_id=" + command.companyId()
                        + " role_code=" + command.roleCode()
                        + " permission_count=" + command.permissionIds().size());
    }

    private record BootstrapDeclaration(boolean enabled, BootstrapSecurityCommand command) {

        private BootstrapDeclaration {
            if (enabled) {
                Objects.requireNonNull(command, "command");
            } else if (command != null) {
                throw new IllegalArgumentException("a disabled bootstrap cannot contain a command");
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

            String displayName = environment.get("LOGIXONE_SECURITY_BOOTSTRAP_DISPLAY_NAME");
            Optional<String> optionalDisplayName = displayName == null || displayName.isBlank()
                    ? Optional.empty()
                    : Optional.of(displayName);

            BootstrapSecurityCommand command = new BootstrapSecurityCommand(
                    new ExternalIdentity(
                            required(environment, "LOGIXONE_OIDC_PROVIDER_URL"),
                            required(environment, "LOGIXONE_SECURITY_BOOTSTRAP_SUBJECT")),
                    optionalDisplayName,
                    CompanyId.parse(required(environment, "LOGIXONE_SECURITY_BOOTSTRAP_COMPANY_ID")),
                    new PluginId(required(environment, "LOGIXONE_SECURITY_BOOTSTRAP_CUSTOMIZATION_PLUGIN")),
                    new RoleCode(required(environment, "LOGIXONE_SECURITY_BOOTSTRAP_ROLE_CODE")),
                    required(environment, "LOGIXONE_SECURITY_BOOTSTRAP_ROLE_NAME"),
                    permissions(required(environment, "LOGIXONE_SECURITY_BOOTSTRAP_PERMISSIONS")));
            return new BootstrapDeclaration(true, command);
        }

        private static String required(Map<String, String> environment, String name) {
            String value = environment.get(name);
            if (value == null || value.isBlank() || !value.equals(value.strip())) {
                throw new IllegalArgumentException(name + " must contain an exact non-blank value");
            }
            return value;
        }

        private static Set<ContributionId> permissions(String value) {
            String[] entries = value.split(",", -1);
            if (Arrays.stream(entries).anyMatch(String::isBlank)) {
                throw new IllegalArgumentException(
                        "LOGIXONE_SECURITY_BOOTSTRAP_PERMISSIONS contains an empty permission");
            }
            return Arrays.stream(entries)
                    .map(String::strip)
                    .map(ContributionId::new)
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }
}
