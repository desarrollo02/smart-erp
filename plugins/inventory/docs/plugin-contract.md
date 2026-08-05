# Contrato del plugin `inventory`

- Plugin: `inventory@1.0.0`
- Contrato público: `inventory-api@1.0.0`
- Plugin API compatible: `[0.4.0,0.5.0)`
- Dependencia requerida: `commercial_catalog@[1.0.0,2.0.0)`
- Estado: dominio, persistencia, aplicación e interfaz neutral verdes; composición pendiente

## Propiedad

`inventory` es dueño de depósitos, ubicaciones, inscripción inventariable,
políticas de trazabilidad, saldos, movimientos, reservas y conteos. No es dueño de
la identidad comercial del producto, compras, ventas, documentos, SIFEN, costos,
valoración o contabilidad.

## API pública

| Contrato | Propósito |
|---|---|
| `InventoryAvailability` | consultar físico, reservado y disponible de una clave exacta |
| `InventoryMovements` | contabilizar un movimiento idempotente con fuente y motivo |
| `InventoryReservations` | reservar, consumir, liberar o expirar una cantidad con versión esperada |
| `StockKey` | ítem local, depósito, ubicación, trazabilidad y condición |
| `MovementQuantity` | snapshot reproducible de unidad, factor y versión de catálogo |

El API no publica eventos en esta versión. Un evento se agregará únicamente cuando
exista un consumidor real y se versionará conforme a ADR-0013.

## Invariantes

- depósito y ubicación siempre están presentes; cada depósito crea `GENERAL`;
- solo un `CatalogItemReference` activo de tipo `PRODUCT` puede inscribirse;
- lote, serie y vencimiento respetan la política del ítem;
- una serie mueve exactamente una unidad base;
- físico, reservado y disponible nunca son negativos;
- disponible siempre es físico menos reservado;
- una transferencia conserva ítem, trazabilidad, condición, unidad y cantidad
  entre ubicaciones distintas;
- un movimiento contabilizado no se modifica ni se elimina;
- una reserva no consume o libera más que su remanente;
- cerrar un conteo genera diferencias para ajustes y no sobrescribe el saldo.

## Fronteras vigentes

El módulo puede importar `commercial-catalog-api`, pero nunca paquetes de dominio,
aplicación o infraestructura del catálogo. No existen relaciones JPA o SQL entre
plugins. El descriptor publica tres capacidades (`availability`, `movements` y
`reservations`), siete permisos, tres menús, tres pantallas y las migraciones V1–V2
de `plg_inventory`; no publica overlays. Diez tablas privadas, diez entidades y
siete repositorios preservan empresa, snapshots, idempotencia y concurrencia sin
depender de tablas o entidades JPA de catálogo.

## Contribuciones visuales

| Orden | Menú | Pantalla | Ruta | Permiso |
|---:|---|---|---|---|
| 1 | Existencias | `inventory:stock` | `/inventory` | `inventory.view` |
| 2 | Depósitos | `inventory:warehouses` | `/inventory/warehouses` | `inventory.view` |
| 3 | Conteos | `inventory:counts` | `/inventory/counts` | `inventory.view` |

Los contratos describen regiones, campos y acciones neutrales. Los handlers
revalidan empresa y permiso en el servidor y el shell es el único dueño del XHTML,
tema y renderer. La composición física y la demo navegable se agregan en J11-S8-06.

Los adaptadores CDI de los tres contratos públicos usan la empresa actual y el
permiso exacto; no aceptan sustituir `CompanyId`. La fachada de aplicación delimita
las mutaciones con JTA y marca rollback si un resultado funcional es fallido. Las
operaciones de reserva conservan un recibo inmutable por clave idempotente y los
movimientos físicos permanecen append-only.
