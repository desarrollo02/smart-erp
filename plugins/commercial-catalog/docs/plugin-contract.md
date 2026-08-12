# Contrato del plugin `commercial_catalog`

- Plugin: `commercial_catalog@1.1.0`
- Contrato público: `commercial-catalog-api@1.1.0`
- Compatibilidad Plugin API: `[0.4.0,0.5.0)`
- Estado del corte: dominio, persistencia, aplicación e interfaz neutral implementados; J11-S8-C03 resuelve moneda mediante `reference_data`; los gates finales del corte normativo permanecen pendientes

## Separación física

`commercial-catalog-api` es el único módulo que otros plugins pueden consumir.
Usa Java estándar y `CompanyId`; no publica entidades, repositorios, tablas,
adaptadores ni objetos internos. `commercial-catalog` implementa dominio y
persistencia y no puede ser una dependencia de otro plugin.

El descriptor requiere `reference_data [1.0.0,2.0.0)` y este módulo consume sólo
`reference-data-api`. No importa implementación, entidades ni tablas del proveedor.

El descriptor CDI/SPI declara la migración propietaria `plg_commercial_catalog`,
cuatro permisos operativos, cinco capacidades, cinco menús y cinco pantallas interactivas.
No declara overlays ni eventos. Las contribuciones visuales son contratos
neutrales: el plugin no incluye XHTML, CSS, JavaScript ni EL.

## Interfaz neutral

- `commercial_catalog:items`, ruta `/catalog`: directorio, alta y ficha de
  artículos/servicios;
- `commercial_catalog:price_lists`, ruta `/catalog/price-lists`: directorio, alta
  y ficha de listas de precios;
- `commercial_catalog:tax_profiles`, ruta `/catalog/tax-profiles`: directorio,
  alta y consulta de perfiles tributarios internos;
- `commercial_catalog:definitions`, ruta `/catalog/definitions`: directorio
  unificado, filtro, alta e inactivación/reactivación de unidades, categorías,
  marcas y etiquetas;
- `commercial_catalog:variant_families`, ruta `/catalog/variant-families`:
  directorio, alta guiada y detalle de familias con atributos ordenados;
- artículos y listas requieren `commercial_catalog.view`; perfiles tributarios
  y definiciones requieren `commercial_catalog.definitions.manage`; los menús sólo
  se aportan cuando el plugin está presente, activo para la empresa y autorizado
  para el usuario;
- las mutaciones de artículos exigen `commercial_catalog.items.manage` y las de
  listas/entradas exigen `commercial_catalog.prices.manage`;
- las definiciones activas autorizadas alimentan selectores; J11-S8-C01 expone
  perfiles tributarios y J11-S8-C02 expone unidades, categorías, marcas y
  etiquetas y familias de variantes bajo
  `commercial_catalog.definitions.manage`; el editor visual admite de uno a ocho
  atributos por alta, con código, nombre, tipo, obligatoriedad y posición;
- las fichas usan versión optimista y vuelven a consultar empresa, recurso,
  permiso y estado en el servidor después de cada acción;
- cada pantalla publica únicamente `directory_extensions` y `detail_extensions`
  para personalizaciones tipadas y compatibles con el contrato `1.0.0`.

El shell es dueño del floorplan JSF, Material Design 3, responsive, accesibilidad,
textos aceptados y renderers. Un contrato, región, pestaña, acción o handler
desconocidos se rechazan. La composición física existente ya incluye el plugin.
Las rutas de definiciones y familias, junto con los accesos contextuales de sus
ocho selectores, deben superar composición y Playwright antes de recongelar el
WAR. Las cuatro definiciones simples cambian entre `ACTIVE` e `INACTIVE` mediante
versión esperada, empresa autenticada y auditoría técnica; una definición
inactiva deja de alimentar operaciones nuevas sin borrar su identidad ni sus
referencias históricas. El corte no implica todavía edición de nombres o
estructura, reemplazo ni retorno seguro con borrador.

## Superficie pública 1.1.0

- `CatalogItemDirectory`: referencia mínima y búsqueda paginada por empresa,
  tipo, estado y alcance comercial; el repositorio aplica todos los filtros antes
  de calcular el total y la página;
- `CatalogUnitConversions`: conversión determinista específica por ítem;
- `CatalogPricing`: cotización sobre una lista indicada explícitamente;
- IDs UUID opacos para ítem, lista y entrada de precio;
- tipos, alcances, estados, moneda, modo tributario, vigencia, factor e importes
  necesarios para que el consumidor conserve su propio snapshot.

La cotización no selecciona automáticamente una lista, cliente, promoción o
condición comercial. El consumidor elige `PriceListId` y conserva el resultado
efectivo; no mantiene una relación JPA hacia el catálogo.

## Reglas del dominio

- producto y servicio comparten `CatalogItem`; el tipo no cambia tras el alta;
- un ítem habilita compra, venta o ambos alcances;
- código e identificadores no reemplazan al UUID técnico;
- la unidad base tiene factor implícito `1`; las demás conversiones son por ítem;
- existe como máximo un default activo por finalidad de unidad;
- clasificación, perfil tributario interno y variante son asignaciones tipadas;
- el perfil tributario no contiene XML, XSD ni códigos SIFEN;
- ítems, identificadores, listas, entradas y definiciones simples se inactivan sin
  borrado físico;
- las mutaciones usan versión esperada y rechazan sobrescritura obsoleta;
- una lista fija una moneda habilitada por `reference_data`, modo
  `NET`/`TAX_INCLUDED`, escala y redondeo; el servidor revalida empresa/código en
  la transacción;
- una misma combinación ítem/unidad/cantidad mínima no admite vigencias activas
  solapadas; los tramos de cantidad distintos pueden coexistir.

La V1 y sus repositorios materializan unicidad empresarial, referencias privadas,
vigencias y control optimista sin cambiar los límites públicos por comodidad de
JPA. La aplicación gobierna comandos, autorización y ciclos del grafo de
reemplazos.

## Aplicación y seguridad

- `commercial_catalog.view`: búsqueda, detalle, definiciones disponibles,
  conversión y cotización;
- `commercial_catalog.items.manage`: alta, identidad, identificadores, unidades,
  clasificación, perfil, variante y ciclo de vida del ítem;
- `commercial_catalog.prices.manage`: listas, entradas de precio y sus estados;
- `commercial_catalog.definitions.manage`: alta y ciclo activo/inactivo de
  unidades, categorías, marcas y etiquetas; perfiles tributarios y familias de
  variantes.

Cada interacción interna recibe `CatalogOperationContext`, derivado de una
`AuthorizedCompanyOperation` actual. El servicio exige el `PluginId`, permiso y
empresa confiables antes de I/O. Los comandos usan versiones esperadas y convierten
conflictos a `CatalogResultCode`; las altas automáticas usan la secuencia por empresa.

`CommercialCatalogUseCases` es la fachada interna CDI/JTA. Las mutaciones y su
auditoría técnica comparten transacción; las consultas usan `SUPPORTS`. La auditoría
incluye actor, empresa, plugin, permiso, operación, ID técnico, versiones, resultado
y correlación, pero nunca nombre, descripción, identificador comercial, importe,
tasa o valor de variante.

Los adaptadores de `CatalogItemDirectory`, `CatalogUnitConversions` y
`CatalogPricing` no representan una autorización de usuario. Un plugin consumidor
debe proteger primero su propio caso de uso y conservar snapshots; una futura UI
obtendrá la prueba exacta del kernel para cada interacción.

## Persistencia privada

- esquema y migración: `plg_commercial_catalog` V1, veinte tablas;
- unidad JPA: `logixone-commercial-catalog-pu`, JTA sobre el datasource común,
  `validate` y generación de DDL deshabilitada;
- raíces: ítem y lista con `@Version`; detalles históricos sin borrado físico;
- vigencias tributarias y de precio protegidas en PostgreSQL, incluida concurrencia;
- secuencia de códigos atómica por `(company_id, sequence_scope)`, tolerante a huecos;
- todos los repositorios reciben o preservan `CompanyId`; no existen entidades,
  FKs ni joins hacia otro propietario.

## Fuera del propietario

Existencias, depósitos, costos, pedidos, facturas, promociones, crédito,
contabilidad y artefactos SIFEN pertenecen a otros plugins. Ninguno puede leer las
tablas privadas futuras de `commercial_catalog`.

## Compatibilidad

Los cambios aditivos compatibles conservan la versión mayor. Quitar o reinterpretar
campos, cambiar semántica de factores/vigencias o ampliar datos sensibles exige
evaluación de compatibilidad y una nueva versión semántica. No se filtran clases
internas para evitar versionar el contrato.

## Pruebas

```powershell
.\mvnw.cmd -B -pl plugins/commercial-catalog-api -am test
.\mvnw.cmd -B -pl plugins/commercial-catalog -am test
.\mvnw.cmd -B -pl plugins/commercial-catalog -am verify `
  -Dlogixone.postgres.integration=true
.\mvnw.cmd -B -pl tests/architecture-tests -am test
```
