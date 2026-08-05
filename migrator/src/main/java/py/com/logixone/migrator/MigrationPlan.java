package py.com.logixone.migrator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;

record MigrationPlan(List<MigrationTarget> targets) {

    MigrationPlan {
        targets = List.copyOf(targets);
    }

    static MigrationPlan discover() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = contextClassLoader == null
                ? MigrationPlan.class.getClassLoader()
                : contextClassLoader;
        List<PluginDefinition> definitions = ServiceLoader.load(PluginDefinition.class, classLoader)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        return from(definitions);
    }

    static MigrationPlan from(Collection<? extends PluginDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        PluginRegistry registry = PluginRegistry.create(definitions);
        List<MigrationTarget> targets = new ArrayList<>();
        targets.add(new MigrationTarget(
                FlywayMigrationExecutor.CORE_OWNER,
                FlywayMigrationExecutor.CORE_SCHEMA,
                List.of(FlywayMigrationExecutor.CORE_LOCATION)));

        registry.orderedPlugins().forEach(descriptor -> addPluginTarget(targets, descriptor));
        return new MigrationPlan(targets);
    }

    private static void addPluginTarget(List<MigrationTarget> targets, PluginDescriptor descriptor) {
        if (descriptor.migrations().isEmpty()) {
            return;
        }

        String expectedSchema = descriptor.id().schemaName();
        Set<String> uniqueLocations = new HashSet<>();
        List<String> locations = new ArrayList<>();
        for (MigrationContribution contribution : descriptor.migrations()) {
            if (!expectedSchema.equals(contribution.schema())) {
                throw new IllegalArgumentException(
                        "Plugin " + descriptor.id() + " cannot migrate schema " + contribution.schema()
                                + "; expected " + expectedSchema);
            }
            if (!uniqueLocations.add(contribution.location())) {
                throw new IllegalArgumentException(
                        "Plugin " + descriptor.id() + " declares duplicate migration location "
                                + contribution.location());
            }
            locations.add(contribution.location());
        }
        targets.add(new MigrationTarget(descriptor.id().value(), expectedSchema, locations));
    }
}

