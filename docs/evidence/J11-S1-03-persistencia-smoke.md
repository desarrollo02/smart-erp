# J11-S1-03 - Persistencia, health y smoke

- Fecha de inicio: 2026-07-23
- Estado: completado
- Incremento: sexto de `J11-S1-03`
- Historia: [Docker e infraestructura como código](../sprints/sprint-01/J11-S1-03-docker-iac.md)

## Objetivo

Demostrar que la composición completa puede recuperarse desde el volumen PostgreSQL preservado, que el migrador permanece idempotente, que WildFly despliega el WAR y responde por HTTP, y que recrear `app` y PostgreSQL sin eliminar el volumen conserva el estado migrado.

## Criterios de aceptación del incremento

- [x] Docker Engine, Compose, imágenes, secreto local y volumen preservado pasan el preflight.
- [x] `docker compose config --quiet` valida la configuración antes del arranque.
- [x] PostgreSQL recreado con `logixone_postgres-data` queda saludable y conserva V1.
- [x] `core.system_metadata` conserva `schema_owner=core` y el historial conserva su checksum.
- [x] El migrador termina con código `0`, aplica cero migraciones y reporta versión `1`.
- [x] `app` queda saludable y el smoke recibe la respuesta HTTP esperada para el WAR actual.
- [x] Recrear `app` conserva el estado de la base.
- [x] Recrear PostgreSQL con el mismo volumen conserva migración y metadatos.
- [x] Logs, configuración e imágenes no exponen secretos ni contenido de `.tools/`.
- [x] `mvnw.cmd verify` termina con el reactor completo verde.
- [x] Runbooks, historia y tablero Scrum reflejan el resultado final.

## Estado inicial conocido

- El quinto incremento terminó con cero contenedores y redes Compose.
- El volumen `logixone_postgres-data` quedó deliberadamente preservado.
- La última base verificada contenía V1 con checksum `-1098736951` y `schema_owner=core`.
- El migrador terminó idempotente con `migrations_executed=0` y `schema_version=1`.
- El JAR, WAR e imágenes permanecen identificados en la evidencia del quinto incremento.

## Registro de ejecución

Los comandos, códigos de salida, estados observados, fallos y correcciones se agregarán por corte sin registrar el contenido del secreto.

### Corte 1 - Preflight

El preflight se ejecutó el 2026-07-23 antes de crear contenedores o redes y terminó con código `0`.

| Control | Resultado |
|---|---|
| Docker Client / Engine | `29.6.2` / `29.6.2` |
| Docker Compose | `5.3.1` |
| Buildx | `0.35.0-desktop.2` |
| Contexto / plataforma | `desktop-linux` / `linux/amd64` |
| Recursos globales Docker | 0 contenedores, 16 imágenes |
| Recursos Compose `logixone` | 0 contenedores, 0 redes |
| Volumen preservado | `logixone_postgres-data`, driver `local` |
| Secreto local | existe, 44 bytes; contenido no leído ni impreso |
| Imagen app | ID y RepoDigest `sha256:c812ebebc6fc430d152922536bc57fe520661969e76032bbe58ea71407b77b34`, usuario `jboss` |
| Imagen migrator | ID y RepoDigest `sha256:8ade1b95b630bb7f7416ec52c734d44e7fedacf781846089f3e9ba1a1e643847`, usuario `10001:10001` |
| `docker compose config --quiet` | válido |

No se observó deriva del Engine ni de Compose respecto al quinto incremento. Buildx pasó de la versión documentada anteriormente a `0.35.0-desktop.2`; el análisis no modifica las imágenes ya construidas y ambas conservaron su identidad local.

### Corte 2 - Recuperación inicial de PostgreSQL

Se ejecutó `docker compose up -d --wait postgres` con el volumen preservado. Compose creó únicamente la red `logixone_backend` y el contenedor `logixone-postgres-1`; no creó un volumen nuevo.

| Control | Resultado |
|---|---|
| Estado del contenedor | `running`, health `healthy` |
| Imagen efectiva | `sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0` |
| Versión del servidor | PostgreSQL `18.4 (Debian 18.4-1.pgdg12+1)` |
| Montaje persistente | `logixone_postgres-data` en `/var/lib/postgresql`, lectura/escritura |
| Montaje del secreto | `/run/secrets/postgres_password`, solo lectura |
| Metadato persistido | `schema_owner=core` |
| Historial Flyway | V1, checksum `-1098736951`, `success=true` |

La consulta se ejecutó con `psql -v ON_ERROR_STOP=1` y terminó con código `0`. El contenido del secreto no fue leído ni impreso.

### Corte 3 - Migrador idempotente

`docker compose run --rm migrator` terminó con código `0` y emitió el evento estable:

```text
event=migration_succeeded schema=core migrations_executed=0 schema_version=1
```

Flyway validó dos entradas de historial: la creación del esquema administrada por Flyway y la migración versionada V1. Solo existe una versión aplicada, `version=1`; `schema_owner=core` permaneció sin cambios.

### Incidencia 1 - Puerto local 8080 ocupado

El primer `docker compose up -d --wait app` terminó con código `1` antes de iniciar `app`. Docker no pudo publicar `127.0.0.1:8080` porque ya estaba ocupado por un proceso Java local:

| Dato | Valor |
|---|---|
| PID | `23432` |
| Proceso | `java` |
| Runtime | `C:\Program Files\Java\jdk1.8.0_202\bin\java.exe` |
| Estado de app | `Created`, nunca llegó a `running` |
| Estado de migrator | `Exited (0)` |
| Estado de PostgreSQL | `running`, health `healthy` |

No se detuvo ni modificó el proceso externo. La corrección prevista es usar la variable documentada `LOGIXONE_HTTP_PORT=18080`, manteniendo la misma imagen y topología Compose.

### Incidencia 2 - Red interna sin publicación al host

Con `LOGIXONE_HTTP_PORT=18080`, `app` arrancó y quedó saludable, pero Docker no creó el listener del host. `HostConfig` contenía el binding solicitado y `NetworkSettings.Ports` permanecía vacío. La recreación forzada produjo el mismo resultado.

Los puertos `18080` y `18081` no pertenecen a rangos excluidos de Windows. Un contenedor efímero de la misma imagen sobre la red bridge predeterminada publicó `127.0.0.1:18081`, respondió HTTP `200` y fue eliminado. Esto aisló la causa en el uso exclusivo de la red `backend` marcada `internal`.

La corrección conserva `backend` como red interna para los tres servicios y agrega `edge`, una red bridge no interna usada únicamente por `app`. El puerto continúa limitado a loopback; PostgreSQL y migrator no se conectan a `edge`.

### Corte 4 - Corrección IaC, arranque y recreación de app

Se modificó `infra/compose/compose.yaml` y se ejecutaron inmediatamente dos validaciones `config --quiet` y diez aserciones sobre el JSON renderizado:

1. siguen existiendo exactamente `postgres`, `migrator` y `app`;
2. existen exactamente las redes `backend` y `edge`;
3. `backend` permanece interna;
4. `edge` no es interna;
5. PostgreSQL y migrator usan únicamente `backend`;
6. `app` usa `backend` y `edge`;
7. el bind permanece en `127.0.0.1`;
8. la sustitución local usa el puerto `18080`;
9. el destino continúa en `8080`;
10. la configuración con el archivo de ejemplo también es válida.

Después se recreó únicamente `app` con la misma imagen y sin reiniciar sus dependencias. Resultado:

| Control | Resultado |
|---|---|
| Estado de app | `running`, health `healthy` |
| Imagen efectiva | `sha256:c812ebebc6fc430d152922536bc57fe520661969e76032bbe58ea71407b77b34` |
| Redes | `logixone_backend` y `logixone_edge` |
| Publicación | `127.0.0.1:18080 -> 8080/tcp` |
| Smoke `/` | HTTP `200` |
| Smoke `/logixone/` | HTTP `403`, transporte y despliegue confirmados |
| WildFly | `WFLYSRV0010` y `WFLYSRV0025`; cero coincidencias críticas |
| Base después de recrear app | `schema_owner=core`, V1, checksum `-1098736951`, exitosa |

El `403` es el resultado previsto mientras el WAR no exponga una ruta pública. `J11-S1-06` reemplazará esta prueba de transporte por endpoints semánticos.

### Incidencia 3 - Sintaxis del arnés PowerShell

El primer intento de ejecutar las diez aserciones tuvo un error de paréntesis en el arnés PowerShell. El script no llegó a ejecutarse y, por tanto, no modificó Compose. Se corrigió únicamente el arnés; la repetición aplicó el cambio y terminó con diez de diez controles verdes.

### Corte 5 - Recreación de PostgreSQL y recuperación completa

El primer arnés para leer el nombre del volumen usó una plantilla Go con comillas incompatibles con PowerShell. Falló antes de detener `app` o modificar contenedores. Se sustituyó únicamente esa lectura por `docker inspect` en JSON y se repitió el procedimiento.

Se detuvo `app` y se recreó solamente PostgreSQL con `--force-recreate --no-deps`, sin `--volumes`:

| Control | Resultado |
|---|---|
| Contenedor PostgreSQL anterior | `889cea3db5007ac68ae043048c5ee8cdffb0c73baebaf7bd1260dde2c77fd9e5` |
| Contenedor PostgreSQL nuevo | `2c8b2f2c04ef8b803990fc56b7e6de00879b85552df1296e48e886fdb47b271c` |
| Volumen antes y después | `logixone_postgres-data` |
| Estado nuevo | `running`, health `healthy` |
| Metadato recuperado | `schema_owner=core` |
| Migración recuperada | V1, checksum `-1098736951`, `success=true` |

Después de la recreación, el migrador terminó nuevamente con código `0`, `migrations_executed=0` y `schema_version=1`. `app` volvió a quedar saludable con la misma imagen; `/` respondió `200` y `/logixone/` respondió `403`. La consulta final confirmó otra vez V1 y `schema_owner=core`.

### Corte 6 - Gate Maven, seguridad e identidad

El gate local usó Maven Wrapper `3.9.16` y Eclipse Temurin `21.0.11`. `mvnw.cmd -B verify` terminó con código `0`, ocho pruebas del migrador y el reactor completo `14/14`.

| Artefacto | SHA-256 |
|---|---|
| `distribution/logixone-war/target/logixone.war` | `23C935CEB30AB75CCDD72A9CF96658E6A0B63B50B4A552AE908D7B7F1488BD39` |
| `migrator/target/migrator-0.1.0-SNAPSHOT-executable.jar` | `56343D2FD82E42FD53F341BC32155A7122D20C30AC342FC7C9653A778A846091` |

La auditoría final terminó con trece de trece controles verdes:

- sin variables de contraseña en claro y con secreto resuelto por archivo externo;
- logs sin marcadores de secreto ni errores críticos;
- identidades exactas de app, migrator y PostgreSQL;
- aislamiento de redes y únicamente `app` con puerto publicado;
- runtimes sin `/workspace` ni `.tools`;
- historiales de imagen sin secretos;
- matriz de configuración por digest exacta.

El primer arnés de auditoría tuvo un error sintáctico y no se ejecutó. Las siguientes dos ejecuciones interpretaron erróneamente puertos expuestos como puertos publicados porque PowerShell contó objetos vacíos; el diagnóstico mostró `HostBindings={}` en PostgreSQL y migrator. Se corrigió el arnés para enumerar bindings reales. No existió una exposición de puertos ni un defecto de seguridad.

Las matrices `development` y `test` variaron únicamente el puerto externo (`18080` y `28080`) y conservaron exactamente:

- app `logixone/app@sha256:c812ebebc6fc430d152922536bc57fe520661969e76032bbe58ea71407b77b34`;
- migrator `logixone/migrator@sha256:8ade1b95b630bb7f7416ec52c734d44e7fedacf781846089f3e9ba1a1e643847`.

Finalmente, `docker compose down` terminó con código `0`: retiró tres contenedores y las redes `backend` y `edge`, dejó cero recursos Compose activos y conservó `logixone_postgres-data`.

## Resultado del incremento

- Persistencia real de `CA-10` y `CA-15` demostrada mediante dos contenedores PostgreSQL distintos y el mismo volumen.
- `CA-14` demostrado con health de Docker, despliegue WildFly y smoke HTTP desde el host.
- `CA-16` y `CA-17` reconfirmados después del cambio de red.
- `CA-18` demostrado mediante identidades por digest iguales en las matrices de desarrollo y prueba sin reconstrucción.
- `CA-19` cumplido con comandos, códigos, incidencias, correcciones e identidades registrados.
- `mvnw.cmd verify` preservó el baseline verde `14/14`.

El sexto incremento queda completado. `J11-S1-03` permanece en curso hasta ejecutar el séptimo incremento de consolidación de runbooks, gates y cierre formal de los 21 criterios.

## Archivos modificados

- `infra/compose/compose.yaml`
- `docs/adr/0004-docker-iac-promocion-digest.md`
- `docs/architecture/overview.md`
- `docs/runbooks/compose.md`
- `docs/evidence/J11-S1-03-persistencia-smoke.md`
- `docs/evidence/README.md`
- `docs/sprints/sprint-01/J11-S1-03-docker-iac.md`
- `docs/sprints/sprint-01/README.md`
- `docs/README.md`

## Riesgos y reglas de seguridad

- No ejecutar `docker compose down --volumes` ni eliminar `logixone_postgres-data`.
- No imprimir ni copiar el contenido de `.tools/secrets/postgres-password.txt`.
- Detener el incremento si el volumen no existe, si el checksum no coincide o si alguna prueba relevante falla.
- No interpretar la respuesta HTTP operativa como health semántico del ERP; los endpoints Jakarta se implementarán en `J11-S1-06`.
