package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Public, framework-neutral definition of one screen element and its allowed customizations. */
public record ScreenElementDefinition(
        ScreenElementId id,
        ScreenElementType type,
        ScreenRegionId regionId,
        int order,
        ScreenTextKey labelKey,
        Optional<ScreenTextKey> helpKey,
        boolean visible,
        boolean enabled,
        boolean required,
        Set<ScreenCustomizationOperation> allowedOperations) {

    public ScreenElementDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(regionId, "regionId");
        if (order < 0) {
            throw new IllegalArgumentException("Screen element order must be non-negative");
        }
        Objects.requireNonNull(labelKey, "labelKey");
        helpKey = Objects.requireNonNull(helpKey, "helpKey");
        allowedOperations = Set.copyOf(allowedOperations);
    }

    /**
     * Compatibility constructor for definitions created before element types became
     * explicit. They remain presentation text until migrated deliberately.
     */
    public ScreenElementDefinition(
            ScreenElementId id,
            ScreenRegionId regionId,
            int order,
            ScreenTextKey labelKey,
            Optional<ScreenTextKey> helpKey,
            boolean visible,
            boolean enabled,
            boolean required,
            Set<ScreenCustomizationOperation> allowedOperations) {
        this(
                id,
                ScreenElementType.DISPLAY_TEXT,
                regionId,
                order,
                labelKey,
                helpKey,
                visible,
                enabled,
                required,
                allowedOperations);
    }
}
