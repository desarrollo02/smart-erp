package py.com.logixone.kernel.application.security.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.security.admin.BusinessSecuritySnapshot;
import py.com.logixone.kernel.application.security.admin.CompanySecurityAdministrationView;
import py.com.logixone.kernel.application.security.admin.SecurityAdministrationActionResult;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.command.AssignRoleCommand;
import py.com.logixone.kernel.application.security.command.ChangeAppUserStatusCommand;
import py.com.logixone.kernel.application.security.command.ChangeMembershipStatusCommand;
import py.com.logixone.kernel.application.security.command.ChangeRoleStatusCommand;
import py.com.logixone.kernel.application.security.command.GrantPermissionCommand;
import py.com.logixone.kernel.application.security.command.RegisterAppUserCommand;
import py.com.logixone.kernel.application.security.command.RegisterMembershipCommand;
import py.com.logixone.kernel.application.security.command.RegisterRoleCommand;
import py.com.logixone.kernel.application.security.command.RevokePermissionCommand;
import py.com.logixone.kernel.application.security.command.UnassignRoleCommand;

public interface BusinessSecurityAdministrationPort {

    BusinessSecuritySnapshot administrationSnapshot();

    Optional<CompanySecurityAdministrationView> findCompanySecurity(CompanyId companyId);

    SecurityAdministrationActionResult registerUser(
            RegisterAppUserCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult changeUserStatus(
            ChangeAppUserStatusCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult registerMembership(
            RegisterMembershipCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult changeMembershipStatus(
            ChangeMembershipStatusCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult registerCompanyRole(
            RegisterRoleCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult changeCompanyRoleStatus(
            ChangeRoleStatusCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult assignCompanyRole(
            AssignRoleCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult unassignCompanyRole(
            UnassignRoleCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult grantCompanyPermission(
            GrantPermissionCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult revokeCompanyPermission(
            RevokePermissionCommand command, SecurityAuditActor actor);
}
