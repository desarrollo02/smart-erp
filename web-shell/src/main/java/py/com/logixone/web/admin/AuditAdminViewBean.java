package py.com.logixone.web.admin;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.audit.admin.AuditEventCategory;
import py.com.logixone.kernel.application.audit.admin.AuditEventOutcome;
import py.com.logixone.kernel.application.audit.admin.AuditPage;
import py.com.logixone.kernel.application.audit.admin.AuditQuery;
import py.com.logixone.kernel.application.audit.admin.AuditTimeWindow;
import py.com.logixone.kernel.application.audit.port.AuditQueryPort;
import py.com.logixone.web.security.TrustedAdminWebAccess;

/** Request-scoped, read-only adapter for bounded technical audit queries. */
@Named("auditAdminView")
@RequestScoped
public class AuditAdminViewBean {

    private static final int PAGE_SIZE = 25;

    @Inject TrustedAdminWebAccess access;
    @Inject AuditQueryPort auditQuery;

    private List<AdminAuditEventView> events = List.of();
    private List<AdminOptionView> categoryOptions = List.of();
    private List<AdminOptionView> outcomeOptions = List.of();
    private List<AdminOptionView> windowOptions = List.of();
    private String category;
    private String outcome;
    private String timeWindow = AuditTimeWindow.LAST_7_DAYS.name();
    private String companyId;
    private String correlationId;
    private String page = "0";
    private boolean hasNext;
    private int currentPage;
    private boolean validQuery = true;

    @PostConstruct
    void initialize() {
        access.require(SystemPermission.AUDIT_VIEW);
        categoryOptions = Arrays.stream(AuditEventCategory.values())
                .map(value -> new AdminOptionView(value.name(), categoryLabel(value)))
                .toList();
        outcomeOptions = Arrays.stream(AuditEventOutcome.values())
                .map(value -> new AdminOptionView(value.name(), outcomeLabel(value)))
                .toList();
        windowOptions = List.of(
                new AdminOptionView(AuditTimeWindow.LAST_24_HOURS.name(), "Últimas 24 horas"),
                new AdminOptionView(AuditTimeWindow.LAST_7_DAYS.name(), "Últimos 7 días"),
                new AdminOptionView(AuditTimeWindow.LAST_30_DAYS.name(), "Últimos 30 días"),
                new AdminOptionView(AuditTimeWindow.ALL.name(), "Todo desde V5"));
    }

    public void load() {
        try {
            AuditPage result = auditQuery.query(toQuery());
            currentPage = result.page();
            hasNext = result.hasNext();
            events = result.events().stream().map(AdminAuditEventView::from).toList();
        } catch (IllegalArgumentException invalid) {
            validQuery = false;
            events = List.of();
            AdminAuditMessages.invalidQuery();
        }
    }

    public String applyFilters() {
        access.require(SystemPermission.AUDIT_VIEW);
        page = "0";
        try {
            toQuery();
            return "/admin/audit.xhtml?faces-redirect=true&includeViewParams=true";
        } catch (IllegalArgumentException invalid) {
            AdminAuditMessages.invalidQuery();
            return null;
        }
    }

    public String clearFilters() {
        access.require(SystemPermission.AUDIT_VIEW);
        return "/admin/audit.xhtml?faces-redirect=true";
    }

    private AuditQuery toQuery() {
        return new AuditQuery(
                enumFilter(category, AuditEventCategory.class),
                enumFilter(outcome, AuditEventOutcome.class),
                requiredEnum(timeWindow, AuditTimeWindow.class),
                companyFilter(companyId),
                textFilter(correlationId),
                page(page),
                PAGE_SIZE);
    }

    private static <E extends Enum<E>> Optional<E> enumFilter(String value, Class<E> type) {
        return textFilter(value).map(candidate -> requiredEnum(candidate, type));
    }

    private static <E extends Enum<E>> E requiredEnum(String value, Class<E> type) {
        try {
            return Enum.valueOf(type, value);
        } catch (NullPointerException | IllegalArgumentException invalid) {
            throw new IllegalArgumentException("unknown closed filter", invalid);
        }
    }

    private static Optional<CompanyId> companyFilter(String value) {
        return textFilter(value).map(CompanyId::parse);
    }

    private static Optional<String> textFilter(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String candidate = value.strip();
        if (!candidate.equals(value)) {
            throw new IllegalArgumentException("filter must be canonical");
        }
        return Optional.of(candidate);
    }

    private static int page(String value) {
        if (value == null || !value.matches("0|[1-9][0-9]{0,4}")) {
            throw new IllegalArgumentException("page must be canonical");
        }
        return Integer.parseInt(value);
    }

    private static String categoryLabel(AuditEventCategory value) {
        return switch (value) {
            case COMPANY_OPERATION -> "Empresa y plugins";
            case SECURITY_OPERATION -> "Seguridad empresarial";
            case TRUSTED_ACCESS -> "Acceso empresarial";
            case SYSTEM_AUTHORITY_OPERATION -> "Autoridad global";
            case SYSTEM_AUTHORITY_ACCESS -> "Acceso administrativo";
            case PLUGIN_OPERATION -> "Operación de plugin";
        };
    }

    private static String outcomeLabel(AuditEventOutcome value) {
        return switch (value) {
            case CHANGED -> "Aplicado";
            case UNCHANGED -> "Sin cambios";
            case REJECTED -> "Rechazado";
            case ALLOWED -> "Permitido";
            case DENIED -> "Denegado";
            case SELECTION_REQUIRED -> "Requiere selección";
        };
    }

    public List<AdminAuditEventView> getEvents() { return events; }
    public List<AdminOptionView> getCategoryOptions() { return categoryOptions; }
    public List<AdminOptionView> getOutcomeOptions() { return outcomeOptions; }
    public List<AdminOptionView> getWindowOptions() { return windowOptions; }
    public boolean isHasNext() { return hasNext; }
    public boolean isHasPrevious() { return currentPage > 0; }
    public boolean isValidQuery() { return validQuery; }
    public int getCurrentPageNumber() { return currentPage + 1; }
    public int getPreviousPage() { return Math.max(0, currentPage - 1); }
    public int getNextPage() { return currentPage + 1; }
    public int getPageSize() { return PAGE_SIZE; }
    public String getCategory() { return category; }
    public void setCategory(String value) { category = value; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String value) { outcome = value; }
    public String getTimeWindow() { return timeWindow; }
    public void setTimeWindow(String value) { timeWindow = value; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String value) { companyId = value; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String value) { correlationId = value; }
    public String getPage() { return page; }
    public void setPage(String value) { page = value; }
}
