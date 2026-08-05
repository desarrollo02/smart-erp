package py.com.logixone.web.admin;

import java.util.Objects;

public final class AdminPermissionView {

    private final String value;
    private final boolean effective;

    AdminPermissionView(String value, boolean effective) {
        this.value = Objects.requireNonNull(value, "value");
        this.effective = effective;
    }

    public String getValue() { return value; }
    public boolean isEffective() { return effective; }
}
