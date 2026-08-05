package py.com.logixone.web.admin;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.application.audit.admin.AuditEventCategory;
import py.com.logixone.kernel.application.audit.admin.AuditEventOutcome;
import py.com.logixone.kernel.application.audit.admin.AuditEventView;

public final class AdminAuditEventView {

    private static final DateTimeFormatter UTC_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final String eventId;
    private final String categoryLabel;
    private final String operation;
    private final String outcomeLabel;
    private final String outcomeStyle;
    private final String actorKindLabel;
    private final String occurredAt;
    private final List<AdminAuditDetailView> details;

    private AdminAuditEventView(
            String eventId,
            String categoryLabel,
            String operation,
            String outcomeLabel,
            String outcomeStyle,
            String actorKindLabel,
            String occurredAt,
            List<AdminAuditDetailView> details) {
        this.eventId = eventId;
        this.categoryLabel = categoryLabel;
        this.operation = operation;
        this.outcomeLabel = outcomeLabel;
        this.outcomeStyle = outcomeStyle;
        this.actorKindLabel = actorKindLabel;
        this.occurredAt = occurredAt;
        this.details = List.copyOf(details);
    }

    static AdminAuditEventView from(AuditEventView event) {
        Objects.requireNonNull(event, "event");
        List<AdminAuditDetailView> details = new ArrayList<>();
        event.actorUserId().ifPresent(value -> add(details, "Actor local", value.toString()));
        event.subjectUserId().ifPresent(value -> add(details, "Usuario afectado", value.toString()));
        event.companyId().ifPresent(value -> add(details, "Empresa", value.toString()));
        event.roleId().ifPresent(value -> add(details, "Rol empresarial", value));
        event.systemRoleId().ifPresent(value -> add(details, "Rol global", value));
        event.pluginId().ifPresent(value -> add(details, "Plugin", value));
        event.permissionId().ifPresent(value -> add(details, "Permiso", value));
        event.screenId().ifPresent(value -> add(details, "Pantalla", value));
        event.resourceType().ifPresent(value -> add(details, "Tipo de recurso", value));
        event.resourceId().ifPresent(value -> add(details, "Recurso", value));
        event.code().ifPresent(value -> add(details, "Código", value));
        event.previousVersion().ifPresent(value -> add(details, "Versión anterior", value.toString()));
        event.resultingVersion().ifPresent(value -> add(details, "Versión resultante", value.toString()));
        event.correlationId().ifPresent(value -> add(details, "Correlación", value));
        return new AdminAuditEventView(
                event.auditEventId().toString(),
                categoryLabel(event.category()),
                event.operation(),
                outcomeLabel(event.outcome()),
                outcomeStyle(event.outcome()),
                actorKindLabel(event.actorKind()),
                UTC_FORMAT.format(event.occurredAt()),
                details);
    }

    private static void add(List<AdminAuditDetailView> details, String label, String value) {
        details.add(new AdminAuditDetailView(label, value));
    }

    private static String categoryLabel(AuditEventCategory category) {
        return switch (category) {
            case COMPANY_OPERATION -> "Empresa y plugins";
            case SECURITY_OPERATION -> "Seguridad empresarial";
            case TRUSTED_ACCESS -> "Acceso empresarial";
            case SYSTEM_AUTHORITY_OPERATION -> "Autoridad global";
            case SYSTEM_AUTHORITY_ACCESS -> "Acceso administrativo";
            case PLUGIN_OPERATION -> "Operación de plugin";
        };
    }

    private static String outcomeLabel(AuditEventOutcome outcome) {
        return switch (outcome) {
            case CHANGED -> "Aplicado";
            case UNCHANGED -> "Sin cambios";
            case REJECTED -> "Rechazado";
            case ALLOWED -> "Permitido";
            case DENIED -> "Denegado";
            case SELECTION_REQUIRED -> "Requiere selección";
        };
    }

    private static String outcomeStyle(AuditEventOutcome outcome) {
        return switch (outcome) {
            case CHANGED, ALLOWED -> "audit-outcome-positive";
            case REJECTED, DENIED -> "audit-outcome-negative";
            case UNCHANGED, SELECTION_REQUIRED -> "audit-outcome-neutral";
        };
    }

    private static String actorKindLabel(String actorKind) {
        return switch (actorKind) {
            case "AUTHENTICATED_USER" -> "Usuario autenticado";
            case "SYSTEM" -> "Sistema";
            case "TEST" -> "Prueba técnica";
            case "UNRESOLVED" -> "Identidad no resuelta";
            default -> "Actor técnico";
        };
    }

    public String getEventId() { return eventId; }
    public String getCategoryLabel() { return categoryLabel; }
    public String getOperation() { return operation; }
    public String getOutcomeLabel() { return outcomeLabel; }
    public String getOutcomeStyle() { return outcomeStyle; }
    public String getActorKindLabel() { return actorKindLabel; }
    public String getOccurredAt() { return occurredAt; }
    public List<AdminAuditDetailView> getDetails() { return details; }
}
