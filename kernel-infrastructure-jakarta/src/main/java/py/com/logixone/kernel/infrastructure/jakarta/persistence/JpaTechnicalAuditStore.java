package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.audit.admin.AuditEventCategory;
import py.com.logixone.kernel.application.audit.admin.AuditEventOutcome;
import py.com.logixone.kernel.application.company.audit.CompanyAuditEvent;
import py.com.logixone.kernel.application.security.audit.AccessAuditEvent;
import py.com.logixone.kernel.application.security.audit.SecurityAuditEvent;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAccessAuditEvent;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditEvent;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

/** Normalizes existing audit events into the append-only V5 envelope. */
@ApplicationScoped
public class JpaTechnicalAuditStore implements TechnicalAudit {

    private static final String AUTHENTICATED_USER = "AUTHENTICATED_USER";
    private static final String SYSTEM = "SYSTEM";
    private static final String UNRESOLVED = "UNRESOLVED";

    @PersistenceContext(unitName = CorePersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaTechnicalAuditStore() {
    }

    JpaTechnicalAuditStore(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    public void record(CompanyAuditEvent event) {
        persist(new AuditEventEntity(
                AuditEventCategory.COMPANY_OPERATION,
                event.operation().name(),
                AuditEventOutcome.valueOf(event.outcome().name()),
                event.actor().name(),
                appUserUuid(event.actorUserId()),
                null,
                event.companyId().value(),
                null,
                null,
                text(event.pluginId()),
                null,
                null,
                null,
                null,
                name(event.code()),
                value(event.previousVersion()),
                value(event.resultingVersion()),
                value(event.correlationId()),
                event.occurredAt()));
    }

    public void record(SecurityAuditEvent event) {
        persist(new AuditEventEntity(
                AuditEventCategory.SECURITY_OPERATION,
                event.operation().name(),
                AuditEventOutcome.valueOf(event.outcome().name()),
                event.actor().userId().isPresent() ? AUTHENTICATED_USER : SYSTEM,
                appUserUuid(event.actor().userId()),
                appUserUuid(event.subjectUserId()),
                companyUuid(event.companyId()),
                roleUuid(event.roleId()),
                null,
                null,
                text(event.permissionId()),
                null,
                null,
                null,
                name(event.code()),
                value(event.previousVersion()),
                value(event.resultingVersion()),
                value(event.actor().correlationId()),
                event.occurredAt()));
    }

    public void record(AccessAuditEvent event) {
        persist(new AuditEventEntity(
                AuditEventCategory.TRUSTED_ACCESS,
                event.operation().name(),
                AuditEventOutcome.valueOf(event.outcome().name()),
                event.actorUserId().isPresent() ? AUTHENTICATED_USER : UNRESOLVED,
                appUserUuid(event.actorUserId()),
                null,
                companyUuid(event.companyId()),
                null,
                null,
                text(event.pluginId()),
                text(event.permissionId()),
                text(event.screenId()),
                null,
                null,
                name(event.code()),
                null,
                null,
                event.correlationId(),
                event.occurredAt()));
    }

    public void record(SystemAuthorityAuditEvent event) {
        persist(new AuditEventEntity(
                AuditEventCategory.SYSTEM_AUTHORITY_OPERATION,
                event.operation().name(),
                AuditEventOutcome.valueOf(event.outcome().name()),
                event.actor().userId().isPresent() ? AUTHENTICATED_USER : SYSTEM,
                appUserUuid(event.actor().userId()),
                appUserUuid(event.subjectUserId()),
                null,
                null,
                systemRoleUuid(event.systemRoleId()),
                null,
                text(event.permission()),
                null,
                null,
                null,
                name(event.code()),
                value(event.previousVersion()),
                value(event.resultingVersion()),
                value(event.actor().correlationId()),
                event.occurredAt()));
    }

    public void record(SystemAuthorityAccessAuditEvent event) {
        persist(new AuditEventEntity(
                AuditEventCategory.SYSTEM_AUTHORITY_ACCESS,
                "AUTHORIZE_SYSTEM_OPERATION",
                AuditEventOutcome.valueOf(event.outcome().name()),
                event.actorUserId().isPresent() ? AUTHENTICATED_USER : UNRESOLVED,
                appUserUuid(event.actorUserId()),
                null,
                null,
                null,
                null,
                null,
                text(event.requiredPermission()),
                null,
                null,
                null,
                name(event.code()),
                null,
                null,
                event.correlationId(),
                event.occurredAt()));
    }

    @Override
    public void record(TechnicalAuditEvent event) {
        Objects.requireNonNull(event, "event");
        persist(new AuditEventEntity(
                AuditEventCategory.PLUGIN_OPERATION,
                event.operation(),
                AuditEventOutcome.valueOf(event.outcome().name()),
                AUTHENTICATED_USER,
                event.actorUserId().value(),
                null,
                event.companyId().value(),
                null,
                null,
                event.pluginId(),
                event.permissionId(),
                null,
                event.resourceType(),
                event.resourceId().orElse(null),
                event.resultCode(),
                event.previousVersion().orElse(null),
                event.resultingVersion().orElse(null),
                event.correlationId(),
                event.occurredAt()));
    }

    private void persist(AuditEventEntity entity) {
        entityManager.persist(entity);
    }

    private static <T> T value(Optional<T> value) {
        return value.orElse(null);
    }

    private static <T> String text(Optional<T> value) {
        return value.map(Object::toString).orElse(null);
    }

    private static <E extends Enum<E>> String name(Optional<E> value) {
        return value.map(Enum::name).orElse(null);
    }

    private static UUID appUserUuid(Optional<AppUserId> value) {
        return value.map(AppUserId::value).orElse(null);
    }

    private static UUID companyUuid(Optional<CompanyId> value) {
        return value.map(CompanyId::value).orElse(null);
    }

    private static UUID roleUuid(Optional<RoleId> value) {
        return value.map(role -> role.value()).orElse(null);
    }

    private static UUID systemRoleUuid(Optional<SystemRoleId> value) {
        return value.map(role -> role.value()).orElse(null);
    }
}
