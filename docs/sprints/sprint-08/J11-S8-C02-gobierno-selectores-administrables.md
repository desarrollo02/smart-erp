# J11-S8-C02 - Gobierno de selectores y datos administrables

- Estado: Corte C02 implementado y validado en 89/89; fuentes normativas transferidas a J11-S8-C03; paginación y recongelación pendientes
- Sprint: 8, corrección posterior a J11-S8-C01
- Fecha de planificación: 2026-08-01
- Decisión: [ADR-0028](../../adr/0028-gobierno-de-selectores-y-datos-administrables.md)
- Inventario: [selectores y fuentes](../../architecture/inventario-selectores-y-datos-administrables.md)

## Objetivo

Eliminar los catálogos empresariales huérfanos del baseline visible y establecer el
patrón neutral que usarán todos los plugins futuros, antes de recongelar Sprint 8 o
iniciar `purchasing`.

## Cortes coherentes

1. decidir versión compatible del contrato de selector, retorno y borrador;
2. decidir propietario de países y monedas normativos;
3. implementar metadatos neutrales y renderer autorizado del shell;
4. completar definiciones de `commercial_catalog`: unidades, categorías, marcas,
   etiquetas, variantes y ciclo de perfiles tributarios;
5. implementar definiciones de `business_partners`: identificaciones, direcciones,
   canales y propósitos;
6. enlazar administradores existentes de empresa, usuario, rol, artículo, depósito
   y ubicación;
7. ejecutar pruebas acumuladas, demo, documentación y recongelación.

Cada corte se implementará y probará por separado. Este documento no autoriza una
tabla genérica compartida ni convierte el kernel en dueño de maestros empresariales.

## Primer corte ejecutable 2026-08-01

El primer corte elevó `PluginApiVersion.CURRENT` de `0.4.0` a `0.4.1` de forma
aditiva. `ScreenInteraction.Handler.selectorSources()` conserva un valor
predeterminado vacío para compatibilidad y permite declarar, por campo `SELECT`,
fuente estable, propietario, clase, versión, ruta, permiso, capacidades, vacío,
inactivos y estrategia de carga.

El shell valida que una declaración corresponda a un selector real y sólo entrega
la ruta a JSF cuando la ruta ya pertenece al menú autorizado y el servidor vuelve
a comprobar el permiso del plugin propietario. Los tres renderers físicos muestran
**Administrar** o **Agregar o administrar** mediante el mismo patrón responsive.
Una denegación no filtra la ruta al modelo JSF y la navegación de destino conserva
su guarda normal.

Estado medido de los 51 selectores de plugins:

| Plugin | Declarados | Total | Pendiente real |
|---|---:|---:|---|
| `business_partners` | 3 | 4 | `channel_kind` hasta crear su maestro |
| `commercial_catalog` | 15 | 20 | cinco usos de unidad/categoría/marca sin ruta completa |
| `inventory` | 27 | 27 | ninguno en el contrato; falta validación visual del atajo |
| **Total** | **45** | **51** | **6** |

No se clasificaron falsamente esos seis campos como cerrados ni se enlazaron a una
pantalla que no administra su fuente. Los 18 selectores nativos, el retorno con
borrador seguro, el ciclo completo de maestros, países/monedas y Playwright siguen
pendientes, por lo que esta historia y Sprint 8 permanecen abiertos.

## Segundo corte ejecutable 2026-08-01

`commercial_catalog` aporta ahora la ruta autorizada `/catalog/definitions`, una
pantalla unificada de **Definiciones del catálogo** y un handler que consulta y
registra unidades, categorías y marcas mediante los casos de uso JTA existentes.
La pantalla permite filtrar por texto, tipo y estado; la unidad declara escala de
decimales y la categoría puede referenciar una categoría padre activa. No agrega
tablas, SQL cruzado ni una lista genérica propiedad del kernel.

Los cinco consumidores antes huérfanos —unidad base, unidad de conversión,
categoría principal, marca y unidad de precio— declaran ahora la ruta y el permiso
`commercial_catalog.definitions.manage`. El estado medido actual es:

| Plugin | Declarados | Total | Pendiente real |
|---|---:|---:|---|
| `business_partners` | 3 | 4 | `channel_kind` hasta crear su maestro |
| `commercial_catalog` | 25 | 25 | ninguno sin fuente; faltan ciclos visuales completos |
| `inventory` | 27 | 27 | ninguno en el contrato; falta validación visual acumulada |
| **Total** | **55** | **56** | **1** |

La pantalla de definiciones aporta cinco selectores propios gobernados, por lo que el
inventario lógico total pasó de 69 a 74: 18 nativos y 56 de plugins. Hay 73 de 74
con fuente y propietario caracterizados, y 55 de 74 declarados mediante el
contrato neutral mientras los 18 nativos esperan migración. Continúan pendientes
el catálogo de canales de socios, edición, reemplazo y ciclo visual de
definiciones, retorno con borrador y países/monedas. El Playwright focal del
segundo corte quedó verde en 375, 720 y 1280 px; los escenarios de los ciclos aún
no implementados siguen como gate de la historia.

## Tercer corte ejecutable 2026-08-01

La misma pantalla incorpora **Etiqueta** como cuarto tipo administrable. El handler
usa `registerTag`, consulta las etiquetas del snapshot y las integra al directorio,
filtros, detalle y textos del shell sin agregar tablas ni migraciones. Los campos
especializados aclaran en su etiqueta que los decimales aplican sólo a unidades y
la categoría superior sólo a categorías.

El recorrido Playwright registra una etiqueta real, vuelve al directorio, filtra
por tipo y comprueba el resultado en 375 px antes de continuar con perfiles,
artículos, precios y seguridad negativa. La imagen verificada, liveness/readiness y
la suite focal quedaron verdes. Las familias de variantes se separan al siguiente
corte porque `RegisterVariantFamily` necesita una lista ordenada de atributos con
tipo, obligatoriedad y posición; registrar una familia vacía no constituye una
administración funcional.

## Cuarto corte ejecutable 2026-08-01

`commercial_catalog` incorpora la quinta ruta `/catalog/variant-families`, menú,
contrato y handler autorizados por `commercial_catalog.definitions.manage`. La
pantalla se separa de las definiciones simples para evitar campos irrelevantes y
construye un borrador legible de 1 a 8 atributos. Cada atributo conserva código,
nombre, tipo `TEXT`/`NUMBER`/`BOOLEAN`, obligatoriedad y posición. **Agregar
atributo** y **Retirar último** no persisten; **Registrar familia** invoca el caso
de uso JTA real y abre el detalle creado.

El shell admite ahora `DISPLAY_TEXT` dentro de regiones de entidad conocidas y
renderiza el resumen con Material Design 3 sin aceptar markup del plugin. La lista
móvil dejó de depender de cinco columnas fijas y presenta dinámicamente cada
columna, por lo que los directorios de cuatro columnas son legibles en compacto.
Los tres selectores de la nueva pantalla declaran fuentes cerradas. El inventario
actual pasa a 77 selectores: 18 nativos y 59 de plugins; 58/59 selectores de
plugins están declarados y 76/77 fuentes tienen propietario caracterizado. Sólo
`business_partners.channel_kind` continúa como brecha de plugins.

La imagen Docker verificada, liveness/readiness, conservación de PostgreSQL y
Keycloak y el recorrido Playwright quedaron verdes. El gate registró una familia
con dos atributos ordenados y comprobó directorio, detalle, filtro, permiso negativo
y responsive en 375, 720 y 1280 px. La validación detectó y corrigió la pérdida del
borrador entre postbacks JSF sin agregar sesión de servidor ni una tabla nueva.

## Quinto corte validado 2026-08-01

`business_partners` incorpora la ruta `/business-partners/definitions`, el menú
**Tipos de canal** y un segundo contrato de pantalla, protegidos por
`business_partners.manage`. La migración privada V2 crea
`business_partner_definition` con clave empresarial compuesta, estado y versión;
no lee tablas del kernel ni de otro plugin. Al migrar, las empresas que ya tienen
socios reciben los cuatro valores anteriores —correo electrónico, teléfono,
WhatsApp y sitio web— sin cambiar los canales históricos.

El handler consulta y registra tipos mediante un caso de uso autorizado y auditado.
La ficha de socios dejó de construir cuatro opciones fijas: carga las definiciones
activas de la empresa y una alta nueva aparece en **Tipo de canal**. La fuente
`business_partners.channel_kind` declara propietario, ruta, permiso, capacidades y
exclusión de inactivos para operaciones nuevas. Los 59/59 selectores de plugins
quedan declarados y las 77/77 fuentes lógicas tienen clase y propietario; los 18
nativos continúan pendientes de publicar el metadato neutral.

Las pruebas focales de dominio, contrato, handler y renderer quedaron verdes. La
unidad JPA validó V1–V2 y el repositorio sobre PostgreSQL 18.4, incluido aislamiento
por empresa y rechazo estable de duplicados. El reactor y el build Docker
`verified` terminaron con 24/24 módulos verdes; el migrador aplicó V2 sin recrear
PostgreSQL ni Keycloak. El recorrido Playwright ampliado quedó verde con alta real,
uso inmediato y revisión responsive en 375/720/1280 px. Generó 17 capturas y su
revisión visual no encontró controles cortados ni overflow horizontal normal.

## Sexto corte validado 2026-08-02

`commercial_catalog:definitions` agrega el ciclo `ACTIVE`/`INACTIVE` para las
cuatro definiciones simples: unidad, categoría, marca y etiqueta. El comando exige
tipo, identidad, estado objetivo y versión esperada. El servicio revalida
`commercial_catalog.definitions.manage`, empresa y versión antes de persistir, y
audita `INACTIVATE_CATALOG_DEFINITION` o
`REACTIVATE_CATALOG_DEFINITION` con versiones anterior/resultante y resultado
`CHANGED`, `UNCHANGED` o `REJECTED`, sin incluir nombre ni otros datos de negocio.

El repositorio JPA conserva las tablas V1 y aplica una correspondencia cerrada de
tipo a tabla/columna. Toda lectura y actualización queda limitada por empresa,
identidad y versión; un recurso ausente o de otra empresa devuelve
`DEFINITION_NOT_FOUND` y una versión obsoleta devuelve conflicto. No se agregó
borrado físico, relación cruzada ni migración.

La ficha neutral incorpora la pestaña **Estado** y acciones **Inactivar** y
**Reactivar**. El recorrido Playwright crea y consume una unidad, la inactiva,
confirma su consulta mediante el filtro **Inactivos**, la reactiva y revisa el
detalle en 375, 720 y 1280 px. El gate focal de aplicación, la integración con
PostgreSQL 18.4 para los cuatro tipos, la regresión de 12 módulos, `mvnw.cmd
verify`, el build Docker `verified`, health y Playwright quedaron verdes. La
edición de nombre/estructura, reemplazo y los ciclos de perfiles/familias/tipos de
canal seguían pendientes al finalizar ese corte.

## Séptimo corte validado 2026-08-02

`commercial_catalog:tax_profiles` agrega el ciclo `ACTIVE`/`INACTIVE` sobre el
maestro y su revisión vigente. El comando exige identidad, estado objetivo y
versión esperada. El servicio revalida
`commercial_catalog.definitions.manage`, empresa y versión, y audita
`INACTIVATE_CATALOG_TAX_PROFILE` o `REACTIVATE_CATALOG_TAX_PROFILE` sin incluir
nombre, descripción ni tratamiento tributario.

El repositorio actualiza por empresa, identidad y versión; cierra la revisión
vigente y copia sus datos a una nueva revisión con la versión resultante. Así una
inactivación no borra referencias históricas ni modifica la semántica tributaria
guardada. La reactivación crea otra revisión vigente y una versión obsoleta o una
identidad de otra empresa se rechazan. No se agregó migración, SQL cruzado ni
relación JPA entre plugins.

La ficha neutral incorpora la pestaña **Estado** y las acciones **Inactivar** y
**Reactivar**. El recorrido Playwright crea y usa un perfil, lo inactiva, confirma
que sigue consultable con el filtro **Inactivos**, lo reactiva y revisa 375, 720 y
1280 px. Las pruebas focales, 9 pruebas PostgreSQL/Testcontainers, el reactor de
24 módulos, ArchUnit, el build Docker `verified`, health y Playwright quedaron
verdes. Permanecen pendientes edición y revisión explícita del perfil, ciclo de
familias y tipos de canal, retorno seguro y gobierno de fuentes nativas/normativas;
por eso la historia y Sprint 8 siguen abiertos.

## Octavo corte validado 2026-08-02

`commercial_catalog:tax_profiles` incorpora **Nueva revisión** como operación
explícita. El código, nombre e identidad técnica permanecen estables; el actor
autorizado puede cambiar tratamiento interno, descripción y vigencia. El comando
exige identidad y versión esperada, revalida
`commercial_catalog.definitions.manage` y audita solamente versiones y resultado,
sin copiar el contenido tributario al evento técnico.

El repositorio desactiva la revisión vigente y agrega una fila con la versión
resultante dentro de la misma transacción. Las referencias históricas continúan
apuntando a su versión original; una empresa ajena o una versión obsoleta se
rechazan. No se agregó migración ni acceso a tablas de otro plugin.

La ficha neutral agrega la pestaña **Nueva revisión**, precarga la versión actual
y conserva selección y borrador seguro cuando la entrada es inválida. Quedaron
verdes 6 pruebas de aplicación, 6 del handler, 5 del renderer, 10 de persistencia
PostgreSQL/Testcontainers y las 333 pruebas del reactor acotado. `mvnw.cmd verify`,
el build Docker `verified`, health y Playwright quedaron verdes. El recorrido creó
la revisión y revisó 1280/720/375 px; generó 37 PNG sin overflow normal ni errores
en la ventana final de logs. Al terminar el octavo corte seguían pendientes la historia visual de revisiones, edición y
reemplazo de definiciones simples, ciclos de familias/tipos de canal, retorno
contextual, fuentes nativas/normativas y recongelación.

## Noveno corte validado 2026-08-02

`commercial_catalog:tax_profiles` incorpora la consulta autorizada y de solo
lectura del historial completo de revisiones. La aplicación revalida
`commercial_catalog.definitions.manage`; el repositorio filtra por empresa e
identidad, ordena desde la versión más reciente y distingue la revisión vigente
sin exponer entidades JPA ni tablas privadas a otros plugins. Una identidad de
otra empresa se responde como `DEFINITION_NOT_FOUND`. No se agregó migración ni se
modificó `plugin-api`.

La ficha neutral agrega **Historial** con versión, estado actual/histórico,
tratamiento interno, descripción y vigencia. El shell renderiza una tabla de solo
lectura en expandido y tarjetas en medio/compacto. La primera revisión visual
detectó que el formulario ocupaba media columna en 1280 px y recortaba
**Vigencia**; se hizo que el formulario de tabs abarque el ancho completo y se
repitieron pruebas, imagen y Playwright.

Quedaron verdes 7 pruebas de aplicación, 6 del handler, 5 del renderer y 10 de
persistencia PostgreSQL/Testcontainers. La regresión acotada ejecutó 12/12 módulos
y 334 pruebas; `mvnw.cmd verify` terminó 24/24 módulos verde con WAR y 24 pruebas
ArchUnit/composición. El build Docker `verified`, health y el Playwright final
quedaron verdes. Las 40 capturas incluyen el historial en 1280/720/375 px, sin
overflow normal ni columnas perdidas; los logs finales no contienen errores ni
excepciones. Siguen pendientes edición, reemplazo e historia de definiciones
simples, ciclos de familias/tipos de canal, retorno contextual,
fuentes nativas/normativas y recongelación.

## Décimo corte validado 2026-08-03

`plugin-api` 0.4.2 incorpora `SelectorSourceMetadata` como contrato común y
`PlatformSelectorSourceDefinition` para fuentes de kernel/shell. La evolución es
aditiva: los 59 selectores de plugins conservan su constructor y accessor
`ownerPluginId()`. `SelectorSourceOwner` distingue explícitamente `PLATFORM` de
`PLUGIN`, por lo que el kernel no se registra ni se autoriza como un plugin falso.

`NativeSelectorSourceCatalog` publica los 18 usos directos de Jakarta Faces:
empresas, personalizaciones físicas, usuarios, membresías, roles, permisos y
filtros de auditoría. Los 77/77 selectores quedan cubiertos por metadato neutral.
Un componente composite propiedad del shell muestra el origen y clasificación;
para las 11 referencias/catálogos administrables sólo entrega la ruta cuando la
autoridad global actual contiene el permiso declarado. Las siete fuentes cerradas
o de despliegue explican su gobierno y nunca ofrecen altas arbitrarias.

La primera prueba de `plugin-api` detectó que el constructor compacto consultaba
el accessor del record antes de asignar sus campos; se corrigió para validar el
parámetro ya comprobado y se repitió 22/22 verde. Las pruebas focales de catálogo,
autorización y recursos Faces quedaron 7/7 verdes. La regresión de los 12 módulos
que alimentan `web-shell` ejecutó 342 pruebas, sin fallos, errores ni omisiones.
El gate integral `mvnw.cmd verify` terminó 24/24 módulos verde en 2:39 min,
construyó el WAR y ejecutó 24 pruebas ArchUnit/composición sin fallos.
Permanecen pendientes los ciclos e historias de maestros, retorno seguro,
fuentes normativas, Playwright acumulado y recongelación.

## Undécimo corte validado 2026-08-03

El shell implementa la ida y vuelta segura de los selectores administrables
renderizados por plugins. Al pulsar **Agregar o administrar**, `ShellViewBean`
revalida pantalla, fuente, ruta y permiso; conserva solamente inputs `TEXT_INPUT`
y `SELECT` realmente renderizados y habilitados, y los normaliza mediante el
handler propietario antes de crear un contexto efímero de sesión. Los valores de
negocio se envían por POST y nunca aparecen en la URL.

`SelectorReturnContextStore` limita el contexto a cuatro entradas por sesión,
diez minutos de vigencia y un único consumo. El token opaco queda ligado a usuario,
empresa y revisión de sesión; se invalida al cambiar de empresa o cerrar sesión.
El retorno vuelve a autorizar origen y destino, restaura modo, pestaña, recurso y
borrador seguro, vuelve a ejecutar el handler para refrescar opciones y muestra
**Opciones actualizadas**. No se usa `localStorage`, `sessionStorage`, una tabla
compartida ni un contrato de plugin con XHTML, EL o JavaScript.

Las ocho pruebas focales cubren filtrado y límites del borrador, payload POST,
token de un uso, expiración, aislamiento de identidad/empresa/revisión,
serialización pasivable y recursos Faces/JS. La regresión de los 12 módulos que
alimentan `web-shell` quedó verde con 42 pruebas de shell. `mvnw.cmd verify`
terminó 24/24 módulos verde en 2:34 min, con WAR y 24 pruebas
ArchUnit/composición. La imagen `logixone/app:j11-s8-c02-selector-return`
(`sha256:10c3d3da3589604da66044967f9dc802d33cef9f707ccc7fc2b6d96f6e646462`)
repitió el `verify` interno 24/24 en 1:01 min. Se recreó sólo `app`; PostgreSQL y Keycloak
conservaron sus IDs y readiness quedó `UP`.

El Playwright final sobre el contenedor definitivo quedó verde en 63,60 s: desde un alta de artículo con unidad
obligatoria abrió Definiciones, creó una unidad, regresó, comprobó código, nombre y
descripción restaurados y seleccionó la opción nueva. Las 44 capturas (7.253.278 bytes) cubren
1280/720/375 px; la revisión visual confirmó banner, tablas/tarjetas y formulario
restaurado sin overflow normal. El primer Playwright detectó una carrera del test
al contar el campo de precios antes de terminar la navegación JSF; el artículo ya
estaba registrado. Se agregó espera explícita manteniendo la aserción de unicidad
y se repitió todo el escenario verde. Los logs finales no contienen errores ni
excepciones. G0 recorrió 270 Markdown y 1.152 enlaces locales sin errores UTF-8,
mojibake ni enlaces rotos.

Este corte todavía no implementaba el retorno desde los formularios nativos de
Administración. También seguían pendientes los ciclos e historias restantes,
países/monedas, Playwright acumulado de cierre y recongelación.

## Duodécimo corte validado 2026-08-03

El shell extiende el retorno seguro a los 11 usos nativos administrables de los 18
inventariados. `NativeSelectorReturnPlan` fija uso, origen, destino, título y los
únicos inputs recuperables; no acepta una ruta, permiso o campo aportados por el
navegador. `NativeSelectorReturnContextStore` conserva hasta cuatro contextos por
sesión durante diez minutos, ligados al usuario y revisión de sesión, y los consume
una sola vez. Los siete selectores cerrados o de despliegue continúan sin acción de
alta.

El enlace JSF envía el borrador por POST mediante un listener de captura compatible
con CSP; la URL contiene sólo un UUID canónico. Empresas, Plugins, Seguridad y
Autoridad global conservan explícitamente el token en cada postback del destino.
Al volver se revalidan autoridad, uso, origen y destino, y cada bean restaura sólo
su mapa cerrado. Cambiar de empresa o cerrar sesión limpia los contextos.

La suite focal de comportamiento, metadatos y recursos quedó 9/9 verde; el ajuste
responsive final quedó 4/4 verde. La imagen `verified` ejecutó los 24 módulos y 24
pruebas ArchUnit/composición en 1:50 min. El manifest final
`sha256:93df0a10f66a109ac1a508f2a8b7d30abf50294e0d62f8e4bfc5c31d014cd11c`
mide 501.000.028 bytes. Sólo se recreó `app`; PostgreSQL y Keycloak conservaron
identidad. Liveness/readiness quedaron HTTP 200/`UP` y 1.499 líneas finales no
contienen errores ni excepciones.

`CommercialCatalogVisualIT` quedó 1/1 verde en 106,1 s contra ese manifest. El
recorrido abrió Usuarios desde la asignación global, conservó el contexto durante
el alta, regresó a Autoridad global y recuperó usuario/rol. Después continuó el
escenario acumulado del catálogo. Las 50 capturas suman 9.647.801 bytes; los seis
originales nativos de 1280/720/375 px fueron revisados sin overflow ni texto
fragmentado. Los defectos CSP, setter de `viewParam`, continuidad de postbacks,
banda de retorno, sincronización E2E y layout medio se corrigieron y repitieron en
verde; no se omitió ninguna prueba fallida. G0 recorrió 270 Markdown y 1.158
enlaces locales sin errores UTF-8, mojibake ni enlaces rotos, y no encontró los
cuatro secretos locales en 1.133 archivos de texto.

## Decimotercer corte validado 2026-08-03

`business_partners` completa el ciclo de vida de `CHANNEL_KIND` sin cambiar la V2
existente: el agregado exige versión esperada, el caso de uso revalida empresa y
`business_partners.manage`, el repositorio actualiza por la clave compuesta
empresa/clase/código y JPA incrementa la versión. Inactivar conserva físicamente el
tipo y sus referencias; la consulta administrativa muestra activos e inactivos,
pero los formularios de socios siguen ofreciendo únicamente opciones activas.

La prueba PostgreSQL quedó 12/12 verde y cubrió conservación de fila, incremento,
conflicto optimista y reactivación. `plugins/business-partners` quedó 43/43 verde;
handlers, selector y renderer también quedaron verdes. El `mvnw.cmd verify` raíz
terminó 24/24 módulos en 3:37 min, con 24 pruebas ArchUnit/composición. La imagen
`logixone/app:j11-s8-c02-channel-lifecycle`, digest
`sha256:b978e6ae283763a50897001d7e2956a9d5c627a8fb79e82a1347b029087a3067`
y 501.005.637 bytes, repitió internamente los 24 módulos en 1:40 min.

Sólo se recreó `logixone-app-1`; PostgreSQL y Keycloak conservaron identidad y
volúmenes. Los tres servicios quedaron saludables y liveness/readiness respondieron
HTTP 200/`UP`. `BusinessPartnersVisualIT` terminó 1/1 verde en 46,13 s sobre la
imagen final: creó un tipo, abrió **Estado**, lo inactivó, comprobó en **Resumen**
que permanecía visible, capturó 1280/720/375 px, volvió a **Estado**, lo reactivó y
continuó usándolo en un socio. Las 20 capturas suman 2.738.907 bytes en
`docs/evidence/screenshots/J11-S8-C02-channel-lifecycle/e2e/`; la revisión visual
no encontró overflow ni controles cortados y la ventana de logs tuvo cero errores
severos o excepciones. G0 validó 270 Markdown y 1.158 enlaces locales sin errores
UTF-8, mojibake ni enlaces rotos, y no encontró los cuatro secretos locales en
1.478 archivos de texto.

Las tres primeras repeticiones Playwright fallaron por omitir la navegación entre
las pestañas **Resumen** y **Estado** al comprobar texto y acciones. El dominio ya
había confirmado cada mutación; se corrigió el recorrido para representar la UI
real y la ejecución completa posterior quedó verde. Antes, compilación, renderer,
expectativas de capacidad y acceso sandbox a Docker también detectaron desajustes;
todos fueron corregidos y repetidos sin omitir ni relajar pruebas.

## Decimocuarto corte validado 2026-08-03

`commercial_catalog:variant_families` completa el ciclo `ACTIVE`/`INACTIVE` sin
alterar la V1 existente. El comando exige identidad, estado objetivo y versión
esperada; el caso de uso revalida empresa y
`commercial_catalog.definitions.manage`, y audita
`INACTIVATE_CATALOG_VARIANT_FAMILY` o
`REACTIVATE_CATALOG_VARIANT_FAMILY` con identidad y versiones técnicas. La
persistencia actualiza por empresa, identidad y versión, incrementa la versión y
recarga la familia completa. Inactivar no borra ni reordena sus atributos y una
identidad ajena o una versión obsoleta se rechazan de forma estable.

La ficha neutral agrega **Estado**, **Inactivar familia** y **Reactivar familia**.
La administración conserva activos e inactivos mediante su filtro. Este corte no
expone todavía la asignación de una familia a un artículo y, por tanto, no afirma
exclusión de inactivos en una operación que aún no existe en la UI.

Las pruebas del servicio quedaron 8/8, el handler 5/5, el renderer 5/5, la
integración PostgreSQL/Testcontainers 11/11 y el módulo
`plugins/commercial-catalog` 67/67 verdes. `mvnw.cmd verify` terminó 24/24
módulos en 1:59 min con WAR y 24 pruebas ArchUnit/composición. La imagen
`logixone/app:j11-s8-c02-variant-family-lifecycle`, digest
`sha256:38c44e0d07da579c7588252e37b538ce3766931e5e8fe2459c9ef95877b0fb30`
y 501.009.110 bytes, repitió internamente los 24 módulos en 1:06 min.

Sólo se recreó `logixone-app-1`; PostgreSQL `3d3243fa0cda` y Keycloak
`0bc73b911f34` conservaron identidad y volúmenes. Los tres servicios quedaron
`healthy`, liveness/readiness respondieron HTTP 200/`UP` y la ventana final de
logs no presentó errores ni excepciones. `CommercialCatalogVisualIT` terminó 1/1
verde en 71,20 s: creó una familia con `COLOR` y `TALLA`, la inactivó, comprobó
estado y atributos conservados en **Resumen**, la consultó mediante el filtro de
inactivas y la reactivó. Las 53 capturas suman 10.222.714 bytes en
`docs/evidence/screenshots/J11-S8-C02-variant-family-lifecycle/e2e/`; los
originales 1280/720/375 fueron revisados sin overflow ni controles cortados.

La primera ejecución Playwright falló porque la prueba buscaba atributos en
**Estado**, aunque la ficha los presenta en **Resumen**. Se explicitó esa
navegación, se mantuvo la verificación de preservación y la repetición completa
quedó verde; no se omitió ni relajó la aserción. G0 recorrió 270 Markdown y
1.161 enlaces locales sin errores UTF-8, mojibake, enlaces rotos ni filtraciones
de los secretos locales.

## Decimoquinto corte validado 2026-08-03

`business_partners:definitions` permite revisar el nombre visible de un
`CHANNEL_KIND` sin cambiar empresa, clase ni código estable. El comando exige la
versión vigente, revalida `business_partners.manage` y audita únicamente identidad,
operación y versiones técnicas. La migración inmutable V3 crea
`business_partner_definition_revision`, conserva una fila append-only por
empresa/clase/código/versión y retroalimenta la revisión vigente de los catálogos
V2 ya existentes. No se agregaron relaciones JPA ni SQL hacia otro plugin.

La ficha neutral incorpora **Nueva revisión** e **Historial**. El historial es de
solo lectura, queda aislado por empresa, se ordena desde la versión más reciente y
distingue el nombre actual del histórico. La pantalla principal continúa usando el
código como identidad y ofrece solamente tipos activos en operaciones nuevas.

Las pruebas de `plugins/business-partners` quedaron 46/46 verdes. La integración
PostgreSQL/Testcontainers quedó 19/19: 13 escenarios JPA/repositorio y 6 de
migración, incluidos backfill V2→V3, historial inmutable, revisión de nombre,
aislamiento empresarial, ciclo de estado y conflicto optimista. `mvnw.cmd verify`
terminó 24/24 módulos en 2:10 min con WAR y 24 pruebas
ArchUnit/composición; ambos Dockerfiles pasaron `--check` sin advertencias.

Las imágenes verificadas fueron
`logixone/app:j11-s8-c02-channel-history`, digest
`sha256:b13a97f263661e315c64ca7961437ae96b40ab5a5a30a93723b7bfb899986b48`
y 501.018.047 bytes, y
`logixone/migrator:j11-s8-c02-channel-history`, digest
`sha256:0a908b12b8b5384755ae0c2ea556114a42f7468eae74f6ee018799d3903d4f63`
y 105.348.213 bytes. El migrador aplicó V3 una vez y la repetición informó cero
cambios. Sólo se recreó `app`; PostgreSQL y Keycloak conservaron sus volúmenes.
Los tres servicios quedaron saludables y liveness/readiness respondieron HTTP
200/`UP`, incluidos base, migraciones y OIDC.

`BusinessPartnersVisualIT` terminó 1/1 verde en 39,48 s: registró un tipo de canal,
creó una revisión de nombre, leyó ambas versiones en **Historial**, recorrió el
ciclo activo/inactivo y utilizó el nombre revisado en la ficha de un socio. Las 23
capturas suman 3.137.997 bytes en
`docs/evidence/screenshots/J11-S8-C02-channel-history/e2e/`; los originales
1280/720/375 fueron revisados sin overflow ni controles cortados. La ventana de
829 líneas de log tuvo cero coincidencias de error severo o excepción.

La primera prueba focal del handler detectó una clave de columna no válida y la
prueba inicial del renderer detectó que las nuevas secciones no formaban parte del
floorplan de detalle. Ambos defectos se corrigieron y sus repeticiones, el módulo y
el gate integral quedaron verdes. El intento Testcontainers dentro del sandbox no
pudo acceder a Docker; la repetición autorizada sobre el Engine local quedó 19/19.
No se omitió, desactivó ni relajó ninguna prueba.
G0 recorrió 274 archivos Markdown sin enlaces rotos, errores UTF-8, mojibake ni
filtraciones de secretos.

## Decimosexto corte validado 2026-08-04

`commercial_catalog:definitions` permite crear una nueva revisión de unidad,
categoría, marca o etiqueta sin cambiar empresa, clase, código ni identidad. El
comando exige la versión vigente; el caso de uso revalida
`commercial_catalog.definitions.manage`, audita identidad y versiones técnicas y
rechaza una categoría que intente convertirse en su propio padre. La nueva V2
privada crea cuatro tablas append-only de revisiones, retroalimenta las filas V1 y
mantiene una historia ordenada desde la versión más reciente. No existen relaciones
JPA ni SQL hacia esquemas de otros plugins.

La ficha neutral agrega **Nueva revisión** e **Historial** para las cuatro clases.
El formulario sólo expone los campos propios de la clase, conserva código e
identidad y excluye la categoría actual del selector de padre. El historial es de
solo lectura, aislado por empresa y conserva versiones anteriores aunque la
definición cambie de estado posteriormente.

Las pruebas de `plugins/commercial-catalog` quedaron 71/71 verdes. Las suites
PostgreSQL 18.4 quedaron 17/17: 12 escenarios JPA/repositorio y 5 de migración,
incluidos backfill V1→V2, historial inmutable, aislamiento empresarial, conflicto
optimista y repetición idempotente. `mvnw.cmd verify` terminó 24/24 módulos en
2:56 min, construyó el WAR y ejecutó 24 pruebas ArchUnit/composición. Ambos
Dockerfiles pasaron `docker build --check` sin advertencias.

Las imágenes exactas del corte son
`logixone/app:j11-s8-c02-simple-definition-history`, digest
`sha256:65ac722b741008c64aff97085b91932ec05868ef7c215229c822a9024727827a`
y 501.031.785 bytes, y
`logixone/migrator:j11-s8-c02-simple-definition-history`, digest
`sha256:eb85e9b3a368c1283ee218191a9c5b3440a0675b1b12d765e1ee5410dc524058`
y 105.361.680 bytes. Sus builds internos verificaron respectivamente 24/24 y
16/16 módulos. El migrador aplicó sólo V2 de `commercial_catalog` sobre el volumen
conservado; la repetición y la recreación final informaron cero migraciones para
todos los propietarios y `schema_version=2` para el catálogo.

La composición final conservó los volúmenes y dejó aplicación, PostgreSQL y
Keycloak saludables. Liveness y readiness respondieron HTTP 200/`UP`, con
catálogo, configuración, base, migraciones y OIDC en `UP`.
`CommercialCatalogVisualIT` terminó 1/1 verde en 61,73 s: creó una unidad,
publicó una revisión, leyó ambas versiones en **Historial**, recorrió el ciclo de
estado y continuó el escenario acumulado de catálogo, retorno contextual y
seguridad negativa. Las 59 capturas PNG suman 6.757.175 bytes en
`docs/evidence/screenshots/J11-S8-C02-simple-definition-history/e2e/`; los
originales de revisión e historial en 1280/720/375 px fueron revisados sin overflow
horizontal ni controles cortados.

Las primeras ejecuciones visuales hicieron visibles tres problemas de la propia
prueba: textos esperados con codificación incorrecta, una carrera antes de contar
la confirmación JSF y una captura de página completa que agotaba Chromium por el
historial acumulado. Se corrigieron las cadenas, se esperó el primer elemento antes
de exigir cardinalidad exacta y la captura pasó al viewport manteniendo la medición
del documento completo para detectar overflow. La ejecución final quedó verde; no
se omitió, desactivó ni relajó ninguna aserción funcional.
G0 recorrió 280 archivos Markdown sin enlaces rotos, errores UTF-8, mojibake ni
filtraciones de los secretos locales. La ventana final de logs no contiene errores
severos ni excepciones; conserva solamente las advertencias conocidas del
certificado autofirmado de desarrollo.

## Decimoséptimo corte validado 2026-08-04

`commercial_catalog:definitions` permite reemplazar una unidad, categoría, marca
o etiqueta por una identidad sucesora nueva. La operación exige la versión
vigente y `commercial_catalog.definitions.manage`, revalida la empresa y ejecuta
atómicamente alta de la sucesora, inactivación de la anterior, revisión inicial y
vínculo privado de reemplazo. El tipo y la empresa deben coincidir; no se permite
autorrelación, reemplazo repetido ni sucesora inactiva.

La definición anterior queda inactiva e inmutable: no puede revisarse ni
reactivarse. Sus referencias existentes no se reescriben y continúan mostrando la
identidad histórica; sólo las selecciones futuras ofrecen la sucesora activa. La
ficha muestra **Reemplazar** y el resumen histórico muestra **Reemplazada por**.
Los selectores cerrados de decimales y categoría superior elevan la cobertura a
63 selectores de plugins más 18 nativos, 81/81 declarados.

La migración privada
`V3__link_replaced_simple_catalog_definitions.sql` agrega cuatro vínculos
opcionales con FK de misma empresa, checks contra autorrelación y contra origen
activo, e índices propios, sin crear relaciones hacia otros plugins. Las suites
PostgreSQL 18.4 quedaron 19/19 verdes, incluidos repositorio JPA, migración
V2→V3, aislamiento empresarial, conservación de referencias, rechazo de
reactivación/revisión y repetición idempotente. Los focos de aplicación y UI
quedaron 10/10 y 14/14; el módulo quedó 74/74 y el reactor raíz terminó 24/24 en
2:11 min, con las 24 pruebas de
arquitectura/composición quedaron verdes.

Los Dockerfiles pasaron la validación estática sin advertencias. Las imágenes
correctas construidas con `with-inventory-demo` son
`logixone/app:j11-s8-c02-simple-definition-replacement`, digest
`sha256:a21616f9bf182ff99f1aabdcf806a7426334216699e48d116d724161555de987`
y 501.046.492 bytes, y
`logixone/migrator:j11-s8-c02-simple-definition-replacement`, digest
`sha256:1062a6fbc34160dd17fa0f29f03780396e080bec23105c725944de90235fe9e8`
y 105.376.370 bytes. Sus builds internos verificaron 24/24 y 16/16 módulos. El
migrador aplicó sólo V3 de `commercial_catalog` sobre el volumen conservado y la
repetición informó cero migraciones para todos los propietarios. PostgreSQL
`3d3243fa0cda` y Keycloak `c69accfa1703` conservaron sus contenedores y volúmenes;
liveness/readiness respondieron HTTP 200/`UP`.

`CommercialCatalogVisualIT` terminó 1/1 verde en 46,71 s. Verificó el formulario
de reemplazo, la sucesora, el vínculo desde el origen inactivo y que el artículo
existente mantenga el código de unidad anterior, además del recorrido acumulado y
seguridad negativa. Las 68 capturas PNG suman 7.784.665 bytes en
`docs/evidence/screenshots/J11-S8-C02-simple-definition-replacement/e2e/`; se
revisaron originales representativos en 1280/720/375 px sin overflow horizontal
ni controles cortados. La ventana final de 5.606 líneas de logs no contiene
`ERROR`, errores severos ni excepciones. G0 recorrió 280 Markdown y 1.207 enlaces
locales sin errores UTF-8, mojibake, enlaces rotos ni filtraciones de secretos.

Tres primeras ejecuciones visuales hicieron visibles supuestos incorrectos de la
prueba: el resumen debía abrirse después del postback de reemplazo, el filtro de
artículos se llama **Nombre, código o identificador** y el siguiente recorrido
debía restaurar el viewport expandido. Se corrigieron las transiciones y
selectores accesibles; la ejecución final quedó verde sin omitir ni relajar
ninguna aserción funcional.

## Decimoctavo corte validado 2026-08-04

`commercial_catalog:variant-families` permite crear una revisión completa del
nombre visible y de la estructura ordenada de atributos. El comando conserva
empresa, identidad y código, exige la versión vigente y el permiso
`commercial_catalog.definitions.manage`, admite de 1 a 8 atributos con códigos y
posiciones únicos y audita `REVISE_CATALOG_VARIANT_FAMILY` sin registrar valores
personales. **Nueva revisión** parte de una copia segura de la estructura actual;
**Historial** expone cada versión en solo lectura.

La migración privada inmutable
`V4__version_variant_family_history.sql`, SHA-256
`FBA77C705B91FD06A3B0FC213954B5CE468B4FE816430AC711FDD91E782B3C5C`,
crea `variant_family_revision` y `variant_attribute_revision`, retroalimenta las
familias V3 y añade `variant_family_version` a las dos tablas de asignación. Sus
FK pasan a apuntar a la revisión inmutable correspondiente; una revisión futura
no reinterpreta artículos o variantes ya asignados. No se agregaron accesos JPA o
SQL entre plugins.

El módulo `commercial-catalog` quedó 77/77 verde. PostgreSQL 18.4 quedó 22/22:
14 escenarios JPA/repositorio y 8 de migración, incluidos backfill V3→V4,
historial estructural, aislamiento empresarial, conflicto optimista, preservación
de versión asignada e idempotencia. El `mvnw.cmd verify` definitivo terminó
24/24 módulos en 2:48 min y las 24 pruebas ArchUnit/composición quedaron verdes;
`web-shell` quedó 55/55 con la regresión UTF-8 incluida.

Para no aplicar una migración irreversible sobre los datos persistentes del
entorno habitual, la validación Docker se ejecutó en la composición aislada
`logixone-vfh`, con volúmenes nuevos y puertos 28080/9180. El entorno original no
se modificó. La primera ejecución aplicó V1–V4; la segunda informó
`migrations_executed=0` y `schema_version=4`. Las imágenes verificadas son
`logixone/app:j11-s8-c02-variant-family-history`, digest
`sha256:671cf8895426aa7ee5cc7d052f2d0eb99e68d68417eb9334b28b449fd0ead9d5`
y 501.056.381 bytes, y
`logixone/migrator:j11-s8-c02-variant-family-history`, digest
`sha256:56a4aba08bc73f7743eef49971e2bceac9959dd303ecf63133720966d9017c5b`
y 105.385.690 bytes. Aplicación, PostgreSQL y Keycloak quedaron `healthy`;
liveness/readiness respondieron HTTP 200/`UP`.

`CommercialCatalogVisualIT` terminó 1/1 verde en 75,33 s. Registró una familia
con `COLOR/TALLA`, creó una revisión que la reemplazó por `NUMERO`, verificó ambas
estructuras en historial, recorrió estado, retorno contextual y seguridad
negativa. Produjo 74 PNG y 8.524.802 bytes en
`docs/evidence/screenshots/J11-S8-C02-variant-family-history/e2e/`. Las seis vistas
nuevas de revisión/historial en 1280/720/375 px se revisaron visualmente: textos
acentuados correctos, controles completos y sin overflow horizontal normal.

El primer arranque limpio rechazó correctamente la administración global porque
no tenía autoridad bootstrap; se aplicó el bootstrap documentado sólo en el stack
aislado y se verificó después el arranque con bootstrap deshabilitado. Las
iteraciones visuales detectaron y corrigieron mojibake en el shell, persistencia
incompleta del `DISPLAY_TEXT` del detalle y transiciones/fixtures no deterministas
del propio E2E. Ninguna prueba ejecutada se ocultó, desactivó o dejó fallando. Dos
errores SQL del log PostgreSQL corresponden a consultas manuales diagnósticas con
columnas incorrectas; la aplicación tuvo cero coincidencias severas y el intervalo
limpio posterior de base no registró errores.

## Decimonoveno corte ejecutable: asignación de familias a artículos

Artículos y servicios incorpora la pestaña neutral **Variantes** y el selector
gobernado de familias activas. El formulario muestra la estructura esperada y
envía identidad/revisión más valores `CODIGO=valor`; no construye tipos de dominio
ni confía en información estructural del navegador. El caso de uso autorizado
resuelve la familia dentro de la empresa con bloqueo compartido, exige estado y
revisión vigentes y valida atributos declarados, obligatorios y tipos. Persistencia
conserva la revisión exacta ya prevista por V4, por lo que no se necesita otra
migración.

Una referencia manipulada se rechaza antes de solicitar autorización de mutación,
se registra sin valores sensibles y se normaliza durante la recarga autorizada.
Las familias inactivas no aparecen en nuevas asignaciones; una asignación histórica
sigue mostrando familia, revisión y valores. El corte quedó validado con 12/12
pruebas focales, 81/81 del módulo, 23/23 sobre PostgreSQL 18.4, reactor de imagen
24/24, `mvnw.cmd -B verify` final 24/24 en 2:12, ArchUnit y composición verdes. La imagen final
`logixone/app:j11-s8-c02-variant-assignment` tiene digest
`sha256:f457b3d2bf150df1bbfc2283f1fb15be937d63f237781d2932f9174e70a447da`
y 501.065.108 bytes. El stack aislado ejecutó ese mismo digest con health `UP` y
cero errores posteriores en aplicación/base. `CommercialCatalogVisualIT` terminó
1/1 verde en 94,89 s y produjo 77 PNG (8.967.738 bytes); las vistas de Variantes
en 1280/720/375 px fueron revisadas sin overflow ni controles cortados. V4 y el
migrador ya validado se conservaron porque este corte no cambia el esquema. G0
revisó 287 Markdown y 1.330 enlaces locales sin errores UTF-8, mojibake, enlaces
rotos ni filtraciones de los cuatro secretos locales.

## Vigésimo corte ejecutable: definiciones gobernadas de socios

`business_partners:definitions` pasa de administrar sólo `CHANNEL_KIND` a un
maestro unificado de cuatro clases: `CHANNEL_KIND`, `IDENTIFICATION_TYPE`,
`ADDRESS_TYPE` y `ADDRESS_PURPOSE`. Los formularios de identificación y dirección
reemplazan texto/códigos implícitos por selectores empresariales activos con ruta
**Agregar o administrar**. La identidad de detalle es `clase:código`, por lo que
una navegación directa conserva la clase sin confiar en estado previo del
formulario.

La aplicación resuelve cada referencia dentro de la empresa, clase y estado
vigentes con bloqueo compartido antes de guardar. V4 amplía las restricciones de
las dos tablas existentes, retroalimenta códigos ya usados, crea sus revisiones
append-only y agrega datos iniciales mínimos; no introduce tablas ni relaciones
JPA/SQL cruzadas. País continúa como texto ISO hasta decidir una fuente normativa
versionada.

El módulo quedó 51/51 verde. PostgreSQL 18.4 quedó 21/21: 14 escenarios JPA y 7
de migración, con V1–V4, backfill, aislamiento y resolución operacional. El gate
`with-inventory-demo` terminó 24/24 módulos, incluidas 24 pruebas
ArchUnit/composición. Las imágenes verificadas son
`logixone/app:j11-s8-c02-partner-definitions`, digest
`sha256:52a2c64e9f690900ca7fdf1b1ef0bd66fcc5b5688cad90c4825d2beb64e84af0`,
501.071.129 bytes, y
`logixone/migrator:j11-s8-c02-partner-definitions`, digest
`sha256:c3cffe4b25f66ffbc187e313b79d3b622b547908eaf8c3de39e69da1e42cecf1`,
105.399.374 bytes.

La composición aislada `logixone-bpd`, con volúmenes nuevos y puertos
38080/10180, aplicó V1–V4 una vez y repitió con cero migraciones; bootstrap global
one-shot quedó nuevamente deshabilitado. Aplicación, PostgreSQL y Keycloak
quedaron `healthy`, liveness/readiness respondieron HTTP 200/`UP` y la ventana
final tuvo cero coincidencias severas en aplicación y PostgreSQL.
`BusinessPartnersVisualIT` terminó 1/1 verde en 54,28 s, preparó idempotentemente
plugin/permisos mediante las superficies administrativas, ejercitó las cuatro
clases, historial, ciclo, consumo y seguridad negativa, y produjo 23 PNG
(2.625.513 bytes) revisados en 1280/720/375 px sin overflow horizontal. G0
validó 288 Markdown y 1.336 enlaces locales sin errores UTF-8, mojibake o enlaces
rotos, y cero coincidencias con los cuatro secretos en 1.180 archivos de texto.

## Decisiones y alcance que bloquean el cierre

- propiedad y actualización verificable de ISO 3166/4217;
- umbral que obliga a búsqueda paginada en vez de `selectOneMenu` completo;
- permisos definitivos por plugin y datos iniciales mínimos por empresa.

## Criterios de aceptación

- cada uno de los 89 selectores lógicos actuales tiene fuente, clase y propietario;
- cada catálogo empresarial tiene alta, consulta, edición permitida e inactivación;
- cada referencia operativa ofrece acceso contextual autorizado al propietario;
- estados cerrados y catálogos normativos no admiten valores arbitrarios;
- volver del administrador conserva un borrador seguro y refresca opciones;
- referencias inactivas históricas siguen visibles sin ofrecerse para nuevas altas;
- listas grandes tienen búsqueda/paginación y no saturan el HTML;
- permisos negativos se revalidan en el servidor y quedan auditados;
- no existen accesos JPA/SQL entre plugins;
- Playwright cubre 375, 720 y 1280 px, teclado, vacío, alta, retorno, inactivo y
  seguridad negativa;
- manuales, guía, fotografía, PDF y demo acumulada quedan actualizados.

## Cierre e instalador

Al completar esta corrección se recongela el baseline y se regenera el PDF. Luego,
conforme a ADR-0029, se pregunta al responsable de producto si se creará un nuevo
instalador Windows. No se modifica `current` antes de recibir esa respuesta.
