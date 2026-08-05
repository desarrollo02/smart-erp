# J11-S8-C05 — Evidencia del cambio de marca seguro a Smart ERP

- Fecha: 2026-08-05
- Rama de trabajo: `chore/J11-S8-C04-adopcion-git`
- Baseline remoto previo: `6d71e7e`
- Estado: implementación, Maven y documentación verdes; Playwright pendiente por Docker no disponible
- Decisión: [ADR-0039](../adr/0039-marca-smart-erp-identificadores-compatibles.md)

## Resultado

La marca mostrada al usuario pasó de Logixone a **Smart ERP** en las ocho vistas
JSF, las respuestas HTML de seguridad, el usuario de respaldo, los nombres
descriptivos de Maven, los metadatos visibles de Keycloak y la documentación
vigente.

El cambio preservó deliberadamente los identificadores técnicos compatibles:

- paquetes `py.com.logixone`, módulos, `groupId` y `artifactId`;
- WAR `logixone.war`, contexto `/logixone` y URLs existentes;
- realm `logixone`, cliente `logixone-web` y variables `LOGIXONE_*`;
- JNDI `LogixoneCoreDS`, bases, usuarios, esquemas, imágenes, redes, volúmenes,
  scripts, propiedades y migraciones;
- nombres históricos de documentos y evidencias.

El instalador interno `0.8.0-internal.1` no fue modificado ni regenerado. Su
ejecutable y textos heredados continúan identificados como Logixone y la guía de
usuario advierte que no representa la nueva marca.

Los PDF derivados existentes pertenecen igualmente a baselines anteriores. No se
reescribieron en esta historia; se conservaron identificados como anteriores y su
regeneración visual corresponde al gate documental de cierre de Sprint 8.

## Aislamiento de herramientas

Todas las pruebas de la historia se ejecutaron con el Maven Wrapper del proyecto
sobre una materialización exacta del índice preparada bajo
`.tools/tmp/validation/J11-S8-C05-r2/`. No se inspeccionó, inició, detuvo ni
modificó WildFly, IntelliJ IDEA ni otra herramienta instalada por el usuario.

## Pruebas ejecutadas

| Prueba | Resultado |
|---|---|
| `SmartErpBrandingResourceTest` y `AdminAuthorizationFilterTest` | 6/6 verdes |
| `mvnw.cmd -B -f <materialización>/pom.xml clean verify` | 26/26 módulos, 480 pruebas, cero fallos, errores u omitidas; `BUILD SUCCESS` en 4:05 |
| ArchUnit incluido en el reactor | 28/28 pruebas verdes |
| validación documental local desde `.tools` | 301 Markdown y 1220 archivos de texto; cero enlaces rotos, errores UTF-8, mojibake o filtraciones de secretos |
| inventario estático de las ocho vistas JSF | 26 apariciones de `Smart ERP` y cero apariciones visibles de `Logixone` |
| revisión del índice y compatibilidad | 55 archivos preparados, cero cambios en `installer/windows/current`, cero coincidencias de secretos de alta confianza e identificadores preservados |

La primera invocación focal no ejecutó pruebas porque PowerShell interpretó una
propiedad `-D` sin comillas como fase Maven. Se corrigió el comando; no fue un
fallo del producto ni se ignoró una prueba fallida.

## Compatibilidad comprobada

La prueba de recursos exige que las ocho vistas presenten `Smart ERP`, el
monograma `S` y ausencia de la marca anterior en el contenido visible. A la vez,
comprueba que permanezcan sin cambio el namespace técnico `ln=logixone` y la API
JavaScript `LogixoneSelectorReturn`.

La revisión estática complementaria verifica que el POM raíz, el nombre final del
WAR, el contexto, el realm, el cliente y los demás identificadores enumerados en
el ADR conserven sus valores técnicos. `installer/windows/current` no contiene
cambios preparados en el índice.

## Validación visual pendiente

El preflight de Docker no pudo consultar el servidor: el daemon no estaba
disponible mediante `npipe:////./pipe/docker_engine` y el entorno también informó
acceso denegado a `C:\Users\sdiaz\.docker\config.json`. Conforme a la regla de
aislamiento, no se intentó iniciar ni alterar Docker o procesos del usuario.

Por ello no se ejecutó todavía Playwright en 375, 720 y 1280 px. La historia no se
declara cerrada hasta disponer del entorno Docker oficial y registrar esa
validación visual. Este pendiente tampoco autoriza cerrar Sprint 8, promover una
imagen o reemplazar el instalador vigente.
