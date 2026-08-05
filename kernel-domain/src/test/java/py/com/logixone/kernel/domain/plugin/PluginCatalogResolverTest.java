package py.com.logixone.kernel.domain.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.ScreenChange;
import py.com.logixone.plugin.api.ScreenCustomizationOperation;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenOverlay;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.ScreenSlotId;
import py.com.logixone.plugin.api.ScreenTextKey;
import py.com.logixone.plugin.api.VersionRange;

class PluginCatalogResolverTest {

    private static final VersionRange API_RANGE = range("0.4.0", "0.5.0");
    private static final VersionRange VERSION_ONE = range("1.0.0", "2.0.0");
    private final PluginCatalogResolver resolver = new PluginCatalogResolver();

    @Test
    void ordersDependenciesBeforeConsumersDeterministically() {
        PluginDescriptor inventory = descriptor("inventory", "1.1.0", List.of());
        PluginDescriptor sales = descriptor(
                "sales", "1.0.0", List.of(dependency("inventory", VERSION_ONE, DependencyKind.REQUIRED)));
        PluginDescriptor reports = descriptor(
                "reports", "1.0.0", List.of(dependency("sales", VERSION_ONE, DependencyKind.OPTIONAL)));

        PluginCatalogResolution first = resolver.resolve(List.of(reports, sales, inventory));
        PluginCatalogResolution second = resolver.resolve(List.of(sales, inventory, reports));

        assertTrue(first.isValid());
        assertEquals(List.of("inventory", "sales", "reports"), ids(first));
        assertEquals(ids(first), ids(second));
    }

    @Test
    void rejectsDuplicatePluginIds() {
        PluginCatalogResolution result = resolver.resolve(
                List.of(descriptor("sales", "1.0.0", List.of()), descriptor("sales", "1.1.0", List.of())));

        assertDiagnosticCodes(result, PluginDiagnosticCode.DUPLICATE_PLUGIN_ID);
    }

    @Test
    void rejectsMissingRequiredDependencyButAllowsMissingOptionalDependency() {
        PluginDescriptor required = descriptor(
                "sales", "1.0.0", List.of(dependency("inventory", VERSION_ONE, DependencyKind.REQUIRED)));
        PluginDescriptor optional = descriptor(
                "reports", "1.0.0", List.of(dependency("analytics", VERSION_ONE, DependencyKind.OPTIONAL)));

        assertDiagnosticCodes(
                resolver.resolve(List.of(required)), PluginDiagnosticCode.MISSING_REQUIRED_DEPENDENCY);
        assertTrue(resolver.resolve(List.of(optional)).isValid());
    }

    @Test
    void rejectsIncompatibleDependencyAndPluginApiVersions() {
        PluginDescriptor inventory = descriptor("inventory", "2.0.0", List.of());
        PluginDescriptor sales = descriptor(
                "sales", "1.0.0", List.of(dependency("inventory", VERSION_ONE, DependencyKind.REQUIRED)));
        PluginDescriptor incompatibleApi = new PluginDescriptor(
                new PluginId("legacy"),
                PluginKind.FUNCTIONAL,
                version("1.0.0"),
                range("1.0.0", "2.0.0"),
                "legacy",
                List.of(), List.of(), List.of(), List.of(), List.of());

        PluginCatalogResolution result = resolver.resolve(List.of(inventory, sales, incompatibleApi));

        assertDiagnosticCodes(
                result,
                PluginDiagnosticCode.INCOMPATIBLE_DEPENDENCY_VERSION,
                PluginDiagnosticCode.INCOMPATIBLE_PLUGIN_API);
    }

    @Test
    void rejectsSelfAndRepeatedDependencies() {
        PluginDependency self = dependency("sales", VERSION_ONE, DependencyKind.REQUIRED);
        PluginDescriptor sales = descriptor("sales", "1.0.0", List.of(self, self));

        PluginCatalogResolution result = resolver.resolve(List.of(sales));

        assertDiagnosticCodes(
                result,
                PluginDiagnosticCode.DUPLICATE_DEPENDENCY,
                PluginDiagnosticCode.SELF_DEPENDENCY);
    }

    @Test
    void rejectsCyclesWithAStableDiagnostic() {
        PluginDescriptor first = descriptor(
                "first", "1.0.0", List.of(dependency("second", VERSION_ONE, DependencyKind.REQUIRED)));
        PluginDescriptor second = descriptor(
                "second", "1.0.0", List.of(dependency("first", VERSION_ONE, DependencyKind.REQUIRED)));

        PluginCatalogResolution result = resolver.resolve(List.of(second, first));

        assertDiagnosticCodes(result, PluginDiagnosticCode.CYCLIC_DEPENDENCY);
        assertEquals("first,second", result.diagnostics().getFirst().subject());
    }

    @Test
    void diagnosesDuplicateContributionsAndForeignMigrationSchema() {
        ContributionId capability = new ContributionId("sales.orders");
        ContributionId permission = new ContributionId("sales.orders.view");
        MenuContribution menu = new MenuContribution(
                new ContributionId("sales.menu"), "sales.menu", "/sales", Optional.of(permission));
        MigrationContribution migration =
                new MigrationContribution("plg_other", "classpath:db/migration/sales");
        PluginDescriptor sales = new PluginDescriptor(
                new PluginId("sales"),
                PluginKind.FUNCTIONAL,
                version("1.0.0"),
                API_RANGE,
                "sales",
                List.of(),
                List.of(capability, capability),
                List.of(permission, permission),
                List.of(menu, menu),
                List.of(migration, migration));

        PluginCatalogResolution result = resolver.resolve(List.of(sales));

        assertDiagnosticCodes(
                result,
                PluginDiagnosticCode.DUPLICATE_CAPABILITY,
                PluginDiagnosticCode.DUPLICATE_MENU_CONTRIBUTION,
                PluginDiagnosticCode.DUPLICATE_MIGRATION,
                PluginDiagnosticCode.DUPLICATE_PERMISSION,
                PluginDiagnosticCode.INVALID_MIGRATION_SCHEMA);
    }

    @Test
    void rejectsContributionIdsOwnedByMoreThanOnePluginBeforeComposition() {
        ContributionId sharedCapability = new ContributionId("shared.dashboard");
        PluginDescriptor first = new PluginDescriptor(
                new PluginId("first"),
                PluginKind.FUNCTIONAL,
                version("1.0.0"),
                API_RANGE,
                "first",
                List.of(),
                List.of(sharedCapability),
                List.of(), List.of(), List.of());
        PluginDescriptor second = new PluginDescriptor(
                new PluginId("second"),
                PluginKind.FUNCTIONAL,
                version("1.0.0"),
                API_RANGE,
                "second",
                List.of(),
                List.of(sharedCapability),
                List.of(), List.of(), List.of());

        PluginCatalogResolution result = resolver.resolve(List.of(second, first));

        assertDiagnosticCodes(result, PluginDiagnosticCode.DUPLICATE_CAPABILITY);
        assertEquals(
                List.of("first", "second"),
                result.diagnostics().stream()
                        .map(diagnostic -> diagnostic.pluginId().value())
                        .toList());
    }

    @Test
    void placesAllFunctionalPluginsBeforeCustomizationPlugins() {
        PluginDescriptor customization = descriptor(
                "a_customization", PluginKind.CUSTOMIZATION, "1.0.0", List.of());
        PluginDescriptor functional = descriptor(
                "z_functional", PluginKind.FUNCTIONAL, "1.0.0", List.of());

        PluginCatalogResolution result = resolver.resolve(List.of(customization, functional));

        assertTrue(result.isValid());
        assertEquals(List.of("z_functional", "a_customization"), ids(result));
    }

    @Test
    void rejectsDependenciesTowardCustomizationPlugins() {
        PluginDescriptor firstCustomization = descriptor(
                "first_customization", PluginKind.CUSTOMIZATION, "1.0.0", List.of());
        PluginDescriptor functional = descriptor(
                "functional", PluginKind.FUNCTIONAL, "1.0.0", List.of(dependency(
                        "first_customization", VERSION_ONE, DependencyKind.REQUIRED)));
        PluginDescriptor secondCustomization = descriptor(
                "second_customization", PluginKind.CUSTOMIZATION, "1.0.0", List.of(dependency(
                        "first_customization", VERSION_ONE, DependencyKind.REQUIRED)));

        PluginCatalogResolution result = resolver.resolve(
                List.of(secondCustomization, functional, firstCustomization));

        assertDiagnosticCodes(
                result,
                PluginDiagnosticCode.FUNCTIONAL_DEPENDS_ON_CUSTOMIZATION,
                PluginDiagnosticCode.CUSTOMIZATION_DEPENDS_ON_CUSTOMIZATION);
    }

    @Test
    void acceptsOwnedScreenAndCustomizationOverlayWithRequiredTargetDependency() {
        PluginDescriptor functional = descriptorWithScreens(
                "functional", PluginKind.FUNCTIONAL, List.of(), List.of(screen("functional")), List.of());
        PluginDescriptor customization = descriptorWithScreens(
                "customization",
                PluginKind.CUSTOMIZATION,
                List.of(dependency("functional", VERSION_ONE, DependencyKind.REQUIRED)),
                List.of(),
                List.of(overlay("customization.overlay", "functional")));

        PluginCatalogResolution result = resolver.resolve(List.of(customization, functional));

        assertTrue(result.isValid());
        assertEquals(List.of("functional", "customization"), ids(result));
    }

    @Test
    void rejectsWrongScreenPublishersOwnersAndMissingOverlayDependency() {
        PluginDescriptor functionalWithOverlay = descriptorWithScreens(
                "functional",
                PluginKind.FUNCTIONAL,
                List.of(),
                List.of(screen("other_owner")),
                List.of(overlay("functional.overlay", "functional")));
        PluginDescriptor customizationWithScreen = descriptorWithScreens(
                "customization",
                PluginKind.CUSTOMIZATION,
                List.of(),
                List.of(screen("customization")),
                List.of(overlay("customization.overlay", "functional")));

        PluginCatalogResolution result = resolver.resolve(
                List.of(functionalWithOverlay, customizationWithScreen));

        assertDiagnosticCodes(
                result,
                PluginDiagnosticCode.INVALID_SCREEN_OWNER,
                PluginDiagnosticCode.SCREEN_DEFINITION_REQUIRES_FUNCTIONAL_PLUGIN,
                PluginDiagnosticCode.SCREEN_OVERLAY_REQUIRES_CUSTOMIZATION_PLUGIN,
                PluginDiagnosticCode.SCREEN_OVERLAY_REQUIRES_TARGET_DEPENDENCY);
    }

    @Test
    void rejectsDuplicateDefinitionsElementsSlotsAndOverlayIds() {
        ScreenDefinition duplicatedStructure = new ScreenDefinition(
                new ScreenId(new PluginId("functional"), "dashboard"),
                version("1.0.0"),
                List.of(element(), element()),
                List.of(slot(), slot()));
        ScreenOverlay firstOverlay = overlay("shared.overlay", "functional");
        PluginDescriptor functional = descriptorWithScreens(
                "functional",
                PluginKind.FUNCTIONAL,
                List.of(),
                List.of(duplicatedStructure, duplicatedStructure),
                List.of());
        PluginDescriptor customization = descriptorWithScreens(
                "customization",
                PluginKind.CUSTOMIZATION,
                List.of(dependency("functional", VERSION_ONE, DependencyKind.REQUIRED)),
                List.of(),
                List.of(firstOverlay, firstOverlay));

        PluginCatalogResolution result = resolver.resolve(List.of(functional, customization));

        assertDiagnosticCodes(
                result,
                PluginDiagnosticCode.DUPLICATE_SCREEN_DEFINITION,
                PluginDiagnosticCode.DUPLICATE_SCREEN_ELEMENT,
                PluginDiagnosticCode.DUPLICATE_SCREEN_OVERLAY,
                PluginDiagnosticCode.DUPLICATE_SCREEN_SLOT);
    }

    private static PluginDescriptor descriptor(
            String id, String pluginVersion, List<PluginDependency> dependencies) {
        return descriptor(id, PluginKind.FUNCTIONAL, pluginVersion, dependencies);
    }

    private static PluginDescriptor descriptor(
            String id, PluginKind kind, String pluginVersion, List<PluginDependency> dependencies) {
        return new PluginDescriptor(
                new PluginId(id),
                kind,
                version(pluginVersion),
                API_RANGE,
                id,
                dependencies,
                List.of(), List.of(), List.of(), List.of());
    }

    private static PluginDescriptor descriptorWithScreens(
            String id,
            PluginKind kind,
            List<PluginDependency> dependencies,
            List<ScreenDefinition> screens,
            List<ScreenOverlay> overlays) {
        return new PluginDescriptor(
                new PluginId(id),
                kind,
                version("1.0.0"),
                API_RANGE,
                id,
                dependencies,
                List.of(), List.of(), List.of(), List.of(),
                screens,
                overlays);
    }

    private static ScreenDefinition screen(String owner) {
        return new ScreenDefinition(
                new ScreenId(new PluginId(owner), "dashboard"),
                version("1.0.0"),
                List.of(element()),
                List.of(slot()));
    }

    private static ScreenElementDefinition element() {
        return new ScreenElementDefinition(
                new ScreenElementId("customer"),
                new ScreenRegionId("main"),
                0,
                new ScreenTextKey("functional.customer"),
                Optional.empty(),
                true,
                true,
                false,
                Set.of(ScreenCustomizationOperation.HIDE));
    }

    private static ScreenSlotDefinition slot() {
        return new ScreenSlotDefinition(
                new ScreenSlotId("summary"), new ScreenRegionId("main"), 1, 1);
    }

    private static ScreenOverlay overlay(String id, String targetOwner) {
        return new ScreenOverlay(
                new ContributionId(id),
                new ScreenId(new PluginId(targetOwner), "dashboard"),
                VERSION_ONE,
                List.of(new ScreenChange.Hide(new ScreenElementId("customer"))));
    }

    private static PluginDependency dependency(
            String id, VersionRange compatibleVersions, DependencyKind kind) {
        return new PluginDependency(new PluginId(id), compatibleVersions, kind);
    }

    private static VersionRange range(String minimum, String maximum) {
        return new VersionRange(version(minimum), version(maximum));
    }

    private static SemanticVersion version(String value) {
        return SemanticVersion.parse(value);
    }

    private static List<String> ids(PluginCatalogResolution resolution) {
        return resolution.orderedPlugins().stream().map(plugin -> plugin.id().value()).toList();
    }

    private static void assertDiagnosticCodes(
            PluginCatalogResolution resolution, PluginDiagnosticCode... expected) {
        assertFalse(resolution.isValid());
        assertTrue(resolution.orderedPlugins().isEmpty());
        Set<PluginDiagnosticCode> actual = resolution.diagnostics().stream()
                .map(PluginDiagnostic::code)
                .collect(Collectors.toSet());
        assertEquals(Set.of(expected), actual);
    }
}
