# ADR-0035 - Operación offline obligatoria en la terminal de punto de venta

- Estado: Aceptado
- Fecha: 2026-08-04
- Decisión de producto: `point_of_sale` debe poder continuar vendiendo cuando la
  terminal pierde conectividad con Internet o con el servidor central
- Sustituye parcialmente: sección 6 y alcance offline de
  [ADR-0027](0027-terminal-punto-venta-y-ampliacion-roadmap.md)
- Alcance actual: requisito y plan arquitectónico; no autoriza iniciar código de
  POS antes de completar sus predecesores

## Contexto

ADR-0027 agregó `point_of_sale` como canal especializado, pero decidió que su
primera versión sería exclusivamente online. Producto establece ahora que una caja
minorista no puede detener todas las ventas por una caída de Internet. El terminal
debe poder aceptar al menos las operaciones offline permitidas, conservarlas de
forma durable y sincronizarlas al recuperar conectividad.

El baseline visual usa Jakarta Faces server-side. Una página almacenada en la
caché del navegador no puede ejecutar por sí sola autorización, precios,
numeración, persistencia, documentos ni checkout cuando el servidor deja de estar
disponible. Declarar "offline" mediante HTML cacheado, `localStorage` o una cola
JavaScript sin un límite transaccional local produciría ventas frágiles y una
segunda fuente de verdad sin gobierno.

## Decisión

### 1. Offline es una capacidad obligatoria del POS

Una terminal previamente aprovisionada debe poder continuar y confirmar, como
mínimo, una venta en efectivo permitida cuando no logra alcanzar el servidor
central o Internet. La operación no puede quedar sólo en memoria ni depender de
que la pestaña permanezca abierta. Debe sobrevivir reinicio del navegador y del
proceso local, conservar una identidad única y mostrar claramente que espera
sincronización.

"Offline" presupone que la terminal conserva energía y acceso a su almacenamiento
local aprobado. La operación ante pérdida total del equipo, disco o energía se
trata como recuperación ante fallo, no como conectividad offline.

### 2. Se agrega una historia de arquitectura antes del dominio POS

Antes de implementar `point_of_sale` se ejecutará `POS-OFF-00`, una
caracterización y análisis de amenazas que deberá aprobar mediante otro ADR
técnico:

- la topología local: nodo de establecimiento, servicio local por terminal o una
  combinación controlada;
- instalación, actualización, respaldo y recuperación del runtime local;
- identidad del dispositivo, autenticación del cajero y duración de la autoridad
  offline;
- almacenamiento cifrado, protección de claves y borrado seguro;
- proyecciones necesarias de catálogo, precios, impuestos, clientes, stock,
  numeración y políticas;
- reglas por medio de pago, crédito, descuento, documento y país;
- sincronización, conflictos, compensaciones y soporte operativo.

La topología de referencia a evaluar primero será un runtime local o nodo de
establecimiento que mantenga la interfaz Jakarta Faces disponible en la red local y
sincronice con el servidor central. Es compatible con el baseline server-side y
evita trasladar lógica ERP sensible al navegador. No queda autorizada una SPA/PWA,
una base completa del ERP por caja ni un cambio de Jakarta Faces sin el ADR técnico
correspondiente.

### 3. Datos locales acotados y cifrados

El POS no replicará tablas privadas completas de otros plugins. El propietario de
cada dominio publicará contratos o proyecciones versionadas mínimas. El paquete
offline podrá contener solamente lo aprobado para la empresa, establecimiento y
terminal, por ejemplo:

- identidad de empresa, establecimiento, terminal y sesión autorizada;
- artículos vendibles, códigos de barras, unidades y descripciones necesarias;
- snapshots versionados de precios, impuestos y promociones simples admitidas;
- referencias de cliente estrictamente necesarias;
- disponibilidad o límites operativos indicativos, con fecha de actualización;
- rangos o namespaces de numeración reservados sin colisión;
- política firmada de operaciones, importes, antigüedad y medios de pago admitidos;
- permisos offline y vencimiento de la autorización local.

El almacenamiento será cifrado, con claves protegidas por el sistema operativo o
el mecanismo aprobado para el runtime. No contendrá PAN, CVV, PIN, tokens OIDC,
contraseñas reutilizables ni credenciales de proveedores.

### 4. Diario durable, idempotencia y sincronización

Cada operación offline tendrá un identificador generado localmente y estable,
único dentro de `(empresa, terminal)`. El diario local será append-only para los
hechos confirmados y conservará snapshots suficientes para reproducir lo que vio y
aceptó el cajero.

El ciclo mínimo será explícito, equivalente a:

`DRAFT -> LOCAL_ACCEPTED -> PENDING_SYNC -> SYNCED`

con estados visibles `CONFLICT` y `RECOVERY_REQUIRED`. Reintentar, reiniciar o
recibir dos veces el mismo mensaje no puede duplicar venta, cobro, documento ni
movimiento. La sincronización usará outbox/inbox, claves idempotentes y entrega
`at-least-once`; "exactamente una vez" se obtiene como efecto de negocio, no como
promesa del transporte.

Una venta ya aceptada ante el cliente no se borra silenciosamente porque el estado
central haya cambiado. Un conflicto produce una conciliación o compensación
auditada, conserva la operación original y guía al operador hasta una resolución.

### 5. Límites comerciales, pagos, stock y fiscalidad

- la capacidad mínima offline cubre efectivo;
- tarjeta, billetera u otro medio que requiera autorización externa no se marca
  aprobado sin respuesta; sólo podrá operar offline si el proveedor y otro ADR
  definen un protocolo verificable;
- crédito offline requiere límites y autorización local explícitos; no se presume;
- el stock offline es una proyección potencialmente desactualizada. La política
  puede bloquear productos, cantidades o importes de alto riesgo y debe conciliar
  diferencias al sincronizar;
- precios, impuestos y permisos tienen versión, antigüedad máxima y regla de
  expiración; una terminal no vende indefinidamente con datos vencidos;
- el POS no inventa cumplimiento fiscal. La estrategia de contingencia,
  numeración, documento y transmisión aplicable en Paraguay se verificará contra
  manual, XSD, catálogos y reglas SIFEN oficiales vigentes antes de implementar o
  certificar.

### 6. Seguridad y autorización offline

La terminal debe estar registrada y aprovisionada online antes de operar offline.
La caracterización decidirá si se permite iniciar una sesión nueva sin conexión o
solamente continuar una sesión válida; cualquiera de los dos recorridos exige una
prueba local verificable, vencimiento acotado y permisos offline separados.

La UI mostrará empresa, terminal, cajero, estado de red, antigüedad del paquete,
última sincronización, cantidad pendiente y conflictos. Una revocación central
puede tardar en llegar mientras no haya conexión; ese riesgo se limita mediante
vencimiento, topes y reconciliación, y debe aparecer en el análisis de amenazas.

### 7. Plan de trabajo obligatorio

La épica de POS incorporará, sin cambiar su orden 12:

1. `POS-OFF-00`: caracterización, amenazas, topología y ADR técnico;
2. `POS-OFF-01`: aprovisionamiento, identidad del terminal, paquete firmado y
   proyecciones versionadas;
3. `POS-OFF-02`: almacenamiento cifrado, diario append-only y recuperación tras
   reinicio o energía interrumpida;
4. `POS-OFF-03`: checkout local permitido, numeración y política por pago/stock;
5. `POS-OFF-04`: sincronización idempotente, conciliación y compensaciones;
6. `POS-OFF-05`: experiencia de red/pendientes/conflictos y operación accesible;
7. `POS-OFF-06`: matriz integral de fallos, seguridad, rendimiento y demo.

No se considerará terminada la primera versión productiva del plugin si sólo vende
online.

### 8. Pruebas mínimas

La matriz deberá cubrir al menos:

- pérdida de conexión antes, durante y después de confirmar;
- reintento, mensajes duplicados, desordenados y respuesta central incierta;
- reinicio de navegador, runtime local y equipo con operaciones pendientes;
- dos o más terminales offline con identidades y rangos distintos;
- precio, permiso o paquete vencido; stock central divergente y terminal revocada;
- reloj incorrecto, almacenamiento lleno, corrupción detectable y restauración;
- reconexión parcial, caída repetida durante sincronización y recuperación;
- intento de pago electrónico sin proveedor y rechazo seguro;
- aislamiento entre empresas, cifrado, acceso físico no autorizado y ausencia de
  secretos en logs;
- demo real en 375, 720, 1280 px y tamaño físico aprobado, mostrando venta offline,
  reinicio, reconexión, sincronización y conflicto recuperable.

## Consecuencias

### Positivas

- una caída de Internet no detiene las ventas permitidas del comercio;
- las operaciones pendientes son visibles, durables y reconciliables;
- el ERP central conserva las fuentes de verdad y recibe efectos idempotentes;
- la capacidad offline queda diseñada desde el inicio del POS y no como parche.

### Costes y riesgos

- se necesita runtime, almacenamiento y operación local además del servidor
  central;
- autorización revocada, stock y precios pueden estar temporalmente desactualizados;
- sincronización y fiscalidad de contingencia amplían considerablemente la matriz;
- instalador, actualización, respaldo, observabilidad y soporte deben cubrir el
  componente local;
- el alcance exacto de arranque offline, crédito y pagos externos requiere decisión
  posterior antes de código.

## Alternativas descartadas

### Mantener la primera versión solamente online

Se descarta por decisión de producto: una caída de Internet no puede impedir toda
venta del establecimiento.

### Simular offline con caché o `localStorage` del navegador

Se descarta porque no ofrece un límite transaccional durable, cifrado, autoridad,
recuperación ni sincronización suficientes, y entra en conflicto con el baseline
Jakarta Faces server-side.

### Replicar el ERP completo en cada caja

Se descarta porque duplicaría dominios, datos privados y migraciones, multiplicaría
conflictos y convertiría cada terminal en una fuente de verdad independiente.

## Referencias

- [ADR-0027 - Terminal de punto de venta](0027-terminal-punto-venta-y-ampliacion-roadmap.md)
- [ADR-0013 - Eventos e idempotencia por plugin](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0006 - Identidad OIDC, membresía y autorización](0006-identidad-oidc-membresia-autorizacion.md)
- [ADR-0007 - Material Design 3 y pantallas responsive](0007-material-design-responsive-sobre-jsf.md)
- [Épica de terminal de punto de venta](../backlog/epica-terminal-punto-venta.md)
