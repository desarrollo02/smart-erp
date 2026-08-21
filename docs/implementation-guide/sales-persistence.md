# Preparación de persistencia de Ventas

La distribución que incorpore `sales` debe registrar su descriptor y permitir al
migrador crear `plg_sales` desde la ubicación
`classpath:db/migration/sales`. No se crean tablas manualmente y no se habilita
actualización automática de Hibernate.

La empresa se conserva en todas las claves operativas. Identificadores de
clientes, artículos, monedas y reservas deben resolverse por APIs públicas; no se
otorgan permisos de lectura sobre esquemas privados de otros plugins. Las
credenciales y la configuración PostgreSQL continúan fuera de la imagen.

Antes de promover una composición con Ventas deben estar verdes la migración
limpia y repetida, `validate`, aislamiento entre empresas, versión optimista e
inmutabilidad de ledger e historia.

La composición debe aportar `inventory` 1.3.0 o superior. Ventas usa
`CatalogStockReservationRequest` y `InventoryReservations.find`; no necesita
permisos SQL ni acceso JPA a `plg_inventory`. Deben asignarse explícitamente los
once permisos de `sales`.
