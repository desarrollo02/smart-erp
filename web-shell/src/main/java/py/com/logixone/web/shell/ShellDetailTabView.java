package py.com.logixone.web.shell;

import java.util.Objects;

/** One shell-owned semantic tab for an entity detail floorplan. */
public final class ShellDetailTabView {

    private final String id;
    private final String label;

    ShellDetailTabView(String id, String label) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
