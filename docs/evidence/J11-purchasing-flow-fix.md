# Corrección transversal del flujo visual de Compras

- Fecha: 2026-08-20
- Baseline afectado: floorplans 2.0 de `purchasing` 1.2
- Estado: Implementada y validada automáticamente; validación independiente
  pendiente
- Origen: revisión funcional independiente de las cinco pantallas de Compras

## Problema confirmado

Las rutas de Solicitudes, Órdenes, Recepciones, Devoluciones y Seguimiento
renderizaban en una sola página la bandeja, los datos de alta, el detalle, el
resumen y las acciones. Aunque la página no desbordaba horizontalmente, rompía el
patrón del producto `lista -> alta -> detalle`, producía recorridos verticales
innecesarios y comprimía los selectores buscables dentro de columnas uniformes.

La causa estaba en el renderer compartido del shell: los floorplans 2.0
transportaban `mode`, pero no lo utilizaban para seleccionar regiones. La grilla
también aplicaba el mismo mínimo a campos simples y referencias buscables.

## Corrección

- Un floorplan que declara filtros, una tabla de resultados y una acción de alta
  o navegación separa sus modos, incluso cuando lista y formulario comparten una
  región semántica:
  - `directory`: filtros, resultados y acceso visible a preparar un documento;
  - `create`: cabecera, captura, orientación y acciones, sin la lista;
  - `detail`: documento seleccionado, líneas, resumen y acciones, sin filtros.
- Los floorplans de una sola etapa, como operaciones de Inventario sin bandeja,
  conservan su composición actual.
- El encabezado ofrece `Preparar...` desde la lista y `Volver a la lista` desde
  alta o detalle.
- Las regiones sin elementos dinámicos visibles dejan de ocupar espacio vacío.
- Cabeceras y líneas usan mínimos diferenciados; las referencias buscables
  abarcan más espacio y las acciones permanecen al final del formulario sin
  superponerse a los campos en escritorio.
- Compacto conserva una sola columna; medio utiliza dos columnas controladas.

No se cambiaron dominio, persistencia, permisos, contratos públicos ni
migraciones de Compras.

## Criterios de regresión

1. La lista no contiene regiones de cabecera, líneas, resumen o acciones del
   editor.
2. `Preparar solicitud/orden/recepción/devolución` navega a `mode=create`.
3. Alta y detalle no contienen filtros ni resultados del directorio.
4. `Volver a la lista` está disponible desde alta y detalle.
5. Los selectores buscables visibles no se colapsan por debajo de 280 px en
   medio/expandido ni de 240 px en compacto.
6. Los anchos 375, 720 y 1280 px no presentan overflow horizontal normal.
7. Inventario y otros floorplans sin `WORK_ITEMS` no pierden sus regiones.

## Evidencia automatizada

- Corte aislado 01: reactor hasta `web-shell`, 16 módulos verdes; `web-shell`
  75/75 pruebas.
- Corte aislado 02: pruebas focales de renderer y recursos, 7/7 verdes, incluida
  la protección de operaciones sin bandeja.
- Corte aislado 03: prueba focal de recursos CSS/XHTML, 2/2 verde.
- Corte aislado 06: pruebas focales del renderer, contratos y XHTML, 9/9 verdes.
- Compilación de `PurchasingVisualIT`: 5 fuentes E2E compiladas correctamente.
- `mvnw.cmd verify` del corte 06: 28/28 módulos verdes, incluidos Compras,
  `web-shell`, WAR y ArchUnit.
- Imágenes de aplicación y migrador construidas desde el corte 06 en modo
  `verified` y perfil `with-purchasing-demo`; ambos builds quedaron verdes.
- Compose aislado `logixone-purchasing-flow-fix-06` alcanzó PostgreSQL, Keycloak
  y aplicación saludables en `28081/18181`. El navegador confirmó el recorrido
  OIDC de la candidata.
- Con autorización explícita del responsable, el bootstrap de una sola ejecución
  preparó la identidad ficticia sin empresa `demo.sin.empresa` con los permisos
  `kernel.plugin.manage`, `kernel.security.manage` y
  `kernel.system_administration.manage`. Después del arranque saludable se dejó
  `LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_ENABLED=false`, se recreó sólo la
  aplicación y volvió a quedar saludable; los datos permanecieron únicamente en
  los volúmenes del Compose aislado.
- El primer intento E2E posterior a la preparación expuso una ambigüedad en el
  selector de prueba: la clase del encabezado interno coincidía con el nombre de
  una región. Se restringió la aserción a elementos `section` y la compilación de
  las cinco fuentes E2E quedó verde.
- El segundo intento completó el recorrido funcional y visual, pero encontró que
  la prueba negativa abría administración con el actor comercial. Se separó el
  contexto administrativo con `demo.sin.empresa`, conservando el actor comercial
  para comprobar la denegación y restaurando el plugin aun ante un fallo.
- Corte aislado 09: `PurchasingVisualIT` 1/1 verde en 75,35 s, incluida la
  creación y transición de solicitudes, órdenes, recepciones, devoluciones,
  seguimiento y la desactivación/denegación/restauración de `purchasing`.
- La revisión de las capturas del corte 09 detectó que la barra de acciones
  `sticky` cubría campos en 1280 px. Se eliminó el posicionamiento flotante y se
  agregó una regresión estática que rechaza `position: sticky` en el floorplan.
- Corte aislado 10: prueba mínima `ShellFloorplanResourceTest` 2/2 verde; imagen
  `logixone/app:j11-purchasing-flow-fix-10` construida desde el Dockerfile
  versionado con `verify` completo de 28/28 módulos y perfil
  `with-purchasing-demo`; manifiesto local
  `sha256:9b00bcdf6a0ba9ea2aca21bc8632e0e94b12d2edb1ac2022376a191dbb87fc48`.
- E2E final del corte 10: `PurchasingVisualIT` 1/1 verde en 86,54 s. Generó 26
  capturas sin overflow horizontal en 375, 720 y 1280 px. La revisión visual
  confirmó lista separada, alta separada, navegación de retorno, grillas legibles,
  selectores buscables amplios y acciones al final sin solapamiento.
- La candidata utilizó exclusivamente `28081/18181`; la instancia del usuario en
  `18080/8180` no fue inspeccionada, reiniciada ni modificada.

La validación independiente continúa siendo una actividad distinta de esta
revisión automatizada.
