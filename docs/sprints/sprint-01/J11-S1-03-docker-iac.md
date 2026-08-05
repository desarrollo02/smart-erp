# J11-S1-03 — Docker e infraestructura como código

- Fecha de inicio: 2026-07-23
- Estado: Completada
- Tipo: Infraestructura, build y operación
- Dependencia: `J11-S1-02` completada
- Incremento actual: séptimo incremento completado; 21 de 21 criterios y gates G0, G2 y G4 verdes

## Objetivo

Construir y verificar una infraestructura reproducible para Logixone Jakarta 11 mediante Docker e infraestructura como código. La misma imagen de aplicación deberá poder promoverse por digest entre desarrollo, pruebas y producción, variando únicamente la configuración externa permitida.

## Estado inicial verificado

- El reactor Maven está verde y genera un WAR reproducible.
- No existen todavía `Dockerfile`, archivos Compose ni lógica de migración ejecutable.
- `.dockerignore` ya excluye `.tools/`, resultados Maven, VCS, configuración local y secretos.
- Docker CLI, Compose y Buildx están instalados.
- El contexto activo es `desktop-linux`, pero Docker Engine no está disponible porque Docker Desktop está detenido.
- No se descargaron ni construyeron imágenes durante el diagnóstico.

La evidencia completa está en [J11-S1-03 — Diagnóstico inicial de Docker](../../evidence/J11-S1-03-diagnostico-docker.md).

## Alcance

- Dockerfile multi-stage para construir el reactor con Java 21 y Maven Wrapper.
- Imagen runtime basada en WildFly 41 con el WAR desplegado.
- Imágenes base con etiqueta legible y digest SHA-256 inmutable.
- Compose con PostgreSQL, migrador one-shot y aplicación.
- Configuración externa, ejemplo de variables sin secretos y volumen persistente.
- Orden de arranque, health checks, smoke tests y casos negativos.
- Registro verificable del digest de la imagen final.
- Runbooks y evidencias suficientes para reproducir construcción, arranque y recuperación.

## Fuera de alcance

- Contratos y grafo de plugins de `J11-S1-04`.
- Descubrimiento CDI y plugin funcional de `J11-S1-05`.
- Endpoints semánticos Jakarta de liveness y readiness de `J11-S1-06`.
- Despliegue real en producción, publicación en un registro OCI o gestión definitiva de secretos de producción.
- Kubernetes, instalación dinámica de plugins y escalado horizontal.

En esta historia el health check de la aplicación probará que WildFly completó el despliegue del WAR y responde por HTTP. La disponibilidad de la composición exigirá además migración exitosa y PostgreSQL saludable. `J11-S1-06` reemplazará o ampliará este control con endpoints semánticos de aplicación sin cambiar la topología.

## Decisiones heredadas

- Java 21, Jakarta EE 11 y WildFly 41 permanecen fijos.
- Se construye una única imagen de aplicación y se promueve exactamente el mismo digest.
- Desarrollo, pruebas y producción conservan la misma topología lógica.
- Las bases se expresan como `etiqueta@sha256:digest`; no se admite `latest`.
- PostgreSQL persiste en un volumen explícito.
- Los secretos no se versionan ni se incorporan a imágenes.
- Todo archivo descargado por herramientas del proyecto se conserva bajo `.tools/`; las capas OCI permanecen en el almacenamiento administrado por Docker y sus identidades se documentan por digest.

## Criterios de aceptación formales

### Diagnóstico y reproducibilidad

- **CA-01:** las versiones de Docker CLI, Engine, Compose y Buildx, el contexto activo y los prerrequisitos quedan registrados sin secretos.
- **CA-02:** el build parte de un checkout limpio y no depende de `target/`, `.tools/`, un Maven global ni recursos manuales no documentados.
- **CA-03:** `mvnw.cmd verify` continúa verde fuera del contenedor después de cada corte coherente que afecte el build.

### Imágenes y Dockerfile

- **CA-04:** el Dockerfile es multi-stage; la etapa builder usa Java 21 y Maven Wrapper, ejecuta `verify` y produce el WAR.
- **CA-05:** la etapa runtime usa WildFly 41 con Java 21, conserva el usuario no privilegiado de la base e incorpora únicamente el runtime necesario.
- **CA-06:** toda imagen base ejecutable está fijada como `etiqueta@sha256:digest`; no existen `latest` ni referencias flotantes.
- **CA-07:** origen, etiqueta, digest, plataforma y fecha de verificación de cada base quedan documentados.
- **CA-08:** `.dockerignore` evita enviar `.tools/`, `.git/`, `target/`, documentación innecesaria, configuración local y secretos al contexto.

### Compose, migraciones y salud

- **CA-09:** `docker compose config` valida una topología declarada con `postgres`, `migrator` y `app`.
- **CA-10:** PostgreSQL usa versión mayor y digest explícitos, health check y volumen nombrado persistente.
- **CA-11:** el migrador es un proceso one-shot, termina con código cero al aplicar o validar migraciones y puede ejecutarse nuevamente sin cambios destructivos.
- **CA-12:** `app` no puede arrancar o declararse disponible antes de que PostgreSQL esté saludable y el migrador haya terminado correctamente.
- **CA-13:** una migración fallida impide el arranque disponible de `app` y produce un error observable sin exponer secretos.
- **CA-14:** la aplicación pasa un smoke test HTTP y un health check operativo después del despliegue del WAR.
- **CA-15:** recrear `app` conserva los datos; recrear PostgreSQL con el mismo volumen conserva el estado migrado.

### Configuración, seguridad y promoción

- **CA-16:** configuración y credenciales se reciben externamente; el repositorio solo incluye nombres y valores de ejemplo no sensibles.
- **CA-17:** una inspección de Dockerfile, Compose, historial y configuración renderizada no encuentra secretos ni contenido de `.tools/`.
- **CA-18:** la imagen final queda identificada por ID y digest; los ambientes referencian el mismo artefacto sin reconstrucción.

### Evidencia y cierre

- **CA-19:** cada comando ejecutado registra fecha, ambiente, código de salida y resultado resumido.
- **CA-20:** G0, G2 y G4 quedan verdes, con build limpio, Compose válido, migración, arranque, health, smoke y persistencia verificados.
- **CA-21:** los runbooks documentan build, arranque, diagnóstico, parada, recreación y recuperación básica.

## Matriz de pruebas prevista

| Corte | Prueba inmediata | Resultado requerido |
|---|---|---|
| Selección de bases | Inspección de referencias y digests | Etiquetas y digests válidos, sin referencias flotantes |
| Dockerfile builder | Build limpio del target builder | Reactor 14/14 y WAR generado |
| Dockerfile runtime | Inspección de imagen e historial | WildFly 41, Java 21, usuario no privilegiado y sin secretos |
| Compose inicial | `docker compose config --quiet` | Configuración válida |
| PostgreSQL | Health check y consulta de versión | Servicio saludable y versión esperada |
| Migrador | Primera y segunda ejecución | Éxito e idempotencia |
| Orden negativo | Migración forzada a fallar | `app` no disponible |
| Aplicación | `docker compose up --wait` y smoke HTTP | Despliegue saludable y respuesta HTTP |
| Persistencia | Recreación sin eliminar volumen | Datos y estado migrado conservados |
| Promoción | Inspección de ID y digest | Misma identidad en la matriz de ambientes |
| Cierre | Maven `verify`, G0 y G4 | Todos los controles verdes |

## Incrementos planificados

1. Diagnosticar Docker y formalizar criterios de aceptación.
2. Iniciar Docker Engine, resolver imágenes candidatas y fijar sus digests.
3. Implementar y probar el Dockerfile multi-stage.
4. Declarar y validar Compose, configuración externa y PostgreSQL persistente.
5. Implementar el migrador mínimo y probar orden, idempotencia y fallo seguro.
6. Probar arranque, salud, smoke, persistencia e identidad de imagen.
7. Completar runbooks, evidencia y cierre de la historia.

No se inicia un incremento posterior con una prueba relevante fallando.

## Definition of Done de la historia

La historia solo podrá marcarse como completada cuando los 21 criterios estén demostrados, no existan pruebas omitidas sin justificación, Maven continúe verde y la evidencia permita reproducir la infraestructura desde un checkout limpio.

## Resultado del incremento inicial

- Diagnóstico completado.
- Criterios de aceptación definidos: 21.
- Historia abierta y Sprint actualizado.
- El diagnóstico inicial encontró Docker Desktop detenido; el segundo incremento confirmó que el Engine quedó operativo.
- Baseline de aplicación preservado; no hubo cambios de código ni descargas.

## Resultado del segundo incremento

- Docker Engine 29.6.2, Compose 5.3.1 y BuildKit 0.31.2 quedaron operativos sobre `linux/amd64`.
- Se fijó `linux/amd64` como plataforma de Sprint 1 para promover exactamente los mismos manifiestos entre ambientes.
- Se seleccionaron y verificaron 4/4 bases por etiqueta, digest de índice y digest de plataforma.
- PostgreSQL quedó fijado en 18.4 Bookworm; su layout persistente específico de PostgreSQL 18 deberá reflejarse en Compose.
- No se descargaron capas, no se crearon contenedores y no se modificó el código.
- Evidencia: [Engine e imágenes base fijadas por digest](../../evidence/J11-S1-03-engine-imagenes-base.md).

## Resultado del tercer incremento

- Se crearon `infra/docker/Dockerfile` y `infra/docker/unzip-with-jar`.
- El análisis nativo de BuildKit terminó sin advertencias.
- El target builder ejecutó Maven Wrapper 3.9.16 con Java 21.0.11 y completó 14/14 proyectos.
- Se corrigió la reproducibilidad entre Windows y Linux eliminando descriptores Maven generados con finales de línea dependientes del sistema.
- El WAR canónico actual es `23C935CEB30AB75CCDD72A9CF96658E6A0B63B50B4A552AE908D7B7F1488BD39` en ambos sistemas.
- El runtime WildFly ejecuta como `jboss`/UID 1000, contiene únicamente el WAR añadido y no conserva el workspace ni cachés del builder.
- El smoke efímero desplegó `logixone.war`, inició WildFly 41, respondió HTTP y terminó con cero errores; el contenedor fue eliminado.
- El manifiesto y la configuración runtime fueron estables entre builds. El índice superior cambió únicamente por la nueva atestación BuildKit; se mantiene la regla de promover un solo digest sin reconstruir.
- Evidencia: [Dockerfile multi-stage](../../evidence/J11-S1-03-dockerfile-multistage.md).

Criterios demostrados en este corte: `CA-02`, `CA-03`, `CA-04`, `CA-05`, `CA-06`, `CA-07`, `CA-08`, la porción operativa disponible de `CA-14`, `CA-17` para Dockerfile/historial y `CA-19`. Los criterios de Compose, migración, persistencia y promoción en registro permanecen abiertos.

## Resultado del cuarto incremento

- Se crearon `infra/compose/compose.yaml` y `infra/compose/compose.env.example`.
- La topología contiene exactamente `postgres`, `migrator` y `app`, todos sobre `linux/amd64`.
- PostgreSQL 18.4 conserva la referencia aprobada por digest, health check y volumen `postgres-data` en `/var/lib/postgresql`.
- El migrador quedó declarado como one-shot después de PostgreSQL saludable; `app` exige además que el migrador termine correctamente.
- Credenciales y configuración se inyectan externamente. El repositorio no contiene contraseña y el secreto real se resolverá bajo `.tools/secrets/`.
- El health check temporal de `app` verifica respuesta HTTP de WildFly; J11-S1-06 añadirá semántica de liveness/readiness.
- Los dos renderizados de Compose y 18 aserciones estructurales quedaron verdes, incluida la sustitución exacta por referencias `@sha256:digest`.
- El gate final Maven rechazó el Java 8 heredado y pasó 14/14 al seleccionar el JDK 21 ya contenido en `.tools/`; el WAR conservó su SHA-256 canónico.
- No se arrancó la composición: quedaron 0 contenedores, 0 volúmenes y 0 redes del proyecto.
- Evidencia: [Validación estática de Compose](../../evidence/J11-S1-03-compose-estatico.md).

Criterios demostrados en este corte: `CA-09`, la parte declarativa de `CA-10` y `CA-12`, `CA-16`, la porción Compose de `CA-17` y `CA-19`. La ejecución, migración, fallo seguro y persistencia permanecen abiertos.

## Punto de entrada del quinto incremento

Implementar y probar la imagen mínima del migrador one-shot. Primero deberá pasar su build y controles aislados; luego se repetirá la validación estática y recién entonces se arrancará PostgreSQL para probar migración, idempotencia y fallo seguro.

## Plan del quinto incremento

El incremento se ejecutará en cortes pequeños y detendrá su avance ante cualquier prueba relevante fallando:

1. fijar centralmente Flyway `12.8.1`, pgJDBC `42.7.10`, JUnit `5.14.2` y Maven Shade `3.6.2`;
2. implementar el contrato de configuración `LOGIXONE_DB_URL`, `LOGIXONE_DB_USER` y `LOGIXONE_DB_PASSWORD_FILE`, sin aceptar ni registrar contraseñas en variables;
3. incorporar una primera migración SQL inmutable para el esquema `core`, con historial Flyway dentro del mismo esquema y `clean` deshabilitado;
4. producir un JAR ejecutable, probar configuración, salida segura, ejecución exitosa y fallo controlado;
5. construir una imagen multi-stage sobre el JRE Temurin ya aprobado, ejecutada con UID/GID no privilegiados;
6. revalidar Compose antes de crear recursos;
7. crear un secreto local ignorado bajo `.tools/`, arrancar PostgreSQL y probar primera ejecución, segunda ejecución idempotente y migración forzada a fallar;
8. demostrar que el fallo impide habilitar `app`, retirar únicamente los recursos efímeros de prueba y conservar la evidencia sin secretos.

Las dependencias nuevas son necesarias para ejecutar migraciones PostgreSQL (`flyway-core`, `flyway-database-postgresql`, pgJDBC), probarlas con el baseline JUnit 5 y empaquetar el proceso one-shot. Flyway y Shade usan Apache-2.0; pgJDBC usa BSD-2-Clause y JUnit EPL-2.0. Las descargas de Maven permanecerán en `.tools/maven-repository` mediante la configuración existente.

### Controles específicos del incremento

- Una configuración incompleta o un secreto ausente/malformado termina con código distinto de cero sin imprimir su contenido.
- La migración inicial crea únicamente objetos propiedad de `core` y una tabla de historial en ese esquema.
- La segunda ejecución aplica cero migraciones y conserva checksums.
- Modificar una migración ya aplicada provoca fallo observable.
- La imagen contiene el ejecutable y sus dependencias, no el workspace, `.tools/` ni salidas ajenas.
- La imagen ejecuta como usuario no privilegiado y sus bases permanecen fijadas por digest.
- `app` continúa condicionado a `service_completed_successfully` del migrador.

## Resultado del quinto incremento

- Se fijaron Flyway 12.8.1, pgJDBC 42.7.10, JUnit 5.14.2 y Maven Shade 3.6.2.
- El migrador valida URL/usuario/secreto por archivo y usa códigos de salida 0, 1 y 2 sin registrar credenciales.
- V1 creó `core.system_metadata`; Flyway mantiene `core.flyway_schema_history` con checksum `-1098736951` y `clean` deshabilitado.
- Ocho pruebas unitarias quedaron verdes en Windows y dentro del builder Linux.
- El JAR ejecutable conserva servicios y avisos de terceros; SHA-256 `56343D2FD82E42FD53F341BC32155A7122D20C30AC342FC7C9653A778A846091`.
- `infra/docker/Dockerfile.migrator` pasó BuildKit sin advertencias y generó una imagen `linux/amd64` de 104,214,960 bytes como `10001:10001`.
- La primera ejecución aplicó una migración; la segunda aplicó cero y confirmó versión 1.
- Un checksum alterado produjo `FlywayValidateException`, código 1 y cero filtraciones; Compose dejó `app` creado pero nunca en ejecución.
- El checksum se restauró, el migrador volvió a quedar idempotente y Maven terminó 14/14.
- `docker compose down` retiró contenedores y red sin eliminar `logixone_postgres-data`, reservado para la prueba de persistencia.
- Evidencia: [Migrador one-shot y fallo seguro](../../evidence/J11-S1-03-migrator-one-shot.md).

Criterios demostrados en este corte: `CA-03`, `CA-09`, la parte ejecutable de `CA-10`, `CA-11`, `CA-12`, `CA-13`, `CA-16`, `CA-17` y `CA-19`. Persistencia, smoke completo y promoción por digest de registro permanecen abiertos.

## Siguiente paso permitido

Iniciar el sexto incremento: recrear PostgreSQL con el volumen preservado, confirmar V1 y `system_metadata`, ejecutar el migrador sin cambios, arrancar `app` y probar health, smoke, recreación y conservación de datos.

## Inicio del sexto incremento

El sexto incremento comenzó el 2026-07-23. Su ejecución se divide en cortes pequeños y cada corte debe quedar verde antes de continuar:

1. validar Docker Engine, Compose, las imágenes locales, el secreto ignorado y el volumen `logixone_postgres-data`;
2. validar la configuración Compose antes de crear recursos;
3. recrear PostgreSQL con el volumen conservado y confirmar V1, checksum y `core.system_metadata`;
4. ejecutar el migrador y exigir código `0`, `migrations_executed=0` y `schema_version=1`;
5. arrancar `app`, esperar el health check y ejecutar el smoke HTTP;
6. recrear `app` y PostgreSQL sin eliminar el volumen y repetir las verificaciones;
7. auditar logs, configuración e imágenes para evitar secretos y confirmar identidades;
8. ejecutar `mvnw.cmd verify`, actualizar runbooks y cerrar la evidencia.

La evidencia incremental se registra en [Persistencia, health y smoke](../../evidence/J11-S1-03-persistencia-smoke.md).

## Resultado del sexto incremento

- PostgreSQL 18.4 se recuperó desde `logixone_postgres-data` y conservó V1, checksum `-1098736951` y `schema_owner=core`.
- El migrador terminó repetidamente con código `0`, cero migraciones nuevas y versión `1`.
- Se detectó que una aplicación conectada únicamente a una red Docker interna no publicaba su puerto en Docker Desktop.
- Compose conserva `backend` interna para los tres servicios y agrega `edge` únicamente a `app`; PostgreSQL y migrator permanecen sin puertos publicados.
- `app` quedó saludable, WildFly desplegó `logixone.war`, `/` respondió `200` y `/logixone/` respondió `403` como smoke de transporte.
- Se recrearon `app` y PostgreSQL sin pérdida de datos; el contenedor PostgreSQL cambió y el volumen permaneció idéntico.
- La auditoría de seguridad e identidad terminó `13/13`; las matrices de desarrollo y prueba conservaron los mismos digests sin reconstrucción.
- Maven Wrapper `3.9.16` con Java `21.0.11` terminó `14/14`; WAR y JAR conservaron sus SHA-256 canónicos.
- `docker compose down` retiró contenedores y redes sin eliminar `logixone_postgres-data`.

Criterios demostrados o completados en este corte: `CA-10`, `CA-14`, `CA-15`, `CA-16`, `CA-17`, `CA-18` y `CA-19`. El séptimo incremento consolidará runbooks, repetirá gates de cierre y resolverá formalmente `CA-20` y `CA-21` antes de completar la historia.

## Siguiente paso permitido después del sexto incremento

Iniciar el séptimo incremento de `J11-S1-03`: auditar los 21 criterios, completar recuperación y runbooks pendientes, repetir G0/G2/G4 y cerrar formalmente la historia sin adelantar `J11-S1-04`.

## Inicio del séptimo incremento

El cierre formal comenzó el 2026-07-23. La auditoría inicial encontró 19 criterios demostrados y dos pendientes de ejecución final:

- `CA-20`: repetir G0, G2 y G4 como gates de cierre;
- `CA-21`: completar y probar comandos concretos de backup, restauración y recuperación básica.

La evidencia del cierre y la matriz `CA-01` a `CA-21` se registran en [Cierre formal de J11-S1-03](../../evidence/J11-S1-03-cierre.md).

## Resultado del séptimo incremento

- Se agregó y probó el runbook de backup/restauración PostgreSQL en una base temporal independiente.
- El backup custom fue validado por índice, tamaño y SHA-256; fuente y restauración coincidieron en metadata y Flyway V1.
- G2 ejecutó `mvnw.cmd -B clean verify`: 14 de 14 módulos y 8 pruebas exitosas.
- G4 usó app y migrator por digest, sin rebuild: migración idempotente, health, smoke `200/403` y recreación de app/PostgreSQL con el mismo volumen.
- La auditoría final de seguridad e identidad pasó 13 de 13 controles.
- La limpieza dejó cero contenedores y redes Compose y preservó `logixone_postgres-data`.
- G0 final confirmó documentación UTF-8 válida y cero enlaces locales rotos.

Los criterios `CA-01` a `CA-21` y la Definition of Done están cumplidos. El detalle reproducible, incluidos los fallos controlados y sus correcciones, queda en [Cierre formal de J11-S1-03](../../evidence/J11-S1-03-cierre.md).

## Siguiente paso permitido después de J11-S1-03

Iniciar `J11-S1-04`: contratos de plugins y validaciones. Esta historia queda permitida, pero no fue iniciada como parte de `J11-S1-03`.
