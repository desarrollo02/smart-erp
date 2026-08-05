# ADR-0024 - Persistencia privada de `inventory`

- Estado: Aceptado
- Fecha: 2026-07-31
- Historia: `J11-S8-03`
- Decisiones relacionadas: ADR-0003, ADR-0012, ADR-0020 y ADR-0023

## Contexto

J11-S8-02 dejó verdes el contrato público y el dominio neutral de inventario. El
siguiente corte debe materializar depósitos, ubicaciones, inscripciones, saldos,
movimientos, reservas y conteos en PostgreSQL sin leer tablas de catálogo, sin
perder snapshots históricos y sin dejar carreras de concurrencia resueltas solo
en la interfaz.

## Decisión

### Propiedad y unidad de persistencia

- `inventory` es propietario exclusivo del esquema `plg_inventory`.
- La V1 vive en
  `classpath:db/migration/inventory/V1__initialize_inventory_schema.sql`.
- La unidad JPA `logixone-inventory-pu` usa el datasource JTA
  `java:/jdbc/LogixoneCoreDS`, ejecuta `hibernate.hbm2ddl.auto=validate` y no crea
  ni actualiza DDL.
- Toda raíz y detalle lleva `company_id`. No existe FK hacia `core.company` ni
  hacia `plg_commercial_catalog`.
- La identidad de catálogo se conserva como UUID y snapshot de código, nombre,
  unidad base y versión; nunca como relación JPA entre plugins.

### Modelo relacional

La V1 separa nueve tablas privadas:

1. `warehouse` y `stock_location` para depósitos y ubicaciones;
2. `inventory_item` para la inscripción local de un producto de catálogo;
3. `inventory_balance` para la proyección mutable por clave exacta;
4. `stock_movement` y `stock_movement_line` para el libro append-only y sus
   snapshots de conversión y catálogo;
5. `stock_reservation` para cantidad original, consumida, liberada y remanente
   derivado;
6. `stock_count` y `stock_count_line` para conteos controlados y diferencias.

`NUMERIC(30,6)` representa cantidades y `NUMERIC(30,12)` factores. Lote, serie,
vencimiento y condición forman parte de la clave de saldo. Las ausencias se
comparan con `UNIQUE NULLS NOT DISTINCT`, no con valores centinela.

### Invariantes y concurrencia

- cada depósito tiene un único código por empresa y una sola ubicación `GENERAL`;
- ubicaciones, ítems, saldos, reservas y conteos usan FKs compuestas privadas que
  incluyen empresa;
- físico, reservado y disponible no pueden ser negativos;
- una serie con saldo positivo no puede existir en dos buckets simultáneos;
- fuente e idempotencia de movimiento/reserva son únicas dentro de la empresa;
- un movimiento de reversión exige origen y un movimiento solo admite una
  reversión directa;
- cabecera y líneas de movimiento son inmutables desde los repositorios;
- depósitos, ubicaciones, inscripciones, saldos, reservas y conteos usan
  `@Version` y conflictos estables;
- un trigger de conteo obtiene `pg_advisory_xact_lock` por empresa/depósito y
  rechaza ámbitos activos solapados con SQLSTATE `23P01`;
- no existe operación de borrado físico en los puertos de repositorio.

### Snapshots y repositorios

El dominio publica snapshots internos del plugin, no contratos públicos nuevos.
Los repositorios reconstruyen agregados completos y siempre reciben `CompanyId`.
Seis puertos separan depósitos, inscripciones, saldos, movimientos, reservas y
conteos. Los adaptadores convierten duplicado, referencia inválida, bloqueo de
ámbito y versión obsoleta en códigos de aplicación estables.

El ID técnico de una fila de saldo permanece interno al adaptador. Los consumidores
identifican el saldo por `StockKey`; no se amplía la API pública para reflejar una
decisión física.

## Alternativas descartadas

- Una tabla de stock por producto sin ubicación/trazabilidad: pierde las
  dimensiones confirmadas en IN-D01 a IN-D10.
- Relacionar `inventory_item` con la entidad JPA o tabla de catálogo: viola la
  propiedad de plugins y acopla despliegue y migraciones.
- Guardar movimientos como una actualización del saldo sin libro: impide auditar,
  revertir y reproducir conversiones históricas.
- Usar JSON/EAV como fuente operativa: debilita claves, precisión, FKs e índices.
- Permitir saldos negativos y compensarlos luego: rompe disponibilidad y reservas.
- Bloquear conteos solo con una consulta Java: conserva una carrera entre
  transacciones concurrentes.
- Usar `double` o `float`: introduce error binario en cantidades y factores.

## Consecuencias

- Desactivar o retirar el plugin conserva migración, tablas y datos.
- Aplicación, permisos, auditoría y transacciones de casos de uso pertenecen a
  J11-S8-04; este ADR no los adelanta.
- Menús, pantallas y responsive pertenecen a J11-S8-05; composición física y demo
  candidata a J11-S8-06.
- La V1 aplicada es inmutable. Todo cambio posterior usa V2 o superior.
- El esquema protege invariantes estructurales; reglas que abarcan una operación
  completa, como transferencia y actualización de saldos, se orquestarán en una
  única transacción JTA en la historia de aplicación.

## Verificación obligatoria

1. migrar PostgreSQL vacío, repetir sin cambios y validar checksum;
2. comprobar nueve tablas, tipos, checks, índices y FKs exclusivamente privadas;
3. validar JPA con DDL deshabilitado;
4. probar aislamiento por empresa, clave de saldo, serie, negativos,
   idempotencia, reversión y bloqueo de conteos;
5. probar round-trip de todos los agregados y snapshots históricos;
6. probar una carrera optimista real y sus códigos estables;
7. comprobar ausencia de borrado, tipos flotantes y referencias cruzadas;
8. ejecutar módulo, PostgreSQL/Testcontainers, ArchUnit y reactor completo.
