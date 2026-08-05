# J11-S4-02 — Migración `core` V4 y bootstrap global cerrado

- Estado: Completada; validada en `J11-S4-08`
- Fecha: 2026-07-28
- Dependencia: `J11-S4-01` completada
- ADR rector: [ADR-0009](../../adr/0009-autoridad-administrativa-global-kernel.md)

## Objetivo

Agregar de forma aditiva las estructuras PostgreSQL de autoridad global y preparar
un bootstrap one-shot, idempotente y cerrado del primer administrador, sin mezclar
roles empresariales, abrir endpoints ni anticipar los adaptadores JPA/JTA de
`J11-S4-03`.

## Alcance

- migración inmutable `V4__add_system_authority.sql`;
- roles globales versionados sin `CompanyId`;
- asignaciones entre `core.app_user` y roles globales;
- concesiones de `SystemPermission` a roles globales;
- restricciones, claves e índices PostgreSQL;
- actualización de readiness para exigir V4;
- comando, estado, repositorio, generador de IDs y puerto neutrales del bootstrap;
- servicio de aplicación idempotente y fail-closed;
- auditoría técnica específica del bootstrap global;
- declaración externa opt-in, deshabilitada por defecto;
- variables Compose y documentación operativa.

## Fuera de alcance

- entidades o repositorios JPA de V4;
- límite JTA y casos de uso administrativos ordinarios;
- habilitar el bootstrap en la composición de referencia;
- endpoints REST, rutas `/admin/*`, beans Faces o pantallas;
- ejecutar pruebas automatizadas en este corte.

`J11-S4-03` implementará el adaptador persistente y transaccional del puerto. Hasta
entonces, habilitar la declaración externa debe fallar explícitamente: nunca se
degrada a SQL, primer login o autoridad de Keycloak.

## Diseño de V4

La migración crea exclusivamente:

- `core.system_role`: identidad UUID, código global único, nombre, estado, versión
  y marcas UTC;
- `core.system_role_permission`: concesión tipada por rol y permiso;
- `core.app_user_system_role`: asignación entre usuario local y rol global.

Las FKs usan `ON DELETE RESTRICT`. Estados, versiones, códigos, permisos, nombres y
timestamps tienen restricciones explícitas. V1, V2 y V3 no se modifican.

## Diseño del bootstrap

La declaración externa identifica exactamente `(issuer, subject)`, nombre opcional,
código/nombre del rol global y permisos. Debe incluir
`kernel.system_administration.manage`; no contiene empresa, password, token ni
claim de roles.

El servicio:

1. valida que la declaración pueda producir un administrador efectivo;
2. crea usuario activo solo si la identidad externa no existe;
3. crea rol global activo solo si el código no existe;
4. crea asignación y concesiones declaradas;
5. al repetirse, devuelve `UNCHANGED` únicamente si el estado existente es compatible;
6. rechaza colisiones, inactividad, asignación ausente o concesiones incompletas;
7. registra auditoría con IDs técnicos, sin issuer, subject o datos sensibles.

Toda escritura deberá quedar bajo una única transacción cuando `J11-S4-03`
materialice el adaptador.

## Criterios de aceptación

- **CA-01:** existe una única V4 aditiva y V1–V3 permanecen byte a byte intactas.
- **CA-02:** las tres tablas pertenecen a `core` y no contienen `company_id`.
- **CA-03:** PK, FK, unicidad, estados, versiones, códigos y timestamps quedan
  restringidos en PostgreSQL.
- **CA-04:** permisos usan el formato canónico de `SystemPermission`.
- **CA-05:** readiness requiere V4 sin ejecutar migraciones desde la aplicación.
- **CA-06:** comando y puertos son Java puro y no dependen de Jakarta/JPA.
- **CA-07:** bootstrap exige el permiso de administración global y falla cerrado.
- **CA-08:** repetición exacta es idempotente; estado incompatible es rechazado.
- **CA-09:** no existe endpoint, ruta JSF ni concesión al primer login.
- **CA-10:** la configuración externa permanece `false` por defecto.
- **CA-11:** antes de `J11-S4-03`, habilitarla sin adaptador falla explícitamente.
- **CA-12:** logs no contienen issuer, subject, tokens, secretos ni nombres libres.
- **CA-13:** documentación, Compose, arquitectura y guía se actualizan en el cambio.

## Matriz de pruebas ejecutada en J11-S4-08

- recurso V4, checksums inmutables V1–V3 y ausencia de `plg_*`;
- PostgreSQL limpio y actualizaciones V1/V2/V3→V4;
- restricciones, FKs, unicidad, índices y reejecución Flyway;
- readiness 503 antes de V4 y 200 después de V4;
- declaración deshabilitada, inválida y habilitada sin adaptador;
- bootstrap nuevo, repetido e incompatible;
- ausencia del permiso administrador y referencias inconsistentes;
- auditoría sin identidad externa ni secretos;
- ArchUnit de módulos puros;
- compilación del reactor y `mvn verify` acumulado.

La matriz se ejecutó en `J11-S4-08`: migraciones 11/11, JPA/PostgreSQL 12/12,
bootstrap runtime y reactor completo quedaron verdes.

## Resultado

V4, readiness 4, contratos neutrales, servicio idempotente, auditoría estructurada,
generador UUID y declaración externa cerrada quedaron implementados. Compose los
expone con `LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_ENABLED=false`; habilitar antes de
`J11-S4-03` falla por adaptador ausente.

Se ejecutaron dos compilaciones principales con
`-Dmaven.test.skip=true`: los siete módulos hasta infraestructura y los cinco hasta
aplicación terminaron `BUILD SUCCESS`. No se ejecutaron ni compilaron tests. La
primera invocación de compilación usó una propiedad PowerShell sin comillas y Maven
la rechazó antes del reactor; se corrigió la sintaxis y no representa un fallo del
código.

La evidencia específica está en
[J11-S4-02](../../evidence/J11-S4-02-migracion-core-v4-bootstrap-global.md) y el
resultado acumulado en
[J11-S4-08](../../evidence/J11-S4-08-validacion-demo-cierre.md). La historia cumple
sus gates técnicos y Definition of Done.

## Siguiente paso condicionado

La historia está completada. La continuidad de Sprint 4 permanece detenida en el
recorrido independiente de `J11-S4-08`.
