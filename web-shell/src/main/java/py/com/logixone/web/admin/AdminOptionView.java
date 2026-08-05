package py.com.logixone.web.admin;

import java.util.Objects;

public final class AdminOptionView {

    private final String value;
    private final String label;

    AdminOptionView(String value, String label) {
        this.value = Objects.requireNonNull(value, "value");
        this.label = Objects.requireNonNull(label, "label");
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
