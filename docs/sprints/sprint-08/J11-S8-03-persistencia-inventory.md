# J11-S8-03 - Persistencia de `inventory`

- Estado: Completada
- Sprint: 8
- Fecha de inicio: 2026-07-31
- Gate principal: G2 datos
- ADR: [ADR-0024](../../adr/0024-persistencia-privada-inventory.md)

## Objetivo

Crear el esquema privado, la migración Flyway, los snapshots, mapeos JPA y
repositorios empresariales de inventario sin adelantar aplicación, seguridad,
interfaz o composición física.

## Alcance

- contribución de migración `plg_inventory` en el descriptor;
- V1 relacional para depósitos, ubicaciones, ítems inventariables, saldos,
  movimientos, reservas y conteos;
- unidad JPA independiente con `validate` y DDL deshabilitado;
- snapshots completos para reconstruir estado y evidencia histórica;
- seis puertos de repositorio siempre acotados por empresa;
- libro de movimientos append-only, idempotencia, reversión única, precisión,
  restricciones de saldo y bloqueo de conteos;
- conflictos estables y concurrencia optimista;
- pruebas unitarias y PostgreSQL/Testcontainers.

## Fuera de alcance

- casos de uso, autorización, permisos, auditoría y demarcación JTA;
- consulta de catálogo durante una operación real;
- menú, Jakarta Faces, Material Design, responsive y Playwright;
- composición WAR/migrador, Docker/Compose y datos de demo;
- compras, ventas, logística, costos, valoración, documentos y SIFEN.

## Criterios de aceptación

- **CA-01:** el descriptor declara V1 para `plg_inventory`.
- **CA-02:** Flyway crea nueve tablas y una segunda ejecución no reaplica V1.
- **CA-03:** no existen FKs, JPA ni SQL hacia otro esquema propietario.
- **CA-04:** claves y referencias internas incluyen empresa.
- **CA-05:** PostgreSQL protege cantidades, serie positiva, idempotencia y
  reversión única.
- **CA-06:** un advisory lock transaccional impide conteos activos solapados.
- **CA-07:** movimientos preservan unidad, factor, cantidad, ítem y descripción
  histórica sin leer catálogo privado.
- **CA-08:** JPA valida las nueve tablas sin crear o actualizar DDL.
- **CA-09:** los seis repositorios realizan round-trip completo y no ofrecen
  borrado físico.
- **CA-10:** una escritura obsoleta produce `VERSION_CONFLICT` estable.
- **CA-11:** API pública, dominio y puertos permanecen libres de JDBC, Hibernate y
  PostgreSQL; JPA reside solo en infraestructura.
- **CA-12:** módulo, PostgreSQL, ArchUnit, reactor y documentación quedan verdes.

## Resultado

Los criterios quedaron satisfechos. La evidencia reproducible, incluyendo los
fallos detectados y corregidos, está en
[Evidencia J11-S8-03](../../evidence/J11-S8-03-persistencia-inventory.md).

J11-S8-04 puede implementar aplicación, autorización, auditoría, idempotencia de
casos de uso y transacciones JTA sobre estos puertos. El plugin todavía no está
compuesto físicamente ni aporta menú o pantalla.
