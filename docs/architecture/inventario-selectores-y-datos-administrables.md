# Inventario de selectores y datos administrables

- Versión: 18
- Fecha: 2026-08-04
- Baseline examinado: corte ejecutable J11-S8-C03 posterior a J11-S8-C02; Sprint 8 abierto
- Decisión: [ADR-0028](../adr/0028-gobierno-de-selectores-y-datos-administrables.md)
- Alcance: código fuente ejecutable; no cuenta `target/` ni mocks de prueba

## Resultado ejecutivo

El baseline declara 91 selectores lógicos:

| Superficie | Cantidad | Resultado |
|---|---:|---|
| shell y administración del kernel | 18 | 18 declarados con propietario de plataforma, origen visible y ruta autorizada cuando corresponde |
| `business_partners` | 10 | 10 declarados; país consume `reference_data`; tipos de identificación, tipos/propósitos de dirección y tipos de canal comparten consulta/alta, revisión de nombre, historial append-only y ciclo activo-inactivo empresarial |
| `commercial_catalog` | 36 | 36 declarados; moneda consume `reference_data`; unidades, categorías, marcas y etiquetas tienen consulta/alta/revisión/historial/ciclo activo-inactivo/reemplazo seguro; familias tienen consulta/alta/revisión completa/historial/ciclo activo-inactivo y asignación versionada a artículos |
| `inventory` | 27 | 27 declarados; accesos contextuales autorizados implementados |
| fixtures de referencia/personalización | 0 | sin selector funcional |

`view.xhtml` contiene tres renderers físicos (`directory`, `create` y `detail`),
pero no son tres fuentes adicionales: materializan los 73 campos declarados por
los plugins.

`plugin-api` 0.4.2 y el shell materializan un mismo patrón neutral sin fingir que
el kernel es un plugin. Los 73 selectores de plugins publican
`SelectorSourceDefinition`; los 18 nativos publican
`PlatformSelectorSourceDefinition`. El inventario lógico y la cobertura
contractual quedan en 91 de 91, validados por pruebas de contrato, catálogo,
autorización y recursos Faces.

El renderer genérico de plugins ya implementa retorno contextual: conserva por
POST sólo inputs renderizados/permitidos, guarda un contexto efímero de un uso
ligado a sesión, usuario y empresa, reautoriza ambos extremos y refresca opciones
al volver. El shell aplica ahora el equivalente a los 11 usos nativos
administrables mediante planes cerrados de origen, destino e inputs; los otros
siete son estados cerrados o composición/despliegue y no crean contexto.

## Criterio de clasificación

| Clase | ¿Se agregan valores en runtime? | Administración esperada |
|---|---|---|
| estado cerrado | no | cambio versionado de dominio, migración y pruebas |
| catálogo empresarial | sí | alta, consulta, edición permitida e inactivación |
| referencia operativa | mediante su entidad propietaria | enlace al administrador dueño |
| catálogo normativo | no arbitrariamente | versión oficial y habilitación controlada |
| composición/despliegue | no desde la aplicación | reconstrucción y redespliegue documentados |

## Kernel y shell

| Selector o grupo | Fuente | Clase | Administración actual | Brecha |
|---|---|---|---|---|
| empresa activa, empresa de plugins y empresa de seguridad | `core.company` y membresías | referencia operativa | Empresas y Seguridad | metadato, enlace y retorno seguro visibles sólo con `kernel.company.manage` |
| personalización al registrar/reemplazar | plugins `CUSTOMIZATION` físicamente presentes y libres | despliegue | build/redespliegue; la UI lo explica | metadato de despliegue; sin alta dinámica |
| usuario para membresía o rol | usuarios locales | referencia operativa | Seguridad registra y activa/inactiva usuarios | metadato y enlace a Seguridad visibles con `kernel.security.manage` |
| membresía para asignación | membresías empresariales | referencia operativa | Seguridad registra y activa/inactiva membresías | metadato, enlace y retorno seguro autorizados |
| rol empresarial/global | roles del ámbito correspondiente | catálogo administrado | Seguridad/Autoridad global crean e inactivan | metadato y enlace según permiso global correspondiente |
| permiso empresarial/global | descriptor de plugin o catálogo del kernel | composición/estado cerrado | se incorpora con código/plugin y se concede o revoca | metadato cerrado/de despliegue; no permite inventar IDs |
| categoría, resultado y ventana de auditoría | enums de consulta | estado cerrado | versión de aplicación | metadato cerrado y origen visible; sin administración arbitraria |

Conclusión: no hay un catálogo empresarial huérfano en el kernel. La mejora es de
descubribilidad y metadatos, no crear una tabla genérica de opciones.

## `reference_data`

`reference_data` es un plugin funcional compartido, no una responsabilidad del
kernel. Su API pública Java pura expone publicaciones, países y monedas por
`CompanyId`; su esquema privado conserva procedencia, hash, completitud y políticas
de habilitación. La ruta `/reference-data` es de sólo lectura y exige
`reference_data.view`.

El corte inicial está marcado `BOOTSTRAP_SUBSET` y contiene `PY`, `PYG` y `USD`.
No admite altas arbitrarias ni acceso a Internet en runtime. Las listas completas,
reconciliación, retirados, administración de políticas y paginación siguen en la
épica normativa.

## `business_partners`

| Campo(s) | Opciones/fuente | Clase | Estado |
|---|---|---|---|
| `search_role` | cliente, proveedor | estado cerrado del rol | correcto |
| `search_state` | activo, inactivo | estado cerrado | correcto |
| `new_kind` | organización, persona física | tipo estructural cerrado | correcto |
| `identification_country` | países habilitados de `reference-data-api` | catálogo normativo | selector; revalidación por empresa/código dentro de la transacción |
| `identification_type` | `business_partner_definition` y revisión, clase `IDENTIFICATION_TYPE` | catálogo empresarial | administrable en **Definiciones de socios**; sólo activos en identificaciones nuevas y referencia revalidada en servidor |
| `address_type` | `business_partner_definition` y revisión, clase `ADDRESS_TYPE` | catálogo empresarial | administrable en **Definiciones de socios**; sólo activos en direcciones nuevas y referencia revalidada en servidor |
| `address_purpose` | `business_partner_definition` y revisión, clase `ADDRESS_PURPOSE` | catálogo empresarial | administrable en **Definiciones de socios**; sólo activos en direcciones nuevas y referencia revalidada en servidor |
| `channel_kind` | `business_partner_definition` y revisión, clase `CHANNEL_KIND` | catálogo empresarial | administrable en **Definiciones de socios**; sólo opciones activas para usos nuevos |
| `definition_search_kind`, `definition_new_kind` | las cuatro clases propietarias | estado cerrado de la pantalla | filtro y alta tipados; la identidad de detalle conserva `clase:código` y no admite clases arbitrarias |

V4 amplía el maestro privado sin agregar tablas, retroalimenta los códigos ya
persistidos, crea revisiones append-only y siembra valores mínimos para empresas
existentes. País permanece fuera de este maestro empresarial y pertenece a
`reference_data`.

## `commercial_catalog`

| Campos | Fuente | Clase | Administración actual | Resultado |
|---|---|---|---|---|
| `item_search_type`, `item_new_type` | producto/servicio | estado cerrado | versión de dominio | correcto |
| `item_search_state`, `price_search_state`, `tax_profile_search_state`, `definition_search_state` | activo/inactivo | estado cerrado | ciclo de vida propietario | correcto |
| `price_currency` | monedas habilitadas de `reference-data-api` | catálogo normativo | selector; revalidación por empresa/código dentro de la transacción | correcto para `PYG/USD` del subconjunto inicial |
| `item_new_scope`, `item_edit_scope`, `conversion_purpose` | compra/venta/ambos | estado cerrado | versión de dominio | correcto |
| `item_new_base_unit`, `conversion_unit`, `price_entry_unit` | `unit_definition` y `unit_definition_revision` | catálogo empresarial | pantalla Definiciones permite consultar, registrar, revisar nombre/decimales, ver historial, inactivar/reactivar, reemplazar y retornar con borrador seguro | correcto para el alcance vigente |
| `main_category` | `category_definition` y `category_definition_revision` | catálogo empresarial | pantalla Definiciones permite consultar, registrar, revisar nombre/padre, ver historial, inactivar/reactivar y reemplazar | correcto para el alcance vigente |
| `brand` | `brand_definition` y `brand_definition_revision` | catálogo empresarial | pantalla Definiciones permite consultar, registrar, revisar nombre, ver historial, inactivar/reactivar y reemplazar | correcto para el alcance vigente |
| `item_new_tax_profile`, `item_tax_profile` | `tax_profile_definition/revision` | catálogo empresarial versionado | pantalla permite registrar, consultar, revisar contenido/vigencia, ver historial, inactivar/reactivar y retornar con borrador seguro | correcto para el alcance vigente |
| `definition_search_kind`, `definition_new_kind` | unidad/categoría/marca/etiqueta | estado cerrado de la pantalla | versión del contrato visual | correcto |
| `definition_unit_scale`, `definition_revision_unit_scale`, `definition_replacement_unit_scale` | 0 a 12 | restricción numérica cerrada | versión del dominio | correcto |
| `definition_category_parent`, `definition_revision_category_parent`, `definition_replacement_category_parent` | categorías activas | referencia operativa local | misma pantalla Definiciones; revisión y reemplazo excluyen la propia categoría | correcto |
| `variant_family_search_state` | activo/inactivo | estado cerrado | ciclo de vida propietario | correcto |
| `variant_attribute_type`, `variant_attribute_required` | texto/número/Sí-No y obligatorio/opcional | estados cerrados del atributo | versión del dominio | correcto |
| `variant_revision_attribute_type`, `variant_revision_attribute_required` | texto/número/Sí-No y obligatorio/opcional | estados cerrados del atributo revisado | versión del dominio | correcto |
| `item_variant_family` | familias activas con identidad y versión vigentes | catálogo empresarial versionado | Artículos y servicios enlaza a Familias de variantes, conserva retorno seguro y valida empresa/estado/revisión en el servidor | correcto para asignaciones nuevas; el detalle conserva familia, revisión y valores históricos |
| `price_tax_mode` | neto/impuesto incluido | política cerrada | versión de dominio | correcto |
| `price_scale` | 0 a 6 | restricción numérica cerrada | versión de dominio | correcto |
| `price_rounding_mode` | modos de redondeo admitidos | política cerrada | versión de dominio | correcto |
| `price_entry_item` | artículos activos | referencia operativa | pantalla Artículos y servicios | enlace contextual y retorno genérico implementados |
| `price_entry_to_inactivate` | entradas de la lista seleccionada | referencia operativa local | misma pantalla | correcto |

Las etiquetas comparten el mismo ciclo activo/inactivo de las demás definiciones
simples. El cambio usa empresa, identidad y versión esperada; no borra físicamente
ni reescribe referencias históricas. Las familias de variantes se consultan y
registran desde su pantalla propia. El
editor conserva un borrador de 1 a 8 atributos, rechaza códigos duplicados y
persiste tipo, obligatoriedad y posición mediante el caso de uso JTA existente.
Las definiciones simples ya permiten revisión explícita e historia visual
append-only mediante V2, sin cambiar código ni identidad. V3 agrega un vínculo privado
de reemplazo de la identidad anterior a una sucesora nueva del mismo tipo y empresa.
La anterior queda inactiva e inmutable, las referencias existentes continúan apuntando
a ella y sólo las operaciones futuras pueden elegir la sucesora. V4 agrega revisiones
append-only completas de familias: nombre, estado y estructura ordenada de 1 a 8
atributos se preservan por versión. Las asignaciones existentes guardan la versión
de familia original para que una revisión posterior no cambie su significado.
La pestaña **Variantes** de Artículos y servicios ofrece únicamente familias
activas de la empresa, muestra la estructura esperada y recibe valores con la
forma `CODIGO=valor; OTRO=valor`. La aplicación vuelve a resolver y bloquear la
familia vigente dentro de la transacción, exige la versión enviada y valida
atributos declarados, obligatorios y tipos antes de persistir. Una revisión o
inactivación posterior deja la asignación histórica legible, pero no disponible
para operaciones nuevas. Perfiles tributarios ya tienen revisión explícita,
historial y ciclo activo/inactivo.

`identifier_type` del identificador alternativo de artículo permanece como texto
y requiere caracterización y un catálogo controlado antes de ampliar ese alcance.

Remediación restante: definir tipos de identificador. El
retorno de plugins y de los 11 usos nativos administrables ya refresca opciones.

## `inventory`

| Grupo de campos | Fuente | Clase | Administración | Resultado |
|---|---|---|---|---|
| estados de depósito/artículo/conteo | ciclo de vida | estado cerrado | acciones del dominio | correcto |
| tipo de ubicación | almacenamiento/recepción/despacho | estado cerrado | versión de dominio | correcto |
| tracking y vencimiento | políticas del artículo inventariable | estado cerrado | inscripción del artículo | correcto |
| tipo de movimiento | entrada/salida/transferencia | estado cerrado | versión de dominio | correcto |
| condición de stock | disponible/cuarentena/dañado | estado cerrado con semántica contable/operativa | versión de dominio | correcto por ahora; ampliar sólo con ADR |
| depósito y ubicación | maestros de inventario | catálogo empresarial/referencia | pantalla Depósitos crea y administra | enlace contextual autorizado implementado |
| artículo de catálogo | artículos activos | referencia de otro plugin | pantalla Artículos y servicios | enlace contextual autorizado implementado |
| artículo inventariable | inscripciones locales | referencia operativa | pantalla Existencias | correcto |
| línea de conteo | líneas del conteo seleccionado | referencia operativa local | misma pantalla Conteos | correcto |

No se debe permitir crear una condición, estado o tipo de movimiento directamente
desde el selector: esos valores cambian invariantes, disponibilidad y contabilización.

## Plugins planificados

Aún no existen campos ni contratos ejecutables para los plugins futuros. Sus
caracterizaciones deberán producir un diccionario de datos y selectores antes del
diseño de pantalla.

| Plugin | Catálogos/referencias previsibles que deben tener propietario |
|---|---|
| `purchasing` | proveedor, depósito, moneda, condición de compra, motivo de devolución |
| `sales` | cliente, lista de precios, vendedor, moneda, condición de venta, motivo de anulación |
| `logistics` | depósito, transportista, vehículo, ruta, motivo y estado de entrega |
| `vehicle_telemetry` | vehículo de logística, dispositivo, conexión/proveedor, estado cerrado de seguimiento, geocerca y regla de alerta |
| `commercial_documents` | tipo, serie, establecimiento, moneda, condición, referencias y motivos |
| `sifen` | catálogos oficiales versionados; nunca alta arbitraria de códigos fiscales |
| `treasury` | caja, cuenta bancaria, moneda, medio de pago y motivo de ajuste |
| `point_of_sale` | terminal, cajero, lista, cliente, medio de pago y documento |
| `accounts_receivable` | cliente, condición, cuota, cobrador, motivo y estado cerrado |
| `accounts_payable` | proveedor, condición, cuota, pagador, motivo y estado cerrado |
| `accounting` | cuenta, diario, centro de costo, período, moneda y tipo de asiento |
| personalización | consume fuentes públicas; no crea listas paralelas silenciosas |

Esta tabla no fija modelos ni dependencias: es una lista de preguntas obligatorias
para cada caracterización.

## Brechas y orden recomendado

1. importar y reconciliar publicaciones completas sin confundirlas con el
   subconjunto `PY/PYG/USD`;
2. completar casos de uso/auditoría de políticas por empresa;
3. decidir el umbral de búsqueda/paginación para listas normativas y empresariales grandes;
4. repetir Docker/Playwright, la demo acumulada y los gates de recongelación antes de iniciar
   `purchasing`.

## Criterio de salida

No queda una brecha resuelta por documentar un botón futuro. El gate termina cuando
la ruta existe, aplica autorización en servidor, conserva historia, actualiza el
selector al volver y tiene evidencia Playwright. País y moneda están implementados
en código y pruebas focalizadas; permanecen **pendientes de gate visual y
recongelación** hasta completar RD-06.
