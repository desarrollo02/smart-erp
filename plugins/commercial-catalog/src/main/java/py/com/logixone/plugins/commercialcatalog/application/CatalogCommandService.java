package py.com.logixone.plugins.commercialcatalog.application;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogCommands;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogCodeSequenceRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogIdGenerator;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogItemRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceCode;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceException;
import py.com.logixone.plugins.commercialcatalog.application.port.CurrencyReferencePolicy;
import py.com.logixone.plugins.commercialcatalog.application.port.PriceListRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.VariantFamilyAssignmentRepository;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItem;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemCode;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemIdentifier;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogVariant;
import py.com.logixone.plugins.commercialcatalog.domain.ConcurrentCatalogChangeException;
import py.com.logixone.plugins.commercialcatalog.domain.PriceEntry;
import py.com.logixone.plugins.commercialcatalog.domain.PriceList;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListCode;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListState;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeValue;

/** Framework-free orchestration of catalog item and price-list mutations. */
public final class CatalogCommandService {

    private static final String ITEM_RESOURCE = "catalog_item";
    private static final String PRICE_LIST_RESOURCE = "catalog_price_list";
    private static final String ITEM_SEQUENCE = "item";
    private static final String PRICE_LIST_SEQUENCE = "price_list";

    private final CatalogItemRepository items;
    private final PriceListRepository priceLists;
    private final VariantFamilyAssignmentRepository variantFamilies;
    private final CatalogCodeSequenceRepository sequences;
    private final CatalogIdGenerator ids;
    private final CurrencyReferencePolicy currencies;
    private final TechnicalAudit audit;
    private final Clock clock;

    public CatalogCommandService(
            CatalogItemRepository items,
            PriceListRepository priceLists,
            VariantFamilyAssignmentRepository variantFamilies,
            CatalogCodeSequenceRepository sequences,
            CatalogIdGenerator ids,
            CurrencyReferencePolicy currencies,
            TechnicalAudit audit,
            Clock clock) {
        this.items = Objects.requireNonNull(items, "items");
        this.priceLists = Objects.requireNonNull(priceLists, "priceLists");
        this.variantFamilies = Objects.requireNonNull(variantFamilies, "variantFamilies");
        this.sequences = Objects.requireNonNull(sequences, "sequences");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.currencies = Objects.requireNonNull(currencies, "currencies");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CatalogOperationResult<CatalogItemSnapshot> registerItem(
            CatalogOperationContext context, CatalogCommands.RegisterItem command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, CommercialCatalogPermissions.ITEMS_MANAGE)) {
            return rejected(context, CommercialCatalogPermissions.ITEMS_MANAGE,
                    "REGISTER_CATALOG_ITEM", ITEM_RESOURCE, Optional.empty(), Optional.empty(),
                    CatalogResultCode.ACCESS_DENIED);
        }
        CatalogItemId id = ids.nextItemId();
        CatalogItemCode code = command.code().orElseGet(() -> new CatalogItemCode(
                "ITEM-%08d".formatted(sequences.next(
                        context.companyContext().companyId(), ITEM_SEQUENCE))));
        CatalogItem item = CatalogItem.create(
                context.companyContext().companyId(), id, code, command.name(),
                command.description(), command.type(), command.scopes(), command.baseUnit(),
                command.taxProfile());
        try {
            CatalogItem inserted = items.insert(item);
            changed(context, CommercialCatalogPermissions.ITEMS_MANAGE,
                    "REGISTER_CATALOG_ITEM", ITEM_RESOURCE, id.value().toString(),
                    Optional.empty(), inserted.version());
            return CatalogOperationResult.success(inserted.snapshot());
        } catch (CatalogPersistenceException failure) {
            return rejected(context, CommercialCatalogPermissions.ITEMS_MANAGE,
                    "REGISTER_CATALOG_ITEM", ITEM_RESOURCE, Optional.of(id.value().toString()),
                    Optional.empty(), map(failure.code()));
        }
    }

    public CatalogOperationResult<CatalogItemSnapshot> reviseItem(
            CatalogOperationContext context, CatalogCommands.ReviseItem command) {
        Objects.requireNonNull(command, "command");
        return mutateItem(context, "REVISE_CATALOG_ITEM", command.id(), command.expectedVersion(),
                item -> item.reviseIdentity(command.code(), command.name(), command.description(),
                        command.scopes(), command.expectedVersion()));
    }

    public CatalogOperationResult<CatalogItemSnapshot> addIdentifier(
            CatalogOperationContext context, CatalogCommands.AddIdentifier command) {
        Objects.requireNonNull(command, "command");
        return mutateItem(context, "ADD_CATALOG_ITEM_IDENTIFIER", command.id(),
                command.expectedVersion(), item -> item.addIdentifier(
                        CatalogItemIdentifier.active(
                                ids.nextDetailId(), command.typeCode(), command.presentedValue()),
                        command.expectedVersion()));
    }

    public CatalogOperationResult<CatalogItemSnapshot> inactivateIdentifier(
            CatalogOperationContext context, CatalogCommands.InactivateIdentifier command) {
        Objects.requireNonNull(command, "command");
        return mutateItem(context, "INACTIVATE_CATALOG_ITEM_IDENTIFIER", command.id(),
                command.expectedVersion(), item -> item.inactivateIdentifier(
                        command.identifierId(), command.expectedVersion()));
    }

    public CatalogOperationResult<CatalogItemSnapshot> addUnitConversion(
            CatalogOperationContext context, CatalogCommands.AddUnitConversion command) {
        Objects.requireNonNull(command, "command");
        return mutateItem(context, "ADD_CATALOG_ITEM_UNIT_CONVERSION", command.id(),
                command.expectedVersion(), item -> item.addUnitConversion(
                        command.conversion(), command.expectedVersion()));
    }

    public CatalogOperationResult<CatalogItemSnapshot> classify(
            CatalogOperationContext context, CatalogCommands.Classify command) {
        Objects.requireNonNull(command, "command");
        return mutateItem(context, "CLASSIFY_CATALOG_ITEM", command.id(),
                command.expectedVersion(), item -> item.classify(
                        command.classification(), command.expectedVersion()));
    }

    public CatalogOperationResult<CatalogItemSnapshot> assignTaxProfile(
            CatalogOperationContext context, CatalogCommands.AssignTaxProfile command) {
        Objects.requireNonNull(command, "command");
        return mutateItem(context, "ASSIGN_CATALOG_ITEM_TAX_PROFILE", command.id(),
                command.expectedVersion(), item -> item.assignTaxProfile(
                        command.taxProfile(), command.expectedVersion()));
    }

    public CatalogOperationResult<CatalogItemSnapshot> assignVariant(
            CatalogOperationContext context, CatalogCommands.AssignVariant command) {
        Objects.requireNonNull(command, "command");
        return mutateItem(context, "ASSIGN_CATALOG_ITEM_VARIANT", command.id(),
                command.expectedVersion(), item -> item.assignVariant(
                        variant(context, command), command.expectedVersion()));
    }

    private CatalogVariant variant(
            CatalogOperationContext context, CatalogCommands.AssignVariant command) {
        CatalogDefinitions.VariantFamily family = variantFamilies.findCurrentForAssignment(
                        context.companyContext().companyId(), command.familyId())
                .filter(candidate -> candidate.state() == CatalogDefinitions.State.ACTIVE)
                .filter(candidate -> candidate.version() == command.familyVersion())
                .orElseThrow(() -> new CatalogPersistenceException(
                        CatalogPersistenceCode.REFERENCE_CONFLICT));
        Map<VariantAttributeCode, CatalogDefinitions.VariantAttribute> definitions =
                family.attributes().stream().collect(Collectors.toUnmodifiableMap(
                        CatalogDefinitions.VariantAttribute::code, Function.identity()));
        if (!definitions.keySet().containsAll(command.attributes().keySet())
                || family.attributes().stream().filter(CatalogDefinitions.VariantAttribute::required)
                        .anyMatch(attribute -> !command.attributes().containsKey(attribute.code()))) {
            throw new IllegalArgumentException("Variant values do not match the family structure");
        }
        Map<VariantAttributeCode, VariantAttributeValue> values = command.attributes().entrySet()
                .stream().collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new VariantAttributeValue(
                                definitions.get(entry.getKey()).valueType(), entry.getValue())));
        return new CatalogVariant(family.id(), family.version(), values);
    }

    public CatalogOperationResult<CatalogItemSnapshot> changeItemLifecycle(
            CatalogOperationContext context, CatalogCommands.ChangeItemLifecycle command) {
        Objects.requireNonNull(command, "command");
        Consumer<CatalogItem> change = command.state() == CatalogItemState.ACTIVE
                ? item -> item.reactivate(command.expectedVersion())
                : item -> item.inactivate(command.replacementId(), command.expectedVersion());
        return mutateItem(context, "CHANGE_CATALOG_ITEM_LIFECYCLE", command.id(),
                command.expectedVersion(), change);
    }

    public CatalogOperationResult<PriceListSnapshot> registerPriceList(
            CatalogOperationContext context, CatalogCommands.RegisterPriceList command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, CommercialCatalogPermissions.PRICES_MANAGE)) {
            return rejected(context, CommercialCatalogPermissions.PRICES_MANAGE,
                    "REGISTER_CATALOG_PRICE_LIST", PRICE_LIST_RESOURCE, Optional.empty(),
                    Optional.empty(), CatalogResultCode.ACCESS_DENIED);
        }
        if (!currencies.isEnabled(
                context.companyContext().companyId(), command.currency())) {
            return rejected(context, CommercialCatalogPermissions.PRICES_MANAGE,
                    "REGISTER_CATALOG_PRICE_LIST", PRICE_LIST_RESOURCE, Optional.empty(),
                    Optional.empty(), CatalogResultCode.INVALID_OPERATION);
        }
        PriceListId id = ids.nextPriceListId();
        PriceListCode code = command.code().orElseGet(() -> new PriceListCode(
                "PRICE-%08d".formatted(sequences.next(
                        context.companyContext().companyId(), PRICE_LIST_SEQUENCE))));
        PriceList priceList = PriceList.create(
                context.companyContext().companyId(), id, code, command.name(),
                command.currency(), command.taxMode(), command.scale(), command.roundingMode());
        try {
            PriceList inserted = priceLists.insert(priceList);
            changed(context, CommercialCatalogPermissions.PRICES_MANAGE,
                    "REGISTER_CATALOG_PRICE_LIST", PRICE_LIST_RESOURCE,
                    id.value().toString(), Optional.empty(), inserted.version());
            return CatalogOperationResult.success(inserted.snapshot());
        } catch (CatalogPersistenceException failure) {
            return rejected(context, CommercialCatalogPermissions.PRICES_MANAGE,
                    "REGISTER_CATALOG_PRICE_LIST", PRICE_LIST_RESOURCE,
                    Optional.of(id.value().toString()), Optional.empty(), map(failure.code()));
        }
    }

    public CatalogOperationResult<PriceListSnapshot> renamePriceList(
            CatalogOperationContext context, CatalogCommands.RenamePriceList command) {
        Objects.requireNonNull(command, "command");
        return mutatePriceList(context, "RENAME_CATALOG_PRICE_LIST", command.id(),
                command.expectedVersion(), list -> list.rename(
                        command.name(), command.expectedVersion()));
    }

    public CatalogOperationResult<PriceListSnapshot> addPriceEntry(
            CatalogOperationContext context, CatalogCommands.AddPriceEntry command) {
        Objects.requireNonNull(command, "command");
        return mutatePriceList(context, "ADD_CATALOG_PRICE_ENTRY", command.id(),
                command.expectedVersion(), list -> list.addEntry(PriceEntry.active(
                        ids.nextPriceEntryId(), command.itemId(), command.unit(),
                        command.minimumQuantity(), command.amount(), command.validFrom(),
                        command.validUntil()), command.expectedVersion()));
    }

    public CatalogOperationResult<PriceListSnapshot> inactivatePriceEntry(
            CatalogOperationContext context, CatalogCommands.InactivatePriceEntry command) {
        Objects.requireNonNull(command, "command");
        return mutatePriceList(context, "INACTIVATE_CATALOG_PRICE_ENTRY", command.id(),
                command.expectedVersion(), list -> list.inactivateEntry(
                        command.entryId(), command.expectedVersion()));
    }

    public CatalogOperationResult<PriceListSnapshot> changePriceListLifecycle(
            CatalogOperationContext context, CatalogCommands.ChangePriceListLifecycle command) {
        Objects.requireNonNull(command, "command");
        Consumer<PriceList> change = command.state() == PriceListState.ACTIVE
                ? list -> list.reactivate(command.expectedVersion())
                : list -> list.inactivate(command.expectedVersion());
        return mutatePriceList(context, "CHANGE_CATALOG_PRICE_LIST_LIFECYCLE", command.id(),
                command.expectedVersion(), change);
    }

    private CatalogOperationResult<CatalogItemSnapshot> mutateItem(
            CatalogOperationContext context,
            String operation,
            CatalogItemId id,
            long expectedVersion,
            Consumer<CatalogItem> mutation) {
        ContributionId permission = CommercialCatalogPermissions.ITEMS_MANAGE;
        if (!authorized(context, permission)) {
            return rejected(context, permission, operation, ITEM_RESOURCE,
                    Optional.of(id.value().toString()), Optional.empty(),
                    CatalogResultCode.ACCESS_DENIED);
        }
        Optional<CatalogItem> found = items.findById(context.companyContext().companyId(), id);
        if (found.isEmpty()) {
            return rejected(context, permission, operation, ITEM_RESOURCE,
                    Optional.of(id.value().toString()), Optional.empty(), CatalogResultCode.NOT_FOUND);
        }
        CatalogItem item = found.orElseThrow();
        long previousVersion = item.version();
        try {
            mutation.accept(item);
            CatalogItem saved = item.version() == previousVersion
                    ? item
                    : items.update(item, previousVersion);
            succeeded(context, permission, operation, ITEM_RESOURCE, id.value().toString(),
                    previousVersion, saved.version());
            return CatalogOperationResult.success(saved.snapshot());
        } catch (ConcurrentCatalogChangeException failure) {
            return rejected(context, permission, operation, ITEM_RESOURCE,
                    Optional.of(id.value().toString()), Optional.of(previousVersion),
                    CatalogResultCode.VERSION_CONFLICT);
        } catch (CatalogPersistenceException failure) {
            return rejected(context, permission, operation, ITEM_RESOURCE,
                    Optional.of(id.value().toString()), Optional.of(previousVersion),
                    map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return rejected(context, permission, operation, ITEM_RESOURCE,
                    Optional.of(id.value().toString()), Optional.of(previousVersion),
                    CatalogResultCode.INVALID_OPERATION);
        }
    }

    private CatalogOperationResult<PriceListSnapshot> mutatePriceList(
            CatalogOperationContext context,
            String operation,
            PriceListId id,
            long expectedVersion,
            Consumer<PriceList> mutation) {
        ContributionId permission = CommercialCatalogPermissions.PRICES_MANAGE;
        if (!authorized(context, permission)) {
            return rejected(context, permission, operation, PRICE_LIST_RESOURCE,
                    Optional.of(id.value().toString()), Optional.empty(),
                    CatalogResultCode.ACCESS_DENIED);
        }
        Optional<PriceList> found = priceLists.findById(context.companyContext().companyId(), id);
        if (found.isEmpty()) {
            return rejected(context, permission, operation, PRICE_LIST_RESOURCE,
                    Optional.of(id.value().toString()), Optional.empty(), CatalogResultCode.NOT_FOUND);
        }
        PriceList priceList = found.orElseThrow();
        long previousVersion = priceList.version();
        try {
            mutation.accept(priceList);
            PriceList saved = priceList.version() == previousVersion
                    ? priceList
                    : priceLists.update(priceList, previousVersion);
            succeeded(context, permission, operation, PRICE_LIST_RESOURCE, id.value().toString(),
                    previousVersion, saved.version());
            return CatalogOperationResult.success(saved.snapshot());
        } catch (ConcurrentCatalogChangeException failure) {
            return rejected(context, permission, operation, PRICE_LIST_RESOURCE,
                    Optional.of(id.value().toString()), Optional.of(previousVersion),
                    CatalogResultCode.VERSION_CONFLICT);
        } catch (CatalogPersistenceException failure) {
            return rejected(context, permission, operation, PRICE_LIST_RESOURCE,
                    Optional.of(id.value().toString()), Optional.of(previousVersion),
                    map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return rejected(context, permission, operation, PRICE_LIST_RESOURCE,
                    Optional.of(id.value().toString()), Optional.of(previousVersion),
                    CatalogResultCode.INVALID_OPERATION);
        }
    }

    private void succeeded(
            CatalogOperationContext context,
            ContributionId permission,
            String operation,
            String resourceType,
            String resourceId,
            long previousVersion,
            long resultingVersion) {
        TechnicalAuditOutcome outcome = previousVersion == resultingVersion
                ? TechnicalAuditOutcome.UNCHANGED
                : TechnicalAuditOutcome.CHANGED;
        record(context, permission, operation, resourceType, Optional.of(resourceId), outcome,
                CatalogResultCode.SUCCESS, Optional.of(previousVersion),
                Optional.of(resultingVersion));
    }

    private void changed(
            CatalogOperationContext context,
            ContributionId permission,
            String operation,
            String resourceType,
            String resourceId,
            Optional<Long> previousVersion,
            long resultingVersion) {
        record(context, permission, operation, resourceType, Optional.of(resourceId),
                TechnicalAuditOutcome.CHANGED, CatalogResultCode.SUCCESS, previousVersion,
                Optional.of(resultingVersion));
    }

    private <T> CatalogOperationResult<T> rejected(
            CatalogOperationContext context,
            ContributionId permission,
            String operation,
            String resourceType,
            Optional<String> resourceId,
            Optional<Long> previousVersion,
            CatalogResultCode code) {
        record(context, permission, operation, resourceType, resourceId,
                TechnicalAuditOutcome.REJECTED, code, previousVersion, Optional.empty());
        return CatalogOperationResult.failure(code);
    }

    private void record(
            CatalogOperationContext context,
            ContributionId permission,
            String operation,
            String resourceType,
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
                permission.value(),
                resourceType,
                resourceId,
                code.name(),
                previousVersion,
                resultingVersion,
                context.correlationId(),
                clock.instant()));
    }

    private static boolean authorized(
            CatalogOperationContext context, ContributionId permission) {
        return Objects.requireNonNull(context, "context").authorizes(permission);
    }

    private static CatalogResultCode map(CatalogPersistenceCode code) {
        return switch (code) {
            case ITEM_NOT_FOUND, PRICE_LIST_NOT_FOUND, DEFINITION_NOT_FOUND ->
                    CatalogResultCode.NOT_FOUND;
            case VERSION_CONFLICT -> CatalogResultCode.VERSION_CONFLICT;
            case CODE_CONFLICT -> CatalogResultCode.CODE_CONFLICT;
            case IDENTIFIER_CONFLICT -> CatalogResultCode.IDENTIFIER_CONFLICT;
            case REFERENCE_CONFLICT -> CatalogResultCode.REFERENCE_CONFLICT;
            case VALIDITY_CONFLICT -> CatalogResultCode.VALIDITY_CONFLICT;
            case UNKNOWN_CONFLICT -> CatalogResultCode.INVALID_OPERATION;
        };
    }
}
