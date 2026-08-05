# Contrato del plugin `reference_data`

- Plugin: `reference_data@1.0.0`
- Contrato público: `reference-data-api@1.0.0`
- Compatibilidad Plugin API: `[0.4.0,0.5.0)`
- ADR: `docs/adr/0038-plugin-datos-referencia-normativos.md`
- Historia: `J11-S8-C03`
- Estado: primer subconjunto implementado; publicación completa y RD-06 pendientes

## Propiedad y contrato público

El plugin posee identidad normativa, publicaciones y habilitación por empresa. No
posee tipos de identificación empresariales, tasas de cambio, catálogos fiscales
SIFEN ni traducciones arbitrarias.

`ReferenceDataDirectory` recibe siempre `CompanyId` y expone:

- publicación corriente por `COUNTRY` o `CURRENCY`;
- países con códigos alpha-2, alpha-3 y numérico;
- monedas con códigos alfabético/numérico y unidad menor;
- búsqueda exacta por código estable.

La API es Java pura salvo el tipo neutral `CompanyId`; no publica entidades,
repositorios, SQL ni adaptadores.

## Descriptor

- tipo `FUNCTIONAL`, versión `1.0.0`;
- sin dependencias funcionales;
- capacidades `directory` y `provenance`;
- permiso `reference_data.view`;
- menú **Datos de referencia**, ruta `/reference-data`;
- pantalla neutral `reference_data:catalogs@1.0.0`;
- migraciones `classpath:db/migration/reference_data` bajo
  `plg_reference_data`.

## Persistencia V1

| Tabla | Propósito |
|---|---|
| `catalog_release` | publicación, autoridad, fuente, hash, completitud y corriente |
| `country_entry` | códigos y nombre del país por publicación |
| `currency_entry` | códigos, nombre y unidad menor por publicación |
| `company_country_policy` | habilitación de país por empresa |
| `company_currency_policy` | habilitación de moneda por empresa |

Sólo existe una publicación corriente por clase. Las migraciones aplicadas son
inmutables; cualquier edición o publicación nueva usa V2 o superior. No hay FK,
JPA ni SQL hacia `core` o esquemas de otros plugins.

## Seguridad e interfaz

La pantalla revalida empresa, activación y `reference_data.view` en servidor. Es
de sólo lectura, sin acciones de alta. Muestra el alcance
`BOOTSTRAP_SUBSET`, release y SHA-256. El shell es dueño de XHTML, Material Design
3, responsive y textos.

Los consumidores validan sus propios permisos y vuelven a consultar
`ReferenceDataDirectory` dentro de la transacción antes de persistir. La
habilitación no se deduce de un valor enviado por el navegador.

## Evolución pendiente

- importador determinista de publicaciones completas conservadas en `.tools/`;
- reconciliación de altas/cambios/retiros y conservación histórica;
- casos de uso autorizados y auditados para políticas por empresa;
- búsqueda/paginación para listas grandes;
- política de traducciones separada de la identidad normativa;
- Docker/Compose, Playwright y recongelación documental del corte.
