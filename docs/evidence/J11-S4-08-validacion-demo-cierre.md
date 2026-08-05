# Evidencia de J11-S4-08 — Validación acumulada, demo y cierre

- Fecha de inicio: 2026-07-28
- Última ejecución técnica: 2026-07-29
- Estado: gates técnicos G0–G6 y PDF verdes; G7 pendiente únicamente de recorrido independiente
- Historia: [J11-S4-08](../sprints/sprint-04/J11-S4-08-validacion-demo-cierre.md)
- Procedimiento: [manual paso a paso](../runbooks/manual-pruebas-j11-s4-08.md)

## Dictamen actual

El código, la persistencia, la seguridad, la infraestructura y la demo visual del
Sprint 4 superaron sus gates técnicos. `J11-S4-01` a `J11-S4-07` se consideran
completadas y validadas. El Sprint todavía no está cerrado: la edición candidata
`1.0-rc22` era la candidata al completar los gates técnicos. Antes del recorrido
fue reemplazada por la edición acumulativa vigente `1.0-rc26`, que debe ser recorrida y
dictaminada por una persona independiente. El PDF
obligatorio ya fue regenerado y verificado contra este baseline técnico.

No se autoriza promover la imagen ni iniciar `business_partners` mientras G7 siga
pendiente.

## Registro de gates

| Gate | Estado | Evidencia principal |
|---|---|---|
| G0 | Verde | documentación, ADR, historias, UTF-8 y trazabilidad actualizados |
| G1 | Verde | reactor de 16 módulos y composiciones WAR 0/1/3 reproducibles |
| G2 | Verde | 174 pruebas unitarias/arquitectónicas, sin fallos, errores u omisiones |
| G3 | Verde | migraciones 11/11, JPA/PostgreSQL 12/12 y JTA runtime 6/6 |
| G4 | Verde | health 2/2, OIDC 4/4, bootstrap y seguridad administrativa positiva/negativa |
| G5 | Verde | Dockerfiles, imagen exacta, Compose, health, fallo DB y volúmenes validados |
| G6 | Verde | Playwright 5/5, 22 capturas y revisión responsive/accesible A/B |
| G7 | Pendiente parcial | PDF verde; recorrido independiente sin ejecutar |

## Ambiente verificado

| Componente | Valor |
|---|---|
| Sistema | Windows 11 amd64, zona `America/Asuncion` |
| Java | Eclipse Temurin 21.0.11+10 bajo `.tools/jdk/` |
| Maven Wrapper | 3.9.16; repositorio y home usados en `.tools/` para los gates |
| Docker / Compose | 29.6.2 / v5.3.1 |
| Runtime | WildFly 41, estabilidad preview únicamente para logout OIDC |
| PostgreSQL | 18.4, esquema `core` V5 |
| Keycloak | 26.7.0 |
| Proyecto Compose | `logixone-s408`, app `18084`, Keycloak `8184` |

No se copiaron contraseñas, tokens, cookies ni contenidos de archivos secretos a la
evidencia. Los cuatro secretos se suministraron por archivos locales ignorados.

## G1/G2 — Reactor, pruebas y composición

El comando final `mvnw.cmd -B verify` terminó con `BUILD SUCCESS` en los 16 módulos.
Los reportes Surefire suman:

```text
tests=174
failures=0
errors=0
skipped=0
```

La cifra incluye 11 pruebas ArchUnit/composición: nueve límites arquitectónicos y
dos verificaciones físicas. Se construyeron y abrieron tres WAR:

1. sin plugins de referencia;
2. solo con `reference-plugin`;
3. demo con plugin funcional y personalizaciones A/B.

La imagen final contiene exactamente los tres JAR de la tercera composición. El
kernel no depende de sus implementaciones y `plugin-api` continúa libre de
`jakarta.*`, JSF, WildFly e infraestructura.

## G3 — PostgreSQL, migraciones, JPA y JTA

| Corte | Resultado |
|---|---|
| migrador sobre PostgreSQL 18.4/Testcontainers | 11/11 verdes |
| repositorios JPA sobre PostgreSQL 18.4/Testcontainers | 12/12 verdes |
| arnés temporal JTA sobre WildFly/PostgreSQL | 6/6 verdes |

Se cubrieron instalación vacía, V1→V2→V3→V4→V5, reejecución, checksums,
restricciones V4, auditoría append-only V5, `hibernate.hbm2ddl.auto=validate`,
concurrencia optimista, filtros/paginación y rollback atómico.

Checksums Flyway conservados antes y después de recrear contenedores:

```text
1|-1098736951|true
2|-1309935940|true
3|1116433995|true
4|-950619835|true
5|1082469995|true
```

El reset del arnés fue endurecido para retirar usuarios, roles, asignaciones y
permisos identificados como datos de prueba. Las consultas finales dieron cero
entidades operativas del arnés. Los eventos ya confirmados permanecen porque la
auditoría es append-only; el arnés se retiró del servidor y no forma parte del WAR
normal ni de la imagen.

## G4 — Bootstrap, OIDC y seguridad administrativa

El bootstrap global produjo una creación, una repetición `UNCHANGED` y un rechazo
cerrado con diagnóstico `SYSTEM_BOOTSTRAP_ROLE_INCOMPATIBLE`. Después se restauró la
configuración y se recreó la aplicación con
`LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_ENABLED=false`.

La ejecución final sobre el digest exacto reunió:

- health REST Assured: 2/2;
- OIDC runtime: 4/4, con token válido aceptado y audience, issuer y expiración
  inválidos rechazados con `401`;
- identidad solo empresarial: acceso administrativo real rechazado con `403`;
- navegación permitida: cinco superficies administrativas y landing protegidas;
- respuestas permitidas y denegadas: `no-store`, `no-cache`, `nosniff`,
  `DENY`, `no-referrer`, `Permissions-Policy` y CSP defensiva.

La autorización se reevalúa en el servidor; ocultar un enlace o manipular una ruta,
ID o formulario no concede autoridad.

## G5 — Imagen, Compose, health y volúmenes

Ambos Dockerfiles pasaron `docker buildx build --check` con el resultado
`Check complete, no warnings found.` La aplicación se reconstruyó en modo
`verified` con el perfil de las dos personalizaciones.

| Artefacto | Identidad |
|---|---|
| imagen app final | `sha256:629c6b8ff9e1ab48fb82d37bfecede4beaa0faeaaf20de13b5d883ab60459d21` |
| config OCI | `sha256:fc077f5d54e3986799fb10c21a397552dbfc7d6d5a7938a8ff696f5029b671f2` |
| manifiesto linux/amd64 | `sha256:ac111ba9bf0218c8953a348b7137b4e03bf921cc12d43dd0e0db1aebe53ef0fa` |
| WAR dentro de imagen | `61A818AD278F8F56D561E24C623EA5FAB458B0AE02C9F87D22DA32283D0D2F82` |
| tamaño imagen | 500125215 bytes |
| imagen migrator | `sha256:e25c28316da6a2a4a2ed71ea709ef71e3f73264ef2413a67e86526b96732310f` |

El runtime usa `jboss` (`uid=1000`, `gid=1000`), pgJDBC existe como módulo de
WildFly y no dentro del WAR, y `/workspace` no existe en la capa final. El contenedor
recreado mostró el mismo ID que la etiqueta final y cero reinicios.

Health positivo:

```text
GET /logixone/health/live  -> 200 UP, application, Cache-Control: no-store
GET /logixone/health/ready -> 200 UP, catalog/configuration/database/
                              migrations/oidc-configuration
```

Al detener solamente PostgreSQL, liveness permaneció `200 UP`, readiness cambió a
`503 DOWN` por `database`/`migrations` y volvió a `200 UP` al recuperar la base sin
reiniciar la aplicación.

Compose creó y conservó los volúmenes explícitos
`logixone-s408_postgres-data` y `logixone-s408_keycloak-data`. `down` se ejecutó sin
`--volumes`; ambos volúmenes continuaron presentes y el migrador posterior informó
`migrations_executed=0 schema_version=5`.

| Dato | Antes de `down` | Después de recrear |
|---|---:|---:|
| empresas | 2 | 2 |
| usuarios | 3 | 3 |
| roles empresariales | 2 | 2 |
| roles globales | 1 | 1 |
| activaciones | 2 | 2 |
| eventos de auditoría | 548 | 556 |

El crecimiento de auditoría corresponde a accesos y operaciones ejecutados durante
la validación; los datos estables no se perdieron ni duplicaron.

Después de todos los resultados verdes, Docker Desktop se detuvo en el puesto de
trabajo. El fallo ocurrió después de registrar digest, health, OIDC, Playwright,
estado de servicios, cero reinicios y revisión limpia de logs. Un intento de
reiniciarlo en segundo plano inició el motor y volvió a cerrarse. No se ejecutó
`down --volumes`; antes de una demo se debe arrancar Docker Desktop y repetir el
preflight de Compose/health.

## G6 — Playwright y demo visual

La repetición final sobre la imagen exacta terminó:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

La suite comprueba login/logout, aislamiento A/B, acceso administrativo permitido y
denegado, navegación canónica, carga del CSS Material, encabezados defensivos,
labels, skip link, un solo `h1` y ausencia de overflow. Las cinco superficies
administrativas se probaron a 375, 720 y 1280 px, y también en los límites
599/600/839/840 px.

Se generaron 22 PNG bajo `.tools/evidence/J11-S4-08/screenshots`: 18 capturas de las
seis superficies administrativas en compacto/medio/expandido y cuatro capturas
empresariales A/B. El conjunto final suma 6174143 bytes. Todas se revisaron
visualmente; no hay texto cortado, controles fuera del viewport, mezcla de empresa,
recursos sin estilo, SQL, stacktraces ni autoridad visible sin permiso.

Chromium 1228 se reutilizó dentro de `.tools/playwright/`. El ejecutable validado
tiene SHA-256
`B798F9E53A98D29EB7F36F8C409F905D3184780A04D2BCB56989067194784BD1`.

## Hallazgos y correcciones

| ID | Hallazgo | Corrección y revalidación |
|---|---|---|
| H-01 | readiness histórico esperaba V3 | prueba actualizada a V5; 126 pruebas del corte verdes |
| H-02 | fixture usaba un accessor de auditoría inexistente | se usó `actorUserId()`; aplicación 53/53 |
| H-03 | `assertTrue` ambiguo sobre resultado genérico | resultado materializado como `boolean`; JPA 12/12 |
| H-04 | arnés seleccionado sin perfil opt-in | comando corregido con `-Pjta-runtime-harness`; JTA 6/6 |
| H-05 | enlaces Faces componían `/admin/admin/...` | rutas canónicas `/faces/admin/...` y prueba de regresión |
| H-06 | estilos Faces se resolvían bajo la ruta anidada y no cargaban | links canónicos a `jakarta.faces.resource`; prueba y navegador verdes |
| H-07 | selects, tarjetas y etiqueta técnica producían overflow móvil | `min-width: 0`, grilla flexible y wrap; 375 y breakpoints verdes |
| H-08 | reset JTA omitía autoridad global del arnés | limpieza identificada y aserciones de cero residuos operativos |
| H-09 | primera terminal heredó Java 8 | se fijó Temurin 21 validado bajo `.tools`; no fue defecto del proyecto |
| H-10 | una ejecución E2E quedó suspendida por pausa/reloj del equipo | se repitió el escenario y luego la suite completa, ambas verdes |
| H-11 | un comando final apuntó a `chrome-win` en vez de `chrome-win64` | navegador ubicado en `.tools`, hash registrado y suite 5/5 repetida |
| H-12 | Docker Desktop se cerró tras los gates | incidente operativo registrado; no se borraron volúmenes y la demo requiere preflight |
| H-13 | el PDF estable todavía describía Sprint 2 y la primera maqueta dejó una página final casi vacía | generador reproducible, inventario actual, última página compactada y nueva revisión de las 33 páginas |

Ningún fallo se convirtió en omisión ni se relajó una aserción para aceptar un
resultado incorrecto.

## PDF obligatorio de cierre

Se agregó el generador reproducible
`tools/generate_repository_guide_pdf.py` y se regeneró
`docs/output/pdf/guia-estructura-repositorio-logixone.pdf` desde la estructura real.

| Control | Resultado |
|---|---|
| Sprint/fecha | Cierre Sprint 4 / 2026-07-29 |
| inventario | 570 archivos y 312 directorios mantenidos |
| páginas | 33 A4 |
| tamaño | 216810 bytes |
| SHA-256 | `D0AA01372543284E03448DD6AD2A85CD6E56D73A012810E89AEE8DDAE77067AB` |
| metadatos | título y autor correctos; sin cifrado, formulario ni JavaScript |
| texto | 33/33 páginas no vacías; 0 reemplazos; 0 guiones Unicode problemáticos |
| secretos | ningún valor de `.tools/secrets/*.txt` aparece en el texto extraído |
| revisión visual | 33/33 renderizadas a PNG; sin cortes, solapamientos, tablas rotas ni glifos dañados |

La primera revisión visual encontró una página 34 casi vacía y continuadores de
línea de `cmd` en un bloque destinado a PowerShell. Se corrigieron ambos puntos. Al
comparar renders, solo cambiaron índice y última página; se revisaron de nuevo a
resolución original. La edición final no contiene páginas vacías.

## G7 — Pendientes de cierre

- [ ] una persona que no implementó Sprint 2/3/4 completa y firma
  `docs/implementation-guide/VALIDATION.md`;
- [ ] todo hallazgo bloqueante o mayor de ese recorrido queda corregido y revalidado;
- [ ] la guía candidata vigente puede pasar de `1.0-rc26` a `1.0` solo con dictamen satisfactorio;
- [x] `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` regenerado,
  renderizado completo y registrado contra el baseline técnico actual.

Hasta completar los tres puntos humanos pendientes, `J11-S4-08` y el Sprint 4
permanecen abiertos. Si el validador exige una corrección material, el PDF deberá
regenerarse otra vez contra el baseline aceptado resultante.
