package py.com.logixone.plugin.api;

import java.util.Objects;

/** Half-open semantic-version range: minimum inclusive and maximum exclusive. */
public record VersionRange(SemanticVersion minimumInclusive, SemanticVersion maximumExclusive) {

    public VersionRange {
        Objects.requireNonNull(minimumInclusive, "minimumInclusive");
        Objects.requireNonNull(maximumExclusive, "maximumExclusive");
        if (minimumInclusive.comparePrecedence(maximumExclusive) >= 0) {
            throw new IllegalArgumentException("Version range minimum must be lower than maximum");
        }
    }

    public boolean contains(SemanticVersion version) {
        Objects.requireNonNull(version, "version");
        return minimumInclusive.comparePrecedence(version) <= 0
                && version.comparePrecedence(maximumExclusive) < 0;
    }

    @Override
    public String toString() {
        return "[" + minimumInclusive + "," + maximumExclusive + ")";
    }
}
