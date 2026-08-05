# Validación y operación de Compose

- Fecha: 2026-08-04
- Historia vigente: `J11-S8-C02`
- Estado operativo: `core` V6, `business_partners` V1–V4, `commercial_catalog` V1–V4 e `inventory` V1–V2 verdes en la composición Docker reproducible
- Archivo principal: `infra/compose/compose.yaml`

## Objetivo

Validar y operar la topología declarada de PostgreSQL, migrador one-shot, Keycloak externo y aplicación. El servicio Keycloak incluido usa `start-dev` y `dev-file` únicamente para desarrollo/demo; producción requiere su propia topología respaldada. Las imágenes de un ambiente compartido deben indicarse como referencias inmutables `repositorio@sha256:digest`.

## Prerrequisitos

- Docker Engine y Compose operativos.
- Contexto Linux compatible con `linux/amd64`.
- Imagen de aplicación construida según [Construcción de la imagen Docker](docker-build.md).
- Para arrancar: imágenes de aplicación/migrador y los cuatro archivos de secreto externos creados.

No se necesita Maven global. Compose no debe leer ni incorporar el contenido de `.tools/` salvo los archivos de secreto montados explícitamente en tiempo de ejecución.

## Validación estática autorizada

Desde la raíz del proyecto:

```powershell
docker compose -f infra/compose/compose.yaml config --quiet
docker compose --env-file infra/compose/compose.env.example -f infra/compose/compose.yaml config --quiet
```

Ambos comandos deben terminar con código 0 y no deben crear contenedores, redes ni volúmenes.

Para revisar la configuración efectiva sin mostrar el contenido de secretos:

```powershell
docker compose --env-file infra/compose/compose.env.example -f infra/compose/compose.yaml config
```

La salida debe contener `postgres`, `migrator`, `keycloak` y `app`; los volúmenes
`postgres-data` y `keycloak-data`; cuatro secretos representados solo por rutas; y
las dependencias `service_healthy` y `service_completed_successfully`.

## Redes

Compose declara tres redes con responsabilidades separadas:

- `backend` es interna y conecta `postgres`, `migrator` y `app`;
- `identity` es interna y conecta únicamente `keycloak` y `app` mediante el alias estable `keycloak.localhost`;
- `edge` es una red bridge no interna conectada a `app` y `keycloak` para permitir la publicación HTTP en loopback;
- PostgreSQL y migrator no publican puertos;
- la aplicación se publica por defecto en `127.0.0.1:8080` y Keycloak en `127.0.0.1:8180`.

Docker Desktop no materializa la publicación al host cuando `app` participa únicamente de una red interna. No eliminar `internal: true` de `backend` o `identity`: la topología conserva la separación `backend`/`identity`/`edge`.

## Preparación local antes del primer arranque

1. Copiar `infra/compose/compose.env.example` a `infra/compose/compose.env.local`. El sufijo `.local` está ignorado por Git.
2. Crear `.tools/secrets/` dentro del proyecto.
3. Generar valores aleatorios distintos y de una sola línea en
   `postgres-password.txt`, `keycloak-admin-password.txt`, `oidc-client-secret.txt` y
   `demo-user-password.txt`.
4. Restringir el acceso a los cuatro archivos según el sistema operativo.
5. Mantener los secretos fuera de `compose.env.local`; ajustar allí solo issuer, cliente, redirect/origin/logout y puertos no sensibles.
6. En ambientes compartidos, reemplazar `LOGIXONE_APP_IMAGE` y `LOGIXONE_MIGRATOR_IMAGE` por referencias del mismo registro con `@sha256:digest`.
7. Asignar a `LOGIXONE_TX_NODE_ID` un identificador estable y único para cada instancia WildFly; no reutilizarlo entre nodos concurrentes.
8. Volver a ejecutar `docker compose config --quiet` con el archivo local.

Nunca copiar los secretos al archivo de variables, al Dockerfile, a argumentos de build, a logs ni a la documentación.

Al arrancar `app`, su entrypoint exige los archivos de PostgreSQL y del cliente OIDC, valida su forma y carga ambos valores solo en el proceso de WildFly. El entrypoint de Keycloak hace lo propio con su autoridad administrativa inicial y el mismo secreto de cliente. El datasource administrado usa expresiones externas; la aplicación no abre conexiones con `DriverManager`. El procedimiento OIDC detallado está en [Keycloak y OIDC para desarrollo y demo](keycloak-oidc.md).

## Datasource y transacciones

La imagen registra el driver como módulo del servidor y materializa `java:/jdbc/LogixoneCoreDS` mediante CLI reproducible. El datasource es JTA, usa `READ_COMMITTED`, valida con `SELECT 1` y mantiene un pool de 2 a 20 conexiones. `logixone-core-pu` usa ese JNDI y solo valida el esquema; cualquier intento de crear o actualizar DDL desde JPA contradice el baseline.

Una comprobación operativa de solo lectura puede ejecutarse dentro de un entorno ya saludable:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml exec -T app `
  /opt/jboss/wildfly/bin/jboss-cli.sh --connect `
  '--commands=/subsystem=transactions:read-attribute(name=node-identifier),/subsystem=datasources/data-source=LogixoneCoreDS:read-resource(include-runtime=true)'
```

La salida debe mostrar `jta => true`, el JNDI estable, el pool esperado y expresiones externas; no debe contener el valor resuelto de la contraseña.

## Migración actual

El corte `J11-S8-C02` incorpora `core` V6, `plg_business_partners` V1–V4,
`plg_commercial_catalog` V1–V4 y `plg_inventory` V1–V2 mediante el perfil
`with-inventory-demo`; readiness exige V6 del núcleo. Una base vacía
aplica V1–V6; una base V1 aplica cinco migraciones, una V2 cuatro, una V3 tres, una
V4 dos y una V5 sólo V6. La matriz está verde en PostgreSQL real y Compose:

```powershell
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml up -d --wait --wait-timeout 120 postgres
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml run --rm migrator
```

Para el núcleo, el resultado es `migrations_executed=6`, `5`, `4`, `3`, `2` o `1`,
según el origen, siempre con `schema_version=6`. En una base vacía participantes
aplica V1–V4, catálogo V1–V4 e inventario V1–V2; una segunda ejecución aplica cero
para núcleo y todos los plugins. J11-S8-C02 aplicó V3 de participantes y luego V2
del catálogo una vez sobre el volumen conservado. El decimoséptimo corte aplicó
V3 del catálogo una vez y preservó los contenedores PostgreSQL/Keycloak; cada repetición oficial informó
cero cambios para todos los propietarios.
El decimoctavo corte validó V4 en la composición independiente `logixone-vfh`,
con puertos 28080/9180 y volúmenes nuevos para no alterar datos persistentes sin
autorización. La primera ejecución instaló V1–V4 y la segunda informó cero
migraciones y `schema_version=4`; el entorno habitual permaneció intacto.
El vigésimo corte validó V4 de participantes en `logixone-bpd`, puertos
38080/10180 y volúmenes nuevos. La primera ejecución aplicó V1–V4; la segunda
informó `migrations_executed=0` y `schema_version=4`. Consulte la
[demo reproducible](demo-definiciones-socios-j11-s8-c02.md).
Consultar el procedimiento detallado en
[Construcción y operación del migrador](migrator.md).

## Bootstrap de autoridad global

Compose declara `LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_*`, pero mantiene
`LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_ENABLED=false`. La declaración identifica el
issuer configurado, subject exacto, nombre opcional, rol global y permisos conocidos
del kernel. Debe incluir `kernel.system_administration.manage` y nunca contiene
password, token, empresa o rol de Keycloak.

`J11-S4-03` conecta el puerto con `JpaSystemAuthorityRepository` y
`TransactionalSystemAuthorityUseCases`. El bootstrap ya puede ejecutarse como una
única transacción, pero su validación PostgreSQL/JTA permanece pendiente; no usarlo
todavía sobre datos compartidos o productivos.

Procedimiento one-shot para el futuro ambiente de desarrollo autorizado:

1. comprobar que V6 fue aplicada y conservar backup si el volumen tiene datos;
2. completar subject, nombre de rol y permisos exactos en el archivo local ignorado;
3. establecer `LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_ENABLED=true`;
4. recrear solo `app` y comprobar el evento `system_authority_bootstrap_completed`;
5. repetir una vez para verificar `UNCHANGED` cuando el gate JTA esté habilitado;
6. volver inmediatamente a `false` y recrear `app`;
7. conservar evidencia sin issuer, subject, tokens ni secretos.

Una incompatibilidad aborta el arranque. No corregirla con SQL, roles de Keycloak
ni un endpoint temporal.

Después del bootstrap y de volver a `ENABLED=false`, la comprobación administrativa
partirá de `/logixone/faces/admin/index.xhtml`. Hasta `J11-S4-07` se agregaron
`/faces/admin/companies.xhtml`, `/faces/admin/plugins.xhtml`,
`/faces/admin/security.xhtml`, `/faces/admin/system-authority.xhtml` y
`/faces/admin/audit.xhtml`; OIDC
autentica y la aplicación revalida la autoridad global exacta en cada request y
comando. Los roles empresariales de `security.xhtml` siguen separados de los roles
globales. Su recorrido runtime permanece pendiente hasta `J11-S4-08`.

## Último arranque completo probado

`J11-S3-08` repitió el último arranque completo validado con V3, Keycloak y health semántico. Si el
puerto `8080` ya está ocupado, usar la variable externa documentada sin modificar
Compose:

```powershell
$env:LOGIXONE_HTTP_PORT = '18080'
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml up --wait --wait-timeout 180
```

Ese baseline produjo:

- PostgreSQL queda saludable.
- El migrador termina una sola vez con código 0.
- `app` arranca únicamente después y queda saludable.
- `http://127.0.0.1:<puerto>/logixone/health/live` responde `200` y `UP`.
- `http://127.0.0.1:<puerto>/logixone/health/ready` responde `200` y contiene
  `catalog`, `configuration`, `database`, `migrations` y `oidc-configuration` en
  `UP`.
- Ambas respuestas son JSON, incluyen `Cache-Control: no-store` y no exponen secretos ni diagnósticos internos.

La candidata S3 agrega Keycloak, el check local `oidc-configuration`, protección de
`/app/*`, `/admin/*`, `/faces/*` y `/api/*`, y dependencia de `app` respecto de Keycloak
saludable. `J11-S3-08` comprobó arranque, login, logout, denegaciones, fallo seguro
de configuración y persistencia.

El health check de Compose consulta readiness con `curl --fail`; un `503` produce un probe fallido. Docker Compose no reinicia por sí solo un contenedor únicamente por quedar `unhealthy`.

### Actualización V1→V2 probada

En una composición efímera se arrancó la aplicación nueva sobre V1 sin adelantar el migrator. Liveness permaneció en 200 y readiness respondió 503 con `migrations=DOWN`. Después de ejecutar el migrator V2, la misma instancia recuperó readiness 200 sin reiniciarse.

La instalación desde una base vacía y el rollback de la imagen histórica de aplicación sobre V2 también quedaron probados. El rollback de aplicación conserva las migraciones y datos V2; nunca se usa `down --volumes` sobre un entorno persistente para simularlo.

## Prueba REST automatizada

Con la composición saludable y el puerto real publicado:

```powershell
.\mvnw.cmd -B -pl tests/integration-tests `
  "-Dlogixone.base-uri=http://127.0.0.1:<puerto>" verify
```

El perfil `runtime-integration` se activa por la propiedad y ejecuta `HealthEndpointsIT` mediante Failsafe y REST Assured. El resultado requerido es 2 pruebas, cero fallos, errores u omitidas.

### Prueba transaccional JTA opt-in

El perfil `jta-runtime-harness` construye un WAR de prueba separado. Se despliega únicamente en una composición efímera controlada y nunca se incorpora a la imagen o WAR normal. Con el arnés temporal ya desplegado:

```powershell
.\mvnw.cmd -B -pl tests/integration-tests `
  "-Dlogixone.base-uri=http://127.0.0.1:<puerto>" `
  "-Dlogixone.jta-probe=true" verify
```

El resultado requerido desde `J11-S2-07` es 6 pruebas verdes: dos de salud, dos del probe JTA básico y dos que recorren los servicios de aplicación. El escenario de dos empresas demuestra commit, resultados efectivos aislados, contribuciones diferentes y composiciones distintas de la misma pantalla con cada personalización al final; el último escenario demuestra rollback por fallo de auditoría. El procedimiento y la evidencia exacta están en [J11-S2-07](../evidence/J11-S2-07-contrato-personalizacion-pantallas.md).

## Caso negativo controlado

Solo sobre una composición local efímera y con nombre de proyecto confirmado:

```powershell
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml stop postgres
curl.exe -i http://127.0.0.1:<puerto>/logixone/health/live
curl.exe -i http://127.0.0.1:<puerto>/logixone/health/ready
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml start postgres
```

Mientras PostgreSQL está detenido, liveness debe seguir en `200 UP` y readiness debe responder `503 DOWN` con `database` y `migrations` en rojo. Después de recuperar PostgreSQL, readiness debe volver a `200 UP` sin reiniciar `app`.

## Diagnóstico

```powershell
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml ps
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml logs --no-color postgres migrator keycloak app
```

No usar comandos que impriman variables de entorno ni inspeccionar el contenido de `/run/secrets` en evidencias.

Si el migrador falla, `app` no debe arrancar. Corregir la migración o configuración, volver a construir/promover el artefacto correspondiente y repetir el migrador; no alterar manualmente el esquema para eludir el fallo.

## Parada y conservación de datos

```powershell
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml down
```

`down` conserva los volúmenes nombrados `postgres-data` y `keycloak-data`. No añadir `--volumes`: esa opción elimina los datos de negocio y el estado del realm, y requiere una decisión destructiva explícita, verificación de backups y autorización.

`J11-S3-08` registró los conteos y checksums de PostgreSQL y el estado del cliente y
usuarios de Keycloak, ejecutó `down` sin `--volumes`, comprobó que ambos volúmenes
seguían presentes y recreó la composición. Los conteos, checksums y estado OIDC se
conservaron. Un volumen solo se elimina al finalizar un entorno explícitamente
efímero y aislado, después de guardar la evidencia.

## Recuperación básica

1. Detener la composición sin eliminar volúmenes.
2. Verificar los volúmenes `logixone_postgres-data` y `logixone_keycloak-data`, además de los logs de ambos servicios.
3. Restaurar desde un backup compatible en un volumen controlado, nunca sobre el único estado recuperable.
4. Ejecutar el migrador contra el estado restaurado.
5. Arrancar `app` únicamente si migración, PostgreSQL y Keycloak terminan saludables y el cliente OIDC conserva el secreto compatible.

El procedimiento reproducible, sus controles de seguridad y la restauración en una base temporal se detallan en [Backup y restauración controlada de PostgreSQL](postgresql-backup-restore.md).
La operación específica del proveedor se detalla en [Keycloak y OIDC para desarrollo y demo](keycloak-oidc.md).
