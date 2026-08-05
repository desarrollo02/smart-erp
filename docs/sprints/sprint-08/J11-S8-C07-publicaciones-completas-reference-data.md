# J11-S8-C07 — Publicaciones completas y listas grandes de referencia

- Estado: Completada
- Fecha: 2026-08-05
- Épica: [Datos de referencia normativos](../../backlog/epica-datos-referencia-normativos.md)
- Corte: RD-05
- Plugin: `reference_data`

## Objetivo

Importar reproduciblemente las publicaciones completas observadas de países y
monedas, conservar su procedencia y ofrecerlas sin cargar listas grandes inline ni
perder el significado de `minor unit = N.A.`.

## Decisiones confirmadas

- `N.A.` se persiste como ausencia explícita, nunca como cero o centinela;
- el contrato 1.x mantiene el acceso entero existente para valores definidos y
  agrega una consulta opcional compatible;
- una fuente con más de 100 opciones usa `SEARCH_ON_DEMAND`;
- cada página contiene como máximo 50 opciones y se filtra en servidor;
- los originales validados permanecen en `.tools/downloads/reference-data/`;
- el importador no usa red y verifica tamaño, SHA-256, cardinalidad y unicidad.

## Alcance

1. contrato compatible para unidad menor opcional y migración privada V3;
2. importador determinista de UN M49 y SIX List One;
3. migración inmutable de publicaciones `FULL` y conciliación con el subconjunto;
4. API de búsqueda paginada por empresa y catálogo;
5. metadatos `SEARCH_ON_DEMAND` para países y monedas;
6. pantalla propietaria con filtro, página y total;
7. pruebas de parser, checksums, cardinalidad, PostgreSQL y responsive.

## Criterios de aceptación

- **CA-01:** se conservan 248 países únicos y 178 códigos de moneda/fondo únicos
  derivados de los originales validados, o el build falla con diferencia explícita;
- **CA-02:** los 13 códigos con `N.A.` conservan ausencia y nunca aparecen como
  cero decimales;
- **CA-03:** el importador falla ante checksum, estructura, duplicado o conflicto;
- **CA-04:** las publicaciones anteriores permanecen inmutables y sólo una edición
  por catálogo queda corriente;
- **CA-05:** búsqueda normaliza el texto, aísla empresa, aplica política y devuelve
  hasta 50 opciones con total verificable;
- **CA-06:** consumidores continúan revalidando por código dentro de su transacción;
- **CA-07:** selectores y pantalla no envían más de 50 filas ni generan overflow en
  375, 720 o 1280 px;
- **CA-08:** módulo, PostgreSQL, ArchUnit, `mvn verify`, Compose y Playwright quedan
  verdes antes de cerrar la historia.

## Validación aislada

Todo corte se prepara en el índice y se materializa bajo
`.tools/tmp/validation/J11-S8-C07-*`. Maven se ejecuta únicamente mediante el
Wrapper raíz; fuentes, temporales y herramientas permanecen bajo `.tools`.

## Implementación materializada

- `reference-data-api` 1.1.0 representa `N.A.` con `OptionalInt` y agrega consulta
  normalizada/paginada compatible;
- `plugin-api` 0.4.3 agrega búsqueda de opciones y página neutral, manteniendo los
  constructores anteriores;
- V3 distingue ausencia de cero; V4 conserva las ediciones bootstrap e incorpora
  `un-m49-2026-08-04-full` y `six-list-one-2026-01-01-full` como corrientes;
- el generador offline conserva y valida originales sólo bajo `.tools`, produce
  248 países, 178 códigos únicos y 13 unidades `N.A.` y no escribe fuera de
  `.tools/tmp`;
- país y moneda declaran `SEARCH_ON_DEMAND`; socios y catálogo reciben hasta 50
  opciones y vuelven a resolver el código dentro de su caso de uso JTA;
- la pantalla propietaria filtra, cuenta y pagina mediante el puerto público; el
  adaptador JPA ejecuta filtro, política empresarial y límites en PostgreSQL.

## Evidencia actual

La generación produjo SHA-256
`72cb35a11073cb232129074551af0d0e8181eb8f4a4e52f1492c2883a287d3c0`, idéntico al
de V4 versionada. El corte focal materializado desde el índice quedó verde en 14
módulos y 40 pruebas seleccionadas de contratos, migración, handlers, metadatos y
renderer. El gate completo posterior quedó verde en 26/26 módulos, 498 pruebas y
28 ArchUnit, sin fallos, errores u omisiones. La evidencia detallada se conserva en
[J11-S8-C07](../../evidence/J11-S8-C07-publicaciones-completas-reference-data.md).

PostgreSQL/Testcontainers quedó 5/5 con Flyway V1–V4 e idempotencia. La pareja de
imágenes C07 arrancó mediante Compose, el migrator terminó en código 0, health y
OIDC quedaron 2/2 y 4/4, y el arnés JTA aislado quedó 12/12 sin omisiones. La demo
Playwright terminó 1/1 y produjo 30 capturas en 375, 720 y 1280 px; la revisión
visual confirmó tarjetas responsive, autorización negativa y ausencia de overflow
horizontal normal. CA-01 a CA-08 quedaron satisfechos.
