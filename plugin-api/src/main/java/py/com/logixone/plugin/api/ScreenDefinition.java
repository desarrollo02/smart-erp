package py.com.logixone.plugin.api;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Versioned public screen contract published by its owning functional plugin. */
public record ScreenDefinition(
        ScreenId id,
        SemanticVersion contractVersion,
        List<ScreenElementDefinition> elements,
        List<ScreenSlotDefinition> slots,
        Optional<ScreenExperienceDefinition> experience) {

    /** Compatibility constructor for v1 screen definitions. */
    public ScreenDefinition(
            ScreenId id,
            SemanticVersion contractVersion,
            List<ScreenElementDefinition> elements,
            List<ScreenSlotDefinition> slots) {
        this(id, contractVersion, elements, slots, Optional.empty());
    }

    public ScreenDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(contractVersion, "contractVersion");
        List<ScreenElementDefinition> copiedElements =
                List.copyOf(Objects.requireNonNull(elements, "elements"));
        elements = copiedElements;
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        experience = Objects.requireNonNull(experience, "experience");
        requireCompatibleExperienceVersion(contractVersion, experience);
        experience.ifPresent(value -> {
            requireUniqueElementIds(copiedElements);
            validateExperience(value, copiedElements);
        });
    }

    private static void requireUniqueElementIds(List<ScreenElementDefinition> elements) {
        HashSet<ScreenElementId> ids = new HashSet<>();
        for (ScreenElementDefinition element : elements) {
            if (!ids.add(element.id())) {
                throw new IllegalArgumentException("Duplicate screen element");
            }
        }
    }

    private static void requireCompatibleExperienceVersion(
            SemanticVersion version,
            Optional<ScreenExperienceDefinition> experience) {
        boolean v2OrNewer = version.major().compareTo(BigInteger.TWO) >= 0;
        if (v2OrNewer != experience.isPresent()) {
            throw new IllegalArgumentException(
                    v2OrNewer
                            ? "Screen contract v2 or newer requires an experience"
                            : "Screen contract v1 cannot declare a v2 experience");
        }
    }

    private static void validateExperience(
            ScreenExperienceDefinition experience,
            List<ScreenElementDefinition> elements) {
        Map<ScreenElementId, ScreenElementDefinition> byId = new HashMap<>();
        elements.forEach(element -> byId.put(element.id(), element));
        Set<ScreenRegionId> regions = experience.regions().stream()
                .map(ScreenRegionDefinition::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (elements.stream().anyMatch(element -> !regions.contains(element.regionId()))) {
            throw new IllegalArgumentException("Screen element references an undeclared experience region");
        }
        if (experience.elementSemantics().keySet().stream().anyMatch(id -> !byId.containsKey(id))) {
            throw new IllegalArgumentException("Screen semantics reference an unknown element");
        }
        Set<ScreenElementId> declaredActions = experience.actions().stream()
                .map(ScreenActionDefinition::elementId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<ScreenElementId> actualActions = elements.stream()
                .filter(element -> element.type() == ScreenElementType.ACTION)
                .map(ScreenElementDefinition::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!declaredActions.equals(actualActions)) {
            throw new IllegalArgumentException(
                    "Every v2 ACTION element requires exactly one action definition");
        }
    }
}
