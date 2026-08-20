# Inventario, tareas y métricas de floorplans de Sprint 10

- Estado: Aceptado como baseline de implementación
- Historia: J11-S10-00
- Fecha: 2026-08-14
- Decisión rectora: [ADR-0047](../../adr/0047-floorplans-operativos-transaccionales.md)
- Baseline funcional: J11-S9-07
- Alcance: caracterización; no modifica código, dominio, persistencia ni permisos

## 1. Convenciones

Las cinco clases cerradas son `MASTER_DATA`, `WORKLIST`,
`TRANSACTION_EDITOR`, `GUIDED_OPERATION` e `INQUIRY`. La clasificación expresa
el propósito dominante de la pantalla, no su apariencia. Una pantalla puede
incluir selección, resumen o consulta auxiliar sin cambiar de clase.

La frecuencia es una hipótesis de diseño, no una medición de producción:

- **alta**: operación diaria o repetida varias veces por jornada;
- **media**: operación periódica, semanal o mensual;
- **baja**: configuración, auditoría o administración eventual.

## 2. Inventario cerrado de pantallas vigentes

El baseline contiene 23 pantallas navegables: 6 propias del shell y 17
publicadas por plugins. No se clasifica una ruta de forma distinta según el actor.

| Nº | Propietario | Ruta o identidad | Propósito | Frecuencia | Justificación dominante |
|---:|---|---|---|---|---|
| 1 | shell | `/faces/admin/index.xhtml` | `INQUIRY` | baja | resume accesos y estado administrativo |
| 2 | shell | `companies.xhtml` | `MASTER_DATA` | baja | administra empresas y su ciclo de vida |
| 3 | shell | `plugins.xhtml` | `MASTER_DATA` | baja | administra composición empresarial permitida |
| 4 | shell | `security.xhtml` | `MASTER_DATA` | baja | administra membresías, roles y permisos |
| 5 | shell | `system-authority.xhtml` | `MASTER_DATA` | baja | administra autoridad global cerrada |
| 6 | shell | `audit.xhtml` | `INQUIRY` | baja | consulta eventos inmutables |
| 7 | `reference_plugin` | `/reference` · `dashboard` | `INQUIRY` | baja | muestra una contribución de referencia |
| 8 | `reference_data` | `/reference-data` · `catalogs` | `MASTER_DATA` | media | administra catálogos normativos/versionados |
| 9 | `business_partners` | `/business-partners` · `directory` | `MASTER_DATA` | media | directorio, alta y ficha de socios |
| 10 | `business_partners` | `/business-partners/definitions` | `MASTER_DATA` | baja | definiciones empresariales del módulo |
| 11 | `commercial_catalog` | `/catalog` · `items` | `MASTER_DATA` | media | administra artículos y servicios |
| 12 | `commercial_catalog` | `/catalog/price-lists` | `MASTER_DATA` | media | administra listas y precios |
| 13 | `commercial_catalog` | `/catalog/definitions` | `MASTER_DATA` | baja | unidades y definiciones del catálogo |
| 14 | `commercial_catalog` | `/catalog/variant-families` | `MASTER_DATA` | baja | administra familias de variantes |
| 15 | `commercial_catalog` | `/catalog/tax-profiles` | `MASTER_DATA` | baja | administra perfiles tributarios internos |
| 16 | `inventory` | `/inventory` · `stock` | `GUIDED_OPERATION` | alta | seleccionar existencia y ejecutar movimiento o reserva controlada |
| 17 | `inventory` | `/inventory/warehouses` | `MASTER_DATA` | media | administra depósitos y ubicaciones |
| 18 | `inventory` | `/inventory/counts` | `GUIDED_OPERATION` | media | recorre borrador, conteo, revisión y contabilización |
| 19 | `purchasing` | `/purchasing/requests` | `WORKLIST` | alta | prioriza solicitudes y decisiones pendientes; el alta es una tarea secundaria |
| 20 | `purchasing` | `/purchasing/orders` | `TRANSACTION_EDITOR` | alta | edita cabecera, líneas, asignaciones, resumen y estado |
| 21 | `purchasing` | `/purchasing/receipts` | `GUIDED_OPERATION` | alta | selecciona orden/línea, captura recepción y confirma stock |
| 22 | `purchasing` | `/purchasing/returns` | `GUIDED_OPERATION` | media | selecciona recepción/línea, captura causa y confirma devolución |
| 23 | `purchasing` | `/purchasing/tracking` | `INQUIRY` | alta | consulta cantidades pedidas, recibidas, devueltas y pendientes |

### Resolución de las pantallas hoy mixtas

- `inventory:stock` se clasifica como `GUIDED_OPERATION`. La búsqueda de un
  artículo es el selector de contexto de la operación; no convierte la pantalla
  en un maestro. Incorporar o inactivar el artículo continúa como acción auxiliar
  autorizada, visualmente separada de movimientos y reservas.
- `purchasing:requests` se clasifica como `WORKLIST`. Crear o editar el borrador
  abre un contexto de edición dentro del mismo contrato, pero la entrada y la
  priorización siguen siendo una bandeja.
- `purchasing:orders` se clasifica como `TRANSACTION_EDITOR`, aunque el directorio
  sea su entrada. El editor de cabecera y líneas es el trabajo principal.
- Recepciones, devoluciones y conteos son operaciones guiadas porque su orden,
  validaciones y confirmación irreversible importan más que la edición libre.

No se crean rutas ni `ScreenId` nuevos sólo para resolver la clasificación. El
contrato v2 describirá regiones y tareas subordinadas sin permitir que un plugin
aporte XHTML, CSS, JavaScript o EL.

## 3. Roles ficticios y tareas piloto

Los nombres son fixtures de prueba; no implican roles obligatorios para cada
empresa.

### 3.1 Mover existencias

- **Actor:** `operador.inventario`.
- **Objetivo:** registrar una entrada, salida o transferencia del artículo
  seleccionado.
- **Precondiciones:** empresa operativa; `inventory` activo; artículo de
  inventario activo; depósito y ubicación activos; permiso
  `inventory.movements.post`; seguimiento de lote/serie/vencimiento conocido.
- **Recorrido vigente:** abrir Existencias, seleccionar artículo, abrir
  Movimientos, elegir tipo, origen, destino cuando corresponda, condición,
  trazabilidad, cantidad y motivo; escribir origen técnico e idempotencia;
  registrar.
- **Recorrido candidato:** seleccionar o escanear artículo, elegir tipo, capturar
  únicamente datos operativos aplicables, revisar efecto origen/destino y
  confirmar. Origen técnico, identidad, versión e idempotencia son generados por
  el sistema.
- **Resultado:** movimiento append-only registrado y disponibilidad actualizada;
  el recibo visible identifica operación y resultado sin mostrar secretos.

### 3.2 Crear y enviar solicitud

- **Actor:** `solicitante.compras`.
- **Objetivo:** preparar una solicitud con una o más líneas y enviarla.
- **Precondiciones:** empresa operativa; dependencias y `purchasing` activos;
  artículo/servicio, unidad y moneda válidos; permisos
  `purchasing.view`, `purchasing.requests.create` y
  `purchasing.requests.submit`.
- **Recorrido vigente:** abrir Nueva solicitud, completar cabecera y primera línea,
  preparar, abrir Aprobación y enviar.
- **Recorrido candidato:** completar cabecera breve, agregar o editar líneas en el
  mismo editor, revisar resumen y enviar desde una acción primaria contextual.
- **Resultado:** solicitud `SUBMITTED`, inmutable para edición de líneas y visible
  en la bandeja del aprobador.

### 3.3 Aprobar o rechazar solicitud

- **Actor:** `aprobador.compras`, distinto del solicitante.
- **Objetivo:** decidir una solicitud enviada sin recorrer formularios de edición.
- **Precondiciones:** solicitud `SUBMITTED`; permiso
  `purchasing.requests.approve`; actor distinto del solicitante; versión vigente.
- **Recorrido vigente:** buscar, abrir resultado, abrir pestaña Aprobación y
  aprobar; el rechazo además exige motivo.
- **Recorrido candidato:** entrar en una bandeja filtrada a trabajo procesable,
  revisar resumen/líneas y ejecutar Aprobar o Rechazar. Rechazar solicita motivo
  en contexto y confirmación.
- **Resultado:** solicitud `APPROVED` o `REJECTED`, con actor y fecha auditables.

### 3.4 Crear y emitir orden

- **Actor:** `analista.compras`.
- **Objetivo:** registrar proveedor, moneda y múltiples líneas y emitir la orden.
- **Precondiciones:** proveedor activo, catálogos vigentes y permisos
  `purchasing.orders.create` y `purchasing.orders.issue`; una compra directa sin
  asignación requiere justificación.
- **Recorrido vigente:** abrir Nueva orden, completar cabecera y una primera línea,
  preparar, abrir Estado y emitir. Cada línea adicional usa un formulario
  separado.
- **Recorrido candidato:** editar cabecera, líneas y asignaciones en una superficie
  continua, con resumen persistente y acciones válidas por estado.
- **Resultado:** orden `ISSUED`, lista para recepción y seguimiento.

### 3.5 Recibir compra

- **Actor:** `receptor.compras`.
- **Objetivo:** confirmar la cantidad recibida y su destino de inventario.
- **Precondiciones:** orden `ISSUED` con cantidad pendiente; línea válida; para
  stock, depósito/ubicación activos y permisos `purchasing.receipts.create`,
  `purchasing.receipts.confirm` e `inventory.movements.purchase.post`.
- **Recorrido vigente:** abrir Nueva recepción, elegir orden y línea, cantidad,
  destino, trazabilidad/condición, preparar, abrir Confirmar y confirmar.
- **Recorrido candidato:** seleccionar o escanear orden/línea, capturar cantidad y
  sólo la trazabilidad aplicable, revisar el impacto y confirmar.
- **Resultado:** recepción `CONFIRMED`, movimiento de stock vinculado y orden con
  cantidades actualizadas en la misma transacción.

### 3.6 Devolver a proveedor

- **Actor:** `inspector.recepciones`.
- **Objetivo:** devolver una cantidad previamente recibida y registrar la causa.
- **Precondiciones:** recepción `CONFIRMED`; cantidad retornable; permisos
  `purchasing.returns.create`, `purchasing.returns.confirm` e
  `inventory.movements.purchase.post` para líneas de stock.
- **Recorrido vigente:** abrir Nueva devolución, elegir orden, recepción y línea,
  capturar cantidad y causa, preparar, abrir Confirmar y confirmar.
- **Recorrido candidato:** partir de la recepción o escanearla, elegir línea,
  cantidad y causa, revisar el efecto y confirmar.
- **Resultado:** devolución `CONFIRMED`, movimiento de stock inverso vinculado y
  cantidad pendiente de la orden recalculada.

## 4. Datos técnicos fuera de la transcripción del operador

### 4.1 Campos técnicos hoy visibles que deben desaparecer

| Contexto | Elementos vigentes | Decisión candidata |
|---|---|---|
| movimiento | `movement_source_type`, `movement_source_id`, `movement_idempotency` | derivados de ruta/acción/recurso y generados en servidor |
| crear reserva | `reservation_source_type`, `reservation_source_id`, `reservation_idempotency` | derivados y generados en servidor |
| gestionar reserva | `manage_reservation_id`, `manage_reservation_version`, `manage_reservation_idempotency` | selección por referencia legible; identidad/versión/clave ocultas |

### 4.2 Datos que ya son internos y deben seguir siéndolo

- UUID de solicitudes, órdenes, recepciones, devoluciones, líneas, asignaciones,
  movimientos y reservas;
- versión optimista del recurso y de sus dependencias;
- claves de idempotencia y claves estables de operación;
- actor, empresa y fecha técnica obtenidos de la sesión y del reloj del servidor;
- identidad del movimiento de stock generado al confirmar recepción/devolución;
- checksums, versión de importación y datos de auditoría;
- referencias internas enviadas por selectores: el usuario ve número, código,
  descripción y estado, nunca debe copiar un UUID.

Estos valores siguen viajando y revalidándose del lado servidor cuando son
necesarios. Ocultarlos no reduce autorización, concurrencia ni idempotencia.

## 5. Acciones válidas por estado, actor y permiso

### 5.1 Solicitud de compra

| Estado | Acción | Permiso | Restricción adicional | Riesgo visual |
|---|---|---|---|---|
| sin recurso | crear | `purchasing.requests.create` | al menos una línea válida | primaria |
| `DRAFT` | editar/agregar línea | `purchasing.requests.create` | sólo solicitante y versión vigente | primaria/secundaria |
| `DRAFT` | enviar | `purchasing.requests.submit` | sólo solicitante | primaria |
| `DRAFT`, `SUBMITTED` | cancelar | `purchasing.requests.create` | motivo obligatorio | destructiva separada |
| `SUBMITTED` | aprobar | `purchasing.requests.approve` | actor distinto del solicitante | primaria de bandeja |
| `SUBMITTED` | rechazar | `purchasing.requests.approve` | actor distinto y motivo obligatorio | destructiva/negativa |
| cualquier estado legible | clonar a borrador | `purchasing.requests.create` | nuevo número; nueva identidad | secundaria |
| `APPROVED`, `REJECTED`, `CANCELLED` | mutar original | — | no disponible | oculta y rechazada por servidor |

### 5.2 Orden de compra

| Estado | Acción | Permiso | Restricción adicional | Riesgo visual |
|---|---|---|---|---|
| sin recurso | crear | `purchasing.orders.create` | línea válida y justificación cuando aplica | primaria |
| `DRAFT` | agregar línea | `purchasing.orders.create` | versión vigente | primaria/secundaria |
| `DRAFT` | emitir | `purchasing.orders.issue` | documento válido | primaria |
| `DRAFT`, `ISSUED` | cancelar | `purchasing.orders.close` | ninguna recepción confirmada; motivo | destructiva separada |
| `ISSUED` | cerrar saldo corto | `purchasing.orders.close` | cubre todo pendiente; motivo | destructiva y confirmada |
| `ISSUED` | preparar recepción | `purchasing.receipts.create` | cantidad pendiente | primaria desde recepción |
| `CLOSED`, `CANCELLED` | editar/emitir | — | no disponible | oculta y rechazada por servidor |

### 5.3 Recepción y devolución

| Documento/estado | Acción | Permiso | Restricción adicional | Riesgo visual |
|---|---|---|---|---|
| recepción nueva | preparar | `purchasing.receipts.create` | orden emitida, cantidad pendiente y destino válido | primaria |
| recepción `DRAFT` | confirmar | `purchasing.receipts.confirm` | versión vigente; movimiento de compra autorizado para stock | irreversible, con revisión |
| recepción `CONFIRMED` | modificar/confirmar | — | inmutable | no disponible |
| devolución nueva | preparar | `purchasing.returns.create` | recepción confirmada y cantidad retornable | primaria |
| devolución `DRAFT` | confirmar | `purchasing.returns.confirm` | versión vigente; movimiento de compra autorizado para stock | irreversible, con revisión |
| devolución `CONFIRMED` | modificar/confirmar | — | inmutable | no disponible |

### 5.4 Movimiento de inventario

| Contexto | Acción/campos aplicables | Permiso y guardas |
|---|---|---|
| entrada `RECEIPT` | origen, condición, trazabilidad aplicable, cantidad, motivo | `inventory.movements.post`; artículo/ubicación activos |
| salida `ISSUE` | origen, condición, trazabilidad aplicable, cantidad, motivo | mismo permiso; saldo no negativo |
| `TRANSFER` | origen y destino distintos, misma trazabilidad/condición/cantidad | mismo permiso; débito y crédito atómicos |
| `ADJUSTMENT`, `REVERSAL` manual | no disponible en el piloto | `inventory.adjustments.post` y caso de uso específico, no formulario manual |
| confirmar movimiento | revisión de efecto y acción primaria | servidor revalida empresa, plugin, permiso, estado, versión e idempotencia |

## 6. Inventario de selectores de los pilotos

| Selector | Fuente y propietario | Clase | Ruta de administración/alta |
|---|---|---|---|
| artículo/servicio | `commercial_catalog` | referencia operativa | `/catalog`, con permiso del catálogo |
| proveedor | `business_partners` | referencia operativa | `/business-partners`, con permiso del módulo |
| unidad | `commercial_catalog` | catálogo empresarial | `/catalog/definitions` |
| moneda | `reference_data` | catálogo normativo | `/reference-data`; no admite alta arbitraria |
| depósito/ubicación | `inventory` | catálogo empresarial/referencia operativa | `/inventory/warehouses` |
| tipo de línea | `purchasing` | estado cerrado | sin alta; dominio versionado |
| estado de documento | `purchasing` | estado cerrado | sin alta; ciclo de vida del dominio |
| tipo de movimiento | `inventory` | estado cerrado | sin alta; entrada, salida o transferencia |
| condición de stock | `inventory` | estado cerrado | sin alta arbitraria |
| lote/serie/vencimiento | política del artículo y stock | referencia operativa/captura condicionada | sólo cuando la política lo requiere |
| orden, recepción y línea | `purchasing` | referencia operativa | búsqueda contextual; sin copiar identidad técnica |

Los selectores grandes usan búsqueda paginada. Al regresar de `Administrar`, el
shell refresca opciones y sólo preserva un borrador seguro. Inactivos permanecen
visibles en historia, pero no se ofrecen para operaciones nuevas.

## 7. Métricas reproducibles del baseline

### 7.1 Definiciones

- **AS — activaciones semánticas:** controles distintos usados desde la acción
  `Nueva` o la apertura de la tarea hasta el resultado. Un buscador compuesto
  cuenta como un control aunque internamente escriba y elija una opción.
- **CF — cambios de foco semántico:** transiciones entre esos controles; para el
  recorrido lineal vigente es `AS - 1`.
- **CT — campos técnicos transcritos:** entradas cuyo valor debería generar o
  resolver el servidor.
- **EV — errores visibles del happy path:** mensajes de validación o recuperación
  producidos durante el recorrido válido.
- **DV — desborde vertical documentado:**
  `max(0, alto_de_captura_fullPage - alto_viewport)`. No equivale al número de
  gestos de rueda, pero permite comparar renderer v1 y v2 sin opinión visual.
- **OH — overflow horizontal:** se acepta sólo cuando
  `document.documentElement.scrollWidth <= viewport + 1`.

La altura de viewport fijada para la comparación es 900 px. Los anchos son 375,
720 y 1280; también se ejecutan 599, 600, 839 y 840. Las capturas previas se
generaron con Playwright `fullPage`; sus dimensiones fueron releídas el
2026-08-14, sin modificar imágenes.

### 7.2 Valores actuales

| Tarea vigente | AS | CF | CT | EV | DV conocido | Fuente reproducible |
|---|---:|---:|---:|---:|---|---|
| movimiento de entrada | 11 | 10 | 3 | 0 | 838 px a 1280 | `InventoryVisualIT` y captura 1280×1738 |
| crear y enviar solicitud de una línea | 13 | 12 | 0 | 0 | 217/0/161 px a 1280/720/375 | `PurchasingVisualIT`; capturas 1280×1117, 720×900, 375×1061 |
| aprobar solicitud localizada | 5 | 4 | 0 | 0 | proxy de la misma pantalla: 217/0/161 px | `PurchasingVisualIT` |
| crear y emitir orden de una línea | 14 | 13 | 0 | 0 | 217/0/87 px a 1280/720/375 | `PurchasingVisualIT`; capturas 1280×1117, 720×900, 375×987 |
| preparar y confirmar recepción | 11 | 10 | 0 | 0 | 0 px conocido a 375 | `PurchasingVisualIT`; captura 375×900 |
| preparar y confirmar devolución | 10 | 9 | 0 | 0 | 0 px conocido a 720 | `PurchasingVisualIT`; captura 720×900 |

El baseline no inventa valores para anchos que la evidencia anterior no capturó.
J11-S10-05 debe ejecutar primero el renderer compatible v1 y luego el candidato
v2 con el mismo fixture, viewport y contador, completando la matriz antes de
aceptar una mejora.

### 7.3 Umbrales del candidato

| Tarea candidata | AS máximo | CT | EV | Condición adicional |
|---|---:|---:|---:|---|
| movimiento | 8 | 0 | 0 | tipo y seguimiento ocultan campos no aplicables |
| crear/enviar solicitud | 11 | 0 | 0 | líneas editables sin formulario aislado |
| aprobar/rechazar | 3 | 0 | 0 | trabajo procesable visible en bandeja |
| crear/emitir orden | 11 | 0 | 0 | cabecera, líneas y resumen en una superficie |
| recibir | 9 | 0 | 0 | orden/línea/destino se encadenan contextualmente |
| devolver | 8 | 0 | 0 | parte de recepción confirmada o búsqueda única |

Además:

- OH debe ser verdadero en 375, 599, 600, 720, 839, 840 y 1280;
- a 1280×900 la acción primaria y el resumen de una tarea de una línea deben ser
  alcanzables sin scroll previo; el DV del resultado no puede empeorar;
- a 720 y 375 la acción primaria puede quedar al final de la secuencia o en barra
  segura, pero nunca fuera del flujo de teclado ni cubierta por contenido;
- el candidato debe reducir AS al umbral y no aumentar DV frente al replay v1;
- agregar una segunda línea no abre otro directorio ni pierde la cabecera; el foco
  termina en la primera celda editable de la nueva línea;
- un error lleva el foco al primer campo inválido, conserva el borrador seguro y
  explica causa y recuperación;
- al completar una mutación, el foco vuelve al título/estado actualizado; ninguna
  acción oculta conserva foco;
- `prefers-reduced-motion` elimina transiciones no esenciales.

## 8. Teclado, escaneo y responsive

### Teclado y foco

- orden: título, contexto/búsqueda, contenido, acción primaria, secundarias y
  destructivas;
- `Enter` ejecuta búsqueda o confirma selección, pero no confirma una mutación
  irreversible desde un campo multilínea;
- `Escape` cierra diálogo o cancela selección local sin perder un borrador seguro;
- tablas y listas exponen encabezados, nombre accesible y apertura por teclado;
- el foco visible conserva contraste y no depende sólo del color.

Un lector de código de barras se trata como teclado. El campo marcado para escaneo
acepta caracteres y terminador `Enter`, valida el código, anuncia el resultado y
permite continuar sin mouse. No se agrega hardware ni API propietaria en Sprint
10.

### Adaptación por ancho

| Rango | Floorplan esperado |
|---|---|
| compacto 0–599, prueba 375/599 | una columna; bandejas en tarjetas; editor por secciones; resumen y acción seguros; sin tabla ancha obligatoria |
| medio 600–839, prueba 600/720/839 | lista y detalle secuenciales o división controlada; cabecera y líneas con resumen reubicado |
| expandido ≥840, prueba 840/1280 | lista-detalle cuando aplica; editor con cabecera/líneas y resumen lateral o persistente; operación guiada con contexto visible |

## 9. Estrategia de versión y compatibilidad aceptada

J11-S10-01 implementará esta frontera:

1. `PluginApiVersion.CURRENT` avanza aditivamente dentro de la familia `0.4.x`;
   los rangos `[0.4.0,0.5.0)` siguen siendo compatibles.
2. `ScreenDefinition` agrega una experiencia opcional mediante un tipo Java puro
   y conserva un constructor de cuatro argumentos para el código v1.
3. Una pantalla con `contractVersion 1.x` y experiencia ausente usa exclusivamente
   el renderer genérico vigente. No se la convierte implícitamente en
   `MASTER_DATA`.
4. Una pantalla con contrato `2.0.0` debe declarar uno de los cinco propósitos,
   regiones semánticas, acciones y disponibilidad contextual.
5. El shell soporta v1 y v2 durante Sprint 10; una versión mayor desconocida falla
   cerrada con diagnóstico, sin ejecutar una acción por aproximación.
6. La disponibilidad del cliente orienta el render; el servidor continúa
   revalidando empresa, plugin, pantalla, acción, permiso, estado, versión e
   idempotencia.
7. Los contratos no contienen Jakarta, XHTML, CSS, JavaScript, EL ni clases
   internas de otro plugin.

## 10. Gate para iniciar código

La caracterización se considera aceptada cuando historia, Sprint y evidencia
enlazan este documento y `tools/validate_docs.py` termina verde. Con ese gate se
habilita J11-S10-01. Siguen fuera de alcance `sales`, cambios de dominio o tablas,
una SPA, layouts arbitrarios y cualquier relajación de autorización.
