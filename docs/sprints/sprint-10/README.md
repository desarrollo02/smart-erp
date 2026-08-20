# Sprint 10 — Floorplans operativos y transaccionales

- Estado: En curso; J11-S10-00 a J11-S10-07 completadas; sólo la validación independiente permanece pendiente
- Fecha de planificación: 2026-08-13
- Dependencia técnica: baseline de Sprint 9 congelado por J11-S9-07
- Dependencia de gobierno: J11-S9-08 registró `SÍ` y creó el instalador interno;
  su matriz Windows externa continúa pendiente
- Siguiente plugin funcional: `sales`, diferido hasta completar este Sprint
- ADR rector: [ADR-0047](../../adr/0047-floorplans-operativos-transaccionales.md)
- Épica: [Floorplans operativos](../../backlog/epica-floorplans-operativos-transaccionales.md)

## Objetivo

Evolucionar los contratos visuales neutrales y el renderer del shell para que
maestros, bandejas de trabajo, documentos y capturas operativas utilicen
floorplans distintos según su tarea, sin entregar XHTML o dependencias Jakarta a
los plugins.

## Razón de precedencia

`sales` será el siguiente plugin ERP y necesitará presupuestos y pedidos con
cabecera, líneas, importes, disponibilidad y acciones de ciclo de vida. Construir
Ventas antes de esta fundación repetiría el patrón genérico y obligaría a migrar
inmediatamente sus contratos. Por eso Sprint 10 se ejecuta entre Compras y Ventas
como gate transversal; no altera la numeración 4 → 5 de ADR-0011.

## Orden planificado

| Orden | Historia | Estado | Resultado esperado |
|---:|---|---|---|
| 1 | J11-S10-00 | Completada | gobierno, inventario de pantallas, tareas y métricas |
| 2 | J11-S10-01 | Completada | `plugin-api` versionada con propósito, semántica y acciones dinámicas |
| 3 | J11-S10-02 | Completada | renderers cerrados del shell y compatibilidad con maestros |
| 4 | J11-S10-03 | Completada automáticamente; validación independiente pendiente | movimiento de Inventario operativo y condicional |
| 5 | J11-S10-04 | Completada automáticamente; validación independiente pendiente | órdenes, recepción/devolución y aprobación de Compras |
| 6 | J11-S10-05 | Completada automáticamente; validación independiente pendiente | regresión, seguridad negativa, accesibilidad y Playwright responsive |
| 7 | J11-S10-06 | Completada automáticamente; validación independiente pendiente | composición, demo, documentación, fotografía, PDF y cierre técnico |
| 8 | J11-S10-07 | Completada; decisión `NO` | no se crea instalador; `current` de Sprint 9 permanece intacto |

## Criterios de entrada

- J11-S9-07 ha congelado un baseline con gates automatizados verdes;
- J11-S9-08 ha registrado `SÍ` o `NO` sobre el instalador;
- si la respuesta fue `SÍ`, sus gates técnicos pendientes se resolvieron o existe
  una continuidad explícita que no los presenta como verdes;
- no existe una prueba automatizada fallando en el baseline de partida;
- las pantallas piloto y las tareas de sus roles están inventariadas;
- la validación independiente pendiente, si continúa diferida, está identificada
  sin presentar Sprint 9 como cerrado.

## Alcance

- clasificación `MASTER_DATA`, `WORKLIST`, `TRANSACTION_EDITOR`,
  `GUIDED_OPERATION` e `INQUIRY`;
- tipos semánticos neutrales y compatibilidad versionada;
- disponibilidad contextual de acciones y campos;
- editor de orden con líneas y resumen;
- bandeja de solicitudes/aprobaciones;
- captura de movimiento, recepción y devolución;
- adaptación 375/720/1280 y límites 599/600/839/840;
- documentación de usuario, desarrollador e implementación afectada;
- demo real, fotografía de plugins, PDF y decisión de instalador.

## Límites

- no implementar `sales` durante este Sprint;
- no cambiar el dominio ni la persistencia de Compras o Inventario salvo que una
  prueba demuestre una carencia funcional independiente del layout y producto la
  acepte por separado;
- no permitir XHTML, CSS, JavaScript o EL de plugins;
- no ocultar autorización detrás de visibilidad de UI;
- no pedir al operador claves técnicas o de idempotencia;
- no convertir toda tarea frecuente en un asistente;
- no declarar cierre sin demo, PDF, gates y decisión de instalador.

## Momento autorizado

Sprint 10 no inició código durante J11-S9-07/J11-S9-08. Al satisfacer los
criterios de entrada, J11-S10-00 será el primer trabajo. `sales` podrá abrir su
caracterización en Sprint 11 después de J11-S10-06 y J11-S10-07.

## Baseline de diseño aceptado

J11-S10-00 aceptó el
[inventario de pantallas, tareas y métricas](inventario-tareas-metricas-floorplans.md)
el 2026-08-14. Clasifica 23 pantallas, caracteriza los seis recorridos piloto,
registra métricas del renderer v1 y fija la compatibilidad v1/v2. Esta aceptación
habilita J11-S10-01; todavía no modifica contratos ni interfaz.

J11-S10-01 entregó `plugin-api` 0.4.4 y la frontera v1/v2 con reactor y
arquitectura verdes. El siguiente trabajo habilitado es J11-S10-02, que pertenece
al shell y no debe migrar todavía los pilotos de Inventario o Compras.

J11-S10-02 propagó la experiencia por `ComposedScreen`, registró los cinco
floorplans cerrados, añadió el renderer Facelets responsive y conservó las
pantallas v1. Sus 546 pruebas y 28 módulos quedaron verdes. El siguiente trabajo
habilitado es J11-S10-03, piloto de movimiento de Inventario.

J11-S10-03 migró `inventory:stock` sobre la ruta existente
`/inventory`, sin crear pantalla ni menú paralelos. `inventory` 1.2.0 usa
`GUIDED_OPERATION` 2.0, selecciona tarea y artículo en contexto, adapta destino y
trazabilidad y mantiene fuente, identidad, versión e idempotencia fuera de la
transcripción. Sus 555 pruebas, 28 módulos, Compose/health y el recorrido
Playwright integral quedaron verdes; 24 capturas responsive fueron revisadas.
La validación independiente continúa pendiente.

J11-S10-04 conservó las cinco rutas y pantallas existentes de Compras y migró
sus contratos a la experiencia 2.0: solicitudes como `WORKLIST`, órdenes como
`TRANSACTION_EDITOR`, recepciones y devoluciones como `GUIDED_OPERATION`, y
seguimiento como `INQUIRY`. `purchasing` 1.2.0 mantiene el dominio y las
migraciones vigentes, oculta identidad, versión e idempotencia técnicas, adapta
las referencias dependientes en contexto y reemplaza las tablas por tarjetas
operativas en compacto. Las suites de Compras, shell y arquitectura, el reactor
de 28 módulos, Docker/Compose, health y el recorrido Playwright quedaron verdes;
18 capturas responsive fueron revisadas. La validación independiente continúa
pendiente. El siguiente trabajo autorizado es J11-S10-05.

J11-S10-05 añadió restauración de foco después de postbacks, verificó foco
visible por teclado y movimiento reducido, y repitió los recorridos completos de
Inventario y Compras en 375/599/600/720/839/840/1280. La revisión visual detectó
y corrigió una superposición de la barra empresarial en compacto. El reactor de
28 módulos, ArchUnit, health/OIDC, migraciones, Docker/Compose y Playwright
quedaron verdes: 573 pruebas automatizadas y 42 capturas revisadas. La seguridad
negativa confirmó la denegación al desactivar cada plugin. La validación
independiente continúa pendiente. El siguiente trabajo autorizado es J11-S10-06.

J11-S10-06 congeló la candidata `j11-s10-06-closing-v3`, repitió reactor,
ArchUnit, migraciones, health, JTA, OIDC, seguridad negativa y Playwright, y
regeneró el paquete documental. Quedaron verdes 586 pruebas automatizadas únicas
y se revisaron 171 capturas de los tres rangos responsive. El arnés JTA opt-in
fue retirado y la aplicación normal conserva únicamente `logixone.war`. La
validación independiente continúa pendiente; el siguiente gate es J11-S10-07,
que debe registrar `SÍ` o `NO` sobre un nuevo instalador Windows.

J11-S10-07 registró `NO` el 2026-08-20. No se ejecutó construcción, promoción ni
instalación y los ocho archivos de `installer/windows/current` permanecen
intactos como edición interna de Sprint 9, sin representar Sprint 10. El último
gate de decisión queda completo; Sprint 10 continúa abierto únicamente por la
validación independiente acumulada. La continuidad autorizada habilita la
caracterización de `sales` en Sprint 11.
