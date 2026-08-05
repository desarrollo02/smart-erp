# Evidencia J11-S8-C03 — Datos de referencia normativos

- Fecha: 2026-08-04
- Estado: corte implementado y validado; publicación completa y recongelación pendientes
- Historia: [J11-S8-C03](../sprints/sprint-08/J11-S8-C03-datos-referencia-normativos.md)

## Fuentes conservadas fuera de Git

| Fuente | Bytes | SHA-256 |
|---|---:|---|
| SIX ISO 4217 List One, observada 2026-08-04 | 47.463 | `838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9` |
| UN M49 overview, observada 2026-08-04 | 1.721.568 | `748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11` |

Los originales están en `.tools/downloads/reference-data/`; no se versionan. La
V1 conserva metadatos y únicamente el subconjunto caracterizado `PY/PYG/USD`.

## Pruebas ejecutadas

| Alcance | Resultado |
|---|---|
| `reference-data-api` y `reference-data` | 3 pruebas de API y 5 de plugin verdes |
| PostgreSQL 18.4 | 2 escenarios verdes; V1 aplicada una vez e idempotencia verificada |
| `business_partners` | 52 pruebas verdes; país inválido rechazado en servidor |
| `commercial_catalog` | 82 pruebas verdes; moneda inválida rechazada en servidor |
| shell | 56 pruebas verdes, incluido renderer y comportamiento responsive de `/reference-data` |
| arquitectura | 28 pruebas verdes; API pura, orden de composición y ausencia de internos cruzados |
| composición ausente | WAR y migrador sin implementaciones de plugins |
| composición `with-inventory-demo` | WAR contiene `reference-data`/API y migrador contiene clase/migración |
| reactor sin perfil | 26 módulos verdes con `mvnw.cmd verify` |
| reactor `with-inventory-demo` | 26/26 módulos verdes en 2 min 27 s; repetido dentro del builder final de aplicación |
| Dockerfile | `buildx --check` verde para aplicación y migrador, sin advertencias |
| migración Compose | V1 aplicada una vez; segunda ejecución sin migraciones pendientes para ningún propietario |
| health REST | 2/2 verdes; liveness, readiness, catálogo, configuración, base, migraciones y OIDC en `UP` |
| E2E Playwright | `BusinessPartnersVisualIT` 1/1 verde en 59,26 s; 0 fallos, errores u omitidas |
| documentación | 281 Markdown UTF-8, 1.356 enlaces locales y 0 hallazgos; SVG válido y revisado visualmente |

La primera prueba nueva del renderer falló durante desarrollo por usar un getter
inexistente y, tras corregirlo, expuso que faltaban las secciones vacías del nuevo
tipo de pantalla. Se corrigieron ambas causas y la repetición focal terminó verde.
La primera ejecución Playwright encontró overflow horizontal en 375 px porque el
SHA-256 no podía cortarse dentro del aviso. Se agregó `min-width: 0` y corte seguro
de palabras largas; la repetición quedó verde y se revisaron visualmente las tres
capturas. Ningún fallo se omitió ni se relajó.

## Imágenes y runtime final

| Artefacto | ID local | Tamaño |
|---|---|---:|
| `logixone/app:j11-s8-c03-reference-data` | `sha256:12a023343a54ed383a8c744ba96e69ccf5ecac2cd511c315c2569b37136f34fd` | 501.101.806 bytes |
| `logixone/migrator:j11-s8-c03-reference-data` | `sha256:2e8402197f88c6c1beec0e2cde2c386b18317c94ad54659e781cdda2a6607d24` | 105.427.861 bytes |

Compose quedó con aplicación, PostgreSQL 18.4 y Keycloak 26.7.0 sanos, sin
eliminar ni recrear los volúmenes de datos. Las capturas revisadas están en
`docs/evidence/screenshots/J11-S8-C03/e2e/` y cubren 1280, 720 y 375 px.

## Persistencia y aislamiento

PostgreSQL confirmó cinco tablas privadas: publicaciones, países, monedas y dos
políticas por empresa. La prueba valida hashes, `BOOTSTRAP_SUBSET`, `PY/PYG/USD`,
la publicación corriente única, aislamiento de anulaciones entre empresas y cero
migraciones en la segunda ejecución.

## Pendientes posteriores al corte mínimo

- implementar RD-04 y RD-05: casos de uso auditados para políticas, publicaciones
  completas, importador reproducible y reconciliación;
- definir y validar la estrategia de listas grandes antes de consumo productivo;
- recongelar la fotografía final, regenerar/verificar el PDF y tomar la decisión
  de instalador antes de cerrar Sprint 8.
