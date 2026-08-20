package py.com.logixone.plugin.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Framework-neutral v2 floorplan semantics. The shell remains the only owner of
 * markup, responsive behavior and visual components.
 */
public record ScreenExperienceDefinition(
        ScreenPurpose purpose,
        List<ScreenRegionDefinition> regions,
        Map<ScreenElementId, ScreenSemanticType> elementSemantics,
        List<ScreenActionDefinition> actions) {

    public ScreenExperienceDefinition {
        Objects.requireNonNull(purpose, "purpose");
        regions = List.copyOf(Objects.requireNonNull(regions, "regions"));
        elementSemantics = Map.copyOf(Objects.requireNonNull(
                elementSemantics, "elementSemantics"));
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("A screen experience requires regions");
        }
        requireUniqueRegionIds(regions);
        requireUniqueActionIds(actions);
    }

    private static void requireUniqueRegionIds(List<ScreenRegionDefinition> regions) {
        HashSet<ScreenRegionId> ids = new HashSet<>();
        for (ScreenRegionDefinition region : regions) {
            if (!ids.add(region.id())) {
                throw new IllegalArgumentException("Duplicate screen experience region");
            }
        }
    }

    private static void requireUniqueActionIds(List<ScreenActionDefinition> actions) {
        HashSet<ScreenElementId> ids = new HashSet<>();
        for (ScreenActionDefinition action : actions) {
            if (!ids.add(action.elementId())) {
                throw new IllegalArgumentException("Duplicate screen experience action");
            }
        }
    }
}
