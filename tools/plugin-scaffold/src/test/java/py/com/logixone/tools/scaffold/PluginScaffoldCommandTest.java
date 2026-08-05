package py.com.logixone.tools.scaffold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginScaffoldCommandTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsPluginFromCommandLine() throws IOException {
        Path root = repository();
        Path output = root.resolve("plugins/inventory");
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream standardError = new ByteArrayOutputStream();

        int exitCode = PluginScaffoldCommand.run(new String[] {
                "--project-root", root.toString(),
                "--output", output.toString(),
                "--artifact-id", "inventory",
                "--plugin-id", "inventory",
                "--package", "py.com.logixone.plugins.inventory",
                "--display-name", "Inventory",
                "--kind", "functional"
        }, stream(standardOutput), stream(standardError));

        assertEquals(0, exitCode);
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8)
                .contains("event=plugin_scaffold_created"));
        assertEquals("", standardError.toString(StandardCharsets.UTF_8));
        assertTrue(Files.isRegularFile(output.resolve("pom.xml")));
    }

    @Test
    void rejectsUnknownOptionWithoutCreatingTarget() throws IOException {
        Path root = repository();
        Path output = root.resolve("plugins/inventory");
        ByteArrayOutputStream standardError = new ByteArrayOutputStream();

        int exitCode = PluginScaffoldCommand.run(new String[] {
                "--project-root", root.toString(),
                "--output", output.toString(),
                "--artifact-id", "inventory",
                "--plugin-id", "inventory",
                "--package", "py.com.logixone.plugins.inventory",
                "--display-name", "Inventory",
                "--kind", "functional",
                "--surprise", "value"
        }, stream(new ByteArrayOutputStream()), stream(standardError));

        assertEquals(2, exitCode);
        assertTrue(standardError.toString(StandardCharsets.UTF_8)
                .contains("Unknown option"));
        assertFalse(Files.exists(output));
    }

    @Test
    void customizationRequiresTargetAndRange() throws IOException {
        Path root = repository();
        Path output = root.resolve("plugins/acme-customization");
        ByteArrayOutputStream standardError = new ByteArrayOutputStream();

        int exitCode = PluginScaffoldCommand.run(new String[] {
                "--project-root", root.toString(),
                "--output", output.toString(),
                "--artifact-id", "acme-customization",
                "--plugin-id", "acme_customization",
                "--package", "py.com.logixone.plugins.acme",
                "--display-name", "Acme customization",
                "--kind", "customization"
        }, stream(new ByteArrayOutputStream()), stream(standardError));

        assertEquals(2, exitCode);
        assertTrue(standardError.toString(StandardCharsets.UTF_8)
                .contains("require target plugin"));
        assertFalse(Files.exists(output));
    }

    private Path repository() throws IOException {
        Path root = temporaryDirectory.resolve("repository");
        Files.createDirectories(root.resolve("plugins"));
        Files.writeString(root.resolve("pom.xml"), "<project/>\n", StandardCharsets.UTF_8);
        return root;
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }
}
