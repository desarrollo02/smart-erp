package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.company.screen.ComposedScreenElement;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugins.purchasing.PurchasingPluginDefinition;
import py.com.logixone.plugins.purchasing.PurchasingScreenContract;

class PurchasingScreenRendererTest {

    @Test
    void rendersRequestsWithLinesApprovalAndCloneJourneys() {
        ShellScreenView view = render(
                PurchasingScreenContract.REQUESTS_ROUTE,
                PurchasingScreenContract.REQUESTS,
                PurchasingScreenContract.requestsDefinition());

        assertEquals("Solicitudes de compra", view.getTitle());
        assertTrue(view.isFloorplanV2());
        assertEquals("floorplan floorplan-worklist", view.getFloorplanClass());
        assertEquals(List.of("filters", "work_items", "header", "lines", "summary", "actions"),
                view.getFloorplanRegions().stream().map(ShellScreenRegionView::getId).toList());
        assertTrue(view.isHasFloorplanRowAction());
        assertTrue(view.isFloorplanSeparatedByMode());
        assertTrue(view.isCreateAction(PurchasingScreenContract.CREATE_REQUEST.value()));
        assertTrue(view.acceptsAction(PurchasingScreenContract.APPROVE_REQUEST.value()));
        assertTrue(view.acceptsAction(PurchasingScreenContract.CLONE_REQUEST.value()));
    }

    @Test
    void rendersOrdersReceiptsReturnsAndTrackingAsSeparateJourneys() {
        ShellScreenView orders = render(
                PurchasingScreenContract.ORDERS_ROUTE,
                PurchasingScreenContract.ORDERS,
                PurchasingScreenContract.ordersDefinition());
        assertEquals("Órdenes de compra", orders.getTitle());
        assertEquals("floorplan floorplan-transaction-editor", orders.getFloorplanClass());
        assertTrue(orders.isHasFloorplanRowAction());
        assertTrue(orders.isFloorplanSeparatedByMode());
        assertTrue(orders.acceptsAction(PurchasingScreenContract.ISSUE_ORDER.value()));

        ShellScreenView receipts = render(
                PurchasingScreenContract.RECEIPTS_ROUTE,
                PurchasingScreenContract.RECEIPTS,
                PurchasingScreenContract.receiptsDefinition());
        assertEquals("Recepciones de compra", receipts.getTitle());
        assertEquals("floorplan floorplan-guided-operation", receipts.getFloorplanClass());
        assertTrue(receipts.isFloorplanSeparatedByMode());
        assertTrue(receipts.acceptsAction(PurchasingScreenContract.CONFIRM_RECEIPT.value()));

        ShellScreenView returns = render(
                PurchasingScreenContract.RETURNS_ROUTE,
                PurchasingScreenContract.RETURNS,
                PurchasingScreenContract.returnsDefinition());
        assertEquals("Devoluciones a proveedores", returns.getTitle());
        assertEquals("floorplan floorplan-guided-operation", returns.getFloorplanClass());
        assertTrue(returns.isFloorplanSeparatedByMode());
        assertTrue(returns.acceptsAction(PurchasingScreenContract.CONFIRM_RETURN.value()));

        ShellScreenView tracking = render(
                PurchasingScreenContract.TRACKING_ROUTE,
                PurchasingScreenContract.TRACKING,
                PurchasingScreenContract.trackingDefinition());
        assertEquals("Seguimiento de compras", tracking.getTitle());
        assertEquals("floorplan floorplan-inquiry", tracking.getFloorplanClass());
        assertTrue(tracking.isFloorplanSeparatedByMode());
        assertTrue(tracking.isSearchAction(PurchasingScreenContract.TRACKING_SEARCH.value()));
        assertFalse(tracking.isCreateAction(PurchasingScreenContract.TRACKING_SEARCH.value()));

        ShellScreenElementView requestResults = render(
                PurchasingScreenContract.REQUESTS_ROUTE,
                PurchasingScreenContract.REQUESTS,
                PurchasingScreenContract.requestsDefinition())
                .getFloorplanRegions().stream()
                .flatMap(region -> region.getTables().stream())
                .filter(table -> table.getId().equals(
                        PurchasingScreenContract.REQUEST_RESULTS.value()))
                .findFirst().orElseThrow();
        ShellScreenElementView requestLines = render(
                PurchasingScreenContract.REQUESTS_ROUTE,
                PurchasingScreenContract.REQUESTS,
                PurchasingScreenContract.requestsDefinition())
                .getFloorplanRegions().stream()
                .flatMap(region -> region.getTables().stream())
                .filter(table -> table.getId().equals(
                        PurchasingScreenContract.REQUEST_LINES.value()))
                .findFirst().orElseThrow();
        assertFalse(requestResults.isEditableLines());
        assertTrue(requestLines.isEditableLines());
    }

    private static ShellScreenView render(
            String route,
            py.com.logixone.plugin.api.ScreenId screenId,
            ScreenDefinition definition) {
        ShellScreenRegistry registry = new ShellScreenRegistry();
        assertEquals(screenId,
                registry.screenFor(PurchasingPluginDefinition.ID, route).orElseThrow());
        return registry.render(composed(definition), new ShellTextCatalog()).orElseThrow();
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
                List.of(),
                definition.experience());
    }
}
