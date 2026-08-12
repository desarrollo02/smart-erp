package py.com.logixone.plugins.businesspartners.api;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Bounded, company-scoped search criteria for public partner selectors. */
public record BusinessPartnerSearchQuery(
        Optional<String> text,
        Optional<BusinessPartnerRole> role,
        Optional<BusinessPartnerState> state,
        int offset,
        int limit) {

    public BusinessPartnerSearchQuery {
        text = Objects.requireNonNull(text, "text").map(value -> {
            String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).strip();
            if (normalized.isEmpty() || normalized.length() > 100
                    || normalized.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Invalid search text");
            }
            return normalized.toLowerCase(Locale.ROOT);
        });
        role = Objects.requireNonNull(role, "role");
        state = Objects.requireNonNull(state, "state");
        if (offset < 0 || offset > 100_000 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Invalid business partner search page");
        }
    }
}
