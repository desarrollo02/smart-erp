# Épica - Terminal de punto de venta

- Estado: Planificada; implementación no autorizada hasta completar sus predecesores
- Plugin: `point_of_sale`
- Orden vigente del roadmap: 12 de 19 reutilizables
- Audiencia: supermercados, minimercados, tiendas y pequeños comercios
- Decisiones: [ADR-0027](../adr/0027-terminal-punto-venta-y-ampliacion-roadmap.md) y [ADR-0035](../adr/0035-operacion-offline-terminal-punto-venta.md)

## Objetivo

Proveer una estación de venta rápida, segura y auditable para uno o varios cajeros,
capaz de continuar las ventas permitidas cuando no hay Internet y sincronizarlas
al recuperar conectividad, sin duplicar los dominios de catálogo, inventario,
ventas, documentos o tesorería.

## Capacidades previstas

- registrar terminal, establecimiento y configuración operativa por empresa;
- abrir, operar y cerrar una sesión de cajero coordinada con tesorería;
- buscar o escanear artículos, modificar cantidades y visualizar totales;
- seleccionar cliente o usar el participante genérico permitido por la empresa;
- aplicar descuentos, anulaciones o cambios de precio sólo con autorización;
- suspender y recuperar ventas sin perder trazabilidad;
- cobrar con efectivo, tarjeta u otros medios y permitir pagos divididos;
- calcular y mostrar vuelto sin sustituir las reglas de tesorería;
- confirmar la venta de forma idempotente, afectar inventario y emitir el documento
  canónico correspondiente;
- imprimir o reimprimir comprobantes con auditoría;
- procesar devoluciones y anulaciones referenciando la operación original;
- mostrar estados comprensibles de red, terminal, caja, documento y periféricos.
- continuar al menos ventas en efectivo permitidas con la terminal aprovisionada y
  sin conexión al servidor central;
- conservar localmente un diario cifrado y append-only que sobreviva reinicios;
- sincronizar venta, cobro, documento y stock mediante claves idempotentes, sin
  duplicar efectos ante reintentos;
- mostrar pendientes, antigüedad del paquete, conflictos y recuperación, sin borrar
  una venta ya aceptada ante el cliente.

## Límites de propiedad

| Información o regla | Plugin propietario |
|---|---|
| artículos, unidades, perfiles tributarios y precios | `commercial_catalog` |
| disponibilidad y movimientos de stock | `inventory` |
| venta, devolución y compromiso comercial | `sales` |
| factura, ticket, nota y snapshots | `commercial_documents` |
| caja, medios de pago, cobros, vuelto y conciliación | `treasury` |
| proyección y transmisión fiscal paraguaya | `sifen` |
| terminal, sesión operativa, carrito y coordinación del checkout | `point_of_sale` |

El POS usa identificadores, contratos y eventos públicos. No importa entidades,
repositorios, controladores o DTO internos ni consulta esquemas privados ajenos.

## Dependencias previstas

- requeridas: `commercial_catalog`, `inventory`, `sales`,
  `commercial_documents` y `treasury`;
- fiscal por país: no requiere directamente `sifen`; consume el estado público del
  documento cuando el adaptador fiscal esté activo;
- personalización: `<empresa>_customization` puede modificar los puntos de extensión
  publicados y se compone siempre después del POS.

Los rangos de versión definitivos se aprueban durante la caracterización.

## Operación offline obligatoria

La primera versión productiva no se considera completa si sólo vende online. Antes
del dominio POS debe ejecutarse `POS-OFF-00`, que elegirá mediante ADR técnico la
topología local compatible con Jakarta Faces server-side, la identidad del
terminal, autenticación del cajero, cifrado, proyecciones, numeración, políticas y
recuperación. Se evaluará primero un runtime local o nodo de establecimiento; una
caché de navegador o `localStorage` no constituye una solución offline válida.

El POS podrá almacenar sólo proyecciones versionadas y acotadas publicadas por los
plugins propietarios. Artículos, precios, impuestos, disponibilidad, clientes y
permisos tendrán versión y antigüedad máxima. Cada operación usará identidad local
estable, diario durable y el ciclo
`LOCAL_ACCEPTED -> PENDING_SYNC -> SYNCED`, con `CONFLICT` y
`RECOVERY_REQUIRED` visibles y recuperables.

El alcance mínimo offline cubre efectivo. Un medio que necesita autorización
externa no puede mostrarse aprobado sin respuesta. Crédito, pago electrónico y
contingencia fiscal requieren políticas explícitas, y la integración paraguaya se
verificará contra la especificación SIFEN oficial vigente antes de implementar o
certificar.

### Historias offline planificadas

| Historia | Resultado esperado |
|---|---|
| `POS-OFF-00` | caracterización, análisis de amenazas, topología local y ADR técnico |
| `POS-OFF-01` | aprovisionamiento, identidad, paquete firmado y proyecciones versionadas |
| `POS-OFF-02` | almacenamiento cifrado, diario append-only y recuperación tras reinicio |
| `POS-OFF-03` | checkout local permitido, numeración y políticas de pago/stock |
| `POS-OFF-04` | sincronización idempotente, conciliación y compensaciones |
| `POS-OFF-05` | UI accesible de red, antigüedad, pendientes, conflictos y recuperación |
| `POS-OFF-06` | matriz de fallos, seguridad, rendimiento y demo offline real |

## Experiencia mínima de terminal

- Jakarta Faces 4.1 y Material Design 3;
- operación completa por teclado y táctil;
- lector de código de barras que funcione como teclado en el primer corte;
- acciones principales grandes, consistentes y resistentes a doble activación;
- responsive en 375, 720 y 1280 px, más el tamaño de terminal físico aprobado;
- mensajes que distingan rechazo, espera, resultado incierto y operación confirmada;
- foco visible, labels, contraste, atajos documentados y soporte de reducción de
  movimiento.

Cada selector del terminal —caja, cajero, cliente, lista de precios, depósito,
terminal y medio de pago— deberá declarar su fuente y enlazar al administrador
propietario conforme a
[ADR-0028](../adr/0028-gobierno-de-selectores-y-datos-administrables.md). Estados de
checkout y códigos fiscales no admitirán altas arbitrarias.

## Seguridad y trazabilidad

La caracterización definirá permisos separados para operar, administrar terminales,
abrir/cerrar sesión, sobrescribir precios, autorizar descuentos, anular, devolver y
reimprimir. Cada operación revalidará actor, empresa, plugin, terminal y permiso en
el servidor y emitirá auditoría sin almacenar datos sensibles de tarjetas.

## Fuera de alcance o condicionado

- drivers nativos o servicios de periféricos no aprobados;
- almacenamiento de PAN, CVV, PIN o credenciales de medios de pago;
- cálculo fiscal propio del país;
- lectura directa de tablas de inventario, ventas, documentos o tesorería;
- promociones complejas hasta definir su propietario y contrato;
- contabilidad dentro del checkout.

## Criterios de aceptación de la épica

- **POS-01:** dos terminales pueden operar concurrentemente sin duplicar venta,
  cobro, documento ni movimiento de stock.
- **POS-02:** un reintento después de timeout devuelve el mismo resultado o un
  estado reconciliable, nunca una segunda venta silenciosa.
- **POS-03:** precio, stock, documento y dinero permanecen en sus plugins
  propietarios.
- **POS-04:** descuentos, cambios de precio, anulaciones, devoluciones y reimpresión
  tienen permisos y auditoría específicos.
- **POS-05:** el terminal admite teclado, táctil y lector de código de barras tipo
  teclado.
- **POS-06:** los tres rangos responsive y el tamaño de caja elegido no presentan
  overflow horizontal normal.
- **POS-07:** SIFEN puede estar presente o ausente sin cambiar el dominio del POS.
- **POS-08:** desactivar el plugin oculta menús/tareas y conserva sesiones cerradas,
  auditoría y datos históricos.
- **POS-09:** la demo visual incluye venta, pago dividido, vuelto, documento,
  impresión simulada, devolución y seguridad negativa.
- **POS-10:** manual de usuario, manual técnico, guía de implementación, gráfico de
  dependencias, PDF e instalador se actualizan en el Sprint correspondiente.
- **POS-11:** una terminal previamente aprovisionada confirma al menos una venta en
  efectivo sin Internet y la operación sobrevive reinicio de navegador y runtime.
- **POS-12:** cada venta offline usa una identidad estable y la reconexión no
  duplica venta, cobro, documento ni movimiento, incluso con mensajes repetidos o
  desordenados.
- **POS-13:** la UI distingue online, offline, paquete vencido, pendiente,
  sincronizado, conflicto y recuperación requerida; nunca presenta como centralmente
  sincronizada una operación que no lo está.
- **POS-14:** datos y diario locales están cifrados, aislados por empresa/terminal y
  no contienen secretos, PAN, CVV, PIN ni credenciales reutilizables.
- **POS-15:** precios, permisos, stock indicativo, numeración y medios de pago tienen
  políticas de vigencia y límites; un proveedor externo no se simula aprobado.
- **POS-16:** la demo corta conectividad antes/durante/después del checkout,
  reinicia el runtime, reconecta, sincroniza y resuelve un conflicto sin pérdida ni
  duplicación.

## Inicio autorizado

Registrar esta épica modifica el roadmap, pero no autoriza comenzar código ahora.
Primero debe cerrarse Sprint 8 y construirse, en orden, `purchasing`, `sales`,
`logistics`, `vehicle_telemetry`, `commercial_documents`, `recurring_billing`,
`sifen` y `treasury`. El terminal consumirá solamente los contratos públicos que
necesite; no depende funcionalmente de telemetría ni facturación recurrente,
aunque ambos lo preceden en el orden de construcción aprobado.

ADR-0035 autoriza planificar desde ahora `POS-OFF-00` a `POS-OFF-06`, pero no
adelanta su implementación. Estas historias se ejecutan dentro del Sprint futuro
de `point_of_sale`, después de estabilizar los contratos de sus predecesores.
