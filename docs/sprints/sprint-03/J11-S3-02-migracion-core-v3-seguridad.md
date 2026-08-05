# J11-S3-02 — Migración `core` V3 de seguridad

- Estado: Completada
- Dependencia: `J11-S3-01` implementada para la candidata
- Evidencia: [migración `core` V3 de seguridad](../../evidence/J11-S3-02-migracion-core-v3-seguridad.md)

## Objetivo

Evolucionar PostgreSQL con estructuras aditivas y restricciones que representen usuarios externos, membresías, roles y permisos sin modificar las migraciones V1 y V2.

## Alcance

- migración Flyway V3 inmutable en el esquema `core`;
- tablas `app_user`, `company_membership`, `security_role`, `role_permission` y `membership_role`;
- UUID, issuer/subject únicos, estados, versiones optimistas y marcas UTC;
- claves y restricciones que impidan relaciones de rol entre empresas distintas;
- índices para resolución de identidad, membresías y permisos;
- compatibilidad de rollback de aplicación sin borrar V3;
- actualización del runbook de migración y respaldo.

## Fuera de alcance

- seeds con empresas, personas o permisos reales;
- tablas de Keycloak dentro de `core`;
- passwords, tokens, sesiones o claims completos;
- migraciones privadas de plugins;
- edición o `repair` de V1/V2.

## Criterios de aceptación

- **CA-01:** V1 y V2 conservan exactamente su identidad y checksum.
- **CA-02:** V3 es aditiva y puede aplicarse tanto en base vacía como después de V2.
- **CA-03:** `(issuer, subject)` es único y no depende de email o username.
- **CA-04:** una membresía es única por usuario y empresa.
- **CA-05:** una asignación de rol no puede cruzar empresas.
- **CA-06:** filas de permisos almacenan identificadores públicos y no crean FK hacia plugins físicos.
- **CA-07:** estados y versiones tienen defaults seguros y restricciones explícitas.
- **CA-08:** timestamps usan zona horaria/UTC coherente con el baseline.
- **CA-09:** índices cubren búsquedas por identidad externa, usuario y empresa.
- **CA-10:** no existe DDL automático de Hibernate.
- **CA-11:** migración reejecutada no produce cambios ni altera checksums.
- **CA-12:** rollback de la aplicación conserva tablas y datos V3.
- **CA-13:** el runbook explica respaldo, actualización, verificación y recuperación.
- **CA-14:** la matriz PostgreSQL prevista queda diferida y trazada a `J11-S3-08`.

## Gates

- G1: recurso SQL validado estructuralmente y candidata empaquetable.
- G3 diferido: Testcontainers y Compose prueban V1→V2→V3, base vacía, restricciones y reejecución en `J11-S3-08`.
- G0 documental inmediato.

## Estado provisional aplicado

Se usó `Implementada pendiente de validación` hasta completar G3.

## Resultado provisional

Se agregó `V3__add_identity_membership_and_authorization.sql` con cinco tablas, cinco índices y 29 restricciones nombradas:

- `app_user`, con identidad única `(issuer, subject)` y sin credenciales;
- `company_membership`, única por usuario/empresa;
- `security_role`, único por ID y por código dentro de una empresa;
- `role_permission`, con `ContributionId` textual y sin FK hacia plugins;
- `membership_role`, con FKs compuestas que obligan a que membresía y rol pertenezcan a la misma empresa.

V1 y V2 conservaron sus SHA-256 anteriores. El migrador empaquetó tres recursos y el reactor completo quedó compilable con pruebas omitidas. No se aplicó todavía V3 sobre PostgreSQL ni se calculó su checksum Flyway runtime; por ello la historia no está completada.

## Validación acumulada

`J11-S3-08` comprobó base vacía, V1→V2→V3, reejecución, checksums, restricciones y
concurrencia sobre PostgreSQL 18.4. G3 quedó verde. Evidencia:
[gates G2–G6](../../evidence/J11-S3-08-validacion-demo-cierre.md).

## Siguiente paso

La historia está completada. El Sprint continúa con G7 de `J11-S3-08`.
