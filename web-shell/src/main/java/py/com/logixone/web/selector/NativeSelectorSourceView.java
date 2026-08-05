package py.com.logixone.web.selector;

import java.util.Objects;
import py.com.logixone.plugin.api.SelectorManagementCapability;
import py.com.logixone.plugin.api.SelectorSourceMetadata;

/** JSF-safe projection of platform selector metadata and its authorized route. */
public final class NativeSelectorSourceView {

    private final String usageId;
    private final String sourceId;
    private final String helpText;
    private final boolean managementAvailable;
    private final String managementRoute;
    private final String managementLabel;

    NativeSelectorSourceView(
            String usageId, SelectorSourceMetadata source, boolean authorized) {
        if (usageId == null || !usageId.matches("[a-z][a-z0-9_.]{2,127}")) {
            throw new IllegalArgumentException("Invalid native selector usage");
        }
        Objects.requireNonNull(source, "source");
        this.usageId = usageId;
        sourceId = source.id().value();
        helpText = switch (source.kind()) {
            case CLOSED_STATE -> "Origen: kernel · opciones cerradas por reglas versionadas.";
            case DEPLOYMENT_COMPOSITION ->
                    "Origen: distribución desplegada · cambia al reconstruir y redesplegar.";
            case BUSINESS_CATALOG -> "Origen: kernel · catálogo administrado y auditable.";
            case OPERATIONAL_REFERENCE ->
                    "Origen: kernel · referencia operativa filtrada por autorización.";
            case NORMATIVE_CATALOG ->
                    "Origen: versión normativa verificada · no admite códigos arbitrarios.";
        };
        managementAvailable = source.manageable() && authorized;
        managementRoute = managementAvailable
                ? source.managementRoute().orElseThrow()
                : "";
        managementLabel = source.managementCapabilities()
                .contains(SelectorManagementCapability.CREATE)
                ? "Agregar o administrar"
                : "Administrar";
    }

    public String getUsageId() {
        return usageId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getHelpText() {
        return helpText;
    }

    public boolean isManagementAvailable() {
        return managementAvailable;
    }

    public String getManagementRoute() {
        return managementRoute;
    }

    public String getManagementLabel() {
        return managementLabel;
    }
}
