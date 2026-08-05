package py.com.logixone.web.admin;

import java.util.Objects;

/** Select option containing only a physically present and currently free customization. */
public final class AdminCustomizationOptionView {

    private final String pluginId;
    private final String label;

    AdminCustomizationOptionView(String pluginId, String displayName, String version) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
        this.label = Objects.requireNonNull(displayName, "displayName")
                + " · " + pluginId + " · v" + Objects.requireNonNull(version, "version");
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getLabel() {
        return label;
    }
}
