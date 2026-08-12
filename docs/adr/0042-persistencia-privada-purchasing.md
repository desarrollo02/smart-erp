# ADR-0042 — Persistencia privada de `purchasing`

- Estado: Aceptado
- Fecha: 2026-08-11
- Historia: `J11-S9-03`
- Decisiones relacionadas: ADR-0012, ADR-0016, ADR-0024, ADR-0040 y ADR-0041

## Contexto

J11-S9-02 separó solicitud, orden, recepción y devolución en cuatro agregados y
definió snapshots para toda referencia externa. El siguiente corte debe conservar
esos documentos en PostgreSQL sin acceder a tablas de socios, catálogo, datos de
referencia o inventario; también debe impedir que una confirmación reescriba la
historia y dejar una frontera utilizable por la aplicación transaccional futura.

Las pruebas de Sprint 9 se acumulan por decisión de producto. Esta decisión no
reduce las invariantes ni permite presentar la persistencia como validada o
comercializable antes de J11-S9-07.

## Decisión

### Propiedad y unidad de persistencia

- `purchasing` es propietario exclusivo del esquema `plg_purchasing`.
- La primera migración vive en
  `classpath:db/migration/purchasing/V1__initialize_purchasing_schema.sql`.
- La unidad `logixone-purchasing-pu` usa el datasource JTA
  `java:/jdbc/LogixoneCoreDS`, ejecuta `hibernate.hbm2ddl.auto=validate` y no crea
  ni actualiza DDL.
- Toda cabecera y línea lleva `company_id`; ninguna FK apunta a `core` ni a un
  esquema de otro plugin.
- Proveedor, artículo, unidad y moneda se conservan como UUID/código y snapshot.
  Depósito, ubicación y movimiento de stock se conservan como identificadores
  públicos, nunca como relaciones JPA externas.

### Modelo relacional

La V1 separa nueve tablas privadas:

1. `purchase_request` y `purchase_request_line`;
2. `purchase_order`, `purchase_order_line` y `purchase_order_allocation`;
3. `goods_receipt` y `goods_receipt_line`;
4. `supplier_return` y `supplier_return_line`.

La asignación enlaza líneas dentro del mismo esquema y permite consumir
parcialmente una solicitud. Recepción y devolución referencian la línea de orden;
la devolución referencia además la línea exacta de recepción que compensa. La
posición explícita preserva el orden estable de líneas y asignaciones.

Cantidades y precios usan `NUMERIC(30,6)`. El pendiente no se persiste como una
fuente independiente: se deriva de
`ordenado - recibido + devuelto - faltante_cerrado`. No se usa EAV, JSON ni tipos
de punto flotante como fuente operativa.

### Invariantes, historia y concurrencia

- números de solicitud, orden, recepción y devolución son únicos por empresa;
- todas las FKs internas incluyen empresa;
- catálogo UUID/código, moneda esperada y metadatos de transición se validan como
  grupos completos;
- líneas `STOCK` exigen catálogo y destino/origen completo; `NON_STOCK` y
  `SERVICE` no admiten ubicación ni movimiento;
- cantidades son positivas y recibido/devuelto/faltante no pueden producir un
  pendiente negativo;
- un lock sobre las líneas privadas impide que asignaciones concurrentes superen
  la cantidad solicitada o la cantidad de la línea de orden;
- una solicitud fuera de `DRAFT` no admite mutar sus líneas;
- una recepción o devolución confirmada no admite modificar cabecera o líneas;
- confirmar una línea `STOCK` exige un `StockMovementId` previamente persistido
  en la misma transacción;
- cada raíz usa `@Version`; una escritura obsoleta se traduce a
  `VERSION_CONFLICT` y no existe borrado físico en los puertos públicos.

Los repositorios actualizan primero las referencias de movimiento y fuerzan
`flush`; luego confirman la cabecera. Así el trigger comprueba una forma completa
sin dejar un corte parcial: cualquier fallo revierte la transacción.

### Repositorios y errores estables

Cuatro puertos —solicitudes, órdenes, recepciones y devoluciones— reciben siempre
`CompanyId`. Reconstruyen agregados completos con líneas, asignaciones y snapshots.
Los adaptadores convierten duplicados, referencias privadas inválidas, documento
inmutable, versión obsoleta, ausencia y fallo de almacenamiento en códigos de
aplicación estables.

`insert` acepta únicamente raíces `DRAFT`; todo estado posterior se alcanza con
una transición versionada. J11-S9-04 aplica la misma regla al importar un
documento abierto, usando contexto de migración autorizado y conservando la fecha
de origen, en lugar de insertar directamente una raíz finalizada.

La V1 no crea el ledger de idempotencia/importación ni la auditoría de casos de
uso. J11-S9-04 agrega esas tablas mediante V2 y la demarcación JTA por código.

## Actualización J11-S9-04

[ADR-0043](0043-aplicacion-jta-idempotencia-purchasing.md) implementa la deuda de
aplicación sin alterar la propiedad del esquema:

- `purchasing_operation` conserva clave idempotente, operación, SHA-256, recurso y
  versión resultante;
- `purchasing_import` conserva empresa, sistema/registro de origen, checksum,
  SHA-256 e identidad del documento.

El baseline privado pasa de nueve tablas V1 a once tablas V1–V2. Antes de la
primera validación o promoción de V1 se completó el snapshot de línea con unidad
presentada/base y factor, y la trazabilidad física con lote, serie, vencimiento y
condición. Una vez aplicado el baseline, toda corrección seguirá requiriendo una
migración superior.

## Alternativas descartadas

- Relacionar entidades de Compras con entidades JPA de otros plugins: rompe
  propiedad y composición física.
- Una tabla única para todos los documentos: mezcla ciclos, multiplica nulos y
  dificulta inmutabilidad y compensaciones.
- Reescribir recepción al devolver: destruye el hecho histórico recibido.
- Guardar sólo acumulados en la orden: pierde documento, actor, destino y
  trazabilidad de recepción/devolución.
- Persistir pendiente como dato editable: crea una segunda verdad susceptible de
  divergencia.
- Usar JSON/EAV o `double`: debilita restricciones, precisión e índices.
- Crear ya outbox, auditoría o import ledger sin caso de uso: adelanta J11-S9-04.

## Consecuencias

- Desactivar o retirar `purchasing` conserva migración, tablas y datos.
- El descriptor aporta una migración, pero el JAR sigue fuera del WAR y migrador
  hasta J11-S9-06.
- Casos de uso, permisos, auditoría, idempotencia y atomicidad con inventario están
  implementados en J11-S9-04 y permanecen pendientes de pruebas.
- Pantallas y manual visual pertenecen a J11-S9-05.
- Toda corrección posterior al uso de V1 requiere V2 o superior.

## Verificación obligatoria pendiente

1. migrar PostgreSQL vacío, repetir sin cambios y validar checksum;
2. comprobar once tablas V1–V2, tipos, checks, índices, triggers y FKs sólo privadas;
3. validar JPA con generación DDL deshabilitada;
4. probar round-trip, orden estable e aislamiento por empresa;
5. probar números duplicados, referencias cruzadas, cantidades imposibles y
   documentos confirmados inmutables;
6. probar confirmación de stock con/sin movimiento y rollback atómico;
7. probar una carrera optimista real y el código `VERSION_CONFLICT`;
8. ejecutar módulo, PostgreSQL/Testcontainers, ArchUnit y reactor completo en el
   gate acumulado de Sprint 9.
