package py.com.logixone.kernel.application.security.access;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.CompanySessionReference;
import py.com.logixone.kernel.application.company.contribution.CompanyContributionService;
import py.com.logixone.kernel.application.company.contribution.CompanyContributions;
import py.com.logixone.kernel.application.company.contribution.PluginContributions;
import py.com.logixone.kernel.application.company.screen.CompanyScreenComposer;
import py.com.logixone.kernel.application.company.screen.CompanyScreenComposition;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.security.SecurityQueryService;
import py.com.logixone.kernel.application.security.audit.AccessAuditEvent;
import py.com.logixone.kernel.application.security.audit.AccessAuditOperation;
import py.com.logixone.kernel.application.security.audit.AccessAuditOutcome;
import py.com.logixone.kernel.application.security.port.AccessAuditPort;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.CompanySelectionResolution;
import py.com.logixone.kernel.domain.security.EffectivePermissionResolution;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.ScreenId;

/**
 * Revalidates the current user, membership, company, plugins and permissions. No
 * authorization result is cached between calls.
 */
public final class TrustedAccessService {

    private final SecurityQueryService securityQueries;
    private final CompanyContributionService contributionService;
    private final AccessAuditPort auditPort;
    private final Clock clock;

    public TrustedAccessService(
            SecurityQueryService securityQueries,
            CompanyContributionService contributionService,
            AccessAuditPort auditPort,
            Clock clock) {
        this.securityQueries = Objects.requireNonNull(securityQueries, "securityQueries");
        this.contributionService = Objects.requireNonNull(
                contributionService, "contributionService");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public TrustedCompanyAccess resolve(
            ExternalIdentity externalIdentity,
            Optional<CompanySessionReference> sessionReference,
            String correlationId) {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        sessionReference = Objects.requireNonNull(sessionReference, "sessionReference");
        CurrentCompanies current = currentCompanies(externalIdentity);

        if (current.failure().isPresent()) {
            TrustedAccessCode code = current.failure().orElseThrow();
            record(
                    AccessAuditOperation.RESOLVE_COMPANY_CONTEXT,
                    AccessAuditOutcome.DENIED,
                    current.actor(),
                    sessionReference.map(CompanySessionReference::companyId),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(code),
                    correlationId);
            return TrustedCompanyAccess.forbidden(Optional.empty(), code);
        }

        AuthenticatedActor actor = current.actor().orElseThrow();
        if (sessionReference.isPresent()) {
            CompanySessionReference reference = sessionReference.orElseThrow();
            if (!reference.userId().equals(actor.userId())) {
                return deniedContext(
                        actor,
                        reference.companyId(),
                        TrustedAccessCode.SESSION_ACTOR_MISMATCH,
                        correlationId);
            }
            if (current.operationalCompanyIds().contains(reference.companyId())) {
                return allowedContext(
                        actor,
                        reference.companyId(),
                        current.operationalCompanyIds(),
                        AccessAuditOperation.RESOLVE_COMPANY_CONTEXT,
                        correlationId);
            }
            TrustedAccessCode code = current.activeMembershipCompanyIds().contains(reference.companyId())
                    ? TrustedAccessCode.COMPANY_NOT_OPERATIONAL
                    : TrustedAccessCode.COMPANY_ACCESS_DENIED;
            return deniedContext(actor, reference.companyId(), code, correlationId);
        }

        if (current.operationalCompanyIds().size() == 1) {
            return allowedContext(
                    actor,
                    current.operationalCompanyIds().getFirst(),
                    current.operationalCompanyIds(),
                    AccessAuditOperation.RESOLVE_COMPANY_CONTEXT,
                    correlationId);
        }
        if (current.operationalCompanyIds().size() > 1) {
            record(
                    AccessAuditOperation.RESOLVE_COMPANY_CONTEXT,
                    AccessAuditOutcome.SELECTION_REQUIRED,
                    Optional.of(actor),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(TrustedAccessCode.COMPANY_SELECTION_REQUIRED),
                    correlationId);
            return TrustedCompanyAccess.selectionRequired(
                    actor, current.operationalCompanyIds());
        }

        TrustedAccessCode code = current.activeMembershipCompanyIds().isEmpty()
                ? TrustedAccessCode.COMPANY_ACCESS_DENIED
                : TrustedAccessCode.COMPANY_NOT_OPERATIONAL;
        return deniedContext(actor, null, code, correlationId);
    }

    public TrustedCompanyAccess select(
            ExternalIdentity externalIdentity,
            CompanyId requestedCompanyId,
            String correlationId) {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        Objects.requireNonNull(requestedCompanyId, "requestedCompanyId");
        CurrentCompanies current = currentCompanies(externalIdentity);

        if (current.failure().isPresent()) {
            TrustedAccessCode code = current.failure().orElseThrow();
            record(
                    AccessAuditOperation.SELECT_COMPANY,
                    AccessAuditOutcome.DENIED,
                    current.actor(),
                    Optional.of(requestedCompanyId),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(code),
                    correlationId);
            return TrustedCompanyAccess.forbidden(Optional.empty(), code);
        }

        AuthenticatedActor actor = current.actor().orElseThrow();
        if (!current.operationalCompanyIds().contains(requestedCompanyId)) {
            TrustedAccessCode code = current.activeMembershipCompanyIds().contains(requestedCompanyId)
                    ? TrustedAccessCode.COMPANY_NOT_OPERATIONAL
                    : TrustedAccessCode.COMPANY_ACCESS_DENIED;
            record(
                    AccessAuditOperation.SELECT_COMPANY,
                    AccessAuditOutcome.DENIED,
                    Optional.of(actor),
                    Optional.of(requestedCompanyId),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(code),
                    correlationId);
            return TrustedCompanyAccess.forbidden(Optional.empty(), code);
        }

        return allowedContext(
                actor,
                requestedCompanyId,
                current.operationalCompanyIds(),
                AccessAuditOperation.SELECT_COMPANY,
                correlationId);
    }

    public OperationAuthorization authorize(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference,
            PluginId requiredPluginId,
            ContributionId requiredPermissionId,
            String correlationId) {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        Objects.requireNonNull(sessionReference, "sessionReference");
        Objects.requireNonNull(requiredPluginId, "requiredPluginId");
        Objects.requireNonNull(requiredPermissionId, "requiredPermissionId");

        SelectedState selected = selectedState(externalIdentity, sessionReference);
        if (selected.failure().isPresent()) {
            TrustedAccessCode code = selected.failure().orElseThrow();
            return deniedOperation(
                    selected.actor(),
                    Optional.empty(),
                    sessionReference.companyId(),
                    requiredPluginId,
                    requiredPermissionId,
                    code,
                    correlationId);
        }

        AuthenticatedActor actor = selected.actor().orElseThrow();
        AuthenticatedCompanyContext context =
                new AuthenticatedCompanyContext(actor, sessionReference.companyId());
        CompanyContributions contributions = selected.contributions().orElseThrow();
        PluginContributions owner = contributions.plugins().stream()
                .filter(plugin -> plugin.pluginId().equals(requiredPluginId))
                .findFirst()
                .orElse(null);
        if (owner == null) {
            return deniedOperation(
                    Optional.of(actor),
                    Optional.of(context),
                    context.companyId(),
                    requiredPluginId,
                    requiredPermissionId,
                    TrustedAccessCode.PLUGIN_ACCESS_DENIED,
                    correlationId);
        }
        if (!owner.permissions().contains(requiredPermissionId)) {
            return deniedOperation(
                    Optional.of(actor),
                    Optional.of(context),
                    context.companyId(),
                    requiredPluginId,
                    requiredPermissionId,
                    TrustedAccessCode.PERMISSION_ACCESS_DENIED,
                    correlationId);
        }

        EffectivePermissionResolution permissions = securityQueries.resolveEffectivePermissions(
                actor.userId(), context.companyId(), contributions.permissions());
        if (!permissions.authorized() || !permissions.permissions().contains(requiredPermissionId)) {
            return deniedOperation(
                    Optional.of(actor),
                    Optional.of(context),
                    context.companyId(),
                    requiredPluginId,
                    requiredPermissionId,
                    TrustedAccessCode.PERMISSION_ACCESS_DENIED,
                    correlationId);
        }

        record(
                AccessAuditOperation.AUTHORIZE_PLUGIN_OPERATION,
                AccessAuditOutcome.ALLOWED,
                Optional.of(actor),
                Optional.of(context.companyId()),
                Optional.of(requiredPluginId),
                Optional.of(requiredPermissionId),
                Optional.empty(),
                correlationId);
        return OperationAuthorization.authorized(
                context, requiredPluginId, requiredPermissionId);
    }

    public TrustedNavigationAccess navigation(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference,
            String correlationId) {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        Objects.requireNonNull(sessionReference, "sessionReference");

        AppUser user = securityQueries.findByExternalIdentity(externalIdentity).orElse(null);
        if (user == null || !user.isActive()) {
            Optional<AuthenticatedActor> actor = user == null
                    ? Optional.empty()
                    : Optional.of(new AuthenticatedActor(user.id()));
            return deniedNavigation(
                    actor,
                    sessionReference.companyId(),
                    TrustedAccessCode.USER_ACCESS_DENIED,
                    correlationId);
        }

        AuthenticatedActor actor = new AuthenticatedActor(user.id());
        if (!sessionReference.userId().equals(actor.userId())) {
            return deniedNavigation(
                    Optional.of(actor),
                    sessionReference.companyId(),
                    TrustedAccessCode.SESSION_ACTOR_MISMATCH,
                    correlationId);
        }

        CompanySelectionResolution selected = securityQueries.resolveCompanies(
                externalIdentity, Optional.of(sessionReference.companyId()));
        if (selected.selectedCompanyId().isEmpty()) {
            return deniedNavigation(
                    Optional.of(actor),
                    sessionReference.companyId(),
                    TrustedAccessCode.COMPANY_ACCESS_DENIED,
                    correlationId);
        }

        CompanyContributions contributions = contributionService.compose(
                sessionReference.companyId());
        if (!contributions.operational()) {
            return deniedNavigation(
                    Optional.of(actor),
                    sessionReference.companyId(),
                    TrustedAccessCode.COMPANY_NOT_OPERATIONAL,
                    correlationId);
        }

        EffectivePermissionResolution permissions = securityQueries.resolveEffectivePermissions(
                actor.userId(), sessionReference.companyId(), contributions.permissions());
        if (!permissions.authorized()) {
            return deniedNavigation(
                    Optional.of(actor),
                    sessionReference.companyId(),
                    TrustedAccessCode.PERMISSION_ACCESS_DENIED,
                    correlationId);
        }

        CompanySelectionResolution memberships = securityQueries.resolveCompanies(
                externalIdentity, Optional.empty());
        List<TrustedCompanyOption> companyOptions = memberships.availableCompanyIds().stream()
                .filter(companyId -> contributionService.compose(companyId).operational())
                .map(companyId -> new TrustedCompanyOption(
                        companyId, companyPresentationLabel(companyId)))
                .toList();

        List<TrustedMenuItem> menuItems = contributions.plugins().stream()
                .flatMap(plugin -> plugin.menuContributions().stream()
                        .filter(menu -> menu.requiredPermission()
                                .map(plugin.permissions()::contains)
                                .orElse(true))
                        .filter(menu -> menu.requiredPermission()
                                .map(permissions.permissions()::contains)
                                .orElse(true))
                        .map(menu -> new TrustedMenuItem(
                                plugin.pluginId(),
                                menu.id(),
                                menu.labelKey(),
                                menu.route(),
                                menu.requiredPermission())))
                .toList();

        AuthenticatedCompanyContext context = new AuthenticatedCompanyContext(
                actor, sessionReference.companyId());
        TrustedNavigationView view = new TrustedNavigationView(
                context,
                user.displayName().orElse("Usuario de Smart ERP"),
                companyOptions,
                menuItems);
        record(
                AccessAuditOperation.RESOLVE_NAVIGATION,
                AccessAuditOutcome.ALLOWED,
                Optional.of(actor),
                Optional.of(context.companyId()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                correlationId);
        return TrustedNavigationAccess.allowed(view);
    }

    public TrustedScreenAccess screen(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference,
            ScreenId requestedScreenId,
            PluginId requiredPluginId,
            ContributionId requiredPermissionId,
            String correlationId) {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        Objects.requireNonNull(sessionReference, "sessionReference");
        Objects.requireNonNull(requestedScreenId, "requestedScreenId");
        Objects.requireNonNull(requiredPluginId, "requiredPluginId");
        Objects.requireNonNull(requiredPermissionId, "requiredPermissionId");

        if (!requestedScreenId.ownerPluginId().equals(requiredPluginId)) {
            recordScreen(
                    AccessAuditOutcome.DENIED,
                    Optional.empty(),
                    Optional.of(sessionReference.companyId()),
                    requestedScreenId.ownerPluginId(),
                    Optional.empty(),
                    requestedScreenId,
                    Optional.of(TrustedAccessCode.SCREEN_ACCESS_DENIED),
                    correlationId);
            return TrustedScreenAccess.forbidden(
                    requestedScreenId,
                    Optional.empty(),
                    TrustedAccessCode.SCREEN_ACCESS_DENIED);
        }

        OperationAuthorization authorization = authorize(
                externalIdentity,
                sessionReference,
                requiredPluginId,
                requiredPermissionId,
                correlationId);
        if (!authorization.authorized()) {
            TrustedAccessCode code = authorization.failure().orElseThrow();
            Optional<AuthenticatedCompanyContext> context = authorization.context();
            recordScreen(
                    AccessAuditOutcome.DENIED,
                    context.map(AuthenticatedCompanyContext::actor),
                    Optional.of(sessionReference.companyId()),
                    requiredPluginId,
                    Optional.of(requiredPermissionId),
                    requestedScreenId,
                    Optional.of(code),
                    correlationId);
            return TrustedScreenAccess.forbidden(requestedScreenId, context, code);
        }

        AuthenticatedCompanyContext context = authorization.context().orElseThrow();
        CompanyScreenComposition composition = new CompanyScreenComposer().compose(
                contributionService.compose(context.companyId()));
        if (!composition.operational()) {
            return deniedScreen(
                    context,
                    requestedScreenId,
                    requiredPluginId,
                    requiredPermissionId,
                    TrustedAccessCode.SCREEN_COMPOSITION_INVALID,
                    correlationId);
        }

        ComposedScreen composed = composition.screens().stream()
                .filter(candidate -> candidate.id().equals(requestedScreenId))
                .findFirst()
                .orElse(null);
        if (composed == null) {
            return deniedScreen(
                    context,
                    requestedScreenId,
                    requiredPluginId,
                    requiredPermissionId,
                    TrustedAccessCode.SCREEN_ACCESS_DENIED,
                    correlationId);
        }

        recordScreen(
                AccessAuditOutcome.ALLOWED,
                Optional.of(context.actor()),
                Optional.of(context.companyId()),
                requiredPluginId,
                Optional.of(requiredPermissionId),
                requestedScreenId,
                Optional.empty(),
                correlationId);
        return TrustedScreenAccess.allowed(context, composed);
    }

    private CurrentCompanies currentCompanies(ExternalIdentity externalIdentity) {
        AppUser user = securityQueries.findByExternalIdentity(externalIdentity).orElse(null);
        if (user == null) {
            return CurrentCompanies.failed(TrustedAccessCode.USER_ACCESS_DENIED);
        }
        AuthenticatedActor actor = new AuthenticatedActor(user.id());
        if (!user.isActive()) {
            return CurrentCompanies.failed(actor, TrustedAccessCode.USER_ACCESS_DENIED);
        }

        CompanySelectionResolution memberships =
                securityQueries.resolveCompanies(externalIdentity, Optional.empty());
        if (memberships.failure().isPresent()
                && memberships.availableCompanyIds().isEmpty()) {
            return CurrentCompanies.failed(actor, TrustedAccessCode.COMPANY_ACCESS_DENIED);
        }
        List<CompanyId> activeMemberships = memberships.availableCompanyIds();
        List<CompanyId> operational = activeMemberships.stream()
                .filter(companyId -> contributionService.compose(companyId).operational())
                .toList();
        return CurrentCompanies.resolved(actor, activeMemberships, operational);
    }

    private SelectedState selectedState(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference) {
        AppUser user = securityQueries.findByExternalIdentity(externalIdentity).orElse(null);
        if (user == null || !user.isActive()) {
            return SelectedState.failed(
                    user == null ? Optional.empty() : Optional.of(new AuthenticatedActor(user.id())),
                    TrustedAccessCode.USER_ACCESS_DENIED);
        }
        AuthenticatedActor actor = new AuthenticatedActor(user.id());
        if (!sessionReference.userId().equals(actor.userId())) {
            return SelectedState.failed(
                    Optional.of(actor), TrustedAccessCode.SESSION_ACTOR_MISMATCH);
        }

        CompanySelectionResolution membership = securityQueries.resolveCompanies(
                externalIdentity, Optional.of(sessionReference.companyId()));
        if (membership.selectedCompanyId().isEmpty()) {
            return SelectedState.failed(
                    Optional.of(actor), TrustedAccessCode.COMPANY_ACCESS_DENIED);
        }
        CompanyContributions contributions = contributionService.compose(sessionReference.companyId());
        if (!contributions.operational()) {
            return SelectedState.failed(
                    Optional.of(actor), TrustedAccessCode.COMPANY_NOT_OPERATIONAL);
        }
        return SelectedState.resolved(actor, contributions);
    }

    private TrustedCompanyAccess allowedContext(
            AuthenticatedActor actor,
            CompanyId companyId,
            List<CompanyId> availableCompanyIds,
            AccessAuditOperation operation,
            String correlationId) {
        record(
                operation,
                AccessAuditOutcome.ALLOWED,
                Optional.of(actor),
                Optional.of(companyId),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                correlationId);
        return TrustedCompanyAccess.selected(actor, companyId, availableCompanyIds);
    }

    private TrustedCompanyAccess deniedContext(
            AuthenticatedActor actor,
            CompanyId companyId,
            TrustedAccessCode code,
            String correlationId) {
        record(
                AccessAuditOperation.RESOLVE_COMPANY_CONTEXT,
                AccessAuditOutcome.DENIED,
                Optional.of(actor),
                Optional.ofNullable(companyId),
                Optional.empty(),
                Optional.empty(),
                Optional.of(code),
                correlationId);
        return TrustedCompanyAccess.forbidden(Optional.empty(), code);
    }

    private OperationAuthorization deniedOperation(
            Optional<AuthenticatedActor> actor,
            Optional<AuthenticatedCompanyContext> context,
            CompanyId companyId,
            PluginId pluginId,
            ContributionId permissionId,
            TrustedAccessCode code,
            String correlationId) {
        record(
                AccessAuditOperation.AUTHORIZE_PLUGIN_OPERATION,
                AccessAuditOutcome.DENIED,
                actor,
                Optional.of(companyId),
                Optional.of(pluginId),
                Optional.of(permissionId),
                Optional.of(code),
                correlationId);
        return OperationAuthorization.forbidden(
                context, pluginId, permissionId, code);
    }

    private TrustedNavigationAccess deniedNavigation(
            Optional<AuthenticatedActor> actor,
            CompanyId companyId,
            TrustedAccessCode code,
            String correlationId) {
        record(
                AccessAuditOperation.RESOLVE_NAVIGATION,
                AccessAuditOutcome.DENIED,
                actor,
                Optional.of(companyId),
                Optional.empty(),
                Optional.empty(),
                Optional.of(code),
                correlationId);
        return TrustedNavigationAccess.forbidden(code);
    }

    private TrustedScreenAccess deniedScreen(
            AuthenticatedCompanyContext context,
            ScreenId screenId,
            PluginId pluginId,
            ContributionId permissionId,
            TrustedAccessCode code,
            String correlationId) {
        recordScreen(
                AccessAuditOutcome.DENIED,
                Optional.of(context.actor()),
                Optional.of(context.companyId()),
                pluginId,
                Optional.of(permissionId),
                screenId,
                Optional.of(code),
                correlationId);
        return TrustedScreenAccess.forbidden(screenId, Optional.of(context), code);
    }

    private static String companyPresentationLabel(CompanyId companyId) {
        String fingerprint = companyId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "Empresa · " + fingerprint;
    }

    private void record(
            AccessAuditOperation operation,
            AccessAuditOutcome outcome,
            Optional<AuthenticatedActor> actor,
            Optional<CompanyId> companyId,
            Optional<PluginId> pluginId,
            Optional<ContributionId> permissionId,
            Optional<TrustedAccessCode> code,
            String correlationId) {
        auditPort.record(new AccessAuditEvent(
                operation,
                outcome,
                actor.map(AuthenticatedActor::userId),
                companyId,
                pluginId,
                permissionId,
                Optional.empty(),
                code,
                correlationId,
                clock.instant()));
    }

    private void recordScreen(
            AccessAuditOutcome outcome,
            Optional<AuthenticatedActor> actor,
            Optional<CompanyId> companyId,
            PluginId pluginId,
            Optional<ContributionId> permissionId,
            ScreenId screenId,
            Optional<TrustedAccessCode> code,
            String correlationId) {
        auditPort.record(new AccessAuditEvent(
                AccessAuditOperation.RESOLVE_SCREEN,
                outcome,
                actor.map(AuthenticatedActor::userId),
                companyId,
                Optional.of(pluginId),
                permissionId,
                Optional.of(screenId),
                code,
                correlationId,
                clock.instant()));
    }

    private record CurrentCompanies(
            Optional<AuthenticatedActor> actor,
            List<CompanyId> activeMembershipCompanyIds,
            List<CompanyId> operationalCompanyIds,
            Optional<TrustedAccessCode> failure) {

        private CurrentCompanies {
            actor = Objects.requireNonNull(actor, "actor");
            activeMembershipCompanyIds = List.copyOf(activeMembershipCompanyIds);
            operationalCompanyIds = List.copyOf(operationalCompanyIds);
            failure = Objects.requireNonNull(failure, "failure");
        }

        static CurrentCompanies resolved(
                AuthenticatedActor actor,
                List<CompanyId> activeMemberships,
                List<CompanyId> operational) {
            return new CurrentCompanies(
                    Optional.of(actor), activeMemberships, operational, Optional.empty());
        }

        static CurrentCompanies failed(TrustedAccessCode failure) {
            return new CurrentCompanies(
                    Optional.empty(), List.of(), List.of(), Optional.of(failure));
        }

        static CurrentCompanies failed(
                AuthenticatedActor actor,
                TrustedAccessCode failure) {
            return new CurrentCompanies(
                    Optional.of(actor), List.of(), List.of(), Optional.of(failure));
        }
    }

    private record SelectedState(
            Optional<AuthenticatedActor> actor,
            Optional<CompanyContributions> contributions,
            Optional<TrustedAccessCode> failure) {

        private SelectedState {
            actor = Objects.requireNonNull(actor, "actor");
            contributions = Objects.requireNonNull(contributions, "contributions");
            failure = Objects.requireNonNull(failure, "failure");
        }

        static SelectedState resolved(
                AuthenticatedActor actor,
                CompanyContributions contributions) {
            return new SelectedState(
                    Optional.of(actor), Optional.of(contributions), Optional.empty());
        }

        static SelectedState failed(
                Optional<AuthenticatedActor> actor,
                TrustedAccessCode failure) {
            return new SelectedState(actor, Optional.empty(), Optional.of(failure));
        }
    }
}
