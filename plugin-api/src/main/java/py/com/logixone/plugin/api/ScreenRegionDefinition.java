package py.com.logixone.plugin.api;

import java.util.Objects;

/** One semantic region in a v2 screen experience. */
public record ScreenRegionDefinition(
        ScreenRegionId id,
        ScreenRegionRole role,
        int order) {

    public ScreenRegionDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
        if (order < 0) {
            throw new IllegalArgumentException("Screen region order must be non-negative");
        }
    }
}
