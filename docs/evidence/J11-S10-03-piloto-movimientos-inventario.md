# Evidencia J11-S10-03 — Piloto de movimientos de Inventario

- Fecha: 2026-08-15
- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Validación independiente: pendiente dentro del calendario autorizado
- Materialización: `.tools/tmp/validation/J11-S10-03/`
- Alcance: `inventory:stock` 2.0, handler, shell y documentación

## Evidencia estática

- `inventory:stock` conserva `/inventory` y migra de 1.0 a 2.0;
- el descriptor mantiene tres pantallas y tres menús, sin ruta paralela;
- la experiencia declara `GUIDED_OPERATION` y cinco regiones cerradas;
- el contrato no declara `movement_source_type` ni `movement_source_id`;
- `movement_idempotency` usa `TECHNICAL_TOKEN` y el shell no lo conserva en un
  borrador seguro;
- el handler genera UUID, deriva `MANUAL_UI`, valida dimensiones y rota el token
  sólo tras éxito;
- `TRANSFER` rechaza origen y destino iguales y construye dos líneas atómicas;
- una acción que no pertenece a la tarea seleccionada se rechaza antes de la
  autorización de mutación.
- `NAVIGATE` procesa sólo la región actual; las mutaciones continúan procesando
  el formulario completo y la incorporación inicial funciona sin un artículo de
  inventario preexistente.
- las acciones dinámicas usan un puente JSF estable, confirmación proporcional y
  transporte `floorplanInput.<id-semántico>`; el servidor valida cada
  `ScreenElementId`, rechaza valores ambiguos y reaplica el valor enviado después
  de refrescar el estado técnico y los valores predeterminados.

## Gates focales ejecutados

```powershell
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-03/pom.xml -pl plugins/inventory -am '-Dtest=InventoryStockFloorplanContractTest,InventoryPluginDefinitionTest,InventorySelectorSourcesTest,InventoryStockScreenHandlerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-03/pom.xml -pl web-shell -am '-Dtest=InventoryScreenRendererTest,ShellFloorplanRendererTest,ShellFloorplanResourceTest,ShellScreenInteractionViewTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
.\mvnw.cmd -f .tools/tmp/validation/J11-S10-03/pom.xml -pl plugins/inventory -am '-Dtest=InventoryStockScreenHandlerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

| Gate | Resultado |
|---|---|
| contrato/descriptor/selectores/handler | 12 pruebas, 0 fallos/errores/omitidas |
| renderer/recursos/estado | 11 pruebas, 0 fallos/errores/omitidas |
| seguridad focal del handler | 9 pruebas, 0 fallos/errores/omitidas |
| navegación regional y contrato | 6 pruebas, 0 fallos/errores/omitidas |
| compilación del recorrido Playwright | 5 clases, 0 errores |

El primer intento del gate de Inventario encontró un archivo obsoleto del
prototipo en una materialización previamente reutilizada. Se verificó la ruta,
se recreó el directorio exclusivamente bajo `.tools/tmp/validation/` y se repitió
el gate verde contra el índice. No fue una falla del código candidato.

El primer gate del shell detectó una prueba de compatibilidad que todavía usaba
`inventory:stock` como ejemplo v1. Se corrigió para proteger
`inventory:warehouses`, que continúa siendo maestro v1, y la repetición quedó
verde.

## Gates finales ejecutados

| Gate | Resultado |
|---|---|
| suite completa `plugins/inventory` | 65 pruebas, 0 fallos/errores/omitidas |
| suite completa `web-shell` | 72 pruebas, 0 fallos/errores/omitidas |
| ArchUnit | 34 pruebas, 0 fallos/errores/omitidas |
| `mvn verify` raíz | 28 módulos; 555 pruebas, 0 fallos/errores/omitidas |
| Playwright `InventoryVisualIT` | 1 recorrido integral, 0 fallos/errores/omitidas |
| responsive | 375, 599, 600, 720, 839, 840 y 1280 px sin overflow normal |
| evidencia visual | 24 PNG; 3.765.277 bytes; revisión visual conforme |
| documentación | 374 Markdown; sin enlaces rotos, errores UTF-8, mojibake ni secretos |
| Compose | `app` saludable, 8 plugins, bootstrap global deshabilitado |
| health | `/health/live` 200 y `/health/ready` 200 |
| migraciones | primera ejecución limpia e idempotencia confirmada en segunda ejecución |

La imagen final de aplicación usa el perfil físico `with-purchasing-demo`:

- manifiesto de plataforma: `sha256:e4bec1b5b30f4088ec1904ef0d8297e276eef43150acf398c796e132604b08ed`;
- manifiesto de ejecución: `sha256:904f02b474a332dfbd5bb00f735bfdb3f6094287680e3394f7c86cfa0270f8c2`.

El E2E recorrió depósito y ubicación, incorporación de artículo, entrada de 12,
reserva de 3, disponibilidad de 9, conteo contabilizado, confirmaciones,
accesibilidad estructural, límites responsive y denegación segura al desactivar
el plugin. Las capturas están en
[`screenshots/J11-S10-03/e2e`](screenshots/J11-S10-03/e2e/).

## Incidencias encontradas y resueltas

- la autoridad global faltante se restauró mediante el bootstrap explícito y se
  comprobó su idempotencia antes de volver a dejarlo deshabilitado;
- un preflight con perfil físico incorrecto fue rechazado y la candidata se
  reconstruyó con `with-purchasing-demo`;
- el ciclo de vida request-scoped de JSF perdía acción y datos dinámicos en el
  postback; el puente estable y el transporte semántico corrigen la causa sin
  confiar en estado del cliente;
- Playwright intentó inicialmente escribir en el perfil global; la repetición
  verde fijó `PLAYWRIGHT_BROWSERS_PATH` dentro de `.tools/playwright`;
- dos localizadores E2E dependían de semántica visual incorrecta: se sustituyeron
  por identificadores semánticos y la regla de labels conserva su alcance sobre
  controles editables.

PostgreSQL/Testcontainers y las migraciones no cambiaron en esta historia; el
recorrido reutiliza el dominio, servicio transaccional y tablas ya validados. La
validación humana independiente continúa pendiente bajo la autorización vigente.
