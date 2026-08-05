package py.com.logixone.plugins.inventory.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugins.inventory.InventoryScreenContract;

class InventorySelectorSourcesTest {

    @Test
    void declaresAValidSourceForEveryInventorySelector() {
        assertComplete(InventoryScreenContract.warehousesDefinition(), InventorySelectorSources.WAREHOUSES);
        assertComplete(InventoryScreenContract.stockDefinition(), InventorySelectorSources.STOCK);
        assertComplete(InventoryScreenContract.countsDefinition(), InventorySelectorSources.COUNTS);
    }

    private static void assertComplete(
            ScreenDefinition screen,
            java.util.Map<ScreenElementId, ?> declared) {
        Set<ScreenElementId> selectors = screen.elements().stream()
                .filter(element -> element.type() == ScreenElementType.SELECT)
                .map(element -> element.id())
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(selectors, declared.keySet());
    }
}
