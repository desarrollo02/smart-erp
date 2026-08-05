package py.com.logixone.kernel.application.company.screen;

import java.util.Objects;
import py.com.logixone.plugin.api.ScreenFragmentId;
import py.com.logixone.plugin.api.ScreenSlotId;

/** Public fragment selected for an explicit screen slot. */
public record ComposedSlotContent(
        ScreenSlotId slotId,
        ScreenFragmentId fragmentId,
        int position) {

    public ComposedSlotContent {
        Objects.requireNonNull(slotId, "slotId");
        Objects.requireNonNull(fragmentId, "fragmentId");
        if (position < 0) {
            throw new IllegalArgumentException("position must be non-negative");
        }
    }
}
