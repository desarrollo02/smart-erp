package py.com.logixone.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
@EnabledIfSystemProperty(named = "logixone.commercial-catalog.e2e", matches = "true")
class CommercialCatalogVisualIT {

    private static final String MULTIPLE_COMPANIES_USER = "demo.empresas.ab";
    private static final String DEMO_ROLE_CODE = "demo_operator";
    private static final String BUSINESS_PARTNERS_VIEW = "business_partners.view";
    private static final List<String> CATALOG_PERMISSIONS = List.of(
            "commercial_catalog.view",
            "commercial_catalog.items.manage",
            "commercial_catalog.prices.manage",
            "commercial_catalog.definitions.manage");

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
    void composesCatalogMenusAndExercisesItemPriceAndSecurityJourneys() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setLocale("es-PY")
                .setViewportSize(1280, 900))) {
            Page page = authenticate(context);
            String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            verifyNativeSelectorReturn(page, suffix);
            String companyId = selectFirstAuthorizedCompany(page);
            enableRequiredPlugins(page, companyId);
            grantCatalogPermissions(page, companyId);
            grantCompanyPermission(page, companyId, BUSINESS_PARTNERS_VIEW);
            openAuthorizedWorkspace(page, companyId);

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Socios comerciales")),
                    "business partners menu merged with catalog");
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Perfiles tributarios")),
                    "tax profiles menu available to definitions manager");
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Definiciones del catálogo")),
                    "catalog definitions menu available to definitions manager");
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Familias de variantes")),
                    "variant families menu available to definitions manager");

            String unitCode = createCatalogUnit(page, suffix);
            createCatalogTag(page, suffix);
            String categoryName = createSimpleCatalogDefinition(
                    page, suffix, "CATEGORY", "C", "Categoría visual ");
            String brandName = createSimpleCatalogDefinition(
                    page, suffix, "BRAND", "B", "Marca visual ");
            String variantFamilyName = createVariantFamily(page, suffix);
            String taxProfileName = createTaxProfile(page, suffix);

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Artículos y servicios")),
                    "catalog items menu").click();
            requireMainHeading(page, "Artículos y servicios");
            assertResponsive(page, 1280, 900, "catalog-items-directory-expanded-1280.png");
            assertResponsive(page, 720, 900, "catalog-items-directory-medium-720.png");
            assertResponsive(page, 375, 900, "catalog-items-directory-compact-375.png");

            String itemCode = "CAT-" + suffix;
            String itemName = "Producto visual " + suffix;
            page.setViewportSize(1280, 900);
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo artículo o servicio")),
                    "new catalog item action").click();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Nuevo artículo o servicio")),
                    "new catalog item heading").waitFor();
            requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                    "new item code").fill(itemCode);
            requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                    "new item name").fill(itemName);
            requireOne(page.getByLabel("Descripción", new Page.GetByLabelOptions().setExact(true)),
                    "new item description").fill("Producto ficticio para la demostración visual");
            requireOne(page.getByLabel("Tipo", new Page.GetByLabelOptions().setExact(true)),
                    "new item type").selectOption("PRODUCT");
            requireOne(page.getByLabel("Alcance", new Page.GetByLabelOptions().setExact(true)),
                    "new item scope").selectOption("BOTH");
            verifySelectorAdministrationReturn(page, suffix, itemCode, itemName, unitCode);
            requireOne(page.getByLabel("Unidad base", new Page.GetByLabelOptions().setExact(true)),
                    "new item unit").selectOption(unitCode);
            requireOne(page.getByLabel("Perfil tributario", new Page.GetByLabelOptions().setExact(true)),
                    "new item tax profile").selectOption(
                            new SelectOption().setLabel(taxProfileName));
            assertResponsive(page, 375, 900, "catalog-item-create-compact-375.png");
            page.setViewportSize(1280, 900);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar")),
                    "register catalog item").click();
            requireOne(page.getByText(
                    "Artículo o servicio registrado", new Page.GetByTextOptions().setExact(true)),
                    "catalog item confirmation").waitFor();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName(itemName)),
                    "catalog item detail").waitFor();

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Identificadores")),
                    "catalog item identifiers tab").click();
            requireOne(page.getByLabel(
                    "Tipo de identificador", new Page.GetByLabelOptions().setExact(true)),
                    "identifier type").fill("SKU_DEMO");
            requireOne(page.getByLabel("Valor", new Page.GetByLabelOptions().setExact(true)),
                    "identifier value").fill("ALT-" + suffix);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agregar identificador")),
                    "add catalog identifier").click();
            requireOne(page.getByText("Identificador agregado", new Page.GetByTextOptions().setExact(true)),
                    "catalog identifier confirmation").waitFor();

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Clasificación")),
                    "catalog classification tab").click();
            requireOne(page.getByLabel(
                    "Categoría principal", new Page.GetByLabelOptions().setExact(true)),
                    "catalog category").selectOption(
                            new SelectOption().setLabel(categoryName));
            requireOne(page.getByLabel("Marca", new Page.GetByLabelOptions().setExact(true)),
                    "catalog brand").selectOption(new SelectOption().setLabel(brandName));
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Guardar clasificación")),
                    "save catalog classification").click();
            requireOne(page.getByText("Clasificación actualizada", new Page.GetByTextOptions().setExact(true)),
                    "catalog classification confirmation").waitFor();

            requireOne(page.getByRole(
                    AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("Variantes").setExact(true)),
                    "catalog variants tab").click();
            Locator variantFamily = requireOne(page.getByLabel(
                    "Familia de variantes", new Page.GetByLabelOptions().setExact(true)),
                    "active variant family selector");
            variantFamily.selectOption(optionValueContaining(variantFamily, variantFamilyName));
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Mostrar atributos")),
                    "prepare variant structure").click();
            requireOne(page.getByText(
                    "NUMERO (Número, número, obligatorio)",
                    new Page.GetByTextOptions().setExact(true)),
                    "current variant family structure").waitFor();
            requireOne(page.getByLabel("Valores", new Page.GetByLabelOptions().setExact(true)),
                    "variant values").fill("NUMERO=42.00");
            assertResponsive(page, 1280, 900, "catalog-item-variant-expanded-1280.png");
            assertResponsive(page, 720, 900, "catalog-item-variant-medium-720.png");
            assertResponsive(page, 375, 900, "catalog-item-variant-compact-375.png");
            page.setViewportSize(1280, 900);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Asignar variante")),
                    "assign item variant").click();
            requireOne(page.getByText(
                    "Familia y valores de variante asignados",
                    new Page.GetByTextOptions().setExact(true)),
                    "variant assignment confirmation").waitFor();
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Resumen")),
                    "catalog summary after variant assignment").click();
            requireOne(page.locator(".business-summary-grid").getByText(
                    "NUMERO=42", new Locator.GetByTextOptions().setExact(false)),
                    "normalized assigned variant value").waitFor();
            assertResponsive(page, 1280, 900, "catalog-item-detail-expanded-1280.png");
            assertResponsive(page, 720, 900, "catalog-item-detail-medium-720.png");
            assertResponsive(page, 375, 900, "catalog-item-detail-compact-375.png");

            page.setViewportSize(1280, 900);
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Listas de precios")),
                    "price lists menu").click();
            requireMainHeading(page, "Listas de precios");
            assertResponsive(page, 375, 900, "price-lists-directory-compact-375.png");
            page.setViewportSize(1280, 900);
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva lista")),
                    "new price list action").click();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Nueva lista de precios")),
                    "new price list heading").waitFor();

            String priceCode = "PL-" + suffix;
            String priceName = "Lista visual " + suffix;
            requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                    "new price list code").fill(priceCode);
            requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                    "new price list name").fill(priceName);
            requireOne(page.getByLabel("Moneda", new Page.GetByLabelOptions().setExact(true)),
                    "price list currency").selectOption("PYG");
            requireOne(page.getByLabel("Impuestos", new Page.GetByLabelOptions().setExact(true)),
                    "price list tax mode").selectOption("TAX_INCLUDED");
            requireOne(page.getByLabel("Decimales", new Page.GetByLabelOptions().setExact(true)),
                    "price list scale").selectOption("0");
            requireOne(page.getByLabel("Redondeo", new Page.GetByLabelOptions().setExact(true)),
                    "price list rounding").selectOption("HALF_UP");
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar lista")),
                    "register price list").click();
            requireOne(page.getByText("Lista de precios registrada", new Page.GetByTextOptions().setExact(true)),
                    "price list confirmation").waitFor();
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName(priceName)),
                    "price list detail").waitFor();

            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Precios").setExact(true)),
                    "price entries tab").click();
            Locator priceItemField = page.getByLabel(
                    "Artículo o servicio", new Page.GetByLabelOptions().setExact(true));
            priceItemField.waitFor();
            Locator priceItem = requireOne(priceItemField, "price item");
            priceItem.selectOption(optionValueContaining(priceItem, itemName));
            requireOne(page.getByLabel("Unidad", new Page.GetByLabelOptions().setExact(true)),
                    "price unit").selectOption(unitCode);
            requireOne(page.getByLabel("Cantidad mínima", new Page.GetByLabelOptions().setExact(true)),
                    "price minimum").fill("1");
            requireOne(page.getByLabel("Importe", new Page.GetByLabelOptions().setExact(true)),
                    "price amount").fill("125000");
            requireOne(page.getByLabel("Vigente desde", new Page.GetByLabelOptions().setExact(true)),
                    "price validity start").fill("2026-07-31T12:00:00Z");
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agregar precio")),
                    "add price entry").click();
            requireOne(page.getByText("Precio agregado", new Page.GetByTextOptions().setExact(true)),
                    "price entry confirmation").waitFor();
            assertAccessibleStructure(page);
            assertResponsive(page, 1280, 900, "price-list-detail-expanded-1280.png");
            assertResponsive(page, 720, 900, "price-list-detail-medium-720.png");
            assertResponsive(page, 375, 900, "price-list-detail-compact-375.png");
            for (int boundary : List.of(599, 600, 839, 840)) {
                assertResponsiveLayout(page, boundary, "commercial-catalog-boundary-" + boundary);
            }

            verifyCatalogUnitReplacement(page, unitCode, suffix, itemName);
            verifyTaxProfileLifecycle(page, taxProfileName);
            verifyTaxProfilePermissionIsEnforcedAndRestore(page, companyId);
            verifyDisabledCatalogIsDeniedAndRestore(page, companyId);
        }
    }

    private void verifyCatalogUnitReplacement(
            Page page, String unitCode, String suffix, String itemName) {
        String unitName = "Unidad visual " + suffix;
        String revisedUnitName = "Unidad revisada " + suffix;
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Definiciones del catálogo")),
                "catalog definitions menu for unit lifecycle").click();
        requireMainHeading(page, "Definiciones del catálogo");
        requireOne(page.getByLabel(
                "Código o nombre", new Page.GetByLabelOptions().setExact(true)),
                "catalog definition lifecycle search").fill(unitCode);
        requireOne(page.getByLabel("Tipo", new Page.GetByLabelOptions().setExact(true)),
                "catalog definition lifecycle type").selectOption("UNIT");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar")),
                "search unit for lifecycle").click();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Abrir " + unitCode)),
                "open unit lifecycle detail").click();
        requireMainHeading(page, unitName);

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva revisión")),
                "unit revision tab").click();
        requireOne(page.getByLabel(
                "Nombre revisado", new Page.GetByLabelOptions().setExact(true)),
                "revised unit name").fill(revisedUnitName);
        requireOne(page.getByLabel(
                "Decimales (sólo unidades)", new Page.GetByLabelOptions().setExact(true)),
                "revised unit scale").selectOption("3");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Crear revisión")),
                "create unit revision").click();
        requireOne(page.getByText(
                "Revisión creada", new Page.GetByTextOptions().setExact(true)),
                "unit revision confirmation").waitFor();
        requireMainHeading(page, revisedUnitName);
        assertResponsive(page, 1280, 900, "catalog-unit-revision-expanded-1280.png");
        assertResponsive(page, 720, 900, "catalog-unit-revision-medium-720.png");
        assertResponsive(page, 375, 900, "catalog-unit-revision-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName("Historial").setExact(true)),
                "unit history tab").click();
        requireOne(page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Historial de revisiones").setExact(true)),
                "unit history heading").waitFor();
        Locator historyRows = page.locator(".revision-history-table tbody tr");
        assertEquals(2, historyRows.count(),
                "unit history must preserve the original and current revisions");
        assertTrue(historyRows.nth(0).innerText().contains(revisedUnitName));
        assertTrue(historyRows.nth(1).innerText().contains(unitName));
        assertResponsive(page, 1280, 900, "catalog-unit-history-expanded-1280.png");
        assertResponsive(page, 720, 900, "catalog-unit-history-medium-720.png");
        assertResponsive(page, 375, 900, "catalog-unit-history-compact-375.png");

        String replacementCode = "N" + suffix.substring(0, 7);
        String replacementName = "Unidad sucesora " + suffix;
        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Reemplazar")),
                "unit replacement tab").click();
        requireOne(page.getByLabel(
                "Nuevo código", new Page.GetByLabelOptions().setExact(true)),
                "replacement unit code").fill(replacementCode);
        requireOne(page.getByLabel(
                "Nombre de la sucesora", new Page.GetByLabelOptions().setExact(true)),
                "replacement unit name").fill(replacementName);
        requireOne(page.getByLabel(
                "Decimales de la sucesora (sólo unidades)",
                new Page.GetByLabelOptions().setExact(true)),
                "replacement unit scale").selectOption("3");
        assertResponsive(page, 1280, 900, "catalog-unit-replacement-expanded-1280.png");
        assertResponsive(page, 720, 900, "catalog-unit-replacement-medium-720.png");
        assertResponsive(page, 375, 900, "catalog-unit-replacement-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Reemplazar definición")),
                "replace unit").click();
        requireOne(page.getByText(
                "Definición reemplazada", new Page.GetByTextOptions().setExact(true)),
                "unit replacement confirmation").waitFor();
        requireMainHeading(page, replacementName);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Resumen")),
                "replacement unit summary").click();
        requireOne(page.locator(".business-summary-grid").getByText(
                replacementCode, new Locator.GetByTextOptions().setExact(true)),
                "replacement unit identity").waitFor();
        assertResponsive(page, 1280, 900, "catalog-unit-successor-expanded-1280.png");
        assertResponsive(page, 720, 900, "catalog-unit-successor-medium-720.png");
        assertResponsive(page, 375, 900, "catalog-unit-successor-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Definiciones del catálogo")),
                "catalog definitions menu after unit replacement").click();
        requireOne(page.getByLabel(
                "Código o nombre", new Page.GetByLabelOptions().setExact(true)),
                "replaced catalog definition search").fill(unitCode);
        requireOne(page.getByLabel("Estado", new Page.GetByLabelOptions().setExact(true)),
                "replaced catalog definition state").selectOption("INACTIVE");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar")),
                "search replaced unit").click();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Abrir " + unitCode)),
                "open replaced unit").click();
        requireMainHeading(page, revisedUnitName);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Resumen")),
                "replaced unit summary").click();
        requireOne(page.locator(".business-summary-grid").getByText(
                replacementCode, new Locator.GetByTextOptions().setExact(false)),
                "replacement link from historical unit").waitFor();
        assertResponsive(page, 1280, 900, "catalog-unit-replaced-link-expanded-1280.png");
        assertResponsive(page, 720, 900, "catalog-unit-replaced-link-medium-720.png");
        assertResponsive(page, 375, 900, "catalog-unit-replaced-link-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Artículos y servicios")),
                "catalog items after unit replacement").click();
        requireOne(page.getByLabel(
                "Nombre, código o identificador", new Page.GetByLabelOptions().setExact(true)),
                "catalog item search after unit replacement").fill(itemName);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar")),
                "search item after unit replacement").click();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Abrir " + itemName)),
                "open item after unit replacement").click();
        requireOne(page.locator(".business-summary-grid").getByText(
                unitCode, new Locator.GetByTextOptions().setExact(true)),
                "item keeps historical unit identity").waitFor();
        assertResponsive(page, 1280, 900, "catalog-item-preserved-unit-expanded-1280.png");
        assertResponsive(page, 720, 900, "catalog-item-preserved-unit-medium-720.png");
        assertResponsive(page, 375, 900, "catalog-item-preserved-unit-compact-375.png");
        assertAccessibleStructure(page);
    }

    private void verifyTaxProfileLifecycle(Page page, String taxProfileName) {
        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Perfiles tributarios")),
                "tax profiles menu for lifecycle").click();
        requireMainHeading(page, "Perfiles tributarios");
        requireOne(page.getByLabel(
                "Código, nombre o tratamiento", new Page.GetByLabelOptions().setExact(true)),
                "tax profile lifecycle search").fill(taxProfileName);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar")),
                "search tax profile for lifecycle").click();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Abrir " + taxProfileName)),
                "open tax profile lifecycle detail").click();
        requireMainHeading(page, taxProfileName);

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva revisión")),
                "tax profile revision tab").click();
        requireOne(page.getByLabel(
                "Tratamiento interno", new Page.GetByLabelOptions().setExact(true)),
                "revised tax profile treatment").fill("TAXED_DEMO_REVISED");
        requireOne(page.getByLabel(
                "Descripción", new Page.GetByLabelOptions().setExact(true)),
                "revised tax profile description").fill(
                        "Revisión ficticia del tratamiento interno; no es una regla SIFEN");
        requireOne(page.getByLabel(
                "Vigente desde", new Page.GetByLabelOptions().setExact(true)),
                "revised tax profile validity start").fill("2026-09-01T00:00:00Z");
        requireOne(page.getByLabel(
                "Vigente hasta", new Page.GetByLabelOptions().setExact(true)),
                "revised tax profile validity end").fill("2026-12-31T23:59:59Z");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Crear revisión")),
                "create tax profile revision").click();
        requireOne(page.getByText(
                "Revisión tributaria creada", new Page.GetByTextOptions().setExact(true)),
                "tax profile revision confirmation").waitFor();
        assertResponsive(page, 1280, 900, "tax-profile-revision-expanded-1280.png");
        assertResponsive(page, 720, 900, "tax-profile-revision-medium-720.png");
        assertResponsive(page, 375, 900, "tax-profile-revision-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Historial").setExact(true)),
                "tax profile history tab").click();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions()
                        .setName("Historial de revisiones").setExact(true)),
                "tax profile history heading").waitFor();
        Locator historyRows = page.locator(".revision-history-table tbody tr");
        assertEquals(2, historyRows.count());
        assertTrue(historyRows.nth(0).innerText().contains("TAXED_DEMO_REVISED"));
        assertTrue(historyRows.nth(0).innerText().contains("Actual"));
        assertTrue(historyRows.nth(1).innerText().contains("TAXED_DEMO"));
        assertTrue(historyRows.nth(1).innerText().contains("Histórica"));
        assertResponsive(page, 1280, 900, "tax-profile-history-expanded-1280.png");
        assertResponsive(page, 720, 900, "tax-profile-history-medium-720.png");
        assertResponsive(page, 375, 900, "tax-profile-history-compact-375.png");

        page.setViewportSize(1280, 900);

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Estado")),
                "tax profile lifecycle tab").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Inactivar")),
                "inactivate tax profile").click();
        requireOne(page.getByText(
                "Perfil tributario inactivado", new Page.GetByTextOptions().setExact(true)),
                "tax profile inactivation confirmation").waitFor();
        assertResponsive(page, 1280, 900, "tax-profile-inactive-expanded-1280.png");
        assertResponsive(page, 720, 900, "tax-profile-inactive-medium-720.png");
        assertResponsive(page, 375, 900, "tax-profile-inactive-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Perfiles tributarios")),
                "tax profiles menu after inactivation").click();
        requireOne(page.getByLabel(
                "Código, nombre o tratamiento", new Page.GetByLabelOptions().setExact(true)),
                "inactive tax profile search").fill(taxProfileName);
        requireOne(page.getByLabel("Estado", new Page.GetByLabelOptions().setExact(true)),
                "inactive tax profile state").selectOption("INACTIVE");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar")),
                "search inactive tax profile").click();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Abrir " + taxProfileName)),
                "open inactive tax profile").click();
        requireMainHeading(page, taxProfileName);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Estado")),
                "inactive tax profile lifecycle tab").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reactivar")),
                "reactivate tax profile").click();
        requireOne(page.getByText(
                "Perfil tributario reactivado", new Page.GetByTextOptions().setExact(true)),
                "tax profile reactivation confirmation").waitFor();
        assertAccessibleStructure(page);
    }

    private String createCatalogUnit(Page page, String suffix) {
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Definiciones del catálogo")),
                "catalog definitions administration menu").click();
        requireMainHeading(page, "Definiciones del catálogo");
        assertResponsive(page, 1280, 900, "catalog-definitions-directory-expanded-1280.png");
        assertResponsive(page, 720, 900, "catalog-definitions-directory-medium-720.png");
        assertResponsive(page, 375, 900, "catalog-definitions-directory-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva definición")),
                "new catalog definition action").click();
        requireMainHeading(page, "Nueva definición");

        String code = "U" + suffix.substring(0, 7);
        String name = "Unidad visual " + suffix;
        requireOne(page.getByLabel(
                "Tipo de definición", new Page.GetByLabelOptions().setExact(true)),
                "definition kind").selectOption("UNIT");
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "definition code").fill(code);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "definition name").fill(name);
        requireOne(page.getByLabel(
                "Decimales (sólo unidades)", new Page.GetByLabelOptions().setExact(true)),
                "unit scale").selectOption("2");
        assertResponsive(page, 375, 900, "catalog-definition-create-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar definición")),
                "register catalog definition").click();
        requireOne(page.getByText(
                "Definición registrada", new Page.GetByTextOptions().setExact(true)),
                "catalog definition confirmation").waitFor();
        requireMainHeading(page, name);
        assertAccessibleStructure(page);
        return code;
    }

    private void verifySelectorAdministrationReturn(
            Page page,
            String suffix,
            String itemCode,
            String itemName,
            String originalUnitCode) {
        String description = "Producto ficticio para la demostración visual";
        requireOne(page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName(
                        "Agregar o administrar opciones de Unidad base").setExact(true)),
                "contextual unit administration action").click();
        requireMainHeading(page, "Definiciones del catálogo");
        requireOne(page.getByText(
                "Administración contextual", new Page.GetByTextOptions().setExact(true)),
                "selector return banner").waitFor();
        assertResponsive(page, 1280, 900, "selector-return-manager-expanded-1280.png");
        assertResponsive(page, 720, 900, "selector-return-manager-medium-720.png");
        assertResponsive(page, 375, 900, "selector-return-manager-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva definición")),
                "new definition from selector context").click();
        requireMainHeading(page, "Nueva definición");

        String refreshedUnitCode = "R" + suffix.substring(0, 7);
        String refreshedUnitName = "Unidad de retorno " + suffix;
        requireOne(page.getByLabel(
                "Tipo de definición", new Page.GetByLabelOptions().setExact(true)),
                "context definition kind").selectOption("UNIT");
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "context definition code").fill(refreshedUnitCode);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "context definition name").fill(refreshedUnitName);
        requireOne(page.getByLabel(
                "Decimales (sólo unidades)", new Page.GetByLabelOptions().setExact(true)),
                "context unit scale").selectOption("2");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar definición")),
                "register contextual unit").click();
        requireOne(page.getByText(
                "Definición registrada", new Page.GetByTextOptions().setExact(true)),
                "contextual unit confirmation").waitFor();

        requireOne(page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName(
                        "Volver a Artículos y servicios").setExact(true)),
                "return to catalog item draft").click();
        requireOne(page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Nuevo artículo o servicio")),
                "restored item create heading").waitFor();
        requireOne(page.getByText(
                "Opciones actualizadas", new Page.GetByTextOptions().setExact(true)),
                "selector options refreshed notice").waitFor();

        assertEquals(itemCode, requireOne(page.getByLabel(
                "Código", new Page.GetByLabelOptions().setExact(true)),
                "restored item code").inputValue());
        assertEquals(itemName, requireOne(page.getByLabel(
                "Nombre", new Page.GetByLabelOptions().setExact(true)),
                "restored item name").inputValue());
        assertEquals(description, requireOne(page.getByLabel(
                "Descripción", new Page.GetByLabelOptions().setExact(true)),
                "restored item description").inputValue());

        Locator unit = requireOne(page.getByLabel(
                "Unidad base", new Page.GetByLabelOptions().setExact(true)),
                "refreshed item unit selector");
        assertEquals(1, unit.locator("option[value='" + refreshedUnitCode + "']").count(),
                "new unit must be available after contextual return");
        unit.selectOption(refreshedUnitCode);
        assertEquals(refreshedUnitCode, unit.inputValue());
        assertResponsive(page, 375, 900, "selector-return-restored-compact-375.png");
        page.setViewportSize(1280, 900);
        unit.selectOption(originalUnitCode);
    }

    private void createCatalogTag(Page page, String suffix) {
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Definiciones del catálogo")),
                "catalog definitions administration menu for tags").click();
        requireMainHeading(page, "Definiciones del catálogo");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva definición")),
                "new tag definition action").click();
        requireMainHeading(page, "Nueva definición");

        String code = "T" + suffix.substring(0, 7);
        String name = "Etiqueta visual " + suffix;
        requireOne(page.getByLabel(
                "Tipo de definición", new Page.GetByLabelOptions().setExact(true)),
                "tag definition kind").selectOption("TAG");
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "tag definition code").fill(code);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "tag definition name").fill(name);
        assertResponsive(page, 375, 900, "catalog-tag-create-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar definición")),
                "register tag definition").click();
        requireOne(page.getByText(
                "Definición registrada", new Page.GetByTextOptions().setExact(true)),
                "tag definition confirmation").waitFor();
        requireMainHeading(page, name);
        assertAccessibleStructure(page);

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Definiciones del catálogo")),
                "catalog definitions menu after tag creation").click();
        requireMainHeading(page, "Definiciones del catálogo");
        requireOne(page.getByLabel("Tipo", new Page.GetByLabelOptions().setExact(true)),
                "catalog definition type filter").selectOption("TAG");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar")),
                "filter tag definitions").click();
        requireOne(page.locator("table.business-table").getByText(
                name, new Locator.GetByTextOptions().setExact(true)),
                "created tag in filtered definitions").waitFor();
        assertResponsive(page, 375, 900, "catalog-tags-filtered-compact-375.png");
        page.setViewportSize(1280, 900);
    }

    private String createSimpleCatalogDefinition(
            Page page,
            String suffix,
            String kind,
            String codePrefix,
            String namePrefix) {
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Definiciones del catálogo")),
                "catalog definitions administration menu for " + kind).click();
        requireMainHeading(page, "Definiciones del catálogo");
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva definición")),
                "new " + kind + " definition action").click();
        requireMainHeading(page, "Nueva definición");

        String code = codePrefix + suffix.substring(0, 7);
        String name = namePrefix + suffix;
        requireOne(page.getByLabel(
                "Tipo de definición", new Page.GetByLabelOptions().setExact(true)),
                kind + " definition kind").selectOption(kind);
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                kind + " definition code").fill(code);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                kind + " definition name").fill(name);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar definición")),
                "register " + kind + " definition").click();
        requireOne(page.getByText(
                "Definición registrada", new Page.GetByTextOptions().setExact(true)),
                kind + " definition confirmation").waitFor();
        requireMainHeading(page, name);
        return name;
    }

    private String createTaxProfile(Page page, String suffix) {
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Perfiles tributarios")),
                "tax profile administration menu").click();
        requireMainHeading(page, "Perfiles tributarios");
        assertResponsive(page, 1280, 900, "tax-profiles-directory-expanded-1280.png");
        assertResponsive(page, 720, 900, "tax-profiles-directory-medium-720.png");
        assertResponsive(page, 375, 900, "tax-profiles-directory-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo perfil")),
                "new tax profile action").click();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Nuevo perfil tributario")),
                "new tax profile heading").waitFor();

        String name = "Perfil visual " + suffix;
        requireOne(page.getByLabel(
                "Código interno", new Page.GetByLabelOptions().setExact(true)),
                "tax profile code").fill("VISUAL_" + suffix);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "tax profile name").fill(name);
        requireOne(page.getByLabel(
                "Tratamiento interno", new Page.GetByLabelOptions().setExact(true)),
                "tax profile internal treatment").fill("TAXED_DEMO");
        requireOne(page.getByLabel("Descripción", new Page.GetByLabelOptions().setExact(true)),
                "tax profile description").fill(
                        "Perfil ficticio creado desde la administración visual; no es una regla SIFEN");
        requireOne(page.getByLabel(
                "Vigente desde", new Page.GetByLabelOptions().setExact(true)),
                "tax profile validity start").fill("2026-08-01T00:00:00Z");
        assertResponsive(page, 375, 900, "tax-profile-create-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar perfil")),
                "register tax profile").click();
        requireOne(page.getByText(
                "Perfil tributario registrado", new Page.GetByTextOptions().setExact(true)),
                "tax profile confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName(name)),
                "created tax profile detail").waitFor();
        assertAccessibleStructure(page);
        return name;
    }

    private String createVariantFamily(Page page, String suffix) {
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Familias de variantes")),
                "variant family administration menu").click();
        requireMainHeading(page, "Familias de variantes");
        assertResponsive(page, 1280, 900, "variant-families-directory-expanded-1280.png");
        assertResponsive(page, 720, 900, "variant-families-directory-medium-720.png");
        assertResponsive(page, 375, 900, "variant-families-directory-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva familia")),
                "new variant family action").click();
        requireMainHeading(page, "Nueva familia de variantes");

        String code = "VF" + suffix.substring(0, 6);
        String name = "Calzado visual " + suffix;
        requireOne(page.getByLabel(
                "Código de familia", new Page.GetByLabelOptions().setExact(true)),
                "variant family code").fill(code);
        requireOne(page.getByLabel(
                "Nombre de familia", new Page.GetByLabelOptions().setExact(true)),
                "variant family name").fill(name);
        requireOne(page.getByLabel(
                "Código del atributo", new Page.GetByLabelOptions().setExact(true)),
                "first variant attribute code").fill("COLOR");
        requireOne(page.getByLabel(
                "Nombre del atributo", new Page.GetByLabelOptions().setExact(true)),
                "first variant attribute name").fill("Color");
        requireOne(page.getByLabel(
                "Tipo de valor", new Page.GetByLabelOptions().setExact(true)),
                "first variant attribute type").selectOption("TEXT");
        requireOne(page.getByLabel(
                "Obligatoriedad", new Page.GetByLabelOptions().setExact(true)),
                "first variant attribute requirement").selectOption("REQUIRED");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agregar atributo")),
                "add first variant attribute").click();
        requireOne(page.getByText(
                "Atributo agregado", new Page.GetByTextOptions().setExact(true)),
                "first attribute confirmation").waitFor();

        requireOne(page.getByLabel(
                "Código del atributo", new Page.GetByLabelOptions().setExact(true)),
                "second variant attribute code").fill("TALLA");
        requireOne(page.getByLabel(
                "Nombre del atributo", new Page.GetByLabelOptions().setExact(true)),
                "second variant attribute name").fill("Talla");
        requireOne(page.getByLabel(
                "Tipo de valor", new Page.GetByLabelOptions().setExact(true)),
                "second variant attribute type").selectOption("NUMBER");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agregar atributo")),
                "add second variant attribute").click();
        requireOne(page.getByText(
                "COLOR | Color | Texto | Obligatorio • TALLA | Talla | Número | Obligatorio",
                new Page.GetByTextOptions().setExact(true)),
                "ordered variant attribute draft").waitFor();
        assertResponsive(page, 375, 900, "variant-family-create-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar familia")),
                "register variant family").click();
        requireOne(page.getByText(
                "Familia de variantes registrada", new Page.GetByTextOptions().setExact(true)),
                "variant family confirmation").waitFor();
        requireMainHeading(page, name);
        requireOne(page.getByText(
                "COLOR · Color · Texto · Obligatorio",
                new Page.GetByTextOptions().setExact(true)),
                "created family first attribute").waitFor();
        requireOne(page.getByText(
                "TALLA · Talla · Número · Obligatorio",
                new Page.GetByTextOptions().setExact(true)),
                "created family second attribute").waitFor();
        assertAccessibleStructure(page);

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva revisión")),
                "variant family revision tab").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Retirar último")),
                "remove second attribute from revision").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Retirar último")),
                "remove first attribute from revision").click();
        requireOne(page.getByText(
                "Sin atributos agregados", new Page.GetByTextOptions().setExact(true)),
                "empty variant revision draft").waitFor();
        String revisedName = "Calzado por número " + suffix;
        requireOne(page.getByLabel(
                "Nombre revisado", new Page.GetByLabelOptions().setExact(true)),
                "variant family revised name").fill(revisedName);
        requireOne(page.getByLabel(
                "Código del atributo", new Page.GetByLabelOptions().setExact(true)),
                "variant revision attribute code").fill("NUMERO");
        requireOne(page.getByLabel(
                "Nombre del atributo", new Page.GetByLabelOptions().setExact(true)),
                "variant revision attribute name").fill("Número");
        requireOne(page.getByLabel(
                "Tipo de valor", new Page.GetByLabelOptions().setExact(true)),
                "variant revision attribute type").selectOption("NUMBER");
        requireOne(page.getByLabel(
                "Obligatoriedad", new Page.GetByLabelOptions().setExact(true)),
                "variant revision attribute requirement").selectOption("REQUIRED");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agregar a la revisión")),
                "add variant revision attribute").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Crear revisión")),
                "create variant family revision").click();
        requireOne(page.getByText(
                "Nueva revisión de familia creada", new Page.GetByTextOptions().setExact(true)),
                "variant family revision confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Resumen")),
                "revised variant family summary tab").click();
        requireMainHeading(page, revisedName);
        requireOne(page.getByText(
                "NUMERO · Número · Número · Obligatorio",
                new Page.GetByTextOptions().setExact(true)),
                "revised family attribute").waitFor();
        assertResponsive(page, 1280, 900, "variant-family-revision-expanded-1280.png");
        assertResponsive(page, 720, 900, "variant-family-revision-medium-720.png");
        assertResponsive(page, 375, 900, "variant-family-revision-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Historial")),
                "variant family history tab").click();
        requireOne(page.locator("table.business-table").getByText(
                revisedName, new Locator.GetByTextOptions().setExact(true)),
                "current family revision in history").waitFor();
        requireOne(page.locator("table.business-table").getByText(
                name, new Locator.GetByTextOptions().setExact(true)),
                "original family revision in history").waitFor();
        requireOne(page.locator("table.business-table").getByText(
                "COLOR · Texto · Obligatorio; TALLA · Número · Obligatorio",
                new Locator.GetByTextOptions().setExact(true)),
                "original family structure in history").waitFor();
        assertResponsive(page, 1280, 900, "variant-family-history-expanded-1280.png");
        assertResponsive(page, 720, 900, "variant-family-history-medium-720.png");
        assertResponsive(page, 375, 900, "variant-family-history-compact-375.png");
        name = revisedName;

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Familias de variantes")),
                "variant families menu after creation").click();
        requireOne(page.getByLabel(
                "Código o nombre", new Page.GetByLabelOptions().setExact(true)),
                "variant family filter").fill(code);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar")),
                "filter variant families").click();
        requireOne(page.locator("table.business-table").getByText(
                name, new Locator.GetByTextOptions().setExact(true)),
                "created family in filtered directory").waitFor();
        assertResponsive(page, 375, 900, "variant-families-filtered-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Abrir " + name)),
                "open family for lifecycle").click();
        requireMainHeading(page, name);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Estado")),
                "variant family lifecycle tab").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Inactivar familia")),
                "inactivate variant family").click();
        requireOne(page.getByText(
                "Familia de variantes inactivada",
                new Page.GetByTextOptions().setExact(true)),
                "variant family inactivation confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Resumen")),
                "variant family summary after inactivation").click();
        requireOne(page.getByText(
                "NUMERO · Número · Número · Obligatorio",
                new Page.GetByTextOptions().setExact(true)),
                "current family revision preserved after inactivation").waitFor();
        requireOne(page.locator(".business-summary-grid").getByText(
                "Inactiva", new Locator.GetByTextOptions().setExact(true)),
                "inactive variant family summary state").waitFor();
        assertResponsive(page, 1280, 900, "variant-family-inactive-expanded-1280.png");
        assertResponsive(page, 720, 900, "variant-family-inactive-medium-720.png");
        assertResponsive(page, 375, 900, "variant-family-inactive-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Familias de variantes")),
                "variant families menu after inactivation").click();
        requireOne(page.getByLabel(
                "Código o nombre", new Page.GetByLabelOptions().setExact(true)),
                "inactive variant family search").fill(code);
        requireOne(page.getByLabel("Estado", new Page.GetByLabelOptions().setExact(true)),
                "inactive variant family state").selectOption("INACTIVE");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar")),
                "search inactive variant family").click();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Abrir " + name)),
                "open inactive variant family").click();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Estado")),
                "inactive variant family lifecycle tab").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reactivar familia")),
                "reactivate variant family").click();
        requireOne(page.getByText(
                "Familia de variantes reactivada",
                new Page.GetByTextOptions().setExact(true)),
                "variant family reactivation confirmation").waitFor();
        assertAccessibleStructure(page);
        return name;
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
        String companyId = firstNonBlankOptionValue(selector, "authorized company");
        selector.selectOption(companyId);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continuar")),
                "continue company selection").click();
        Locator authorizedWorkspace = page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles"));
        authorizedWorkspace.waitFor();
        requireOne(authorizedWorkspace, "authorized workspace");
        return companyId;
    }

    private void verifyNativeSelectorReturn(Page page, String suffix) {
        page.navigate(systemAuthorityUrl());
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Roles y permisos de la instancia")),
                "system authority heading").waitFor();

        Locator assignmentForm = requireOne(
                page.locator("#system-assignment-form"), "system assignment form");
        Locator user = requireOne(assignmentForm.getByLabel("Usuario"), "system user selector");
        Locator role = requireOne(
                assignmentForm.getByLabel("Rol global"), "system role selector");
        String selectedUser = firstNonBlankOptionValue(user, "system user");
        String selectedRole = firstNonBlankOptionValue(role, "system role");
        user.selectOption(selectedUser);
        role.selectOption(selectedRole);

        Locator nativeManagementActions = assignmentForm.locator(".selector-management-link");
        assertEquals(2, nativeManagementActions.count(),
                "system assignment must expose user and role management");
        assertEquals("object", page.evaluate("typeof window.LogixoneSelectorReturn"),
                "native selector return client helper must be loaded");
        assertEquals("function", page.evaluate("typeof mojarra.cljs"),
                "JSF command-link submit helper must be loaded");
        var pageErrors = new java.util.ArrayList<String>();
        page.onPageError(pageErrors::add);
        nativeManagementActions.first().click();
        page.waitForURL("**/admin/security.xhtml?selectorContext=*");
        page.waitForLoadState();
        assertTrue(pageErrors.isEmpty(),
                "native selector navigation must not raise client errors: " + pageErrors);
        Locator nativeTargetHeading = page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName(
                        "Usuarios y acceso por empresa"));
        assertEquals(1, nativeTargetHeading.count(),
                "native selector target heading missing; url=" + page.url()
                        + ", body=" + page.locator("body").innerText());
        nativeTargetHeading.waitFor();
        requireOne(page.getByText(
                "Administración contextual", new Page.GetByTextOptions().setExact(true)),
                "native selector target banner").waitFor();
        assertTrue(page.url().contains("selectorContext="));
        assertFalse(page.url().contains(selectedUser));
        assertFalse(page.url().contains(selectedRole));
        assertResponsive(page, 1280, 900, "native-selector-target-expanded-1280.png");
        assertResponsive(page, 720, 900, "native-selector-target-medium-720.png");
        assertResponsive(page, 375, 900, "native-selector-target-compact-375.png");

        String subject = "demo.native.return." + suffix.toLowerCase();
        String displayName = "Usuario retorno nativo " + suffix;
        page.setViewportSize(1280, 900);
        Locator registerForm = requireOne(
                page.locator("#register-user-form"), "native user registration form");
        requireOne(registerForm.getByLabel("Subject OIDC"), "native user subject")
                .fill(subject);
        requireOne(registerForm.getByLabel("Nombre de presentación"), "native user display name")
                .fill(displayName);
        requireOne(registerForm.getByRole(
                AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Registrar usuario")),
                "native user registration action").click();
        requireOne(page.locator(".admin-message"), "native user registration result").waitFor();
        assertTrue(page.url().contains("selectorContext="),
                "native context lost after target action; url=" + page.url()
                        + ", returnBannerCount=" + page.locator(".selector-return-banner").count()
                        + ", formActions=" + page.locator("form").evaluateAll(
                                "forms => forms.map(form => form.action).join(' | ')"));
        assertFalse(page.url().contains(subject));

        requireOne(page.locator(".selector-return-banner").getByRole(AriaRole.LINK),
                "native return action").click();
        page.waitForLoadState();
        assertTrue(page.url().contains("/admin/system-authority.xhtml?selectorReturn="),
                "native return did not navigate; url=" + page.url()
                        + ", returnBannerCount=" + page.locator(".selector-return-banner").count()
                        + ", pageErrors=" + pageErrors
                        + ", body=" + page.locator("body").innerText());
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Roles y permisos de la instancia")),
                "restored system authority heading").waitFor();
        requireOne(page.getByText(
                "Opciones actualizadas", new Page.GetByTextOptions().setExact(true)),
                "native restored notice").waitFor();
        assignmentForm = requireOne(
                page.locator("#system-assignment-form"), "restored system assignment form");
        user = requireOne(assignmentForm.getByLabel("Usuario"), "restored system user selector");
        role = requireOne(assignmentForm.getByLabel("Rol global"), "restored system role selector");
        assertEquals(selectedUser, user.inputValue());
        assertEquals(selectedRole, role.inputValue());
        assertEquals(1, user.locator("option").filter(
                new Locator.FilterOptions().setHasText(displayName)).count());
        assertTrue(page.url().contains("selectorReturn="));
        assertFalse(page.url().contains(subject));
        assertResponsive(page, 1280, 900, "native-selector-restored-expanded-1280.png");
        assertResponsive(page, 720, 900, "native-selector-restored-medium-720.png");
        assertResponsive(page, 375, 900, "native-selector-restored-compact-375.png");
        page.setViewportSize(1280, 900);
        page.navigate(appUrl);
        page.locator("main").waitFor();
    }

    private void enableRequiredPlugins(Page page, String companyId) {
        enablePlugin(page, companyId, "reference_data");
        enablePlugin(page, companyId, "business_partners");
        enablePlugin(page, companyId, "commercial_catalog");
        enablePlugin(page, companyId, "inventory");
    }

    private void enablePlugin(Page page, String companyId, String pluginId) {
        page.navigate(pluginsUrl(companyId));
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Plugins por empresa")),
                "plugins administration heading").waitFor();
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
    }

    private void grantCatalogPermissions(Page page, String companyId) {
        for (String permission : CATALOG_PERMISSIONS) {
            grantCompanyPermission(page, companyId, permission);
        }
    }

    private void grantCompanyPermission(Page page, String companyId, String permission) {
        page.navigate(securityUrl(companyId));
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Usuarios y acceso por empresa")),
                "security administration heading").waitFor();
        Locator form = requireOne(page.locator("#grant-permission-form"), "grant permission form");
        Locator role = requireOne(form.getByLabel(
                "Rol empresarial", new Locator.GetByLabelOptions().setExact(true)),
                "grant role");
        role.selectOption(optionValueContaining(role, DEMO_ROLE_CODE));
        requireOne(form.getByLabel(
                "Permiso disponible", new Locator.GetByLabelOptions().setExact(true)),
                "available permission").selectOption(permission);
        requireOne(form.getByRole(
                AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Conceder permiso")),
                "grant permission action").click();
        requireOne(page.locator(".admin-message"),
                "permission grant result").waitFor();
    }

    private void openAuthorizedWorkspace(Page page, String companyId) {
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
                "catalog workspace").waitFor();
    }

    private void verifyTaxProfilePermissionIsEnforcedAndRestore(Page page, String companyId) {
        String permission = "commercial_catalog.definitions.manage";
        boolean revoked = false;
        try {
            page.navigate(securityUrl(companyId));
            Locator roleCode = page.locator(".record-card-heading .eyebrow").filter(
                    new Locator.FilterOptions().setHasText(DEMO_ROLE_CODE));
            Locator roleCard = requireOne(page.locator("article.admin-record-card").filter(
                    new Locator.FilterOptions().setHas(roleCode)),
                    "demo operator role card");
            Locator relation = requireOne(roleCard.locator(".relation-row").filter(
                    new Locator.FilterOptions().setHasText(permission)),
                    "definitions permission relation");
            page.onceDialog(dialog -> dialog.accept());
            requireOne(relation.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Revocar")),
                    "revoke definitions permission").click();
            requireOne(page.locator(".admin-message"),
                    "permission revocation result").waitFor();
            revoked = true;

            page.navigate(appUrl);
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles")),
                    "workspace after definitions permission revocation").waitFor();
            assertEquals(0, page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Perfiles tributarios")).count(),
                    "tax profiles menu must disappear without definitions permission");
            assertEquals(0, page.getByRole(
                    AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("Definiciones del catálogo")).count(),
                    "catalog definitions menu must disappear without definitions permission");
            assertEquals(0, page.getByRole(
                    AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("Familias de variantes")).count(),
                    "variant families menu must disappear without definitions permission");
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Artículos y servicios")),
                    "catalog items remain available without definitions permission");

            page.navigate(appUrl.replace(
                    "index.xhtml", "view.xhtml?route=%2Fcatalog%2Ftax-profiles&mode=directory"));
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName(
                            "Esta función no está disponible para tu contexto actual")),
                    "tax profile direct route permission denial").waitFor();
            assertResponsive(page, 375, 900,
                    "tax-profiles-permission-denial-compact-375.png");

            page.navigate(appUrl.replace(
                    "index.xhtml", "view.xhtml?route=%2Fcatalog%2Fdefinitions&mode=directory"));
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName(
                            "Esta función no está disponible para tu contexto actual")),
                    "catalog definitions direct route permission denial").waitFor();
        } finally {
            if (revoked) {
                grantCompanyPermission(page, companyId, permission);
            }
        }
    }

    private void verifyDisabledCatalogIsDeniedAndRestore(Page page, String companyId) {
        String pluginsUrl = pluginsUrl(companyId);
        String catalogUrl = appUrl.replace(
                "index.xhtml", "view.xhtml?route=%2Fcatalog&mode=directory");
        boolean catalogDisabled = false;
        boolean inventoryDisabled = false;
        try {
            page.setViewportSize(1280, 900);
            page.navigate(pluginsUrl);
            Locator card = pluginCard(page, "commercial_catalog");
            page.onceDialog(dialog -> dialog.accept());
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    "disable commercial catalog").click();
            requireOne(page.locator(".admin-message").filter(new Locator.FilterOptions().setHasText(
                    "La composición solicitada no cumple sus dependencias. Revise los estados actuales.")),
                    "active inventory dependency rejection").waitFor();
            requireOne(pluginCard(page, "commercial_catalog").getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    "commercial catalog remains enabled after dependency rejection").waitFor();

            page.navigate(pluginsUrl);
            Locator inventoryCard = pluginCard(page, "inventory");
            page.onceDialog(dialog -> dialog.accept());
            requireOne(inventoryCard.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    "disable inventory before commercial catalog").click();
            inventoryDisabled = true;

            page.navigate(pluginsUrl);
            card = pluginCard(page, "commercial_catalog");
            page.onceDialog(dialog -> dialog.accept());
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    "disable commercial catalog without active dependents").click();
            catalogDisabled = true;
            page.navigate(catalogUrl);
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName(
                            "Esta función no está disponible para tu contexto actual")),
                    "disabled catalog denial").waitFor();
            assertResponsive(page, 375, 900, "commercial-catalog-disabled-denial-compact-375.png");

            page.navigate(pluginsUrl);
            card = pluginCard(page, "commercial_catalog");
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Habilitar")),
                    "restore commercial catalog").click();
            catalogDisabled = false;
            card = pluginCard(page, "commercial_catalog");
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    "restored commercial catalog state").waitFor();

            inventoryCard = pluginCard(page, "inventory");
            requireOne(inventoryCard.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Habilitar")),
                    "restore inventory after commercial catalog").click();
            inventoryDisabled = false;
            inventoryCard = pluginCard(page, "inventory");
            requireOne(inventoryCard.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    "restored inventory state").waitFor();
        } finally {
            if (catalogDisabled) {
                page.navigate(pluginsUrl);
                Locator enable = pluginCard(page, "commercial_catalog").getByRole(
                        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Habilitar"));
                if (enable.count() == 1) {
                    enable.click();
                }
            }
            if (inventoryDisabled) {
                page.navigate(pluginsUrl);
                Locator enable = pluginCard(page, "inventory").getByRole(
                        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Habilitar"));
                if (enable.count() == 1) {
                    enable.click();
                }
            }
        }
    }

    private Locator pluginCard(Page page, String pluginId) {
        return requireOne(page.locator("article.plugin-record-card").filter(
                new Locator.FilterOptions().setHasText(pluginId)), pluginId + " plugin card");
    }

    private String pluginsUrl(String companyId) {
        return adminUrl.replace("index.xhtml", "plugins.xhtml") + "?company=" + companyId;
    }

    private String securityUrl(String companyId) {
        return adminUrl.replace("index.xhtml", "security.xhtml") + "?company=" + companyId;
    }

    private String systemAuthorityUrl() {
        return adminUrl.replace("index.xhtml", "system-authority.xhtml");
    }

    private void assertResponsive(Page page, int width, int height, String screenshotName) {
        page.setViewportSize(width, height);
        assertResponsiveLayout(page, width, screenshotName);
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(evidenceDirectory.resolve(screenshotName))
                .setFullPage(false));
    }

    private void assertResponsiveLayout(Page page, int width, String description) {
        page.setViewportSize(width, 900);
        page.evaluate("""
                () => new Promise(resolve => requestAnimationFrame(
                  () => requestAnimationFrame(resolve)))
                """);
        boolean stylesLoaded = (Boolean) page.evaluate("""
                () => Array.from(document.styleSheets)
                  .filter(sheet => sheet.href
                    && sheet.href.includes('/faces/jakarta.faces.resource/'))
                  .every(sheet => sheet.cssRules.length > 0)
                """);
        int documentWidth = ((Number) page.evaluate(
                "() => document.documentElement.scrollWidth")).intValue();
        String overflowSources = (String) page.evaluate("""
                () => {
                  const viewport = document.documentElement.clientWidth;
                  return Array.from(document.querySelectorAll('body *'))
                    .map(element => ({ element, rect: element.getBoundingClientRect() }))
                    .filter(entry => entry.rect.right > viewport + 1 || entry.rect.left < -1)
                    .slice(0, 12)
                    .map(entry => {
                      const element = entry.element;
                      const identity = element.tagName.toLowerCase()
                        + (element.id ? '#' + element.id : '')
                        + (element.className && typeof element.className === 'string'
                          ? '.' + element.className.trim().replaceAll(' ', '.') : '');
                      return identity + '[left=' + Math.round(entry.rect.left)
                        + ',right=' + Math.round(entry.rect.right)
                        + ',width=' + Math.round(entry.rect.width) + ']';
                    })
                    .join(' | ');
                }
                """);
        assertTrue(stylesLoaded,
                description + " must load the application Material Design stylesheets");
        assertTrue(documentWidth <= width + 1,
                description + " has horizontal overflow: document="
                        + documentWidth + " px, viewport=" + width
                        + " px, sources=" + overflowSources);
    }

    private void assertAccessibleStructure(Page page) {
        assertEquals(1, page.locator("main h1").count(),
                "the catalog screen must expose exactly one main heading");
        boolean everyEditableControlHasLabel = (Boolean) page.evaluate("""
                () => Array.from(document.querySelectorAll(
                    "input:not([type='hidden']):not([type='submit']), select, textarea"))
                  .every(control => control.labels && control.labels.length > 0)
                """);
        assertTrue(everyEditableControlHasLabel,
                "every editable commercial-catalog control must have a label");
    }

    private static void requireMainHeading(Page page, String expectedText) {
        Locator heading = page.locator("main h1");
        heading.waitFor();
        requireOne(heading, "main heading");
        assertEquals(expectedText, heading.innerText(), "the main heading text must match");
    }

    private static String firstNonBlankOptionValue(Locator select, String description) {
        Locator options = select.locator("option");
        int count = options.count();
        for (int index = 0; index < count; index++) {
            String value = options.nth(index).getAttribute("value");
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new AssertionError(description + " must expose a non-blank option");
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
        locator.first().waitFor();
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
