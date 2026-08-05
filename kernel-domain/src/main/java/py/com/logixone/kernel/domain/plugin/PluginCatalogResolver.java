package py.com.logixone.kernel.domain.plugin;

import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.CYCLIC_DEPENDENCY;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.CUSTOMIZATION_DEPENDS_ON_CUSTOMIZATION;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_CAPABILITY;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_DEPENDENCY;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_MENU_CONTRIBUTION;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_MIGRATION;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_PERMISSION;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_PLUGIN_ID;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_SCREEN_DEFINITION;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_SCREEN_ELEMENT;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_SCREEN_OVERLAY;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.DUPLICATE_SCREEN_SLOT;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.INCOMPATIBLE_DEPENDENCY_VERSION;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.INCOMPATIBLE_PLUGIN_API;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.FUNCTIONAL_DEPENDS_ON_CUSTOMIZATION;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.INVALID_MIGRATION_SCHEMA;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.INVALID_SCREEN_OWNER;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.MISSING_REQUIRED_DEPENDENCY;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.SELF_DEPENDENCY;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.SCREEN_DEFINITION_REQUIRES_FUNCTIONAL_PLUGIN;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.SCREEN_OVERLAY_REQUIRES_CUSTOMIZATION_PLUGIN;
import static py.com.logixone.kernel.domain.plugin.PluginDiagnosticCode.SCREEN_OVERLAY_REQUIRES_TARGET_DEPENDENCY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.PluginApiVersion;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementDefinition;
import py.com.logixone.plugin.api.ScreenOverlay;
import py.com.logixone.plugin.api.ScreenSlotDefinition;

/** Validates a physically present plugin catalog and orders dependencies before consumers. */
public final class PluginCatalogResolver {

    private static final Comparator<PluginDescriptor> DESCRIPTOR_ORDER = Comparator
            .comparing(PluginDescriptor::id)
            .thenComparing(descriptor -> descriptor.version().toString())
            .thenComparing(PluginDescriptor::displayName);

    private final SemanticVersion pluginApiVersion;

    public PluginCatalogResolver() {
        this(PluginApiVersion.CURRENT);
    }

    public PluginCatalogResolver(SemanticVersion pluginApiVersion) {
        this.pluginApiVersion = Objects.requireNonNull(pluginApiVersion, "pluginApiVersion");
    }

    public PluginCatalogResolution resolve(Collection<PluginDescriptor> descriptors) {
        Objects.requireNonNull(descriptors, "descriptors");
        List<PluginDescriptor> sortedDescriptors = descriptors.stream()
                .map(descriptor -> Objects.requireNonNull(descriptor, "descriptor"))
                .sorted(DESCRIPTOR_ORDER)
                .toList();
        Map<PluginId, List<PluginDescriptor>> descriptorsById = sortedDescriptors.stream()
                .collect(Collectors.groupingBy(
                        PluginDescriptor::id, TreeMap::new, Collectors.toUnmodifiableList()));
        List<PluginDiagnostic> diagnostics = new ArrayList<>();

        validateDuplicatePluginIds(descriptorsById, diagnostics);
        for (PluginDescriptor descriptor : sortedDescriptors) {
            validateDescriptor(descriptor, descriptorsById, diagnostics);
        }
        validateCrossPluginContributionIds(sortedDescriptors, diagnostics);
        if (!diagnostics.isEmpty()) {
            return invalid(diagnostics);
        }

        Map<PluginId, PluginDescriptor> uniqueDescriptors = descriptorsById.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getFirst()));
        return topologicalOrder(uniqueDescriptors);
    }

    private static void validateDuplicatePluginIds(
            Map<PluginId, List<PluginDescriptor>> descriptorsById,
            List<PluginDiagnostic> diagnostics) {
        descriptorsById.forEach((id, matches) -> {
            if (matches.size() > 1) {
                diagnostics.add(new PluginDiagnostic(DUPLICATE_PLUGIN_ID, id, id.value()));
            }
        });
    }

    private void validateDescriptor(
            PluginDescriptor descriptor,
            Map<PluginId, List<PluginDescriptor>> descriptorsById,
            List<PluginDiagnostic> diagnostics) {
        if (!descriptor.pluginApiCompatibility().contains(pluginApiVersion)) {
            diagnostics.add(new PluginDiagnostic(
                    INCOMPATIBLE_PLUGIN_API, descriptor.id(), pluginApiVersion.toString()));
        }
        validateDependencies(descriptor, descriptorsById, diagnostics);
        addDuplicateDiagnostics(
                descriptor,
                descriptor.capabilities(),
                Function.identity(),
                DUPLICATE_CAPABILITY,
                diagnostics);
        addDuplicateDiagnostics(
                descriptor,
                descriptor.permissions(),
                Function.identity(),
                DUPLICATE_PERMISSION,
                diagnostics);
        addDuplicateDiagnostics(
                descriptor,
                descriptor.menuContributions(),
                MenuContribution::id,
                DUPLICATE_MENU_CONTRIBUTION,
                diagnostics);
        addDuplicateDiagnostics(
                descriptor,
                descriptor.migrations(),
                migration -> migration.schema() + "|" + migration.location(),
                DUPLICATE_MIGRATION,
                diagnostics);
        validateScreenStructures(descriptor, diagnostics);
        for (MigrationContribution migration : descriptor.migrations()) {
            if (!migration.schema().equals(descriptor.id().schemaName())) {
                diagnostics.add(new PluginDiagnostic(
                        INVALID_MIGRATION_SCHEMA, descriptor.id(), migration.schema()));
            }
        }
    }

    private static void validateScreenStructures(
            PluginDescriptor descriptor,
            List<PluginDiagnostic> diagnostics) {
        addDuplicateDiagnostics(
                descriptor,
                descriptor.screenDefinitions(),
                ScreenDefinition::id,
                DUPLICATE_SCREEN_DEFINITION,
                diagnostics);
        addDuplicateDiagnostics(
                descriptor,
                descriptor.screenOverlays(),
                ScreenOverlay::id,
                DUPLICATE_SCREEN_OVERLAY,
                diagnostics);
        if (descriptor.kind() != PluginKind.FUNCTIONAL && !descriptor.screenDefinitions().isEmpty()) {
            diagnostics.add(new PluginDiagnostic(
                    SCREEN_DEFINITION_REQUIRES_FUNCTIONAL_PLUGIN,
                    descriptor.id(),
                    descriptor.screenDefinitions().getFirst().id().toString()));
        }
        if (descriptor.kind() != PluginKind.CUSTOMIZATION && !descriptor.screenOverlays().isEmpty()) {
            diagnostics.add(new PluginDiagnostic(
                    SCREEN_OVERLAY_REQUIRES_CUSTOMIZATION_PLUGIN,
                    descriptor.id(),
                    descriptor.screenOverlays().getFirst().id().value()));
        }
        for (ScreenDefinition screen : descriptor.screenDefinitions()) {
            if (!screen.id().ownerPluginId().equals(descriptor.id())) {
                diagnostics.add(new PluginDiagnostic(
                        INVALID_SCREEN_OWNER, descriptor.id(), screen.id().toString()));
            }
            addDuplicateDiagnostics(
                    descriptor,
                    screen.elements(),
                    ScreenElementDefinition::id,
                    DUPLICATE_SCREEN_ELEMENT,
                    diagnostics);
            addDuplicateDiagnostics(
                    descriptor,
                    screen.slots(),
                    ScreenSlotDefinition::id,
                    DUPLICATE_SCREEN_SLOT,
                    diagnostics);
        }
        for (ScreenOverlay overlay : descriptor.screenOverlays()) {
            boolean hasRequiredTargetDependency = descriptor.dependencies().stream()
                    .anyMatch(dependency -> dependency.pluginId().equals(
                                    overlay.targetScreen().ownerPluginId())
                            && dependency.kind() == DependencyKind.REQUIRED);
            if (!hasRequiredTargetDependency) {
                diagnostics.add(new PluginDiagnostic(
                        SCREEN_OVERLAY_REQUIRES_TARGET_DEPENDENCY,
                        descriptor.id(),
                        overlay.targetScreen().ownerPluginId().value()));
            }
        }
    }

    private static void validateDependencies(
            PluginDescriptor descriptor,
            Map<PluginId, List<PluginDescriptor>> descriptorsById,
            List<PluginDiagnostic> diagnostics) {
        Set<PluginId> seenDependencies = new HashSet<>();
        for (PluginDependency dependency : descriptor.dependencies()) {
            if (!seenDependencies.add(dependency.pluginId())) {
                diagnostics.add(new PluginDiagnostic(
                        DUPLICATE_DEPENDENCY, descriptor.id(), dependency.pluginId().value()));
                continue;
            }
            if (dependency.pluginId().equals(descriptor.id())) {
                diagnostics.add(new PluginDiagnostic(
                        SELF_DEPENDENCY, descriptor.id(), dependency.pluginId().value()));
                continue;
            }
            List<PluginDescriptor> matches = descriptorsById.getOrDefault(dependency.pluginId(), List.of());
            if (matches.isEmpty()) {
                if (dependency.kind() == DependencyKind.REQUIRED) {
                    diagnostics.add(new PluginDiagnostic(
                            MISSING_REQUIRED_DEPENDENCY,
                            descriptor.id(),
                            dependency.pluginId().value()));
                }
                continue;
            }
            if (matches.size() == 1 && !dependency.compatibleVersions().contains(matches.getFirst().version())) {
                diagnostics.add(new PluginDiagnostic(
                        INCOMPATIBLE_DEPENDENCY_VERSION,
                        descriptor.id(),
                        dependency.pluginId() + "@" + matches.getFirst().version()));
            }
            if (matches.size() == 1) {
                PluginKind dependencyKind = matches.getFirst().kind();
                if (descriptor.kind() == PluginKind.FUNCTIONAL
                        && dependencyKind == PluginKind.CUSTOMIZATION) {
                    diagnostics.add(new PluginDiagnostic(
                            FUNCTIONAL_DEPENDS_ON_CUSTOMIZATION,
                            descriptor.id(),
                            dependency.pluginId().value()));
                } else if (descriptor.kind() == PluginKind.CUSTOMIZATION
                        && dependencyKind == PluginKind.CUSTOMIZATION) {
                    diagnostics.add(new PluginDiagnostic(
                            CUSTOMIZATION_DEPENDS_ON_CUSTOMIZATION,
                            descriptor.id(),
                            dependency.pluginId().value()));
                }
            }
        }
    }

    private static <T, K> void addDuplicateDiagnostics(
            PluginDescriptor descriptor,
            List<T> values,
            Function<T, K> keyExtractor,
            PluginDiagnosticCode code,
            List<PluginDiagnostic> diagnostics) {
        Map<K, Long> counts = values.stream()
                .collect(Collectors.groupingBy(keyExtractor, HashMap::new, Collectors.counting()));
        counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> new PluginDiagnostic(code, descriptor.id(), entry.getKey().toString()))
                .sorted()
                .forEach(diagnostics::add);
    }

    private static void validateCrossPluginContributionIds(
            List<PluginDescriptor> descriptors,
            List<PluginDiagnostic> diagnostics) {
        addCrossPluginContributionDiagnostics(
                descriptors,
                PluginDescriptor::capabilities,
                Function.identity(),
                DUPLICATE_CAPABILITY,
                diagnostics);
        addCrossPluginContributionDiagnostics(
                descriptors,
                PluginDescriptor::permissions,
                Function.identity(),
                DUPLICATE_PERMISSION,
                diagnostics);
        addCrossPluginContributionDiagnostics(
                descriptors,
                PluginDescriptor::menuContributions,
                MenuContribution::id,
                DUPLICATE_MENU_CONTRIBUTION,
                diagnostics);
        addCrossPluginContributionDiagnostics(
                descriptors,
                PluginDescriptor::screenOverlays,
                ScreenOverlay::id,
                DUPLICATE_SCREEN_OVERLAY,
                diagnostics);
    }

    private static <T> void addCrossPluginContributionDiagnostics(
            List<PluginDescriptor> descriptors,
            Function<PluginDescriptor, List<T>> valuesExtractor,
            Function<T, ContributionId> idExtractor,
            PluginDiagnosticCode code,
            List<PluginDiagnostic> diagnostics) {
        Map<ContributionId, Set<PluginId>> ownersByContribution = new TreeMap<>();
        for (PluginDescriptor descriptor : descriptors) {
            valuesExtractor.apply(descriptor).stream()
                    .map(idExtractor)
                    .distinct()
                    .forEach(id -> ownersByContribution
                            .computeIfAbsent(id, ignored -> new TreeSet<>())
                            .add(descriptor.id()));
        }
        ownersByContribution.forEach((contributionId, owners) -> {
            if (owners.size() > 1) {
                owners.forEach(owner -> diagnostics.add(new PluginDiagnostic(
                        code,
                        owner,
                        contributionId.value())));
            }
        });
    }

    private static PluginCatalogResolution topologicalOrder(
            Map<PluginId, PluginDescriptor> descriptorsById) {
        Map<PluginId, Integer> remainingDependencies = new HashMap<>();
        Map<PluginId, List<PluginId>> dependents = new HashMap<>();
        descriptorsById.keySet().forEach(id -> {
            remainingDependencies.put(id, 0);
            dependents.put(id, new ArrayList<>());
        });
        descriptorsById.values().forEach(descriptor -> descriptor.dependencies().stream()
                .map(PluginDependency::pluginId)
                .filter(descriptorsById::containsKey)
                .forEach(dependencyId -> {
                    remainingDependencies.compute(descriptor.id(), (ignored, count) -> count + 1);
                    dependents.get(dependencyId).add(descriptor.id());
                }));
        dependents.values().forEach(ids -> ids.sort(Comparator.naturalOrder()));

        Comparator<PluginId> compositionOrder = Comparator
                .comparing((PluginId id) -> descriptorsById.get(id).kind())
                .thenComparing(Comparator.naturalOrder());
        PriorityQueue<PluginId> ready = new PriorityQueue<>(compositionOrder);
        remainingDependencies.forEach((id, count) -> {
            if (count == 0) {
                ready.add(id);
            }
        });
        List<PluginDescriptor> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            PluginId id = ready.remove();
            ordered.add(descriptorsById.get(id));
            for (PluginId dependent : dependents.get(id)) {
                int count = remainingDependencies.compute(dependent, (ignored, current) -> current - 1);
                if (count == 0) {
                    ready.add(dependent);
                }
            }
        }
        if (ordered.size() != descriptorsById.size()) {
            List<PluginId> unresolved = remainingDependencies.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
            String subject = unresolved.stream().map(PluginId::value).collect(Collectors.joining(","));
            return invalid(List.of(new PluginDiagnostic(CYCLIC_DEPENDENCY, unresolved.getFirst(), subject)));
        }
        return new PluginCatalogResolution(ordered, List.of());
    }

    private static PluginCatalogResolution invalid(Collection<PluginDiagnostic> diagnostics) {
        return new PluginCatalogResolution(List.of(), diagnostics.stream().sorted().toList());
    }
}
