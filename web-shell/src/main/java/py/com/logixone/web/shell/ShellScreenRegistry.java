package py.com.logixone.web.shell;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashMap;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.company.screen.ComposedScreenElement;
import py.com.logixone.kernel.application.company.screen.ComposedSlotContent;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenFragmentId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.ScreenSlotId;

/** Closed mapping from public screen contracts to shell-owned JSF presentations. */
@ApplicationScoped
public class ShellScreenRegistry {

    private static final PluginId REFERENCE_PLUGIN = new PluginId("reference_plugin");
    private static final ScreenId REFERENCE_SCREEN = new ScreenId(REFERENCE_PLUGIN, "dashboard");
    private static final PluginId REFERENCE_DATA_PLUGIN = new PluginId("reference_data");
    private static final ScreenId REFERENCE_DATA_CATALOGS =
            new ScreenId(REFERENCE_DATA_PLUGIN, "catalogs");
    private static final PluginId BUSINESS_PARTNERS_PLUGIN = new PluginId("business_partners");
    private static final ScreenId BUSINESS_PARTNERS_SCREEN =
            new ScreenId(BUSINESS_PARTNERS_PLUGIN, "directory");
    private static final ScreenId BUSINESS_PARTNERS_DEFINITIONS =
            new ScreenId(BUSINESS_PARTNERS_PLUGIN, "definitions");
    private static final PluginId COMMERCIAL_CATALOG_PLUGIN = new PluginId("commercial_catalog");
    private static final ScreenId COMMERCIAL_CATALOG_ITEMS =
            new ScreenId(COMMERCIAL_CATALOG_PLUGIN, "items");
    private static final ScreenId COMMERCIAL_CATALOG_PRICE_LISTS =
            new ScreenId(COMMERCIAL_CATALOG_PLUGIN, "price_lists");
    private static final ScreenId COMMERCIAL_CATALOG_DEFINITIONS =
            new ScreenId(COMMERCIAL_CATALOG_PLUGIN, "definitions");
    private static final ScreenId COMMERCIAL_CATALOG_VARIANT_FAMILIES =
            new ScreenId(COMMERCIAL_CATALOG_PLUGIN, "variant_families");
    private static final ScreenId COMMERCIAL_CATALOG_TAX_PROFILES =
            new ScreenId(COMMERCIAL_CATALOG_PLUGIN, "tax_profiles");
    private static final PluginId INVENTORY_PLUGIN = new PluginId("inventory");
    private static final ScreenId INVENTORY_STOCK = new ScreenId(INVENTORY_PLUGIN, "stock");
    private static final ScreenId INVENTORY_WAREHOUSES =
            new ScreenId(INVENTORY_PLUGIN, "warehouses");
    private static final ScreenId INVENTORY_COUNTS = new ScreenId(INVENTORY_PLUGIN, "counts");
    private static final ScreenRegionId MAIN_REGION = new ScreenRegionId("main");
    private static final ScreenRegionId ACTIONS_REGION = new ScreenRegionId("actions");
    private static final ScreenSlotId EXTENSIONS_SLOT = new ScreenSlotId("dashboard_extensions");
    private static final Set<ScreenElementType> SUPPORTED_TYPES = EnumSet.allOf(ScreenElementType.class);

    private static final Map<RouteKey, ScreenId> ROUTES = Map.ofEntries(
            Map.entry(new RouteKey(REFERENCE_PLUGIN, "/reference"), REFERENCE_SCREEN),
            Map.entry(new RouteKey(REFERENCE_DATA_PLUGIN, "/reference-data"),
                    REFERENCE_DATA_CATALOGS),
            Map.entry(new RouteKey(BUSINESS_PARTNERS_PLUGIN, "/business-partners"), BUSINESS_PARTNERS_SCREEN),
            Map.entry(new RouteKey(BUSINESS_PARTNERS_PLUGIN, "/business-partners/definitions"),
                    BUSINESS_PARTNERS_DEFINITIONS),
            Map.entry(new RouteKey(COMMERCIAL_CATALOG_PLUGIN, "/catalog"), COMMERCIAL_CATALOG_ITEMS),
            Map.entry(new RouteKey(COMMERCIAL_CATALOG_PLUGIN, "/catalog/price-lists"),
                    COMMERCIAL_CATALOG_PRICE_LISTS),
            Map.entry(new RouteKey(COMMERCIAL_CATALOG_PLUGIN, "/catalog/definitions"),
                    COMMERCIAL_CATALOG_DEFINITIONS),
            Map.entry(new RouteKey(COMMERCIAL_CATALOG_PLUGIN, "/catalog/variant-families"),
                    COMMERCIAL_CATALOG_VARIANT_FAMILIES),
            Map.entry(new RouteKey(COMMERCIAL_CATALOG_PLUGIN, "/catalog/tax-profiles"),
                    COMMERCIAL_CATALOG_TAX_PROFILES),
            Map.entry(new RouteKey(INVENTORY_PLUGIN, "/inventory"), INVENTORY_STOCK),
            Map.entry(new RouteKey(INVENTORY_PLUGIN, "/inventory/warehouses"), INVENTORY_WAREHOUSES),
            Map.entry(new RouteKey(INVENTORY_PLUGIN, "/inventory/counts"), INVENTORY_COUNTS));

    private static final Map<ScreenFragmentId, FragmentPresentation> FRAGMENTS = Map.of(
            new ScreenFragmentId(new PluginId("reference_custom_a"), "tax_notice"),
            new FragmentPresentation(
                    "Capa empresarial A",
                    "Validación tributaria destacada",
                    "Esta tarjeta fue insertada por el plugin exclusivo de la empresa A mediante un slot público.",
                    "screen-fragment-a"),
            new ScreenFragmentId(new PluginId("reference_custom_b"), "company_notice"),
            new FragmentPresentation(
                    "Capa empresarial B",
                    "Operación simplificada",
                    "La empresa B oculta el campo de resumen y muestra este aviso propio sin reemplazar la pantalla base.",
                    "screen-fragment-b"));

    public Optional<ScreenId> screenFor(PluginId pluginId, String route) {
        return Optional.ofNullable(ROUTES.get(new RouteKey(pluginId, route)));
    }

    public Optional<ShellScreenView> render(
            ComposedScreen screen,
            ShellTextCatalog textCatalog) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(textCatalog, "textCatalog");
        if (REFERENCE_SCREEN.equals(screen.id())) {
            return renderReference(screen, textCatalog);
        }
        if (REFERENCE_DATA_CATALOGS.equals(screen.id())) {
            return renderEntity(screen, textCatalog, referenceDataSpec());
        }
        if (BUSINESS_PARTNERS_SCREEN.equals(screen.id())) {
            return renderEntity(screen, textCatalog, businessPartnersSpec());
        }
        if (BUSINESS_PARTNERS_DEFINITIONS.equals(screen.id())) {
            return renderEntity(screen, textCatalog, businessPartnerDefinitionsSpec());
        }
        if (COMMERCIAL_CATALOG_ITEMS.equals(screen.id())) {
            return renderEntity(screen, textCatalog, catalogItemsSpec());
        }
        if (COMMERCIAL_CATALOG_PRICE_LISTS.equals(screen.id())) {
            return renderEntity(screen, textCatalog, catalogPriceListsSpec());
        }
        if (COMMERCIAL_CATALOG_DEFINITIONS.equals(screen.id())) {
            return renderEntity(screen, textCatalog, catalogDefinitionsSpec());
        }
        if (COMMERCIAL_CATALOG_VARIANT_FAMILIES.equals(screen.id())) {
            return renderEntity(screen, textCatalog, catalogVariantFamiliesSpec());
        }
        if (COMMERCIAL_CATALOG_TAX_PROFILES.equals(screen.id())) {
            return renderEntity(screen, textCatalog, catalogTaxProfilesSpec());
        }
        if (INVENTORY_STOCK.equals(screen.id())) {
            return renderEntity(screen, textCatalog, inventoryStockSpec());
        }
        if (INVENTORY_WAREHOUSES.equals(screen.id())) {
            return renderEntity(screen, textCatalog, inventoryWarehousesSpec());
        }
        if (INVENTORY_COUNTS.equals(screen.id())) {
            return renderEntity(screen, textCatalog, inventoryCountsSpec());
        }
        return Optional.empty();
    }

    private Optional<ShellScreenView> renderReference(
            ComposedScreen screen,
            ShellTextCatalog textCatalog) {
        if (!validReferenceSlots(screen.slots())) {
            return Optional.empty();
        }

        List<ShellScreenElementView> main = new ArrayList<>();
        List<ShellScreenElementView> actions = new ArrayList<>();
        int hidden = 0;
        for (ComposedScreenElement element : screen.elements()) {
            if (!SUPPORTED_TYPES.contains(element.type()) || !validRegion(element)) {
                return Optional.empty();
            }
            Optional<String> label = textCatalog.screenText(element.labelKey());
            Optional<String> help = element.helpKey().isEmpty()
                    ? Optional.empty()
                    : textCatalog.screenText(element.helpKey().orElseThrow());
            if (label.isEmpty() || (element.helpKey().isPresent() && help.isEmpty())) {
                return Optional.empty();
            }
            if (!element.visible()) {
                hidden++;
                continue;
            }
            ShellScreenElementView view = new ShellScreenElementView(
                    element.id().value(),
                    element.type(),
                    label.orElseThrow(),
                    help,
                    element.enabled(),
                    element.required());
            if (element.regionId().equals(MAIN_REGION)) {
                main.add(view);
            } else {
                actions.add(view);
            }
        }

        List<ShellScreenFragmentView> fragments = new ArrayList<>();
        for (ComposedSlotContent content : screen.slotContents()) {
            if (!content.slotId().equals(EXTENSIONS_SLOT)) {
                return Optional.empty();
            }
            FragmentPresentation presentation = FRAGMENTS.get(content.fragmentId());
            if (presentation == null) {
                return Optional.empty();
            }
            fragments.add(new ShellScreenFragmentView(
                    content.fragmentId().toString(),
                    presentation.eyebrow(),
                    presentation.title(),
                    presentation.body(),
                    presentation.toneClass()));
        }

        Variant variant = variant(fragments);
        return Optional.of(new ShellScreenView(
                screen.id().toString(),
                screen.contractVersion().toString(),
                "Contrato neutral renderizado",
                "Panel de composición empresarial",
                "La vista fue construida por Jakarta Faces desde un ComposedScreen autorizado y sin cargar vistas del plugin.",
                variant.label(),
                variant.toneClass(),
                main,
                actions,
                fragments,
                hidden));
    }

    private Optional<ShellScreenView> renderEntity(
            ComposedScreen screen,
            ShellTextCatalog textCatalog,
            EntityScreenSpec spec) {
        if (!validEntitySlots(screen.slots()) || !screen.slotContents().isEmpty()) {
            return Optional.empty();
        }

        Map<String, List<ShellScreenElementView>> regions = new LinkedHashMap<>();
        int hidden = 0;
        for (ComposedScreenElement element : screen.elements()) {
            if (!SUPPORTED_TYPES.contains(element.type()) || !validEntityRegion(element, spec)) {
                return Optional.empty();
            }
            Optional<String> label = textCatalog.screenText(element.labelKey());
            Optional<String> help = element.helpKey().isEmpty()
                    ? Optional.empty()
                    : textCatalog.screenText(element.helpKey().orElseThrow());
            if (label.isEmpty() || (element.helpKey().isPresent() && help.isEmpty())) {
                return Optional.empty();
            }
            if (!element.visible()) {
                hidden++;
                continue;
            }
            regions.computeIfAbsent(element.regionId().value(), ignored -> new ArrayList<>())
                    .add(new ShellScreenElementView(
                            element.id().value(),
                            element.type(),
                            label.orElseThrow(),
                            help,
                            element.enabled(),
                            element.required()));
        }

        ShellScreenElementView table = single(regions, "results", ScreenElementType.DATA_TABLE);
        ShellScreenElementView rowAction = single(regions, "row_actions", ScreenElementType.ACTION);
        if (table == null && !regions.getOrDefault("results", List.of()).isEmpty()) {
            return Optional.empty();
        }
        if (rowAction == null && !regions.getOrDefault("row_actions", List.of()).isEmpty()) {
            return Optional.empty();
        }

        List<ShellScreenSectionView> directory = directorySpecs(screen.id()).stream()
                .map(sectionSpec -> section(regions, sectionSpec))
                .toList();
        List<ShellScreenSectionView> detail = detailSpecs(screen.id()).stream()
                .map(sectionSpec -> section(regions, sectionSpec))
                .toList();

        return Optional.of(new ShellScreenView(
                screen.id().toString(),
                screen.contractVersion().toString(),
                "Plugin productivo",
                spec.title(),
                spec.description(),
                "Contrato interactivo 1.0",
                "screen-variant-business",
                List.of(),
                List.of(),
                List.of(),
                hidden,
                true,
                directory,
                detail,
                spec.detailTabs().stream()
                        .map(tab -> new ShellDetailTabView(tab.id(), tab.label()))
                        .toList(),
                table,
                rowAction,
                spec.presentation()));
    }

    private static ShellScreenSectionView section(
            Map<String, List<ShellScreenElementView>> regions,
            SectionSpec spec) {
        return new ShellScreenSectionView(
                spec.fieldsRegion(),
                spec.tabId(),
                spec.title(),
                spec.description(),
                regions.getOrDefault(spec.fieldsRegion(), List.of()),
                regions.getOrDefault(spec.actionsRegion(), List.of()));
    }

    private static ShellScreenElementView single(
            Map<String, List<ShellScreenElementView>> regions,
            String region,
            ScreenElementType type) {
        List<ShellScreenElementView> elements = regions.getOrDefault(region, List.of());
        if (elements.isEmpty()) {
            return null;
        }
        if (elements.size() != 1) {
            return null;
        }
        ShellScreenElementView element = elements.getFirst();
        boolean valid = switch (type) {
            case DATA_TABLE -> element.isDataTable();
            case ACTION -> element.isAction();
            default -> false;
        };
        return valid ? element : null;
    }

    private static boolean validReferenceSlots(List<ScreenSlotDefinition> slots) {
        return slots.size() == 1
                && slots.getFirst().id().equals(EXTENSIONS_SLOT)
                && slots.getFirst().regionId().equals(MAIN_REGION);
    }

    private static boolean validRegion(ComposedScreenElement element) {
        return switch (element.type()) {
            case DISPLAY_TEXT, TEXT_INPUT -> element.regionId().equals(MAIN_REGION);
            case ACTION -> element.regionId().equals(ACTIONS_REGION);
            case SELECT, DATA_TABLE -> false;
        };
    }

    private static boolean validEntitySlots(List<ScreenSlotDefinition> slots) {
        if (slots.size() != 2) {
            return false;
        }
        return slots.stream().anyMatch(slot ->
                        slot.id().value().equals("directory_extensions")
                                && slot.regionId().value().equals("directory_extensions")
                                && slot.maxContents() == 2)
                && slots.stream().anyMatch(slot ->
                        slot.id().value().equals("detail_extensions")
                                && slot.regionId().value().equals("detail_extensions")
                                && slot.maxContents() == 2);
    }

    private static boolean validEntityRegion(
            ComposedScreenElement element, EntityScreenSpec spec) {
        String region = element.regionId().value();
        return switch (element.type()) {
            case TEXT_INPUT, SELECT, DISPLAY_TEXT -> spec.fieldRegions().contains(region);
            case DATA_TABLE -> region.equals("results") || spec.fieldRegions().contains(region);
            case ACTION -> region.equals("row_actions") || spec.actionRegions().contains(region);
        };
    }

    private static List<SectionSpec> directorySpecs(ScreenId screenId) {
        if (REFERENCE_DATA_CATALOGS.equals(screenId)) {
            return List.of(sectionSpec(
                    "search",
                    "search_actions",
                    "search",
                    "Buscar datos de referencia",
                    "Filtra una publicación corriente por nombre o código, con hasta 50 filas por página."));
        }
        if (BUSINESS_PARTNERS_SCREEN.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar socios comerciales",
                            "Filtra por nombre, código, identificación, rol o estado."),
                    sectionSpec("create", "create_actions", "create", "Registrar socio comercial",
                            "El código puede quedar vacío para usar la secuencia transaccional."));
        }
        if (BUSINESS_PARTNERS_DEFINITIONS.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar definiciones",
                            "Selecciona la clase y filtra por código, nombre o estado."),
                    sectionSpec("create", "create_actions", "create", "Registrar definición",
                            "Crea un valor reutilizable en la clase y empresa activas."));
        }
        if (COMMERCIAL_CATALOG_ITEMS.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar artículos y servicios",
                            "Filtra por nombre, código, identificador, tipo o estado."),
                    sectionSpec("create", "create_actions", "create", "Registrar concepto comercial",
                            "El código puede quedar vacío para utilizar la secuencia transaccional."));
        }
        if (COMMERCIAL_CATALOG_PRICE_LISTS.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar listas de precios",
                            "Filtra por nombre, código o estado."),
                    sectionSpec("create", "create_actions", "create", "Registrar lista de precios",
                            "Moneda, impuestos y redondeo quedan definidos para todas sus entradas."));
        }
        if (COMMERCIAL_CATALOG_DEFINITIONS.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar definiciones",
                            "Filtra unidades, categorías, marcas y etiquetas por código, nombre, tipo o estado."),
                    sectionSpec("create", "create_actions", "create", "Registrar definición",
                            "El tipo determina si se utilizan decimales o una categoría superior."));
        }
        if (COMMERCIAL_CATALOG_VARIANT_FAMILIES.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar familias de variantes",
                            "Filtra por código, nombre o estado dentro de la empresa activa."),
                    sectionSpec("create", "create_actions", "create", "Registrar familia de variantes",
                            "Define la familia y construye sus atributos en el orden en que se utilizarán."));
        }
        if (COMMERCIAL_CATALOG_TAX_PROFILES.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar perfiles tributarios",
                            "Filtra por código, nombre, tratamiento interno o estado."),
                    sectionSpec("create", "create_actions", "create", "Registrar perfil tributario",
                            "Crea una definición interna versionada; no configura SIFEN."));
        }
        if (INVENTORY_STOCK.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar existencias",
                            "Filtra productos incorporados al inventario por nombre, código o estado."),
                    sectionSpec("create", "create_actions", "create", "Incorporar producto",
                            "Selecciona un producto activo y define sus reglas de seguimiento."));
        }
        if (INVENTORY_WAREHOUSES.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar depósitos",
                            "Filtra por código, nombre o estado dentro de la empresa activa."),
                    sectionSpec("create", "create_actions", "create", "Crear depósito",
                            "El alta crea automáticamente una ubicación general no eliminable."));
        }
        if (INVENTORY_COUNTS.equals(screenId)) {
            return List.of(
                    sectionSpec("search", "search_actions", "search", "Buscar conteos físicos",
                            "Filtra por etapa del flujo de conteo y revisión."),
                    sectionSpec("create", "create_actions", "create", "Preparar conteo",
                            "Define un depósito completo o limita el alcance a una ubicación."));
        }
        throw new IllegalArgumentException("Unsupported entity screen");
    }

    private static List<SectionSpec> detailSpecs(ScreenId screenId) {
        if (REFERENCE_DATA_CATALOGS.equals(screenId)) {
            return List.of(
                    sectionSpec("history", "history_actions", "history",
                            "Historial empresarial",
                            "Consulta cambios preservados sin alterar la publicación normativa."),
                    sectionSpec("policy", "policy_actions", "lifecycle",
                            "Disponibilidad para nuevas operaciones",
                            "Habilitar o inhabilitar conserva documentos y referencias históricas."));
        }
        if (BUSINESS_PARTNERS_SCREEN.equals(screenId)) {
            return List.of(
                    sectionSpec("code", "code_actions", "general", "Código",
                            "Actualiza el código visible sin cambiar la identidad técnica."),
                    sectionSpec("names", "names_actions", "general", "Nombres",
                            "Conserva nombre visible, legal y comercial de forma independiente."),
                    sectionSpec("identification", "identification_actions", "identifications", "Identificación",
                            "Agrega una identificación; coincidencias posibles se informan sin fusionar."),
                    sectionSpec("address", "address_actions", "addresses", "Dirección",
                            "Registra una ubicación del socio comercial."),
                    sectionSpec("channel", "channel_actions", "contacts", "Canal general",
                            "Agrega correo, teléfono, WhatsApp o sitio web."),
                    sectionSpec("contact", "contact_actions", "contacts", "Contacto",
                            "Registra una persona de contacto liviana, no otro socio comercial."),
                    sectionSpec("role", "role_actions", "roles", "Roles comerciales",
                            "Cliente y proveedor pueden coexistir y tener estados independientes."),
                    sectionSpec("lifecycle", "lifecycle_actions", "roles", "Ciclo de vida",
                            "Inactivar preserva identidad, relaciones e historia."));
        }
        if (BUSINESS_PARTNERS_DEFINITIONS.equals(screenId)) {
            return List.of(
                    sectionSpec("history", "history_actions", "history",
                            "Historial de revisiones",
                            "Consulta versiones preservadas sin modificar la identidad estable."),
                    sectionSpec("revision", "revision_actions", "revision",
                            "Nueva revisión",
                            "Actualiza el nombre visible sin cambiar el código estable."),
                    sectionSpec("lifecycle", "lifecycle_actions", "lifecycle",
                            "Ciclo de vida",
                            "Inactivar conserva identidad y referencias; reactivar exige la versión vigente."));
        }
        if (COMMERCIAL_CATALOG_ITEMS.equals(screenId)) {
            return List.of(
                    sectionSpec("general", "general_actions", "general", "Datos generales",
                            "Actualiza código, nombre, descripción y alcances sin cambiar el tipo."),
                    sectionSpec("identifiers", "identifiers_actions", "identifiers", "Identificadores",
                            "Agrega códigos alternativos o escaneables sin reemplazar el código empresarial."),
                    sectionSpec("classification", "classification_actions", "classification", "Clasificación",
                            "Asigna categoría principal y marca desde definiciones activas."),
                    sectionSpec("units", "units_actions", "units", "Unidades y conversiones",
                            "Convierte una unidad alternativa a la unidad base para compra o venta."),
                    sectionSpec("tax", "tax_actions", "tax", "Impuestos",
                            "Asigna un perfil tributario interno versionado."),
                    sectionSpec("variants", "variants_actions", "variants", "Familia y valores",
                            "Asigna una familia activa y valores que respetan su revisión vigente."),
                    sectionSpec("lifecycle", "lifecycle_actions", "lifecycle", "Ciclo de vida",
                            "Inactivar conserva referencias, precios e historia."));
        }
        if (COMMERCIAL_CATALOG_PRICE_LISTS.equals(screenId)) {
            return List.of(
                    sectionSpec("general", "general_actions", "general", "Datos generales",
                            "Actualiza el nombre visible sin alterar moneda o política histórica."),
                    sectionSpec("entries", "entries_actions", "entries", "Entradas de precio",
                            "Agrega precios efectivos o inactiva una entrada conservando historia."),
                    sectionSpec("lifecycle", "lifecycle_actions", "lifecycle", "Ciclo de vida",
                            "Inactivar la lista conserva todas sus entradas."));
        }
        if (COMMERCIAL_CATALOG_DEFINITIONS.equals(screenId)) {
            return List.of(
                    sectionSpec("history", "history_actions", "history",
                            "Historial de revisiones",
                            "Consulta nombres, estructura y estados preservados sin modificar la definición."),
                    sectionSpec("revision", "revision_actions", "revision",
                            "Nueva revisión",
                            "Actualiza nombre y configuración aplicable sin cambiar código ni identidad."),
                    sectionSpec("replacement", "replacement_actions", "replacement",
                            "Reemplazar definición",
                            "Crea una sucesora y conserva la identidad anterior para referencias históricas."),
                    sectionSpec("lifecycle", "lifecycle_actions", "lifecycle",
                            "Ciclo de vida",
                            "Inactivar conserva identidad y referencias; reactivar vuelve a validar la versión vigente."));
        }
        if (COMMERCIAL_CATALOG_VARIANT_FAMILIES.equals(screenId)) {
            return List.of(
                    sectionSpec("history", "history_actions", "history",
                            "Historial de revisiones",
                            "Consulta nombres, estados y estructuras preservadas sin modificar la familia."),
                    sectionSpec("revision", "revision_actions", "revision",
                            "Nueva revisión",
                            "Reemplaza nombre y estructura sin cambiar el código ni la identidad."),
                    sectionSpec("lifecycle", "lifecycle_actions", "lifecycle",
                            "Ciclo de vida",
                            "Inactivar conserva atributos y referencias; reactivar exige la versión vigente."));
        }
        if (COMMERCIAL_CATALOG_TAX_PROFILES.equals(screenId)) {
            return List.of(
                    sectionSpec("history", "history_actions", "history",
                            "Historial de revisiones",
                            "Consulta versiones y vigencias preservadas sin modificar el perfil."),
                    sectionSpec("revision", "revision_actions", "revision",
                            "Nueva revisión",
                            "Actualiza tratamiento y vigencia sin cambiar el código ni la identidad del perfil."),
                    sectionSpec("lifecycle", "lifecycle_actions", "lifecycle",
                            "Ciclo de vida",
                            "Inactivar conserva revisiones y referencias; reactivar exige la versión vigente."));
        }
        if (INVENTORY_STOCK.equals(screenId)) {
            return List.of(
                    sectionSpec("availability", "availability_actions", "availability", "Disponibilidad",
                            "Consulta una posición exacta por depósito, ubicación y dimensiones de seguimiento."),
                    sectionSpec("movements", "movements_actions", "movements", "Movimientos",
                            "Registra entradas, salidas o transferencias en la unidad base del producto."),
                    sectionSpec("reservation_create", "reservation_create_actions", "reservations", "Crear reserva",
                            "Separa cantidad disponible para una operación externa identificada."),
                    sectionSpec("reservation_manage", "reservation_manage_actions", "reservations", "Gestionar reserva",
                            "Consume, libera o vence una reserva mediante identidad, versión e idempotencia."),
                    sectionSpec("lifecycle", "lifecycle_actions", "lifecycle", "Ciclo de vida",
                            "Actualiza la referencia de catálogo o inactiva el artículo sin borrar historia."));
        }
        if (INVENTORY_WAREHOUSES.equals(screenId)) {
            return List.of(
                    sectionSpec("general", "general_actions", "general", "Datos generales",
                            "Actualiza el nombre visible sin cambiar el código ni la identidad técnica."),
                    sectionSpec("locations", "locations_actions", "locations", "Ubicaciones",
                            "Agrega, renombra o inactiva posiciones operativas dentro del depósito."),
                    sectionSpec("lifecycle", "lifecycle_actions", "lifecycle", "Ciclo de vida",
                            "Inactivar conserva ubicaciones, movimientos y referencias históricas."));
        }
        if (INVENTORY_COUNTS.equals(screenId)) {
            return List.of(
                    sectionSpec("lines", "lines_actions", "lines", "Líneas del conteo",
                            "Agrega posiciones exactas mientras el conteo permanece en borrador."),
                    sectionSpec("capture", "capture_actions", "capture", "Captura física",
                            "Registra la cantidad observada para cada línea iniciada."),
                    sectionSpec("lifecycle", "lifecycle_actions", "lifecycle", "Flujo controlado",
                            "Inicia, revisa, contabiliza o cancela respetando estado, permiso y versión."));
        }
        throw new IllegalArgumentException("Unsupported entity screen");
    }

    private static SectionSpec sectionSpec(
            String fieldsRegion,
            String actionsRegion,
            String tabId,
            String title,
            String description) {
        return new SectionSpec(fieldsRegion, actionsRegion, tabId, title, description);
    }

    private static EntityScreenSpec referenceDataSpec() {
        return new EntityScreenSpec(
                "Datos de referencia",
                "Consulta los catálogos normativos vigentes, su procedencia y su alcance.",
                new ShellEntityPresentation(
                        "Configuración compartida",
                        "Publicación controlada",
                        "Las publicaciones cambian mediante migraciones o importaciones verificadas, no desde el navegador.",
                        "Sin altas manuales",
                        "Volver a datos de referencia",
                        "Consulta códigos, versión de publicación y habilitación para la empresa activa.",
                        "La publicación corriente contiene 248 países y 178 códigos de moneda o fondo; las listas se buscan y paginan en servidor."),
                Set.of("search", "history"),
                Set.of("search_actions", "policy_actions"),
                List.of(
                        new DetailTabSpec("history", "Historial"),
                        new DetailTabSpec("lifecycle", "Estado")));
    }

    private static EntityScreenSpec businessPartnersSpec() {
        return new EntityScreenSpec(
                "Socios comerciales",
                "Administra participantes, roles, identificaciones, direcciones y contactos dentro de la empresa activa.",
                new ShellEntityPresentation(
                        "Socios comerciales",
                        "Nuevo socio comercial",
                        "Completa los datos principales. Los detalles adicionales se agregan después del alta.",
                        "Nuevo socio",
                        "Volver al directorio",
                        "Consulta el resumen y abre únicamente la sección que necesitas administrar.",
                        "Información vigente dentro de la empresa activa."),
                Set.of("search", "create", "code", "names", "identification", "address", "channel", "contact"),
                Set.of(
                        "search_actions", "create_actions", "code_actions", "names_actions",
                        "identification_actions", "address_actions", "channel_actions", "contact_actions",
                        "role_actions", "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("general", "Datos generales"),
                        new DetailTabSpec("identifications", "Identificaciones"),
                        new DetailTabSpec("addresses", "Direcciones"),
                        new DetailTabSpec("contacts", "Contacto"),
                        new DetailTabSpec("roles", "Roles y estado")));
    }

    private static EntityScreenSpec businessPartnerDefinitionsSpec() {
        return new EntityScreenSpec(
                "Definiciones de socios",
                "Administra tipos de canal, identificación y dirección dentro de la empresa activa.",
                new ShellEntityPresentation(
                        "Configuración de socios comerciales",
                        "Nueva definición",
                        "Selecciona una clase y define el código estable y el nombre visible.",
                        "Nueva definición",
                        "Volver a definiciones",
                        "Consulta clase, identidad, estado y versión del valor empresarial.",
                        "Definición vigente dentro de la empresa activa."),
                Set.of("search", "create", "history", "revision"),
                Set.of(
                        "search_actions", "create_actions", "revision_actions",
                        "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("history", "Historial"),
                        new DetailTabSpec("revision", "Nueva revisión"),
                        new DetailTabSpec("lifecycle", "Estado")));
    }

    private static EntityScreenSpec catalogItemsSpec() {
        return new EntityScreenSpec(
                "Artículos y servicios",
                "Administra conceptos comerciales reutilizables, unidades, clasificación tributaria e identificadores.",
                new ShellEntityPresentation(
                        "Catálogo comercial",
                        "Nuevo artículo o servicio",
                        "Define la identidad comercial mínima; clasificación y conversiones se completan en la ficha.",
                        "Nuevo artículo o servicio",
                        "Volver al catálogo",
                        "Consulta el resumen y administra una sola dimensión del concepto por vez.",
                        "Información vigente del concepto dentro de la empresa activa."),
                Set.of(
                        "search", "create", "general", "identifiers", "classification",
                        "units", "tax", "variants"),
                Set.of(
                        "search_actions", "create_actions", "general_actions", "identifiers_actions",
                        "classification_actions", "units_actions", "tax_actions", "variants_actions",
                        "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("general", "Datos generales"),
                        new DetailTabSpec("identifiers", "Identificadores"),
                        new DetailTabSpec("classification", "Clasificación"),
                        new DetailTabSpec("units", "Unidades"),
                        new DetailTabSpec("tax", "Impuestos"),
                        new DetailTabSpec("variants", "Variantes"),
                        new DetailTabSpec("lifecycle", "Estado")));
    }

    private static EntityScreenSpec catalogPriceListsSpec() {
        return new EntityScreenSpec(
                "Listas de precios",
                "Administra moneda, política tributaria, redondeo y precios efectivos del catálogo.",
                new ShellEntityPresentation(
                        "Catálogo comercial",
                        "Nueva lista de precios",
                        "Define la política monetaria de la lista; sus entradas se agregan después del alta.",
                        "Nueva lista",
                        "Volver a listas",
                        "Consulta la política y administra sus precios sin mezclar otras tareas.",
                        "Política y entradas vigentes dentro de la empresa activa."),
                Set.of("search", "create", "general", "entries"),
                Set.of(
                        "search_actions", "create_actions", "general_actions", "entries_actions",
                        "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("general", "Datos generales"),
                        new DetailTabSpec("entries", "Precios"),
                        new DetailTabSpec("lifecycle", "Estado")));
    }

    private static EntityScreenSpec catalogTaxProfilesSpec() {
        return new EntityScreenSpec(
                "Perfiles tributarios",
                "Administra tratamientos internos reutilizables para artículos y servicios de la empresa activa.",
                new ShellEntityPresentation(
                        "Configuración del catálogo",
                        "Nuevo perfil tributario",
                        "Define identidad, tratamiento interno y vigencia. La correspondencia fiscal se configura por separado.",
                        "Nuevo perfil",
                        "Volver a perfiles",
                        "Consulta identidad, vigencia y versión sin confundir el perfil con una regla fiscal oficial.",
                        "Definición interna vigente dentro de la empresa activa."),
                Set.of("search", "create", "history", "revision"),
                Set.of(
                        "search_actions", "create_actions", "revision_actions",
                        "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("history", "Historial"),
                        new DetailTabSpec("revision", "Nueva revisión"),
                        new DetailTabSpec("lifecycle", "Estado")));
    }

    private static EntityScreenSpec catalogDefinitionsSpec() {
        return new EntityScreenSpec(
                "Definiciones del catálogo",
                "Administra unidades, categorías, marcas y etiquetas reutilizables dentro de la empresa activa.",
                new ShellEntityPresentation(
                        "Configuración del catálogo",
                        "Nueva definición",
                        "Selecciona el tipo y completa únicamente los datos que correspondan.",
                        "Nueva definición",
                        "Volver a definiciones",
                        "Consulta tipo, identidad, estado y versión del valor empresarial.",
                        "Definición vigente dentro de la empresa activa."),
                Set.of("search", "create", "history", "revision", "replacement"),
                Set.of(
                        "search_actions", "create_actions", "revision_actions",
                        "replacement_actions", "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("history", "Historial"),
                        new DetailTabSpec("revision", "Nueva revisión"),
                        new DetailTabSpec("replacement", "Reemplazar"),
                        new DetailTabSpec("lifecycle", "Estado")));
    }

    private static EntityScreenSpec catalogVariantFamiliesSpec() {
        return new EntityScreenSpec(
                "Familias de variantes",
                "Administra atributos ordenados para describir variantes de artículos dentro de la empresa activa.",
                new ShellEntityPresentation(
                        "Configuración del catálogo",
                        "Nueva familia de variantes",
                        "Agrega de 1 a 8 atributos al borrador y revisa su orden antes de registrar.",
                        "Nueva familia",
                        "Volver a familias",
                        "Consulta identidad, atributos, obligatoriedad, estado y versión de la familia.",
                        "Familia vigente dentro de la empresa activa."),
                Set.of("search", "create", "history", "revision"),
                Set.of(
                        "search_actions", "create_actions", "revision_actions",
                        "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("history", "Historial"),
                        new DetailTabSpec("revision", "Nueva revisión"),
                        new DetailTabSpec("lifecycle", "Estado")));
    }

    private static EntityScreenSpec inventoryStockSpec() {
        return new EntityScreenSpec(
                "Existencias",
                "Consulta disponibilidad y administra movimientos, reservas y seguimiento por producto.",
                new ShellEntityPresentation(
                        "Inventario",
                        "Incorporar producto",
                        "Selecciona un producto del catálogo y define lote, serie y vencimiento una sola vez.",
                        "Incorporar producto",
                        "Volver a existencias",
                        "Consulta el resumen y trabaja por disponibilidad, movimientos o reservas.",
                        "Cantidades expresadas en la unidad base vigente del producto."),
                Set.of(
                        "search", "create", "availability", "movements",
                        "reservation_create", "reservation_manage"),
                Set.of(
                        "search_actions", "create_actions", "availability_actions", "movements_actions",
                        "reservation_create_actions", "reservation_manage_actions", "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("availability", "Disponibilidad"),
                        new DetailTabSpec("movements", "Movimientos"),
                        new DetailTabSpec("reservations", "Reservas"),
                        new DetailTabSpec("lifecycle", "Estado")));
    }

    private static EntityScreenSpec inventoryWarehousesSpec() {
        return new EntityScreenSpec(
                "Depósitos",
                "Administra depósitos y ubicaciones operativas sin mezclar existencias ni conteos.",
                new ShellEntityPresentation(
                        "Inventario",
                        "Nuevo depósito",
                        "Define código y nombre; la ubicación general se crea automáticamente.",
                        "Nuevo depósito",
                        "Volver a depósitos",
                        "Consulta el resumen y administra datos generales, ubicaciones o estado.",
                        "Estructura vigente del depósito dentro de la empresa activa."),
                Set.of("search", "create", "general", "locations"),
                Set.of(
                        "search_actions", "create_actions", "general_actions",
                        "locations_actions", "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("general", "Datos generales"),
                        new DetailTabSpec("locations", "Ubicaciones"),
                        new DetailTabSpec("lifecycle", "Estado")));
    }

    private static EntityScreenSpec inventoryCountsSpec() {
        return new EntityScreenSpec(
                "Conteos físicos",
                "Prepara, ejecuta y contabiliza conteos con alcance, versión y permisos explícitos.",
                new ShellEntityPresentation(
                        "Inventario",
                        "Nuevo conteo",
                        "Selecciona el alcance; las posiciones a contar se agregan en la ficha.",
                        "Nuevo conteo",
                        "Volver a conteos",
                        "Consulta estado y diferencias antes de ejecutar la siguiente transición.",
                        "El ajuste se genera únicamente al contabilizar un conteo revisado."),
                Set.of("search", "create", "lines", "capture"),
                Set.of(
                        "search_actions", "create_actions", "lines_actions",
                        "capture_actions", "lifecycle_actions"),
                List.of(
                        new DetailTabSpec("lines", "Líneas"),
                        new DetailTabSpec("capture", "Captura"),
                        new DetailTabSpec("lifecycle", "Flujo")));
    }

    private static Variant variant(List<ShellScreenFragmentView> fragments) {
        if (fragments.isEmpty()) {
            return new Variant("Pantalla estándar", "screen-variant-standard");
        }
        String fragmentId = fragments.getFirst().getId();
        if (fragmentId.startsWith("reference_custom_a:")) {
            return new Variant("Personalización A aplicada", "screen-variant-a");
        }
        if (fragmentId.startsWith("reference_custom_b:")) {
            return new Variant("Personalización B aplicada", "screen-variant-b");
        }
        throw new IllegalStateException("unsupported fragment passed the closed registry");
    }

    private record RouteKey(PluginId pluginId, String route) {
        private RouteKey {
            Objects.requireNonNull(pluginId, "pluginId");
            Objects.requireNonNull(route, "route");
        }
    }

    private record FragmentPresentation(
            String eyebrow,
            String title,
            String body,
            String toneClass) {
    }

    private record EntityScreenSpec(
            String title,
            String description,
            ShellEntityPresentation presentation,
            Set<String> fieldRegions,
            Set<String> actionRegions,
            List<DetailTabSpec> detailTabs) {

        private EntityScreenSpec {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(presentation, "presentation");
            fieldRegions = Set.copyOf(fieldRegions);
            actionRegions = Set.copyOf(actionRegions);
            detailTabs = List.copyOf(detailTabs);
        }
    }

    private record DetailTabSpec(String id, String label) {
    }

    private record SectionSpec(
            String fieldsRegion,
            String actionsRegion,
            String tabId,
            String title,
            String description) {
    }

    private record Variant(String label, String toneClass) {
    }
}
