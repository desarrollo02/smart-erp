# ADR-0017 - Interacción visual neutral de plugins

- Estado: Aceptado
- Fecha: 2026-07-29

## Contexto

Los contratos de Sprint 2 permitían que un plugin declarase una pantalla y que la
personalización de una empresa aplicase overlays tipados. El primer renderer de
Sprint 3 sólo mostraba contenido técnico de referencia. `business_partners`
necesita ahora buscar, registrar y modificar datos reales sin entregar al plugin
control sobre XHTML, beans del shell, expresiones EL, CSS o JavaScript.

La alternativa de crear una vista JSF dentro de cada JAR volvería a acoplar el
producto: el shell perdería el control del tema Material Design 3, la seguridad,
la accesibilidad, los límites responsive y la compatibilidad de personalizaciones.
También se necesita conservar selección y versión optimista entre postbacks sin
confiar en valores enviados por el navegador como autorización.

## Decisión

1. `plugin-api` 0.4.0 publica `ScreenInteraction`, un contrato Java puro de
   solicitud y resultado. El resultado puede aportar inputs, opciones cerradas,
   tabla paginada, detalle seleccionado, avisos y la pareja identidad/versión del
   recurso.
2. Un plugin interactivo implementa exactamente un `ScreenInteraction.Handler`
   para su `ScreenId`. El handler traduce acciones públicas a casos de uso propios;
   no devuelve markup ni tipos Jakarta.
3. El shell conserva la propiedad del XHTML, componentes Faces, textos, tokens,
   layout, accesibilidad y breakpoints. Sólo renderiza tipos y acciones declarados
   por el `ComposedScreen` autorizado.
4. Cada carga y acción vuelve a resolver actor, empresa, plugin, permiso y pantalla
   en el servidor. El handler solicita además la autorización específica de
   lectura, administración, roles o ciclo de vida antes de invocar aplicación.
5. Identidad y versión seleccionadas viajan como estado técnico de formulario,
   pero se validan, se vuelven a consultar y nunca conceden acceso por sí mismas.
   Una mutación usa la versión esperada y devuelve el nuevo valor confirmado.
6. Inputs, tablas, textos, cantidad de filas y avisos tienen límites cerrados. Un
   handler ausente/duplicado, una acción no declarada o un resultado inconsistente
   producen denegación segura.
7. Los nuevos tipos `SELECT` y `DATA_TABLE` son semánticos. No incorporan una
   biblioteca visual a `plugin-api`; el renderer del shell decide su representación
   Material Design 3 y su patrón compacto.

## Alternativas descartadas

- XHTML o Facelets dentro de cada plugin: permite importar beans ajenos y dispersa
  tema, accesibilidad y responsive.
- Un JSON genérico o esquema libre de componentes: amplía la superficie hasta
  convertirla en un lenguaje de UI difícil de validar y versionar.
- Endpoints REST temporales consumidos por JavaScript: duplican seguridad y estado,
  y contradicen la UI server-side Jakarta Faces acordada.
- Guardar el bean de pantalla en sesión: conserva autorización o versión obsoletas
  y aumenta el riesgo de filtración entre empresas.
- Inferir operaciones por nombre de campo o reflexión: elimina el contrato público
  explícito y hace frágiles las personalizaciones.

## Consecuencias

- `PluginApiVersion.CURRENT` avanza de 0.3.0 a 0.4.0; plugins compatibles declaran
  el rango `[0.4.0,0.5.0)`.
- Agregar un nuevo tipo de interacción requiere versionar el contrato neutral,
  implementar el renderer shell-owned y probar el rechazo cuando falta soporte.
- La pantalla `business_partners:directory` puede exponer los slots
  `directory_extensions` y `detail_extensions`; una personalización futura actúa
  sobre esos contratos públicos, nunca sobre la estructura XHTML.
- Request scope sigue siendo la frontera de los beans web. Los parámetros de ruta,
  recurso y versión se reconstruyen antes del postback y se revalidan en cada
  acción.
- La primera UI productiva demuestra el patrón, pero la composición física oficial
  del plugin en WAR/migrator continúa siendo responsabilidad de `J11-S6-06`.

## Verificación

- pruebas unitarias del contrato y sus invariantes;
- descriptor, handler y renderer de `business_partners`;
- autorización negativa y versión optimista en los casos de uso;
- ArchUnit para preservar `plugin-api` sin Jakarta;
- Playwright en 375, 599, 600, 720, 839, 840 y 1280 px;
- demo real sobre WildFly, OIDC, PostgreSQL y JTA con alta, búsqueda, detalle y
  asignación de rol auditada.

