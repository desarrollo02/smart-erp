# J11-S9-03 — Persistencia de `purchasing`

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Sprint: 9
- Fecha: 2026-08-11
- Gate principal: G2 datos
- ADR: [ADR-0042](../../adr/0042-persistencia-privada-purchasing.md)

## Objetivo

Crear el esquema privado, la migración Flyway, los mapeos JPA y repositorios
empresariales de Compras sin adelantar casos de uso, permisos, UI o composición.

## Alcance implementado

- contribución de migración `plg_purchasing` en el descriptor;
- V1 relacional de nueve tablas para solicitudes, órdenes, asignaciones,
  recepciones y devoluciones;
- posiciones estables de líneas y asignaciones;
- snapshots externos sin FK o JPA hacia otros plugins;
- unidad JPA independiente con `validate` y DDL deshabilitado;
- cuatro repositorios siempre acotados por `CompanyId` y sin borrado físico;
- alta únicamente en `DRAFT` y transiciones posteriores versionadas;
- versionado optimista de las cuatro raíces;
- triggers de inmutabilidad y confirmación de stock con movimiento;
- bloqueo y límite relacional de asignaciones parciales concurrentes;
- traducción de errores PostgreSQL a códigos estables;
- pruebas unitarias, JPA y PostgreSQL/Testcontainers escritas para el gate
  acumulado.

## Fuera de alcance

- casos de uso, permisos, auditoría, JTA e idempotency/import ledger;
- resolución real de proveedor, catálogo, moneda o inventario;
- menús, Jakarta Faces, responsive y Playwright;
- composición WAR/migrador, Docker/Compose y datos de demo;
- factura de proveedor, deuda, pago, retención, costo o asiento;
- migración histórica o adaptador Oracle.

## Criterios de aceptación

- **CA-01:** el descriptor aporta una única migración privada. **Cumplido y probado.**
- **CA-02:** V1 declara nueve tablas normalizadas con `company_id`. **Cumplido en PostgreSQL.**
- **CA-03:** no existen FK, SQL o relaciones JPA hacia otro esquema. **Revisión
  estática, ArchUnit y PostgreSQL cumplidos.**
- **CA-04:** snapshots y posiciones reconstruyen documentos en orden estable.
  **Cumplido con round-trip JPA.**
- **CA-05:** cantidades usan `NUMERIC(30,6)` y el pendiente continúa derivado.
  **Cumplido y probado.**
- **CA-06:** líneas finalizadas y documentos confirmados son inmutables.
  **Cumplido con triggers reales.**
- **CA-07:** confirmar `STOCK` exige movimiento y no-stock/servicio no lo admite.
  **Cumplido en PostgreSQL.**
- **CA-08:** todas las FKs privadas incluyen empresa y referencias cruzadas
  fallan. **Cumplido y probado.**
- **CA-09:** las cuatro raíces usan `@Version` y conflictos estables.
  **Cumplido y probado.**
- **CA-10:** los cuatro puertos comienzan búsquedas por `CompanyId` y no exponen
  borrado. **Cumplido y probado.**
- **CA-11:** JPA valida sin crear o actualizar DDL. **Cumplido en PostgreSQL.**
- **CA-12:** ADR, historia, evidencia y manuales técnicos distinguen implementación
  de validación. **Cumplido.**

## Pruebas automatizadas ejecutadas

Se escribieron pruebas de recurso/migración, mapeos, errores estables, Flyway real,
aislamiento empresarial, FKs, cantidades, confirmación e inmutabilidad, además de
validación JPA y round-trip de los cuatro agregados. También se amplió ArchUnit
para reconocer el propietario JPA y prohibir que aplicación dependa de
infraestructura.

El corte reproducible `.tools/tmp/validation/J11-S9-05-automated` ejecutó:

```powershell
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl plugins/purchasing -am test
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl plugins/purchasing -am verify "-Dlogixone.postgres.integration=true"
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl tests/architecture-tests -am test
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml verify
```

Resultados: 19 unitarias y 6 integraciones de Compras verdes sobre PostgreSQL
18.4/Testcontainers; Flyway aplicó V1–V2 y validó idempotencia; 32 pruebas de
arquitectura y el reactor completo quedaron verdes. La primera ejecución real
detectó que un trigger compartido accedía a `NEW.return_state` sobre
`goods_receipt`; se separaron las funciones por tabla y la repetición quedó verde.

## Resultado

El código y la documentación del alcance están implementados y validados
automáticamente. La validación independiente y la aceptación comercial siguen
pendientes. J11-S9-04 quedó habilitada para aplicación, seguridad, auditoría, JTA
e idempotencia sobre estos puertos.
