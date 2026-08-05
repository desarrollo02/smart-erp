package py.com.logixone.plugins.referencedata.application;

import py.com.logixone.plugin.api.PluginId;

/** Stable plugin identity shared by descriptor and application adapters. */
public final class ReferenceDataIdentity {

    public static final PluginId PLUGIN_ID = new PluginId("reference_data");

    private ReferenceDataIdentity() {
    }
}
