# Sprint 1 — Fundación técnica verificable

- Estado: Completado
- Fecha de inicio: 2026-07-23
- Fecha de cierre: 2026-07-27
- Duración propuesta: 2 semanas

## Objetivo

Obtener un proyecto Jakarta EE 11 reproducible que arranque en Docker y demuestre que un plugin puede incluirse o excluirse sin modificar el kernel.

## Backlog ordenado

| Historia | Resultado esperado | Estado |
|---|---|---|
| `J11-S1-00` | Gobierno documental | Completado |
| `J11-S1-01` | Baseline y decisiones arquitectónicas | Completado |
| `J11-S1-02` | Esqueleto Maven reproducible | Completado |
| `J11-S1-03` | Docker e infraestructura como código | Completado |
| `J11-S1-04` | Contratos de plugins y validaciones | Completado |
| `J11-S1-05` | Kernel, descubrimiento CDI y plugin de referencia | Completado |
| `J11-S1-06` | Aplicación mínima y endpoints de salud | Completado |
| `J11-S1-07` | Validación integral y cierre del Sprint | Completado |

No se inicia una historia posterior mientras la anterior tenga pruebas relevantes pendientes o fallidas.

## Cierre

- [J11-S1-07 — Validación integral y cierre del Sprint](J11-S1-07-validacion-integral-cierre.md)
- Resultado: 16 de 16 criterios cumplidos; 56 pruebas Maven, 4 reglas ArchUnit y 2 pruebas REST Assured ejecutadas contra cada composición.
- Las variantes con y sin plugin son reproducibles, arrancan saludables y conservan los límites arquitectónicos.
- Migración inicial, idempotencia, rechazo de checksum, persistencia y caída/recuperación de PostgreSQL quedaron verificadas.
- Evidencia: [Validación integral y cierre del Sprint 1](../../evidence/J11-S1-07-validacion-integral-cierre.md).

## Retrospectiva

- La secuencia estricta por historias evitó adelantar capacidades sobre un baseline rojo.
- Las variantes Maven y Compose demostraron que el plugin es físicamente opcional sin modificar el kernel.
- Health semántico permitió distinguir proceso vivo de instancia lista y diagnosticar la base sin divulgar detalles internos.
- La mayor fricción operativa fue restablecer explícitamente JDK, home del Wrapper y Docker Desktop al iniciar una sesión nueva; el runbook debe seguir siendo el punto de entrada.
- La falta de metadata Git impide demostrar un diff o commit del Sprint y permanece como riesgo de gobierno del trabajo.

## Siguiente paso permitido

El [Sprint 2 — Kernel multiempresa y activación persistida](../sprint-02/README.md) quedó propuesto con objetivo, historias ordenadas, dependencias y criterios. No se inicia nueva implementación hasta aceptar ese backlog.
