package py.com.logixone.web.shell;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;
import py.com.logixone.plugin.api.PlatformSelectorSourceDefinition;
import py.com.logixone.web.security.TrustedAdminWebAccess;
import py.com.logixone.web.security.TrustedCompanySession;
import py.com.logixone.web.security.TrustedWebAccessException;
import py.com.logixone.web.selector.NativeSelectorSourceCatalog;

/** POST navigation and reauthorization boundary for shell-owned native selectors. */
@Named("nativeSelectorReturn")
@RequestScoped
public class NativeSelectorReturnViewBean {

    @Inject TrustedAdminWebAccess access;
    @Inject TrustedCompanySession companySession;
    @Inject NativeSelectorReturnContextStore contexts;
    @Inject HttpServletRequest request;

    private String selectorContextId;
    private Optional<NativeSelectorReturnContext> targetContext = Optional.empty();
    private boolean restored;

    @PostConstruct
    void initialize() {
        selectorContextId = normalizedToken(request.getParameter("selectorContext"));
        if (selectorContextId != null) {
            targetContext = authorizedTarget(currentRoute());
        }
    }

    public String open(String usageId) {
        try {
            NativeSelectorReturnPlan plan = NativeSelectorReturnPlan.find(usageId)
                    .orElseThrow(TrustedWebAccessException::forbidden);
            if (!plan.originRoute().equals(currentRoute())) {
                throw TrustedWebAccessException.forbidden();
            }
            SystemAuthorityContext actor = requireManagement(usageId);
            Map<String, String> draft = SelectorReturnDraft.retain(
                    SelectorReturnDraft.decode(request.getParameter("selectorDraft")),
                    plan.draftInputIds());
            String token = contexts.remember(new NativeSelectorReturnContext(
                    actor.actorUserId().toString(),
                    companySession.revision(),
                    usageId,
                    plan.originRoute(),
                    plan.originTitle(),
                    plan.targetRoute(),
                    draft));
            return redirect(plan.targetRoute(), "selectorContext", token);
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            return null;
        }
    }

    public String returnToOrigin() {
        Optional<NativeSelectorReturnContext> context = authorizedTarget(currentRoute());
        if (context.isEmpty()) {
            return null;
        }
        NativeSelectorReturnContext value = context.orElseThrow();
        return redirect(value.originRoute(), "selectorReturn", selectorContextId);
    }

    public Optional<NativeSelectorReturnRestoration> restore(String originRoute) {
        String token = normalizedToken(request.getParameter("selectorReturn"));
        if (token == null || !validRoute(originRoute)) {
            return Optional.empty();
        }
        try {
            SystemAuthorityContext actor = access.requireAny();
            Optional<NativeSelectorReturnContext> found = contexts.findForOrigin(
                    token, actor.actorUserId().toString(), companySession.revision(), originRoute);
            if (found.isEmpty()) {
                return Optional.empty();
            }
            NativeSelectorReturnContext context = found.orElseThrow();
            requireManagement(context.usageId());
            Optional<NativeSelectorReturnContext> consumed = contexts.consumeForOrigin(
                    token, actor.actorUserId().toString(), companySession.revision(), originRoute);
            if (consumed.isEmpty()) {
                return Optional.empty();
            }
            restored = true;
            return Optional.of(new NativeSelectorReturnRestoration(
                    context.usageId(), context.inputs()));
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            return Optional.empty();
        }
    }

    public Map<String, String> targetInputs(String targetRoute) {
        if (!targetRoute.equals(currentRoute())) {
            return Map.of();
        }
        return targetContext.map(NativeSelectorReturnContext::inputs).orElse(Map.of());
    }

    public String preserve(String outcome) {
        if (outcome == null || targetContext.isEmpty() || selectorContextId == null) {
            return outcome;
        }
        String separator = outcome.contains("?") ? "&" : "?";
        return outcome + separator + "selectorContext=" + query(selectorContextId);
    }

    public boolean isReturnAvailable() {
        return targetContext.isPresent();
    }

    public boolean isRestored() {
        return restored;
    }

    public String getSelectorContextId() {
        return selectorContextId;
    }

    public void setSelectorContextId(String selectorContextId) {
        this.selectorContextId = normalizedToken(selectorContextId);
    }

    public String getReturnLabel() {
        return targetContext.map(context -> "Volver a " + context.originTitle())
                .orElse("Volver al formulario anterior");
    }

    private Optional<NativeSelectorReturnContext> authorizedTarget(String targetRoute) {
        if (selectorContextId == null || !validRoute(targetRoute)) {
            return Optional.empty();
        }
        try {
            SystemAuthorityContext actor = access.requireAny();
            Optional<NativeSelectorReturnContext> found = contexts.findForTarget(
                    selectorContextId,
                    actor.actorUserId().toString(),
                    companySession.revision(),
                    targetRoute);
            if (found.isEmpty()) {
                return Optional.empty();
            }
            SystemAuthorityContext authorized = requireManagement(found.orElseThrow().usageId());
            if (!authorized.actorUserId().equals(actor.actorUserId())) {
                return Optional.empty();
            }
            return found;
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            return Optional.empty();
        }
    }

    private SystemAuthorityContext requireManagement(String usageId) {
        PlatformSelectorSourceDefinition source = NativeSelectorSourceCatalog.source(usageId);
        if (!source.manageable()) {
            throw TrustedWebAccessException.forbidden();
        }
        return access.require(new SystemPermission(
                source.managementPermission().orElseThrow().value()));
    }

    private String currentRoute() {
        return applicationRoute(request.getRequestURI(), request.getContextPath());
    }

    static String applicationRoute(String requestUri, String contextPath) {
        if (requestUri == null) {
            return "";
        }
        String route = requestUri;
        if (contextPath != null && !contextPath.isEmpty() && route.startsWith(contextPath)) {
            route = route.substring(contextPath.length());
        }
        if (route.startsWith("/faces/")) {
            route = route.substring("/faces".length());
        }
        return validRoute(route) ? route : "";
    }

    private static String redirect(String route, String parameter, String token) {
        return route + "?faces-redirect=true&" + parameter + "=" + query(token);
    }

    private static String query(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String normalizedToken(String token) {
        if (token == null || token.length() != 36) {
            return null;
        }
        try {
            return java.util.UUID.fromString(token).toString().equals(token) ? token : null;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static boolean validRoute(String route) {
        return route != null && route.startsWith("/") && !route.startsWith("//")
                && route.length() <= 160
                && route.codePoints().noneMatch(Character::isWhitespace);
    }
}
