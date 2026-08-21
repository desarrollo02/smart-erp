# Persistencia de Ventas

`plugins/sales` usa la unidad JPA `logixone-sales-pu` y el esquema privado
`plg_sales`. La fuente canónica del esquema es
`db/migration/sales/V1__initialize_sales_schema.sql`; Hibernate opera con
`hbm2ddl.auto=validate`.

Los repositorios de condiciones, cotizaciones y pedidos están definidos como
puertos de aplicación y requieren `CompanyId` en cada búsqueda. Las entidades
guardan UUID externos y snapshots, nunca relaciones JPA ni SQL hacia tablas de
Socios, Catálogo, Referencias o Inventario. Los agregados usan versión optimista y
no existe operación de borrado físico.

La prueba normal compila el IT PostgreSQL. Para ejecutarlo se requiere Docker y:

```powershell
.\mvnw.cmd -pl plugins/sales -am verify -Dlogixone.postgres.integration=true
```

El IT migra dos veces, valida Flyway/JPA y reconstruye condiciones, cotizaciones,
pedidos y reservas con aislamiento empresarial.

## Aplicación y transacciones

`SalesUseCases` mantiene dominio y aplicación independientes de CDI.
`TransactionalSalesUseCases` aporta la frontera JTA y marca rollback ante todo
resultado fallido. Ledger, historia, pedido y reservas participan en la misma
unidad lógica cuando la composición incorpora ambos plugins.

`inventory-api@1.3.0` permite reservar por `CatalogItemId`; nunca se entrega a
Ventas el `InventoryItemId` privado. La prueba runtime de atomicidad se ejecutará
al componer `sales` en J11-S11-06.
