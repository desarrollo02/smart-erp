# Evidencia J11-S6-06 - Integración y composición de `business_partners`

- Fecha: 2026-07-29
- Resultado: Verde, incluida la corrección visual de aceptación
- Alcance: selección física, artefactos, migraciones, Docker, persistencia y demo

## Contrato de composición

`PhysicalPluginSetBuildContractTest` verifica que
`with-business-partners-demo` existe sólo como selección en
`distribution/logixone-plugin-set`, contiene exactamente los cuatro plugins
esperados, que WAR/migrador sólo dependen del plugin set y que ambos Dockerfiles
aceptan los modos `verified` y `visual-candidate` para el mismo perfil.

El build con perfil produjo en `WEB-INF/lib`:

```text
business-partners-0.1.0-SNAPSHOT.jar
business-partners-api-0.1.0-SNAPSHOT.jar
reference-plugin-0.1.0-SNAPSHOT.jar
reference-customization-a-0.1.0-SNAPSHOT.jar
reference-customization-b-0.1.0-SNAPSHOT.jar
```

El servicio combinado del migrador enumeró `BusinessPartnersPluginDefinition`,
`ReferencePluginDefinition`, `ReferenceCustomizationADefinition` y
`ReferenceCustomizationBDefinition`. Una construcción posterior con `clean` y sin
perfil produjo cero bibliotecas de plugins en el WAR, cero providers y cero clases
de definición de plugins en el ejecutable del migrador.

## Corrección visual de aceptación

La primera candidata fue rechazada por organización y densidad: acumulaba búsqueda,
alta, detalle y todas las operaciones en una sola página vertical. Se aprobó
[ADR-0018](../adr/0018-floorplan-erp-directorio-alta-ficha.md) y se mantuvieron sin
cambios `plugin-api` 0.4.0, `ScreenInteraction`, permisos, casos de uso, JPA y
migraciones.

La UI corregida incorpora navegación persistente/colapsable y tres modos separados:

- directorio con filtros y tabla en expandido;
- directorio como lista adaptable en medio y compacto;
- alta enfocada en datos principales;
- ficha con resumen de lectura y pestañas por tarea.

La pantalla productiva dejó de mostrar `ScreenId`, versión optimista, slots y texto
explicativo de JTA. Esos controles permanecen activos en el servidor y documentados
fuera de la superficie operativa.

## Pruebas Maven y PostgreSQL

```powershell
.\mvnw.cmd -B -Pwith-business-partners-demo verify
.\mvnw.cmd -B -Pwith-business-partners-demo,postgres-integration `
  '-Dlogixone.postgres.integration=true' -pl migrator -am verify
```

Resultados:

- reactor con perfil: 20 módulos verdes en 1 min 03 s;
- reactor base sin perfil: 20 módulos y 243 pruebas verdes en 1 min 06 s;
- Surefire: 243 pruebas en 63 suites, 0 fallos y 0 errores;
- arquitectura: 17 pruebas, incluidas las 2 nuevas de composición;
- PostgreSQL de `business_partners`: 14 escenarios verdes;
- migrador PostgreSQL: 12 escenarios verdes;
- perfil limpio sin plugins: WAR/migrador construidos e inspeccionados sin residuos.

El primer intento del build de aplicación se detuvo porque la prueba nueva no
encontraba los Dockerfiles dentro del stage temporal. Se corrigió el contexto del
builder copiando ambos archivos únicamente al stage de verificación. La prueba y
el build se repitieron completos y quedaron verdes; no se relajó ningún gate.

## Imágenes Docker y migración real

| Artefacto | Tag local | ID de imagen `linux/amd64` |
|---|---|---|
| aplicación | `logixone/app:j11-s6-06-business-partners-demo` | `sha256:ce79c8f4ab4728936ed17aa759bf8f19c2eed090f2ee69f32d169919a37ef1e9` |
| migrador | `logixone/migrator:j11-s6-06-business-partners-demo` | `sha256:6c6f88d92f660bd9f7e461ee73fb1b0561062ed41eec319bed93fd21e6b1f61d` |

`docker build --check` terminó sin advertencias para ambos Dockerfiles. Los builds
usaron `LOGIXONE_BUILD_MODE=verified` y
`LOGIXONE_MAVEN_PROFILE=with-business-partners-demo`.

La corrección visual generó y desplegó la imagen verificada
`logixone/app:j11-s6-06-ux-redesign`, manifest local
`sha256:6f78508b4be6e5c6f6a0caffc4530d39d00b7ed5b44d52e19d2c6cb5c476a5cb`.
Se recreó únicamente `logixone-app-1`; PostgreSQL y Keycloak no fueron recreados y
los volúmenes existentes no se eliminaron.

Dos ejecuciones consecutivas del migrador sobre Compose informaron:

```text
owner=kernel schema=core migrations_executed=0 schema_version=6
owner=business_partners schema=plg_business_partners migrations_executed=0 schema_version=1
owner=reference_plugin schema=plg_reference_plugin migrations_executed=0 schema_version=1
```

## Conservación de estado y salud

Antes de recrear `app`, el volumen contenía tres participantes y un
`BP-DEMO-001`. Después de ejecutar el migrador, recrear únicamente `app` y ejecutar
el escenario visual, el esquema continuó en V1, `BP-DEMO-001` siguió presente y
la empresa A conservó `business_partners=ENABLED`. El cuarto registro corresponde
al dato ficticio creado por Playwright.

- `logixone_postgres-data`: conservado;
- `logixone_keycloak-data`: conservado;
- PostgreSQL y Keycloak: contenedores no recreados;
- `logixone-app-1`: nueva imagen, estado `healthy`;
- `/health/live`: HTTP 200 `UP`;
- `/health/ready`: HTTP 200 `UP` con catálogo, configuración, base, migraciones y
  OIDC en verde;
- errores recientes de aplicación: 0 líneas.

No se ejecutó `down --volumes`, SQL destructivo, `repair` de Flyway ni edición de
migraciones aplicadas.

## Playwright y revisión visual corregida

La prueba final `BusinessPartnersVisualIT` ejecutó 1 escenario, 0 fallos, 0 errores
y 0 omitidos en 22,13 s. Autenticó por OIDC, seleccionó empresa, recorrió directorio
y alta, registró un participante ficticio, volvió al directorio, lo buscó, abrió la
ficha, cambió a “Roles y estado”, asignó cliente y confirmó `Cliente · Activo` en el
resumen. Validó Material Design, labels, un solo `h1`, nombre accesible de la acción
de fila y ausencia de metadatos técnicos visibles.

La matriz comprobó ausencia de overflow de página en 375, 599, 600, 720, 839, 840 y
1280 px. Medio y compacto usan lista adaptable; expandido usa tabla y navegación
lateral. Las nueve capturas fueron abiertas y revisadas: no presentan acciones
cortadas, asteriscos separados, pestañas truncadas, caracteres dañados ni overflow
horizontal normal.

| Vista | 375 px | 720 px | 1280 px |
|---|---|---|---|
| Directorio | [captura](screenshots/J11-S6-06-redesign/e2e/business-partners-directory-compact-375.png) | [captura](screenshots/J11-S6-06-redesign/e2e/business-partners-directory-medium-720.png) | [captura](screenshots/J11-S6-06-redesign/e2e/business-partners-directory-expanded-1280.png) |
| Alta | [captura](screenshots/J11-S6-06-redesign/e2e/business-partners-create-compact-375.png) | [captura](screenshots/J11-S6-06-redesign/e2e/business-partners-create-medium-720.png) | [captura](screenshots/J11-S6-06-redesign/e2e/business-partners-create-expanded-1280.png) |
| Ficha | [captura](screenshots/J11-S6-06-redesign/e2e/business-partners-detail-compact-375.png) | [captura](screenshots/J11-S6-06-redesign/e2e/business-partners-detail-medium-720.png) | [captura](screenshots/J11-S6-06-redesign/e2e/business-partners-detail-expanded-1280.png) |

| Captura | SHA-256 |
|---|---|
| directorio 375 | `90659518c48f32f69378d145e14a4428ab0bf3362295930b3d2125aee924079e` |
| directorio 720 | `8aaf76abc92844b067a76cd2a59b55c9006333d4843089c9417d6c300c69c001` |
| directorio 1280 | `8a66f50d4e2a9616dc17bd8e7897836cb5dd55939d4a5b27847ae714c5840b09` |
| alta 375 | `87c0ab72f180b985053b178a45b101c944f3d09d8e830eea1256047fee51121d` |
| alta 720 | `4b55ba4f2a6ea55dffe5299d0d1d203b2782fd2775b7acecd41ee8035dd2dbb2` |
| alta 1280 | `90221049546fd0cc496ac54490ab249a38998ac6e2a5fb66a183af5d67047098` |
| ficha 375 | `d920c0b380fa66a956314f39f34b238dbe7f4be685beb7d11ce4f2f5427fa360` |
| ficha 720 | `ddc27222be2c2484528e71dcfba591919fea48d3f0fe468d8ac4ed62b0b9e4a1` |
| ficha 1280 | `925f9df5c138caec8cee199dc8204279fd6c566ca105fe2548d756f063d0e624` |

Una ejecución intermedia falló porque el test dejó el viewport en 375 px después de
capturar el alta y luego buscó una fila de tabla de escritorio. El fallo se trató
como bloqueo; el escenario fija 1280 px antes del tramo transaccional y la repetición
completa quedó verde. No se relajó ninguna aserción funcional.

## Documentación

`tmp/validate_docs.py` revisó 169 archivos Markdown: 0 enlaces locales rotos, 0
errores UTF-8, 0 archivos con mojibake y 0 coincidencias con secretos locales.
La guía de implementación avanzó a `1.0-rc34`.

## Pendientes

- gate integral y seguridad negativa acumulada de `J11-S6-07`;
- demo oficial de cierre, retrospectiva y PDF obligatorio de Sprint 6;
- validación independiente transversal de la guía candidata;
- publicación/promoción de imágenes: no autorizada por esta historia.
