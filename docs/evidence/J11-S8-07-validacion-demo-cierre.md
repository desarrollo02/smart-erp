# Evidencia J11-S8-07 - Validación integral y demo oficial de Sprint 8

- Fecha: 2026-08-01
- Estado: G0-G6 verdes; G7 independiente y G8 instalador pendientes
- Historia: [J11-S8-07](../sprints/sprint-08/J11-S8-07-validacion-demo-cierre.md)
- Demo: [runbook de cierre](../runbooks/demo-cierre-sprint-08.md)
- Perfil: `with-inventory-demo`

## Resultado ejecutivo

`inventory` quedó demostrado junto con `commercial_catalog` y
`business_partners` sobre WildFly 41, PostgreSQL 18.4 y Keycloak 26.7.0. El shell
fusionó siete menús autorizados y la demo ejecutó depósitos, inscripción de
productos, movimientos, reservas, disponibilidad y conteos reales.

La demo visual nueva está disponible. J11-S8-07 congela el baseline técnico; no
cierra formalmente Sprint 8 porque faltan la validación independiente y el
instalador Windows J11-S8-08.

## G0 - Documentación y protección de información

El control final recorrió 236 archivos Markdown: cero enlaces locales rotos, cero
errores de codificación UTF-8, cero archivos con caracteres de reemplazo o
mojibake y cero coincidencias con los cuatro secretos locales. La fotografía SVG
es XML válido, contiene `title` y `desc`, y fue inspeccionada visualmente.

## G1 - Reactor y arquitectura

Con JDK 21.0.11+10 y Maven Wrapper 3.9.16 se ejecutaron:

```powershell
.\mvnw.cmd -B clean verify
.\mvnw.cmd -B -Pwith-inventory-demo clean verify
```

Ambos terminaron con código 0 y 24/24 módulos. La composición completa produjo 104
reportes normales, 369 pruebas, 0 fallos, 0 errores y 0 omitidas. ArchUnit ejecutó
24/24 escenarios verdes.

Una primera repetición PostgreSQL usó accidentalmente el JDK 8 del sistema y Maven
Enforcer la detuvo antes de compilar. Se corrigió el entorno para apuntar al JDK 21
local y el gate oficial terminó verde. Este fallo de preparación no fue una prueba
fallida del producto.

## G2 - Composición física

La variante base conserva cero implementaciones de plugins. La composición
`with-inventory-demo` contiene seis definiciones físicas:

1. `business_partners`;
2. `commercial_catalog`;
3. `inventory`;
4. `reference_plugin`;
5. `reference_custom_a`;
6. `reference_custom_b`.

El WAR incluye las tres API públicas de plugins productivos y las seis
implementaciones esperadas. WAR y migrador consumen la misma selección de
`logixone-plugin-set`.

| Artefacto | Bytes | SHA-256 |
|---|---:|---|
| `logixone.war` | 1324240 | `DE7048F21141C8E2B1CEDC36D829F63A04B57E25863BD95CDF65B7BE2DABDD89` |
| migrador ejecutable | 5998576 | `0BCC58A51EB4EEFD25A964FB230F3D8C4E0ABF5F8241798D8A2A9279EC9B4EF4` |

Los hashes calculados dentro de ambas imágenes coinciden exactamente con los
artefactos locales.

## G3 - PostgreSQL, JPA/JTA y migraciones

El gate PostgreSQL focalizado terminó verde en 4:22 min:

| Propietario | Pruebas | Fallos/errores/omitidas |
|---|---:|---:|
| `commercial_catalog` | 12 | 0/0/0 |
| `inventory` | 12 | 0/0/0 |
| `business_partners` | 14 | 0/0/0 |
| migrator/core | 12 | 0/0/0 |
| **Total** | **50** | **0/0/0** |

Se validaron aislamiento por empresa, constraints, concurrencia, JPA en modo
`validate`, rollback JTA, checksums e idempotencia. No existe relación JPA, `JOIN`
ni lectura directa entre los esquemas privados de catálogo e inventario.

## G4 - Docker, Compose, health, OIDC y persistencia

Los dos `docker build --check` terminaron sin advertencias. Las imágenes verificadas
del baseline final son:

| Artefacto | Tag | ID/digest local | Bytes |
|---|---|---|---:|
| aplicación | `logixone/app:j11-s8-07-closing` | `sha256:a44293d0bc1a0df01e4e13025a6bc202266dec82fa6bb5f74f858cd70667d4fb` | 500839377 |
| migrador | `logixone/migrator:j11-s8-07-closing` | `sha256:bcf5a51b535c30cb466a10d782f6059bc383ea8db8360575f01a52086451fd81` | 105224222 |

Docker Desktop estaba detenido al iniciar este gate. Se inició el motor y se
confirmaron los tres contenedores existentes antes de continuar; no se eliminaron
recursos ni volúmenes.

El migrador se ejecutó dos veces. Ambas informaron cero migraciones pendientes para
`core` V6, `plg_business_partners` V1, `plg_commercial_catalog` V1,
`plg_inventory` V2 y `plg_reference_plugin` V1.

Antes de recrear la aplicación se registraron estos conteos:

`27|16|7|6|8|8|6|4|4`

El orden corresponde a socios, artículos, listas, precios, depósitos, artículos de
inventario, movimientos, reservas y conteos. Después de recrear `app`, los nueve
valores fueron exactamente iguales. PostgreSQL y Keycloak conservaron sus
contenedores y volúmenes.

Liveness respondió HTTP 200 `UP`. Readiness respondió HTTP 200 con `catalog`,
`configuration`, `database`, `migrations` y `oidc-configuration` en `UP`. El scan
de logs posterior al despliegue encontró cero coincidencias de `ERROR`, `SEVERE`,
`Exception` o `Caused by:`.

Una consulta diagnóstica incidental usó primero `/logixone/api/health/*` y recibió
la redirección esperada de la frontera protegida. Se repitió con las rutas públicas
correctas `/logixone/health/live` y `/logixone/health/ready`; ambas quedaron
verdes. La ruta equivocada no se contabiliza como prueba del producto.

La matriz runtime final ejecutó health 2/2 y OIDC 4/4, sin fallos ni errores. Las
seis pruebas JTA del arnés quedaron explícitamente omitidas porque ese WAR técnico
no forma parte de la distribución final.

## G5 - Seguridad, dependencias y demo responsive

La primera suite acumulada detectó una expectativa desactualizada: intentaba
desactivar catálogo mientras inventario seguía activo. El kernel rechazó
correctamente la operación por la dependencia requerida. Se corrigió únicamente
`CommercialCatalogVisualIT` para comprobar:

1. rechazo de la composición incompatible y catálogo todavía habilitado;
2. desactivación previa de inventario;
3. desactivación y denegación de catálogo;
4. restauración en orden catálogo, luego inventario.

La prueba focal y la suite oficial completa terminaron verdes:

- `BusinessPartnersVisualIT`: 1/1;
- `CommercialCatalogVisualIT`: 1/1;
- `InventoryVisualIT`: 1/1;
- `VisualDemoIT`: 5/5;
- **total: 8/8, 0 fallos, 0 errores y 0 omitidas**.

Playwright verificó login/logout, empresa A/B, administración, permisos, menú
fusionado, operaciones reales, dependencias, plugin inactivo, denegación,
restauración y ausencia de overflow en 375/599/600/720/839/840/1280 px.

Se generaron 70 PNG en
`docs/evidence/screenshots/J11-S8-07-closing/e2e/`, con 12479257 bytes. Se revisaron
originales de shell, administración, directorios, disponibilidad, conteo y
denegación en los tres rangos. No se encontraron recortes, overflow horizontal,
acciones perdidas ni contenido técnico vacío. Checksums representativos:

- `admin-plugins-expanded.png`:
  `9C56EB371EE07E0FCA8B1AC8439AB6F7C3FC148D83EEAB6AC61B3EE9804C4403`;
- `inventory-merged-workspace-compact-375.png`:
  `411B5A0CEDFDDBD09368D7307937722758F5494529B50AAC65A14AF0EE19C7BD`;
- `inventory-merged-workspace-medium-720.png`:
  `73DB25583C5A56E2394AD6ED980FE343E0C7C359A2548138D351C71DEFB5438D`;
- `inventory-merged-workspace-expanded-1280.png`:
  `897CB5A4D31A37AB7572751048A6CB0E2FD3518F8AD502E2DEEBED86F0CEFC18`;
- `inventory-count-posted-expanded-1280.png`:
  `E3BAD3817126F1C8AB4D21376B6C1A2F8B31A10899AA6B50A08B997EA2A3EEAC`;
- `inventory-disabled-denial-compact-375.png`:
  `D50AB247127177822277390A01E250FD7E3912A422A933EA4CC29FAEBEE5BB16`.

La demo se dejó abierta en la pantalla real **Existencias** de la empresa ficticia
A, con sus nueve registros y el menú fusionado visible.

## G6 - Paquete documental y PDF

Se crearon o revisaron:

- [demo de cierre](../runbooks/demo-cierre-sprint-08.md);
- [fotografía de plugins](../sprints/sprint-08/estructura-plugins-y-dependencias.md)
  y su SVG accesible;
- [retrospectiva](../sprints/sprint-08/retrospective.md);
- manual de usuario, manual técnico, guía de implementación, Visual Studio Code e
  IntelliJ IDEA;
- índices, arquitectura, roadmap y estado de Sprint 8.

El PDF obligatorio fue regenerado contra este baseline y promovido a
`docs/output/pdf/guia-estructura-repositorio-logixone.pdf`: 63 páginas, 306.270
bytes y SHA-256
`347CCFD7898AAE34D798216539B7115D0F545854AD3F11597D6CCD12F6DE2BF5`.
`pypdf` confirmó metadatos, texto extraíble, cero páginas vacías, cero caracteres
de reemplazo, ausencia de cifrado y ausencia de acciones o formularios. Las 63
páginas se rasterizaron a 144 DPI; se revisaron seis hojas de contacto y las
páginas críticas a tamaño original, sin cortes, tablas ilegibles, caracteres
dañados ni defectos de composición.

## Pendientes que impiden el cierre formal

1. G7: una persona independiente debe completar
   `docs/implementation-guide/VALIDATION.md`.
2. G8: J11-S8-08 debe construir y verificar el instalador Windows contra los
   digests congelados de esta evidencia.

Hasta resolver ambos gates no se promueven imágenes, no se publica la guía `1.0`,
no se entrega un instalador productivo y no se autoriza producción.
