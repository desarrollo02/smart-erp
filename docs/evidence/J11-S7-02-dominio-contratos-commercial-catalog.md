# Evidencia J11-S7-02 - Dominio y contratos de `commercial_catalog`

- Fecha: 2026-07-30
- Estado: Verde
- Historia: [J11-S7-02](../sprints/sprint-07/J11-S7-02-dominio-contratos-commercial-catalog.md)
- ADR: [ADR-0019](../adr/0019-modelo-catalogo-comercial-y-contratos-publicos.md)

## Resultado

Se crearon dos módulos Maven físicos. `commercial-catalog-api` publica el contrato
Java puro `1.0.0`; `commercial-catalog` contiene el descriptor CDI/SPI y dominio
privado. Es otro plugin funcional y no una ampliación interna de
`business_partners`.

La API separa directorio/búsqueda, conversión de unidades y cotización. El dominio
materializa ítem común para producto/servicio, alcances, identificadores, unidades,
clasificación, perfil tributario interno, variante, ciclo de vida, reemplazo,
listas y entradas de precio con control optimista.

## Pruebas incrementales

| Corte | Comando | Resultado |
|---|---|---|
| API pública | `.\mvnw.cmd -B -pl plugins/commercial-catalog-api -am test` | 9 propias, verdes |
| descriptor CDI/SPI | test dirigido `CommercialCatalogPluginDefinitionTest` | 2 verdes |
| agregado de ítem | test dirigido `CatalogItemTest` | 5 verdes |
| listas y precios | test dirigido `PriceListTest` | 4 verdes |
| módulos completos | `.\mvnw.cmd -B -pl plugins/commercial-catalog -am test` | API 9 + plugin 11; 49 acumuladas con dependencias, verdes |
| límites | test dirigido `ModuleBoundariesArchitectureTest` | 15 reglas verdes sobre 18 módulos del alcance |
| reactor | `.\mvnw.cmd -B verify` | 22/22 módulos y 266 pruebas verdes |

Resultado consolidado de Surefire:

```text
REPORTS=69
TESTS=266
FAILURES=0
ERRORS=0
SKIPPED=0
```

No se ejecutaron PostgreSQL/Testcontainers, Docker/Compose o Playwright: J11-S7-02
no agrega SQL, JPA, composición física, endpoints ni UI. Esos gates corresponden
a J11-S7-03 y J11-S7-05 a S7-07.

## Límites arquitectónicos

ArchUnit comprueba que:

- `commercial-catalog-api` solo usa Java, su paquete y `CompanyId`;
- API y dominio no dependen de Jakarta, `javax`, Hibernate, JDBC, PostgreSQL o
  PrimeFaces;
- el catálogo no alcanza clases de la implementación de `business_partners`;
- kernel y shell no dependen de implementaciones de plugins.

## Inspección de artefactos

```text
API_ENTRIES=27
API_JAKARTA=0
API_INTERNAL_DOMAIN=0
PLUGIN_ENTRIES=39
SPI=1
BEANS=1
MIGRATIONS=0
BASE_WAR_COMMERCIAL_CATALOG=0
```

El JAR público no filtra Jakarta ni clases internas. El JAR funcional registra una
definición SPI y un archivo `beans.xml`; no inventa migraciones. El WAR base
continúa sin los dos JAR porque la selección física pertenece a J11-S7-06.

## Cobertura de comportamiento

- UUID opacos, contrato semántico `1.0.0` y referencias defensivas;
- búsqueda paginada, conversión y cotización acotadas por empresa;
- producto/servicio en un agregado con tipo inmutable y alcances independientes;
- código e identificadores normalizados sin usarlos como PK;
- factor base implícito, conversiones positivas y default único por finalidad;
- categoría principal/secundarias, marca, etiquetas y variante explícita;
- perfil tributario interno versionado sin SIFEN;
- inactivación/reemplazo sin baja física y rechazo de versión obsoleta;
- lista con moneda ISO, modo tributario, escala y redondeo fijos;
- tramos de cantidad, vigencias adyacentes y rechazo de solapamiento ambiguo;
- `BigDecimal` y cotización determinista con snapshot suficiente.

## Incidencias y correcciones

La primera invocación dirigida del descriptor no citó
`-Dsurefire.failIfNoSpecifiedTests=false`; PowerShell entregó el fragmento posterior
al punto como una fase Maven. No se ejecutó ninguna prueba ni falló código. Se
repitió con ambas propiedades citadas y las 2 pruebas quedaron verdes.

El primer G0 final encontró el enlace a esta ficha desde el índice antes de que la
ficha existiera. Se creó la evidencia con los resultados reales y se repitió G0.
No se omitió, relajó o desactivó ninguna prueba.

El G0 repetido sobre documentación y ambos módulos produjo:

```text
MARKDOWN_FILES=180
BAD_FILES=0
LOCAL_LINKS=677
BROKEN_LINKS=0
```

## Archivos principales

- `plugins/commercial-catalog-api/`;
- `plugins/commercial-catalog/`;
- `pom.xml`;
- `tests/architecture-tests/`;
- ADR-0019, historia, arquitectura, guía e índices documentales.

## Conclusión

J11-S7-02 queda técnicamente verde sin adelantar persistencia, aplicación o UI.
J11-S7-03 puede diseñar el esquema privado y los repositorios a partir de este
dominio aceptado.
