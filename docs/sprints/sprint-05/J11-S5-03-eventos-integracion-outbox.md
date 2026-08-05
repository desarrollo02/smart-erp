# J11-S5-03 — Contrato de eventos de integración y outbox

- Estado: Completada documentalmente
- Fecha: 2026-07-29
- Dependencias: `J11-S5-01` y `J11-S5-02` completadas
- ADR relacionados: [ADR-0002](../../adr/0002-arquitectura-plugins.md),
  [ADR-0003](../../adr/0003-persistencia-migraciones.md) y
  [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)

## Objetivo

Definir antes de los plugins productivos cuándo corresponde usar comunicación
síncrona o eventos, quién posee cada contrato y cómo debe implementarse un outbox
recuperable. La historia debe evitar dos extremos: enviar antes del commit y perder
eventos, o construir ahora un bus genérico sin productor, consumidor ni payload
real que lo justifiquen.

## Pregunta de decisión

¿Debe Sprint 5 introducir tablas, dispatcher y transporte de outbox antes de
`business_partners`, o basta con fijar el contrato operativo obligatorio y
materializarlo junto al primer intercambio asíncrono concreto?

## Criterios de aceptación

- **CA-01:** distinguir evento interno de dominio, evento público de integración y
  auditoría; ninguno sustituye a los otros.
- **CA-02:** la respuesta inmediata usa un contrato síncrono público; la
  propagación desacoplada usa un evento pasado e inmutable.
- **CA-03:** el productor es dueño del tipo y payload público en su `<plugin>-api`.
- **CA-04:** cada evento empresarial identifica evento, tipo, versión, productor,
  empresa, sujeto, versión del sujeto, instante, correlación y payload.
- **CA-05:** el estado empresarial y el registro outbox se confirman en la misma
  transacción local/JTA.
- **CA-06:** la entrega se declara `at-least-once`; consumidores deduplican por
  `event_id` y no se promete exactamente una vez.
- **CA-07:** orden, reintentos, backoff, cuarentena, replay y observabilidad tienen
  propietario y reglas explícitas antes del primer uso.
- **CA-08:** la tabla outbox pertenece al esquema del productor; la deduplicación o
  inbox pertenece al consumidor; `core` no almacena payloads empresariales.
- **CA-09:** desactivar o retirar un plugin no borra outbox, inbox, historial ni
  datos, y no produce ACK silencioso de mensajes pendientes.
- **CA-10:** payloads y logs aplican minimización; no contienen secretos, tokens,
  contraseñas ni datos personales innecesarios.
- **CA-11:** no se elige broker, librería o framework sin un caso de uso y un ADR
  que justifique versión, licencia y operación.
- **CA-12:** la decisión indica expresamente si modifica código o difiere la
  infraestructura, y qué condición habilita esa implementación.
- **CA-13:** guía, arquitectura, roadmap y evidencia quedan sincronizados.

## Límites

- no crear eventos de `business_partners` antes de caracterizar sus casos de uso;
- no definir payloads genéricos tipo mapa como contrato de negocio;
- no reutilizar `core.audit_event` como cola;
- no crear una tabla `core.outbox` con datos privados de todos los plugins;
- no usar listeners en memoria como garantía de entrega;
- no prometer orden global, entrega exactamente una vez ni transacciones
  distribuidas;
- no incorporar Kafka, JMS u otro transporte en esta historia sin necesidad
  demostrada.

## Validación prevista

Esta historia puede cerrarse documentalmente si la decisión concluye que todavía
no existe un intercambio asíncrono que permita diseñar un payload y una
suscripción reales. En ese caso se validarán trazabilidad, enlaces, UTF-8 y
coherencia con las reglas de plugins/datos; no se simularán pruebas de runtime.

Si la decisión introduce clases, tablas o dependencias, debe ejecutar las pruebas
incrementales correspondientes, PostgreSQL/Testcontainers, ArchUnit y
`mvn verify` antes del cierre.

## Decisión obtenida

[ADR-0013](../../adr/0013-eventos-integracion-outbox-por-plugin.md) fija desde
ahora el contrato operativo, pero difiere clases, migraciones, dispatcher y
transporte hasta el primer intercambio asíncrono real. No existe todavía un
productor, consumidor o payload empresarial que permita probar una implementación
sin inventar comportamiento.

El [contrato operativo](../../architecture/integration-events-outbox.md) define el
sobre mínimo, propiedad por plugin, escritura atómica, entrega `at-least-once`,
deduplicación/inbox, orden por sujeto, activación, bootstrap, reintentos,
cuarentena, replay, observabilidad y seguridad. La primera historia que publique
un evento debe materializar y probar estas reglas dentro de los esquemas de sus
plugins.

`business_partners` puede comenzar sin outbox: usará contratos síncronos sólo
cuando sus casos de uso necesiten respuesta inmediata y no publicará eventos
preventivos. La [evidencia](../../evidence/J11-S5-03-eventos-integracion-outbox.md)
registra la revisión y el alcance exacto.
