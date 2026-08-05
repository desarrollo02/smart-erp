package py.com.logixone.plugin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScreenContractTest {

    @Test
    void identitiesAreStableTypedAndRejectImplementationExpressions() {
        PluginId owner = new PluginId("sales_plugin");

        assertEquals("sales_plugin:invoice", new ScreenId(owner, "invoice").toString());
        assertEquals("sales_plugin:tax_summary", new ScreenFragmentId(owner, "tax_summary").toString());
        assertThrows(IllegalArgumentException.class, () -> new ScreenElementId("customer.name"));
        assertThrows(IllegalArgumentException.class, () -> new ScreenSlotId("#{bean.slot}"));
        assertThrows(IllegalArgumentException.class, () -> new ScreenTextKey("#{bean.label}"));
        assertThrows(IllegalArgumentException.class, () -> new ScreenTextKey("/internal/messages.properties"));
        assertEquals(
                Set.of(
                        ScreenCustomizationOperation.CHANGE_LABEL,
                        ScreenCustomizationOperation.CHANGE_HELP,
                        ScreenCustomizationOperation.HIDE,
                        ScreenCustomizationOperation.DISABLE,
                        ScreenCustomizationOperation.REQUIRE,
                        ScreenCustomizationOperation.REORDER),
                Set.of(ScreenCustomizationOperation.values()),
                "the public contract must not gain SHOW, ENABLE or OPTIONAL operations implicitly");
    }

    @Test
    void screenDefinitionAndOverlayCopyAllCollections() {
        ScreenElementDefinition element = element();
        List<ScreenElementDefinition> elements = new ArrayList<>(List.of(element));
        List<ScreenSlotDefinition> slots = new ArrayList<>(List.of(new ScreenSlotDefinition(
                new ScreenSlotId("after_totals"), new ScreenRegionId("main"), 1, 2)));
        ScreenDefinition definition = new ScreenDefinition(
                new ScreenId(new PluginId("sales_plugin"), "invoice"),
                SemanticVersion.parse("1.2.0"),
                elements,
                slots);
        List<ScreenChange> changes = new ArrayList<>(List.of(
                new ScreenChange.Require(element.id())));
        ScreenOverlay overlay = new ScreenOverlay(
                new ContributionId("customer_a.invoice"),
                definition.id(),
                new VersionRange(
                        SemanticVersion.parse("1.0.0"), SemanticVersion.parse("2.0.0")),
                changes);
        elements.clear();
        slots.clear();
        changes.clear();

        assertEquals(List.of(element), definition.elements());
        assertEquals(1, definition.slots().size());
        assertEquals(1, overlay.changes().size());
        assertThrows(UnsupportedOperationException.class, () -> definition.elements().clear());
        assertThrows(UnsupportedOperationException.class, () -> overlay.changes().clear());
    }

    @Test
    void contractsRejectInvalidPositionsEmptyOverlaysAndMutableOperationSets() {
        Set<ScreenCustomizationOperation> operations = new java.util.HashSet<>();
        operations.add(ScreenCustomizationOperation.HIDE);
        ScreenElementDefinition element = new ScreenElementDefinition(
                new ScreenElementId("customer"),
                new ScreenRegionId("main"),
                0,
                new ScreenTextKey("sales.customer"),
                Optional.empty(),
                true,
                true,
                false,
                operations);
        operations.clear();

        assertEquals(Set.of(ScreenCustomizationOperation.HIDE), element.allowedOperations());
        assertThrows(UnsupportedOperationException.class, () -> element.allowedOperations().clear());
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScreenChange.Move(element.id(), -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScreenSlotDefinition(
                        new ScreenSlotId("summary"), new ScreenRegionId("main"), 0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScreenOverlay(
                        new ContributionId("customer_a.empty"),
                        new ScreenId(new PluginId("sales_plugin"), "invoice"),
                        new VersionRange(
                                SemanticVersion.parse("1.0.0"), SemanticVersion.parse("2.0.0")),
                        List.of()));
    }

    private static ScreenElementDefinition element() {
        return new ScreenElementDefinition(
                new ScreenElementId("customer"),
                new ScreenRegionId("main"),
                0,
                new ScreenTextKey("sales.customer"),
                Optional.of(new ScreenTextKey("sales.customer.help")),
                true,
                true,
                false,
                Set.of(
                        ScreenCustomizationOperation.CHANGE_LABEL,
                        ScreenCustomizationOperation.REQUIRE));
    }
}
