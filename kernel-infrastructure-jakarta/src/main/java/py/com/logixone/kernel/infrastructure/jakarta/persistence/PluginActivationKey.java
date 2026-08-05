package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.PluginId;

@Embeddable
public class PluginActivationKey implements Serializable {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "plugin_id", nullable = false, length = 59, updatable = false)
    private String pluginId;

    protected PluginActivationKey() {
    }

    PluginActivationKey(CompanyId companyId, PluginId pluginId) {
        this.companyId = Objects.requireNonNull(companyId, "companyId").value();
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId").value();
    }

    CompanyId companyId() {
        return new CompanyId(companyId);
    }

    PluginId pluginId() {
        return new PluginId(pluginId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PluginActivationKey that)) {
            return false;
        }
        return companyId.equals(that.companyId) && pluginId.equals(that.pluginId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, pluginId);
    }
}
