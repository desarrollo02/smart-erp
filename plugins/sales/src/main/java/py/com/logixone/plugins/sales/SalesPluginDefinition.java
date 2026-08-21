package py.com.logixone.plugins.sales;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import py.com.logixone.plugin.api.*;
import py.com.logixone.plugins.sales.api.SalesContractVersion;
import py.com.logixone.plugins.sales.application.SalesPermissions;

@ApplicationScoped
public class SalesPluginDefinition implements PluginDefinition {
    public static final PluginId ID = new PluginId("sales");
    private static final PluginDescriptor DESCRIPTOR = new PluginDescriptor(
            ID, PluginKind.FUNCTIONAL, SemanticVersion.parse(SalesContractVersion.CURRENT),
            new VersionRange(SemanticVersion.parse("0.4.0"), SemanticVersion.parse("0.5.0")), "Sales",
            List.of(required("business_partners","1.1.0"), required("commercial_catalog","1.1.0"), required("reference_data","1.0.0"), required("inventory","1.3.0")),
            List.of(new ContributionId("sales.quotes"), new ContributionId("sales.orders"), new ContributionId("sales.commitments"), new ContributionId("sales.terms")),
            SalesPermissions.all(), List.of(), List.of(new MigrationContribution(
                    "plg_sales", "classpath:db/migration/sales")));
    private static PluginDependency required(String id,String minimum){return new PluginDependency(new PluginId(id),new VersionRange(SemanticVersion.parse(minimum),SemanticVersion.parse("2.0.0")),DependencyKind.REQUIRED);}
    @Override public PluginDescriptor descriptor(){return DESCRIPTOR;}
}
