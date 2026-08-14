# Evidencia J11-S9-07 — Validación integral y demo oficial de Sprint 9

- Fecha: 2026-08-13
- Estado: G0–G6 verdes; G7 independiente y G8 decisión de instalador pendientes
- Historia: [J11-S9-07](../sprints/sprint-09/J11-S9-07-validacion-demo-cierre.md)
- Demo: [runbook de cierre](../runbooks/demo-cierre-sprint-09.md)
- Perfil: `with-purchasing-demo`
- Corte Git de partida: `2cc1485`; cambios de cierre materializados desde el índice

## Resultado ejecutivo

`purchasing` quedó demostrado con `reference_data`, `business_partners`,
`commercial_catalog` e `inventory` sobre WildFly 41, PostgreSQL 18.4 y Keycloak
26.7.0. Las pruebas recorrieron solicitud, aprobación separada, orden, recepción,
devolución, seguimiento, integración de stock, autorización y activación.

J11-S9-07 congela el baseline técnico y completa G0–G6. No cierra Sprint 9: G7
requiere otra persona y G8 requiere la respuesta explícita de producto sobre el
instalador Windows.

## G0 — Documentación y protección de información

El control final validó Markdown, enlaces locales, UTF-8, SVG accesible y ausencia
de los secretos locales usados por Compose. La fotografía contiene `title`,
`desc`, alternativa Mermaid y lectura textual. El diagrama se incorporó al PDF y
se revisó dentro de sus páginas renderizadas.

## G1 — Reactor y arquitectura

El Wrapper informó Maven 3.9.16 y seleccionó el JDK aislado 21.0.11+10. Sobre
materializaciones reproducibles se ejecutaron:

```powershell
.\mvnw.cmd -B clean verify
.\mvnw.cmd -B -Pwith-purchasing-demo clean verify
```

Ambos terminaron con código 0 y 28/28 módulos. La composición completa produjo
145 reportes, 535 pruebas, 0 fallos, 0 errores y 0 omitidas. ArchUnit ejecutó
34/34 escenarios verdes. La construcción verificada de la imagen final repitió
el reactor y la arquitectura sobre el mismo corte que empaquetó el WAR.

## G2 — Composición física

La variante base conserva cero implementaciones. `with-purchasing-demo` contiene
ocho definiciones físicas: cinco productivas y tres fixtures. WAR y migrador
consumen la misma selección de `logixone-plugin-set`.

| Artefacto interno de imagen | Bytes | SHA-256 |
|---|---:|---|
| `logixone.war` | 1993021 | `F30A9C6664C17B3439CF1C125C44B47603EF7BC8B8626EDD1D009F38BB9829E7` |
| `migrator.jar` | 6649030 | `B20F6FF3455D77A371F0E20EE2090D5EFC5A5B3BF204F0F4EA3D17C2760972B5` |

La inspección del conjunto confirmó APIs públicas 1.1.0, `plugin-api` 0.4.3 y
ausencia de `jakarta/` en `plugin-api` y en el SPI neutral del migrador.

## G3 — PostgreSQL, JPA/JTA y migraciones

El gate focal de Compras con PostgreSQL 18.4/Testcontainers terminó verde:

| Prueba | Casos | Fallos/errores/omitidas |
|---|---:|---:|
| JPA y repositorios de `purchasing` | 3 | 0/0/0 |
| propietarios, migraciones e idempotencia | 4 | 0/0/0 |
| **Total** | **7** | **0/0/0** |

Se validaron aislamiento por empresa, constraints, JPA `validate`, rollback JTA,
concurrencia optimista, checksums y ledgers idempotentes. La revisión estática y
ArchUnit confirmaron que no existen relaciones JPA ni consultas SQL entre
esquemas privados de plugins.

## G4 — Docker, Compose, health, OIDC y persistencia

`docker build --check` terminó sin advertencias para ambos Dockerfiles. Las
imágenes verificadas del corte final son:

| Artefacto | Tag | ID local | Bytes | Usuario |
|---|---|---|---:|---|
| aplicación | `logixone/app:j11-s9-07-closing` | `sha256:60f5de23f43e13991da30ef95be698c64f91862e38b9e75269cf13fd6d58d49a` | 501507736 | `jboss` |
| migrador | `logixone/migrator:j11-s9-07-closing` | `sha256:5e1d1db7de7a03451e368f60c021f341054c2b8de093a3d0f0b1c382b8e8fb95` | 105812331 | `10001:10001` |

El proyecto Compose aislado fue `logixone-j11-s9-07`. El migrador final se
ejecutó dos veces y ambas informaron cero cambios para `core` V6,
`plg_reference_data` V4, `plg_business_partners` V4,
`plg_commercial_catalog` V4, `plg_inventory` V2, `plg_purchasing` V2 y
`plg_reference_plugin` V1.

Antes y después de recrear únicamente `app` se obtuvieron exactamente los mismos
conteos:

| Dato | Filas |
|---|---:|
| socios | 11 |
| artículos de catálogo | 14 |
| empresas | 2 |
| activaciones | 7 |
| recepciones | 3 |
| artículos de inventario | 8 |
| órdenes | 3 |
| solicitudes | 3 |
| movimientos de stock | 11 |
| devoluciones | 3 |

La aplicación recreada quedó `healthy` y ejecutó exactamente la imagen de cierre.
Health terminó 2/2 y OIDC 4/4, sin fallos, errores u omisiones. El scan final no
encontró `ERROR`, `SEVERE`, `Exception` ni `Caused by:`. Registró dos advertencias
de arranque `WFLYELY00023`/`WFLYELY01084`: WildFly no encontró el keystore local y
lo autogenerará con certificado autofirmado para `localhost`; no son errores de
dominio, datos ni seguridad de la candidata.

## G5 — Seguridad, dependencias y demo responsive

La primera repetición acumulada encontró supuestos obsoletos del arnés E2E:
códigos semilla implícitos, etiquetas duplicadas en selectores, orden de
desactivación incompatible, carga asíncrona de páginas/tarjetas y comparación de
totales sin considerar la unidad creada por la prueba. El kernel rechazó
correctamente las composiciones inválidas. Se corrigieron únicamente los tres
recorridos afectados y cada prueba focal volvió a verde antes de repetir la matriz.

La suite oficial terminó:

- `BusinessPartnersVisualIT`: 1/1;
- `CommercialCatalogVisualIT`: 1/1;
- `InventoryVisualIT`: 1/1;
- `PurchasingVisualIT`: 1/1;
- `VisualDemoIT`: 5/5;
- **total: 9/9, 0 fallos, 0 errores y 0 omitidas**.

Playwright verificó login/logout, empresas A/B, administración, permisos, menús,
selectores, operaciones reales, dependencias, plugin inactivo, denegación,
restauración y ausencia de overflow en 375/599/600/720/839/840/1280 px. La
autoridad temporal de sistema terminó en `false`.

Se generaron 170 PNG, 26821078 bytes, en
`docs/evidence/screenshots/J11-S9-07-closing/e2e/`. Se revisaron originales de
shell, administración, directorios, estados de Compras, disponibilidad,
denegaciones y los tres rangos. No se observaron recortes, overflow horizontal ni
acciones esenciales perdidas. Checksums representativos:

- `purchasing-merged-workspace-compact-375.png`:
  `D1779B1451EA06B68F7FF0BD6F82DB3E3AEB1235252F046C794D2DED1EDDBE9A`;
- `purchasing-merged-workspace-medium-720.png`:
  `E148810D336D7C83CA25C635DB3BAAE433B1AB381E0D9EED050551F01185488A`;
- `purchasing-merged-workspace-expanded-1280.png`:
  `9A053EB09028C331E58A4387204908085CDC52EBEF98D3946BC2B831FCEDEF0C`;
- `purchasing-order-issued-expanded-1280.png`:
  `96EA10E78FEA9E56D70193DC832B2E30D6C466BE1433F06ACD9507A6FC81EEF7`;
- `purchasing-return-confirmed-medium-720.png`:
  `C747D16C075C30D3374382B24413A2CAED3B26587C42CBF77655EDF2E6028642`;
- `purchasing-disabled-denial-compact-375.png`:
  `E6F734C80B7CD46C8DE3BACA79E274C12771680742B09803DCF839A657BA4378`.

## G6 — Paquete documental y PDF

Se crearon o revisaron historia, evidencia, demo, retrospectiva, fotografía SVG,
manual de usuario, manual técnico, guía de implementación, ficha independiente,
Visual Studio Code, arquitectura, estrategia de pruebas, backlog e índices.

El manual 07 de Compras fue regenerado: 15 páginas, 279491 bytes, SHA-256
`24224FACEFF610015EA1DB83652A271FA6C51207C67B57667852167E5F6776F3`;
se renderizó y revisó por completo, sin cortes, solapamientos, páginas vacías ni
caracteres dañados.

Para el PDF de estructura se aprovisionaron exclusivamente bajo `.tools/`:

- Python 3.13.14, instalador oficial de 29225624 bytes, SHA-256
  `C54D9B9BBB8A36E6489363DDD01139707FD781D72F1F9E90C7EC65D0061368E0`;
- Poppler 26.02.0-0, paquete de 16107283 bytes, SHA-256
  `993E4A94376ED712FAFC7058D724EA0B943D118BBD2305CD9ED55174EB85CDA5`;
- librerías Python fijadas y sus hashes de PyPI registrados en
  `.tools/evidence/pdf-python-packages-2026-08-13.json`.

El PDF obligatorio se regeneró en
`docs/output/pdf/guia-estructura-repositorio-logixone.pdf`: **112 páginas**,
**444208 bytes**, SHA-256
**`40078523C450FECB8050B2F40A804C7D5216F6D0AAB9B11BD8D2DC998D6FD669`**.
`pypdf`, `pdfplumber` y
Poppler confirmaron metadatos, texto extraíble, cero páginas vacías, cero
caracteres de reemplazo y ausencia de cifrado. Las páginas se rasterizaron a 144
DPI y se revisaron en hojas de contacto, además de portada, índice, diagrama,
tablas densas y cierre a tamaño original.

## Pendientes que impiden el cierre formal

1. G7: una persona independiente debe completar
   `docs/implementation-guide/VALIDATION.md` contra `1.0-rc102`.
2. G8: producto debe responder `SÍ` o `NO` a la decisión de instalador. Sólo con
   `SÍ` se reconstruye y valida Windows; sin respuesta no se modifica `current`.

Hasta resolver ambos gates no se promueven imágenes, no se publica la guía `1.0`,
no se despliega a producción y no se presenta el corte como comercializable.
