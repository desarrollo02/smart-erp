# J11-S11-03 — Persistencia privada de `sales`

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Fecha: 2026-08-21
- Decisión: [ADR-0050](../../adr/0050-persistencia-privada-sales.md)

## Alcance implementado

- Flyway V1 con ocho tablas privadas en `plg_sales`;
- snapshots comerciales sin claves foráneas cruzadas;
- condición administrable e inactivable, cotización, pedido, líneas y reservas;
- vínculo único cotización→pedido por empresa;
- ledger idempotente e historia append-only;
- ocho entidades JPA y unidad `logixone-sales-pu` en modo `validate`;
- tres puertos/repositorios empresariales sin borrado físico;
- versión optimista para condición, cotización y pedido;
- IT Testcontainers para migración repetida, JPA y reconstrucción empresarial.

## Evidencia ejecutada

Tres cortes aislados ejecutaron pruebas de dominio, migración y JPA. El último
`-pl plugins/sales -am test` terminó verde con 9 pruebas propias y 9 módulos del
reactor. ArchUnit terminó con 37 pruebas verdes y el reactor requerido de 26
módulos verde. `mvn verify` terminó con los 30 módulos en `BUILD SUCCESS` en
4 min 21 s sobre la materialización final reproducible.

Docker Desktop fue iniciado de forma controlada. El primer gate PostgreSQL detectó
una incompatibilidad real entre `CHAR(64)` y el `VARCHAR(64)` esperado por JPA en
`request_fingerprint`; V1 todavía no estaba publicada, se corrigió y se repitió el
corte desde cero. El resultado final fue 2 pruebas Testcontainers verdes,
migración 1+0, Flyway `validate`, Hibernate `validate`, ocho tablas, repositorios,
reservas y aislamiento empresarial verdes sobre PostgreSQL 18.4. El reactor de 9
módulos con el perfil terminó en `BUILD SUCCESS` en 59,803 s.

J11-S11-04 queda habilitada. La validación independiente continúa pendiente.

## Fuera de alcance

Casos de uso JTA, permisos, coordinación con Inventario, CDI de contratos
públicos, UI y composición física.
