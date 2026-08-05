package py.com.logixone.web.shell;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.web.selector.NativeSelectorSourceCatalog;

/** Closed whitelist of native forms that may open contextual selector administration. */
record NativeSelectorReturnPlan(
        String usageId,
        String originRoute,
        String originTitle,
        String targetRoute,
        Set<String> draftInputIds) {

    private static final Map<String, NativeSelectorReturnPlan> PLANS = plans();

    NativeSelectorReturnPlan {
        if (!usageId.matches("[a-z][a-z0-9_.]{2,127}")) {
            throw new IllegalArgumentException("Invalid native selector usage");
        }
        originRoute = route(originRoute);
        targetRoute = route(targetRoute);
        if (originTitle == null || originTitle.isBlank() || originTitle.length() > 160) {
            throw new IllegalArgumentException("Invalid native selector origin title");
        }
        draftInputIds = Set.copyOf(draftInputIds);
    }

    static Optional<NativeSelectorReturnPlan> find(String usageId) {
        return Optional.ofNullable(PLANS.get(usageId));
    }

    static Map<String, NativeSelectorReturnPlan> all() {
        return PLANS;
    }

    private static Map<String, NativeSelectorReturnPlan> plans() {
        Map<String, NativeSelectorReturnPlan> plans = new LinkedHashMap<>();
        add(plans, NativeSelectorSourceCatalog.APP_COMPANY_SWITCHER,
                "/app/index.xhtml", "selección de empresa", "/admin/companies.xhtml",
                Set.of("selected_company_id"));
        add(plans, NativeSelectorSourceCatalog.APP_COMPANY_SELECTION,
                "/app/index.xhtml", "selección de empresa", "/admin/companies.xhtml",
                Set.of("selected_company_id"));
        add(plans, NativeSelectorSourceCatalog.PLUGINS_COMPANY,
                "/admin/plugins.xhtml", "configuración de plugins", "/admin/companies.xhtml",
                Set.of("company_id"));
        add(plans, NativeSelectorSourceCatalog.SECURITY_COMPANY,
                "/admin/security.xhtml", "seguridad empresarial", "/admin/companies.xhtml",
                Set.of("company_id"));
        add(plans, NativeSelectorSourceCatalog.SECURITY_MEMBERSHIP_USER,
                "/admin/security.xhtml", "registro de membresía", "/admin/security.xhtml",
                Set.of("company_id", "membership_user_id"));
        add(plans, NativeSelectorSourceCatalog.SECURITY_ASSIGNMENT_USER,
                "/admin/security.xhtml", "asignación de rol empresarial", "/admin/security.xhtml",
                Set.of("company_id", "assignment_user_id", "assignment_role_id"));
        add(plans, NativeSelectorSourceCatalog.SECURITY_ASSIGNMENT_ROLE,
                "/admin/security.xhtml", "asignación de rol empresarial", "/admin/security.xhtml",
                Set.of("company_id", "assignment_user_id", "assignment_role_id"));
        add(plans, NativeSelectorSourceCatalog.SECURITY_GRANT_ROLE,
                "/admin/security.xhtml", "concesión de permiso empresarial", "/admin/security.xhtml",
                Set.of("company_id", "grant_role_id", "grant_permission_id"));
        add(plans, NativeSelectorSourceCatalog.SYSTEM_ASSIGNMENT_USER,
                "/admin/system-authority.xhtml", "asignación de rol global", "/admin/security.xhtml",
                Set.of("assignment_user_id", "assignment_role_id"));
        add(plans, NativeSelectorSourceCatalog.SYSTEM_ASSIGNMENT_ROLE,
                "/admin/system-authority.xhtml", "asignación de rol global", "/admin/system-authority.xhtml",
                Set.of("assignment_user_id", "assignment_role_id"));
        add(plans, NativeSelectorSourceCatalog.SYSTEM_GRANT_ROLE,
                "/admin/system-authority.xhtml", "concesión de permiso global", "/admin/system-authority.xhtml",
                Set.of("grant_role_id", "grant_permission_id"));
        return Map.copyOf(plans);
    }

    private static void add(
            Map<String, NativeSelectorReturnPlan> plans,
            String usageId,
            String originRoute,
            String originTitle,
            String targetRoute,
            Set<String> draftInputIds) {
        NativeSelectorReturnPlan previous = plans.put(usageId, new NativeSelectorReturnPlan(
                usageId, originRoute, originTitle, targetRoute, draftInputIds));
        if (previous != null) {
            throw new IllegalStateException("Duplicate native selector return plan: " + usageId);
        }
    }

    private static String route(String value) {
        if (value == null || !value.startsWith("/") || value.startsWith("//")
                || value.length() > 160 || value.codePoints().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Invalid native selector route");
        }
        return value;
    }
}
