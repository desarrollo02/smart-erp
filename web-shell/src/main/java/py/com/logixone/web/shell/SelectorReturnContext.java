package py.com.logixone.web.shell;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/** One bounded server-side origin context; business inputs never enter the URL. */
record SelectorReturnContext(
        String userId,
        String companyId,
        long sessionRevision,
        String originRoute,
        String originTitle,
        String targetRoute,
        String mode,
        String tab,
        String resourceId,
        Long resourceVersion,
        Map<String, String> inputs) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    SelectorReturnContext {
        userId = requiredText(userId, "userId", 64);
        companyId = requiredText(companyId, "companyId", 64);
        if (sessionRevision < 0) {
            throw new IllegalArgumentException("sessionRevision must not be negative");
        }
        originRoute = route(originRoute, "originRoute");
        originTitle = requiredText(originTitle, "originTitle", 200);
        targetRoute = route(targetRoute, "targetRoute");
        mode = requiredText(mode, "mode", 16);
        if (!mode.matches("directory|create|detail")) {
            throw new IllegalArgumentException("Invalid selector return mode");
        }
        tab = requiredText(tab, "tab", 64);
        if (!tab.matches("[a-z][a-z0-9_]{0,63}")) {
            throw new IllegalArgumentException("Invalid selector return tab");
        }
        resourceId = resourceId == null ? null : requiredText(resourceId, "resourceId", 160);
        if ((resourceId == null) != (resourceVersion == null)
                || resourceVersion != null && resourceVersion < 0) {
            throw new IllegalArgumentException("Invalid selector return resource context");
        }
        inputs = Map.copyOf(Objects.requireNonNull(inputs, "inputs"));
    }

    boolean belongsTo(String expectedUserId, String expectedCompanyId, long expectedRevision) {
        return userId.equals(expectedUserId)
                && companyId.equals(expectedCompanyId)
                && sessionRevision == expectedRevision;
    }

    private static String route(String value, String name) {
        value = requiredText(value, name, 160);
        if (!value.startsWith("/") || value.startsWith("//")
                || value.codePoints().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return value;
    }

    private static String requiredText(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.length() > maxLength
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return value;
    }
}
