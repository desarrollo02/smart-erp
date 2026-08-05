package py.com.logixone.plugins.commercialcatalog.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/** Typed, item-owned and inactivatable external identifier. */
public record CatalogItemIdentifier(
        CatalogDetailId id,
        String typeCode,
        String presentedValue,
        String normalizedValue,
        boolean active) {

    public CatalogItemIdentifier {
        Objects.requireNonNull(id, "id");
        typeCode = DomainValues.code(typeCode, "identifier type", 32);
        presentedValue = DomainValues.text(presentedValue, "presented identifier", 128);
        normalizedValue = DomainValues.code(normalizedValue, "normalized identifier", 128);
    }

    public static CatalogItemIdentifier active(
            CatalogDetailId id, String typeCode, String presentedValue) {
        String normalized = Normalizer.normalize(
                        Objects.requireNonNull(presentedValue, "presentedValue"),
                        Normalizer.Form.NFKC)
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
        return new CatalogItemIdentifier(id, typeCode, presentedValue, normalized, true);
    }

    public CatalogItemIdentifier inactivate() {
        return active ? new CatalogItemIdentifier(id, typeCode, presentedValue, normalizedValue, false) : this;
    }
}
