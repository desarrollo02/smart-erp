package py.com.logixone.kernel.application.security.access;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

/** Result of revalidating actor, company, plugin and permission for one operation. */
public record OperationAuthorization(
        OperationAuthorizationStatus status,
        Optional<AuthenticatedCompanyContext> context,
        PluginId pluginId,
        ContributionId permissionId,
        Optional<TrustedAccessCode> failure) {

    public OperationAuthorization {
        Objects.requireNonNull(status, "status");
        context = Objects.requireNonNull(context, "context");
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(permissionId, "permissionId");
        failure = Objects.requireNonNull(failure, "failure");
        if (status == OperationAuthorizationStatus.AUTHORIZED) {
            if (context.isEmpty() || failure.isPresent()) {
                throw new IllegalArgumentException(
                        "authorized operation requires context and no failure");
            }
        } else if (failure.isEmpty()) {
            throw new IllegalArgumentException("forbidden operation requires a failure");
        }
    }

    public static OperationAuthorization authorized(
            AuthenticatedCompanyContext context,
            PluginId pluginId,
            ContributionId permissionId) {
        return new OperationAuthorization(
                OperationAuthorizationStatus.AUTHORIZED,
                Optional.of(Objects.requireNonNull(context, "context")),
                pluginId,
                permissionId,
                Optional.empty());
    }

    public static OperationAuthorization forbidden(
            Optional<AuthenticatedCompanyContext> context,
            PluginId pluginId,
            ContributionId permissionId,
            TrustedAccessCode failure) {
        return new OperationAuthorization(
                OperationAuthorizationStatus.FORBIDDEN,
                Objects.requireNonNull(context, "context"),
                pluginId,
                permissionId,
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }

    public boolean authorized() {
        return status == OperationAuthorizationStatus.AUTHORIZED;
    }
}
