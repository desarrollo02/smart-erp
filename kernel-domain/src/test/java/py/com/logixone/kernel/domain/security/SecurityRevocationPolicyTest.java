package py.com.logixone.kernel.domain.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugin.api.ContributionId;

class SecurityRevocationPolicyTest {

    private static final AppUserId USER_ID =
            new AppUserId(UUID.fromString("10000000-0000-4000-8000-000000000001"));
    private static final CompanyId COMPANY_A =
            new CompanyId(UUID.fromString("20000000-0000-4000-8000-000000000001"));
    private static final CompanyId COMPANY_B =
            new CompanyId(UUID.fromString("20000000-0000-4000-8000-000000000002"));
    private static final RoleId ROLE_ID =
            new RoleId(UUID.fromString("30000000-0000-4000-8000-000000000001"));
    private static final ContributionId VIEW_PERMISSION =
            new ContributionId("reference.dashboard.view");

    private final AppUser activeUser = new AppUser(
            USER_ID,
            new ExternalIdentity("https://identity.example.test/realms/logixone", "subject-1"),
            Optional.of("Test User"),
            UserStatus.ACTIVE,
            0);

    @Test
    void revokingMembershipImmediatelyRemovesCompanySelection() {
        CompanyAccessPolicy policy = new CompanyAccessPolicy();
        CompanyMembership active = membership(COMPANY_A, MembershipStatus.ACTIVE);

        assertEquals(
                COMPANY_A,
                policy.resolve(activeUser, List.of(active), Optional.empty())
                        .selectedCompanyId()
                        .orElseThrow());

        CompanySelectionResolution revoked = policy.resolve(
                activeUser,
                List.of(membership(COMPANY_A, MembershipStatus.INACTIVE)),
                Optional.of(COMPANY_A));

        assertFalse(revoked.selectedCompanyId().isPresent());
        assertEquals(SecurityDiagnosticCode.COMPANY_ACCESS_DENIED, revoked.failure().orElseThrow());
    }

    @Test
    void revokingMembershipImmediatelyRemovesEffectivePermissions() {
        EffectivePermissionResolution revoked = permissionPolicy().resolve(
                activeUser,
                membership(COMPANY_A, MembershipStatus.INACTIVE),
                List.of(role(COMPANY_A, RoleStatus.ACTIVE)),
                List.of(new MembershipRoleAssignment(USER_ID, COMPANY_A, ROLE_ID)),
                List.of(new RolePermissionGrant(COMPANY_A, ROLE_ID, VIEW_PERMISSION)),
                Set.of(VIEW_PERMISSION));

        assertFalse(revoked.authorized());
        assertTrue(revoked.permissions().isEmpty());
        assertEquals(SecurityDiagnosticCode.MEMBERSHIP_INACTIVE, revoked.failure().orElseThrow());
    }

    @Test
    void revokingRoleImmediatelyRemovesItsGrantedPermission() {
        EffectivePermissionResolution granted = resolvePermissions(RoleStatus.ACTIVE, Set.of(VIEW_PERMISSION));
        EffectivePermissionResolution revoked = resolvePermissions(RoleStatus.INACTIVE, Set.of(VIEW_PERMISSION));

        assertTrue(granted.permissions().contains(VIEW_PERMISSION));
        assertTrue(revoked.authorized());
        assertTrue(revoked.permissions().isEmpty());
    }

    @Test
    void disablingPluginRemovesPermissionsNoLongerPresentInItsContributions() {
        EffectivePermissionResolution enabled = resolvePermissions(RoleStatus.ACTIVE, Set.of(VIEW_PERMISSION));
        EffectivePermissionResolution disabled = resolvePermissions(RoleStatus.ACTIVE, Set.of());

        assertTrue(enabled.permissions().contains(VIEW_PERMISSION));
        assertTrue(disabled.authorized());
        assertTrue(disabled.permissions().isEmpty());
    }

    @Test
    void roleFromAnotherCompanyFailsClosed() {
        EffectivePermissionResolution invalid = permissionPolicy().resolve(
                activeUser,
                membership(COMPANY_A, MembershipStatus.ACTIVE),
                List.of(role(COMPANY_B, RoleStatus.ACTIVE)),
                List.of(new MembershipRoleAssignment(USER_ID, COMPANY_A, ROLE_ID)),
                List.of(new RolePermissionGrant(COMPANY_A, ROLE_ID, VIEW_PERMISSION)),
                Set.of(VIEW_PERMISSION));

        assertFalse(invalid.authorized());
        assertTrue(invalid.permissions().isEmpty());
        assertEquals(SecurityDiagnosticCode.ROLE_CONTEXT_INVALID, invalid.failure().orElseThrow());
    }

    private EffectivePermissionResolution resolvePermissions(
            RoleStatus roleStatus,
            Set<ContributionId> availablePermissions) {
        return permissionPolicy().resolve(
                activeUser,
                membership(COMPANY_A, MembershipStatus.ACTIVE),
                List.of(role(COMPANY_A, roleStatus)),
                List.of(new MembershipRoleAssignment(USER_ID, COMPANY_A, ROLE_ID)),
                List.of(new RolePermissionGrant(COMPANY_A, ROLE_ID, VIEW_PERMISSION)),
                availablePermissions);
    }

    private CompanyMembership membership(CompanyId companyId, MembershipStatus status) {
        return new CompanyMembership(USER_ID, companyId, status, 0);
    }

    private CompanyRole role(CompanyId companyId, RoleStatus status) {
        return new CompanyRole(
                ROLE_ID,
                companyId,
                new RoleCode("operator"),
                "Operator",
                status,
                0);
    }

    private EffectivePermissionPolicy permissionPolicy() {
        return new EffectivePermissionPolicy();
    }
}
