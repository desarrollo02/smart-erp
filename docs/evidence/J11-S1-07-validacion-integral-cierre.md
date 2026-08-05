# J11-S1-07 — Evidencia de validación integral y cierre del Sprint 1

- Fecha: 2026-07-27
- Entorno: Windows 11, `linux/amd64`
- Java: Eclipse Temurin 21.0.11+10 desde `.tools/jdk/`
- Maven Wrapper: 3.9.16 desde `.tools/maven-wrapper-home/`
- Docker Engine: 29.6.2
- Docker Compose: 5.3.1
- WildFly: 41.0.0.Final
- PostgreSQL: 18.4 Bookworm
- Estado: Verde

## Objetivo certificado

La validación reúne las historias `J11-S1-00` a `J11-S1-06` y demuestra desde construcciones limpias que el baseline es reproducible, respeta sus límites arquitectónicos, se ensambla con el plugin de referencia presente o ausente, migra PostgreSQL de forma segura y expone salud semántica real en WildFly.

No se agregó lógica funcional. Los únicos cambios permanentes del corte son la historia, esta evidencia, los índices y la corrección de una referencia arquitectónica desactualizada.

## Ambiente y recuperación del toolchain

Al iniciar la sesión, el `java` global resolvió Java 8 y Docker Desktop estaba detenido. El primer `mvnw.cmd --version` no pudo arrancar porque no tenía definido el home local del Wrapper. Se reutilizaron, sin nuevas descargas, los artefactos ya validados dentro de `.tools`:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools\jdk\jdk-21.0.11+10').Path
$env:MAVEN_USER_HOME = (Resolve-Path '.tools\maven-wrapper-home').Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd --version
```

Resultado: Maven 3.9.16 sobre Temurin 21.0.11+10, locale `es_PY` y codificación UTF-8. Docker Desktop se inició explícitamente y el Engine confirmó cliente/servidor 29.6.2, Compose 5.3.1 y runtime `linux/amd64`.

## Gate G0 — documentación y trazabilidad

- 43 archivos Markdown se decodificaron como UTF-8 estricto.
- Cero enlaces Markdown locales rotos.
- Los 14 POM se analizaron como XML válido.
- `J11-S1-00` a `J11-S1-06` conservan estado completado y evidencia asociada.
- `AGENTS.md`, `.dockerignore` y `.gitignore` mantienen fuera de artefactos y contexto a `.tools`, `target`, archivos locales y secretos.
- Se corrigió en `docs/architecture/overview.md` la frase que todavía trataba health semántico como trabajo futuro.

El primer script G0 no fue evidencia válida: una variable `$_` quedó sombreada dentro del bucle regex, generó rutas nulas y, por el manejo de errores no terminantes de PowerShell, terminó con código 0. Se reescribió con variables explícitas y `$ErrorActionPreference = 'Stop'`; la repetición válida produjo `markdown_files=43 utf8=valid local_links=valid`.

## Gates Maven, Enforcer y ArchUnit

Comando ejecutado tres veces desde limpio: las dos primeras para validar el conjunto y comparar reproducibilidad, y la tercera después de consolidar la documentación:

```powershell
.\mvnw.cmd -B clean verify
```

Resultado de las tres ejecuciones:

- reactor 14/14 `SUCCESS`;
- 17 reportes Surefire;
- 56 pruebas, cero fallos, errores u omitidas;
- 4 reglas ArchUnit verdes;
- Maven Enforcer validó Maven `[3.9.16,3.9.17)`, Java `[21,22)`, convergencia y duplicados de versión;
- los módulos neutrales no contienen imports Jakarta, JDBC, Hibernate, JBoss, WildFly ni PostgreSQL;
- la única cadena `javax..` de los fuentes Java pertenece a la regla ArchUnit que la prohíbe.

## Composición y reproducibilidad Maven

### Variante sin plugin

El WAR predeterminado contiene cero `reference-plugin`, un pgJDBC, cero API Jakarta, cero entradas `.tools` y cero nombres de secreto. SHA-256 del WAR:

```text
4fb2491db2e8f390138c243464a212509de2d119de3e460fa21aa5ba984627e5
```

Los 13 artefactos JAR/WAR primarios coincidieron en ruta, tamaño y SHA-256 después de dos `clean verify` equivalentes:

| Artefacto | SHA-256 |
|---|---|
| `distribution/logixone-war/target/logixone.war` | `4fb2491db2e8f390138c243464a212509de2d119de3e460fa21aa5ba984627e5` |
| `kernel-api/target/kernel-api-0.1.0-SNAPSHOT.jar` | `4238e80c154f7ebeb19d2222ebb793b564f689b5fdf56ffa7effc1ca0c239abb` |
| `kernel-application/target/kernel-application-0.1.0-SNAPSHOT.jar` | `5833683498ee145e108ea4793a8a6a684b9420a17eef709311483ee54e8ad8ff` |
| `kernel-domain/target/kernel-domain-0.1.0-SNAPSHOT.jar` | `225ba57656f6d8982185b822f3462c6379283514a3892fde04e8a1106a1348ec` |
| `kernel-infrastructure-jakarta/target/kernel-infrastructure-jakarta-0.1.0-SNAPSHOT.jar` | `d74beb874af068cafd1b3bcb44da618b4e72e4b5b6a79c1ce17e1d19b1e01efa` |
| `migrator/target/migrator-0.1.0-SNAPSHOT.jar` | `e7f82e9c0debf3449900fdda36e5a48277ec1087456a34986112d67f53589cf4` |
| `migrator/target/migrator-0.1.0-SNAPSHOT-executable.jar` | `c734b2a92a7dcdb94f2014e9be4fbee75642305343bfdacc96c046964f4b30bd` |
| `plugin-api/target/plugin-api-0.1.0-SNAPSHOT.jar` | `921dd52e304335dd4f77baff60ea003aea5b8cd60d04e2da94ac37cee437ef81` |
| `plugins/reference-plugin/target/reference-plugin-0.1.0-SNAPSHOT.jar` | `003660d7f11596af6490a1db1da6097c78284600ee10d224be04da9744a82c14` |
| `tests/architecture-tests/target/architecture-tests-0.1.0-SNAPSHOT.jar` | `4238e80c154f7ebeb19d2222ebb793b564f689b5fdf56ffa7effc1ca0c239abb` |
| `tests/e2e-tests/target/e2e-tests-0.1.0-SNAPSHOT.jar` | `4238e80c154f7ebeb19d2222ebb793b564f689b5fdf56ffa7effc1ca0c239abb` |
| `tests/integration-tests/target/integration-tests-0.1.0-SNAPSHOT.jar` | `4238e80c154f7ebeb19d2222ebb793b564f689b5fdf56ffa7effc1ca0c239abb` |
| `web-shell/target/web-shell-0.1.0-SNAPSHOT.jar` | `57c925d3a5c80afc8ee49c2d8969689d83c0d4ae78b86299d66538b3137c561a` |

### Variante con plugin

Comando ejecutado dos veces:

```powershell
.\mvnw.cmd -B -Pwith-reference-plugin `
  -pl distribution/logixone-war -am clean package
```

Ambos builds terminaron 9/9 y produjeron exactamente un JAR `reference-plugin`, un pgJDBC, cero API Jakarta y el mismo WAR de 1.275.893 bytes:

```text
94aa17ad03d390b0cc900bf3f6351036113ff726dc0d2a8700ebbc2793130c4d
```

## Docker e imágenes

Los dos Dockerfile pasaron `docker buildx build --check` sin advertencias. Compose validó con defaults, con `compose.env.example` y con el archivo local, siempre mediante `config --quiet`.

Imágenes construidas desde el corte certificado:

| Imagen | ID local BuildKit | Tamaño | Usuario | Artefacto interno SHA-256 |
|---|---|---:|---|---|
| `logixone/app:j11-s1-07` | `sha256:b863cdc83d1e00a24a32146ec4851342cea656abfe80606c005419d6a0bde5b0` | 499.715.133 | `jboss` | WAR `4fb2491db2e8f390138c243464a212509de2d119de3e460fa21aa5ba984627e5` |
| `logixone/app:j11-s1-07-reference` | `sha256:721e5f9574c1dd0b2fbf09c23504fb3291b5d34721bba045b50a462b811e2187` | 499.716.504 | `jboss` | WAR `94aa17ad03d390b0cc900bf3f6351036113ff726dc0d2a8700ebbc2793130c4d` |
| `logixone/migrator:j11-s1-07` | `sha256:9c6a6e533d9027fbe6e1b691a3902993b20ffa32608453f7eb329d777af4348e` | 104.228.453 | `10001:10001` | JAR `c734b2a92a7dcdb94f2014e9be4fbee75642305343bfdacc96c046964f4b30bd` |

Las tres imágenes son `linux/amd64` y no contienen `/workspace` ni `.tools`. Sus configuraciones no incorporan variables sensibles. Los IDs son manifiestos locales de BuildKit, no digests publicados en un registro.

## Runtime sin plugin

Proyecto aislado `logixone-s107-absent`, puerto `18107`:

- PostgreSQL quedó `healthy`, migrator terminó `0` y app quedó `healthy`.
- El primer migrador informó `migrations_executed=1 schema_version=1`.
- V1 quedó exitosa con checksum `-1098736951` y `schema_owner=core`.
- La repetición informó `migrations_executed=0 schema_version=1`.
- Liveness respondió `200`, JSON y `Cache-Control: no-store`.
- Readiness respondió `200 UP` con `catalog`, `configuration`, `database` y `migrations` ordenados y verdes.
- REST Assured ejecutó 2 pruebas con cero fallos, errores u omitidas.

### Caída y recuperación de PostgreSQL

Con PostgreSQL detenido:

- liveness respondió `200 UP` en 64 ms;
- readiness respondió `503 DOWN` en 2.049 ms;
- únicamente `database` y `migrations` quedaron `DOWN`;
- el healthcheck Docker registró `FailingStreak=1` y salida `22` de `curl --fail`;
- el estado Docker siguió temporalmente `healthy` porque Compose exige 12 fallos consecutivos, no porque el probe aceptara el `503`.

Al reiniciar PostgreSQL, readiness volvió a `200 UP` y el ID de `app` permaneció idéntico.

### Persistencia

Se insertó el centinela controlado `s107_persistence=verified` en `core.system_metadata`:

- al forzar la recreación de `app`, cambió su contenedor y el centinela permaneció;
- al detener y recrear PostgreSQL, cambió su contenedor, el volumen nombrado permaneció idéntico y el centinela sobrevivió;
- después de la recreación, el migrador continuó idempotente con cero migraciones nuevas.

### Checksum negativo

Se copió el JAR ejecutable a `.tools/tmp`, se sustituyó únicamente dentro de esa copia la V1 por una variante con un comentario adicional y se montó de solo lectura sobre un contenedor migrador efímero. No se modificó la migración versionada del repositorio.

- JAR original: `c734b2a92a7dcdb94f2014e9be4fbee75642305343bfdacc96c046964f4b30bd`.
- JAR alterado temporal: `c98975ab37e5ccd05394181679663b160f92a9a5808e862450c54790d45d7890`.
- Resultado esperado: código `1`, `event=migration_failed type=FlywayValidateException`.
- El valor del secreto no apareció en la salida.
- La imagen original se ejecutó inmediatamente después y volvió a informar cero migraciones y versión 1.
- El JAR y directorio alterados se eliminaron al terminar.

## Runtime con plugin de referencia

Proyecto aislado `logixone-s107-reference`, puerto `18108`:

- PostgreSQL quedó `healthy`, migrator terminó `0` y app quedó `healthy`.
- La base vacía recibió una migración.
- WildFly registró despliegue de `logixone.war` y arranque completo.
- CDI informó `plugin_count=1 plugins=reference_plugin@1.0.0`.
- Readiness respondió `200 UP` con los cuatro checks verdes.
- Las mismas 2 pruebas REST Assured terminaron nuevamente sin fallos.

En total hubo 4 ejecuciones runtime verdes de los 2 casos HTTP: 2 contra cada composición. No se presentan como cuatro casos distintos.

## Auditoría de seguridad y limpieza

Se analizaron de forma redactada 384 líneas de logs de ambos proyectos:

- cero apariciones del valor del secreto;
- cero apariciones de la ruta host del secreto;
- cero URL JDBC con credenciales embebidas;
- cero líneas de stack trace Java;
- cero variables sensibles incorporadas en las tres imágenes.

La respuesta pública negativa de readiness conservó únicamente `status`, nombres de checks y estados controlados. No mostró excepciones, mensajes internos, URL, usuario ni rutas.

Los proyectos `logixone-s107-absent` y `logixone-s107-reference` se retiraron con sus redes y volúmenes efímeros. Verificación final: cero contenedores y cero volúmenes residuales. Las tres imágenes `j11-s1-07` se conservaron como evidencia local y Docker Desktop quedó en ejecución.

## Fallos de ejecución y correcciones

1. Java global era 8 y el Wrapper no tenía su home local: se restablecieron `JAVA_HOME` y `MAVEN_USER_HOME` con rutas ya validadas en `.tools`.
2. Docker Desktop estaba detenido y, una vez iniciado, el sandbox negó inicialmente el named pipe: se solicitó el acceso explícito requerido y se repitió el diagnóstico.
3. El primer validador Markdown sombreó una variable de PowerShell y generó errores no terminantes: se corrigió y G0 se repitió con modo de error estricto.
4. La primera auditoría redactada de logs tuvo una subexpresión PowerShell inválida: no leyó ni imprimió logs; se simplificó el script y la repetición terminó verde.
5. La primera comprobación agregada de contenedores combinó dos filtros de proyecto mutuamente excluyentes y no demostraba por sí sola la ausencia de residuos. La verificación final consultó cada proyecto por separado y confirmó cero contenedores y cero volúmenes en ambos.

Ningún gate del producto se omitió, desactivó o relajó.

## Pruebas no aplicables en este baseline

- Playwright: no existe UI navegable.
- Testcontainers para JPA/repositorios: todavía no existen unidad JPA ni repositorios empresariales; PostgreSQL real se validó mediante Compose.
- Autenticación/autorización negativa: identidad, empresa y permisos están fuera del Sprint 1.
- Promoción remota por digest y firma OCI: no hay registro ni ambiente compartido configurado.

Estas ausencias delimitan el baseline y deben convertirse en gates cuando aparezca la capacidad correspondiente.

## Cobertura de aceptación

| Criterios | Evidencia |
|---|---|
| `CA-01` y `CA-02` | Historias/evidencias auditadas; UTF-8, enlaces, POM e índices verdes. |
| `CA-03` y `CA-04` | Dos `clean verify`, 14/14 módulos, 56 pruebas, Enforcer y 4 ArchUnit verdes. |
| `CA-05` y `CA-06` | Composición exacta y SHA-256 repetibles para los dos WAR y 13 artefactos del build predeterminado. |
| `CA-07` y `CA-08` | Dockerfile/Compose sin advertencias; tres imágenes construidas, inspeccionadas y sin contenido local. |
| `CA-09` | Migración inicial, reejecución idempotente, checksum rechazado y recuperación verificadas. |
| `CA-10` a `CA-12` | Ambas composiciones saludables, CDI correcto y REST Assured verde en las dos. |
| `CA-13` | Recreación de app/PostgreSQL con volumen y centinela preservados. |
| `CA-14` y `CA-15` | Respuestas mínimas, auditoría redactada, checksums, fallos, límites y limpieza documentados. |
| `CA-16` | Último `clean verify` y G0 verdes; historia y Sprint cerrados, con Sprint 2 aún por definir. |

## Riesgos residuales

- La activación de plugins por empresa, seguridad, UI y dominios ERP aún no existen; son trabajo de Sprints posteriores.
- El migrador todavía no descubre ubicaciones de plugins persistentes; debe resolverse antes del primer plugin con esquema propio.
- Los IDs de imagen son locales. Una promoción real exige publicar, capturar el digest del registro y promover exactamente esa identidad.
- La carpeta no contiene metadata Git; no existe diff, commit ni estado de rama verificable.

## Conclusión

Los 16 criterios de aceptación y la Definition of Done están cumplidos. El último `mvnw.cmd -B clean verify`, posterior a esta consolidación, terminó 14/14 con 56 pruebas y 4 reglas ArchUnit verdes. G0 confirmó 44 documentos Markdown válidos y cero enlaces locales rotos.

`J11-S1-07` y el Sprint 1 quedan completados. El siguiente paso permitido es definir explícitamente objetivo, alcance, dependencias y criterios del Sprint 2 antes de iniciar nuevo código.
