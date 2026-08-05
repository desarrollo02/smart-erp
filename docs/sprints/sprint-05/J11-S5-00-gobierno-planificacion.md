# J11-S5-00 - Gobierno y planificación de fundaciones de plugins

- Estado: Completada documentalmente
- Fecha: 2026-07-29
- Dependencia: `J11-S4-08` técnicamente verde; validación independiente pendiente

## Objetivo

Registrar la autorización de continuidad, acotar Sprint 5 y decidir la arquitectura
antes de modificar Maven, el migrador o los plugins de referencia.

## Decisión de producto

La validación independiente de Sprint 4 se mantiene pendiente. No se degradan a
pendientes las pruebas técnicas que ya finalizaron verdes. Se autoriza avanzar con
fundaciones transversales de plugins sin cerrar Sprint 4 ni promover artefactos.

Los cambios nuevos usan el flujo incremental normal de pruebas. Esta autorización
no crea una nueva acumulación de suites como la utilizada en Sprint 3 y 4.

## Entregables

- ADR-0012 aceptado;
- alcance y cinco historias secuenciadas para Sprint 5;
- gates técnicos, visuales y documentales;
- trazabilidad con ADR-0011 y la épica de plugins productivos;
- condición explícita para iniciar `business_partners` en Sprint 6.

## Criterios de aceptación

- **CA-01:** Sprint 4 conserva su estado real y su validación humana pendiente.
- **CA-02:** la continuidad no autoriza promoción ni producción.
- **CA-03:** WAR y migrador tendrán una sola fuente de composición.
- **CA-04:** las migraciones pertenecen al plugin y usan su esquema derivado.
- **CA-05:** la activación por empresa no decide qué migraciones físicas aplicar.
- **CA-06:** `business_partners` no comienza antes de completar los habilitadores.
- **CA-07:** Sprint 5 termina con demo visual y PDF obligatorio.
- **CA-08:** las pruebas nuevas se ejecutan incrementalmente.

## Validación

Historia documental: se revisaron ADR-0002, ADR-0003, ADR-0011, la épica de roadmap,
el estado de Sprint 4, los descriptores `plugin-api`, la composición del WAR y el
migrador actual. No se ejecutó Maven porque esta historia no modifica código.

## Siguiente paso

`J11-S5-01`: implementar composición única y migraciones de plugins conforme a
ADR-0012.

