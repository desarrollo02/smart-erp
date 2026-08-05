package py.com.logixone.kernel.application.security.system.port;

import py.com.logixone.kernel.application.security.admin.SecurityAdministrationActionResult;
import py.com.logixone.kernel.application.security.admin.SystemAuthorityAdministrationSnapshot;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.system.command.AssignSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.command.ChangeSystemRoleStatusCommand;
import py.com.logixone.kernel.application.security.system.command.GrantSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.RegisterSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.command.RevokeSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.UnassignSystemRoleCommand;

public interface SystemAuthorityAdministrationPort {

    SystemAuthorityAdministrationSnapshot authoritySnapshot();

    SecurityAdministrationActionResult registerSystemRole(
            RegisterSystemRoleCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult changeSystemRoleStatus(
            ChangeSystemRoleStatusCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult assignSystemRole(
            AssignSystemRoleCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult unassignSystemRole(
            UnassignSystemRoleCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult grantSystemPermission(
            GrantSystemPermissionCommand command, SecurityAuditActor actor);

    SecurityAdministrationActionResult revokeSystemPermission(
            RevokeSystemPermissionCommand command, SecurityAuditActor actor);
}
