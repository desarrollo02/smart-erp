# ADR-0051 — Aplicación JTA, seguridad y reservas de `sales`

- Estado: Aceptado
- Fecha: 2026-08-21

## Decisión

La aplicación de `sales` revalida empresa, plugin y permiso antes de consultar
referencias o generar identidades. Cada mutación usa clave idempotente, huella
SHA-256, ledger durable, auditoría técnica y, para transiciones, historia
append-only. La infraestructura CDI delimita una transacción JTA y marca rollback
cuando el resultado funcional no es exitoso.

Confirmar un pedido reserva exactamente todas sus líneas inventariables. Ventas
envía `CatalogItemId`, depósito, ubicación y trazabilidad mediante
`CatalogStockReservationRequest`; Inventario resuelve su `InventoryItemId`
privado. `inventory-api` avanza de 1.2.0 a 1.3.0 de forma compatible y publica la
consulta mínima necesaria para liberar el remanente al cancelar. `sales` exige
Inventario 1.3.0.

Mientras Socios Comerciales no publique una identificación fiscal seleccionable,
el límite de Ventas recibe el identificador que se congelará y revalida por
separado que el socio esté activo y tenga rol `CLIENT`. J11-S11-05 deberá
presentarlo y validarlo explícitamente.

## Consecuencias

- no se filtran identidades privadas ni existen JPA/SQL cruzados;
- un fallo de reserva produce `INVENTORY_FAILURE` y rollback de la unidad;
- un reintento idéntico devuelve el agregado sin repetir efectos;
- reutilizar una clave con otra huella se rechaza;
- cerrar no significa facturar, cobrar, despachar o entregar;
- la prueba runtime JTA compuesta pertenece a J11-S11-06.
