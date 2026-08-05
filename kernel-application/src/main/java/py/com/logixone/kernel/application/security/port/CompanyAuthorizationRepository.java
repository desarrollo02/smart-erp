package py.com.logixone.kernel.application.security.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.CompanyRole;
import py.com.logixone.kernel.domain.security.MembershipRoleAssignment;
import py.com.logixone.kernel.domain.security.RoleCode;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.kernel.domain.security.RolePermissionGrant;
import py.com.logixone.plugin.api.ContributionId;

public interface CompanyAuthorizationRepository {

    List<CompanyRole> findRolesByCompanyId(CompanyId companyId);

    Optional<CompanyRole> findRoleById(RoleId roleId);

    Optional<CompanyRole> findRoleByCompanyAndCode(CompanyId companyId, RoleCode roleCode);

    List<MembershipRoleAssignment> findAssignments(
            AppUserId userId,
            CompanyId companyId);

    List<MembershipRoleAssignment> findAssignmentsByCompanyId(CompanyId companyId);

    Optional<MembershipRoleAssignment> findAssignment(
            AppUserId userId,
            CompanyId companyId,
            RoleId roleId);

    List<RolePermissionGrant> findPermissionGrants(CompanyId companyId);

    Optional<RolePermissionGrant> findPermissionGrant(
            CompanyId companyId,
            RoleId roleId,
            ContributionId permissionId);

    CompanyRole saveRole(CompanyRole role);

    MembershipRoleAssignment saveAssignment(MembershipRoleAssignment assignment);

    RolePermissionGrant savePermissionGrant(RolePermissionGrant grant);

    boolean removeAssignment(MembershipRoleAssignment assignment);

    boolean removePermissionGrant(RolePermissionGrant grant);
}
