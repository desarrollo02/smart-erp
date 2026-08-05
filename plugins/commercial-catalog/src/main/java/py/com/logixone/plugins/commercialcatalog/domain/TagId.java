package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.Objects;
import java.util.UUID;

public record TagId(UUID value) implements Comparable<TagId> {
    public TagId { Objects.requireNonNull(value, "value"); }
    @Override public int compareTo(TagId other) { return value.compareTo(Objects.requireNonNull(other, "other").value); }
}
