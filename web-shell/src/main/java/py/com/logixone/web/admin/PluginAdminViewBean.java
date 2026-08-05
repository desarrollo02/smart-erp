package py.com.logixone.web.admin;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationActionResult;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationSnapshot;
import py.com.logixone.kernel.application.company.admin.CompanyPluginAdministrationView;
import py.com.logixone.kernel.application.company.admin.PluginCatalogView;
import py.com.logixone.kernel.application.company.audit.CompanyAuditContext;
import py.com.logixone.kernel.application.company.command.ChangePluginActivationCommand;
import py.com.logixone.kernel.application.company.command.ReplaceCustomizationCommand;
import py.com.logixone.kernel.application.company.port.CompanyAdministrationPort;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;
import py.com.logixone.kernel.domain.company.CompanyPluginDiagnosticCode;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.web.security.RequestCorrelation;
import py.com.logixone.web.security.TrustedAdminWebAccess;
import py.com.logixone.web.security.TrustedWebAccessException;
import py.com.logixone.web.selector.NativeSelectorSourceCatalog;
import py.com.logixone.web.shell.NativeSelectorReturnViewBean;

/** Thin request adapter for physical catalog and per-company plugin decisions. */
@Named("pluginAdminView")
@RequestScoped
public class PluginAdminViewBean {

    @Inject
    TrustedAdminWebAccess access;

    @Inject
    RequestCorrelation correlation;

    @Inject
    CompanyAdministrationPort administration;

    @Inject
    HttpServletRequest request;

    @Inject
    NativeSelectorReturnViewBean nativeSelectorReturn;

    private CompanyAdministrationSnapshot snapshot;
    private List<AdminCompanyView> companies = List.of();
    private List<AdminPluginCatalogView> physicalPlugins = List.of();
    private List<AdminPluginActivationView> functionalPlugins = List.of();
    private List<AdminCustomizationOptionView> availableCustomizations = List.of();
    private List<String> diagnostics = List.of();
    private AdminCompanyView selectedCompany;
    private String companyId;
    private String customizationPluginId;
    private boolean operational;
    private boolean canManageCompanies;

    @PostConstruct
    void initialize() {
        SystemAuthorityContext context = access.require(SystemPermission.PLUGIN_MANAGE);
        canManageCompanies = context.hasPermission(SystemPermission.COMPANY_MANAGE);
        snapshot = administration.snapshot();
        Map<String, String> pluginNames = pluginNames();
        companies = snapshot.companies().stream()
                .map(company -> AdminCompanyView.from(company, pluginNames))
                .toList();
        physicalPlugins = snapshot.physicalPlugins().stream()
                .map(AdminPluginCatalogView::from)
                .toList();
        String requestedCompany = nativeSelectorReturn
                .restore("/admin/plugins.xhtml")
                .filter(restoration -> restoration.usageId().equals(
                        NativeSelectorSourceCatalog.PLUGINS_COMPANY))
                .map(restoration -> restoration.inputs().get("company_id"))
                .orElse(request.getParameter("company"));
        if (requestedCompany != null && !requestedCompany.isBlank()) {
            companyId = requestedCompany;
            loadCompany();
        }
    }

    /** Invoked after the optional company view parameter has been applied. */
    public void loadCompany() {
        if (companyId == null || companyId.isBlank()) {
            return;
        }
        try {
            CompanyPluginAdministrationView detail = administration
                    .findCompany(AdminTechnicalInput.companyId(companyId))
                    .orElse(null);
            if (detail == null) {
                AdminCompanyOperationMessages.targetUnavailable();
                return;
            }
            selectedCompany = AdminCompanyView.from(detail.company(), pluginNames());
            operational = detail.operational();
            functionalPlugins = detail.functionalPlugins().stream()
                    .map(AdminPluginActivationView::from)
                    .toList();
            diagnostics = detail.diagnostics().stream()
                    .map(PluginAdminViewBean::diagnosticLabel)
                    .distinct()
                    .toList();
            availableCustomizations = customizationOptions(detail);
        } catch (IllegalArgumentException invalid) {
            AdminCompanyOperationMessages.targetUnavailable();
        }
    }

    public String openCompany() {
        try {
            return pluginRedirect(AdminTechnicalInput.companyId(companyId));
        } catch (IllegalArgumentException invalid) {
            AdminCompanyOperationMessages.invalidInput();
            return null;
        }
    }

    public String enable(String pluginId, long expectedVersion) {
        return changeActivation(pluginId, expectedVersion, PluginActivationState.ENABLED);
    }

    public String disable(String pluginId, long expectedVersion) {
        return changeActivation(pluginId, expectedVersion, PluginActivationState.DISABLED);
    }

    public String enableSelected() {
        return changeSelectedActivation(PluginActivationState.ENABLED);
    }

    public String disableSelected() {
        return changeSelectedActivation(PluginActivationState.DISABLED);
    }

    private String changeSelectedActivation(PluginActivationState desiredState) {
        try {
            return changeActivation(
                    request.getParameter("plugin"),
                    AdminTechnicalInput.version(request.getParameter("decisionVersion")),
                    desiredState);
        } catch (IllegalArgumentException invalid) {
            AdminCompanyOperationMessages.invalidInput();
            return null;
        }
    }

    private String changeActivation(
            String pluginId,
            long expectedVersion,
            PluginActivationState desiredState) {
        try {
            SystemAuthorityContext actor = access.require(SystemPermission.PLUGIN_MANAGE);
            CompanyId targetCompany = AdminTechnicalInput.companyId(companyId);
            CompanyAdministrationActionResult result = administration.changeActivation(
                    new ChangePluginActivationCommand(
                            targetCompany,
                            AdminTechnicalInput.pluginId(pluginId),
                            desiredState,
                            expectedVersion),
                    audit(actor));
            return AdminCompanyOperationMessages.finish(
                    result,
                    desiredState == PluginActivationState.ENABLED
                            ? "El plugin quedó habilitado para la empresa."
                            : "El plugin quedó deshabilitado; sus datos fueron conservados.",
                    "El plugin ya tenía el estado solicitado.",
                    pluginRedirect(targetCompany));
        } catch (TrustedWebAccessException denied) {
            AdminCompanyOperationMessages.denied();
            return null;
        } catch (IllegalArgumentException invalid) {
            AdminCompanyOperationMessages.invalidInput();
            return null;
        }
    }

    public String replaceCustomization(long expectedVersion) {
        try {
            SystemAuthorityContext actor = access.require(SystemPermission.COMPANY_MANAGE);
            CompanyId targetCompany = AdminTechnicalInput.companyId(companyId);
            CompanyAdministrationActionResult result = administration.replaceCustomization(
                    new ReplaceCustomizationCommand(
                            targetCompany,
                            AdminTechnicalInput.pluginId(customizationPluginId),
                            expectedVersion),
                    audit(actor));
            return AdminCompanyOperationMessages.finish(
                    result,
                    "La personalización obligatoria fue reemplazada.",
                    "La empresa ya utilizaba esa personalización.",
                    pluginRedirect(targetCompany));
        } catch (TrustedWebAccessException denied) {
            AdminCompanyOperationMessages.denied();
            return null;
        } catch (IllegalArgumentException invalid) {
            AdminCompanyOperationMessages.invalidInput();
            return null;
        }
    }

    private List<AdminCustomizationOptionView> customizationOptions(
            CompanyPluginAdministrationView detail) {
        Set<String> assignedElsewhere = snapshot.companies().stream()
                .filter(company -> !company.companyId().equals(detail.company().companyId()))
                .map(company -> company.customizationPluginId().value())
                .collect(Collectors.toUnmodifiableSet());
        return snapshot.physicalPlugins().stream()
                .filter(plugin -> plugin.kind() == PluginKind.CUSTOMIZATION)
                .filter(plugin -> !plugin.pluginId().equals(detail.company().customizationPluginId()))
                .filter(plugin -> !assignedElsewhere.contains(plugin.pluginId().value()))
                .map(plugin -> new AdminCustomizationOptionView(
                        plugin.pluginId().value(), plugin.displayName(), plugin.version()))
                .toList();
    }

    private Map<String, String> pluginNames() {
        return snapshot.physicalPlugins().stream()
                .collect(Collectors.toUnmodifiableMap(
                        plugin -> plugin.pluginId().value(), PluginCatalogView::displayName));
    }

    private CompanyAuditContext audit(SystemAuthorityContext actor) {
        return CompanyAuditContext.authenticated(actor.actorUserId(), correlation.value());
    }

    static String pluginRedirect(CompanyId companyId) {
        return "plugins.xhtml?faces-redirect=true&company=" + companyId;
    }

    private static String diagnosticLabel(CompanyPluginDiagnosticCode code) {
        return switch (code) {
            case COMPANY_INACTIVE -> "La empresa está inactiva; no expone plugins efectivos.";
            case REQUIRED_DEPENDENCY_NOT_EFFECTIVE ->
                    "Existe un plugin habilitado cuya dependencia requerida no es efectiva.";
            case CUSTOMIZATION_INCOMPATIBLE ->
                    "La personalización requiere plugins funcionales que aún no están efectivos.";
            case CUSTOMIZATION_NOT_PRESENT, CUSTOMIZATION_WRONG_KIND,
                    CUSTOMIZATION_ALREADY_ASSIGNED, CUSTOMIZATION_CONTRACT_INVALID ->
                    "La personalización obligatoria no puede componerse con el catálogo actual.";
            case PLUGIN_NOT_PRESENT, PLUGIN_NOT_FUNCTIONAL ->
                    "Existe una decisión conservada que no coincide con un plugin funcional físico.";
            default -> "La composición actual requiere revisión antes de quedar operativa.";
        };
    }

    public List<AdminCompanyView> getCompanies() {
        return companies;
    }

    public List<AdminPluginCatalogView> getPhysicalPlugins() {
        return physicalPlugins;
    }

    public List<AdminPluginActivationView> getFunctionalPlugins() {
        return functionalPlugins;
    }

    public List<AdminCustomizationOptionView> getAvailableCustomizations() {
        return availableCustomizations;
    }

    public List<String> getDiagnostics() {
        return diagnostics;
    }

    public AdminCompanyView getSelectedCompany() {
        return selectedCompany;
    }

    public boolean isCompanySelected() {
        return selectedCompany != null;
    }

    public boolean isOperational() {
        return operational;
    }

    public boolean isCanManageCompanies() {
        return canManageCompanies;
    }

    public boolean isCustomizationReplacementAvailable() {
        return canManageCompanies && !availableCustomizations.isEmpty();
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCustomizationPluginId() {
        return customizationPluginId;
    }

    public void setCustomizationPluginId(String customizationPluginId) {
        this.customizationPluginId = customizationPluginId;
    }
}
