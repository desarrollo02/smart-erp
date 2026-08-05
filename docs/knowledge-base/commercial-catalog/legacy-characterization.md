# Caracterización del legado para `commercial_catalog`

- Fecha de inspección: 2026-07-30
- Proyecto observado: `C:\cosme\multienvios\miaterra`
- Modo de trabajo: solo lectura
- Estado: caracterización completa; CC-D01 a CC-D10 confirmadas sin cambios el 2026-07-30
- Alcance futuro: segundo plugin productivo de Logixone

## Propósito

Transformar el comportamiento útil del catálogo legado en lenguaje, casos de uso,
invariantes y decisiones neutrales. Este documento no aprueba entidades, tablas ni
interfaces del nuevo ERP y no autoriza copiar código `javax.*`, SQL o XHTML del
legado.

El análisis distingue cinco conceptos que el legado concentra alrededor de
`StwArticulos`:

1. identidad y descripción del concepto comercial;
2. clasificación, marca, unidades y tributación comercial;
3. listas y vigencias de precios;
4. inventario, costos, existencias y movimientos;
5. reglas particulares de ventas, compras, contabilidad, producción, taller y
   transporte.

Solo los tres primeros son candidatos para `commercial_catalog`. Los demás
pertenecen a consumidores futuros y se comunicarán mediante contratos públicos.

## Fuentes contrastadas

Las rutas siguientes pertenecen al proyecto legado y fueron consultadas sin
modificarlas:

| Área | Fuente principal | Evidencia observada |
|---|---|---|
| maestro | `StwArticulos.java` | código por empresa, descripción, tipo, estado, código de barras y muchas relaciones cruzadas |
| pantalla principal | `controlStock/StwCOArticulos.xhtml` y `seccionAddArticulos.xhtml` | directorio/ABM con datos, equivalencias, precios, movimientos, análisis y estadísticas |
| aplicación | `StwCOArticulosControlador.java` | alta, modificación, generación de código, unidades, precios y marcas operativas |
| pantalla antigua | `StwArticulosControlador.java` | búsqueda, ABM y eliminación física |
| documentación funcional | `docs/controlStock/StwCOArticulos.md` | campos, clasificadores, listas activas y usos por otros módulos |
| listas de precio | `StwListaPrecio.java`, controlador, pantalla y `StwListaPrecio.md` | listas por empresa, orden, estado, condición de venta y lista predeterminada |
| precios | `StwPreciosFijos.java` | importe, moneda, cantidad, vigencia, lista, empresa y sucursal |
| unidades | `StwUnidadesMedida.java` | código, descripción, abreviatura y multiplicador |
| conversiones | `StwArticuloUnidadConversion.java` y script 20260711 | factor por artículo/unidad y defaults por operación |
| clasificaciones | `StwFamilia`, `StwGruposArt`, `StwLinea`, `StwMarcas` | códigos y descripciones, mayormente por empresa |
| tributación | `StwIva.java` | código, porcentajes de compra/venta y vigencia |
| importación | `CU-001-importacion-masiva-articulos.md` | validación previa, filas independientes, código de artículo y listas configurables |
| consumo en ventas | `CU-001-lista-precio-facturacion-v2.md` | prioridad de listas, condición de venta, fallback y recalculo |

También se buscaron usos de `StwArticulos` en ventas, compras, stock, taller,
producción, transporte, contratos y servicios. Esos usos demuestran consumidores,
pero no convierten sus datos en propiedad del catálogo.

## Lenguaje neutral

| Término legado | Término candidato | Interpretación |
|---|---|---|
| artículo | ítem comercial | referencia seleccionable por procesos comerciales |
| producto/servicio informal | tipo de ítem | naturaleza estable `PRODUCT` o `SERVICE` |
| tipo de concepto C/V/T | alcance comercial | habilitado para compra, venta o ambos; no expresa producto/servicio |
| código de artículo | código empresarial | identificador legible único dentro de una empresa |
| código de barras | identificador escaneable | identificador alternativo, nunca identidad técnica |
| familia/grupo/línea | categoría | clasificación jerárquica o complementaria |
| marca | marca comercial | catálogo controlado e independiente de categoría |
| unidad de medida | unidad | código, símbolo, descripción y precisión |
| equivalencia | conversión por ítem | cantidad de unidad base representada por otra unidad |
| IVA | perfil tributario interno | clasificación versionada, separada de una codificación fiscal externa |
| lista de precio | lista de precios | política empresarial con moneda, modo tributario y vigencia |
| precio fijo | entrada de precio | importe efectivo de un ítem en una lista y rango de vigencia/cantidad |
| activo/inactivo | ciclo de vida | disponibilidad operativa sin eliminar identidad ni historia |
| artículo relacionado | relación o reemplazo | vínculo explícito con semántica controlada |

`Ítem comercial` es el término de trabajo. El nombre público definitivo se decide
en CC-D01 y se valida antes de crear `commercial-catalog-api`.

## Comportamiento observado y requisito neutral

| ID | Observación del legado | Requisito neutral candidato | Tratamiento |
|---|---|---|---|
| CC-O01 | código único por empresa | identidad técnica opaca y código legible aislado por empresa | conservar y endurecer |
| CC-O02 | búsqueda/listado por datos del artículo y clasificadores | búsqueda paginada por código, descripción, identificador, tipo, categoría, marca y estado | conservar |
| CC-O03 | el mismo maestro se usa para bienes y servicios | catálogo común con tipo estable y alcances de compra/venta independientes | decidir CC-D01 |
| CC-O04 | un solo campo de código de barras | permitir identificadores alternativos tipados y normalizados | ampliar, CC-D02 |
| CC-O05 | talle, color y modelo generan detalles relacionados | soportar variantes sin copiar el generador acoplado | decidir CC-D03 |
| CC-O06 | unidad base y conversiones por artículo | conversión positiva, precisa y con defaults no ambiguos | conservar, CC-D04 |
| CC-O07 | IVA contiene tasas y fecha de vigencia | perfil interno versionado, sin acoplarse a SIFEN | separar, CC-D05 |
| CC-O08 | listas activas por empresa reemplazan precios 1/2/3 | listas configurables y entradas de precio efectivas | conservar, CC-D06 |
| CC-O09 | ventas asocia listas a clientes/condiciones | catálogo publica precios; ventas administra elegibilidad y prioridad comercial | separar |
| CC-O10 | precio guarda moneda, sucursal, cantidad y fechas | precio explícito en moneda y vigencia; alcance adicional solo si se aprueba | simplificar, CC-D07 |
| CC-O11 | familia, grupo, línea y marca se solapan | categorías coherentes y marca independiente | simplificar, CC-D08 |
| CC-O12 | existe eliminación física en un controlador | inactivar/reactivar y reemplazar, nunca borrar en operación normal | rechazar, CC-D09 |
| CC-O13 | stock y movimientos cuelgan del maestro | inventario consume ID y unidad; el catálogo no posee existencias | separar |
| CC-O14 | costo, proveedor y cuentas contables están en la entidad | compras/costeo/contabilidad son consumidores externos | separar |
| CC-O15 | flags de producción, taller, flete y combustible viven en el maestro | cada plugin aporta su clasificación mediante contrato propio | separar |
| CC-O16 | importación valida antes de escribir y procesa por fila | conservar como capacidad futura, fuera del primer corte | diferir |
| CC-O17 | precios de listas inactivas se conservan | preservar entradas históricas y ocultarlas solo para nuevas operaciones | conservar |
| CC-O18 | documentos y pedidos recalculan usando datos actuales | consumidores deben persistir snapshot de lo utilizado | corregir, CC-D10 |

## Deudas que no deben heredarse

- `StwArticulos` importa entidades de numerosos dominios y expone relaciones JPA
  bidireccionales hacia movimientos, cuentas, proveedores, módulos y documentos.
- Existen columnas fijas `precioBase`, `precio2` y `precio3` junto con un modelo de
  listas configurables; el nuevo sistema tendrá un solo origen de precios.
- Un método obtiene el siguiente código mediante `MAX(cod_articulo) + 1`; no es
  seguro bajo concurrencia.
- La pantalla antigua permite eliminación física.
- La semántica de estado usa valores `S`, `A`, `N`, `I` o nulos según la tabla.
- El código de barras está limitado a un único texto corto y no distingue GTIN,
  código interno, empaque u otro identificador.
- El tipo C/V/T mezcla alcance de compra/venta con la naturaleza del ítem.
- Unidad, moneda, IVA, categoría y marca tienen reglas de empresa inconsistentes.
- El precio mezcla lista, sucursal, cantidades, descuento, devolución y tipos
  heredados en una misma fila.
- El ABM mezcla configuración del catálogo con stock, historial de movimientos,
  costos, contabilidad, análisis y estadísticas.
- Los flags `esIngrediente`, `esInsumoTaller`, `esCargaFlete` y `esCombustible`
  trasladan decisiones de plugins consumidores al maestro central.
- Hay consultas nativas y filtros concatenados que no deben convertirse en puertos
  públicos ni repositorios del nuevo plugin.

## Frontera candidata del plugin

`commercial_catalog` sería propietario de:

- identidad, código, nombre y descripción del ítem;
- tipo producto/servicio y alcance compra/venta;
- identificadores alternativos;
- categorías, marcas y atributos de variante aprobados;
- unidad base y conversiones por ítem;
- perfil tributario comercial interno y su asignación efectiva;
- listas de precios y entradas de precio;
- estados, reemplazos, versión optimista y auditoría funcional;
- vistas y resoluciones públicas mínimas por identificador.

Quedan fuera:

| Responsabilidad | Propietario futuro |
|---|---|
| existencias, depósitos, lotes, series, reservas y movimientos | `inventory` |
| proveedores preferidos, costos de compra y condiciones | `purchasing` |
| clientes elegibles, condición de venta, promociones y descuentos | `sales`/`pricing` futuro si se separa |
| pedidos, facturas, notas, remisiones y snapshots documentales | plugins de documentos/ventas/compras |
| cuentas, centros de costo e imputación | `accounting` |
| fórmulas, ingredientes y elaborados | `manufacturing` |
| taller, combustible, fletes y otros usos especializados | plugin funcional correspondiente |
| catálogos, XSD, CDC, firma y reglas SIFEN | integración fiscal/SIFEN |
| monedas maestras corporativas si luego se centralizan | contrato público dedicado; por ahora código ISO |

Ningún consumidor accederá a tablas privadas ni importará entidades JPA del
catálogo.

## Actores y permisos candidatos

| Permiso | Capacidad |
|---|---|
| `commercial_catalog:read` | buscar, consultar y resolver referencias |
| `commercial_catalog:manage_items` | crear y modificar ítems, identificadores y clasificaciones |
| `commercial_catalog:manage_prices` | administrar listas y entradas de precio |
| `commercial_catalog:manage_definitions` | administrar unidades, categorías, marcas y perfiles tributarios |

La interfaz ocultará acciones no autorizadas, pero cada comando y consulta se
autorizará nuevamente en el servidor con empresa confiable y plugin activo.

## Casos de uso neutrales candidatos

| ID | Caso de uso | Resultado esperado |
|---|---|---|
| CC-UC-01 | buscar ítems | página aislada por empresa con filtros y orden estable |
| CC-UC-02 | consultar ficha | detalle autorizado con estado, versión y definiciones vigentes |
| CC-UC-03 | registrar ítem | producto o servicio con identidad, código y unidad base válidos |
| CC-UC-04 | modificar ítem | cambio con versión optimista y auditoría |
| CC-UC-05 | administrar identificadores | agregar/inactivar códigos de barras u otros identificadores sin cambiar el ID |
| CC-UC-06 | administrar categorías y marca | asignaciones válidas dentro de la empresa |
| CC-UC-07 | administrar variante | vincular familia/atributos según el alcance aprobado |
| CC-UC-08 | administrar unidades y conversiones | factores positivos y defaults no ambiguos |
| CC-UC-09 | asignar perfil tributario | vigencia sin intervalos incompatibles |
| CC-UC-10 | administrar lista de precios | lista por empresa con moneda, modo tributario, orden y estado |
| CC-UC-11 | administrar entrada de precio | importe y rango efectivo sin ambigüedad para el mismo alcance |
| CC-UC-12 | inactivar/reactivar | impedir nuevas selecciones sin borrar historia |
| CC-UC-13 | reemplazar ítem | vínculo sin ciclos que no reescribe referencias previas |
| CC-UC-14 | resolver referencia pública | vista mínima estable por ID y empresa |
| CC-UC-15 | obtener cotización de catálogo | resultado determinista con lista, moneda, vigencia y versión usadas |

Importación masiva, promoción, costo, existencia, reserva y generación de variantes
en matriz quedan fuera de estos casos iniciales.

## Invariantes candidatos

| ID | Invariante |
|---|---|
| CC-I01 | toda entidad privada operativa pertenece a una empresa no nula |
| CC-I02 | el ID técnico es opaco, estable y nunca se reutiliza |
| CC-I03 | el código normalizado del ítem es único por empresa |
| CC-I04 | el tipo producto/servicio no cambia después del alta |
| CC-I05 | un ítem debe estar habilitado para compra, venta o ambos |
| CC-I06 | una unidad base válida es obligatoria y su conversión implícita es 1 |
| CC-I07 | toda conversión es positiva y una unidad aparece una sola vez por ítem |
| CC-I08 | existe como máximo un default activo por ítem y propósito de unidad |
| CC-I09 | códigos alternativos normalizados no son ambiguos dentro de una empresa |
| CC-I10 | categorías, marcas, listas y perfiles referenciados pertenecen a la misma empresa |
| CC-I11 | una lista fija su moneda y modo de impuesto; una entrada no puede contradecirlos |
| CC-I12 | para igual lista, ítem, cantidad y alcance no existen vigencias activas superpuestas |
| CC-I13 | importes se representan con `BigDecimal`, nunca `double` o `float` |
| CC-I14 | inactivar o reemplazar no elimina precios, asignaciones ni referencias históricas |
| CC-I15 | un reemplazo no puede apuntarse a sí mismo ni formar ciclos |
| CC-I16 | cada mutación requiere versión esperada y registra empresa, actor, operación y correlación |
| CC-I17 | un plugin inactivo no aporta menú, consulta ni comando funcional a esa empresa |
| CC-I18 | contratos públicos no exponen entidades, nombres de tablas ni DTO internos |

## Datos que consumidores deberán preservar como snapshot

El catálogo mantiene datos vigentes; no reescribe documentos, pedidos o
movimientos ya emitidos. Cada consumidor es propietario de su snapshot.

| Consumidor | Snapshot mínimo recomendado |
|---|---|
| ventas/compras | ID, código, descripción, tipo, unidad presentada, factor usado y versión de referencia |
| documento comercial | además precio, moneda, descuento, modo de impuesto, perfil/tasas y descripciones impresas |
| inventario | ID del ítem, unidad base, unidad operativa y factor efectivo del movimiento |
| contabilidad | descripción e imputación resueltas al contabilizar; la cuenta no pertenece al catálogo |
| auditoría de precio | lista, versión, entrada, moneda, importe, cantidad, vigencia y momento de resolución |

Una actualización de nombre, marca, categoría, tasa o precio nunca modifica esos
snapshots. El perfil fiscal externo y la representación SIFEN se agregan por el
plugin fiscal correspondiente.

## Decisiones para confirmación de producto

### CC-D01 - Producto y servicio

Alternativas:

1. agregados y pantallas separados;
2. agregado común con tipo obligatorio e inmutable;
3. agregado común con flags combinables.

**Recomendación:** alternativa 2. `CatalogItem` comparte identidad, descripción,
unidad, impuestos y precios; `PRODUCT` o `SERVICE` es estable. Compra/venta son
alcances independientes. Inventario decide después si un producto administra
stock; no se deduce únicamente del tipo.

Estado: **aceptada sin cambios el 2026-07-30**.

### CC-D02 - Códigos e identificadores escaneables

Alternativas:

1. un código manual y un solo código de barras;
2. código empresarial obligatorio más múltiples identificadores tipados;
3. usar código de barras como identidad principal.

**Recomendación:** alternativa 2. ID técnico opaco; código legible obligatorio,
manual o generado con secuencia transaccional; múltiples identificadores
opcionales con tipo y valor normalizado. Nunca `MAX + 1` ni barcode como PK.

Estado: **aceptada sin cambios el 2026-07-30**.

### CC-D03 - Variantes y atributos

Alternativas:

1. excluir variantes del primer modelo;
2. implementar inmediatamente una matriz completa de combinaciones;
3. incluir fundamento relacional de familia, definición tipada y asignación, sin
   generador masivo en el primer corte.

**Recomendación:** alternativa 3. Permite productos simples y variantes con SKU
propio, evita copiar talle/color/modelo como columnas y limita los atributos al
subdominio de variantes; la generación cartesiana queda diferida.

Estado: **aceptada sin cambios el 2026-07-30**.

### CC-D04 - Unidades, conversiones y precisión

Alternativas:

1. una sola unidad sin conversiones;
2. conversiones genéricas globales;
3. unidad base por ítem y conversiones específicas por ítem/purpose.

**Recomendación:** alternativa 3. Unidad base obligatoria; factor positivo con
precisión decimal explícita; defaults separados para compra y venta/consumo; cada
operación conserva el factor aplicado. Las unidades tienen códigos estables y son
administradas dentro del plugin por empresa.

Estado: **aceptada sin cambios el 2026-07-30**.

### CC-D05 - Clasificación tributaria y catálogos fiscales

Alternativas:

1. guardar directamente códigos y tasas SIFEN en el ítem;
2. guardar solo una tasa numérica actual;
3. perfil tributario interno versionado con mapeos fiscales externos separados.

**Recomendación:** alternativa 3. El catálogo asigna un perfil efectivo interno;
el plugin fiscal mapea ese perfil a la versión oficial aplicable. Documentos
conservan snapshot de tasas y códigos usados. No se certifica SIFEN en Sprint 7.

Estado: **aceptada sin cambios el 2026-07-30**.

### CC-D06 - Propiedad y vigencia de precios

Alternativas:

1. precio directo fijo dentro del ítem;
2. listas y entradas efectivas propiedad de `commercial_catalog`;
3. extraer desde ahora un plugin `pricing` independiente.

**Recomendación:** alternativa 2 para el corte inicial. Catálogo posee listas y
precios efectivos; ventas posee asignación a clientes, condiciones, promociones y
prioridad. El contrato permite separar `pricing` más adelante sin compartir JPA.

Estado: **aceptada sin cambios el 2026-07-30**.

### CC-D07 - Moneda, redondeo e impuestos incluidos

Alternativas:

1. heredar moneda y redondeo de una configuración implícita de empresa;
2. fijar moneda ISO, modo de impuesto y redondeo en cada lista;
3. permitir que cada fila de precio mezcle políticas.

**Recomendación:** alternativa 2. Cada lista declara moneda ISO 4217, `NET` o
`TAX_INCLUDED`, escala y regla de redondeo. Las entradas contienen importes
decimales no negativos y no contradicen la lista.

Estado: **aceptada sin cambios el 2026-07-30**.

### CC-D08 - Categorías, marcas y etiquetas

Alternativas:

1. conservar familia/grupo/línea como tres jerarquías rígidas;
2. categoría jerárquica controlada, marca independiente y etiquetas controladas;
3. texto libre sin catálogos.

**Recomendación:** alternativa 2. Un ítem tiene una categoría principal y puede
tener categorías secundarias; marca es opcional; etiquetas son valores controlados
por empresa. No se copian campos sectoriales al catálogo base.

Estado: **aceptada sin cambios el 2026-07-30**.

### CC-D09 - Inactivación, reemplazo e historia

Alternativas:

1. baja física si no existen referencias conocidas;
2. activo/inactivo sin relación de reemplazo;
3. inactivación normal y reemplazo opcional con control de ciclos.

**Recomendación:** alternativa 3. La baja física no es operación normal. Un
reemplazo orienta nuevas selecciones, no cambia documentos ni movimientos previos.
La reactivación exige que códigos e identificadores sigan libres.

Estado: **aceptada sin cambios el 2026-07-30**.

### CC-D10 - Contratos públicos y snapshots

Alternativas:

1. exponer entidades/DTO completos y que consumidores consulten siempre el dato
   vigente;
2. contratos pequeños por propósito y snapshots persistidos por cada consumidor;
3. duplicar el catálogo completo en cada plugin.

**Recomendación:** alternativa 2. API pública versionada para búsqueda, resolución
por ID, referencia mínima, conversiones y cotización. Cada consumidor persiste el
snapshot que da sentido histórico a su operación; no existen relaciones JPA entre
plugins.

Estado: **aceptada sin cambios el 2026-07-30**.

## Riesgos y controles

| Riesgo | Control propuesto |
|---|---|
| recrear la entidad gigante | agregado pequeño y contratos por propósito |
| convertir catálogo en inventario | inventario fuera del esquema y de la API de escritura |
| ambigüedad de precio | política en lista, vigencia no superpuesta y resultado versionado |
| inconsistencia multiempresa | empresa obligatoria en aplicación, repositorio y restricciones |
| códigos concurrentes | secuencia transaccional y restricción única normalizada |
| atributos libres sin gobierno | definiciones tipadas y acotadas a variantes |
| tasa fiscal desactualizada | perfil efectivo y mapeo fiscal versionado separado |
| romper documentos al editar maestros | snapshots propiedad del consumidor |
| variantes explosivas | sin generador cartesiano en el primer corte |
| migración sucia del legado | proyecto de migración posterior con staging, mapeo y conciliación |

## Migración futura

Sprint 7 no migra datos. Una migración futura deberá, como mínimo:

- identificar duplicados por empresa, código y barcode;
- separar conceptos físicos, servicios y alcances compra/venta;
- mapear estados heterogéneos;
- convertir precios 1/2/3 y `StwPreciosFijos` a listas efectivas sin perder
  vigencia, moneda ni empresa;
- conciliar unidades y factores;
- convertir familia/grupo/línea/marca a definiciones aceptadas;
- separar tasas internas de códigos fiscales externos;
- conservar una tabla de correspondencia `legacy_id` → ID público;
- producir conteos, errores y checksum de entrada/salida;
- ejecutarse fuera de las tablas privadas del legado y sin modificarlas.

## Condición para avanzar

El responsable de producto confirmó CC-D01 a CC-D10 sin cambios el 2026-07-30 y
aclaró que `commercial_catalog` debe ser otro módulo funcional. Esto autoriza
`J11-S7-02`: módulos Maven separados para API pública e implementación de dominio.
Persistencia, migraciones, aplicación y UI continúan reservadas a sus historias.
