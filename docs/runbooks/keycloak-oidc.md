# Keycloak y OIDC para desarrollo y demo

- Fecha: 2026-07-28
- Historia: `J11-S3-04` y extensión `J11-S4-04`
- Estado: OIDC empresarial validado en `J11-S3-08`; constraint administrativo implementado pendiente de pruebas
- Alcance: entorno local/demo `linux/amd64`, no producción

## Resultado declarado

Compose incorpora Keycloak como proveedor externo y WildFly protege el WAR con el
subsistema nativo `elytron-oidc-client`. El WAR no contiene un adaptador Java de
Keycloak. La configuración indispensable y los secretos llegan en tiempo de
ejecución; una ausencia o valor local inválido impide que la aplicación se declare
lista.

Baseline fijado:

| Elemento | Referencia |
|---|---|
| Keycloak | `26.7.0` |
| Imagen legible | `quay.io/keycloak/keycloak:26.7.0` |
| Digest ejecutable `linux/amd64` | `sha256:26939e1318d6f008fc2ee6e10cec1cf8f1ba8a21846c1bc81b91ed0506bc2a7a` |
| WildFly | `41.0.0.Final-jdk21` fijado en el Dockerfile |
| Realm | `logixone` |
| Cliente confidencial | `logixone-web` por defecto |
| Issuer local | `http://keycloak.localhost:8180/realms/logixone` |

La licencia de Keycloak es Apache License 2.0. La imagen y su estado operativo no
se incorporan a la imagen de Logixone; se ejecutan como servicio separado. El
detalle de origen, digest y persistencia está en
[`infra/keycloak/README.md`](../../infra/keycloak/README.md).

## Topología y orden de arranque

```text
postgres healthy -> migrator completado -> app
keycloak healthy ------------------------> app
```

- `postgres`, `migrator` y `app` comparten la red interna `backend`.
- `keycloak` y `app` comparten la red interna `identity`.
- `app` y `keycloak` usan `edge` solamente para publicar HTTP en loopback.
- Keycloak escucha en `keycloak.localhost:8180`; el alias existe dentro de Compose
  y `.localhost` resuelve a loopback en el navegador.
- `postgres-data` conserva el ERP y `keycloak-data` conserva el realm local.
- El bootstrap de seguridad del ERP solo puede ejecutarse dentro de `app`, después
  de que el migrador haya terminado correctamente.

## Preparación de secretos

Crear cuatro archivos locales ignorados, cada uno con un valor aleatorio diferente,
no vacío y en una única línea:

```text
.tools/secrets/postgres-password.txt
.tools/secrets/keycloak-admin-password.txt
.tools/secrets/oidc-client-secret.txt
.tools/secrets/demo-user-password.txt
```

No copiar esos valores a `compose.env.local`, al realm JSON, a argumentos, logs o
evidencias. El archivo de administrador crea la autoridad inicial de Keycloak; el
secreto de cliente debe ser el mismo archivo montado en Keycloak y en WildFly.

Copiar `infra/compose/compose.env.example` a `compose.env.local` y ajustar únicamente
valores no sensibles. Si cambia el host o puerto público de la aplicación, actualizar
como una unidad:

- `LOGIXONE_OIDC_REDIRECT_URI`;
- `LOGIXONE_OIDC_WEB_ORIGIN`;
- `LOGIXONE_OIDC_POST_LOGOUT_REDIRECT_URI`.

Si cambia la URL pública de Keycloak, actualizar juntos
`LOGIXONE_KEYCLOAK_PUBLIC_URL` y `LOGIXONE_OIDC_PROVIDER_URL`. Producción exige
HTTPS, hostname verificable y certificados confiables; no se permiten
`allow-any-hostname` ni `disable-trust-manager`.

`LOGIXONE_PROXY_ADDRESS_FORWARDING=false` es el valor seguro directo. Solo cambiarlo
a `true` detrás de un proxy confiable que reemplace y sanee `X-Forwarded-*`; Keycloak
usa por separado `LOGIXONE_KEYCLOAK_PROXY_HEADERS`. Ambas decisiones son externas y
no requieren reconstruir la imagen.

## Realm declarativo

`infra/keycloak/import/logixone-realm.json` declara:

- Authorization Code Flow con cliente confidencial;
- cliente confidencial con Direct Access Grants, Implicit Flow y Service Account
  deshabilitados;
- redirect, origin y post-logout externos;
- audience explícita `logixone-web` en el access token;
- tres identidades ficticias de demo y cero passwords literales versionados.

WildFly 41 protege el WAR mediante el modelo administrado de
`elytron-oidc-client`, que en este baseline no expone la activación de PKCE. Por
eso el realm no exige `code_challenge` al cliente confidencial. La defensa vigente
incluye secreto de cliente externo, redirects exactos, audience, firma RS256,
issuer/expiración, cambio de identificador de sesión y HTTPS obligatorio fuera de
la demo local.

`start-dev --import-realm` importa el archivo solamente cuando el realm no existe.
Cambiar el JSON o el archivo del secreto no sobrescribe un realm persistido. La
rotación posterior debe ejecutarse mediante un procedimiento administrativo
versionado; no retirar el volumen como mecanismo de rotación.

## Protección de WildFly

La CLI de imagen configura `secure-deployment=logixone.war` con:

- `provider-url`, cliente, secreto y URI de salida como expresiones externas;
- cliente no público;
- firma `RS256`, audience obligatoria, issuer y expiración validados por OIDC;
- validación TLS y hostname activas;
- `principal-attribute=sub`;
- roles funcionales no derivados del token;
- detección automática de REST para responder `401` en vez de redirigir.

WildFly 41 clasifica `logout-path` y `post-logout-redirect-uri` como capacidad
`preview`. La imagen configura el modelo y arranca el servidor con
`--stability=preview`; omitirlo deja el logout local sin coordinación efectiva con
el proveedor. La decisión, sus riesgos y el gate obligatorio están en
[ADR-0008](../adr/0008-logout-oidc-estabilidad-preview-wildfly.md).

`WEB-INF/web.xml` protege `/app/*`, `/admin/*` y `/api/*`. `/health/live` y `/health/ready`
permanecen públicos. Readiness agrega `oidc-configuration`, que solo comprueba la
configuración local; nunca consulta Keycloak. Liveness no cambia y no consulta
ninguna dependencia.

## Bootstrap one-shot del ERP

El bootstrap está cerrado por defecto:

```text
LOGIXONE_SECURITY_BOOTSTRAP_ENABLED=false
```

Solo puede habilitarse cuando la empresa ya existe, está activa y tiene exactamente
el plugin de personalización declarado. Al habilitarlo se requieren:

| Variable | Contenido |
|---|---|
| `LOGIXONE_SECURITY_BOOTSTRAP_SUBJECT` | `sub` exacto emitido por el realm |
| `LOGIXONE_SECURITY_BOOTSTRAP_DISPLAY_NAME` | etiqueta opcional; no es identidad |
| `LOGIXONE_SECURITY_BOOTSTRAP_COMPANY_ID` | UUID canónico de la empresa existente |
| `LOGIXONE_SECURITY_BOOTSTRAP_CUSTOMIZATION_PLUGIN` | `PluginId` asignado a esa empresa |
| `LOGIXONE_SECURITY_BOOTSTRAP_ROLE_CODE` | código estable del rol inicial |
| `LOGIXONE_SECURITY_BOOTSTRAP_ROLE_NAME` | nombre visible del rol |
| `LOGIXONE_SECURITY_BOOTSTRAP_PERMISSIONS` | `ContributionId` separados por coma |

El issuer se toma de `LOGIXONE_OIDC_PROVIDER_URL`; no existe un segundo valor que
pueda divergir. La operación es transaccional e idempotente: una repetición exacta
devuelve `UNCHANGED`; empresa, identidad, membresía, rol, asignación o permisos
incompatibles abortan el despliegue. No existe endpoint REST o Faces de bootstrap.

Después de una ejecución aceptada, volver a dejar `ENABLED=false` y recrear `app`.
La fila persistida permanece y los futuros cambios se realizan mediante casos de uso
administrativos autorizados, nunca mediante SQL directo.

## Arranque y resultados verificados

La validación runtime de `J11-S3-08` usó:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml config --quiet

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up --wait --wait-timeout 240
```

Resultados demostrados en la candidata local:

- Keycloak importa el realm sin exponer secretos y queda saludable;
- el migrador termina antes de `app`;
- liveness responde aunque Keycloak se detenga;
- readiness no llama a Keycloak y conserva su semántica local;
- `/logixone/faces/app/index.xhtml` redirige a login cuando falta identidad;
- `/logixone/api/protected-probe` con `Accept: application/json` responde `401`;
- tokens con issuer, audience o expiración inválidos son rechazados;
- configuración OIDC indispensable inválida impide disponibilidad;
- logout invalida la sesión local, coordina la salida OIDC y una segunda visita no
  reutiliza la sesión anterior.

El gate runtime sumó 2 pruebas de health, 4 de JTA y 4 de OIDC, sin fallos ni
omisiones. Playwright agregó 3 escenarios: cero/una/múltiples membresías,
manipulación negativa de empresa, selección/cambio, variantes A/B responsive y
logout real. Estos resultados no sustituyen la validación independiente de la guía
ni autorizan promoción a producción.

El realm contiene tres identidades ficticias: sin empresa, una empresa y dos
empresas. Su contraseña común se inyecta desde el archivo local ignorado
`.tools/secrets/demo-user-password.txt`; no se copia a documentación, logs ni
variables versionadas. `ConfiguredDemoProvisioning` crea o verifica, solo cuando
`LOGIXONE_DEMO_PROVISIONING_ENABLED=true`, las empresas, membresías, roles,
permisos y activaciones necesarias para la matriz, sin exponer un endpoint.

## Persistencia, parada y recuperación

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down
```

`down` conserva `postgres-data` y `keycloak-data`. Una recreación vuelve a usar las
mismas bases y el importador omite el realm existente. No usar `down --volumes`: esa
opción elimina ambos estados y requiere una decisión destructiva explícita, backups
y confirmación del proyecto exacto.

El almacén `dev-file` y `start-dev` son solo para demo. Producción debe usar una base
externa respaldada y procedimientos separados de backup/restore para Keycloak y el
ERP; restaurar uno sin compatibilidad con el otro puede dejar issuer, cliente o
secreto desalineados.

## Diagnóstico seguro

Consultar únicamente estado y logs filtrados por servicio:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml ps

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml logs --no-color keycloak app
```

No ejecutar comandos que impriman el entorno del proceso, el realm efectivo con su
secreto, cookies, tokens o `/run/secrets`. Los eventos de bootstrap registran estado,
empresa, rol y cantidad de permisos, nunca issuer, subject, nombre o secreto.

## Referencias oficiales

- [WildFly 41 Admin Guide — Elytron OIDC Client](https://docs.wildfly.org/41/Admin_Guide.html)
- [WildFly 41 — modelo `secure-deployment`](https://docs.wildfly.org/41/feature-pack/doc/reference/subsystem/elytron-oidc-client/secure-deployment/index.html)
- [Keycloak — importación y exportación](https://www.keycloak.org/server/importExport)
- [Keycloak — ejecución en contenedores](https://www.keycloak.org/server/containers)
- [Keycloak 26.7.0 — notas de versión](https://www.keycloak.org/docs/26.7.0/release_notes/)
