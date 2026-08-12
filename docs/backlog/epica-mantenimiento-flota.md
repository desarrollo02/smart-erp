# Épica — Mantenimiento de flota

- Estado: Planificada; FM-D01 a FM-D12 aceptadas el 2026-08-12
- Plugin: `fleet_maintenance`
- Clasificación: funcional vertical, reutilizable y opcional por empresa
- Orden interno: F1
- Prerrequisito principal: `logistics-api` con `VehicleId` estable
- Decisión: [ADR-0046](../adr/0046-familia-mantenimiento-flota-taller-automotriz.md)
- Fuente: [caracterización del Taller legado](../knowledge-base/vehicle-maintenance/legacy-characterization.md)

## Objetivo

Planificar, ejecutar y mejorar mantenimiento preventivo y correctivo de vehículos
propios o administrados, con órdenes técnicas, checklists, personal, repuestos,
servicios externos, costos, indisponibilidad e historia, sin apropiarse de los
maestros y transacciones de otros plugins.

## Valor de negocio

- reduce mantenimiento vencido y fallas evitables;
- hace visible qué vehículo está detenido, por qué y hasta cuándo;
- conserva costo total e historial por vehículo, plan, sistema y reparación;
- relaciona repuestos, mano de obra y servicios externos sin duplicar stock;
- permite comparar reincidencias, duración y desempeño del taller;
- admite operación con o sin telemetría;
- publica disponibilidad técnica para que Logística planifique con información.

## Audiencias

| Actor | Necesidad |
|---|---|
| solicitante/conductor | reportar falla y adjuntar evidencia |
| planificador | definir planes, prioridades, agenda y puestos |
| jefe de taller | aprobar/liberar OT, asignar personal y resolver bloqueos |
| técnico | ejecutar tareas, checklist, tiempos y consumos desde móvil |
| depósito | reservar, entregar, devolver y conciliar repuestos |
| compras | recibir solicitudes públicas de repuesto o servicio externo |
| logística | conocer restricción/liberación técnica del vehículo |
| dirección | analizar disponibilidad, costos, duración y reincidencia |
| auditoría/soporte | reconstruir versión, transición, actor y efecto externo |

## Capacidades

### Planes y disparos

- planes por tipo/categoría/vehículo y versiones publicadas inmutables;
- intervalos por calendario, kilómetros u horas;
- tolerancia, anticipación, prioridad y política de aplazamiento;
- lecturas manuales o de Telemetría con fuente, unidad, calidad e instante;
- generación idempotente de solicitud/OT por plan, versión, umbral y ciclo;
- proyección de próximos, vencidos y cumplimiento.

### Solicitudes, defectos e inspecciones

- reporte por vehículo, síntoma, severidad, ubicación, solicitante y evidencia;
- triage, aprobación/rechazo y conversión única;
- inspecciones configuradas con tipos cerrados y versiones;
- defecto relacionado con viaje/evento por ID público opcional;
- inmovilización sugerida sin control físico remoto.

### Orden técnica

- estado cerrado, versión optimista e idempotencia;
- tareas, dependencias, prioridad, fechas, puesto y asignaciones;
- checklist obligatorio por versión;
- tiempos activos y pausas con motivo;
- repuestos requeridos/reservados/consumidos/devueltos;
- compra o servicio tercerizado correlacionados;
- adjuntos y evidencia con política de retención;
- costo técnico por componentes y moneda;
- cierre, inmutabilidad y reapertura explícita.

### Seguimiento y mejora

- vehículo disponible/restringido y duración de indisponibilidad;
- agenda de taller y carga por puesto/técnico;
- mantenimiento próximo/vencido y cumplimiento del plan;
- costo, duración, reincidencia y consumo por vehículo/tipo;
- topologías o códigos normalizados cuando la licencia lo permita;
- exportación autorizada y proyecciones por eventos, sin joins privados.

## Límites y dependencias

| Dominio | Relación con F1 |
|---|---|
| `logistics` | requerido para `VehicleId`; conserva vehículo y decide disponibilidad operativa |
| `vehicle_telemetry` | opcional; publica lecturas/alertas, no genera ni posee OT |
| `commercial_catalog` | publica repuestos/servicios; F1 conserva referencias/snapshots |
| `inventory` | decide reservas, salidas, devoluciones y costo de stock |
| `purchasing` | decide solicitudes/órdenes/recepciones de repuestos y terceros |
| `business_partners` | publica proveedores o propietarios por ID |
| `human_resources` | publica personas/empleados; F1 posee asignación/tiempo de OT |
| `accounting` | consume hechos de costo; F1 no escribe asientos |
| `business_process_management` | coordinación opcional; F1 conserva autoridad |

F1 no gestiona combustible, rutas, dispositivos GPS, stock, compras, nómina,
facturas, pagos o deuda. Un lubricante utilizado durante una OT es un repuesto de
Catálogo/Inventario; una carga de combustible operacional no pertenece a F1.

## Modelo conceptual

- `MaintenancePlanId`, `MaintenancePlanVersionId`, `MaintenanceTriggerId`;
- `MaintenanceRequestId`, `DefectReportId`, `InspectionId`;
- `WorkOrderId`, `WorkOrderTaskId`, `ChecklistVersionId`;
- `TechnicianAssignmentId`, `LaborEntryId`, `WorkshopBayId`;
- `PartRequirementId`, `ExternalServiceReferenceId`;
- `AcceptedMeterReadingId`, `VehicleDowntimeId`;
- snapshots de vehículo, artículo, proveedor, moneda y costo cuando la historia lo
  requiera.

El futuro `fleet-maintenance-api` será Java puro. Expondrá referencias mínimas,
consulta autorizada de estado técnico y eventos pasados como:

- `fleet_maintenance.request.reported`;
- `fleet_maintenance.work_order.released`;
- `fleet_maintenance.work_order.completed`;
- `fleet_maintenance.vehicle.restricted`;
- `fleet_maintenance.vehicle.released`;
- `fleet_maintenance.maintenance.due`.

Los nombres/versiones se congelarán en FM-01.

## Estados

Solicitud:

`OPEN`, `TRIAGED`, `APPROVED`, `CONVERTED`, `REJECTED`, `CLOSED`, `CANCELLED`.

Orden:

`DRAFT`, `PLANNED`, `RELEASED`, `IN_PROGRESS`, `PAUSED`, `COMPLETED`,
`CANCELLED`.

No existe actualización arbitraria del estado desde UI. Cada transición declara
precondiciones, permiso, versión esperada, idempotencia y evento resultante.

## Pantallas previstas

| Pantalla | Objetivo |
|---|---|
| Dashboard | vencidos, próximos, indisponibilidad, costo y carga |
| Vehículo / historial | línea de tiempo técnica y próximos planes |
| Planes | definir, versionar, publicar y retirar políticas |
| Solicitudes y defectos | reportar, clasificar y convertir |
| Órdenes | buscar, filtrar, priorizar y exportar |
| Ficha de OT | tareas, checklist, personal, repuestos, terceros, evidencia e historia |
| Ejecución móvil | registrar trabajo autorizado sin enlace permanente |
| Agenda/puestos | programar recursos y visualizar conflictos |
| Costos e indicadores | duración, reincidencia, consumo e indisponibilidad |

Todos los selectores declararán fuente/propietario y ruta Administrar cuando
corresponda. La interfaz será JSF 4.1, Material Design 3, accesible y responsive en
375, 720 y 1280 px.

## Permisos iniciales

- `fleet_maintenance.view`;
- `fleet_maintenance.plans.manage`;
- `fleet_maintenance.requests.create`;
- `fleet_maintenance.requests.triage`;
- `fleet_maintenance.work_orders.manage`;
- `fleet_maintenance.work_orders.release`;
- `fleet_maintenance.work_orders.execute`;
- `fleet_maintenance.work_orders.complete`;
- `fleet_maintenance.parts.request`;
- `fleet_maintenance.parts.consume`;
- `fleet_maintenance.schedule.manage`;
- `fleet_maintenance.costs.view`;
- `fleet_maintenance.export`.

## Decisiones aceptadas FM-D01 a FM-D12

Las decisiones están detalladas en ADR-0046 y quedan aceptadas sin cambios:
plugin independiente; familia F1/F2; vehículo de Logística; planes/OT de F1;
repuestos y stock externos; Telemetría opcional; compras públicas; empleado
externo; estados cerrados; móvil seguro; checklist sin código y combustible fuera
de F1.

## Mapa de historias

| Orden | Historia | Entregable |
|---:|---|---|
| 1 | FM-00 | casos reales, categorías, métricas, políticas y licencia VMRS |
| 2 | FM-01 | API Java pura, dominio, estados, eventos e idempotencia |
| 3 | FM-02 | esquema privado, migraciones, JPA y repositorios |
| 4 | FM-03 | aplicación JTA, permisos, auditoría y adaptadores públicos |
| 5 | FM-04 | planes, lecturas aceptadas y generación preventiva |
| 6 | FM-05 | solicitudes, OT, checklist, personal, repuestos y terceros |
| 7 | FM-06 | UI responsive, móvil, agenda, tablero y manual |
| 8 | FM-07 | composición y demo con integraciones presentes/ausentes |
| 9 | FM-08 | gates integrales, PDF, cierre y decisión de instalador |

## Criterios de aceptación

- **FM-CE01:** cada consulta y comando se aísla por empresa y permiso.
- **FM-CE02:** F1 usa `VehicleId` público y no duplica el maestro.
- **FM-CE03:** planes/versiones publicados permanecen inmutables.
- **FM-CE04:** lectura duplicada, tardía o inválida no duplica mantenimiento.
- **FM-CE05:** una solicitud se convierte como máximo una vez.
- **FM-CE06:** transiciones de OT inválidas o concurrentes son rechazadas.
- **FM-CE07:** checklist obligatorio bloquea cierre hasta resolución autorizada.
- **FM-CE08:** reservas/consumos/devoluciones no duplican movimientos de stock.
- **FM-CE09:** una falla de Inventario o Compras deja estado recuperable.
- **FM-CE10:** completar/cancelar inmoviliza cambios sensibles y conserva historia.
- **FM-CE11:** restricción/liberación se publica sin escribir Logística.
- **FM-CE12:** F1 opera sin Telemetría, BPM y F2.
- **FM-CE13:** accesos móviles vencen, se revocan y revalidan autorización.
- **FM-CE14:** logs no contienen token, ubicación precisa, evidencia ni datos
  personales innecesarios.
- **FM-CE15:** las nueve pantallas cumplen teclado, foco, contraste y responsive.
- **FM-CE16:** el plugin presente/ausente/inactivo no rompe Logística ni otros ERP.
- **FM-CE17:** desactivar o retirar no elimina tablas, evidencia o historia.
- **FM-CE18:** composición, migraciones, JTA, outbox/inbox, PostgreSQL, seguridad,
  Docker/Compose, health y Playwright quedan verdes antes del cierre.

## Fuera de alcance V1

- venta de reparaciones a clientes (pertenece a F2);
- combustible y administración integral de neumáticos;
- campañas/recalls automáticos de fabricantes;
- diagnóstico remoto completo o control físico del vehículo;
- IA que apruebe o cierre reparaciones;
- certificación ISO 55001 o redistribución VMRS sin licencia;
- mantenimiento genérico de plantas, edificios o activos no vehiculares.
