package py.com.logixone.plugins.referencedata.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;

/** Immutable provenance of one observed normative publication. */
public record ReferenceDataRelease(
        ReferenceDataCatalog catalog,
        String releaseId,
        String standardId,
        String authority,
        URI sourceUri,
        String sourceSha256,
        LocalDate observedOn,
        CatalogCompleteness completeness,
        int entryCount) {

    public ReferenceDataRelease {
        Objects.requireNonNull(catalog, "catalog");
        releaseId = text(releaseId, "releaseId", 64);
        standardId = text(standardId, "standardId", 64);
        authority = text(authority, "authority", 160);
        Objects.requireNonNull(sourceUri, "sourceUri");
        if (!"https".equalsIgnoreCase(sourceUri.getScheme())) {
            throw new IllegalArgumentException("sourceUri must use HTTPS");
        }
        sourceSha256 = Objects.requireNonNull(sourceSha256, "sourceSha256")
                .strip().toLowerCase(java.util.Locale.ROOT);
        if (!sourceSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sourceSha256 must be lowercase SHA-256");
        }
        Objects.requireNonNull(observedOn, "observedOn");
        Objects.requireNonNull(completeness, "completeness");
        if (entryCount < 1) {
            throw new IllegalArgumentException("entryCount must be positive");
        }
    }

    private static String text(String value, String field, int maximumLength) {
        value = Objects.requireNonNull(value, field).strip();
        if (value.isEmpty() || value.length() > maximumLength
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return value;
    }
}
