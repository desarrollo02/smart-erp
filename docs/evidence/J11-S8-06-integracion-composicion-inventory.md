# Evidencia J11-S8-06 - Integración y composición de `inventory`

- Fecha: 2026-08-01
- Resultado: Verde
- Alcance: perfil físico, WAR/migrador, PostgreSQL, Docker, seguridad y demo
- Historia: [J11-S8-06](../sprints/sprint-08/J11-S8-06-integracion-composicion-inventory.md)

## Composición física

`PhysicalPluginSetBuildContractTest` verifica que `with-inventory-demo` exista en
`distribution/logixone-plugin-set`, que WAR y migrador dependan del mismo set y que
los Dockerfiles acepten el perfil en `verified` y `visual-candidate`.

La construcción incorporó tres implementaciones productivas, sus tres APIs,
`reference-plugin` y las personalizaciones A/B. El registro descubre seis
definiciones:

```text
plugin_count=6
business_partners@1.0.0
commercial_catalog@1.0.0
inventory@1.0.0
reference_plugin@1.0.0
reference_custom_a@1.0.0
reference_custom_b@1.0.0
```

`inventory` declara como requerida la dependencia
`commercial_catalog [1.0.0,2.0.0)`. ArchUnit conserva `inventory-api` y dominio
libres de Jakarta y prohíbe dependencias hacia internos del catálogo. No existen
FK, relaciones JPA ni consultas cruzadas hacia `plg_commercial_catalog`.

## Pruebas focalizadas

- contrato de composición: 2/2 pruebas verdes;
- movimiento de inventario: 3/3 pruebas verdes;
- renderer y corrección responsive del shell: 2/2 pruebas verdes;
- PostgreSQL/JPA de inventario: 6/6 escenarios verdes sobre PostgreSQL 18.4;
- `InventoryVisualIT`: 1/1 escenario verde en 43,699 s;
- recorrido visual finalizado: `2026-08-01T00:56:14-03:00`.

El escenario PostgreSQL adicional ejecuta `blocks(...)` bajo cumplimiento JPA
estricto. Protege la corrección del alias JPQL reservado encontrada durante la
primera ejecución del movimiento.

## Imágenes, migraciones y salud

| Artefacto | Tag local | Digest local |
|---|---|---|
| aplicación | `logixone/app:j11-s8-06-inventory-demo` | `sha256:4d6806d247222234992b42926d0717e7b52dfcbcb9cd468a4a0e916b11ece6be` |
| migrador | `logixone/migrator:j11-s8-06-inventory-demo` | `sha256:60d48fd0a67efb803f296bac93b048d5fbab29ca5559b38d9b8c0c15b08fda18` |

`docker build --check` terminó sin advertencias. Ambos builds usaron
`with-inventory-demo`; el WAR inspeccionado contiene los JAR exactos del perfil.

El migrador se ejecutó dos veces sobre el volumen conservado. La primera aplicó V1
y V2 de `plg_inventory`; la segunda informó cero cambios para núcleo y todos los
plugins. El fixture idempotente de la empresa A conservó dos unidades activas, una
categoría, una marca y un perfil tributario.

No se ejecutó `down --volumes`, SQL destructivo, `repair` de Flyway ni edición de
una migración aplicada. Al finalizar:

- aplicación: imagen esperada, `running` y `healthy`;
- PostgreSQL 18.4: `running` y `healthy` con volumen conservado;
- Keycloak 26.7.0: `running` y `healthy` con volumen conservado;
- `/health/live`: HTTP 200 `UP`;
- `/health/ready`: HTTP 200 `UP`.

## Playwright, seguridad y responsive

`InventoryVisualIT` autenticó por OIDC, seleccionó la empresa A, habilitó las
dependencias, concedió permisos y abrió una sesión nueva para obtener las
autoridades actualizadas. El menú resultante fusionó los tres plugins productivos.

El recorrido creó por la UI un producto de catálogo, depósito y ubicación,
inscripción de inventario, entrada de 12, reserva de 3, disponibilidad 9 y conteo
contabilizado con cero diferencias. Luego deshabilitó `inventory`, comprobó la
denegación de ruta y lo restauró. Ninguna operación funcional se preparó con SQL.

Se verificó ausencia de overflow en 375, 599, 600, 720, 839, 840 y 1280 px. Las 23
capturas fueron revisadas visualmente; no presentan controles cortados,
superposición, caracteres dañados ni desplazamiento horizontal normal.

| Vista | 375 px | 720 px | 1280 px |
|---|---|---|---|
| Menú fusionado | [captura](screenshots/J11-S8-06/e2e/inventory-merged-workspace-compact-375.png) | [captura](screenshots/J11-S8-06/e2e/inventory-merged-workspace-medium-720.png) | [captura](screenshots/J11-S8-06/e2e/inventory-merged-workspace-expanded-1280.png) |
| Depósito | [captura](screenshots/J11-S8-06/e2e/inventory-warehouse-detail-compact-375.png) | [captura](screenshots/J11-S8-06/e2e/inventory-warehouse-detail-medium-720.png) | [captura](screenshots/J11-S8-06/e2e/inventory-warehouse-detail-expanded-1280.png) |
| Existencias | [captura](screenshots/J11-S8-06/e2e/inventory-stock-directory-compact-375.png) | [captura](screenshots/J11-S8-06/e2e/inventory-stock-directory-medium-720.png) | [captura](screenshots/J11-S8-06/e2e/inventory-stock-directory-expanded-1280.png) |
| Conteo contabilizado | [captura](screenshots/J11-S8-06/e2e/inventory-count-posted-compact-375.png) | [captura](screenshots/J11-S8-06/e2e/inventory-count-posted-medium-720.png) | [captura](screenshots/J11-S8-06/e2e/inventory-count-posted-expanded-1280.png) |

Controles adicionales:

- [disponibilidad compacta](screenshots/J11-S8-06/e2e/inventory-stock-availability-compact-375.png);
- [movimiento expandido](screenshots/J11-S8-06/e2e/inventory-stock-movement-expanded-1280.png);
- [reserva media](screenshots/J11-S8-06/e2e/inventory-stock-reservation-medium-720.png);
- [denegación con plugin inactivo](screenshots/J11-S8-06/e2e/inventory-disabled-denial-compact-375.png).

Una inspección independiente en el navegador confirmó a 375 px un ancho de
documento igual al viewport y el conteo final en estado **Contabilizado**, con una
línea, una contada y cero diferencias.

## Capturas y checksums

```text
SCREENSHOTS=23
SCREENSHOT_BYTES=2967114
inventory-count-posted-compact-375.png bc5053a1346e6371e651478455886554d034fa4443c4bd7db9d4cc06505f0ea0
inventory-count-posted-expanded-1280.png e4232d7a698dd2143525cd257e448ad2a77425c4ae67bab8fa3c53d7b2af6e78
inventory-count-posted-medium-720.png b3eb5cf81289ee701567273e4bf439356479f18d7bae94e3fde2fc612610d280
inventory-counts-directory-compact-375.png 596cbe58e29a859cbd4dd3300ab99db2e6e116ab01fa128328e60c2d4a2f69d1
inventory-counts-directory-expanded-1280.png 01edfbe43e947373b50ee636cd34434c84f1641b15bf7ac37fb4d99f163654b2
inventory-counts-directory-medium-720.png b7219444b0a5aaa3013a9b7ccb9b57dd313a116bb1398aec482018d3f0301148
inventory-disabled-denial-compact-375.png d50ab247127177822277390a01e250fd7e3912a422a933ea4cc29faebee5bb16
inventory-merged-workspace-compact-375.png fa6a842c2e3a3f35c402c721cad7ff8fa43a1ddc51013cda5b620255f427d941
inventory-merged-workspace-expanded-1280.png 40837f620d3e984383e6c667fcbac05dfef6572ac1d5c93165968dafdf034387
inventory-merged-workspace-medium-720.png bcaa04fea6c6d1844d882be8fb2b127508491ff621c478c23a9502519698c7eb
inventory-stock-availability-compact-375.png 5f68f406c13fabeee644cc25080d8c8439917afa15ec37422d8d07996063848d
inventory-stock-directory-compact-375.png aba5dedbd3197b06b9734d16957774b4df88d4f91ce5710a3d3996975efdf4f5
inventory-stock-directory-expanded-1280.png 4a829387ef66572d5f653669467e8d002afe718c0babc52e3777517933c1c812
inventory-stock-directory-medium-720.png f6cb932062b0852fa61c02e00d7dfe0c44e4d93c9048e230409a4b7994f731e8
inventory-stock-movement-expanded-1280.png a1bcf46261ae717c7822f94a665ef2a2301c0d66a3aa0a0711f35c4e1b238e74
inventory-stock-reservation-medium-720.png eab347c855cc1da09964f5b3cb2ac0d00e9f4a03d12c938e8dfcc990dde245ed
inventory-warehouse-create-compact-375.png 2fa1a6db3cf00fcdab91479f101b1983d8a186a41587e1b89335d32551e01595
inventory-warehouse-detail-compact-375.png d246a9138750ba1534e2032b3335a3e8448e96e04c26a1e617715cb4c6425299
inventory-warehouse-detail-expanded-1280.png 07226947837f78e2a48d8ed9a9fa6155344cdac0ad512873959e04f59928bf00
inventory-warehouse-detail-medium-720.png 4a8980393165c80bca561ac444d593173f1712682b8ede660bfacd334b909c05
inventory-warehouses-directory-compact-375.png 6b0796b42dce0daefeaa644b7ce7258f65a977338254abdaa34a9a55c25b83b6
inventory-warehouses-directory-expanded-1280.png 1588f8a1928dbbe8387b8400a794cedd862213f2bb66cb3520760efae32968a9
inventory-warehouses-directory-medium-720.png 9b8772759353f08bfbd756f764fea1767266715957733f66366744683dc6dc78
```

## Incidencias corregidas

1. Hibernate con cumplimiento JPQL estricto rechazó `count` como alias reservado en
   la consulta que detecta bloqueos de conteo. Se renombró a `stockCount`, se agregó
   logging técnico seguro y una prueba PostgreSQL de regresión.
2. Los datos persistentes de ejecuciones anteriores generaban múltiples depósitos
   y controles con el mismo nombre visible. Los locators se acotaron por región y
   el escenario selecciona la ubicación del depósito creado, sin borrar historia.
3. Después de conceder permisos, la sesión existente conservaba su snapshot de
   autoridad. El escenario inicia una sesión nueva antes de probar el menú.
4. El encabezado compacto podía superar el ancho por el selector de empresa. Los
   hijos flexibles ahora permiten ajuste y el selector ocupa su propia fila.

Después de cada corrección se repitió la prueba mínima correspondiente y el
recorrido completo. No se omitieron ni relajaron comprobaciones funcionales.

## Pendientes

- gate integral y demo oficial de J11-S8-07;
- retrospectiva, fotografía de plugins y PDF obligatorio de Sprint 8;
- congelación del baseline y creación del instalador Windows en J11-S8-08;
- validación independiente transversal pendiente;
- publicación o promoción de imágenes: no autorizada por esta historia.

## Gate final de la historia

```powershell
.\mvnw.cmd -B -Pwith-inventory-demo verify
```

Resultado final del `2026-08-01T01:12:56-03:00`:

- reactor: 24/24 módulos con `BUILD SUCCESS` en 1 min 13 s;
- Surefire: 104 reportes, 369 pruebas, 0 fallos, 0 errores y 0 omitidas;
- arquitectura: 24/24, incluidas 20 reglas ArchUnit y 2 contratos de composición;
- WAR final: nueve JAR del perfil, seis implementaciones/descriptores y tres APIs;
- catálogo CDI desplegado: `plugin_count=6` con las identidades esperadas;
- Compose efectivo: configuración válida; `app`, `postgres` y `keycloak` activos;
- liveness/readiness: HTTP 200 y estado `UP`;
- log de la aplicación desde el arranque de la imagen final: 0 coincidencias de
  `ERROR`, `SEVERE`, `Exception` o `Caused by:`;
- documentación: 220 Markdown, 0 enlaces locales rotos y 0 archivos con mojibake.

Este gate completa J11-S8-06, pero no sustituye la repetición integral ni los
artefactos obligatorios de cierre de J11-S8-07/J11-S8-08.
