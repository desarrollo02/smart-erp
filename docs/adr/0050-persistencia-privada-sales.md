# ADR-0050 — Persistencia privada de `sales`

- Estado: Aceptado
- Fecha: 2026-08-20

## Decisión

`sales` posee el esquema `plg_sales` y lo evoluciona exclusivamente mediante
Flyway. La V1 contiene ocho tablas: condiciones comerciales, cotizaciones y sus
líneas, pedidos y sus líneas, reservas públicas de inventario, operaciones
idempotentes e historia de transiciones.

Cliente, moneda, condición, artículo, unidad, precio e impuesto se almacenan como
snapshots. Los UUID externos son referencias opacas: no existen claves foráneas
ni relaciones JPA hacia esquemas de otros plugins. Cotización, pedido y condición
usan `@Version`; los repositorios siempre reciben `CompanyId` y no ofrecen borrado
físico. Una cotización sólo puede originar un pedido por empresa y cada línea de
stock confirmada conserva el identificador público de su reserva.

La historia y el ledger idempotente son append-only mediante triggers. La futura
capa de aplicación será responsable de grabarlos en la misma transacción JTA que
la transición, sin introducir esa coordinación en el dominio ni en esta historia.

## Consecuencias

- Desactivar una condición no modifica snapshots históricos.
- Cancelar conserva las referencias de reserva; la compensación se coordinará con
  `InventoryReservations` en J11-S11-04.
- Hibernate valida el esquema y nunca lo actualiza automáticamente.
- El gate PostgreSQL/Testcontainers es obligatorio antes de cerrar J11-S11-03.
