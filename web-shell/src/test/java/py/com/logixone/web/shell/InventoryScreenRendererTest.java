package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.company.screen.ComposedScreenElement;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugins.inventory.InventoryPluginDefinition;
import py.com.logixone.plugins.inventory.InventoryScreenContract;

class InventoryScreenRendererTest {

    @Test
    void rendersStockAsFocusedAvailabilityMovementReservationAndLifecycleTabs() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertEquals(InventoryScreenContract.STOCK,
                registry.screenFor(
                        InventoryPluginDefinition.ID,
                        InventoryScreenContract.STOCK_ROUTE).orElseThrow());

        ShellScreenView view = registry.render(
                composed(InventoryScreenContract.stockDefinition()),
                new ShellTextCatalog()).orElseThrow();

        assertTrue(view.isInteractive());
        assertEquals("Existencias", view.getTitle());
        assertEquals("Incorporar producto", view.getNewActionLabel());
        assertEquals(2, view.getDirectorySections().size());
        assertEquals(5, view.getDetailSections().size());
        assertEquals(List.of("availability", "movements", "reservations", "lifecycle"),
                view.getDetailTabs().stream().map(ShellDetailTabView::getId).toList());
        assertTrue(view.acceptsAction(InventoryScreenContract.POST_MOVEMENT.value()));
        assertTrue(view.acceptsAction(InventoryScreenContract.CREATE_RESERVATION.value()));
        assertFalse(view.acceptsDetailTab("general"));
    }

    @Test
    void rendersWarehousesAsAnIndependentStructureJourney() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertEquals(InventoryScreenContract.WAREHOUSES,
                registry.screenFor(
                        InventoryPluginDefinition.ID,
                        InventoryScreenContract.WAREHOUSES_ROUTE).orElseThrow());

        ShellScreenView view = registry.render(
                composed(InventoryScreenContract.warehousesDefinition()),
                new ShellTextCatalog()).orElseThrow();

        assertEquals("Depósitos", view.getTitle());
        assertEquals(3, view.getDetailSections().size());
        assertEquals(List.of("general", "locations", "lifecycle"),
                view.getDetailTabs().stream().map(ShellDetailTabView::getId).toList());
        assertTrue(view.acceptsAction(InventoryScreenContract.ADD_LOCATION.value()));
        assertTrue(view.isCreateAction(InventoryScreenContract.OPEN_WAREHOUSE.value()));
    }

    @Test
    void rendersCountsWithPostingSeparatedFromRoutineCapture() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertEquals(InventoryScreenContract.COUNTS,
                registry.screenFor(
                        InventoryPluginDefinition.ID,
                        InventoryScreenContract.COUNTS_ROUTE).orElseThrow());

        ShellScreenView view = registry.render(
                composed(InventoryScreenContract.countsDefinition()),
                new ShellTextCatalog()).orElseThrow();

        assertEquals("Conteos físicos", view.getTitle());
        assertEquals(3, view.getDetailSections().size());
        assertEquals(List.of("lines", "capture", "lifecycle"),
                view.getDetailTabs().stream().map(ShellDetailTabView::getId).toList());
        assertTrue(view.acceptsAction(InventoryScreenContract.RECORD_COUNT.value()));
        assertTrue(view.acceptsAction(InventoryScreenContract.POST_COUNT.value()));
        assertTrue(view.isSearchAction(InventoryScreenContract.COUNT_SEARCH.value()));
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
