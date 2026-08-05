# Evidencia J11-S5-04 - Validación, demo visual y corte técnico de Sprint 5

- Fecha: 2026-07-29
- Estado: gates técnicos G0-G6 verdes; G7 independiente pendiente
- Historia: [J11-S5-04](../sprints/sprint-05/J11-S5-04-validacion-demo-cierre.md)
- Guía evaluada: `1.0-rc27`

## Ambiente

| Componente | Versión o selección |
|---|---|
| Sistema | Windows 11 amd64, locale `es_PY`, UTF-8 |
| Java | Eclipse Adoptium 21.0.11 |
| Maven Wrapper | Apache Maven 3.9.16 |
| Docker Engine | 29.6.2 |
| Docker Compose | 5.3.1 |
| PostgreSQL | 18.4-bookworm fijado por digest en Compose |
| Keycloak | 26.7.0 fijado por digest en Compose |
| Aplicación demo | `logixone/app:j11-s5-02-customized` |
| Migrador demo | `logixone/migrator:j11-s5-02-customized` |
| Proyecto Compose | `logixone-s5-01`, puertos públicos 18085/18185 |

Los binarios, cachés, navegador, credenciales ficticias y capturas permanecieron
dentro de `.tools/`. No se imprimieron secretos ni se modificó el legado.

## G1 y G2 - Reactor, arquitectura y composiciones

La variante A/B se ejecutó con:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools\jdk\jdk-21.0.11+10').Path
$env:MAVEN_USER_HOME=(Resolve-Path '.tools\maven-wrapper-home').Path
.\mvnw.cmd -B -Pwith-screen-customization-plugins verify
```

Resultado: 18/18 proyectos `SUCCESS`; 191 pruebas, cero fallos, errores u
omisiones. ArchUnit incluyó 13 pruebas y conservó los límites del kernel, API,
plugins, herramienta y ensamblado. El WAR y el proveedor del migrador contenían
exactamente:

1. `reference-plugin`;
2. `reference-customization-a`;
3. `reference-customization-b`.

La composición generada temporalmente con el CLI para `cli-probe` verificó 11/11
proyectos y fue eliminada por el siguiente `clean`; no quedó incorporada al árbol.

Después de todos los cambios se restauró el artefacto canónico base:

```powershell
.\mvnw.cmd -B clean verify
```

Resultado final: 18/18 proyectos `SUCCESS` en 1 min 29 s; 191 pruebas, cero
fallos, errores u omisiones. La inspección ZIP encontró
`WAR_PLUGIN_COUNT=0` y `MIGRATOR_PROVIDER_COUNT=0`. Los módulos de referencia se
compilan y prueban en el reactor, pero no entran en la distribución base.

## G3 - PostgreSQL y migraciones

```powershell
.\mvnw.cmd -B -Ppostgres-integration `
  "-Dlogixone.postgres.integration=true" -pl migrator -am verify
```

`CoreMigrationPostgreSqlIT` terminó 12/12 verde. Cubrió base vacía, orden `core`
antes de plugins, esquema `plg_reference_plugin`, historial por propietario,
checksum, repetición idempotente y datos persistentes.

En Compose, la segunda ejecución del migrador informó:

| Propietario | Migraciones ejecutadas | Versión final |
|---|---:|---:|
| `core` | 0 | 5 |
| `reference_plugin` | 0 | 1 |

La primera consulta manual al fixture usó por error una columna inexistente
`marker`. PostgreSQL rechazó la consulta. Se inspeccionó la migración propietaria,
se corrigió a `fixture_key` y el resultado fue `marker_count=1`. No se alteró el
esquema ni se relajó una prueba.

## G4 - Imágenes, Compose, health y volumen

| Imagen | Digest local | Tamaño |
|---|---|---:|
| app | `sha256:54441d13d12c06dbd22118114f9e81451d70e9689ab45739434837819ead4953` | 500126463 bytes |
| migrator | `sha256:bbad86783ea5fa7e1b39b48143809752e7963e72650fe0e5be08d20371096701` | 104569197 bytes |

La pila aislada quedó saludable. `GET /logixone/health/live` respondió `200 UP`
para `application`; `GET /logixone/health/ready` respondió `200 UP` para
`catalog`, `configuration`, `database`, `migrations` y `oidc-configuration`. El
catálogo runtime informó tres plugins físicos, igual que el migrador.

Al terminar se ejecutó:

```powershell
docker compose --project-name logixone-s5-01 `
  -f infra\compose\compose.yaml down
```

No se usó `--volumes`. Los contenedores y redes se retiraron y continuaron
existiendo:

- `logixone-s5-01_postgres-data`;
- `logixone-s5-01_keycloak-data`.

## G5 - Demo real, OIDC y responsive

El navegador integrado bloqueó URLs locales por política del cliente, por lo que
se usó el arnés Playwright versionado del repositorio con Chromium de `.tools/`.
La primera ejecución completa detectó dos problemas reales: 1 fallo y 1 error de
cinco pruebas.

1. La landing administrativa no estaba autorizada porque el bootstrap global
   one-shot estaba desactivado. Un primer intento usó el display name con una
   mayúscula distinta y readiness falló cerrado con
   `SYSTEM_BOOTSTRAP_IDENTITY_INCOMPATIBLE`. Se corrigió al nombre ficticio exacto,
   se creó el rol global con cinco permisos y se recreó inmediatamente la
   aplicación con bootstrap desactivado.
2. El logout produjo `invalid_redirect_uri` en Keycloak. Se alineó
   `post.logout.redirect.uris` del cliente persistido con
   `/logixone/faces/app/index.xhtml`, se recreó la aplicación y se repitió primero
   la prueba afectada.

No se desactivaron aserciones. La prueba dirigida de logout terminó 1/1 y la suite
completa final terminó 5/5 verde, sin fallos, errores u omisiones, en 39,59 s.
Cubrió login/logout OIDC, autoridad global, navegación administrativa, aislamiento
empresarial, composición de pantallas y personalizaciones A/B.

Playwright generó 22 PNG en
`.tools/evidence/J11-S5-04/screenshots/`, con 6.250.109 bytes en total:

- ocho recorridos expandidos a 1280 px;
- siete recorridos medios a 720 px;
- siete recorridos compactos a 375 px.

Se revisaron visualmente todas las capturas. No hay overflow horizontal normal,
solapamientos, cortes, controles fuera del viewport ni mezcla de empresa. Las
pantallas conservan Material Design 3 sobre JSF; la personalización A modifica
etiqueta, obligatoriedad y slot, mientras la B muestra su variante aislada. Las
vistas aclaran que son una demostración técnica y no módulos ERP productivos.

## G0 y G6 - Documentación, continuidad y PDF

- ADR-0012 y ADR-0013 permanecen aceptados e indexados;
- la guía de implementación avanzó a `1.0-rc27`;
- Sprint 6 quedó planificado sin código y comienza por caracterización de
  `business_partners`;
- la retrospectiva registra los fallos y acciones de mejora;
- el escaneo final cubrió 144 Markdown: cero errores UTF-8, archivos con `U+FFFD`
  o enlaces locales rotos;
- la búsqueda de estados obsoletos no encontró placeholders del PDF ni referencias
  activas que conservaran J11-S5-04 como siguiente incremento.

PDF final pendiente de completar en esta misma evidencia:

| Control | Resultado |
|---|---|
| Ruta | `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` |
| Sprint/fecha | Corte técnico Sprint 5 / 2026-07-29 |
| Páginas | 36 A4 |
| Tamaño | 225815 bytes |
| SHA-256 | `d21f82868114875c5996565ec3cdb079f7d8fb9cce319434c039d739dc8da698` |
| Revisión | 36/36 páginas renderizadas; portada, índice, encabezados, pies, tablas, cortes y caracteres aprobados |

La lectura con `pypdf` y `pdfplumber` confirmó título y autor Unicode, texto
extraíble en 36/36 páginas, cero páginas vacías, cero `U+FFFD`, cero coincidencias
con secretos locales, sin cifrado, JavaScript ni AcroForm. El generador usa metadatos
invariantes para que la evidencia pueda registrar el checksum y volver a generar
el mismo artefacto después de actualizar esta ficha.

## Gate pendiente y decisión

G0-G6 quedan verdes. G7 requiere que una persona que no haya implementado estas
capacidades complete [VALIDATION.md](../implementation-guide/VALIDATION.md). Esta
evidencia no reemplaza su recorrido. Por ello Sprint 4 y Sprint 5 permanecen
formalmente abiertos, la guía no se publica como `1.0`, la imagen no se promueve y
no se autoriza producción. Sí queda autorizado iniciar la documentación y
caracterización de Sprint 6.
