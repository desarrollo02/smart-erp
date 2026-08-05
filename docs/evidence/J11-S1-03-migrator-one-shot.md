# J11-S1-03 — Migrador one-shot y fallo seguro

- Fecha: 2026-07-23
- Estado: Verde
- Alcance: quinto incremento de `J11-S1-03`
- Plataforma: Windows anfitrión, Docker Desktop `desktop-linux`, contenedores `linux/amd64`
- Java: Temurin 21.0.11+10
- Maven Wrapper: 3.9.16
- Docker Engine: 29.6.2
- Docker Compose: 5.3.1
- PostgreSQL: 18.4

## Objetivo

Implementar el primer migrador ejecutable, demostrar una migración real e idempotente sobre PostgreSQL y probar que un checksum inválido termina de forma observable, no expone credenciales e impide que Compose inicie `app`.

## Dependencias fijadas

| Componente | Versión | Uso | Licencia |
|---|---:|---|---|
| Flyway Core | 12.8.1 | Motor de migración | Apache-2.0 |
| Flyway Database PostgreSQL | 12.8.1 | Soporte específico PostgreSQL | Apache-2.0 |
| pgJDBC | 42.7.10 | Driver JDBC | BSD-2-Clause |
| JUnit Jupiter | 5.14.2 | Pruebas unitarias | EPL-2.0 |
| Maven Shade Plugin | 3.6.2 | JAR ejecutable reproducible | Apache-2.0 |

Las versiones se centralizaron en el POM padre. Maven almacenó todos los artefactos descargados en `.tools/maven-repository`; el build de contenedor utilizó su caché BuildKit declarada. Los avisos `LICENSE` y `NOTICE` de las dependencias se agregan dentro del JAR ejecutable.

Fuentes verificadas:

- [Flyway Core 12.8.1](https://central.sonatype.com/artifact/org.flywaydb/flyway-core/12.8.1)
- [Flyway PostgreSQL 12.8.1](https://central.sonatype.com/artifact/org.flywaydb/flyway-database-postgresql/12.8.1)
- [Descargas oficiales de pgJDBC](https://jdbc.postgresql.org/download/)
- [Maven Shade Plugin 3.6.2](https://maven.apache.org/plugins/maven-shade-plugin/plugin-info.html)
- [Ubicaciones Flyway](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-locations-setting)
- [Configuración `cleanDisabled`](https://documentation.red-gate.com/fd/flyway-clean-disabled-setting-277578981.html)

## Implementación

### Configuración

El proceso exige:

- `LOGIXONE_DB_URL`: URL `jdbc:postgresql://...` sin usuario ni contraseña embebidos;
- `LOGIXONE_DB_USER`: usuario de base de datos;
- `LOGIXONE_DB_PASSWORD_FILE`: ruta al archivo de secreto.

El lector acepta como máximo un salto final habitual, rechaza secretos vacíos, multilinea, mayores a 4096 bytes o no codificados en UTF-8, y nunca incluye el contenido en `toString` ni en eventos de error.

### Migración inicial

`V1__initialize_core_schema.sql` crea `core.system_metadata` y registra `schema_owner=core`. Flyway crea el esquema `core`, mantiene `core.flyway_schema_history`, valida nombres y checksums, deshabilita `clean`, rechaza ubicaciones ausentes y no permite ejecución fuera de orden.

### Códigos de salida

| Código | Significado | Evento estable |
|---:|---|---|
| 0 | Migración/validación exitosa | `event=migration_succeeded` |
| 1 | Fallo Flyway o de base | `event=migration_failed` |
| 2 | Configuración inválida | `event=configuration_failed` |

Los eventos no imprimen mensajes de excepciones ni valores de configuración. Flyway registra URL, versión de servidor y estado de migración, pero no la contraseña.

### Imagen

`infra/docker/Dockerfile.migrator` es multi-stage. El builder ejecuta `mvnw -pl migrator -am package`; el runtime parte del JRE Temurin 21 fijado por digest y ejecuta únicamente `/opt/logixone/migrator.jar` como `10001:10001`.

## Pruebas ejecutadas

Todos los comandos se ejecutaron el 2026-07-23 desde `C:\cosme\LogixoneJakarta11`.

| Corte | Resultado | Código |
|---|---|---:|
| Dependencias: `mvnw.cmd -B -pl migrator -am test` | Reactor mínimo 3/3 | 0 |
| Lector de configuración | 4 pruebas verdes | 0 |
| Comando, recurso SQL y salida segura | 8 pruebas verdes | 0 |
| `mvnw.cmd -B -pl migrator -am package` | 8 pruebas y JAR ejecutable, sin advertencias Shade finales | 0 |
| JAR sin variables | Evento `MISSING_DB_URL`, salida esperada 2 | 2 esperado |
| `docker build --check -f infra/docker/Dockerfile.migrator .` | Sin advertencias | 0 |
| Build limpio de la imagen | 8 pruebas dentro de Linux, imagen creada | 0 |
| Contenedor sin variables | Evento `MISSING_DB_URL`, salida esperada 2 | 2 esperado |
| `docker compose config --quiet` | Topología válida antes del arranque | 0 |
| `up -d --wait postgres` | PostgreSQL `18.4`, saludable | 0 |
| Primera ejecución del migrador | `migrations_executed=1 schema_version=1` | 0 |
| Consulta de objetos e historial | `schema_owner=core`, V1 exitosa, checksum `-1098736951` | 0 |
| Segunda ejecución | `migrations_executed=0 schema_version=1` | 0 |
| Checksum alterado temporalmente | `FlywayValidateException`, salida 1, sin secretos | 1 esperado |
| Orden negativo mediante `compose up -d app` | Compose salió 1; migrador salió 1; `app` quedó creado pero nunca ejecutándose | 1 esperado |
| Restauración y recuperación | Checksum original y migrador nuevamente idempotente | 0 |
| `mvnw.cmd -B verify` | Reactor completo 14/14 | 0 |
| `docker compose down` | Contenedores/red retirados; volumen conservado | 0 |

## Identidades finales

| Artefacto | Identidad |
|---|---|
| JAR ejecutable | SHA-256 `56343D2FD82E42FD53F341BC32155A7122D20C30AC342FC7C9653A778A846091` |
| Imagen local migrador | ID `sha256:8ade1b95b630bb7f7416ec52c734d44e7fedacf781846089f3e9ba1a1e643847` |
| Tamaño imagen | 104,214,960 bytes |
| Usuario runtime | `10001:10001` |
| WAR de aplicación | SHA-256 `23C935CEB30AB75CCDD72A9CF96658E6A0B63B50B4A552AE908D7B7F1488BD39` sin cambio |

El ID superior local incluye atestación BuildKit. La promoción por digest de registro permanece pendiente; no debe reconstruirse entre ambientes.

## Seguridad

- El secreto aleatorio local tiene 44 bytes, está bajo `.tools/secrets/postgres-password.txt` y nunca se imprimió.
- No hay contraseña en Compose, variables directas, Dockerfile, JAR, historial de imagen ni evidencia.
- La imagen no contiene `/workspace` ni `.tools`.
- El JAR pertenece a UID/GID `10001:10001`, modo `0644`.
- Los eventos negativos no incluyen el mensaje original de la excepción.
- `app` no llegó al estado `running` cuando el migrador terminó con código 1.

## Incidencias y correcciones

1. El sandbox negó inicialmente acceso a Buildx; se repitió el análisis con autorización explícita.
2. PowerShell trató el `stderr` y código 2 esperado del JAR como excepción; se corrigió solo el arnés para capturar `$LASTEXITCODE`.
3. Shade informó recursos de licencia y módulos duplicados; se agregaron `LICENSE`/`NOTICE`, se excluyeron descriptores JPMS no válidos en un uber-JAR y se generó un único manifiesto final. El package quedó sin advertencias Shade.
4. `RandomNumberGenerator.Fill` no estaba disponible en el runtime .NET de PowerShell; no creó archivo. Se usó `RandomNumberGenerator.Create().GetBytes` y se generó el secreto sin mostrarlo.
5. La primera aserción de montajes concatenó volumen y secreto; se filtró por `Type=volume` y se verificó correctamente `/var/lib/postgresql`.
6. La primera segunda ejecución fue idempotente, pero informó `schema_version=none`; se cambió el ejecutor para consultar la versión actual y se reconstruyó la imagen. El resultado final es `schema_version=1`.

## Estado final

- Contenedores Compose: 0.
- Redes Compose: 0.
- Volumen conservado: `logixone_postgres-data`.
- Secreto local conservado bajo `.tools/`.
- Historial final antes de detener: V1, checksum `-1098736951`, exitosa.

El volumen se conserva deliberadamente para demostrar persistencia al recrear PostgreSQL en el sexto incremento.

## Criterios demostrados

- `CA-03`: gate Maven completo 14/14.
- `CA-09`: Compose continuó válido.
- `CA-10`: PostgreSQL real por digest, saludable y con volumen nombrado; falta la recreación.
- `CA-11`: primera ejecución, reejecución idempotente e historial verificados.
- `CA-12`: `app` no se ejecutó antes de un migrador exitoso.
- `CA-13`: checksum inválido bloqueó Compose con salida observable y segura.
- `CA-16` y `CA-17`: secreto externo y auditorías sin filtración.
- `CA-19`: comandos, fallos, correcciones, códigos e identidades registrados.

La persistencia de `CA-15`, el arranque/smoke completo de `CA-14`, la identidad promovible de `CA-18` y el cierre de `CA-20`/`CA-21` siguen abiertos.

## Siguiente paso permitido

Recrear PostgreSQL con `logixone_postgres-data`, confirmar que V1 y `system_metadata` sobreviven, ejecutar el migrador con cero cambios y arrancar `app` para probar health, smoke y recreación sin pérdida de datos.

