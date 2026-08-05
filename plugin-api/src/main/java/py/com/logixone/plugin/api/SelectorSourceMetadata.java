package py.com.logixone.plugin.api;

import java.util.Optional;
import java.util.Set;

/** Common framework-neutral metadata published by plugin and platform selectors. */
public sealed interface SelectorSourceMetadata
        permits SelectorSourceDefinition, PlatformSelectorSourceDefinition {

    SelectorSourceId id();

    SelectorSourceOwner owner();

    SelectorSourceKind kind();

    SemanticVersion sourceVersion();

    Optional<String> managementRoute();

    Optional<ContributionId> managementPermission();

    Set<SelectorManagementCapability> managementCapabilities();

    SelectorEmptyOptionPolicy emptyOptionPolicy();

    SelectorInactiveValuePolicy inactiveValuePolicy();

    SelectorLoadingStrategy loadingStrategy();

    default boolean manageable() {
        return !managementCapabilities().isEmpty();
    }
}
