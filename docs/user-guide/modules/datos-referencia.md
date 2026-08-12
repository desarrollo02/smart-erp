<article>
  <div class="page-footer">LogixOne · Manual de Datos de referencia · edición 2026-08-11 · página <span class="page-number"></span></div>
  <header class="cover">
    <div class="eyebrow">Manual de usuario por módulo</div>
    <h1>Datos de referencia</h1>
    <p class="subtitle">Cómo consultar países y monedas oficiales y decidir cuáles puede usar cada empresa.</p>
    <div class="meta"><strong>Versión documentada:</strong> plugin <code>reference_data</code> 1.1.0, baseline Sprint 8.<br><strong>Audiencia:</strong> operadores de consulta y administradores de políticas empresariales.<br><strong>Fuente técnica:</strong> pantalla, contrato público, migraciones V1–V4 y metadatos PostgreSQL verificados en modo de solo lectura.</div>
  </header>

  <section class="toc">
    <h2>Cómo usar este manual</h2>
    <ol><li>Lea primero los términos para entender la diferencia entre un dato oficial y la decisión de habilitarlo.</li><li>Abra la única pantalla del módulo y siga el recorrido recomendado.</li><li>Consulte el diagrama para saber qué queda registrado.</li></ol>
    <div class="callout"><strong>Idea clave:</strong> habilitar o deshabilitar no modifica el catálogo oficial. Solo cambia la política de la empresa activa y conserva historial.</div>
    <h3>Permisos</h3>
    <table><thead><tr><th>Permiso</th><th>Qué permite</th></tr></thead><tbody><tr><td><code>reference_data.view</code></td><td>Abrir la pantalla, buscar y consultar detalles e historial.</td></tr><tr><td><code>reference_data.policy.manage</code></td><td>Habilitar o deshabilitar códigos para la empresa activa.</td></tr></tbody></table>
    <h3>Glosario</h3>
    <dl class="term-grid"><dt>Catálogo</dt><dd>Conjunto gobernado de códigos. En este módulo: países y monedas.</dd><dt>Entrada</dt><dd>Un país o una moneda individual dentro de una publicación.</dd><dt>Publicación (release)</dt><dd>Versión importada del catálogo, identificada por autoridad, fecha, origen y huella SHA-256.</dd><dt>Política empresarial</dt><dd>Decisión de la empresa de permitir o no un código oficial en sus selectores.</dd><dt>Habilitado</dt><dd>El código puede ser ofrecido a otros módulos de la empresa.</dd><dt>Deshabilitado</dt><dd>El código permanece registrado, pero deja de estar disponible para nuevas operaciones.</dd><dt>Historial</dt><dd>Secuencia inmutable de cambios de política, con versión, actor, correlación y fecha.</dd><dt>Empresa activa</dt><dd>Contexto empresarial seleccionado en la sesión. Toda política se guarda dentro de ese contexto.</dd></dl>
  </section>

  <section class="screen" data-screen="reference-data">
    <div class="screen-title"><h2>1. Datos de referencia</h2><span class="route">/faces/reference-data</span></div>
    <p><strong>Objetivo:</strong> localizar países o monedas oficiales, revisar la publicación de origen y administrar su disponibilidad para la empresa activa.</p>
    <p><strong>Prerrequisitos:</strong> sesión iniciada, empresa activa, plugin habilitado y permiso de consulta. Para cambiar una política se requiere además <code>reference_data.policy.manage</code>.</p>

    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Datos de referencia ───────────────────────────────────────────────┐
│ Catálogo [ Países ▼ ]   Buscar [ código o nombre          ] [Buscar] │
├ Resultados ──────────────────────────────────────────────────────────┤
│ Código │ Nombre │ Código alternativo │ Política │ Acción              │
│ PY     │ Paraguay │ PRY / 600         │ Habilitado │ [Ver] [Deshabilitar]│
├ Detalle e historial de la entrada seleccionada ──────────────────────┤
│ Publicación · autoridad · fecha · origen · versiones de la política  │
└──────────────────────────────────────────────────────────────────────┘</div>

    <h3>Términos, datos y controles</h3>
    <table><thead><tr><th>Dato o control</th><th>Significado y formato</th><th>Regla u origen</th></tr></thead><tbody>
      <tr><td>Catálogo</td><td>Selector cerrado: <em>Países</em> o <em>Monedas</em>.</td><td>Obligatorio para buscar; no admite altas libres.</td></tr>
      <tr><td>Buscar</td><td>Texto parcial por código o nombre visible.</td><td>Filtro opcional; no cambia datos.</td></tr>
      <tr><td>Código país</td><td>ISO alfabético de 2 caracteres; también se muestran alfa-3 y numérico.</td><td>Proviene de la publicación de países.</td></tr>
      <tr><td>Código moneda</td><td>Código alfabético de 3 caracteres; puede incluir código numérico.</td><td>Proviene de la publicación de monedas.</td></tr>
      <tr><td>Nombre visible</td><td>Denominación legible del país o moneda.</td><td>Dato oficial importado; no editable en esta pantalla.</td></tr>
      <tr><td>Decimales menores</td><td>Número de dígitos usuales de la unidad fraccionaria de una moneda; puede estar vacío si la fuente no lo informa.</td><td>Solo aparece para monedas.</td></tr>
      <tr><td>Estado de política</td><td><em>Habilitado</em> o <em>Deshabilitado</em> para la empresa.</td><td>Si no existe una decisión empresarial, la interfaz explica el estado efectivo definido por el servicio.</td></tr>
      <tr><td>Publicación actual</td><td>Identificador, estándar, autoridad, URL de origen, SHA-256, fecha observada, completitud y cantidad de entradas.</td><td>Trazabilidad de importación; solo lectura.</td></tr>
      <tr><td>Historial</td><td>Versión, estado, usuario actor, identificador de correlación y fecha.</td><td>Se agrega una versión por cambio; no se borra.</td></tr>
      <tr><td>Habilitar / Deshabilitar</td><td>Acciones que cambian la política empresarial de la entrada seleccionada.</td><td>Exigen permiso de gestión y validan nuevamente empresa y versión.</td></tr>
    </tbody></table>

    <h3>Recorrido recomendado</h3>
    <ol><li>Seleccione <strong>Países</strong> o <strong>Monedas</strong>.</li><li>Escriba un código o nombre; deje el texto vacío para explorar la lista permitida por la pantalla.</li><li>Pulse <strong>Buscar</strong> y seleccione una fila.</li><li>Compruebe la publicación y el historial antes de cambiar la política.</li><li>Pulse <strong>Habilitar</strong> o <strong>Deshabilitar</strong> y confirme el nuevo estado.</li></ol>
    <p class="success"><strong>Resultado esperado:</strong> la fila refleja el estado nuevo y el historial muestra una versión adicional con el actor y la fecha.</p>
    <p class="warning"><strong>Errores frecuentes:</strong> sin empresa activa, plugin deshabilitado, permiso insuficiente, versión ya modificada por otra sesión o código no presente en la publicación actual. Actualice la pantalla antes de reintentar. Si persiste, entregue a soporte empresa, catálogo, código, hora y correlación; nunca credenciales.</p>

    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">catalog_release</div><div><code>catalog_kind</code> + <code>release_id</code> (PK), estándar, autoridad, origen, SHA-256, fecha, completitud, cantidad, indicador de actual.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">country_entry</div><div>Publicación + <code>alpha2</code> (PK/FK), <code>alpha3</code>, numérico y nombre visible.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">currency_entry</div><div>Publicación + código alfabético (PK/FK), código numérico, decimales menores y nombre visible.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">company_country_policy</div><div><code>company_id</code> + <code>alpha2</code> (PK), habilitado, versión y fecha de actualización.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">company_currency_policy</div><div><code>company_id</code> + código alfabético (PK), habilitado, versión y fecha.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">company_reference_policy_history</div><div>Empresa + catálogo + código + versión (PK), estado, actor, correlación y fecha.</div><div class="crud">C/R</div></div>
      <p class="relation"><strong>Relaciones:</strong> cada entrada pertenece a una publicación; cada política pertenece a una empresa y código; cada cambio agrega una fila de historial. La empresa y el usuario se reciben por contratos del kernel (<strong>EXT</strong>), sin FK cruzada. No se encontraron vistas, funciones ni triggers propios en este esquema; las reglas se aplican en el servicio y por PK/FK/UK.</p>
    </div>
  </section>

  <section><h2>Accesibilidad, límites y soporte</h2><ul><li>Use etiquetas visibles y teclado; el estado se expresa con texto y no solo con color.</li><li>La pantalla no crea países ni monedas ni certifica por sí sola cumplimiento normativo.</li><li>Deshabilitar afecta nuevas selecciones; no debe reescribir documentos históricos.</li><li>Canal de soporte: mesa interna del proyecto. Informe ruta, empresa, código, resultado esperado, resultado observado y hora.</li></ul></section>
</article>
