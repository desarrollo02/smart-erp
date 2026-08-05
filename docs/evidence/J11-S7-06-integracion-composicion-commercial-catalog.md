# Evidencia J11-S7-06 - Integración y composición de `commercial_catalog`

- Fecha: 2026-07-30
- Resultado: Verde
- Alcance: perfil físico, WAR/migrador, PostgreSQL, Docker, permisos y demo
- Historia: [J11-S7-06](../sprints/sprint-07/J11-S7-06-integracion-composicion-commercial-catalog.md)

## Composición física

`PhysicalPluginSetBuildContractTest` exige que
`with-commercial-catalog-demo` exista únicamente en
`distribution/logixone-plugin-set`, que WAR y migrador dependan del mismo set y que
los Dockerfiles acepten ese perfil en `verified` y `visual-candidate`.

La construcción con perfil incorporó los JAR de `business-partners`,
`commercial-catalog`, sus dos APIs, `reference-plugin` y las personalizaciones A/B.
El provider combinado del migrador contiene cinco definiciones no vacías. WildFly
registró en runtime:

```text
plugin_count=5
business_partners@1.0.0
commercial_catalog@1.0.0
reference_plugin@1.0.0
reference_custom_a@1.0.0
reference_custom_b@1.0.0
```

Una construcción posterior con `clean` y sin perfil produjo:

```text
BASE_WAR_PLUGIN_ENTRIES=0
BASE_MIGRATOR_PLUGIN_DEFINITION_CLASSES=0
BASE_MIGRATOR_PROVIDER_LINES=0
```

## Reactor, arquitectura y PostgreSQL

```powershell
.\mvnw.cmd -Pwith-commercial-catalog-demo verify
.\mvnw.cmd -Pwith-commercial-catalog-demo `
  -pl migrator,plugins/commercial-catalog -am verify `
  "-Dlogixone.postgres.integration=true"
```

Resultados:

- reactor: 22/22 módulos verdes en 1 min 27 s;
- Surefire: 81 reportes, 302 pruebas, 0 fallos, 0 errores, 0 omitidas;
- `commercial-catalog`: 44/44 pruebas verdes;
- ArchUnit/composición: 20/20 pruebas verdes;
- PostgreSQL del catálogo: 12/12 escenarios, 0 fallos, errores u omisiones;
- PostgreSQL del migrador: 12/12 escenarios, 0 fallos, errores u omisiones.

El comando PostgreSQL superó la ventana externa de dos minutos; el proceso Maven
continuó y finalizó. Se verificaron sus tres reportes XML y ambos
`failsafe-summary.xml`: 24 escenarios completados, sin timeout, flakes, fallos,
errores u omisiones. No se contabilizó el timeout externo como prueba verde hasta
comprobar esos artefactos finales.

## Imágenes, migraciones y salud

| Artefacto | Tag local | Digest local |
|---|---|---|
| aplicación | `logixone/app:j11-s7-06-commercial-catalog-demo` | `sha256:dd64ebbfeef59a071f1166b7100bd874e2235f1bcdb8dfc602580f60fc24513b` |
| migrador | `logixone/migrator:j11-s7-06-commercial-catalog-demo` | `sha256:a226525545c57e223ad1a1430b7e4453c8ce6ad2787f17c1de99b237bbda51b2` |

`docker build --check` terminó sin advertencias. Ambos builds usaron modo
`verified` y el perfil `with-commercial-catalog-demo`.

El migrador se ejecutó dos veces sobre el volumen existente. La primera aplicó
V1 de `commercial_catalog`; la segunda informó cero cambios y conservó `core` V6,
`business_partners` V1 y `reference_plugin` V1.

El fixture demo se aplicó repetidamente. Después de reparar la codificación de tres
nombres controlados, la ejecución siguiente informó cinco `INSERT 0 0` y conservó:

```text
active_units=2
active_categories=1
active_brands=1
active_tax_profiles=1
```

PostgreSQL y Keycloak reutilizaron sus volúmenes. No se ejecutó
`down --volumes`, SQL destructivo, `repair` de Flyway ni edición de una migración
aplicada.

Estado observado después de la demo:

```text
business_partners=ENABLED
commercial_catalog=ENABLED
reference_plugin=ENABLED
commercial_catalog permissions=4
catalog items=3
price lists=2
price entries=1
```

- `/health/live`: HTTP 200 `UP`;
- `/health/ready`: HTTP 200 `UP` con catálogo, configuración, base, migraciones y
  OIDC;
- contenedor `app`: `healthy`;
- logs de aplicación revisados: 922 líneas, 0 coincidencias de error.

## Playwright, seguridad y responsive

`CommercialCatalogVisualIT` ejecutó un escenario completo en 31,64 s, con 0
fallos, 0 errores y 0 omitidos. Autenticó por OIDC, seleccionó empresa, habilitó el
plugin, concedió los cuatro permisos al rol demo y verificó el menú fusionado.

El recorrido registró un artículo/servicio, agregó identificador y clasificación,
creó una lista de precios y una entrada efectiva. Luego deshabilitó el plugin,
comprobó denegación de la ruta directa y lo restauró habilitado. Todas las
mutaciones usaron UI y casos de uso reales; sólo las definiciones controladas de
preparación provienen del fixture privado de prueba.

Se comprobó ausencia de overflow en 375, 599, 600, 720, 839, 840 y 1280 px, carga
del tema Material Design, un único `h1` y labels para controles editables.

Las doce capturas fueron abiertas y revisadas. No muestran controles cortados,
caracteres dañados, superposición ni overflow horizontal normal.

| Vista | 375 px | 720 px | 1280 px |
|---|---|---|---|
| Directorio de artículos | [captura](screenshots/J11-S7-06/e2e/catalog-items-directory-compact-375.png) | [captura](screenshots/J11-S7-06/e2e/catalog-items-directory-medium-720.png) | [captura](screenshots/J11-S7-06/e2e/catalog-items-directory-expanded-1280.png) |
| Ficha de artículo | [captura](screenshots/J11-S7-06/e2e/catalog-item-detail-compact-375.png) | [captura](screenshots/J11-S7-06/e2e/catalog-item-detail-medium-720.png) | [captura](screenshots/J11-S7-06/e2e/catalog-item-detail-expanded-1280.png) |
| Ficha de lista | [captura](screenshots/J11-S7-06/e2e/price-list-detail-compact-375.png) | [captura](screenshots/J11-S7-06/e2e/price-list-detail-medium-720.png) | [captura](screenshots/J11-S7-06/e2e/price-list-detail-expanded-1280.png) |

Control negativo: [captura compacta](screenshots/J11-S7-06/e2e/commercial-catalog-disabled-denial-compact-375.png).

## Incidencias tratadas como bloqueos

1. PowerShell interpretó inicialmente `-Dit.test` sin comillas; Maven no ejecutó
   pruebas. Se corrigió el comando.
2. El primer recorrido encontró dos headings con el mismo texto. La aserción se
   ajustó al único `main h1`, conservando la exigencia de accesibilidad.
3. El fixture canalizado a `psql` degradó acentos. Se reemplazaron tres literales
   por escapes Unicode PostgreSQL y `DO UPDATE ... IS DISTINCT FROM`; la siguiente
   ejecución quedó sin cambios.
4. “Precios” coincidía también con “Listas de precios”. El locator de la pestaña se
   hizo exacto.

Después de cada corrección se repitió el escenario completo. No se omitieron ni
relajaron comprobaciones funcionales.

## Capturas y checksums

```text
SCREENSHOTS=12
SCREENSHOT_BYTES=1320454
catalog-item-create-compact-375.png 793f819b7c13382bec8970e941afcfcf303681e47f8df14227840171dc633ee9
catalog-item-detail-compact-375.png 1a15f756abbc7de7e597f0da75b69a883ff6f75464e8041dfb7a8f1ac379bea5
catalog-item-detail-expanded-1280.png c4c301e8187ca984dca3c5ba90399bbdc2818ee7981ed74457a71c7235f73ce5
catalog-item-detail-medium-720.png 1616523c71b7581e218a4709d238044d443bc7d42cda1b995ea6422ffd43e3f5
catalog-items-directory-compact-375.png 6d3c18f6f04a8a959ed94fcc2643726390b423ae5594807efa4fbba1c069e441
catalog-items-directory-expanded-1280.png 2c1a07a21b5804cec0847ac6a336ea7fdb87e877c48c920c830faa734c72c54c
catalog-items-directory-medium-720.png d54eabf06ed6f476b05c511ca416f46413e855e32095c26a27440928f73dc2c7
commercial-catalog-disabled-denial-compact-375.png d50ab247127177822277390a01e250fd7e3912a422a933ea4cc29faebee5bb16
price-list-detail-compact-375.png c861446e9b4bd84c70a54011e882dd797d3b6d32c0d8948762f155ed3fb381b9
price-list-detail-expanded-1280.png 36c0670dc051795382ff9ba58e984caf3369dbdc8832c84fad1bde834f4cb264
price-list-detail-medium-720.png 813e6cf1aa3073084ba454162ac306425add092e8cb335e85ee54cd664b4fd3b
price-lists-directory-compact-375.png 398441ea45dac2fa9d850b551682a5c5c75545a803d8e74302998d85dae9e015
```

## Pendientes

- gate integral y demo oficial de `J11-S7-07`;
- retrospectiva y planificación del siguiente corte;
- PDF obligatorio de cierre de Sprint 7;
- validación independiente transversal de la guía candidata;
- publicación o promoción de imágenes: no autorizada por esta historia.

## Gate documental

`tmp/validate_docs.py` recorrió 197 archivos Markdown:

```text
BROKEN_LINKS=0
ENCODING_ERRORS=0
MOJIBAKE_FILES=0
SECRET_LEAKS=0
```

La guía de implementación avanzó a `1.0-rc42` y mantiene pendiente su validación
independiente transversal.
