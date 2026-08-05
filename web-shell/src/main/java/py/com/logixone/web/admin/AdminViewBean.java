package py.com.logixone.web.admin;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.List;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;
import py.com.logixone.web.security.TrustedAdminWebAccess;

/** Thin request model; authorization and permission resolution remain in application. */
@Named("adminView")
@RequestScoped
public class AdminViewBean {

    @Inject
    TrustedAdminWebAccess access;

    private List<AdminSectionView> sections = List.of();

    @PostConstruct
    void initialize() {
        SystemAuthorityContext context = access.requireAny();
        List<AdminSectionView> allowed = new ArrayList<>();
        addIfAllowed(context, allowed, SystemPermission.COMPANY_MANAGE,
                "E", "Empresas", "Alta, estado y personalización obligatoria por empresa.",
                "/admin/companies.xhtml");
        addIfAllowed(context, allowed, SystemPermission.PLUGIN_MANAGE,
                "P", "Plugins", "Catálogo físico y activación compatible por empresa.",
                "/admin/plugins.xhtml");
        addIfAllowed(context, allowed, SystemPermission.SECURITY_MANAGE,
                "S", "Seguridad", "Usuarios, membresías, roles y permisos empresariales.",
                "/admin/security.xhtml");
        addIfAllowed(context, allowed, SystemPermission.AUDIT_VIEW,
                "A", "Auditoría", "Consulta paginada de operaciones y resultados técnicos.",
                "/admin/audit.xhtml");
        addIfAllowed(context, allowed, SystemPermission.SYSTEM_ADMINISTRATION_MANAGE,
                "G", "Autoridad global", "Roles y permisos globales protegidos contra bloqueo.",
                "/admin/system-authority.xhtml");
        sections = List.copyOf(allowed);
    }

    private static void addIfAllowed(
            SystemAuthorityContext context,
            List<AdminSectionView> target,
            SystemPermission permission,
            String icon,
            String title,
            String description,
            String outcome) {
        if (context.hasPermission(permission)) {
            target.add(new AdminSectionView(icon, title, description, outcome));
        }
    }

    public List<AdminSectionView> getSections() {
        return sections;
    }
}
