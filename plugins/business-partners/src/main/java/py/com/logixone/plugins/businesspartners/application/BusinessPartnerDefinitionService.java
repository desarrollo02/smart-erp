package py.com.logixone.plugins.businesspartners.application;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.businesspartners.application.command.ChangeBusinessPartnerDefinitionState;
import py.com.logixone.plugins.businesspartners.application.command.RegisterBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.command.ReviseBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerDefinitionRepository;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceCode;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceException;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;
import py.com.logixone.plugins.businesspartners.domain.ConcurrentBusinessPartnerChangeException;

/** Framework-free orchestration for company-owned selector definitions. */
public final class BusinessPartnerDefinitionService implements BusinessPartnerDefinitionUseCases {

    private static final String RESOURCE_TYPE = "business_partner_definition";

    private final BusinessPartnerDefinitionRepository repository;
    private final TechnicalAudit audit;
    private final Clock clock;

    public BusinessPartnerDefinitionService(
            BusinessPartnerDefinitionRepository repository, TechnicalAudit audit, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public BusinessPartnerOperationResult<List<BusinessPartnerDefinition>> definitions(
            BusinessPartnerOperationContext context, BusinessPartnerDefinitionKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!authorizedForRead(context)) {
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.ACCESS_DENIED);
        }
        List<BusinessPartnerDefinition> values = repository
                .findAll(context.companyContext().companyId(), kind).stream()
                .sorted(Comparator.comparing(value -> value.displayName().value()))
                .toList();
        return BusinessPartnerOperationResult.success(values, List.of());
    }

    @Override
    public BusinessPartnerOperationResult<BusinessPartnerDefinition> registerDefinition(
            BusinessPartnerOperationContext context, RegisterBusinessPartnerDefinition command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, BusinessPartnerPermissions.MANAGE)) {
            audit(context, command, TechnicalAuditOutcome.REJECTED,
                    BusinessPartnerResultCode.ACCESS_DENIED, Optional.empty());
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.ACCESS_DENIED);
        }
        BusinessPartnerDefinition definition = BusinessPartnerDefinition.create(
                context.companyContext().companyId(), command.kind(), command.code(), command.displayName());
        try {
            BusinessPartnerDefinition inserted = repository.insert(definition);
            audit(context, command, TechnicalAuditOutcome.CHANGED,
                    BusinessPartnerResultCode.SUCCESS, Optional.of(inserted.version()));
            return BusinessPartnerOperationResult.success(inserted, List.of());
        } catch (BusinessPartnerPersistenceException failure) {
            BusinessPartnerResultCode result = failure.code()
                            == BusinessPartnerPersistenceCode.GENERAL_CODE_ALREADY_EXISTS
                    ? BusinessPartnerResultCode.GENERAL_CODE_CONFLICT
                    : BusinessPartnerResultCode.INVALID_OPERATION;
            audit(context, command, TechnicalAuditOutcome.REJECTED, result, Optional.empty());
            return BusinessPartnerOperationResult.failure(result);
        }
    }

    @Override
    public BusinessPartnerOperationResult<BusinessPartnerDefinition> reviseDefinition(
            BusinessPartnerOperationContext context,
            ReviseBusinessPartnerDefinition command) {
        Objects.requireNonNull(command, "command");
        String operation = "REVISE_BUSINESS_PARTNER_DEFINITION";
        if (!authorized(context, BusinessPartnerPermissions.MANAGE)) {
            audit(context, operation, command.kind(), command.code().value(),
                    TechnicalAuditOutcome.REJECTED, BusinessPartnerResultCode.ACCESS_DENIED,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.ACCESS_DENIED);
        }
        Optional<BusinessPartnerDefinition> found = repository.findByCode(
                context.companyContext().companyId(), command.kind(), command.code());
        if (found.isEmpty()) {
            audit(context, operation, command.kind(), command.code().value(),
                    TechnicalAuditOutcome.REJECTED, BusinessPartnerResultCode.NOT_FOUND,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.NOT_FOUND);
        }

        BusinessPartnerDefinition current = found.orElseThrow();
        try {
            BusinessPartnerDefinition revised = current.reviseDisplayName(
                    command.displayName(), command.expectedVersion());
            BusinessPartnerDefinition saved = revised.version() == current.version()
                    ? current
                    : repository.update(revised, current.version());
            audit(context, operation, command.kind(), command.code().value(),
                    saved.version() == current.version()
                            ? TechnicalAuditOutcome.UNCHANGED
                            : TechnicalAuditOutcome.CHANGED,
                    BusinessPartnerResultCode.SUCCESS,
                    Optional.of(current.version()), Optional.of(saved.version()));
            return BusinessPartnerOperationResult.success(saved, List.of());
        } catch (ConcurrentBusinessPartnerChangeException failure) {
            audit(context, operation, command.kind(), command.code().value(),
                    TechnicalAuditOutcome.REJECTED, BusinessPartnerResultCode.VERSION_CONFLICT,
                    Optional.of(current.version()), Optional.empty());
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.VERSION_CONFLICT);
        } catch (BusinessPartnerPersistenceException failure) {
            BusinessPartnerResultCode result = map(failure.code());
            audit(context, operation, command.kind(), command.code().value(),
                    TechnicalAuditOutcome.REJECTED, result,
                    Optional.of(current.version()), Optional.empty());
            return BusinessPartnerOperationResult.failure(result);
        }
    }

    @Override
    public BusinessPartnerOperationResult<List<BusinessPartnerDefinitionRevision>> definitionHistory(
            BusinessPartnerOperationContext context,
            BusinessPartnerDefinitionKind kind,
            py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode code) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
        if (!authorizedForRead(context)) {
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.ACCESS_DENIED);
        }
        if (repository.findByCode(context.companyContext().companyId(), kind, code).isEmpty()) {
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.NOT_FOUND);
        }
        try {
            return BusinessPartnerOperationResult.success(
                    repository.history(context.companyContext().companyId(), kind, code),
                    List.of());
        } catch (BusinessPartnerPersistenceException failure) {
            return BusinessPartnerOperationResult.failure(map(failure.code()));
        }
    }

    @Override
    public BusinessPartnerOperationResult<BusinessPartnerDefinition> changeDefinitionState(
            BusinessPartnerOperationContext context,
            ChangeBusinessPartnerDefinitionState command) {
        Objects.requireNonNull(command, "command");
        String operation = command.targetState()
                        == py.com.logixone.plugins.businesspartners.api.BusinessPartnerState.ACTIVE
                ? "REACTIVATE_BUSINESS_PARTNER_DEFINITION"
                : "INACTIVATE_BUSINESS_PARTNER_DEFINITION";
        if (!authorized(context, BusinessPartnerPermissions.MANAGE)) {
            audit(context, operation, command.kind(), command.code().value(),
                    TechnicalAuditOutcome.REJECTED, BusinessPartnerResultCode.ACCESS_DENIED,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.ACCESS_DENIED);
        }

        Optional<BusinessPartnerDefinition> found = repository.findByCode(
                context.companyContext().companyId(), command.kind(), command.code());
        if (found.isEmpty()) {
            audit(context, operation, command.kind(), command.code().value(),
                    TechnicalAuditOutcome.REJECTED, BusinessPartnerResultCode.NOT_FOUND,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.NOT_FOUND);
        }

        BusinessPartnerDefinition current = found.orElseThrow();
        try {
            BusinessPartnerDefinition changed = current.changeState(
                    command.targetState(), command.expectedVersion());
            BusinessPartnerDefinition saved = changed.version() == current.version()
                    ? current
                    : repository.update(changed, current.version());
            audit(context, operation, command.kind(), command.code().value(),
                    saved.version() == current.version()
                            ? TechnicalAuditOutcome.UNCHANGED
                            : TechnicalAuditOutcome.CHANGED,
                    BusinessPartnerResultCode.SUCCESS,
                    Optional.of(current.version()), Optional.of(saved.version()));
            return BusinessPartnerOperationResult.success(saved, List.of());
        } catch (ConcurrentBusinessPartnerChangeException failure) {
            audit(context, operation, command.kind(), command.code().value(),
                    TechnicalAuditOutcome.REJECTED, BusinessPartnerResultCode.VERSION_CONFLICT,
                    Optional.of(current.version()), Optional.empty());
            return BusinessPartnerOperationResult.failure(BusinessPartnerResultCode.VERSION_CONFLICT);
        } catch (BusinessPartnerPersistenceException failure) {
            BusinessPartnerResultCode result = map(failure.code());
            audit(context, operation, command.kind(), command.code().value(),
                    TechnicalAuditOutcome.REJECTED, result,
                    Optional.of(current.version()), Optional.empty());
            return BusinessPartnerOperationResult.failure(result);
        }
    }

    private void audit(
            BusinessPartnerOperationContext context,
            RegisterBusinessPartnerDefinition command,
            TechnicalAuditOutcome outcome,
            BusinessPartnerResultCode result,
            Optional<Long> resultingVersion) {
        audit.record(new TechnicalAuditEvent(
                "REGISTER_BUSINESS_PARTNER_DEFINITION",
                outcome,
                context.companyContext().actor().userId(),
                context.companyContext().companyId(),
                BusinessPartnersIdentity.PLUGIN_ID.value(),
                BusinessPartnerPermissions.MANAGE.value(),
                RESOURCE_TYPE,
                Optional.of(command.kind().name() + ":" + command.code().value()),
                result.name(),
                Optional.empty(),
                resultingVersion,
                context.correlationId(),
                clock.instant()));
    }

    private void audit(
            BusinessPartnerOperationContext context,
            String operation,
            BusinessPartnerDefinitionKind kind,
            String code,
            TechnicalAuditOutcome outcome,
            BusinessPartnerResultCode result,
            Optional<Long> previousVersion,
            Optional<Long> resultingVersion) {
        audit.record(new TechnicalAuditEvent(
                operation,
                outcome,
                context.companyContext().actor().userId(),
                context.companyContext().companyId(),
                BusinessPartnersIdentity.PLUGIN_ID.value(),
                BusinessPartnerPermissions.MANAGE.value(),
                RESOURCE_TYPE,
                Optional.of(kind.name() + ":" + code),
                result.name(),
                previousVersion,
                resultingVersion,
                context.correlationId(),
                clock.instant()));
    }

    private static boolean authorizedForRead(BusinessPartnerOperationContext context) {
        return authorized(context, BusinessPartnerPermissions.VIEW)
                || authorized(context, BusinessPartnerPermissions.MANAGE);
    }

    private static BusinessPartnerResultCode map(BusinessPartnerPersistenceCode code) {
        return switch (code) {
            case DEFINITION_NOT_FOUND, PARTNER_NOT_FOUND -> BusinessPartnerResultCode.NOT_FOUND;
            case VERSION_CONFLICT -> BusinessPartnerResultCode.VERSION_CONFLICT;
            case PARTNER_ALREADY_EXISTS, GENERAL_CODE_ALREADY_EXISTS ->
                    BusinessPartnerResultCode.GENERAL_CODE_CONFLICT;
            case ROLE_CODE_ALREADY_EXISTS -> BusinessPartnerResultCode.ROLE_CODE_CONFLICT;
            case INVALID_PERSISTED_STATE -> BusinessPartnerResultCode.INVALID_OPERATION;
        };
    }

    private static boolean authorized(
            BusinessPartnerOperationContext context, ContributionId permission) {
        return Objects.requireNonNull(context, "context").authorizes(permission);
    }
}
