package py.com.logixone.plugins.referencedata.api;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/** Bounded, normalized server-side query over one current normative publication. */
public record ReferenceDataQuery(
        String text,
        int offset,
        int limit,
        boolean enabledOnly) {

    public ReferenceDataQuery {
        Objects.requireNonNull(text, "text");
        text = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .strip()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        if (text.length() > 100) {
            throw new IllegalArgumentException("Reference-data query exceeds 100 characters");
        }
        if (offset < 0 || limit < 1 || limit > 50) {
            throw new IllegalArgumentException(
                    "Reference-data pages must contain between 1 and 50 entries");
        }
    }

    public boolean matches(String... candidates) {
        Objects.requireNonNull(candidates, "candidates");
        if (text.isEmpty()) {
            return true;
        }
        for (String candidate : candidates) {
            if (candidate != null && Normalizer.normalize(candidate, Normalizer.Form.NFKC)
                    .toLowerCase(Locale.ROOT)
                    .contains(text)) {
                return true;
            }
        }
        return false;
    }
}
