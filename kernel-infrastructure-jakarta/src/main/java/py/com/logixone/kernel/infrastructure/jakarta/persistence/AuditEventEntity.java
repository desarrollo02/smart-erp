package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.audit.admin.AuditEventCategory;
import py.com.logixone.kernel.application.audit.admin.AuditEventOutcome;
import py.com.logixone.kernel.application.audit.admin.AuditEventView;

/** Append-only JPA representation of the technical audit envelope. */
@Entity
@Table(name = "audit_event", schema = "core")
public class AuditEventEntity {

    @Id
    @Column(name = "audit_event_id", nullable = false, updatable = false)
    private UUID auditEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40, updatable = false)
    private AuditEventCategory category;

    @Column(name = "operation", nullable = false, length = 96, updatable = false)
    private String operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32, updatable = false)
    private AuditEventOutcome outcome;

    @Column(name = "actor_kind", nullable = false, length = 32, updatable = false)
    private String actorKind;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "subject_user_id", updatable = false)
    private UUID subjectUserId;

    @Column(name = "company_id", updatable = false)
    private UUID companyId;

    @Column(name = "role_id", updatable = false)
    private UUID roleId;

    @Column(name = "system_role_id", updatable = false)
    private UUID systemRoleId;

    @Column(name = "plugin_id", length = 128, updatable = false)
    private String pluginId;

    @Column(name = "permission_id", length = 160, updatable = false)
    private String permissionId;

    @Column(name = "screen_id", length = 260, updatable = false)
    private String screenId;

    @Column(name = "resource_type", length = 96, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", length = 160, updatable = false)
    private String resourceId;

    @Column(name = "result_code", length = 128, updatable = false)
    private String resultCode;

    @Column(name = "previous_version", updatable = false)
    private Long previousVersion;

    @Column(name = "resulting_version", updatable = false)
    private Long resultingVersion;

    @Column(name = "correlation_id", length = 128, updatable = false)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEventEntity() {
    }

    AuditEventEntity(
            AuditEventCategory category,
            String operation,
            AuditEventOutcome outcome,
            String actorKind,
            UUID actorUserId,
            UUID subjectUserId,
            UUID companyId,
            UUID roleId,
            UUID systemRoleId,
            String pluginId,
            String permissionId,
            String screenId,
            String resourceType,
            String resourceId,
            String resultCode,
            Long previousVersion,
            Long resultingVersion,
            String correlationId,
            Instant occurredAt) {
        this.auditEventId = UUID.randomUUID();
        this.category = category;
        this.operation = operation;
        this.outcome = outcome;
        this.actorKind = actorKind;
        this.actorUserId = actorUserId;
        this.subjectUserId = subjectUserId;
        this.companyId = companyId;
        this.roleId = roleId;
        this.systemRoleId = systemRoleId;
        this.pluginId = pluginId;
        this.permissionId = permissionId;
        this.screenId = screenId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resultCode = resultCode;
        this.previousVersion = previousVersion;
        this.resultingVersion = resultingVersion;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }

    AuditEventView toView() {
        return new AuditEventView(
                auditEventId,
                category,
                operation,
                outcome,
                actorKind,
                Optional.ofNullable(actorUserId).map(AppUserId::new),
                Optional.ofNullable(subjectUserId).map(AppUserId::new),
                Optional.ofNullable(companyId).map(CompanyId::new),
                Optional.ofNullable(roleId).map(UUID::toString),
                Optional.ofNullable(systemRoleId).map(UUID::toString),
                Optional.ofNullable(pluginId),
                Optional.ofNullable(permissionId),
                Optional.ofNullable(screenId),
                Optional.ofNullable(resourceType),
                Optional.ofNullable(resourceId),
                Optional.ofNullable(resultCode),
                Optional.ofNullable(previousVersion),
                Optional.ofNullable(resultingVersion),
                Optional.ofNullable(correlationId),
                occurredAt);
    }
}
