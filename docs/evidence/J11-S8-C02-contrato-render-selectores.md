# Evidencia J11-S8-C02 - Contrato, renderer y definiciones del catálogo

- Fecha: 2026-08-04
- Estado: decimonoveno corte validado; historia y Sprint abiertos
- Decisión: [ADR-0028](../adr/0028-gobierno-de-selectores-y-datos-administrables.md)
- Demos: [definiciones del catálogo](../runbooks/demo-definiciones-catalogo-j11-s8-c02.md), [tipos de canal](../runbooks/demo-tipos-canal-j11-s8-c02.md) y [definiciones de socios](../runbooks/demo-definiciones-socios-j11-s8-c02.md)

## Resultado

`plugin-api` 0.4.1 incorporó metadatos neutrales y aditivos de fuente de selector
para plugins; 0.4.2 agrega propietario de plataforma y metadatos nativos sin romper
el constructor ni `ownerPluginId()` existentes.
El shell los copia a un modelo JSF cerrado solamente después de verificar que el
campo es un `SELECT`, que la ruta ya es navegable para el actor y que el servidor
autoriza el permiso del plugin propietario. La ruta no se entrega al modelo cuando
falta autorización.

El segundo corte agregó `/catalog/definitions`, su handler autorizado y cinco
selectores consumidores que apuntan a la fuente propietaria. El cuarto corte
agrega tres selectores cerrados para familias de variantes.

El tercer corte agregó **Etiqueta** al directorio, alta, filtro y detalle de la
misma ruta. Reutiliza el caso de uso JTA `registerTag` y la persistencia V1
existentes; no cambia el número de selectores ni requiere una migración nueva.

El cuarto corte agrega `/catalog/variant-families` y reutiliza el caso de uso JTA,
repositorio y migración V1 existentes. El borrador neutral conserva de 1 a 8
atributos ordenados; el shell renderiza su resumen mediante `DISPLAY_TEXT` y una
lista móvil independiente de la cantidad de columnas.

El quinto corte agrega `/business-partners/definitions`, el menú **Tipos de
canal**, la migración privada V2 y un repositorio JPA empresarial. La ficha de
socios consume las definiciones activas en lugar de cuatro opciones hardcodeadas.
El sexto corte agrega inactivación/reactivación versionada y auditada a unidades,
categorías, marcas y etiquetas sin cambiar la migración V1.
El séptimo corte extiende el mismo gobierno a perfiles tributarios y conserva cada
cambio de estado como una nueva revisión versionada, también sin migración nueva.
El octavo corte permite crear una revisión explícita de tratamiento, descripción
y vigencia, conservando identidad, código, nombre e historial relacional.
El noveno corte publica ese historial mediante una consulta autorizada y una
pestaña neutral de solo lectura, aislada por empresa y ordenada desde la revisión
más reciente.
El décimo corte declara con propietario de plataforma los 18 selectores nativos y
completa la cobertura neutral 77/77. El undécimo implementa el retorno contextual
de los selectores renderizados por plugins mediante un token opaco, borrador POST
filtrado en servidor, contexto efímero ligado a sesión/usuario/empresa y refresco
de opciones al volver. El duodécimo extiende el mismo límite de seguridad a los
11 usos nativos administrables; los otros siete continúan cerrados o gobernados
por composición y no ofrecen altas arbitrarias.
El decimotercer corte agrega el ciclo activo/inactivo versionado y auditado de
tipos de canal; conserva referencias y excluye inactivos de operaciones nuevas.
El decimocuarto aplica el mismo ciclo a familias de variantes conservando sus
atributos. El decimoquinto agrega una revisión explícita del nombre visible de
`CHANNEL_KIND` y un historial append-only de solo lectura mediante V3 privada,
sin cambiar el código estable ni exponer entidades JPA.
El decimosexto agrega revisión e historial append-only de unidades, categorías,
marcas y etiquetas mediante V2 privada de `commercial_catalog`, conservando su
identidad y excluyendo el autoparentesco de categorías.
El decimoséptimo agrega reemplazo seguro mediante V3 privada: crea una identidad
sucesora, inactiva e inmoviliza el origen, conserva referencias existentes y
declara los dos selectores cerrados propios del formulario.
El decimoctavo agrega revisión estructural e historial append-only de familias
mediante V4 privada y declara los dos selectores cerrados del editor de atributos.
El decimonoveno corte agrega el selector gobernado `item_variant_family` a
Artículos y servicios y completa la asignación neutral/versionada. Sólo ofrece
familias activas de la empresa; el caso de uso vuelve a resolver y bloquear la
familia vigente, compara revisión, valida atributos declarados/obligatorios/tipos
y persiste la revisión exacta. Una selección manipulada se rechaza antes de la
autorización de mutación y se normaliza al recargar la pantalla.

El inventario vigente declara los 71 selectores de plugins:

| Módulo | Declarados/total | Prueba de frontera |
|---|---:|---|
| `business-partners` | 9/9 | cuatro clases usan catálogo privado, ruta autorizada, ciclo activo/inactivo, revisión de nombre e historial visible append-only; los tres consumidores nuevos se revalidan en servidor |
| `commercial-catalog` | 35/35 | todos tienen fuente; definiciones simples tienen ciclo activo/inactivo, revisión, historial y reemplazo seguro; perfiles y familias tienen ciclo, revisión e historial; asignación versionada de familias implementada |
| `inventory` | 27/27 | igualdad exacta entre campos `SELECT` y fuentes |
| **Total** | **71/71** | todos los selectores de plugins tienen fuente declarada |

## Pruebas

Todos los comandos exitosos usaron Java 21.0.11 desde `.tools/jdk` y el repositorio
Maven local del proyecto.

| Gate | Resultado |
|---|---|
| regresión del bootstrap Windows con `JAVA_HOME` Java 8 y Maven global | `mvnw.cmd --version` seleccionó Java 21.0.11 y Maven 3.9.16 desde `.tools` |
| `mvnw.cmd -pl plugin-api test` | 21 pruebas, verdes |
| `mvnw.cmd -pl tools/plugin-scaffold -am test` después de aislar temporales | 30 pruebas, verdes: 21 de `plugin-api` y 9 de `plugin-scaffold` |
| prueba focalizada `ShellScreenInteractionViewTest` con `web-shell -am` | 12 módulos del corte y 1 prueba focalizada, verdes |
| pruebas de declaración de los tres plugins | 3 pruebas, verdes; compilaron los 9 módulos del reactor acotado |
| validación XML de `view.xhtml` | documento bien formado, raíz `html` |
| `mvnw.cmd verify` | 24/24 módulos verdes; WAR construido; 24 pruebas ArchUnit/composición verdes |
| G0 documental | 253 Markdown, 994 enlaces locales, 0 errores UTF-8 y 0 enlaces rotos |
| descriptor, fuentes y handler de definiciones | 6 pruebas focales verdes |
| renderer de `/catalog/definitions` con `web-shell -am` | 12 módulos verdes; 3 pruebas del handler y 4 del renderer verdes |
| regresión de `commercial-catalog` y `web-shell` | 12 módulos verdes; 25 pruebas de `web-shell`, 0 fallos |
| compilación de recorrido E2E actualizado | `tests/e2e-tests -am test` verde; cuatro fuentes Playwright compiladas |
| `mvnw.cmd verify` después del segundo corte | 24/24 módulos verdes; WAR construido; 51 pruebas de catálogo, 25 de shell y 24 de arquitectura/composición verdes |
| G0 documental después del segundo corte | 254 Markdown; 0 enlaces rotos, 0 errores UTF-8, 0 mojibake y 0 secretos detectados |
| Dockerfile e imagen candidata | `--check` sin advertencias; build `verified` 24/24 módulos verde; imagen `j11-s8-c02-definitions` |
| runtime | se recreó sólo `app`; PostgreSQL, Keycloak y volúmenes se conservaron; liveness/readiness `UP` |
| Playwright focal | 1 prueba, 0 fallos, 0 errores, 0 omitidas; 21 PNG en 375/720/1280 px |
| handler de definiciones con etiqueta | 4 pruebas focales, verdes |
| renderer y textos de definiciones | 4 pruebas focales, verdes; 12 módulos compilados |
| regresión del corte | `commercial-catalog` 52 pruebas y `web-shell` 25 pruebas, verdes |
| `mvnw.cmd verify` después del tercer corte | 24/24 módulos verdes; WAR y 24 pruebas ArchUnit/composición verdes |
| imagen candidata con etiquetas | Dockerfile `--check` sin advertencias; build `verified` 24/24 módulos verde |
| Playwright focal con etiqueta | 1 prueba, 0 fallos, 0 errores, 0 omitidas; 23 PNG y 2.937.690 bytes |
| contrato, fuentes y handler de familias | 7 pruebas focales verdes |
| renderer de familias y XHTML | 5 pruebas focales verdes; XML bien formado |
| compilación del Playwright ampliado | cuatro fuentes de prueba compiladas con Java 21 |
| regresión focal después de corregir el postback JSF | 11 pruebas verdes: 4 del handler y 7 del shell |
| `mvnw.cmd verify` del cuarto corte | 24/24 módulos verdes en 2:34 min; WAR y 24 pruebas ArchUnit/composición verdes |
| Dockerfile e imagen del cuarto corte | `--check` sin advertencias; build limpio `verified` 24/24 módulos verde; imagen `j11-s8-c02-variants` |
| runtime del cuarto corte | sólo se recreó `app`; PostgreSQL y Keycloak conservaron sus IDs; liveness/readiness `UP` |
| Playwright focal de familias | dos ejecuciones finales consecutivas verdes; 1 prueba, 0 fallos, 0 errores y 28 PNG |
| G0 documental final | 254 Markdown y 1.003 enlaces locales; 0 enlaces rotos, 0 errores UTF-8, 0 mojibake y 0 secretos detectados |
| dominio y aplicación de tipos de canal | 3 pruebas focales verdes |
| migración V2, mapeos JPA y repositorio | 8 pruebas focales verdes; 11 pruebas JPA sobre PostgreSQL 18.4, sin filtración entre empresas |
| contrato, fuentes, handlers y renderer de tipos de canal | pruebas focales verdes; la falla inicial por slots ausentes fue corregida y repetida |
| compilación del Playwright ampliado | `tests/e2e-tests -am -DskipTests test`, exit 0 |
| `mvnw.cmd verify` del quinto corte | 24/24 módulos verdes en 2:32 min; WAR y 24 pruebas ArchUnit/composición verdes |
| imágenes verificadas del quinto corte | Dockerfiles `--check` sin advertencias; builds internos verdes; `app:j11-s8-c02-channel-kinds` y `migrator:j11-s8-c02-channel-kinds` |
| migración y runtime del quinto corte | V2 aplicada una vez; sólo se recreó `app`; PostgreSQL y Keycloak conservaron IDs y estado saludable |
| liveness/readiness | HTTP 200 y `UP`; readiness confirmó catálogo, configuración, base, migraciones y OIDC |
| Playwright focal de tipos de canal | 1 prueba, 0 fallos, 0 errores y 0 omitidas en 43,58 s; 17 PNG y 2.320.440 bytes |
| revisión visual y logs | originales 1280/720/375 revisados; sin overflow normal ni controles cortados; 0 errores o excepciones en logs |
| G0 documental del quinto corte | 255 Markdown; 0 enlaces rotos, 0 errores UTF-8, 0 mojibake y 0 secretos detectados |
| aplicación del ciclo de definiciones simples | `CatalogDefinitionServiceTest`: 4 pruebas verdes; inactivar/reactivar, versión obsoleta, permiso y auditoría cubiertos |
| persistencia PostgreSQL del sexto corte | prueba focal verde sobre PostgreSQL 18.4 para unidad, categoría, marca y etiqueta; aislamiento por empresa y conflicto de versión cubiertos |
| regresión PostgreSQL completa de `commercial-catalog` | 13/13 pruebas Testcontainers verdes: 8 de repositorios/JPA y 5 de migración/idempotencia; reactor 5/5 módulos verde |
| handler, renderer y regresión afectada | focal UI: 10 pruebas verdes; `web-shell -am test`: 12/12 módulos verdes, catálogo 59 pruebas y shell 27 |
| `mvnw.cmd verify` del sexto corte | 24/24 módulos verdes en 3:05 min; WAR y 24 pruebas ArchUnit/composición verdes |
| imagen candidata del sexto corte | Dockerfile `--check` sin advertencias; build `verified` 24/24 módulos verde; `logixone/app:j11-s8-c02-definition-lifecycle` |
| runtime y health del sexto corte | sólo se recreó `app`; PostgreSQL y Keycloak conservaron sus IDs; liveness/readiness `UP` con cinco checks |
| Playwright focal del sexto corte | 1 prueba, 0 fallos, 0 errores y 0 omitidas en 61,61 s; 31 PNG y 4.353.458 bytes |
| revisión visual del ciclo | originales 1280/720/375 revisados; confirmación visible, acciones legibles, sin overflow ni controles cortados; 0 errores/excepciones en logs |
| G0 documental del sexto corte | 255 Markdown y 1.007 enlaces locales; 0 enlaces rotos, 0 errores UTF-8 y 0 archivos con mojibake |
| aplicación y UI del ciclo tributario | `CatalogDefinitionServiceTest` 5 pruebas; focal de handler/descriptor 11 pruebas; renderer 5 pruebas, todas verdes |
| persistencia PostgreSQL del séptimo corte | `CommercialCatalogJpaValidationPostgreSqlIT` 9/9 verdes sobre PostgreSQL 18.4; historial de tres revisiones, empresa y versión obsoleta cubiertos |
| `mvnw.cmd verify` del séptimo corte | 24/24 módulos verdes en 3:26 min; WAR construido y 24 pruebas ArchUnit/composición verdes |
| imagen candidata del séptimo corte | Dockerfile `--check` sin advertencias; build `verified` 24/24 módulos verde; `logixone/app:j11-s8-c02-tax-profile-lifecycle` |
| runtime y health del séptimo corte | sólo se recreó `app`; PostgreSQL `3d3243fa0cda` y Keycloak `0bc73b911f34` conservaron identidad; liveness/readiness `UP` |
| Playwright focal del séptimo corte | ejecución final: 1 prueba, 0 fallos, 0 errores y 0 omitidas en 57,90 s; 34 PNG y 4.927.086 bytes |
| revisión visual y logs del séptimo corte | originales 1280/720/375 revisados; sin overflow ni controles cortados; ventana final sin `ERROR`, `SEVERE`, excepciones ni causas encadenadas |
| G0 documental del séptimo corte | 258 Markdown y 1.037 enlaces locales; 0 errores UTF-8, 0 archivos con mojibake y 0 enlaces rotos |
| aplicación y UI de revisión tributaria | `CatalogDefinitionServiceTest` 6/6, `CommercialCatalogTaxProfileScreenHandlerTest` 6/6 y `CommercialCatalogScreenRendererTest` 5/5 verdes |
| persistencia PostgreSQL del octavo corte | `CommercialCatalogJpaValidationPostgreSqlIT` 10/10 verde sobre PostgreSQL 18.4; historial inmutable, revisión activa, empresa y versión obsoleta cubiertos |
| regresión acotada del octavo corte | 12/12 módulos y 333 pruebas verdes |
| `mvnw.cmd verify` del octavo corte | 24/24 módulos verdes en 1:27 min; WAR construido y 24 pruebas ArchUnit/composición verdes |
| imagen candidata del octavo corte | Dockerfile `--check` sin advertencias; build `verified` 24/24 módulos verde; `logixone/app:j11-s8-c02-tax-profile-revision` |
| runtime y health del octavo corte | sólo se recreó `app`; PostgreSQL `3d3243fa0cda` y Keycloak `0bc73b911f34` conservaron identidad; liveness/readiness `UP` |
| Playwright ampliado del octavo corte | 1 prueba, 0 fallos, 0 errores y 0 omitidas en 107,1 s; 37 PNG y 5.395.048 bytes |
| revisión visual y logs del octavo corte | originales 1280/720/375 revisados; tabs, formulario, mensaje y acción legibles, sin overflow normal; ventana final sin `ERROR`, `SEVERE`, excepciones ni causas encadenadas |
| G0 documental del octavo corte | 270 Markdown y 1.144 enlaces locales; 0 enlaces rotos, errores UTF-8, archivos con mojibake y secretos detectados |
| aplicación y UI del historial tributario | `CatalogDefinitionServiceTest` 7/7, `CommercialCatalogTaxProfileScreenHandlerTest` 6/6 y `CommercialCatalogScreenRendererTest` 5/5 verdes; XHTML bien formado |
| persistencia PostgreSQL del noveno corte | `CommercialCatalogJpaValidationPostgreSqlIT` 10/10 verde sobre PostgreSQL 18.4; orden descendente, revisión vigente y aislamiento entre empresas cubiertos |
| regresión acotada del noveno corte | 12/12 módulos y 334 pruebas verdes; cuatro fuentes Playwright compilaron |
| `mvnw.cmd verify` final del noveno corte | 24/24 módulos verdes en 2:57 min; WAR construido y 24 pruebas ArchUnit/composición verdes |
| imagen candidata del noveno corte | build `verified` 24/24 módulos verde en 1:57 min; `logixone/app:j11-s8-c02-tax-profile-history`, digest `sha256:432bdd96bca653ed8a3a2e6883dff2b99189ea7cf93e13895548ae47a55f5820` |
| runtime y health del noveno corte | sólo se recreó `app`; PostgreSQL `3d3243fa0cda` y Keycloak `0bc73b911f34` conservaron identidad; los tres servicios y cinco checks de readiness quedaron `UP` |
| Playwright final del noveno corte | 1 prueba, 0 fallos, 0 errores y 0 omitidas en 73,57 s; 40 PNG y 5.919.119 bytes |
| revisión visual y logs del noveno corte | originales 1280/720/375 revisados; cinco columnas visibles en expandido y tarjetas sin overflow en medio/compacto; ventana final sin `ERROR`, `SEVERE`, excepciones ni causas encadenadas |
| G0 documental del noveno corte | 270 Markdown y 1.148 enlaces locales; 0 enlaces rotos, errores UTF-8, archivos con mojibake y secretos detectados |

El primer intento Maven heredó Java 8. La recurrencia se clasificó como defecto del
flujo y se corrigió `mvnw.cmd`: ahora selecciona automáticamente el JDK 21.0.11 y
el Maven home validados bajo `.tools`. La regresión se reprodujo forzando como
entorno inicial Java 8 y el perfil Maven global; `mvnw.cmd --version` informó Java
21.0.11 y ambos runtimes dentro del proyecto. El gate completo descubrió además
que JUnit heredaba el temporal global sin acceso; el mismo bootstrap pasó a aislar
`TEMP` y `TMP` en `.tools/tmp` antes de repetir la prueba. Un intento focalizado
adicional no inició por interpretación de un parámetro `-D` de PowerShell; se
corrigió el quoting y no se ocultó ninguna prueba fallida.

Durante el segundo corte, la primera compilación focal del handler detectó un
paréntesis sobrante. Se corrigió el error y se repitió exactamente el alcance:
seis pruebas quedaron verdes. Una primera invocación adicional tampoco inició por
el quoting de una propiedad `-D` en PowerShell; se corrigió la invocación antes de
registrar resultados. Ninguna prueba ejecutada quedó fallando.

En el tercer corte, el primer Playwright sí inició y falló después de crear y
filtrar una etiqueta: la captura compacta dejó el viewport en 375 px y el paso
siguiente buscó un enlace del menú lateral oculto. Se restauró explícitamente el
viewport a 1280 px al finalizar ese subrecorrido y se repitió exactamente la misma
suite; quedó verde. El fallo y su causa no se clasificaron como diferidos.

## Runtime y revisión visual del segundo corte

La imagen `logixone/app:j11-s8-c02-definitions` tiene ID local
`sha256:7675753dffce8d772cdfe82d94917b20cf6069b48b72c7de5aafcb8d6736e6e1`
y 500.886.874 bytes. Se recreó sólo `logixone-app-1`; PostgreSQL, Keycloak,
secretos y volúmenes existentes no fueron modificados. Liveness y readiness
respondieron HTTP 200/`UP`; el filtro posterior de logs encontró cero coincidencias
con `ERROR`, `SEVERE`, `Exception` o `Caused by:`.

`CommercialCatalogVisualIT` ejecutó un escenario verde en 48,55 segundos. Validó
la nueva opción del menú, directorio, alta real de una unidad, uso inmediato en los
selectores de artículo y precio, permiso negativo y restauración del estado. Generó
21 PNG y 2.683.095 bytes en `docs/evidence/screenshots/J11-S8-C02/e2e/`.

Se revisaron visualmente los originales de directorio en 1280, 720 y 375 px y el
alta compacta. No se observaron controles cortados, overflow horizontal normal ni
pérdida de acciones. Checksums representativos:

- directorio 1280: `3E563FBC1F3A52636836CF01DD7B6707D559DB1699F8B1F7506A04E8A263C037`;
- directorio 720: `8D54002B517C33900B5C14B179CB33C3D6679043FD0C0703A943D65888B15F19`;
- directorio 375: `89B76944F1A5A68F9B02458273157258FA3FB968F2EFAFB645648E6345C9EF67`;
- alta 375: `0325C3F983F4DC044F3BD1AFD70E8FA98D305CE9242FA4A77131C0C4EF69BD78`.

El primer sondeo manual usó por error `/logixone/api/health/*`, que está protegido,
y recibió `Unauthorized`. Se repitió sobre las rutas públicas correctas
`/logixone/health/live` y `/logixone/health/ready`; ambas quedaron `UP`.

## Runtime y revisión visual del tercer corte

La imagen `logixone/app:j11-s8-c02-tags` tiene ID local
`sha256:934029dbf4f0b90336e348962d28d57bf896bba6789f61f8b9a3fb905254a588`
y 500.887.317 bytes. Se recreó únicamente `logixone-app-1`; PostgreSQL, Keycloak y
los volúmenes se conservaron. Liveness y readiness respondieron `UP` y el filtro
de los últimos diez minutos de logs produjo cero coincidencias con `ERROR`,
`SEVERE`, `Exception` o `Caused by:`.

`CommercialCatalogVisualIT` terminó verde en 38,93 segundos después de la
corrección del guion. Además del recorrido anterior, registró una etiqueta, volvió
al directorio y filtró por el tipo `TAG`. Generó 23 PNG y 2.937.690 bytes en
`docs/evidence/screenshots/J11-S8-C02-tags/e2e/`.

Se revisaron visualmente en resolución original el directorio expandido, el alta
compacta de etiqueta y el resultado compacto filtrado. Los campos no aplicables
indican expresamente “sólo unidades” o “sólo categorías”; no se observaron cortes
ni overflow horizontal normal. Checksums representativos:

- directorio 1280: `E2D1E0A0F45CF2587D1ABA923B180F226B78F6715AD045F58B31BD7AD7AB16A7`;
- alta de etiqueta 375: `FE1359002FBD58B0A230B430DF68EC3C291B26CC376061593C1FC3675B069B29`;
- etiquetas filtradas 375: `AF663A1C3F86EE94FBD0F73D27E1AA75BECDD8C8945C47020A0D6E1A12492A71`.

## Runtime y revisión visual del cuarto corte

La imagen `logixone/app:j11-s8-c02-variants` tiene ID local
`sha256:32c6a4771e771168892202c53846d365671052f6bc6bfc6cde255f806e7a6382`
y 500.903.554 bytes. Se recreó únicamente `logixone-app-1`; PostgreSQL conservó
el contenedor `3d3243fa0cda`, Keycloak conservó `0bc73b911f34` y no se recrearon
sus volúmenes. Liveness respondió `UP`; readiness respondió `UP` para catálogo,
configuración, base de datos, migraciones y OIDC. El filtro final de logs produjo
cero coincidencias con `ERROR`, `SEVERE`, `Exception` o `Caused by:`.

`CommercialCatalogVisualIT` terminó con dos ejecuciones finales consecutivas
verdes, de 42,96 y 42,71 segundos. El último recorrido generó 28 PNG y 3.929.581
bytes en `docs/evidence/screenshots/J11-S8-C02-variants/e2e/`. Registró una familia
real con `COLOR` y `TALLA`, conservó ambos atributos ordenados durante los postbacks,
abrió el detalle, filtró el directorio, comprobó el menú y denegó la ruta sin el
permiso `commercial_catalog.definitions.manage` antes de restaurarlo.

Los primeros intentos revelaron tres defectos que no se difirieron. El locator de
una definición encontraba simultáneamente la tabla de escritorio y la lista móvil;
se acotó a la tabla visible del paso. Después, el resumen `DISPLAY_TEXT` no viajaba
entre solicitudes porque `ShellViewBean` es `RequestScoped`; el shell ahora lo
conserva en un `h:inputHidden` y el handler vuelve a validar el contenido. Por último,
una medición tomada inmediatamente después de cambiar a 375 px informó 380 px de
ancho una vez; el gate espera dos ciclos de render, informa hasta doce elementos
causantes si reaparece y quedó verde dos veces consecutivas.

Se revisaron en resolución original el directorio en 1280, 720 y 375 px, el alta
compacta con dos atributos y el resultado compacto filtrado. También se revisó la
lista de precios compacta que disparó la medición transitoria. No se observaron
controles cortados, pérdida de acciones ni overflow horizontal normal. Checksums:

- directorio 1280: `05721B99B50D866309E9660C7AC9F36B6FC5DBE204646BE4A93818049646754E`;
- directorio 720: `ABF8C20C3477609C7A7029EF5598EE993BA6B7090AF790EA122BB167C352DCB0`;
- directorio 375: `3702CB86E58338434313FD0E8D029CF4DAB3475F3675753BE546FDD51EB99C80`;
- alta con atributos 375: `15667A40253EA7759489D1164B6895A194FDC20E175E25396E22D65810B30F8E`;
- resultado filtrado 375: `5B640ED550F13F46EB891510381C7961AF6E873D0979B6CE073428CD129A65D6`.

## Runtime y revisión visual del quinto corte

La imagen `logixone/app:j11-s8-c02-channel-kinds` tiene ID local y digest
`sha256:38be695a4d7b5d5cfe20eea69b9e25fceec70a1e0b71c4eafa4ea73e1653b388`
y 500.926.884 bytes. El migrador
`logixone/migrator:j11-s8-c02-channel-kinds` tiene digest
`sha256:9297028a59cfc71c7b7e76891e472aecf799fbbc616f2c7ec4bac37e6766ddf0`
y 105.303.549 bytes. Ambos builds verificados ejecutaron su reactor interno sin
fallos. El migrador aplicó exactamente V2 de `business_partners`; los demás
esquemas no requirieron migraciones nuevas.

Se recreó únicamente `logixone-app-1`. PostgreSQL conservó el contenedor
`3d3243fa0cda`, Keycloak conservó `0bc73b911f34` y ambos continuaron saludables;
no se recrearon sus volúmenes. Liveness respondió `UP` para la aplicación y
readiness respondió `UP` para catálogo, configuración, base, migraciones y OIDC.
Un sondeo diagnóstico inicial usó por error las rutas protegidas `/api/health/*`
y obtuvo el `401` esperado; se repitió con `/health/live` y `/health/ready`, ambas
públicas y verdes. El filtro posterior de logs produjo cero coincidencias con
`ERROR`, `SEVERE`, `Exception` o `Caused by:`.

`BusinessPartnersVisualIT` terminó verde en 43,58 segundos. Registró un tipo
`Telegram empresarial`, confirmó su consulta empresarial, creó un socio, usó el
nuevo valor en **Tipo de canal**, ejercitó autorización negativa y restauró la
activación del plugin. Generó 17 PNG y 2.320.440 bytes en
`docs/evidence/screenshots/J11-S8-C02-channel-kinds/e2e/`.

Los dos intentos anteriores revelaron defectos que se corrigieron y repitieron. El
primer render exponía el mismo nombre accesible **Tipos de canal** en el título y
en los resultados; el segundo pasó a llamarse **Tipos disponibles** y su prueba
exacta quedó verde. Luego el guion intentó usar el menú lateral después de capturar
el formulario a 375 px, donde el menú está correctamente replegado; ahora restaura
1280 px antes de navegar. La misma suite se repitió completa y no quedó ninguna
prueba fallando.

Se revisaron en resolución original el directorio expandido y compacto, el alta
compacta y la pestaña **Contacto** expandida con el tipo recién creado. La tabla se
convierte en tarjetas en móvil, las acciones siguen visibles y no se observaron
controles cortados ni overflow horizontal normal. Checksums representativos:

- directorio 1280: `F0FAC500B061A1779926EFAFEA2EBD5DABF9802995D859DADFC346272D98987C`;
- directorio 720: `3D539FF11BAE97E73887FAB7C5FDC448A135420FB1D648BB151B58C45E601547`;
- directorio 375: `5BF8F4F5A137D797D52640D14E39561838583FF7C36B4A92A6ADAEB6FA28E29C`;
- alta 375: `E8ABE90D1EF077FA79E3ED350D822CB37B39499708530F0D1E23DD790B1C0A4F`;
- contacto 1280: `5CAE15DB3C63822BA1DD011CDB7E818955ECF3892AA291A0A19430B517318EBC`.

## Runtime y revisión visual del sexto corte

La imagen `logixone/app:j11-s8-c02-definition-lifecycle` tiene digest local
`sha256:78678e68b2ec6fc8573565b03dac46f72ace580a128f2d75baf46421afae8be7`
y 500.937.240 bytes. Su build `verified` ejecutó 24/24 módulos verdes. Se recreó
únicamente `logixone-app-1` (`ec55cdffa2e2`); PostgreSQL conservó
`3d3243fa0cda` y Keycloak `0bc73b911f34`, ambos saludables y con sus volúmenes
intactos. Liveness y readiness respondieron `UP`; readiness confirmó catálogo,
configuración, base, migraciones y OIDC.

`CommercialCatalogVisualIT` terminó verde en 61,61 segundos. Creó una unidad, la
usó en un artículo, abrió su pestaña **Estado**, la inactivó, confirmó que seguía
consultable mediante el filtro **Inactivos** y la reactivó antes de terminar. El
recorrido también repitió seguridad negativa y restauró permisos/activación. Las
31 capturas suman 4.353.458 bytes en
`docs/evidence/screenshots/J11-S8-C02-lifecycle/e2e/`.

Se revisaron en resolución original las tres capturas nuevas. La confirmación y
las acciones permanecen legibles, el menú se repliega en compacto y no hay
controles cortados ni overflow horizontal normal. El filtro de logs de los diez
minutos de build/runtime/recorrido encontró cero coincidencias con `ERROR`,
`SEVERE`, `Exception` o `Caused by:`. Checksums SHA-256:

- inactiva 1280: `355932F6ED65D5662E61BE4AB139CF30C31FF19A1B142E8B9E0978C5D25390C3`;
- inactiva 720: `92574BCDF091747B05A22CAD68F296ACFD1870F2EA89D8A2212B72067999C72F`;
- inactiva 375: `FF6D6D0D28CEDA8820E1810DC88A66522899666F79AEF613D53C12E92A79A4AC`.

La primera compilación focal del sexto corte detectó imports ausentes en el
repositorio JPA; se corrigió y la misma prueba quedó verde. El primer intento de
integración no encontró el daemon Docker; Docker Desktop se inició con aprobación
y el gate exacto pasó fuera del sandbox, cuya tubería Docker no era accesible. El
primer test de renderer detectó que faltaba registrar la sección **Estado** en el
floorplan; se corrigió y se repitieron focal, regresión y `verify` sin pruebas
fallando.

## Runtime y revisión visual del séptimo corte

La imagen `logixone/app:j11-s8-c02-tax-profile-lifecycle` tiene digest local
`sha256:20dd8a7381ad6c1bac93652560af551f9c80052b63201ee664920bc80a2eea75`
y 500.941.030 bytes. El build `verified` ejecutó 24/24 módulos verdes. Se recreó
únicamente `logixone-app-1` (`3d7e3a70173d`); PostgreSQL conservó
`3d3243fa0cda` y Keycloak `0bc73b911f34`. Los tres quedaron saludables y
liveness/readiness respondieron `UP`.

`CommercialCatalogVisualIT` terminó verde en 57,90 segundos. Creó un perfil
tributario, lo usó en un artículo, abrió **Estado**, lo inactivó, confirmó su
consulta mediante el filtro **Inactivos** y lo reactivó. Repitió seguridad negativa
y restauró permisos y activaciones. Las 34 capturas suman 4.927.086 bytes en
`docs/evidence/screenshots/J11-S8-C02-tax-profile-lifecycle/e2e/`.

La revisión original de 1280, 720 y 375 px confirmó mensajes y acciones legibles,
menú replegado en compacto y ausencia de overflow horizontal normal o controles
cortados. Checksums SHA-256:

- inactivo 1280: `5BB5D795F8DCA6D493DB157A08ABF02A404E0EB490A36F46567A9E04A8A0993F`;
- inactivo 720: `3E6BA2F06064B4D9374EDF799AC66D27898FBBB88FFA36E1367F1E65475C8EE4`;
- inactivo 375: `6A67CA538FFD89569CBF4723EB87312E6CDEE5F72567774F669C408F140D7387`.

El primer Testcontainers se ejecutó dentro del sandbox y falló antes de las
aserciones por acceso denegado al socket Docker; la repetición fuera del sandbox
quedó 9/9 verde. Playwright tuvo primero un bloqueo equivalente al crear el
directorio de capturas. Ya fuera del sandbox, dos ejecuciones detectaron nombres
accesibles incorrectos en el test —campo de búsqueda y acción de fila—; ambos
locators se alinearon al renderer y la tercera ejecución completa quedó verde. No
se ocultó ni difirió ninguna prueba ejecutada.

## Runtime y revisión visual del octavo corte

La imagen `logixone/app:j11-s8-c02-tax-profile-revision` tiene digest local
`sha256:2b137bb395160f3be1cfdc0a483e124ffc827dbf9fc932ae8925ef59dcb5208a`
y 500.945.181 bytes. El build `verified` ejecutó 24/24 módulos verdes en 2:03
min. Se recreó únicamente `logixone-app-1` (`05401dd554a9`); PostgreSQL conservó
`3d3243fa0cda` y Keycloak `0bc73b911f34`. Los tres quedaron saludables y
liveness/readiness respondieron `UP`.

`CommercialCatalogVisualIT` terminó verde en 107,1 segundos. Creó un perfil, lo
usó en un artículo, abrió **Nueva revisión**, cambió tratamiento, descripción y
vigencia, confirmó la identidad estable y luego ejecutó el ciclo inactivo/activo.
Repitió seguridad negativa y restauró permisos y activaciones. Las 37 capturas
suman 5.395.048 bytes en
`docs/evidence/screenshots/J11-S8-C02-tax-profile-revision/e2e/`.

La revisión original de 1280, 720 y 375 px confirmó tabs, mensaje, formulario y
acción legibles, menú replegado en compacto y ausencia de overflow horizontal
normal o controles cortados. La ventana reciente del contenedor no contiene
`ERROR`, `SEVERE`, excepciones ni causas encadenadas. Checksums SHA-256:

- revisión 1280: `ACBB18526A80466BF7A848B239E248EC9EB8EFB9610018F13E8CC471D18B808D`;
- revisión 720: `E9CA5919F226097749C55393CA9293B6C34D04CBEE36AD375E99618FF6F18D89`;
- revisión 375: `7CC561D2A88B6884F3599145A6A421727734FBD97E8C3BE5BCA7D4C9394A4C31`.

El primer comando focal no resolvió el Wrapper desde PowerShell y el segundo no
construyó el API dependiente; ambos fallaron antes de pruebas. El comando corregido
usó `./mvnw.cmd`, `-am` y argumentos `-D` citados. Testcontainers tampoco accedió
al daemon dentro del sandbox; su repetición autorizada quedó 10/10 verde. El primer
test del renderer encontró la sección visual omitida en el floorplan; se agregó y
se repitieron focal, regresión, `verify`, imagen y Playwright sin fallas.

## Runtime y revisión visual del noveno corte

La imagen final `logixone/app:j11-s8-c02-tax-profile-history` tiene digest local
`sha256:432bdd96bca653ed8a3a2e6883dff2b99189ea7cf93e13895548ae47a55f5820`
y 500.949.861 bytes. El build `verified` ejecutó 24/24 módulos verdes en 1:57
min. Se recreó únicamente `logixone-app-1` (`3253113f79f1`); PostgreSQL conservó
`3d3243fa0cda` y Keycloak `0bc73b911f34`. Los tres quedaron saludables y
readiness respondió `UP` para catálogo, configuración, base, migraciones y OIDC.

`CommercialCatalogVisualIT` terminó verde en 73,57 segundos sobre la imagen final.
Creó un perfil, lo usó en un artículo, generó una nueva revisión, abrió
**Historial** y comprobó dos filas ordenadas: la versión vigente primero y la
histórica después. Luego ejecutó el ciclo inactivo/activo y la seguridad negativa,
restaurando permisos y activaciones. Las 40 capturas suman 5.919.119 bytes en
`docs/evidence/screenshots/J11-S8-C02-tax-profile-history/e2e/`.

La revisión visual original de 1280, 720 y 375 px confirma tratamiento,
descripción y vigencia completos, tabla en expandido, tarjetas en medio/compacto,
menú replegado y ausencia de overflow horizontal normal. La ventana reciente del
contenedor no contiene `ERROR`, `SEVERE`, excepciones ni causas encadenadas.
Checksums SHA-256:

- historial 1280: `C52C3947D5BA81F765665D33501521BD5549482AE240AC412CA24BFED65818A6`;
- historial 720: `93415C690A70C75815198749C072D3C74C1C10DA3D5F1042A8E67E245BE1B0E5`;
- historial 375: `2734911DABB8865B64986EFF3488EDD91B3280735ED7AC44211D3ADA7D81AF04`.

El primer Testcontainers del corte se ejecutó dentro del sandbox y falló antes de
las aserciones por falta de acceso a Docker. En la primera repetición autorizada,
una expectativa del test contenía un literal de acento dañado; se corrigió y el
gate quedó 10/10 verde. El renderer detectó sucesivamente que `DATA_TABLE` no podía
pertenecer a una región de detalle y que `history` no estaba declarada como región;
ambas restricciones se corrigieron antes de repetirlo 5/5 verde. La primera
ejecución Playwright quedó funcionalmente verde, pero la revisión humana encontró
que el layout expandido recortaba **Vigencia**. El formulario pasó a ocupar las dos
columnas; después se repitieron prueba focal, `mvnw.cmd verify`, imagen verificada,
runtime, Playwright y revisión visual. Un intento Maven aislado sin `-am` no llegó
a ejecutar pruebas por dependencias reactor ausentes; el comando corregido quedó
verde. No se ocultó ni difirió ninguna prueba ejecutada.

## Décimo corte - selectores nativos 2026-08-03

| Control | Resultado |
|---|---|
| contrato | `SelectorSourceMetadata`, propietario `PLATFORM`/`PLUGIN` y definición de plataforma compatibles en `plugin-api` 0.4.2 |
| inventario | 18/18 nativos y 59/59 de plugins; cobertura neutral 77/77 |
| renderer | composite Faces del shell con origen/clase y ruta sólo bajo permiso global declarado |
| fuentes cerradas | permisos, personalizaciones físicas y filtros de auditoría no ofrecen altas arbitrarias |
| prueba mínima | `plugin-api` 22/22 verde después de corregir la validación temprana del constructor compacto |
| pruebas focales | catálogo, autorización y recursos Faces 7/7 verdes |
| regresión | 12 módulos que alimentan `web-shell`; 342 pruebas, 0 fallos, 0 errores, 0 omitidas |
| `mvnw.cmd verify` del décimo corte | 24/24 módulos verdes en 2:39 min; WAR construido y 24 pruebas ArchUnit/composición verdes |

No se agregó migración, tabla, relación JPA, lectura SQL cruzada ni una identidad
de plugin ficticia para el kernel. La autorización de la ruta se resuelve con la
autoridad global actual y el destino conserva su filtro/guarda de servidor.

## Undécimo corte - retorno contextual de plugins 2026-08-03

| Control | Resultado |
|---|---|
| borrador seguro | sólo `TEXT_INPUT`/`SELECT` renderizados y habilitados; 96 campos, 2.048 caracteres por valor, 16.384 totales y 32.768 bytes de payload como límites cerrados |
| transporte | valores de negocio por POST; la URL contiene sólo ruta, modo/pestaña y token opaco |
| contexto de sesión | máximo cuatro, TTL de diez minutos, un consumo, ligado a usuario/empresa/revisión; limpieza al cambiar empresa o cerrar sesión |
| retorno | origen/destino reautorizados, modo/pestaña/recurso restaurados, handler reejecutado y opciones refrescadas |
| pruebas focales | 8/8 verdes: filtrado, límites, POST, expiración, un uso, aislamiento, pasivación y recursos Faces/JS |
| regresión | 12/12 módulos verdes; `web-shell` 42 pruebas, 0 fallos, 0 errores, 0 omitidas |
| gate integral | `mvnw.cmd verify` 24/24 módulos verde en 2:34 min; WAR y 24 pruebas ArchUnit/composición |
| imagen | `logixone/app:j11-s8-c02-selector-return`, `sha256:10c3d3da3589604da66044967f9dc802d33cef9f707ccc7fc2b6d96f6e646462`, 500.979.265 bytes; build final interno `verified` 24/24 en 1:01 min |
| runtime | sólo `app` pasó a `47e4bc3916bc`; PostgreSQL `3d3243fa0cda` y Keycloak `0bc73b911f34` conservaron identidad; liveness/readiness `UP` con cinco checks |
| Playwright final | 1/1 verde en 63,60 s sobre `47e4bc3916bc`; alta de unidad desde selector obligatorio, retorno, borrador restaurado, opción nueva, ciclo y seguridad negativa |
| evidencia visual | 44 PNG, 7.253.278 bytes; originales 1280/720/375 del contenedor final revisados sin overflow ni controles cortados |
| logs | ventana final sin `ERROR`, `SEVERE`, excepciones ni causas encadenadas |
| G0 documental | 270 Markdown y 1.152 enlaces locales; 0 errores UTF-8, 0 archivos con mojibake y 0 enlaces rotos |

El primer Playwright ejecutado falló después de completar el nuevo recorrido:
el test contó el campo de la pestaña **Precios** antes de finalizar la navegación
JSF. El artículo ya estaba registrado, con identificador y clasificación. Se agregó
una espera explícita antes de mantener la aserción estricta de unicidad, se compiló
la suite y se repitió completa en verde. No se omitió ni relajó una aserción.

Checksums SHA-256 representativos:

- administrador 1280: `BCEB6AFB4E94C56483C87310F136519DB01CDBA965A8ACC3AD6353CB94286EF5`;
- administrador 720: `B27FAD286D6EAB8828E2E311B940AFEBEBF3AE2F9F990E5E29886A8D0F1FECF7`;
- administrador 375: `09008F1498F68FA350139EBC164A9BDDA9ECA1EB88EE5B95E5086832D60FFB81`;
- formulario restaurado 375: `EA13C6B7EFAD59BBB2D77F8A47C262BE3773C7A399E7B2D1E073EA8547434D57`.

## Duodécimo corte - retorno contextual nativo 2026-08-03

| Control | Resultado |
|---|---|
| cobertura | whitelist cerrada para 11/11 usos nativos administrables de empresa, usuario, membresía y rol empresarial/global; los siete selectores cerrados o de despliegue no crean contexto |
| borrador y transporte | solamente IDs de inputs permitidos por cada plan; valores enviados por POST y retenidos en servidor; la URL expone únicamente un UUID canónico |
| contexto | máximo cuatro entradas, TTL de diez minutos, un solo consumo y binding a usuario autenticado y revisión de sesión; limpieza al cambiar de empresa o cerrar sesión |
| autorización | origen, destino, uso y permiso global se revalidan en servidor; rutas y pares origen/destino pertenecen a un mapa cerrado |
| continuidad JSF | `selectorContext` se conserva explícitamente en los POST del destino y en la banda de retorno; no depende de `unsafe-eval`, `localStorage` ni `sessionStorage` |
| restauración | los beans nativos aplican solamente el mapa permitido y muestran **Opciones actualizadas**; la prueba real conservó usuario y rol global al volver |
| pruebas focales | suite de comportamiento/metadatos/recursos 9/9 verde; prueba final de recursos 4/4 verde en 26,072 s |
| gate integral final | build Docker `verified` con `mvnw -B -Pwith-inventory-demo verify`: 24/24 módulos verdes en 1:50 min; WAR y 24 pruebas ArchUnit/composición verdes; `web-shell` 54/54 |
| imagen final | `logixone/app:j11-s8-c02-native-selector-return`, `sha256:93df0a10f66a109ac1a508f2a8b7d30abf50294e0d62f8e4bfc5c31d014cd11c`, 501.000.028 bytes |
| runtime | sólo `app` pasó a `ee68a8d3513e`; PostgreSQL `3d3243fa0cda` y Keycloak `0bc73b911f34` conservaron identidad y volumen; los tres quedaron `healthy` |
| health y logs | liveness/readiness HTTP 200/`UP`; 1.499 líneas finales revisadas, 0 coincidencias `ERROR`, `SEVERE`, `FATAL`, `Exception` o `Caused by:` |
| Playwright final | 1/1 verde en 106,1 s sobre el manifest final; recorrido nativo de ida, POST en destino, retorno y borrador recuperado, seguido por el escenario acumulado de catálogo |
| evidencia visual | 50 PNG, 9.647.801 bytes; seis originales nativos 1280/720/375 revisados sin overflow horizontal ni fragmentación vertical |
| G0 documental | 270 Markdown y 1.158 enlaces locales; 0 errores UTF-8, 0 archivos con mojibake, 0 enlaces rotos y 0 coincidencias con los cuatro secretos locales en 1.133 archivos de texto |

Durante el corte se detectaron y corrigieron, sin ocultar pruebas, cinco defectos:
el `onclick` de Mojarra intentaba usar `unsafe-eval` bajo CSP; el `f:viewParam`
carecía de setter; `includeViewParams` no preservaba el token en los postbacks;
la propia banda de retorno no lo reenviaba; y el layout medio fragmentaba los
metadatos de asignación/concesión. La captura de borrador pasó a un listener de
captura compatible con CSP, el parámetro quedó validado como UUID canónico, todos
los formularios relevantes conservan el hidden explícito y 720 px apila ambos
formularios.

El primer intento focal con filtro Surefire no llegó a las pruebas de `web-shell`
porque el filtro se aplicó también a módulos ascendentes; se repitió con
`surefire.failIfNoSpecifiedTests=false` y quedó verde. Un Playwright final falló
al contar el encabezado antes de que la navegación JSF lo materializara; se cambió
el helper para esperar primero y mantener después la cardinalidad exacta. La
repetición y la ejecución contra el manifest definitivo quedaron verdes.

Checksums SHA-256 de la evidencia nativa:

- destino 1280: `952B676BA34C6FD8951D2CA7EC1D79BDEA7A7E42D6FCE47F72B9D16FF549751E`;
- destino 720: `73E0B10E72B470B5E1F1BF45DA484E58BD189EADDAB7E04BD94B1EB98975BF01`;
- destino 375: `17FE73134DF15A3A548906D0BFA9DCF81F3AF712C1005ED23E76386FC6A6FF4C`;
- restaurado 1280: `6A0E7653BB2269B6DAB229166F6E66F031B03D4DA949D386E8198E0826A11DE6`;
- restaurado 720: `279D817596B49522E2EC796E745DD2E7A8FBB95F935077180F22ED49D0BBD910`;
- restaurado 375: `04B5903C51FBC1FFD4D4914C57C5A035F5EA887AE6AF4DB61A9CAEC7B3FF7EE1`.

## Decimotercer corte - ciclo de tipos de canal 2026-08-03

| Control | Resultado |
|---|---|
| dominio | cambio activo/inactivo idempotente con versión esperada; identidad y fila conservadas |
| aplicación | autorización por empresa y `business_partners.manage`, auditoría con identidad estable y versiones anterior/resultante |
| persistencia | actualización por empresa/clase/código, conflicto optimista y `@PreUpdate`; la V2 existente ya contenía estado, versión e índice |
| selector consumidor | los tipos inactivos permanecen en administración y referencias históricas, pero se excluyen del alta de canales |
| pruebas de módulo | `plugins/business-partners` 43/43 verde; servicio 5/5, handler de definiciones 5/5 y selector consumidor 5/5 |
| PostgreSQL | `BusinessPartnerJpaRepositoryPostgreSqlIT` 12/12 verde, con inactivación, conservación, conflicto y reactivación |
| shell | renderer 3/3 verde; pestaña **Estado** y acciones **Inactivar tipo**/**Reactivar tipo** materializadas por el contrato neutral |
| gate integral final | `mvnw.cmd verify` 24/24 módulos verde en 3:37 min; WAR y 24 pruebas ArchUnit/composición |
| imagen final | `logixone/app:j11-s8-c02-channel-lifecycle`, `sha256:b978e6ae283763a50897001d7e2956a9d5c627a8fb79e82a1347b029087a3067`, 501.005.637 bytes; build `verified` interno 24/24 en 1:40 min |
| runtime | sólo `app` pasó a `a0f4e77cc169`; PostgreSQL `3d3243fa0cda` y Keycloak `0bc73b911f34` conservaron identidad y volúmenes; los tres `healthy` |
| health y logs | liveness/readiness HTTP 200/`UP`; cero coincidencias `ERROR`, `SEVERE`, `FATAL`, `Exception` o `Caused by:` en la ventana reciente |
| Playwright final | `BusinessPartnersVisualIT` 1/1 verde en 46,13 s: alta, inactivación, consulta histórica, reactivación, consumo y seguridad negativa |
| evidencia visual | 20 PNG y 2.738.907 bytes en `docs/evidence/screenshots/J11-S8-C02-channel-lifecycle/e2e/`; originales 1280/720/375 revisados sin overflow ni controles cortados |
| G0 documental | 270 Markdown y 1.158 enlaces locales; 0 errores UTF-8, 0 archivos con mojibake, 0 enlaces rotos y 0 coincidencias con los cuatro secretos locales en 1.478 archivos de texto |

Las primeras tres ejecuciones Playwright fallaron por una secuencia incorrecta del
test: buscaban las acciones de ciclo en **Resumen** o el texto del estado en
**Estado**. Se explicitó la navegación entre ambas pestañas manteniendo las
aserciones de unicidad y estado; la cuarta ejecución quedó completamente verde.
Antes se corrigieron fallos reales de compilación por el nuevo puerto, dobles de
prueba desactualizados, ausencia de la sección de detalle y una expectativa antigua
de capacidades. El primer Testcontainers dentro del sandbox no obtuvo Docker; la
repetición autorizada quedó 12/12 verde. Ninguna prueba ejecutada se ocultó,
desactivó o dejó fallando.

Checksums SHA-256 de la evidencia de inactivación:

- 1280: `C7FA0234859A9F90ADF7A318F75665811B2E39E095CBDABDDD27106E50147DD5`;
- 720: `A7C6FBC40CE9118793779AB02FABD737D53CD8D2C5927917B87EAE78EFDDA40E`;
- 375: `A1F5F8C272AE1247805E8CA83278671FCB2FE4E815F4019C2AAADCCC9BD10FF9`.

## Decimocuarto corte - ciclo de familias de variantes 2026-08-03

| Control | Resultado |
|---|---|
| aplicación | comando de estado con identidad y versión esperada; permiso `commercial_catalog.definitions.manage`, empresa autenticada y auditoría `INACTIVATE_CATALOG_VARIANT_FAMILY`/`REACTIVATE_CATALOG_VARIANT_FAMILY` |
| persistencia | lectura y actualización por empresa, identidad y versión; conflicto optimista, no-op estable e incremento; la V1 ya incluía estado/versión y no requirió migración |
| preservación | identidad, orden, código, nombre, tipo y obligatoriedad de atributos permanecen iguales al inactivar/reactivar |
| alcance | la administración filtra activos/inactivos; asignación a artículos y validación de familia activa todavía no están expuestas |
| pruebas focales | servicio 8/8, handler 5/5, renderer 5/5 y `plugins/commercial-catalog` 67/67 verdes |
| PostgreSQL | `CommercialCatalogJpaValidationPostgreSqlIT` 11/11 verde con no-op, conflicto, aislamiento por empresa y atributos preservados |
| gate integral | `mvnw.cmd verify` 24/24 módulos verde en 1:59 min; WAR y 24 pruebas ArchUnit/composición |
| imagen final | `logixone/app:j11-s8-c02-variant-family-lifecycle`, `sha256:38c44e0d07da579c7588252e37b538ce3766931e5e8fe2459c9ef95877b0fb30`, 501.009.110 bytes; build `verified` interno 24/24 en 1:06 min |
| runtime | sólo `app` pasó a `5bdf0b0af550`; PostgreSQL `3d3243fa0cda` y Keycloak `0bc73b911f34` conservaron identidad y volúmenes; los tres `healthy` |
| health y logs | liveness/readiness HTTP 200/`UP`; cero coincidencias `ERROR`, `SEVERE`, `Exception` o `Caused by:` en la ventana final |
| Playwright final | `CommercialCatalogVisualIT` 1/1 verde en 71,20 s: alta con dos atributos, inactivación, preservación visible, filtro de inactivas, reactivación y accesibilidad |
| evidencia visual | 53 PNG y 10.222.714 bytes en `docs/evidence/screenshots/J11-S8-C02-variant-family-lifecycle/e2e/`; originales 1280/720/375 revisados sin overflow ni controles cortados |
| G0 documental | 270 Markdown y 1.161 enlaces locales; 0 errores UTF-8, 0 archivos con mojibake, 0 enlaces rotos y 0 filtraciones de secretos locales |

La primera ejecución Playwright falló porque comprobaba atributos mientras la
pestaña activa era **Estado**. Se navegó explícitamente a **Resumen**, se agregó la
comprobación del estado inactivo y se repitió todo el recorrido en verde. La
asignación no se simuló ni se declaró implementada.

Checksums SHA-256 de la evidencia de familia inactiva:

- 1280: `563E96F7B2BFC2326ACFDD0FEBBC48B440D72017C2462C08817DDE833C831753`;
- 720: `83BE5196FDAE6E6715381D57D9927A9237A241552CB6359761630424E2DB9659`;
- 375: `793F614C94BC7AA2C40376CDAFB7ABB6817AFC92491D531D4E60E7B36BA6F8C1`.

## Decimoquinto corte - revisión e historial de tipos de canal 2026-08-03

| Control | Resultado |
|---|---|
| dominio y aplicación | revisión exclusiva del nombre visible; empresa, clase y código estables; versión esperada, permiso `business_partners.manage` y auditoría sin datos personales |
| persistencia | V3 inmutable crea `business_partner_definition_revision`, retroalimenta V2 y conserva una revisión append-only por empresa/clase/código/versión; sin tablas ni JPA cruzadas |
| pruebas focales | servicio 6/6, handler 6/6, renderer 3/3 y módulo `plugins/business-partners` 46/46 verdes |
| PostgreSQL | 19/19 verdes sobre PostgreSQL 18.4: 13 escenarios JPA/repositorio y 6 de migración, con backfill, historial, revisión, empresa, versión y ciclo de estado |
| gate integral | `mvnw.cmd verify` 24/24 módulos verde en 2:10 min; WAR construido y 24 pruebas ArchUnit/composición |
| Dockerfiles | aplicación y migrador pasaron `docker build --check` sin advertencias |
| imagen de aplicación | `logixone/app:j11-s8-c02-channel-history`, `sha256:b13a97f263661e315c64ca7961437ae96b40ab5a5a30a93723b7bfb899986b48`, 501.018.047 bytes; build `verified` interno 24/24 en 1:06 min |
| imagen de migrador | `logixone/migrator:j11-s8-c02-channel-history`, `sha256:0a908b12b8b5384755ae0c2ea556114a42f7468eae74f6ee018799d3903d4f63`, 105.348.213 bytes |
| migración oficial | primera ejecución aplicó una migración y llevó `plg_business_partners` de V2 a V3; segunda ejecución informó `migrations_executed=0`, `schema_version=3` |
| runtime | sólo se recreó `logixone-app-1`; PostgreSQL y Keycloak conservaron volúmenes; contenedor `app` confirmó el digest candidato y quedó `healthy` |
| health | liveness y readiness HTTP 200/`UP`; catálogo, configuración, base, migraciones y OIDC en `UP` |
| Playwright | `BusinessPartnersVisualIT` 1/1 verde en 39,48 s; alta, revisión de nombre, historial, ciclo, retorno al selector y seguridad negativa |
| evidencia visual | 23 PNG y 3.137.997 bytes en `docs/evidence/screenshots/J11-S8-C02-channel-history/e2e/`; 1280/720/375 revisados sin overflow ni controles cortados |
| logs | 829 líneas revisadas; cero coincidencias `ERROR`, `SEVERE`, `Exception` o `Caused by:` |
| G0 documental | 274 Markdown; 0 enlaces rotos, 0 errores UTF-8, 0 archivos con mojibake y 0 filtraciones de secretos |

Checksums SHA-256 del historial visible:

- 1280: `D105D1F20B606E4B8773FE40CDCC9CD9911C5E3EDBDD8A6231C1262D46A7BEBA`;
- 720: `BF0542A049DA8758D41194A41F83988CBDE3208E5A329C65D512FC1FF5FF335D`;
- 375: `0540A9F72940DB9F3DE1473E16B17D8B1391EA67F8432941057A9AB6788D2393`.

La primera prueba focal del handler falló por usar una clave de columna no
declarada y el primer renderer focal falló porque el floorplan no incluía las
secciones nuevas. Se corrigieron ambos defectos y se repitieron sus gates en
verde. Testcontainers no obtuvo el Engine dentro del sandbox; la ejecución
autorizada posterior quedó 19/19. Ninguna prueba fue omitida o relajada.

## Decimosexto corte - revisión e historial de definiciones simples 2026-08-04

| Control | Resultado |
|---|---|
| dominio y aplicación | revisión de nombre y atributos propios de unidad, categoría, marca o etiqueta; empresa, clase, código e identidad estables; versión esperada, permiso `commercial_catalog.definitions.manage`, auditoría técnica y prohibición de autoparentesco |
| persistencia | V2 inmutable crea cuatro tablas privadas append-only y retroalimenta las definiciones V1; historia descendente, conflicto optimista y aislamiento empresarial, sin tablas ni JPA cruzadas |
| pruebas focales | servicio 9/9, handler 7/7, renderer 5/5, recursos de migración 2/2 y `plugins/commercial-catalog` 71/71 verdes |
| PostgreSQL | 17/17 verdes sobre PostgreSQL 18.4: 12 escenarios JPA/repositorio y 5 de migración, con backfill V1→V2, revisión, historia inmutable, empresa, versión y ciclo de estado |
| REST runtime | `HealthEndpointsIT` 2/2 verde; las otras 10 integraciones opt-in permanecieron omitidas por configuración, como está definido para este gate focal |
| gate integral | `mvnw.cmd verify` 24/24 módulos verde en 2:56 min; WAR construido y 24 pruebas ArchUnit/composición |
| Dockerfiles | aplicación y migrador pasaron `docker build --check` sin advertencias |
| imagen de aplicación | `logixone/app:j11-s8-c02-simple-definition-history`, `sha256:65ac722b741008c64aff97085b91932ec05868ef7c215229c822a9024727827a`, 501.031.785 bytes; build interno 24/24 en 1:26 min |
| imagen de migrador | `logixone/migrator:j11-s8-c02-simple-definition-history`, `sha256:eb85e9b3a368c1283ee218191a9c5b3440a0675b1b12d765e1ee5410dc524058`, 105.361.680 bytes; build interno 16/16 en 57,376 s |
| migración oficial | primera ejecución aplicó solamente V2 y llevó `plg_commercial_catalog` de V1 a V2; segunda ejecución y recreación final informaron `migrations_executed=0`, `schema_version=2` |
| runtime | composición recreada sin eliminar volúmenes; aplicación en el digest candidato y los tres servicios `healthy` |
| health | liveness y readiness HTTP 200/`UP`; catálogo, configuración, base, migraciones y OIDC en `UP` |
| Playwright | `CommercialCatalogVisualIT` 1/1 verde en 61,73 s; alta, revisión, historial, ciclo, retorno contextual, responsive y seguridad negativa |
| evidencia visual | 59 PNG y 6.757.175 bytes en `docs/evidence/screenshots/J11-S8-C02-simple-definition-history/e2e/`; revisión e historial en 1280/720/375 revisados sin overflow ni controles cortados |
| logs | ventana final solicitada de 500 líneas sin `ERROR`, `SEVERE`, `FATAL`, `Exception` ni `Caused by:`; sólo advertencias conocidas del certificado autofirmado de desarrollo |
| G0 documental | 280 Markdown; 0 enlaces rotos, 0 errores UTF-8, 0 archivos con mojibake y 0 filtraciones de los secretos locales |

Checksums SHA-256 de las seis vistas nuevas:

- revisión 1280: `F2B3DFA8F3AB0597D18E5599AD13F14AA4FD9D1E64C22AED52954E5B7D8D86D5`;
- revisión 720: `6343981B4283C25B6CC70C7C4F20F720BB01E8CCFDFC21890017E40F5B91662F`;
- revisión 375: `39D721006BEF38358824753735E2B18D6F56FDD33C8FA00AA25276C9748BC7F0`;
- historial 1280: `94C51D97918B464D4BCE322827C4EB6155850FA05277F10C329D44AA46BFED89`;
- historial 720: `24120F8A99823790633903C2D5E3351404CB9CFBD1E3F482C71030F49CC54993`;
- historial 375: `0DB1619D9ECEA352C57D3B4A31D851C9C5A5B2510F0CD75D5B4246898B8C4760`.

El primer intento no pudo crear el directorio de evidencia dentro del sandbox;
se agregó el directorio versionable y se repitió. Luego se corrigieron tres fallos
del recorrido: cadenas de prueba mal codificadas, conteo antes de que JSF
materializara la confirmación y captura de página completa que hacía caer Chromium
por el historial acumulado. La espera conserva la cardinalidad exacta y la captura
de viewport conserva una medición independiente del documento completo. La suite
final quedó verde y ninguna aserción funcional fue omitida o relajada.

## Decimoséptimo corte - reemplazo seguro de definiciones simples 2026-08-04

| Control | Resultado |
|---|---|
| dominio y aplicación | reemplazo de unidad, categoría, marca o etiqueta con sucesora nueva del mismo tipo/empresa; versión esperada, permiso y auditoría técnica; origen inactivo e inmutable; referencias existentes conservadas |
| persistencia | V3 agrega cuatro vínculos privados opcionales, FK de misma empresa, checks de autorrelación y estado, e índices; rechazo de reemplazo repetido, revisión o reactivación del origen |
| pruebas focales | servicio 10/10, handler/selector/renderer 14/14 y módulo `commercial-catalog` 74/74 verdes |
| PostgreSQL | 19/19 verdes sobre PostgreSQL 18.4: 13 escenarios JPA/repositorio y 6 de migración, incluida actualización V2→V3 e idempotencia |
| gate integral | `mvnw.cmd verify` 24/24 módulos verde en 2:11 min; WAR construido y 24 pruebas ArchUnit/composición |
| Dockerfiles | aplicación y migrador pasaron `docker build --check` sin advertencias |
| imagen de aplicación | `logixone/app:j11-s8-c02-simple-definition-replacement`, `sha256:a21616f9bf182ff99f1aabdcf806a7426334216699e48d116d724161555de987`, 501.046.492 bytes; build interno 24/24 |
| imagen de migrador | `logixone/migrator:j11-s8-c02-simple-definition-replacement`, `sha256:1062a6fbc34160dd17fa0f29f03780396e080bec23105c725944de90235fe9e8`, 105.376.370 bytes; build interno 16/16 |
| migración oficial | primera ejecución aplicó sólo V3 y llevó `plg_commercial_catalog` de V2 a V3; segunda ejecución informó `migrations_executed=0`, `schema_version=3` |
| runtime | aplicación recreada; PostgreSQL y Keycloak conservaron contenedores/volúmenes y quedaron `healthy` |
| health | liveness y readiness HTTP 200/`UP`; catálogo, configuración, base, migraciones y OIDC en `UP` |
| Playwright | `CommercialCatalogVisualIT` 1/1 verde en 46,71 s; reemplazo, sucesora, vínculo, referencia histórica, responsive y seguridad negativa |
| evidencia visual | 68 PNG y 7.784.665 bytes en `docs/evidence/screenshots/J11-S8-C02-simple-definition-replacement/e2e/`; originales 1280/720/375 revisados sin overflow ni controles cortados |
| logs | 5.606 líneas finales; 0 coincidencias de `ERROR`, `SEVERE`, `Exception` o `Caused by:` |
| G0 documental | 280 Markdown y 1.207 enlaces locales; 0 errores UTF-8, mojibake, enlaces rotos o filtraciones de secretos |

## Decimoctavo corte - revisión e historial de familias de variantes 2026-08-04

| Control | Resultado |
|---|---|
| dominio y aplicación | revisión completa de nombre y 1–8 atributos ordenados; empresa, identidad y código estables; versión esperada, permiso `commercial_catalog.definitions.manage`, conflicto optimista y auditoría `REVISE_CATALOG_VARIANT_FAMILY` |
| persistencia | V4 crea `variant_family_revision` y `variant_attribute_revision`, retroalimenta V3, versiona las dos tablas de asignación y dirige sus FK a revisiones inmutables |
| checksum V4 | `FBA77C705B91FD06A3B0FC213954B5CE468B4FE816430AC711FDD91E782B3C5C` |
| pruebas focales | servicio, repositorio, handler, contrato y renderer verdes; módulo `commercial-catalog` 77/77 |
| PostgreSQL | 22/22 verdes sobre PostgreSQL 18.4: 14 escenarios JPA/repositorio y 8 de migración, con backfill V3→V4, historial, aislamiento, versión asignada e idempotencia |
| gate integral | `mvnw.cmd verify` 24/24 módulos verde en 2:48 min; WAR construido, `web-shell` 55/55 y 24 pruebas ArchUnit/composición |
| imagen de aplicación | `logixone/app:j11-s8-c02-variant-family-history`, `sha256:671cf8895426aa7ee5cc7d052f2d0eb99e68d68417eb9334b28b449fd0ead9d5`, 501.056.381 bytes; build interno `verified` 24/24 en 1:29 min |
| imagen de migrador | `logixone/migrator:j11-s8-c02-variant-family-history`, `sha256:56a4aba08bc73f7743eef49971e2bceac9959dd303ecf63133720966d9017c5b`, 105.385.690 bytes |
| migración oficial | stack aislado `logixone-vfh`: primera ejecución aplicó V1–V4 sobre volúmenes nuevos; segunda ejecución informó `migrations_executed=0`, `schema_version=4` |
| runtime | entorno original intacto; composición aislada en puertos 28080/9180 con aplicación, PostgreSQL y Keycloak `healthy` |
| health | liveness/readiness HTTP 200/`UP`; catálogo, configuración, base, migraciones y OIDC en `UP` |
| Playwright | `CommercialCatalogVisualIT` 1/1 verde en 75,33 s; familia inicial `COLOR/TALLA`, revisión `NUMERO`, historial completo, ciclo, retorno y seguridad negativa |
| evidencia visual | 74 PNG y 8.524.802 bytes en `docs/evidence/screenshots/J11-S8-C02-variant-family-history/e2e/`; revisión e historial en 1280/720/375 revisados sin overflow ni controles cortados |
| logs | arranque final de aplicación: 157 líneas y 0 coincidencias severas; PostgreSQL: dos errores atribuibles a consultas manuales diagnósticas, seguidos por intervalo limpio sin errores |
| G0 documental | 280 Markdown y 1.260 enlaces locales; 0 errores UTF-8, mojibake o enlaces rotos; `view.xhtml` bien formado; 0 coincidencias con los cuatro secretos locales en 1.172 archivos de texto |

Checksums SHA-256 de las seis vistas nuevas:

- revisión 1280: `29F891FD2D5DB2E346FC224667AC06EEF3DC5834036EF7101770E86371D39F47`;
- revisión 720: `A03852027DF085B5F7B6F90C8A086A5DCE179B61F3F65995AA9DEE5C5EFDBA9F`;
- revisión 375: `D45C3C9A166C2EC74E37144CD76F1AC802E37CA312112D9C341AF760DD5E7B30`;
- historial 1280: `0B4E1D2D78E372A949B94961AD5AE72DBE7BF5361BB63F02506E1E372565F3F8`;
- historial 720: `BF6E254CFE3F5CA592837F04340A96A52F06D542797B309DDC96A6326D06453C`;
- historial 375: `4CE4B93132462396E705E4984A558598D9E66A476A464B381DE13577E61001BA`.

La validación del volumen habitual se detuvo antes de migrar porque no había
autorización para una alteración irreversible de datos persistentes. Se creó una
composición independiente con volúmenes nuevos. El primer arranque limpio falló
cerrado por ausencia de autoridad global; el bootstrap documentado se ejecutó una
vez en ese entorno y luego se comprobó el arranque con bootstrap deshabilitado.
Las iteraciones E2E descubrieron mojibake real en textos del shell, ausencia del
valor `DISPLAY_TEXT` dentro del formulario de detalle y dependencias del test en
datos acumulados; los tres defectos se corrigieron y sus gates se repitieron en
verde. Ninguna prueba funcional se omitió o relajó.

## Decimonoveno corte - asignación versionada de familias a artículos 2026-08-04

El contrato neutral de Artículos y servicios agrega la pestaña **Variantes**, el
selector gobernado `item_variant_family`, la estructura esperada como
`DISPLAY_TEXT` y acciones para preparar y asignar. El selector enlaza a
`/catalog/variant-families` sólo con el permiso propietario y excluye familias
inactivas de operaciones nuevas.

`AssignVariant` ya no acepta un `CatalogVariant` construido por el adaptador.
Recibe identidad y revisión de familia más valores textuales. Dentro del límite
JTA, `VariantFamilyAssignmentRepository` resuelve la familia por empresa y toma
un bloqueo compartido sobre su fila vigente; la aplicación exige estado activo y
revisión exacta, rechaza códigos desconocidos u obligatorios ausentes y crea los
valores tipados desde la definición autorizada. La asignación y cada atributo
continúan enlazados a la revisión V4 exacta.

La UI muestra la estructura vigente, acepta `CODIGO=valor; OTRO=valor`, normaliza
selecciones manipuladas al recargar y conserva en el detalle familia, revisión y
valores históricos aunque el maestro cambie después.

| Gate del decimonoveno corte | Resultado final |
|---|---|
| pruebas focales de aplicación y handler | 12/12 verdes, incluidos familia manipulada, atributo desconocido, obligatoriedad, tipos y aislamiento empresarial |
| módulo `commercial-catalog` | 81/81 verdes; también quedaron verdes 22 de `plugin-api`, 10 de `kernel-api` y 9 de `commercial-catalog-api` en el reactor acotado |
| PostgreSQL 18.4/Testcontainers | 23/23 verdes: 15 de repositorio/JPA y 8 de migraciones V1–V4 |
| `mvnw.cmd -B verify` final | 24/24 módulos `SUCCESS` en 2:12; 24 pruebas ArchUnit/composición verdes |
| reactor de la imagen final | 24/24 módulos `SUCCESS` con `-Pwith-inventory-demo verify`; ArchUnit y composición verdes |
| imagen de aplicación | `logixone/app:j11-s8-c02-variant-assignment`, `sha256:f457b3d2bf150df1bbfc2283f1fb15be937d63f237781d2932f9174e70a447da`, 501.065.108 bytes |
| migrador y datos | se reutilizó el migrador V4 ya validado `sha256:56a4aba08bc73f7743eef49971e2bceac9959dd303ecf63133720966d9017c5b`; este corte no agrega migración y no recreó PostgreSQL ni Keycloak |
| runtime aislado | `logixone-vfh` ejecutó exactamente el digest final; aplicación, PostgreSQL y Keycloak `healthy`; liveness/readiness HTTP 200/`UP` |
| Playwright | `CommercialCatalogVisualIT` 1/1 verde en 94,89 s sobre la imagen final; ninguna aserción omitida o desactivada |
| evidencia visual | 77 PNG y 8.967.738 bytes en `docs/evidence/screenshots/J11-S8-C02-variant-assignment/e2e/`; pestaña Variantes revisada en 1280/720/375 px sin overflow ni controles cortados |
| logs posteriores | 0 coincidencias severas de aplicación y 0 errores PostgreSQL |
| G0 documental final | 287 Markdown y 1.330 enlaces locales; 0 errores UTF-8, mojibake, enlaces rotos o filtraciones de los cuatro secretos locales |

Antes del resultado final, Playwright detectó dos desajustes en la propia prueba:
una expectativa de tipo que todavía decía **Servicio** después de convertir el
fixture en **Producto**, y un localizador ambiguo para **Variantes**. Ambos se
corrigieron, se reconstruyó la imagen y se repitió el recorrido completo en verde;
no se relajó cobertura funcional.

Checksums SHA-256 de la pestaña nueva:

- expandido 1280: `F2DCA1707DB80331519FFDCEAC3DB757A59788FCD9A95703EB54F9B6904FC71F`;
- medio 720: `F3FF47661C8E2314131FE6041A1C671C0D1BD61152D434DD7A04CB351351D5F3`;
- compacto 375: `E91F0B16D0459A06677DBE89E865D08CD11C72041A8498BC6CFEE579F24287C7`.

## Vigésimo corte - definiciones de identificación y dirección 2026-08-04

| Gate del vigésimo corte | Resultado final |
|---|---|
| dominio y aplicación | cuatro clases empresariales; referencias de identificación/dirección revalidadas por empresa, clase, estado activo y bloqueo compartido |
| contrato visual | `business_partners:directory@1.1.0`; identificación, tipo y propósito de dirección son `SELECT`; administración genérica conserva identidad `clase:código` incluso en navegación directa |
| selector sources | 9/9 usos de socios declarados; los tres nuevos consumidores enlazan a `/business-partners/definitions` con `business_partners.manage` y retorno seguro |
| migración V4 | checksum `481CBC4684F47FB559DA6F1EAFE8E9534DC7CC92BF314E11B8ED170B4F830D99`; backfill e inicialización sin tablas nuevas |
| módulo | 51/51 verdes; handlers, fuentes, dominio, aplicación y migraciones |
| PostgreSQL 18.4/Testcontainers | 21/21 verdes: 14 JPA/repositorio y 7 de migraciones V1–V4 |
| gate integral | `-Pwith-inventory-demo verify` 24/24 módulos; 24 pruebas ArchUnit/composición verdes |
| imágenes | aplicación `sha256:52a2c64e9f690900ca7fdf1b1ef0bd66fcc5b5688cad90c4825d2beb64e84af0`, 501.071.129 bytes; migrador `sha256:c3cffe4b25f66ffbc187e313b79d3b622b547908eaf8c3de39e69da1e42cecf1`, 105.399.374 bytes |
| migración oficial | `logixone-bpd` con volúmenes nuevos aplicó V1–V4; repetición con `migrations_executed=0`, `schema_version=4` |
| runtime | puertos 38080/10180; bootstrap one-shot cerrado después de crear autoridad; aplicación, PostgreSQL y Keycloak `healthy`; health HTTP 200/`UP` |
| logs finales | 0 coincidencias severas en aplicación y 0 `ERROR`/`FATAL`/`PANIC` en PostgreSQL; evento de bootstrap deshabilitado presente |
| Playwright | `BusinessPartnersVisualIT` 1/1 verde en 54,28 s; preparación idempotente por UI, cuatro clases, historial, ciclo, consumo y seguridad negativa |
| evidencia visual | 23 PNG y 2.625.513 bytes en `docs/evidence/screenshots/J11-S8-C02-partner-definitions/e2e/`; originales revisados en 1280/720/375 px sin overflow ni controles cortados |
| G0 documental | 288 Markdown y 1.336 enlaces locales; 0 errores UTF-8, mojibake o enlaces rotos; 0 coincidencias con los cuatro secretos locales en 1.180 archivos de texto |

La primera ejecución E2E detectó una expectativa textual obsoleta después de
generalizar la administración. Se alineó con el mensaje real. Al repetir sobre el
digest final, un localizador por rol del workspace resultó transitorio aunque los
logs confirmaron selección autorizada y ausencia de error; se estabilizó contra el
ID semántico `modules-title`. El recorrido completo final quedó verde sin relajar
aserciones funcionales. El escenario además dejó de depender de activaciones o
permisos residuales: prepara ambos mediante las pantallas administrativas y omite
de forma idempotente lo ya concedido.

Checksums SHA-256 representativos:

- alta compacta 375: `107B3E18DCBD26D916175145536134F67531FDEB04469A0C4F0105E8D8CC64E3`;
- historial medio 720: `90AD046F56AF418DC5D3845D87FCE37A5ADC1B7990A8D7E15B70EA03DDEFF56A`;
- identificación expandida 1280: `E6AD0CED686D6E73C7DEFED0243494256F7C7AF01EF1B988F87E05A89CB2A554`;
- dirección expandida 1280: `0FC084D7F35C100C9DC0EDB590774BBBF8D941F85B725ED8C9B9FAA2D02C7CCD`;
- denegación compacta 375: `6B178E028964DCECF94C6AF4A22E076A4E5B9AFEE87EEB1CEC5585393F4D5BC4`.

## Pendientes que impiden cerrar

- decisión de propiedad de países y monedas;
- fuentes normativas y recorridos todavía no implementados;
- umbral y estrategia de búsqueda/paginación para catálogos grandes;
- recongelación, PDF y pregunta de instalador.

No se regeneró el instalador ni se modificó `installer/windows/current`.
