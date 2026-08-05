package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Framework-neutral governance metadata for a selector option source.
 *
 * <p>The source owns values and authorization; the shell remains the sole owner of
 * markup and visual behavior.</p>
 */
public record SelectorSourceDefinition(
        SelectorSourceId id,
        PluginId ownerPluginId,
        SelectorSourceKind kind,
        SemanticVersion sourceVersion,
        Optional<String> managementRoute,
        Optional<ContributionId> managementPermission,
        Set<SelectorManagementCapability> managementCapabilities,
        SelectorEmptyOptionPolicy emptyOptionPolicy,
        SelectorInactiveValuePolicy inactiveValuePolicy,
        SelectorLoadingStrategy loadingStrategy) implements SelectorSourceMetadata {

    public SelectorSourceDefinition {
        Objects.requireNonNull(ownerPluginId, "ownerPluginId");
        managementRoute = SelectorSourcePolicy.validatedRoute(managementRoute);
        managementPermission = Objects.requireNonNull(
                managementPermission, "managementPermission");
        managementCapabilities = Set.copyOf(Objects.requireNonNull(
                managementCapabilities, "managementCapabilities"));
        SelectorSourcePolicy.validate(
                id,
                SelectorSourceOwner.plugin(ownerPluginId),
                kind,
                sourceVersion,
                managementRoute,
                managementPermission,
                managementCapabilities,
                emptyOptionPolicy,
                inactiveValuePolicy,
                loadingStrategy);
    }

    @Override
    public SelectorSourceOwner owner() {
        return SelectorSourceOwner.plugin(ownerPluginId);
    }
}
