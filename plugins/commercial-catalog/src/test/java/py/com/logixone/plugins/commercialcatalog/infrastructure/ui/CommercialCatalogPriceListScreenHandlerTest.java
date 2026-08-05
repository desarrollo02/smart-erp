package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogScreenContract;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationResult;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogUseCases;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchPage;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSummary;
import py.com.logixone.plugins.commercialcatalog.domain.PriceEntry;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListCode;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListName;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListState;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.referencedata.api.CountryCode;
import py.com.logixone.plugins.referencedata.api.CountryReference;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.CurrencyReference;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.referencedata.api.ReferenceDataRelease;

class CommercialCatalogPriceListScreenHandlerTest {

    private static final CompanyId COMPANY = new CompanyId(
            UUID.fromString("00000000-0000-0000-0000-000000000101"));
    private static final CatalogItemId ITEM_ID = CatalogItemId.parse(
            "00000000-0000-0000-0000-000000000201");
    private static final PriceListId LIST_ID = PriceListId.parse(
            "00000000-0000-0000-0000-000000000301");

    private RecordingAuthorization authorization;
    private RecordingUseCases recording;
    private CommercialCatalogPriceListScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        recording = new RecordingUseCases();
        handler = new CommercialCatalogPriceListScreenHandler();
        handler.authorization = authorization;
        handler.useCases = recording.proxy();
        handler.referenceDataDirectory = new FakeReferenceDataDirectory();
    }

    @Test
    void loadsListDirectoryOptionsAndDetailUsingViewPermission() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(), Map.of(), Optional.of(LIST_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals("Lista general", result.detail().orElseThrow().title());
        assertEquals(null, result.inputs().get(CommercialCatalogScreenContract.PRICE_CURRENCY));
        assertEquals(List.of(), result.options()
                .get(CommercialCatalogScreenContract.PRICE_CURRENCY).stream()
                .map(ScreenInteraction.Option::value)
                .toList());
        assertEquals(ITEM_ID.toString(), result.inputs().get(
                CommercialCatalogScreenContract.PRICE_ENTRY_ITEM));
    }

    @Test
    void searchesCurrencyOptionsOnDemandWithA50OptionCeiling() {
        ScreenInteraction.SelectorOptionPage page = handler.searchOptions(
                new ScreenInteraction.SelectorOptionRequest(
                        CommercialCatalogScreenContract.PRICE_CURRENCY,
                        "dollar",
                        0,
                        50));

        assertEquals(List.of("USD"), page.options().stream()
                .map(ScreenInteraction.Option::value).toList());
        assertEquals(1, page.total());
        assertEquals(List.of(CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
    }

    @Test
    void registrationUsesPricePermissionThenRefreshesThroughView() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_PRICE_LIST),
                Map.of(
                        CommercialCatalogScreenContract.PRICE_NEW_CODE, "PRICE-2",
                        CommercialCatalogScreenContract.PRICE_NEW_NAME, "Mayorista",
                        CommercialCatalogScreenContract.PRICE_CURRENCY, "PYG",
                        CommercialCatalogScreenContract.PRICE_TAX_MODE, "TAX_INCLUDED",
                        CommercialCatalogScreenContract.PRICE_SCALE, "0",
                        CommercialCatalogScreenContract.PRICE_ROUNDING_MODE, "HALF_UP"),
                Optional.empty(), Optional.empty()));

        assertEquals(List.of(
                CommercialCatalogPermissions.PRICES_MANAGE.value(),
                CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertTrue(result.notices().stream().anyMatch(
                notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
        assertEquals("Mayorista", result.detail().orElseThrow().title());
        assertTrue(recording.invocations.contains("registerPriceList"));
    }

    @Test
    void addingAnEntryUsesPricePermissionAndExposesTheConfirmedPrice() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.ADD_PRICE_ENTRY),
                Map.of(
                        CommercialCatalogScreenContract.PRICE_ENTRY_ITEM, ITEM_ID.toString(),
                        CommercialCatalogScreenContract.PRICE_ENTRY_UNIT, "EA",
                        CommercialCatalogScreenContract.PRICE_ENTRY_MINIMUM, "1",
                        CommercialCatalogScreenContract.PRICE_ENTRY_AMOUNT, "125000",
                        CommercialCatalogScreenContract.PRICE_ENTRY_VALID_FROM,
                        "2026-07-30T12:00:00Z"),
                Optional.of(LIST_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(
                CommercialCatalogPermissions.PRICES_MANAGE.value(),
                CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1L, result.selectedResourceVersion().orElseThrow());
        assertTrue(result.detail().orElseThrow().items().stream().anyMatch(
                item -> item.label().equals("Entradas activas") && item.value().equals("1")));
    }

    @Test
    void invalidPriceEntryIsRejectedBeforePriceAuthorization() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.ADD_PRICE_ENTRY),
                Map.of(CommercialCatalogScreenContract.PRICE_ENTRY_AMOUNT, "no-es-numero"),
                Optional.of(LIST_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
        assertTrue(recording.invocations.stream().noneMatch(name -> name.equals("addPriceEntry")));
    }

    private static final class RecordingAuthorization implements CurrentCompanyAuthorization {
        private final List<String> permissions = new ArrayList<>();

        @Override
        public AuthorizedCompanyOperation require(String pluginId, String permissionId) {
            permissions.add(permissionId);
            return new AuthorizedCompanyOperation(
                    new AuthenticatedCompanyContext(
                            new AuthenticatedActor(new AppUserId(UUID.fromString(
                                    "00000000-0000-0000-0000-000000000501"))),
                            COMPANY),
                    pluginId,
                    permissionId,
                    "ui:test");
        }
    }

    private static final class RecordingUseCases implements InvocationHandler {
        private final List<String> invocations = new ArrayList<>();
        private PriceListSnapshot priceList = snapshot(
                LIST_ID, "PRICE-1", "Lista general", List.of(), 0);

        CommercialCatalogUseCases proxy() {
            return (CommercialCatalogUseCases) Proxy.newProxyInstance(
                    CommercialCatalogUseCases.class.getClassLoader(),
                    new Class<?>[]{CommercialCatalogUseCases.class},
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            invocations.add(method.getName());
            return switch (method.getName()) {
                case "definitions" -> CatalogOperationResult.success(definitions());
                case "search" -> CatalogOperationResult.success(new CatalogSearchPage(
                        List.of(item()), 1, 0, 100));
                case "priceLists" -> CatalogOperationResult.success(new PriceListSearchPage(
                        List.of(summary(priceList)), 1, 0, 20));
                case "priceListDetail" -> CatalogOperationResult.success(priceList);
                case "registerPriceList" -> register((CatalogCommands.RegisterPriceList) args[1]);
                case "addPriceEntry" -> addEntry((CatalogCommands.AddPriceEntry) args[1]);
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private CatalogOperationResult<PriceListSnapshot> register(
                CatalogCommands.RegisterPriceList command) {
            priceList = new PriceListSnapshot(
                    COMPANY,
                    PriceListId.parse("00000000-0000-0000-0000-000000000302"),
                    command.code().orElse(new PriceListCode("PRICE-2")),
                    command.name(), command.currency(), command.taxMode(), command.scale(),
                    command.roundingMode(), PriceListState.ACTIVE, List.of(), 0);
            return CatalogOperationResult.success(priceList);
        }

        private CatalogOperationResult<PriceListSnapshot> addEntry(
                CatalogCommands.AddPriceEntry command) {
            PriceEntry entry = PriceEntry.active(
                    PriceEntryId.parse("00000000-0000-0000-0000-000000000401"),
                    command.itemId(), command.unit(), command.minimumQuantity(), command.amount(),
                    command.validFrom(), command.validUntil());
            priceList = new PriceListSnapshot(
                    priceList.companyId(), priceList.id(), priceList.code(), priceList.name(),
                    priceList.currency(), priceList.taxMode(), priceList.scale(),
                    priceList.roundingMode(), priceList.state(), List.of(entry),
                    priceList.version() + 1);
            return CatalogOperationResult.success(priceList);
        }
    }

    private static final class FakeReferenceDataDirectory implements ReferenceDataDirectory {

        @Override
        public ReferenceDataRelease currentRelease(
                CompanyId companyId, ReferenceDataCatalog catalog) {
            throw new UnsupportedOperationException("Not needed by the price-list screen test");
        }

        @Override
        public List<CountryReference> countries(CompanyId companyId) {
            return List.of();
        }

        @Override
        public Optional<CountryReference> findCountry(CompanyId companyId, CountryCode code) {
            return Optional.empty();
        }

        @Override
        public List<CurrencyReference> currencies(CompanyId companyId) {
            return List.of(
                    new CurrencyReference(
                            new CurrencyCode("PYG"), "600", 0, "Guaraní", "iso-4217-2026-08-04", true),
                    new CurrencyReference(
                            new CurrencyCode("USD"), "840", 2, "US Dollar", "iso-4217-2026-08-04", true));
        }

        @Override
        public Optional<CurrencyReference> findCurrency(CompanyId companyId, CurrencyCode code) {
            return currencies(companyId).stream()
                    .filter(currency -> currency.code().equals(code)).findFirst();
        }
    }

    private static CatalogDefinitions.Snapshot definitions() {
        return new CatalogDefinitions.Snapshot(
                List.of(new CatalogDefinitions.Unit(
                        new UnitCode("EA"), "Unidad", 0, CatalogDefinitions.State.ACTIVE, 0)),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static CatalogItemReference item() {
        return new CatalogItemReference(
                ITEM_ID, "ITEM-1", "Producto demo", CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE, Set.of(CatalogItemScope.SALE), "EA", 0);
    }

    private static PriceListSnapshot snapshot(
            PriceListId id,
            String code,
            String name,
            List<PriceEntry> entries,
            long version) {
        return new PriceListSnapshot(
                COMPANY, id, new PriceListCode(code), new PriceListName(name), "PYG",
                CatalogTaxMode.TAX_INCLUDED, 0, RoundingMode.HALF_UP,
                PriceListState.ACTIVE, entries, version);
    }

    private static PriceListSummary summary(PriceListSnapshot snapshot) {
        long active = snapshot.entries().stream().filter(PriceEntry::active).count();
        return new PriceListSummary(
                snapshot.id(), snapshot.code(), snapshot.name(), snapshot.currency(),
                snapshot.taxMode(), snapshot.state(), snapshot.entries().size(), active,
                snapshot.version());
    }
}
