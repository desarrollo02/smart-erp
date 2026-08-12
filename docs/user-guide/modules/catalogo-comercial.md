<article>
  <div class="page-footer">LogixOne · Manual de Catálogo comercial · edición 2026-08-11 · página <span class="page-number"></span></div>
  <header class="cover">
    <div class="eyebrow">Manual de usuario por módulo</div>
    <h1>Catálogo comercial</h1>
    <p class="subtitle">Artículos y servicios, unidades, clasificaciones, variantes, perfiles tributarios y listas de precios.</p>
    <div class="meta"><strong>Versión documentada:</strong> plugin <code>commercial_catalog</code> 1.0, baseline Sprint 8.<br><strong>Audiencia:</strong> responsables de datos maestros, precios, impuestos internos y soporte.<br><strong>Dependencia:</strong> Datos de referencia para monedas y otros códigos oficiales, consumidos por contrato público.</div>
  </header>

  <section class="toc">
    <h2>Conceptos y permisos</h2>
    <table><thead><tr><th>Permiso</th><th>Capacidad</th></tr></thead><tbody><tr><td><code>commercial_catalog.view</code></td><td>Consultar todas las pantallas.</td></tr><tr><td><code>commercial_catalog.items.manage</code></td><td>Crear y mantener artículos/servicios.</td></tr><tr><td><code>commercial_catalog.prices.manage</code></td><td>Administrar listas y entradas de precio.</td></tr><tr><td><code>commercial_catalog.definitions.manage</code></td><td>Unidades, categorías, marcas, etiquetas, variantes y perfiles tributarios.</td></tr></tbody></table>
    <h3>Glosario</h3>
    <dl class="term-grid"><dt>Artículo</dt><dd>Bien comercializable que puede ser inventariable.</dd><dt>Servicio</dt><dd>Prestación comercial no necesariamente inventariable.</dd><dt>Alcance</dt><dd>Capacidad habilitada para el ítem, por ejemplo compra o venta.</dd><dt>Unidad base</dt><dd>Unidad canónica en la que se expresa el ítem.</dd><dt>Conversión</dt><dd>Factor que transforma una unidad alternativa a la unidad base.</dd><dt>Propósito de unidad</dt><dd>Uso permitido de una unidad, con opción predeterminada.</dd><dt>Identificador</dt><dd>Código alternativo presentado y normalizado para localizar el ítem.</dd><dt>Categoría principal</dt><dd>Clasificación primaria; puede coexistir con otras categorías.</dd><dt>Variante</dt><dd>Combinación de atributos gobernados por una familia y versión.</dd><dt>Perfil tributario</dt><dd>Clasificación interna con revisiones vigentes en intervalos.</dd><dt>Lista de precios</dt><dd>Conjunto de reglas de precio con moneda, impuestos, escala y redondeo.</dd><dt>Vigencia</dt><dd>Intervalo desde/hasta en que una revisión o precio aplica.</dd><dt>Reemplazo</dt><dd>Definición sucesora que conserva trazabilidad de la anterior.</dd></dl>
  </section>

  <section class="screen" data-screen="catalog-items">
    <div class="screen-title"><h2>1. Artículos y servicios</h2><span class="route">/faces/catalog</span></div>
    <p><strong>Objetivo:</strong> registrar la identidad comercial del ítem y completar clasificaciones, unidades, tributación y variantes.</p>
    <p><strong>Permiso:</strong> consulta con <code>commercial_catalog.view</code>; cambios con <code>commercial_catalog.items.manage</code>.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Catálogo · Artículos y servicios ─────────────────────────────────┐
│ Buscar [código/nombre] Tipo [▼] Estado [▼] [Buscar]                │
├ Resultados ────────────┬ Ficha seleccionada ───────────────────────┤
│ código · nombre · tipo │ nombres/descripción/alcances · identificadores│
│ [Seleccionar]          │ categoría/marca · unidades · impuestos    │
│                        │ familia/atributos de variante · ciclo vida│
├ Nuevo: código · nombre · descripción · tipo · alcance              │
│ unidad base · perfil tributario                        [Registrar] │
└────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos y controles</h3>
    <table><thead><tr><th>Grupo</th><th>Datos explicados</th><th>Reglas</th></tr></thead><tbody>
      <tr><td>Búsqueda</td><td>Texto por código/nombre, tipo, estado; resultados y Seleccionar.</td><td>Filtros de consulta; empresa activa implícita.</td></tr>
      <tr><td>Alta</td><td>Código, nombre visible, descripción, tipo de ítem, alcance inicial, unidad base y perfil tributario/revisión.</td><td>Código y nombre obligatorios; código único por empresa. Tipo y unidad base condicionan operaciones posteriores.</td></tr>
      <tr><td>Edición</td><td>Código de referencia, nombre, descripción y alcances; acciones Cambiar/Actualizar.</td><td>El código permanece estable; versión evita sobrescritura concurrente.</td></tr>
      <tr><td>Identificador</td><td>Tipo, valor presentado, valor normalizado, estado y fechas.</td><td>Agregar conserva cómo fue ingresado y la forma usada para búsqueda/unicidad.</td></tr>
      <tr><td>Clasificación</td><td>Categoría principal, categorías secundarias, marca y etiquetas.</td><td>Las definiciones deben estar activas. Una sola categoría puede ser principal.</td></tr>
      <tr><td>Unidad alternativa</td><td>Unidad, factor a base, estado; propósito e indicador predeterminado.</td><td>Factor decimal positivo. La unidad base representa factor 1; una unidad/purpose predeterminada debe ser coherente.</td></tr>
      <tr><td>Perfil tributario</td><td>Perfil y versión vigente asignada.</td><td>Es una clasificación interna; no equivale por sí sola a certificación fiscal.</td></tr>
      <tr><td>Variante</td><td>Familia, versión de estructura, atributos, tipo de valor y valor.</td><td>Los atributos deben pertenecer a la revisión elegida; obligatorios no pueden quedar vacíos.</td></tr>
      <tr><td>Ciclo de vida</td><td>Activar/Inactivar y reemplazo opcional por otro ítem.</td><td>Inactivar conserva historia y referencias; no elimina precios ni movimientos externos.</td></tr>
    </tbody></table>
    <h3>Secuencia recomendada</h3>
    <ol><li>Prepare unidades, categorías, marca, perfiles y familia de variante.</li><li>Busque por código/identificador para evitar duplicados.</li><li>Registre la identidad y unidad base.</li><li>Agregue clasificaciones e identificadores.</li><li>Configure conversiones y propósitos de unidad.</li><li>Asigne perfil tributario y, si aplica, variante.</li><li>Revise y active para consumo por otros módulos.</li></ol>
    <p class="warning"><strong>Errores frecuentes:</strong> código/identificador duplicado, definición inactiva, factor cero o negativo, revisión tributaria/familia antigua, atributo requerido vacío o conflicto de versión. Actualice la ficha antes de reintentar.</p>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">catalog_item</div><div>Empresa + ítem (PK); código (UK), nombre, descripción, tipo, estado, unidad base (FK), perfil/revisión tributaria (FK), marca (FK), reemplazo (FK propia), versión/fechas.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">catalog_item_scope</div><div>Empresa + ítem + alcance (PK/FK).</div><div class="crud">C/R/D</div></div>
      <div class="db-row"><div class="db-name">catalog_item_identifier</div><div>ID (PK/FK ítem), tipo, valor presentado/normalizado, activo y fechas.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">catalog_item_category</div><div>Ítem + categoría (PK/FK), indicador principal.</div><div class="crud">C/R/U/D</div></div>
      <div class="db-row"><div class="db-name">catalog_item_tag</div><div>Ítem + etiqueta (PK/FK).</div><div class="crud">C/R/D</div></div>
      <div class="db-row"><div class="db-name">catalog_item_unit_conversion</div><div>Ítem + unidad (PK/FK), factor a base y estado.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">catalog_item_unit_purpose</div><div>Ítem + unidad + propósito (PK/FK), indicador predeterminado.</div><div class="crud">C/R/U/D</div></div>
      <div class="db-row"><div class="db-name">catalog_item_variant_assignment</div><div>Un ítem (PK/FK) → familia y versión (FK).</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">catalog_item_variant_attribute</div><div>Ítem + atributo (PK/FK); familia, tipo, valor y versión.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">Definiciones</div><div>Unidad, categoría, marca, etiqueta, perfil tributario y familia/revisión consultados por FK.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">catalog_code_sequence</div><div>Empresa + alcance (PK); próximo valor y fecha cuando se usa numeración gobernada.</div><div class="crud">R/U</div></div>
      <p class="relation">Las relaciones son internas al esquema del catálogo. Los países/monedas oficiales llegan desde Datos de referencia por <strong>EXT</strong>. No se encontraron triggers de usuario para el ítem.</p>
    </div>
  </section>

  <section class="screen" data-screen="price-lists">
    <div class="screen-title"><h2>2. Listas de precios</h2><span class="route">/faces/catalog/price-lists</span></div>
    <p><strong>Objetivo:</strong> crear listas monetarias y mantener precios por artículo, unidad, cantidad mínima y vigencia.</p>
    <p><strong>Permiso:</strong> <code>commercial_catalog.prices.manage</code> para cambios.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Listas de precios ────────────────────────────────────────────────┐
│ Buscar [código/nombre] Estado [▼] [Buscar]                         │
├ Listas ────────────────┬ Lista seleccionada ───────────────────────┤
│ código · moneda · modo │ nombre [Renombrar] [Activar/Inactivar]    │
│ [Seleccionar]          │ Entradas: artículo · unidad · mínimo      │
├ Nueva lista ───────────┤ monto · desde/hasta [Agregar precio]      │
│ código, nombre, moneda │ entradas vigentes [Inactivar entrada]     │
│ impuesto, escala, redondeo [Registrar]                             │
└────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos y reglas</h3>
    <table><thead><tr><th>Dato</th><th>Significado</th><th>Regla</th></tr></thead><tbody>
      <tr><td>Código / nombre</td><td>Identidad estable y etiqueta de lista.</td><td>Código único por empresa; Renombrar no cambia código.</td></tr>
      <tr><td>Moneda</td><td>Código oficial habilitado para la empresa.</td><td>Proviene de Datos de referencia por contrato.</td></tr>
      <tr><td>Modo tributario</td><td>Indica cómo se interpretan impuestos en los montos.</td><td>Selector cerrado; debe comunicarse a consumidores.</td></tr>
      <tr><td>Escala</td><td>Cantidad de decimales almacenados/presentados.</td><td>Entero dentro del rango del dominio.</td></tr>
      <tr><td>Redondeo</td><td>Regla cerrada para ajustar decimales.</td><td>No es texto libre.</td></tr>
      <tr><td>Artículo / unidad</td><td>Ítem y unidad a los que aplica el precio.</td><td>La unidad debe ser válida para el ítem.</td></tr>
      <tr><td>Cantidad mínima</td><td>Umbral decimal desde el que aplica.</td><td>No negativa; distingue escalas de precio.</td></tr>
      <tr><td>Monto</td><td>Precio decimal en la moneda de la lista.</td><td>Respeta escala y reglas de negocio.</td></tr>
      <tr><td>Válido desde/hasta</td><td>Intervalo temporal; hasta puede quedar abierto.</td><td>Hasta no puede preceder a desde. No se permiten intervalos activos superpuestos para la misma clave comercial.</td></tr>
      <tr><td>Activo</td><td>Disponibilidad de lista o entrada.</td><td>Inactivar conserva historia; una entrada inactiva no compite por vigencia activa.</td></tr>
    </tbody></table>
    <ol><li>Confirme moneda, impuestos, escala y redondeo antes de registrar: definen toda la lista.</li><li>Seleccione el artículo y una unidad válida.</li><li>Ingrese cantidad mínima, monto y vigencia.</li><li>Revise que no exista una entrada activa para el mismo tramo y periodo.</li><li>Agregue el precio y confirme su aparición.</li></ol>
    <p class="warning"><strong>Solapamiento:</strong> si dos sesiones intentan crear vigencias superpuestas, la base bloquea el alcance y rechaza una operación con restricción <code>uq_price_entry_validity</code>. Corrija fechas o inactive la entrada anterior; no repita sin revisar.</p>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram"><div class="db-row"><div class="db-name">price_list</div><div>Empresa + lista (PK); código (UK), nombre, moneda, modo tributario, escala, redondeo, estado, versión y fechas.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">price_entry</div><div>Empresa + lista + entrada (PK/FK); ítem/unidad (FK), mínimo, monto, desde/hasta, activo y fechas.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">catalog_item / unit</div><div>Ítem y unidad válidos para el precio.</div><div class="crud">R</div></div><div class="db-row"><div class="db-name">enforce_price_entry_no_overlap()</div><div>Advisory lock por alcance y rechazo SQLSTATE <code>23505</code> cuando existe vigencia activa superpuesta.</div><div class="crud">TRIGGER</div></div><p class="relation"><strong>Trigger:</strong> <code>trg_price_entry_overlap</code>, BEFORE INSERT/UPDATE cuando <code>NEW.active</code>. Moneda se valida por contrato con Datos de referencia (<strong>EXT</strong>).</p></div>
  </section>

  <section class="screen" data-screen="catalog-definitions">
    <div class="screen-title"><h2>3. Definiciones del catálogo</h2><span class="route">/faces/catalog/definitions</span></div>
    <p><strong>Objetivo:</strong> gobernar unidades, categorías, marcas y etiquetas con revisión, jerarquía y reemplazo.</p>
    <p><strong>Permiso:</strong> <code>commercial_catalog.definitions.manage</code>.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Definiciones del catálogo ───────────────────────────────────────┐
│ Tipo [Unidad/Categoría/Marca/Etiqueta▼] Buscar [...] Estado [▼]    │
├ Resultados ───────────┬ Detalle e historial ───────────────────────┤
│ código · nombre       │ versión · escala/jerarquía · estado        │
├ Nueva definición ─────┴────────────────────────────────────────────┤
│ código · nombre · escala(unidad) · padre(categoría) [Registrar]    │
│ Nueva revisión […] · Reemplazo […] [Reemplazar] [Activar/Inactivar]│
└────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos</h3>
    <table><thead><tr><th>Dato</th><th>Aplicación</th><th>Regla</th></tr></thead><tbody>
      <tr><td>Tipo</td><td>Unidad, categoría, marca o etiqueta.</td><td>Cerrado; determina campos disponibles.</td></tr><tr><td>Código</td><td>Identificador estable y único por empresa/tipo.</td><td>No cambia con revisiones.</td></tr><tr><td>Nombre</td><td>Etiqueta mostrada a usuarios.</td><td>Obligatoria.</td></tr><tr><td>Escala decimal</td><td>Solo unidad: decimales admitidos.</td><td>Entero dentro del rango del dominio.</td></tr><tr><td>Categoría padre</td><td>Solo categoría: jerarquía opcional.</td><td>Debe ser categoría válida de la empresa; no puede crear ciclos.</td></tr><tr><td>Estado/versión</td><td>Activo/inactivo y revisión actual.</td><td>Inactivar retira de nuevas selecciones sin borrar referencias.</td></tr><tr><td>Crear revisión</td><td>Nuevo nombre/escala/padre.</td><td>Conserva revisión anterior.</td></tr><tr><td>Reemplazo</td><td>Código, nombre, escala/padre del sucesor.</td><td>Crea/selecciona sucesor y enlaza la definición anterior; no reescribe ítems históricos.</td></tr>
    </tbody></table>
    <ol><li>Seleccione el tipo correcto y busque incluidos los inactivos.</li><li>Registre código y nombre; complete escala o padre solo cuando aplique.</li><li>Use revisión para cambios compatibles.</li><li>Use reemplazo cuando cambia la identidad semántica.</li><li>Inactive el anterior después de verificar consumidores.</li></ol>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">unit_definition</div><div>Empresa + código (PK); nombre, escala, estado, versión, reemplazo (FK propia), fechas.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">unit_definition_revision</div><div>Unidad + versión (PK/FK); nombre, escala, estado y fecha.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">category_definition</div><div>ID (PK); padre (FK propia), código (UK), nombre, estado, versión, reemplazo y fechas.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">category_definition_revision</div><div>Categoría + versión (PK/FK); padre, nombre, estado y fecha.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">brand_definition</div><div>ID (PK), código (UK), nombre, estado, versión, reemplazo y fechas.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">brand_definition_revision</div><div>Marca + versión (PK/FK); nombre, estado y fecha.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">tag_definition</div><div>ID (PK), código (UK), nombre, estado, versión, reemplazo y fechas.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">tag_definition_revision</div><div>Etiqueta + versión (PK/FK); nombre, estado y fecha.</div><div class="crud">C/R</div></div>
      <p class="relation">Cada definición tiene revisiones; las FK propias expresan jerarquía o reemplazo. No se encontraron triggers/funciones de usuario en estas tablas.</p>
    </div>
  </section>

  <section class="screen" data-screen="variant-families">
    <div class="screen-title"><h2>4. Familias de variantes</h2><span class="route">/faces/catalog/variant-families</span></div>
    <p><strong>Objetivo:</strong> definir estructuras versionadas de atributos para variantes de ítems.</p>
    <p><strong>Permiso:</strong> <code>commercial_catalog.definitions.manage</code>.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Familias de variantes ───────────────────────────────────────────┐
│ Buscar [código/nombre] Estado [▼] [Buscar]                         │
├ Familias ──────────────┬ Borrador de estructura ───────────────────┤
│ código · nombre · v.   │ código atributo · nombre · tipo · requerido│
│ [Seleccionar]          │ [Agregar/Quitar] [Registrar familia]      │
├ Revisión: nombre + atributos [Crear revisión]                       │
│ Historial [Ver] [Activar/Inactivar]                                 │
└────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos</h3>
    <table><thead><tr><th>Dato</th><th>Explicación</th><th>Regla</th></tr></thead><tbody><tr><td>Código/nombre de familia</td><td>Identidad estable y etiqueta de estructura.</td><td>Código único por empresa.</td></tr><tr><td>Código de atributo</td><td>Identificador estable dentro de familia.</td><td>Único en la familia.</td></tr><tr><td>Nombre de atributo</td><td>Etiqueta para captura, por ejemplo Color.</td><td>Obligatoria.</td></tr><tr><td>Tipo de valor</td><td>Clase cerrada que gobierna validación del valor.</td><td>No se interpreta como texto libre.</td></tr><tr><td>Requerido</td><td>Indica si toda variante debe informar el atributo.</td><td>Booleano.</td></tr><tr><td>Posición</td><td>Orden de presentación calculado por el borrador.</td><td>Se conserva en definición/revisión.</td></tr><tr><td>Versión</td><td>Revisión de estructura asignable a ítems.</td><td>Una revisión nueva no reescribe variantes ligadas a una versión anterior.</td></tr><tr><td>Agregar/Quitar</td><td>Edita el borrador antes de registrar/revisar.</td><td>Quitar del borrador no elimina historia publicada.</td></tr></tbody></table>
    <ol><li>Diseñe la estructura antes de crear ítems variantes.</li><li>Use códigos de atributo estables y nombres claros.</li><li>Marque requerido solo cuando el dato siempre exista.</li><li>Registre familia; para cambios posteriores cree una revisión.</li><li>Verifique la versión al asignarla a un ítem.</li></ol>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram"><div class="db-row"><div class="db-name">variant_family</div><div>Empresa + familia (PK); código (UK), nombre, estado, versión y fechas.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">variant_attribute_definition</div><div>Familia + atributo (PK/FK); nombre, tipo, requerido y posición actuales.</div><div class="crud">C/R/U/D*</div></div><div class="db-row"><div class="db-name">variant_family_revision</div><div>Familia + versión (PK/FK); nombre, estado y fecha.</div><div class="crud">C/R</div></div><div class="db-row"><div class="db-name">variant_attribute_revision</div><div>Familia + versión + atributo (PK/FK); nombre, tipo, requerido y posición.</div><div class="crud">C/R</div></div><p class="relation"><strong>*</strong> Quitar opera sobre el borrador/estructura actual según el caso de uso; las revisiones publicadas permanecen. No hay triggers/funciones de usuario.</p></div>
  </section>

  <section class="screen" data-screen="tax-profiles">
    <div class="screen-title"><h2>5. Perfiles tributarios</h2><span class="route">/faces/catalog/tax-profiles</span></div>
    <p><strong>Objetivo:</strong> mantener clasificaciones tributarias internas y sus intervalos de vigencia.</p>
    <p><strong>Permiso:</strong> <code>commercial_catalog.definitions.manage</code>.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Perfiles tributarios ────────────────────────────────────────────┐
│ Buscar [código/nombre] Estado [▼] [Buscar]                         │
├ Perfiles ──────────────┬ Revisión vigente e historial ─────────────┤
│ código · nombre · v.   │ clase interna · descripción · desde/hasta │
│ [Seleccionar]          │ [Crear revisión] [Activar/Inactivar]      │
├ Nuevo perfil: código · nombre · clase · descripción · vigencia     │
│                                                        [Registrar]│
└────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos y límites</h3>
    <table><thead><tr><th>Dato</th><th>Explicación</th><th>Regla</th></tr></thead><tbody><tr><td>Código/nombre</td><td>Identidad estable y etiqueta interna.</td><td>Código único por empresa.</td></tr><tr><td>Clase interna</td><td>Código cerrado que clasifica comportamiento tributario interno.</td><td>No es el XML ni un código fiscal arbitrario.</td></tr><tr><td>Descripción</td><td>Explicación funcional para operadores.</td><td>Debe ser clara y no prometer certificación.</td></tr><tr><td>Válido desde/hasta</td><td>Intervalo de la revisión; hasta opcional.</td><td>Hasta no precede a desde; revisiones activas del mismo perfil no se superponen.</td></tr><tr><td>Versión/historial</td><td>Número y lista de revisiones.</td><td>Nueva revisión conserva anteriores.</td></tr><tr><td>Activar/Inactivar</td><td>Cambia disponibilidad.</td><td>No reescribe ítems/documentos históricos.</td></tr></tbody></table>
    <p class="callout"><strong>Alcance fiscal:</strong> el perfil ayuda a modelar el catálogo comercial. No sustituye verificación del manual, XSD, catálogos y reglas oficiales SIFEN vigentes ni certifica un documento.</p>
    <ol><li>Confirme con el responsable funcional la clase interna y vigencia.</li><li>Busque perfiles existentes e históricos.</li><li>Registre o cree una revisión sin superponer periodos activos.</li><li>Asigne la revisión correcta al ítem y conserve la historia.</li></ol>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram"><div class="db-row"><div class="db-name">tax_profile</div><div>Empresa + perfil (PK); código (UK), nombre, estado, versión y fechas.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">tax_profile_revision</div><div>Perfil + versión (PK/FK); clase interna, descripción, desde/hasta, activo y fecha.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">enforce_tax_profile_revision_no_overlap()</div><div>Advisory lock por empresa/perfil y rechazo SQLSTATE <code>23505</code>, restricción <code>uq_tax_profile_revision_validity</code>.</div><div class="crud">TRIGGER</div></div><p class="relation"><strong>Trigger:</strong> <code>trg_tax_profile_revision_overlap</code>, BEFORE INSERT/UPDATE de revisiones activas. <code>catalog_item</code> referencia perfil + versión, por lo que la historia no se sustituye en silencio.</p></div>
  </section>

  <section><h2>Accesibilidad, recuperación y soporte</h2><ul><li>Fechas, montos y estados tienen labels y texto; no dependen solo del color.</li><li>En ancho compacto, las fichas se apilan y las tablas usan presentación alternativa.</li><li>Inactivar conserva historia. Antes de hacerlo revise artículos, precios y consumidores externos.</li><li>Para soporte entregue empresa, pantalla, código, versión, intervalo, acción, hora y correlación; no comparta secretos ni datos fiscales reales innecesarios.</li></ul></section>
</article>
