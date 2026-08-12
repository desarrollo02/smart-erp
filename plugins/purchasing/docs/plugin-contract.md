# Contrato del plugin `purchasing`

- Plugin: `purchasing@1.1.0`
- API pública: `purchasing-api@1.1.0`
- Plugin API compatible: `[0.4.0,0.5.0)`
- Dependencias: `business_partners@[1.1.0,2.0.0)`,
  `commercial_catalog@[1.1.0,2.0.0)`, `inventory@[1.1.0,2.0.0)` y
  `reference_data@[1.0.0,2.0.0)`
- Estado: aplicación e interfaz implementadas pendientes de pruebas; composición ausente

## Propiedad

Compras posee solicitudes, órdenes, recepciones, devoluciones, asignaciones y sus
ledgers de idempotencia/importación. No posee factura, deuda, pago, retención,
costo, valoración, asiento ni tablas maestras de otros plugins.

## API pública

| Contrato | Propósito |
|---|---|
| `PurchasingDirectory` | localizar una solicitud u orden por identidad opaca y empresa |
| `PurchasingImports` | importar de forma controlada e idempotente una solicitud u orden abierta |

La importación deduplica por empresa, sistema y registro de origen. Un adaptador
de legado nunca escribe `plg_purchasing`.

## Aplicación

Doce permisos separan consulta, solicitud, aprobación, orden, recepción,
devolución e importación. `TransactionalPurchasingUseCases` delimita JTA y marca
rollback ante cualquier resultado fallido. Cada mutación registra huella SHA-256,
recurso, versión y auditoría técnica.

Una confirmación `STOCK` usa `InventoryPurchaseMovements` y genera un movimiento
por línea. Compras entrega `CatalogItemId`; Inventario conserva y resuelve su
identidad local. Servicio y no-stock no alteran saldos.

## Persistencia

`plg_purchasing` contiene nueve tablas V1 para los cuatro agregados y dos tablas
V2 para operaciones e importaciones. No existen FKs o relaciones JPA hacia otros
esquemas. Las referencias externas son identidades públicas y snapshots.

## Interfaz

Cinco `ScreenDefinition` neutrales exponen las rutas `/purchasing/requests`,
`/purchasing/orders`, `/purchasing/receipts`, `/purchasing/returns` y
`/purchasing/tracking`. Sus handlers usan directorios paginados privados y APIs
públicas de socios, catálogo, referencia e inventario. Cada `SELECT` declara
fuente, propietario, administración, permiso, vacío, inactivos y carga.
El catálogo filtra el alcance `PURCHASE` en servidor antes de calcular total y
página.

El shell es el único dueño de la presentación Jakarta Faces y registra las cinco
especificaciones. El plugin no aporta XHTML, CSS, JavaScript ni EL arbitraria.

## Límites de este corte

No hay overlays, outbox ni composición física. La UI implementada todavía no es
navegable desde la distribución oficial hasta J11-S9-06. Las pruebas permanecen
diferidas hasta el gate de candidata comercializable; el corte no es verde.
