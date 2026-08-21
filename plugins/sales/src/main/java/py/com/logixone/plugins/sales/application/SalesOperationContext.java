package py.com.logixone.plugins.sales.application;

import java.util.Objects;
import java.util.regex.Pattern;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

public record SalesOperationContext(AuthenticatedCompanyContext companyContext,
        PluginId pluginId, ContributionId permissionId, String correlationId) {
    private static final Pattern CORRELATION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    public SalesOperationContext {
        Objects.requireNonNull(companyContext, "companyContext");
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(permissionId, "permissionId");
        if (correlationId == null || !CORRELATION.matcher(correlationId).matches()) {
            throw new IllegalArgumentException("Invalid correlationId");
        }
    }
    public boolean authorizes(ContributionId required) {
        return SalesIdentity.PLUGIN_ID.equals(pluginId) && permissionId.equals(required);
    }
    public static SalesOperationContext from(AuthorizedCompanyOperation value) {
        return new SalesOperationContext(value.context(), new PluginId(value.pluginId()),
                new ContributionId(value.permissionId()), value.correlationId());
    }
}
