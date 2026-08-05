package py.com.logixone.kernel.api.security;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Short-lived server-side proof that actor, company, plugin activation and one
 * permission were revalidated for the current operation.
 */
public record AuthorizedCompanyOperation(
        AuthenticatedCompanyContext context,
        String pluginId,
        String permissionId,
        String correlationId) {

    private static final Pattern QUALIFIED =
            Pattern.compile("[a-z][a-z0-9_]*(?:[.:][a-z][a-z0-9_]*)*");
    private static final Pattern CORRELATION =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public AuthorizedCompanyOperation {
        Objects.requireNonNull(context, "context");
        pluginId = qualified(pluginId, "pluginId", 59);
        permissionId = qualified(permissionId, "permissionId", 128);
        Objects.requireNonNull(correlationId, "correlationId");
        if (!CORRELATION.matcher(correlationId).matches()) {
            throw new IllegalArgumentException("Invalid correlationId");
        }
    }

    private static String qualified(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        if (value.length() > maxLength || !QUALIFIED.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return value;
    }
}
