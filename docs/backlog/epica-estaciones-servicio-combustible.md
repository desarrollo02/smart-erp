# Épica — Operación de estaciones de servicio de combustible

- Estado: Planificada como plugin funcional `fuel_station`
- Fecha: 2026-08-02
- Orden vigente: 13, después de `point_of_sale` y antes de `accounts_receivable`
- Decisión: [ADR-0032](../adr/0032-plugin-estaciones-servicio-combustible.md)
- Fuente: [caracterización del legado y referencias oficiales](../knowledge-base/fuel-station/legacy-characterization.md)
- Prioridad: futura; no modifica el trabajo activo de Sprint 8

## Objetivo

Administrar la operación física y trazable de estaciones de servicio y puestos de
consumo propio: tanques, surtidores, picos, lecturas, turnos, recepción, despacho y
conciliación, integrándose con los propietarios existentes de producto, stock,
venta, factura y dinero.

## Alcance inicial

- estaciones, playas/islas y zona horaria;
- tanques, capacidad, producto y estados;
- surtidores, picos, series y producto servido;
- turnos de playa, apertura y cierre;
- totalizadores y mediciones manuales/automáticas;
- recepción de cisterna y distribución por tanque;
- despachos idempotentes y referencia a POS;
- conciliación de inventario húmedo;
- verificaciones metrológicas, precintos e incidentes como evidencia;
- API/archivo neutral para importar operaciones de hardware;
- interfaz Jakarta Faces Material Design 3 responsive.

## Recorridos principales

### Configurar una estación

1. registrar estación, ubicación, zona horaria y estado;
2. agregar tanques con capacidad y producto público;
3. registrar máquinas y picos con serie/producto;
4. cargar verificaciones y vigencias conocidas;
5. activar únicamente recursos aptos y autorizados.

### Recibir combustible

1. identificar proveedor, remisión/orden por ID público y cisterna;
2. registrar mediciones antes de descarga;
3. distribuir cantidades entre tanques compatibles;
4. registrar mediciones posteriores y diferencias;
5. confirmar la recepción física y publicar el movimiento idempotente a
   `inventory`.

### Operar y cerrar un turno

1. abrir el turno y asignar actores;
2. capturar totalizadores y mediciones iniciales;
3. registrar/importar despachos;
4. asociar ventas POS cuando corresponda;
5. capturar lecturas finales;
6. calcular apertura + recepciones - despachos ± ajustes frente al cierre;
7. explicar y aprobar diferencias fuera de tolerancia;
8. cerrar y publicar los hechos confirmados.

## Historias propuestas

| Historia | Resultado |
|---|---|
| FS-01 | caracterización, ADR, referencias oficiales y confirmación FS-D01–FS-D10 |
| FS-02 | módulos `fuel-station-api`/`fuel-station`, descriptor y dominio Java puro |
| FS-03 | persistencia privada de estación, equipos, turnos, mediciones y evidencia |
| FS-04 | aplicación, permisos, auditoría, idempotencia y outbox/inbox |
| FS-05 | recepción, despacho y conciliación con `inventory` |
| FS-06 | integración opcional con POS/documentos/tesorería mediante contratos |
| FS-07 | UI responsive de configuración, turno, recepción y cierre |
| FS-08 | importador neutral, simulador y recuperación; sin comandos de hardware |
| FS-09 | composición, seguridad negativa, carga, demo, manuales y PDF |

## Criterios de aceptación

- **FS-CE01:** cada tanque, surtidor, pico, turno y medición pertenece a una empresa
  y estación verificadas.
- **FS-CE02:** un pico no puede servir un producto distinto del configurado.
- **FS-CE03:** lectura totalizadora menor que la anterior se rechaza o exige evento
  correctivo autorizado.
- **FS-CE04:** repetir un despacho importado no duplica volumen, venta ni movimiento
  de inventario.
- **FS-CE05:** recepción distribuye cantidades solo entre tanques compatibles y no
  excede límites operativos aprobados.
- **FS-CE06:** cierre reproduce la ecuación de inventario húmedo y conserva
  mediciones originales.
- **FS-CE07:** diferencias fuera de tolerancia requieren explicación, permiso y
  auditoría; no ajustan automáticamente stock contable.
- **FS-CE08:** precio efectivo, volumen e importe quedan como snapshot y no cambian
  al editar la lista de precios.
- **FS-CE09:** pico reprobado/fuera de servicio no admite nuevos despachos.
- **FS-CE10:** POS, inventario, documentos y tesorería se integran solo mediante IDs,
  contratos o eventos; ArchUnit demuestra ausencia de entidades/JPA cruzados.
- **FS-CE11:** caída/reinicio del importador continúa desde el último cursor
  confirmado sin perder ni duplicar operaciones.
- **FS-CE12:** secretos y credenciales de dispositivo permanecen fuera de código,
  logs, auditoría y tablas de negocio.
- **FS-CE13:** compacto 375 px, medio 720 px y expandido 1280 px permiten operar sin
  overflow horizontal normal.
- **FS-CE14:** Playwright cubre estación vacía, turno, recepción, cierre con
  diferencia, acceso denegado y equipo inhabilitado.
- **FS-CE15:** la UI distingue evidencia registrada de cumplimiento certificado y
  nunca afirma habilitación automática.

## Contratos públicos previstos

`fuel-station-api` será Java puro y expondrá únicamente contratos mínimos como:

- `ServiceStationId`, `FuelTankId`, `FuelNozzleId`, `ForecourtShiftId` y
  `FuelDispenseId`;
- referencia pública de estación/equipo/estado;
- comando idempotente de importación de despacho;
- consulta de turno y conciliación por empresa;
- eventos pasados `fuel_station.dispense.recorded`,
  `fuel_station.delivery.received`, `fuel_station.shift.closed` y
  `fuel_station.nozzle.disabled`, versionados al materializarlos.

No expondrá entidades JPA, credenciales, payload del fabricante ni documentos
regulatorios completos.

## Persistencia conceptual

El esquema `plg_fuel_station` tendrá equivalentes de:

- estación/playa, tanque, surtidor y pico;
- turno y asignación de actor;
- lectura totalizadora y medición de tanque append-only;
- recepción, distribución por tanque y despacho;
- conciliación, diferencia y aprobación;
- verificación metrológica, precinto e incidente;
- cursor/inbox/outbox propios del adaptador.

Las FKs son privadas al esquema. IDs de productos, depósitos, compras, POS y
documentos se guardan como identificadores opacos y snapshots mínimos.

## Permisos previstos

- `fuel_station.view`;
- `fuel_station.configuration.manage`;
- `fuel_station.shifts.operate`;
- `fuel_station.readings.record`;
- `fuel_station.deliveries.receive`;
- `fuel_station.dispenses.import`;
- `fuel_station.reconciliations.close`;
- `fuel_station.variances.approve`;
- `fuel_station.compliance.manage`;
- `fuel_station.integrations.manage`.

## Gates

- JUnit para precisión, estados e invariantes;
- PostgreSQL/Testcontainers para constraints, concurrencia, cursores e
  idempotencia;
- JPA/JTA para turno, recepción, conciliación y outbox;
- ArchUnit y matrices con dependencias opcionales presentes/ausentes;
- simulador de controlador y pruebas de pérdida/repetición/fuera de orden;
- pruebas de carga con volumen de despachos declarado;
- Docker/Compose, health, métricas, cuarentena y recuperación;
- Playwright en 375/720/1280 px y seguridad negativa;
- documentación oficial vigente registrada con versión/checksum;
- demo visual, fotografía de plugins, manuales y PDF de cierre.

## Fuera de alcance inicial

- control remoto de surtidor, bomba o válvula;
- SDK/protocolo específico de fabricante dentro del dominio;
- GLP, GNV, hidrógeno o carga eléctrica;
- tienda, lavadero, fidelización o flota completa;
- fijación estatal de precios o certificación automática;
- acceso a tablas privadas de otros plugins.

## Orden y autorización

ADR-0032 incorporó la épica, ADR-0033 desplazó su orden de 11 a 12 y ADR-0034 lo
desplazó a 13. El roadmap vigente contiene diecinueve reutilizables y desplaza
`accounts_receivable`, `accounts_payable`, `accounting`, `human_resources`,
`payroll` y `payroll_paraguay` a los órdenes 14–19. No se inicia hasta cerrar
Sprint 8 y completar los plugins 4–12.
