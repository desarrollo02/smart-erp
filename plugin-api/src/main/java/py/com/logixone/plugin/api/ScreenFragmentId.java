package py.com.logixone.plugin.api;

import java.util.Objects;

/** Public identity of content owned by a plugin and eligible for an explicitly declared slot. */
public record ScreenFragmentId(PluginId ownerPluginId, String localName)
        implements Comparable<ScreenFragmentId> {

    public ScreenFragmentId {
        Objects.requireNonNull(ownerPluginId, "ownerPluginId");
        localName = new ScreenElementId(localName).value();
    }

    @Override
    public int compareTo(ScreenFragmentId other) {
        Objects.requireNonNull(other, "other");
        int ownerComparison = ownerPluginId.compareTo(other.ownerPluginId);
        return ownerComparison != 0 ? ownerComparison : localName.compareTo(other.localName);
    }

    @Override
    public String toString() {
        return ownerPluginId + ":" + localName;
    }
}
