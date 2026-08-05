package py.com.logixone.web.selector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PlatformSelectorSourceDefinition;
import py.com.logixone.plugin.api.SelectorEmptyOptionPolicy;
import py.com.logixone.plugin.api.SelectorInactiveValuePolicy;
import py.com.logixone.plugin.api.SelectorLoadingStrategy;
import py.com.logixone.plugin.api.SelectorManagementCapability;
import py.com.logixone.plugin.api.SelectorSourceId;
import py.com.logixone.plugin.api.SelectorSourceKind;
import py.com.logixone.plugin.api.SelectorSourceOwner;
import py.com.logixone.plugin.api.SemanticVersion;

/** Canonical metadata for every selector rendered directly by the kernel shell. */
public final class NativeSelectorSourceCatalog {

    public static final String APP_COMPANY_SWITCHER = "app.company_switcher";
    public static final String APP_COMPANY_SELECTION = "app.company_selection";
    public static final String COMPANIES_CUSTOMIZATION = "admin.companies.customization";
    public static final String PLUGINS_COMPANY = "admin.plugins.company";
    public static final String PLUGINS_CUSTOMIZATION = "admin.plugins.customization";
    public static final String SECURITY_COMPANY = "admin.security.company";
    public static final String SECURITY_MEMBERSHIP_USER = "admin.security.membership_user";
    public static final String SECURITY_ASSIGNMENT_USER = "admin.security.assignment_user";
    public static final String SECURITY_ASSIGNMENT_ROLE = "admin.security.assignment_role";
    public static final String SECURITY_GRANT_ROLE = "admin.security.grant_role";
    public static final String SECURITY_GRANT_PERMISSION = "admin.security.grant_permission";
    public static final String SYSTEM_ASSIGNMENT_USER = "admin.system_authority.assignment_user";
    public static final String SYSTEM_ASSIGNMENT_ROLE = "admin.system_authority.assignment_role";
    public static final String SYSTEM_GRANT_ROLE = "admin.system_authority.grant_role";
    public static final String SYSTEM_GRANT_PERMISSION = "admin.system_authority.grant_permission";
    public static final String AUDIT_CATEGORY = "admin.audit.category";
    public static final String AUDIT_OUTCOME = "admin.audit.outcome";
    public static final String AUDIT_WINDOW = "admin.audit.window";

    private static final SelectorSourceOwner KERNEL = SelectorSourceOwner.platform("kernel");
    private static final SemanticVersion VERSION = SemanticVersion.parse("1.0.0");
    private static final String COMPANIES_ROUTE = "/admin/companies.xhtml";
    private static final String SECURITY_ROUTE = "/admin/security.xhtml";
    private static final String SYSTEM_AUTHORITY_ROUTE = "/admin/system-authority.xhtml";
    private static final ContributionId COMPANY_MANAGE = permission("kernel.company.manage");
    private static final ContributionId SECURITY_MANAGE = permission("kernel.security.manage");
    private static final ContributionId SYSTEM_AUTHORITY_MANAGE =
            permission("kernel.system_administration.manage");

    private static final Map<String, PlatformSelectorSourceDefinition> SOURCES = sources();

    private NativeSelectorSourceCatalog() {
    }

    public static Map<String, PlatformSelectorSourceDefinition> all() {
        return SOURCES;
    }

    public static PlatformSelectorSourceDefinition source(String usageId) {
        PlatformSelectorSourceDefinition source = SOURCES.get(usageId);
        if (source == null) {
            throw new IllegalArgumentException("Unknown native selector usage: " + usageId);
        }
        return source;
    }

    private static Map<String, PlatformSelectorSourceDefinition> sources() {
        Map<String, PlatformSelectorSourceDefinition> sources = new LinkedHashMap<>();

        PlatformSelectorSourceDefinition authorizedCompanies = managed(
                "kernel.authorized_companies",
                SelectorSourceKind.OPERATIONAL_REFERENCE,
                COMPANIES_ROUTE,
                COMPANY_MANAGE,
                Set.of(
                        SelectorManagementCapability.VIEW,
                        SelectorManagementCapability.CREATE,
                        SelectorManagementCapability.INACTIVATE));
        sources.put(APP_COMPANY_SWITCHER, authorizedCompanies);
        sources.put(APP_COMPANY_SELECTION, authorizedCompanies);

        PlatformSelectorSourceDefinition companies = managed(
                "kernel.companies",
                SelectorSourceKind.OPERATIONAL_REFERENCE,
                COMPANIES_ROUTE,
                COMPANY_MANAGE,
                Set.of(
                        SelectorManagementCapability.VIEW,
                        SelectorManagementCapability.CREATE,
                        SelectorManagementCapability.INACTIVATE));
        sources.put(PLUGINS_COMPANY, companies);
        sources.put(SECURITY_COMPANY, companies);

        PlatformSelectorSourceDefinition customizations = closed(
                "kernel.physical_customizations",
                SelectorSourceKind.DEPLOYMENT_COMPOSITION,
                SelectorEmptyOptionPolicy.NOT_ALLOWED);
        sources.put(COMPANIES_CUSTOMIZATION, customizations);
        sources.put(PLUGINS_CUSTOMIZATION, customizations);

        PlatformSelectorSourceDefinition users = managed(
                "kernel.users",
                SelectorSourceKind.OPERATIONAL_REFERENCE,
                SECURITY_ROUTE,
                SECURITY_MANAGE,
                Set.of(
                        SelectorManagementCapability.VIEW,
                        SelectorManagementCapability.CREATE,
                        SelectorManagementCapability.INACTIVATE));
        sources.put(SECURITY_MEMBERSHIP_USER, users);
        sources.put(SECURITY_ASSIGNMENT_USER, users);
        sources.put(SYSTEM_ASSIGNMENT_USER, users);

        PlatformSelectorSourceDefinition companyRoles = managed(
                "kernel.company_roles",
                SelectorSourceKind.BUSINESS_CATALOG,
                SECURITY_ROUTE,
                SECURITY_MANAGE,
                Set.of(
                        SelectorManagementCapability.VIEW,
                        SelectorManagementCapability.CREATE,
                        SelectorManagementCapability.INACTIVATE));
        sources.put(SECURITY_ASSIGNMENT_ROLE, companyRoles);
        sources.put(SECURITY_GRANT_ROLE, companyRoles);

        PlatformSelectorSourceDefinition companyPermissions = closed(
                "kernel.plugin_permissions",
                SelectorSourceKind.DEPLOYMENT_COMPOSITION,
                SelectorEmptyOptionPolicy.NOT_ALLOWED);
        sources.put(SECURITY_GRANT_PERMISSION, companyPermissions);

        PlatformSelectorSourceDefinition systemRoles = managed(
                "kernel.system_roles",
                SelectorSourceKind.BUSINESS_CATALOG,
                SYSTEM_AUTHORITY_ROUTE,
                SYSTEM_AUTHORITY_MANAGE,
                Set.of(
                        SelectorManagementCapability.VIEW,
                        SelectorManagementCapability.CREATE,
                        SelectorManagementCapability.INACTIVATE));
        sources.put(SYSTEM_ASSIGNMENT_ROLE, systemRoles);
        sources.put(SYSTEM_GRANT_ROLE, systemRoles);

        sources.put(SYSTEM_GRANT_PERMISSION, closed(
                "kernel.system_permissions",
                SelectorSourceKind.CLOSED_STATE,
                SelectorEmptyOptionPolicy.NOT_ALLOWED));
        sources.put(AUDIT_CATEGORY, closed(
                "kernel.audit_categories",
                SelectorSourceKind.CLOSED_STATE,
                SelectorEmptyOptionPolicy.MEANS_ALL));
        sources.put(AUDIT_OUTCOME, closed(
                "kernel.audit_outcomes",
                SelectorSourceKind.CLOSED_STATE,
                SelectorEmptyOptionPolicy.MEANS_ALL));
        sources.put(AUDIT_WINDOW, closed(
                "kernel.audit_windows",
                SelectorSourceKind.CLOSED_STATE,
                SelectorEmptyOptionPolicy.NOT_ALLOWED));

        return Map.copyOf(sources);
    }

    private static PlatformSelectorSourceDefinition managed(
            String sourceId,
            SelectorSourceKind kind,
            String route,
            ContributionId permission,
            Set<SelectorManagementCapability> capabilities) {
        return new PlatformSelectorSourceDefinition(
                new SelectorSourceId(sourceId),
                KERNEL,
                kind,
                VERSION,
                Optional.of(route),
                Optional.of(permission),
                capabilities,
                SelectorEmptyOptionPolicy.NOT_ALLOWED,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE);
    }

    private static PlatformSelectorSourceDefinition closed(
            String sourceId,
            SelectorSourceKind kind,
            SelectorEmptyOptionPolicy emptyOptionPolicy) {
        return new PlatformSelectorSourceDefinition(
                new SelectorSourceId(sourceId),
                KERNEL,
                kind,
                VERSION,
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                emptyOptionPolicy,
                SelectorInactiveValuePolicy.NOT_APPLICABLE,
                SelectorLoadingStrategy.INLINE);
    }

    private static ContributionId permission(String value) {
        return new ContributionId(value);
    }
}
