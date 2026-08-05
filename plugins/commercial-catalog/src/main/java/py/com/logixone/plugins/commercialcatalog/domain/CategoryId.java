package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) implements Comparable<CategoryId> {
    public CategoryId { Objects.requireNonNull(value, "value"); }
    @Override public int compareTo(CategoryId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
}
