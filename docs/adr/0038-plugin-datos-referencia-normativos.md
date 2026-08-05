# ADR-0038 — Plugin de datos de referencia normativos compartidos

- Estado: Aceptado
- Fecha: 2026-08-04
- Decisión de producto: incorporar `reference_data` como plugin funcional
  compartido para catálogos normativos versionados
- Modifica: la propiedad pendiente de países y monedas de ADR-0028 y la
  composición fundacional de ADR-0011, sin renumerar ERP 1–19

## Contexto

`business_partners` conserva países como texto ISO alfa-2 y
`commercial_catalog` conserva monedas como texto ISO 4217. Ambos dominios validan
la forma, pero no pueden comprobar que el código exista, pertenezca a una edición
conocida o esté admitido para la empresa. Los plugins futuros de compras, ventas,
documentos, tesorería, contabilidad y cooperativas necesitan las mismas
referencias.

El kernel no puede convertirse en maestro de países, monedas o catálogos fiscales.
Duplicar listas en cada plugin produciría versiones divergentes y obligaría a
actualizar muchos esquemas. ADR-0028 dejó expresamente pendiente una decisión
propietaria antes de iniciar `purchasing`.

ISO informa que la agencia de mantenimiento de ISO 3166 mantiene la lista vigente
en la Online Browsing Platform. La lista ISO 4217 es mantenida y publicada en línea
por SIX Financial Information como agencia oficial. Una edición de códigos cambia
independientemente del binario del consumidor, por lo que su procedencia debe
quedar reproducible y auditable.

## Decisión

### 1. Propietario y posición

Se crea el plugin funcional reutilizable `reference_data`, con API pública
`reference-data-api` y esquema privado `plg_reference_data`.

`reference_data` es una fundación compartida **R0**: precede a los consumidores
que necesiten países o monedas, pero no recibe un número dentro del roadmap ERP
1–19 ni renumera esos plugins. El catálogo futuro general pasa de veintiocho a
veintinueve plugins reutilizables.

`business_partners` y `commercial_catalog` declararán dependencia funcional
`REQUIRED` de `reference_data` cuando consuman sus contratos. Un perfil físico que
incluya cualquiera de esos consumidores debe incluir también el proveedor. La
activación por empresa respeta el orden topológico normal.

### 2. Alcance inicial

La primera versión posee:

- países con código alfa-2, alfa-3, numérico, nombre de referencia y edición;
- monedas y fondos con código alfabético, numérico, unidad menor, nombre de
  referencia y edición;
- metadatos inmutables de cada publicación: estándar, autoridad, URI, fecha de
  observación, checksum SHA-256, alcance y cantidad de entradas;
- política por empresa para habilitar o inhabilitar códigos sin modificar la
  publicación normativa;
- consulta pública por empresa y código;
- una pantalla neutral de consulta de ediciones y procedencia.

El primer corte usa un `BOOTSTRAP_SUBSET` verificable —`PY`, `PYG` y `USD`— para
cerrar contratos, migración y consumo sin fingir una importación completa. La
edición completa, el proceso reproducible de importación y la conciliación de
cambios permanecen como gates explícitos antes de uso multinacional o certificación.

### 3. Modelo de versiones

Una publicación se identifica por `(catalog_kind, release_id)`. Sus filas son
inmutables. Una edición nueva crea otra publicación y otras filas; nunca actualiza
retroactivamente la anterior. Solamente una publicación puede ser `CURRENT` por
catálogo.

Cada publicación registra `FULL` o `BOOTSTRAP_SUBSET`. Ninguna interfaz, guía o
reporte puede presentar un subconjunto como catálogo oficial completo. El checksum
corresponde al artefacto fuente observado, no a una cadena inventada ni a la
migración SQL.

La configuración empresarial se modela como una anulación separada. Ausencia de
anulación significa que la entrada corriente está habilitada; una fila
`DISABLED` la excluye para operaciones nuevas y una fila `ENABLED` permite
recongelar explícitamente la decisión. Inactivar conserva códigos históricos y no
reescribe documentos, identificaciones o listas de precios existentes.

### 4. Contrato público y límites

`reference-data-api` es Java puro y sólo depende de `kernel-api` para `CompanyId`.
Expone valores inmutables y búsquedas; no expone entidades, repositorios, DTO
internos ni Jakarta.

Los consumidores:

- persisten el código estable que corresponde a su dominio;
- vuelven a resolverlo dentro de la transacción antes de crear una operación;
- no consultan tablas `plg_reference_data`;
- conservan snapshots cuando un documento histórico lo requiera;
- no convierten un label localizado en identidad.

No existen relaciones JPA entre `reference_data` y otro plugin. Una baja de
publicación o una inhabilitación empresarial no puede borrar referencias ajenas.

### 5. Gobierno y actualización

La pantalla `/reference-data` permite consultar fuente, alcance, checksum y
entradas visibles. No permite crear países o monedas. El permiso
`reference_data.view` controla la consulta.

La habilitación empresarial y la promoción de una edición completa se agregarán
mediante comandos auditados y permiso separado. Importar una publicación exige:

1. obtener el artefacto de una fuente primaria autorizada;
2. conservar el original local bajo `.tools/downloads/reference-data/`;
3. registrar tamaño, SHA-256, URI y fecha de observación;
4. validar estructura, unicidad, cardinalidad y cambios respecto de la edición
   anterior;
5. generar una migración inmutable y pruebas de caracterización;
6. revisar licencias y no redistribuir una colección restringida;
7. promover la edición solamente después de conciliación y rollback probado.

No habrá actualización automática desde internet en runtime. La aplicación debe
seguir funcionando con la última edición verificada si una fuente externa no está
disponible.

### 6. Fuentes iniciales verificadas

El 2026-08-04 se observaron:

| Catálogo | Fuente primaria | Tamaño | SHA-256 | Alcance usado |
|---|---|---:|---|---|
| país | UN Statistics Division, tabla M49 con columnas ISO alfa-2/alfa-3 | 1.721.568 bytes | `748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11` | `PY/PRY/600`, subconjunto |
| moneda | SIX, ISO 4217 List One XML | 47.463 bytes | `838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9` | `PYG/600/0` y `USD/840/2`, subconjunto |

La tabla UN M49 es la fuente pública de los nombres y códigos numéricos y publica
las columnas ISO alfa-2/alfa-3. No se presenta como sustituto contractual de una
suscripción ISO. Una composición que requiera certificación o la lista completa
de ISO 3166 debe adquirir o usar la fuente autorizada aplicable y registrar su
licencia.

## Consecuencias

### Positivas

- una sola frontera posee países y monedas;
- los consumidores validan existencia y edición sin tablas cruzadas;
- la procedencia y el alcance incompleto quedan visibles;
- los códigos históricos sobreviven a nuevas publicaciones;
- compras, ventas, finanzas y cooperativas reutilizan el mismo contrato.

### Costes y riesgos

- agrega dos módulos Maven y una dependencia funcional temprana;
- una lista completa de países puede exigir licencia o fuente autorizada;
- el subconjunto inicial no habilita operación multinacional;
- todavía falta la administración auditada de anulaciones empresariales;
- el renderer actual necesita una decisión de umbral antes de ofrecer listas
  completas inline.

## Alternativas descartadas

### Guardar países y monedas en el kernel

Se descarta porque amplía el kernel con maestros funcionales y obliga a acoplar
actualizaciones normativas al núcleo.

### Duplicar listas en cada consumidor

Se descarta porque produce ediciones divergentes y no ofrece una fuente única de
auditoría.

### Consultar internet durante cada operación

Se descarta por disponibilidad, latencia, privacidad, reproducibilidad y riesgo de
que una edición cambie sin migración ni revisión.

### Declarar completa una lista parcial

Se descarta porque haría imposible evaluar cobertura y podría inducir afirmaciones
de cumplimiento falsas.

## Referencias

- [ADR-0011 — Roadmap de plugins productivos](0011-roadmap-dependencias-plugins-productivos.md)
- [ADR-0012 — Composición física y migraciones](0012-composicion-unica-y-migraciones-de-plugins.md)
- [ADR-0028 — Gobierno de selectores](0028-gobierno-de-selectores-y-datos-administrables.md)
- [Épica de datos de referencia](../backlog/epica-datos-referencia-normativos.md)
- [ISO 3166 — Country codes](https://www.iso.org/iso-3166-country-codes.html)
- [UN M49 — Standard country or area codes](https://unstats.un.org/unsd/methodology/m49/overview/)
- [SIX — ISO 4217 Currency Codes](https://www.six-group.com/en/products-services/financial-information/market-reference-data/data-standards.html)
