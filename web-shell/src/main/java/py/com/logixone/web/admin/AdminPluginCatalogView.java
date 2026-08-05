package py.com.logixone.web.admin;

import java.util.Objects;
import py.com.logixone.kernel.application.company.admin.PluginCatalogView;
import py.com.logixone.plugin.api.PluginKind;

/** JSF-friendly read-only physical plugin row. */
public final class AdminPluginCatalogView {

    private final String pluginId;
    private final String displayName;
    private final String kind;
    private final String kindLabel;
    private final String version;
    private final String dependencies;

    private AdminPluginCatalogView(
            String pluginId,
            String displayName,
            String kind,
            String kindLabel,
            String version,
            String dependencies) {
        this.pluginId = pluginId;
        this.displayName = displayName;
        this.kind = kind;
        this.kindLabel = kindLabel;
        this.version = version;
        this.dependencies = dependencies;
    }

    static AdminPluginCatalogView from(PluginCatalogView plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return new AdminPluginCatalogView(
                plugin.pluginId().value(),
                plugin.displayName(),
                plugin.kind().name(),
                plugin.kind() == PluginKind.CUSTOMIZATION ? "Personalización" : "Funcional",
                plugin.version(),
                plugin.dependencies().isEmpty()
                        ? "Sin dependencias declaradas"
                        : String.join(", ", plugin.dependencies()));
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getKind() {
        return kind;
    }

    public String getKindLabel() {
        return kindLabel;
    }

    public String getKindCss() {
        return kind.toLowerCase(java.util.Locale.ROOT);
    }

    public String getVersion() {
        return version;
    }

    public String getDependencies() {
        return dependencies;
    }
}
