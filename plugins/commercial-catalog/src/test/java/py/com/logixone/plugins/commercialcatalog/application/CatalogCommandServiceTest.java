package py.com.logixone.plugins.commercialcatalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogIdGenerator;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogItemRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.CurrencyReferencePolicy;
import py.com.logixone.plugins.commercialcatalog.application.port.PriceListRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.VariantFamilyAssignmentRepository;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchPage;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogDetailId;
import py.com.logixone.plugins.commercialcatalog.domain.BrandId;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.TagId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItem;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemCode;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemName;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogVariant;
import py.com.logixone.plugins.commercialcatalog.domain.PriceList;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListName;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileReference;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantValueType;

class CatalogCommandServiceTest {

    private static final CompanyId COMPANY_A = company(1);
    private static final CatalogItemId ITEM_ID = itemId(10);
    private static final PriceListId PRICE_LIST_ID = priceListId(20);
    private static final VariantFamilyId FAMILY_ID = new VariantFamilyId(new UUID(0, 40));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-30T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void registersItemWithCompanySequenceAndAuditsOnlyTechnicalData() {
        MemoryItems items = new MemoryItems();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogCommandService service = service(items, new MemoryPriceLists(), audit);

        var result = service.registerItem(context(CommercialCatalogPermissions.ITEMS_MANAGE),
                new CatalogCommands.RegisterItem(
                        Optional.empty(),
                        new CatalogItemName("Servicio confidencial"),
                        "Descripción privada",
                        CatalogItemType.SERVICE,
                        Set.of(CatalogItemScope.SALE),
                        new UnitCode("UN"),
                        taxProfile()));

        assertTrue(result.successful());
        assertEquals("ITEM-00000001", result.value().orElseThrow().code().value());
        assertEquals(1, items.insertions);
        assertEquals("REGISTER_CATALOG_ITEM", audit.getFirst().operation());
        assertEquals("catalog_item", audit.getFirst().resourceType());
        assertEquals(Optional.of(ITEM_ID.value().toString()), audit.getFirst().resourceId());
        assertFalse(audit.getFirst().toString().contains("Servicio confidencial"));
        assertFalse(audit.getFirst().toString().contains("Descripción privada"));
    }

    @Test
    void rejectsWrongPluginBeforeGeneratingIdentityOrTouchingRepositories() {
        MemoryItems items = new MemoryItems();
        CountingIds ids = new CountingIds();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogCommandService service = new CatalogCommandService(
                items, new MemoryPriceLists(), (company, family) -> Optional.empty(),
                (company, scope) -> 1, ids, (company, currency) -> true, audit::add, CLOCK);
        CatalogOperationContext wrongPlugin = new CatalogOperationContext(
                authenticated(), new PluginId("inventory"),
                CommercialCatalogPermissions.ITEMS_MANAGE, "request:catalog-denied");

        var result = service.registerItem(wrongPlugin, registerItem());

        assertEquals(CatalogResultCode.ACCESS_DENIED, result.code());
        assertEquals(0, ids.calls);
        assertEquals(0, items.insertions);
        assertEquals("ACCESS_DENIED", audit.getFirst().resultCode());
    }

    @Test
    void optimisticConflictPreservesItemAndProducesRejectedAudit() {
        MemoryItems items = new MemoryItems();
        items.insert(newItem());
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogCommandService service = service(items, new MemoryPriceLists(), audit);

        var result = service.reviseItem(context(CommercialCatalogPermissions.ITEMS_MANAGE),
                new CatalogCommands.ReviseItem(
                        ITEM_ID, 7, new CatalogItemCode("NEW"),
                        new CatalogItemName("Nuevo nombre"), "",
                        Set.of(CatalogItemScope.SALE)));

        assertEquals(CatalogResultCode.VERSION_CONFLICT, result.code());
        assertEquals("Nombre inicial", items.findById(COMPANY_A, ITEM_ID)
                .orElseThrow().name().value());
        assertEquals("VERSION_CONFLICT", audit.getFirst().resultCode());
    }

    @Test
    void priceListsRequireTheirDedicatedPermissionAndUseTheirOwnSequence() {
        MemoryPriceLists prices = new MemoryPriceLists();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogCommandService service = service(new MemoryItems(), prices, audit);
        CatalogCommands.RegisterPriceList command = new CatalogCommands.RegisterPriceList(
                Optional.empty(), new PriceListName("Minorista"), "PYG",
                CatalogTaxMode.TAX_INCLUDED, 0, RoundingMode.HALF_UP);

        var denied = service.registerPriceList(
                context(CommercialCatalogPermissions.ITEMS_MANAGE), command);
        var allowed = service.registerPriceList(
                context(CommercialCatalogPermissions.PRICES_MANAGE), command);

        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
        assertTrue(allowed.successful());
        assertEquals("PRICE-00000001", allowed.value().orElseThrow().code().value());
        assertEquals(1, prices.insertions);
    }

    @Test
    void rejectsDisabledCurrencyBeforeGeneratingPriceListIdentity() {
        MemoryPriceLists prices = new MemoryPriceLists();
        CountingIds ids = new CountingIds();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogCommandService service = new CatalogCommandService(
                new MemoryItems(), prices, (company, family) -> Optional.empty(),
                (company, scope) -> 1, ids, (company, currency) -> false,
                audit::add, CLOCK);

        var result = service.registerPriceList(
                context(CommercialCatalogPermissions.PRICES_MANAGE),
                new CatalogCommands.RegisterPriceList(
                        Optional.empty(), new PriceListName("Moneda deshabilitada"), "USD",
                        CatalogTaxMode.NET, 2, RoundingMode.HALF_EVEN));

        assertEquals(CatalogResultCode.INVALID_OPERATION, result.code());
        assertEquals(0, ids.calls);
        assertEquals(0, prices.insertions);
        assertEquals("INVALID_OPERATION", audit.getFirst().resultCode());
    }

    @Test
    void assignsTheCurrentActiveFamilyAndNormalizesTypedValues() {
        MemoryItems items = new MemoryItems();
        items.insert(newItem());
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogCommandService service = service(
                items, new MemoryPriceLists(), audit, activeFamilyDirectory());

        var result = service.assignVariant(context(CommercialCatalogPermissions.ITEMS_MANAGE),
                new CatalogCommands.AssignVariant(
                        ITEM_ID,
                        0,
                        FAMILY_ID,
                        2,
                        Map.of(
                                new VariantAttributeCode("COLOR"), "Azul",
                                new VariantAttributeCode("NUMERO"), "42.00")));

        assertTrue(result.successful());
        CatalogVariant assigned = result.value().orElseThrow().variant().orElseThrow();
        assertEquals(2, assigned.familyVersion());
        assertEquals("42", assigned.attributes()
                .get(new VariantAttributeCode("NUMERO")).value());
        assertEquals("ASSIGN_CATALOG_ITEM_VARIANT", audit.getLast().operation());
        assertEquals("CHANGED", audit.getLast().outcome().name());
    }

    @Test
    void rejectsInactiveStaleOrStructurallyInvalidFamilyAssignments() {
        MemoryItems items = new MemoryItems();
        items.insert(newItem());
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitions.VariantFamily inactive = family(
                CatalogDefinitions.State.INACTIVE, 2);
        CatalogCommandService inactiveService = service(
                items, new MemoryPriceLists(), audit,
                (company, id) -> Optional.of(inactive));

        var inactiveResult = inactiveService.assignVariant(
                context(CommercialCatalogPermissions.ITEMS_MANAGE),
                assignment(2, Map.of(new VariantAttributeCode("COLOR"), "Azul")));

        assertEquals(CatalogResultCode.REFERENCE_CONFLICT, inactiveResult.code());
        assertTrue(items.findById(COMPANY_A, ITEM_ID).orElseThrow().snapshot().variant().isEmpty());

        CatalogCommandService activeService = service(
                items, new MemoryPriceLists(), audit, activeFamilyDirectory());
        var staleResult = activeService.assignVariant(
                context(CommercialCatalogPermissions.ITEMS_MANAGE),
                assignment(1, Map.of(new VariantAttributeCode("COLOR"), "Azul")));
        var missingRequired = activeService.assignVariant(
                context(CommercialCatalogPermissions.ITEMS_MANAGE),
                assignment(2, Map.of(new VariantAttributeCode("NUMERO"), "10")));
        var wrongType = activeService.assignVariant(
                context(CommercialCatalogPermissions.ITEMS_MANAGE),
                assignment(2, Map.of(new VariantAttributeCode("COLOR"), "Azul",
                        new VariantAttributeCode("NUMERO"), "no-es-numero")));
        var unknownAttribute = activeService.assignVariant(
                context(CommercialCatalogPermissions.ITEMS_MANAGE),
                assignment(2, Map.of(
                        new VariantAttributeCode("COLOR"), "Azul",
                        new VariantAttributeCode("OTRO"), "No declarado")));

        assertEquals(CatalogResultCode.REFERENCE_CONFLICT, staleResult.code());
        assertEquals(CatalogResultCode.INVALID_OPERATION, missingRequired.code());
        assertEquals(CatalogResultCode.INVALID_OPERATION, wrongType.code());
        assertEquals(CatalogResultCode.INVALID_OPERATION, unknownAttribute.code());
    }

    private static CatalogCommands.RegisterItem registerItem() {
        return new CatalogCommands.RegisterItem(
                Optional.of(new CatalogItemCode("MANUAL-1")),
                new CatalogItemName("Nombre"), "", CatalogItemType.PRODUCT,
                Set.of(CatalogItemScope.PURCHASE, CatalogItemScope.SALE),
                new UnitCode("UN"), taxProfile());
    }

    private static CatalogCommandService service(
            MemoryItems items,
            MemoryPriceLists prices,
            List<TechnicalAuditEvent> audit) {
        return service(items, prices, audit, activeFamilyDirectory());
    }

    private static CatalogCommandService service(
            MemoryItems items,
            MemoryPriceLists prices,
            List<TechnicalAuditEvent> audit,
            VariantFamilyAssignmentRepository variantFamilies) {
        return new CatalogCommandService(
                items, prices, variantFamilies, (company, scope) -> 1,
                new CountingIds(), (company, currency) -> true, audit::add, CLOCK);
    }

    private static VariantFamilyAssignmentRepository activeFamilyDirectory() {
        return (company, id) -> company.equals(COMPANY_A) && id.equals(FAMILY_ID)
                ? Optional.of(family(CatalogDefinitions.State.ACTIVE, 2))
                : Optional.empty();
    }

    private static CatalogDefinitions.VariantFamily family(
            CatalogDefinitions.State state, long version) {
        return new CatalogDefinitions.VariantFamily(
                FAMILY_ID,
                "ROPA",
                "Ropa",
                List.of(
                        new CatalogDefinitions.VariantAttribute(
                                new VariantAttributeCode("COLOR"), "Color",
                                VariantValueType.TEXT, true, 0),
                        new CatalogDefinitions.VariantAttribute(
                                new VariantAttributeCode("NUMERO"), "Número",
                                VariantValueType.NUMBER, false, 1)),
                state,
                version);
    }

    private static CatalogCommands.AssignVariant assignment(
            long familyVersion, Map<VariantAttributeCode, String> attributes) {
        return new CatalogCommands.AssignVariant(
                ITEM_ID, 0, FAMILY_ID, familyVersion, attributes);
    }

    private static CatalogOperationContext context(ContributionId permission) {
        return new CatalogOperationContext(
                authenticated(), CommercialCatalogIdentity.PLUGIN_ID, permission,
                "request:catalog-test");
    }

    private static AuthenticatedCompanyContext authenticated() {
        return new AuthenticatedCompanyContext(
                new AuthenticatedActor(new AppUserId(new UUID(0, 99))), COMPANY_A);
    }

    private static CatalogItem newItem() {
        return CatalogItem.create(
                COMPANY_A, ITEM_ID, new CatalogItemCode("ITEM-1"),
                new CatalogItemName("Nombre inicial"), "", CatalogItemType.PRODUCT,
                Set.of(CatalogItemScope.SALE), new UnitCode("UN"), taxProfile());
    }

    private static TaxProfileReference taxProfile() {
        return new TaxProfileReference(new TaxProfileId(new UUID(0, 30)), 0);
    }

    private static CompanyId company(long suffix) {
        return new CompanyId(new UUID(0, suffix));
    }

    private static CatalogItemId itemId(long suffix) {
        return new CatalogItemId(new UUID(0, suffix));
    }

    private static PriceListId priceListId(long suffix) {
        return new PriceListId(new UUID(0, suffix));
    }

    private static final class CountingIds implements CatalogIdGenerator {
        private int calls;

        @Override
        public CatalogItemId nextItemId() {
            calls++;
            return ITEM_ID;
        }

        @Override
        public CatalogDetailId nextDetailId() {
            calls++;
            return new CatalogDetailId(new UUID(0, 11));
        }

        @Override
        public PriceListId nextPriceListId() {
            calls++;
            return PRICE_LIST_ID;
        }

        @Override
        public PriceEntryId nextPriceEntryId() {
            calls++;
            return new PriceEntryId(new UUID(0, 21));
        }

        @Override public CategoryId nextCategoryId() { calls++; return new CategoryId(new UUID(0, 31)); }
        @Override public BrandId nextBrandId() { calls++; return new BrandId(new UUID(0, 32)); }
        @Override public TagId nextTagId() { calls++; return new TagId(new UUID(0, 33)); }
        @Override public TaxProfileId nextTaxProfileId() { calls++; return new TaxProfileId(new UUID(0, 34)); }
        @Override public VariantFamilyId nextVariantFamilyId() { calls++; return new VariantFamilyId(new UUID(0, 35)); }
    }

    private static final class MemoryItems implements CatalogItemRepository {
        private final Map<String, CatalogItem> values = new HashMap<>();
        private int insertions;

        @Override
        public Optional<CatalogItem> findById(CompanyId companyId, CatalogItemId itemId) {
            return Optional.ofNullable(values.get(key(companyId, itemId)));
        }

        @Override
        public CatalogItem insert(CatalogItem item) {
            insertions++;
            values.put(key(item.companyId(), item.id()), item);
            return item;
        }

        @Override
        public CatalogSearchPage search(CompanyId companyId, CatalogSearchCriteria criteria) {
            var references = values.values().stream()
                    .filter(item -> item.companyId().equals(companyId))
                    .map(CatalogItem::reference)
                    .toList();
            return new CatalogSearchPage(
                    references, references.size(), criteria.offset(), criteria.limit());
        }

        @Override
        public CatalogItem update(CatalogItem item, long expectedPersistedVersion) {
            values.put(key(item.companyId(), item.id()), item);
            return item;
        }

        private static String key(CompanyId companyId, CatalogItemId itemId) {
            return companyId + ":" + itemId;
        }
    }

    private static final class MemoryPriceLists implements PriceListRepository {
        private final Map<String, PriceList> values = new HashMap<>();
        private int insertions;

        @Override
        public Optional<PriceList> findById(CompanyId companyId, PriceListId priceListId) {
            return Optional.ofNullable(values.get(companyId + ":" + priceListId));
        }

        @Override
        public PriceListSearchPage search(
                CompanyId companyId, PriceListSearchCriteria criteria) {
            return new PriceListSearchPage(List.of(), 0, criteria.offset(), criteria.limit());
        }

        @Override
        public PriceList insert(PriceList priceList) {
            insertions++;
            values.put(priceList.companyId() + ":" + priceList.id(), priceList);
            return priceList;
        }

        @Override
        public PriceList update(PriceList priceList, long expectedPersistedVersion) {
            values.put(priceList.companyId() + ":" + priceList.id(), priceList);
            return priceList;
        }
    }
}
