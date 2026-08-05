package py.com.logixone.web.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.CompanySessionReference;
import py.com.logixone.kernel.application.security.access.OperationAuthorization;
import py.com.logixone.kernel.application.security.access.TrustedCompanyAccess;
import py.com.logixone.kernel.application.security.access.TrustedCompanyAccessStatus;
import py.com.logixone.kernel.application.security.access.TrustedNavigationAccess;
import py.com.logixone.kernel.application.security.access.TrustedNavigationView;
import py.com.logixone.kernel.application.security.access.TrustedScreenAccess;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.security.port.TrustedAccessPort;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.ScreenId;

/** Web boundary for trusted company selection and server-side authorization. */
@RequestScoped
public class TrustedWebAccess {

    @Inject
    ValidatedOidcPrincipal oidcPrincipal;

    @Inject
    TrustedAccessPort accessPort;

    @Inject
    TrustedCompanySession session;

    @Inject
    RequestCorrelation correlation;

    public TrustedCompanyAccess current() {
        ExternalIdentity identity = requiredIdentity();
        TrustedCompanyAccess access = accessPort.resolve(
                identity, session.reference(), correlation.value());
        if (access.status() == TrustedCompanyAccessStatus.SELECTED) {
            session.bind(access.context().orElseThrow());
        } else {
            session.clear();
        }
        if (access.status() == TrustedCompanyAccessStatus.FORBIDDEN) {
            throw TrustedWebAccessException.forbidden();
        }
        return access;
    }

    public AuthenticatedCompanyContext requireCompany() {
        TrustedCompanyAccess access = current();
        return access.context().orElseThrow(TrustedWebAccessException::forbidden);
    }

    public TrustedCompanyAccess selectCompany(String candidateCompanyId) {
        ExternalIdentity identity = requiredIdentity();
        session.clear();

        CompanyId companyId;
        try {
            companyId = CompanyId.parse(candidateCompanyId);
        } catch (IllegalArgumentException | NullPointerException invalidCandidate) {
            throw TrustedWebAccessException.forbidden();
        }

        TrustedCompanyAccess access = accessPort.select(
                identity, companyId, correlation.value());
        if (access.status() != TrustedCompanyAccessStatus.SELECTED) {
            throw TrustedWebAccessException.forbidden();
        }
        session.bind(access.context().orElseThrow());
        return access;
    }

    public TrustedNavigationView navigation() {
        TrustedCompanyAccess current = current();
        if (current.status() != TrustedCompanyAccessStatus.SELECTED) {
            throw TrustedWebAccessException.forbidden();
        }

        ExternalIdentity identity = requiredIdentity();
        CompanySessionReference reference = session.reference()
                .orElseThrow(TrustedWebAccessException::forbidden);
        TrustedNavigationAccess navigation = accessPort.navigation(
                identity, reference, correlation.value());
        if (!navigation.allowed()) {
            session.clear();
            throw TrustedWebAccessException.forbidden();
        }
        return navigation.view().orElseThrow();
    }

    public OperationAuthorization requireAuthorization(
            PluginId requiredPluginId,
            ContributionId requiredPermissionId) {
        TrustedCompanyAccess current = current();
        if (current.status() != TrustedCompanyAccessStatus.SELECTED) {
            throw TrustedWebAccessException.forbidden();
        }

        ExternalIdentity identity = requiredIdentity();
        CompanySessionReference reference = session.reference()
                .orElseThrow(TrustedWebAccessException::forbidden);
        OperationAuthorization authorization = accessPort.authorize(
                identity,
                reference,
                requiredPluginId,
                requiredPermissionId,
                correlation.value());
        if (!authorization.authorized()) {
            if (authorization.context().isEmpty()) {
                session.clear();
            }
            throw TrustedWebAccessException.forbidden();
        }
        return authorization;
    }

    public ComposedScreen requireScreen(
            ScreenId screenId,
            PluginId requiredPluginId,
            ContributionId requiredPermissionId) {
        TrustedCompanyAccess current = current();
        if (current.status() != TrustedCompanyAccessStatus.SELECTED) {
            throw TrustedWebAccessException.forbidden();
        }

        ExternalIdentity identity = requiredIdentity();
        CompanySessionReference reference = session.reference()
                .orElseThrow(TrustedWebAccessException::forbidden);
        TrustedScreenAccess screen = accessPort.screen(
                identity,
                reference,
                screenId,
                requiredPluginId,
                requiredPermissionId,
                correlation.value());
        if (!screen.allowed()) {
            if (screen.context().isEmpty()) {
                session.clear();
            }
            throw TrustedWebAccessException.forbidden();
        }
        return screen.screen().orElseThrow();
    }

    public void clear() {
        session.clear();
    }

    private ExternalIdentity requiredIdentity() {
        Optional<ExternalIdentity> identity = oidcPrincipal.currentIdentity();
        if (identity.isEmpty()) {
            session.clear();
            throw TrustedWebAccessException.unauthorized();
        }
        return identity.orElseThrow();
    }
}
