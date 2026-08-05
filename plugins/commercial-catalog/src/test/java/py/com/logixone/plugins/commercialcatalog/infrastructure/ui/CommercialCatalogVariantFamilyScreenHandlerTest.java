package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationResult;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogUseCases;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogDefinitionCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantValueType;

class CommercialCatalogVariantFamilyScreenHandlerTest {

    private static final CompanyId COMPANY = new CompanyId(new UUID(0, 101));
    private RecordingAuthorization authorization;
    private RecordingUseCases recording;
    private CommercialCatalogVariantFamilyScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        recording = new RecordingUseCases();
        handler = new CommercialCatalogVariantFamilyScreenHandler();
        handler.authorization = authorization;
        handler.useCases = recording.proxy();
    }

    @Test
    void loadsAndFiltersFamiliesWithGovernedSelectors() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(),
                Map.of(CommercialCatalogScreenContract.VARIANT_FAMILY_SEARCH_TEXT, "ropa"),
                Optional.empty(), Optional.empty()));

        assertEquals(List.of(CommercialCatalogPermissions.DEFINITIONS_MANAGE.value()),
                authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals("Ropa", result.table().orElseThrow().rows().getFirst().cells().get(1));
        assertEquals(5, result.options().size());
        assertEquals(CommercialCatalogSelectorSources.VARIANT_FAMILIES,
                handler.selectorSources());
    }

    @Test
    void buildsAnOrderedDraftAndRegistersItWithoutLosingAttributes() {
        ScreenInteraction.Result first = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.ADD_VARIANT_ATTRIBUTE),
                Map.of(
                        CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_CODE, "CALZADO",
                        CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_NAME, "Calzado",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_CODE, "COLOR",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_NAME, "Color",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE, "TEXT",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED, "REQUIRED"),
                Optional.empty(), Optional.empty()));

        Map<py.com.logixone.plugin.api.ScreenElementId, String> secondInputs =
                new java.util.LinkedHashMap<>(first.inputs());
        secondInputs.put(CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_CODE, "TALLA");
        secondInputs.put(CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_NAME, "Talla");
        secondInputs.put(CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE, "NUMBER");
        secondInputs.put(CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED, "REQUIRED");
        ScreenInteraction.Result second = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.ADD_VARIANT_ATTRIBUTE),
                secondInputs, Optional.empty(), Optional.empty()));

        ScreenInteraction.Result registered = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_VARIANT_FAMILY),
                second.inputs(), Optional.empty(), Optional.empty()));

        assertTrue(first.inputs().get(CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_DRAFT)
                .contains("COLOR | Color | Texto | Obligatorio"));
        assertEquals(List.of("COLOR", "TALLA"), recording.registeredAttributes.stream()
                .map(attribute -> attribute.code().value()).toList());
        assertEquals(List.of(0, 1), recording.registeredAttributes.stream()
                .map(CatalogDefinitions.VariantAttribute::position).toList());
        assertEquals("Calzado", registered.detail().orElseThrow().title());
        assertEquals(ScreenInteraction.NoticeLevel.SUCCESS,
                registered.notices().getFirst().level());
    }

    @Test
    void includesACompleteCurrentAttributeWhenRegisteringDirectly() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_VARIANT_FAMILY),
                Map.of(
                        CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_CODE, "ACABADO",
                        CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_NAME, "Acabado",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_CODE, "MATE",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_NAME, "Acabado mate",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE, "BOOLEAN",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED, "OPTIONAL"),
                Optional.empty(), Optional.empty()));

        assertEquals(1, recording.registeredAttributes.size());
        assertEquals(VariantValueType.BOOLEAN,
                recording.registeredAttributes.getFirst().valueType());
        assertTrue(!recording.registeredAttributes.getFirst().required());
        assertEquals("Acabado", result.detail().orElseThrow().title());
    }

    @Test
    void rejectsARepeatedAttributeBeforeCallingRegistration() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_VARIANT_FAMILY),
                Map.of(
                        CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_CODE, "DUP",
                        CommercialCatalogScreenContract.VARIANT_FAMILY_NEW_NAME, "Duplicada",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_DRAFT,
                                "COLOR | Color | Texto | Obligatorio",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_CODE, "color",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_NAME, "Otro color",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE, "TEXT",
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED, "REQUIRED"),
                Optional.empty(), Optional.empty()));

        assertTrue(recording.invocations.stream().noneMatch("registerVariantFamily"::equals));
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
    }

    @Test
    void inactivatesAndReactivatesTheSelectedFamilyWithItsCurrentVersion() {
        String id = "VARIANT_FAMILY:" + new UUID(0, 301);
        ScreenInteraction.Result inactive = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.INACTIVATE_VARIANT_FAMILY),
                Map.of(), Optional.of(id), Optional.of(0L)));
        ScreenInteraction.Result active = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.ACTIVATE_VARIANT_FAMILY),
                Map.of(), Optional.of(id), inactive.selectedResourceVersion()));

        assertEquals(2, recording.invocations.stream()
                .filter("changeVariantFamilyState"::equals).count());
        assertEquals(1L, inactive.selectedResourceVersion().orElseThrow());
        assertEquals(2L, active.selectedResourceVersion().orElseThrow());
        assertTrue(inactive.detail().orElseThrow().items().stream().anyMatch(item ->
                item.label().equals("Estado") && item.value().equals("Inactiva")));
        assertTrue(active.detail().orElseThrow().items().stream().anyMatch(item ->
                item.label().equals("Estado") && item.value().equals("Activa")));
        assertEquals(1, active.detail().orElseThrow().items().stream()
                .filter(item -> item.label().startsWith("Atributo ")).count());
        assertTrue(active.notices().stream().anyMatch(notice ->
                notice.summary().equals("Familia de variantes reactivada")));
    }

    @Test
    void replacesTheDraftStructureAndShowsAppendOnlyFamilyHistory() {
        String selected = "VARIANT_FAMILY:" + new UUID(0, 301);
        ScreenInteraction.Result opened = handler.interact(new ScreenInteraction.Request(
                Optional.empty(), Map.of(), Optional.of(selected), Optional.of(0L)));
        ScreenInteraction.Result emptied = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REMOVE_VARIANT_REVISION_ATTRIBUTE),
                opened.inputs(), Optional.of(selected), Optional.of(0L)));
        Map<py.com.logixone.plugin.api.ScreenElementId, String> attributeInputs =
                new LinkedHashMap<>(emptied.inputs());
        attributeInputs.put(
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_CODE, "SIZE");
        attributeInputs.put(
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_NAME, "Size");
        attributeInputs.put(
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_TYPE, "NUMBER");
        attributeInputs.put(
                CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_REQUIRED, "REQUIRED");
        ScreenInteraction.Result drafted = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.ADD_VARIANT_REVISION_ATTRIBUTE),
                attributeInputs, Optional.of(selected), Optional.of(0L)));
        Map<py.com.logixone.plugin.api.ScreenElementId, String> revisionInputs =
                new LinkedHashMap<>(drafted.inputs());
        revisionInputs.put(
                CommercialCatalogScreenContract.VARIANT_FAMILY_REVISION_NAME,
                "Ropa por talla");

        ScreenInteraction.Result revised = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REVISE_VARIANT_FAMILY),
                revisionInputs, Optional.of(selected), Optional.of(0L)));

        assertEquals(CommercialCatalogScreenContract.VARIANT_FAMILY_HISTORY,
                opened.table().orElseThrow().elementId());
        assertTrue(opened.inputs().get(
                        CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_DRAFT)
                .contains("COLOR | Color | Texto | Obligatorio"));
        assertEquals(List.of("SIZE"), recording.revisedAttributes.stream()
                .map(attribute -> attribute.code().value()).toList());
        assertEquals("Ropa por talla", revised.detail().orElseThrow().title());
        assertEquals(1L, revised.selectedResourceVersion().orElseThrow());
        assertEquals(2, revised.table().orElseThrow().rows().size());
        assertEquals(List.of("1", "Actual", "Ropa por talla", "Activa",
                        "SIZE · Número · Obligatorio"),
                revised.table().orElseThrow().rows().getFirst().cells());
        assertTrue(revised.table().orElseThrow().rows().get(1).cells().get(4)
                .contains("COLOR · Texto · Obligatorio"));
        assertTrue(revised.notices().stream().anyMatch(notice ->
                notice.summary().equals("Nueva revisión de familia creada")));
    }

    private static final class RecordingAuthorization implements CurrentCompanyAuthorization {
        private final List<String> permissions = new ArrayList<>();

        @Override
        public AuthorizedCompanyOperation require(String pluginId, String permissionId) {
            permissions.add(permissionId);
            return new AuthorizedCompanyOperation(
                    new AuthenticatedCompanyContext(
                            new AuthenticatedActor(new AppUserId(new UUID(0, 501))), COMPANY),
                    pluginId, permissionId, "ui:variant-family-test");
        }
    }

    private static final class RecordingUseCases implements InvocationHandler {
        private final List<String> invocations = new ArrayList<>();
        private final List<CatalogDefinitions.VariantFamily> families = new ArrayList<>();
        private final Map<VariantFamilyId, List<CatalogDefinitions.VariantFamilyRevision>>
                histories = new LinkedHashMap<>();
        private List<CatalogDefinitions.VariantAttribute> registeredAttributes = List.of();
        private List<CatalogDefinitions.VariantAttribute> revisedAttributes = List.of();

        private RecordingUseCases() {
            CatalogDefinitions.VariantFamily initial =
                    family(new UUID(0, 301), "ROPA", "Ropa");
            families.add(initial);
            appendRevision(initial);
        }

        CommercialCatalogUseCases proxy() {
            return (CommercialCatalogUseCases) Proxy.newProxyInstance(
                    CommercialCatalogUseCases.class.getClassLoader(),
                    new Class<?>[]{CommercialCatalogUseCases.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            invocations.add(method.getName());
            return switch (method.getName()) {
                case "managedDefinitions" -> CatalogOperationResult.success(snapshot());
                case "registerVariantFamily" -> register(
                        (CatalogDefinitionCommands.RegisterVariantFamily) args[1]);
                case "changeVariantFamilyState" -> changeState(
                        (CatalogDefinitionCommands.ChangeVariantFamilyState) args[1]);
                case "reviseVariantFamily" -> revise(
                        (CatalogDefinitionCommands.ReviseVariantFamily) args[1]);
                case "variantFamilyHistory" -> history((VariantFamilyId) args[1]);
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private CatalogOperationResult<CatalogDefinitions.VariantFamily> changeState(
                CatalogDefinitionCommands.ChangeVariantFamilyState command) {
            for (int index = 0; index < families.size(); index++) {
                CatalogDefinitions.VariantFamily current = families.get(index);
                if (!current.id().equals(command.id())) {
                    continue;
                }
                CatalogDefinitions.VariantFamily changed =
                        new CatalogDefinitions.VariantFamily(
                                current.id(), current.code(), current.displayName(),
                                current.attributes(), command.targetState(),
                                command.expectedVersion() + 1);
                families.set(index, changed);
                appendRevision(changed);
                return CatalogOperationResult.success(changed);
            }
            return CatalogOperationResult.failure(
                    py.com.logixone.plugins.commercialcatalog.application.CatalogResultCode.NOT_FOUND);
        }

        private CatalogOperationResult<CatalogDefinitions.VariantFamily> register(
                CatalogDefinitionCommands.RegisterVariantFamily command) {
            registeredAttributes = command.attributes();
            CatalogDefinitions.VariantFamily created = new CatalogDefinitions.VariantFamily(
                    new VariantFamilyId(new UUID(0, families.size() + 302L)),
                    command.code(), command.displayName(), command.attributes(),
                    CatalogDefinitions.State.ACTIVE, 0);
            families.add(created);
            appendRevision(created);
            return CatalogOperationResult.success(created);
        }

        private CatalogOperationResult<CatalogDefinitions.VariantFamily> revise(
                CatalogDefinitionCommands.ReviseVariantFamily command) {
            for (int index = 0; index < families.size(); index++) {
                CatalogDefinitions.VariantFamily current = families.get(index);
                if (!current.id().equals(command.id())) {
                    continue;
                }
                revisedAttributes = command.attributes();
                CatalogDefinitions.VariantFamily revised =
                        new CatalogDefinitions.VariantFamily(
                                current.id(), current.code(), command.displayName(),
                                command.attributes(), current.state(),
                                command.expectedVersion() + 1);
                families.set(index, revised);
                appendRevision(revised);
                return CatalogOperationResult.success(revised);
            }
            return CatalogOperationResult.failure(
                    py.com.logixone.plugins.commercialcatalog.application.CatalogResultCode.NOT_FOUND);
        }

        private CatalogOperationResult<List<CatalogDefinitions.VariantFamilyRevision>> history(
                VariantFamilyId id) {
            return CatalogOperationResult.success(histories.getOrDefault(id, List.of()));
        }

        private void appendRevision(CatalogDefinitions.VariantFamily family) {
            List<CatalogDefinitions.VariantFamilyRevision> revisions =
                    histories.computeIfAbsent(family.id(), ignored -> new ArrayList<>());
            revisions.replaceAll(revision -> new CatalogDefinitions.VariantFamilyRevision(
                    revision.familyId(), revision.version(), revision.displayName(),
                    revision.attributes(), revision.state(), false));
            revisions.add(0, new CatalogDefinitions.VariantFamilyRevision(
                    family.id(), family.version(), family.displayName(), family.attributes(),
                    family.state(), true));
        }

        private CatalogDefinitions.Snapshot snapshot() {
            return new CatalogDefinitions.Snapshot(
                    List.of(), List.of(), List.of(), List.of(), List.of(), families);
        }

        private static CatalogDefinitions.VariantFamily family(
                UUID id, String code, String name) {
            return new CatalogDefinitions.VariantFamily(
                    new VariantFamilyId(id), code, name,
                    List.of(new CatalogDefinitions.VariantAttribute(
                            new VariantAttributeCode("COLOR"), "Color",
                            VariantValueType.TEXT, true, 0)),
                    CatalogDefinitions.State.ACTIVE, 0);
        }
    }
}
