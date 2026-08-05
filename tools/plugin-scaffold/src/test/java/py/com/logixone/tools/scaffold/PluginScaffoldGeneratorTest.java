package py.com.logixone.tools.scaffold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

class PluginScaffoldGeneratorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void generatesCompilableFunctionalPluginAndDiscoverableProvider() throws Exception {
        Path root = repository("functional");
        PluginScaffoldRequest request = functionalRequest(root, "business-partners");

        PluginScaffoldGenerator.GenerationResult result =
                new PluginScaffoldGenerator().generate(request);

        assertEquals(7, result.generatedFiles().size());
        assertTrue(Files.isRegularFile(result.outputDirectory().resolve("pom.xml")));
        String definition = Files.readString(
                result.outputDirectory().resolve(
                        "src/main/java/py/com/logixone/plugins/businesspartners/"
                                + "BusinessPartnersPluginDefinition.java"));
        assertTrue(definition.contains("PluginKind.FUNCTIONAL"));
        assertFalse(definition.contains("py.com.logixone.kernel"));
        assertFalse(definition.contains("javax."));

        Path classes = compileGeneratedSources(result.outputDirectory());
        copyProviderResource(result.outputDirectory(), classes);
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] {classes.toUri().toURL()}, getClass().getClassLoader())) {
            List<PluginDefinition> providers =
                    ServiceLoader.load(PluginDefinition.class, loader).stream()
                            .map(ServiceLoader.Provider::get)
                            .toList();
            assertEquals(1, providers.size());
            assertEquals("business_partners", providers.getFirst().descriptor().id().value());
            assertEquals(PluginKind.FUNCTIONAL, providers.getFirst().descriptor().kind());
        }
    }

    @Test
    void generatesCustomizationWithExplicitFunctionalDependency() throws Exception {
        Path root = repository("customization");
        Path output = root.resolve("plugins/acme-customization");
        PluginScaffoldRequest request = new PluginScaffoldRequest(
                root,
                output,
                "acme-customization",
                new PluginId("acme_customization"),
                "py.com.logixone.plugins.acme",
                "Acme customization",
                SemanticVersion.parse("1.0.0"),
                PluginKind.CUSTOMIZATION,
                Optional.of(new PluginId("business_partners")),
                Optional.of(new VersionRange(
                        SemanticVersion.parse("1.0.0"),
                        SemanticVersion.parse("2.0.0"))));

        PluginScaffoldGenerator.GenerationResult result =
                new PluginScaffoldGenerator().generate(request);

        String definition = Files.readString(result.outputDirectory().resolve(
                "src/main/java/py/com/logixone/plugins/acme/AcmeCustomizationPluginDefinition.java"));
        assertTrue(definition.contains("PluginKind.CUSTOMIZATION"));
        assertTrue(definition.contains("new PluginId(\"business_partners\")"));
        assertTrue(definition.contains("DependencyKind.REQUIRED"));
        compileGeneratedSources(result.outputDirectory());
    }

    @Test
    void rendersIdenticalTextForIdenticalInputs() throws Exception {
        Path firstRoot = repository("deterministic-a");
        Path secondRoot = repository("deterministic-b");

        Path first = new PluginScaffoldGenerator()
                .generate(functionalRequest(firstRoot, "inventory"))
                .outputDirectory();
        Path second = new PluginScaffoldGenerator()
                .generate(functionalRequest(secondRoot, "inventory"))
                .outputDirectory();

        assertEquals(readTree(first), readTree(second));
    }

    @Test
    void rejectsExistingTargetWithoutChangingItsContent() throws Exception {
        Path root = repository("existing");
        Path target = root.resolve("plugins/inventory");
        Files.createDirectory(target);
        Files.writeString(target.resolve("owner.txt"), "user-content", StandardCharsets.UTF_8);

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new PluginScaffoldGenerator().generate(functionalRequest(root, "inventory")));

        assertTrue(failure.getMessage().contains("already exists"));
        assertEquals("user-content", Files.readString(target.resolve("owner.txt")));
        assertEquals(1, Files.list(target).count());
    }

    @Test
    void rejectsOutputOutsideProjectAndReservedIdentity() throws Exception {
        Path root = repository("boundaries");
        PluginScaffoldRequest outside = new PluginScaffoldRequest(
                root,
                temporaryDirectory.resolve("outside"),
                "outside",
                new PluginId("outside"),
                "py.com.logixone.plugins.outside",
                "Outside",
                SemanticVersion.parse("1.0.0"),
                PluginKind.FUNCTIONAL,
                Optional.empty(),
                Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> new PluginScaffoldGenerator().generate(outside));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PluginScaffoldRequest(
                        root,
                        root.resolve("plugins/core-plugin"),
                        "core-plugin",
                        new PluginId("core"),
                        "py.com.logixone.plugins.core",
                        "Core",
                        SemanticVersion.parse("1.0.0"),
                        PluginKind.FUNCTIONAL,
                        Optional.empty(),
                        Optional.empty()));
    }

    @Test
    void rejectsInvalidKindSpecificInputs() throws Exception {
        Path root = repository("kind-inputs");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PluginScaffoldRequest(
                        root,
                        root.resolve("plugins/acme-customization"),
                        "acme-customization",
                        new PluginId("acme_customization"),
                        "py.com.logixone.plugins.acme",
                        "Acme customization",
                        SemanticVersion.parse("1.0.0"),
                        PluginKind.CUSTOMIZATION,
                        Optional.empty(),
                        Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PluginScaffoldRequest(
                        root,
                        root.resolve("plugins/inventory"),
                        "inventory",
                        new PluginId("inventory"),
                        "py.com.logixone.plugins.inventory",
                        "Inventory",
                        SemanticVersion.parse("1.0.0"),
                        PluginKind.FUNCTIONAL,
                        Optional.of(new PluginId("business_partners")),
                        Optional.of(new VersionRange(
                                SemanticVersion.parse("1.0.0"),
                                SemanticVersion.parse("2.0.0")))));
    }

    private Path repository(String name) throws IOException {
        Path root = temporaryDirectory.resolve(name);
        Files.createDirectories(root.resolve("plugins"));
        Files.writeString(root.resolve("pom.xml"), "<project/>\n", StandardCharsets.UTF_8);
        return root;
    }

    private static PluginScaffoldRequest functionalRequest(Path root, String artifactId) {
        String pluginId = artifactId.replace('-', '_');
        String compactName = artifactId.replace("-", "");
        return new PluginScaffoldRequest(
                root,
                root.resolve("plugins").resolve(artifactId),
                artifactId,
                new PluginId(pluginId),
                "py.com.logixone.plugins." + compactName,
                "Generated " + artifactId,
                SemanticVersion.parse("1.0.0"),
                PluginKind.FUNCTIONAL,
                Optional.empty(),
                Optional.empty());
    }

    private static Path compileGeneratedSources(Path module) throws IOException {
        Path classes = module.resolve("target/compile-check");
        Files.createDirectories(classes);
        List<String> sources;
        try (var paths = Files.walk(module.resolve("src"))) {
            sources = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }
        ToolProvider javac = ToolProvider.findFirst("javac").orElseThrow();
        List<String> arguments = new java.util.ArrayList<>(List.of(
                "--release", "21",
                "-classpath", System.getProperty("java.class.path"),
                "-d", classes.toString()));
        arguments.addAll(sources);
        int exitCode = javac.run(System.out, System.err, arguments.toArray(String[]::new));
        assertEquals(0, exitCode, "generated Java sources must compile");
        return classes;
    }

    private static void copyProviderResource(Path module, Path classes) throws IOException {
        Path relative = Path.of(
                "META-INF/services/py.com.logixone.plugin.api.PluginDefinition");
        Path target = classes.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.copy(module.resolve("src/main/resources").resolve(relative), target);
    }

    private static Map<String, String> readTree(Path root) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                if (!path.startsWith(root.resolve("target"))) {
                    result.put(
                            root.relativize(path).toString().replace('\\', '/'),
                            Files.readString(path, StandardCharsets.UTF_8));
                }
            }
        }
        return result;
    }
}
