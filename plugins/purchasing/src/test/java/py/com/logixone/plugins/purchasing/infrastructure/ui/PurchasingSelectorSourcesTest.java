package py.com.logixone.plugins.purchasing.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugins.purchasing.PurchasingScreenContract;

class PurchasingSelectorSourcesTest {

    @Test
    void declaresOneGovernedSourceForEveryPurchasingSelector() {
        assertComplete(PurchasingScreenContract.requestsDefinition(),
                PurchasingSelectorSources.REQUESTS);
        assertComplete(PurchasingScreenContract.ordersDefinition(),
                PurchasingSelectorSources.ORDERS);
        assertComplete(PurchasingScreenContract.receiptsDefinition(),
                PurchasingSelectorSources.RECEIPTS);
        assertComplete(PurchasingScreenContract.returnsDefinition(),
                PurchasingSelectorSources.RETURNS);
        assertComplete(PurchasingScreenContract.trackingDefinition(),
                PurchasingSelectorSources.TRACKING);
    }

    private static void assertComplete(
            ScreenDefinition screen,
            Map<ScreenElementId, SelectorSourceDefinition> declared) {
        Set<ScreenElementId> selectors = screen.elements().stream()
                .filter(element -> element.type() == ScreenElementType.SELECT)
                .map(element -> element.id())
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(selectors, declared.keySet());
    }
}
