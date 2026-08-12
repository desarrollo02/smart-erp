# Construcción de la imagen Docker

- Fecha: 2026-08-05
- Estado: `J11-S8-C07` validado con `reference_data` completo y Docker/Compose verde
- Plataforma aprobada: `linux/amd64`

## Prerrequisitos

- Docker Engine accesible mediante el contexto `desktop-linux`.
- Docker Buildx y BuildKit operativos.
- Ejecutar los comandos desde `C:\cosme\smart-erp`.
- Acceso a Docker Hub, Quay y Maven Central durante el primer build.
- No se requiere Java ni Maven del host para construir la imagen.

Comprobación inicial:

```powershell
docker version
docker compose version
docker buildx version
docker context show
```

## Analizar el Dockerfile

```powershell
docker buildx build --check --platform linux/amd64 `
  --file infra/docker/Dockerfile .
```

Resultado esperado: `Check complete, no warnings found.`

## Probar el builder desde cero

```powershell
docker buildx build --pull --no-cache --platform linux/amd64 `
  --target builder --load `
  --tag logixone/builder:j11-s1-03 `
  --file infra/docker/Dockerfile .
```

El modo predeterminado `LOGIXONE_BUILD_MODE=verified` ejecuta Maven Wrapper 3.9.16
sobre Java 21 con `verify` y comprueba que el WAR existe. El modo excepcional
`visual-candidate` usa `-DskipTests package` y solo estuvo autorizado para construir
la candidata antes del gate acumulado de Sprint 3; no sirve para cerrar ni promover
una imagen.

Las dependencias se guardan en una caché BuildKit montada como `/workspace/.tools`; el montaje no queda incorporado en la imagen. `.dockerignore` impide copiar `.tools` o `target` desde el host.

Antes de ejecutar Maven, el builder normaliza los archivos fuente y recursos a
`0644` y restaura `mvnw` a `0755`. Docker Desktop puede presentar como ejecutables
archivos ordinarios del contexto Windows; sin esta normalización, XHTML, CSS y XML
quedan con atributos ZIP distintos al mismo artefacto construido en Linux/Windows y
rompen la reproducibilidad por SHA-256 aunque su contenido sea idéntico. Los scripts
del runtime reciben sus modos mediante `COPY --chmod` independiente.

## Construir el runtime

```powershell
docker buildx build --pull --platform linux/amd64 `
  --target runtime --load `
  --tag logixone/app:j11-s2-04 `
  --file infra/docker/Dockerfile .
```

La variante predeterminada usa `LOGIXONE_MAVEN_PROFILE=none` y no contiene el plugin de referencia. Para construir la variante de composición de `J11-S1-05`:

```powershell
docker buildx build --platform linux/amd64 `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-reference-plugin `
  --target runtime --load `
  --tag logixone/app:j11-s2-04-reference `
  --file infra/docker/Dockerfile .
```

Desde `J11-S2-07`, la variante que certifica el funcional y las personalizaciones A/B se construye con:

```powershell
docker buildx build --platform linux/amd64 `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-screen-customization-plugins `
  --target runtime --load `
  --tag logixone/app:j11-s2-07-screens `
  --file infra/docker/Dockerfile .
```

Desde `J11-S6-06`, la demo del primer plugin productivo usa una única composición
para WAR y migrador:

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-business-partners-demo `
  --tag logixone/app:j11-s6-06-business-partners-demo `
  --file infra/docker/Dockerfile .
```

La candidata vigente J11-S8-C03 agrega cuatro plugins productivos con:

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/app:j11-s8-06-inventory-demo `
  --file infra/docker/Dockerfile .
```

`LOGIXONE_MAVEN_PROFILE` acepta exclusivamente `none`, `with-reference-plugin`,
`with-screen-customization-plugins`, `with-business-partners-demo`,
`with-commercial-catalog-demo`, `with-inventory-demo` y `with-purchasing-demo`;
`LOGIXONE_BUILD_MODE` acepta `verified` y `visual-candidate`. Cualquier combinación
no declarada detiene el builder con código 64. No usar estos argumentos para
configuración del entorno ni secretos. La imagen que se pretenda promover debe
construirse siempre en modo `verified`.

Desde `J11-S5-01`, construir la imagen del migrador con el mismo perfil exacto. Por
ejemplo, para la composición A/B:

```powershell
docker buildx build --platform linux/amd64 `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-screen-customization-plugins `
  --target runtime --load `
  --tag logixone/migrator:j11-s5-01-screens `
  --file infra/docker/Dockerfile.migrator .
```

Para Socios Comerciales, cambiar perfil y tag en el mismo comando:

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-business-partners-demo `
  --tag logixone/migrator:j11-s6-06-business-partners-demo `
  --file infra/docker/Dockerfile.migrator .
```

Para la candidata vigente, el migrador correspondiente es:

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/migrator:j11-s8-06-inventory-demo `
  --file infra/docker/Dockerfile.migrator .
```

Desde J11-S8-C03 el mismo perfil incorpora también `reference_data` y su API. Para
evitar confundir la imagen anterior con el nuevo baseline candidato, use etiquetas
específicas del corte para ambos artefactos:

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/app:j11-s8-c03-reference-data `
  --file infra/docker/Dockerfile .

docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/migrator:j11-s8-c03-reference-data `
  --file infra/docker/Dockerfile.migrator .
```

La imagen de aplicación y la del migrador son una pareja inseparable de release:
registrar ambos digests y no combinar imágenes construidas con perfiles distintos.

La pareja final validada de J11-S8-C07 usa el mismo perfil y las etiquetas
específicas siguientes:

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/app:j11-s8-c07-reference-data `
  --file infra/docker/Dockerfile .

docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/migrator:j11-s8-c07-reference-data `
  --file infra/docker/Dockerfile.migrator .
```

Resultados inspeccionados para `linux/amd64`:

- app: `sha256:52cf22451dc7ff89192a9b88d89e97b26b0e45f508654d67c52b6fd38b83d9fd`,
  501.161.623 bytes, usuario `jboss`;
- migrator: `sha256:1b598fb140659a04501a5890c2279c80545cf0115eba0711ef37a30cfdf19c77`,
  105.478.277 bytes, usuario `10001:10001`.

Son digests locales del baseline validado, no evidencia de publicación en un
registro. No promover ni reconstruir bajo la misma etiqueta.

La pareja candidata J11-S9-06 agrega Compras y debe construirse con:

```powershell
docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo `
  --tag logixone/app:j11-s9-06-purchasing-demo-r5 `
  --file infra/docker/Dockerfile .

docker build --platform linux/amd64 `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-purchasing-demo `
  --tag logixone/migrator:j11-s9-06-purchasing-demo `
  --file infra/docker/Dockerfile.migrator .
```

Identidades locales verificadas el 2026-08-12:

- app: `sha256:4e7e84da913b64ae08cdd72188640af5a023e824db67dfb0aecdc2d40c38fba8`,
  501.507.736 bytes;
- migrator: `sha256:7a03dca088e04b79b7e83c6568b982f2b5f728695ed3de800e1dbd8a0f4fcef8`.

Estas identidades son locales, no referencias publicadas en un registro.

El builder toma la versión de pgJDBC de `postgresql.jdbc.version` en el POM padre. El stage runtime instala ese JAR como módulo WildFly y ejecuta `infra/wildfly/configure-runtime.cli` al construir la imagen. El CLI guarda expresiones externas, no valores de entorno ni secretos.

## Inspección mínima

```powershell
docker image inspect logixone/app:j11-s2-04

docker run --rm --platform linux/amd64 --entrypoint sh `
  logixone/app:j11-s2-04 `
  -c 'id; sha256sum /opt/jboss/wildfly/standalone/deployments/logixone.war; test -s /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/postgresql.jar; test ! -e /workspace'
```

Resultado esperado:

- usuario `jboss`, UID 1000;
- WAR presente bajo `standalone/deployments`;
- SHA-256 del WAR registrado para la variante construida;
- módulo pgJDBC presente en el servidor y ausente de `WEB-INF/lib`;
- `/workspace` ausente.

## Smoke test temporal

La aplicación exige secretos PostgreSQL y OIDC válidos, además de provider, cliente y logout, antes de iniciar WildFly. Un `docker run` aislado sin esos archivos debe terminar con código 78; no constituye un smoke de liveness válido. La prueba oficial se realiza mediante Compose, que aporta PostgreSQL, Keycloak, migración previa, redes, variables y secretos por archivo:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up --detach --wait

.\mvnw.cmd -B -pl tests/integration-tests `
  "-Dlogixone.base-uri=http://127.0.0.1:8080" verify
```

Los logs deberán contener `WFLYSRV0010` para `logixone.war`, `WFLYSRV0025` para
WildFly 41, el binding de `LogixoneCoreDS`, las dos fases de `logixone-core-pu` y la
inicialización/omisión controlada del bootstrap. Readiness debe responder `200 UP`
después de V6 y con `oidc-configuration=UP`. `J11-S3-08` construyó en modo
`verified`, comprobó ambos Dockerfiles con `buildx --check`, inspeccionó el WAR y
arrancó la composición completa sin advertencias de BuildKit.

`J11-S6-06` repitió ambos checks, construyó la pareja con el perfil de Socios
Comerciales, registró los IDs de imagen, ejecutó el migrador dos veces y recreó
solamente `app` conservando volúmenes, activaciones y datos.

## Integridad y promoción

- No reemplazar digests por etiquetas flotantes.
- Un cambio de base o digest obliga a repetir análisis, builder limpio, inspección y smoke.
- El digest superior puede incluir una atestación específica del build. Publicar una vez, registrar el digest del registro y promover exactamente ese digest sin reconstruir.
- No publicar `logixone/builder`; solo es una imagen local de prueba.

## Diagnóstico

- `Failed to validate Maven distribution SHA-256`: comprobar que `unzip-with-jar` está presente y que no se cambió el checksum del Wrapper.
- `Permission denied` en `bin/mvn`: comprobar que el adaptador mantiene los permisos de los tres lanzadores Unix.
- `cannot connect to docker API`: iniciar Docker Desktop y repetir el gate del Engine.
- Cambio inesperado del WAR: ejecutar Maven en host y builder, comparar entradas antes de aceptar el nuevo hash.
