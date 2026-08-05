# ADR-0019 — Modelo de catálogo comercial y contratos públicos

- Estado: Aceptado
- Fecha: 2026-07-30
- Historia: `J11-S7-02`
- Decisión de producto: CC-D01 a CC-D10 confirmadas sin cambios el 2026-07-30

## Contexto

El legado concentra artículos, servicios, unidades, clasificaciones, impuestos,
precios y dependencias de stock, ventas, compras y sectores específicos en un
maestro ampliamente acoplado. Los plugins posteriores necesitan referencias y
cotizaciones comerciales estables sin importar entidades, repositorios o tablas
del catálogo.

ADR-0002 y ADR-0011 exigen que `commercial_catalog` sea otro plugin funcional,
independiente de `business_partners`, y que sus contratos públicos residan en un
módulo Java puro separado. La caracterización CC-O01 a CC-O18, los casos CC-UC-01
a CC-UC-15, las invariantes CC-I01 a CC-I18 y las decisiones CC-D01 a CC-D10 son la
entrada aceptada.

## Decisión

### 1. Separación física

Se crean dos módulos Maven nuevos:

- `commercial-catalog-api`: contratos públicos Java puros; puede depender de
  `kernel-api` únicamente para `CompanyId`;
- `commercial-catalog`: plugin funcional con descriptor, dominio y futuros
  adaptadores; depende de su API, `plugin-api` y contratos públicos del kernel.

`business_partners` no contiene ni administra catálogo. El kernel y los otros
plugins no dependen de la implementación de `commercial_catalog`.

### 2. Ítem comercial

`CatalogItem` es el agregado raíz común. Conserva empresa, ID opaco, código,
nombre, descripción, tipo inmutable `PRODUCT`/`SERVICE`, alcances de compra/venta,
unidad base, perfil tributario interno, clasificaciones, identificadores,
conversiones, variante opcional, estado y versión optimista.

- El código se normaliza y su unicidad física será por empresa.
- Un ítem debe permitir compra, venta o ambos alcances.
- El tipo no cambia después del alta.
- No existe baja física normal.
- La inactivación puede señalar un reemplazo; no modifica referencias históricas.
- Los ciclos de reemplazo se validan con el grafo empresarial en aplicación.

### 3. Identificadores, variantes y definiciones

Un código empresarial legible no es la identidad técnica. Los códigos de barras y
otros identificadores son múltiples, tipados, normalizados e inactivables.

La primera versión incluye IDs y asignaciones tipadas para familia/atributos de
variantes, pero no genera combinaciones cartesianas. Categoría principal,
categorías secundarias, marca opcional y etiquetas controladas se referencian por
IDs del mismo propietario y empresa.

### 4. Unidades y perfil tributario

Cada ítem tiene unidad base y conversión implícita `1`. Las conversiones
adicionales son positivas, específicas por ítem y pueden declarar propósitos y un
único default por propósito. Cada consumidor conserva el factor efectivamente
usado.

El catálogo referencia un perfil tributario interno versionado. Los códigos,
catálogos, XSD y reglas SIFEN pertenecen al futuro plugin fiscal. Los documentos
persisten snapshot de tasas y códigos efectivos.

### 5. Listas y precios

En el primer corte, `commercial_catalog` posee listas y entradas de precio.
Cada lista fija moneda ISO 4217, modo `NET`/`TAX_INCLUDED`, escala y redondeo. Los
importes usan `BigDecimal`; para el mismo alcance no pueden existir vigencias
activas ambiguas.

Ventas decidirá elegibilidad, prioridad, promociones y lista aplicable. El puerto
de cotización recibe una lista explícita y devuelve lista, versión, moneda,
vigencia, unidad, importe y cantidades usados. El contrato permite separar un
plugin `pricing` futuro sin compartir JPA.

### 6. Contratos públicos

La versión inicial es `1.0.0` y publica contratos pequeños por propósito:

- identidad, tipo y estado del ítem;
- referencia mínima y búsqueda paginada por empresa;
- conversión de cantidad entre unidades de un ítem;
- cotización determinista sobre una lista explícita.

No publica entidades, DTO internos, nombres de tablas, detalles de persistencia ni
objetos de otros plugins. Cada consumidor conserva su snapshot histórico.

### 7. Alcance temporal

`J11-S7-02` crea API, descriptor neutral y dominio sin JPA. `J11-S7-03` será dueña
de esquema, Flyway, JPA y repositorios; `J11-S7-04` de aplicación, permisos y
auditoría; `J11-S7-05` de interfaz. No se declara outbox sin productor y consumidor
reales.

## Alternativas descartadas

- Un único módulo para API e implementación: debilita los límites de consumo.
- Agregados separados para producto y servicio: duplica identidad, unidades,
  impuestos y precios sin aportar un límite de dominio.
- Barcode o código como PK: son identificadores empresariales mutables.
- Tablas o entidades compartidas: contradicen propiedad privada por plugin.
- Copiar familia/grupo/línea y flags sectoriales: traslada el acoplamiento legado.
- Guardar SIFEN directamente en el ítem: acopla catálogo a una versión fiscal.
- Precio fijo dentro del ítem: no representa moneda, vigencia, escalas ni listas.
- Baja física normal: rompe referencias históricas.

## Consecuencias

- El reactor incorpora dos módulos Maven adicionales.
- Los consumidores podrán compilar únicamente contra la API pública.
- Persistencia deberá materializar unicidad, vigencia y aislamiento empresarial sin
  cambiar el dominio por comodidad de JPA.
- La primera superficie pública es mayor que la de `business_partners` porque
  existen tres propósitos distintos: referencia, conversión y cotización.
- Inventario, costos, compras, ventas, documentos y SIFEN continúan fuera del
  plugin.

## Verificación

- unitarias de IDs, referencias, requests y resultados públicos;
- normalización, alcances, identificadores, conversiones, clasificación, variante,
  inactivación/reemplazo y concurrencia del agregado;
- lista, entradas, vigencias, `BigDecimal`, moneda, impuesto y redondeo;
- descriptor CDI/SPI sin migraciones, permisos, menú, pantalla ni eventos;
- ArchUnit para API/dominio sin frameworks y límites entre plugins;
- `mvn verify` del reactor antes de cerrar `J11-S7-02`.
