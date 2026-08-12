# Commercial catalog

- Artifact Maven: `commercial-catalog`
- Plugin ID: `commercial_catalog`
- Tipo: `FUNCTIONAL`
- Versión actual: `1.1.0` (versión inicial `1.0.0`)
- Compatibilidad Plugin API: `[0.4.0,0.5.0)`

Este es un plugin funcional distinto de `business_partners`. El módulo
`commercial-catalog-api` contiene su contrato público Java puro; este JAR contiene
el dominio y la persistencia privada del catálogo.

`J11-S7-03` aporta la migración Flyway V1, veinte tablas bajo
`plg_commercial_catalog`, una unidad JPA con DDL deshabilitado y repositorios de
ítems, listas y secuencias. J11-S7-04 a J11-S7-06 agregaron aplicación,
autorización, auditoría, artículos, listas y composición. J11-S8-C01 agrega el
maestro visual de perfiles tributarios bajo
`commercial_catalog.definitions.manage`; no agrega tasas oficiales ni códigos
SIFEN al catálogo. El sexto corte de J11-S8-C02 agrega inactivación y reactivación
visual de unidades, categorías, marcas y etiquetas con versión optimista,
aislamiento empresarial y auditoría. El decimosexto corte agrega V2 con cuatro
tablas de revisión, backfill e historial append-only; permite revisar nombre,
escala de unidad o padre de categoría sin cambiar código ni identidad. V3 agrega
reemplazo seguro de esas definiciones y V4 conserva revisiones estructurales de
familias y la versión exacta de cada asignación. El decimonoveno corte expone la
asignación neutral desde Artículos y servicios, acepta sólo familias activas de la
empresa y revalida revisión, atributos obligatorios y tipos dentro de la
transacción. El borrado físico continúa fuera del alcance.

J11-S8-C03 conecta la moneda de cada lista a `reference-data-api`, revalida el
código dentro de la transacción y declara `reference_data` 1.x como dependencia
funcional requerida. No consulta su esquema privado.

J11-S9-05 eleva el contrato público a `1.1.0`: `CatalogSearchCriteria` admite
alcances comerciales y JPA aplica ese filtro antes del total y la paginación. El
selector de Compras puede solicitar únicamente artículos `PURCHASE` sin cargar ni
descartar páginas del lado consumidor.

## Prueba local

```powershell
.\mvnw.cmd -B -pl plugins/commercial-catalog -am test
.\mvnw.cmd -B -pl plugins/commercial-catalog -am verify `
  -Dlogixone.postgres.integration=true
```
