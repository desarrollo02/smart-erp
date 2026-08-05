# J11-S7-05 - Interfaz de `commercial_catalog`

- Estado: Completa; composición y Playwright satisfechos por J11-S7-06
- Sprint: 7
- Fecha de inicio: 2026-07-30
- Gate principal: G4 interfaz y seguridad visual
- ADR: [ADR-0022](../../adr/0022-recorridos-visuales-commercial-catalog.md)

## Objetivo

Construir los recorridos JSF productivos de artículos/servicios y listas de precios
sobre los casos de uso reales de J11-S7-04, respetando el floorplan ERP, Material
Design 3, responsive y autorización server-side.

## Alcance

- pantalla y menú de artículos/servicios;
- pantalla y menú de listas de precios;
- modos separados `directory`, `create` y `detail`;
- filtros, tablas/listas adaptables, resumen y secciones de mantenimiento;
- selectores construidos desde definiciones autorizadas;
- handlers neutrales sin XHTML, CSS, JavaScript ni EL desde el plugin;
- generalización del renderer del shell sin regresión de `business_partners`;
- contratos de personalización y slots públicos acotados.

## Fuera de alcance

- composición física en WAR/migrador y activación empresarial;
- fixtures, imagen Docker y guion oficial de demo;
- administración visual completa de definiciones maestras;
- importación masiva, stock, costos, promociones, ventas, documentos o SIFEN;
- paginación interactiva avanzada y generador masivo de variantes.

## Criterios de aceptación

- **CA-01:** el descriptor publica dos menús y dos pantallas, ambos protegidos por
  `commercial_catalog.view`.
- **CA-02:** el shell representa ambos contratos mediante un registro cerrado y no
  contiene copias o pestañas exclusivas de socios comerciales en el floorplan.
- **CA-03:** artículos permite buscar, registrar, abrir ficha, modificar datos,
  agregar identificador, clasificar, agregar conversión y cambiar ciclo de vida.
- **CA-04:** listas permite buscar, registrar, abrir ficha, renombrar, agregar e
  inactivar entradas y cambiar ciclo de vida.
- **CA-05:** cada acción exige el permiso exacto y vuelve a consultar empresa,
  recurso y versión en el servidor.
- **CA-06:** ausencia de unidad, perfil tributario o artículo elegible produce un
  estado comprensible y no SQL, IDs inventados ni bypass de autorización.
- **CA-07:** directorio, alta y ficha funcionan en 375, 720 y 1280 px y en los
  límites 599/600 y 839/840 sin overflow horizontal normal.
- **CA-08:** navegación por teclado, labels, foco, contraste, avisos y estados
  vacíos/error permanecen accesibles.
- **CA-09:** pruebas de plugin, renderer, arquitectura y reactor quedan verdes; la
  prueba en navegador se ejecuta sobre la composición real de J11-S7-06 antes de
  cerrar esta historia.
- **CA-10:** no se adelanta composición, Docker, inventario, ventas ni documentos.

## Secuencia

1. fijar contratos de pantalla, menús, regiones y textos;
2. generalizar el floorplan shell-owned;
3. implementar handler de artículos y sus pruebas;
4. implementar handler de listas y sus pruebas;
5. validar renderer, responsive, seguridad y reactor;
6. componer en J11-S7-06, ejecutar Playwright y cerrar la historia.

## Resultado del corte de implementación

El plugin publica dos menús y dos pantallas interactivas neutrales. El shell quedó
generalizado para representar, sin XHTML por plugin, los recorridos de artículos y
listas de precios además de conservar el recorrido de socios comerciales.

Los handlers autorizados cubren búsqueda, alta, ficha, mantenimiento y ciclo de
vida. Los selectores provienen de definiciones reales y los estados sin unidades,
perfiles tributarios o artículos elegibles se informan sin SQL ni identificadores
inventados. Un hallazgo de prueba corrigió el orden de validación/autorización al
agregar precios: la entrada se valida antes de solicitar autoridad y la mutación se
autoriza inmediatamente antes de ejecutarse.

Gates ejecutados el 2026-07-30:

- `plugins/commercial-catalog`: 43/43 pruebas verdes;
- `web-shell`: 19/19 pruebas verdes;
- arquitectura: 20/20 pruebas verdes;
- reactor completo: 22/22 módulos y 301/301 pruebas verdes.

J11-S7-06 compuso el JAR, migró PostgreSQL, activó el plugin y sus permisos por la
administración real y ejecutó Playwright sobre OIDC en los siete anchos. CA-07,
CA-08 y la porción de navegador de CA-09 quedaron satisfechos; la historia se
cierra sin alterar la evidencia histórica del corte de implementación. Véase la
[evidencia de integración](../../evidence/J11-S7-06-integracion-composicion-commercial-catalog.md).
