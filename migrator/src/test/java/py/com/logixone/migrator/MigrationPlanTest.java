package py.com.logixone.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.plugin.InvalidPluginCatalogException;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

class MigrationPlanTest {

    private static final VersionRange API_RANGE = new VersionRange(
            SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0"));
    private static final VersionRange VERSION_ONE = new VersionRange(
            SemanticVersion.parse("1.0.0"), SemanticVersion.parse("2.0.0"));

    @Test
    void alwaysStartsWithCoreWhenNoPluginsArePresent() {
        MigrationPlan plan = MigrationPlan.from(List.of());

        assertEquals(1, plan.targets().size());
        assertEquals("kernel", plan.targets().getFirst().owner());
        assertEquals("core", plan.targets().getFirst().schema());
        assertEquals(List.of("classpath:db/migration/core"), plan.targets().getFirst().locations());
    }

    @Test
    void serviceDiscoveryAlwaysProducesAValidPlanForTheSelectedComposition() {
        MigrationPlan plan = MigrationPlan.discover();

        assertEquals("core", plan.targets().getFirst().schema());
        plan.targets().stream().skip(1).forEach(target ->
                assertEquals("plg_" + target.owner(), target.schema()));
    }

    @Test
    void ordersPluginsTopologicallyAndGroupsTheirLocations() {
        PluginDefinition foundation = definition(
                "foundation",
                List.of(),
                List.of(
                        migration("foundation", "base"),
                        migration("foundation", "extension")));
        PluginDefinition operation = definition(
                "operation",
                List.of(new PluginDependency(
                        new PluginId("foundation"), VERSION_ONE, DependencyKind.REQUIRED)),
                List.of(migration("operation", "base")));

        MigrationPlan plan = MigrationPlan.from(List.of(operation, foundation));

        assertEquals(List.of("kernel", "foundation", "operation"),
                plan.targets().stream().map(MigrationTarget::owner).toList());
        assertEquals(
                List.of(
                        "classpath:db/migration/foundation/base",
                        "classpath:db/migration/foundation/extension"),
                plan.targets().get(1).locations());
    }

    @Test
    void rejectsAPluginTryingToOwnAnotherSchema() {
        PluginDefinition definition = definition(
                "foundation",
                List.of(),
                List.of(new MigrationContribution(
                        "plg_operation",
                        "classpath:db/migration/foundation")));

        assertThrows(InvalidPluginCatalogException.class, () -> MigrationPlan.from(List.of(definition)));
    }

    @Test
    void rejectsDuplicateLocationsWithinAPlugin() {
        MigrationContribution migration = migration("foundation", "base");
        PluginDefinition definition = definition(
                "foundation",
                List.of(),
                List.of(migration, migration));

        assertThrows(InvalidPluginCatalogException.class, () -> MigrationPlan.from(List.of(definition)));
    }

    @Test
    void rejectsAnInvalidPhysicalCatalogBeforeProducingAPlan() {
        PluginDefinition orphan = definition(
                "operation",
                List.of(new PluginDependency(
                        new PluginId("missing"), VERSION_ONE, DependencyKind.REQUIRED)),
                List.of(migration("operation", "base")));

        assertThrows(InvalidPluginCatalogException.class, () -> MigrationPlan.from(List.of(orphan)));
    }

    private static MigrationContribution migration(String pluginId, String suffix) {
        return new MigrationContribution(
                "plg_" + pluginId,
                "classpath:db/migration/" + pluginId + "/" + suffix);
    }

    private static PluginDefinition definition(
            String id,
            List<PluginDependency> dependencies,
            List<MigrationContribution> migrations) {
        PluginDescriptor descriptor = new PluginDescriptor(
                new PluginId(id),
                PluginKind.FUNCTIONAL,
                SemanticVersion.parse("1.0.0"),
                API_RANGE,
                id,
                dependencies,
                List.of(),
                List.of(),
                List.of(),
                migrations);
        return () -> descriptor;
    }
}
