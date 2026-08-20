package py.com.logixone.web.shell;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.plugin.api.ScreenTextKey;

/** Closed presentation catalog for public text keys supported by this shell cut. */
@ApplicationScoped
public class ShellTextCatalog {

    private static final Map<String, String> TEXTS = texts();

    public String menuLabel(String labelKey) {
        return TEXTS.getOrDefault(labelKey, "Función disponible");
    }

    public Optional<String> screenText(ScreenTextKey textKey) {
        return Optional.ofNullable(TEXTS.get(textKey.value()));
    }

    private static Map<String, String> texts() {
        Map<String, String> texts = new LinkedHashMap<>();
        texts.put("reference.menu", "Panel de demostración");
        texts.put("reference.dashboard.greeting", "Composición lista");
        texts.put("reference.dashboard.greeting.help",
                "El contenido visible proviene de un contrato público y el shell controla su representación.");
        texts.put("reference.dashboard.summary", "Nota de la demostración");
        texts.put("reference.dashboard.refresh", "Actualizar vista");
        texts.put("reference_custom_a.dashboard.summary", "Referencia tributaria de ejemplo");
        texts.put("reference_custom_a.dashboard.summary.help",
                "La personalización A cambió la etiqueta, agregó esta ayuda y volvió obligatorio el campo.");
        texts.put("reference_custom_b.dashboard.summary", "Resumen interno de empresa B");
        texts.put("reference_data.menu.catalogs", "Datos de referencia");
        texts.put("reference_data.catalogs.results.label", "Países y monedas");
        texts.put("reference_data.catalogs.results.help",
                "Consulta códigos normativos, publicación, procedencia y habilitación para la empresa activa.");
        referenceDataElement(texts, "search_text", "Buscar referencia",
                "Filtra por nombre o código dentro de la publicación corriente.");
        referenceDataElement(texts, "search_catalog", "Catálogo",
                "Elige países o monedas; cada página contiene hasta 50 referencias.");
        referenceDataElement(texts, "search", "Buscar",
                "Ejecuta el filtro en el servidor para la empresa activa.");
        referenceDataElement(texts, "history", "Historial empresarial",
                "Cambios de habilitación preservados para el código y la empresa activos.");
        referenceDataElement(texts, "select_reference", "Abrir",
                "Muestra procedencia, estado efectivo, versión e historial empresarial.");
        referenceDataElement(texts, "enable_reference", "Habilitar referencia",
                "Vuelve a ofrecer el código para nuevas operaciones de la empresa activa.");
        referenceDataElement(texts, "disable_reference", "Inhabilitar referencia",
                "Impide nuevas selecciones sin borrar documentos ni referencias históricas.");
        texts.put("business_partners.menu.directory", "Socios comerciales");
        texts.put("business_partners.menu.definitions", "Definiciones de socios");
        element(texts, "search_text", "Nombre, código o identificación", "Hasta 100 caracteres.");
        element(texts, "search_role", "Rol comercial", "Filtra clientes o proveedores activos.");
        element(texts, "search_state", "Estado", "Incluye activos o inactivos.");
        element(texts, "search", "Buscar", "Actualiza el listado con los filtros actuales.");
        element(texts, "results", "Resultados", "Los datos pertenecen únicamente a la empresa activa.");
        element(texts, "select_partner", "Abrir", "Muestra el detalle y las acciones autorizadas.");
        element(texts, "new_code", "Código", "Déjalo vacío para asignar el siguiente código automático.");
        element(texts, "new_kind", "Tipo de participante", "El tipo no cambia después del alta.");
        element(texts, "new_display_name", "Nombre visible", "Nombre principal utilizado en búsquedas y selecciones.");
        element(texts, "new_legal_name", "Razón social", "Opcional para participantes con denominación legal.");
        element(texts, "new_trade_name", "Nombre comercial", "Opcional y separado de la razón social.");
        element(texts, "register", "Registrar", "Crea el socio comercial dentro de la empresa activa.");
        element(texts, "edit_code", "Código actual", "Debe ser único dentro de la empresa.");
        element(texts, "change_code", "Cambiar código", "Actualiza el código usando control de versión.");
        element(texts, "edit_display_name", "Nombre visible", "Nombre principal mostrado por el ERP.");
        element(texts, "edit_legal_name", "Razón social", "Puede quedar vacío cuando no corresponde.");
        element(texts, "edit_trade_name", "Nombre comercial", "Puede quedar vacío cuando no corresponde.");
        element(texts, "rename", "Guardar nombres", "Actualiza los nombres usando control de versión.");
        element(texts, "identification_type", "Tipo de identificación",
                "Selecciona un tipo activo administrado por la empresa.");
        element(texts, "identification_country", "País",
                "Selecciona un país habilitado por la publicación normativa vigente.");
        element(texts, "identification_value", "Número presentado", "Se conserva como fue ingresado y se normaliza para buscar.");
        element(texts, "add_identification", "Agregar identificación", "Las coincidencias generan una advertencia, no una fusión.");
        element(texts, "address_type", "Tipo de dirección",
                "Selecciona un tipo activo, por ejemplo física o postal.");
        element(texts, "address_purpose", "Propósito de dirección",
                "Indica si la dirección es general, de facturación o de entrega.");
        element(texts, "address_line", "Dirección", "Calle, ruta o descripción principal de la ubicación.");
        element(texts, "address_locality", "Localidad", "Ciudad o localidad opcional.");
        element(texts, "add_address", "Agregar dirección", "Añade una nueva ubicación sin reemplazar las anteriores.");
        element(texts, "channel_kind", "Tipo de canal", "Selecciona correo, teléfono, WhatsApp o sitio web.");
        element(texts, "channel_value", "Dato de contacto", "Valor del canal seleccionado.");
        element(texts, "add_channel", "Agregar canal", "Añade un medio general de contacto.");
        element(texts, "contact_name", "Nombre del contacto", "Persona de contacto asociada; no crea otro socio comercial.");
        element(texts, "contact_position", "Cargo o función", "Descripción opcional del rol del contacto.");
        element(texts, "add_contact", "Agregar contacto", "Añade el contacto al participante seleccionado.");
        element(texts, "assign_client", "Asignar cliente", "Agrega el rol cliente activo.");
        element(texts, "assign_supplier", "Asignar proveedor", "Agrega el rol proveedor activo.");
        element(texts, "activate_client", "Activar cliente", "Reactiva el rol cliente existente.");
        element(texts, "deactivate_client", "Inactivar cliente", "Inactiva solo el rol cliente.");
        element(texts, "activate_supplier", "Activar proveedor", "Reactiva el rol proveedor existente.");
        element(texts, "deactivate_supplier", "Inactivar proveedor", "Inactiva solo el rol proveedor.");
        element(texts, "deactivate_partner", "Inactivar participante", "Preserva identidad, relaciones e historia.");
        element(texts, "reactivate_partner", "Reactivar participante", "Vuelve a habilitar el participante completo.");
        businessPartnerElement(texts, "definitions", "definition_kind",
                "Clase de definición", "Selecciona qué catálogo empresarial quieres administrar.");
        businessPartnerElement(texts, "definitions", "definition_search_text",
                "Código o nombre", "Busca dentro de la clase seleccionada para la empresa activa.");
        businessPartnerElement(texts, "definitions", "definition_search_state",
                "Estado", "Incluye valores activos o inactivos.");
        businessPartnerElement(texts, "definitions", "definition_search",
                "Buscar", "Actualiza el directorio con los filtros actuales.");
        businessPartnerElement(texts, "definitions", "definition_results",
                "Definiciones disponibles", "Valores reutilizables de contacto, identificación y dirección.");
        businessPartnerElement(texts, "definitions", "definition_history",
                "Historial de revisiones", "Versiones de solo lectura ordenadas desde la más reciente.");
        businessPartnerElement(texts, "definitions", "select_definition",
                "Abrir", "Muestra el detalle de la definición empresarial.");
        businessPartnerElement(texts, "definitions", "definition_new_kind",
                "Clase de definición", "Selecciona el catálogo empresarial que recibirá el nuevo valor.");
        businessPartnerElement(texts, "definitions", "definition_new_code",
                "Código", "Identificador estable en minúsculas, por ejemplo telegram.");
        businessPartnerElement(texts, "definitions", "definition_new_name",
                "Nombre", "Texto comprensible que aparecerá en el selector.");
        businessPartnerElement(texts, "definitions", "register_definition",
                "Registrar definición", "Crea el valor dentro de la clase seleccionada y la empresa activa.");
        businessPartnerElement(texts, "definitions", "definition_edit_name",
                "Nombre revisado", "Actualiza el texto visible sin cambiar el código estable.");
        businessPartnerElement(texts, "definitions", "revise_definition",
                "Guardar revisión", "Crea una nueva versión auditable y conserva las anteriores.");
        businessPartnerElement(texts, "definitions", "activate_definition",
                "Reactivar definición", "Vuelve a ofrecer el valor para nuevas selecciones.");
        businessPartnerElement(texts, "definitions", "inactivate_definition",
                "Inactivar definición", "La excluye de nuevas selecciones sin borrar referencias históricas.");

        texts.put("commercial_catalog.menu.items", "Artículos y servicios");
        texts.put("commercial_catalog.menu.price_lists", "Listas de precios");
        texts.put("commercial_catalog.menu.definitions", "Definiciones del catálogo");
        texts.put("commercial_catalog.menu.variant_families", "Familias de variantes");
        texts.put("commercial_catalog.menu.tax_profiles", "Perfiles tributarios");
        catalogElement(texts, "items", "item_search_text", "Nombre, código o identificador",
                "Busca hasta 100 caracteres dentro de la empresa activa.");
        catalogElement(texts, "items", "item_search_type", "Tipo", "Filtra productos o servicios.");
        catalogElement(texts, "items", "item_search_state", "Estado", "Incluye activos o inactivos.");
        catalogElement(texts, "items", "item_search", "Buscar", "Actualiza el directorio con los filtros actuales.");
        catalogElement(texts, "items", "item_results", "Artículos y servicios",
                "Los resultados pertenecen únicamente a la empresa activa.");
        catalogElement(texts, "items", "select_item", "Abrir", "Muestra la ficha autorizada del concepto.");
        catalogElement(texts, "items", "item_new_code", "Código", "Déjalo vacío para asignar el siguiente código.");
        catalogElement(texts, "items", "item_new_name", "Nombre", "Nombre comercial utilizado en búsquedas y documentos futuros.");
        catalogElement(texts, "items", "item_new_description", "Descripción", "Descripción operativa opcional.");
        catalogElement(texts, "items", "item_new_type", "Tipo", "Producto o servicio; no cambia después del alta.");
        catalogElement(texts, "items", "item_new_scope", "Alcance", "Indica si participa en compra, venta o ambas.");
        catalogElement(texts, "items", "item_new_base_unit", "Unidad base", "Unidad principal para cantidades y conversiones.");
        catalogElement(texts, "items", "item_new_tax_profile", "Perfil tributario", "Referencia interna versionada, no un nodo SIFEN.");
        catalogElement(texts, "items", "register_item", "Registrar", "Crea el concepto dentro de la empresa activa.");
        catalogElement(texts, "items", "item_edit_code", "Código", "Debe continuar siendo único en la empresa.");
        catalogElement(texts, "items", "item_edit_name", "Nombre", "Nombre visible del concepto comercial.");
        catalogElement(texts, "items", "item_edit_description", "Descripción", "Puede quedar vacía.");
        catalogElement(texts, "items", "item_edit_scope", "Alcance", "Actualiza compra, venta o ambos alcances.");
        catalogElement(texts, "items", "revise_item", "Guardar datos", "Usa la versión vigente del registro.");
        catalogElement(texts, "items", "identifier_type", "Tipo de identificador", "Ejemplo: EAN, SKU alternativo o código interno.");
        catalogElement(texts, "items", "identifier_value", "Valor", "Se conserva presentado y se normaliza para buscar.");
        catalogElement(texts, "items", "add_identifier", "Agregar identificador", "No reemplaza el código empresarial.");
        catalogElement(texts, "items", "main_category", "Categoría principal", "Selecciona una categoría activa.");
        catalogElement(texts, "items", "brand", "Marca", "Opcional y separada de la categoría.");
        catalogElement(texts, "items", "classify_item", "Guardar clasificación", "Actualiza la clasificación controlada.");
        catalogElement(texts, "items", "conversion_unit", "Unidad alternativa", "Debe ser una unidad activa distinta de la base.");
        catalogElement(texts, "items", "conversion_factor", "Factor a unidad base", "Número positivo; por ejemplo 12 para una docena.");
        catalogElement(texts, "items", "conversion_purpose", "Finalidad", "Compra, venta o ambas.");
        catalogElement(texts, "items", "add_conversion", "Agregar conversión", "Registra una conversión específica del concepto.");
        catalogElement(texts, "items", "item_tax_profile", "Perfil tributario", "Selecciona la versión interna vigente; los perfiles se crean desde Perfiles tributarios.");
        catalogElement(texts, "items", "assign_tax_profile", "Asignar perfil", "Conserva la referencia versionada.");
        catalogElement(texts, "items", "item_variant_family", "Familia de variantes", "Ofrece sólo familias activas de la empresa; las asignaciones históricas conservan su revisión original.");
        catalogElement(texts, "items", "item_variant_structure", "Atributos esperados", "Completa los códigos mostrados respetando tipo y obligatoriedad.");
        catalogElement(texts, "items", "item_variant_values", "Valores", "Usa CÓDIGO=valor separado por punto y coma; por ejemplo COLOR=Azul; TALLA=M.");
        catalogElement(texts, "items", "prepare_item_variant", "Mostrar atributos", "Actualiza la estructura después de cambiar la familia sin guardar el artículo.");
        catalogElement(texts, "items", "assign_item_variant", "Asignar variante", "Revalida empresa, estado, revisión y estructura antes de guardar.");
        catalogElement(texts, "items", "activate_item", "Reactivar", "Vuelve a habilitar el concepto.");
        catalogElement(texts, "items", "inactivate_item", "Inactivar", "Preserva identificadores, precios y referencias históricas.");

        catalogElement(texts, "price_lists", "price_search_text", "Nombre o código", "Busca listas dentro de la empresa activa.");
        catalogElement(texts, "price_lists", "price_search_state", "Estado", "Incluye listas activas o inactivas.");
        catalogElement(texts, "price_lists", "price_search", "Buscar", "Actualiza el directorio de listas.");
        catalogElement(texts, "price_lists", "price_results", "Listas de precios", "Muestra política y cantidad de entradas.");
        catalogElement(texts, "price_lists", "select_price_list", "Abrir", "Muestra la ficha y sus entradas.");
        catalogElement(texts, "price_lists", "price_new_code", "Código", "Déjalo vacío para asignar el siguiente código.");
        catalogElement(texts, "price_lists", "price_new_name", "Nombre", "Nombre visible de la política de precios.");
        catalogElement(texts, "price_lists", "price_currency", "Moneda",
                "Selecciona una moneda ISO 4217 habilitada para la empresa activa.");
        catalogElement(texts, "price_lists", "price_tax_mode", "Impuestos", "Indica si los importes incluyen impuestos.");
        catalogElement(texts, "price_lists", "price_scale", "Decimales", "Escala monetaria entre cero y seis.");
        catalogElement(texts, "price_lists", "price_rounding_mode", "Redondeo", "Política aplicada de forma uniforme a la lista.");
        catalogElement(texts, "price_lists", "register_price_list", "Registrar lista", "Crea la política sin entradas iniciales.");
        catalogElement(texts, "price_lists", "price_edit_name", "Nombre", "Actualiza únicamente el nombre visible.");
        catalogElement(texts, "price_lists", "rename_price_list", "Guardar nombre", "Mantiene moneda y política históricas.");
        catalogElement(texts, "price_lists", "price_entry_item", "Artículo o servicio", "Selecciona un concepto activo.");
        catalogElement(texts, "price_lists", "price_entry_unit", "Unidad", "Unidad en la que se expresa el precio.");
        catalogElement(texts, "price_lists", "price_entry_minimum", "Cantidad mínima", "Número positivo desde el que aplica.");
        catalogElement(texts, "price_lists", "price_entry_amount", "Importe", "Importe no negativo en la moneda de la lista.");
        catalogElement(texts, "price_lists", "price_entry_valid_from", "Vigente desde", "Instante ISO-8601, por ejemplo 2026-07-30T12:00:00Z.");
        catalogElement(texts, "price_lists", "price_entry_valid_until", "Vigente hasta", "Opcional; debe ser posterior al inicio.");
        catalogElement(texts, "price_lists", "add_price_entry", "Agregar precio", "Rechaza vigencias solapadas para el mismo alcance.");
        catalogElement(texts, "price_lists", "price_entry_to_inactivate", "Entrada vigente", "Selecciona una entrada para inactivarla sin borrarla.");
        catalogElement(texts, "price_lists", "inactivate_price_entry", "Inactivar precio", "Conserva importe, vigencia e identidad histórica.");
        catalogElement(texts, "price_lists", "activate_price_list", "Reactivar lista", "Vuelve a habilitar la política.");
        catalogElement(texts, "price_lists", "inactivate_price_list", "Inactivar lista", "Conserva todas sus entradas históricas.");

        catalogElement(texts, "definitions", "definition_search_text", "Código o nombre", "Busca únicamente en las definiciones de la empresa activa.");
        catalogElement(texts, "definitions", "definition_search_kind", "Tipo", "Filtra unidades, categorías, marcas o etiquetas.");
        catalogElement(texts, "definitions", "definition_search_state", "Estado", "Incluye definiciones activas o inactivas.");
        catalogElement(texts, "definitions", "definition_search", "Buscar", "Actualiza el directorio con los filtros actuales.");
        catalogElement(texts, "definitions", "definition_results", "Unidades, categorías, marcas y etiquetas", "Valores empresariales reutilizables por artículos, conversiones y precios.");
        catalogElement(texts, "definitions", "select_definition", "Abrir", "Muestra tipo, identidad, configuración y versión.");
        catalogElement(texts, "definitions", "definition_new_kind", "Tipo de definición", "Selecciona unidad, categoría, marca o etiqueta.");
        catalogElement(texts, "definitions", "definition_new_code", "Código", "Código estable y único para este tipo dentro de la empresa.");
        catalogElement(texts, "definitions", "definition_new_name", "Nombre", "Nombre comprensible que se mostrará en los selectores.");
        catalogElement(texts, "definitions", "definition_unit_scale", "Decimales (sólo unidades)", "Se utiliza sólo para unidades; admite valores de 0 a 12.");
        catalogElement(texts, "definitions", "definition_category_parent", "Categoría superior (sólo categorías)", "Se utiliza sólo para categorías y permite construir su jerarquía.");
        catalogElement(texts, "definitions", "register_definition", "Registrar definición", "Crea y audita el valor para la empresa activa.");
        catalogElement(texts, "definitions", "definition_revision_name", "Nombre revisado", "Actualiza el texto visible sin cambiar el código estable ni la identidad.");
        catalogElement(texts, "definitions", "definition_revision_unit_scale", "Decimales (sólo unidades)", "Se utiliza sólo para unidades; admite valores de 0 a 12.");
        catalogElement(texts, "definitions", "definition_revision_category_parent", "Categoría superior (sólo categorías)", "Permite cambiar la jerarquía sin reescribir las revisiones anteriores.");
        catalogElement(texts, "definitions", "revise_definition", "Crear revisión", "Conserva código e identidad y registra una nueva versión auditable.");
        catalogElement(texts, "definitions", "definition_history", "Revisiones registradas", "Historial de solo lectura aislado por empresa y ordenado desde la versión más reciente.");
        catalogElement(texts, "definitions", "definition_replacement_code", "Nuevo código", "Debe ser distinto y quedará como identidad estable de la definición sucesora.");
        catalogElement(texts, "definitions", "definition_replacement_name", "Nombre de la sucesora", "Nombre visible de la nueva definición activa.");
        catalogElement(texts, "definitions", "definition_replacement_unit_scale", "Decimales de la sucesora (sólo unidades)", "Se utiliza sólo para unidades; admite valores de 0 a 12.");
        catalogElement(texts, "definitions", "definition_replacement_category_parent", "Categoría superior de la sucesora", "Sólo para categorías; no puede ser la definición reemplazada.");
        catalogElement(texts, "definitions", "replace_definition", "Reemplazar definición", "Crea una identidad nueva, inactiva la anterior y no reasigna referencias históricas.");
        catalogElement(texts, "definitions", "activate_definition", "Reactivar", "Vuelve a ofrecer la definición para operaciones nuevas sin cambiar su identidad.");
        catalogElement(texts, "definitions", "inactivate_definition", "Inactivar", "Impide nuevas selecciones y conserva referencias e historia anteriores.");

        catalogElement(texts, "variant_families", "variant_family_search_text", "Código o nombre", "Busca únicamente en las familias de la empresa activa.");
        catalogElement(texts, "variant_families", "variant_family_search_state", "Estado", "Incluye familias activas o inactivas.");
        catalogElement(texts, "variant_families", "variant_family_search", "Buscar", "Actualiza el directorio con los filtros actuales.");
        catalogElement(texts, "variant_families", "variant_family_results", "Familias de variantes", "Atributos reutilizables para describir variantes de artículos.");
        catalogElement(texts, "variant_families", "select_variant_family", "Abrir", "Muestra identidad, atributos, estado y versión.");
        catalogElement(texts, "variant_families", "variant_family_new_code", "Código de familia", "Código estable y único dentro de la empresa.");
        catalogElement(texts, "variant_families", "variant_family_new_name", "Nombre de familia", "Nombre comprensible, por ejemplo Ropa o Calzado.");
        catalogElement(texts, "variant_families", "variant_attribute_code", "Código del atributo", "Código único dentro de la familia, por ejemplo COLOR o TALLA.");
        catalogElement(texts, "variant_families", "variant_attribute_name", "Nombre del atributo", "Nombre que verá el usuario al completar una variante.");
        catalogElement(texts, "variant_families", "variant_attribute_type", "Tipo de valor", "Texto, número o Sí/No según el dato que se capturará.");
        catalogElement(texts, "variant_families", "variant_attribute_required", "Obligatoriedad", "Define si toda variante debe informar este atributo.");
        catalogElement(texts, "variant_families", "variant_attribute_draft", "Atributos preparados", "Revisa el orden antes de registrar; puedes retirar solamente el último.");
        catalogElement(texts, "variant_families", "add_variant_attribute", "Agregar atributo", "Incorpora el atributo actual al final del borrador.");
        catalogElement(texts, "variant_families", "remove_variant_attribute", "Retirar último", "Retira el último atributo sin guardar la familia.");
        catalogElement(texts, "variant_families", "register_variant_family", "Registrar familia", "Crea y audita la familia con todos sus atributos.");
        catalogElement(texts, "variant_families", "variant_family_revision_name", "Nombre revisado", "Actualiza el nombre visible sin cambiar el código estable ni la identidad de la familia.");
        catalogElement(texts, "variant_families", "variant_revision_attribute_code", "Código del atributo", "Código único dentro de la nueva revisión, por ejemplo COLOR o TALLA.");
        catalogElement(texts, "variant_families", "variant_revision_attribute_name", "Nombre del atributo", "Nombre que se mostrará al capturar variantes con esta revisión.");
        catalogElement(texts, "variant_families", "variant_revision_attribute_type", "Tipo de valor", "Texto, número o Sí/No; las asignaciones anteriores conservan su tipo original.");
        catalogElement(texts, "variant_families", "variant_revision_attribute_required", "Obligatoriedad", "Indica si las nuevas asignaciones basadas en esta revisión deben informar el atributo.");
        catalogElement(texts, "variant_families", "variant_revision_attribute_draft", "Estructura revisada", "Contiene de 1 a 8 atributos; retira desde el final o agrega la nueva estructura antes de guardar.");
        catalogElement(texts, "variant_families", "add_variant_revision_attribute", "Agregar a la revisión", "Incorpora el atributo actual al final del borrador revisado.");
        catalogElement(texts, "variant_families", "remove_variant_revision_attribute", "Retirar último", "Retira el último atributo sólo del borrador de la revisión.");
        catalogElement(texts, "variant_families", "revise_variant_family", "Crear revisión", "Conserva código e identidad y registra nombre y estructura como una nueva versión append-only.");
        catalogElement(texts, "variant_families", "variant_family_history", "Revisiones registradas", "Historial de solo lectura aislado por empresa; las asignaciones existentes siguen ligadas a su revisión original.");
        catalogElement(texts, "variant_families", "activate_variant_family", "Reactivar familia", "Recupera el estado activo sin cambiar los atributos ni la identidad de la familia.");
        catalogElement(texts, "variant_families", "inactivate_variant_family", "Inactivar familia", "Marca la familia como inactiva y conserva atributos, identidad y referencias anteriores.");

        catalogElement(texts, "tax_profiles", "tax_profile_search_text", "Código, nombre o tratamiento", "Busca únicamente dentro de la empresa activa.");
        catalogElement(texts, "tax_profiles", "tax_profile_search_state", "Estado", "Incluye perfiles activos o inactivos.");
        catalogElement(texts, "tax_profiles", "tax_profile_search", "Buscar", "Actualiza el directorio con los filtros actuales.");
        catalogElement(texts, "tax_profiles", "tax_profile_results", "Perfiles tributarios", "Definiciones internas reutilizables por artículos y servicios.");
        catalogElement(texts, "tax_profiles", "select_tax_profile", "Abrir", "Muestra identidad, tratamiento, vigencia y versión.");
        catalogElement(texts, "tax_profiles", "tax_profile_new_code", "Código interno", "Código estable y único dentro de la empresa, por ejemplo IVA_REDUCIDO.");
        catalogElement(texts, "tax_profiles", "tax_profile_new_name", "Nombre", "Nombre comprensible que verán los usuarios en los selectores.");
        catalogElement(texts, "tax_profiles", "tax_profile_new_kind", "Tratamiento interno", "Clasificación neutral, por ejemplo TAXED_STANDARD, TAXED_REDUCED o EXEMPT.");
        catalogElement(texts, "tax_profiles", "tax_profile_new_description", "Descripción", "Explica el uso previsto sin copiar códigos o reglas SIFEN.");
        catalogElement(texts, "tax_profiles", "tax_profile_new_valid_from", "Vigente desde", "Instante ISO-8601, por ejemplo 2026-08-01T00:00:00Z.");
        catalogElement(texts, "tax_profiles", "tax_profile_new_valid_until", "Vigente hasta", "Opcional; debe ser posterior al inicio de vigencia.");
        catalogElement(texts, "tax_profiles", "register_tax_profile", "Registrar perfil", "Crea y audita la definición interna para la empresa activa.");
        catalogElement(texts, "tax_profiles", "tax_profile_revision_kind", "Tratamiento interno", "Define el tratamiento neutral de la nueva revisión sin cambiar la identidad del perfil.");
        catalogElement(texts, "tax_profiles", "tax_profile_revision_description", "Descripción", "Explica el uso de esta revisión sin copiar reglas del proveedor fiscal.");
        catalogElement(texts, "tax_profiles", "tax_profile_revision_valid_from", "Vigente desde", "Instante ISO-8601 desde el cual se aplicará la nueva revisión.");
        catalogElement(texts, "tax_profiles", "tax_profile_revision_valid_until", "Vigente hasta", "Opcional; debe ser posterior al inicio de la nueva revisión.");
        catalogElement(texts, "tax_profiles", "revise_tax_profile", "Crear revisión", "Conserva código e identidad y registra una nueva versión auditable.");
        catalogElement(texts, "tax_profiles", "tax_profile_history", "Revisiones registradas", "Historial de solo lectura aislado por empresa y ordenado desde la versión más reciente.");
        catalogElement(texts, "tax_profiles", "activate_tax_profile", "Reactivar", "Vuelve a ofrecer el perfil para operaciones nuevas sin cambiar su identidad.");
        catalogElement(texts, "tax_profiles", "inactivate_tax_profile", "Inactivar", "Impide nuevas selecciones y conserva revisiones y referencias históricas.");
        texts.put("inventory.menu.stock", "Existencias");
        texts.put("inventory.menu.warehouses", "Depósitos");
        texts.put("inventory.menu.counts", "Conteos físicos");

        inventoryElement(texts, "warehouses", "warehouse_search_text", "Código o nombre", "Busca hasta 100 caracteres dentro de la empresa activa.");
        inventoryElement(texts, "warehouses", "warehouse_search_state", "Estado", "Incluye depósitos activos o inactivos.");
        inventoryElement(texts, "warehouses", "warehouse_search", "Buscar", "Actualiza el directorio con los filtros actuales.");
        inventoryElement(texts, "warehouses", "warehouse_results", "Depósitos", "Muestra únicamente la estructura de la empresa activa.");
        inventoryElement(texts, "warehouses", "select_warehouse", "Abrir", "Muestra ubicaciones y acciones autorizadas.");
        inventoryElement(texts, "warehouses", "warehouse_new_code", "Código", "Identificador empresarial único del depósito.");
        inventoryElement(texts, "warehouses", "warehouse_new_name", "Nombre", "Nombre visible para búsquedas y operaciones.");
        inventoryElement(texts, "warehouses", "open_warehouse", "Crear depósito", "Crea también su ubicación general obligatoria.");
        inventoryElement(texts, "warehouses", "warehouse_edit_name", "Nombre", "Actualiza únicamente el nombre visible.");
        inventoryElement(texts, "warehouses", "rename_warehouse", "Guardar nombre", "Usa control de versión para evitar sobrescrituras.");
        inventoryElement(texts, "warehouses", "location_new_code", "Código de ubicación", "Debe ser único dentro del depósito.");
        inventoryElement(texts, "warehouses", "location_new_name", "Nombre de ubicación", "Describe la posición operativa.");
        inventoryElement(texts, "warehouses", "location_new_type", "Tipo", "Almacenamiento, recepción o despacho.");
        inventoryElement(texts, "warehouses", "add_location", "Agregar ubicación", "Incorpora una posición al depósito seleccionado.");
        inventoryElement(texts, "warehouses", "location_to_rename", "Ubicación", "Selecciona una ubicación activa para renombrar.");
        inventoryElement(texts, "warehouses", "location_edit_name", "Nuevo nombre", "Conserva el código y la identidad de la ubicación.");
        inventoryElement(texts, "warehouses", "rename_location", "Renombrar ubicación", "Actualiza con la versión vigente.");
        inventoryElement(texts, "warehouses", "location_to_inactivate", "Ubicación operativa", "La ubicación general no puede inactivarse.");
        inventoryElement(texts, "warehouses", "inactivate_location", "Inactivar ubicación", "Solo se permite cuando no conserva cantidades.");
        inventoryElement(texts, "warehouses", "inactivate_warehouse", "Inactivar depósito", "Preserva estructura e historia y exige saldo cero.");

        inventoryElement(texts, "stock", "stock_search_text", "Código o nombre", "Busca productos incorporados al inventario.");
        inventoryElement(texts, "stock", "stock_search_state", "Estado", "Incluye artículos activos o inactivos.");
        inventoryElement(texts, "stock", "stock_search", "Buscar", "Actualiza el resumen de existencias.");
        inventoryElement(texts, "stock", "stock_results", "Existencias", "Cantidades agregadas en la unidad base del producto.");
        inventoryElement(texts, "stock", "select_stock_item", "Abrir", "Muestra disponibilidad, movimientos y reservas.");
        inventoryElement(texts, "stock", "stock_task", "Tarea", "Elige movimiento, disponibilidad, reserva o administración del artículo.");
        inventoryElement(texts, "stock", "apply_stock_task", "Continuar", "Actualiza los campos aplicables a la tarea seleccionada.");
        inventoryElement(texts, "stock", "stock_guidance", "Datos aplicables", "La pantalla muestra solamente la captura necesaria para la tarea elegida.");
        inventoryElement(texts, "stock", "stock_summary", "Resumen", "Revisa el artículo y el efecto previsto antes de confirmar.");
        inventoryElement(texts, "stock", "stock_new_catalog_item", "Producto de catálogo", "Solo aparecen productos activos de la empresa.");
        inventoryElement(texts, "stock", "stock_new_tracking", "Seguimiento", "Define si requiere lote, serie o ninguna dimensión.");
        inventoryElement(texts, "stock", "stock_new_expiry", "Vencimiento", "Define si la fecha está prohibida, es opcional u obligatoria.");
        inventoryElement(texts, "stock", "enroll_stock_item", "Incorporar", "Crea la referencia local de inventario sin duplicar el catálogo.");
        inventoryElement(texts, "stock", "availability_warehouse", "Depósito", "Selecciona el depósito de la posición exacta.");
        inventoryElement(texts, "stock", "availability_location", "Ubicación", "Debe pertenecer al depósito seleccionado.");
        inventoryElement(texts, "stock", "availability_condition", "Condición", "Disponible, en cuarentena o dañado.");
        inventoryElement(texts, "stock", "availability_lot", "Lote", "Obligatorio únicamente para seguimiento por lote.");
        inventoryElement(texts, "stock", "availability_serial", "Número de serie", "Obligatorio únicamente para seguimiento serial.");
        inventoryElement(texts, "stock", "availability_expiry", "Fecha de vencimiento", "Formato ISO AAAA-MM-DD cuando corresponda.");
        inventoryElement(texts, "stock", "check_availability", "Consultar", "Muestra físico, reservado y disponible para la posición.");
        inventoryElement(texts, "stock", "movement_type", "Tipo de movimiento", "Entrada, salida o transferencia manual controlada.");
        inventoryElement(texts, "stock", "movement_warehouse", "Depósito de origen", "En una entrada representa el depósito receptor.");
        inventoryElement(texts, "stock", "movement_location", "Ubicación de origen", "En una entrada representa la ubicación receptora.");
        inventoryElement(texts, "stock", "movement_target_warehouse", "Depósito destino", "Obligatorio solamente para transferencias.");
        inventoryElement(texts, "stock", "movement_target_location", "Ubicación destino", "Debe ser distinta del origen en una transferencia.");
        inventoryElement(texts, "stock", "movement_condition", "Condición", "La transferencia conserva esta condición.");
        inventoryElement(texts, "stock", "movement_lot", "Lote", "Dimensión requerida por la política del artículo.");
        inventoryElement(texts, "stock", "movement_serial", "Número de serie", "Una serie admite cantidad base uno por línea.");
        inventoryElement(texts, "stock", "movement_expiry", "Fecha de vencimiento", "Formato ISO AAAA-MM-DD cuando corresponda.");
        inventoryElement(texts, "stock", "movement_quantity", "Cantidad", "Cantidad positiva en la unidad base mostrada en la ficha.");
        inventoryElement(texts, "stock", "movement_reason", "Motivo", "Código estable que explica el movimiento.");
        inventoryElement(texts, "stock", "movement_source_type", "Tipo de origen", "Identifica el proceso o documento que solicita el movimiento.");
        inventoryElement(texts, "stock", "movement_source_id", "Identidad de origen", "Referencia externa neutral, sin importar DTO de otro plugin.");
        inventoryElement(texts, "stock", "movement_idempotency", "Clave de idempotencia", "Reutilizarla con otros datos produce un conflicto seguro.");
        inventoryElement(texts, "stock", "post_movement", "Registrar movimiento", "Confirma el asiento inmutable de existencias.");

        inventoryElement(texts, "movements", "movement_item", "Artículo", "Selecciona un producto activo incorporado al inventario.");
        inventoryElement(texts, "movements", "movement_type", "Tipo de movimiento", "Entrada, salida o transferencia manual controlada.");
        inventoryElement(texts, "movements", "movement_warehouse", "Depósito de origen", "En una entrada representa el depósito receptor.");
        inventoryElement(texts, "movements", "movement_location", "Ubicación de origen", "En una entrada representa la ubicación receptora.");
        inventoryElement(texts, "movements", "movement_target_warehouse", "Depósito destino", "Aparece únicamente para transferencias.");
        inventoryElement(texts, "movements", "movement_target_location", "Ubicación destino", "Debe ser distinta del origen.");
        inventoryElement(texts, "movements", "movement_condition", "Condición", "Disponible, en cuarentena o dañado.");
        inventoryElement(texts, "movements", "movement_lot", "Lote", "Aparece cuando el artículo usa seguimiento por lote.");
        inventoryElement(texts, "movements", "movement_serial", "Número de serie", "Aparece cuando el artículo usa seguimiento serial.");
        inventoryElement(texts, "movements", "movement_expiry", "Fecha de vencimiento", "Aparece cuando la política admite vencimiento.");
        inventoryElement(texts, "movements", "movement_quantity", "Cantidad", "Cantidad positiva en la unidad base del artículo.");
        inventoryElement(texts, "movements", "movement_reason", "Motivo", "Código breve y estable que explica la operación.");
        inventoryElement(texts, "movements", "movement_idempotency", "Referencia técnica", "Token interno generado por el servidor y no editable.");
        inventoryElement(texts, "movements", "movement_guidance", "Datos aplicables", "El formulario adapta destino, lote, serie y vencimiento al tipo y al artículo.");
        inventoryElement(texts, "movements", "movement_summary", "Resumen de la operación", "Revisa dirección, cantidad y unidad antes de confirmar.");
        inventoryElement(texts, "movements", "post_movement", "Registrar movimiento", "Confirma un asiento inmutable y auditado.");
        inventoryElement(texts, "stock", "reservation_warehouse", "Depósito", "Depósito de la posición a reservar.");
        inventoryElement(texts, "stock", "reservation_location", "Ubicación", "Ubicación exacta de la reserva.");
        inventoryElement(texts, "stock", "reservation_condition", "Condición", "La reserva se aplica a una sola condición.");
        inventoryElement(texts, "stock", "reservation_lot", "Lote", "Completa según el seguimiento del artículo.");
        inventoryElement(texts, "stock", "reservation_serial", "Número de serie", "Completa según el seguimiento del artículo.");
        inventoryElement(texts, "stock", "reservation_expiry_date", "Vencimiento del producto", "Fecha del lote o serie en formato AAAA-MM-DD.");
        inventoryElement(texts, "stock", "reservation_quantity", "Cantidad", "Cantidad positiva que no puede superar lo disponible.");
        inventoryElement(texts, "stock", "reservation_expires_at", "La reserva vence en", "Instante ISO-8601, por ejemplo 2026-08-01T12:00:00Z.");
        inventoryElement(texts, "stock", "reservation_source_type", "Tipo de origen", "Proceso o documento que solicita la reserva.");
        inventoryElement(texts, "stock", "reservation_source_id", "Identidad de origen", "Referencia estable de la operación externa.");
        inventoryElement(texts, "stock", "reservation_idempotency", "Clave de idempotencia", "Evita crear dos reservas por el mismo intento.");
        inventoryElement(texts, "stock", "create_reservation", "Crear reserva", "Separa cantidad disponible hasta su consumo, liberación o vencimiento.");
        inventoryElement(texts, "stock", "manage_reservation_id", "Identidad de reserva", "UUID entregado al crear la reserva.");
        inventoryElement(texts, "stock", "manage_reservation_version", "Versión de reserva", "Debe coincidir con la versión consultada.");
        inventoryElement(texts, "stock", "manage_reservation_quantity", "Cantidad", "Obligatoria para consumir o liberar parcialmente.");
        inventoryElement(texts, "stock", "manage_reservation_idempotency", "Clave de operación", "Identifica de forma única este consumo, liberación o vencimiento.");
        inventoryElement(texts, "stock", "consume_reservation", "Consumir", "Descuenta la cantidad reservada y la existencia física.");
        inventoryElement(texts, "stock", "release_reservation", "Liberar", "Devuelve cantidad reservada a disponibilidad.");
        inventoryElement(texts, "stock", "expire_reservation", "Marcar vencida", "Vence la reserva cuando corresponde por fecha y estado.");
        inventoryElement(texts, "stock", "refresh_stock_item", "Actualizar desde catálogo", "Refresca código, nombre, unidad y versión del producto activo.");
        inventoryElement(texts, "stock", "inactivate_stock_item", "Inactivar artículo", "Solo se permite sin cantidades físicas o reservadas.");

        inventoryElement(texts, "counts", "count_search_state", "Estado", "Filtra borradores, conteos, revisiones y resultados finales.");
        inventoryElement(texts, "counts", "count_search", "Buscar", "Actualiza el directorio de conteos.");
        inventoryElement(texts, "counts", "count_results", "Conteos físicos", "Muestra alcance, etapa y cantidad de líneas.");
        inventoryElement(texts, "counts", "select_count", "Abrir", "Muestra detalle, captura y transiciones autorizadas.");
        inventoryElement(texts, "counts", "count_new_warehouse", "Depósito", "Define el alcance principal del conteo.");
        inventoryElement(texts, "counts", "count_new_location", "Ubicación opcional", "Selecciona todo el depósito o una única ubicación.");
        inventoryElement(texts, "counts", "draft_count", "Preparar conteo", "Crea un borrador sin bloquear movimientos todavía.");
        inventoryElement(texts, "counts", "count_line_item", "Artículo", "Producto activo que participa en la posición contada.");
        inventoryElement(texts, "counts", "count_line_location", "Ubicación", "Debe quedar dentro del alcance definido.");
        inventoryElement(texts, "counts", "count_line_condition", "Condición", "Condición exacta de la posición.");
        inventoryElement(texts, "counts", "count_line_lot", "Lote", "Completa según el seguimiento del artículo.");
        inventoryElement(texts, "counts", "count_line_serial", "Número de serie", "Completa según el seguimiento del artículo.");
        inventoryElement(texts, "counts", "count_line_expiry", "Fecha de vencimiento", "Formato ISO AAAA-MM-DD cuando corresponda.");
        inventoryElement(texts, "counts", "add_count_line", "Agregar línea", "Captura la existencia teórica actual de la posición.");
        inventoryElement(texts, "counts", "count_capture_line", "Línea", "Selecciona una posición del conteo iniciado.");
        inventoryElement(texts, "counts", "count_capture_quantity", "Cantidad contada", "Cantidad física observada, incluido cero.");
        inventoryElement(texts, "counts", "record_count", "Registrar cantidad", "Actualiza la línea con control de versión.");
        inventoryElement(texts, "counts", "start_count", "Iniciar conteo", "Verifica saldos teóricos y bloquea el alcance controlado.");
        inventoryElement(texts, "counts", "review_count", "Enviar a revisión", "Exige que todas las líneas tengan cantidad contada.");
        inventoryElement(texts, "counts", "post_count", "Contabilizar", "Genera el ajuste inmutable con permiso específico.");
        inventoryElement(texts, "counts", "cancel_count", "Cancelar", "Finaliza el conteo sin generar ajustes.");

        texts.put("purchasing.menu.requests", "Solicitudes de compra");
        texts.put("purchasing.menu.orders", "Órdenes de compra");
        texts.put("purchasing.menu.receipts", "Recepciones");
        texts.put("purchasing.menu.returns", "Devoluciones");
        texts.put("purchasing.menu.tracking", "Seguimiento");

        purchasingElement(texts, "requests", "request_search_text", "Número o descripción", "Busca solicitudes dentro de la empresa activa.");
        purchasingElement(texts, "requests", "request_search_state", "Estado", "Filtra borradores, pendientes, aprobadas, rechazadas o canceladas.");
        purchasingElement(texts, "requests", "request_search", "Buscar", "Actualiza el directorio con los filtros actuales.");
        purchasingElement(texts, "requests", "request_results", "Solicitudes", "Muestra fecha, estado y cantidad de líneas.");
        purchasingElement(texts, "requests", "request_lines", "Líneas de la solicitud", "Mantiene el documento visible mientras agregas conceptos en el mismo espacio de trabajo.");
        purchasingElement(texts, "requests", "request_summary", "Resumen de la solicitud", "Revisa estado, solicitante y cantidad de líneas antes de decidir.");
        purchasingElement(texts, "requests", "select_request", "Abrir", "Muestra las líneas y las acciones autorizadas.");
        purchasingElement(texts, "requests", "request_number", "Número", "Código único de la solicitud dentro de la empresa.");
        purchasingElement(texts, "requests", "request_date", "Fecha solicitada", "Fecha en formato AAAA-MM-DD.");
        purchasingElement(texts, "requests", "request_kind", "Tipo de línea", "Producto de stock o servicio sin movimiento de inventario.");
        purchasingElement(texts, "requests", "request_item", "Artículo o servicio", "Selecciona un concepto activo del catálogo cuando corresponda.");
        purchasingElement(texts, "requests", "request_description", "Descripción", "Explica con precisión qué se necesita comprar.");
        purchasingElement(texts, "requests", "request_unit", "Unidad", "Unidad en que se expresa la cantidad solicitada.");
        purchasingElement(texts, "requests", "request_quantity", "Cantidad", "Valor positivo con hasta seis decimales.");
        purchasingElement(texts, "requests", "request_expected_price", "Precio esperado", "Estimación opcional; no compromete una orden.");
        purchasingElement(texts, "requests", "request_currency", "Moneda estimada", "Moneda habilitada para el precio esperado.");
        purchasingElement(texts, "requests", "create_request", "Preparar solicitud", "Crea un borrador con su primera línea.");
        purchasingElement(texts, "requests", "request_add_kind", "Tipo de línea", "Define si la línea adicional es producto o servicio.");
        purchasingElement(texts, "requests", "request_add_item", "Artículo o servicio", "Concepto activo relacionado con la nueva línea.");
        purchasingElement(texts, "requests", "request_add_description", "Descripción", "Texto que verá quien revise y compre.");
        purchasingElement(texts, "requests", "request_add_unit", "Unidad", "Unidad de la cantidad adicional.");
        purchasingElement(texts, "requests", "request_add_quantity", "Cantidad", "Cantidad positiva solicitada.");
        purchasingElement(texts, "requests", "request_add_expected_price", "Precio esperado", "Estimación opcional para la línea adicional.");
        purchasingElement(texts, "requests", "request_add_currency", "Moneda estimada", "Completa junto con el precio cuando corresponda.");
        purchasingElement(texts, "requests", "add_request_line", "Agregar línea", "Reemplaza el borrador por una versión que conserva las líneas anteriores y agrega la nueva.");
        purchasingElement(texts, "requests", "request_reason", "Motivo", "Explica un rechazo o una cancelación.");
        purchasingElement(texts, "requests", "submit_request", "Enviar a aprobación", "Cambia el borrador a pendiente de aprobación.");
        purchasingElement(texts, "requests", "approve_request", "Aprobar", "Autoriza la solicitud para usarla en órdenes.");
        purchasingElement(texts, "requests", "reject_request", "Rechazar", "Finaliza la solicitud y conserva el motivo.");
        purchasingElement(texts, "requests", "cancel_request", "Cancelar", "Impide continuar el documento y conserva el motivo.");
        purchasingElement(texts, "requests", "request_clone_number", "Número de la copia", "Número único que tendrá el nuevo borrador.");
        purchasingElement(texts, "requests", "request_clone_date", "Fecha de la copia", "Fecha solicitada del nuevo borrador en formato AAAA-MM-DD.");
        purchasingElement(texts, "requests", "clone_request", "Clonar", "Crea otra solicitud en borrador con las mismas líneas.");

        purchasingElement(texts, "orders", "order_search_text", "Número o proveedor", "Busca órdenes dentro de la empresa activa.");
        purchasingElement(texts, "orders", "order_search_state", "Estado", "Filtra borradores, emitidas, cerradas o canceladas.");
        purchasingElement(texts, "orders", "order_search", "Buscar", "Actualiza el directorio con los filtros actuales.");
        purchasingElement(texts, "orders", "order_results", "Órdenes", "Muestra proveedor, moneda, estado y cantidad de líneas.");
        purchasingElement(texts, "orders", "order_lines", "Líneas de la orden", "Muestra cantidades, precios, asignaciones y cumplimiento sin abandonar el editor.");
        purchasingElement(texts, "orders", "order_summary", "Resumen de la orden", "Revisa proveedor, estado, total y saldo pendiente antes de emitir o cerrar.");
        purchasingElement(texts, "orders", "select_order", "Abrir", "Muestra importes, cumplimiento y acciones autorizadas.");
        purchasingElement(texts, "orders", "order_number", "Número", "Código único de la orden dentro de la empresa.");
        purchasingElement(texts, "orders", "order_supplier", "Proveedor", "Socio comercial activo con rol proveedor.");
        purchasingElement(texts, "orders", "order_currency", "Moneda", "Moneda habilitada en que se expresan los precios.");
        purchasingElement(texts, "orders", "order_justification", "Justificación de compra directa", "Obligatoria cuando alguna cantidad no proviene de una solicitud aprobada.");
        purchasingElement(texts, "orders", "order_kind", "Tipo de línea", "Producto de stock o servicio.");
        purchasingElement(texts, "orders", "order_item", "Artículo o servicio", "Concepto activo del catálogo comercial.");
        purchasingElement(texts, "orders", "order_description", "Descripción", "Snapshot que se conservará en la orden.");
        purchasingElement(texts, "orders", "order_unit", "Unidad", "Unidad contractual de la cantidad ordenada.");
        purchasingElement(texts, "orders", "order_quantity", "Cantidad", "Cantidad positiva ordenada.");
        purchasingElement(texts, "orders", "order_price", "Precio unitario", "Importe unitario en la moneda de la orden.");
        purchasingElement(texts, "orders", "order_request", "Solicitud aprobada", "Origen opcional para justificar la necesidad.");
        purchasingElement(texts, "orders", "order_request_line", "Línea solicitada", "Debe pertenecer a la solicitud seleccionada.");
        purchasingElement(texts, "orders", "order_allocation_quantity", "Cantidad asignada", "Parte de la orden imputada a la línea solicitada.");
        purchasingElement(texts, "orders", "create_order", "Preparar orden", "Crea la orden en borrador con su primera línea.");
        purchasingElement(texts, "orders", "order_add_kind", "Tipo de línea", "Producto o servicio de la línea adicional.");
        purchasingElement(texts, "orders", "order_add_item", "Artículo o servicio", "Concepto activo relacionado con la línea adicional.");
        purchasingElement(texts, "orders", "order_add_description", "Descripción", "Texto contractual conservado en la orden.");
        purchasingElement(texts, "orders", "order_add_unit", "Unidad", "Unidad de la cantidad adicional.");
        purchasingElement(texts, "orders", "order_add_quantity", "Cantidad", "Cantidad positiva adicional.");
        purchasingElement(texts, "orders", "order_add_price", "Precio unitario", "Importe unitario adicional.");
        purchasingElement(texts, "orders", "add_order_line", "Agregar línea", "Solo está disponible mientras la orden permanezca en borrador.");
        purchasingElement(texts, "orders", "order_reason", "Motivo", "Explica la cancelación o el cierre con faltante.");
        purchasingElement(texts, "orders", "issue_order", "Emitir orden", "Formaliza el compromiso con el proveedor.");
        purchasingElement(texts, "orders", "cancel_order", "Cancelar orden", "Cancela cuando el estado y las recepciones lo permiten.");
        purchasingElement(texts, "orders", "close_order_short", "Cerrar con faltante", "Cierra todas las cantidades todavía pendientes y conserva el motivo.");

        purchasingElement(texts, "receipts", "receipt_search_text", "Número u orden", "Busca recepciones dentro de la empresa activa.");
        purchasingElement(texts, "receipts", "receipt_search_state", "Estado", "Filtra borradores o confirmadas.");
        purchasingElement(texts, "receipts", "receipt_search", "Buscar", "Actualiza el directorio con los filtros actuales.");
        purchasingElement(texts, "receipts", "receipt_results", "Recepciones", "Muestra orden, estado y cantidad de líneas.");
        purchasingElement(texts, "receipts", "receipt_guidance", "Datos aplicables", "El destino y la trazabilidad aparecen sólo cuando la línea mueve existencias.");
        purchasingElement(texts, "receipts", "receipt_summary", "Impacto de la recepción", "Revisa orden, línea, cantidad y destino antes de confirmar.");
        purchasingElement(texts, "receipts", "select_receipt", "Abrir", "Muestra trazabilidad y confirmación disponible.");
        purchasingElement(texts, "receipts", "receipt_number", "Número", "Código único de la recepción dentro de la empresa.");
        purchasingElement(texts, "receipts", "receipt_order", "Orden emitida", "Orden que entrega el proveedor.");
        purchasingElement(texts, "receipts", "receipt_order_line", "Línea ordenada", "Línea y cantidad pendiente que se reciben.");
        purchasingElement(texts, "receipts", "receipt_quantity", "Cantidad recibida", "No puede superar la cantidad pendiente.");
        purchasingElement(texts, "receipts", "receipt_warehouse", "Depósito", "Obligatorio para productos de stock.");
        purchasingElement(texts, "receipts", "receipt_location", "Ubicación", "Debe estar activa y pertenecer al depósito seleccionado.");
        purchasingElement(texts, "receipts", "receipt_lot", "Lote", "Completa cuando el producto se controla por lote.");
        purchasingElement(texts, "receipts", "receipt_serial", "Número de serie", "Completa cuando el producto se controla por serie.");
        purchasingElement(texts, "receipts", "receipt_expiry", "Fecha de vencimiento", "Formato AAAA-MM-DD cuando corresponda.");
        purchasingElement(texts, "receipts", "receipt_condition", "Condición", "Disponible, cuarentena o dañado al ingresar.");
        purchasingElement(texts, "receipts", "create_receipt", "Preparar recepción", "Crea un comprobante interno todavía sin afectar inventario.");
        purchasingElement(texts, "receipts", "confirm_receipt", "Confirmar recepción", "Actualiza cumplimiento y publica el ingreso de stock cuando corresponde.");

        purchasingElement(texts, "returns", "return_search_text", "Número, orden o causa", "Busca devoluciones dentro de la empresa activa.");
        purchasingElement(texts, "returns", "return_search_state", "Estado", "Filtra borradores o confirmadas.");
        purchasingElement(texts, "returns", "return_search", "Buscar", "Actualiza el directorio con los filtros actuales.");
        purchasingElement(texts, "returns", "return_results", "Devoluciones", "Muestra orden, causa, estado y cantidad de líneas.");
        purchasingElement(texts, "returns", "return_guidance", "Datos aplicables", "La devolución parte de una recepción confirmada y conserva su trazabilidad.");
        purchasingElement(texts, "returns", "return_summary", "Impacto de la devolución", "Revisa recepción, línea, cantidad y causa antes de confirmar.");
        purchasingElement(texts, "returns", "select_return", "Abrir", "Muestra el origen y la confirmación disponible.");
        purchasingElement(texts, "returns", "return_number", "Número", "Código único de la devolución dentro de la empresa.");
        purchasingElement(texts, "returns", "return_order", "Orden", "Orden emitida o cerrada a la que pertenece la devolución.");
        purchasingElement(texts, "returns", "return_receipt", "Recepción confirmada", "Comprobante que prueba la cantidad recibida.");
        purchasingElement(texts, "returns", "return_receipt_line", "Línea recibida", "Línea exacta desde la que se devuelve.");
        purchasingElement(texts, "returns", "return_quantity", "Cantidad devuelta", "No puede superar la cantidad confirmada todavía no devuelta.");
        purchasingElement(texts, "returns", "return_reason", "Causa", "Explica por qué la mercadería o el servicio se devuelve.");
        purchasingElement(texts, "returns", "create_return", "Preparar devolución", "Crea un borrador relacionado con la recepción.");
        purchasingElement(texts, "returns", "confirm_return", "Confirmar devolución", "Actualiza cumplimiento y publica la salida de stock cuando corresponde.");

        purchasingElement(texts, "tracking", "tracking_search_text", "Número o proveedor", "Busca órdenes para consultar su cumplimiento.");
        purchasingElement(texts, "tracking", "tracking_search_state", "Estado", "Filtra órdenes por su etapa vigente.");
        purchasingElement(texts, "tracking", "tracking_search", "Buscar", "Actualiza el seguimiento con los filtros actuales.");
        purchasingElement(texts, "tracking", "tracking_results", "Cumplimiento de órdenes", "Resume las órdenes disponibles para seguimiento.");
        purchasingElement(texts, "tracking", "tracking_summary", "Detalle de cumplimiento", "Muestra cantidades ordenadas, recibidas, devueltas, cerradas y pendientes.");
        purchasingElement(texts, "tracking", "select_tracking_order", "Ver seguimiento", "Muestra cantidades pedidas, recibidas, devueltas y pendientes.");
        return Map.copyOf(texts);
    }

    private static void element(
            Map<String, String> texts, String elementId, String label, String help) {
        businessPartnerElement(texts, "directory", elementId, label, help);
    }

    private static void referenceDataElement(
            Map<String, String> texts, String element, String label, String help) {
        texts.put("reference_data.catalogs." + element + ".label", label);
        texts.put("reference_data.catalogs." + element + ".help", help);
    }

    private static void businessPartnerElement(
            Map<String, String> texts,
            String screen,
            String elementId,
            String label,
            String help) {
        String key = "business_partners." + screen + "." + elementId;
        texts.put(key + ".label", label);
        texts.put(key + ".help", help);
    }

    private static void catalogElement(
            Map<String, String> texts,
            String screen,
            String elementId,
            String label,
            String help) {
        String key = "commercial_catalog." + screen + "." + elementId;
        texts.put(key + ".label", label);
        texts.put(key + ".help", help);
    }

    private static void inventoryElement(
            Map<String, String> texts,
            String screen,
            String elementId,
            String label,
            String help) {
        String key = "inventory." + screen + "." + elementId;
        texts.put(key + ".label", label);
        texts.put(key + ".help", help);
    }

    private static void purchasingElement(
            Map<String, String> texts,
            String screen,
            String elementId,
            String label,
            String help) {
        String key = "purchasing." + screen + "." + elementId;
        texts.put(key + ".label", label);
        texts.put(key + ".help", help);
    }
}
