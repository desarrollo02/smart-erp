package py.com.logixone.plugin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScreenInteractionTest {

    private static final PluginId PLUGIN = new PluginId("sample_plugin");
    private static final ScreenElementId QUERY = new ScreenElementId("query");
    private static final ScreenElementId RESULTS = new ScreenElementId("results");

    @Test
    void copiesInteractiveValuesAndKeepsResourceVersionPaired() {
        var table = new ScreenInteraction.Table(
                RESULTS,
                List.of(new ScreenInteraction.Column("code", "Código")),
                List.of(new ScreenInteraction.Row("resource-1", List.of("BP-1"))),
                1,
                "Sin resultados",
                "Ajusta los filtros.");
        var result = new ScreenInteraction.Result(
                Map.of(QUERY, "acme"),
                Map.of(QUERY, List.of(new ScreenInteraction.Option("all", "Todos"))),
                Optional.of(table),
                Optional.of(new ScreenInteraction.Detail(
                        "resource-1",
                        "ACME",
                        List.of(new ScreenInteraction.DetailItem("Estado", "Activo")))),
                List.of(new ScreenInteraction.Notice(
                        ScreenInteraction.NoticeLevel.SUCCESS, "Guardado", "Cambio confirmado.")),
                Optional.of("resource-1"),
                Optional.of(3L));

        assertEquals("acme", result.inputs().get(QUERY));
        assertEquals(1, result.table().orElseThrow().rows().size());
        assertThrows(UnsupportedOperationException.class,
                () -> result.inputs().put(QUERY, "changed"));
        assertThrows(IllegalArgumentException.class, () -> new ScreenInteraction.Result(
                Map.of(), Map.of(), Optional.empty(), Optional.empty(), List.of(),
                Optional.of("resource-1"), Optional.empty()));
    }

    @Test
    void rejectsMalformedTablesAndUnboundedInput() {
        assertThrows(IllegalArgumentException.class, () -> new ScreenInteraction.Table(
                RESULTS,
                List.of(new ScreenInteraction.Column("code", "Código")),
                List.of(new ScreenInteraction.Row("resource-1", List.of("BP-1", "extra"))),
                1,
                "Sin resultados",
                "Ajusta los filtros."));
        assertThrows(IllegalArgumentException.class, () -> ScreenInteraction.Request.load(
                Map.of(QUERY, "x".repeat(2049))));
        assertThrows(IllegalArgumentException.class,
                () -> new ScreenInteraction.TablePageRequest(0, 51));
        assertThrows(IllegalArgumentException.class,
                () -> new ScreenInteraction.SelectorOptionPage(List.of(), 0, 0, 51));
    }

    @Test
    void carriesBoundedTableAndSelectorPagesWithoutBreakingExistingConstructors() {
        var request = new ScreenInteraction.Request(
                Optional.empty(),
                Map.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new ScreenInteraction.TablePageRequest(50, 50)));
        var page = new ScreenInteraction.SelectorOptionPage(
                List.of(new ScreenInteraction.Option("PY", "Paraguay")),
                248,
                0,
                50);
        var table = new ScreenInteraction.Table(
                RESULTS,
                List.of(new ScreenInteraction.Column("code", "Código")),
                List.of(new ScreenInteraction.Row("COUNTRY:PY", List.of("PY"))),
                248,
                "Sin resultados",
                "Ajusta los filtros.",
                Optional.of(new ScreenInteraction.TablePage(0, 50)));

        assertEquals(50, request.tablePage().orElseThrow().limit());
        assertEquals(248, page.total());
        assertEquals(50, table.page().orElseThrow().limit());
    }

    @Test
    void handlerIdentityRemainsAFrameworkNeutralScreenId() {
        ScreenInteraction.Handler handler = new ScreenInteraction.Handler() {
            @Override
            public ScreenId screenId() {
                return new ScreenId(PLUGIN, "directory");
            }

            @Override
            public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
                return new ScreenInteraction.Result(
                        request.inputs(), Map.of(), Optional.empty(), Optional.empty(), List.of(),
                        Optional.empty(), Optional.empty());
            }
        };

        assertEquals("sample_plugin:directory", handler.screenId().toString());
        assertTrue(handler.selectorSources().isEmpty());
    }

    @Test
    void selectorSourceDeclaresOwnerPoliciesAndAuthorizedManagement() {
        var source = new SelectorSourceDefinition(
                new SelectorSourceId("sample_plugin.tax_profiles"),
                PLUGIN,
                SelectorSourceKind.BUSINESS_CATALOG,
                SemanticVersion.parse("1.0.0"),
                Optional.of("/catalog/tax-profiles"),
                Optional.of(new ContributionId("sample_plugin.tax_profiles.manage")),
                Set.of(
                        SelectorManagementCapability.VIEW,
                        SelectorManagementCapability.CREATE,
                        SelectorManagementCapability.INACTIVATE),
                SelectorEmptyOptionPolicy.NOT_ALLOWED,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE);

        assertTrue(source.manageable());
        assertEquals("sample_plugin", source.ownerPluginId().value());
        assertEquals(SelectorSourceOwnerKind.PLUGIN, source.owner().kind());
        assertEquals("sample_plugin", source.owner().id());
        assertEquals("/catalog/tax-profiles", source.managementRoute().orElseThrow());
        assertThrows(UnsupportedOperationException.class,
                () -> source.managementCapabilities().add(SelectorManagementCapability.EDIT));
    }

    @Test
    void selectorSourceRejectsUngovernedCatalogsAndManagementForClosedStates() {
        assertThrows(IllegalArgumentException.class, () -> new SelectorSourceDefinition(
                new SelectorSourceId("sample_plugin.tax_profiles"),
                PLUGIN,
                SelectorSourceKind.BUSINESS_CATALOG,
                SemanticVersion.parse("1.0.0"),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                SelectorEmptyOptionPolicy.NOT_ALLOWED,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE));

        assertThrows(IllegalArgumentException.class, () -> new SelectorSourceDefinition(
                new SelectorSourceId("sample_plugin.states"),
                PLUGIN,
                SelectorSourceKind.CLOSED_STATE,
                SemanticVersion.parse("1.0.0"),
                Optional.of("/states"),
                Optional.of(new ContributionId("sample_plugin.states.manage")),
                Set.of(SelectorManagementCapability.VIEW),
                SelectorEmptyOptionPolicy.MEANS_ALL,
                SelectorInactiveValuePolicy.NOT_APPLICABLE,
                SelectorLoadingStrategy.INLINE));

        var closed = new SelectorSourceDefinition(
                new SelectorSourceId("sample_plugin.states"),
                PLUGIN,
                SelectorSourceKind.CLOSED_STATE,
                SemanticVersion.parse("1.0.0"),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                SelectorEmptyOptionPolicy.MEANS_ALL,
                SelectorInactiveValuePolicy.NOT_APPLICABLE,
                SelectorLoadingStrategy.INLINE);
        assertFalse(closed.manageable());
    }

    @Test
    void platformSelectorSourceDeclaresKernelOwnershipWithoutCreatingAFakePlugin() {
        var source = new PlatformSelectorSourceDefinition(
                new SelectorSourceId("kernel.companies"),
                SelectorSourceOwner.platform("kernel"),
                SelectorSourceKind.OPERATIONAL_REFERENCE,
                SemanticVersion.parse("1.0.0"),
                Optional.of("/admin/companies.xhtml"),
                Optional.of(new ContributionId("kernel.company.manage")),
                Set.of(SelectorManagementCapability.VIEW),
                SelectorEmptyOptionPolicy.NOT_ALLOWED,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE);

        assertTrue(source.manageable());
        assertEquals(SelectorSourceOwnerKind.PLATFORM, source.owner().kind());
        assertEquals("kernel", source.owner().id());
        assertThrows(IllegalArgumentException.class, () -> new PlatformSelectorSourceDefinition(
                source.id(),
                SelectorSourceOwner.plugin(PLUGIN),
                source.kind(),
                source.sourceVersion(),
                source.managementRoute(),
                source.managementPermission(),
                source.managementCapabilities(),
                source.emptyOptionPolicy(),
                source.inactiveValuePolicy(),
                source.loadingStrategy()));
    }
}
