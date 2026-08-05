package py.com.logixone.kernel.application.security.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.CompanySessionReference;
import py.com.logixone.kernel.application.security.access.OperationAuthorization;
import py.com.logixone.kernel.application.security.access.TrustedCompanyAccess;
import py.com.logixone.kernel.application.security.access.TrustedNavigationAccess;
import py.com.logixone.kernel.application.security.access.TrustedScreenAccess;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.ScreenId;

/** Trusted adapter boundary; callers may pass only identities validated by the container. */
public interface TrustedAccessPort {

    TrustedCompanyAccess resolve(
            ExternalIdentity externalIdentity,
            Optional<CompanySessionReference> sessionReference,
            String correlationId);

    TrustedCompanyAccess select(
            ExternalIdentity externalIdentity,
            CompanyId requestedCompanyId,
            String correlationId);

    TrustedNavigationAccess navigation(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference,
            String correlationId);

    TrustedScreenAccess screen(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference,
            ScreenId requestedScreenId,
            PluginId requiredPluginId,
            ContributionId requiredPermissionId,
            String correlationId);

    OperationAuthorization authorize(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference,
            PluginId requiredPluginId,
            ContributionId requiredPermissionId,
            String correlationId);
}
