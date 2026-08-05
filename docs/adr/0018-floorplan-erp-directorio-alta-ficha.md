# ADR-0018 - Floorplan ERP de directorio, alta y ficha

- Estado: Aceptado
- Fecha: 2026-07-29
- Historia: corrección visual de aceptación de `J11-S6-06`

## Contexto

La primera pantalla productiva de `business_partners` demostró la interacción real,
pero colocó búsqueda, alta, resultado, detalle y todas las mutaciones en una sola
página vertical. Aunque era funcional y responsive, la densidad era baja, la
jerarquía de tareas no era evidente y el usuario debía recorrer una página extensa
para localizar una operación.

Los patrones vigentes de aplicaciones empresariales separan la exploración de una
colección de la edición de un objeto. SAP Fiori usa el *list report* como entrada para
buscar y filtrar, y navega al *object page* para ver o modificar un registro. Material
Design 3 propone estructuras adaptables de lista-detalle por breakpoint. La decisión
se adopta como patrón del shell, no como dependencia de SAPUI5, OData, una SPA o una
biblioteca visual.

## Decisión

1. Una entidad empresarial con búsquedas y mantenimiento usa tres modos de página:
   `directory`, `create` y `detail`. No se presentan simultáneamente como una página
   continua.
2. `directory` contiene encabezado compacto, acción primaria de alta, filtros y un
   único conjunto de resultados. En expandido se representa como tabla; en medio y
   compacto, como lista adaptable sin scroll horizontal de página.
3. `create` contiene sólo los datos necesarios para dar de alta el agregado. Las
   relaciones y datos secundarios se administran después de crear la identidad.
4. `detail` comienza con un resumen de lectura y divide las operaciones por pestañas
   semánticas. Cada pestaña muestra únicamente la sección requerida; no renderiza
   todos los formularios debajo del resumen.
5. El shell mantiene navegación persistente en expandido y un menú colapsable en
   medio/compacto. Encabezado, filtros, controles y espacios usan una densidad apta
   para trabajo repetitivo sin reducir las áreas de interacción necesarias.
6. Metadatos técnicos como `ScreenId`, versión optimista, slots y explicación JTA no
   forman parte de la pantalla operativa. Continúan disponibles en contratos, logs,
   documentación y diagnóstico autorizado.
7. Las acciones destructivas se distinguen de las primarias y secundarias. Ocultar
   una sección por pestaña nunca sustituye autorización: cada request vuelve a
   resolver actor, empresa, plugin, pantalla y permiso.
8. El modo y la pestaña son navegación no autoritativa. Recurso y versión se vuelven
   a consultar y validar en el servidor conforme a ADR-0017.

## Alternativas descartadas

- Conservar la página única y reducir sólo tipografía/márgenes: no corrige la mezcla
  de tareas ni el recorrido vertical.
- Abrir formularios en modales: no ofrece espacio suficiente para entidades ERP
  extensibles y complica foco, URL y navegación server-side.
- Exponer una vista JSF por plugin: rompe la propiedad del shell, los overlays
  neutrales y la consistencia responsive.
- Copiar SAP Fiori, Dynamics u Odoo: se adoptan sus principios de jerarquía, no sus
  frameworks, estilos, componentes ni contratos.
- Mostrar la versión optimista al usuario final: es estado técnico requerido por el
  postback, no información de negocio.

## Consecuencias

- El renderer de `web-shell` conoce el floorplan y decide su adaptación visual;
  `plugin-api` permanece Java puro y no cambia de versión por esta decisión.
- Las pantallas futuras deben declarar cuál es su colección, alta, ficha o flujo
  guiado; una página larga multioperación requiere justificación y revisión UX.
- La personalización empresarial continúa operando sobre IDs, slots y overlays
  públicos. No puede acoplarse a la estructura XHTML de pestañas o navegación.
- Las pruebas visuales deben recorrer al menos directorio, alta y ficha, además de
  compacto `375px`, medio `720px`, expandido `1280px` y límites del proyecto.

## Verificación

- pruebas del bean y parseo del XHTML;
- prueba del renderer para modos, pestañas, navegación y ausencia de metadatos
  técnicos en la pantalla productiva;
- Playwright con alta, búsqueda, apertura, cambio de pestaña y mutación real;
- medición de overflow y revisión visual en `375px`, `720px` y `1280px`;
- WildFly, OIDC, PostgreSQL, JTA, health y composición física sin cambios de
  contrato ni migración.

## Referencias verificadas

- [SAP Fiori - List Report](https://experience.sap.com/fiori-design-web/v1-46/list-report-floorplan-sap-fiori-element/), consultada el 2026-07-29.
- [SAP Fiori - Object Page](https://experience.sap.com/fiori-design-web/v1-50/object-page/), consultada el 2026-07-29.
- [Material Design 3 - Canonical layout examples](https://m3.material.io/foundations/layout/canonical-examples/overview), consultada el 2026-07-29.

