package py.com.logixone.plugins.referencedata.api;

import java.util.List;
import java.util.Objects;

/** Immutable bounded result page; consumers never need to load a large source inline. */
public record ReferenceDataPage<T>(
        List<T> entries,
        long total,
        int offset,
        int limit) {

    public ReferenceDataPage {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (offset < 0
                || limit < 1
                || limit > 50
                || entries.size() > limit
                || total < entries.size()
                || offset > total) {
            throw new IllegalArgumentException("Invalid reference-data page");
        }
    }
}
