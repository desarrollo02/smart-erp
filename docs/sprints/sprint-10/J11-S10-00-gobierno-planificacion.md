# J11-S10-00 — Gobierno y planificación de floorplans operativos

- Estado: Completada; caracterización aceptada
- Sprint: 10
- Fecha de planificación: 2026-08-13
- Fecha de ejecución: 2026-08-14
- Tipo: gobierno, caracterización de tareas y planificación
- ADR rector: [ADR-0047](../../adr/0047-floorplans-operativos-transaccionales.md)

## Objetivo

Antes de cambiar `plugin-api` o el shell, clasificar las pantallas vigentes,
describir las tareas de los roles operativos y fijar métricas reproducibles que
permitan comprobar que el nuevo diseño facilita la operación.

## Actividades ejecutadas

1. inventariar pantallas vigentes por propósito y frecuencia de uso;
2. caracterizar con roles ficticios las tareas de ordenar, aprobar, recibir,
   devolver y mover existencias;
3. medir el recorrido actual: pasos, cambios de foco, scroll, errores y campos
   técnicos expuestos;
4. definir estados, acción primaria, acciones secundarias y destructivas por
   documento;
5. definir comportamiento de teclado, búsqueda, selección, líneas y escaneo;
6. fijar adaptación en compacto, medio y expandido;
7. confirmar versión y estrategia de compatibilidad de contratos;
8. convertir el resultado en criterios ejecutables para J11-S10-01 a J11-S10-06.

## Criterios de aceptación

- **CA-01:** cada pantalla vigente queda clasificada sin ambigüedad.
- **CA-02:** cada piloto tiene actor, objetivo, precondiciones, recorrido y
  resultado esperado.
- **CA-03:** se identifican todos los datos técnicos que no debe transcribir el
  operador.
- **CA-04:** las acciones válidas quedan definidas por estado y permiso.
- **CA-05:** las métricas comparan baseline y candidato sin depender de opinión
  visual aislada.
- **CA-06:** se definen 375/720/1280 y 599/600/839/840, teclado y accesibilidad.
- **CA-07:** no se modifica código antes de aceptar la caracterización.
- **CA-08:** `sales` continúa fuera de alcance hasta completar Sprint 10.

## Resultado

La caracterización canónica se conserva en
[Inventario, tareas y métricas de floorplans](inventario-tareas-metricas-floorplans.md).
El corte:

- clasifica las 23 pantallas navegables vigentes;
- define seis recorridos piloto con actor, precondiciones y resultado;
- separa acciones por estado, permiso y riesgo;
- identifica los campos técnicos hoy visibles y los datos internos que deben
  permanecer fuera de la transcripción;
- fija AS, CF, CT, EV, DV y OH como métricas reproducibles;
- registra dimensiones de capturas Playwright existentes y umbrales del candidato;
- fija teclado, escaneo, accesibilidad y adaptación en 375/599/600/720/839/840/1280;
- acepta una evolución aditiva compatible de contratos v1 a v2.

## Trazabilidad de aceptación

| Criterio | Evidencia |
|---|---|
| CA-01 | inventario cerrado de 6 pantallas shell y 17 de plugins |
| CA-02 | seis fichas de tarea en la sección 3 |
| CA-03 | inventario técnico de movimiento, reservas y documentos |
| CA-04 | matrices de solicitud, orden, recepción, devolución y movimiento |
| CA-05 | definiciones, baseline numérico, fuentes y umbrales candidatos |
| CA-06 | teclado, foco, escaneo y siete anchos de prueba |
| CA-07 | historia exclusivamente documental; código sin modificar |
| CA-08 | `sales` sigue explícitamente fuera de alcance |

La evidencia de ejecución y validación está en
[J11-S10-00-gobierno-floorplans](../../evidence/J11-S10-00-gobierno-floorplans.md).

## Dependencia de inicio resuelta

Esta historia se habilita únicamente después de J11-S9-07 y J11-S9-08. Crear el
documento de planificación no inicia código ni modifica el baseline de Compras.

J11-S9-07 congeló el baseline técnico y J11-S9-08 registró `SÍ` y construyó el
instalador interno. Su validación independiente continúa pendiente y no se
presenta como aceptación comercial. La continuidad funcional vigente autoriza
J11-S10-01 con gates automatizados normales.
