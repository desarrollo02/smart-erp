# Épica — Floorplans operativos y transaccionales

- Estado: Implementada y validada automáticamente; decisión de instalador `NO`; validación independiente pendiente
- Prioridad: inmediata después de Sprint 9 y antes de `sales`
- Decisión: [ADR-0047](../adr/0047-floorplans-operativos-transaccionales.md)
- Iteración objetivo: [Sprint 10](../sprints/sprint-10/README.md)
- Propietario técnico: shell y `plugin-api`

## Problema

Las pantallas de maestros, movimientos y documentos comparten hoy el mismo
floorplan. La consistencia visual se conserva, pero una orden, una recepción o un
movimiento necesita una experiencia más densa, contextual y orientada a completar
trabajo que la administración de un catálogo.

## Resultado de producto

Entregar una familia cerrada de patrones de shell que permita:

- procesar una bandeja por estado y prioridad;
- crear documentos con cabecera, múltiples líneas y resumen;
- capturar recepciones, devoluciones y movimientos con campos condicionales;
- mostrar sólo acciones válidas para estado, permiso y actor;
- mantener ocultos versión, identidad técnica e idempotencia;
- operar con teclado y adaptar el recorrido a compacto, medio y expandido;
- reutilizar la misma fundación en `sales` sin XHTML aportado por plugins.

## Historias planificadas

| Orden | Historia | Estado | Resultado esperado |
|---:|---|---|---|
| 1 | J11-S10-00 | Completada | gobierno, clasificación de pantallas y métricas de tarea |
| 2 | J11-S10-01 | Completada | contrato neutral versionado de propósito, semántica y acciones |
| 3 | J11-S10-02 | Completada | renderers shell-owned para los cinco propósitos y compatibilidad v1 |
| 4 | J11-S10-03 | Completada automáticamente; validación independiente pendiente | piloto de movimiento de Inventario con captura condicional |
| 5 | J11-S10-04 | Completada automáticamente; validación independiente pendiente | piloto de Compras: órdenes, recepciones, devoluciones y aprobación |
| 6 | J11-S10-05 | Completada automáticamente; validación independiente pendiente | regresión de maestros, accesibilidad, responsive y seguridad negativa |
| 7 | J11-S10-06 | Completada automáticamente; validación independiente pendiente | composición, demo, documentación, PDF y gate acumulado |
| 8 | J11-S10-07 | Completada; decisión `NO` | no se crea instalador y se conserva intacto el de Sprint 9 |

## Criterios de aceptación

- **OT-CE01:** cada pantalla declara un propósito cerrado y compatible.
- **OT-CE02:** los maestros vigentes conservan directorio, alta y ficha sin
  regresiones funcionales.
- **OT-CE03:** una orden permite trabajar cabecera, líneas y resumen sin convertir
  cada línea en un formulario aislado.
- **OT-CE04:** la bandeja muestra sólo trabajo procesable y las acciones válidas
  para estado, actor y permiso.
- **OT-CE05:** las acciones destructivas están separadas y requieren la
  confirmación proporcional al riesgo.
- **OT-CE06:** movimientos y recepciones muestran únicamente campos aplicables al
  tipo y seguimiento seleccionados.
- **OT-CE07:** versión, identidad e idempotencia no son campos editables del
  operador.
- **OT-CE08:** el servidor revalida empresa, plugin, pantalla, acción, permiso,
  estado, versión e idempotencia en cada mutación.
- **OT-CE09:** 375, 720 y 1280 px, más 599/600/839/840, no presentan overflow
  horizontal normal y tienen adaptación operativa explícita.
- **OT-CE10:** teclado, foco, labels, contraste, mensajes y movimiento reducido
  quedan cubiertos por pruebas.
- **OT-CE11:** la demo usa Inventario y Compras reales, sin mocks ni datos
  técnicos solicitados al usuario.
- **OT-CE12:** `sales` no inicia interfaz ni contratos visuales antes de que esta
  épica complete sus gates automatizados.

## Fuera de alcance

- cambiar dominio, tablas o propiedad de datos de Inventario y Compras;
- implementar `sales` dentro de Sprint 10;
- agregar una SPA o biblioteca visual no aprobada;
- permitir layouts, XHTML o scripts arbitrarios desde plugins;
- rediseñar por estética todas las pantallas administrativas;
- implementar todavía la terminal `point_of_sale`.

## Momento de ejecución

El siguiente trabajo continúa siendo J11-S9-07 y luego J11-S9-08. Después de
congelar ese baseline técnico y registrar la decisión del instalador comienza
Sprint 10. Si la validación independiente continúa diferida bajo la autorización
vigente, Sprint 9 permanecerá formalmente abierto, pero la materialización de
Sprint 10 partirá del baseline congelado y conservará explícito ese pendiente.

Ventas, plugin ERP 5, comenzará a partir de Sprint 11.

J11-S10-00 quedó aceptada el 2026-08-14 mediante el
[inventario de pantallas, tareas y métricas](../sprints/sprint-10/inventario-tareas-metricas-floorplans.md).
Esto habilitó en ese momento J11-S10-01.

J11-S10-01 quedó completada el 2026-08-14 con `plugin-api` 0.4.4, compatibilidad
v1/v2, estados dinámicos y gates automatizados verdes. El siguiente trabajo
habilitado es J11-S10-02.

J11-S10-02 completó la propagación por el kernel y los renderers cerrados del
shell sin migrar aún Inventario o Compras; 546 pruebas y 28 módulos quedaron
verdes. El siguiente trabajo habilitado es J11-S10-03.

J11-S10-03 conserva `/inventory` e `inventory:stock`, migra el contrato a
`GUIDED_OPERATION` 2.0 y genera internamente la fuente e idempotencia del
movimiento. Destino, lote, serie y vencimiento son condicionales y el servidor
rechaza campos ocultos manipulados, tareas ajenas y tokens no canónicos antes de
autorizar la mutación. Sus 555 pruebas, 28 módulos, Docker/Compose, health y
Playwright integral quedaron verdes; la validación independiente continúa
pendiente.

J11-S10-04 conserva las cinco rutas de Compras y migra sus contratos a
`WORKLIST`, `TRANSACTION_EDITOR`, `GUIDED_OPERATION` e `INQUIRY` 2.0. La
aprobación independiente, la edición de órdenes por cabecera/líneas/resumen, la
recepción y devolución guiadas y el seguimiento de cumplimiento quedaron
validados en aplicación real, incluidos los rangos 375/720/1280 y los límites
599/600/839/840. No se cambió dominio ni persistencia y no se creó una pantalla
paralela. La validación independiente continúa pendiente y J11-S10-05 queda
habilitada como siguiente trabajo.

J11-S10-05 completó la regresión transversal con restauración de foco,
movimiento reducido, foco visible por teclado, seguridad negativa y los límites
responsive 375/599/600/720/839/840/1280. La revisión visual encontró y corrigió
la superposición compacta de la barra empresarial. Reactor, ArchUnit,
health/OIDC, migraciones, Docker/Compose y los recorridos Playwright de
Inventario y Compras quedaron verdes, con 573 pruebas automatizadas y 42
capturas revisadas. La validación independiente continúa pendiente y J11-S10-06
queda habilitada como siguiente trabajo.

J11-S10-06 congeló la candidata técnica final, confirmó 28/28 módulos, 565
pruebas Surefire, 34 escenarios ArchUnit, 12 pruebas de integración y 9
recorridos Playwright. Las migraciones fueron repetibles, la candidata quedó
saludable y se revisaron 171 capturas responsive. La historia queda implementada
y validada automáticamente; la aceptación independiente sigue pendiente y
J11-S10-07 es el siguiente gate.

J11-S10-07 registró `NO` el 2026-08-20. No modificó
`installer/windows/current`; su edición `0.9.0-internal.1` sigue perteneciendo a
Sprint 9 y no representa este baseline. La épica queda implementada y validada
automáticamente, con aceptación independiente pendiente. `sales` puede iniciar
su caracterización en Sprint 11 conforme al orden de ADR-0011.
