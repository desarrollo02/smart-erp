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
import com.microsoft.playwright.options.ReducedMotion;
import com.microsoft.playwright.options.SelectOption;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "logixone.purchasing.e2e", matches = "true")
class PurchasingVisualIT {

    private static final String MULTIPLE_COMPANIES_USER = "demo.empresas.ab";
    private static final String SINGLE_COMPANY_USER = "demo.empresa.a";
    private static final String DEMO_ROLE_CODE = "demo_operator";
    private static final List<String> REQUIRED_PLUGINS = List.of(
            "reference_data", "business_partners", "commercial_catalog", "inventory",
            "purchasing");
    private static final List<String> REQUIRED_PERMISSIONS = List.of(
            "reference_data.view",
            "business_partners.view",
            "business_partners.manage",
            "business_partners.roles.manage",
            "commercial_catalog.view",
            "commercial_catalog.items.manage",
            "commercial_catalog.definitions.manage",
            "inventory.view",
            "inventory.storage.manage",
            "inventory.items.manage",
            "inventory.movements.purchase.post",
            "purchasing.view",
            "purchasing.requests.create",
            "purchasing.requests.submit",
            "purchasing.requests.approve",
            "purchasing.orders.create",
            "purchasing.orders.issue",
            "purchasing.orders.close",
            "purchasing.receipts.create",
            "purchasing.receipts.confirm",
            "purchasing.returns.create",
            "purchasing.returns.confirm");

    private Playwright playwright;
    private Browser browser;
    private String appUrl;
    private String adminUrl;
    private String password;
    private Path evidenceDirectory;

    @BeforeAll
    @Timeout(
            value = 90,
            unit = TimeUnit.SECONDS,
            threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
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
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void composesPurchasingAndExercisesApprovalFulfillmentAndSecurityJourneys() {
        String approvalCompanyId;
        try (BrowserContext approvalDiscoveryContext = newContext()) {
            Page approvalPage = authenticate(approvalDiscoveryContext, SINGLE_COMPANY_USER);
            approvalCompanyId = enterWorkspace(approvalPage);
        }

        String companyId;
        try (BrowserContext setupContext = newContext()) {
            Page setupPage = authenticate(setupContext, MULTIPLE_COMPANIES_USER);
            companyId = enterWorkspace(setupPage, approvalCompanyId);
            enableRequiredPlugins(setupPage, companyId);
            grantRequiredPermissions(setupPage, companyId);
        }

        try (BrowserContext context = newContext()) {
            Page page = authenticate(context, MULTIPLE_COMPANIES_USER);
            assertEquals(companyId, enterWorkspace(page, companyId));
            assertMergedMenus(page);

            String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String supplierName = createSupplier(page, suffix);
            String unitCode = createCatalogUnit(page, suffix);
            String taxProfileName = createTaxProfile(page, suffix);
            String itemName = createCatalogProduct(page, suffix, unitCode, taxProfileName);
            String warehouseCode = createWarehouse(page, suffix);
            enrollCatalogProduct(page, itemName);

            String requestNumber = "SC-" + suffix;
            createAndSubmitRequest(page, requestNumber, itemName, unitCode);
            approveRequestWithIndependentActor(requestNumber, companyId);

            String orderNumber = "OC-" + suffix;
            createAndIssueOrder(page, orderNumber, supplierName, itemName, unitCode);
            String receiptNumber = "RC-" + suffix;
            createAndConfirmReceipt(page, receiptNumber, orderNumber, warehouseCode);
            String returnNumber = "DV-" + suffix;
            createAndConfirmReturn(page, returnNumber, orderNumber, receiptNumber);
            verifyTracking(page, orderNumber);
            verifyDisabledPurchasingIsDeniedAndRestore(page, companyId);
        }
    }

    private BrowserContext newContext() {
        return browser.newContext(new Browser.NewContextOptions()
                .setLocale("es-PY")
                .setReducedMotion(ReducedMotion.REDUCE)
                .setViewportSize(1280, 900));
    }

    private String createSupplier(Page page, String suffix) {
        page.navigate(routeUrl("%2Fbusiness-partners", "directory"));
        requireMainHeading(page, "Socios comerciales");
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo socio")),
                "new supplier action").click();
        requireOne(page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Nuevo socio comercial")),
                "new supplier heading").waitFor();

        String supplierName = "Proveedor visual " + suffix;
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "supplier code").fill("SUP-" + suffix);
        requireOne(page.getByLabel("Nombre visible", new Page.GetByLabelOptions().setExact(true)),
                "supplier name").fill(supplierName);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar")),
                "register supplier").click();
        requireOne(page.getByText(
                "Socio comercial registrado", new Page.GetByTextOptions().setExact(true)),
                "supplier confirmation").waitFor();
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Roles y estado")),
                "supplier roles tab").click();
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Asignar proveedor")),
                "assign supplier role").click();
        requireOne(page.getByText(
                "Rol proveedor asignado", new Page.GetByTextOptions().setExact(true)),
                "supplier role confirmation").waitFor();
        return supplierName;
    }

    private String createCatalogUnit(Page page, String suffix) {
        page.navigate(routeUrl("%2Fcatalog%2Fdefinitions", "directory"));
        requireMainHeading(page, "Definiciones del catálogo");
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nueva definición")),
                "new catalog definition action").click();
        requireMainHeading(page, "Nueva definición");

        String code = "U" + suffix.substring(0, 7);
        requireOne(page.getByLabel(
                "Tipo de definición", new Page.GetByLabelOptions().setExact(true)),
                "definition kind").selectOption("UNIT");
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "unit code").fill(code);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "unit name").fill("Unidad compras " + suffix);
        requireOne(page.getByLabel(
                "Decimales (sólo unidades)", new Page.GetByLabelOptions().setExact(true)),
                "unit scale").selectOption("2");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar definición")),
                "register purchasing unit").click();
        requireOne(page.getByText(
                "Definición registrada", new Page.GetByTextOptions().setExact(true)),
                "purchasing unit confirmation").waitFor();
        return code;
    }

    private String createTaxProfile(Page page, String suffix) {
        page.navigate(routeUrl("%2Fcatalog%2Ftax-profiles", "directory"));
        requireMainHeading(page, "Perfiles tributarios");
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo perfil")),
                "new purchasing tax profile action").click();
        requireMainHeading(page, "Nuevo perfil tributario");

        String name = "Perfil compras " + suffix;
        requireOne(page.getByLabel(
                "Código interno", new Page.GetByLabelOptions().setExact(true)),
                "tax profile code").fill("PC_" + suffix);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "tax profile name").fill(name);
        requireOne(page.getByLabel(
                "Tratamiento interno", new Page.GetByLabelOptions().setExact(true)),
                "tax profile treatment").fill("TAXED_DEMO");
        requireOne(page.getByLabel("Descripción", new Page.GetByLabelOptions().setExact(true)),
                "tax profile description").fill(
                        "Perfil ficticio del escenario de compras; no representa una regla SIFEN");
        requireOne(page.getByLabel(
                "Vigente desde", new Page.GetByLabelOptions().setExact(true)),
                "tax profile validity start").fill(
                        LocalDate.now().minusDays(1) + "T00:00:00Z");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar perfil")),
                "register purchasing tax profile").click();
        requireOne(page.getByText(
                "Perfil tributario registrado", new Page.GetByTextOptions().setExact(true)),
                "purchasing tax profile confirmation").waitFor();
        return name;
    }

    private String createCatalogProduct(
            Page page, String suffix, String unitCode, String taxProfileName) {
        page.navigate(routeUrl("%2Fcatalog", "directory"));
        requireMainHeading(page, "Artículos y servicios");
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo artículo o servicio")),
                "new purchasing product action").click();

        String itemName = "Producto compra " + suffix;
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "product code").fill("PC-" + suffix);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "product name").fill(itemName);
        requireOne(page.getByLabel("Descripción", new Page.GetByLabelOptions().setExact(true)),
                "product description").fill("Producto ficticio para validar el ciclo de compras");
        requireOne(page.getByLabel("Tipo", new Page.GetByLabelOptions().setExact(true)),
                "product type").selectOption("PRODUCT");
        requireOne(page.getByLabel("Alcance", new Page.GetByLabelOptions().setExact(true)),
                "product scope").selectOption("BOTH");
        requireOne(page.getByLabel("Unidad base", new Page.GetByLabelOptions().setExact(true)),
                "product unit").selectOption(unitCode);
        requireOne(page.getByLabel("Perfil tributario", new Page.GetByLabelOptions().setExact(true)),
                "product tax profile").selectOption(
                        new SelectOption().setLabel(taxProfileName));
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar")),
                "register purchasing product").click();
        requireOne(page.getByText(
                "Artículo o servicio registrado", new Page.GetByTextOptions().setExact(true)),
                "product confirmation").waitFor();
        return itemName;
    }

    private String createWarehouse(Page page, String suffix) {
        page.navigate(routeUrl("%2Finventory%2Fwarehouses", "directory"));
        requireMainHeading(page, "Depósitos");
        requireOne(page.getByRole(
                AriaRole.LINK, new Page.GetByRoleOptions().setName("Nuevo depósito")),
                "new purchasing warehouse action").click();
        String warehouseCode = "PC-" + suffix;
        requireOne(page.getByLabel("Código", new Page.GetByLabelOptions().setExact(true)),
                "warehouse code").fill(warehouseCode);
        requireOne(page.getByLabel("Nombre", new Page.GetByLabelOptions().setExact(true)),
                "warehouse name").fill("Depósito compras " + suffix);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Crear depósito")),
                "create purchasing warehouse").click();
        requireOne(page.getByText("Depósito creado", new Page.GetByTextOptions().setExact(true)),
                "warehouse confirmation").waitFor();
        return warehouseCode;
    }

    private void enrollCatalogProduct(Page page, String itemName) {
        page.navigate(routeUrl("%2Finventory", "directory"));
        requireMainHeading(page, "Existencias");
        requireOne(page.getByLabel("Tarea", new Page.GetByLabelOptions().setExact(true)),
                "guided stock task").selectOption("ITEM_ADMIN");
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continuar")),
                "apply item administration task").click();
        Locator product = requireOne(page.getByLabel(
                "Producto de catálogo", new Page.GetByLabelOptions().setExact(true)),
                "catalog product for inventory");
        product.selectOption(optionValueContaining(product, itemName));
        requireOne(page.getByLabel("Seguimiento", new Page.GetByLabelOptions().setExact(true)),
                "purchasing product tracking").selectOption("NONE");
        requireOne(page.getByLabel("Vencimiento", new Page.GetByLabelOptions().setExact(true)),
                "purchasing product expiry").selectOption("NONE");
        page.onceDialog(dialog -> dialog.accept());
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Incorporar")),
                "enroll purchasing product").click();
        requireOne(page.getByText(
                "Artículo incorporado al inventario", new Page.GetByTextOptions().setExact(true)),
                "inventory enrollment confirmation").waitFor();
    }

    private void createAndSubmitRequest(
            Page page, String requestNumber, String itemName, String unitCode) {
        page.navigate(routeUrl("%2Fpurchasing%2Frequests", "directory"));
        assertFloorplan(page, "worklist");
        requireMainHeading(page, "Solicitudes de compra");
        assertResponsive(page, 1280, 900, "purchasing-requests-directory-expanded-1280.png");
        assertResponsive(page, 720, 900, "purchasing-requests-directory-medium-720.png");
        assertResponsive(page, 375, 900, "purchasing-requests-directory-compact-375.png");

        page.setViewportSize(1280, 900);
        requireOne(page.getByLabel("Número", new Page.GetByLabelOptions().setExact(true)),
                "request number").fill(requestNumber);
        requireOne(page.getByLabel("Fecha solicitada", new Page.GetByLabelOptions().setExact(true)),
                "request date").fill(LocalDate.now().toString());
        requireOne(page.getByLabel("Tipo de línea", new Page.GetByLabelOptions().setExact(true)),
                "request line kind").selectOption("STOCK");
        selectSearchOption(page, "Artículo o servicio", itemName, itemName);
        requireOne(page.getByLabel("Descripción", new Page.GetByLabelOptions().setExact(true)),
                "request description").fill(itemName);
        requireOne(page.getByLabel("Unidad", new Page.GetByLabelOptions().setExact(true)),
                "request unit").fill(unitCode);
        requireOne(page.getByLabel("Cantidad", new Page.GetByLabelOptions().setExact(true)),
                "request quantity").fill("10");
        requireOne(page.getByLabel("Precio esperado", new Page.GetByLabelOptions().setExact(true)),
                "request price").fill("100");
        selectSearchOption(page, "Moneda estimada", "PYG", "PYG");
        acceptNextConfirmation(page);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Preparar solicitud")),
                "create purchase request").click();
        requireOne(page.getByText("Solicitud creada", new Page.GetByTextOptions().setExact(true)),
                "request creation confirmation").waitFor();
        acceptNextConfirmation(page);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Enviar a aprobación")),
                "submit purchase request").click();
        requireOne(page.getByText(
                "Estado de la solicitud actualizado", new Page.GetByTextOptions().setExact(true)),
                "request submission confirmation").waitFor();
        assertAccessibleStructure(page);
        assertResponsive(page, 1280, 900, "purchasing-request-submitted-expanded-1280.png");
        assertResponsive(page, 720, 900, "purchasing-request-submitted-medium-720.png");
        assertResponsive(page, 375, 900, "purchasing-request-submitted-compact-375.png");
    }

    private void approveRequestWithIndependentActor(String requestNumber, String companyId) {
        try (BrowserContext approvalContext = newContext()) {
            Page page = authenticate(approvalContext, SINGLE_COMPANY_USER);
            assertEquals(companyId, enterWorkspace(page));
            assertEquals(0, page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Administración")).count(),
                    "the independent approver must not receive system administration access");
            page.navigate(routeUrl("%2Fpurchasing%2Frequests", "directory"));
            requireOne(page.getByLabel(
                    "Número o descripción", new Page.GetByLabelOptions().setExact(true)),
                    "request search").fill(requestNumber);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar").setExact(true)),
                    "search submitted request").click();
            openResult(page, requestNumber);
            acceptNextConfirmation(page);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Aprobar").setExact(true)),
                    "approve request as independent actor").click();
            requireOne(page.getByText(
                    "Estado de la solicitud actualizado", new Page.GetByTextOptions().setExact(true)),
                    "request approval confirmation").waitFor();
            requireOne(page.locator(".floorplan-output").filter(
                    new Locator.FilterOptions().setHasText("Aprobada")),
                    "approved request state").waitFor();
        }
    }

    private void createAndIssueOrder(
            Page page, String orderNumber, String supplierName, String itemName,
            String unitCode) {
        page.navigate(routeUrl("%2Fpurchasing%2Forders", "directory"));
        assertFloorplan(page, "transaction-editor");
        requireMainHeading(page, "Órdenes de compra");
        requireOne(page.getByLabel("Número", new Page.GetByLabelOptions().setExact(true)),
                "order number").fill(orderNumber);
        selectSearchOption(page, "Proveedor", supplierName, supplierName);
        selectSearchOption(page, "Moneda", "PYG", "PYG");
        requireOne(page.getByLabel(
                "Justificación de compra directa", new Page.GetByLabelOptions().setExact(true)),
                "direct purchase justification").fill("Reposición validada en la demo de Compras");
        requireOne(page.getByLabel("Tipo de línea", new Page.GetByLabelOptions().setExact(true)),
                "order line kind").selectOption("STOCK");
        selectSearchOption(page, "Artículo o servicio", itemName, itemName);
        requireOne(page.getByLabel("Descripción", new Page.GetByLabelOptions().setExact(true)),
                "order description").fill(itemName);
        requireOne(page.getByLabel("Unidad", new Page.GetByLabelOptions().setExact(true)),
                "order unit").fill(unitCode);
        requireOne(page.getByLabel("Cantidad", new Page.GetByLabelOptions().setExact(true)),
                "order quantity").fill("10");
        requireOne(page.getByLabel("Precio unitario", new Page.GetByLabelOptions().setExact(true)),
                "order price").fill("100");
        acceptNextConfirmation(page);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Preparar orden")),
                "create purchase order").click();
        requireOne(page.getByText("Orden creada", new Page.GetByTextOptions().setExact(true)),
                "order creation confirmation").waitFor();
        acceptNextConfirmation(page);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Emitir orden")),
                "issue purchase order").click();
        requireOne(page.getByText(
                "Estado de la orden actualizado", new Page.GetByTextOptions().setExact(true)),
                "order issue confirmation").waitFor();
        assertResponsive(page, 1280, 900, "purchasing-order-issued-expanded-1280.png");
        assertResponsive(page, 720, 900, "purchasing-order-issued-medium-720.png");
        assertResponsive(page, 375, 900, "purchasing-order-issued-compact-375.png");
    }

    private void createAndConfirmReceipt(
            Page page, String receiptNumber, String orderNumber, String warehouseCode) {
        page.setViewportSize(1280, 900);
        page.navigate(routeUrl("%2Fpurchasing%2Freceipts", "directory"));
        assertFloorplan(page, "guided-operation");
        requireMainHeading(page, "Recepciones de compra");
        requireOne(page.getByLabel("Número", new Page.GetByLabelOptions().setExact(true)),
                "receipt number").fill(receiptNumber);
        selectSearchOption(page, "Orden emitida", orderNumber, orderNumber);
        selectFirstNonBlank(page, "Línea ordenada");
        requireOne(page.getByLabel("Cantidad recibida", new Page.GetByLabelOptions().setExact(true)),
                "received quantity").fill("6");
        selectSearchOption(page, "Depósito", warehouseCode, warehouseCode);
        selectContaining(page, "Ubicación", "GENERAL");
        requireOne(page.getByLabel("Condición", new Page.GetByLabelOptions().setExact(true)),
                "receipt stock condition").selectOption("AVAILABLE");
        acceptNextConfirmation(page);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Preparar recepción")),
                "prepare receipt").click();
        requireOne(page.getByText(
                "Recepción preparada", new Page.GetByTextOptions().setExact(true)),
                "receipt preparation confirmation").waitFor();
        acceptNextConfirmation(page);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar recepción")),
                "confirm receipt").click();
        requireOne(page.getByText(
                "Recepción confirmada", new Page.GetByTextOptions().setExact(true)),
                "receipt confirmation").waitFor();
        assertResponsive(page, 375, 900, "purchasing-receipt-confirmed-compact-375.png");
    }

    private void createAndConfirmReturn(
            Page page, String returnNumber, String orderNumber, String receiptNumber) {
        page.setViewportSize(1280, 900);
        page.navigate(routeUrl("%2Fpurchasing%2Freturns", "directory"));
        assertFloorplan(page, "guided-operation");
        requireMainHeading(page, "Devoluciones a proveedores");
        requireOne(page.getByLabel("Número", new Page.GetByLabelOptions().setExact(true)),
                "return number").fill(returnNumber);
        selectSearchOption(page, "Orden", orderNumber, orderNumber);
        selectSearchOption(page, "Recepción confirmada", receiptNumber, receiptNumber);
        selectFirstNonBlank(page, "Línea recibida");
        requireOne(page.getByLabel("Cantidad devuelta", new Page.GetByLabelOptions().setExact(true)),
                "returned quantity").fill("2");
        requireOne(page.getByLabel("Causa", new Page.GetByLabelOptions().setExact(true)),
                "return reason").fill("Embalaje dañado en la inspección de recepción");
        acceptNextConfirmation(page);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Preparar devolución")),
                "prepare supplier return").click();
        requireOne(page.getByText(
                "Devolución preparada", new Page.GetByTextOptions().setExact(true)),
                "return preparation confirmation").waitFor();
        acceptNextConfirmation(page);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar devolución")),
                "confirm supplier return").click();
        requireOne(page.getByText(
                "Devolución confirmada", new Page.GetByTextOptions().setExact(true)),
                "return confirmation").waitFor();
        assertResponsive(page, 720, 900, "purchasing-return-confirmed-medium-720.png");
    }

    private void verifyTracking(Page page, String orderNumber) {
        page.setViewportSize(1280, 900);
        page.navigate(routeUrl("%2Fpurchasing%2Ftracking", "directory"));
        assertFloorplan(page, "inquiry");
        requireMainHeading(page, "Seguimiento de compras");
        requireOne(page.getByLabel(
                "Número o proveedor", new Page.GetByLabelOptions().setExact(true)),
                "tracking search").fill(orderNumber);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Buscar").setExact(true)),
                "search order tracking").click();
        openResult(page, orderNumber);
        requireOne(page.locator(".floorplan-output").filter(
                new Locator.FilterOptions().setHasText(orderNumber)),
                "tracking fulfillment summary").waitFor();
        String detail = page.locator("main").innerText();
        assertTrue(detail.contains("pedida 10"), "tracking must show the ordered quantity");
        assertTrue(detail.contains("recibida 6"), "tracking must show the received quantity");
        assertTrue(detail.contains("devuelta 2"), "tracking must show the returned quantity");
        assertTrue(detail.contains("pendiente 6"),
                "a confirmed return must restore the quantity pending from the supplier");
        assertAccessibleStructure(page);
        assertResponsive(page, 1280, 900, "purchasing-tracking-expanded-1280.png");
        assertResponsive(page, 720, 900, "purchasing-tracking-medium-720.png");
        assertResponsive(page, 375, 900, "purchasing-tracking-compact-375.png");
        for (int boundary : List.of(599, 600, 839, 840)) {
            assertResponsiveLayout(page, boundary, "purchasing-tracking-boundary-" + boundary);
        }
    }

    private Page authenticate(BrowserContext context, String username) {
        Page page = context.newPage();
        page.navigate(appUrl);
        requireOne(page.getByLabel("Username"), "Keycloak username").fill(username);
        requireOne(page.locator("input[type='password']"), "Keycloak password").fill(password);
        requireOne(page.getByRole(
                AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign In")),
                "Keycloak sign in").click();
        page.waitForURL(appUrl);
        page.locator("main").waitFor(new Locator.WaitForOptions().setTimeout(30_000));
        return page;
    }

    private String enterWorkspace(Page page) {
        return enterWorkspace(page, "");
    }

    private String enterWorkspace(Page page, String preferredCompanyId) {
        Locator selector = page.getByLabel("Empresa autorizada");
        String companyId = "";
        if (selector.count() == 1) {
            companyId = preferredCompanyId.isBlank()
                    ? firstNonBlankOptionValue(selector, "authorized company")
                    : preferredCompanyId;
            selector.selectOption(companyId);
            requireOne(page.getByRole(
                    AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continuar")),
                    "continue company selection").click();
        }
        Locator workspace = page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Funciones disponibles"));
        workspace.waitFor();
        requireOne(workspace, "authorized workspace");
        if (companyId.isBlank()) {
            Locator companySwitcher = page.getByLabel("Cambiar empresa");
            if (companySwitcher.count() == 1) {
                companyId = companySwitcher.inputValue();
            }
        }
        if (companyId.isBlank()) {
            companyId = requireOne(
                    page.locator(".identity-copy[data-company-id]"),
                    "validated company context").getAttribute("data-company-id");
        }
        if (companyId.isBlank()) {
            Locator administration = page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName("Administración"));
            if (administration.count() == 1) {
                companyId = queryParameter(administration.getAttribute("href"), "company");
            }
        }
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
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions().setName("Habilitar").setExact(true));
            if (enable.count() == 1) {
                enable.click();
                card = pluginCard(page, pluginId);
            }
            requireOne(card.getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions().setName("Deshabilitar").setExact(true)),
                    pluginId + " enabled state").waitFor();
        }
    }

    private void grantRequiredPermissions(Page page, String companyId) {
        String securityUrl = adminUrl.replace("index.xhtml", "security.xhtml")
                + "?company=" + companyId;
        for (String permission : REQUIRED_PERMISSIONS) {
            page.navigate(securityUrl);
            Locator form = requireOne(page.locator("#grant-permission-form"),
                    "grant permission form");
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

    private void assertMergedMenus(Page page) {
        for (String menu : List.of(
                "Socios comerciales", "Artículos y servicios", "Existencias", "Depósitos",
                "Solicitudes de compra", "Órdenes de compra", "Recepciones", "Devoluciones",
                "Seguimiento")) {
            requireOne(page.getByRole(
                    AriaRole.LINK, new Page.GetByRoleOptions().setName(menu)),
                    menu + " merged menu");
        }
        assertResponsive(page, 1280, 900, "purchasing-merged-workspace-expanded-1280.png");
        assertResponsive(page, 720, 900, "purchasing-merged-workspace-medium-720.png");
        assertResponsive(page, 375, 900, "purchasing-merged-workspace-compact-375.png");
    }

    private void verifyDisabledPurchasingIsDeniedAndRestore(Page page, String companyId) {
        String pluginsUrl = pluginsUrl(companyId);
        String purchasingUrl = routeUrl("%2Fpurchasing%2Frequests", "directory");
        boolean disabled = false;
        try {
            page.setViewportSize(1280, 900);
            page.navigate(pluginsUrl);
            Locator card = pluginCard(page, "purchasing");
            page.onceDialog(dialog -> dialog.accept());
            requireOne(card.getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions().setName("Deshabilitar").setExact(true)),
                    "disable purchasing").click();
            requireOne(pluginCard(page, "purchasing").getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions().setName("Habilitar").setExact(true)),
                    "disabled purchasing state").waitFor();
            disabled = true;
            page.navigate(purchasingUrl);
            requireOne(page.getByRole(
                    AriaRole.HEADING, new Page.GetByRoleOptions().setName(
                            "Esta función no está disponible para tu contexto actual")),
                    "disabled purchasing denial").waitFor();
            assertResponsive(page, 375, 900, "purchasing-disabled-denial-compact-375.png");

            page.navigate(pluginsUrl);
            card = pluginCard(page, "purchasing");
            requireOne(card.getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions().setName("Habilitar").setExact(true)),
                    "restore purchasing").click();
            requireOne(pluginCard(page, "purchasing").getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions().setName("Deshabilitar").setExact(true)),
                    "restored purchasing state").waitFor();
            disabled = false;
        } finally {
            if (disabled) {
                page.navigate(pluginsUrl);
                Locator enable = pluginCard(page, "purchasing").getByRole(
                        AriaRole.BUTTON,
                        new Locator.GetByRoleOptions().setName("Habilitar").setExact(true));
                if (enable.count() == 1) {
                    enable.click();
                    requireOne(pluginCard(page, "purchasing").getByRole(
                            AriaRole.BUTTON,
                            new Locator.GetByRoleOptions()
                                    .setName("Deshabilitar").setExact(true)),
                            "restored purchasing state after recovery").waitFor();
                }
            }
        }
    }

    private String selectSearchOption(
            Page page, String fieldLabel, String query, String expectedOptionText) {
        String searchLabel = "Buscar opciones de " + fieldLabel;
        Locator search = requireOne(page.getByLabel(searchLabel),
                "search-on-demand input for " + fieldLabel);
        search.fill(query);
        Locator field = selectorField(search);
        requireOne(field.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Buscar opciones").setExact(true)),
                "search-on-demand action for " + fieldLabel).click();

        Locator searchAfterQuery = page.getByLabel(searchLabel);
        searchAfterQuery.waitFor();
        search = requireOne(searchAfterQuery,
                "search-on-demand input after search for " + fieldLabel);
        field = selectorField(search);
        Locator matchingOption = field.locator(".selector-option-button").filter(
                new Locator.FilterOptions().setHasText(expectedOptionText));
        matchingOption.waitFor();
        Locator option = requireOne(matchingOption,
                "search-on-demand option for " + fieldLabel);
        option.click();

        Locator searchAfterSelection = page.getByLabel(searchLabel);
        searchAfterSelection.waitFor();
        search = requireOne(searchAfterSelection,
                "search-on-demand input after selection for " + fieldLabel);
        field = selectorField(search);
        String value = requireOne(field.locator(".selector-current-value strong"),
                "selected search-on-demand value for " + fieldLabel).innerText().strip();
        assertTrue(!value.isBlank() && !"Ninguna".equals(value),
                "the search-on-demand selection must update " + fieldLabel);
        return value;
    }

    private static Locator selectorField(Locator search) {
        return search.locator(
                "xpath=ancestor::*[contains(concat(' ', normalize-space(@class), ' '),"
                        + " ' selector-search-field ')][1]");
    }

    private static void openResult(Page page, String expectedText) {
        Locator row = requireOne(page.locator("tbody tr").filter(
                new Locator.FilterOptions().setHasText(expectedText)),
                "result row for " + expectedText);
        requireOne(row.getByRole(
                AriaRole.LINK, new Locator.GetByRoleOptions().setName("Abrir")),
                "open result for " + expectedText).click();
    }

    private static void selectFirstNonBlank(Page page, String label) {
        Locator select = requireOne(page.getByLabel(
                label, new Page.GetByLabelOptions().setExact(true)), label + " select");
        select.selectOption(firstNonBlankOptionValue(select, label));
    }

    private static void selectContaining(Page page, String label, String expectedText) {
        Locator select = requireOne(page.getByLabel(
                label, new Page.GetByLabelOptions().setExact(true)), label + " select");
        select.selectOption(optionValueContaining(select, expectedText));
    }

    private Locator pluginCard(Page page, String pluginId) {
        return requireOne(page.locator("article.plugin-record-card").filter(
                new Locator.FilterOptions().setHasText(pluginId)), pluginId + " plugin card");
    }

    private String pluginsUrl(String companyId) {
        return adminUrl.replace("index.xhtml", "plugins.xhtml") + "?company=" + companyId;
    }

    private String routeUrl(String encodedRoute, String mode) {
        return appUrl.replace("index.xhtml", "view.xhtml?route=" + encodedRoute + "&mode=" + mode);
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
        String overflowSources = (String) page.evaluate("""
                () => Array.from(document.querySelectorAll('body *'))
                  .map(element => {
                    const bounds = element.getBoundingClientRect();
                    return {
                      tag: element.tagName.toLowerCase(),
                      id: element.id,
                      classes: Array.from(element.classList).join('.'),
                      left: Math.round(bounds.left),
                      right: Math.round(bounds.right),
                      width: Math.round(bounds.width)
                    };
                  })
                  .filter(item => item.right > window.innerWidth + 1 || item.left < -1)
                  .sort((left, right) => right.right - left.right)
                  .slice(0, 12)
                  .map(item => `${item.tag}${item.id ? '#' + item.id : ''}`
                    + `${item.classes ? '.' + item.classes : ''}`
                    + ` [${item.left},${item.right}; ${item.width}px]`)
                  .join(' | ')
                """);
        assertTrue(stylesLoaded,
                description + " must load the application Material Design stylesheets");
        assertTrue(documentWidth <= width + 1,
                description + " has horizontal overflow: document="
                        + documentWidth + " px, viewport=" + width + " px; sources="
                        + overflowSources);
    }

    private static void assertAccessibleStructure(Page page) {
        assertEquals(1, page.locator("main h1").count(),
                "the purchasing screen must expose exactly one main heading");
        boolean everyEditableControlHasLabel = (Boolean) page.evaluate("""
                () => Array.from(document.querySelectorAll(
                    "input:not([type='hidden']):not([type='submit']):not([type='button']), select, textarea"))
                  .every(control => control.labels && control.labels.length > 0)
                """);
        boolean everyActionHasAccessibleName = (Boolean) page.evaluate("""
                () => Array.from(document.querySelectorAll(
                    "button:not([aria-hidden='true']), input[type='button']:not([aria-hidden='true']), input[type='submit']:not([aria-hidden='true'])"))
                  .every(control => (control.getAttribute('aria-label')
                    || control.value || control.textContent || '').trim().length > 0)
                """);
        assertTrue(everyEditableControlHasLabel,
                "every editable purchasing control must have a label");
        assertTrue(everyActionHasAccessibleName,
                "every purchasing action must expose an accessible name");
        assertReducedMotionAndKeyboardFocus(page, "purchasing");
    }

    private static void assertReducedMotionAndKeyboardFocus(Page page, String journey) {
        boolean reducedMotionActive = (Boolean) page.evaluate(
                "() => window.matchMedia('(prefers-reduced-motion: reduce)').matches");
        String transitionDuration = (String) page.evaluate(
                "() => getComputedStyle(document.body).transitionDuration");
        assertTrue(reducedMotionActive,
                journey + " journey must execute with the reduced-motion preference");
        assertEquals("0s", transitionDuration,
                journey + " must remove non-essential transitions");

        page.evaluate("() => document.activeElement && document.activeElement.blur()");
        page.keyboard().press("Tab");
        boolean keyboardFocusVisible = (Boolean) page.evaluate("""
                () => document.activeElement
                  && document.activeElement !== document.body
                  && document.activeElement.matches(':focus-visible')
                """);
        assertTrue(keyboardFocusVisible,
                journey + " must expose a visible focus target during keyboard navigation");
    }

    private static void assertFloorplan(Page page, String expectedCode) {
        requireOne(page.locator("main .floorplan-" + expectedCode),
                expectedCode + " floorplan").waitFor();
        requireOne(page.getByText("Contrato 2.0.0", new Page.GetByTextOptions().setExact(true)),
                expectedCode + " contract version").waitFor();
        assertEquals(0, page.locator(".screen-mode-tabs").count(),
                "v2 purchasing screens must not render legacy mode tabs");
    }

    private static void acceptNextConfirmation(Page page) {
        page.onceDialog(dialog -> dialog.accept());
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

    private static String queryParameter(String url, String name) {
        if (url == null) {
            throw new AssertionError("administration link must contain the active company");
        }
        String marker = name + "=";
        int start = url.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("missing " + name + " in " + url);
        }
        int valueStart = start + marker.length();
        int end = url.indexOf('&', valueStart);
        return url.substring(valueStart, end < 0 ? url.length() : end);
    }

    private static Locator requireOne(Locator locator, String description) {
        locator.waitFor();
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
