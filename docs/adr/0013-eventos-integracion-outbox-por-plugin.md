# ADR-0013 — Eventos de integración y outbox por plugin

- Estado: Aceptado
- Fecha: 2026-07-29
- Historia: `J11-S5-03`

## Contexto

ADR-0002 permite comunicación entre plugins mediante contratos públicos, puertos
o eventos y reserva estos últimos para propagación desacoplada. ADR-0011 exige
decidir el outbox antes del primer intercambio asíncrono. Todavía no existe un
plugin productivo, por lo que no hay productor, consumidor, hecho empresarial,
payload, volumen ni requisito de latencia concretos.

Crear ahora una tabla global, un dispatcher o elegir JMS/Kafka obligaría a adivinar
esas propiedades. No hacerlo sin reglas previas también sería riesgoso: un plugin
podría enviar antes del commit, perder eventos, duplicar efectos o guardar payloads
privados en `core`.

## Decisión

### 1. Elegir síncrono o asíncrono por necesidad

- Una respuesta necesaria para completar la operación actual usa un puerto
  síncrono definido en el `<plugin>-api` propietario.
- La propagación de un hecho ya confirmado, sin bloquear al productor por el
  trabajo del consumidor, usa un evento público de integración.
- Un evento interno de dominio no cruza la implementación del plugin.
- Un evento de auditoría explica quién intentó qué y con qué resultado; no se usa
  como cola ni como contrato empresarial.

### 2. Propiedad y contrato público

El productor posee el nombre, versión, semántica y payload tipado del evento en su
`<plugin>-api`. El consumidor depende de ese contrato público o de una proyección
explícita, nunca de clases de implementación ni de la tabla outbox.

Todo evento empresarial de integración incluye como mínimo:

- `event_id`: UUID global e inmutable;
- `event_type`: código estable `<plugin>.<subject>.<past_fact>`;
- `event_version`: SemVer del contrato público;
- `producer_plugin_id`;
- `company_id` canónico y obligatorio;
- `subject_type`, `subject_id` opaco y `subject_version` monotónica;
- `occurred_at` como instante UTC;
- `correlation_id` generado o propagado por el servidor;
- `causation_event_id` opcional;
- `payload` tipado, versionado y mínimo.

No se publican entidades JPA, nombres de clases internas, SQL, credenciales,
tokens, contraseñas ni datos personales innecesarios. Un evento global que no
pertenezca a una empresa requerirá otro contrato explícito; no se representará con
un `company_id` nulo.

### 3. Outbox e inbox pertenecen a los plugins

Cuando exista el primer evento real, el productor agregará una tabla outbox
versionada en `plg_<producer>`. La mutación empresarial y el registro del evento se
confirman en la misma transacción local/JTA. Si no puede persistirse el outbox, la
mutación se revierte.

El consumidor que produzca efectos persistentes conserva su deduplicación o inbox
en su propio esquema y confirma el `event_id` junto con el efecto. `core` no tendrá
una tabla global con payloads empresariales y `core.audit_event` no se reutiliza.

### 4. Semántica de entrega y orden

- La garantía es **at-least-once**. Un fallo después de publicar y antes de marcar
  puede producir un duplicado.
- Cada consumidor debe ser idempotente por `event_id`; “exactly once” no se
  promete.
- No existe orden global. Cuando el caso lo necesite, se conserva orden por
  `(producer_plugin_id, subject_type, subject_id)` usando `subject_version`.
- Un hueco, duplicado o versión incompatible no se ignora: se reintenta, se
  reconstruye desde un contrato público o pasa a cuarentena según la suscripción.
- No se usan transacciones distribuidas entre base y transporte.

### 5. Activación, retirada y recuperación

Una operación productora sólo se ejecuta con el plugin activo para la empresa. Un
consumidor inactivo no ejecuta su efecto. Cada suscripción debe elegir y probar
antes del despliegue una de estas políticas:

- retener y aplicar al reactivarse; o
- omitir durante la inactividad únicamente si existe una reconstrucción completa
  mediante contrato público.

Nunca se confirma silenciosamente un evento sin que la política lo autorice. Un
plugin que se agrega después debe hacer bootstrap desde una proyección pública
antes de consumir hechos nuevos; no presupone retención infinita del productor.
Desactivar o retirar un JAR no borra outbox, inbox, historial ni datos.

Reintentos usan backoff acotado. Tras el máximo aprobado, el registro pasa a
cuarentena recuperable con código técnico, sin payload en logs. Requeue, descarte
excepcional y replay requieren autorización y auditoría. Deben exponerse métricas
de pendientes, antigüedad, intentos y cuarentena.

### 6. Implementación diferida con condición concreta

Sprint 5 no agrega todavía clases Java, tablas, dispatcher ni transporte. El
primer evento asíncrono debe abrir una historia que identifique:

1. productor, consumidor y plugins activos involucrados;
2. hecho pasado, payload público y rango de versiones;
3. transacción que escribe estado + outbox;
4. volumen, latencia, retención y orden requeridos;
5. política ante inactividad/ausencia y bootstrap;
6. reintentos, cuarentena, replay, métricas y autorización operativa;
7. transporte elegido, versión, licencia y estrategia de promoción;
8. pruebas de commit/rollback, duplicados, reinicio, concurrencia, incompatibilidad
   y recuperación.

Hasta entonces, `business_partners` puede comenzar con contratos síncronos mínimos
para las respuestas inmediatas que sus casos de uso demuestren. No publicará
eventos “por si acaso”.

## Consecuencias

### Positivas

- se evita perder eventos por enviarlos antes del commit;
- cada payload y dato operativo conserva propietario;
- los consumidores asumen explícitamente duplicados y recuperación;
- el kernel no se convierte en almacén de datos empresariales;
- no se incorpora infraestructura sin conocer sus requisitos.

### Costes y riesgos

- el primer intercambio asíncrono deberá implementar y probar outbox/inbox antes
  de entregar esa capacidad;
- la entrega al menos una vez exige idempotencia real en cada consumidor;
- una política de inactividad mal elegida puede requerir bootstrap costoso;
- cambios incompatibles de payload requieren nueva versión y convivencia.

## Alternativas descartadas

### Publicación en memoria después del caso de uso

Se descarta porque una caída entre el commit y el envío pierde el hecho, y publicar
antes del commit puede difundir una operación luego revertida.

### Tabla `core.outbox` para todos los plugins

Se descarta porque mezcla payloads empresariales de distintos propietarios y
convierte al kernel en dueño accidental de integración de negocio.

### Prometer entrega exactamente una vez

Se descarta porque no elimina las ventanas de duplicación entre base y transporte.
La solución verificable es entrega al menos una vez más consumidores idempotentes.

### Elegir ahora un broker

Se descarta porque todavía no existen volumen, latencia, topología ni operación que
justifiquen dependencia, licencia y coste.

## Referencias

- [ADR-0002 — Arquitectura de plugins](0002-arquitectura-plugins.md)
- [ADR-0003 — Persistencia y migraciones](0003-persistencia-migraciones.md)
- [ADR-0011 — Roadmap de plugins productivos](0011-roadmap-dependencias-plugins-productivos.md)
- [Contrato operativo de eventos y outbox](../architecture/integration-events-outbox.md)
