package py.com.logixone.kernel.application.company.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationActionResult;
import py.com.logixone.kernel.application.company.admin.CompanyAdministrationSnapshot;
import py.com.logixone.kernel.application.company.admin.CompanyPluginAdministrationView;
import py.com.logixone.kernel.application.company.audit.CompanyAuditContext;
import py.com.logixone.kernel.application.company.command.ChangeCompanyStatusCommand;
import py.com.logixone.kernel.application.company.command.ChangePluginActivationCommand;
import py.com.logixone.kernel.application.company.command.RegisterCompanyCommand;
import py.com.logixone.kernel.application.company.command.ReplaceCustomizationCommand;

/** Internal neutral contract consumed by the authenticated administrative web adapter. */
public interface CompanyAdministrationPort {

    CompanyAdministrationSnapshot snapshot();

    Optional<CompanyPluginAdministrationView> findCompany(CompanyId companyId);

    CompanyAdministrationActionResult register(
            RegisterCompanyCommand command,
            CompanyAuditContext auditContext);

    CompanyAdministrationActionResult changeStatus(
            ChangeCompanyStatusCommand command,
            CompanyAuditContext auditContext);

    CompanyAdministrationActionResult replaceCustomization(
            ReplaceCustomizationCommand command,
            CompanyAuditContext auditContext);

    CompanyAdministrationActionResult changeActivation(
            ChangePluginActivationCommand command,
            CompanyAuditContext auditContext);
}
