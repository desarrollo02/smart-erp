package py.com.logixone.plugin.api;

import java.util.List;
import java.util.Objects;

/** Versioned public screen contract published by its owning functional plugin. */
public record ScreenDefinition(
        ScreenId id,
        SemanticVersion contractVersion,
        List<ScreenElementDefinition> elements,
        List<ScreenSlotDefinition> slots) {

    public ScreenDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(contractVersion, "contractVersion");
        elements = List.copyOf(elements);
        slots = List.copyOf(slots);
    }
}
