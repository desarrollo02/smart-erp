package py.com.logixone.plugins.inventory.api;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Bounded public search for company warehouses. */
public record StorageSearchQuery(
        Optional<String> text,
        boolean activeOnly,
        int offset,
        int limit) {

    public StorageSearchQuery {
        text = Objects.requireNonNull(text, "text").map(value -> {
            String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).strip();
            if (normalized.isEmpty() || normalized.length() > 100
                    || normalized.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Invalid warehouse search text");
            }
            return normalized.toLowerCase(Locale.ROOT);
        });
        if (offset < 0 || offset > 100_000 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid warehouse search page");
        }
    }
}
