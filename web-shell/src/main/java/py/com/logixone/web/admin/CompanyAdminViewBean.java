package py.com.logixone.web.admin;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationActionResult;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationSnapshot;
import py.com.logixone.kernel.application.company.admin.PluginCatalogView;
import py.com.logixone.kernel.application.company.audit.CompanyAuditContext;
import py.com.logixone.kernel.application.company.command.ChangeCompanyStatusCommand;
import py.com.logixone.kernel.application.company.command.RegisterCompanyCommand;
import py.com.logixone.kernel.application.company.port.CompanyAdministrationPort;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.web.security.RequestCorrelation;
import py.com.logixone.web.security.TrustedAdminWebAccess;
import py.com.logixone.web.security.TrustedWebAccessException;
import py.com.logixone.web.shell.NativeSelectorReturnViewBean;

/** Thin request adapter for global company administration. */
@Named("companyAdminView")
@RequestScoped
public class CompanyAdminViewBean {

    @Inject
    TrustedAdminWebAccess access;

    @Inject
    RequestCorrelation correlation;

    @Inject
    CompanyAdministrationPort administration;

    @Inject
    NativeSelectorReturnViewBean nativeSelectorReturn;

    private List<AdminCompanyView> companies = List.of();
    private List<AdminCustomizationOptionView> availableCustomizations = List.of();
    private String customizationPluginId;
    private boolean canManagePlugins;

    @PostConstruct
    void initialize() {
        SystemAuthorityContext context = access.require(SystemPermission.COMPANY_MANAGE);
        canManagePlugins = context.hasPermission(SystemPermission.PLUGIN_MANAGE);
        load();
    }

    public String register() {
        try {
            SystemAuthorityContext actor = access.require(SystemPermission.COMPANY_MANAGE);
            CompanyAdministrationActionResult result = administration.register(
                    new RegisterCompanyCommand(AdminTechnicalInput.pluginId(customizationPluginId)),
                    audit(actor));
            return AdminCompanyOperationMessages.finish(
                    result,
                    "La empresa fue registrada inactiva con su personalización obligatoria.",
                    "La empresa ya se encontraba registrada.",
                    nativeSelectorReturn.preserve(
                            "/admin/companies.xhtml?faces-redirect=true"));
        } catch (TrustedWebAccessException denied) {
            AdminCompanyOperationMessages.denied();
            return null;
        } catch (IllegalArgumentException invalid) {
            AdminCompanyOperationMessages.invalidInput();
            return null;
        }
    }

    public String activate(String companyId, long expectedVersion) {
        return changeStatus(companyId, expectedVersion, CompanyStatus.ACTIVE);
    }

    public String inactivate(String companyId, long expectedVersion) {
        return changeStatus(companyId, expectedVersion, CompanyStatus.INACTIVE);
    }

    private String changeStatus(
            String companyId,
            long expectedVersion,
            CompanyStatus desiredStatus) {
        try {
            SystemAuthorityContext actor = access.require(SystemPermission.COMPANY_MANAGE);
            CompanyAdministrationActionResult result = administration.changeStatus(
                    new ChangeCompanyStatusCommand(
                            AdminTechnicalInput.companyId(companyId), desiredStatus, expectedVersion),
                    audit(actor));
            return AdminCompanyOperationMessages.finish(
                    result,
                    desiredStatus == CompanyStatus.ACTIVE
                            ? "La empresa quedó activa."
                            : "La empresa quedó inactiva; sus plugins y datos se conservaron.",
                    "La empresa ya tenía el estado solicitado.",
                    nativeSelectorReturn.preserve(
                            "/admin/companies.xhtml?faces-redirect=true"));
        } catch (TrustedWebAccessException denied) {
            AdminCompanyOperationMessages.denied();
            return null;
        } catch (IllegalArgumentException invalid) {
            AdminCompanyOperationMessages.invalidInput();
            return null;
        }
    }

    private void load() {
        CompanyAdministrationSnapshot snapshot = administration.snapshot();
        Map<String, String> pluginNames = snapshot.physicalPlugins().stream()
                .collect(Collectors.toUnmodifiableMap(
                        plugin -> plugin.pluginId().value(), PluginCatalogView::displayName));
        companies = snapshot.companies().stream()
                .map(company -> AdminCompanyView.from(company, pluginNames))
                .toList();
        Set<String> assignedCustomizations = snapshot.companies().stream()
                .map(company -> company.customizationPluginId().value())
                .collect(Collectors.toUnmodifiableSet());
        availableCustomizations = snapshot.physicalPlugins().stream()
                .filter(plugin -> plugin.kind() == PluginKind.CUSTOMIZATION)
                .filter(plugin -> !assignedCustomizations.contains(plugin.pluginId().value()))
                .map(plugin -> new AdminCustomizationOptionView(
                        plugin.pluginId().value(), plugin.displayName(), plugin.version()))
                .toList();
    }

    private CompanyAuditContext audit(SystemAuthorityContext actor) {
        return CompanyAuditContext.authenticated(actor.actorUserId(), correlation.value());
    }

    public List<AdminCompanyView> getCompanies() {
        return companies;
    }

    public List<AdminCustomizationOptionView> getAvailableCustomizations() {
        return availableCustomizations;
    }

    public boolean isCustomizationAvailable() {
        return !availableCustomizations.isEmpty();
    }

    public boolean isCanManagePlugins() {
        return canManagePlugins;
    }

    public String getCustomizationPluginId() {
        return customizationPluginId;
    }

    public void setCustomizationPluginId(String customizationPluginId) {
        this.customizationPluginId = customizationPluginId;
    }
}
