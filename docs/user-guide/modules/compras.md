<article>
  <div class="page-footer">Smart ERP · Manual de Compras · edición 2026-08-11 · página <span class="page-number"></span></div>
  <header class="cover">
    <div class="eyebrow">Manual de usuario por módulo · orden de lectura 07</div>
    <h1>Compras</h1>
    <p class="subtitle">Solicitudes, aprobación, órdenes, recepciones, devoluciones y seguimiento de cantidades.</p>
    <div class="meta"><strong>Versión documentada:</strong> plugin <code>purchasing</code> 1.1, historia J11-S9-06.<br><strong>Estado:</strong> compuesta y validada automáticamente con Maven, PostgreSQL, Docker/Compose, health, OIDC y Playwright; la validación independiente y el cierre J11-S9-07 siguen pendientes. No es una versión comercializable ni productiva.<br><strong>Acceso local de la candidata:</strong> <code>http://localhost:18080/logixone/</code>.<br><strong>Audiencia:</strong> solicitantes, aprobadores, compradores, responsables de recepción, supervisores y soporte.<br><strong>Fuente de datos:</strong> código, contratos, migraciones V1–V2 y pruebas PostgreSQL/Testcontainers del repositorio.</div>
  </header>

  <section class="toc">
    <h2>Qué cubre y qué no cubre</h2>
    <p>Compras controla la necesidad interna, el compromiso con el proveedor y las cantidades entregadas o devueltas. No registra factura del proveedor, deuda, pago, retención, asiento contable, costo ni valoración. Esas capacidades pertenecen a módulos posteriores.</p>
    <h3>Conceptos que debe conocer antes de operar</h3>
    <dl class="term-grid">
      <dt>Solicitud de compra</dt><dd>Documento interno que expresa una necesidad. Todavía no compromete a un proveedor.</dd>
      <dt>Línea</dt><dd>Producto o servicio, unidad y cantidad que forman parte de un documento.</dd>
      <dt>Producto de stock</dt><dd>Artículo físico cuya recepción o devolución afecta Inventario.</dd>
      <dt>No stock</dt><dd>Bien controlado por Compras que no genera un movimiento de existencias en este alcance.</dd>
      <dt>Servicio</dt><dd>Prestación no física que no requiere depósito, ubicación, lote ni serie.</dd>
      <dt>Orden de compra</dt><dd>Compromiso formal con un proveedor, en una moneda y por cantidades/precios definidos.</dd>
      <dt>Asignación</dt><dd>Vínculo cuantitativo entre una línea de orden y una línea de solicitud aprobada.</dd>
      <dt>Compra directa</dt><dd>Cantidad ordenada sin asignación completa a una solicitud aprobada; exige justificación.</dd>
      <dt>Recepción</dt><dd>Comprobante interno de lo entregado por el proveedor. Solo al confirmar afecta cumplimiento y, si es stock, Inventario.</dd>
      <dt>Devolución</dt><dd>Comprobante interno de una cantidad enviada de vuelta al proveedor desde una recepción confirmada.</dd>
      <dt>Pendiente</dt><dd>Cantidad ordenada que todavía no quedó recibida neta ni cerrada como faltante.</dd>
      <dt>Cierre con faltante</dt><dd>Decisión explícita de no esperar las cantidades aún pendientes, conservando un motivo.</dd>
      <dt>Snapshot</dt><dd>Copia histórica del nombre, código, unidad, moneda o proveedor conservada al crear el documento.</dd>
      <dt>Versión</dt><dd>Número de control de concurrencia. Si otra persona cambió el documento, debe recargar antes de operar.</dd>
      <dt>Confirmar</dt><dd>Acción irreversible para recepción o devolución: consolida cantidades y puede publicar un movimiento de Inventario.</dd>
      <dt>Idempotencia</dt><dd>Protección técnica que evita duplicar una misma operación cuando el navegador o la red la reintentan.</dd>
    </dl>
    <h3>Permisos</h3>
    <table><thead><tr><th>Permiso</th><th>Qué habilita</th></tr></thead><tbody>
      <tr><td><code>purchasing.view</code></td><td>Consultar las cinco pantallas y sus documentos.</td></tr>
      <tr><td><code>purchasing.requests.create</code></td><td>Crear, agregar líneas, clonar y cancelar solicitudes propias según estado.</td></tr>
      <tr><td><code>purchasing.requests.submit</code></td><td>Enviar una solicitud a aprobación.</td></tr>
      <tr><td><code>purchasing.requests.approve</code></td><td>Aprobar o rechazar; quien solicitó no puede aprobar su propia solicitud.</td></tr>
      <tr><td><code>purchasing.orders.create</code></td><td>Crear órdenes, agregar líneas y cancelar borradores según reglas.</td></tr>
      <tr><td><code>purchasing.orders.issue</code></td><td>Emitir la orden al proveedor.</td></tr>
      <tr><td><code>purchasing.orders.close</code></td><td>Cerrar cantidades pendientes con un motivo.</td></tr>
      <tr><td><code>purchasing.receipts.create</code></td><td>Preparar recepciones.</td></tr>
      <tr><td><code>purchasing.receipts.confirm</code></td><td>Confirmar recepciones y su ingreso de stock cuando corresponda.</td></tr>
      <tr><td><code>purchasing.returns.create</code></td><td>Preparar devoluciones desde recepciones confirmadas.</td></tr>
      <tr><td><code>purchasing.returns.confirm</code></td><td>Confirmar devoluciones y su salida de stock cuando corresponda.</td></tr>
      <tr><td><code>inventory.view</code></td><td>Buscar el depósito y sus ubicaciones al preparar una recepción de Stock.</td></tr>
      <tr><td><code>inventory.movements.purchase.post</code></td><td>Publicar la entrada o salida de Inventario al confirmar una recepción o devolución de Stock. Se exige además del permiso de confirmación de Compras.</td></tr>
      <tr><td><code>business_partners.manage</code>, <code>commercial_catalog.items.manage</code> e <code>inventory.storage.manage</code></td><td>Mostrar la ruta Administrar del selector de proveedor, artículo o almacenamiento. No son necesarios para elegir datos ya existentes.</td></tr>
      <tr><td><code>reference_data.view</code></td><td>Abrir la consulta del catálogo normativo de monedas desde su ruta propietaria.</td></tr>
    </tbody></table>
  </section>

  <section class="screen" data-screen="purchasing-requests">
    <div class="screen-title"><h2>1. Solicitudes de compra</h2><span class="route">/faces/purchasing/requests</span></div>
    <p><strong>Objetivo:</strong> registrar una necesidad, completar sus líneas y someterla a una decisión independiente.</p>
    <p><strong>Estados:</strong> Borrador permite editar; Pendiente espera decisión; Aprobada puede alimentar órdenes; Rechazada y Cancelada son finales.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Compras · Solicitudes ───────────────────────────────────────────────┐
│ Buscar [número/descripción] Estado [▼] [Buscar]                     │
├ Solicitudes ─────────────────┬ Solicitud seleccionada ──────────────┤
│ número · fecha · estado      │ líneas · estado · versión             │
│ [Abrir]                      │ [Líneas] [Aprobación] [Clonar]         │
├ Nueva solicitud ─────────────┴───────────────────────────────────────┤
│ número · fecha · tipo · artículo · descripción · unidad · cantidad  │
│ precio esperado · moneda                         [Preparar]          │
├ Línea adicional / decisión / copia ─────────────────────────────────┤
│ datos de línea [Agregar] · motivo [Enviar/Aprobar/Rechazar/Cancelar]│
│ número y fecha de copia [Clonar]                                    │
└──────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos y acciones</h3>
    <table><thead><tr><th>Dato o acción</th><th>Significado</th><th>Regla y resultado</th></tr></thead><tbody>
      <tr><td>Texto de búsqueda</td><td>Número o descripción de línea.</td><td>Opcional; solo busca dentro de la empresa activa.</td></tr>
      <tr><td>Estado de búsqueda</td><td>Todos, Borrador, Pendiente, Aprobada, Rechazada o Cancelada.</td><td>Es una lista cerrada; no se administran estados desde la pantalla.</td></tr>
      <tr><td>Número</td><td>Identificador empresarial del documento.</td><td>Obligatorio y único por empresa, hasta 64 caracteres.</td></tr>
      <tr><td>Fecha solicitada</td><td>Día en que se registra o requiere la necesidad.</td><td>Obligatoria, formato AAAA-MM-DD.</td></tr>
      <tr><td>Tipo de línea</td><td>Stock, No stock o Servicio.</td><td>Stock exige artículo de catálogo.</td></tr>
      <tr><td>Artículo o servicio</td><td>Concepto activo con alcance de compra.</td><td>Se busca en Catálogo comercial por contrato; Compras guarda identidad y snapshot, no lee su tabla.</td></tr>
      <tr><td>Descripción</td><td>Texto que explica lo solicitado.</td><td>Obligatorio; queda preservado aunque cambie el catálogo.</td></tr>
      <tr><td>Unidad</td><td>Unidad presentada al solicitante.</td><td>Obligatoria. La conversión a unidad base se conserva con la línea.</td></tr>
      <tr><td>Cantidad</td><td>Magnitud solicitada.</td><td>Positiva, hasta seis decimales.</td></tr>
      <tr><td>Precio esperado</td><td>Estimación informativa por unidad.</td><td>Opcional, no negativa; si se informa debe acompañarse de moneda.</td></tr>
      <tr><td>Moneda estimada</td><td>Código monetario habilitado.</td><td>Se busca en Datos de referencia; queda snapshot de código, nombre, decimales y publicación.</td></tr>
      <tr><td>Preparar solicitud</td><td>Crea el borrador con la primera línea.</td><td>No envía a aprobación automáticamente.</td></tr>
      <tr><td>Línea adicional</td><td>Segundo conjunto de tipo, artículo, descripción, unidad, cantidad y precio esperado.</td><td>Solo se agrega en Borrador; conserva las líneas existentes y aumenta la versión.</td></tr>
      <tr><td>Motivo</td><td>Explicación del rechazo o cancelación.</td><td>Obligatorio para esas acciones; no es necesario para enviar o aprobar.</td></tr>
      <tr><td>Enviar a aprobación</td><td>Pasa Borrador a Pendiente.</td><td>Luego ya no se editan líneas.</td></tr>
      <tr><td>Aprobar</td><td>Autoriza la necesidad para una orden.</td><td>Requiere aprobador distinto del solicitante.</td></tr>
      <tr><td>Rechazar / Cancelar</td><td>Finaliza el documento conservando la razón.</td><td>No borra cabecera ni líneas.</td></tr>
      <tr><td>Número y fecha de copia</td><td>Identidad del nuevo documento clonado.</td><td>Crea otro Borrador con nuevas identidades de línea; el original no cambia.</td></tr>
    </tbody></table>
    <h3>Secuencia recomendada</h3>
    <ol><li>Busque solicitudes similares para evitar duplicados.</li><li>Prepare el borrador con una primera línea completa.</li><li>Ábralo y agregue las demás líneas.</li><li>Revise descripción, unidad, cantidad y estimación.</li><li>Envíe a aprobación.</li><li>Un aprobador distinto decide. Si hay error de versión, recargue y vuelva a evaluar.</li></ol>
    <p class="warning"><strong>Recuperación:</strong> si falta catálogo o moneda, use la ruta Administrar cuando tenga permiso; al volver, refresque el selector. Si la solicitud ya no está en Borrador, no intente alterar sus líneas con otra URL.</p>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">purchase_request</div><div><code>company_id + purchase_request_id</code> (PK), número (UK), solicitante, fecha, estado, envío, decisión, motivo y versión.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">purchase_request_line</div><div>Solicitud + línea (PK/FK), posición, artículo y snapshots, unidades, factor, tipo, cantidad y estimación monetaria.</div><div class="crud">C/R/U*</div></div>
      <div class="db-row"><div class="db-name">purchasing_operation</div><div>Empresa + idempotencia (PK), operación, huella, recurso, versión y fecha.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">Catálogo / referencia</div><div>Artículo y moneda consultados por APIs públicas; sin FK ni lectura de tablas externas.</div><div class="crud">EXT</div></div>
      <div class="db-row"><div class="db-name">trg_purchase_request_line_immutable</div><div>Impide insertar, actualizar o borrar líneas cuando la solicitud dejó de estar en Borrador.</div><div class="crud">TRIGGER</div></div>
      <p class="relation"><strong>*</strong> La edición de líneas se materializa solo mientras la cabecera está en Borrador. Las tablas y el trigger se verificaron contra la migración V1 versionada, no contra una base local.</p>
    </div>
  </section>

  <section class="screen" data-screen="purchasing-orders">
    <div class="screen-title"><h2>2. Órdenes de compra</h2><span class="route">/faces/purchasing/orders</span></div>
    <p><strong>Objetivo:</strong> documentar el compromiso con un proveedor y relacionarlo, cuando corresponda, con una solicitud aprobada.</p>
    <p><strong>Estados:</strong> Borrador se prepara; Emitida admite recepciones; Cerrada no conserva pendiente; Cancelada conserva el motivo.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Compras · Órdenes ──────────────────────────────────────────────────┐
│ Buscar [número/proveedor] Estado [▼] [Buscar]                       │
├ Órdenes ────────────────────┬ Orden seleccionada ───────────────────┤
│ número · proveedor · moneda │ total · líneas · cumplimiento · estado│
│ [Abrir]                     │ [Líneas] [Estado]                     │
├ Nueva orden ────────────────┴───────────────────────────────────────┤
│ número · proveedor · moneda · justificación directa                │
│ tipo · artículo · descripción · unidad · cantidad · precio          │
│ solicitud · línea solicitada · cantidad asignada [Preparar]         │
├ Línea adicional / ciclo ────────────────────────────────────────────┤
│ datos de línea [Agregar] · motivo [Emitir/Cancelar/Cerrar faltante] │
└──────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos y acciones</h3>
    <table><thead><tr><th>Dato o acción</th><th>Significado</th><th>Regla y resultado</th></tr></thead><tbody>
      <tr><td>Búsqueda y estado</td><td>Localiza por número/proveedor y filtra Borrador, Emitida, Cerrada o Cancelada.</td><td>Consulta paginada dentro de la empresa.</td></tr>
      <tr><td>Número de orden</td><td>Identificador visible del compromiso.</td><td>Obligatorio y único por empresa.</td></tr>
      <tr><td>Proveedor</td><td>Socio comercial activo con rol Proveedor.</td><td>Se busca por contrato público; se conservan ID, código, nombre y versión en snapshot.</td></tr>
      <tr><td>Moneda</td><td>Moneda común para los precios de la orden.</td><td>Obligatoria; se conserva su publicación y cantidad de decimales.</td></tr>
      <tr><td>Justificación directa</td><td>Razón empresarial de una cantidad no cubierta por solicitud.</td><td>Obligatoria si la suma asignada es menor que la cantidad ordenada.</td></tr>
      <tr><td>Tipo / artículo / descripción / unidad</td><td>Identidad y presentación histórica de lo comprado.</td><td>Stock exige artículo; descripción y unidad quedan como snapshot.</td></tr>
      <tr><td>Cantidad</td><td>Cantidad comprometida con el proveedor.</td><td>Positiva; no puede ser menor que las asignaciones.</td></tr>
      <tr><td>Precio unitario</td><td>Importe por unidad en la moneda de la orden.</td><td>No negativo. El total usa los decimales de la moneda.</td></tr>
      <tr><td>Solicitud aprobada</td><td>Origen interno opcional de la necesidad.</td><td>Solo se ofrecen solicitudes Aprobadas.</td></tr>
      <tr><td>Línea solicitada</td><td>Línea concreta que se abastece.</td><td>Debe pertenecer a la solicitud elegida.</td></tr>
      <tr><td>Cantidad asignada</td><td>Porción de la línea de orden vinculada a la solicitud.</td><td>No puede exceder ni la línea ordenada ni la solicitada sumando otras órdenes.</td></tr>
      <tr><td>Preparar orden</td><td>Crea el Borrador y su primera línea.</td><td>Proveedor, moneda y snapshots ya quedan preservados.</td></tr>
      <tr><td>Línea adicional</td><td>Tipo, artículo, descripción, unidad, cantidad y precio adicionales.</td><td>Solo en Borrador. En esta primera pantalla la línea adicional es directa y requiere que la orden tenga justificación directa.</td></tr>
      <tr><td>Emitir</td><td>Formaliza la orden para comenzar a recibir.</td><td>Requiere permiso específico y versión vigente.</td></tr>
      <tr><td>Cancelar</td><td>Finaliza una orden permitida conservando motivo.</td><td>No se permite si existen recepciones confirmadas.</td></tr>
      <tr><td>Cerrar con faltante</td><td>Da por cerrada toda cantidad aún pendiente.</td><td>Solo en Emitida; cubre exactamente cada pendiente y exige motivo.</td></tr>
    </tbody></table>
    <ol><li>Seleccione proveedor y moneda correctos.</li><li>Si parte de una solicitud, asigne la línea y cantidad antes de crear.</li><li>Explique cualquier porción directa.</li><li>Revise total, snapshots y cantidades.</li><li>Emita solo cuando el compromiso esté listo.</li><li>Use cierre con faltante únicamente cuando no llegará lo pendiente.</li></ol>
    <p class="warning"><strong>Errores frecuentes:</strong> proveedor inactivo, solicitud no aprobada, asignación excesiva, compra directa sin justificación, versión antigua o intento de cancelar después de recibir.</p>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">purchase_order</div><div>Empresa + orden (PK), número (UK), proveedor/snapshot, moneda/snapshot, justificación, estado, emisión, motivo y versión.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">purchase_order_line</div><div>Orden + línea (PK/FK), posición, artículo/snapshots, unidad/factor, tipo, precio y cantidades ordenada, recibida, devuelta y cerrada.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">purchase_order_allocation</div><div>Línea de orden + línea de solicitud (PK/FK), posición y cantidad asignada.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">purchase_request + line</div><div>Solicitud y límites cuantitativos de la asignación.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">purchasing_operation</div><div>Ledger idempotente de alta, línea y transiciones.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">enforce_purchase_order_allocation_limits()</div><div>Bloquea y suma asignaciones; rechaza exceder la cantidad solicitada u ordenada.</div><div class="crud">TRIGGER</div></div>
      <div class="db-row"><div class="db-name">Socios / Catálogo / referencia</div><div>Proveedor, artículo y moneda se resuelven por contrato; no hay acceso cruzado.</div><div class="crud">EXT</div></div>
      <p class="relation">Una orden tiene muchas líneas; una línea puede tener varias asignaciones. Las relaciones internas incluyen empresa para impedir cruces entre compañías.</p>
    </div>
  </section>

  <section class="screen" data-screen="purchasing-receipts">
    <div class="screen-title"><h2>3. Recepciones</h2><span class="route">/faces/purchasing/receipts</span></div>
    <p><strong>Objetivo:</strong> preparar y confirmar una entrega contra una orden emitida. La pantalla registra una línea por comprobante en este primer corte.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Compras · Recepciones ──────────────────────────────────────────────┐
│ Buscar [número/orden] Estado [▼] [Buscar]                           │
├ Recepciones ────────────────┬ Recepción seleccionada ───────────────┤
│ número · orden · estado     │ línea · cantidad · trazabilidad       │
│ [Abrir]                     │ versión                 [Confirmar]    │
├ Nueva recepción ────────────┴───────────────────────────────────────┤
│ número · orden emitida · línea pendiente · cantidad                │
│ depósito · ubicación · lote · serie · vencimiento · condición      │
│                                                   [Preparar]        │
└──────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos y acciones</h3>
    <table><thead><tr><th>Dato o acción</th><th>Significado</th><th>Regla y resultado</th></tr></thead><tbody>
      <tr><td>Búsqueda / estado</td><td>Localiza recepción u orden y filtra Borrador o Confirmada.</td><td>Solo consulta datos de la empresa activa.</td></tr>
      <tr><td>Número</td><td>Identificador visible de la recepción.</td><td>Obligatorio y único por empresa.</td></tr>
      <tr><td>Orden emitida</td><td>Compromiso que el proveedor está cumpliendo.</td><td>Solo se ofrecen órdenes Emitidas.</td></tr>
      <tr><td>Línea ordenada</td><td>Artículo/servicio y cantidad pendiente.</td><td>Debe pertenecer a la orden seleccionada.</td></tr>
      <tr><td>Cantidad recibida</td><td>Cantidad entregada en este comprobante.</td><td>Positiva y no superior al pendiente.</td></tr>
      <tr><td>Depósito</td><td>Unidad logística que recibe stock.</td><td>Obligatorio solo para tipo Stock; requiere referencia activa de Inventario.</td></tr>
      <tr><td>Ubicación</td><td>Posición exacta dentro del depósito.</td><td>Obligatoria para Stock, activa y perteneciente al depósito.</td></tr>
      <tr><td>Lote</td><td>Grupo de producción o ingreso.</td><td>Completar según la política de seguimiento del artículo.</td></tr>
      <tr><td>Número de serie</td><td>Identidad individual del producto.</td><td>Completar según la política; una serie puede limitar cantidad.</td></tr>
      <tr><td>Fecha de vencimiento</td><td>Caducidad del lote o serie.</td><td>Formato AAAA-MM-DD cuando la política la requiera.</td></tr>
      <tr><td>Condición</td><td>Disponible, En cuarentena o Dañado.</td><td>Para Stock; valor inicial sugerido Disponible.</td></tr>
      <tr><td>Preparar</td><td>Crea una recepción en Borrador.</td><td>Todavía no altera cantidades de la orden ni stock.</td></tr>
      <tr><td>Confirmar</td><td>Consolida la recepción.</td><td>Actualiza recibido/pendiente y crea una entrada de Inventario para Stock; es irreversible.</td></tr>
    </tbody></table>
    <ol><li>Identifique la orden y compare físicamente la entrega.</li><li>Elija la línea exacta y escriba la cantidad recibida.</li><li>Para Stock, confirme depósito, ubicación, trazabilidad y condición.</li><li>Prepare el documento.</li><li>Abra y revise su detalle.</li><li>Confirme una sola vez; el reintento seguro usa idempotencia.</li></ol>
    <p class="warning"><strong>Si falla la confirmación:</strong> no cree otra recepción sin investigar. Recargue el documento y revise si ya quedó confirmado, si la orden cambió o si Inventario rechazó ubicación, seguimiento o permiso.</p>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">goods_receipt</div><div>Empresa + recepción (PK), número (UK), orden (FK), estado, confirmador, instante y versión.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">goods_receipt_line</div><div>Recepción + línea (PK/FK), orden/línea (FK), tipo, cantidad, depósito, ubicación, lote, serie, vencimiento, condición y movimiento.</div><div class="crud">C/R/U*</div></div>
      <div class="db-row"><div class="db-name">purchase_order + line</div><div>Orden validada; al confirmar aumenta <code>received_quantity</code> y puede cerrar la orden si no queda pendiente.</div><div class="crud">R/U*</div></div>
      <div class="db-row"><div class="db-name">purchasing_operation</div><div>Huella y resultado idempotente de preparación/confirmación.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">Inventario</div><div>Depósito/ubicación leídos y movimiento de entrada publicado por <code>InventoryPurchaseMovements</code>; sin FK ni SQL cruzado.</div><div class="crud">EXT</div></div>
      <div class="db-row"><div class="db-name">trg_goods_receipt_line_immutable</div><div>Impide cambiar líneas confirmadas.</div><div class="crud">TRIGGER</div></div>
      <div class="db-row"><div class="db-name">trg_goods_receipt_confirmation / immutable</div><div>Exige movimiento para cada línea Stock confirmada e impide modificar/borrar el documento confirmado.</div><div class="crud">TRIGGER</div></div>
      <p class="relation"><strong>*</strong> Las cantidades y el movimiento cambian únicamente al confirmar. Preparar conserva un borrador sin impacto de stock.</p>
    </div>
  </section>

  <section class="screen" data-screen="purchasing-returns">
    <div class="screen-title"><h2>4. Devoluciones a proveedores</h2><span class="route">/faces/purchasing/returns</span></div>
    <p><strong>Objetivo:</strong> devolver una cantidad comprobable desde una recepción confirmada y conservar su causa. La pantalla registra una línea por comprobante en este primer corte.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Compras · Devoluciones ─────────────────────────────────────────────┐
│ Buscar [número/orden/causa] Estado [▼] [Buscar]                     │
├ Devoluciones ───────────────┬ Devolución seleccionada ──────────────┤
│ número · orden · causa      │ recepción/línea · cantidad · estado   │
│ estado · líneas [Abrir]     │ versión                 [Confirmar]    │
├ Nueva devolución ───────────┴───────────────────────────────────────┤
│ número · orden · recepción confirmada · línea recibida             │
│ cantidad · causa                                  [Preparar]        │
└──────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos y acciones</h3>
    <table><thead><tr><th>Dato o acción</th><th>Significado</th><th>Regla y resultado</th></tr></thead><tbody>
      <tr><td>Búsqueda / estado</td><td>Localiza por número, orden o causa y filtra Borrador/Confirmada.</td><td>Consulta paginada dentro de la empresa.</td></tr>
      <tr><td>Número</td><td>Identificador visible de la devolución.</td><td>Obligatorio y único por empresa.</td></tr>
      <tr><td>Orden</td><td>Orden Emitida o Cerrada asociada.</td><td>Debe ser la misma que originó la recepción.</td></tr>
      <tr><td>Recepción confirmada</td><td>Prueba de que la cantidad fue recibida.</td><td>No se puede devolver desde un borrador.</td></tr>
      <tr><td>Línea recibida</td><td>Trazabilidad exacta de orden, artículo y dimensiones de stock.</td><td>Debe pertenecer a la recepción elegida.</td></tr>
      <tr><td>Cantidad devuelta</td><td>Parte que vuelve al proveedor.</td><td>Positiva y no superior a lo recibido menos devoluciones previas.</td></tr>
      <tr><td>Causa</td><td>Razón operativa de la devolución.</td><td>Obligatoria, hasta 240 caracteres; queda preservada.</td></tr>
      <tr><td>Preparar</td><td>Crea el Borrador.</td><td>No cambia orden ni stock.</td></tr>
      <tr><td>Confirmar</td><td>Consolida la salida al proveedor.</td><td>Aumenta devuelto, reabre pendiente si corresponde y publica salida de Inventario para Stock.</td></tr>
    </tbody></table>
    <ol><li>Seleccione la orden y una recepción ya Confirmada.</li><li>Elija la línea exacta.</li><li>Verifique cantidad disponible para devolver y escriba una causa concreta.</li><li>Prepare el documento.</li><li>Revise antes de confirmar.</li><li>Confirme cuando la salida física esté autorizada.</li></ol>
    <p class="warning"><strong>No improvise trazabilidad:</strong> depósito, ubicación, lote, serie, vencimiento y condición provienen de la recepción. Si son incorrectos, escale el caso; no modifique una recepción confirmada.</p>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">supplier_return</div><div>Empresa + devolución (PK), número (UK), orden (FK), causa, estado, confirmador, instante y versión.</div><div class="crud">C/R/U</div></div>
      <div class="db-row"><div class="db-name">supplier_return_line</div><div>Devolución + línea (PK/FK), orden/línea, recepción/línea (FK), tipo, cantidad, trazabilidad y movimiento.</div><div class="crud">C/R/U*</div></div>
      <div class="db-row"><div class="db-name">goods_receipt + line</div><div>Origen confirmado y límite cuantitativo/trazabilidad de lo devuelto.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">purchase_order + line</div><div>Al confirmar aumenta <code>returned_quantity</code> y recalcula el pendiente.</div><div class="crud">R/U*</div></div>
      <div class="db-row"><div class="db-name">purchasing_operation</div><div>Protección idempotente de preparación/confirmación.</div><div class="crud">C/R</div></div>
      <div class="db-row"><div class="db-name">Inventario</div><div>Salida de stock publicada por contrato con las dimensiones heredadas; sin tabla cruzada.</div><div class="crud">EXT</div></div>
      <div class="db-row"><div class="db-name">trg_supplier_return_line_immutable</div><div>Impide cambiar líneas confirmadas.</div><div class="crud">TRIGGER</div></div>
      <div class="db-row"><div class="db-name">trg_supplier_return_confirmation / immutable</div><div>Exige movimiento para Stock e impide modificar/borrar una devolución confirmada.</div><div class="crud">TRIGGER</div></div>
      <p class="relation"><strong>*</strong> Orden e Inventario cambian solo al confirmar; el borrador conserva preparación sin impacto.</p>
    </div>
  </section>

  <section class="screen" data-screen="purchasing-tracking">
    <div class="screen-title"><h2>5. Seguimiento de compras</h2><span class="route">/faces/purchasing/tracking</span></div>
    <p><strong>Objetivo:</strong> consultar, sin modificar, el cumplimiento neto de cada orden.</p>
    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Compras · Seguimiento ──────────────────────────────────────────────┐
│ Buscar [número/proveedor] Estado [▼] [Buscar]                       │
├ Órdenes ────────────────────┬ Cumplimiento seleccionado ────────────┤
│ número · proveedor · estado │ proveedor · moneda · total · versión  │
│ líneas [Ver seguimiento]    │ por línea: pedida · recibida          │
│                             │ devuelta · pendiente                  │
└──────────────────────────────────────────────────────────────────────┘</div>
    <h3>Datos</h3>
    <table><thead><tr><th>Dato</th><th>Cómo interpretarlo</th><th>Origen</th></tr></thead><tbody>
      <tr><td>Número / proveedor</td><td>Identifica el compromiso y el tercero histórico.</td><td>Cabecera de orden y snapshot de proveedor.</td></tr>
      <tr><td>Estado</td><td>Borrador, Emitida, Cerrada o Cancelada.</td><td>Ciclo de la orden.</td></tr>
      <tr><td>Moneda / total</td><td>Valor contractual calculado por cantidad y precio.</td><td>Snapshot monetario y líneas de orden.</td></tr>
      <tr><td>Pedida</td><td>Cantidad comprometida originalmente.</td><td><code>ordered_quantity</code>.</td></tr>
      <tr><td>Recibida</td><td>Total de recepciones confirmadas acumuladas.</td><td><code>received_quantity</code>.</td></tr>
      <tr><td>Devuelta</td><td>Total confirmado que regresó al proveedor.</td><td><code>returned_quantity</code>.</td></tr>
      <tr><td>Pendiente</td><td>Lo que aún se espera después de recepciones netas y cierres.</td><td>Cálculo del dominio con cantidades recibidas, devueltas y cerradas.</td></tr>
      <tr><td>Versión</td><td>Corte del documento mostrado.</td><td><code>entity_version</code>; solo lectura.</td></tr>
    </tbody></table>
    <ol><li>Filtre por número, proveedor o estado.</li><li>Abra la orden.</li><li>Compare pedida, recibida, devuelta y pendiente por línea.</li><li>Si una cifra no coincide con la operación real, abra Recepciones o Devoluciones; no intente corregir el resumen.</li></ol>
    <p class="warning"><strong>Límite:</strong> esta pantalla resume el estado vigente. No reemplaza el historial de auditoría ni constituye una factura, cuenta por pagar o conciliación contable.</p>
    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">purchase_order</div><div>Número, proveedor/snapshot, moneda/snapshot, estado y versión.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">purchase_order_line</div><div>Descripción, cantidad ordenada, recibida, devuelta, cerrada y pendiente calculada.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">purchase_order_allocation</div><div>Origen solicitado de cada cantidad, disponible para trazabilidad técnica.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">goods_receipt / line</div><div>Documentos que explican cantidades recibidas.</div><div class="crud">R</div></div>
      <div class="db-row"><div class="db-name">supplier_return / line</div><div>Documentos que explican cantidades devueltas.</div><div class="crud">R</div></div>
      <p class="relation">Seguimiento es estrictamente de lectura: no crea, actualiza ni elimina filas y no consulta tablas privadas de otros plugins.</p>
    </div>
  </section>

  <section>
    <h2>Accesibilidad, seguridad y soporte</h2>
    <ul>
      <li>Los estados se muestran con texto y no dependen solo del color.</li>
      <li>En ancho compacto los formularios se apilan; no debe existir desplazamiento horizontal normal.</li>
      <li>Los selectores grandes buscan en servidor. Use teclado y etiquetas visibles; no escriba UUID manuales.</li>
      <li>La ruta Administrar aparece solo con permiso del módulo propietario. Al volver, refresque opciones y confirme el borrador preservado.</li>
      <li>Ante un error, anote empresa, pantalla, número de documento, acción, hora y mensaje. No envíe contraseñas, tokens ni datos personales innecesarios.</li>
      <li>Un plugin desactivado o un permiso ausente oculta menús y bloquea operaciones también en el servidor.</li>
    </ul>
    <p><strong>Canal de soporte:</strong> utilice el canal definido por el implementador y adjunte el identificador de correlación cuando la pantalla lo muestre. Mientras falten el gate de cierre y la validación independiente, cualquier uso debe limitarse a revisión interna controlada.</p>
  </section>
</article>
