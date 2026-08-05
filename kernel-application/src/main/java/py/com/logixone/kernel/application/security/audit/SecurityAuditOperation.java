package py.com.logixone.kernel.application.security.audit;

public enum SecurityAuditOperation {
    REGISTER_USER,
    CHANGE_USER_STATUS,
    REGISTER_MEMBERSHIP,
    CHANGE_MEMBERSHIP_STATUS,
    REGISTER_ROLE,
    CHANGE_ROLE_STATUS,
    ASSIGN_ROLE,
    UNASSIGN_ROLE,
    GRANT_PERMISSION,
    REVOKE_PERMISSION,
    BOOTSTRAP_SECURITY
}
