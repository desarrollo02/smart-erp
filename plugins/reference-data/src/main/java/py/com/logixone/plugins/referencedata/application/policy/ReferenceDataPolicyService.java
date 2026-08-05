package py.com.logixone.plugins.referencedata.application.policy;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.plugins.referencedata.application.ReferenceDataIdentity;
import py.com.logixone.plugins.referencedata.application.ReferenceDataPermissions;

/** Framework-free orchestration for enterprise reference-data overrides. */
public final class ReferenceDataPolicyService {

    private static final String OPERATION = "CHANGE_REFERENCE_DATA_POLICY";
    private static final String RESOURCE_TYPE = "reference_data_policy";

    private final ReferenceDataPolicyRepository repository;
    private final TechnicalAudit audit;
    private final Clock clock;

    public ReferenceDataPolicyService(
            ReferenceDataPolicyRepository repository, TechnicalAudit audit, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ReferenceDataPolicyResult<ReferenceDataPolicy> current(
            AuthorizedCompanyOperation authorization,
            py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog catalog,
            String code) {
        Objects.requireNonNull(authorization, "authorization");
        String canonicalCode = ReferenceDataPolicy.canonicalCode(catalog, code);
        if (!authorizedToView(authorization)) {
            return ReferenceDataPolicyResult.failure(
                    ReferenceDataPolicyResult.Code.ACCESS_DENIED);
        }
        if (!repository.existsInCurrentRelease(catalog, canonicalCode)) {
            return ReferenceDataPolicyResult.failure(ReferenceDataPolicyResult.Code.NOT_FOUND);
        }
        return ReferenceDataPolicyResult.success(repository
                .find(authorization.context().companyId(), catalog, canonicalCode)
                .orElseGet(() -> ReferenceDataPolicy.defaultEnabled(
                        authorization.context().companyId(), catalog, canonicalCode)));
    }

    public ReferenceDataPolicyResult<ReferenceDataPolicy> change(
            AuthorizedCompanyOperation authorization, ChangeReferenceDataPolicy command) {
        Objects.requireNonNull(authorization, "authorization");
        Objects.requireNonNull(command, "command");
        if (!authorizedToManage(authorization)) {
            record(authorization, command, TechnicalAuditOutcome.REJECTED,
                    ReferenceDataPolicyResult.Code.ACCESS_DENIED,
                    Optional.empty(), Optional.empty());
            return ReferenceDataPolicyResult.failure(
                    ReferenceDataPolicyResult.Code.ACCESS_DENIED);
        }
        if (!repository.existsInCurrentRelease(command.catalog(), command.code())) {
            record(authorization, command, TechnicalAuditOutcome.REJECTED,
                    ReferenceDataPolicyResult.Code.NOT_FOUND,
                    Optional.empty(), Optional.empty());
            return ReferenceDataPolicyResult.failure(ReferenceDataPolicyResult.Code.NOT_FOUND);
        }

        ReferenceDataPolicy current = repository
                .find(authorization.context().companyId(), command.catalog(), command.code())
                .orElseGet(() -> ReferenceDataPolicy.defaultEnabled(
                        authorization.context().companyId(), command.catalog(), command.code()));
        if (current.version() != command.expectedVersion()) {
            record(authorization, command, TechnicalAuditOutcome.REJECTED,
                    ReferenceDataPolicyResult.Code.VERSION_CONFLICT,
                    Optional.of(current.version()), Optional.empty());
            return ReferenceDataPolicyResult.failure(
                    ReferenceDataPolicyResult.Code.VERSION_CONFLICT);
        }
        if (current.enabled() == command.enabled()) {
            record(authorization, command, TechnicalAuditOutcome.UNCHANGED,
                    ReferenceDataPolicyResult.Code.SUCCESS,
                    Optional.of(current.version()), Optional.of(current.version()));
            return ReferenceDataPolicyResult.success(current);
        }

        try {
            ReferenceDataPolicy changed = repository.change(
                    authorization.context().companyId(),
                    command,
                    authorization.context().actor().userId(),
                    authorization.correlationId(),
                    clock.instant());
            record(authorization, command, TechnicalAuditOutcome.CHANGED,
                    ReferenceDataPolicyResult.Code.SUCCESS,
                    Optional.of(current.version()), Optional.of(changed.version()));
            return ReferenceDataPolicyResult.success(changed);
        } catch (ConcurrentReferenceDataPolicyChangeException conflict) {
            record(authorization, command, TechnicalAuditOutcome.REJECTED,
                    ReferenceDataPolicyResult.Code.VERSION_CONFLICT,
                    Optional.of(current.version()), Optional.empty());
            return ReferenceDataPolicyResult.failure(
                    ReferenceDataPolicyResult.Code.VERSION_CONFLICT);
        }
    }

    public ReferenceDataPolicyResult<List<ReferenceDataPolicyRevision>> history(
            AuthorizedCompanyOperation authorization,
            py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog catalog,
            String code) {
        Objects.requireNonNull(authorization, "authorization");
        String canonicalCode = ReferenceDataPolicy.canonicalCode(catalog, code);
        if (!authorizedToView(authorization)) {
            return ReferenceDataPolicyResult.failure(
                    ReferenceDataPolicyResult.Code.ACCESS_DENIED);
        }
        if (!repository.existsInCurrentRelease(catalog, canonicalCode)) {
            return ReferenceDataPolicyResult.failure(ReferenceDataPolicyResult.Code.NOT_FOUND);
        }
        return ReferenceDataPolicyResult.success(repository.history(
                authorization.context().companyId(), catalog, canonicalCode));
    }

    private static boolean authorizedToManage(AuthorizedCompanyOperation authorization) {
        return ReferenceDataIdentity.PLUGIN_ID.value().equals(authorization.pluginId())
                && ReferenceDataPermissions.POLICY_MANAGE.value()
                        .equals(authorization.permissionId());
    }

    private static boolean authorizedToView(AuthorizedCompanyOperation authorization) {
        return ReferenceDataIdentity.PLUGIN_ID.value().equals(authorization.pluginId())
                && (ReferenceDataPermissions.VIEW.value().equals(authorization.permissionId())
                        || ReferenceDataPermissions.POLICY_MANAGE.value()
                                .equals(authorization.permissionId()));
    }

    private void record(
            AuthorizedCompanyOperation authorization,
            ChangeReferenceDataPolicy command,
            TechnicalAuditOutcome outcome,
            ReferenceDataPolicyResult.Code code,
            Optional<Long> previousVersion,
            Optional<Long> resultingVersion) {
        audit.record(new TechnicalAuditEvent(
                OPERATION,
                outcome,
                authorization.context().actor().userId(),
                authorization.context().companyId(),
                ReferenceDataIdentity.PLUGIN_ID.value(),
                ReferenceDataPermissions.POLICY_MANAGE.value(),
                RESOURCE_TYPE,
                Optional.of(command.catalog().name() + ":" + command.code()),
                code.name(),
                previousVersion,
                resultingVersion,
                authorization.correlationId(),
                clock.instant()));
    }
}
