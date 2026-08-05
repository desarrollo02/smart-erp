# Evidencia J11-S3-02 — Migración `core` V3 de seguridad

- Fecha: 2026-07-28
- Estado: Completada; validación acumulada en [J11-S3-08](J11-S3-08-validacion-demo-cierre.md)
- Tipo de entrega: migración aditiva y documentación operativa
- Historia: [J11-S3-02](../sprints/sprint-03/J11-S3-02-migracion-core-v3-seguridad.md)

## Resultado

Se agregó la migración inmutable `V3__add_identity_membership_and_authorization.sql`. La candidata introduce cinco tablas, cinco índices explícitos y 29 restricciones nombradas sin editar V1 ni V2:

| Tabla | Responsabilidad | Aislamiento principal |
|---|---|---|
| `core.app_user` | identidad local enlazada a OIDC | unicidad de `(issuer, subject)` |
| `core.company_membership` | pertenencia de un usuario a una empresa | PK `(app_user_id, company_id)` |
| `core.security_role` | rol administrado por una empresa | unicidad de código dentro de `company_id` |
| `core.role_permission` | permisos públicos otorgados a un rol | FK compuesta al rol de la misma empresa |
| `core.membership_role` | asignación de roles a membresías | FKs compuestas a membresía y rol de la misma empresa |

La migración no contiene usuarios semilla, contraseñas, tokens, datos de Keycloak ni claves foráneas hacia tablas de plugins.

## Inmutabilidad de migraciones anteriores

Los SHA-256 calculados directamente sobre los recursos fuente son:

| Recurso | SHA-256 |
|---|---|
| `V1__initialize_core_schema.sql` | `07A375F06F9EBB9D6E6EC162E113ADA35397348BFCD03486870FAF28CC424DA6` |
| `V2__add_companies_and_plugin_activation.sql` | `F5186A3817F7A31569C58551A9339911B29B44F7409E47AE470FC999AFA5CC11` |
| `V3__add_identity_membership_and_authorization.sql` | `6C34C64C0739F4988287C7B9DBA5A0DFF2808C976B30A0B2C066F382F7961170` |

V1 y V2 conservan los checksums de fuente documentados antes de esta historia. El checksum que Flyway registre para V3 queda pendiente de obtener sobre PostgreSQL en `J11-S3-08`.

## Auditoría estática del SQL

La inspección del recurso V3 produjo:

```text
CREATE_TABLES=5
CREATE_INDEXES=5
CONSTRAINTS=29
DUPLICATE_CONSTRAINTS=0
LONG_CONSTRAINTS=0
SEMICOLONS=10
FORBIDDEN_TERMS=0
```

Los términos sensibles o indebidamente acoplados buscados fueron `password`, `access_token`, `refresh_token`, `keycloak` y `plg_`. No aparecieron en el recurso.

Esta auditoría confirma únicamente propiedades estáticas. Todavía no demuestra que PostgreSQL acepte la migración ni que sus restricciones reaccionen correctamente ante datos inválidos.

## Empaquetado de la candidata

Se usaron JDK 21 y los directorios locales de Wrapper y repositorio Maven bajo `.tools/`, con pruebas omitidas según la excepción temporal del Sprint 3.

### Migrador y dependencias

```powershell
mvnw.cmd -B -DskipTests -pl migrator -am package
```

Resultado observado:

- 3 de 3 módulos construidos correctamente;
- 3 recursos de migración incorporados;
- pruebas omitidas;
- tiempo Maven: 3.945 s.

### Reactor completo

```powershell
mvnw.cmd -B -DskipTests package
```

Resultado observado:

- 16 de 16 módulos construidos correctamente;
- WAR ensamblado;
- pruebas omitidas;
- tiempo Maven: 7.047 s.

### Artefacto del migrador

El JAR ejecutable contiene:

```text
db/migration/core/V1__initialize_core_schema.sql
db/migration/core/V2__add_companies_and_plugin_activation.sql
db/migration/core/V3__add_identity_membership_and_authorization.sql
```

| Propiedad | Valor |
|---|---|
| Artefacto | `migrator/target/migrator-0.1.0-SNAPSHOT-executable.jar` |
| Tamaño | 4,893,962 bytes |
| SHA-256 | `009CAA98DA98D78274271373F841734B0A20533C8B36A9499B70A10CE2DCF741` |

El empaquetado correcto no equivale a una validación de migración o una ejecución de pruebas.

## Cobertura provisional de criterios

| Criterio | Evidencia actual | Estado |
|---|---|---|
| CA-01 | SHA-256 de V1 y V2 conservados | Cubierto estáticamente |
| CA-02 | V3 es un recurso nuevo y aditivo | PostgreSQL pendiente |
| CA-03 | `app_user_external_identity_uk` sobre `(issuer, subject)` | Cubierto estáticamente |
| CA-04 | PK compuesta de `company_membership` | Cubierto estáticamente |
| CA-05 | FKs compuestas de `membership_role` | Cubierto estáticamente |
| CA-06 | `permission_id` es identificador público sin FK a plugins | Cubierto estáticamente |
| CA-07 | checks de estado, versión y formato; defaults seguros | Cubierto estáticamente |
| CA-08 | `TIMESTAMP WITH TIME ZONE` y `CURRENT_TIMESTAMP` | Cubierto estáticamente |
| CA-09 | unicidades e índices explícitos para identidad, usuario y empresa | Cubierto estáticamente |
| CA-10 | no hubo cambios JPA ni se habilitó DDL automático | Cubierto por alcance |
| CA-11 | reejecución y checksum runtime de Flyway | Pendiente en S3-08 |
| CA-12 | rollback conserva V3; procedimiento sin `clean` ni borrado de volumen | Documentado |
| CA-13 | runbooks de migración y respaldo actualizados | Cubierto documentalmente |
| CA-14 | matriz PostgreSQL diferida y trazada | Pendiente en S3-08 |

## Validaciones aplazadas y deuda explícita

Por decisión del responsable de producto, en esta historia no se ejecutaron JUnit, ArchUnit, Testcontainers, PostgreSQL, Flyway contra base real, Docker, Compose ni smoke tests. La historia no está completada y no se presenta ningún gate de pruebas como verde.

La suite existente también requiere evolucionar en el gate acumulado:

- `CoreMigrationPostgreSqlIT` todavía espera versión de esquema 2 y dos migraciones;
- `CoreMigrationResourceTest` conserva cobertura de checksum de V1 y estructura de V2;
- faltan casos específicos de V3 para base vacía, V2→V3, reejecución, checks, unicidades, FKs compuestas y aislamiento entre empresas.

Estas diferencias no son resultados fallidos porque esas pruebas no se ejecutaron. Son trabajo obligatorio de `J11-S3-08` antes de aceptar la demo o completar la historia.

## Recuperación y compatibilidad

Los procedimientos actualizados establecen que:

- debe realizarse un respaldo antes de promover una aplicación que requiera V3;
- un rollback de aplicación puede ignorar las tablas V3, pero no debe eliminarlas;
- no se debe ejecutar `flyway clean`, borrar el volumen ni editar/renombrar una migración aplicada;
- ante una falla de actualización se detiene la promoción, se conserva la evidencia y se restaura en una instancia controlada si corresponde.

Referencias: [runbook del migrador](../runbooks/migrator.md) y [backup/restauración de PostgreSQL](../runbooks/postgresql-backup-restore.md).

## Gate documental G0

Se recorrieron todos los Markdown del repositorio, excluyendo `.git`, `.tools`, `target` y temporales. Cada archivo se decodificó con UTF-8 estricto, se buscaron caracteres de reemplazo y marcadores comunes de mojibake, y se resolvieron los enlaces locales desde su documento de origen.

```text
MARKDOWN_FILES=84
BAD_FILES=0
LOCAL_LINKS=239
BROKEN_LINKS=0
```

G0 queda cubierto para el alcance documental de esta historia. Esto no modifica el estado pendiente de los gates de ejecución.

## Archivos del cambio

- `migrator/src/main/resources/db/migration/core/V3__add_identity_membership_and_authorization.sql`
- `docs/sprints/sprint-03/J11-S3-02-migracion-core-v3-seguridad.md`
- `docs/sprints/sprint-03/README.md`
- `docs/architecture/overview.md`
- `docs/architecture/test-strategy.md`
- `docs/implementation-guide/README.md`
- `docs/implementation-guide/VALIDATION.md`
- `docs/runbooks/migrator.md`
- `docs/runbooks/postgresql-backup-restore.md`
- `docs/evidence/J11-S3-02-migracion-core-v3-seguridad.md`
- `docs/evidence/README.md`
- `docs/README.md`

## Conclusión y siguiente paso

`J11-S3-02` queda **Implementada pendiente de validación**. La candidata empaqueta V3 y conserva V1/V2, pero falta demostrar su comportamiento sobre PostgreSQL y actualizar/ejecutar la suite acumulada.

El siguiente trabajo autorizado es `J11-S3-03`: persistencia JPA/JTA y casos de uso de seguridad sobre este esquema, manteniendo `hibernate.hbm2ddl.auto=validate` y sin considerar cerradas S3-01 ni S3-02.
