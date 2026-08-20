package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorEmptyOptionPolicy;
import py.com.logixone.plugin.api.SelectorInactiveValuePolicy;
import py.com.logixone.plugin.api.SelectorLoadingStrategy;
import py.com.logixone.plugin.api.SelectorManagementCapability;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugin.api.SelectorSourceId;
import py.com.logixone.plugin.api.SelectorSourceKind;
import py.com.logixone.plugin.api.SemanticVersion;

class ShellScreenInteractionViewTest {

    private static final ScreenElementId TAX_PROFILE = new ScreenElementId("tax_profile");

    @Test
    void exposesOnlyAnAuthorizedManagementRouteToJsf() {
        var result = new ScreenInteraction.Result(
                Map.of(),
                Map.of(TAX_PROFILE, List.of(new ScreenInteraction.Option("general", "IVA general"))),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty());
        var source = new SelectorSourceDefinition(
                new SelectorSourceId("commercial_catalog.tax_profiles"),
                new PluginId("commercial_catalog"),
                SelectorSourceKind.BUSINESS_CATALOG,
                SemanticVersion.parse("1.0.0"),
                Optional.of("/catalog/tax-profiles"),
                Optional.of(new ContributionId("commercial_catalog.definitions.manage")),
                Set.of(
                        SelectorManagementCapability.VIEW,
                        SelectorManagementCapability.CREATE),
                SelectorEmptyOptionPolicy.NOT_ALLOWED,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE);

        ShellScreenInteractionView denied = ShellScreenInteractionView.from(
                result, Map.of(TAX_PROFILE, source), Set.of());
        ShellScreenInteractionView allowed = ShellScreenInteractionView.from(
                result, Map.of(TAX_PROFILE, source), Set.of(TAX_PROFILE));

        assertFalse(denied.getSelectorSources().get("tax_profile").isManagementAvailable());
        assertEquals("", denied.getSelectorSources().get("tax_profile").getManagementRoute());
        assertTrue(allowed.getSelectorSources().get("tax_profile").isManagementAvailable());
        assertEquals("/catalog/tax-profiles",
                allowed.getSelectorSources().get("tax_profile").getManagementRoute());
        assertEquals("Agregar o administrar",
                allowed.getSelectorSources().get("tax_profile").getManagementLabel());
    }

    @Test
    void exposesOnDemandLoadingAndBoundedTableNavigationToJsf() {
        var result = new ScreenInteraction.Result(
                Map.of(),
                Map.of(),
                Optional.of(new ScreenInteraction.Table(
                        new ScreenElementId("results"),
                        List.of(new ScreenInteraction.Column("code", "Código")),
                        List.of(new ScreenInteraction.Row("COUNTRY:PY", List.of("PY"))),
                        248,
                        "Sin resultados",
                        "Ajuste el filtro.",
                        Optional.of(new ScreenInteraction.TablePage(50, 50)))),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty());
        var country = new ScreenElementId("country");
        var source = new SelectorSourceDefinition(
                new SelectorSourceId("reference_data.countries"),
                new PluginId("reference_data"),
                SelectorSourceKind.NORMATIVE_CATALOG,
                SemanticVersion.parse("1.0.0"),
                Optional.of("/reference-data"),
                Optional.of(new ContributionId("reference_data.policy.manage")),
                Set.of(SelectorManagementCapability.VIEW),
                SelectorEmptyOptionPolicy.NOT_ALLOWED,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.SEARCH_ON_DEMAND);

        ShellScreenInteractionView view = ShellScreenInteractionView.from(
                result, Map.of(country, source), Set.of());

        assertTrue(view.getSelectorSources().get("country").isSearchOnDemand());
        assertTrue(view.getTable().isPaged());
        assertTrue(view.getTable().isHasPreviousPage());
        assertTrue(view.getTable().isHasNextPage());
        assertEquals(51, view.getTable().getFirstVisible());
        assertEquals(51, view.getTable().getLastVisible());
        assertEquals(50, view.getTable().getPageSize());
    }

    @Test
    void exposesDynamicStateAndBlocksUnavailableActions() {
        var action = new ScreenElementId("approve");
        var result = new ScreenInteraction.Result(
                Map.of(),
                Map.of(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Map.of(action, ScreenInteraction.ElementState.blocked(
                        "La solicitud ya no está pendiente")));

        ShellScreenInteractionView view = ShellScreenInteractionView.from(result);

        assertFalse(view.acceptsAction("approve"));
        assertTrue(view.acceptsAction("not_overridden"));
        assertEquals("La solicitud ya no está pendiente",
                view.getElementStates().get("approve").getUnavailableReason());
    }

    @Test
    void resolvesHumanReadableSelectorLabelsWithoutFallingBackToTechnicalValues() {
        var result = new ScreenInteraction.Result(
                Map.of(),
                Map.of(TAX_PROFILE, List.of(
                        new ScreenInteraction.Option("3a61df60-cc1e-4c7e-a19a-8cb495d5d496",
                                "IVA general"))),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty());

        ShellScreenInteractionView view = ShellScreenInteractionView.from(result);

        assertEquals("Ninguna", view.selectedOptionLabel("tax_profile", ""));
        assertEquals("IVA general", view.selectedOptionLabel(
                "tax_profile", "3a61df60-cc1e-4c7e-a19a-8cb495d5d496"));
        assertEquals("Selección no disponible",
                view.selectedOptionLabel("tax_profile", "technical-id-not-listed"));
    }
}
