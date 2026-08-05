package py.com.logixone.plugin.api;

import java.util.Objects;

/** Globally stable identity of a screen, including its owning functional plugin. */
public record ScreenId(PluginId ownerPluginId, String localName) implements Comparable<ScreenId> {

    public ScreenId {
        Objects.requireNonNull(ownerPluginId, "ownerPluginId");
        localName = new ScreenElementId(localName).value();
    }

    @Override
    public int compareTo(ScreenId other) {
        Objects.requireNonNull(other, "other");
        int ownerComparison = ownerPluginId.compareTo(other.ownerPluginId);
        return ownerComparison != 0 ? ownerComparison : localName.compareTo(other.localName);
    }

    @Override
    public String toString() {
        return ownerPluginId + ":" + localName;
    }
}
