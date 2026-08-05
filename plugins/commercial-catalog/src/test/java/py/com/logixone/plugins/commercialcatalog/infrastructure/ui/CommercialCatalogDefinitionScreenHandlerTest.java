package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
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
import py.com.logixone.plugins.commercialcatalog.domain.BrandId;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.TagId;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;

class CommercialCatalogDefinitionScreenHandlerTest {

    private static final CompanyId COMPANY = new CompanyId(new UUID(0, 101));
    private RecordingAuthorization authorization;
    private RecordingUseCases recording;
    private CommercialCatalogDefinitionScreenHandler handler;

    @BeforeEach
    void setUp() {
        authorization = new RecordingAuthorization();
        recording = new RecordingUseCases();
        handler = new CommercialCatalogDefinitionScreenHandler();
        handler.authorization = authorization;
        handler.useCases = recording.proxy();
    }

    @Test
    void loadsAndFiltersAllSimpleDefinitionsWithDefinitionsPermission() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.empty(),
                Map.of(CommercialCatalogScreenContract.DEFINITION_SEARCH_KIND, "CATEGORY"),
                Optional.empty(), Optional.empty()));

        assertEquals(List.of(CommercialCatalogPermissions.DEFINITIONS_MANAGE.value()),
                authorization.permissions);
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertEquals("Categoría", result.table().orElseThrow().rows().getFirst().cells().getFirst());
        assertEquals(9, result.options().size());
        assertTrue(result.options().get(
                CommercialCatalogScreenContract.DEFINITION_SEARCH_KIND).stream()
                .anyMatch(option -> option.value().equals("TAG")));
    }

    @Test
    void registersCategoryWithOptionalParentAndOpensCreatedDetail() {
        UUID parent = new UUID(0, 302);
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_DEFINITION),
                Map.of(
                        CommercialCatalogScreenContract.DEFINITION_NEW_KIND, "CATEGORY",
                        CommercialCatalogScreenContract.DEFINITION_NEW_CODE, "BEBIDAS",
                        CommercialCatalogScreenContract.DEFINITION_NEW_NAME, "Bebidas",
                        CommercialCatalogScreenContract.DEFINITION_CATEGORY_PARENT,
                                parent.toString()),
                Optional.empty(), Optional.empty()));

        assertTrue(recording.invocations.contains("registerCategory"));
        assertEquals(Optional.of(new CategoryId(parent)), recording.registeredParent);
        assertEquals("Bebidas", result.detail().orElseThrow().title());
        assertTrue(result.notices().stream().anyMatch(
                notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
    }

    @Test
    void rejectsAnInvalidUnitScaleWithoutCallingTheUseCase() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_DEFINITION),
                Map.of(
                        CommercialCatalogScreenContract.DEFINITION_NEW_KIND, "UNIT",
                        CommercialCatalogScreenContract.DEFINITION_NEW_CODE, "CAJA",
                        CommercialCatalogScreenContract.DEFINITION_NEW_NAME, "Caja",
                        CommercialCatalogScreenContract.DEFINITION_UNIT_SCALE, "NO_NUMERICO"),
                Optional.empty(), Optional.empty()));

        assertTrue(recording.invocations.stream().noneMatch("registerUnit"::equals));
        assertEquals(ScreenInteraction.NoticeLevel.ERROR, result.notices().getFirst().level());
        assertTrue(result.notices().getFirst().detail().contains("0 a 12"));
    }

    @Test
    void registersTagAndOpensCreatedDetail() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REGISTER_DEFINITION),
                Map.of(
                        CommercialCatalogScreenContract.DEFINITION_NEW_KIND, "TAG",
                        CommercialCatalogScreenContract.DEFINITION_NEW_CODE, "TEMPORADA",
                        CommercialCatalogScreenContract.DEFINITION_NEW_NAME, "Temporada"),
                Optional.empty(), Optional.empty()));

        assertTrue(recording.invocations.contains("registerTag"));
        assertEquals("Temporada", result.detail().orElseThrow().title());
        assertTrue(result.notices().stream().anyMatch(
                notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
    }

    @Test
    void inactivatesASelectedDefinitionAndReloadsItsNewVersion() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.INACTIVATE_DEFINITION),
                Map.of(), Optional.of("UNIT:UN"), Optional.of(0L)));

        assertTrue(recording.invocations.contains("changeSimpleDefinitionState"));
        assertEquals(CatalogDefinitions.State.INACTIVE, recording.units.getFirst().state());
        assertEquals(Optional.of(1L), result.selectedResourceVersion());
        assertTrue(result.detail().orElseThrow().items().stream().anyMatch(item ->
                item.label().equals("Estado") && item.value().equals("Inactivo")));
        assertTrue(result.notices().stream().anyMatch(notice ->
                notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
    }

    @Test
    void revisesASelectedUnitWithoutChangingIdentityAndShowsItsHistory() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REVISE_DEFINITION),
                Map.of(
                        CommercialCatalogScreenContract.DEFINITION_REVISION_NAME,
                                "Unidad fraccionable",
                        CommercialCatalogScreenContract.DEFINITION_REVISION_UNIT_SCALE, "3"),
                Optional.of("UNIT:UN"), Optional.of(0L)));

        assertTrue(recording.invocations.contains("reviseSimpleDefinition"));
        assertEquals("UN", recording.revisedCommand.orElseThrow().identity());
        assertEquals("Unidad fraccionable", recording.units.getFirst().displayName());
        assertEquals(3, recording.units.getFirst().decimalScale());
        assertEquals(Optional.of(1L), result.selectedResourceVersion());
        assertEquals(CommercialCatalogScreenContract.DEFINITION_HISTORY,
                result.table().orElseThrow().elementId());
        assertEquals(List.of("1", "0"), result.table().orElseThrow().rows().stream()
                .map(row -> row.cells().getFirst()).toList());
        assertTrue(result.table().orElseThrow().rows().getFirst().cells()
                .contains("Unidad fraccionable"));
        assertTrue(result.notices().stream().anyMatch(notice ->
                notice.level() == ScreenInteraction.NoticeLevel.SUCCESS));
    }

    @Test
    void revisesASelectedCategoryAndExcludesItFromItsOwnParentOptions() {
        String categoryId = new UUID(0, 302).toString();
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REVISE_DEFINITION),
                Map.of(
                        CommercialCatalogScreenContract.DEFINITION_REVISION_NAME,
                                "Alimentos y bebidas",
                        CommercialCatalogScreenContract.DEFINITION_REVISION_CATEGORY_PARENT,
                                "NONE"),
                Optional.of("CATEGORY:" + categoryId), Optional.of(0L)));

        CatalogDefinitionCommands.ReviseSimpleDefinition command =
                recording.revisedCommand.orElseThrow();
        assertEquals(CatalogDefinitions.SimpleKind.CATEGORY, command.kind());
        assertTrue(command.parentId().isEmpty());
        assertEquals("Alimentos y bebidas", recording.categories.getFirst().displayName());
        assertTrue(result.options().get(
                CommercialCatalogScreenContract.DEFINITION_REVISION_CATEGORY_PARENT)
                .stream().noneMatch(option -> option.value().equals(categoryId)));
        assertEquals(CommercialCatalogScreenContract.DEFINITION_HISTORY,
                result.table().orElseThrow().elementId());
        assertEquals(Optional.of(1L), result.selectedResourceVersion());
    }

    @Test
    void replacesAUnitAndKeepsThePreviousDefinitionLinkedAndInactive() {
        ScreenInteraction.Result result = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.REPLACE_DEFINITION),
                Map.of(
                        CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_CODE, "UN2",
                        CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_NAME,
                                "Unidad sucesora",
                        CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_UNIT_SCALE, "2"),
                Optional.of("UNIT:UN"), Optional.of(0L)));

        assertTrue(recording.invocations.contains("replaceSimpleDefinition"));
        assertEquals("UN2", recording.replacedCommand.orElseThrow().replacementCode());
        assertEquals(Optional.of("UNIT:UN2"), result.selectedResourceId());
        assertEquals(Optional.of(0L), result.selectedResourceVersion());
        assertEquals(CatalogDefinitions.State.INACTIVE, recording.units.getFirst().state());
        assertEquals(CatalogDefinitions.State.ACTIVE, recording.units.get(1).state());
        assertTrue(result.notices().stream().anyMatch(notice ->
                notice.summary().equals("Definición reemplazada")));

        ScreenInteraction.Result previous = handler.interact(new ScreenInteraction.Request(
                Optional.of(CommercialCatalogScreenContract.SELECT_DEFINITION),
                Map.of(), Optional.of("UNIT:UN"), Optional.empty()));
        assertTrue(previous.detail().orElseThrow().items().stream().anyMatch(item ->
                item.label().equals("Reemplazada por")
                        && item.value().contains("Unidad sucesora")));
    }

    private static final class RecordingAuthorization implements CurrentCompanyAuthorization {
        private final List<String> permissions = new ArrayList<>();

        @Override
        public AuthorizedCompanyOperation require(String pluginId, String permissionId) {
            permissions.add(permissionId);
            return new AuthorizedCompanyOperation(
                    new AuthenticatedCompanyContext(
                            new AuthenticatedActor(new AppUserId(new UUID(0, 501))), COMPANY),
                    pluginId, permissionId, "ui:catalog-definition-test");
        }
    }

    private static final class RecordingUseCases implements InvocationHandler {
        private final List<String> invocations = new ArrayList<>();
        private final List<CatalogDefinitions.Unit> units = new ArrayList<>(List.of(
                new CatalogDefinitions.Unit(
                        new UnitCode("UN"), "Unidad", 0,
                        CatalogDefinitions.State.ACTIVE, 0)));
        private final List<CatalogDefinitions.Category> categories = new ArrayList<>(List.of(
                new CatalogDefinitions.Category(
                        new CategoryId(new UUID(0, 302)), Optional.empty(),
                        "ALIMENTOS", "Alimentos", CatalogDefinitions.State.ACTIVE, 0)));
        private final List<CatalogDefinitions.Brand> brands = new ArrayList<>(List.of(
                new CatalogDefinitions.Brand(
                        new BrandId(new UUID(0, 303)), "GENERICO", "Genérico",
                        CatalogDefinitions.State.ACTIVE, 0)));
        private final List<CatalogDefinitions.Tag> tags = new ArrayList<>(List.of(
                new CatalogDefinitions.Tag(
                        new TagId(new UUID(0, 306)), "NOVEDAD", "Novedad",
                        CatalogDefinitions.State.ACTIVE, 0)));
        private Optional<CategoryId> registeredParent = Optional.empty();
        private Optional<CatalogDefinitionCommands.ReviseSimpleDefinition> revisedCommand =
                Optional.empty();
        private Optional<CatalogDefinitionCommands.ReplaceSimpleDefinition> replacedCommand =
                Optional.empty();
        private final List<CatalogDefinitions.ReplacementLink> replacements = new ArrayList<>();
        private final Map<String, List<CatalogDefinitions.SimpleRevision>> histories =
                new java.util.HashMap<>();

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
                case "registerUnit" -> registerUnit(
                        (CatalogDefinitionCommands.RegisterUnit) args[1]);
                case "registerCategory" -> registerCategory(
                        (CatalogDefinitionCommands.RegisterCategory) args[1]);
                case "registerBrand" -> registerBrand(
                        (CatalogDefinitionCommands.RegisterBrand) args[1]);
                case "registerTag" -> registerTag(
                        (CatalogDefinitionCommands.RegisterTag) args[1]);
                case "changeSimpleDefinitionState" -> changeState(
                        (CatalogDefinitionCommands.ChangeSimpleState) args[1]);
                case "reviseSimpleDefinition" -> revise(
                        (CatalogDefinitionCommands.ReviseSimpleDefinition) args[1]);
                case "replaceSimpleDefinition" -> replace(
                        (CatalogDefinitionCommands.ReplaceSimpleDefinition) args[1]);
                case "simpleDefinitionHistory" -> history(
                        (CatalogDefinitions.SimpleKind) args[1], (String) args[2]);
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private CatalogOperationResult<CatalogDefinitions.Lifecycle> changeState(
                CatalogDefinitionCommands.ChangeSimpleState command) {
            CatalogDefinitions.Unit current = units.getFirst();
            long version = command.expectedVersion() + 1;
            CatalogDefinitions.Unit changed = new CatalogDefinitions.Unit(
                    current.code(), current.displayName(), current.decimalScale(),
                    command.targetState(), version);
            units.set(0, changed);
            histories.put(key(command.kind(), command.identity()), List.of(
                    revision(changed, true), revision(current, false)));
            return CatalogOperationResult.success(new CatalogDefinitions.Lifecycle(
                    command.kind(), command.identity(), command.targetState(), version, true));
        }

        private CatalogOperationResult<CatalogDefinitions.SimpleRevision> revise(
                CatalogDefinitionCommands.ReviseSimpleDefinition command) {
            revisedCommand = Optional.of(command);
            return switch (command.kind()) {
                case UNIT -> reviseUnit(command);
                case CATEGORY -> reviseCategory(command);
                case BRAND -> reviseBrand(command);
                case TAG -> reviseTag(command);
            };
        }

        private CatalogOperationResult<CatalogDefinitions.Replacement> replace(
                CatalogDefinitionCommands.ReplaceSimpleDefinition command) {
            replacedCommand = Optional.of(command);
            if (command.kind() != CatalogDefinitions.SimpleKind.UNIT) {
                throw new UnsupportedOperationException(command.kind().name());
            }
            CatalogDefinitions.Unit previous = units.stream()
                    .filter(unit -> unit.code().value().equals(command.identity()))
                    .findFirst().orElseThrow();
            CatalogDefinitions.Unit inactive = new CatalogDefinitions.Unit(
                    previous.code(), previous.displayName(), previous.decimalScale(),
                    CatalogDefinitions.State.INACTIVE, command.expectedVersion() + 1);
            CatalogDefinitions.Unit successor = new CatalogDefinitions.Unit(
                    new UnitCode(command.replacementCode()), command.replacementDisplayName(),
                    command.replacementDecimalScale().orElseThrow(),
                    CatalogDefinitions.State.ACTIVE, 0);
            units.set(units.indexOf(previous), inactive);
            units.add(successor);
            replacements.add(new CatalogDefinitions.ReplacementLink(
                    command.kind(), command.identity(), successor.code().value()));
            histories.put(key(command.kind(), command.identity()), List.of(
                    revision(inactive, true), revision(previous, false)));
            histories.put(key(command.kind(), successor.code().value()),
                    List.of(revision(successor, true)));
            return CatalogOperationResult.success(new CatalogDefinitions.Replacement(
                    command.kind(), command.identity(), inactive.version(),
                    successor.code().value(), successor.code().value(), successor.version()));
        }

        private CatalogOperationResult<CatalogDefinitions.SimpleRevision> reviseUnit(
                CatalogDefinitionCommands.ReviseSimpleDefinition command) {
            CatalogDefinitions.Unit previous = units.stream()
                    .filter(unit -> unit.code().value().equals(command.identity()))
                    .findFirst().orElseThrow();
            CatalogDefinitions.Unit revised = new CatalogDefinitions.Unit(
                    previous.code(), command.displayName(), command.decimalScale().orElseThrow(),
                    previous.state(), command.expectedVersion() + 1);
            units.set(units.indexOf(previous), revised);
            return revised(command, revision(revised, true), revision(previous, false));
        }

        private CatalogOperationResult<CatalogDefinitions.SimpleRevision> reviseCategory(
                CatalogDefinitionCommands.ReviseSimpleDefinition command) {
            CatalogDefinitions.Category previous = categories.stream()
                    .filter(category -> category.id().value().toString()
                            .equals(command.identity()))
                    .findFirst().orElseThrow();
            CatalogDefinitions.Category revised = new CatalogDefinitions.Category(
                    previous.id(), command.parentId(), previous.code(), command.displayName(),
                    previous.state(), command.expectedVersion() + 1);
            categories.set(categories.indexOf(previous), revised);
            return revised(command, revision(revised, true), revision(previous, false));
        }

        private CatalogOperationResult<CatalogDefinitions.SimpleRevision> reviseBrand(
                CatalogDefinitionCommands.ReviseSimpleDefinition command) {
            CatalogDefinitions.Brand previous = brands.stream()
                    .filter(brand -> brand.id().value().toString().equals(command.identity()))
                    .findFirst().orElseThrow();
            CatalogDefinitions.Brand revised = new CatalogDefinitions.Brand(
                    previous.id(), previous.code(), command.displayName(),
                    previous.state(), command.expectedVersion() + 1);
            brands.set(brands.indexOf(previous), revised);
            return revised(command, revision(revised, true), revision(previous, false));
        }

        private CatalogOperationResult<CatalogDefinitions.SimpleRevision> reviseTag(
                CatalogDefinitionCommands.ReviseSimpleDefinition command) {
            CatalogDefinitions.Tag previous = tags.stream()
                    .filter(tag -> tag.id().value().toString().equals(command.identity()))
                    .findFirst().orElseThrow();
            CatalogDefinitions.Tag revised = new CatalogDefinitions.Tag(
                    previous.id(), previous.code(), command.displayName(),
                    previous.state(), command.expectedVersion() + 1);
            tags.set(tags.indexOf(previous), revised);
            return revised(command, revision(revised, true), revision(previous, false));
        }

        private CatalogOperationResult<CatalogDefinitions.SimpleRevision> revised(
                CatalogDefinitionCommands.ReviseSimpleDefinition command,
                CatalogDefinitions.SimpleRevision current,
                CatalogDefinitions.SimpleRevision previous) {
            histories.put(key(command.kind(), command.identity()), List.of(current, previous));
            return CatalogOperationResult.success(current);
        }

        private CatalogOperationResult<List<CatalogDefinitions.SimpleRevision>> history(
                CatalogDefinitions.SimpleKind kind, String identity) {
            return CatalogOperationResult.success(histories.computeIfAbsent(
                    key(kind, identity), ignored -> List.of(currentRevision(kind, identity))));
        }

        private CatalogDefinitions.SimpleRevision currentRevision(
                CatalogDefinitions.SimpleKind kind, String identity) {
            return switch (kind) {
                case UNIT -> revision(units.stream()
                        .filter(unit -> unit.code().value().equals(identity))
                        .findFirst().orElseThrow(), true);
                case CATEGORY -> {
                    CatalogDefinitions.Category value = categories.stream()
                            .filter(category -> category.id().value().toString().equals(identity))
                            .findFirst().orElseThrow();
                    yield new CatalogDefinitions.SimpleRevision(
                            kind, identity, value.version(), value.displayName(),
                            Optional.empty(), value.parentId(), value.state(), true);
                }
                case BRAND -> {
                    CatalogDefinitions.Brand value = brands.stream()
                            .filter(brand -> brand.id().value().toString().equals(identity))
                            .findFirst().orElseThrow();
                    yield new CatalogDefinitions.SimpleRevision(
                            kind, identity, value.version(), value.displayName(),
                            Optional.empty(), Optional.empty(), value.state(), true);
                }
                case TAG -> {
                    CatalogDefinitions.Tag value = tags.stream()
                            .filter(tag -> tag.id().value().toString().equals(identity))
                            .findFirst().orElseThrow();
                    yield new CatalogDefinitions.SimpleRevision(
                            kind, identity, value.version(), value.displayName(),
                            Optional.empty(), Optional.empty(), value.state(), true);
                }
            };
        }

        private static CatalogDefinitions.SimpleRevision revision(
                CatalogDefinitions.Unit unit, boolean current) {
            return new CatalogDefinitions.SimpleRevision(
                    CatalogDefinitions.SimpleKind.UNIT,
                    unit.code().value(), unit.version(), unit.displayName(),
                    Optional.of(unit.decimalScale()), Optional.empty(),
                    unit.state(), current);
        }

        private static CatalogDefinitions.SimpleRevision revision(
                CatalogDefinitions.Category category, boolean current) {
            return new CatalogDefinitions.SimpleRevision(
                    CatalogDefinitions.SimpleKind.CATEGORY,
                    category.id().value().toString(), category.version(),
                    category.displayName(), Optional.empty(), category.parentId(),
                    category.state(), current);
        }

        private static CatalogDefinitions.SimpleRevision revision(
                CatalogDefinitions.Brand brand, boolean current) {
            return new CatalogDefinitions.SimpleRevision(
                    CatalogDefinitions.SimpleKind.BRAND,
                    brand.id().value().toString(), brand.version(), brand.displayName(),
                    Optional.empty(), Optional.empty(), brand.state(), current);
        }

        private static CatalogDefinitions.SimpleRevision revision(
                CatalogDefinitions.Tag tag, boolean current) {
            return new CatalogDefinitions.SimpleRevision(
                    CatalogDefinitions.SimpleKind.TAG,
                    tag.id().value().toString(), tag.version(), tag.displayName(),
                    Optional.empty(), Optional.empty(), tag.state(), current);
        }

        private static String key(CatalogDefinitions.SimpleKind kind, String identity) {
            return kind.name() + ":" + identity;
        }

        private CatalogOperationResult<CatalogDefinitions.Unit> registerUnit(
                CatalogDefinitionCommands.RegisterUnit command) {
            CatalogDefinitions.Unit created = new CatalogDefinitions.Unit(
                    command.code(), command.displayName(), command.decimalScale(),
                    CatalogDefinitions.State.ACTIVE, 0);
            units.add(created);
            return CatalogOperationResult.success(created);
        }

        private CatalogOperationResult<CatalogDefinitions.Category> registerCategory(
                CatalogDefinitionCommands.RegisterCategory command) {
            registeredParent = command.parentId();
            CatalogDefinitions.Category created = new CatalogDefinitions.Category(
                    new CategoryId(new UUID(0, 304)), command.parentId(), command.code(),
                    command.displayName(), CatalogDefinitions.State.ACTIVE, 0);
            categories.add(created);
            return CatalogOperationResult.success(created);
        }

        private CatalogOperationResult<CatalogDefinitions.Brand> registerBrand(
                CatalogDefinitionCommands.RegisterBrand command) {
            CatalogDefinitions.Brand created = new CatalogDefinitions.Brand(
                    new BrandId(new UUID(0, 305)), command.code(), command.displayName(),
                    CatalogDefinitions.State.ACTIVE, 0);
            brands.add(created);
            return CatalogOperationResult.success(created);
        }

        private CatalogOperationResult<CatalogDefinitions.Tag> registerTag(
                CatalogDefinitionCommands.RegisterTag command) {
            CatalogDefinitions.Tag created = new CatalogDefinitions.Tag(
                    new TagId(new UUID(0, 307)), command.code(), command.displayName(),
                    CatalogDefinitions.State.ACTIVE, 0);
            tags.add(created);
            return CatalogOperationResult.success(created);
        }

        private CatalogDefinitions.Snapshot snapshot() {
            return new CatalogDefinitions.Snapshot(
                    units, categories, brands, tags, List.of(), List.of(), replacements);
        }
    }
}
