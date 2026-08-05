package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.company.screen.ComposedScreenElement;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogPluginDefinition;
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogScreenContract;

class CommercialCatalogScreenRendererTest {

    @Test
    void rendersItemsAsASeparatedDirectoryCreateAndDetailJourney() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertEquals(CommercialCatalogScreenContract.ITEMS,
                registry.screenFor(
                        CommercialCatalogPluginDefinition.ID,
                        CommercialCatalogScreenContract.ITEMS_ROUTE).orElseThrow());

        ShellScreenView view = registry.render(
                composed(CommercialCatalogScreenContract.itemsDefinition()),
                new ShellTextCatalog()).orElseThrow();

        assertTrue(view.isInteractive());
        assertEquals("Artículos y servicios", view.getTitle());
        assertEquals("Nuevo artículo o servicio", view.getCreateTitle());
        assertEquals(2, view.getDirectorySections().size());
        assertEquals(7, view.getDetailSections().size());
        assertEquals(List.of(
                        "general", "identifiers", "classification", "units", "tax",
                        "variants", "lifecycle"),
                view.getDetailTabs().stream().map(ShellDetailTabView::getId).toList());
        assertTrue(view.acceptsAction(CommercialCatalogScreenContract.REGISTER_ITEM.value()));
        assertTrue(view.isCreateAction(CommercialCatalogScreenContract.REGISTER_ITEM.value()));
        assertTrue(view.isSearchAction(CommercialCatalogScreenContract.ITEM_SEARCH.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.ASSIGN_ITEM_VARIANT.value()));
    }

    @Test
    void rendersPriceListsAsAnIndependentAggregateJourney() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertEquals(CommercialCatalogScreenContract.PRICE_LISTS,
                registry.screenFor(
                        CommercialCatalogPluginDefinition.ID,
                        CommercialCatalogScreenContract.PRICE_LISTS_ROUTE).orElseThrow());

        ShellScreenView view = registry.render(
                composed(CommercialCatalogScreenContract.priceListsDefinition()),
                new ShellTextCatalog()).orElseThrow();

        assertEquals("Listas de precios", view.getTitle());
        assertEquals("Nueva lista", view.getNewActionLabel());
        assertEquals(3, view.getDetailSections().size());
        assertEquals(List.of("general", "entries", "lifecycle"),
                view.getDetailTabs().stream().map(ShellDetailTabView::getId).toList());
        assertTrue(view.acceptsDetailTab("entries"));
        assertFalse(view.acceptsDetailTab("roles"));
        assertTrue(view.acceptsAction(CommercialCatalogScreenContract.ADD_PRICE_ENTRY.value()));
    }

    @Test
    void rendersTaxProfilesAsAnAuthorizedMasterDataJourney() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertEquals(CommercialCatalogScreenContract.TAX_PROFILES,
                registry.screenFor(
                        CommercialCatalogPluginDefinition.ID,
                        CommercialCatalogScreenContract.TAX_PROFILES_ROUTE).orElseThrow());

        ShellScreenView view = registry.render(
                composed(CommercialCatalogScreenContract.taxProfilesDefinition()),
                new ShellTextCatalog()).orElseThrow();

        assertEquals("Perfiles tributarios", view.getTitle());
        assertEquals("Nuevo perfil", view.getNewActionLabel());
        assertEquals(2, view.getDirectorySections().size());
        assertEquals(3, view.getDetailSections().size());
        assertEquals(List.of("Historial", "Nueva revisión", "Estado"),
                view.getDetailTabs().stream().map(ShellDetailTabView::getLabel).toList());
        assertEquals(List.of("history", "revision", "lifecycle"),
                view.getDetailTabs().stream().map(ShellDetailTabView::getId).toList());
        assertTrue(view.getDetailSections().stream()
                .filter(section -> section.getId().equals("history"))
                .flatMap(section -> section.getFields().stream())
                .anyMatch(field -> field.getId().equals(
                        CommercialCatalogScreenContract.TAX_PROFILE_HISTORY.value())
                        && field.isDataTable()));
        assertTrue(view.isCreateAction(
                CommercialCatalogScreenContract.REGISTER_TAX_PROFILE.value()));
        assertTrue(view.isSearchAction(
                CommercialCatalogScreenContract.TAX_PROFILE_SEARCH.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.REVISE_TAX_PROFILE.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.ACTIVATE_TAX_PROFILE.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.INACTIVATE_TAX_PROFILE.value()));
    }

    @Test
    void rendersDefinitionsAsOneOrganizedMasterDataJourney() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertEquals(CommercialCatalogScreenContract.DEFINITIONS,
                registry.screenFor(
                        CommercialCatalogPluginDefinition.ID,
                        CommercialCatalogScreenContract.DEFINITIONS_ROUTE).orElseThrow());

        ShellScreenView view = registry.render(
                composed(CommercialCatalogScreenContract.definitionsDefinition()),
                new ShellTextCatalog()).orElseThrow();

        assertEquals("Definiciones del catálogo", view.getTitle());
        assertEquals("Nueva definición", view.getNewActionLabel());
        assertEquals(2, view.getDirectorySections().size());
        assertEquals(4, view.getDetailSections().size());
        assertEquals(List.of("history", "revision", "replacement", "lifecycle"),
                view.getDetailTabs().stream().map(ShellDetailTabView::getId).toList());
        assertTrue(view.getDetailSections().stream()
                .filter(section -> section.getId().equals("history"))
                .flatMap(section -> section.getFields().stream())
                .anyMatch(field -> field.getId().equals(
                        CommercialCatalogScreenContract.DEFINITION_HISTORY.value())
                        && field.isDataTable()));
        assertTrue(view.isCreateAction(
                CommercialCatalogScreenContract.REGISTER_DEFINITION.value()));
        assertTrue(view.isSearchAction(
                CommercialCatalogScreenContract.DEFINITION_SEARCH.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.REVISE_DEFINITION.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.REPLACE_DEFINITION.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.ACTIVATE_DEFINITION.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.INACTIVATE_DEFINITION.value()));
    }

    @Test
    void rendersVariantFamiliesWithAContractOwnedAttributeDraft() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertEquals(CommercialCatalogScreenContract.VARIANT_FAMILIES,
                registry.screenFor(
                        CommercialCatalogPluginDefinition.ID,
                        CommercialCatalogScreenContract.VARIANT_FAMILIES_ROUTE).orElseThrow());

        ShellScreenView view = registry.render(
                composed(CommercialCatalogScreenContract.variantFamiliesDefinition()),
                new ShellTextCatalog()).orElseThrow();

        assertEquals("Familias de variantes", view.getTitle());
        assertEquals("Nueva familia", view.getNewActionLabel());
        assertEquals(2, view.getDirectorySections().size());
        assertEquals(3, view.getDetailSections().size());
        assertTrue(view.getDirectorySections().stream()
                .flatMap(section -> section.getFields().stream())
                .anyMatch(field -> field.getId().equals(
                        CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_DRAFT.value())
                        && field.isDisplayText()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.ADD_VARIANT_ATTRIBUTE.value()));
        assertTrue(view.isCreateAction(
                CommercialCatalogScreenContract.REGISTER_VARIANT_FAMILY.value()));
        assertTrue(view.getDetailSections().stream()
                .filter(section -> section.getId().equals("history"))
                .flatMap(section -> section.getFields().stream())
                .anyMatch(field -> field.getId().equals(
                        CommercialCatalogScreenContract.VARIANT_FAMILY_HISTORY.value())
                        && field.isDataTable()));
        assertTrue(view.getDetailSections().stream()
                .filter(section -> section.getId().equals("revision"))
                .flatMap(section -> section.getFields().stream())
                .anyMatch(field -> field.getId().equals(
                        CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_DRAFT.value())
                        && field.isDisplayText()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.REVISE_VARIANT_FAMILY.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.ACTIVATE_VARIANT_FAMILY.value()));
        assertTrue(view.acceptsAction(
                CommercialCatalogScreenContract.INACTIVATE_VARIANT_FAMILY.value()));
    }

    private static ComposedScreen composed(ScreenDefinition definition) {
        return new ComposedScreen(
                definition.id(),
                definition.contractVersion(),
                definition.elements().stream()
                        .map(element -> new ComposedScreenElement(
                                element.id(),
                                element.type(),
                                element.regionId(),
                                element.order(),
                                element.labelKey(),
                                element.helpKey(),
                                element.visible(),
                                element.enabled(),
                                element.required()))
                        .toList(),
                definition.slots(),
                List.of());
    }
}
