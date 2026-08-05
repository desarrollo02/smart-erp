package py.com.logixone.plugins.businesspartners.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Extensible stable code for types and purposes owned by business_partners. */
public record BusinessPartnerAttributeCode(String value)
        implements Comparable<BusinessPartnerAttributeCode> {

    private static final Pattern FORMAT = Pattern.compile("[a-z][a-z0-9_]{0,47}");

    public BusinessPartnerAttributeCode {
        Objects.requireNonNull(value, "value");
        value = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Attribute code must match " + FORMAT.pattern());
        }
    }

    @Override
    public int compareTo(BusinessPartnerAttributeCode other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
