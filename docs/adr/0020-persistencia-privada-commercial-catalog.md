# ADR-0020 - Persistencia privada de `commercial_catalog`

- Estado: Aceptado
- Fecha: 2026-07-30
- Historia: `J11-S7-03`
- Decisiones relacionadas: ADR-0003, ADR-0012 y ADR-0019

## Contexto

J11-S7-02 dejó verdes la API pública y el dominio neutral del catálogo. Falta
materializar sus invariantes en PostgreSQL sin compartir entidades o tablas, sin
confundir código/barcode con identidad, sin guardar SIFEN en el maestro y sin
permitir vigencias de precio ambiguas bajo concurrencia.

## Decisión

### Propiedad y unidad de persistencia

- El plugin es propietario exclusivo del esquema `plg_commercial_catalog`.
- La migración inicial vive en
  `classpath:db/migration/commercial_catalog/V1__initialize_commercial_catalog_schema.sql`.
- La unidad JPA `logixone-commercial-catalog-pu` usa el datasource JTA
  `java:/jdbc/LogixoneCoreDS`, deshabilita DDL automático y valida el esquema.
- Cada fila operativa y cada definición lleva `company_id`; no existe FK hacia
  `core.company`. Aplicación valida empresa y autorización antes del repositorio.
- No existen FKs, joins JPA ni asociaciones hacia esquemas de otros plugins.

### Modelo relacional

La V1 separa veinte tablas:

1. definiciones: `unit_definition`, `category_definition`, `brand_definition`,
   `tag_definition`, `tax_profile`, `tax_profile_revision`, `variant_family` y
   `variant_attribute_definition`;
2. ítems: `catalog_item`, `catalog_item_scope`, `catalog_item_identifier`,
   `catalog_item_unit_conversion`, `catalog_item_unit_purpose`,
   `catalog_item_category`, `catalog_item_tag`, `catalog_item_variant` y
   `catalog_item_variant_attribute`;
3. precios: `price_list` y `price_entry`;
4. códigos futuros: `catalog_code_sequence`, contador transaccional por empresa y
   ámbito, nunca `MAX + 1`.

Las definiciones son propiedad del mismo plugin y empresa. Categorías admiten
padre opcional; los perfiles tributarios conservan revisiones internas sin XML,
XSD o códigos SIFEN; familias declaran atributos tipados sin generar combinaciones.

### Restricciones

- código de ítem, lista y definiciones es único por empresa y ámbito;
- un ítem tiene al menos un alcance y una unidad base controlada;
- identificadores activos normalizados no son ambiguos dentro de la empresa;
- factores e importes usan `NUMERIC`, son positivos/no negativos y nunca flotantes;
- un único default activo existe por ítem/finalidad de unidad;
- categoría principal, marca, etiquetas, perfil y variante pertenecen a la misma
  empresa mediante FKs compuestas privadas;
- reemplazo es interno a empresa, no puede ser el mismo ítem y los ciclos se
  validan además en aplicación;
- la lista fija moneda, modo tributario, escala y redondeo;
- entradas con igual lista, ítem, unidad y cantidad mínima no pueden tener
  vigencias activas superpuestas. Un trigger usa un advisory lock transaccional
  por alcance antes de comprobar el intervalo, evitando carreras concurrentes;
- raíces usan `@Version`; no se expone borrado físico.

### Mapeo y repositorios

JPA mapea únicamente raíces y detalles operativos. Las entidades permanecen en
`infrastructure.persistence`; dominio y puertos conservan Java puro. Los
repositorios siempre reciben `CompanyId`, reconstruyen snapshots completos y
convierten unicidad, referencia inválida, vigencia solapada y versión obsoleta en
códigos estables.

Las tablas de definiciones se prueban en esta historia como integridad relacional.
Sus comandos administrativos se implementarán en J11-S7-04; no se adelantan
permisos o UI.

### Evolución V2 aprobada en J11-S8-C02

La migración inmutable
`V2__version_simple_catalog_definitions.sql` agrega cuatro tablas privadas:
`unit_definition_revision`, `category_definition_revision`,
`brand_definition_revision` y `tag_definition_revision`. Cada una usa la clave
empresa/identidad/versión y conserva nombre, estado y sólo la estructura que
corresponde al tipo: escala decimal para unidad o padre para categoría.

V2 retroalimenta una revisión vigente desde cada raíz existente. Desde su
aplicación, toda alta, revisión permitida y transición real de estado agrega una
fila; no actualiza ni elimina revisiones anteriores. Código e identidad son
inmutables. La raíz conserva la versión optimista actual y la consulta marca la
revisión vigente comparando versiones. No se crean relaciones con otros esquemas,
EAV, JSON operativo ni contenido de auditoría con nombres empresariales.

El backfill no intenta inventar estados anteriores a V2: conserva como punto de
partida únicamente la versión vigente que puede demostrarse desde la raíz.

## Alternativas descartadas

- Una tabla gigante o JSON/EAV como fuente operativa: impide cardinalidades,
  claves e índices claros.
- Definiciones globales sin empresa: rompe aislamiento y personalización de datos.
- Códigos SIFEN en `catalog_item`: acopla el dominio a un formato fiscal externo.
- Precio dentro del ítem: pierde lista, moneda, vigencia y política de redondeo.
- Validar solapamientos solo con una consulta Java: conserva una carrera entre
  transacciones concurrentes.
- Relacionar JPA con empresa, inventario o ventas: viola propiedad por plugin.

## Consecuencias

- La V1 es más amplia que la de participantes porque el catálogo posee
  definiciones y precios además de la raíz.
- V2 eleva el esquema a veinticuatro tablas y permite historial append-only de
  definiciones simples sin cambiar el contrato público `1.0.0`.
- Desactivar o retirar el plugin conserva esquema y datos.
- La composición WAR/migrador continúa reservada a J11-S7-06.
- Aplicación, permisos y auditoría corresponden a J11-S7-04; UI a J11-S7-05.
- Una migración aplicada es inmutable; todo cambio posterior usa V2 o superior.

## Verificación obligatoria

1. migrar PostgreSQL vacío y repetir sin reaplicar;
2. comprobar veinte tablas, FKs privadas, checks e índices;
3. validar JPA con generación de DDL deshabilitada;
4. probar unicidad/aislamiento por empresa, identificadores y defaults;
5. probar perfiles, clasificaciones y variantes de la misma empresa;
6. probar vigencias adyacentes, solapamiento y carrera protegida;
7. probar round-trip y conflicto optimista de ítem/lista;
8. comprobar ausencia de API de borrado, referencias cruzadas y tipos flotantes;
9. comprobar V1→V2, backfill, repetición idempotente e historial por empresa;
10. ejecutar módulo, PostgreSQL/Testcontainers, ArchUnit y reactor completo.
