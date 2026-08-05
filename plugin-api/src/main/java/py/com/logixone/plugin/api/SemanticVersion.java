package py.com.logixone.plugin.api;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable SemVer 2.0.0 value. Build metadata does not affect precedence. */
public final class SemanticVersion {

    private static final Pattern SEMVER = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
                    + "(?:-((?:0|[1-9]\\d*|\\d*[A-Za-z-][0-9A-Za-z-]*)"
                    + "(?:\\.(?:0|[1-9]\\d*|\\d*[A-Za-z-][0-9A-Za-z-]*))*))?"
                    + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$");

    private final BigInteger major;
    private final BigInteger minor;
    private final BigInteger patch;
    private final List<String> preRelease;
    private final String buildMetadata;
    private final String value;

    private SemanticVersion(
            BigInteger major,
            BigInteger minor,
            BigInteger patch,
            List<String> preRelease,
            String buildMetadata,
            String value) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = List.copyOf(preRelease);
        this.buildMetadata = buildMetadata;
        this.value = value;
    }

    public static SemanticVersion parse(String value) {
        Objects.requireNonNull(value, "value");
        Matcher matcher = SEMVER.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid semantic version: " + value);
        }
        String preRelease = matcher.group(4);
        return new SemanticVersion(
                new BigInteger(matcher.group(1)),
                new BigInteger(matcher.group(2)),
                new BigInteger(matcher.group(3)),
                preRelease == null ? List.of() : List.of(preRelease.split("\\.")),
                matcher.group(5),
                value);
    }

    public BigInteger major() {
        return major;
    }

    public BigInteger minor() {
        return minor;
    }

    public BigInteger patch() {
        return patch;
    }

    public List<String> preRelease() {
        return preRelease;
    }

    public Optional<String> buildMetadata() {
        return Optional.ofNullable(buildMetadata);
    }

    public int comparePrecedence(SemanticVersion other) {
        Objects.requireNonNull(other, "other");
        int result = major.compareTo(other.major);
        if (result == 0) {
            result = minor.compareTo(other.minor);
        }
        if (result == 0) {
            result = patch.compareTo(other.patch);
        }
        if (result != 0) {
            return result;
        }
        if (preRelease.isEmpty() && other.preRelease.isEmpty()) {
            return 0;
        }
        if (preRelease.isEmpty()) {
            return 1;
        }
        if (other.preRelease.isEmpty()) {
            return -1;
        }
        int sharedLength = Math.min(preRelease.size(), other.preRelease.size());
        for (int index = 0; index < sharedLength; index++) {
            result = compareIdentifier(preRelease.get(index), other.preRelease.get(index));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(preRelease.size(), other.preRelease.size());
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = isNumeric(left);
        boolean rightNumeric = isNumeric(right);
        if (leftNumeric && rightNumeric) {
            return new BigInteger(left).compareTo(new BigInteger(right));
        }
        if (leftNumeric) {
            return -1;
        }
        if (rightNumeric) {
            return 1;
        }
        return left.compareTo(right);
    }

    private static boolean isNumeric(String value) {
        return value.chars().allMatch(Character::isDigit);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof SemanticVersion version && value.equals(version.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
