# ADR-0046 — Familia de mantenimiento de flota y taller automotriz

- Estado: Aceptado para planificación; implementación no autorizada
- Fecha: 2026-08-12
- Decisión de producto: incorporar todos los puntos propuestos para mantenimiento
  de vehículos y separar la operación técnica de la venta de servicios de taller
- Familia propuesta: Flota, con orden interno F1–F2
- Plugins propuestos: `fleet_maintenance` y `automotive_workshop`
- Fuente: [caracterización controlada del Taller legado](../knowledge-base/vehicle-maintenance/legacy-characterization.md)
- Épicas: [Mantenimiento de flota](../backlog/epica-mantenimiento-flota.md) y
  [Taller automotriz comercial](../backlog/epica-taller-automotriz-comercial.md)

## Contexto

El legado Miaterra actualizado contiene un módulo visible como Gestión de
Operaciones/Taller con solicitudes, órdenes de trabajo, personal, actividades,
checklists, repuestos, movimientos de stock, mantenimiento programado, acceso
móvil, consultas y tableros. También mezcla en las mismas entidades y
controladores vehículos, clientes, preventas, contratos, compras, stock,
combustible, tesorería, transporte y liquidaciones.

Smart ERP ya planifica fronteras separadas para participantes, catálogo,
inventario, compras, ventas, logística, telemetría, documentos comerciales,
tesorería, cuentas y recursos humanos. Copiar el Taller como un único módulo
recrearía las asociaciones JPA y reglas cruzadas del legado. Incorporar la
capacidad dentro de `logistics` haría que rutas y despachos poseyeran planes,
checklists, mano de obra, repuestos y costos de reparación. Incorporarla en
`vehicle_telemetry` mezclaría observaciones de dispositivos con decisiones de
mantenimiento.

Existe además una diferencia empresarial importante:

1. mantener vehículos utilizados por la propia empresa, con trabajos internos o
   contratados a proveedores;
2. vender diagnóstico y reparación a propietarios externos, con recepción,
   presupuesto, autorización, facturación y deuda.

Ambos recorridos comparten la ejecución técnica, pero no el ciclo comercial. La
decisión separa esas responsabilidades.

## Decisión

### 1. Familia, identidad y conteo

Se crea una familia vertical opcional de Flota con secuencia propia:

| Orden interno | Plugin | Propósito |
|---:|---|---|
| F1 | `fleet_maintenance` | planificar, ejecutar y analizar el mantenimiento técnico de vehículos |
| F2 | `automotive_workshop` | vender y coordinar servicios de taller para vehículos de clientes sin duplicar la ejecución técnica |

- La familia no recibe números ERP 20–21 y no renumera la secuencia ERP 1–19.
- El catálogo global planificado aumenta de treinta y uno a treinta y tres
  plugins reutilizables.
- F1 puede planificarse cuando `logistics-api` publique una identidad estable de
  vehículo; `vehicle_telemetry` es una capacidad opcional.
- F2 se construye después de F1 y de estabilizar `sales` y
  `commercial_documents`.
- J11-S9-06 continúa siendo el siguiente trabajo autorizado. Este ADR no agrega
  módulos Maven, descriptores, migraciones, pantallas ni composición ejecutable.

### 2. Propiedad de `fleet_maintenance`

F1 será dueño de:

- políticas, planes y versiones de mantenimiento;
- intervalos por tiempo, kilometraje u horas de operación;
- solicitudes técnicas, defectos e inspecciones;
- órdenes de trabajo y su ciclo de estado;
- actividades, plantillas y checklists tipados/versionados;
- asignaciones técnicas, tiempos de trabajo y puestos del taller;
- requerimientos y snapshots de consumo de repuestos;
- trabajos tercerizados y su correlación operativa;
- lecturas aceptadas para decidir mantenimiento, con fuente y calidad;
- indisponibilidad técnica, evidencia, historia, costos y métricas;
- inbox/outbox e idempotencia propias.

No será dueño del maestro de vehículo, dispositivos GPS, artículos, depósitos,
stock, órdenes de compra, empleados, proveedores, facturas, caja, deuda o
asientos.

### 3. Propiedad de `automotive_workshop`

F2 será dueño de:

- recepción comercial del vehículo y evidencia de condición de ingreso;
- correlación entre cliente, propietario autorizado, vehículo y visita;
- solicitud de diagnóstico y autorización para inspeccionar;
- autorización del cliente sobre alcance, límite y excepciones del trabajo;
- correlación con presupuesto/pedido de Ventas y orden técnica de F1;
- comunicación, entrega, aceptación y evidencia comercial del servicio;
- métricas de conversión, espera del cliente y entrega.

No duplicará orden técnica, repuesto, precio maestro, presupuesto/pedido
canónico, factura, cobro, deuda ni asiento. `sales` conserva presupuesto y pedido;
`commercial_documents` conserva factura/notas; `treasury` conserva dinero y
`accounts_receivable` la deuda comercial.

### 4. Vehículo, propietario y disponibilidad

`logistics` conserva `VehicleId`, clasificación, capacidades, matrícula/chapa y
estado público mínimo. La referencia puede representar un vehículo propio o de
tercero; el propietario o cliente se resuelve mediante `BusinessPartnerId` cuando
corresponda. F1 y F2 guardan IDs opacos y snapshots históricos necesarios, sin FK
ni asociaciones JPA entre plugins.

F1 publica eventos de restricción y liberación técnica. Logística decide cómo
esas señales afectan planificación, despacho o disponibilidad operacional. F1 no
actualiza tablas privadas de logística y Logística no cambia una orden de trabajo.

### 5. Integración con inventario, compras y catálogo

- `commercial_catalog` conserva artículos, servicios y unidades. F1 puede poseer
  actividades técnicas y checklists, pero un repuesto comercial se referencia
  por identificador público.
- `inventory` conserva existencia, reserva, salida, devolución y costo de stock.
  F1 solicita operaciones idempotentes y conserva el resultado/snapshot en la OT.
- `purchasing` conserva solicitudes, órdenes, recepciones y devoluciones. F1 puede
  solicitar un repuesto o servicio externo mediante un comando público y guardar
  la correlación.
- La falta o rechazo de una operación externa no se oculta: la OT queda en un
  estado recuperable y no simula consumo o compra.

### 6. Telemetría y lecturas

`vehicle_telemetry` conserva dispositivos, observaciones crudas/normalizadas,
recorridos y sensores. F1 puede consumir kilometraje, horómetro o alertas por
contrato/evento público, pero debe validar fuente, unidad, instante, calidad,
secuencia y plausibilidad antes de aceptar una lectura para mantenimiento.

Una observación tardía, duplicada o inválida no genera otra OT. El disparo
preventivo se identifica por empresa, vehículo, plan, versión, umbral y ciclo de
mantenimiento. También se admiten lecturas manuales autorizadas y auditadas
cuando no exista telemetría.

### 7. Personas, BPM y contabilidad

- Recursos Humanos conserva el empleado. F1 usa `ActorId` o una referencia
  pública y guarda asignación, función y tiempo en el contexto de la OT.
- BPM puede coordinar aprobaciones, vencimientos o escalamiento, pero F1 conserva
  estados, permisos e invariantes. La familia funciona con BPM ausente o inactivo.
- Contabilidad consume hechos públicos de costo; ningún plugin de Flota escribe
  asientos ni depende de tablas contables.

### 8. Ciclos iniciales

La solicitud técnica usará inicialmente:

`OPEN → TRIAGED → APPROVED → CONVERTED/CLOSED`, con `REJECTED` y `CANCELLED` como
finales explícitos.

La orden de trabajo usará:

`DRAFT → PLANNED → RELEASED → IN_PROGRESS → PAUSED → COMPLETED`, con
`CANCELLED` como cierre alternativo.

Una orden completada o cancelada es inmutable para operaciones sensibles. Una
reapertura crea una transición explícita, motivo, autorización y nueva versión;
no reescribe el historial. Iniciar, pausar, continuar, consumir repuestos,
completar y cancelar requieren versión esperada e idempotencia.

### 9. Persistencia conceptual

El esquema privado `plg_fleet_maintenance` tendrá equivalentes de:

- `maintenance_policy`, `maintenance_plan`, `maintenance_plan_version` y
  `maintenance_trigger`;
- `maintenance_request`, `defect_report` y `inspection`;
- `work_order`, `work_order_task`, `work_order_event` y `work_order_attachment`;
- `checklist_definition`, `checklist_version`, `checklist_item` y
  `checklist_result`;
- `technician_assignment`, `labor_entry` y `workshop_bay_booking`;
- `part_requirement`, `part_operation_snapshot` y `external_service_reference`;
- `accepted_meter_reading`, `vehicle_downtime`, `maintenance_cost_snapshot`,
  inbox/outbox y proyecciones.

El esquema privado `plg_automotive_workshop` tendrá equivalentes de:

- `customer_vehicle_visit`, `vehicle_intake` y `intake_evidence`;
- `diagnostic_authorization` y `repair_authorization`;
- correlaciones con `VehicleId`, `BusinessPartnerId`, `SalesQuoteId`,
  `SalesOrderId`, `WorkOrderId` y `CommercialDocumentId`;
- `customer_communication`, `vehicle_delivery` y aceptación;
- inbox/outbox, idempotencia, historia y métricas.

Los nombres se congelarán en historias de persistencia. No existen todavía
tablas, entidades o migraciones con estos nombres.

### 10. Interfaz y permisos

F1 planifica pantallas responsive para dashboard, historial por vehículo, planes,
solicitudes/defectos, directorio y ficha de OT, ejecución móvil, agenda/puestos,
repuestos/servicios y costos/indisponibilidad.

F2 planifica recepción, autorización de diagnóstico, seguimiento de presupuesto,
autorización de reparación, estado del servicio y entrega del vehículo.

La UI usa Jakarta Faces 4.1, Material Design 3 y 375/720/1280 px. Checklists
permiten tipos cerrados como booleano, número, rango, texto acotado y opción
versionada. No se aceptan plantillas HTML, EL, CSS, JavaScript o JSON ejecutable
arbitrario. Un acceso móvil o externo es autenticado, acotado por operación,
empresa, vencimiento y revocación; no se reproducen enlaces permanentes por token
del legado.

Permisos iniciales previstos para F1:

- `fleet_maintenance.view`;
- `fleet_maintenance.plans.manage`;
- `fleet_maintenance.requests.create` y `.triage`;
- `fleet_maintenance.work_orders.manage` y `.release`;
- `fleet_maintenance.work_orders.execute` y `.complete`;
- `fleet_maintenance.parts.request` y `.consume`;
- `fleet_maintenance.schedule.manage`;
- `fleet_maintenance.costs.view` y `.export`.

F2 separará permisos de recepción, diagnóstico, presupuesto, autorización,
entrega y consulta comercial. Toda operación revalida empresa, plugin efectivo,
actor, permiso, vehículo, estado y versión en el servidor.

### 11. Combustible y lubricantes

Las cargas de combustible, totalizadores y consumo de flota no pertenecen a F1.
Permanecen en Logística, Telemetría o `fuel_station` según su propietario. Un
lubricante usado durante una reparación se trata como repuesto/insumo mediante
Catálogo e Inventario. Esta separación elimina la duplicación observada en el
Taller legado.

### 12. Referencias de gestión y codificación

ISO 55000/55001 orientará terminología, ciclo de vida y equilibrio entre costo,
riesgo y desempeño sin afirmar certificación. VMRS de ATA/TMC podrá orientar una
codificación interoperable de sistemas, componentes y reparaciones. Antes de
incorporar catálogos VMRS se verificará versión, licencia y derecho de
redistribución; no se copiarán tablas propietarias desde el legado ni desde una
fuente externa.

## Decisiones aprobadas

Producto aprobó el 2026-08-12, sin cambios, las decisiones FM-D01 a FM-D12 y
AW-D01 a AW-D10:

| ID | Decisión aprobada |
|---|---|
| FM-D01 | F1 es un plugin funcional independiente de Logística y Telemetría. |
| FM-D02 | Flota usa orden interno F1–F2 y no renumera ERP 1–19. |
| FM-D03 | Logística conserva el maestro y `VehicleId`; F1 usa referencias/snapshots. |
| FM-D04 | F1 posee planes, solicitudes, defectos, checklists y órdenes técnicas. |
| FM-D05 | Catálogo e Inventario conservan repuestos, stock y movimientos. |
| FM-D06 | Telemetría es opcional y F1 sólo usa lecturas aceptadas e idempotentes. |
| FM-D07 | Compras conserva reposición y servicios tercerizados. |
| FM-D08 | RR. HH. conserva empleados; F1 posee asignación y tiempo técnico. |
| FM-D09 | El ciclo de OT es cerrado, versionado, auditable y recuperable. |
| FM-D10 | El acceso móvil es autenticado, revocable y sin token permanente. |
| FM-D11 | Checklists no ejecutan HTML, EL, scripts ni configuración arbitraria. |
| FM-D12 | Combustible no pertenece a F1; lubricantes de mantenimiento usan Inventario. |
| AW-D01 | El taller comercial es F2, separado de F1. |
| AW-D02 | F2 comienza después de F1, Ventas y Documentos Comerciales. |
| AW-D03 | Vehículo y cliente pertenecen a Logística y Socios Comerciales. |
| AW-D04 | F2 posee recepción y autorizaciones, no el presupuesto canónico. |
| AW-D05 | F1 conserva la única orden técnica; F2 la referencia. |
| AW-D06 | Ventas y Documentos conservan presupuesto/pedido y factura/notas. |
| AW-D07 | Tesorería y Cuentas por Cobrar conservan pago y deuda. |
| AW-D08 | F2 conserva snapshots de aceptación, alcance y entrega. |
| AW-D09 | Toda aprobación externa es autenticada, expirable, revocable y auditada. |
| AW-D10 | F2 es opcional y F1 opera sin vender servicios a terceros. |

## Secuencia planificada

### F1 — `fleet_maintenance`

| Orden | Historia | Resultado |
|---:|---|---|
| 1 | FM-00 | validación del relevamiento, categorías, métricas y licencia VMRS |
| 2 | FM-01 | API Java pura, dominio, estados, eventos e idempotencia |
| 3 | FM-02 | esquema privado, migraciones, JPA y repositorios |
| 4 | FM-03 | aplicación JTA, permisos, auditoría e integraciones públicas |
| 5 | FM-04 | planes, disparos preventivos y lecturas aceptadas |
| 6 | FM-05 | solicitudes, órdenes, checklists, personal y repuestos |
| 7 | FM-06 | UI responsive, ejecución móvil, agenda y tableros |
| 8 | FM-07 | composición con integraciones presentes/ausentes y demo |
| 9 | FM-08 | matriz integral, manuales, PDF y cierre |

### F2 — `automotive_workshop`

| Orden | Historia | Resultado |
|---:|---|---|
| 1 | AW-00 | políticas comerciales, recepción, autorización y entrega |
| 2 | AW-01 | API Java pura y correlaciones públicas con F1/Ventas |
| 3 | AW-02 | esquema privado, historia, snapshots e idempotencia |
| 4 | AW-03 | aplicación, permisos y contratos con Ventas/Documentos |
| 5 | AW-04 | UI responsive de recepción, autorización y seguimiento |
| 6 | AW-05 | integración de entrega, factura, deuda y comunicaciones |
| 7 | AW-06 | composición, seguridad externa, demo y matriz integral |
| 8 | AW-07 | manuales, PDF, operación y cierre |

## Consecuencias

### Positivas

- mantenimiento preventivo/correctivo obtiene un propietario claro;
- Logística y Telemetría permanecen utilizables sin Taller;
- repuestos, compras, empleados y contabilidad conservan sus fuentes de verdad;
- el taller interno puede operar sin activar venta de servicios;
- el taller comercial reutiliza la orden técnica sin duplicarla;
- costos, indisponibilidad y reincidencias pueden analizarse con historia trazable.

### Costes y riesgos

- se agregan dos plugins, dos esquemas y nuevos contratos públicos;
- lecturas erróneas pueden adelantar o retrasar mantenimiento si no se validan;
- generación preventiva y consumo de repuestos requieren idempotencia extremo a
  extremo;
- una OT mal cerrada puede afectar disponibilidad, costo y facturación;
- accesos móviles, evidencias y datos de ubicación amplían la superficie de
  privacidad;
- el uso de VMRS puede requerir licencia;
- F2 depende de varios dominios y sólo debe comenzar con sus APIs estables.

## Alternativas descartadas

### Incluir mantenimiento en `logistics`

Se descarta porque despacho y transporte no deben poseer planes, checklists,
repuestos, técnicos ni costos de reparación.

### Incluir mantenimiento en `vehicle_telemetry`

Se descarta porque telemetría publica observaciones; no decide ni ejecuta trabajo
técnico.

### Un único plugin Taller para operación y venta

Se descarta porque mezclaría vehículos propios y de clientes, costo y precio,
orden técnica y factura, inventario y deuda.

### Copiar el módulo legado

Se descarta por asociaciones y controladores cruzados, estados incompatibles,
accesos externos por token, plantillas configurables y duplicación de combustible,
stock y documentos.

## Gates antes de implementar

- [ ] producto abre explícitamente FM-00 después de estabilizar `logistics-api`;
- [ ] se congelan categorías de vehículos, propiedad y disponibilidad pública;
- [ ] se define el contrato de lecturas aceptadas y operación sin telemetría;
- [ ] se definen reservas/consumos y compras idempotentes por contrato público;
- [ ] se aprueban retención, evidencias, privacidad y acceso móvil;
- [ ] se verifica licencia antes de incorporar cualquier catálogo VMRS;
- [ ] se prueban composición F1 presente/ausente e integraciones opcionales;
- [ ] F2 no comienza hasta completar F1, `sales` y `commercial_documents`;
- [ ] ambos plugins completan JUnit, ArchUnit, PostgreSQL/Testcontainers, JTA,
  outbox/inbox, seguridad negativa, Docker/Compose y Playwright responsive.

## Referencias

- [ADR-0011 — Roadmap de plugins](0011-roadmap-dependencias-plugins-productivos.md)
- [ADR-0013 — Eventos de integración y outbox](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0028 — Gobierno de selectores](0028-gobierno-de-selectores-y-datos-administrables.md)
- [ADR-0034 — Telemetría vehicular](0034-plugin-telemetria-vehicular.md)
- [ADR-0045 — BPM](0045-plugin-gestion-procesos-negocio-bpm.md)
- [ISO 55000:2024](https://www.iso.org/standard/83053.html)
- [ISO 55001:2024](https://www.iso.org/standard/83054.html)
- [ATA/TMC — VMRS Overview](https://tmc.trucking.org/VMRS-Overview)
