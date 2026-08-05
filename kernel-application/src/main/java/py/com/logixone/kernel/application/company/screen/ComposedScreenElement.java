package py.com.logixone.kernel.application.company.screen;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenTextKey;

/** Resolved presentation state of one element after the company overlay. */
public record ComposedScreenElement(
        ScreenElementId id,
        ScreenElementType type,
        ScreenRegionId regionId,
        int position,
        ScreenTextKey labelKey,
        Optional<ScreenTextKey> helpKey,
        boolean visible,
        boolean enabled,
        boolean required) {

    public ComposedScreenElement {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(regionId, "regionId");
        if (position < 0) {
            throw new IllegalArgumentException("position must be non-negative");
        }
        Objects.requireNonNull(labelKey, "labelKey");
        helpKey = Objects.requireNonNull(helpKey, "helpKey");
    }

    /** Compatibility constructor for callers compiled against the initial neutral contract. */
    public ComposedScreenElement(
            ScreenElementId id,
            ScreenRegionId regionId,
            int position,
            ScreenTextKey labelKey,
            Optional<ScreenTextKey> helpKey,
            boolean visible,
            boolean enabled,
            boolean required) {
        this(
                id,
                ScreenElementType.DISPLAY_TEXT,
                regionId,
                position,
                labelKey,
                helpKey,
                visible,
                enabled,
                required);
    }
}
