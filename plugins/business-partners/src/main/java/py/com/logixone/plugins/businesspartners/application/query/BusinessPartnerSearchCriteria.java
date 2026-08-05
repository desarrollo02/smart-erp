package py.com.logixone.plugins.businesspartners.application.query;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;

public record BusinessPartnerSearchCriteria(
        Optional<String> text,
        Optional<BusinessPartnerRole> role,
        Optional<BusinessPartnerState> state,
        int offset,
        int limit) {

    public BusinessPartnerSearchCriteria {
        text = Objects.requireNonNull(text, "text").map(value -> {
            String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
            if (normalized.isEmpty() || normalized.length() > 100
                    || normalized.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Invalid search text");
            }
            return normalized.toLowerCase(Locale.ROOT);
        });
        role = Objects.requireNonNull(role, "role");
        state = Objects.requireNonNull(state, "state");
        if (offset < 0 || offset > 100_000) {
            throw new IllegalArgumentException("offset must be between 0 and 100000");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }
}
