# J11-S4-00 — Gobierno y planificación de la administración del kernel

- Estado: Completada documentalmente
- Fecha: 2026-07-28
- Dependencia: candidata visual de Sprint 3 disponible; cierre G7 pendiente

## Objetivo

Definir la autoridad administrativa global, el alcance de Sprint 4, la secuencia de
historias y los gates antes de modificar código o el esquema `core`.

## Decisión de producto

El responsable de producto autorizó continuar y pidió que únicamente las pruebas
queden pendientes. Por tanto, las historias `J11-S4-01` a `J11-S4-07` podrán
completar implementación y documentación con estado
`Implementada pendiente de pruebas`; `J11-S4-08` ejecutará la matriz acumulada.

Esta decisión no permite ignorar una prueba fallida, cerrar el Sprint, promover una
imagen o desplegar a producción.

## Entregables

- ADR-0009 aceptado;
- épica de administración operativa del kernel;
- Sprint 4 con nueve historias secuenciadas;
- criterios globales y gates G0-G7;
- actualización de la metodología en `AGENTS.md`;
- evidencia documental de planificación.

## Criterios de aceptación

- **CA-01:** autoridad global se separa de roles empresariales y de Keycloak.
- **CA-02:** primer administrador usa bootstrap cerrado e idempotente.
- **CA-03:** se prohíbe revocar al último administrador efectivo.
- **CA-04:** `/admin/*` exige OIDC y permiso global en cada operación.
- **CA-05:** alcance incluye empresas, plugins, personalización, seguridad y auditoría.
- **CA-06:** UI conserva JSF, Material Design 3 y responsive obligatorio.
- **CA-07:** no se introduce carga dinámica de JAR ni API pública improvisada.
- **CA-08:** V4 se define aditiva y V1-V3 inmutables.
- **CA-09:** historias y gates quedan ordenados antes del código.
- **CA-10:** solo las pruebas pueden quedar pendientes en historias implementadas.
- **CA-11:** una prueba fallida sigue siendo bloqueo.
- **CA-12:** Sprint 3 no se declara cerrado por esta continuidad.

## Pruebas

No se ejecutaron builds ni suites automatizadas en esta historia documental. La
validación automatizada de enlaces, arquitectura y código queda pendiente conforme
a la decisión de producto. La inspección realizada se limitó a coherencia de
alcance y trazabilidad entre ADR, épica y Sprint.

## Siguiente paso

`J11-S4-01`: crear contratos y modelo neutral de roles/permisos globales y la
protección conceptual del último administrador, sin dependencias Jakarta.
