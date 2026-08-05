# J11-S3-08 — Evidencia técnica de demo y gates G2–G6

- Fecha: 2026-07-28
- Estado: G2–G6 verdes; G7 pendiente de validación independiente y PDF
- Historia: [J11-S3-08](../sprints/sprint-03/J11-S3-08-validacion-demo-cierre.md)
- Guía evaluable: [1.0-rc11](../implementation-guide/README.md)

## Resultado

La candidata visual está implementada, arrancada y validada técnicamente. Maven,
ArchUnit, PostgreSQL/Testcontainers, JPA, JTA, OIDC, Docker/Compose, health,
persistencia, seguridad negativa y Playwright terminaron sin fallos conocidos en el
alcance ejecutado. Las historias `J11-S3-01` a `J11-S3-07` pueden cambiar a
`Completada` porque sus gates acumulados están verdes.

El Sprint 3 no está cerrado. Continúan pendientes CA-17, CA-18, CA-20 y CA-22: una
persona independiente debe completar `VALIDATION.md`; solo entonces se podrá elevar
la guía a `1.0`, consolidar la retrospectiva/siguiente incremento y generar el PDF
obligatorio contra el baseline definitivo.

## Ambiente

| Componente | Versión o identidad |
|---|---|
| Sistema | Windows 11 amd64, UTF-8, zona `America/Asuncion` |
| Java | Eclipse Adoptium 21.0.11 |
| Maven Wrapper | 3.9.16 |
| Docker Engine/Client | 29.6.2 |
| Docker Compose | 5.3.1 |
| PostgreSQL | 18.4, imagen fijada por digest |
| Keycloak | 26.7.0, digest `sha256:26939e1318d6f008fc2ee6e10cec1cf8f1ba8a21846c1bc81b91ed0506bc2a7a` |
| WildFly | 41.0.0.Final-jdk21, base fijada por digest |
| Aplicación | `logixone/app:j11-s3-08`, ID local `sha256:d6967d2f0666a77a3b787a89f4281a9e2f8bbac9b2839265dce9c86a85af5979` |
| Migrador | `logixone/migrator:j11-s3-08`, ID local `sha256:39723866084ccf2f6cefa94589cf26425561442f36eb4faf6478124de61471b2` |

Los dos últimos valores son identificadores locales de la lista de manifiestos
BuildKit, no digests publicados en un registro. No se realizó promoción ni despliegue
a producción.

## G2 — Maven, unitarias y arquitectura

Comando final:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools\jdk\jdk-21.0.11+10').Path
$env:MAVEN_USER_HOME=(Resolve-Path '.tools\maven-wrapper-home').Path
.\mvnw.cmd -B -Pwith-screen-customization-plugins `
  "-Dlogixone.postgres.integration=true" verify
```

Resultado explícito: código `0`, `BUILD SUCCESS`, 16/16 módulos, 35 reportes, 145
pruebas, 0 fallos, 0 errores y 0 omitidas.

| Alcance | Pruebas |
|---|---:|
| `plugin-api` | 16 |
| `kernel-api` | 3 |
| `kernel-domain`, incluidas 5 de revocación | 32 |
| `kernel-application` | 37 |
| migrator unitario + PostgreSQL | 17 |
| infraestructura unitaria + repositorios PostgreSQL | 24 |
| `web-shell` | 3 |
| plugins de referencia y personalizaciones A/B | 4 |
| arquitectura/composición, incluidas 7 reglas ArchUnit | 9 |
| **Total** | **145** |

Las pruebas de revocación demostraron que inactivar membresía o rol, retirar un
permiso disponible, desactivar un plugin y cruzar un rol de otra empresa eliminan el
acceso efectivo o fallan cerrados.

## G3 — Migraciones, repositorios y JTA

Testcontainers ejecutó PostgreSQL 18.4 real. La matriz cubrió instalación vacía,
V1→V2→V3, reejecución, checksum, restricciones, concurrencia y repositorios JPA. Los
checksums runtime conservados fueron:

```text
1|-1098736951|t
2|-1309935940|t
3|1116433995|t
```

El arnés runtime temporal ejecutó 4 pruebas JTA: commit, rollback, dos empresas con
composición A/B aislada y rollback ante fallo de auditoría. Se agregó `DELETE /reset`
solo al arnés y limpieza antes/después de cada prueba; la consulta final encontró
cero filas `jta_custom_*` o `custom_probe_*`. El arnés no aparece en el WAR ni en la
imagen productiva.

El conteo demo estable, antes y después de recrear la composición, fue:

```text
company=2
company_plugin_activation=2
app_user=3
company_membership=3
security_role=2
membership_role=3
role_permission=2
```

## G4 — OIDC, sesión y seguridad negativa

La integración runtime reunió 10 pruebas sin fallos ni omisiones:

- 2 de health semántico;
- 4 JTA;
- 4 OIDC: token válido aceptado y audience, issuer y expiración inválidos
  rechazados con `401`.

Los clientes, realm y usuario auxiliares de OIDC se crearon con sufijos aleatorios y
se retiraron al finalizar; la vida del token del realm se restauró incluso ante
fallo. Las contraseñas administrativa y demo se leyeron por archivo, nunca por
literal o argumento con valor.

El logout local inicialmente permitía reutilizar la sesión de Keycloak. La causa fue
que `logout-path` y `post-logout-redirect-uri` de WildFly 41 son preview y quedaban
fuera del modelo `community`. Se corrigió la configuración y el arranque con
`--stability=preview`, se verificaron los atributos efectivos y se registró
[ADR-0008](../adr/0008-logout-oidc-estabilidad-preview-wildfly.md). Playwright probó
logout, retorno al login y segundo intento sobre una ruta protegida sin reutilización
de sesión.

## G5 — Imágenes, Compose, health, secretos y persistencia

Ambos Dockerfiles pasaron:

```powershell
docker buildx build --check --platform linux/amd64 --file infra/docker/Dockerfile .
docker buildx build --check --platform linux/amd64 --file infra/docker/Dockerfile.migrator .
```

Resultado: `Check complete, no warnings found.` Las dos imágenes finales se
construyeron con `LOGIXONE_BUILD_MODE=verified`; el builder de aplicación ejecutó sus
16 módulos con `BUILD SUCCESS` y el migrador ejecutó 9 pruebas unitarias.

El WAR final mide 369587 bytes y tiene SHA-256
`60FFAAD7002C39ECA94C06848106B1081FACB87ADB1DFA2EFF9C455C14C4028A`.
Contiene exactamente `reference-plugin`, `reference-customization-a` y
`reference-customization-b`; no contiene pgJDBC, Playwright, Testcontainers, JUnit,
REST Assured ni el arnés JTA.

La primera comparación Windows↔Docker detectó SHA distintos aunque las entradas
tenían contenido idéntico. Tres recursos XML/XHTML/CSS quedaban `0755` en el contexto
de Docker Desktop y `0644` en Windows. Los Dockerfiles ahora normalizan a `0644` solo
el contexto fuente, excluyen la caché `.tools` y restauran `mvnw` a `0755`. Un primer
intento demasiado amplio tocó la caché y falló con código 126; se corrigió el alcance
y el siguiente build fue verde. La comparación final host↔imagen produjo el mismo
SHA-256 anterior.

Compose recreó `migrator` y `app` con los IDs finales. El contenedor activo de
aplicación usa exactamente
`sha256:d6967d2f0666a77a3b787a89f4281a9e2f8bbac9b2839265dce9c86a85af5979`.
PostgreSQL y Keycloak permanecieron saludables y conservaron sus volúmenes.

Health final:

```text
GET /logixone/health/live  -> 200 UP: application
GET /logixone/health/ready -> 200 UP: catalog, configuration, database,
                              migrations, oidc-configuration
```

Al detener PostgreSQL de forma controlada, liveness permaneció `200`, readiness
respondió `503` y volvió a `200` al recuperar la base sin reiniciar la aplicación.
Una imagen iniciada sin configuración indispensable terminó con código 78 y mensaje
seguro.

Se ejecutó `docker compose down` sin `--volumes`; los volúmenes
`logixone_postgres-data` y `logixone_keycloak-data` permanecieron. Después de
recrear, coincidieron checksums, conteos, tres usuarios demo, cliente OIDC y URI de
post-logout. No se ejecutó `down --volumes`.

Los barridos de fuentes, WAR, respuestas, logs y capas runtime no encontraron
valores de los cuatro secretos ni tokens. Readiness no expone provider, cliente,
redirect, logout o diagnóstico interno.

## G6 — Playwright y revisión visual

El comando final usó la ruta
`http://localhost:18080/logixone/faces/app/index.xhtml`, Chrome headless y la
contraseña leída desde `.tools/secrets/demo-user-password.txt`. Resultado: 3 pruebas,
0 fallos, 0 errores, 0 omitidas.

Escenarios:

1. múltiples membresías: rechazo de empresa inyectada, selección autorizada, cambio
   de empresa y variantes A/B aisladas;
2. una membresía: selección automática, variante A y logout coordinado sin
   reutilización de sesión;
3. cero membresías: denegación controlada sin menú ni enumeración empresarial.

Playwright verificó ausencia de overflow horizontal a `375px`, `720px` y `1280px`.
Las cuatro capturas finales se revisaron visualmente y no mostraron cortes, mezcla de
personalizaciones ni contenido de dominio ERP ficticio:

| Captura local ignorada | Bytes | SHA-256 |
|---|---:|---|
| `company-a-desktop.png` | 198089 | `FD2950B2304D8AD21025756CEAD81D92D823C93AC2B4C7F0CFE2F77E24842CB9` |
| `company-b-desktop.png` | 191817 | `7EC38F78CC55C7D96A19A2B928C30C0B913E176E7D33898CFF19152CFCD047BD` |
| `company-b-medium.png` | 170151 | `AF2835DE7AF01991B0C9150225BF2DFAF43D5506F2A83F0CB229D0B30E22CFC8` |
| `company-b-compact.png` | 149331 | `EBB1AFE2F1DC6F1B6E3D749D0A6F0FC19DC360A54539286534EB209E7B2BDA67` |

## Hallazgos corregidos

| Hallazgo | Corrección | Revalidación |
|---|---|---|
| recursos Faces no quedaban disponibles desde los JAR modulares | copia explícita de recursos mantenidos al WAR y mappings protegidos | login/shell A/B en navegador |
| el cliente administrado de WildFly 41 no emitía PKCE | se retiró la exigencia incompatible del cliente confidencial y se mantuvieron secreto, redirects exactos, RS256, issuer, audience y expiración | login y OIDC positivo/negativo |
| logout solo local reutilizaba sesión del proveedor | `--stability=preview`, atributos efectivos y ADR-0008 | E2E de logout y segundo acceso |
| el E2E de logout tenía una carrera de navegación | espera explícita del login antes del reintento | 3/3 repetido sobre imagen final |
| arnés JTA dejaba una personalización exclusiva entre repeticiones | reset acotado a datos del arnés antes/después | 4/4 repetible y cero filas residuales |
| WAR distinto entre Windows y Docker por permisos ZIP | normalización del contexto, sin tocar `.tools` | SHA host = SHA imagen |
| primera captura del gate Maven agotó la ventana de 60 s | se esperó el proceso y luego se repitió con ventana suficiente | código 0 explícito, 145/145 |

## Criterios de aceptación

| Criterios | Estado |
|---|---|
| CA-01 a CA-16 | Cumplidos por G2–G6 y la evidencia anterior |
| CA-17 | Pendiente: recorrido por implementador independiente |
| CA-18 | Pendiente: la guía permanece `1.0-rc11` |
| CA-19 | Cumplido en este documento |
| CA-20 | Pendiente: el PDF se genera solo contra el baseline aceptado final |
| CA-21 | Cumplido: historias 01–07 se actualizan después de gates verdes |
| CA-22 | Pendiente de retrospectiva y decisión posterior al recorrido independiente |

## Siguiente acción autorizada

Entregar la [ficha de validación independiente](../implementation-guide/VALIDATION.md)
a una persona que no haya implementado Sprint 2/3. Mantener la composición local
como demo técnica si resulta útil. No elevar la guía a `1.0`, generar el PDF de cierre
ni declarar terminado el Sprint hasta resolver y documentar el dictamen.
