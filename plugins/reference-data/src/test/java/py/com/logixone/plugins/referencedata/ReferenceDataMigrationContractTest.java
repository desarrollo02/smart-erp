package py.com.logixone.plugins.referencedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReferenceDataMigrationContractTest {

    @Test
    void migrationMarksTheVerifiedSeedAsAnIncompleteImmutableSubset() throws IOException {
        String sql;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/reference_data/V1__initialize_reference_data_schema.sql")) {
            sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(sql.contains("BOOTSTRAP_SUBSET"));
        assertTrue(sql.contains("748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11"));
        assertTrue(sql.contains("838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9"));
        assertEquals(1, occurrences(sql, "'PY', 'PRY', '600'"));
        assertEquals(1, occurrences(sql, "'PYG', '600', 0"));
        assertEquals(1, occurrences(sql, "'USD', '840', 2"));
        assertTrue(sql.contains("CREATE UNIQUE INDEX uq_catalog_release_current"));
    }

    private static int occurrences(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
