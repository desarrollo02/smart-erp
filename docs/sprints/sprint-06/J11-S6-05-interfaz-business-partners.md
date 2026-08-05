# J11-S6-05 - Interfaz de `business_partners`

- Estado: Completa
- Fecha: 2026-07-29
- Gate: G4 interfaz productiva
- Dependencia: J11-S6-04 verde

## Objetivo

Publicar la primera pantalla ERP productiva mediante Jakarta Faces 4.1, Material
Design 3 y un contrato de interacción neutral. La pantalla debe usar los casos de
uso reales de `business_partners`, respetar empresa/permisos actuales, funcionar en
compacto, medio y expandido y exponer slots estables para la futura personalización
exclusiva de cada empresa.

## Contrato visual público

- ruta autorizada: `/business-partners`;
- pantalla: `business_partners:directory`, versión `1.0.0`;
- permiso de menú y carga: `business_partners.view`;
- permisos de acción: `view`, `manage`, `roles.manage` y `lifecycle.manage`;
- slots: `directory_extensions` y `detail_extensions`;
- tipos neutrales: texto, selección, acción y tabla de datos;
- compatibilidad de `plugin-api`: `[0.4.0,0.5.0)`.

El shell resuelve textos, markup, layout y tokens. El plugin sólo aporta descriptor,
handler, valores, opciones, tabla, detalle y avisos. Ningún XHTML, CSS o JavaScript
del plugin se ejecuta dentro de la aplicación.

## Operaciones visibles

- búsqueda por texto, rol y estado;
- alta de organización o persona con código manual u automático;
- apertura de detalle y actualización versionada de código/nombres;
- alta de identificación, dirección, canal y contacto;
- asignación/estado de cliente y proveedor;
- inactivación/reactivación del participante;
- avisos de éxito, advertencia o error sin exponer excepciones internas.

La UI no simula corrección histórica de identificaciones ni edición/desactivación
de detalles: esas capacidades continúan fuera del corte definido en J11-S6-04.

## Criterios de aceptación

1. el menú sólo aparece si el plugin está efectivo y el actor posee `view`;
2. la ruta directa vuelve a autorizar empresa, plugin, permiso y pantalla;
3. cada acción exige el permiso específico en servidor;
4. identidad y versión del detalle sobreviven al postback y la mutación usa control
   optimista;
5. el shell no importa internos ni entidades del plugin;
6. no se aceptan fragmentos XHTML, EL, CSS o JavaScript aportados por plugins;
7. inputs, tablas, opciones, avisos y selección usan un contrato neutral acotado;
8. Material Design 3, labels, foco y jerarquía semántica son propiedad del shell;
9. 375/599/600/720/839/840/1280 px no presentan overflow horizontal;
10. unitarias, ArchUnit, PostgreSQL estricto, reactor y Playwright quedan verdes;
11. runbook, guía de implementación, ADR y evidencia se actualizan en el mismo
    cambio;
12. el WAR base no incorpora aún físicamente el plugin: eso pertenece a S6-06.

## Resultado

La historia quedó completa. Se registró un socio ficticio sobre PostgreSQL, se
buscó, se abrió su detalle y se asignó el rol cliente. El servidor confirmó la
operación, avanzó la versión optimista de `0` a `1` y emitió auditoría técnica. La
prueba Playwright específica repitió el recorrido con un código aleatorio y validó
los siete anchos previstos sin overflow.

El perfil completo ejecutó 241 pruebas unitarias/arquitectónicas sin fallos en 20
módulos. La prueba visual ejecutó un escenario end-to-end verde y generó tres
capturas revisadas. La composición usada para observar esta historia es local y
efímera; el perfil único y reproducible de la distribución se incorpora en
`J11-S6-06`.

Evidencia: [J11-S6-05](../../evidence/J11-S6-05-interfaz-business-partners.md).
Guion: [demo de `business_partners`](../../runbooks/demo-business-partners-j11-s6-05.md).

