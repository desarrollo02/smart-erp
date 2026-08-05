package py.com.logixone.kernel.infrastructure.jakarta.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.plugin.InvalidPluginCatalogException;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

class CdiPluginCatalogTest {

    private static final VersionRange API_RANGE = new VersionRange(
            SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0"));

    @Test
    void bootstrapAcceptsNoDiscoveredPlugins() {
        PluginRegistry registry = CdiPluginCatalog.bootstrap(Stream.empty());

        assertEquals(0, registry.size());
    }

    @Test
    void bootstrapRegistersDiscoveredDefinitions() {
        PluginRegistry registry = CdiPluginCatalog.bootstrap(Stream.of(definition("reference_plugin", "1.0.0")));

        assertEquals(1, registry.size());
        assertEquals("reference_plugin", registry.orderedPlugins().getFirst().id().value());
    }

    @Test
    void bootstrapRejectsTheSameInvalidCatalogUsedAtRuntime() {
        assertThrows(
                InvalidPluginCatalogException.class,
                () -> CdiPluginCatalog.bootstrap(Stream.of(
                        definition("duplicate_plugin", "1.0.0"),
                        definition("duplicate_plugin", "1.1.0"))));
    }

    @Test
    void classDeclaresTheCdiScopeInjectionAndStartupObserver() throws Exception {
        assertNotNull(CdiPluginCatalog.class.getAnnotation(ApplicationScoped.class));
        assertNotNull(CdiPluginCatalog.class.getDeclaredField("definitions").getAnnotation(Inject.class));
        Method initialize = CdiPluginCatalog.class.getDeclaredMethod("initialize", Object.class);
        Annotation[] annotations = initialize.getParameterAnnotations()[0];
        assertNotNull(annotation(annotations, Observes.class));
        Initialized initialized = annotation(annotations, Initialized.class);
        assertNotNull(initialized);
        assertEquals(ApplicationScoped.class, initialized.value());

        CdiPluginCatalog catalog = new CdiPluginCatalog();
        assertFalse(catalog.isInitialized());
        assertThrows(IllegalStateException.class, catalog::registry);
    }

    private static PluginDefinition definition(String id, String version) {
        PluginDescriptor descriptor = new PluginDescriptor(
                new PluginId(id),
                PluginKind.FUNCTIONAL,
                SemanticVersion.parse(version),
                API_RANGE,
                id,
                List.of(), List.of(), List.of(), List.of(), List.of());
        return () -> descriptor;
    }

    private static <T extends Annotation> T annotation(Annotation[] annotations, Class<T> type) {
        for (Annotation annotation : annotations) {
            if (type.isInstance(annotation)) {
                return type.cast(annotation);
            }
        }
        return null;
    }
}
