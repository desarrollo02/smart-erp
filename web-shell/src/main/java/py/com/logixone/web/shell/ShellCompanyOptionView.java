package py.com.logixone.web.shell;

import java.util.Objects;

/** JSF-friendly immutable company option; the label is presentation only. */
public final class ShellCompanyOptionView {

    private final String id;
    private final String label;

    public ShellCompanyOptionView(String id, String label) {
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
