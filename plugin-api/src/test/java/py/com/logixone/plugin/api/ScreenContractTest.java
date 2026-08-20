package py.com.logixone.plugin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Test
    void v1DefinitionsRemainCompatibleAndV2RequiresAnExperience() {
        ScreenDefinition v1 = new ScreenDefinition(
                new ScreenId(new PluginId("sales_plugin"), "invoice"),
                SemanticVersion.parse("1.9.0"),
                List.of(element()),
                List.of());

        assertTrue(v1.experience().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new ScreenDefinition(
                v1.id(), SemanticVersion.parse("2.0.0"), v1.elements(), v1.slots()));
        assertThrows(IllegalArgumentException.class, () -> new ScreenDefinition(
                v1.id(), SemanticVersion.parse("1.9.0"), v1.elements(), v1.slots(),
                Optional.of(experience(v1.elements().getFirst()))));
    }

    @Test
    void v2DefinitionsValidateRegionsSemanticsAndActions() {
        ScreenElementDefinition quantity = new ScreenElementDefinition(
                new ScreenElementId("quantity"), ScreenElementType.TEXT_INPUT,
                new ScreenRegionId("lines"), 0, new ScreenTextKey("sales.quantity"),
                Optional.empty(), true, true, true, Set.of());
        ScreenElementDefinition submit = new ScreenElementDefinition(
                new ScreenElementId("submit"), ScreenElementType.ACTION,
                new ScreenRegionId("actions"), 1, new ScreenTextKey("sales.submit"),
                Optional.empty(), true, true, false, Set.of());
        ScreenExperienceDefinition experience = new ScreenExperienceDefinition(
                ScreenPurpose.TRANSACTION_EDITOR,
                List.of(
                        new ScreenRegionDefinition(
                                new ScreenRegionId("lines"), ScreenRegionRole.LINES, 0),
                        new ScreenRegionDefinition(
                                new ScreenRegionId("actions"), ScreenRegionRole.ACTIONS, 1)),
                Map.of(quantity.id(), ScreenSemanticType.QUANTITY),
                List.of(new ScreenActionDefinition(
                        submit.id(), ScreenActionIntent.SUBMIT,
                        ScreenActionEmphasis.PRIMARY,
                        ScreenConfirmationMode.ACKNOWLEDGEMENT)));

        ScreenDefinition definition = new ScreenDefinition(
                new ScreenId(new PluginId("sales_plugin"), "invoice"),
                SemanticVersion.parse("2.0.0"),
                List.of(quantity, submit),
                List.of(),
                Optional.of(experience));

        assertEquals(ScreenPurpose.TRANSACTION_EDITOR,
                definition.experience().orElseThrow().purpose());
        assertThrows(UnsupportedOperationException.class,
                () -> definition.experience().orElseThrow().elementSemantics().clear());
        assertThrows(IllegalArgumentException.class, () -> new ScreenDefinition(
                definition.id(), definition.contractVersion(),
                List.of(new ScreenElementDefinition(
                        quantity.id(), ScreenElementType.TEXT_INPUT,
                        new ScreenRegionId("missing"), 0, quantity.labelKey(),
                        Optional.empty(), true, true, false, Set.of()), submit),
                List.of(), Optional.of(experience)));
        assertThrows(IllegalArgumentException.class, () -> new ScreenDefinition(
                definition.id(), definition.contractVersion(), List.of(quantity, submit),
                List.of(), Optional.of(new ScreenExperienceDefinition(
                        experience.purpose(), experience.regions(),
                        Map.of(new ScreenElementId("unknown"), ScreenSemanticType.STATUS),
                        experience.actions()))));
    }

    @Test
    void experienceRejectsDuplicateOrUnsafeActionDefinitions() {
        ScreenElementDefinition action = new ScreenElementDefinition(
                new ScreenElementId("execute"), ScreenElementType.ACTION,
                new ScreenRegionId("actions"), 0, new ScreenTextKey("inventory.execute"),
                Optional.empty(), true, true, false, Set.of());
        ScreenActionDefinition safe = new ScreenActionDefinition(
                action.id(), ScreenActionIntent.EXECUTE, ScreenActionEmphasis.PRIMARY,
                ScreenConfirmationMode.ACKNOWLEDGEMENT);

        assertThrows(IllegalArgumentException.class, () -> new ScreenActionDefinition(
                action.id(), ScreenActionIntent.CANCEL, ScreenActionEmphasis.DESTRUCTIVE,
                ScreenConfirmationMode.NONE));
        assertThrows(IllegalArgumentException.class, () -> new ScreenExperienceDefinition(
                ScreenPurpose.GUIDED_OPERATION,
                List.of(new ScreenRegionDefinition(
                        action.regionId(), ScreenRegionRole.ACTIONS, 0)),
                Map.of(), List.of(safe, safe)));
        assertThrows(IllegalArgumentException.class, () -> new ScreenDefinition(
                new ScreenId(new PluginId("inventory"), "movement"),
                SemanticVersion.parse("2.0.0"), List.of(action), List.of(),
                Optional.of(new ScreenExperienceDefinition(
                        ScreenPurpose.GUIDED_OPERATION,
                        List.of(new ScreenRegionDefinition(
                                action.regionId(), ScreenRegionRole.ACTIONS, 0)),
                        Map.of(), List.of()))));
    }

    private static ScreenExperienceDefinition experience(ScreenElementDefinition element) {
        return new ScreenExperienceDefinition(
                ScreenPurpose.MASTER_DATA,
                List.of(new ScreenRegionDefinition(
                        element.regionId(), ScreenRegionRole.CONTENT, 0)),
                Map.of(element.id(), ScreenSemanticType.TEXT),
                List.of());
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
