package py.com.logixone.plugins.businesspartners;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.MigrationContribution;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerContractVersion;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerPermissions;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnersIdentity;

/** Neutral entry point for Business partners. */
@ApplicationScoped
public class BusinessPartnersPluginDefinition implements PluginDefinition {

    public static final PluginId ID = BusinessPartnersIdentity.PLUGIN_ID;
    public static final PluginId REFERENCE_DATA_ID = new PluginId("reference_data");

    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            ID,
            PluginKind.FUNCTIONAL,
            SemanticVersion.parse(BusinessPartnerContractVersion.CURRENT),
            new VersionRange(
                    SemanticVersion.parse("0.4.0"),
                    SemanticVersion.parse("0.5.0")),
            "Business partners",
            List.of(new PluginDependency(
                    REFERENCE_DATA_ID,
                    new VersionRange(
                            SemanticVersion.parse("1.0.0"),
                            SemanticVersion.parse("2.0.0")),
                    DependencyKind.REQUIRED)),
            List.of(
                    new ContributionId("business_partners.directory"),
                    new ContributionId("business_partners.administration")),
            BusinessPartnerPermissions.all(),
            List.of(
                    new MenuContribution(
                            new ContributionId("business_partners.directory.menu"),
                            "business_partners.menu.directory",
                            BusinessPartnersScreenContract.ROUTE,
                            java.util.Optional.of(BusinessPartnerPermissions.VIEW)),
                    new MenuContribution(
                            new ContributionId("business_partners.definitions.menu"),
                            "business_partners.menu.definitions",
                            BusinessPartnersScreenContract.DEFINITIONS_ROUTE,
                            java.util.Optional.of(BusinessPartnerPermissions.MANAGE))),
            List.of(new MigrationContribution(
                    "plg_business_partners",
                    "classpath:db/migration/business_partners")),
            List.of(
                    BusinessPartnersScreenContract.definition(),
                    BusinessPartnersScreenContract.definitions()),
            List.of());

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
