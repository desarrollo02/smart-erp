# Sprint 10 — Floorplans operativos y transaccionales

- Estado: Planificado; no iniciado
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
| 1 | J11-S10-00 | Planificada | gobierno, inventario de pantallas, tareas y métricas |
| 2 | J11-S10-01 | Planificada | `plugin-api` versionada con propósito, semántica y acciones dinámicas |
| 3 | J11-S10-02 | Planificada | renderers cerrados del shell y compatibilidad con maestros |
| 4 | J11-S10-03 | Planificada | movimiento de Inventario operativo y condicional |
| 5 | J11-S10-04 | Planificada | órdenes, recepción/devolución y aprobación de Compras |
| 6 | J11-S10-05 | Planificada | regresión, seguridad negativa, accesibilidad y Playwright responsive |
| 7 | J11-S10-06 | Planificada | composición, demo, documentación, fotografía, PDF y cierre técnico |
| 8 | J11-S10-07 | Planificada | decisión y, sólo con `SÍ`, gate de instalador Windows |

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
