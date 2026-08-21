package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.company.screen.ComposedScreenElement;
import py.com.logixone.plugin.api.ScreenActionDefinition;
import py.com.logixone.plugin.api.ScreenActionEmphasis;
import py.com.logixone.plugin.api.ScreenActionIntent;
import py.com.logixone.plugin.api.ScreenConfirmationMode;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenExperienceDefinition;
import py.com.logixone.plugin.api.ScreenPurpose;
import py.com.logixone.plugin.api.ScreenRegionDefinition;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenRegionRole;
import py.com.logixone.plugin.api.ScreenSemanticType;
import py.com.logixone.plugin.api.ScreenTextKey;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugins.inventory.InventoryScreenContract;

class ShellFloorplanRendererTest {

    private static final ShellTextCatalog TEXTS = new ShellTextCatalog() {
        @Override
        public Optional<String> screenText(ScreenTextKey textKey) {
            return Optional.of(textKey.value());
        }
    };

    @Test
    void selectsEveryClosedV2FloorplanAndOrdersItsRegions() {
        Map<ScreenPurpose, List<ScreenRegionRole>> roles = new LinkedHashMap<>();
        roles.put(ScreenPurpose.MASTER_DATA,
                List.of(ScreenRegionRole.CONTENT, ScreenRegionRole.ACTIONS));
        roles.put(ScreenPurpose.WORKLIST,
                List.of(ScreenRegionRole.WORK_ITEMS, ScreenRegionRole.ACTIONS));
        roles.put(ScreenPurpose.TRANSACTION_EDITOR,
                List.of(
                        ScreenRegionRole.HEADER,
                        ScreenRegionRole.LINES,
                        ScreenRegionRole.SUMMARY,
                        ScreenRegionRole.ACTIONS));
        roles.put(ScreenPurpose.GUIDED_OPERATION,
                List.of(ScreenRegionRole.CONTENT, ScreenRegionRole.ACTIONS));
        roles.put(ScreenPurpose.INQUIRY, List.of(ScreenRegionRole.CONTENT));

        ShellScreenRegistry registry = new ShellScreenRegistry();
        roles.forEach((purpose, regionRoles) -> {
            ShellScreenView view = registry.render(
                    screen("2.0.0", purpose, regionRoles), TEXTS).orElseThrow();

            assertTrue(view.isFloorplanV2());
            assertEquals("floorplan floorplan-"
                            + purpose.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-'),
                    view.getFloorplanClass());
            assertEquals(regionRoles.stream().map(ShellFloorplanRendererTest::regionId).toList(),
                    view.getFloorplanRegions().stream()
                            .map(ShellScreenRegionView::getId)
                            .toList());
        });
    }

    @Test
    void mapsSemanticsAndDestructiveActionSafetyWithoutAcceptingPluginMarkup() {
        ShellScreenView view = new ShellScreenRegistry().render(
                screen(
                        "2.0.0",
                        ScreenPurpose.GUIDED_OPERATION,
                        List.of(ScreenRegionRole.CONTENT, ScreenRegionRole.ACTIONS)),
                TEXTS).orElseThrow();

        ShellScreenElementView field = view.getFloorplanRegions().getFirst().getFields().getFirst();
        ShellScreenElementView action = view.getFloorplanRegions().getLast().getActions().getFirst();
        assertEquals("semantic-text", field.getSemanticClass());
        ShellScreenElementView dependentReference = new ShellScreenElementView(
                "dependent", ScreenElementType.SELECT, "Dependent", Optional.empty(),
                true, false, Optional.of(ScreenSemanticType.SEARCHABLE_REFERENCE),
                Optional.empty());
        assertTrue(dependentReference.isContextRefreshOnChange());
        assertFalse(field.isContextRefreshOnChange());
        assertEquals("button button-destructive", action.getActionClass());
        assertTrue(action.getConfirmationScript().startsWith("return confirm("));
        assertTrue(action.getConfirmationGuardScript().startsWith("if (!confirm("));
        assertFalse(action.isNavigateIntent());
        assertTrue(view.acceptsAction(action.getId()));
    }

    @Test
    void reservesNavigateForRowsOnlyWhenTheFloorplanHasATable() {
        ShellScreenRegistry registry = new ShellScreenRegistry();
        ShellScreenView guided = registry.render(
                screen(
                        "2.0.0",
                        ScreenPurpose.GUIDED_OPERATION,
                        List.of(ScreenRegionRole.CONTENT, ScreenRegionRole.ACTIONS),
                        ScreenActionIntent.NAVIGATE),
                TEXTS).orElseThrow();
        ShellScreenView worklist = registry.render(
                screen(
                        "2.0.0",
                        ScreenPurpose.WORKLIST,
                        List.of(ScreenRegionRole.WORK_ITEMS, ScreenRegionRole.ACTIONS),
                        ScreenActionIntent.NAVIGATE),
                TEXTS).orElseThrow();

        assertFalse(guided.isHasFloorplanRowAction());
        assertTrue(guided.getFloorplanRegions().getLast().getActions().getFirst().isNavigateIntent());
        assertTrue(worklist.isHasFloorplanRowAction());
    }

    @Test
    void separatesDirectoryFromCreateAndDetailRegions() {
        ShellScreenView view = new ShellScreenRegistry().render(
                screen(
                        "2.0.0",
                        ScreenPurpose.WORKLIST,
                        List.of(
                                ScreenRegionRole.FILTERS,
                                ScreenRegionRole.WORK_ITEMS,
                                ScreenRegionRole.HEADER,
                                ScreenRegionRole.LINES,
                                ScreenRegionRole.SUMMARY,
                                ScreenRegionRole.ACTIONS),
                        ScreenActionIntent.CREATE),
                TEXTS).orElseThrow();

        assertTrue(view.isFloorplanSeparatedByMode());
        assertEquals(1, view.getFloorplanCreateActions().size());

        ShellScreenView singleStage = new ShellScreenRegistry().render(
                screen(
                        "2.0.0",
                        ScreenPurpose.GUIDED_OPERATION,
                        List.of(ScreenRegionRole.CONTENT, ScreenRegionRole.ACTIONS),
                        ScreenActionIntent.CREATE),
                TEXTS).orElseThrow();
        assertFalse(singleStage.isFloorplanSeparatedByMode());
    }

    @Test
    void rejectsUnsupportedMajorAndIncompletePurposeButKeepsV1MastersUnchanged() {
        ShellScreenRegistry registry = new ShellScreenRegistry();

        assertTrue(registry.render(
                screen(
                        "3.0.0",
                        ScreenPurpose.GUIDED_OPERATION,
                        List.of(ScreenRegionRole.CONTENT, ScreenRegionRole.ACTIONS)),
                TEXTS).isEmpty());
        assertTrue(registry.render(
                screen(
                        "2.0.0",
                        ScreenPurpose.WORKLIST,
                        List.of(ScreenRegionRole.ACTIONS)),
                TEXTS).isEmpty());

        var definition = InventoryScreenContract.warehousesDefinition();
        ComposedScreen v1 = new ComposedScreen(
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
        ShellScreenView legacy = registry.render(v1, new ShellTextCatalog()).orElseThrow();
        assertFalse(legacy.isFloorplanV2());
        assertEquals(2, legacy.getDirectorySections().size());
    }

    private static ComposedScreen screen(
            String version,
            ScreenPurpose purpose,
            List<ScreenRegionRole> roles) {
        return screen(version, purpose, roles, ScreenActionIntent.EXECUTE);
    }

    private static ComposedScreen screen(
            String version,
            ScreenPurpose purpose,
            List<ScreenRegionRole> roles,
            ScreenActionIntent actionIntent) {
        List<ScreenRegionDefinition> regions = new ArrayList<>();
        List<ComposedScreenElement> elements = new ArrayList<>();
        Map<ScreenElementId, ScreenSemanticType> semantics = new LinkedHashMap<>();
        List<ScreenActionDefinition> actions = new ArrayList<>();
        for (int index = 0; index < roles.size(); index++) {
            ScreenRegionRole role = roles.get(index);
            ScreenRegionId regionId = new ScreenRegionId(regionId(role));
            regions.add(new ScreenRegionDefinition(regionId, role, index));
            ScreenElementId elementId = new ScreenElementId("element_" + index);
            ScreenElementType type = type(role);
            elements.add(new ComposedScreenElement(
                    elementId,
                    type,
                    regionId,
                    0,
                    new ScreenTextKey("test." + elementId.value()),
                    Optional.empty(),
                    true,
                    true,
                    false));
            if (type == ScreenElementType.ACTION) {
                actions.add(new ScreenActionDefinition(
                        elementId,
                        actionIntent,
                        ScreenActionEmphasis.DESTRUCTIVE,
                        ScreenConfirmationMode.ACKNOWLEDGEMENT));
            } else {
                semantics.put(elementId, semantic(role));
            }
        }
        return new ComposedScreen(
                InventoryScreenContract.STOCK,
                SemanticVersion.parse(version),
                elements,
                List.of(),
                List.of(),
                Optional.of(new ScreenExperienceDefinition(
                        purpose, regions, semantics, actions)));
    }

    private static String regionId(ScreenRegionRole role) {
        return role.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static ScreenElementType type(ScreenRegionRole role) {
        return switch (role) {
            case ACTIONS -> ScreenElementType.ACTION;
            case WORK_ITEMS, LINES -> ScreenElementType.DATA_TABLE;
            case SUMMARY -> ScreenElementType.DISPLAY_TEXT;
            default -> ScreenElementType.TEXT_INPUT;
        };
    }

    private static ScreenSemanticType semantic(ScreenRegionRole role) {
        return switch (role) {
            case WORK_ITEMS, SUMMARY -> ScreenSemanticType.SUMMARY;
            case LINES -> ScreenSemanticType.EDITABLE_LINES;
            default -> ScreenSemanticType.TEXT;
        };
    }
}
