# Commercial Catalog API

Contrato empresarial público y Java puro de `commercial_catalog`.

- Versión: `1.0.0`.
- Dependencia permitida: `kernel-api` sólo para `CompanyId`.
- Prohibido: Jakarta, JPA, JDBC, Hibernate, JSF e internos del plugin.

## Contratos por propósito

- referencia y búsqueda: `CatalogItemDirectory`;
- conversión: `CatalogUnitConversions`;
- cotización explícita: `CatalogPricing`;
- identidades opacas: `CatalogItemId`, `PriceListId` y `PriceEntryId`;
- proyecciones inmutables: `CatalogItemReference`, `CatalogUnitConversionResult`
  y `CatalogPriceQuote`.

Los consumidores guardan sus snapshots históricos y nunca dependen de entidades o
tablas privadas del catálogo.

## Prueba

```powershell
.\mvnw.cmd -B -pl plugins/commercial-catalog-api -am test
```
