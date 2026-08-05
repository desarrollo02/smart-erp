package py.com.logixone.plugin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticVersionTest {

    @Test
    void parsesCompleteSemanticVersion() {
        SemanticVersion version = SemanticVersion.parse("2.10.3-rc.1+build.7");

        assertEquals("2", version.major().toString());
        assertEquals(List.of("rc", "1"), version.preRelease());
        assertEquals("build.7", version.buildMetadata().orElseThrow());
        assertEquals("2.10.3-rc.1+build.7", version.toString());
    }

    @Test
    void implementsSemverPrecedenceIncludingPreReleaseIdentifiers() {
        List<SemanticVersion> versions = List.of(
                SemanticVersion.parse("1.0.0-alpha"),
                SemanticVersion.parse("1.0.0-alpha.1"),
                SemanticVersion.parse("1.0.0-alpha.beta"),
                SemanticVersion.parse("1.0.0-beta"),
                SemanticVersion.parse("1.0.0-beta.2"),
                SemanticVersion.parse("1.0.0-beta.11"),
                SemanticVersion.parse("1.0.0-rc.1"),
                SemanticVersion.parse("1.0.0"));

        for (int index = 0; index < versions.size() - 1; index++) {
            assertTrue(versions.get(index).comparePrecedence(versions.get(index + 1)) < 0);
        }
        assertEquals(
                0,
                SemanticVersion.parse("1.0.0+one")
                        .comparePrecedence(SemanticVersion.parse("1.0.0+two")));
    }

    @Test
    void rejectsNonSemverAndLeadingZeroes() {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("1.0"));
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("01.0.0"));
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("1.0.0-01"));
    }
}
