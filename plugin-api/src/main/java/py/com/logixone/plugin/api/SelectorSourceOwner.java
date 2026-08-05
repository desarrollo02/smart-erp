package py.com.logixone.plugin.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable owner identity without pretending that a platform component is a plugin. */
public record SelectorSourceOwner(SelectorSourceOwnerKind kind, String id) {

    private static final int MAX_LENGTH = 59;
    private static final Pattern VALID_ID =
            Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

    public SelectorSourceOwner {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        if (id.length() > MAX_LENGTH || !VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "Selector source owner id must be lower-case snake_case and at most "
                            + MAX_LENGTH + " characters: " + id);
        }
    }

    public static SelectorSourceOwner platform(String id) {
        return new SelectorSourceOwner(SelectorSourceOwnerKind.PLATFORM, id);
    }

    public static SelectorSourceOwner plugin(PluginId pluginId) {
        return new SelectorSourceOwner(
                SelectorSourceOwnerKind.PLUGIN,
                Objects.requireNonNull(pluginId, "pluginId").value());
    }
}
