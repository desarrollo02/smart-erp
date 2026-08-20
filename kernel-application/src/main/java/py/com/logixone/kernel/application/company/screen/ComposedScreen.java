package py.com.logixone.kernel.application.company.screen;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugin.api.ScreenExperienceDefinition;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.SemanticVersion;

/** Immutable screen contract after the unique company customization has been applied. */
public record ComposedScreen(
        ScreenId id,
        SemanticVersion contractVersion,
        List<ComposedScreenElement> elements,
        List<ScreenSlotDefinition> slots,
        List<ComposedSlotContent> slotContents,
        Optional<ScreenExperienceDefinition> experience) {

    /** Compatibility constructor for composed v1 screens. */
    public ComposedScreen(
            ScreenId id,
            SemanticVersion contractVersion,
            List<ComposedScreenElement> elements,
            List<ScreenSlotDefinition> slots,
            List<ComposedSlotContent> slotContents) {
        this(id, contractVersion, elements, slots, slotContents, Optional.empty());
    }

    public ComposedScreen {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(contractVersion, "contractVersion");
        elements = List.copyOf(elements);
        slots = List.copyOf(slots);
        slotContents = List.copyOf(slotContents);
        experience = Objects.requireNonNull(experience, "experience");
    }
}
