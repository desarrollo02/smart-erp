# ADR-0043 — Aplicación JTA e idempotencia de `purchasing`

- Estado: Aceptado
- Fecha: 2026-08-11
- Historia: [J11-S9-04](../sprints/sprint-09/J11-S9-04-aplicacion-purchasing.md)
- Estado de validación: implementada pendiente de pruebas

## Contexto

El dominio y la persistencia de Compras ya separaban solicitud, orden, recepción
y devolución, pero todavía faltaban autorización, auditoría, demarcación JTA,
reintentos e integración real con Inventario. La frontera pública existente de
Inventario recibía `InventoryItemId`, una identidad privada que Compras no puede
resolver sin romper el límite entre plugins. Además, el snapshot de línea de
Compras conservaba una sola unidad y las líneas físicas no conservaban lote,
serie, vencimiento o condición.

La importación controlada también necesitaba deduplicar por empresa y procedencia.
En solicitudes importadas, un precio esperado no era reproducible sin declarar
la moneda correspondiente.

## Decisión

### Contrato público de Inventario 1.1

`inventory-api@1.1.0` agrega `CatalogStockMovementRequest` e
`InventoryPurchaseMovements.postCatalogItem`. El consumidor entrega la identidad pública
de catálogo, destino/origen, trazabilidad, condición y snapshot de conversión.
Inventario resuelve internamente su `InventoryItemId`, valida inscripción,
políticas, aritmética del snapshot, unidad base, saldo e idempotencia, y publica un
movimiento de una línea. Una versión histórica sigue siendo válida si la unidad
base inscripta no cambió; un cambio real de unidad base se rechaza para no mezclar
significados dentro del saldo.

La operación exige `inventory.movements.purchase.post`. No concede el permiso
general de movimientos manuales. El permiso del actor se revalida en el adaptador
CDI de Inventario y la empresa solicitada debe coincidir con el contexto actual.

### Snapshot reproducible de Compras

`PurchasedItemSnapshot` conserva unidad presentada, unidad base, factor con hasta
12 decimales y versión de catálogo. Las líneas de recepción y devolución de stock
conservan depósito, ubicación, lote, serie, vencimiento y condición. V1 se ajusta
antes de su primera validación/aplicación para reflejar estos datos; no se reescribe
una migración ya promovida.

`purchasing-api@1.1.0` agrega moneda esperada opcional a la importación de
solicitudes. Su presencia es obligatoria exactamente cuando alguna línea contiene
precio esperado.

### Aplicación, permisos y auditoría

El descriptor publica doce permisos:

- consulta;
- creación, envío y aprobación de solicitudes;
- creación, emisión y cierre de órdenes;
- creación y confirmación de recepciones;
- creación y confirmación de devoluciones;
- ejecución de importaciones.

Los servicios de aplicación revalidan el permiso antes de leer estado empresarial,
resuelven proveedor, moneda, ítem y conversión mediante APIs públicas, aplican
versión esperada y registran cada resultado en `TechnicalAudit`. Los adaptadores
públicos vuelven a comprobar empresa, plugin activo y permiso mediante el kernel.

### Atomicidad de recepción y devolución

`TransactionalPurchasingUseCases` es la frontera JTA. Al confirmar una recepción o
devolución:

1. carga documento y orden dentro de la empresa autorizada;
2. publica un movimiento idempotente por cada línea `STOCK` mediante
   `inventory-api`;
3. actualiza cantidades acumuladas de la orden;
4. confirma el documento con las identidades de movimiento;
5. registra recibo de idempotencia y auditoría.

Un resultado fallido marca rollback. Las líneas `NON_STOCK` y `SERVICE` no llaman
Inventario. Una devolución hereda ubicación y trazabilidad de la recepción
confirmada; no acepta un bucket físico libre diferente.

### Ledgers V2

V2 agrega dos tablas append-only privadas:

- `purchasing_operation`, por empresa y clave idempotente, con tipo de operación,
  SHA-256 del comando, recurso y versión resultante;
- `purchasing_import`, por empresa, sistema y registro de origen, con checksum de
  lote opcional, SHA-256, tipo e identidad del documento.

Un reintento exacto devuelve el recurso existente sin repetir la mutación. Reusar
la clave o procedencia con otra huella produce conflicto. El adaptador de
`legacy_migration` sólo consume `PurchasingImports`; nunca escribe estas tablas.

## Consecuencias

- Compras no importa dominio, JPA, repositorios ni SQL de Inventario.
- El descriptor de Compras exige `inventory@[1.1.0,2.0.0)`; los demás predecesores
  permanecen compatibles desde 1.0.
- El rol receptor necesitará, además de `purchasing.receipts.confirm`, el permiso
  acotado `inventory.movements.purchase.post`; la composición de roles corresponde
  a J11-S9-06.
- No se agrega outbox porque aún no existe un consumidor aprobado.
- Las pantallas y sus menús permanecen fuera de esta historia y corresponden a
  J11-S9-05.
- Las pruebas se escriben, pero no se ejecutan hasta el gate acumulado autorizado;
  este corte no es verde ni comercializable.

## Alternativas descartadas

- resolver `InventoryItemId` leyendo `plg_inventory`;
- publicar la entidad o repositorio privado de Inventario;
- usar el permiso amplio de movimiento manual para recepciones de compra;
- recalcular unidades históricas sólo con el catálogo vigente;
- confirmar Compras y luego compensar Inventario fuera de JTA;
- deduplicar importaciones únicamente por número comercial;
- introducir eventos sin consumidor real.

## Referencias

- [ADR-0041 — Modelo de `purchasing`](0041-modelo-purchasing-y-contratos-publicos.md)
- [ADR-0042 — Persistencia privada de `purchasing`](0042-persistencia-privada-purchasing.md)
- [ADR-0023 — Modelo de `inventory`](0023-modelo-inventory-y-contratos-publicos.md)
- [Épica de Compras](../backlog/epica-compras.md)
