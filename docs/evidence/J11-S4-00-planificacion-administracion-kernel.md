# Evidencia de J11-S4-00 — Planificación de administración del kernel

- Fecha: 2026-07-28
- Estado: documentación creada; pruebas automatizadas pendientes

## Resultado

Se definió Sprint 4 como el incremento que completa la frontera operativa inicial
del kernel. La autoridad global permanece en `core`, separada de Keycloak y de
roles empresariales. El panel administrativo reutilizará casos de uso tipados,
JTA, auditoría y Jakarta Faces responsive.

La decisión quedó registrada en ADR-0009. La épica y el Sprint fijan la secuencia
desde modelo neutral y V4 hasta UI, auditoría y validación acumulada.

## Decisión de calendario

El responsable de producto indicó continuar dejando únicamente las pruebas como
pendientes. Se actualizó `AGENTS.md` con una excepción acotada a `J11-S4-01` a
`J11-S4-07`. Cada historia deberá terminar su implementación y documentación antes
de adoptar `Implementada pendiente de pruebas`.

No se autoriza ignorar fallos, cerrar Sprint 4, declarar terminado el kernel,
promover imágenes o desplegar a producción con gates pendientes.

## Archivos creados o modificados

- `AGENTS.md`;
- `docs/adr/0009-autoridad-administrativa-global-kernel.md`;
- `docs/adr/README.md`;
- `docs/backlog/epica-administracion-operativa-kernel.md`;
- `docs/sprints/sprint-04/README.md`;
- `docs/sprints/sprint-04/J11-S4-00-gobierno-planificacion.md`;
- índices documentales correspondientes.

## Pruebas y verificación

No se ejecutaron Maven, ArchUnit, PostgreSQL, Docker ni Playwright. Todos los gates
automatizados de Sprint 4 permanecen pendientes por decisión de producto. No se
observó ni se ignoró una prueba fallida durante esta tarea.

## Pendiente explícito

Iniciar `J11-S4-01`. Su único pendiente permitido al finalizar será la ejecución de
pruebas; el modelo, contratos y documentación deben quedar completos.
