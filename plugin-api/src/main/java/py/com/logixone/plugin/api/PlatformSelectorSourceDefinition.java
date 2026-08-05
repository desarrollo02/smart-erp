package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Selector metadata owned by the kernel or shell rather than by an installable plugin. */
public record PlatformSelectorSourceDefinition(
        SelectorSourceId id,
        SelectorSourceOwner owner,
        SelectorSourceKind kind,
        SemanticVersion sourceVersion,
        Optional<String> managementRoute,
        Optional<ContributionId> managementPermission,
        Set<SelectorManagementCapability> managementCapabilities,
        SelectorEmptyOptionPolicy emptyOptionPolicy,
        SelectorInactiveValuePolicy inactiveValuePolicy,
        SelectorLoadingStrategy loadingStrategy) implements SelectorSourceMetadata {

    public PlatformSelectorSourceDefinition {
        if (Objects.requireNonNull(owner, "owner").kind() != SelectorSourceOwnerKind.PLATFORM) {
            throw new IllegalArgumentException("Platform selector source requires a platform owner");
        }
        managementRoute = SelectorSourcePolicy.validatedRoute(managementRoute);
        managementPermission = Objects.requireNonNull(
                managementPermission, "managementPermission");
        managementCapabilities = Set.copyOf(Objects.requireNonNull(
                managementCapabilities, "managementCapabilities"));
        SelectorSourcePolicy.validate(
                id,
                owner,
                kind,
                sourceVersion,
                managementRoute,
                managementPermission,
                managementCapabilities,
                emptyOptionPolicy,
                inactiveValuePolicy,
                loadingStrategy);
    }
}
