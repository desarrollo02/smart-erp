package py.com.logixone.web.admin;

import java.util.Objects;

public final class AdminAuditDetailView {

    private final String label;
    private final String value;

    AdminAuditDetailView(String label, String value) {
        this.label = Objects.requireNonNull(label, "label");
        this.value = Objects.requireNonNull(value, "value");
    }

    public String getLabel() { return label; }
    public String getValue() { return value; }
}
