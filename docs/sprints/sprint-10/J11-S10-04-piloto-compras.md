# J11-S10-04 — Piloto de Compras

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Sprint: 10
- Fecha: 2026-08-15
- Tipo: contratos funcionales, handlers y recorridos Jakarta Faces
- Dependencia: J11-S10-03 completada
- ADR rector: [ADR-0047](../../adr/0047-floorplans-operativos-transaccionales.md)

## Objetivo

Recuperar el recorrido completo de Compras sobre sus cinco rutas existentes y
migrarlo a los floorplans 2.0, sin crear pantallas paralelas ni cambiar dominio,
persistencia o propiedad de datos.

## Alcance

- publicar `purchasing` 1.2.0 con cinco contratos visuales 2.0;
- conservar `purchasing:requests`, `purchasing:orders`,
  `purchasing:receipts`, `purchasing:returns` y `purchasing:tracking`;
- presentar solicitudes y aprobación como `WORKLIST`;
- presentar órdenes como `TRANSACTION_EDITOR` con cabecera, líneas, resumen y
  acciones de ciclo de vida;
- presentar recepciones y devoluciones como `GUIDED_OPERATION` con referencias
  dependientes y datos condicionales;
- presentar seguimiento como `INQUIRY` de solo lectura;
- mantener identidad, versión e idempotencia como estado técnico oculto;
- conservar autorización y reglas de negocio en los handlers y casos de uso;
- adaptar resultados tabulares a tarjetas estáticas en ancho compacto;
- actualizar el manual de Compras y registrar evidencia reproducible.

## Criterios de aceptación

- **CA-01:** las cinco rutas y los cinco `ScreenId` existentes siguen resolviendo
  sin menús ni pantallas duplicados.
- **CA-02:** cada contrato declara versión 2.0, propósito cerrado, regiones y
  semántica de elementos.
- **CA-03:** Solicitudes permite buscar, seleccionar, preparar, agregar líneas,
  enviar y decidir con acciones válidas para estado, permiso y actor.
- **CA-04:** el solicitante no puede aprobar su propia solicitud y el servidor no
  confía en que la acción esté meramente oculta.
- **CA-05:** Órdenes reúne cabecera, líneas, total y ciclo de vida en una sola
  experiencia y conserva asignaciones y compras directas.
- **CA-06:** Recepciones solicita depósito, ubicación y trazabilidad únicamente
  cuando la línea seleccionada es Stock.
- **CA-07:** Devoluciones parte de una recepción confirmada, conserva su
  trazabilidad y limita la cantidad disponible.
- **CA-08:** Seguimiento continúa siendo estrictamente de lectura y muestra
  pedida, recibida, devuelta y pendiente.
- **CA-09:** los selectores dependientes conservan su contexto tanto al cambiar
  la referencia como al buscar opciones en servidor.
- **CA-10:** identidad, versión e idempotencia no son entradas editables ni datos
  técnicos solicitados al operador.
- **CA-11:** 375, 720 y 1280 px, además de 599/600/839/840, no presentan overflow
  horizontal normal y las tablas usan una alternativa explícita en compacto.
- **CA-12:** pruebas focales, módulos, arquitectura, reactor, Docker/Compose,
  health y Playwright quedan verdes.

## Decisiones de implementación

La migración ocurre en los contratos y handlers vigentes. El dominio, las
migraciones V1–V2 y los casos de uso permanecen sin cambios. El renderer del
shell es dueño del XHTML, CSS y JavaScript; el plugin aporta solamente contratos
neutrales y estado.

Las tablas v2 conservan semántica tabular en medio y expandido, y publican una
representación estática como tarjetas en compacto. Cuando existe un documento
seleccionado, la bandeja deja el resultado en segundo plano y prioriza el detalle
y las acciones correspondientes.

Los selectores de referencias operativas usan postback regional para recalcular
opciones y campos dependientes. Como la vista es request-scoped, el shell
transporta exclusivamente los controles declarados por el contrato mediante
`floorplanInput.<id-semántico>`, valida los identificadores en servidor y
reaplica el borrador seguro. Así, buscar una opción no pierde la orden, línea,
recepción o depósito ya elegidos.

Las acciones de fila reservan navegación sólo cuando existe tabla operativa; el
contenido no enlazable se mantiene como lectura. Los botones dinámicos utilizan
un puente JSF estable, y las mutaciones siguen revalidando empresa, plugin,
pantalla, acción, permiso, estado y versión.

## Validación

- `plugins/purchasing`: 28 pruebas verdes;
- `web-shell`: 74 pruebas verdes;
- ArchUnit: 34 pruebas verdes;
- reactor raíz: 28 módulos y 565 pruebas verdes;
- `PurchasingVisualIT`: 1 recorrido integral verde en aplicación real;
- 18 capturas PNG revisadas en 375, 720 y 1280 px, más límites
  599/600/839/840 sin overflow horizontal normal;
- Docker/Compose saludable con PostgreSQL y Keycloak conservados;
- health live/ready 200;
- imagen `logixone/app:j11-s10-04-r7`, manifiesto de plataforma
  `sha256:54fe57c6f4bdc17a05c4713dc76225bbed0397aa58a7282169ec536f82f1d3f3`
  y manifiesto de ejecución
  `sha256:b9aefdee95a150021f5efb2baa4279c1e323b169218a9c3d8b0fbcab566016d8`;
- materialización final: `.tools/tmp/validation/J11-S10-04/`.

La historia queda completada automáticamente. La validación independiente
continúa pendiente y no equivale a aceptación humana ni cierre de Sprint 10. El
siguiente trabajo autorizado es J11-S10-05.

La evidencia acumulada está en
[J11-S10-04-piloto-compras](../../evidence/J11-S10-04-piloto-compras.md).
