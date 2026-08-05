# J11-S8-C03 — Datos de referencia normativos compartidos

- Estado: Implementada y validada; publicación completa y recongelación pendientes
- Fecha: 2026-08-04
- Plugin: `reference_data`
- ADR: [ADR-0038](../../adr/0038-plugin-datos-referencia-normativos.md)
- Épica: [datos de referencia](../../backlog/epica-datos-referencia-normativos.md)
- Evidencia: [J11-S8-C03](../../evidence/J11-S8-C03-datos-referencia-normativos.md)
- Demo: [runbook reproducible](../../runbooks/demo-datos-referencia-j11-s8-c03.md)

## Objetivo

Resolver la propiedad de países y monedas antes de `purchasing`, sin hardcodear
listas en consumidores ni convertir el kernel en maestro. El primer corte debe ser
pequeño, trazable y honesto sobre su completitud.

## Alcance implementado

- API Java pura `reference-data-api` 1.0.0;
- plugin funcional `reference_data` 1.0.0 y esquema privado
  `plg_reference_data` V1;
- publicaciones inmutables con autoridad, URI, fecha observada, SHA-256,
  completitud y cantidad;
- subconjunto `BOOTSTRAP_SUBSET` con `PY`, `PYG` y `USD`;
- políticas de habilitación por empresa, cuyo estado ausente habilita el valor;
- pantalla de sólo lectura `/reference-data` protegida por
  `reference_data.view`;
- país de identificación y moneda de lista como selectores normativos;
- revalidación transaccional en altas de identificación y lista de precios;
- dependencia `REQUIRED reference_data [1.0.0,2.0.0)` desde
  `business_partners` y `commercial_catalog`;
- composición coherente en perfiles de demo, WAR y migrador.

## Criterios de aceptación

- **CA-01:** API y dominio no importan internos de consumidores ni Jakarta en la
  API pública. **Cumplido.**
- **CA-02:** una V1 idempotente crea cinco tablas privadas y conserva procedencia
  verificable. **Cumplido en PostgreSQL 18.4.**
- **CA-03:** la UI no permite altas arbitrarias y muestra que el corte es un
  subconjunto. **Cumplido en contrato, renderer y Playwright responsive.**
- **CA-04:** códigos ausentes o inhabilitados se rechazan nuevamente dentro de la
  transacción del consumidor. **Cumplido.**
- **CA-05:** el perfil sin plugins queda limpio y `with-inventory-demo` incluye
  proveedor, API y migración. **Cumplido.**
- **CA-06:** no existen relaciones JPA, SQL ni imports internos cruzados.
  **Cumplido por arquitectura y revisión.**
- **CA-07:** Docker/Compose, health, OIDC y Playwright responsive quedan verdes
  antes de recongelar Sprint 8. **Cumplido para el corte C03; la recongelación del
  Sprint continúa como gate independiente.**

## Límites explícitos

Este corte no es el catálogo mundial completo, no consulta Internet en runtime,
no implementa tasas de cambio y no permite editar códigos normativos desde el
navegador. El importador completo, reconciliación, historia de retirados,
administración de políticas y paginación/listas grandes permanecen en RD-04 a
RD-06.
