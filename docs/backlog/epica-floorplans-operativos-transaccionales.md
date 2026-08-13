# Épica — Floorplans operativos y transaccionales

- Estado: Aceptada y planificada
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

| Orden | Historia | Resultado esperado |
|---:|---|---|
| 1 | J11-S10-00 | gobierno, clasificación de pantallas y métricas de tarea |
| 2 | J11-S10-01 | contrato neutral versionado de propósito, semántica y acciones |
| 3 | J11-S10-02 | renderers shell-owned para `WORKLIST`, `TRANSACTION_EDITOR` y `GUIDED_OPERATION` |
| 4 | J11-S10-03 | piloto de movimiento de Inventario con captura condicional |
| 5 | J11-S10-04 | piloto de Compras: órdenes, recepciones, devoluciones y aprobación |
| 6 | J11-S10-05 | regresión de maestros, accesibilidad, responsive y seguridad negativa |
| 7 | J11-S10-06 | composición, demo, documentación, PDF y gate acumulado |
| 8 | J11-S10-07 | decisión explícita del instalador Windows |

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
