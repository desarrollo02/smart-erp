package py.com.logixone.tools.scaffold;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

/** Command-line entry point for the versioned plugin scaffold. */
public final class PluginScaffoldCommand {

    private static final Set<String> ALLOWED_OPTIONS = Set.of(
            "project-root",
            "output",
            "artifact-id",
            "plugin-id",
            "package",
            "display-name",
            "kind",
            "version",
            "target-plugin-id",
            "target-min-version",
            "target-max-version");

    private static final String USAGE = """
            Usage:
              java -jar plugin-scaffold-*-executable.jar \\
                --project-root <repository> \\
                --output <repository/plugins/artifact-id> \\
                --artifact-id <kebab-case> \\
                --plugin-id <snake_case> \\
                --package <java.package> \\
                --display-name <name> \\
                --kind <functional|customization> [--version <semver>] \\
                [--target-plugin-id <snake_case> \\
                 --target-min-version <semver> --target-max-version <semver>]
            """;

    private PluginScaffoldCommand() {
    }

    public static void main(String[] arguments) {
        int exitCode = run(arguments, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] arguments, PrintStream output, PrintStream error) {
        try {
            Map<String, String> options = parse(arguments);
            if (options.containsKey("help")) {
                output.print(USAGE);
                return 0;
            }
            PluginKind kind = PluginKind.valueOf(required(options, "kind").toUpperCase());
            Optional<PluginId> targetId = optional(options, "target-plugin-id")
                    .map(PluginId::new);
            Optional<VersionRange> targetVersions = targetVersionRange(options);
            PluginScaffoldRequest request = new PluginScaffoldRequest(
                    Path.of(required(options, "project-root")),
                    Path.of(required(options, "output")),
                    required(options, "artifact-id"),
                    new PluginId(required(options, "plugin-id")),
                    required(options, "package"),
                    required(options, "display-name"),
                    SemanticVersion.parse(options.getOrDefault("version", "1.0.0")),
                    kind,
                    targetId,
                    targetVersions);
            PluginScaffoldGenerator.GenerationResult result =
                    new PluginScaffoldGenerator().generate(request);
            output.printf(
                    "event=plugin_scaffold_created plugin_id=%s artifact_id=%s kind=%s file_count=%d output=%s%n",
                    request.pluginId().value(),
                    request.artifactId(),
                    request.kind(),
                    result.generatedFiles().size(),
                    result.outputDirectory());
            return 0;
        } catch (IllegalArgumentException exception) {
            error.println("event=plugin_scaffold_rejected reason=" + safeMessage(exception));
            error.print(USAGE);
            return 2;
        } catch (IOException exception) {
            error.println("event=plugin_scaffold_failed reason=" + safeMessage(exception));
            return 1;
        }
    }

    private static Map<String, String> parse(String[] arguments) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 0; index < arguments.length; index++) {
            String argument = arguments[index];
            if (argument.equals("--help")) {
                if (arguments.length != 1) {
                    throw new IllegalArgumentException("--help cannot be combined with other options");
                }
                options.put("help", "true");
                continue;
            }
            if (!argument.startsWith("--") || argument.length() == 2) {
                throw new IllegalArgumentException("Expected --option, found: " + argument);
            }
            if (++index >= arguments.length || arguments[index].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + argument);
            }
            String name = argument.substring(2);
            if (!ALLOWED_OPTIONS.contains(name)) {
                throw new IllegalArgumentException("Unknown option: " + argument);
            }
            if (options.putIfAbsent(name, arguments[index]) != null) {
                throw new IllegalArgumentException("Duplicate option: " + argument);
            }
        }
        return options;
    }

    private static Optional<VersionRange> targetVersionRange(Map<String, String> options) {
        Optional<String> minimum = optional(options, "target-min-version");
        Optional<String> maximum = optional(options, "target-max-version");
        if (minimum.isPresent() != maximum.isPresent()) {
            throw new IllegalArgumentException(
                    "Target minimum and maximum versions must be provided together");
        }
        return minimum.map(value -> new VersionRange(
                SemanticVersion.parse(value),
                SemanticVersion.parse(maximum.orElseThrow())));
    }

    private static String required(Map<String, String> options, String name) {
        return optional(options, name)
                .orElseThrow(() -> new IllegalArgumentException("Missing --" + name));
    }

    private static Optional<String> optional(Map<String, String> options, String name) {
        return Optional.ofNullable(options.get(name));
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.replace('\r', ' ').replace('\n', ' ');
    }
}
