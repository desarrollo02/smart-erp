<article>
  <div class="page-footer">LogixOne · Manual de Socios comerciales · edición 2026-08-11 · página <span class="page-number"></span></div>
  <header class="cover">
    <div class="eyebrow">Manual de usuario por módulo</div>
    <h1>Socios comerciales</h1>
    <p class="subtitle">Alta y mantenimiento de personas u organizaciones con las que la empresa compra, vende o se relaciona.</p>
    <div class="meta"><strong>Versión documentada:</strong> plugin <code>business_partners</code> 1.0, baseline Sprint 8.<br><strong>Audiencia:</strong> operadores comerciales, responsables de datos maestros y soporte.<br><strong>Pantallas:</strong> Socios comerciales y Definiciones de socios.</div>
  </header>

  <section class="toc">
    <h2>Conceptos y preparación</h2>
    <p>Un socio es un maestro empresarial. Su código e historia identifican al mismo tercero aunque cambien el nombre, los canales o sus roles.</p>
    <h3>Permisos</h3>
    <table><thead><tr><th>Permiso</th><th>Alcance</th></tr></thead><tbody><tr><td><code>business_partners.view</code></td><td>Buscar y consultar.</td></tr><tr><td><code>business_partners.manage</code></td><td>Crear y editar identidad, identificaciones, direcciones, canales y contactos.</td></tr><tr><td><code>business_partners.roles.manage</code></td><td>Asignar, activar o desactivar roles cliente/proveedor.</td></tr><tr><td><code>business_partners.lifecycle.manage</code></td><td>Desactivar o reactivar el socio.</td></tr></tbody></table>
    <h3>Glosario</h3>
    <dl class="term-grid"><dt>Socio comercial</dt><dd>Persona u organización identificada dentro de una empresa.</dd><dt>Tipo</dt><dd>Clasificación principal, por ejemplo persona u organización.</dd><dt>Nombre visible</dt><dd>Nombre corto mostrado en búsquedas y selectores.</dd><dt>Razón social</dt><dd>Nombre legal del socio.</dd><dt>Nombre comercial</dt><dd>Denominación usada públicamente.</dd><dt>Identificación</dt><dd>Documento o código presentado por una autoridad, normalizado para búsqueda y unicidad.</dd><dt>Canal</dt><dd>Medio de comunicación, como teléfono o correo.</dd><dt>Contacto</dt><dd>Persona de enlace dentro del socio.</dd><dt>Rol</dt><dd>Capacidad operativa del socio, como cliente o proveedor.</dd><dt>Estado</dt><dd>Ciclo de vida. Inactivo conserva historia y referencias; no equivale a borrar.</dd><dt>Definición</dt><dd>Catálogo empresarial administrable usado por los campos del socio.</dd><dt>Revisión</dt><dd>Versión histórica de una definición.</dd></dl>
  </section>

  <section class="screen" data-screen="business-partners">
    <div class="screen-title"><h2>1. Socios comerciales</h2><span class="route">/faces/business-partners</span></div>
    <p><strong>Objetivo:</strong> buscar, registrar y mantener el expediente empresarial de un socio y sus roles.</p>
    <p><strong>Prerrequisitos:</strong> empresa activa, plugin habilitado y permiso de vista; las acciones se habilitan según los permisos anteriores.</p>

    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Socios comerciales ──────────────────────────────────────────────┐
│ Buscar [ código o nombre ] Rol [Todos▼] Estado [Activos▼] [Buscar]│
├ Resultados ──────────────┬ Ficha del socio seleccionado ──────────┤
│ Código · Nombre · Estado │ Identidad | Identificaciones | Dirección│
│ [Seleccionar]            │ Canales | Contactos | Roles | Ciclo vida│
├ Nuevo socio ─────────────┴─────────────────────────────────────────┤
│ Código · tipo · nombres legal/visible/comercial       [Registrar] │
└───────────────────────────────────────────────────────────────────┘</div>

    <h3>Términos, datos y controles</h3>
    <table><thead><tr><th>Grupo</th><th>Datos explicados</th><th>Reglas principales</th></tr></thead><tbody>
      <tr><td>Búsqueda</td><td><strong>Texto:</strong> código o nombre parcial. <strong>Rol:</strong> todos, cliente o proveedor. <strong>Estado:</strong> activo/inactivo. <strong>Resultados:</strong> código, tipo, nombre visible, roles, estado y acción Seleccionar.</td><td>Los filtros no modifican datos. La consulta queda limitada a la empresa activa.</td></tr>
      <tr><td>Alta</td><td><strong>Código:</strong> identificador empresarial único. <strong>Tipo:</strong> persona/organización. <strong>Nombre visible:</strong> etiqueta operativa. <strong>Razón social</strong> y <strong>nombre comercial:</strong> denominaciones legal y pública.</td><td>Código, tipo y nombre visible son obligatorios. No reutilice códigos de otro socio de la misma empresa.</td></tr>
      <tr><td>Edición</td><td>Código de referencia, nombre visible, razón social y nombre comercial; acción <strong>Cambiar nombres</strong>.</td><td>La versión impide sobrescribir cambios simultáneos. Cambiar nombres no cambia la identidad técnica.</td></tr>
      <tr><td>Identificación</td><td><strong>Tipo:</strong> clase documental. <strong>País:</strong> código oficial habilitado. <strong>Valor:</strong> presentación del documento; el sistema conserva valor presentado, normalizado, dígito verificador y vencimiento cuando corresponde.</td><td>País llega desde Datos de referencia por contrato público. Evite espacios o separadores innecesarios; la normalización se aplica en servidor.</td></tr>
      <tr><td>Dirección</td><td>Tipo, propósito, línea principal, línea adicional, número de casa, código postal, país, área, localidad; además estado activo e indicador principal.</td><td>País es catálogo oficial; tipo/propósito/área/localidad son referencias gobernadas. Una dirección inactiva se conserva.</td></tr>
      <tr><td>Canal</td><td>Tipo (teléfono, correo u otro), propósito y valor; estado activo e indicador principal.</td><td>Valide formato y propósito antes de agregar. No registre secretos.</td></tr>
      <tr><td>Contacto</td><td>Nombre y cargo; puede tener canales propios con tipo, propósito, valor, estado e indicador principal.</td><td>El contacto pertenece al socio seleccionado; su canal pertenece al contacto.</td></tr>
      <tr><td>Roles</td><td>Rol cliente/proveedor, código de rol, estado y acciones asignar, activar o desactivar.</td><td>Un socio puede tener ambos roles. Desactivar un rol no desactiva todo el socio.</td></tr>
      <tr><td>Ciclo de vida</td><td>Acciones <strong>Desactivar socio</strong> y <strong>Reactivar socio</strong>.</td><td>No hay borrado físico. Revise operaciones dependientes antes de inactivar.</td></tr>
    </tbody></table>

    <h3>Secuencia recomendada</h3>
    <ol><li>Busque primero por código, nombre e identificación para evitar duplicados.</li><li>Registre identidad básica con un código estable.</li><li>Seleccione el socio creado y agregue identificaciones.</li><li>Agregue dirección, canales y contactos.</li><li>Asigne los roles operativos necesarios.</li><li>Revise la ficha completa antes de usar el socio en otro módulo.</li></ol>
    <p class="success"><strong>Resultado esperado:</strong> el socio queda visible con su versión, datos relacionados y roles dentro de la empresa activa.</p>
    <p class="warning"><strong>Errores frecuentes:</strong> código duplicado, identificación repetida o inválida, país no habilitado, definición inactiva, cambio concurrente o permiso insuficiente. Actualice y vuelva a seleccionar la ficha; si persiste, informe código, empresa, acción y correlación a soporte.</p>

    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">business_partner</div><div>Empresa + <code>partner_id</code> (PK); código (UK por empresa), tipo, nombres, estado, versión y fechas.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">business_partner_identification</div><div>Empresa + socio + identificación (PK/FK); tipo, país, valor presentado/normalizado, dígito y vigencia.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">business_partner_address</div><div>Empresa + socio + dirección (PK/FK); tipo, propósito, líneas, número, postal, país, área, localidad, activo/principal.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">business_partner_channel</div><div>Empresa + socio + canal (PK/FK); tipo, propósito, valor, activo/principal.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">business_partner_contact</div><div>Empresa + socio + contacto (PK/FK); nombre, cargo, activo y fechas.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">business_partner_contact_channel</div><div>Empresa + socio + contacto + canal (PK/FK); tipo, propósito, valor, activo/principal.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">business_partner_role</div><div>Empresa + socio + tipo de rol (PK/FK); estado, código de rol y fechas.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">business_partner_code_sequence</div><div>Empresa + alcance (PK); siguiente valor y fecha. Soporta numeración gobernada cuando el caso de uso la solicita.</div><div class="crud">R/U</div></div>
      <div class="db-row"><div class="db-name">Datos de referencia</div><div>Países habilitados y otras referencias resueltas mediante contrato; no hay acceso directo a tablas privadas.</div><div class="crud">EXT</div></div>
      <p class="relation"><strong>Relaciones:</strong> el socio es padre de identificaciones, direcciones, canales, contactos y roles; un contacto es padre de sus canales. Las PK/FK incluyen <code>company_id</code> para impedir cruces de empresa. No se encontraron vistas, funciones ni triggers de usuario en el esquema; la concurrencia se protege con versión y restricciones PK/FK/UK.</p>
    </div>
  </section>

  <section class="screen" data-screen="business-partner-definitions">
    <div class="screen-title"><h2>2. Definiciones de socios</h2><span class="route">/faces/business-partners/definitions</span></div>
    <p><strong>Objetivo:</strong> administrar catálogos empresariales usados por los datos del socio, con revisiones e inactivación sin perder historia.</p>
    <p><strong>Permiso:</strong> la consulta requiere <code>business_partners.view</code>; registrar, revisar o cambiar estado requiere <code>business_partners.manage</code>.</p>

    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Definiciones de socios ──────────────────────────────────────────┐
│ Tipo [Tipo de dirección▼] Buscar [código/nombre] Estado [▼] [Buscar]│
├ Resultados ────────────────┬ Detalle e historial ─────────────────┤
│ Código · Nombre · Estado   │ Versión · nombre · estado · fecha     │
├ Nueva definición ──────────┴───────────────────────────────────────┤
│ Tipo · código · nombre [Registrar]   Nuevo nombre [Crear revisión] │
│                                      [Activar] [Inactivar]          │
└───────────────────────────────────────────────────────────────────┘</div>

    <h3>Datos y acciones</h3>
    <table><thead><tr><th>Dato</th><th>Explicación</th><th>Condición o efecto</th></tr></thead><tbody>
      <tr><td>Tipo de definición</td><td>Familia del catálogo: determina qué selector consume sus valores.</td><td>Obligatorio y cerrado por el dominio; no crea nuevos tipos de catálogo.</td></tr>
      <tr><td>Texto y estado</td><td>Filtros por código/nombre y activo/inactivo.</td><td>Solo consulta.</td></tr>
      <tr><td>Código</td><td>Clave empresarial estable dentro del tipo.</td><td>Obligatoria y única por empresa + tipo; no se cambia al revisar.</td></tr>
      <tr><td>Nombre</td><td>Etiqueta que verá el usuario en selectores.</td><td>Obligatoria.</td></tr>
      <tr><td>Versión</td><td>Número de revisión del catálogo.</td><td>Aumenta al crear una revisión; ayuda a conservar historia.</td></tr>
      <tr><td>Registrar</td><td>Crea definición y primera revisión.</td><td>La nueva definición inicia en el estado definido por el caso de uso.</td></tr>
      <tr><td>Crear revisión</td><td>Publica un nombre nuevo sin reescribir revisiones previas.</td><td>Valida versión actual para concurrencia.</td></tr>
      <tr><td>Activar/Inactivar</td><td>Cambia disponibilidad para nuevas selecciones.</td><td>No borra ni invalida referencias históricas.</td></tr>
    </tbody></table>

    <h3>Secuencia recomendada y errores</h3>
    <ol><li>Seleccione el tipo correcto.</li><li>Busque por código y también entre inactivos para evitar duplicados.</li><li>Registre un código estable y un nombre comprensible.</li><li>Para renombrar, seleccione la definición y cree una revisión.</li><li>Inactive únicamente cuando ya no deba ofrecerse en nuevas operaciones.</li></ol>
    <p class="warning"><strong>Atención:</strong> no cree sinónimos para reemplazar un valor existente. Use revisión o inactivación. Un código duplicado, una versión antigua o un permiso insuficiente produce rechazo sin cambio parcial.</p>

    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">business_partner_definition</div><div>Empresa + <code>definition_kind</code> + código (PK); nombre, estado, versión y fechas.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">business_partner_definition_revision</div><div>Empresa + tipo + código + versión (PK/FK); nombre, estado y fecha de cambio.</div><div class="crud">C/R</div></div>
      <p class="relation"><strong>Relación:</strong> una definición tiene una o más revisiones. El padre conserva estado y versión actuales; la tabla de revisiones conserva cada edición. No hay borrado desde la pantalla ni triggers/funciones de usuario en el esquema.</p>
    </div>
  </section>

  <section><h2>Accesibilidad, límites y soporte</h2><ul><li>Los labels deben permanecer visibles y el foco indicar claramente la acción activa.</li><li>En ancho compacto, la ficha se apila debajo de resultados; no debe requerir desplazamiento horizontal normal.</li><li>La pantalla no emite documentos fiscales ni borra historia.</li><li>Para soporte, entregue ruta, empresa, código del socio/definición, acción, hora y mensaje exacto; no incluya identificaciones personales completas si no son imprescindibles.</li></ul></section>
</article>
