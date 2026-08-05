package py.com.logixone.web.shell;

import java.util.List;
import java.util.Objects;

/** Shell-owned layout section built from public screen regions. */
public final class ShellScreenSectionView {

    private final String id;
    private final String tabId;
    private final String title;
    private final String description;
    private final List<ShellScreenElementView> fields;
    private final List<ShellScreenElementView> actions;

    ShellScreenSectionView(
            String id,
            String tabId,
            String title,
            String description,
            List<ShellScreenElementView> fields,
            List<ShellScreenElementView> actions) {
        this.id = Objects.requireNonNull(id, "id");
        this.tabId = Objects.requireNonNull(tabId, "tabId");
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.fields = List.copyOf(fields);
        this.actions = List.copyOf(actions);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTabId() {
        return tabId;
    }

    public String getDescription() {
        return description;
    }

    public List<ShellScreenElementView> getFields() {
        return fields;
    }

    public List<ShellScreenElementView> getActions() {
        return actions;
    }
}
