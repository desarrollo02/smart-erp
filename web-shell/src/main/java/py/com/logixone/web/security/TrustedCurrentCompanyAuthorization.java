package py.com.logixone.web.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

/** Converts the current OIDC/session revalidation into the neutral plugin proof. */
@RequestScoped
public class TrustedCurrentCompanyAuthorization implements CurrentCompanyAuthorization {

    @Inject
    TrustedWebAccess trustedAccess;

    @Inject
    RequestCorrelation correlation;

    @Override
    public AuthorizedCompanyOperation require(String pluginId, String permissionId) {
        PluginId requiredPlugin = new PluginId(pluginId);
        ContributionId requiredPermission = new ContributionId(permissionId);
        var authorization = trustedAccess.requireAuthorization(
                requiredPlugin, requiredPermission);
        return new AuthorizedCompanyOperation(
                authorization.context().orElseThrow(TrustedWebAccessException::forbidden),
                requiredPlugin.value(),
                requiredPermission.value(),
                correlation.value());
    }
}
