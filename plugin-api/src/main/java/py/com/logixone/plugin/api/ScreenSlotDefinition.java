package py.com.logixone.plugin.api;

import java.util.Objects;

/** Explicit extension point owned by a screen contract. */
public record ScreenSlotDefinition(
        ScreenSlotId id,
        ScreenRegionId regionId,
        int order,
        int maxContents) {

    public ScreenSlotDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(regionId, "regionId");
        if (order < 0) {
            throw new IllegalArgumentException("Screen slot order must be non-negative");
        }
        if (maxContents < 1) {
            throw new IllegalArgumentException("Screen slot maxContents must be positive");
        }
    }
}
