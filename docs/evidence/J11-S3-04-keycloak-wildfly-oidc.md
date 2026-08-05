# Evidencia J11-S3-04 — Keycloak y WildFly OIDC

- Fecha: 2026-07-28
- Estado: Completada; G4/G5 verdes en [J11-S3-08](J11-S3-08-validacion-demo-cierre.md)
- Política aplicada: no se ejecutaron pruebas automatizadas antes de la demo visual

## Baseline externo fijado

Consulta ejecutada contra el registro oficial:

```text
docker buildx imagetools inspect quay.io/keycloak/keycloak:26.7.0
```

Resultado observado:

| Dato | Valor |
|---|---|
| Índice OCI | `sha256:0f198be292568439d700cdbfb893e69a6009bb43a94a06a945b1d3d506c76b13` |
| Manifiesto `linux/amd64` | `sha256:26939e1318d6f008fc2ee6e10cec1cf8f1ba8a21846c1bc81b91ed0506bc2a7a` |
| Manifiesto `linux/arm64` | `sha256:043c86f180b45ff61331a9b7ad8ba9eb713b7862685a54a2c8595314bd6c9f18` |
| Manifiesto `linux/ppc64le` | `sha256:13aa8d61e9f42dc0915234fcae3245fd8fe1bce5ddcfafb4ff9148246cfbd27e` |

Compose usa la etiqueta legible `26.7.0`, el digest ejecutable `linux/amd64` y
`platform: linux/amd64`. No utiliza `latest` ni depende de resolver otra
arquitectura al arrancar.

Keycloak se mantiene como servicio externo bajo Apache License 2.0. Origen,
limitaciones y operación están documentados en
[`infra/keycloak/README.md`](../../infra/keycloak/README.md) y en el
[runbook OIDC](../runbooks/keycloak-oidc.md).

## Artefactos implementados

### Infraestructura

- `infra/compose/compose.yaml`: servicio Keycloak, volumen, red, secretos, health y orden de arranque.
- `infra/compose/compose.env.example`: contrato no sensible de puertos, URLs, archivos y bootstrap.
- `infra/keycloak/import/logixone-realm.json`: realm/cliente con tres identidades ficticias y sin credenciales literales.
- `infra/keycloak/keycloak-entrypoint.sh`: lectura cerrada de archivos de secreto.
- `infra/keycloak/README.md`: versión, digest, licencia, persistencia y límites productivos.

### Aplicación y WildFly

- `infra/wildfly/configure-runtime.cli`: `secure-deployment=logixone.war` con expresiones externas.
- `infra/docker/logixone-entrypoint.sh`: validación local y carga de secretos sin imprimirlos.
- `distribution/logixone-war/src/main/webapp/WEB-INF/web.xml`: restricciones `/app/*` y `/api/*`.
- `web-shell/.../ProtectedProbeResource.java`: objetivo mínimo para distinguir REST `401`.
- `kernel-infrastructure-jakarta/.../OidcConfigurationReadinessCheck.java`: validación local sin llamada externa.
- `kernel-infrastructure-jakarta/.../ConfiguredSecurityBootstrap.java`: bootstrap cerrado, idempotente y posterior a migraciones por el orden Compose.

No se agregó dependencia `org.keycloak` ni un adaptador propietario. El único
`javax.*` detectado en los módulos inspeccionados es el tipo Java SE preexistente
`javax.sql.DataSource`; no se introdujo una API Jakarta antigua.

## Seguridad materializada

- El realm activa Code Flow y cliente confidencial. La validación visual de
  `J11-S3-08` retiró la exigencia PKCE porque el modelo administrado de
  `elytron-oidc-client` de WildFly 41 no emite `code_challenge`; se conservan
  secreto externo, redirects exactos, RS256, audience, issuer/expiración y
  rotación de sesión.
- Implicit Flow, Direct Access Grants y Service Account están deshabilitados.
- Un mapper agrega la audience exacta del cliente al access token.
- WildFly exige audience, usa `RS256`, provider discovery, cliente no público y
  verificación normal de TLS/hostname.
- Issuer, firma y expiración quedan en la validación estándar del mecanismo OIDC;
  `verify-token-audience=true` agrega la comprobación explícita de audience.
- `use-resource-role-mappings=false`: los roles funcionales permanecen en `core`.
- `autodetect-bearer-only=true`: la matriz deberá confirmar `401` REST y redirect web.
- Undertow mantiene `proxy-address-forwarding=false` por defecto y solo lo activa por variable externa detrás de un proxy confiable.
- Liveness no cambió. Readiness solo inspecciona el entorno local y no llama al IdP.
- Ningún log nuevo incluye issuer, subject, display name, password, secreto o token.

## Bootstrap y orden

`app` depende simultáneamente de:

1. PostgreSQL saludable;
2. migrator terminado correctamente;
3. Keycloak saludable.

Después de iniciar CDI, `ConfiguredSecurityBootstrap` lee la declaración externa.
`false` la omite; cualquier otro valor distinto de `true`/`false` aborta. Cuando
está habilitada, todos los campos exactos son obligatorios salvo el display name.
El issuer se reutiliza desde el provider URL. Un resultado `REJECTED` o una
excepción transaccional se vuelve fallo de despliegue, no disponibilidad parcial.

No existe ruta web para invocarlo. La empresa debe existir y coincidir con la
personalización antes de habilitarlo.

## Persistencia declarada

| Estado | Volumen | Comportamiento al recrear sin `--volumes` |
|---|---|---|
| ERP/PostgreSQL | `postgres-data` | conserva tablas, migraciones y datos |
| Keycloak demo | `keycloak-data` | conserva realm, cliente y administración |

Keycloak ejecuta `--import-realm`, pero omite un realm existente. Esta semántica
evita pisar el estado en cada arranque. También implica que cambiar JSON/secreto no
rota un cliente ya persistido; esa operación debe ser administrativa y explícita.

## Validaciones ejecutadas

### Gate documental G0

Se recorrieron los Markdown excluyendo `.git`, `.tools` y `target`, con decodificación
UTF-8 estricta, detección de caracteres dañados y resolución de enlaces locales desde
cada archivo de origen.

```text
MARKDOWN_FILES=88 LOCAL_LINKS=256 BROKEN_LINKS=0 BAD_ENCODING=0
```

Resultado: G0 correcto para el alcance de la historia.

### Compose estático

```text
docker compose --env-file infra\compose\compose.env.example \
  -f infra\compose\compose.yaml config --quiet
```

Resultado: código `0`. Docker informó únicamente que el sandbox no podía leer el
archivo de configuración del perfil de usuario; el modelo Compose se procesó sin
error.

### Sintaxis de recursos

```text
Get-Content infra\keycloak\import\logixone-realm.json -Raw | ConvertFrom-Json
$webXml = [xml] (Get-Content distribution\logixone-war\src\main\webapp\WEB-INF\web.xml -Raw)
```

Resultado: `JSON_AND_XML_OK`.

### Compilación y empaquetado

```text
JAVA_HOME=.tools/jdk/jdk-21.0.11+10
mvnw.cmd -B -DskipTests \
  -pl kernel-infrastructure-jakarta,web-shell,distribution/logixone-war \
  -am package
```

Resultado:

- nueve proyectos del reactor: `SUCCESS`;
- `BUILD SUCCESS`;
- tiempo Maven: `28.278 s`;
- WAR: `distribution/logixone-war/target/logixone.war`;
- cada módulo informó `Tests are skipped`.

Un primer intento usó accidentalmente Java 8 del sistema y el Enforcer lo rechazó
antes de compilar. Se corrigió usando el JDK 21 validado bajo `.tools/`; no se relajó
ninguna regla.

### Contenido del WAR

`jar tf` confirmó `WEB-INF/web.xml`. La búsqueda en POM y fuentes no encontró
`org.keycloak` ni artefactos de adaptador. Los recursos JAX-RS se incluyen dentro de
`WEB-INF/lib/web-shell-0.1.0-SNAPSHOT.jar`.

### Validación de CLI WildFly pendiente

Se intentó ejecutar la CLI dentro de la imagen WildFly fijada mediante un contenedor
efímero. Docker Desktop estaba detenido y el daemon respondió que no existía
`dockerDesktopLinuxEngine`. No se modificó estado ni se obtuvo una validación falsa.
La CLI se contrastó con el modelo y Admin Guide oficiales de WildFly 41, pero la
ejecución real queda obligatoriamente en G4/G5 de `J11-S3-08`.

## Trazabilidad de aceptación

| CA | Evidencia candidata | Validación restante |
|---|---|---|
| CA-01 | versión, plataforma y digest exactos en Compose/README | pull/inspect en gate limpio |
| CA-02 | licencia, origen, límites y runbook documentados | revisión independiente |
| CA-03 | POM/WAR sin adaptador Keycloak | ArchUnit/dependency tree acumulado |
| CA-04 | provider URL como expresión externa | lectura runtime del modelo |
| CA-05 | RS256, discovery y audience configurados | tokens negativos reales |
| CA-06 | constraints y autodetección REST | browser/REST runtime |
| CA-07 | tres archivos de secretos y entrypoints cerrados | inspección de imagen/logs/runtime |
| CA-08 | realm con placeholders y `users: []` | importación real |
| CA-09 | redirect/origin/logout/hostname externos | proxy/redirect por ambientes |
| CA-10 | código health sin llamada externa | caída controlada de Keycloak |
| CA-11 | entrypoint y check local fallan cerrado | casos de entorno inválido |
| CA-12 | orden Compose y adaptador transaccional | bootstrap exacto/incompatible |
| CA-13 | dos volúmenes y semántica de importación | `down`/recreate con datos |
| CA-14 | matriz documentada en historia/runbook | ejecución completa S3-08 |

## Fuentes oficiales consultadas

- [WildFly 41 Admin Guide](https://docs.wildfly.org/41/Admin_Guide.html)
- [WildFly 41 `secure-deployment`](https://docs.wildfly.org/41/feature-pack/doc/reference/subsystem/elytron-oidc-client/secure-deployment/index.html)
- [Keycloak — import/export](https://www.keycloak.org/server/importExport)
- [Keycloak — contenedores](https://www.keycloak.org/server/containers)
- [Keycloak 26.7.0 — notas](https://www.keycloak.org/docs/26.7.0/release_notes/)

## Conclusión

La candidata cumple G0 y G1. No se declara G4/G5 ni ninguna prueba verde. El estado
correcto es `Implementada pendiente de validación`, y el siguiente trabajo autorizado
es `J11-S3-05`.
