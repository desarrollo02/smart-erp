package py.com.logixone.plugins.referencedata;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;
import py.com.logixone.plugins.referencedata.api.ReferenceDataContractVersion;
import py.com.logixone.plugins.referencedata.application.ReferenceDataIdentity;
import py.com.logixone.plugins.referencedata.application.ReferenceDataPermissions;

/** Neutral discovery entry point for normative reference data. */
@ApplicationScoped
public class ReferenceDataPluginDefinition implements PluginDefinition {

    public static final PluginId ID = ReferenceDataIdentity.PLUGIN_ID;

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            ID,
            PluginKind.FUNCTIONAL,
            SemanticVersion.parse(ReferenceDataContractVersion.CURRENT),
            new VersionRange(SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0")),
            "Reference data",
            List.of(),
            List.of(
                    new ContributionId("reference_data.directory"),
                    new ContributionId("reference_data.provenance")),
            ReferenceDataPermissions.all(),
            List.of(new MenuContribution(
                    new ContributionId("reference_data.catalogs.menu"),
                    "reference_data.menu.catalogs",
                    ReferenceDataScreenContract.ROUTE,
                    Optional.of(ReferenceDataPermissions.VIEW))),
            List.of(new MigrationContribution(
                    "plg_reference_data",
                    "classpath:db/migration/reference_data")),
            List.of(ReferenceDataScreenContract.definition()),
            List.of());

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
