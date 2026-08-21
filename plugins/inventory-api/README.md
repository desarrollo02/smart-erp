# Inventory API

Contrato público Java puro `1.3.0` del plugin `inventory`.

Publica identidades opacas, disponibilidad por clave exacta, snapshot de
conversión, movimientos y reservas. Solo depende de `kernel-api` para `CompanyId`;
no expone Jakarta, JPA, tablas, entidades, repositorios ni clases privadas del
plugin o de `commercial_catalog`.

Las cantidades admiten hasta 6 decimales y los factores de conversión hasta 12.
Cada consumidor debe pasar una empresa obtenida de un contexto confiable; el
contrato no concede autorización por sí mismo.

Desde 1.1, `CatalogStockMovementRequest` permite que Compras publique una entrada
o salida usando la identidad pública de catálogo. Inventario resuelve su identidad
local internamente y exige el permiso acotado
`inventory.movements.purchase.post`.

Desde 1.3, `CatalogStockReservationRequest` permite que Ventas reserve usando
`CatalogItemId` y la clave física. Inventario resuelve su identidad privada.
`InventoryReservations.find` entrega sólo la proyección pública necesaria para
liberar remanentes de forma versionada.

La misma versión publica `InventoryStorageDirectory`, `WarehouseReference` y
`StockLocationReference` para que un selector autorizado busque o recupere un
depósito exacto con sus ubicaciones. Este contrato no concede acceso a tablas ni
omite la revalidación de empresa y `inventory.view`.
