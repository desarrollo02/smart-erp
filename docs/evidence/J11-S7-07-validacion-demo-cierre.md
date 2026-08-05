# Evidencia J11-S7-07 - Validación integral y demo de Sprint 7

- Fecha: 2026-07-31
- Estado: G0-G6 verdes; G7 independiente pendiente
- Historia: [J11-S7-07](../sprints/sprint-07/J11-S7-07-validacion-demo-cierre.md)
- Demo: [runbook de cierre](../runbooks/demo-cierre-sprint-07.md)

## Resultado ejecutivo

`commercial_catalog` quedó demostrado junto con `business_partners` sobre WildFly
41, PostgreSQL 18.4, Keycloak 26.7.0 y las imágenes verificadas finales. El shell
fusionó los menús autorizados y la demo ejecutó altas, consultas, clasificación,
identificación, lista y precio reales, además de denegación por plugin inactivo.

La demo visual está disponible. G7 continúa pendiente y exige el recorrido de una
persona independiente; este corte no autoriza promoción ni producción.

## G0 - Documentación y trazabilidad

Antes del cierre, el validador recorrió 197 Markdown y encontró cero enlaces rotos,
errores UTF-8, archivos con mojibake y filtraciones de secretos. Tras incorporar la
documentación y el PDF se repitió sobre el baseline final:

- Markdown: 203;
- enlaces rotos: 0;
- errores UTF-8: 0;
- mojibake: 0;
- filtraciones de secretos: 0.

## G1 - Reactor y arquitectura

Con JDK 21.0.11+10 y Maven Wrapper 3.9.16:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -Pwith-commercial-catalog-demo clean verify
```

Ambos terminaron con código 0 y 22/22 módulos. La composición completa produjo 81
reportes, 302 pruebas, 0 fallos, 0 errores y 0 omitidas. ArchUnit ejecutó 20/20
escenarios verdes.

La primera ejecución base terminó correctamente, pero su canal de salida se perdió
durante una compactación de la sesión. Para no aceptar un gate ambiguo se repitió
desde `clean`; la repetición oficial terminó `BUILD SUCCESS` en 1:55 min.

## G2 - Composición física

La variante base fue construida sin el perfil y los contratos físicos verificaron
cero implementaciones de plugin en el WAR y cero definiciones/proveedores de
plugin en el migrador. `with-commercial-catalog-demo` contiene cinco plugins
efectivos:

1. `business_partners`;
2. `commercial_catalog`;
3. `reference_plugin`;
4. `reference_custom_a`;
5. `reference_custom_b`.

El WAR final mide 1040380 bytes y tiene SHA-256
`66F1021245186125BA98BE051B74FA6DD1CEEDD4530B2EA49AA80628055407E6`.
El mismo hash fue calculado dentro de la imagen final.

## G3 - PostgreSQL, JPA y migraciones

El gate focalizado terminó con código 0 en 199,8 s:

```powershell
.\mvnw.cmd -Pwith-commercial-catalog-demo `
  -pl migrator,plugins/commercial-catalog -am verify `
  "-Dlogixone.postgres.integration=true"
```

| Módulo | Pruebas totales | Failsafe PostgreSQL | Fallos/errores/omitidas |
|---|---:|---:|---:|
| `commercial-catalog` | 56 | 12 | 0/0/0 |
| `migrator` | 31 | 12 | 0/0/0 |

Ambos resúmenes Failsafe informaron `timeout=false`, `flakes=0`, 12 completadas, 0
fallos, 0 errores y 0 omitidas.

## G4 - Docker, Compose, health, OIDC y persistencia

`docker build --check` terminó sin advertencias para ambos Dockerfiles. Las
imágenes se construyeron en modo `verified` con el mismo perfil:

| Artefacto | Tag local | Digest local |
|---|---|---|
| aplicación | `logixone/app:j11-s7-07-closing` | `sha256:769a078532b26e766675349c50b9dee0be134168aefcde1144b05dfc8e7f2975` |
| migrador | `logixone/migrator:j11-s7-07-closing` | `sha256:8343559f7accf81a4ef916415fd2248a51a70a29968ce012f5ba6e897e55bc0d` |

El migrador se ejecutó dos veces; en ambas informó cero migraciones para `core` V6,
`plg_business_partners` V1, `plg_commercial_catalog` V1 y
`plg_reference_plugin` V1. Sólo se recreó `app`; PostgreSQL, Keycloak y sus
volúmenes permanecieron en ejecución.

Los conteos antes y después del redespliegue coincidieron:

- socios: 24;
- artículos/servicios: 3;
- listas de precios: 2;
- entradas de precio: 1.

Después de la demo, los casos de uso dejaron 25, 4, 3 y 2 respectivamente. El
incremento de una fila por agregado corresponde a datos ficticios creados por
Playwright, no a pérdida o duplicación de migraciones.

El contenedor final ejecuta el digest registrado, descubrió cinco plugins y tres
unidades de persistencia. Sus 139 líneas iniciales tuvieron 0 coincidencias de
error. Liveness respondió HTTP 200 `UP`; readiness respondió HTTP 200 con
`catalog`, `configuration`, `database`, `migrations` y `oidc-configuration` en
`UP`.

Una consulta diagnóstica inicial usó por error `/api/health/live` y
`/api/health/ready`; el servidor devolvió el `401` esperado para `/api/*`. Se
repitió con las rutas públicas correctas `/health/live` y `/health/ready`, ambas
verdes. El error de ruta no se contabiliza como prueba del producto.

La matriz runtime final ejecutó health 2/2 y OIDC 4/4, sin fallos ni errores. Las 6
pruebas JTA quedaron explícitamente omitidas porque la sonda requiere el WAR de
arnés técnico y no forma parte de la distribución final.

## G5 - Seguridad, demo y responsive

La suite oficial contra la imagen final terminó:

- `BusinessPartnersVisualIT`: 1/1;
- `CommercialCatalogVisualIT`: 1/1;
- `VisualDemoIT`: 5/5;
- total: 7/7, 0 fallos, 0 errores y 0 omitidos.

Playwright verificó login/logout, empresa A/B, administración, permisos, menú
fusionado, operaciones reales, plugin inactivo, denegación, restauración y ausencia
de overflow en 375/599/600/720/839/840/1280 px.

La consulta final de la empresa A (`8f818892…`) confirmó
`business_partners=ENABLED` en versión de decisión 10 y
`commercial_catalog=ENABLED` en versión 4. Liveness/readiness permanecieron en
HTTP 200 `UP` y no aparecieron errores posteriores a la demo.

Se generaron 47 PNG en
`docs/evidence/screenshots/J11-S7-07-closing/e2e/`, con 9145521 bytes. Las cuatro
hojas de contacto y cinco originales a resolución completa fueron revisados; no se
encontraron recortes, overflow horizontal, acciones perdidas ni marcadores técnicos
vacíos. Checksums representativos:

- `admin-audit-expanded.png`:
  `8E444C94B2E29D0AB0254A22804A2C54C644AD5248B9F13ACBA02FF3BF9AFE81`;
- `business-partners-directory-compact-375.png`:
  `8ADC91A59EE0135DDABB2A55A8E98B69C045A6A463D94FB97909E27E63E181A1`;
- `catalog-items-directory-expanded-1280.png`:
  `DF3DA862289F8BB1D544081CD35AD500BDC3BB04558737FCBFCB99B810935DA3`;
- `price-list-detail-medium-720.png`:
  `6EDDA49A07797587E269BF9BB5E604164B2954D52703B0C251698873259ED14E`;
- `commercial-catalog-disabled-denial-compact-375.png`:
  `D50AB247127177822277390A01E250FD7E3912A422A933EA4CC29FAEBEE5BB16`.

## G6 - Guía, retrospectiva, siguiente Sprint y PDF

Se actualizaron la guía de implementación, la ficha independiente, el runbook, la
[retrospectiva](../sprints/sprint-07/retrospective.md), el roadmap y la
[planificación de Sprint 8](../sprints/sprint-08/README.md). El PDF obligatorio:

- ruta: `docs/output/pdf/guia-estructura-repositorio-logixone.pdf`;
- páginas: 53 A4;
- tamaño: 277345 bytes;
- SHA-256: `0C4AEFD26C23739D371568138D72369BCA3D84E8139EA6DF93C25F100F16CF33`;
- metadatos, texto, páginas vacías, cifrado, formularios y JavaScript:
  correctos; no cifrado, formulario, JavaScript, acciones de apertura/página,
  páginas vacías, controles extraños ni caracteres de reemplazo;
- render y revisión visual completa: 53/53 páginas a 144 dpi, cinco hojas de
  contacto y seis páginas críticas a resolución completa, sin defectos.

El primer candidato tenía 54 páginas y reveló una página casi vacía causada por un
salto forzado; se corrigió el generador y se repitieron validación y render. La
segunda revisión encontró la descripción histórica de 20 módulos en el inventario;
se corrigió a 22 y se ejecutó una tercera generación completa. Sólo la tercera se
promovió a la ruta estable y quedó byte a byte idéntica al candidato revisado.

### Adenda documental del 2026-07-31

Después del gate técnico, el responsable de producto incorporó cuatro entregables
obligatorios para los cierres presentes y futuros:

- [estructura de plugins y dependencias](../sprints/sprint-07/estructura-plugins-y-dependencias.md),
  con SVG portable, fuente Mermaid y alternativa textual;
- [guía para Visual Studio Code](../runbooks/levantar-logixone-visual-studio-code.md);
- [manual de usuario](../user-guide/README.md), alineado sin afirmación de
  certificación con referencias internacionales;
- [manual técnico para desarrolladores](../developer-guide/README.md).

`AGENTS.md`, el índice documental, arquitectura, backlog, Sprint 7 y la guía de
implementación `1.0-rc44` incorporan la nueva metodología. La validación documental
posterior recorrió 207 Markdown y obtuvo cero enlaces locales rotos, errores de
codificación, mojibake y coincidencias con los cuatro secretos locales.
El SVG resultó XML válido, conserva `title` y `desc` accesibles y su render
1440×900 fue revisado visualmente: texto, flechas, leyenda, plugins actuales y el
futuro `inventory` se leen sin recortes ni solapamientos. La tabla y la lectura
textual equivalentes también fueron contrastadas con POM y descriptores reales.

No se ejecutó Maven ni se reconstruyeron imágenes porque la adenda no modifica
código, POM, runtime, migraciones ni configuración desplegable. El PDF estable
conserva la evidencia del corte técnico anterior; como G7 sigue pendiente, deberá
regenerarse contra el baseline documental final antes de declarar el cierre formal
de Sprint 7.

## G7 - Validación independiente

Pendiente. Requiere una persona que no haya implementado las capacidades y que
complete `docs/implementation-guide/VALIDATION.md` sin asistencia oral. Hasta su
dictamen Sprint 7 no se declara formalmente cerrado, la guía permanece `1.0-rc`,
las imágenes no se promueven y no se autoriza producción.
