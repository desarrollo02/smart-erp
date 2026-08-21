# J11-S11-02 — Dominio neutral y contratos públicos de `sales`

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Fecha: 2026-08-20
- Decisión: [ADR-0049](../../adr/0049-modelo-sales-y-contratos-publicos.md)

## Resultado

Se registraron `sales-api` y `sales` en el reactor. La API `1.0.0` publica identidades, estados, referencias y búsquedas empresariales sin Jakarta. El dominio implementa cotización y pedido separados, pedido directo o derivado, snapshots históricos, excepción de precio motivada, versión optimista y reservas de inventario exactas al confirmar.

El descriptor declara cuatro dependencias funcionales públicas requeridas y, deliberadamente, todavía no aporta migraciones, permisos, menú o UI.

## Evidencia automatizada

La materialización aislada `.tools/tmp/validation/J11-S11-02-minimal` ejecutó:

```powershell
.\mvnw.cmd -f .tools\tmp\validation\J11-S11-02-minimal\pom.xml -pl plugins/sales -am test
```

Resultado: reactor de 9 módulos verde; 4 pruebas propias de `sales` y 1 de
`sales-api` verdes, además de sus dependencias. El corte final también ejecutó
`-pl tests/architecture-tests -am test`: 26 módulos verdes y 36 pruebas de
arquitectura verdes. Finalmente, `mvn verify` completó los 30 módulos del reactor
con `BUILD SUCCESS` en 4 min 17 s. La validación independiente continúa pendiente.

## Fuera de alcance

Flyway/JPA, aplicación JTA, permisos, auditoría durable, UI, composición física y Playwright pertenecen a las siguientes historias.
