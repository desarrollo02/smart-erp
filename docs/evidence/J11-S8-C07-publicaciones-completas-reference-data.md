# J11-S8-C07 — Publicaciones completas de datos de referencia

- Fecha: 2026-08-05
- Estado: implementación y gates técnicos verdes
- Rama local: `chore/J11-S8-C04-adopcion-git`
- Herramientas del proyecto: Maven, Java, Chromium, fuentes y temporales bajo `.tools/`

## Publicación reproducible

Producto confirmó que `minor unit = N.A.` es ausencia explícita y que toda fuente
con más de 100 opciones usa `SEARCH_ON_DEMAND`, filtro servidor y páginas máximas
de 50.

| Fuente local validada | SHA-256 | Resultado |
|---|---|---|
| UN M49 observado 2026-08-04 | `748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11` | 248 países únicos |
| SIX List One observado 2026-08-04 | `838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9` | 178 códigos únicos; 13 `N.A.` |

El generador offline produjo el mismo SHA-256 que V4 versionada:
`72cb35a11073cb232129074551af0d0e8181eb8f4a4e52f1492c2883a287d3c0`.
Valida ruta, tamaño, checksum, estructura, fecha, cardinalidad, unicidad y
conflictos antes de escribir únicamente en `.tools/tmp`.

## Contratos y datos observados en PostgreSQL

- `reference-data-api` 1.1.0 conserva compatibilidad 1.x y expone unidad menor
  opcional;
- `plugin-api` 0.4.3 limita solicitudes y páginas a 50;
- V3 representa `N.A.` con `NULL`, nunca cero o `-1`;
- V4 conserva las ediciones bootstrap y deja una única edición `FULL` corriente;
- la base real informó 248 países, 178 monedas/fondos y 13 unidades `NULL`;
- `business_partners` y `commercial_catalog` consumen el API público y no leen
  tablas privadas de `reference_data`.

## Matriz final

| Validación | Resultado |
|---|---|
| Generador, XML/XHTML, UTF-8, secretos y enlaces | verde |
| PostgreSQL/Testcontainers | 5/5, Flyway V1–V4 e idempotencia |
| `clean verify` final | 26/26 módulos, 498 pruebas y 28 ArchUnit; cero fallos, errores u omitidas |
| Build OCI verificado | ambos Dockerfiles aceptados; app y migrator construidos para `linux/amd64` |
| Migrator/Compose | primera ejecución aplicó V2–V4; segunda ejecución informó cero migraciones |
| Health/OIDC normal | 2/2 + 4/4 verdes |
| Health/JTA/OIDC aislado | 12/12, cero omisiones; recursos efímeros eliminados |
| Playwright | 1/1; 30 capturas; búsqueda/paginación, XDR, `N.A.`, permisos y restauraciones |
| Logs finales | cero `ERROR`, `SEVERE`, excepciones o `Caused by` en la ventana final |

La revisión visual abarcó directorio normativo, historial de política, selección de
país y denegaciones en 375, 720 y 1280 px. Las tablas se transforman en tarjetas
en medio/compacto y conservan acciones, foco y lectura sin overflow horizontal.

## Imágenes exactas

- `logixone/app:j11-s8-c07-reference-data`:
  `sha256:52cf22451dc7ff89192a9b88d89e97b26b0e45f508654d67c52b6fd38b83d9fd`,
  501.161.623 bytes, `jboss`;
- `logixone/migrator:j11-s8-c07-reference-data`:
  `sha256:1b598fb140659a04501a5890c2279c80545cf0115eba0711ef37a30cfdf19c77`,
  105.478.277 bytes, `10001:10001`.

El stack oficial quedó saludable y el migrator en código 0. La composición JTA
aislada y sus dos volúmenes se eliminaron; los volúmenes principales permanecen.

## Estado de cierre

J11-S8-C07 satisface CA-01 a CA-08. No se promovió imagen ni se tocó producción.

## PDF obligatorio regenerado

- ruta: `docs/output/pdf/guia-estructura-repositorio-logixone.pdf`;
- páginas: 98 A4;
- tamaño: 399.547 bytes;
- SHA-256:
  `9527bb7554a88ced1398e6668f71b7be8af3b5b0d7462e1ce560a9b1c18934ca`;
- metadatos: título `Smart ERP - Guía de estructura - Sprint 8 - J11-S8-C07`,
  autor `Proyecto Smart ERP`, PDF 1.4, sin cifrado, formularios ni JavaScript;
- texto: 486.599 caracteres extraíbles, mínimo 529 por página y cero páginas
  vacías;
- revisión visual inicial: las 98 páginas se renderizaron bajo
  `.tools/tmp/pdfs/J11-S8-C07-final-20260805-02/` y se revisaron en diez hojas de
  contacto, además de portada, estado y cierre a tamaño original;
- revisión después de la decisión `NO`: las 98 páginas finales se renderizaron
  bajo `.tools/tmp/pdfs/J11-S8-C07-final-20260805-03/`; 94 conservaron exactamente
  el mismo SHA-256 visual y las páginas 1, 4, 97 y 98 cambiadas se revisaron a
  resolución original;
- resultado: sin recortes, solapamientos, páginas vacías, tablas partidas de forma
  ilegible, glifos dañados ni defectos en encabezados, pies o numeración.

El G0 documental final validó 307 archivos Markdown: cero enlaces rotos, errores
UTF-8, mojibake o filtraciones de secretos. `git diff --check`, excluyendo los PDF
binarios, no informó errores; sólo anticipó la normalización CRLF futura del
generador Python en Windows.

## Decisión de instalador Windows

- respuesta: `NO`;
- fecha: 2026-08-05;
- responsable: responsable de producto;
- razón: crear un nuevo instalador cuando Smart ERP tenga una versión
  comercializable y útil para al menos un tipo de negocio;
- efecto: `installer/windows/current` no se borró ni reemplazó, permanece asociado
  al baseline anterior y no puede entregarse como instalador de J11-S8-C07;
- gates no aplicables a este cierre: regeneración, VM Windows, UAC/cancelación y
  Authenticode del instalador nuevo.

El Sprint sigue abierto por G7 independiente.
