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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "logixone.inventory.e2e", matches = "true")
class InventoryVisualIT {

    private static final String MULTIPLE_COMPANIES_USER = "demo.empresas.ab";
    private static final String DEMO_ROLE_CODE = "demo_operator";
    private static final List<String> REQUIRED_PLUGINS = List.of(
            "reference_data", "business_partners", "commercial_catalog", "inventory");
    private static final List<String> REQUIRED_PERMISSIONS = List.of(
            "reference_data.view",
            "business_partners.view",
            "commercial_catalog.view",
            "commercial_catalog.items.manage",
            "commercial_catalog.definitions.manage",
            "inventory.view",
            "inventory.storage.manage",
            "inventory.items.manage",
            "inventory.movements.post",
            "inventory.reservations.manage",
            "inventory.counts.manage",
            "inventory.adjustments.post");

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
    void composesInventoryAndExercisesStockReservationCountAndSecurityJourneys() {
        String companyId;
        try (BrowserContext setupContext = newContext()) {
            Page setupPage = authenticate(setupContext);
            companyId = selectFirstAuthorizedCompany(setupPage);
            enableRequiredPlugins(setupPage, companyId);
            grantRequiredPermissions(setupPage, companyId);
        }

        try (BrowserContext context = newContext()) {
            Page page = authenticate(context);
            String selectedCompanyId = selectFirstAuthorizedCompany(page);
            assertEquals(companyId, selectedCompanyId,
                    "the functional journey must use the company prepared through administration");
            assertMergedMenus(page);

            String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String unitCode = createCatalogUnit(page, suffix);
            String taxProfileName = createTaxProfile(page, suffix);
            String itemName = createCatalogProduct(page, suffix, unitCode, taxProfileName);
            String warehouseCode = createWarehouse(page, suffix);
            enrollAndOperateStock(page, suffix, itemName, unitCode, warehouseCode);
            executeStockCount(page, itemName, warehouseCode);
            verifyDisabledInventoryIsDeniedAndRestore(page, companyId);
        }
    }

    private BrowserContext newContext() {
        return browser.newContext(new Browser.NewContextOptions()
                .setLocale("es-PY")
                .setViewportSize(1280, 900));
    }

    private String createCatalogUnit(Page page, String suffix) {
        page.navigate(routeUrl("%2Fcatalog%2Fdefinitions", "directory"));
        requireMainHeading(page, "Definiciones del catálogo");
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva definición")),
                "new catalog definition action").click();
        requireMainHeading(page, "Nueva definición");

        String code = "I" + suffix.substring(0, 7);
        requireOne(page.getByLabel(
                "Tipo de definición", new Page.GetByLabelOptions().setExact(true)),
                "definition kind").selectOption("UNIT");
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "unit code").fill(code);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "unit name").fill("Unidad inventario " + suffix);
        requireOne(page.getByLabel(
                "Decimales (sólo unidades)", new Page.GetByLabelOptions().setExact(true)),
                "unit scale").selectOption("2");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar definición")),
                "register inventory unit").click();
        requireOne(page.getByText(
                "Definición registrada", new Page.GetByTextOptions().setExact(true)),
                "inventory unit confirmation").waitFor();
        return code;
    }

    private String createTaxProfile(Page page, String suffix) {
        page.navigate(routeUrl("%2Fcatalog%2Ftax-profiles", "directory"));
        requireMainHeading(page, "Perfiles tributarios");
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo perfil")),
                "new inventory tax profile action").click();
        requireMainHeading(page, "Nuevo perfil tributario");

        String name = "Perfil inventario " + suffix;
        requireOne(page.getByLabel(
                "Código interno", new Page.GetByLabelOptions().setExact(true)),
                "tax profile code").fill("PI_" + suffix);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "tax profile name").fill(name);
        requireOne(page.getByLabel(
                "Tratamiento interno", new Page.GetByLabelOptions().setExact(true)),
                "tax profile treatment").fill("TAXED_DEMO");
        requireOne(page.getByLabel("Descripción", new Page.GetByLabelOptions().setExact(true)),
                "tax profile description").fill(
                        "Perfil ficticio del escenario de inventario; no representa una regla SIFEN");
        requireOne(page.getByLabel(
                "Vigente desde", new Page.GetByLabelOptions().setExact(true)),
                "tax profile validity start").fill(
                        LocalDate.now().minusDays(1) + "T00:00:00Z");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar perfil")),
                "register inventory tax profile").click();
        requireOne(page.getByText(
                "Perfil tributario registrado", new Page.GetByTextOptions().setExact(true)),
                "inventory tax profile confirmation").waitFor();
        return name;
    }

    private String createCatalogProduct(
            Page page, String suffix, String unitCode, String taxProfileName) {
        page.navigate(routeUrl("%2Fcatalog", "directory"));
        requireMainHeading(page, "Artículos y servicios");
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo artículo o servicio")),
                "new catalog product action").click();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Nuevo artículo o servicio")),
                "new catalog product heading").waitFor();

        String itemName = "Producto inventariable " + suffix;
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "catalog product code").fill("INV-" + suffix);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "catalog product name").fill(itemName);
        requireOne(page.getByLabel("Descripción", new Page.GetByLabelOptions().setExact(true)),
                "catalog product description").fill("Producto ficticio para la demostración de inventario");
        requireOne(page.getByLabel("Tipo", new Page.GetByLabelOptions().setExact(true)),
                "catalog product type").selectOption("PRODUCT");
        requireOne(page.getByLabel("Alcance", new Page.GetByLabelOptions().setExact(true)),
                "catalog product scope").selectOption("BOTH");
        requireOne(page.getByLabel("Unidad base", new Page.GetByLabelOptions().setExact(true)),
                "catalog product unit").selectOption(unitCode);
        requireOne(page.getByLabel("Perfil tributario", new Page.GetByLabelOptions().setExact(true)),
                "catalog product tax profile").selectOption(
                        new SelectOption().setLabel(taxProfileName));
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar")),
                "register catalog product").click();
        requireOne(page.getByText(
                "Artículo o servicio registrado", new Page.GetByTextOptions().setExact(true)),
                "catalog product confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName(itemName)),
                "catalog product detail").waitFor();
        return itemName;
    }

    private String createWarehouse(Page page, String suffix) {
        page.navigate(routeUrl("%2Finventory%2Fwarehouses", "directory"));
        requireMainHeading(page, "Depósitos");
        assertResponsive(page, 1280, 900, "inventory-warehouses-directory-expanded-1280.png");
        assertResponsive(page, 720, 900, "inventory-warehouses-directory-medium-720.png");
        assertResponsive(page, 375, 900, "inventory-warehouses-directory-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo depósito")),
                "new warehouse action").click();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Nuevo depósito")),
                "new warehouse heading").waitFor();

        String warehouseCode = "DEP-" + suffix;
        String warehouseName = "Depósito visual " + suffix;
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "warehouse code").fill(warehouseCode);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "warehouse name").fill(warehouseName);
        assertResponsive(page, 375, 900, "inventory-warehouse-create-compact-375.png");
        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Crear depósito")),
                "create warehouse").click();
        requireOne(page.getByText("Depósito creado", new Page.GetByTextOptions().setExact(true)),
                "warehouse confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName(warehouseName)),
                "warehouse detail").waitFor();

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Ubicaciones")),
                "warehouse locations tab").click();
        requireOne(page.getByLabel("Código de ubicación", new Page.GetByLabelOptions().setExact(true)),
                "location code").fill("DEMO");
        requireOne(page.getByLabel("Nombre de ubicación", new Page.GetByLabelOptions().setExact(true)),
                "location name").fill("Área de demostración");
        requireOne(page.getByLabel("Tipo", new Page.GetByLabelOptions().setExact(true)),
                "location type").selectOption("STORAGE");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agregar ubicación")),
                "add warehouse location").click();
        requireOne(page.getByText("Ubicación agregada", new Page.GetByTextOptions().setExact(true)),
                "location confirmation").waitFor();
        assertResponsive(page, 1280, 900, "inventory-warehouse-detail-expanded-1280.png");
        assertResponsive(page, 720, 900, "inventory-warehouse-detail-medium-720.png");
        assertResponsive(page, 375, 900, "inventory-warehouse-detail-compact-375.png");
        return warehouseCode;
    }

    private void enrollAndOperateStock(
            Page page, String suffix, String itemName, String unitCode, String warehouseCode) {
        page.navigate(routeUrl("%2Finventory", "directory"));
        requireMainHeading(page, "Existencias");
        assertResponsive(page, 1280, 900, "inventory-stock-directory-expanded-1280.png");
        assertResponsive(page, 720, 900, "inventory-stock-directory-medium-720.png");
        assertResponsive(page, 375, 900, "inventory-stock-directory-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Incorporar producto")),
                "enroll inventory product action").click();
        Locator product = requireOne(page.getByLabel(
                "Producto de catálogo", new Page.GetByLabelOptions().setExact(true)),
                "catalog product for inventory");
        product.selectOption(optionValueContaining(product, itemName));
        requireOne(page.getByLabel("Seguimiento", new Page.GetByLabelOptions().setExact(true)),
                "inventory tracking").selectOption("NONE");
        requireOne(page.getByLabel("Vencimiento", new Page.GetByLabelOptions().setExact(true)),
                "inventory expiry policy").selectOption("NONE");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Incorporar")),
                "enroll inventory product").click();
        requireOne(page.getByText(
                "Artículo incorporado al inventario", new Page.GetByTextOptions().setExact(true)),
                "inventory enrollment confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName(itemName)),
                "inventory product detail").waitFor();

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Movimientos")),
                "inventory movements tab").click();
        requireOne(page.getByLabel("Tipo de movimiento", new Page.GetByLabelOptions().setExact(true)),
                "movement type").selectOption("RECEIPT");
        selectInputContaining(page, "movement_warehouse", warehouseCode);
        selectInputContaining(page, "movement_location", warehouseCode + " · GENERAL");
        requireOne(page.getByLabel("Condición", new Page.GetByLabelOptions().setExact(true)),
                "movement condition").selectOption("AVAILABLE");
        requireOne(page.getByLabel("Cantidad", new Page.GetByLabelOptions().setExact(true)),
                "movement quantity").fill("12");
        requireOne(page.getByLabel("Motivo", new Page.GetByLabelOptions().setExact(true)),
                "movement reason").fill("DEMO_RECEIPT");
        requireOne(page.getByLabel("Tipo de origen", new Page.GetByLabelOptions().setExact(true)),
                "movement source type").fill("DEMO_UI");
        requireOne(page.getByLabel("Identidad de origen", new Page.GetByLabelOptions().setExact(true)),
                "movement source id").fill("MOV-" + suffix);
        requireOne(page.getByLabel("Clave de idempotencia", new Page.GetByLabelOptions().setExact(true)),
                "movement idempotency").fill("inventory-demo-movement-" + suffix);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar movimiento")),
                "post stock receipt").click();
        requireOne(page.getByText(
                "Movimiento de existencias registrado", new Page.GetByTextOptions().setExact(true)),
                "stock movement confirmation").waitFor();
        assertResponsive(page, 1280, 900, "inventory-stock-movement-expanded-1280.png");

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Reservas")),
                "inventory reservations tab").click();
        selectInputContaining(page, "reservation_warehouse", warehouseCode);
        selectInputContaining(page, "reservation_location", warehouseCode + " · GENERAL");
        requireOne(page.getByLabel("Condición", new Page.GetByLabelOptions().setExact(true)),
                "reservation condition").selectOption("AVAILABLE");
        Locator createReservation = requireOne(page.getByRole(
                AriaRole.REGION, new Page.GetByRoleOptions().setName("Crear reserva").setExact(true)),
                "create reservation region");
        requireOne(createReservation.getByLabel(
                "Cantidad", new Locator.GetByLabelOptions().setExact(true)),
                "reservation quantity").fill("3");
        requireOne(page.getByLabel("La reserva vence en", new Page.GetByLabelOptions().setExact(true)),
                "reservation expiration").fill("2027-01-01T00:00:00Z");
        requireOne(page.getByLabel("Tipo de origen", new Page.GetByLabelOptions().setExact(true)),
                "reservation source type").fill("DEMO_UI");
        requireOne(page.getByLabel("Identidad de origen", new Page.GetByLabelOptions().setExact(true)),
                "reservation source id").fill("RES-" + suffix);
        requireOne(page.getByLabel("Clave de idempotencia", new Page.GetByLabelOptions().setExact(true)),
                "reservation idempotency").fill("inventory-demo-reservation-" + suffix);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Crear reserva")),
                "create stock reservation").click();
        requireOne(page.getByText("Reserva creada", new Page.GetByTextOptions().setExact(true)),
                "reservation confirmation").waitFor();
        assertResponsive(page, 720, 900, "inventory-stock-reservation-medium-720.png");

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Disponibilidad")),
                "inventory availability tab").click();
        selectInputContaining(page, "availability_warehouse", warehouseCode);
        selectInputContaining(page, "availability_location", warehouseCode + " · GENERAL");
        requireOne(page.getByLabel("Condición", new Page.GetByLabelOptions().setExact(true)),
                "availability condition").selectOption("AVAILABLE");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Consultar")),
                "check stock availability").click();
        requireOne(page.getByText("Disponibilidad consultada", new Page.GetByTextOptions().setExact(true)),
                "availability confirmation").waitFor();
        requireOne(page.getByText(
                        "Físico: 12 · Reservado: 3 · Disponible: 9 " + unitCode),
                "availability quantities").waitFor();
        assertAccessibleStructure(page);
        assertResponsive(page, 375, 900, "inventory-stock-availability-compact-375.png");
        for (int boundary : List.of(599, 600, 839, 840)) {
            assertResponsiveLayout(page, boundary, "inventory-stock-boundary-" + boundary);
        }
    }

    private void executeStockCount(Page page, String itemName, String warehouseCode) {
        page.navigate(routeUrl("%2Finventory%2Fcounts", "directory"));
        requireMainHeading(page, "Conteos físicos");
        assertResponsive(page, 1280, 900, "inventory-counts-directory-expanded-1280.png");
        assertResponsive(page, 720, 900, "inventory-counts-directory-medium-720.png");
        assertResponsive(page, 375, 900, "inventory-counts-directory-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo conteo")),
                "new stock count action").click();
        selectInputContaining(page, "count_new_warehouse", warehouseCode);
        selectInputContaining(page, "count_new_location", warehouseCode + " · GENERAL");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Preparar conteo")),
                "draft stock count").click();
        requireOne(page.getByText("Conteo físico preparado", new Page.GetByTextOptions().setExact(true)),
                "stock count draft confirmation").waitFor();

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Líneas")),
                "stock count lines tab").click();
        selectInputContaining(page, "count_line_item", itemName);
        selectInputContaining(page, "count_line_location", "GENERAL");
        requireOne(page.getByLabel("Condición", new Page.GetByLabelOptions().setExact(true)),
                "count line condition").selectOption("AVAILABLE");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Agregar línea")),
                "add stock count line").click();
        requireOne(page.getByText("Línea agregada al conteo", new Page.GetByTextOptions().setExact(true)),
                "stock count line confirmation").waitFor();

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Flujo")),
                "stock count flow tab").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Iniciar conteo")),
                "start stock count").click();
        requireOne(page.getByText("Conteo iniciado", new Page.GetByTextOptions().setExact(true)),
                "stock count start confirmation").waitFor();

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Captura")),
                "stock count capture tab").click();
        requireOne(page.getByLabel("Cantidad contada", new Page.GetByLabelOptions().setExact(true)),
                "counted quantity").fill("12");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar cantidad")),
                "record counted quantity").click();
        requireOne(page.getByText(
                "Cantidad contada registrada", new Page.GetByTextOptions().setExact(true)),
                "counted quantity confirmation").waitFor();

        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Flujo")),
                "stock count final flow tab").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Enviar a revisión")),
                "review stock count").click();
        requireOne(page.getByText(
                "Conteo enviado a revisión", new Page.GetByTextOptions().setExact(true)),
                "stock count review confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Contabilizar")),
                "post stock count").click();
        requireOne(page.getByText(
                "Conteo contabilizado y ajustes registrados", new Page.GetByTextOptions().setExact(true)),
                "stock count posting confirmation").waitFor();
        assertAccessibleStructure(page);
        assertResponsive(page, 1280, 900, "inventory-count-posted-expanded-1280.png");
        assertResponsive(page, 720, 900, "inventory-count-posted-medium-720.png");
        assertResponsive(page, 375, 900, "inventory-count-posted-compact-375.png");
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
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles")),
                "authorized workspace").waitFor();
        return companyId;
    }

    private void enableRequiredPlugins(Page page, String companyId) {
        page.navigate(pluginsUrl(companyId));
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Plugins por empresa")),
                "plugins administration heading").waitFor();
        for (String pluginId : REQUIRED_PLUGINS) {
            Locator card = pluginCard(page, pluginId);
            Locator enable = card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Habilitar"));
            if (enable.count() == 1) {
                enable.click();
                card = pluginCard(page, pluginId);
            }
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    pluginId + " enabled state").waitFor();
        }
    }

    private void grantRequiredPermissions(Page page, String companyId) {
        String securityUrl = adminUrl.replace("index.xhtml", "security.xhtml")
                + "?company=" + companyId;
        for (String permission : REQUIRED_PERMISSIONS) {
            page.navigate(securityUrl);
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
            requireOne(page.locator(".admin-message"), "permission grant result").waitFor();
        }
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
                "inventory workspace").waitFor();
    }

    private void assertMergedMenus(Page page) {
        for (String menu : List.of(
                "Socios comerciales", "Artículos y servicios", "Listas de precios",
                "Definiciones del catálogo", "Perfiles tributarios",
                "Existencias", "Depósitos", "Conteos físicos")) {
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName(menu)),
                    menu + " merged menu");
        }
        assertResponsive(page, 1280, 900, "inventory-merged-workspace-expanded-1280.png");
        assertResponsive(page, 720, 900, "inventory-merged-workspace-medium-720.png");
        assertResponsive(page, 375, 900, "inventory-merged-workspace-compact-375.png");
    }

    private void verifyDisabledInventoryIsDeniedAndRestore(Page page, String companyId) {
        String pluginsUrl = pluginsUrl(companyId);
        String inventoryUrl = routeUrl("%2Finventory", "directory");
        boolean inventoryDisabled = false;
        boolean purchasingDisabled = false;
        try {
            page.setViewportSize(1280, 900);
            purchasingDisabled = disablePluginIfEnabled(page, pluginsUrl, "purchasing");
            page.navigate(pluginsUrl);
            Locator card = pluginCard(page, "inventory");
            page.onceDialog(dialog -> dialog.accept());
            requireOne(card.getByRole(
                    AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deshabilitar")),
                    "disable inventory").click();
            requireOne(pluginCard(page, "inventory").getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions().setName("Habilitar").setExact(true)),
                    "disabled inventory state").waitFor();
            inventoryDisabled = true;
            page.navigate(inventoryUrl);
            Locator disabledDenial = page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName(
                            "Esta función no está disponible para tu contexto actual"));
            disabledDenial.waitFor();
            requireOne(disabledDenial, "disabled inventory denial");
            assertResponsive(page, 375, 900, "inventory-disabled-denial-compact-375.png");

            enablePlugin(page, pluginsUrl, "inventory");
            inventoryDisabled = false;
            if (purchasingDisabled) {
                enablePlugin(page, pluginsUrl, "purchasing");
                purchasingDisabled = false;
            }
        } finally {
            if (inventoryDisabled) {
                enablePlugin(page, pluginsUrl, "inventory");
            }
            if (purchasingDisabled) {
                enablePlugin(page, pluginsUrl, "purchasing");
            }
        }
    }

    private boolean disablePluginIfEnabled(Page page, String pluginsUrl, String pluginId) {
        page.navigate(pluginsUrl);
        Locator cards = page.locator("article.plugin-record-card").filter(
                new Locator.FilterOptions().setHasText(pluginId));
        if (cards.count() == 0) {
            return false;
        }
        Locator disable = requireOne(cards, pluginId + " plugin card").getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Deshabilitar").setExact(true));
        if (disable.count() == 0) {
            return false;
        }
        page.onceDialog(dialog -> dialog.accept());
        disable.click();
        requireOne(pluginCard(page, pluginId).getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Habilitar").setExact(true)),
                pluginId + " disabled state").waitFor();
        return true;
    }

    private void enablePlugin(Page page, String pluginsUrl, String pluginId) {
        page.navigate(pluginsUrl);
        Locator enable = pluginCard(page, pluginId).getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Habilitar").setExact(true));
        if (enable.count() == 1) {
            enable.click();
        }
        requireOne(pluginCard(page, pluginId).getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Deshabilitar").setExact(true)),
                pluginId + " restored state").waitFor();
    }

    private Locator pluginCard(Page page, String pluginId) {
        Locator cards = page.locator("article.plugin-record-card").filter(
                new Locator.FilterOptions().setHasText(pluginId));
        cards.first().waitFor();
        return requireOne(cards, pluginId + " plugin card");
    }

    private String pluginsUrl(String companyId) {
        return adminUrl.replace("index.xhtml", "plugins.xhtml") + "?company=" + companyId;
    }

    private String routeUrl(String encodedRoute, String mode) {
        return appUrl.replace("index.xhtml", "view.xhtml?route=" + encodedRoute + "&mode=" + mode);
    }

    private void selectInputContaining(Page page, String inputId, String expectedText) {
        Locator select = requireOne(page.locator(
                        "select[data-screen-input='" + inputId + "']"),
                inputId + " select");
        select.selectOption(optionValueContaining(select, expectedText));
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
                "the inventory screen must expose exactly one main heading");
        boolean everyEditableControlHasLabel = (Boolean) page.evaluate("""
                () => Array.from(document.querySelectorAll(
                    "input:not([type='hidden']):not([type='submit']), select, textarea"))
                  .every(control => control.labels && control.labels.length > 0)
                """);
        assertTrue(everyEditableControlHasLabel,
                "every editable inventory control must have a label");
    }

    private static void requireMainHeading(Page page, String expectedText) {
        Locator heading = requireOne(page.locator("main h1"), "main heading");
        heading.waitFor();
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
