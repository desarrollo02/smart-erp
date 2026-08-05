package py.com.logixone.plugin.api;

import java.util.List;
import java.util.Objects;

/** Immutable overlay published by a customization plugin against a versioned public screen. */
public record ScreenOverlay(
        ContributionId id,
        ScreenId targetScreen,
        VersionRange compatibleScreenVersions,
        List<ScreenChange> changes) {

    public ScreenOverlay {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(targetScreen, "targetScreen");
        Objects.requireNonNull(compatibleScreenVersions, "compatibleScreenVersions");
        changes = List.copyOf(changes);
        if (changes.isEmpty()) {
            throw new IllegalArgumentException("Screen overlay must declare at least one change");
        }
    }
}
