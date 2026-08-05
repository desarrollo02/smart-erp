package py.com.logixone.kernel.domain.company;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.PluginId;

/** Persistable neutral company state; operational availability is derived separately. */
public record Company(
        CompanyId id,
        CompanyStatus status,
        PluginId customizationPluginId,
        long version) {

    public Company {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(customizationPluginId, "customizationPluginId");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public boolean isActive() {
        return status == CompanyStatus.ACTIVE;
    }
}
