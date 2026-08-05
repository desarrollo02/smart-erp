# J11-S1-06 — Evidencia de aplicación mínima y health semántico

- Fecha: 2026-07-24
- Entorno: Windows, Java Temurin 21.0.11+10, Maven Wrapper 3.9.16, Docker Engine 29.6.2, WildFly 41, PostgreSQL 18.4
- Estado final: Verde

## Resultado funcional

- `GET /logixone/health/live` responde `200`, JSON, `UP` y `Cache-Control: no-store` sin consultar PostgreSQL.
- `GET /logixone/health/ready` responde `200 UP` cuando catálogo, configuración, base y migraciones están verdes.
- Con PostgreSQL detenido, liveness permaneció `200 UP` y readiness respondió `503 DOWN` en aproximadamente 2,02 segundos.
- Al restaurar PostgreSQL, readiness volvió a `200 UP` sin reiniciar la aplicación.
- Las distribuciones sin plugin y con `reference_plugin@1.0.0` quedaron saludables.

## Gates ejecutados

| Gate | Comando o procedimiento | Resultado |
|---|---|---|
| Modelo neutral | `mvnw.cmd -B -pl kernel-application -am test` | 6 pruebas nuevas; verde |
| Infraestructura | `mvnw.cmd -B -pl kernel-infrastructure-jakarta -am test` | configuración, probes y alcance CDI verdes |
| Recurso REST | `mvnw.cmd -B -pl web-shell -am test` | contrato, rutas y JSON verdes |
| Arquitectura y WAR | `mvnw.cmd -B -pl tests/architecture-tests,distribution/logixone-war -am verify` | 4 reglas ArchUnit y empaquetado verdes |
| Variante presente | `mvnw.cmd -B -Pwith-reference-plugin -pl distribution/logixone-war -am clean package` | un JAR de referencia, un pgJDBC y cero API Jakarta empaquetada |
| Gate local final | `mvnw.cmd -B clean verify` | 14/14 módulos; 56 pruebas, cero fallos, errores u omitidas |
| REST real | `mvnw.cmd -B -pl tests/integration-tests "-Dlogixone.base-uri=http://127.0.0.1:18088" verify` | 2 pruebas REST Assured, cero fallos |
| Dockerfile | `docker buildx build --check --file infra/docker/Dockerfile .` | sin advertencias |
| Compose estático | `docker compose ... config --quiet` con defaults y archivo ejemplo | ambos verdes |
| Compose runtime | proyectos aislados presente/ausente | PostgreSQL healthy, migrator exit 0, app healthy y readiness `UP` |

Los 17 reportes Surefire del gate final sumaron 56 pruebas. Las 2 pruebas Failsafe se ejecutaron separadamente porque necesitan una aplicación real ya iniciada; no se omitieron mediante anotaciones ni condiciones.

## Contrato HTTP observado

Liveness:

```json
{"status":"UP","checks":[{"name":"application","status":"UP"}]}
```

Readiness saludable:

```json
{"status":"UP","checks":[{"name":"catalog","status":"UP"},{"name":"configuration","status":"UP"},{"name":"database","status":"UP"},{"name":"migrations","status":"UP"}]}
```

Readiness con PostgreSQL detenido:

```json
{"status":"DOWN","checks":[{"name":"catalog","status":"UP"},{"name":"configuration","status":"UP"},{"name":"database","status":"DOWN"},{"name":"migrations","status":"DOWN"}]}
```

La prueba negativa final midió liveness `200` en aproximadamente 0,005 segundos y readiness `503` en aproximadamente 2,016 segundos. El health check de Docker registró salida 22 por `503`, no una respuesta falsamente exitosa.

## Imágenes finales

| Variante | Imagen local | ID local | Tamaño | SHA-256 del WAR |
|---|---|---|---:|---|
| Sin plugin | `logixone/app:j11-s1-06` | `sha256:681b4f16abd47511eaa6f8b8571d6d91dd2a8da1c8d7be8b554ced9dc0fd2074` | 499.715.133 | `4fb2491db2e8f390138c243464a212509de2d119de3e460fa21aa5ba984627e5` |
| Referencia | `logixone/app:j11-s1-06-reference` | `sha256:3a8491d62915310fc99a23cc51e7af123cd34e373367f457edb2f0343325f6af` | 499.716.504 | `94aa17ad03d390b0cc900bf3f6351036113ff726dc0d2a8700ebbc2793130c4d` |

Los IDs son identidades locales de manifiestos cargados por BuildKit, no digests publicados en un registro. Las imágenes se conservaron como evidencia; todos los contenedores, redes y volúmenes `logixone-s106-*` fueron eliminados.

## Dependencias

- pgJDBC `42.7.10` ya estaba centralizado y se agregó con alcance `runtime` a infraestructura para los probes; licencia BSD-2-Clause.
- REST Assured `6.0.0` se agregó exclusivamente para pruebas HTTP; licencia Apache-2.0 y baseline Java 17+, compatible con Java 21.
- Fuente oficial: [REST Assured](https://github.com/rest-assured/rest-assured).
- JAR principal almacenado en `.tools/maven-repository/io/rest-assured/rest-assured/6.0.0/` con SHA-256 `5253f655e795b64e55734f5ac42e13481a70729f02c55a3fb9740457ad1cf784`.
- Maven conservó las dependencias y sus metadatos de checksum dentro de `.tools/maven-repository`; no se descargaron artefactos del proyecto fuera de `.tools`.

## Fallos de caracterización y correcciones

1. El primer arranque detectó que un productor `@ApplicationScoped` intentaba crear un proxy CDI del servicio neutral `final`. Se cambió únicamente el productor a `@Dependent` y se agregó una prueba de alcance.
2. El recurso REST con inyección por constructor no satisfizo conjuntamente los requisitos de proxy CDI y construcción RESTEasy. La forma final usa recurso `@ApplicationScoped`, constructor público vacío e inyección de campo en el adaptador HTTP; una prueba conserva ese contrato.
3. Dos probes JDBC secuenciales podían acercarse al timeout anterior de cinco segundos durante una caída. Los timeouts JDBC quedaron en un segundo por operación y Compose en ocho segundos; el caso final respondió `503` antes del límite.
4. El primer comando REST Assured perdió parte del argumento `-D` por parsing de PowerShell. Al citarlo como un solo argumento, Failsafe ejecutó las 2 pruebas con resultado verde.

Ninguna prueba fue desactivada o relajada. Cada corrección se validó primero con el módulo afectado y después con el gate integral.

## Cobertura de aceptación

| Criterios | Evidencia |
|---|---|
| `CA-01` y `CA-02` | Modelo neutral, orden determinista, fallos y excepciones cubiertos por JUnit. |
| `CA-03` y `CA-04` | Liveness/readiness verificados por REST Assured y curl real. |
| `CA-05` a `CA-09` | Catálogo, configuración, JDBC, migraciones, redacción y logs seguros probados. |
| `CA-10` y `CA-11` | Compose usa readiness; caída y recuperación de PostgreSQL verificadas. |
| `CA-12` y `CA-13` | Variantes presente/ausente, WAR y ArchUnit verdes. |
| `CA-14` | Maven, Dockerfile, imágenes, Compose, REST, evidencia y documentación verdes. |

## Riesgos y límites

- Los probes JDBC abren conexiones breves mediante `DriverManager`; la configuración de un datasource JTA para persistencia empresarial queda fuera de esta historia.
- Docker Compose dispone de un único health check; se usa readiness. Liveness queda expuesto para monitorización y futuros orquestadores.
- La carpeta continúa sin metadata Git, por lo que no existe diff ni commit verificable.
- Docker Desktop quedó en ejecución al finalizar.

## Conclusión

`J11-S1-06` queda completada. El siguiente paso permitido es `J11-S1-07`: pruebas integrales, evidencias y cierre del Sprint 1.
