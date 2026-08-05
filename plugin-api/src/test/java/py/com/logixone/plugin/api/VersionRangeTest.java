package py.com.logixone.plugin.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionRangeTest {

    @Test
    void isInclusiveAtMinimumAndExclusiveAtMaximum() {
        VersionRange range = new VersionRange(
                SemanticVersion.parse("1.2.0"), SemanticVersion.parse("2.0.0"));

        assertTrue(range.contains(SemanticVersion.parse("1.2.0")));
        assertTrue(range.contains(SemanticVersion.parse("1.9.9")));
        assertFalse(range.contains(SemanticVersion.parse("2.0.0")));
    }

    @Test
    void requiresIncreasingExplicitBounds() {
        SemanticVersion version = SemanticVersion.parse("1.0.0");
        assertThrows(IllegalArgumentException.class, () -> new VersionRange(version, version));
    }
}
