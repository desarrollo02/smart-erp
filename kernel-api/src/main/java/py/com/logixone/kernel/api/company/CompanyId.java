package py.com.logixone.kernel.api.company;

import java.util.Objects;
import java.util.UUID;

/** Opaque and immutable company identity shared through neutral contracts. */
public record CompanyId(UUID value) implements Comparable<CompanyId> {

    public CompanyId {
        Objects.requireNonNull(value, "value");
    }

    public static CompanyId parse(String value) {
        Objects.requireNonNull(value, "value");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Company id must be a canonical UUID", failure);
        }
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException("Company id must be a canonical lower-case UUID");
        }
        return new CompanyId(parsed);
    }

    @Override
    public int compareTo(CompanyId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
