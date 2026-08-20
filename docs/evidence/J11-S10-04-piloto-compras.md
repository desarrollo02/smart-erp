# Evidencia J11-S10-04 — Piloto de Compras

- Fecha: 2026-08-15
- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Validación independiente: pendiente dentro del calendario autorizado
- Materialización: `.tools/tmp/validation/J11-S10-04/`
- Alcance: cinco pantallas de `purchasing` 2.0, handlers, shell y documentación

## Evidencia estática

- las rutas `/faces/purchasing/requests`, `/orders`, `/receipts`, `/returns` y
  `/tracking` conservan los mismos `ScreenId` y opciones de menú;
- `purchasing` cambia de 1.1.0 a 1.2.0 sin modificar migraciones ni tablas;
- los contratos declaran respectivamente `WORKLIST`, `TRANSACTION_EDITOR`, dos
  `GUIDED_OPERATION` e `INQUIRY`;
- las regiones cerradas separan filtros, trabajo o contexto, cabecera/contenido,
  líneas, guía, resumen y acciones según la tarea;
- las acciones se calculan de nuevo con empresa, permiso, actor, estado y versión
  vigentes; la aprobación propia continúa rechazada en servidor;
- versión e identidades técnicas se transportan como estado oculto y no aparecen
  como entradas editables;
- los selectores dependientes refrescan su contexto y el buscador conserva el
  borrador seguro mediante `floorplanInput.<id-semántico>`;
- las tablas v2 incluyen una representación estática como tarjetas en compacto y
  no alteran la semántica de escritorio;
- seguimiento no declara mutaciones ni consulta tablas privadas de otros plugins.

## Gates focales ejecutados

```powershell
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-04/pom.xml -pl plugins/purchasing -am test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-04/pom.xml -pl web-shell -am '-Dtest=SelectorReturnResourceTest,ShellFloorplanRendererTest,ShellFloorplanResourceTest,ShellFloorplanSubmissionTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-04/pom.xml -pl tests/architecture-tests -am test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-04/pom.xml -Pwith-purchasing-demo verify
```

| Gate | Resultado |
|---|---|
| suite completa `plugins/purchasing` | 28 pruebas, 0 fallos/errores/omitidas |
| suite completa `web-shell` | 74 pruebas, 0 fallos/errores/omitidas |
| transporte contextual focal del shell | 11 pruebas, 0 fallos/errores/omitidas |
| ArchUnit | 34 pruebas, 0 fallos/errores/omitidas |
| `mvn verify` raíz | 28 módulos; 565 pruebas, 0 fallos/errores/omitidas |
| Playwright `PurchasingVisualIT` | 1 recorrido integral, 0 fallos/errores/omitidas |
| responsive | 375, 599, 600, 720, 839, 840 y 1280 px sin overflow normal |
| evidencia visual | 18 PNG; 3.117.444 bytes; revisión visual conforme |
| documentación | 379 Markdown; sin enlaces rotos, errores UTF-8, mojibake ni secretos |
| Compose | `app`, Keycloak y PostgreSQL saludables; datos conservados |
| health | `/health/live` 200 y `/health/ready` 200 |

La imagen validada usa el perfil físico `with-purchasing-demo`:

- etiqueta: `logixone/app:j11-s10-04-r7`;
- manifiesto de plataforma:
  `sha256:54fe57c6f4bdc17a05c4713dc76225bbed0397aa58a7282169ec536f82f1d3f3`;
- manifiesto de ejecución:
  `sha256:b9aefdee95a150021f5efb2baa4279c1e323b169218a9c3d8b0fbcab566016d8`;
- configuración:
  `sha256:bf3e91f24444a4b3ac80176cc22acb921f92f75f9d8ad8789f4774ea969753ae`.

El recorrido integral crea y envía una solicitud, comprueba la separación entre
solicitante y aprobador, emite una orden, confirma una recepción de Stock,
confirma una devolución y consulta el seguimiento con 10 pedidas, 6 recibidas,
2 devueltas y 6 pendientes. También recorre los rangos responsive, la
accesibilidad estructural y la denegación segura al desactivar el plugin. Las
capturas están en
[`screenshots/J11-S10-04/e2e`](screenshots/J11-S10-04/e2e/).

## Incidencias encontradas y resueltas

- las tablas v2 desbordaban en compacto; se añadió una representación estática
  como tarjetas conservando la tabla accesible para medio y expandido;
- el postback dinámico perdía modo, pestaña, recurso y versión; esos valores
  pasaron al estado técnico oculto del formulario;
- un localizador Playwright confundía una opción oculta del catálogo con el
  estado visible; se acotó a la salida semántica de la pantalla;
- seleccionar una línea de orden no refrescaba Depósito y Ubicación; los
  selectores de referencia operativa ahora solicitan actualización contextual;
- abrir el buscador de una referencia reconstruía una vista request-scoped sin
  la línea elegida; el transporte semántico conserva y valida el contexto antes
  de consultar o seleccionar opciones.

No se modificaron persistencia, migraciones ni integración con Inventario. La
validación independiente continúa pendiente bajo la autorización vigente; esta
evidencia automática no declara cierre del Sprint.
