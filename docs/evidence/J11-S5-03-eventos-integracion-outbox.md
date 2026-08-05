# Evidencia de J11-S5-03 — Eventos de integración y outbox

- Fecha: 2026-07-29
- Estado: verde documental
- Historia: [J11-S5-03](../sprints/sprint-05/J11-S5-03-eventos-integracion-outbox.md)

## Fuentes revisadas

- ADR-0002: contratos públicos, puertos y eventos entre plugins;
- ADR-0003: propiedad exclusiva de esquemas y migraciones;
- ADR-0011 y épica del roadmap: intercambios futuros de inventario, logística,
  documentos, tesorería y contabilidad;
- módulos `kernel-api`, `plugin-api` y `kernel-application` actuales;
- migraciones y mecanismo de composición cerrados en J11-S5-01;
- plantilla neutral cerrada en J11-S5-02.

La revisión confirmó que no existe un productor, consumidor, hecho empresarial ni
payload de integración aprobado. Los objetos llamados “event” en la aplicación
actual son auditoría o logging técnico; no constituyen un bus empresarial.

## Decisión

Se aceptó [ADR-0013](../adr/0013-eventos-integracion-outbox-por-plugin.md):

- el productor posee contrato y outbox en su plugin;
- el consumidor posee inbox/deduplicación y efectos en su plugin;
- estado + outbox y efecto + inbox se confirman atómicamente;
- entrega `at-least-once`, idempotencia por `event_id` y sin orden global;
- `core.audit_event` no es una cola y `core` no almacena payload empresarial;
- activación, ausencia, bootstrap, replay y recuperación deben definirse por
  suscripción;
- no se elige transporte ni se agrega infraestructura hasta el primer evento real.

## Por qué no hubo código ni migración

Una interfaz genérica sin payload real no prueba compatibilidad semántica. Una
tabla o dispatcher sin volumen, latencia, retención y transporte sería una
dependencia especulativa. La historia cumplió el habilitador mediante un contrato
operativo verificable y una condición de materialización; afirmar pruebas runtime
habría sido engañoso.

El primer intercambio asíncrono deberá ejecutar unitarias, ArchUnit,
PostgreSQL/Testcontainers, commit/rollback, duplicados, concurrencia, reinicio,
incompatibilidad, activación y recuperación. Hasta entonces, el primer plugin no
genera eventos preventivos.

## Validación

- Markdown y referencias locales revisados desde la raíz;
- cero enlaces locales faltantes;
- cero caracteres de reemplazo U+FFFD;
- no se agregaron dependencias, secretos, tablas ni artefactos runtime;
- la guía para implementadores avanzó a `1.0-rc26`;
- Sprint 5 conserva pendientes su demo visual y PDF de cierre en `J11-S5-04`.
