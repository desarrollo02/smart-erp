# J11-S9-07 — Validación integral, demo oficial y congelación técnica

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Sprint: 9
- Fecha: 2026-08-13
- Dependencias: `J11-S9-00` a `J11-S9-06` completas
- Cierre formal: pendientes G7 independiente y decisión de instalador `J11-S9-08`
- Perfil demostrado: `with-purchasing-demo`
- Evidencia: [resultado reproducible](../../evidence/J11-S9-07-validacion-demo-cierre.md)
- Demo: [guion oficial](../../runbooks/demo-cierre-sprint-09.md)

## Objetivo

Validar de forma acumulada `purchasing` junto con sus cuatro plugins productivos
precedentes, ejecutar la demo oficial sobre imágenes construidas desde el mismo
corte y congelar la candidata técnica que recibirá la decisión de instalador
Windows. Esta historia completa los gates automatizados y documentales; no
equivale a aceptación independiente ni cierra Sprint 9.

## Alcance validado

- reactor base y composición completa con Java 21 y Maven Wrapper;
- límites ArchUnit, contratos públicos y ausencia de JPA/SQL cruzado;
- WAR y migrador derivados de `with-purchasing-demo`;
- PostgreSQL/Testcontainers, JPA/JTA, migraciones e idempotencia;
- Dockerfiles, imágenes, Compose, health, OIDC y conservación de volúmenes;
- dependencias requeridas y orden seguro de desactivación/restauración;
- solicitudes, aprobaciones, órdenes, recepciones, devoluciones y seguimiento;
- demo JSF Material Design 3 en 375, 720 y 1280 px, además de límites responsive;
- manuales, fotografía de plugins, retrospectiva, runbook y PDF obligatorio.

## Criterios de aceptación

- **CA-01:** documentación sin enlaces locales rotos, mojibake ni secretos.
- **CA-02:** `clean verify` verde sin perfil y con `with-purchasing-demo`.
- **CA-03:** ArchUnit conserva los límites de kernel, APIs y plugins.
- **CA-04:** la distribución base no contiene implementaciones de plugins y la
  completa contiene los ocho JAR físicos esperados.
- **CA-05:** PostgreSQL valida propietarios, esquemas privados, JPA/JTA,
  migraciones, checksums e idempotencia.
- **CA-06:** WAR e imágenes corresponden al perfil completo y son identificables
  por tamaño, hash o digest.
- **CA-07:** dos ejecuciones del migrador no aplican cambios adicionales y recrear
  la aplicación conserva los datos existentes.
- **CA-08:** liveness, readiness y OIDC terminan verdes.
- **CA-09:** Playwright recorre shell, administración y los cinco plugins
  productivos sin mocks de dominio.
- **CA-10:** las dependencias impiden desactivaciones incompatibles y las pruebas
  restauran plugins en el orden permitido.
- **CA-11:** 375, 720 y 1280 px no presentan overflow horizontal normal ni pierden
  acciones esenciales.
- **CA-12:** la demo conserva datos ficticios y deja deshabilitada la autoridad de
  sistema temporal usada por las pruebas.
- **CA-13:** el guion declara preparación, recorrido, límites y restauración.
- **CA-14:** la fotografía deriva de POM, descriptores y migraciones reales.
- **CA-15:** el PDF identifica Sprint 9 y queda renderizado y revisado por completo.
- **CA-16:** Sprint 9 permanece abierto hasta registrar G7 y la respuesta explícita
  a `¿Crearemos un nuevo instalador Windows para este Sprint?`.

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
| G7 | validación independiente acumulada | Pendiente; requiere otra persona |
| G8 | decisión y eventual instalador Windows | Pendiente en J11-S9-08 |

## Resultado

G0–G6 quedan verdes. El baseline técnico se identifica con las imágenes
`logixone/app:j11-s9-07-closing` y
`logixone/migrator:j11-s9-07-closing`. El recorrido está en el
[runbook de cierre](../../runbooks/demo-cierre-sprint-09.md), los resultados en la
[evidencia](../../evidence/J11-S9-07-validacion-demo-cierre.md) y la composición en
la [fotografía de plugins](estructura-plugins-y-dependencias.md).

Sprint 9 continúa abierto. No se promueven imágenes, no se publica la guía `1.0`,
no se despliega a producción y no se inicia código de Sprint 10 hasta registrar
la decisión J11-S9-08. La validación independiente continúa diferida conforme a
la autorización de producto del 2026-08-11.
