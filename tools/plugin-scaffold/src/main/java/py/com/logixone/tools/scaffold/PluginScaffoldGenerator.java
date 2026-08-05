package py.com.logixone.tools.scaffold;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import py.com.logixone.plugin.api.PluginKind;

/** Writes a complete plugin skeleton through a staging directory and a final move. */
public final class PluginScaffoldGenerator {

    private static final int WINDOWS_MOVE_ATTEMPTS = 6;
    private static final long WINDOWS_MOVE_RETRY_MILLIS = 50L;
    private static final Pattern UNRESOLVED_TOKEN = Pattern.compile("\\{\\{[A-Z0-9_]+}}") ;
    private static final List<TemplateFile> COMMON_FILES = List.of(
            new TemplateFile("plugin/pom.xml.tpl", "pom.xml"),
            new TemplateFile("plugin/beans.xml.tpl", "src/main/resources/META-INF/beans.xml"),
            new TemplateFile(
                    "plugin/README.md.tpl",
                    "README.md"),
            new TemplateFile(
                    "plugin/contract.md.tpl",
                    "docs/plugin-contract.md"));

    public GenerationResult generate(PluginScaffoldRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        Path projectRoot = request.projectRoot().toRealPath();
        if (!Files.isRegularFile(projectRoot.resolve("pom.xml"))) {
            throw new IllegalArgumentException("projectRoot must contain pom.xml");
        }

        Path output = request.outputDirectory();
        Path outputParent = output.getParent();
        if (outputParent == null || !Files.isDirectory(outputParent)) {
            throw new IllegalArgumentException("Output parent directory must already exist");
        }
        Path realParent = outputParent.toRealPath();
        if (!realParent.startsWith(projectRoot)) {
            throw new IllegalArgumentException("Output directory must stay inside projectRoot");
        }
        output = realParent.resolve(output.getFileName()).normalize();
        if (Files.exists(output)) {
            throw new IllegalArgumentException("Output directory already exists: " + output);
        }

        Path staging = Files.createTempDirectory(realParent, ".plugin-scaffold-");
        try {
            Map<String, String> tokens = tokens(request, output, projectRoot);
            List<String> generatedFiles = new ArrayList<>();
            for (TemplateFile template : COMMON_FILES) {
                writeTemplate(staging, template, tokens, generatedFiles);
            }
            String kind = request.kind() == PluginKind.FUNCTIONAL
                    ? "functional"
                    : "customization";
            writeTemplate(
                    staging,
                    new TemplateFile(
                            "plugin/definition-" + kind + ".java.tpl",
                            "src/main/java/" + request.packagePath() + "/"
                                    + request.definitionClassName() + ".java"),
                    tokens,
                    generatedFiles);
            writeTemplate(
                    staging,
                    new TemplateFile(
                            "plugin/definition-" + kind + "-test.java.tpl",
                            "src/test/java/" + request.packagePath() + "/"
                                    + request.definitionClassName() + "Test.java"),
                    tokens,
                    generatedFiles);
            writeText(
                    staging,
                    "src/main/resources/META-INF/services/py.com.logixone.plugin.api.PluginDefinition",
                    request.packageName() + "." + request.definitionClassName() + "\n",
                    generatedFiles);

            moveCompletedTree(staging, output);
            generatedFiles.sort(String::compareTo);
            return new GenerationResult(output, generatedFiles);
        } catch (IOException | RuntimeException failure) {
            try {
                deleteTree(staging);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private static Map<String, String> tokens(
            PluginScaffoldRequest request,
            Path output,
            Path projectRoot) {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("ARTIFACT_ID", request.artifactId());
        tokens.put("DISPLAY_NAME", request.displayName());
        tokens.put("DISPLAY_NAME_XML", escapeXml(request.displayName()));
        tokens.put("DISPLAY_NAME_JAVA", escapeJava(request.displayName()));
        tokens.put("PLUGIN_ID", request.pluginId().value());
        tokens.put("PACKAGE_NAME", request.packageName());
        tokens.put("DEFINITION_CLASS", request.definitionClassName());
        tokens.put("PLUGIN_VERSION", request.version().toString());
        tokens.put("PLUGIN_KIND", request.kind().name());
        tokens.put("PLUGIN_API_MIN", request.pluginApiMinimum().toString());
        tokens.put("PLUGIN_API_MAX", request.pluginApiMaximum().toString());
        tokens.put(
                "PARENT_RELATIVE_PATH",
                output.relativize(projectRoot.resolve("pom.xml")).toString().replace('\\', '/'));
        tokens.put(
                "CUSTOMIZATION_DETAILS",
                request.kind() == PluginKind.CUSTOMIZATION
                        ? "- Target funcional: `" + request.targetPluginId().orElseThrow().value()
                                + "` en `" + request.targetPluginVersions().orElseThrow() + "`.\n"
                                + "- Agregue overlays sólo mediante `ScreenOverlay` y contratos públicos."
                        : "- Este esqueleto funcional nace sin capacidades, permisos, menús, pantallas ni migraciones.\n"
                                + "- Agréguelos únicamente desde requisitos y contratos aprobados.");
        request.targetPluginId().ifPresent(value -> tokens.put("TARGET_PLUGIN_ID", value.value()));
        request.targetPluginVersions().ifPresent(value -> {
            tokens.put("TARGET_VERSION_MIN", value.minimumInclusive().toString());
            tokens.put("TARGET_VERSION_MAX", value.maximumExclusive().toString());
        });
        return Map.copyOf(tokens);
    }

    private static void writeTemplate(
            Path root,
            TemplateFile template,
            Map<String, String> tokens,
            List<String> generatedFiles) throws IOException {
        String rendered = readTemplate(template.resource());
        for (Map.Entry<String, String> token : tokens.entrySet()) {
            rendered = rendered.replace("{{" + token.getKey() + "}}", token.getValue());
        }
        if (UNRESOLVED_TOKEN.matcher(rendered).find()) {
            throw new IllegalStateException("Unresolved token in template " + template.resource());
        }
        writeText(root, template.output(), rendered, generatedFiles);
    }

    private static void writeText(
            Path root,
            String relativePath,
            String content,
            List<String> generatedFiles) throws IOException {
        Path destination = root.resolve(relativePath).normalize();
        if (!destination.startsWith(root)) {
            throw new IllegalArgumentException("Template output escapes staging directory");
        }
        Files.createDirectories(destination.getParent());
        Files.writeString(
                destination,
                normalizeLines(content),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        generatedFiles.add(relativePath.replace('\\', '/'));
    }

    private static String readTemplate(String resource) throws IOException {
        try (InputStream input = PluginScaffoldGenerator.class
                .getClassLoader()
                .getResourceAsStream("templates/" + resource)) {
            if (input == null) {
                throw new IOException("Missing scaffold template: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String normalizeLines(String value) {
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        return normalized.endsWith("\n") ? normalized : normalized + "\n";
    }

    private static String escapeJava(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static void moveCompletedTree(Path source, Path target) throws IOException {
        boolean atomicMove = true;
        AccessDeniedException lastAccessDenied = null;
        for (int attempt = 1; attempt <= WINDOWS_MOVE_ATTEMPTS; attempt++) {
            try {
                if (atomicMove) {
                    Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
                } else {
                    Files.move(source, target);
                }
                return;
            } catch (AtomicMoveNotSupportedException ignored) {
                atomicMove = false;
            } catch (AccessDeniedException failure) {
                lastAccessDenied = failure;
                atomicMove = false;
                if (Files.exists(target) || attempt == WINDOWS_MOVE_ATTEMPTS) {
                    throw failure;
                }
                waitBeforeMoveRetry(attempt, failure);
            }
        }
        throw lastAccessDenied == null
                ? new IOException("Unable to move completed plugin tree to " + target)
                : lastAccessDenied;
    }

    private static void waitBeforeMoveRetry(int attempt, IOException failure) throws IOException {
        try {
            Thread.sleep(WINDOWS_MOVE_RETRY_MILLIS * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            IOException aborted = new IOException("Interrupted while retrying plugin tree move", interrupted);
            aborted.addSuppressed(failure);
            throw aborted;
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record TemplateFile(String resource, String output) {
    }

    public record GenerationResult(Path outputDirectory, List<String> generatedFiles) {

        public GenerationResult {
            Objects.requireNonNull(outputDirectory, "outputDirectory");
            generatedFiles = List.copyOf(generatedFiles);
        }
    }
}
