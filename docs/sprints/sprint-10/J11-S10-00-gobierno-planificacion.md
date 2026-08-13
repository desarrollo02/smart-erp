# J11-S10-00 — Gobierno y planificación de floorplans operativos

- Estado: Planificada; no iniciada
- Sprint: 10
- Fecha de planificación: 2026-08-13
- Tipo: gobierno, caracterización de tareas y planificación
- ADR rector: [ADR-0047](../../adr/0047-floorplans-operativos-transaccionales.md)

## Objetivo

Antes de cambiar `plugin-api` o el shell, clasificar las pantallas vigentes,
describir las tareas de los roles operativos y fijar métricas reproducibles que
permitan comprobar que el nuevo diseño facilita la operación.

## Actividades planificadas

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

## Dependencia de inicio

Esta historia se habilita únicamente después de J11-S9-07 y J11-S9-08. Crear el
documento de planificación no inicia código ni modifica el baseline de Compras.
