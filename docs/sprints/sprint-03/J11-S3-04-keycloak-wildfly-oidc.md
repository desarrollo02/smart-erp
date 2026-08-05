# J11-S3-04 — Keycloak y WildFly OIDC reproducibles

- Estado: Completada
- Dependencia: `J11-S3-03` implementada para la candidata

## Objetivo

Declarar Keycloak como infraestructura externa y proteger el WAR mediante `elytron-oidc-client`, con configuración reproducible, secretos externos y sin adaptadores propietarios empaquetados.

## Alcance

- versión exacta y digest de Keycloak compatibles con el baseline;
- servicio Keycloak en Compose para desarrollo/demo y persistencia explícita cuando corresponda;
- realm y cliente configurados de forma declarativa sin credenciales reales;
- cliente confidencial, redirect URI y logout coherentes con el shell;
- secretos de administrador y cliente por archivos ignorados;
- configuración `provider-url` de WildFly mediante variables externas;
- protección diferenciada de páginas y recursos REST;
- bootstrap one-shot integrado en el orden de infraestructura;
- logs y health sin secretos.

## Fuera de alcance

- empaquetar Keycloak en el WAR o la imagen de aplicación;
- usar `latest`, deshabilitar TLS/hostname en producción o versionar passwords;
- realm por empresa;
- LDAP/Active Directory real, SCIM o MFA administrado por el ERP;
- roles funcionales provenientes del token.

## Criterios de aceptación

- **CA-01:** la imagen Keycloak tiene etiqueta legible y digest ejecutable registrado.
- **CA-02:** la licencia, versión, origen y superficie operativa quedan documentados.
- **CA-03:** el WAR no contiene dependencias de adaptadores Java de Keycloak.
- **CA-04:** `elytron-oidc-client` usa `provider-url` configurable y no una URL codificada en Java.
- **CA-05:** issuer, firma, audience y expiración se validan.
- **CA-06:** páginas protegidas redirigen y REST protegido devuelve `401` sin redirección.
- **CA-07:** secretos solo entran por mecanismos externos y no aparecen en Compose, imagen o logs.
- **CA-08:** el realm declarativo no contiene passwords reales.
- **CA-09:** redirect URI y proxy se configuran por ambiente sin reconstruir la imagen.
- **CA-10:** liveness no consulta Keycloak y readiness no realiza una llamada externa por sondeo.
- **CA-11:** configuración OIDC indispensable inválida impide declarar lista la aplicación.
- **CA-12:** bootstrap se ejecuta después de migraciones y falla cerrado.
- **CA-13:** recrear contenedores sin retirar volúmenes conserva PostgreSQL y el estado previsto de Keycloak.
- **CA-14:** la matriz positiva/negativa OIDC y Compose queda preparada para `J11-S3-08`.

## Gates

- G1: imágenes/configuración suficientes para abrir la candidata, sin declarar validación completa.
- G4/G5 diferidos: runtime OIDC, seguridad negativa, Compose, secretos, persistencia y recreación en `J11-S3-08`.
- G0 documental inmediato.

## Implementación candidata

### Keycloak externo

- Se fijó Keycloak `26.7.0` a la imagen `linux/amd64` con digest ejecutable `sha256:26939e1318d6f008fc2ee6e10cec1cf8f1ba8a21846c1bc81b91ed0506bc2a7a`.
- Compose agrega el servicio `keycloak`, la red interna `identity`, publicación loopback `keycloak.localhost:8180` y el volumen explícito `keycloak-data`.
- El baseline local usa `start-dev` y `dev-file` solo para demo; la limitación productiva está explícita.
- `infra/keycloak/import/logixone-realm.json` declara realm, cliente confidencial, Code Flow, audience, redirects y tres identidades ficticias sin passwords literales.
- `keycloak-entrypoint.sh` recibe autoridad administrativa, secreto de cliente y password de demo desde archivos; no imprime valores.
- La prueba visual de `J11-S3-08` confirmó que el modelo administrado de WildFly 41 no envía PKCE; el realm no lo exige al cliente confidencial y conserva el resto de los controles OIDC documentados.

### WildFly OIDC

- `configure-runtime.cli` crea `secure-deployment=logixone.war` en `elytron-oidc-client` con expresiones externas.
- Se fijan `RS256`, audience obligatoria, cliente no público, verificación normal TLS/hostname y principal `sub`.
- Los roles funcionales no se derivan del token.
- `WEB-INF/web.xml` protege `/app/*` y `/api/*`; health permanece fuera de las restricciones.
- `autodetect-bearer-only` prepara redirect para páginas y `401` para REST; `/api/protected-probe` es el objetivo mínimo de la matriz diferida.
- No se agregó ninguna dependencia Maven de Keycloak al WAR.

### Salud, secretos y bootstrap

- El entrypoint de aplicación valida provider, cliente, logout y los secretos de DB/OIDC antes de iniciar WildFly.
- `OidcConfigurationReadinessCheck` valida solo configuración local; no hace discovery ni llama al proveedor.
- Liveness no cambió y no consulta Keycloak.
- Compose espera migraciones completas y Keycloak saludable antes de `app`.
- `ConfiguredSecurityBootstrap` conecta el puerto interno one-shot con configuración externa, permanece deshabilitado por defecto y aborta el despliegue ante declaración inválida o incompatible.
- El issuer del bootstrap es exactamente `LOGIXONE_OIDC_PROVIDER_URL`; no existe una segunda fuente que pueda divergir.

### Persistencia y recreación

- `docker compose down` conserva `postgres-data` y `keycloak-data`.
- Al recrear, Keycloak reutiliza su almacén y el importador omite un realm ya existente.
- Cambiar el JSON o el archivo del secreto no sobrescribe estado persistido; rotación y cambios posteriores requieren un procedimiento administrativo explícito.
- `down --volumes` continúa prohibido salvo decisión destructiva, backups y verificación del proyecto exacto.

## Matriz preparada para J11-S3-08

| Caso | Resultado requerido |
|---|---|
| Realm nuevo / realm existente | importa una vez / conserva estado sin sobrescribir |
| Página sin sesión | redirige a Keycloak |
| REST sin sesión con `Accept: application/json` | `401`, sin redirección |
| Login y logout válidos | sesión rotada y salida coordinada |
| Issuer, firma, audience o expiración inválidos | acceso denegado |
| Provider/cliente/secreto/logout ausente o inválido | aplicación no disponible/lista |
| Keycloak cae después del arranque | liveness no cae por sondeo; readiness no lo consulta externamente |
| Bootstrap exacto repetido | `UNCHANGED`, sin duplicados |
| Bootstrap incompatible | despliegue abortado y transacción cerrada |
| `down` y recreación | PostgreSQL y Keycloak conservan el estado previsto |

## Resultado de validación ejecutado ahora

- `docker compose ... config --quiet`: código `0` con el archivo de ejemplo.
- Realm JSON y `web.xml`: parseo estático correcto.
- Maven con JDK 21: nueve módulos relevantes empaquetados con `BUILD SUCCESS`; las pruebas aparecen explícitamente como `Tests are skipped`.
- WAR generado con `WEB-INF/web.xml` y sin artefactos Java de Keycloak.
- La comprobación efímera de la CLI dentro de WildFly no pudo iniciarse porque Docker Desktop estaba detenido; no se presenta como superada y pasa al gate runtime de `J11-S3-08`.

La evidencia reproducible queda en [J11-S3-04 — Keycloak y WildFly OIDC](../../evidence/J11-S3-04-keycloak-wildfly-oidc.md).

## Estado provisional aplicado

Se usó `Implementada pendiente de validación` hasta superar G4 y G5.

## Validación acumulada

`J11-S3-08` dejó verdes Compose, persistencia, configuración segura, 4 casos OIDC y
login/logout real. El soporte preview de logout se formalizó en
[ADR-0008](../../adr/0008-logout-oidc-estabilidad-preview-wildfly.md). G4/G5 quedaron
verdes. Evidencia: [gates G2–G6](../../evidence/J11-S3-08-validacion-demo-cierre.md).

## Siguiente paso

La historia está completada. El Sprint continúa con G7 de `J11-S3-08`.
