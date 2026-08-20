# Evidencia J11-S10-06 — Validación integral y demo oficial de Sprint 10

- Fecha: 2026-08-20
- Estado: G0–G6 verdes; J11-S10-07 decidió `NO`; sólo G7 independiente permanece pendiente
- Historia: [J11-S10-06](../sprints/sprint-10/J11-S10-06-validacion-demo-cierre.md)
- Demo: [runbook de cierre](../runbooks/demo-cierre-sprint-10.md)
- Perfil: `with-purchasing-demo`
- Materialización final de código: `.tools/tmp/validation/J11-S10-06-headerfix/`

## Resultado ejecutivo

Sprint 10 quedó validado automáticamente sobre WildFly 41, PostgreSQL 18.4 y
Keycloak 26.7.0. Inventario y Compras usan sus floorplans v2 reales y los
maestros v1 conservan compatibilidad. El corte suma 586 pruebas automatizadas
únicas, sin fallos, errores ni omisiones.

La evidencia completa G0–G6, pero no cierra el Sprint: la revisión independiente
G7 continúa diferida. J11-S10-07 registró posteriormente la decisión explícita
`NO`, sin modificar el instalador de Sprint 9.

## G0 — Documentación y protección

Se revisaron historia, demo, retrospectiva, fotografía de plugins, inventario de
selectores, manuales, arquitectura, guía de desarrollo e implementación e
índices. La fotografía incluye SVG accesible, alternativa Mermaid y descripción
textual. `tools/validate_docs.py` revisó 387 Markdown: cero enlaces rotos,
errores de codificación, mojibake o fugas de secretos. No se incorporaron datos
reales.

## G1 — Reactor y arquitectura

El Wrapper raíz gobernado bajo `.tools/` ejecutó el corte de código final:

```powershell
.\mvnw.cmd -B clean verify
.\mvnw.cmd -B -Pwith-purchasing-demo clean verify
```

Ambas composiciones terminaron con 28/28 módulos. La composición completa produjo
565 pruebas Surefire y 34 escenarios ArchUnit, todos verdes. La construcción
verificada de la imagen final repitió el reactor sobre el mismo corte que
empaquetó el WAR.

## G2 — Composición física e imágenes

La composición base conserva cero implementaciones; `with-purchasing-demo`
contiene cinco plugins productivos y tres fixtures técnicos. La aplicación
saludable declaró exactamente:

`reference_data@1.1.0`, `business_partners@1.1.0`,
`commercial_catalog@1.1.0`, `inventory@1.2.0`, `purchasing@1.2.0`,
`reference_plugin@1.0.0`, `reference_custom_a@1.0.0` y
`reference_custom_b@1.0.0`.

| Artefacto | Tag | ID local | Bytes |
|---|---|---|---:|
| aplicación | `logixone/app:j11-s10-06-closing-v3` | `sha256:cfaf4295b7ef55e1dca08ec5049a1c391c4a2ed0874a3b515b9b4f99679c033b` | 501567086 |
| migrador | `logixone/migrator:j11-s10-06-closing` | `sha256:09bcda52430cdba667e151c0842f7c879626081b2ec00a6515482ff06f6e4fca` | 105845810 |

Manifiesto interno de aplicación:
`sha256:d2d6a3e4ea5585a8e920160a36d5c7f0e60a0419799e224c4e50adccd13c9b81`;
configuración:
`sha256:45131abbdcb2b74413417fc225aaeb34b3af6b6af33e50c37f1eae744d7b1828`.
Manifiesto interno del migrador:
`sha256:f6a24ecb76c6ce0a48d20cd3b93010abc0a9b733e5d5868f2298c463b2f6b0dd`;
configuración:
`sha256:d49fa1d3225ac1805fa78c70a413880160d12e8b17eac9b6275207db4fd25ca9`.

Una candidata intermedia recibió por error un nombre de argumento Maven no
canónico, declaró cero plugins y falló de forma segura al preparar los datos. Se
descartó antes de ejecutar la matriz; la candidata final v3 usa
`LOGIXONE_MAVEN_PROFILE=with-purchasing-demo`.

## G3 — PostgreSQL, Flyway y JTA

Sobre una base vacía se aplicaron 23 migraciones: `core` V1–V6,
`reference_data` V1–V4, `business_partners` V1–V4,
`commercial_catalog` V1–V4, `inventory` V1–V2, `purchasing` V1–V2 y
`reference_plugin` V1. La segunda ejecución informó cero migraciones nuevas en
los siete propietarios.

La inspección de WildFly confirmó datasource habilitado, `jta=true`, JNDI
`java:/jdbc/LogixoneCoreDS`, aislamiento `READ_COMMITTED`, pool 2–20,
credenciales y URL externalizadas y validación `SELECT 1`. El arnés JTA opt-in
ejecutó 6/6 casos de commit, rollback y datasource administrado. Después se
retiró; `deployment-info` mostró únicamente `logixone.war OK`.

## G4 — Docker, health, OIDC y runtime

`docker build --check` terminó sin advertencias en aplicación y migrador. El
proyecto aislado `logixone-j11-s10-06` inició PostgreSQL, Keycloak y la aplicación
final v3; esta quedó `healthy` y ejecutó el ID exacto documentado.

Las pruebas contra runtime terminaron 12/12: health 2/2, JTA 6/6 y OIDC 4/4. El
scan final no encontró excepciones de servidor. Las dos coincidencias con texto
`ERROR` corresponden a mensajes Faces de validación intencional producidos por
las pruebas negativas, no a errores de log ni de runtime.

El bootstrap global se usó una sola vez con un sujeto ficticio, se deshabilitó y
la aplicación fue recreada con `LOGIXONE_SECURITY_BOOTSTRAP_ENABLED=false` antes
de la matriz final.

Al terminar se ejecutó `down --volumes --remove-orphans` únicamente sobre
`logixone-j11-s10-06`. Se eliminaron sus cuatro contenedores, tres redes y dos
volúmenes ficticios; la verificación por etiqueta no dejó recursos del proyecto.

## G5 — Playwright, seguridad y revisión visual

La suite oficial terminó:

- `BusinessPartnersVisualIT`: 1/1;
- `CommercialCatalogVisualIT`: 1/1;
- `InventoryVisualIT`: 1/1;
- `PurchasingVisualIT`: 1/1;
- `VisualDemoIT`: 5/5;
- **total: 9/9, 0 fallos, 0 errores y 0 omitidas**.

Se verificaron login, empresas A/B, administración, cinco plugins productivos,
dependencias, rutas directas con plugin inactivo, denegación de servidor,
restauración, teclado, foco, movimiento reducido y los anchos
375/599/600/720/839/840/1280. El hallazgo de ancho insuficiente en la fuente de
la barra empresarial media se corrigió y quedó protegido por una aserción
geométrica.

Se conservaron **171 PNG, 26436529 bytes** en
`docs/evidence/screenshots/J11-S10-06/e2e/`. Se revisaron los originales de los
tres rangos, estados operativos, administración y denegaciones; no se observaron
recortes, overflow horizontal normal ni acciones esenciales perdidas.

Checksums representativos:

- Inventario 375: `B517D60D6EDFAEA68687229B256D94D95D4CB75344E1E888EEEC5400C50BE45D`;
- Inventario 720: `312325C79B6FC2B6136C1A06DA9E54039C188B5F4852ABA1399A0D2A2BCDF5EA`;
- Inventario 1280: `ADCD6BE90E0095A776AB9B65A7947019F0AA751E7B33F2F66477AFE9D9BCC7DB`;
- Compras 375: `12EA1E59378294325378F37FB94540400DBBB2292F2E3C6E8AEE6D5A1456CCFA`;
- Compras 720: `C27B0D7FA6165F446359A83210D1B868B1431CD682BF4F6F6B191D502BB088BC`;
- Compras 1280: `4CB309CD3E07C6D6CD09E6827B749CF140EF66329181F9BA5B9B43143F4401D9`;
- denegación Compras 375: `E6F734C80B7CD46C8DE3BACA79E274C12771680742B09803DCF839A657BA4378`.

## G6 — PDF y paquete documental

Se regeneraron la guía de estructura del repositorio y los manuales de Inventario
y Compras desde el corte documentado:

| PDF | Páginas | Bytes | SHA-256 |
|---|---:|---:|---|
| guía de estructura | 122 | 468615 | `A553D4B9DC2E8D00CFF4EC401F6DF3E1CAB7A85C8AF83E0251523973D24CCF76` |
| manual 05 Inventario | 8 | 175586 | `BFF841142AC597496CEA62DD7DA8CFFCE36CBC0A1420AF67F97C706F50D4242A` |
| manual 07 Compras | 15 | 281527 | `14427F9F3AFBD5ADD3BC5A54E7FA6B1C923F14AEED9DD57291F7B029DD5A4688` |

Poppler rasterizó las 145 páginas. Se revisaron todas mediante siete hojas de
contacto y, a tamaño completo, portada, índice, arquitectura, tablas densas,
inicio y cierre de cada manual. `pypdf`, `pdfplumber`, `pdfinfo` y Poppler
confirmaron metadatos, A4, texto extraíble, cero páginas vacías, cero caracteres
de reemplazo y ausencia de cifrado. No se observaron cortes, solapamientos,
páginas en blanco ni caracteres dañados.

## Resumen de pruebas únicas

| Capa | Casos | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| Surefire / reactor | 565 | 0 | 0 | 0 |
| integración runtime | 12 | 0 | 0 | 0 |
| Playwright | 9 | 0 | 0 | 0 |
| **Total** | **586** | **0** | **0** | **0** |

## Pendientes que impiden el cierre formal

1. G7: una persona independiente debe completar la validación acumulada.

J11-S10-07 ya registró `NO`; `installer/windows/current` permanece intacto y no
representa Sprint 10.

Hasta resolver ambos pendientes no se promueven imágenes, no se publica la guía
`1.0`, no se despliega a producción y no se presenta el Sprint como cerrado o el
corte como comercializable.
