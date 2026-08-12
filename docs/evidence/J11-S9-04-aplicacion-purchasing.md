# Evidencia J11-S9-04 — Aplicación de `purchasing`

- Fecha: 2026-08-11
- Rama local: `sprint/09-purchasing`
- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Pruebas ejecutadas: módulo, PostgreSQL/Testcontainers, ArchUnit y `mvn verify`

## Evidencia estática

- `purchasing-api` e `inventory-api` declaran versión `1.1.0`.
- `CatalogStockMovementRequest` mantiene privada la identidad local de Inventario.
- `PurchasingPermissions` declara doce permisos y el descriptor los publica.
- `TransactionalPurchasingUseCases` concentra mutaciones JTA y marca rollback ante
  cualquier resultado fallido.
- `CdiPurchasingContracts` revalida empresa y permiso en las APIs públicas.
- `PurchasingRequestService`, `PurchasingOrderService` y
  `PurchasingFulfillmentService` aplican versión esperada, auditoría e
  idempotencia.
- `PurchasingImportService` deduplica por empresa y procedencia.
- V2 agrega `purchasing_operation` y `purchasing_import` sin claves foráneas hacia
  otros esquemas.
- V1 conserva unidad presentada/base, factor, lote, serie, vencimiento y condición.
- Se agregaron y ejecutaron pruebas de contrato, autorización previa, reintento
  exacto, conflicto, resolución de catálogo y migración V2.

## Revisión estática ejecutada

- inspección de referencias residuales a `unit_code_snapshot`;
- inspección de versiones y cantidades de permisos/entidades;
- inspección estructural de firmas repetidas en fuentes y pruebas;
- `git diff --check` sin errores reportados;
- revisión manual de SQL, persistencia y fronteras públicas.

Estas comprobaciones estáticas se complementaron con los gates automatizados.

## Gates automatizados y pendientes reales

- 19 unitarias y 6 integraciones PostgreSQL de Compras: verdes;
- 32 ArchUnit: verdes;
- `mvn verify`, 28 módulos: verde;
- JTA runtime, seguridad de despliegue y health: se ejecutan en J11-S9-06 cuando
  Compras forme parte del WAR/migrador;
- validación independiente por otra persona: pendiente.
