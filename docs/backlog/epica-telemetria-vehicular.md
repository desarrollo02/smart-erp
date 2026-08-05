# Épica — Telemetría vehicular y seguimiento GPS

- Estado: Planificada como plugin funcional `vehicle_telemetry`
- Fecha: 2026-08-03
- Orden: 7, después de `logistics` y antes de `commercial_documents`
- Decisión: [ADR-0034](../adr/0034-plugin-telemetria-vehicular.md)
- Fuente: [caracterización del legado](../knowledge-base/vehicle-telemetry/legacy-characterization.md)
- Prioridad: futura; no modifica el trabajo activo de Sprint 8

## Objetivo

Recibir, normalizar y consultar telemetría de vehículos de cualquier categoría,
incluidos posición actual, recorridos, sensores y alertas, permitiendo pausar o
finalizar el seguimiento de forma autorizada, auditada y reversible sin acoplar el
ERP a un fabricante GPS.

## Alcance inicial

- dispositivos y conexiones de proveedor sin secretos en el dominio;
- asignación dispositivo–vehículo con vigencia;
- simulador e ingestión neutral por API, webhook, archivo o polling;
- posición actual e historial por rango acotado;
- velocidad, rumbo, odómetro, horas, temperatura, combustible u otras mediciones
  disponibles con unidad y calidad;
- recorridos, detenciones, geocercas y alertas básicas;
- estados `ACTIVE`, `PAUSED` y `STOPPED` del seguimiento;
- retención, cuarentena, idempotencia y recuperación;
- interfaz Jakarta Faces Material Design 3 responsive.

## Límites de propiedad

| Concepto | Propietario |
|---|---|
| vehículo, tipo, capacidades, transportista, conductor, ruta y viaje | `logistics` |
| dispositivo, asignación, observación, recorrido, geocerca, alerta y estado de seguimiento | `vehicle_telemetry` |
| producto, existencias y combustible inventariable | `commercial_catalog`/`inventory` |
| despacho físico de una estación | `fuel_station` |
| factura, remisión y snapshot comercial | `commercial_documents` |
| credenciales, protocolo y transporte de un proveedor GPS | adaptador técnico/configuración externa |

No existen relaciones JPA, joins ni repositorios cruzados. El plugin conserva
únicamente `VehicleId` y otras referencias públicas opacas.

## Recorridos principales

### Asignar un dispositivo

1. seleccionar un vehículo activo publicado por `logistics`;
2. registrar o seleccionar un dispositivo administrable;
3. validar empresa, conexión, compatibilidad y ausencia de otra asignación
   solapada;
4. fijar vigencia y política de seguimiento;
5. activar y auditar la asignación.

### Consultar posición y recorrido

1. revalidar empresa, plugin y permiso de ubicación actual o histórica;
2. seleccionar vehículo y rango permitido;
3. mostrar última posición, calidad e instante recibido;
4. consultar puntos/segmentos paginados o agregados;
5. distinguir pérdida de señal, pausa, dato tardío y ausencia real de movimiento.

### Pausar o finalizar seguimiento

1. seleccionar vehículo/asignación y acción exacta;
2. explicar si se pausa la aceptación/publicación o se cierra la asignación;
3. exigir permiso, motivo, versión y confirmación;
4. registrar transición `ACTIVE -> PAUSED/STOPPED`;
5. conservar historia y permitir reanudación solo desde un estado válido.

La acción nunca implica inmovilización o apagado remoto del vehículo.

## Historias propuestas

| Historia | Resultado |
|---|---|
| VT-01 | confirmar VT-D01–VT-D10, proveedor, volumen, retención, privacidad, mapas y semántica de pausa |
| VT-02 | módulos `vehicle-telemetry-api`/`vehicle-telemetry`, descriptor y dominio Java puro |
| VT-03 | persistencia privada, partición/índices justificados, asignaciones, observaciones, inbox/outbox y retención |
| VT-04 | aplicación, permisos, auditoría, tracking lifecycle y consultas acotadas |
| VT-05 | contrato de ingestión, simulador, idempotencia, eventos tardíos, cuarentena y recuperación |
| VT-06 | posición actual, recorridos, geocercas y alertas básicas |
| VT-07 | UI responsive, selectores gobernados y visualización neutral de mapa/alternativa textual |
| VT-08 | primer adaptador real autorizado, composición, carga, seguridad, demo, manuales y PDF |

## Decisiones de producto registradas

| Código | Decisión |
|---|---|
| VT-D01 | Crear `vehicle_telemetry` como plugin funcional independiente. |
| VT-D02 | Ubicarlo después de `logistics`, en el orden 7. |
| VT-D03 | `logistics` posee `VehicleId`, clasificación y capacidades; telemetría admite cualquier categoría por referencia pública. |
| VT-D04 | Dispositivos, observaciones, recorridos, geocercas y alertas pertenecen a telemetría. |
| VT-D05 | Fabricantes y proveedores se aíslan en adaptadores versionados. |
| VT-D06 | El ciclo inicial de seguimiento es `ACTIVE`, `PAUSED`, `STOPPED` y no borra historia. |
| VT-D07 | Inmovilización, apagado o comandos físicos remotos quedan fuera del alcance inicial. |
| VT-D08 | PostgreSQL es la persistencia inicial; extensiones especializadas requieren volumen y ADR. |
| VT-D09 | Ubicación actual/histórica es sensible y exige permisos, auditoría y retención. |
| VT-D10 | No se inicia código hasta cerrar Sprint 8 y estabilizar los contratos de vehículo de `logistics`. |

## Criterios de aceptación

- **VT-CE01:** cada dispositivo, asignación y observación pertenece a una empresa
  verificada.
- **VT-CE02:** un dispositivo no tiene asignaciones activas solapadas y el cambio de
  vehículo conserva historia.
- **VT-CE03:** repetir un evento del proveedor no duplica posición, recorrido,
  alerta ni publicación.
- **VT-CE04:** eventos tardíos o fuera de orden conservan ambos instantes y no
  retroceden silenciosamente la última posición.
- **VT-CE05:** coordenadas, velocidad, rumbo y sensores validan rango, unidad,
  precisión, procedencia y calidad.
- **VT-CE06:** `PAUSED` y `STOPPED` impiden nueva publicación según su política sin
  borrar observaciones previas.
- **VT-CE07:** reanudar o reasignar exige versión vigente, permiso y auditoría.
- **VT-CE08:** `logistics` opera sin telemetría y documentos comerciales no dependen
  de posición GPS.
- **VT-CE09:** API y dominio no contienen DTO, SQL, tabla, acción, SDK o secreto del
  proveedor.
- **VT-CE10:** las consultas actuales, históricas, sensibles y exportaciones tienen
  permisos distintos y pruebas negativas.
- **VT-CE11:** logs, URL, mensajes y auditoría general no exponen coordenadas,
  recorridos, identidad personal, tokens o payloads crudos.
- **VT-CE12:** caída/reinicio continúa desde cursor confirmado sin pérdida o
  duplicación silenciosa.
- **VT-CE13:** carga y retención se prueban con frecuencia, vehículos y período
  declarados antes de producción.
- **VT-CE14:** UI y alternativa textual funcionan en 375, 720 y 1280 px, teclado,
  foco, vacío, señal perdida, pausa, error y acceso denegado.
- **VT-CE15:** ningún botón o API del primer alcance inmoviliza, apaga o controla
  físicamente un vehículo.

## Contratos públicos previstos

`vehicle-telemetry-api` será Java puro y expondrá contratos mínimos como:

- `TelemetryDeviceId`, `VehicleTrackingAssignmentId`, `TelemetryObservationId`,
  `JourneyId`, `GeofenceId` y `TelemetryAlertId`;
- `VehicleTelemetryStatus` y última posición autorizada;
- consulta acotada de recorrido con precisión/calidad;
- comando idempotente neutral de ingestión para adaptadores;
- eventos pasados `vehicle_telemetry.position.received`,
  `vehicle_telemetry.tracking.paused`, `vehicle_telemetry.tracking.resumed`,
  `vehicle_telemetry.tracking.stopped` y
  `vehicle_telemetry.geofence.transitioned`.

No expondrá entidades JPA, credenciales, payloads de fabricante ni una consulta
masiva sin empresa, permiso, vehículo y rango.

## Persistencia conceptual

El esquema `plg_vehicle_telemetry` tendrá equivalentes de:

- dispositivo y conexión lógica de proveedor;
- asignación histórica a vehículo y política de seguimiento;
- observación normalizada y mediciones tipadas;
- última posición derivada;
- recorrido/segmento, detención, geocerca, transición y alerta;
- transición auditada del tracking lifecycle;
- cursor, inbox, outbox, cuarentena y política de retención.

Las claves foráneas son privadas. `VehicleId`, viaje o conductor se conservan como
identificadores opacos y snapshots mínimos cuando corresponda.

## Selectores previstos

| Selector | Fuente/propietario | Clase y administración |
|---|---|---|
| vehículo | `logistics` | referencia operativa; administrar en logística con permiso propio |
| dispositivo | `vehicle_telemetry` | catálogo empresarial; alta/administración autorizada |
| conexión/proveedor | `vehicle_telemetry` y composición | configuración administrada sin mostrar secretos |
| estado de seguimiento | dominio de telemetría | estado cerrado, sin altas arbitrarias |
| geocerca | `vehicle_telemetry` | catálogo empresarial con ciclo activo/inactivo |
| regla de alerta | `vehicle_telemetry` | catálogo empresarial versionado |

## Permisos previstos

- `vehicle_telemetry.current.view`;
- `vehicle_telemetry.history.view`;
- `vehicle_telemetry.sensitive.view`;
- `vehicle_telemetry.devices.manage`;
- `vehicle_telemetry.tracking.manage`;
- `vehicle_telemetry.geofences.manage`;
- `vehicle_telemetry.alerts.manage`;
- `vehicle_telemetry.integrations.manage`;
- `vehicle_telemetry.export`.

## Gates

- JUnit para estados, vigencias, coordenadas, unidades y eventos tardíos;
- PostgreSQL/Testcontainers para constraints, partición/índices, concurrencia,
  idempotencia y retención;
- JPA/JTA para asignación, ingestión, pausa y outbox;
- ArchUnit y composición con telemetría presente/ausente/inactiva;
- simulador con duplicados, desorden, pérdida de señal, reloj incorrecto y reinicio;
- carga con volumen y frecuencia declarados;
- seguridad negativa, aislamiento empresarial y auditoría de lectura sensible;
- Docker/Compose, health, métricas, cuarentena y recuperación;
- Playwright en 375/720/1280 px;
- demo visual, fotografía de plugins, manuales, PDF y decisión del instalador.

## Fuera de alcance inicial

- inmovilización, apagado o cualquier comando físico remoto;
- conducción autónoma, diagnóstico mecánico completo o mantenimiento integral;
- protocolo/SDK de fabricante dentro del dominio;
- proveedor de mapas o scripts remotos embebidos sin decisión/licencia;
- acceso a tablas privadas de logística u otros plugins;
- migración de datos reales sin perfilado, retención y autorización;
- implementación durante Sprint 8.

## Orden y autorización

ADR-0034 amplía el roadmap a diecinueve reutilizables. `vehicle_telemetry` ocupa
el orden 7; `commercial_documents`, `recurring_billing`, `sifen`, `treasury`, POS,
`fuel_station`, finanzas y la familia de personas pasan a los órdenes 8–19.

Registrar esta épica no autoriza iniciar código. Primero deben cerrar Sprint 8 y
construirse `purchasing`, `sales` y `logistics`. VT-01 comenzará después de que
`logistics-api` estabilice `VehicleId` y la referencia pública de vehículo.

