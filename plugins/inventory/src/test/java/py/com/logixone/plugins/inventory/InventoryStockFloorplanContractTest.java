package py.com.logixone.plugins.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.ScreenActionEmphasis;
import py.com.logixone.plugin.api.ScreenConfirmationMode;
import py.com.logixone.plugin.api.ScreenPurpose;
import py.com.logixone.plugin.api.ScreenRegionRole;
import py.com.logixone.plugin.api.ScreenSemanticType;

class InventoryStockFloorplanContractTest {

    @Test
    void stockMigratesInPlaceToTheGuidedOperationContract() {
        var definition = InventoryScreenContract.stockDefinition();
        var experience = definition.experience().orElseThrow();

        assertEquals(InventoryScreenContract.STOCK, definition.id());
        assertEquals("2.0.0", definition.contractVersion().toString());
        assertEquals(ScreenPurpose.GUIDED_OPERATION, experience.purpose());
        assertEquals(
                java.util.List.of(
                        ScreenRegionRole.CONTEXT,
                        ScreenRegionRole.CONTENT,
                        ScreenRegionRole.GUIDANCE,
                        ScreenRegionRole.SUMMARY,
                        ScreenRegionRole.ACTIONS),
                experience.regions().stream().map(region -> region.role()).toList());
        assertTrue(definition.slots().isEmpty());
        assertTrue(definition.elements().stream().anyMatch(
                element -> element.id().equals(InventoryScreenContract.STOCK_TASK)));
        assertTrue(definition.elements().stream().anyMatch(
                element -> element.id().equals(InventoryScreenContract.MOVEMENT_ITEM)));
        assertFalse(definition.elements().stream()
                .filter(element -> element.id().equals(InventoryScreenContract.MOVEMENT_ITEM))
                .findFirst().orElseThrow().required());
        assertEquals("context", definition.elements().stream()
                .filter(element -> element.id().equals(InventoryScreenContract.MOVEMENT_TYPE))
                .findFirst().orElseThrow().regionId().value());
        assertFalse(definition.elements().stream().anyMatch(element ->
                element.id().equals(InventoryScreenContract.MOVEMENT_SOURCE_TYPE)
                        || element.id().equals(InventoryScreenContract.MOVEMENT_SOURCE_ID)));
        assertEquals(
                ScreenSemanticType.TECHNICAL_TOKEN,
                experience.elementSemantics().get(
                        InventoryScreenContract.MOVEMENT_IDEMPOTENCY));
        var post = experience.actions().stream()
                .filter(action -> action.elementId().equals(
                        InventoryScreenContract.POST_MOVEMENT))
                .findFirst()
                .orElseThrow();
        assertEquals(ScreenActionEmphasis.PRIMARY, post.emphasis());
        assertEquals(ScreenConfirmationMode.ACKNOWLEDGEMENT, post.confirmationMode());
        assertTrue(experience.actions().stream().anyMatch(action ->
                action.elementId().equals(InventoryScreenContract.INACTIVATE_STOCK_ITEM)
                        && action.emphasis() == ScreenActionEmphasis.DESTRUCTIVE));
    }
}
