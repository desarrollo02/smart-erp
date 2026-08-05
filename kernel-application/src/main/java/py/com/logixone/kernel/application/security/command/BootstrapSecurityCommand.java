package py.com.logixone.kernel.application.security.command;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.RoleCode;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

/** Exact external declaration for the one-shot initial security authority. */
public record BootstrapSecurityCommand(
        ExternalIdentity externalIdentity,
        Optional<String> userDisplayName,
        CompanyId companyId,
        PluginId expectedCustomizationPluginId,
        RoleCode roleCode,
        String roleDisplayName,
        Set<ContributionId> permissionIds) {

    public BootstrapSecurityCommand {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        userDisplayName = Objects.requireNonNull(userDisplayName, "userDisplayName");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(expectedCustomizationPluginId, "expectedCustomizationPluginId");
        Objects.requireNonNull(roleCode, "roleCode");
        Objects.requireNonNull(roleDisplayName, "roleDisplayName");
        permissionIds = Collections.unmodifiableSet(
                new TreeSet<>(Objects.requireNonNull(permissionIds, "permissionIds")));
        if (permissionIds.isEmpty()) {
            throw new IllegalArgumentException("bootstrap requires at least one permission");
        }
    }
}
