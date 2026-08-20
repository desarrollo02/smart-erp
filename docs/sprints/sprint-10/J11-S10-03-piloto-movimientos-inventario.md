# J11-S10-03 — Piloto de movimientos de Inventario

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Sprint: 10
- Fecha de inicio: 2026-08-14
- Tipo: contrato funcional, handler y recorrido Jakarta Faces
- Dependencia: J11-S10-02 completada
- ADR rector: [ADR-0047](../../adr/0047-floorplans-operativos-transaccionales.md)

## Objetivo

Migrar `inventory:stock` en `/inventory` al floorplan
`GUIDED_OPERATION` 2.0, de modo que un operador registre una entrada, salida o
transferencia capturando sólo los datos aplicables y sin transcribir identidad,
versión, fuente o idempotencia.

## Alcance

- conservar el `ScreenId`, la ruta y el menú existentes;
- publicar `inventory` 1.2.0 y `plugin-api` 0.4.5;
- usar contexto, contenido, guía, resumen y acciones del renderer cerrado;
- seleccionar en contexto una tarea y un artículo activo;
- conservar como tareas subordinadas disponibilidad, reservas y administración
  del artículo;
- adaptar destino al tipo `TRANSFER`;
- adaptar lote, serie y vencimiento a las políticas del artículo;
- generar una clave UUID canónica por intento y mantenerla como token técnico
  oculto;
- derivar en servidor la fuente manual y su referencia;
- revalidar tarea, artículo, empresa y permiso antes de mutar;
- conservar el token en un fallo funcional y rotarlo después de un éxito;
- excluir tokens técnicos del borrador seguro del selector.
- procesar `Continuar` sólo sobre la región de contexto para que cambiar tarea o
  preparar una transferencia no active requisitos de una operación aún incompleta.

## Criterios de aceptación

- **CA-01:** `/inventory` continúa resolviendo `inventory:stock`; no existe una
  ruta ni un `ScreenId` adicionales para el piloto.
- **CA-02:** el contrato 2.0 declara `GUIDED_OPERATION`, las cinco regiones y
  semántica para todo elemento no acción.
- **CA-03:** el operador selecciona una referencia legible de artículo; no copia
  UUID, versión, fuente ni clave de idempotencia.
- **CA-04:** entrada y salida no muestran destino; transferencia exige depósito y
  ubicación destino distintos del origen.
- **CA-05:** lote, serie y vencimiento se muestran, ocultan o requieren según
  `TrackingMode` y `ExpiryPolicy`.
- **CA-06:** `ADJUSTMENT` y `REVERSAL` continúan fuera del formulario manual.
- **CA-07:** un campo oculto manipulado, un token no canónico o una acción de
  otra tarea se rechazan antes de solicitar permiso de movimiento.
- **CA-08:** contabilizar exige `inventory.movements.post` en servidor y utiliza
  el mismo caso de uso transaccional vigente.
- **CA-09:** una transferencia produce débito y crédito atómicos con igual
  cantidad, condición y trazabilidad.
- **CA-10:** el shell exige confirmación para registrar y no conserva tokens en
  borradores de retorno.
- **CA-11:** los manuales de usuario, desarrollo e implementación describen el
  contrato y el recorrido vigentes.
- **CA-12:** pruebas focales, módulos, arquitectura, reactor y Playwright
  responsive quedan verdes antes de cerrar la historia.

## Decisiones de implementación

La migración se realiza sobre `inventory:stock`, tal como fijó J11-S10-00. La
pantalla mantiene una sola ruta y ofrece cuatro tareas cerradas: movimiento,
disponibilidad, reserva y administración del artículo. `MOVEMENT` es la tarea
inicial del piloto.

`TECHNICAL_TOKEN` extiende la semántica neutral sin exponer una primitiva visual.
El shell lo materializa como estado oculto, no lo incluye en borradores seguros y
el handler acepta solamente UUID canónicos. La fuente persistida es `MANUAL_UI` y
la referencia deriva del token, no de una entrada del operador.

El handler continúa siendo `InventoryStockScreenHandler`; no se duplica el
adaptador ni el caso de uso. Las políticas se leen otra vez desde el artículo
activo antes de construir `StockMovementRequest`, por lo que ocultar un control
no sustituye la validación del servidor.

`APPLY_STOCK_TASK` conserva intención `NAVIGATE`. El renderer la ejecuta por Ajax
contra la región de contexto; las mutaciones continúan enviando el formulario
completo. `MOVEMENT_ITEM` es opcional en el contrato para permitir incorporar el
primer artículo de una empresa vacía, pero cada operación que lo necesita lo
exige y resuelve otra vez en el handler.

El postback de acciones v2 se desacopla del alcance request-scoped mediante un
puente JSF estable. Antes de enviarlo, JavaScript propiedad del shell copia sólo
controles declarados con `data-screen-input` a nombres
`floorplanInput.<id-semántico>`. El servidor reconstruye y valida esos IDs,
refresca permisos, opciones y tokens técnicos, y después reaplica los valores
enviados para ejecutar la acción. Los valores ambiguos o fuera del contrato se
rechazan.

## Validación final

- contrato, descriptor, selectores y handler: gates focales verdes;
- `plugins/inventory`: 65 pruebas verdes;
- `web-shell`: 72 pruebas verdes;
- ArchUnit: 34 pruebas verdes;
- reactor raíz: 28 módulos y 555 pruebas verdes;
- `InventoryVisualIT`: 1 recorrido integral verde en aplicación real;
- 24 capturas revisadas en 375, 720 y 1280 px, más límites
  599/600/839/840 sin overflow horizontal normal;
- Docker/Compose saludable con 8 plugins, health live/ready 200 y bootstrap
  global deshabilitado;
- materialización exacta: `.tools/tmp/validation/J11-S10-03/`.

La historia queda completada automáticamente el 2026-08-15. La validación
independiente continúa pendiente y no equivale a aceptación humana ni cierre de
Sprint 10. El siguiente trabajo autorizado es J11-S10-04; no se inició en este
corte.

La evidencia acumulada y los gates finales se registran en
[J11-S10-03-piloto-movimientos-inventario](../../evidence/J11-S10-03-piloto-movimientos-inventario.md).
