package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationResult;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogUseCases;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemCode;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemName;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogVariant;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileReference;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeValue;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantValueType;

class CommercialCatalogItemScreenHandlerTest {

    private static final CompanyId COMPANY = new CompanyId(
            UUID.fromString("00000000-0000-0000-0000-000000000101"));
    private static final CatalogItemId ITEM_ID = CatalogItemId.parse(
            "00000000-0000-0000-0000-000000000201");
    private static final TaxProfileId TAX_ID = new TaxProfileId(
            UUID.fromString("00000000-0000-0000-0000-000000000301"));
    private static final CategoryId CATEGORY_ID = new CategoryId(
            UUID.fromString("00000000-0000-0000-0000-000000000401"));
    private static final VariantFamilyId FAMILY_ID = new VariantFamilyId(
            UUID.fromString("00000000-0000-0000-0000-000000000402"));

    private RecordingAuthorization authorization;
    private RecordingUseCases recording;
    private CommercialCatalogItemScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        recording = new RecordingUseCases();
        handler = new CommercialCatalogItemScreenHandler();
        handler.authorization = authorization;
        handler.useCases = recording.proxy();
    }

    @Test
    void loadsCompanyDefinitionsTableAndDetailUsingOnlyViewPermission() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(), Map.of(), Optional.of(ITEM_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals("Producto demo", result.detail().orElseThrow().title());
        assertEquals("EA", result.inputs().get(CommercialCatalogScreenContract.ITEM_NEW_BASE_UNIT));
        assertEquals(3, result.options().get(
                CommercialCatalogScreenContract.ITEM_SEARCH_TYPE).size());
        assertEquals(1, result.options().get(
                CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY).size());
        assertTrue(result.inputs().get(CommercialCatalogScreenContract.ITEM_VARIANT_STRUCTURE)
                .contains("COLOR"));
    }

    @Test
    void registrationUsesItemsPermissionThenRefreshesThroughView() {
        String taxReference = TAX_ID.value() + "|0";
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_ITEM),
                Map.of(
                        CommercialCatalogScreenContract.ITEM_NEW_CODE, "NEW-1",
                        CommercialCatalogScreenContract.ITEM_NEW_NAME, "Servicio demo",
                        CommercialCatalogScreenContract.ITEM_NEW_TYPE, "SERVICE",
                        CommercialCatalogScreenContract.ITEM_NEW_SCOPE, "SALE",
                        CommercialCatalogScreenContract.ITEM_NEW_BASE_UNIT, "EA",
                        CommercialCatalogScreenContract.ITEM_NEW_TAX_PROFILE, taxReference),
                Optional.empty(),
                Optional.empty()));

        assertEquals(List.of(
                CommercialCatalogPermissions.ITEMS_MANAGE.value(),
                CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertTrue(result.notices().stream().anyMatch(
                notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
        assertEquals("Servicio demo", result.detail().orElseThrow().title());
        assertTrue(recording.invocations.contains("registerItem"));
    }

    @Test
    void lifecycleUsesItemsPermissionAndReturnsTheNewVersion() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.INACTIVATE_ITEM),
                Map.of(), Optional.of(ITEM_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(
                CommercialCatalogPermissions.ITEMS_MANAGE.value(),
                CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertEquals(1L, result.selectedResourceVersion().orElseThrow());
        assertTrue(result.detail().orElseThrow().items().stream().anyMatch(
                item -> item.label().equals("Estado") && item.value().equals("Inactivo")));
    }

    @Test
    void invalidRegistrationIsRejectedBeforeItemsAuthorization() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_ITEM),
                Map.of(CommercialCatalogScreenContract.ITEM_NEW_TYPE, "PRODUCT"),
                Optional.empty(), Optional.empty()));

        assertEquals(List.of(CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
        assertTrue(recording.invocations.stream().noneMatch(name -> name.equals("registerItem")));
    }

    @Test
    void assignsTypedVariantValuesFromTheActiveCompanyFamily() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.ASSIGN_ITEM_VARIANT),
                Map.of(
                        CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY,
                        FAMILY_ID.value() + "|2",
                        CommercialCatalogScreenContract.ITEM_VARIANT_VALUES,
                        "COLOR=Azul; NUMERO=42.00"),
                Optional.of(ITEM_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(
                CommercialCatalogPermissions.ITEMS_MANAGE.value(),
                CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertTrue(recording.invocations.contains("assignVariant"));
        assertEquals("42.00", recording.lastVariantAssignment.attributes()
                .get(new VariantAttributeCode("NUMERO")));
        assertTrue(result.detail().orElseThrow().items().stream().anyMatch(
                item -> item.label().equals("Variante")
                        && item.value().contains("COLOR=Azul")
                        && item.value().contains("NUMERO=42")));
        assertTrue(result.options().get(CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY)
                .stream().noneMatch(option -> option.label().contains("Inactiva")));
    }

    @Test
    void rejectsAndNormalizesAMalformedVariantFamilySelectionBeforeAuthorization() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.ASSIGN_ITEM_VARIANT),
                Map.of(
                        CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY, "referencia-invalida",
                        CommercialCatalogScreenContract.ITEM_VARIANT_VALUES, "COLOR=Azul"),
                Optional.of(ITEM_ID.toString()), Optional.of(0L)));

        assertEquals(List.of(CommercialCatalogPermissions.VIEW.value()), authorization.permissions);
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
        assertTrue(recording.invocations.stream().noneMatch(name -> name.equals("assignVariant")));
        assertEquals(FAMILY_ID.value() + "|2",
                result.inputs().get(CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY));
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
        private CatalogItemSnapshot item = snapshot(
                ITEM_ID, "ITEM-1", "Producto demo", CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE, 0);
        private CatalogCommands.AssignVariant lastVariantAssignment;

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
                        List.of(reference(item)), 1, 0, 20));
                case "detail" -> CatalogOperationResult.success(item);
                case "registerItem" -> register((CatalogCommands.RegisterItem) args[1]);
                case "changeItemLifecycle" -> lifecycle(
                        (CatalogCommands.ChangeItemLifecycle) args[1]);
                case "assignVariant" -> assignVariant((CatalogCommands.AssignVariant) args[1]);
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private CatalogOperationResult<CatalogItemSnapshot> register(
                CatalogCommands.RegisterItem command) {
            item = new CatalogItemSnapshot(
                    COMPANY,
                    CatalogItemId.parse("00000000-0000-0000-0000-000000000202"),
                    command.code().orElse(new CatalogItemCode("ITEM-2")),
                    command.name(),
                    command.description(),
                    command.type(),
                    command.scopes(),
                    command.baseUnit(),
                    command.taxProfile(),
                    Optional.empty(), Optional.empty(), CatalogItemState.ACTIVE,
                    Optional.empty(), List.of(), List.of(), 0);
            return CatalogOperationResult.success(item);
        }

        private CatalogOperationResult<CatalogItemSnapshot> lifecycle(
                CatalogCommands.ChangeItemLifecycle command) {
            item = new CatalogItemSnapshot(
                    item.companyId(), item.id(), item.code(), item.name(), item.description(),
                    item.type(), item.scopes(), item.baseUnit(), item.taxProfile(),
                    item.classification(), item.variant(), command.state(), Optional.empty(),
                    item.identifiers(), item.conversions(), item.version() + 1);
            return CatalogOperationResult.success(item);
        }

        private CatalogOperationResult<CatalogItemSnapshot> assignVariant(
                CatalogCommands.AssignVariant command) {
            lastVariantAssignment = command;
            Map<VariantAttributeCode, VariantAttributeValue> attributes = command.attributes()
                    .entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> new VariantAttributeValue(
                                    entry.getKey().value().equals("NUMERO")
                                            ? VariantValueType.NUMBER
                                            : VariantValueType.TEXT,
                                    entry.getValue())));
            item = new CatalogItemSnapshot(
                    item.companyId(), item.id(), item.code(), item.name(), item.description(),
                    item.type(), item.scopes(), item.baseUnit(), item.taxProfile(),
                    item.classification(), Optional.of(new CatalogVariant(
                            command.familyId(), command.familyVersion(), attributes)),
                    item.state(), item.replacementId(), item.identifiers(), item.conversions(),
                    item.version() + 1);
            return CatalogOperationResult.success(item);
        }
    }

    private static CatalogDefinitions.Snapshot definitions() {
        return new CatalogDefinitions.Snapshot(
                List.of(new CatalogDefinitions.Unit(
                        new UnitCode("EA"), "Unidad", 0, CatalogDefinitions.State.ACTIVE, 0)),
                List.of(new CatalogDefinitions.Category(
                        CATEGORY_ID, Optional.empty(), "GENERAL", "General",
                        CatalogDefinitions.State.ACTIVE, 0)),
                List.of(),
                List.of(),
                List.of(new CatalogDefinitions.TaxProfile(
                        TAX_ID, "IVA10", "IVA general", "TAXED", "Perfil demo",
                        Instant.parse("2026-01-01T00:00:00Z"), Optional.empty(),
                        CatalogDefinitions.State.ACTIVE, 0)),
                List.of(
                        new CatalogDefinitions.VariantFamily(
                                FAMILY_ID, "ROPA", "Ropa", List.of(
                                        new CatalogDefinitions.VariantAttribute(
                                                new VariantAttributeCode("COLOR"), "Color",
                                                VariantValueType.TEXT, true, 0),
                                        new CatalogDefinitions.VariantAttribute(
                                                new VariantAttributeCode("NUMERO"), "Número",
                                                VariantValueType.NUMBER, false, 1)),
                                CatalogDefinitions.State.ACTIVE, 2),
                        new CatalogDefinitions.VariantFamily(
                                new VariantFamilyId(UUID.fromString(
                                        "00000000-0000-0000-0000-000000000403")),
                                "INACTIVA", "Inactiva", List.of(
                                        new CatalogDefinitions.VariantAttribute(
                                                new VariantAttributeCode("OTRO"), "Otro",
                                                VariantValueType.TEXT, true, 0)),
                                CatalogDefinitions.State.INACTIVE, 1)));
    }

    private static CatalogItemSnapshot snapshot(
            CatalogItemId id,
            String code,
            String name,
            CatalogItemType type,
            CatalogItemState state,
            long version) {
        return new CatalogItemSnapshot(
                COMPANY, id, new CatalogItemCode(code), new CatalogItemName(name), "",
                type, Set.of(CatalogItemScope.PURCHASE, CatalogItemScope.SALE),
                new UnitCode("EA"), new TaxProfileReference(TAX_ID, 0),
                Optional.empty(), Optional.empty(), state, Optional.empty(),
                List.of(), List.of(), version);
    }

    private static CatalogItemReference reference(CatalogItemSnapshot snapshot) {
        return new CatalogItemReference(
                snapshot.id(), snapshot.code().value(), snapshot.name().value(),
                snapshot.type(), snapshot.state(), snapshot.scopes(),
                snapshot.baseUnit().value(), snapshot.version());
    }
}
