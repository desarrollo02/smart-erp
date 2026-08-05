package py.com.logixone.plugins.commercialcatalog.application;

import java.util.Objects;
import java.util.regex.Pattern;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

/** Single-operation proof produced after the kernel revalidates current access. */
public record CatalogOperationContext(
        AuthenticatedCompanyContext companyContext,
        PluginId pluginId,
        ContributionId permissionId,
        String correlationId) {

    private static final Pattern CORRELATION =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public CatalogOperationContext {
        Objects.requireNonNull(companyContext, "companyContext");
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(permissionId, "permissionId");
        Objects.requireNonNull(correlationId, "correlationId");
        if (!CORRELATION.matcher(correlationId).matches()) {
            throw new IllegalArgumentException("Invalid correlationId");
        }
    }

    public boolean authorizes(ContributionId requiredPermission) {
        return CommercialCatalogIdentity.PLUGIN_ID.equals(pluginId)
                && Objects.requireNonNull(requiredPermission, "requiredPermission").equals(permissionId);
    }

    public static CatalogOperationContext from(AuthorizedCompanyOperation authorization) {
        Objects.requireNonNull(authorization, "authorization");
        return new CatalogOperationContext(
                authorization.context(),
                new PluginId(authorization.pluginId()),
                new ContributionId(authorization.permissionId()),
                authorization.correlationId());
    }
}
