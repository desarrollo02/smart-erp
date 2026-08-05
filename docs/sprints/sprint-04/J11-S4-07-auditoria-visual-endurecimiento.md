# J11-S4-07 — Auditoría visual y endurecimiento administrativo

- Estado: Completada; validada en `J11-S4-08`
- Sprint: 4
- Fecha: 2026-07-28
- Dependencias: `J11-S4-01` a `J11-S4-06` completadas
- ADR rector: [ADR-0009](../../adr/0009-autoridad-administrativa-global-kernel.md)

## Objetivo

Persistir y consultar desde Jakarta Faces la auditoría técnica del kernel con
paginación y filtros cerrados, sin leer archivos de log, exponer datos sensibles ni
permitir que la UI acceda directamente a JPA. Endurecer además las respuestas de
`/admin/*` con cabeceras defensivas uniformes.

## Decisión de persistencia

ADR-0009 exige auditoría transaccional consultable. Los adaptadores existentes sólo
emiten logs estructurados y no forman una fuente paginable. Esta historia agrega la
migración aditiva `core` V5 con `core.audit_event`:

- almacén técnico append-only propiedad del kernel;
- un UUID propio por evento, categoría, operación, resultado e instante UTC;
- IDs locales opcionales de actor, sujeto, empresa, rol, plugin, permiso y pantalla;
- código estable, versiones y correlación cuando correspondan;
- sin relaciones JPA ni claves foráneas hacia recursos mutables, para conservar
  historia aunque el estado actual cambie;
- sin issuer, subject OIDC, nombre visible, token, cookie, contraseña, claim, SQL,
  stacktrace ni dato comercial;
- índices por orden temporal, categoría/resultado, empresa, actor y correlación.

Los eventos anteriores a V5 permanecen únicamente en los logs existentes; no se
inventará un backfill. No se implementa borrado, exportación o política de retención
en esta historia. Una futura purga necesitará autorización, respaldo y decisión
documentada.

Las auditorías de una mutación participan en su misma transacción: si no pueden
persistirse, la mutación falla y revierte. Las decisiones de acceso obtienen una
transacción corta propia para conservar tanto permisos concedidos como denegaciones.
Los logs estructurados se mantienen como salida operativa adicional, no como fuente
de la pantalla.

## Consulta administrativa

- Ruta: `/admin/audit.xhtml`.
- Permiso exacto: `AUDIT_VIEW` en filtro, bean y cada nueva petición.
- Página fija de 25 registros y máximo técnico de 50 en el contrato neutral.
- Orden: `occurredAt DESC, auditEventId DESC`.
- Filtros cerrados: categoría, resultado, ventana temporal, empresa técnica exacta
  y correlación exacta.
- Ventanas disponibles: últimas 24 horas, 7 días, 30 días o todo lo persistido.
- La navegación anterior/siguiente conserva filtros validados y no acepta JPQL,
  nombres de columnas ni orden arbitrario desde el navegador.
- La vista muestra sólo identificadores técnicos y códigos estables.

## Endurecimiento

Toda respuesta permitida o denegada de `/admin/*` agrega:

- `Cache-Control: no-store` y `Pragma: no-cache`;
- `X-Content-Type-Options: nosniff`;
- `X-Frame-Options: DENY` y `frame-ancestors 'none'`;
- `Referrer-Policy: no-referrer`;
- CSP limitada a recursos del propio origen, formularios propios y el JavaScript
  inline requerido por las confirmaciones Faces actuales.

Jakarta Faces conserva su estado de vista de servidor para formularios POST. Estas
cabeceras no sustituyen autorización, validación de comandos ni pruebas negativas.

## Criterios de aceptación

- **CA-01:** V5 es aditiva, V1–V4 permanecen inmutables y readiness exige V5.
- **CA-02:** una mutación y su auditoría persistente confirman o revierten juntas.
- **CA-03:** decisiones de acceso permitidas y denegadas quedan persistidas.
- **CA-04:** los cinco tipos existentes de auditoría se normalizan sin perder sus
  identificadores técnicos aplicables.
- **CA-05:** no se persisten secretos, identidad externa ni datos comerciales.
- **CA-06:** `AUDIT_VIEW` es obligatorio para abrir o volver a consultar la ruta.
- **CA-07:** filtros desconocidos, páginas negativas, UUID inválidos y correlaciones
  mal formadas fallan cerrados con mensaje genérico.
- **CA-08:** tamaño de página y orden no son controlables libremente por el cliente.
- **CA-09:** la proyección y el puerto de consulta son Java puro.
- **CA-10:** el bean no importa JPA, infraestructura ni entidades.
- **CA-11:** la pantalla identifica que sólo contiene eventos producidos desde V5.
- **CA-12:** la navegación preserva filtros sin serializar autoridad en sesión.
- **CA-13:** las respuestas administrativas permitidas y denegadas incluyen las
  cabeceras de endurecimiento.
- **CA-14:** la UI usa Material Design 3 y es utilizable a 375, 720 y 1280 px.
- **CA-15:** no existe borrado, edición, exportación ni REST de auditoría.

## Documentación afectada

- esta historia y evidencia técnica;
- índice de Sprint 4 y backlog;
- arquitectura de persistencia y estrategia de pruebas;
- guía de implementación y validación independiente;
- runbooks de migración, Compose y shell.

## Matriz de pruebas ejecutada en J11-S4-08

Por la excepción aprobada, se acumularon y ejecutaron en `J11-S4-08`:

- unitarias de validación de consulta, paginación, filtros y mapeos;
- PostgreSQL V1→V5, índices, checks, orden y ausencia de backfill;
- JPA `validate` y consultas paginadas;
- JTA de commit/rollback conjunto y persistencia de accesos denegados;
- autorización positiva/negativa y manipulación de filtros;
- cabeceras en rutas permitidas, denegadas y variante `/faces/admin/*`;
- ArchUnit de puerto neutral y bean web;
- Playwright a 375/720/1280 px, teclado, foco y regresión A/B.

El gate acumulado terminó verde sin fallos ni omisiones. La historia está
completada; el baseline todavía no está promovido ni apto para producción.

## Resultado implementado

- V5 agrega `core.audit_event`, índices de consulta y protección append-only ante
  `UPDATE` o `DELETE`.
- Los cinco puertos de auditoría conservan su log estructurado y persisten un sobre
  técnico común mediante `JpaTechnicalAuditStore`.
- `AuditQueryPort` expone filtros y paginación neutrales; `JpaAuditQueryAdapter`
  construye exclusivamente predicados controlados.
- `/admin/audit.xhtml` muestra los eventos desde V5 y conserva filtros entre páginas.
- `AdminAuthorizationFilter` exige `AUDIT_VIEW` y aplica cabeceras defensivas a
  respuestas administrativas permitidas y denegadas.

La compilación de aplicación, infraestructura y web, el test-compile y el
empaquetado de los doce módulos terminaron correctamente con pruebas omitidas. La
evidencia reproducible se encuentra en
[J11-S4-07](../../evidence/J11-S4-07-auditoria-visual-endurecimiento.md).
