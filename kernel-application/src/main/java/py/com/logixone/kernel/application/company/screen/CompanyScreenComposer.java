package py.com.logixone.kernel.application.company.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.kernel.application.company.contribution.CompanyContributions;
import py.com.logixone.kernel.application.company.contribution.PluginContributions;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.ScreenChange;
import py.com.logixone.plugin.api.ScreenCustomizationOperation;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenFragmentId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenOverlay;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.ScreenSlotId;
import py.com.logixone.plugin.api.ScreenTextKey;

/** Validates the complete company overlay before producing any composed screen. */
public final class CompanyScreenComposer {

    public CompanyScreenComposition compose(CompanyContributions contributions) {
        Objects.requireNonNull(contributions, "contributions");
        if (!contributions.operational()) {
            return unavailable(
                    contributions,
                    List.of(ScreenCompositionDiagnostic.company(sourceFailure(contributions))));
        }

        List<PluginContributions> plugins = contributions.plugins();
        PluginContributions customization = plugins.getLast();
        List<PluginContributions> functionalPlugins = plugins.subList(0, plugins.size() - 1);
        Map<PluginId, PluginContributions> functionalById = new HashMap<>();
        Map<ScreenId, ScreenDefinition> definitions = new LinkedHashMap<>();
        List<ScreenId> definitionOrder = new ArrayList<>();
        for (PluginContributions plugin : functionalPlugins) {
            functionalById.put(plugin.pluginId(), plugin);
            for (ScreenDefinition definition : plugin.screenDefinitions()) {
                definitions.put(definition.id(), definition);
                definitionOrder.add(definition.id());
            }
        }

        List<ScreenCompositionDiagnostic> diagnostics = validate(
                customization,
                functionalById,
                definitions);
        if (!diagnostics.isEmpty()) {
            return unavailable(contributions, diagnostics);
        }

        Map<ScreenId, MutableScreen> screens = new LinkedHashMap<>();
        definitions.forEach((id, definition) -> screens.put(id, new MutableScreen(definition)));
        for (ScreenOverlay overlay : customization.screenOverlays()) {
            MutableScreen screen = screens.get(overlay.targetScreen());
            for (ScreenChange change : overlay.changes()) {
                screen.apply(change);
            }
        }
        List<ComposedScreen> composedScreens = definitionOrder.stream()
                .distinct()
                .map(screens::get)
                .map(MutableScreen::toComposed)
                .toList();
        return new CompanyScreenComposition(
                contributions.companyId(),
                true,
                composedScreens,
                List.of());
    }

    private static List<ScreenCompositionDiagnostic> validate(
            PluginContributions customization,
            Map<PluginId, PluginContributions> functionalById,
            Map<ScreenId, ScreenDefinition> definitions) {
        List<ScreenCompositionDiagnostic> diagnostics = new ArrayList<>();
        Set<String> changedProperties = new HashSet<>();
        Set<String> slotPositions = new HashSet<>();
        Set<String> fragments = new HashSet<>();
        Map<String, Integer> slotContentCounts = new HashMap<>();

        for (ScreenOverlay overlay : customization.screenOverlays()) {
            ScreenDefinition definition = definitions.get(overlay.targetScreen());
            PluginContributions targetPlugin = functionalById.get(
                    overlay.targetScreen().ownerPluginId());
            if (definition == null) {
                diagnostics.add(diagnostic(
                        ScreenCompositionDiagnosticCode.SCREEN_TARGET_NOT_FOUND,
                        customization,
                        overlay,
                        overlay.targetScreen().toString()));
            } else if (!overlay.compatibleScreenVersions().contains(definition.contractVersion())) {
                diagnostics.add(diagnostic(
                        ScreenCompositionDiagnosticCode.SCREEN_VERSION_INCOMPATIBLE,
                        customization,
                        overlay,
                        definition.contractVersion().toString()));
            }
            if (!hasCompatibleRequiredDependency(customization, targetPlugin)) {
                diagnostics.add(diagnostic(
                        ScreenCompositionDiagnosticCode.SCREEN_TARGET_DEPENDENCY_INVALID,
                        customization,
                        overlay,
                        overlay.targetScreen().ownerPluginId().value()));
            }
            if (definition != null) {
                validateChanges(
                        customization,
                        overlay,
                        definition,
                        changedProperties,
                        slotPositions,
                        fragments,
                        slotContentCounts,
                        diagnostics);
            }
        }
        return diagnostics.stream().distinct().sorted().toList();
    }

    private static boolean hasCompatibleRequiredDependency(
            PluginContributions customization,
            PluginContributions targetPlugin) {
        if (targetPlugin == null) {
            return false;
        }
        return customization.dependencies().stream()
                .filter(dependency -> dependency.kind() == DependencyKind.REQUIRED)
                .filter(dependency -> dependency.pluginId().equals(targetPlugin.pluginId()))
                .map(PluginDependency::compatibleVersions)
                .anyMatch(range -> range.contains(targetPlugin.pluginVersion()));
    }

    private static void validateChanges(
            PluginContributions customization,
            ScreenOverlay overlay,
            ScreenDefinition definition,
            Set<String> changedProperties,
            Set<String> slotPositions,
            Set<String> fragments,
            Map<String, Integer> slotContentCounts,
            List<ScreenCompositionDiagnostic> diagnostics) {
        Map<ScreenElementId, ScreenElementDefinition> elements = definition.elements().stream()
                .collect(java.util.stream.Collectors.toMap(ScreenElementDefinition::id, element -> element));
        Map<ScreenSlotId, ScreenSlotDefinition> slots = definition.slots().stream()
                .collect(java.util.stream.Collectors.toMap(ScreenSlotDefinition::id, slot -> slot));
        for (ScreenChange change : overlay.changes()) {
            if (change instanceof ScreenChange.SlotContent content) {
                validateSlotContent(
                        customization,
                        overlay,
                        content,
                        slots,
                        slotPositions,
                        fragments,
                        slotContentCounts,
                        diagnostics);
                continue;
            }
            ScreenElementId elementId = elementId(change);
            ScreenElementDefinition element = elements.get(elementId);
            if (element == null) {
                diagnostics.add(diagnostic(
                        ScreenCompositionDiagnosticCode.SCREEN_ELEMENT_NOT_FOUND,
                        customization,
                        overlay,
                        elementId.value()));
                continue;
            }
            ScreenCustomizationOperation operation = operation(change);
            if (!element.allowedOperations().contains(operation)) {
                diagnostics.add(diagnostic(
                        ScreenCompositionDiagnosticCode.SCREEN_OPERATION_NOT_ALLOWED,
                        customization,
                        overlay,
                        elementId + ":" + operation));
            }
            String propertyKey = overlay.targetScreen() + "|" + elementId + "|" + operation;
            if (!changedProperties.add(propertyKey)) {
                diagnostics.add(diagnostic(
                        ScreenCompositionDiagnosticCode.SCREEN_CHANGE_CONFLICT,
                        customization,
                        overlay,
                        elementId + ":" + operation));
            }
            if (change instanceof ScreenChange.Move move) {
                long regionSize = definition.elements().stream()
                        .filter(candidate -> candidate.regionId().equals(element.regionId()))
                        .count();
                if (move.position() >= regionSize) {
                    diagnostics.add(diagnostic(
                            ScreenCompositionDiagnosticCode.SCREEN_POSITION_OUT_OF_RANGE,
                            customization,
                            overlay,
                            elementId + ":" + move.position()));
                }
            }
        }
        for (Map.Entry<String, Integer> entry : slotContentCounts.entrySet()) {
            String prefix = overlay.targetScreen() + "|";
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            ScreenSlotId slotId = new ScreenSlotId(entry.getKey().substring(prefix.length()));
            ScreenSlotDefinition slot = slots.get(slotId);
            if (slot != null && entry.getValue() > slot.maxContents()) {
                diagnostics.add(diagnostic(
                        ScreenCompositionDiagnosticCode.SCREEN_SLOT_CAPACITY_EXCEEDED,
                        customization,
                        overlay,
                        slotId.value()));
            }
        }
    }

    private static void validateSlotContent(
            PluginContributions customization,
            ScreenOverlay overlay,
            ScreenChange.SlotContent content,
            Map<ScreenSlotId, ScreenSlotDefinition> slots,
            Set<String> slotPositions,
            Set<String> fragments,
            Map<String, Integer> slotContentCounts,
            List<ScreenCompositionDiagnostic> diagnostics) {
        ScreenSlotDefinition slot = slots.get(content.slotId());
        if (slot == null) {
            diagnostics.add(diagnostic(
                    ScreenCompositionDiagnosticCode.SCREEN_SLOT_NOT_FOUND,
                    customization,
                    overlay,
                    content.slotId().value()));
            return;
        }
        if (!content.fragmentId().ownerPluginId().equals(customization.pluginId())) {
            diagnostics.add(diagnostic(
                    ScreenCompositionDiagnosticCode.SCREEN_FRAGMENT_OWNER_MISMATCH,
                    customization,
                    overlay,
                    content.fragmentId().toString()));
        }
        String slotKey = overlay.targetScreen() + "|" + content.slotId();
        slotContentCounts.merge(slotKey, 1, Integer::sum);
        if (content.position() >= slot.maxContents()) {
            diagnostics.add(diagnostic(
                    ScreenCompositionDiagnosticCode.SCREEN_POSITION_OUT_OF_RANGE,
                    customization,
                    overlay,
                    content.slotId() + ":" + content.position()));
        }
        if (!slotPositions.add(slotKey + "|" + content.position())
                || !fragments.add(overlay.targetScreen() + "|" + content.fragmentId())) {
            diagnostics.add(diagnostic(
                    ScreenCompositionDiagnosticCode.SCREEN_CHANGE_CONFLICT,
                    customization,
                    overlay,
                    content.slotId() + ":" + content.fragmentId()));
        }
    }

    private static ScreenElementId elementId(ScreenChange change) {
        return switch (change) {
            case ScreenChange.Label label -> label.elementId();
            case ScreenChange.Help help -> help.elementId();
            case ScreenChange.Hide hide -> hide.elementId();
            case ScreenChange.Disable disable -> disable.elementId();
            case ScreenChange.Require require -> require.elementId();
            case ScreenChange.Move move -> move.elementId();
            case ScreenChange.SlotContent ignored -> throw new IllegalArgumentException("slot content has no element");
        };
    }

    private static ScreenCustomizationOperation operation(ScreenChange change) {
        return switch (change) {
            case ScreenChange.Label ignored -> ScreenCustomizationOperation.CHANGE_LABEL;
            case ScreenChange.Help ignored -> ScreenCustomizationOperation.CHANGE_HELP;
            case ScreenChange.Hide ignored -> ScreenCustomizationOperation.HIDE;
            case ScreenChange.Disable ignored -> ScreenCustomizationOperation.DISABLE;
            case ScreenChange.Require ignored -> ScreenCustomizationOperation.REQUIRE;
            case ScreenChange.Move ignored -> ScreenCustomizationOperation.REORDER;
            case ScreenChange.SlotContent ignored -> throw new IllegalArgumentException("slot content has no operation");
        };
    }

    private static ScreenCompositionDiagnostic diagnostic(
            ScreenCompositionDiagnosticCode code,
            PluginContributions customization,
            ScreenOverlay overlay,
            String subject) {
        return ScreenCompositionDiagnostic.overlay(
                code,
                customization.pluginId(),
                overlay.id(),
                overlay.targetScreen(),
                subject);
    }

    private static CompanyScreenComposition unavailable(
            CompanyContributions contributions,
            List<ScreenCompositionDiagnostic> diagnostics) {
        return new CompanyScreenComposition(
                contributions.companyId(),
                false,
                List.of(),
                diagnostics.stream().distinct().sorted().toList());
    }

    private static String sourceFailure(CompanyContributions contributions) {
        return contributions.failure()
                .map(Enum::name)
                .orElseGet(() -> contributions.diagnostics().stream()
                        .findFirst()
                        .map(diagnostic -> diagnostic.code().name())
                        .orElse("COMPANY_NOT_OPERATIONAL"));
    }

    private static final class MutableScreen {

        private final ScreenDefinition definition;
        private final Map<ScreenElementId, MutableElement> elements;
        private final List<ComposedSlotContent> slotContents = new ArrayList<>();

        private MutableScreen(ScreenDefinition definition) {
            this.definition = definition;
            this.elements = definition.elements().stream().collect(java.util.stream.Collectors.toMap(
                    ScreenElementDefinition::id,
                    MutableElement::new,
                    (first, ignored) -> first,
                    LinkedHashMap::new));
            normalizeAllRegions();
        }

        private void apply(ScreenChange change) {
            switch (change) {
                case ScreenChange.Label label -> elements.get(label.elementId()).labelKey = label.labelKey();
                case ScreenChange.Help help -> elements.get(help.elementId()).helpKey = Optional.of(help.helpKey());
                case ScreenChange.Hide hide -> elements.get(hide.elementId()).visible = false;
                case ScreenChange.Disable disable -> elements.get(disable.elementId()).enabled = false;
                case ScreenChange.Require require -> elements.get(require.elementId()).required = true;
                case ScreenChange.Move move -> move(move.elementId(), move.position());
                case ScreenChange.SlotContent content -> slotContents.add(new ComposedSlotContent(
                        content.slotId(), content.fragmentId(), content.position()));
            }
        }

        private void move(ScreenElementId elementId, int position) {
            MutableElement target = elements.get(elementId);
            List<MutableElement> region = elements.values().stream()
                    .filter(element -> element.regionId.equals(target.regionId))
                    .sorted(Comparator.comparingInt((MutableElement element) -> element.position)
                            .thenComparing(element -> element.id))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            region.remove(target);
            region.add(position, target);
            for (int index = 0; index < region.size(); index++) {
                region.get(index).position = index;
            }
        }

        private void normalizeAllRegions() {
            elements.values().stream()
                    .map(element -> element.regionId)
                    .distinct()
                    .forEach(regionId -> {
                        List<MutableElement> region = elements.values().stream()
                                .filter(element -> element.regionId.equals(regionId))
                                .sorted(Comparator.comparingInt((MutableElement element) -> element.position)
                                        .thenComparing(element -> element.id))
                                .toList();
                        for (int index = 0; index < region.size(); index++) {
                            region.get(index).position = index;
                        }
                    });
        }

        private ComposedScreen toComposed() {
            List<ComposedScreenElement> composedElements = elements.values().stream()
                    .sorted(Comparator.comparing((MutableElement element) -> element.regionId)
                            .thenComparingInt(element -> element.position)
                            .thenComparing(element -> element.id))
                    .map(MutableElement::toComposed)
                    .toList();
            List<ScreenSlotDefinition> slots = definition.slots().stream()
                    .sorted(Comparator.comparing(ScreenSlotDefinition::regionId)
                            .thenComparingInt(ScreenSlotDefinition::order)
                            .thenComparing(ScreenSlotDefinition::id))
                    .toList();
            List<ComposedSlotContent> contents = slotContents.stream()
                    .sorted(Comparator.comparing(ComposedSlotContent::slotId)
                            .thenComparingInt(ComposedSlotContent::position)
                            .thenComparing(ComposedSlotContent::fragmentId))
                    .toList();
            return new ComposedScreen(
                    definition.id(),
                    definition.contractVersion(),
                    composedElements,
                    slots,
                    contents,
                    definition.experience());
        }
    }

    private static final class MutableElement {

        private final ScreenElementId id;
        private final ScreenElementType type;
        private final ScreenRegionId regionId;
        private int position;
        private ScreenTextKey labelKey;
        private Optional<ScreenTextKey> helpKey;
        private boolean visible;
        private boolean enabled;
        private boolean required;

        private MutableElement(ScreenElementDefinition definition) {
            id = definition.id();
            type = definition.type();
            regionId = definition.regionId();
            position = definition.order();
            labelKey = definition.labelKey();
            helpKey = definition.helpKey();
            visible = definition.visible();
            enabled = definition.enabled();
            required = definition.required();
        }

        private ComposedScreenElement toComposed() {
            return new ComposedScreenElement(
                    id, type, regionId, position, labelKey, helpKey, visible, enabled, required);
        }
    }
}
