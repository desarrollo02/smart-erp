package py.com.logixone.tools.scaffold;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import py.com.logixone.plugin.api.PluginApiVersion;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

/** Validated inputs for a deterministic plugin module generation. */
public record PluginScaffoldRequest(
        Path projectRoot,
        Path outputDirectory,
        String artifactId,
        PluginId pluginId,
        String packageName,
        String displayName,
        SemanticVersion version,
        PluginKind kind,
        Optional<PluginId> targetPluginId,
        Optional<VersionRange> targetPluginVersions) {

    private static final Pattern ARTIFACT_ID =
            Pattern.compile("[a-z][a-z0-9]*(?:-[a-z0-9]+)*");
    private static final Set<String> RESERVED_PLUGIN_IDS =
            Set.of("core", "kernel", "logixone", "plugin_api");
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "_", "true",
            "false", "null", "record", "sealed", "permits", "non-sealed", "var",
            "yield");

    public PluginScaffoldRequest {
        projectRoot = normalize(Objects.requireNonNull(projectRoot, "projectRoot"));
        outputDirectory = normalize(Objects.requireNonNull(outputDirectory, "outputDirectory"));
        artifactId = requireArtifactId(artifactId);
        Objects.requireNonNull(pluginId, "pluginId");
        packageName = requirePackageName(packageName);
        displayName = requireDisplayName(displayName);
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(kind, "kind");
        targetPluginId = Objects.requireNonNull(targetPluginId, "targetPluginId");
        targetPluginVersions = Objects.requireNonNull(targetPluginVersions, "targetPluginVersions");

        if (RESERVED_PLUGIN_IDS.contains(pluginId.value())) {
            throw new IllegalArgumentException("Plugin id is reserved: " + pluginId.value());
        }
        if (!outputDirectory.getFileName().toString().equals(artifactId)) {
            throw new IllegalArgumentException("Output directory name must equal artifactId");
        }
        if (kind == PluginKind.FUNCTIONAL
                && (targetPluginId.isPresent() || targetPluginVersions.isPresent())) {
            throw new IllegalArgumentException("Functional plugins cannot declare a customization target");
        }
        if (kind == PluginKind.CUSTOMIZATION
                && (targetPluginId.isEmpty() || targetPluginVersions.isEmpty())) {
            throw new IllegalArgumentException(
                    "Customization plugins require target plugin id and version range");
        }
        if (targetPluginId.filter(pluginId::equals).isPresent()) {
            throw new IllegalArgumentException("A customization plugin cannot target itself");
        }
    }

    public String definitionClassName() {
        StringBuilder result = new StringBuilder();
        for (String part : pluginId.value().split("_")) {
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.append("PluginDefinition").toString();
    }

    public String packagePath() {
        return packageName.replace('.', '/');
    }

    public SemanticVersion pluginApiMinimum() {
        return PluginApiVersion.CURRENT;
    }

    public SemanticVersion pluginApiMaximum() {
        SemanticVersion current = PluginApiVersion.CURRENT;
        return SemanticVersion.parse(
                current.major() + "." + current.minor().add(BigInteger.ONE) + ".0");
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static String requireArtifactId(String value) {
        Objects.requireNonNull(value, "artifactId");
        if (!ARTIFACT_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "artifactId must use lower-case kebab-case: " + value);
        }
        return value;
    }

    private static String requirePackageName(String value) {
        Objects.requireNonNull(value, "packageName");
        if (value.isBlank()) {
            throw new IllegalArgumentException("packageName must not be blank");
        }
        for (String part : value.split("\\.", -1)) {
            if (part.isEmpty()
                    || JAVA_KEYWORDS.contains(part)
                    || !Character.isJavaIdentifierStart(part.charAt(0))
                    || part.chars().skip(1).anyMatch(character -> !Character.isJavaIdentifierPart(character))) {
                throw new IllegalArgumentException("Invalid Java package: " + value);
            }
        }
        return value;
    }

    private static String requireDisplayName(String value) {
        Objects.requireNonNull(value, "displayName");
        if (value.isBlank() || value.length() > 128 || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "displayName must contain 1 to 128 printable characters");
        }
        return value;
    }
}
