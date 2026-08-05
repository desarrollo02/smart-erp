package py.com.logixone.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "logixone.e2e", matches = "true")
class VisualDemoIT {

    private static final String MULTIPLE_COMPANIES_USER = "demo.empresas.ab";
    private static final String SINGLE_COMPANY_USER = "demo.empresa.a";
    private static final String NO_COMPANY_USER = "demo.sin.empresa";

    private Playwright playwright;
    private Browser browser;
    private String appUrl;
    private String adminUrl;
    private String password;
    private Path evidenceDirectory;

    @BeforeAll
    void launchBrowser() throws IOException {
        appUrl = requiredProperty("logixone.app-url");
        adminUrl = requiredProperty("logixone.admin-url");
        password = readSecret(requiredProperty("logixone.demo-user-password-file"));
        evidenceDirectory = Path.of(requiredProperty("logixone.evidence-dir"));
        Files.createDirectories(evidenceDirectory);

        playwright = Playwright.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
        String executable = System.getProperty("logixone.playwright.executable");
        if (executable != null && !executable.isBlank()) {
            options.setExecutablePath(Path.of(executable));
        }
        browser = playwright.chromium().launch(options);
    }

    @AfterAll
    void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    void multipleMembershipsRejectManipulationAndRenderIsolatedResponsiveVariants() {
        try (BrowserContext context = newContext(1280, 900)) {
            Page page = login(context, MULTIPLE_COMPANIES_USER);
            Locator selector = requireOne(page.getByLabel("Empresa autorizada"), "company selector");
            assertEquals(3, selector.locator("option").count());

            String forgedCompanyId = UUID.randomUUID().toString();
            selector.evaluate("""
                    (element, forgedValue) => {
                      const option = document.createElement('option');
                      option.value = forgedValue;
                      option.textContent = 'Empresa no autorizada';
                      element.appendChild(option);
                      element.value = forgedValue;
                      element.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    """, forgedCompanyId);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continuar")),
                    "continue button").click();
            assertEquals(0, page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles")).count());

            page.navigate(appUrl);
            selector = requireOne(page.getByLabel("Empresa autorizada"), "company selector after rejection");
            List<String> authorizedValues = optionValues(selector).stream()
                    .filter(value -> !value.isBlank())
                    .toList();
            assertEquals(2, authorizedValues.size());
            assertFalse(authorizedValues.contains(forgedCompanyId));

            selector.selectOption(new SelectOption().setValue(authorizedValues.getFirst()));
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continuar")),
                    "continue authorized company").click();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles")),
                    "authorized menu").waitFor();

            List<VariantVisit> visits = new ArrayList<>();
            visits.add(openReferenceScreen(page));
            screenshot(page, "company-" + visits.getFirst().variant().toLowerCase() + "-desktop.png");

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Volver al espacio de trabajo")),
                    "workspace link").click();
            Locator switcher = requireOne(page.getByLabel("Cambiar empresa"), "company switcher");
            String currentCompany = switcher.inputValue();
            String otherCompany = authorizedValues.stream()
                    .filter(value -> !value.equals(currentCompany))
                    .findFirst()
                    .orElseThrow();
            switcher.selectOption(otherCompany);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cambiar")),
                    "change company button").click();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles")),
                    "authorized menu after company change").waitFor();
            visits.add(openReferenceScreen(page));
            screenshot(page, "company-" + visits.getLast().variant().toLowerCase() + "-desktop.png");

            List<String> variants = visits.stream()
                    .map(VariantVisit::variant)
                    .sorted(Comparator.naturalOrder())
                    .toList();
            assertEquals(List.of("A", "B"), variants);
            VariantVisit companyA = visits.stream().filter(visit -> visit.variant().equals("A")).findFirst().orElseThrow();
            VariantVisit companyB = visits.stream().filter(visit -> visit.variant().equals("B")).findFirst().orElseThrow();
            assertTrue(companyA.insertedNotice());
            assertFalse(companyA.simplifiedNotice());
            assertFalse(companyA.hiddenElementNotice());
            assertFalse(companyB.insertedNotice());
            assertTrue(companyB.simplifiedNotice());
            assertTrue(companyB.hiddenElementNotice());

            assertResponsive(page, 720, 900, "company-" + companyB.variant().toLowerCase() + "-medium.png");
            assertResponsive(page, 375, 812, "company-" + companyB.variant().toLowerCase() + "-compact.png");
        }
    }

    @Test
    void singleMembershipSelectsItsOnlyCompanyAndLogoutInvalidatesTheSession() {
        try (BrowserContext context = newContext(1280, 900)) {
            Page page = login(context, SINGLE_COMPANY_USER);
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles")),
                    "single-company workspace").waitFor();
            assertEquals(0, page.getByLabel("Empresa autorizada").count());
            VariantVisit variant = openReferenceScreen(page);
            assertEquals("A", variant.variant());

            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cerrar sesión")),
                    "logout button").click();
            Locator loginAfterLogout = page.getByLabel("Username");
            loginAfterLogout.waitFor();
            assertEquals(1, loginAfterLogout.count(), "Keycloak login after logout");
            assertNotEquals("localhost", URIHost.from(page.url()));

            page.navigate(appUrl);
            Locator loginAfterReuseAttempt = page.getByLabel("Username");
            loginAfterReuseAttempt.waitFor();
            assertEquals(1, loginAfterReuseAttempt.count(), "Keycloak login after session reuse attempt");
            assertNotEquals("localhost", URIHost.from(page.url()));
        }
    }

    @Test
    void identityWithoutMembershipReceivesAControlledDenial() {
        try (BrowserContext context = newContext(1280, 900)) {
            Page page = login(context, NO_COMPANY_USER);

            requireOne(page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(
                            "No podemos abrir un espacio de trabajo para esta sesión")),
                    "controlled no-membership denial").waitFor();
            assertEquals(0, page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles")).count());
        }
    }

    @Test
    void globalAdministratorNavigatesCanonicalResponsiveAdministrativeSurfaces() {
        try (BrowserContext context = newContext(1280, 900)) {
            AtomicReference<Map<String, String>> allowedHeaders = new AtomicReference<>();
            String protectedLandingPath = java.net.URI.create(adminUrl).getPath();
            Page page = context.newPage();
            page.onResponse(response -> {
                String responsePath = java.net.URI.create(response.url()).getPath();
                if (protectedLandingPath.equals(responsePath) && response.status() == 200) {
                    allowedHeaders.set(response.headers());
                }
            });
            authenticate(page, adminUrl, MULTIPLE_COMPANIES_USER);
            page.locator("main").waitFor(new Locator.WaitForOptions().setTimeout(30_000));
            requireOne(page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("Administración segura del kernel")),
                    "administration landing").waitFor();
            assertAdminSecurityHeaders(allowedHeaders.get(), "allowed administration landing");

            String adminPath = java.net.URI.create(adminUrl).getPath();
            String adminBasePath = adminPath.substring(0, adminPath.lastIndexOf('/') + 1);
            List<AdminSurface> surfaces = List.of(
                    new AdminSurface("index.xhtml", "Administración segura del kernel", "landing"),
                    new AdminSurface("companies.xhtml", "Empresas y personalización obligatoria", "companies"),
                    new AdminSurface("plugins.xhtml", "Plugins por empresa", "plugins"),
                    new AdminSurface("security.xhtml", "Usuarios y acceso por empresa", "security"),
                    new AdminSurface("system-authority.xhtml", "Roles y permisos de la instancia", "system-authority"),
                    new AdminSurface("audit.xhtml", "Actividad técnica del kernel", "audit"));

            for (AdminSurface surface : surfaces) {
                String targetUrl = adminUrl.substring(0, adminUrl.length() - "index.xhtml".length())
                        + surface.view();
                page.navigate(targetUrl);
                requireOne(page.getByRole(
                        AriaRole.HEADING,
                        new Page.GetByRoleOptions().setName(surface.heading())),
                        surface.name() + " heading").waitFor();
                assertAccessibleStructure(page, surface.name());

                assertResponsive(page, 1280, 900, "admin-" + surface.name() + "-expanded.png");
                assertResponsive(page, 720, 900, "admin-" + surface.name() + "-medium.png");
                assertResponsive(page, 375, 812, "admin-" + surface.name() + "-compact.png");
                for (int boundary : List.of(599, 600, 839, 840)) {
                    assertResponsiveWithoutScreenshot(
                            page, boundary, 900, surface.name() + "-boundary-" + boundary);
                }
            }

            page.setViewportSize(1280, 900);
            page.navigate(adminUrl);
            page.keyboard().press("Tab");
            String focusedClass = String.valueOf(page.evaluate(
                    "() => document.activeElement.className"));
            assertTrue(focusedClass.contains("skip-link"),
                    "the first keyboard target must expose the skip link");
            page.keyboard().press("Enter");
            assertEquals("#admin-content", page.evaluate("() => location.hash"));
            assertEquals("admin-content", page.evaluate("() => document.activeElement.id"));

            for (AdminSurface surface : surfaces.subList(1, surfaces.size())) {
                Locator canonicalLink = page.locator(
                        "a[href='" + adminBasePath + surface.view() + "']");
                assertEquals(1, canonicalLink.count(),
                        surface.name() + " canonical navigation link");
            }

            Locator companiesLink = requireOne(
                    page.locator("a[href='" + adminBasePath + "companies.xhtml']"),
                    "canonical companies link");
            companiesLink.click();
            requireOne(page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("Empresas y personalización obligatoria")),
                    "companies page after canonical click").waitFor();
            assertEquals(adminBasePath + "companies.xhtml", java.net.URI.create(page.url()).getPath());
        }
    }

    @Test
    void businessOnlyIdentityCannotEnterGlobalAdministration() {
        try (BrowserContext context = newContext(1280, 900)) {
            AtomicInteger denialStatus = new AtomicInteger();
            AtomicReference<Map<String, String>> denialHeaders = new AtomicReference<>();
            String protectedPath = java.net.URI.create(adminUrl).getPath();
            Page page = context.newPage();
            page.onResponse(response -> {
                String responsePath = java.net.URI.create(response.url()).getPath();
                if (protectedPath.equals(responsePath) && response.status() >= 400) {
                    denialStatus.set(response.status());
                    denialHeaders.set(response.headers());
                }
            });
            authenticate(page, adminUrl, SINGLE_COMPANY_USER);

            assertEquals(403, denialStatus.get(),
                    "the business-only identity must receive HTTP 403 after authentication");
            assertAdminSecurityHeaders(denialHeaders.get(), "denied administration landing");
            assertEquals(0, page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("Administración segura del kernel")).count());
            assertEquals(0, page.getByLabel("Username").count(),
                    "the denial must happen after authentication");
        }
    }

    private BrowserContext newContext(int width, int height) {
        return browser.newContext(new Browser.NewContextOptions()
                .setLocale("es-PY")
                .setViewportSize(width, height));
    }

    private Page login(BrowserContext context, String username) {
        Page page = loginAt(context, appUrl, username);
        return page;
    }

    private Page loginAt(BrowserContext context, String targetUrl, String username) {
        Page page = authenticate(context, targetUrl, username);
        page.locator("main").waitFor(new Locator.WaitForOptions().setTimeout(30_000));
        return page;
    }

    private Page authenticate(BrowserContext context, String targetUrl, String username) {
        Page page = context.newPage();
        return authenticate(page, targetUrl, username);
    }

    private Page authenticate(Page page, String targetUrl, String username) {
        page.navigate(targetUrl);
        requireOne(page.getByLabel("Username"), "Keycloak username").fill(username);
        requireOne(page.locator("input[type='password']"), "Keycloak password").fill(password);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")),
                "Keycloak sign in").click();
        page.waitForLoadState();
        return page;
    }

    private VariantVisit openReferenceScreen(Page page) {
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Panel de demostración")),
                "reference menu link").click();
        requireOne(page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Panel de composición empresarial")),
                "composed screen heading").waitFor();
        String body = page.locator("body").innerText();
        String variant;
        if (body.contains("Personalización A aplicada")) {
            variant = "A";
        } else if (body.contains("Personalización B aplicada")) {
            variant = "B";
        } else {
            throw new AssertionError("The composed screen did not expose an authorized variant");
        }
        return new VariantVisit(
                variant,
                body.contains("Validación tributaria destacada"),
                body.contains("Operación simplificada"),
                body.contains("elemento de la pantalla base fue ocultado"));
    }

    private void assertResponsive(Page page, int width, int height, String screenshotName) {
        page.setViewportSize(width, height);
        screenshot(page, screenshotName);
        assertResponsiveLayout(page, width, screenshotName);
    }

    private void assertResponsiveWithoutScreenshot(
            Page page, int width, int height, String description) {
        page.setViewportSize(width, height);
        assertResponsiveLayout(page, width, description);
    }

    private void assertResponsiveLayout(Page page, int width, String description) {
        boolean stylesLoaded = (Boolean) page.evaluate("""
                () => {
                  const applicationSheets = Array.from(document.styleSheets)
                    .filter(sheet => sheet.href
                      && sheet.href.includes('/faces/jakarta.faces.resource/'));
                  return applicationSheets.length > 0
                    && applicationSheets.every(sheet => sheet.cssRules.length > 0);
                }
                """);
        int documentWidth = ((Number) page.evaluate(
                "() => document.documentElement.scrollWidth")).intValue();
        assertTrue(stylesLoaded,
                description + " must load the application Material Design stylesheets");
        assertTrue(documentWidth <= width + 1,
                description + " has horizontal overflow: document="
                        + documentWidth + " px, viewport=" + width + " px");
    }

    private void assertAccessibleStructure(Page page, String description) {
        assertEquals(1, page.locator("main h1").count(),
                description + " must expose exactly one main heading");
        boolean everyEditableControlHasLabel = (Boolean) page.evaluate("""
                () => Array.from(document.querySelectorAll(
                    "input:not([type='hidden']):not([type='submit']), select, textarea"))
                  .every(control => control.labels && control.labels.length > 0)
                """);
        assertTrue(everyEditableControlHasLabel,
                description + " must associate every editable control with a label");
    }

    private void assertAdminSecurityHeaders(Map<String, String> headers, String description) {
        assertTrue(headers != null && !headers.isEmpty(),
                description + " must expose response headers");
        assertTrue(headers.getOrDefault("cache-control", "").contains("no-store"), description);
        assertEquals("no-cache", headers.get("pragma"), description);
        assertEquals("nosniff", headers.get("x-content-type-options"), description);
        assertEquals("DENY", headers.get("x-frame-options"), description);
        assertEquals("no-referrer", headers.get("referrer-policy"), description);
        assertTrue(headers.getOrDefault("permissions-policy", "").contains("camera=()"), description);
        assertTrue(headers.getOrDefault("content-security-policy", "")
                .contains("frame-ancestors 'none'"), description);
    }

    private void screenshot(Page page, String name) {
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(evidenceDirectory.resolve(name))
                .setFullPage(true));
    }

    private static Locator requireOne(Locator locator, String description) {
        int count = locator.count();
        assertEquals(1, count, description + " must resolve exactly one element");
        return locator;
    }

    private static List<String> optionValues(Locator selector) {
        Locator options = selector.locator("option");
        List<String> values = new ArrayList<>();
        for (int index = 0; index < options.count(); index++) {
            String value = options.nth(index).getAttribute("value");
            values.add(value == null ? "" : value);
        }
        return List.copyOf(values);
    }

    private static String readSecret(String pathValue) throws IOException {
        String value = Files.readString(Path.of(pathValue), StandardCharsets.UTF_8).strip();
        if (value.isEmpty() || value.length() > 4096 || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalStateException("The demo password file contains an invalid secret");
        }
        return value;
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for visual E2E tests");
        }
        return value;
    }

    private record VariantVisit(
            String variant,
            boolean insertedNotice,
            boolean simplifiedNotice,
            boolean hiddenElementNotice) {
    }

    private record AdminSurface(String view, String heading, String name) {
    }

    private static final class URIHost {

        private URIHost() {
        }

        static String from(String value) {
            return java.net.URI.create(value).getHost();
        }
    }
}
