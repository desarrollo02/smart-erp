# Sprint 11 - Ventas `sales`

- Estado: En curso; aplicación y reservas validadas automáticamente, interfaz pendiente
- Fecha: 2026-08-20
- Dependencia: Sprint 10 con gates automáticos y decisión de instalador `NO`
- Pendiente heredado: validación independiente acumulada
- Orden: ERP 5
- Épica: [Ventas](../../backlog/epica-ventas.md)

## Objetivo

Implementar presupuestos, pedidos y compromisos con precios históricos y reserva
de stock, usando APIs públicas y floorplans 2.0, sin adelantar documentos,
finanzas o logística.

## Decisiones de producto

Producto confirmó presupuesto/pedido V1, pedido directo o derivado, reserva al
confirmar, exclusión de factura/remisión/pagos/cuentas por cobrar/entrega/POS,
consulta local de sólo lectura y SA-D01 a SA-D10 sin cambios.

## Orden

| Orden | Historia | Estado |
|---:|---|---|
| 1 | [J11-S11-00](J11-S11-00-gobierno-planificacion.md) | Completada documentalmente |
| 2 | [J11-S11-01](J11-S11-01-caracterizacion-sales.md) | Completada documentalmente |
| 3 | J11-S11-02 API/dominio | Implementada y validada automáticamente; validación independiente pendiente |
| 4 | [J11-S11-03](J11-S11-03-persistencia-sales.md) | Implementada y validada automáticamente; validación independiente pendiente |
| 5 | [J11-S11-04](J11-S11-04-aplicacion-seguridad-sales.md) | Implementada y validada automáticamente; validación independiente pendiente |
| 6 | J11-S11-05 interfaz | Pendiente |
| 7 | J11-S11-06 composición | Pendiente |
| 8 | J11-S11-07 validación/demo | Pendiente |
| 9 | J11-S11-08 instalador | Pendiente |

## Rutas candidatas

| Ruta | Floorplan |
|---|---|
| `/sales/quotes` | `TRANSACTION_EDITOR` |
| `/sales/orders` | `TRANSACTION_EDITOR` |
| `/sales/commitments` | `INQUIRY` |
| `/sales/terms` | `MASTER_DATA` |

Todavía no existen en runtime; se formalizarán en J11-S11-05.

## Gates y límites

Se exigirán módulo/reactor/ArchUnit, PostgreSQL, migraciones, JPA/JTA,
concurrencia, composición presente/ausente, Compose/health/OIDC, seguridad
negativa, Playwright 375/720/1280, demo, manuales, fotografía, PDF y decisión de
instalador. No se copiará legado ni `javax.*`, no habrá JPA/SQL cruzado y no se
modelará factura, cobro o entrega como estado del pedido.

## Siguiente trabajo autorizado

J11-S11-04 completó en verde sus gates de módulo, PostgreSQL, arquitectura y
reactor. J11-S11-05 queda habilitada como siguiente historia.
