# J11-S8-07 - Validación integral, demo oficial y congelación técnica

- Estado: Implementada y validada técnicamente; baseline congelado para J11-S8-08
- Sprint: 8
- Fecha: 2026-08-01
- Dependencias: `J11-S8-00` a `J11-S8-06` completas
- Cierre formal: pendiente del instalador Windows `J11-S8-08`
- Pendiente transversal: validación independiente G7 de la guía candidata
- Perfil demostrado: `with-inventory-demo`

## Objetivo

Validar `inventory` junto con `commercial_catalog` y `business_partners`, ejecutar
la demo oficial sobre imágenes construidas desde el mismo baseline y congelar la
candidata técnica que consumirá el instalador Windows. Esta historia completa los
gates técnicos y documentales, pero no cierra formalmente Sprint 8.

## Alcance validado

- reactor base y composición completa con Java 21;
- límites ArchUnit, contratos públicos y ausencia de JPA/SQL cruzado;
- WAR y migrador derivados de una selección física única;
- PostgreSQL/Testcontainers, JPA/JTA, migraciones e idempotencia;
- Dockerfiles, imágenes verificadas, Compose, health, OIDC y volúmenes;
- dependencia requerida `inventory -> commercial_catalog` y rechazo de una
  desactivación incompatible;
- operaciones reales de depósito, existencias, movimientos, reservas y conteos;
- menú fusionado de siete funciones mediante contribuciones de plugins;
- demo JSF Material Design 3 en 375, 720 y 1280 px;
- manuales, fotografía de plugins, retrospectiva, runbook y PDF obligatorio.

## Criterios de aceptación

- **CA-01:** documentación sin enlaces rotos, mojibake ni secretos.
- **CA-02:** `clean verify` verde sin perfil y con `with-inventory-demo`.
- **CA-03:** ArchUnit conserva los límites de kernel, APIs y plugins.
- **CA-04:** la distribución base contiene cero implementaciones de plugins y la
  completa contiene los seis plugins esperados.
- **CA-05:** las pruebas PostgreSQL validan propietarios, esquemas privados,
  concurrencia, checksums e idempotencia.
- **CA-06:** WAR e imágenes corresponden al mismo perfil y sus hashes internos
  coinciden con los artefactos locales.
- **CA-07:** dos ejecuciones del migrador informan cero cambios y recrear la
  aplicación conserva exactamente los datos existentes.
- **CA-08:** liveness, readiness y OIDC terminan verdes.
- **CA-09:** Playwright recorre shell, administración y los tres plugins
  productivos sin mocks.
- **CA-10:** no se puede desactivar catálogo mientras inventario depende de él;
  después de desactivar inventario, catálogo puede desactivarse y la ruta queda
  denegada.
- **CA-11:** 375, 720 y 1280 px no presentan overflow horizontal normal ni pierden
  acciones esenciales.
- **CA-12:** la demo termina con catálogo e inventario restaurados y conserva los
  datos ficticios creados por casos de uso.
- **CA-13:** el guion declara preparación, recorrido, límites y restauración.
- **CA-14:** la fotografía de plugins deriva de POM, descriptores y migraciones.
- **CA-15:** el PDF identifica Sprint 8 y queda renderizado y revisado por completo.
- **CA-16:** J11-S8-08 recibe un baseline inequívoco y el Sprint no se presenta como
  cerrado antes de verificar el instalador.

## Gates

| Gate | Alcance | Estado |
|---|---|---|
| G0 | documentación, enlaces, UTF-8, trazabilidad y secretos | Verde |
| G1 | Java 21, reactor, JUnit y ArchUnit | Verde |
| G2 | composición base/completa e inspección WAR/migrador | Verde |
| G3 | PostgreSQL/Testcontainers, JPA/JTA y migraciones | Verde |
| G4 | Docker/Compose, health, OIDC y conservación de volúmenes | Verde |
| G5 | seguridad de servidor y demo Playwright responsive | Verde |
| G6 | manuales, retrospectiva, fotografía, runbook y PDF | Verde |
| G7 | validación independiente heredada | Pendiente; requiere otra persona |
| G8 | instalador Windows del baseline congelado | Pendiente en J11-S8-08 |

## Resultado

G0-G6 están verdes y la demo visual oficial está disponible. El baseline técnico
queda congelado con las imágenes `logixone/app:j11-s8-07-closing` y
`logixone/migrator:j11-s8-07-closing`. El recorrido reproducible está en el
[runbook de cierre](../../runbooks/demo-cierre-sprint-08.md), los resultados en la
[evidencia](../../evidence/J11-S8-07-validacion-demo-cierre.md) y la estructura en
la [fotografía de plugins](estructura-plugins-y-dependencias.md).

Sprint 8 continúa abierto. El siguiente y único paso autorizado para su cierre
formal es [J11-S8-08](J11-S8-08-instalador-windows-cierre.md); no se promueven
imágenes ni se autoriza producción mientras G7/G8 permanezcan pendientes.
