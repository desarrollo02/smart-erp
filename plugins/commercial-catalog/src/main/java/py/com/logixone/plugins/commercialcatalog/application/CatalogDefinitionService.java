package py.com.logixone.plugins.commercialcatalog.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogDefinitionCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogDefinitionRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogIdGenerator;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceCode;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceException;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;

/** Authorized creation and discovery of controlled catalog definitions. */
public final class CatalogDefinitionService {

    private static final String RESOURCE_TYPE = "catalog_definition";

    private final CatalogDefinitionRepository repository;
    private final CatalogIdGenerator ids;
    private final TechnicalAudit audit;
    private final Clock clock;

    public CatalogDefinitionService(
            CatalogDefinitionRepository repository,
            CatalogIdGenerator ids,
            TechnicalAudit audit,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CatalogOperationResult<CatalogDefinitions.Snapshot> available(
            CatalogOperationContext context) {
        if (!authorized(context, CommercialCatalogPermissions.VIEW)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        return CatalogOperationResult.success(
                repository.findAll(context.companyContext().companyId()));
    }

    public CatalogOperationResult<CatalogDefinitions.Snapshot> managed(
            CatalogOperationContext context) {
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        return CatalogOperationResult.success(
                repository.findAll(context.companyContext().companyId()));
    }

    public CatalogOperationResult<CatalogDefinitions.Unit> registerUnit(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterUnit command) {
        Objects.requireNonNull(command, "command");
        return create(context, "REGISTER_CATALOG_UNIT", Optional.empty(), () -> repository.insert(
                context.companyContext().companyId(), new CatalogDefinitions.Unit(
                        command.code(), command.displayName(), command.decimalScale(),
                        CatalogDefinitions.State.ACTIVE, 0)));
    }

    public CatalogOperationResult<CatalogDefinitions.Category> registerCategory(
            CatalogOperationContext context,
            CatalogDefinitionCommands.RegisterCategory command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            return denied(context, "REGISTER_CATALOG_CATEGORY");
        }
        var id = ids.nextCategoryId();
        return create(context, "REGISTER_CATALOG_CATEGORY", Optional.of(id.value().toString()),
                () -> repository.insert(context.companyContext().companyId(),
                        new CatalogDefinitions.Category(
                                id, command.parentId(), command.code(), command.displayName(),
                                CatalogDefinitions.State.ACTIVE, 0)));
    }

    public CatalogOperationResult<CatalogDefinitions.Brand> registerBrand(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterBrand command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            return denied(context, "REGISTER_CATALOG_BRAND");
        }
        var id = ids.nextBrandId();
        return create(context, "REGISTER_CATALOG_BRAND", Optional.of(id.value().toString()),
                () -> repository.insert(context.companyContext().companyId(),
                        new CatalogDefinitions.Brand(
                                id, command.code(), command.displayName(),
                                CatalogDefinitions.State.ACTIVE, 0)));
    }

    public CatalogOperationResult<CatalogDefinitions.Tag> registerTag(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterTag command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            return denied(context, "REGISTER_CATALOG_TAG");
        }
        var id = ids.nextTagId();
        return create(context, "REGISTER_CATALOG_TAG", Optional.of(id.value().toString()),
                () -> repository.insert(context.companyContext().companyId(),
                        new CatalogDefinitions.Tag(
                                id, command.code(), command.displayName(),
                                CatalogDefinitions.State.ACTIVE, 0)));
    }

    public CatalogOperationResult<CatalogDefinitions.Lifecycle> changeSimpleState(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ChangeSimpleState command) {
        Objects.requireNonNull(command, "command");
        String operation = command.targetState() == CatalogDefinitions.State.ACTIVE
                ? "REACTIVATE_CATALOG_DEFINITION"
                : "INACTIVATE_CATALOG_DEFINITION";
        Optional<String> resourceId = Optional.of(
                command.kind().name() + ":" + command.identity());
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.ACCESS_DENIED, Optional.of(command.expectedVersion()),
                    Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            CatalogDefinitions.Lifecycle lifecycle = repository.changeSimpleState(
                    context.companyContext().companyId(), command.kind(), command.identity(),
                    command.targetState(), command.expectedVersion());
            record(context, operation, resourceId,
                    lifecycle.changed()
                            ? TechnicalAuditOutcome.CHANGED
                            : TechnicalAuditOutcome.UNCHANGED,
                    CatalogResultCode.SUCCESS, Optional.of(command.expectedVersion()),
                    Optional.of(lifecycle.version()));
            return CatalogOperationResult.success(lifecycle);
        } catch (CatalogPersistenceException failure) {
            CatalogResultCode code = map(failure.code());
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED, code,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(code);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.INVALID_OPERATION,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    public CatalogOperationResult<CatalogDefinitions.SimpleRevision> reviseSimpleDefinition(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReviseSimpleDefinition command) {
        Objects.requireNonNull(command, "command");
        String operation = "REVISE_CATALOG_DEFINITION";
        Optional<String> resourceId = Optional.of(
                command.kind().name() + ":" + command.identity());
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.ACCESS_DENIED, Optional.of(command.expectedVersion()),
                    Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            CatalogDefinitions.SimpleRevision revision = repository.reviseSimpleDefinition(
                    context.companyContext().companyId(), command.kind(), command.identity(),
                    command.displayName(), command.decimalScale(), command.parentId(),
                    command.expectedVersion());
            record(context, operation, resourceId, TechnicalAuditOutcome.CHANGED,
                    CatalogResultCode.SUCCESS, Optional.of(command.expectedVersion()),
                    Optional.of(revision.version()));
            return CatalogOperationResult.success(revision);
        } catch (CatalogPersistenceException failure) {
            CatalogResultCode code = map(failure.code());
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED, code,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(code);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.INVALID_OPERATION,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    public CatalogOperationResult<List<CatalogDefinitions.SimpleRevision>>
            simpleDefinitionHistory(
                    CatalogOperationContext context,
                    CatalogDefinitions.SimpleKind kind,
                    String identity) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(identity, "identity");
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            return CatalogOperationResult.success(repository.simpleDefinitionHistory(
                    context.companyContext().companyId(), kind, identity));
        } catch (CatalogPersistenceException failure) {
            return CatalogOperationResult.failure(map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    public CatalogOperationResult<CatalogDefinitions.Replacement> replaceSimpleDefinition(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReplaceSimpleDefinition command) {
        Objects.requireNonNull(command, "command");
        String operation = "REPLACE_CATALOG_DEFINITION";
        Optional<String> previousResourceId = Optional.of(
                command.kind().name() + ":" + command.identity());
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            record(context, operation, previousResourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.ACCESS_DENIED, Optional.of(command.expectedVersion()),
                    Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            CatalogDefinitions.ReplacementCandidate replacement = replacement(command);
            Optional<String> resourceId = Optional.of(previousResourceId.orElseThrow()
                    + "->" + replacement.identity());
            CatalogDefinitions.Replacement result = repository.replaceSimpleDefinition(
                    context.companyContext().companyId(), command.kind(), command.identity(),
                    replacement, command.expectedVersion());
            record(context, operation, resourceId, TechnicalAuditOutcome.CHANGED,
                    CatalogResultCode.SUCCESS, Optional.of(command.expectedVersion()),
                    Optional.of(result.previousVersion()));
            return CatalogOperationResult.success(result);
        } catch (CatalogPersistenceException failure) {
            CatalogResultCode code = map(failure.code());
            record(context, operation, previousResourceId, TechnicalAuditOutcome.REJECTED, code,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(code);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            record(context, operation, previousResourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.INVALID_OPERATION,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    private CatalogDefinitions.ReplacementCandidate replacement(
            CatalogDefinitionCommands.ReplaceSimpleDefinition command) {
        String identity = switch (command.kind()) {
            case UNIT -> new UnitCode(command.replacementCode()).value();
            case CATEGORY -> ids.nextCategoryId().value().toString();
            case BRAND -> ids.nextBrandId().value().toString();
            case TAG -> ids.nextTagId().value().toString();
        };
        return new CatalogDefinitions.ReplacementCandidate(
                command.kind(), identity, command.replacementCode(),
                command.replacementDisplayName(), command.replacementDecimalScale(),
                command.replacementParentId());
    }

    public CatalogOperationResult<CatalogDefinitions.TaxProfile> registerTaxProfile(
            CatalogOperationContext context,
            CatalogDefinitionCommands.RegisterTaxProfile command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            return denied(context, "REGISTER_CATALOG_TAX_PROFILE");
        }
        var id = ids.nextTaxProfileId();
        return create(context, "REGISTER_CATALOG_TAX_PROFILE",
                Optional.of(id.value().toString()), () -> repository.insert(
                        context.companyContext().companyId(), new CatalogDefinitions.TaxProfile(
                                id, command.code(), command.displayName(),
                                command.internalKindCode(), command.description(),
                                command.validFrom(), command.validUntil(),
                                CatalogDefinitions.State.ACTIVE, 0)));
    }

    public CatalogOperationResult<CatalogDefinitions.TaxProfile> changeTaxProfileState(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ChangeTaxProfileState command) {
        Objects.requireNonNull(command, "command");
        String operation = command.targetState() == CatalogDefinitions.State.ACTIVE
                ? "REACTIVATE_CATALOG_TAX_PROFILE"
                : "INACTIVATE_CATALOG_TAX_PROFILE";
        Optional<String> resourceId = Optional.of(command.id().value().toString());
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.ACCESS_DENIED, Optional.of(command.expectedVersion()),
                    Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            CatalogDefinitions.TaxProfile profile = repository.changeTaxProfileState(
                    context.companyContext().companyId(), command.id(),
                    command.targetState(), command.expectedVersion());
            record(context, operation, resourceId,
                    profile.version() == command.expectedVersion()
                            ? TechnicalAuditOutcome.UNCHANGED
                            : TechnicalAuditOutcome.CHANGED,
                    CatalogResultCode.SUCCESS, Optional.of(command.expectedVersion()),
                    Optional.of(profile.version()));
            return CatalogOperationResult.success(profile);
        } catch (CatalogPersistenceException failure) {
            CatalogResultCode code = map(failure.code());
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED, code,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(code);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.INVALID_OPERATION,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    public CatalogOperationResult<CatalogDefinitions.TaxProfile> reviseTaxProfile(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReviseTaxProfile command) {
        Objects.requireNonNull(command, "command");
        String operation = "REVISE_CATALOG_TAX_PROFILE";
        Optional<String> resourceId = Optional.of(command.id().value().toString());
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.ACCESS_DENIED, Optional.of(command.expectedVersion()),
                    Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            CatalogDefinitions.TaxProfile profile = repository.reviseTaxProfile(
                    context.companyContext().companyId(), command.id(),
                    command.internalKindCode(), command.description(), command.validFrom(),
                    command.validUntil(), command.expectedVersion());
            record(context, operation, resourceId, TechnicalAuditOutcome.CHANGED,
                    CatalogResultCode.SUCCESS, Optional.of(command.expectedVersion()),
                    Optional.of(profile.version()));
            return CatalogOperationResult.success(profile);
        } catch (CatalogPersistenceException failure) {
            CatalogResultCode code = map(failure.code());
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED, code,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(code);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.INVALID_OPERATION,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    public CatalogOperationResult<List<CatalogDefinitions.TaxProfileRevision>> taxProfileHistory(
            CatalogOperationContext context, TaxProfileId id) {
        Objects.requireNonNull(id, "id");
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            return CatalogOperationResult.success(repository.taxProfileHistory(
                    context.companyContext().companyId(), id));
        } catch (CatalogPersistenceException failure) {
            return CatalogOperationResult.failure(map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    public CatalogOperationResult<CatalogDefinitions.VariantFamily> registerVariantFamily(
            CatalogOperationContext context,
            CatalogDefinitionCommands.RegisterVariantFamily command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            return denied(context, "REGISTER_CATALOG_VARIANT_FAMILY");
        }
        var id = ids.nextVariantFamilyId();
        return create(context, "REGISTER_CATALOG_VARIANT_FAMILY",
                Optional.of(id.value().toString()), () -> repository.insert(
                        context.companyContext().companyId(), new CatalogDefinitions.VariantFamily(
                                id, command.code(), command.displayName(), command.attributes(),
                                CatalogDefinitions.State.ACTIVE, 0)));
    }

    public CatalogOperationResult<CatalogDefinitions.VariantFamily> changeVariantFamilyState(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ChangeVariantFamilyState command) {
        Objects.requireNonNull(command, "command");
        String operation = command.targetState() == CatalogDefinitions.State.ACTIVE
                ? "REACTIVATE_CATALOG_VARIANT_FAMILY"
                : "INACTIVATE_CATALOG_VARIANT_FAMILY";
        Optional<String> resourceId = Optional.of(
                "VARIANT_FAMILY:" + command.id().value());
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.ACCESS_DENIED, Optional.of(command.expectedVersion()),
                    Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            CatalogDefinitions.VariantFamily family = repository.changeVariantFamilyState(
                    context.companyContext().companyId(), command.id(),
                    command.targetState(), command.expectedVersion());
            record(context, operation, resourceId,
                    family.version() == command.expectedVersion()
                            ? TechnicalAuditOutcome.UNCHANGED
                            : TechnicalAuditOutcome.CHANGED,
                    CatalogResultCode.SUCCESS, Optional.of(command.expectedVersion()),
                    Optional.of(family.version()));
            return CatalogOperationResult.success(family);
        } catch (CatalogPersistenceException failure) {
            CatalogResultCode code = map(failure.code());
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED, code,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(code);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.INVALID_OPERATION,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    public CatalogOperationResult<CatalogDefinitions.VariantFamily> reviseVariantFamily(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReviseVariantFamily command) {
        Objects.requireNonNull(command, "command");
        String operation = "REVISE_CATALOG_VARIANT_FAMILY";
        Optional<String> resourceId = Optional.of(
                "VARIANT_FAMILY:" + command.id().value());
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.ACCESS_DENIED, Optional.of(command.expectedVersion()),
                    Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            CatalogDefinitions.VariantFamily family = repository.reviseVariantFamily(
                    context.companyContext().companyId(), command.id(), command.displayName(),
                    command.attributes(), command.expectedVersion());
            record(context, operation, resourceId, TechnicalAuditOutcome.CHANGED,
                    CatalogResultCode.SUCCESS, Optional.of(command.expectedVersion()),
                    Optional.of(family.version()));
            return CatalogOperationResult.success(family);
        } catch (CatalogPersistenceException failure) {
            CatalogResultCode code = map(failure.code());
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED, code,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(code);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.INVALID_OPERATION,
                    Optional.of(command.expectedVersion()), Optional.empty());
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    public CatalogOperationResult<List<CatalogDefinitions.VariantFamilyRevision>>
            variantFamilyHistory(CatalogOperationContext context, VariantFamilyId id) {
        Objects.requireNonNull(id, "id");
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            return CatalogOperationResult.success(repository.variantFamilyHistory(
                    context.companyContext().companyId(), id));
        } catch (CatalogPersistenceException failure) {
            return CatalogOperationResult.failure(map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    private <T> CatalogOperationResult<T> create(
            CatalogOperationContext context,
            String operation,
            Optional<String> resourceId,
            Supplier<T> insertion) {
        if (!authorized(context, CommercialCatalogPermissions.DEFINITIONS_MANAGE)) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.ACCESS_DENIED);
            return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
        }
        try {
            T inserted = insertion.get();
            record(context, operation, resourceId, TechnicalAuditOutcome.CHANGED,
                    CatalogResultCode.SUCCESS);
            return CatalogOperationResult.success(inserted);
        } catch (CatalogPersistenceException failure) {
            CatalogResultCode code = map(failure.code());
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED, code);
            return CatalogOperationResult.failure(code);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            record(context, operation, resourceId, TechnicalAuditOutcome.REJECTED,
                    CatalogResultCode.INVALID_OPERATION);
            return CatalogOperationResult.failure(CatalogResultCode.INVALID_OPERATION);
        }
    }

    private <T> CatalogOperationResult<T> denied(
            CatalogOperationContext context, String operation) {
        record(context, operation, Optional.empty(), TechnicalAuditOutcome.REJECTED,
                CatalogResultCode.ACCESS_DENIED);
        return CatalogOperationResult.failure(CatalogResultCode.ACCESS_DENIED);
    }

    private void record(
            CatalogOperationContext context,
            String operation,
            Optional<String> resourceId,
            TechnicalAuditOutcome outcome,
            CatalogResultCode code) {
        record(context, operation, resourceId, outcome, code, Optional.empty(),
                code == CatalogResultCode.SUCCESS ? Optional.of(0L) : Optional.empty());
    }

    private void record(
            CatalogOperationContext context,
            String operation,
            Optional<String> resourceId,
            TechnicalAuditOutcome outcome,
            CatalogResultCode code,
            Optional<Long> previousVersion,
            Optional<Long> resultingVersion) {
        audit.record(new TechnicalAuditEvent(
                operation,
                outcome,
                context.companyContext().actor().userId(),
                context.companyContext().companyId(),
                CommercialCatalogIdentity.PLUGIN_ID.value(),
                CommercialCatalogPermissions.DEFINITIONS_MANAGE.value(),
                RESOURCE_TYPE,
                resourceId,
                code.name(),
                previousVersion,
                resultingVersion,
                context.correlationId(),
                clock.instant()));
    }

    private static boolean authorized(
            CatalogOperationContext context,
            py.com.logixone.plugin.api.ContributionId permission) {
        return Objects.requireNonNull(context, "context").authorizes(permission);
    }

    private static CatalogResultCode map(CatalogPersistenceCode code) {
        return switch (code) {
            case CODE_CONFLICT -> CatalogResultCode.CODE_CONFLICT;
            case REFERENCE_CONFLICT -> CatalogResultCode.REFERENCE_CONFLICT;
            case VALIDITY_CONFLICT -> CatalogResultCode.VALIDITY_CONFLICT;
            case VERSION_CONFLICT -> CatalogResultCode.VERSION_CONFLICT;
            case ITEM_NOT_FOUND, PRICE_LIST_NOT_FOUND, DEFINITION_NOT_FOUND ->
                    CatalogResultCode.NOT_FOUND;
            case IDENTIFIER_CONFLICT -> CatalogResultCode.IDENTIFIER_CONFLICT;
            case UNKNOWN_CONFLICT -> CatalogResultCode.INVALID_OPERATION;
        };
    }
}
