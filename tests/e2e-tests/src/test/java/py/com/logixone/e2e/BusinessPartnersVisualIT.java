package py.com.logixone.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "logixone.business-partners.e2e", matches = "true")
class BusinessPartnersVisualIT {

    private static final String MULTIPLE_COMPANIES_USER = "demo.empresas.ab";
    private static final String DEMO_ROLE_CODE = "demo_operator";
    private static final List<String> BUSINESS_PARTNER_PERMISSIONS = List.of(
            "reference_data.view",
            "reference_data.policy.manage",
            "business_partners.view",
            "business_partners.manage",
            "business_partners.roles.manage",
            "business_partners.lifecycle.manage");

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
    void exercisesTheCompleteBusinessPartnerDemoWithoutResponsiveOverflow() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setLocale("es-PY")
                .setViewportSize(1280, 900))) {
            Page page = authenticate(context);
            String companyId = selectFirstAuthorizedCompany(page);
            enablePluginAndGrantPermissions(page, companyId);
            String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String channelKindCode = "telegram_" + suffix.toLowerCase();
            String channelKindName = "Telegram empresarial " + suffix;
            String revisedChannelKindName = "Telegram prioritario " + suffix;
            String identificationTypeCode = "membership_" + suffix.toLowerCase();
            String identificationTypeName = "Carné de socio " + suffix;
            String addressTypeCode = "agency_" + suffix.toLowerCase();
            String addressTypeName = "Agencia " + suffix;
            String addressPurposeCode = "collections_" + suffix.toLowerCase();
            String addressPurposeName = "Cobranzas " + suffix;

            exerciseReferenceData(page);

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Definiciones de socios")),
                    "business-partner definitions menu").click();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions()
                            .setName("Definiciones de socios")
                            .setExact(true)),
                    "business-partner definitions heading").waitFor();
            assertResponsive(page, 1280, 900, "channel-kinds-directory-expanded-1280.png");
            assertResponsive(page, 720, 900, "channel-kinds-directory-medium-720.png");
            assertResponsive(page, 375, 900, "channel-kinds-directory-compact-375.png");

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva definición")),
                    "new channel kind action").click();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Nueva definición")),
                    "new channel kind heading").waitFor();
            requireOne(page.getByLabel(
                    "Clase de definición", new Page.GetByLabelOptions().setExact(true)),
                    "channel kind definition class").selectOption("CHANNEL_KIND");
            requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                    "channel kind code").fill(channelKindCode);
            requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                    "channel kind name").fill(channelKindName);
            assertResponsive(page, 375, 900, "channel-kinds-create-compact-375.png");
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar definición")),
                    "register channel kind").click();
            requireOne(page.getByText(
                    "Tipo de canal registrado", new Page.GetByTextOptions().setExact(true)),
                    "channel kind confirmation").waitFor();
            assertTrue(page.getByText(channelKindName, new Page.GetByTextOptions().setExact(true)).count() >= 1,
                    "the new company-owned channel kind must be visible");

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva revisión")),
                    "channel kind revision tab").click();
            requireOne(page.getByLabel(
                    "Nombre revisado", new Page.GetByLabelOptions().setExact(true)),
                    "revised channel kind name").fill(revisedChannelKindName);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Guardar revisión")),
                    "save channel kind revision").click();
            requireOne(page.getByText(
                    "Nombre de tipo de canal actualizado",
                    new Page.GetByTextOptions().setExact(true)),
                    "channel kind revision confirmation").waitFor();
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Historial")),
                    "channel kind history tab").click();
            assertTrue(page.getByText(
                            revisedChannelKindName,
                            new Page.GetByTextOptions().setExact(true)).count() >= 1,
                    "the current channel kind revision must be visible");
            assertTrue(page.getByText(
                            channelKindName,
                            new Page.GetByTextOptions().setExact(true)).count() >= 1,
                    "the original channel kind revision must remain visible");
            assertResponsive(page, 1280, 900, "channel-kind-history-expanded-1280.png");
            assertResponsive(page, 720, 900, "channel-kind-history-medium-720.png");
            assertResponsive(page, 375, 900, "channel-kind-history-compact-375.png");

            page.setViewportSize(1280, 900);
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Estado")),
                    "channel kind lifecycle tab").click();
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Inactivar definición")),
                    "inactivate channel kind").click();
            requireOne(page.getByText(
                    "Tipo de canal inactivado", new Page.GetByTextOptions().setExact(true)),
                    "channel kind inactivation confirmation").waitFor();
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Resumen")),
                    "inactive channel kind summary tab").click();
            assertTrue(page.getByText(
                            "Inactivo", new Page.GetByTextOptions().setExact(true)).count() >= 1,
                    "the inactive channel kind must remain visible in its detail");
            assertResponsive(page, 1280, 900, "channel-kind-inactive-expanded-1280.png");
            assertResponsive(page, 720, 900, "channel-kind-inactive-medium-720.png");
            assertResponsive(page, 375, 900, "channel-kind-inactive-compact-375.png");

            page.setViewportSize(1280, 900);
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Estado")),
                    "inactive channel kind lifecycle tab").click();
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reactivar definición")),
                    "reactivate channel kind").click();
            requireOne(page.getByText(
                    "Tipo de canal reactivado", new Page.GetByTextOptions().setExact(true)),
                    "channel kind reactivation confirmation").waitFor();
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Resumen")),
                    "reactivated channel kind summary tab").click();
            assertTrue(page.getByText(
                            "Activo", new Page.GetByTextOptions().setExact(true)).count() >= 1,
                    "the reactivated channel kind must become available again");

            registerDefinition(
                    page, "IDENTIFICATION_TYPE", identificationTypeCode, identificationTypeName);
            registerDefinition(page, "ADDRESS_TYPE", addressTypeCode, addressTypeName);
            registerDefinition(
                    page, "ADDRESS_PURPOSE", addressPurposeCode, addressPurposeName);

            page.setViewportSize(1280, 900);
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Socios comerciales")),
                    "business partners menu").click();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions()
                            .setName("Socios comerciales")
                            .setExact(true)),
                    "business partners heading").waitFor();
            assertResponsive(page, 1280, 900, "business-partners-directory-expanded-1280.png");
            assertResponsive(page, 720, 900, "business-partners-directory-medium-720.png");
            assertResponsive(page, 375, 900, "business-partners-directory-compact-375.png");

            String code = "E2E-" + suffix;
            String displayName = "Cliente visual " + suffix;

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo socio")),
                    "new partner action").click();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Nuevo socio comercial")),
                    "new partner heading").waitFor();
            assertResponsive(page, 1280, 900, "business-partners-create-expanded-1280.png");
            assertResponsive(page, 720, 900, "business-partners-create-medium-720.png");
            assertResponsive(page, 375, 900, "business-partners-create-compact-375.png");
            page.setViewportSize(1280, 900);
            requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                    "new partner code").fill(code);
            requireOne(page.getByLabel(
                    "Nombre visible", new Page.GetByLabelOptions().setExact(true)),
                    "new partner display name").fill(displayName);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar")),
                    "register partner").click();

            requireOne(page.getByText("Socio comercial registrado", new Page.GetByTextOptions().setExact(true)),
                    "registration confirmation").waitFor();
            assertTrue(page.getByText(code, new Page.GetByTextOptions().setExact(true)).count() >= 1,
                    "the registered partner code must be visible");

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Volver al directorio")),
                    "return to directory").click();
            requireOne(page.getByLabel("Nombre, código o identificación"),
                    "partner search").fill(code);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar")),
                    "search partner").click();
            Locator resultRow = requireOne(page.locator("tbody tr").filter(
                    new Locator.FilterOptions().setHasText(code)), "the exact search result");

            requireOne(resultRow.getByRole(
                    AriaRole.LINK, new Locator.GetByRoleOptions().setName("Abrir " + displayName)),
                    "open partner detail").click();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName(displayName)),
                    "partner detail heading").waitFor();
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Roles y estado")),
                    "roles tab").click();
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Asignar cliente")),
                    "assign client role").click();

            requireOne(page.getByText("Rol cliente asignado", new Page.GetByTextOptions().setExact(true)),
                    "client role confirmation").waitFor();

            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Asignar proveedor")),
                    "assign supplier role").click();
            requireOne(page.getByText("Rol proveedor asignado", new Page.GetByTextOptions().setExact(true)),
                    "supplier role confirmation").waitFor();

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Identificaciones")),
                    "identifications tab").click();
            Locator identificationType = requireOne(page.locator(
                    "select[data-screen-input='identification_type']"),
                    "identification type");
            identificationType.selectOption(identificationTypeCode);
            assertEquals(identificationTypeName, identificationType.locator("option:checked").innerText(),
                    "the company-owned identification type must be selectable");
            selectSearchOption(page, "País", "Paraguay", "Paraguay · PY", "PY");
            requireOne(page.getByLabel("Número presentado", new Page.GetByLabelOptions().setExact(true)),
                    "presented identification").fill("800" + suffix);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agregar identificación")),
                    "add identification").click();
            requireOne(page.getByText("Identificación agregada", new Page.GetByTextOptions().setExact(true)),
                    "identification confirmation").waitFor();
            assertResponsive(page, 1280, 900, "business-partners-identifications-expanded-1280.png");

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Direcciones")),
                    "addresses tab").click();
            Locator addressSection = requireOne(page.locator("#detail-address-title")
                    .locator("xpath=ancestor::section[1]"), "address section");
            Locator addressType = requireOne(addressSection.getByLabel(
                    "Tipo de dirección", new Locator.GetByLabelOptions().setExact(true)),
                    "address type");
            addressType.selectOption(addressTypeCode);
            assertEquals(addressTypeName, addressType.locator("option:checked").innerText(),
                    "the company-owned address type must be selectable");
            Locator addressPurpose = requireOne(addressSection.getByLabel(
                    "Propósito de dirección", new Locator.GetByLabelOptions().setExact(true)),
                    "address purpose");
            addressPurpose.selectOption(addressPurposeCode);
            assertEquals(addressPurposeName, addressPurpose.locator("option:checked").innerText(),
                    "the company-owned address purpose must be selectable");
            requireOne(addressSection.getByLabel(
                    "Dirección", new Locator.GetByLabelOptions().setExact(true)),
                    "address line").fill("Avenida Demo 123");
            requireOne(addressSection.getByLabel(
                    "Localidad", new Locator.GetByLabelOptions().setExact(true)),
                    "address locality").fill("Asunción");
            requireOne(addressSection.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Agregar dirección")),
                    "add address").click();
            requireOne(page.getByText("Dirección agregada", new Page.GetByTextOptions().setExact(true)),
                    "address confirmation").waitFor();
            assertResponsive(page, 1280, 900, "business-partners-addresses-expanded-1280.png");

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Contacto")),
                    "contacts tab").click();
            Locator channelSection = requireOne(page.locator("#detail-channel-title")
                    .locator("xpath=ancestor::section[1]"), "general contact channel section");
            Locator channelKind = requireOne(channelSection.getByLabel(
                    "Tipo de canal", new Locator.GetByLabelOptions().setExact(true)),
                    "general contact channel kind");
            channelKind.selectOption(channelKindCode);
            assertEquals(revisedChannelKindName, channelKind.locator("option:checked").innerText(),
                    "the newly registered channel kind must be selectable in the partner detail");
            Locator channelValue = requireOne(channelSection.getByLabel(
                    "Dato de contacto", new Locator.GetByLabelOptions().setExact(true)),
                    "general contact channel");
            channelValue.fill("demo-" + suffix.toLowerCase() + "@example.invalid");
            String submittedKind = channelKind.inputValue();
            String submittedValue = channelValue.inputValue();
            Locator detailTabForm = requireOne(
                    channelSection.locator("xpath=ancestor::form[1]"), "detail tab form");
            String submittedResource = requireOne(
                    detailTabForm.locator("input[name='resource']"), "channel resource").inputValue();
            String submittedVersion = requireOne(
                    detailTabForm.locator("input[name='version']"), "channel version").inputValue();
            requireOne(channelSection.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Agregar canal")),
                    "add general channel").click();
            Locator channelConfirmation = page.getByText(
                    "Canal de contacto agregado", new Page.GetByTextOptions().setExact(true));
            assertEquals(1, channelConfirmation.count(), () ->
                    "channel confirmation was absent; url=" + page.url()
                            + "; submittedKind=" + submittedKind
                            + "; submittedValueLength=" + submittedValue.length()
                            + "; submittedResource=" + submittedResource
                            + "; submittedVersion=" + submittedVersion
                            + "; notices=" + page.locator(".screen-notices, .state-card").allTextContents());
            channelConfirmation.waitFor();
            requireOne(page.getByLabel("Nombre del contacto", new Page.GetByLabelOptions().setExact(true)),
                    "contact name").fill("Contacto Demo " + suffix);
            requireOne(page.getByLabel("Cargo o función", new Page.GetByLabelOptions().setExact(true)),
                    "contact position").fill("Compras");
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agregar contacto")),
                    "add named contact").click();
            requireOne(page.getByText("Contacto agregado", new Page.GetByTextOptions().setExact(true)),
                    "contact confirmation").waitFor();
            assertResponsive(page, 1280, 900, "business-partners-contacts-expanded-1280.png");

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Roles y estado")),
                    "roles tab before inactivation").click();
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Inactivar participante")),
                    "inactivate partner").click();
            requireOne(page.getByText("Socio comercial inactivado", new Page.GetByTextOptions().setExact(true)),
                    "partner inactivation confirmation").waitFor();
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Resumen")),
                    "summary tab").click();
            Locator roleSummary = requireOne(page.locator(".business-summary-grid dd")
                    .filter(new Locator.FilterOptions().setHasText("Cliente")), "commercial role summary");
            assertTrue(roleSummary.innerText().contains("Cliente · Activo"),
                    "the client role must remain active in the combined summary");
            assertTrue(roleSummary.innerText().contains("Proveedor · Activo"),
                    "the supplier role must remain active in the combined summary");
            assertTrue(page.locator(".business-summary-grid").getByText(
                    "Inactivo", new Locator.GetByTextOptions().setExact(true)).count() >= 1,
                    "the participant must remain visible after inactivation");
            assertEquals(0, page.getByText("ID público", new Page.GetByTextOptions().setExact(true)).count(),
                    "technical contract metadata must stay out of the productive screen");

            assertAccessibleStructure(page);
            assertResponsive(page, 1280, 900, "business-partners-detail-expanded-1280.png");
            assertResponsive(page, 720, 900, "business-partners-detail-medium-720.png");
            assertResponsive(page, 375, 900, "business-partners-detail-compact-375.png");
            for (int boundary : List.of(599, 600, 839, 840)) {
                assertResponsiveLayout(page, boundary, "business-partners-boundary-" + boundary);
            }

            verifyReferenceDataPermissionIsEnforcedAndRestore(page, companyId);
            verifyDisabledPluginIsDeniedAndRestore(page, companyId);
        }
    }

    private void exerciseReferenceData(Page page) {
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Datos de referencia")),
                "reference data menu").click();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions()
                        .setName("Datos de referencia")
                        .setExact(true)),
                "reference data heading").waitFor();
        requireOne(page.getByText("248 encontrados", new Page.GetByTextOptions().setExact(true)),
                "complete country publication count").waitFor();
        requireOne(page.getByText(
                "Mostrando 1–50 de 248", new Page.GetByTextOptions().setExact(true)),
                "first country page summary").waitFor();
        assertResponsive(page, 1280, 900, "reference-data-expanded-1280.png");
        assertResponsive(page, 720, 900, "reference-data-medium-720.png");
        assertResponsive(page, 375, 900, "reference-data-compact-375.png");
        page.setViewportSize(1280, 900);

        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Siguiente").setExact(true)),
                "next reference-data page").click();
        requireOne(page.getByText(
                "Mostrando 51–100 de 248", new Page.GetByTextOptions().setExact(true)),
                "second country page summary").waitFor();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Anterior").setExact(true)),
                "previous reference-data page").click();

        Locator catalog = requireOne(page.getByLabel(
                "Catálogo", new Page.GetByLabelOptions().setExact(true)),
                "reference-data catalog filter");
        Locator search = requireOne(page.getByLabel(
                "Buscar referencia", new Page.GetByLabelOptions().setExact(true)),
                "reference-data search filter");
        catalog.selectOption("COUNTRY");
        search.fill("Paraguay");
        clickReferenceSearch(page);
        requireOne(referenceDataTable(page).getByText(
                "PY / PRY", new Locator.GetByTextOptions().setExact(true)),
                "Paraguay country reference").waitFor();

        catalog = requireOne(page.getByLabel(
                "Catálogo", new Page.GetByLabelOptions().setExact(true)),
                "reference-data catalog filter after country search");
        search = requireOne(page.getByLabel(
                "Buscar referencia", new Page.GetByLabelOptions().setExact(true)),
                "reference-data search filter after country search");
        catalog.selectOption("CURRENCY");
        search.fill("");
        clickReferenceSearch(page);
        requireOne(page.getByText("178 encontrados", new Page.GetByTextOptions().setExact(true)),
                "complete currency publication count").waitFor();

        search = requireOne(page.getByLabel(
                "Buscar referencia", new Page.GetByLabelOptions().setExact(true)),
                "reference-data currency search filter");
        search.fill("PYG");
        clickReferenceSearch(page);
        requireOne(referenceDataTable(page).getByText(
                "PYG", new Locator.GetByTextOptions().setExact(true)),
                "Guarani currency reference").waitFor();

        search = requireOne(page.getByLabel(
                "Buscar referencia", new Page.GetByLabelOptions().setExact(true)),
                "reference-data N.A. search filter");
        search.fill("XDR");
        clickReferenceSearch(page);
        requireOne(referenceDataTable(page).getByText(
                "XDR", new Locator.GetByTextOptions().setExact(true)),
                "special drawing rights reference").waitFor();
        requireOne(referenceDataTable(page).getByText(
                "960 · unidad menor N.A.", new Locator.GetByTextOptions().setExact(true)),
                "explicit not-applicable minor unit").waitFor();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Abrir XDR")),
                "open special drawing rights reference").click();

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Estado").setExact(true)),
                "reference-data lifecycle tab").click();
        Locator restoreInterruptedPolicy = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Habilitar referencia").setExact(true));
        if (restoreInterruptedPolicy.count() == 1) {
            restoreInterruptedPolicy.click();
            requireOne(page.getByText(
                    "Referencia habilitada", new Page.GetByTextOptions().setExact(true)),
                    "interrupted reference policy restoration").waitFor();
        }
        requireOne(page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Inhabilitar referencia").setExact(true)),
                "disable reference policy").click();
        requireOne(page.getByText(
                "Referencia inhabilitada", new Page.GetByTextOptions().setExact(true)),
                "reference disabled confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Historial").setExact(true)),
                "reference-data history tab after disable").click();
        assertTrue(referenceDataTable(page).getByText(
                        "Inhabilitada", new Locator.GetByTextOptions().setExact(true)).count() >= 1,
                "append-only history must retain a disabled reference policy revision");
        assertResponsive(page, 1280, 900, "reference-data-policy-history-expanded-1280.png");
        assertResponsive(page, 720, 900, "reference-data-policy-history-medium-720.png");
        assertResponsive(page, 375, 900, "reference-data-policy-history-compact-375.png");
        page.setViewportSize(1280, 900);

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Estado").setExact(true)),
                "reference-data lifecycle tab after history").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Habilitar referencia").setExact(true)),
                "restore reference policy").click();
        requireOne(page.getByText(
                "Referencia habilitada", new Page.GetByTextOptions().setExact(true)),
                "reference restored confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Historial").setExact(true)),
                "reference-data history tab after restore").click();
        assertTrue(page.getByText(
                        "Habilitada", new Page.GetByTextOptions().setExact(true)).count() >= 1,
                "restored reference policy must remain in append-only history");
    }

    private static void clickReferenceSearch(Page page) {
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar").setExact(true)),
                "reference-data search action").click();
    }

    private static Locator referenceDataTable(Page page) {
        return requireOne(page.locator("table.business-table"), "reference-data table");
    }

    private static void selectSearchOption(
            Page page,
            String fieldLabel,
            String query,
            String optionLabel,
            String expectedValue) {
        String searchLabel = "Buscar opciones de " + fieldLabel;
        Locator search = requireOne(page.getByLabel(searchLabel),
                "search-on-demand input for " + fieldLabel);
        search.fill(query);
        Locator field = search.locator(
                "xpath=ancestor::*[contains(concat(' ', normalize-space(@class), ' '),"
                        + " ' selector-search-field ')][1]");
        requireOne(field.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Buscar opciones").setExact(true)),
                "search-on-demand action for " + fieldLabel).click();

        Locator searchAfterQuery = page.getByLabel(searchLabel);
        searchAfterQuery.waitFor();
        search = requireOne(searchAfterQuery,
                "search-on-demand input after search for " + fieldLabel);
        field = search.locator(
                "xpath=ancestor::*[contains(concat(' ', normalize-space(@class), ' '),"
                        + " ' selector-search-field ')][1]");
        requireOne(field.getByRole(
                AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(optionLabel)),
                "search-on-demand option for " + fieldLabel).click();
        Locator searchAfterSelection = page.getByLabel(searchLabel);
        searchAfterSelection.waitFor();
        search = requireOne(searchAfterSelection,
                "search-on-demand input after selection for " + fieldLabel);
        field = search.locator(
                "xpath=ancestor::*[contains(concat(' ', normalize-space(@class), ' '),"
                        + " ' selector-search-field ')][1]");
        Locator currentValue = requireOne(
                field.locator(".selector-current-value strong"),
                "selected search-on-demand value for " + fieldLabel);
        assertEquals(expectedValue, currentValue.innerText().strip(),
                "the search-on-demand selection must update the screen input");
    }

    private static void registerDefinition(
            Page page, String kind, String code, String displayName) {
        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName("Definiciones de socios")),
                "business-partner definitions menu").click();
        requireOne(page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                        .setName("Definiciones de socios")
                        .setExact(true)),
                "business-partner definitions heading").waitFor();
        requireOne(page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName("Nueva definición")),
                "new business-partner definition action").click();
        requireOne(page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Nueva definición")),
                "new business-partner definition heading").waitFor();
        requireOne(page.getByLabel(
                "Clase de definición", new Page.GetByLabelOptions().setExact(true)),
                "business-partner definition class").selectOption(kind);
        requireOne(page.getByLabel(
                "Código", new Page.GetByLabelOptions().setExact(true)),
                "business-partner definition code").fill(code);
        requireOne(page.getByLabel(
                "Nombre", new Page.GetByLabelOptions().setExact(true)),
                "business-partner definition name").fill(displayName);
        requireOne(page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Registrar definición")),
                "register business-partner definition").click();
        requireOne(page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(displayName).setExact(true)),
                "registered business-partner definition detail").waitFor();
    }

    private Page authenticate(BrowserContext context) {
        Page page = context.newPage();
        page.navigate(appUrl);
        requireOne(page.getByLabel("Username"), "Keycloak username").fill(MULTIPLE_COMPANIES_USER);
        requireOne(page.locator("input[type='password']"), "Keycloak password").fill(password);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")),
                "Keycloak sign in").click();
        page.locator("main").waitFor(new Locator.WaitForOptions().setTimeout(30_000));
        return page;
    }

    private String selectFirstAuthorizedCompany(Page page) {
        Locator selector = requireOne(page.getByLabel("Empresa autorizada"), "company selector");
        Locator options = selector.locator("option");
        String firstAuthorizedCompany = null;
        for (int index = 0; index < options.count(); index++) {
            String value = options.nth(index).getAttribute("value");
            if (value != null && !value.isBlank()) {
                firstAuthorizedCompany = value;
                break;
            }
        }
        assertTrue(firstAuthorizedCompany != null, "an authorized company must be available");
        selector.selectOption(new SelectOption().setValue(firstAuthorizedCompany));
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continuar")),
                "continue company selection").click();
        Locator workspaceHeading = page.locator("#modules-title");
        workspaceHeading.waitFor();
        assertEquals("Funciones disponibles", workspaceHeading.innerText(),
                "authorized workspace heading must remain stable after company selection");
        return firstAuthorizedCompany;
    }

    private void enablePluginAndGrantPermissions(Page page, String companyId) {
        String pluginsUrl = adminUrl.replace("index.xhtml", "plugins.xhtml")
                + "?company=" + companyId;
        page.navigate(pluginsUrl);
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Plugins por empresa")),
                "plugins administration heading").waitFor();
        enablePluginCard(page, "reference_data");
        enablePluginCard(page, "business_partners");

        for (String permission : BUSINESS_PARTNER_PERMISSIONS) {
            grantPermissionIfAvailable(page, companyId, permission);
        }

        page.navigate(appUrl);
        Locator selector = page.getByLabel("Empresa autorizada");
        if (selector.count() == 1) {
            selector.selectOption(companyId);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continuar")),
                    "continue selected company").click();
        }
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles")),
                "business partners workspace").waitFor();
    }

    private Locator enablePluginCard(Page page, String pluginId) {
        Locator card = requireOne(page.locator("article.plugin-record-card").filter(
                new Locator.FilterOptions().setHasText(pluginId)),
                pluginId + " plugin card");
        Locator enable = card.getByRole(
                AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Habilitar"));
        if (enable.count() == 1) {
            enable.click();
            card = requireOne(page.locator("article.plugin-record-card").filter(
                    new Locator.FilterOptions().setHasText(pluginId)),
                    "enabled " + pluginId + " plugin card");
        }
        requireOne(card.getByRole(
                AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                pluginId + " enabled state").waitFor();
        return card;
    }

    private void grantPermissionIfAvailable(Page page, String companyId, String permission) {
        String securityUrl = adminUrl.replace("index.xhtml", "security.xhtml")
                + "?company=" + companyId;
        page.navigate(securityUrl);
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Usuarios y acceso por empresa")),
                "security administration heading").waitFor();
        Locator form = requireOne(page.locator("#grant-permission-form"), "grant permission form");
        Locator availablePermission = requireOne(form.getByLabel(
                "Permiso disponible", new Locator.GetByLabelOptions().setExact(true)),
                "available permission");
        if (availablePermission.locator("option[value='" + permission + "']").count() == 0) {
            return;
        }
        Locator role = requireOne(form.getByLabel(
                "Rol empresarial", new Locator.GetByLabelOptions().setExact(true)),
                "grant role");
        role.selectOption(optionValueContaining(role, DEMO_ROLE_CODE));
        availablePermission.selectOption(permission);
        requireOne(form.getByRole(
                AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Conceder permiso")),
                "grant permission action").click();
        requireOne(page.locator(".admin-message"), "permission grant result").waitFor();
    }

    private void verifyReferenceDataPermissionIsEnforcedAndRestore(
            Page page, String companyId) {
        String permission = "reference_data.policy.manage";
        String securityUrl = adminUrl.replace("index.xhtml", "security.xhtml")
                + "?company=" + companyId;
        String referenceDataUrl = appUrl.replace(
                "index.xhtml", "view.xhtml?route=%2Freference-data&mode=directory");
        boolean revoked = false;
        try {
            page.navigate(securityUrl);
            Locator roleCode = page.locator(".record-card-heading .eyebrow").filter(
                    new Locator.FilterOptions().setHasText(DEMO_ROLE_CODE));
            Locator roleCard = requireOne(page.locator("article.admin-record-card").filter(
                    new Locator.FilterOptions().setHas(roleCode)),
                    "demo operator role card");
            Locator relation = requireOne(roleCard.locator(".relation-row").filter(
                    new Locator.FilterOptions().setHasText(permission)),
                    "reference-data policy permission relation");
            page.onceDialog(dialog -> dialog.accept());
            requireOne(relation.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Revocar")),
                    "revoke reference-data policy permission").click();
            revoked = true;
            requireOne(page.locator(".admin-message"),
                    "reference-data permission revocation result").waitFor();

            page.navigate(appUrl);
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles")),
                    "workspace after reference-data permission revocation").waitFor();
            assertEquals(0, page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Datos de referencia")).count(),
                    "reference-data menu must disappear without policy permission");

            page.navigate(referenceDataUrl);
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName(
                            "Esta función no está disponible para tu contexto actual")),
                    "reference-data direct route permission denial").waitFor();
            assertResponsive(page, 375, 900,
                    "reference-data-permission-denial-compact-375.png");
        } finally {
            if (revoked) {
                grantPermissionIfAvailable(page, companyId, permission);
            }
        }
    }

    private void verifyDisabledPluginIsDeniedAndRestore(Page page, String companyId) {
        String pluginsUrl = adminUrl.replace("index.xhtml", "plugins.xhtml") + "?company=" + companyId;
        String businessPartnersUrl = appUrl.replace(
                "index.xhtml", "view.xhtml?route=%2Fbusiness-partners&mode=directory");
        boolean disabled = false;
        try {
            page.setViewportSize(1280, 900);
            page.navigate(pluginsUrl);
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Plugins por empresa")),
                    "plugins administration heading").waitFor();
            Locator card = requireOne(page.locator("article.plugin-record-card").filter(
                    new Locator.FilterOptions().setHasText("business_partners")),
                    "business partners plugin card");
            assertEquals(companyId, requireOne(
                    card.locator("input[name='company']"), "plugin company candidate").inputValue());
            assertEquals("business_partners", requireOne(
                    card.locator("input[name='plugin']"), "plugin id candidate").inputValue());
            assertTrue(Long.parseLong(requireOne(
                    card.locator("input[name='decisionVersion']"),
                    "plugin version candidate").inputValue()) >= 0);
            page.onceDialog(dialog -> dialog.accept());
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    "disable business partners").click();
            card = requireOne(page.locator("article.plugin-record-card").filter(
                    new Locator.FilterOptions().setHasText("business_partners")),
                    "disabled business partners plugin card after action");
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Habilitar")),
                    "disabled business partners state").waitFor();
            disabled = true;
            requireOne(page.locator(".admin-message").filter(new Locator.FilterOptions().setHasText(
                    "El plugin quedó deshabilitado; sus datos fueron conservados.")),
                    "plugin disabled confirmation").waitFor();

            page.navigate(businessPartnersUrl);
            requireOne(page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName(
                            "Esta función no está disponible para tu contexto actual")),
                    "disabled plugin denial").waitFor();
            assertResponsive(page, 375, 900, "business-partners-disabled-denial-compact-375.png");

            page.navigate(pluginsUrl);
            card = requireOne(page.locator("article.plugin-record-card").filter(
                    new Locator.FilterOptions().setHasText("business_partners")),
                    "disabled business partners plugin card");
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Habilitar")),
                    "restore business partners").click();
            Locator restoredMessage = page.locator(".admin-message").filter(
                    new Locator.FilterOptions().setHasText(
                            "El plugin quedó habilitado para la empresa."));
            restoredMessage.waitFor();
            requireOne(restoredMessage, "plugin restored confirmation");
            card = requireOne(page.locator("article.plugin-record-card").filter(
                    new Locator.FilterOptions().setHasText("business_partners")),
                    "restored business partners plugin card");
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    "restored business partners state").waitFor();
            disabled = false;

            page.navigate(businessPartnersUrl);
            requireOne(page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("Socios comerciales").setExact(true)),
                    "business partners restored").waitFor();
        } finally {
            if (disabled) {
                page.navigate(pluginsUrl);
                Locator card = page.locator("article.plugin-record-card").filter(
                        new Locator.FilterOptions().setHasText("business_partners"));
                Locator enable = card.getByRole(
                        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Habilitar"));
                if (enable.count() == 1) {
                    enable.click();
                }
            }
        }
    }

    private void assertResponsive(Page page, int width, int height, String screenshotName) {
        page.setViewportSize(width, height);
        assertResponsiveLayout(page, width, screenshotName);
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(evidenceDirectory.resolve(screenshotName))
                .setFullPage(true));
    }

    private void assertResponsiveLayout(Page page, int width, String description) {
        page.setViewportSize(width, 900);
        boolean stylesLoaded = (Boolean) page.evaluate("""
                () => Array.from(document.styleSheets)
                  .filter(sheet => sheet.href
                    && sheet.href.includes('/faces/jakarta.faces.resource/'))
                  .every(sheet => sheet.cssRules.length > 0)
                """);
        int documentWidth = ((Number) page.evaluate(
                "() => document.documentElement.scrollWidth")).intValue();
        assertTrue(stylesLoaded,
                description + " must load the application Material Design stylesheets");
        assertTrue(documentWidth <= width + 1,
                description + " has horizontal overflow: document="
                        + documentWidth + " px, viewport=" + width + " px");
    }

    private void assertAccessibleStructure(Page page) {
        assertEquals(1, page.locator("main h1").count(),
                "the business screen must expose exactly one main heading");
        boolean everyEditableControlHasLabel = (Boolean) page.evaluate("""
                () => Array.from(document.querySelectorAll(
                    "input:not([type='hidden']):not([type='submit']), select, textarea"))
                  .every(control => control.labels && control.labels.length > 0)
                """);
        assertTrue(everyEditableControlHasLabel,
                "every editable business-partner control must have a label");
    }

    private static String optionValueContaining(Locator select, String expectedText) {
        Locator options = select.locator("option");
        int count = options.count();
        for (int index = 0; index < count; index++) {
            Locator option = options.nth(index);
            String text = option.innerText();
            String value = option.getAttribute("value");
            if (text.contains(expectedText) && value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new AssertionError("select must expose an option containing " + expectedText);
    }

    private static Locator requireOne(Locator locator, String description) {
        int count = locator.count();
        assertEquals(1, count, description + " must resolve exactly one element");
        return locator;
    }

    private static String readSecret(String pathValue) throws IOException {
        String value = Files.readString(Path.of(pathValue), StandardCharsets.UTF_8).strip();
        if (value.isEmpty() || value.length() > 4096
                || value.codePoints().anyMatch(Character::isISOControl)) {
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
}
