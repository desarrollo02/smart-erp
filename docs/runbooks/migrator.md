# Construcción y operación del migrador

- Fecha: 2026-08-04
- Historia vigente: `J11-S8-C02`
- Estado: `core` V1–V6, `plg_reference_data` V1, `plg_business_partners` V1–V4, `plg_commercial_catalog` V1–V4, `plg_inventory` V1–V2 y `plg_reference_plugin` V1 verdes en PostgreSQL; repetición Docker de C03 pendiente
- Última imagen técnica verificada: `logixone/migrator:j11-s8-c02-partner-definitions`, `sha256:c3cffe4b25f66ffbc187e313b79d3b622b547908eaf8c3de39e69da1e42cecf1`

## Objetivo

Construir y ejecutar el proceso one-shot que valida y aplica las migraciones antes de que Compose permita iniciar `app`.

## Contrato de configuración

| Variable | Requerida | Contenido |
|---|---|---|
| `LOGIXONE_DB_URL` | Sí | URL `jdbc:postgresql://host:puerto/base`, sin credenciales |
| `LOGIXONE_DB_USER` | Sí | Usuario PostgreSQL |
| `LOGIXONE_DB_PASSWORD_FILE` | Sí | Ruta al archivo que contiene la contraseña |

No existe variable `LOGIXONE_DB_PASSWORD`. No escribir el secreto en archivos de entorno, argumentos, Dockerfile, Compose ni logs.

## Prerrequisitos

- JDK 21 y Maven Wrapper del proyecto.
- Docker Engine operativo sobre `linux/amd64`.
- Secreto local preparado según [Validación y operación de Compose](compose.md).
- PostgreSQL saludable.

## Pruebas y construcción local

```powershell
.\mvnw.cmd -B -pl migrator -am test
.\mvnw.cmd -B -Ppostgres-integration `
  "-Dlogixone.postgres.integration=true" -pl migrator -am verify
.\mvnw.cmd -B -pl migrator -am package
```

Para una distribución con plugins, usar el mismo perfil al construir migrador y
WAR. El perfil se declara una sola vez en `distribution/logixone-plugin-set`:

```powershell
.\mvnw.cmd -B -Pwith-reference-plugin `
  -pl migrator,distribution/logixone-war -am clean package
```

Para la demo reproducible de Socios Comerciales:

```powershell
.\mvnw.cmd -B -Pwith-business-partners-demo `
  -pl migrator,distribution/logixone-war -am clean package
```

Para la candidata actual con Compras y sus dependencias:

```powershell
.\mvnw.cmd -B -Pwith-purchasing-demo `
  -pl migrator,distribution/logixone-war -am clean package
```

El migrador conserva sus pruebas unitarias y la matriz PostgreSQL/Testcontainers de
actualización core V1–V5 a V6 e instalación/idempotencia por propietario. J11-S8-C02
agregó V3 append-only de `business_partners`, la aplicó una vez sobre el volumen
conservado y repitió el migrador oficial con cero cambios. El decimosexto corte
agregó V2 append-only de `commercial_catalog`, la aplicó una vez y comprobó la
idempotencia tanto en la repetición directa como al recrear Compose.
El decimoséptimo corte agregó V3 de `commercial_catalog` para enlazar reemplazos
seguros de definiciones simples. Sobre el volumen conservado ejecutó una sola
migración y avanzó de V2 a V3; la repetición oficial informó
`migrations_executed=0` y `schema_version=3`.
El decimoctavo corte agregó V4 para versionar la cabecera y los atributos de cada
familia y enlazar asignaciones a una revisión inmutable. La validación se ejecutó
en el stack aislado `logixone-vfh` con volúmenes nuevos: la primera ejecución
aplicó V1–V4 y la segunda informó `migrations_executed=0`,
`schema_version=4`; el volumen habitual no se modificó.
El vigésimo corte agregó V4 de `business_partners` para ampliar clases, hacer
backfill de identificaciones/direcciones y crear revisiones iniciales. El stack
aislado `logixone-bpd` aplicó V1–V4 una vez y repitió con cero cambios.
El empaquetado con `-DskipTests` solo demuestra que el recurso está incluido; no
certifica migración ni restricciones sobre PostgreSQL.

Sin configuración, el JAR debe fallar de forma segura:

```powershell
java -jar migrator/target/migrator-0.1.0-SNAPSHOT-executable.jar
```

Resultado: `event=configuration_failed code=MISSING_DB_URL`, código 2.

## Construcción de imagen

```powershell
docker build --check --file infra/docker/Dockerfile.migrator .
docker build --pull --file infra/docker/Dockerfile.migrator `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo `
  --tag logixone/migrator:j11-s9-06-purchasing-demo .
```

BuildKit debe terminar sin advertencias. La imagen final debe ser `linux/amd64`, usar `10001:10001` y no contener `/workspace` ni `.tools`.

En un registro OCI, sustituir el tag local por `repositorio@sha256:digest` y promover exactamente esa referencia.

## Ejecución con Compose

```powershell
docker compose -f infra/compose/compose.yaml up -d --wait --wait-timeout 120 postgres
docker compose -f infra/compose/compose.yaml run --rm migrator
docker compose -f infra/compose/compose.yaml run --rm migrator
```

Resultado para `core` sobre una base vacía:

```text
event=migration_succeeded owner=kernel schema=core migrations_executed=6 schema_version=6
event=migration_succeeded owner=reference_data schema=plg_reference_data migrations_executed=1 schema_version=1
event=migration_succeeded owner=business_partners schema=plg_business_partners migrations_executed=4 schema_version=4
event=migration_succeeded owner=commercial_catalog schema=plg_commercial_catalog migrations_executed=4 schema_version=4
event=migration_succeeded owner=inventory schema=plg_inventory migrations_executed=2 schema_version=2
event=migration_succeeded owner=reference_plugin schema=plg_reference_plugin migrations_executed=1 schema_version=1
```

Ejecuciones posteriores:

```text
event=migration_succeeded owner=kernel schema=core migrations_executed=0 schema_version=6
event=migration_succeeded owner=reference_data schema=plg_reference_data migrations_executed=0 schema_version=1
event=migration_succeeded owner=business_partners schema=plg_business_partners migrations_executed=0 schema_version=4
event=migration_succeeded owner=commercial_catalog schema=plg_commercial_catalog migrations_executed=0 schema_version=4
event=migration_succeeded owner=inventory schema=plg_inventory migrations_executed=0 schema_version=2
event=migration_succeeded owner=reference_plugin schema=plg_reference_plugin migrations_executed=0 schema_version=1
```

Una base histórica `core` detenida en V1 informa 5 migraciones; en V2, 4; en V3,
3; en V4, 2; y en V5, 1. J11-S6-04 verificó esa matriz en PostgreSQL/Testcontainers;
la repetición en Docker con el plugin productivo quedó verde en J11-S6-06.

## Verificación sin secretos

```powershell
docker compose -f infra/compose/compose.yaml exec -T postgres `
  psql -U logixone -d logixone -AtF '|' `
  -c "SELECT version, checksum, success FROM core.flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank;"

docker compose -f infra/compose/compose.yaml exec -T postgres `
  psql -U logixone -d logixone -Atc `
  "SELECT property_value FROM core.system_metadata WHERE property_key='schema_owner';"

docker compose -f infra/compose/compose.yaml exec -T postgres `
  psql -U logixone -d logixone -AtF '|' `
  -c "SELECT version, checksum, success FROM plg_reference_plugin.flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank;"

docker compose -f infra/compose/compose.yaml exec -T postgres `
  psql -U logixone -d logixone -AtF '|' `
  -c "SELECT version, checksum, success FROM plg_reference_data.flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank;"
```

Resultado parcial histórico verificado en `J11-S3-08`:

```text
1|-1098736951|t
2|-1309935940|t
3|1116433995|t
core
```

Los checksums forman parte de la identidad de todas las migraciones. `J11-S4-08`
validó V1–V5 y `J11-S5-01` volvió a validar `core` V1–V5 junto a
`plg_reference_plugin` V1. J11-S6-04 agregó y validó V6 sin modificar esos recursos.
Consultar el historial real con los comandos anteriores;
no editar una migración aplicada ni ejecutar `repair` para ocultar una diferencia.
Una corrección normal requiere una migración nueva.

Identidad estática de los recursos empaquetados:

```text
V1 SHA-256 07A375F06F9EBB9D6E6EC162E113ADA35397348BFCD03486870FAF28CC424DA6
V2 SHA-256 F5186A3817F7A31569C58551A9339911B29B44F7409E47AE470FC999AFA5CC11
V3 SHA-256 6C34C64C0739F4988287C7B9DBA5A0DFF2808C976B30A0B2C066F382F7961170
V4 SHA-256 8C35EF550FFC0949915758389781B25F9243A1E49AEC8AC2AFC16F26CB46B67A
V5 SHA-256 0AACBA3999424DBB00337D7DF39936E9D702E1E2DF8D413A80817E5C8A52D625
V6 SHA-256 AC4F1128E6ED31618376D213BC801B29C77B0DBA99F3AEC7C49B1DD10B4BEE35
```

El checksum de archivo no sustituye el checksum Flyway. Los valores runtime V1–V6
y el del plugin se conservaron antes y después de recrear la composición con los
mismos volúmenes.

El recurso privado nuevo de `commercial_catalog` tiene identidad estática:

```text
V3__link_replaced_simple_catalog_definitions.sql SHA-256 1A4A9E39F349F7FC4E2FCB5953BC0CEA116EF15FA011C5C373DA11845799ABF4
V4__version_variant_family_history.sql SHA-256 FBA77C705B91FD06A3B0FC213954B5CE468B4FE816430AC711FDD91E782B3C5C
```

## Compatibilidad de actualización y rollback

- El corte `J11-S4-08` verificó V5 en readiness, `AuditEventEntity`, JPA
  `validate`, commit/rollback JTA y PostgreSQL real.
- J11-S6-04 verificó V6, la categoría `PLUGIN_OPERATION` y recursos técnicos desde bases V1–V5.
- El artefacto de Sprint 3 puede ejecutarse sobre V4/V5/V6 porque exige V3 y puede ignorar tablas aditivas.
- Un rollback de aplicación no autoriza borrar V2–V6, sus tablas ni sus datos. Para cambios destructivos futuros se necesita backup, estrategia específica y autorización.
- Antes de aplicar V4–V6 en un volumen compartido, identificar el estado y seguir el [runbook de backup y restauración](postgresql-backup-restore.md).

## Códigos y diagnóstico

| Código | Acción |
|---:|---|
| 0 | Continuar con el arranque de `app` |
| 1 | Detener; revisar conectividad, SQL, checksum y logs Flyway |
| 2 | Detener; corregir las variables/ruta del secreto |

Comandos seguros:

```powershell
docker compose -f infra/compose/compose.yaml ps -a
docker compose -f infra/compose/compose.yaml logs --no-color migrator postgres
```

No ejecutar comandos que impriman variables completas ni el contenido de `/run/secrets`.

## Parada

```powershell
docker compose -f infra/compose/compose.yaml down
```

Este comando conserva `logixone_postgres-data`. No añadir `--volumes` sin autorización destructiva explícita y un backup probado.
