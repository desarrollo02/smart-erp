# J11-S5-04 — Validación integral, demo visual y cierre de Sprint 5

- Estado: Gates técnicos G0-G6 verdes; G7 independiente pendiente
- Sprint: 5
- Fecha: 2026-07-29
- Dependencias: `J11-S5-01` a `J11-S5-03` completadas
- ADR rectores: [ADR-0012](../../adr/0012-composicion-unica-y-migraciones-de-plugins.md)
  y [ADR-0013](../../adr/0013-eventos-integracion-outbox-por-plugin.md)

## Objetivo

Validar el baseline final de fundaciones para plugins productivos, demostrarlo en
la interfaz real y regenerar el PDF obligatorio contra el repositorio mantenido.
El resultado debe indicar con precisión qué está listo para iniciar
`business_partners` y qué sigue bloqueado por la validación humana independiente
heredada de Sprint 4.

## Alcance

- coherencia documental, ADR, enlaces y UTF-8;
- reactor base, pruebas unitarias, ArchUnit y artefactos sin plugins;
- composición A/B simétrica en WAR y migrador;
- PostgreSQL/Testcontainers, migraciones `core` y `plg_*`;
- Docker/Compose, health, idempotencia y conservación del volumen;
- demostración visual de catálogo físico y personalización A/B;
- viewport compacto, medio y expandido con Material Design 3 sobre JSF;
- guía para implementadores actualizada;
- retrospectiva y siguiente trabajo autorizado;
- PDF de estructura regenerado, renderizado y revisado página por página.

## Criterios de aceptación

- **CA-01:** Markdown, ADR, estados, enlaces y UTF-8 son coherentes.
- **CA-02:** `clean verify` con Java 21 termina verde sin pruebas omitidas.
- **CA-03:** ArchUnit conserva límites de kernel, herramienta y plugins.
- **CA-04:** la composición base tiene cero plugins y la A/B exactamente los tres
  fixtures en WAR y migrador.
- **CA-05:** PostgreSQL valida migración inicial, idempotencia, checksum e
  historial separado por propietario.
- **CA-06:** Dockerfiles, imágenes y Compose terminan verdes con la misma selección
  física.
- **CA-07:** readiness/liveness son semánticos y la recreación conserva datos.
- **CA-08:** la UI muestra catálogo y demo A/B reales, no mocks ni diapositivas.
- **CA-09:** la demo funciona en 375, 720 y 1280 px sin overflow, cortes ni mezcla
  de empresa.
- **CA-10:** se aclara visualmente que la plantilla y ADR-0013 son habilitadores de
  build/arquitectura, no nuevos módulos ERP navegables.
- **CA-11:** la guía vigente explica generar, componer, migrar y retirar plugins,
  y cuándo implementar outbox.
- **CA-12:** el PDF identifica Sprint 5 y fecha, refleja el inventario final y
  distingue fuentes de generados.
- **CA-13:** todas las páginas del PDF se renderizan y revisan; metadatos, texto,
  tamaño y SHA-256 quedan registrados.
- **CA-14:** la retrospectiva y el Sprint de `business_partners` quedan
  documentados sin adelantar código.
- **CA-15:** el resultado no declara cerrados Sprint 4/5 ni publica guía `1.0` si
  el recorrido independiente continúa pendiente.

## Gates

| Gate | Alcance | Estado inicial |
|---|---|---|
| G0 | documentación, ADR, enlaces, UTF-8 y trazabilidad | Verde |
| G1 | Java 21, reactor base, JUnit y ArchUnit | Verde: 18/18; 191 pruebas |
| G2 | composición base/A-B e inspección WAR/migrador | Verde: base 0; A/B 3 |
| G3 | PostgreSQL/Testcontainers e idempotencia | Verde: 12/12 |
| G4 | Docker/Compose, health y volúmenes | Verde; volúmenes conservados |
| G5 | navegador, demo responsive A/B | Verde: 5/5 y 22 capturas revisadas |
| G6 | guía, retrospectiva y PDF obligatorio | Verde: rc27 y PDF verificado |
| G7 | validación independiente heredada | Pendiente; no puede autocompletarse |

## Regla de parada

Una prueba ejecutada y fallida detiene el gate. Se corrige la causa y se repite el
alcance afectado; no se convierte el fallo en omisión. `docker compose down` se
ejecuta sin `--volumes`.

## Resultado documental esperado

La historia podrá quedar con gates técnicos G0-G6 verdes y G7 pendiente. Ese
estado autoriza planificar el Sprint 6 y caracterizar `business_partners`, pero no
autoriza promoción, producción, publicación de la guía `1.0` ni afirmar que los
Sprints 4/5 están formalmente cerrados.

## Resultado

La evidencia reproducible está en
[J11-S5-04-validacion-demo-cierre.md](../../evidence/J11-S5-04-validacion-demo-cierre.md).
El corte confirmó:

- reactor base final con 191 pruebas verdes y cero plugins en WAR/migrador;
- composición A/B con exactamente tres plugins en ambos artefactos;
- PostgreSQL 12/12, migraciones idempotentes y fixture persistente;
- imágenes y Compose saludables, con volúmenes PostgreSQL/Keycloak preservados;
- Playwright 5/5 y revisión visual de 22 capturas a 375/720/1280 px;
- guía `1.0-rc27`, retrospectiva, Sprint 6 planificado y PDF de Sprint 5
  renderizado página por página.

G7 continúa pendiente por diseño: solo puede completarlo una persona independiente.
