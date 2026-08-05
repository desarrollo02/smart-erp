package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Shared invariants for plugin-owned and platform-owned selector metadata. */
final class SelectorSourcePolicy {

    private static final int MAX_ROUTE_LENGTH = 160;

    private SelectorSourcePolicy() {
    }

    static Optional<String> validatedRoute(Optional<String> route) {
        return Objects.requireNonNull(route, "managementRoute")
                .map(SelectorSourcePolicy::validRoute);
    }

    static void validate(
            SelectorSourceId id,
            SelectorSourceOwner owner,
            SelectorSourceKind kind,
            SemanticVersion sourceVersion,
            Optional<String> managementRoute,
            Optional<ContributionId> managementPermission,
            Set<SelectorManagementCapability> managementCapabilities,
            SelectorEmptyOptionPolicy emptyOptionPolicy,
            SelectorInactiveValuePolicy inactiveValuePolicy,
            SelectorLoadingStrategy loadingStrategy) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(sourceVersion, "sourceVersion");
        Objects.requireNonNull(managementRoute, "managementRoute");
        Objects.requireNonNull(managementPermission, "managementPermission");
        Objects.requireNonNull(managementCapabilities, "managementCapabilities");
        Objects.requireNonNull(emptyOptionPolicy, "emptyOptionPolicy");
        Objects.requireNonNull(inactiveValuePolicy, "inactiveValuePolicy");
        Objects.requireNonNull(loadingStrategy, "loadingStrategy");

        boolean hasManagement = !managementCapabilities.isEmpty();
        if (hasManagement != managementRoute.isPresent()
                || hasManagement != managementPermission.isPresent()) {
            throw new IllegalArgumentException(
                    "Management route, permission and capabilities must appear together");
        }
        if (hasManagement && !managementCapabilities.contains(SelectorManagementCapability.VIEW)) {
            throw new IllegalArgumentException("Managed selector sources must allow VIEW");
        }

        boolean immutableSource = kind == SelectorSourceKind.CLOSED_STATE
                || kind == SelectorSourceKind.DEPLOYMENT_COMPOSITION;
        if (immutableSource && hasManagement) {
            throw new IllegalArgumentException(
                    "Closed and deployment selector sources cannot expose runtime management");
        }
        if (immutableSource && inactiveValuePolicy != SelectorInactiveValuePolicy.NOT_APPLICABLE) {
            throw new IllegalArgumentException(
                    "Closed and deployment selector sources do not have inactive values");
        }
        if (immutableSource && loadingStrategy != SelectorLoadingStrategy.INLINE) {
            throw new IllegalArgumentException(
                    "Closed and deployment selector sources must load inline");
        }

        boolean governedSource = kind == SelectorSourceKind.BUSINESS_CATALOG
                || kind == SelectorSourceKind.OPERATIONAL_REFERENCE
                || kind == SelectorSourceKind.NORMATIVE_CATALOG;
        if (governedSource && !hasManagement) {
            throw new IllegalArgumentException(
                    "Governed selector sources require an authorized administration route");
        }
        if (governedSource && inactiveValuePolicy == SelectorInactiveValuePolicy.NOT_APPLICABLE) {
            throw new IllegalArgumentException(
                    "Governed selector sources must define their inactive value behavior");
        }
    }

    private static String validRoute(String value) {
        Objects.requireNonNull(value, "management route");
        if (value.isBlank()
                || value.length() > MAX_ROUTE_LENGTH
                || !value.startsWith("/")
                || value.startsWith("//")
                || value.codePoints().anyMatch(Character::isISOControl)
                || value.codePoints().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Invalid selector management route");
        }
        return value;
    }
}
