<article>
  <div class="page-footer">LogixOne · Manual de Inventario · edición 2026-08-14 · página <span class="page-number"></span></div>
  <header class="cover">
    <div class="eyebrow">Manual de usuario por módulo</div>
    <h1>Inventario</h1>
    <p class="subtitle">Almacenes, ubicaciones, existencias, movimientos, reservas y conteos físicos con trazabilidad.</p>
    <div class="meta"><strong>Versión documentada:</strong> plugin <code>inventory</code> 1.2, baseline candidato de Sprint 10.<br><strong>Audiencia:</strong> responsables de almacén, operadores de stock, supervisores y soporte.<br><strong>Dependencia funcional:</strong> Catálogo comercial, consultado por contrato; Inventario conserva snapshots y no accede a sus tablas privadas.</div>
  </header>

  <section class="toc">
    <h2>Conceptos y permisos</h2>
    <table><thead><tr><th>Permiso</th><th>Capacidad</th></tr></thead><tbody>
      <tr><td><code>inventory.view</code></td><td>Consultar almacenes, stock y conteos.</td></tr><tr><td><code>inventory.storage.manage</code></td><td>Crear/editar almacenes y ubicaciones.</td></tr><tr><td><code>inventory.items.manage</code></td><td>Incorporar artículos del catálogo y mantener políticas.</td></tr><tr><td><code>inventory.movements.post</code></td><td>Contabilizar movimientos.</td></tr><tr><td><code>inventory.reservations.manage</code></td><td>Crear, consumir, liberar o expirar reservas.</td></tr><tr><td><code>inventory.counts.manage</code></td><td>Crear y capturar conteos.</td></tr><tr><td><code>inventory.adjustments.post</code></td><td>Contabilizar el ajuste resultante de un conteo.</td></tr>
    </tbody></table>
    <h3>Glosario</h3>
    <dl class="term-grid"><dt>Almacén</dt><dd>Unidad logística que agrupa ubicaciones.</dd><dt>Ubicación</dt><dd>Lugar preciso dentro de un almacén.</dd><dt>Artículo inventariable</dt><dd>Proyección local de un artículo de catálogo, con código/nombre/unidad en snapshot.</dd><dt>Tracking</dt><dd>Política de seguimiento: ninguno, lote, serie u otra opción cerrada.</dd><dt>Caducidad</dt><dd>Política que indica si se requiere fecha de vencimiento.</dd><dt>Condición</dt><dd>Estado físico utilizable para separar saldo, por ejemplo disponible o dañado.</dd><dt>Saldo físico</dt><dd>Cantidad realmente contabilizada en una dimensión de stock.</dd><dt>Saldo reservado</dt><dd>Parte comprometida por reservas activas.</dd><dt>Disponible</dt><dd>Físico menos reservado, según reglas del dominio.</dd><dt>Movimiento</dt><dd>Documento inmutable que aumenta o disminuye stock al contabilizarse.</dd><dt>Idempotencia</dt><dd>Clave única que impide contabilizar dos veces la misma solicitud.</dd><dt>Reserva</dt><dd>Compromiso temporal de cantidad para una fuente externa.</dd><dt>Conteo</dt><dd>Proceso de comparar cantidad teórica con cantidad capturada.</dd><dt>Snapshot</dt><dd>Copia histórica de código, nombre, unidad y versión del catálogo al operar.</dd></dl>
  </section>

  <section class="screen" data-screen="inventory-stock">
    <div class="screen-title"><h2>1. Existencias y operaciones</h2><span class="route">/faces/inventory</span></div>
    <p><strong>Objetivo:</strong> incorporar artículos del catálogo, consultar disponibilidad, contabilizar movimientos y administrar reservas.</p>
    <p><strong>Prerrequisitos:</strong> empresa activa, plugin Inventario y Catálogo efectivos, almacén/ubicación activos y permisos específicos de la acción.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Inventario · Existencias · Operación guiada ──────────────┐
│ Tarea [Registrar movimiento ▼]  Artículo [producto activo ▼] │
│ [Continuar]                                                │
├ Datos de la operación ───────────────────────────┤
│ Tipo [Entrada ▼] Depósito [▼] Ubicación [▼] Condición [▼] │
│ Destino: sólo transferencia · Lote/serie: según artículo │
│ Vencimiento: según política · Cantidad [ ] · Motivo [ ]   │
├ Guía y resumen ────────────────────────────┤
│ Efecto previsto, unidad base y reglas del artículo seleccionado │
│ [Registrar movimiento] — confirmación antes de contabilizar    │
└────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos y controles</h3>
    <table><thead><tr><th>Grupo</th><th>Datos explicados</th><th>Reglas</th></tr></thead><tbody>
      <tr><td>Contexto</td><td>Tarea y artículo activo. <strong>Continuar</strong> adapta la captura sin cambiar de ruta.</td><td>El selector muestra referencias legibles; la identidad interna no se transcribe.</td></tr>
      <tr><td>Incorporación</td><td>Artículo de catálogo, modo de tracking y política de caducidad.</td><td>El artículo debe existir y ser elegible. Se guarda UUID externo y snapshot; un mismo artículo de catálogo solo se incorpora una vez por empresa.</td></tr>
      <tr><td>Disponibilidad</td><td>Almacén, ubicación, condición, lote, serie, fecha de expiración; cantidades física, reservada y disponible; unidad base y versión.</td><td>Las dimensiones aplicables dependen del tracking/caducidad. Vacío no significa siempre “desconocido”: puede significar no aplicable.</td></tr>
      <tr><td>Movimiento</td><td>Tipo, ubicación origen y destino cuando corresponde, condición, lote/serie/vencimiento aplicable, cantidad y motivo.</td><td>El sistema genera fuente e idempotencia. Entrada aumenta; salida disminuye; transferencia debita y acredita atómicamente posiciones distintas.</td></tr>
      <tr><td>Reserva nueva</td><td>Artículo, ubicación, condición, trazabilidad aplicable, cantidad y fecha de expiración.</td><td>Fuente, identidad, versión e idempotencia se mantienen como datos técnicos no editables.</td></tr>
      <tr><td>Operar reserva</td><td>Cantidad y acciones Consumir, Liberar o Expirar cuando existe una reserva vigente en contexto.</td><td>La identidad, versión y clave de operación viajan internamente y el servidor las revalida.</td></tr>
      <tr><td>Ciclo de artículo</td><td>Refrescar snapshot e Inactivar.</td><td>Refrescar toma datos actuales por contrato; inactivar conserva movimientos, saldos y reservas históricas.</td></tr>
    </tbody></table>
    <h3>Registrar una entrada, salida o transferencia</h3>
    <ol><li>Configure previamente depósitos y ubicaciones y confirme que el artículo esté activo.</li><li>En <strong>Tarea</strong>, elija <strong>Registrar movimiento</strong>; seleccione el artículo y pulse <strong>Continuar</strong>.</li><li>Elija Entrada, Salida o Transferencia. Para transferencia, complete un destino distinto del origen.</li><li>Complete solamente lote, serie o vencimiento que la pantalla muestre según la política del artículo.</li><li>Ingrese cantidad positiva y motivo; revise la guía y el resumen.</li><li>Pulse <strong>Registrar movimiento</strong> y confirme. Espere el mensaje de éxito antes de iniciar otra operación.</li></ol>
    <p class="warning"><strong>Errores frecuentes:</strong> ubicación de otro depósito, destino igual al origen, lote/serie requerido, vencimiento inválido, stock insuficiente o artículo/ubicación inactivos. Corrija los datos conservados en pantalla y reintente; no edite saldos por SQL ni busque una clave técnica.</p>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">inventory_item</div><div>Empresa + ID (PK); <code>catalog_item_id</code> (UK), snapshots de código/nombre/unidad/versión, tracking, caducidad, estado y versión.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">inventory_balance</div><div>ID (PK); artículo/almacén/ubicación (FK), lote, serie, expiración, condición, unidad, físico, reservado y versión; UK por dimensiones.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">stock_movement</div><div>Movimiento (PK); tipo, razón, fuente, idempotencia (UK), fecha de contabilización y reversión opcional (FK propia).</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">stock_movement_line</div><div>Movimiento + línea (PK/FK); artículo/ubicación, dimensiones, dirección, snapshots, unidad/cantidad presentada, factor y cantidad base.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">stock_reservation</div><div>Reserva (PK); artículo/ubicación, dimensiones, cantidades original/consumida/liberada, fuente, idempotencia, estado, expiración y versión.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">reservation_operation</div><div>Idempotencia de operación (PK); reserva (FK), tipo, cantidad, saldos resultantes, estado/versión y fecha.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">warehouse / stock_location</div><div>Almacén y ubicación válidos para las dimensiones.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">Catálogo comercial</div><div>Artículo, código, nombre, unidad y versión resueltos por contrato público; no existe FK ni lectura cruzada.</div><div class="crud">EXT</div></div>
      <p class="relation">El movimiento crea cabecera/líneas y actualiza balances en una transacción. Las reservas actualizan <code>reserved_quantity</code> y registran cada operación idempotente. No se encontraron triggers de usuario sobre estas tablas; PK/FK/UK y servicios protegen consistencia.</p>
    </div>
  </section>

  <section class="screen" data-screen="inventory-warehouses">
    <div class="screen-title"><h2>2. Almacenes y ubicaciones</h2><span class="route">/faces/inventory/warehouses</span></div>
    <p><strong>Objetivo:</strong> definir la estructura física en la que se mantienen saldos y se ejecutan conteos.</p>
    <p><strong>Permiso:</strong> consulta con <code>inventory.view</code>; cambios con <code>inventory.storage.manage</code>.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Almacenes y ubicaciones ─────────────────────────────────────────┐
│ Buscar [código/nombre] Estado [▼] [Buscar]                         │
├ Almacenes ───────────────┬ Almacén seleccionado ──────────────────┤
│ código · nombre · estado │ nombre nuevo [Renombrar] [Inactivar]    │
│ [Seleccionar]            │ Ubicación: código · nombre · tipo [Añadir]│
│                          │ ubicaciones [Renombrar] [Inactivar]      │
└───────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos</h3>
    <table><thead><tr><th>Dato</th><th>Significado</th><th>Regla</th></tr></thead><tbody>
      <tr><td>Código de almacén</td><td>Identificador estable único por empresa.</td><td>Obligatorio; no se cambia al renombrar.</td></tr><tr><td>Nombre de almacén</td><td>Etiqueta operativa.</td><td>Obligatoria; Renombrar incrementa versión.</td></tr><tr><td>Estado de almacén</td><td>Activo/inactivo.</td><td>Inactivar conserva ubicaciones e historia y bloquea nuevas operaciones según dominio.</td></tr><tr><td>Código de ubicación</td><td>Identificador estable único dentro del almacén.</td><td>Obligatorio.</td></tr><tr><td>Nombre de ubicación</td><td>Etiqueta física legible.</td><td>Obligatoria.</td></tr><tr><td>Tipo de ubicación</td><td>Clase cerrada usada por reglas logísticas.</td><td>Seleccione según uso real; no es texto libre.</td></tr><tr><td>Versión</td><td>Concurrencia de almacén/ubicación.</td><td>Recargue ante conflicto.</td></tr>
    </tbody></table>
    <ol><li>Busque para evitar códigos duplicados.</li><li>Registre almacén con código estable y nombre reconocible.</li><li>Seleccione el almacén y agregue ubicaciones.</li><li>Use nombres que coincidan con la señalización física.</li><li>Antes de inactivar, revise balances, reservas y conteos abiertos.</li></ol>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram"><div class="db-row"><div class="db-name">warehouse</div><div>Empresa + <code>warehouse_id</code> (PK); código (UK por empresa), nombre, activo y versión.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">stock_location</div><div>Empresa + <code>location_id</code> (PK); almacén (FK), código (UK dentro del almacén), nombre, tipo, activo y versión.</div><div class="crud">C/R/U</div></div><div class="db-row"><div class="db-name">balances/reservas/conteos</div><div>Referencias existentes que deben considerarse antes de inactivar.</div><div class="crud">R</div></div><p class="relation">Un almacén tiene muchas ubicaciones; las FK incluyen empresa. No hay borrado físico ni triggers/funciones de usuario en estas dos tablas.</p></div>
  </section>

  <section class="screen" data-screen="inventory-counts">
    <div class="screen-title"><h2>3. Conteos físicos</h2><span class="route">/faces/inventory/counts</span></div>
    <p><strong>Objetivo:</strong> preparar, capturar, revisar y contabilizar diferencias entre stock teórico y físico.</p>
    <p><strong>Permisos:</strong> <code>inventory.counts.manage</code> para alta/captura/ciclo; <code>inventory.adjustments.post</code> para contabilizar el ajuste.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Conteos físicos ─────────────────────────────────────────────────┐
│ Estado [▼] [Buscar]  Resultados: almacén · ubicación · estado      │
├ Nuevo conteo: almacén [▼] ubicación opcional [▼] [Crear borrador] │
├ Conteo seleccionado ───────────────────────────────────────────────┤
│ Línea: artículo · ubicación · condición · lote/serie/expiración    │
│ [Agregar línea]  Línea [ID] Cantidad contada [ ] [Capturar]        │
│ [Iniciar] [Enviar a revisión] [Contabilizar] [Cancelar]            │
└───────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos y estados</h3>
    <table><thead><tr><th>Dato/acción</th><th>Explicación</th><th>Condición</th></tr></thead><tbody>
      <tr><td>Almacén / ubicación</td><td>Alcance del conteo; ubicación puede ser opcional para conteo global del almacén.</td><td>Deben estar activos y pertenecer a la empresa.</td></tr>
      <tr><td>Estado</td><td>Borrador, En conteo, En revisión, Contabilizado o Cancelado.</td><td>Las transiciones son cerradas; no se salta libremente entre estados.</td></tr>
      <tr><td>Línea</td><td>Artículo, ubicación, condición, lote, serie y expiración.</td><td>Debe respetar alcance y políticas de tracking/caducidad.</td></tr>
      <tr><td>Cantidad teórica</td><td>Snapshot de saldo esperado para la línea.</td><td>Solo lectura para el operador.</td></tr>
      <tr><td>Cantidad contada</td><td>Valor físico capturado.</td><td>Decimal no negativo; se registra en la línea seleccionada.</td></tr>
      <tr><td>Iniciar</td><td>Pasa borrador a En conteo.</td><td>Activa el bloqueo lógico de alcance.</td></tr>
      <tr><td>Enviar a revisión</td><td>Cierra captura ordinaria y prepara validación.</td><td>Requiere líneas completas según reglas.</td></tr>
      <tr><td>Contabilizar</td><td>Genera ajuste por diferencias y finaliza conteo.</td><td>Exige permiso de ajustes; es una operación sensible e idempotente a nivel de servicio.</td></tr>
      <tr><td>Cancelar</td><td>Finaliza sin contabilizar ajustes.</td><td>Conserva cabecera y líneas para trazabilidad.</td></tr>
    </tbody></table>
    <ol><li>Cree borrador con el menor alcance práctico.</li><li>Agregue líneas y verifique dimensiones.</li><li>Inicie el conteo.</li><li>Capture cantidades físicas sin consultar/alterar manualmente saldos.</li><li>Envíe a revisión y explique diferencias según el procedimiento interno.</li><li>Un usuario autorizado contabiliza; confirme balances posteriores.</li></ol>
    <p class="warning"><strong>Bloqueo de alcance:</strong> no puede haber dos conteos activos superpuestos en el mismo almacén (global con cualquiera, o dos de la misma ubicación). Termine o cancele el anterior; no intente evadirlo creando otro alcance equivalente.</p>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">stock_count</div><div>Empresa + conteo (PK); almacén/ubicación (FK), estado y versión.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">stock_count_line</div><div>Conteo + línea (PK/FK); artículo/ubicación (FK), condición, lote/serie/expiración, cantidad teórica y contada.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">inventory_balance</div><div>Saldos teóricos leídos y actualizados por ajustes al contabilizar.</div><div class="crud">R/U*</div></div>
      <div class="db-row"><div class="db-name">stock_movement + line</div><div>Documento/líneas de ajuste que pueden generarse al contabilizar diferencias.</div><div class="crud">C*</div></div>
      <div class="db-row"><div class="db-name">warehouse/location/item</div><div>Alcance y dimensiones válidas.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">enforce_stock_count_scope_lock()</div><div>Bloquea el almacén y rechaza otro conteo COUNTING/REVIEW superpuesto; error SQLSTATE <code>23P01</code>.</div><div class="crud">TRIGGER</div></div>
      <p class="relation"><strong>Trigger:</strong> <code>trg_stock_count_scope_lock</code>, BEFORE INSERT o UPDATE de almacén/ubicación/estado. <strong>*</strong> balance y movimiento solo cambian al contabilizar, no al capturar.</p>
    </div>
  </section>

  <section><h2>Accesibilidad, seguridad y soporte</h2><ul><li>Cantidades y estados siempre se expresan como texto, no solo color.</li><li>En compacto, formularios se apilan; en medio y expandido la guía y el resumen permanecen junto a la captura.</li><li>Puede recorrer los controles con Tab; después de actualizar contexto o ejecutar una acción, el foco vuelve al primer campo inválido, al mensaje visible o al título de la tarea.</li><li>Si el sistema operativo solicita movimiento reducido, el shell elimina las transiciones no esenciales.</li><li>Identidad, versión, fuente e idempotencia son datos internos: no se solicitan ni se copian entre operaciones.</li><li>Para soporte entregue empresa, artículo, depósito/ubicación, tipo de operación, hora y mensaje visible; no incluya secretos.</li></ul></section>
</article>
