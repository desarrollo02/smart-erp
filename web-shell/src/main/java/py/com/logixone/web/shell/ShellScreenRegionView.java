package py.com.logixone.web.shell;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import py.com.logixone.plugin.api.ScreenRegionRole;

/** JSF-safe region selected and labelled by a closed shell floorplan. */
public final class ShellScreenRegionView {

    private final String id;
    private final ScreenRegionRole role;
    private final String title;
    private final List<ShellScreenElementView> elements;

    ShellScreenRegionView(
            String id,
            ScreenRegionRole role,
            List<ShellScreenElementView> elements) {
        this.id = Objects.requireNonNull(id, "id");
        this.role = Objects.requireNonNull(role, "role");
        this.title = title(role);
        this.elements = List.copyOf(elements);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStyleClass() {
        return "floorplan-region floorplan-region-"
                + role.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public List<ShellScreenElementView> getFields() {
        return elements.stream()
                .filter(element -> !element.isAction() && !element.isDataTable())
                .toList();
    }

    public List<ShellScreenElementView> getTables() {
        return elements.stream().filter(ShellScreenElementView::isDataTable).toList();
    }

    public List<ShellScreenElementView> getActions() {
        return elements.stream().filter(ShellScreenElementView::isAction).toList();
    }

    private static String title(ScreenRegionRole role) {
        return switch (role) {
            case CONTEXT -> "Contexto";
            case FILTERS -> "Filtros";
            case WORK_ITEMS -> "Trabajo pendiente";
            case HEADER -> "Cabecera";
            case LINES -> "Líneas";
            case SUMMARY -> "Resumen";
            case GUIDANCE -> "Guía";
            case CONTENT -> "Datos de la operación";
            case ACTIONS -> "Acciones disponibles";
        };
    }
}
