# J11-S6-07 - Validación integral, demo visual y cierre técnico de Sprint 6

- Estado: Implementada y validada técnicamente; G7 independiente pendiente
- Sprint: 6
- Fecha: 2026-07-30
- Dependencias: `J11-S6-00` a `J11-S6-06` completas
- Pendiente transversal: validación independiente G7 de la guía candidata
- ADR rectores: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md),
  [ADR-0012](../../adr/0012-composicion-unica-y-migraciones-de-plugins.md),
  [ADR-0013](../../adr/0013-eventos-integracion-outbox-por-plugin.md) y
  [ADR-0018](../../adr/0018-floorplan-erp-directorio-alta-ficha.md)

## Objetivo

Validar el baseline completo del primer plugin productivo, ejecutar una demo real
y reproducible y regenerar el PDF obligatorio del Sprint. El resultado debe
distinguir los gates técnicos ejecutables de la validación humana independiente
heredada, sin promover imágenes ni declarar un cierre formal mientras G7 siga
pendiente.

## Alcance

- documentación, ADR, enlaces, UTF-8 y trazabilidad;
- Java 21, reactor completo, pruebas unitarias y ArchUnit;
- composición base sin plugins y composición con `business_partners` coherente en
  WAR y migrador;
- PostgreSQL/Testcontainers, JPA, migraciones, checksum e idempotencia;
- Dockerfiles, imágenes verificadas, Compose, health, OIDC y persistencia de
  volúmenes;
- autorización positiva y negativa, aislamiento por empresa y plugin activo;
- demo JSF Material Design 3 en 375, 720 y 1280 px;
- directorio, alta, ficha, roles, contacto, dirección e inactivación sin borrado;
- contrato de extensión pública verificable sin mostrar marcadores técnicos vacíos
  al usuario final;
- guía de implementación, retrospectiva, siguiente trabajo autorizado y PDF.

## Criterios de aceptación

- **CA-01:** la documentación mantenida no tiene enlaces rotos, secretos, mojibake
  ni estados contradictorios.
- **CA-02:** `clean verify` con Java 21 termina verde para el reactor final.
- **CA-03:** ArchUnit conserva las fronteras de kernel y plugins.
- **CA-04:** la composición base contiene cero plugins y el perfil de demo contiene
  exactamente `reference_plugin`, `reference_customization_a`,
  `reference_customization_b` y `business_partners` en WAR y migrador.
- **CA-05:** PostgreSQL valida esquema privado, JPA, invariantes, aislamiento,
  concurrencia, checksum e idempotencia.
- **CA-06:** las imágenes de aplicación y migrador se construyen en modo
  `verified`, con la misma selección física y sin secretos incorporados.
- **CA-07:** Compose arranca saludable, el migrador es idempotente y recrear la
  aplicación conserva datos y volúmenes.
- **CA-08:** liveness/readiness, OIDC positivo y casos negativos de issuer,
  audience y expiración terminan verdes.
- **CA-09:** el servidor rechaza operaciones sin permiso, fuera de empresa o con
  el plugin inactivo.
- **CA-10:** la demo recorre operaciones reales del plugin y no usa mocks ni datos
  empresariales reales.
- **CA-11:** 375, 720 y 1280 px no presentan overflow horizontal normal, cortes ni
  pérdida de acciones esenciales.
- **CA-12:** el shell mantiene separados directorio, alta y ficha; la ficha organiza
  datos por pestañas y no expone metadatos técnicos internos.
- **CA-13:** los slots siguen siendo contratos públicos versionados, pero una
  extensión vacía no ocupa espacio ni aparece como texto técnico en la UI.
- **CA-14:** el guion de demo incluye preparación, pasos, resultados, limitaciones
  y restauración.
- **CA-15:** la guía de implementadores, retrospectiva, backlog y siguiente Sprint
  reflejan el baseline comprobado.
- **CA-16:** el PDF identifica Sprint 6 y fecha, se renderiza por completo y queda
  verificado en contenido, metadatos, páginas, tamaño y SHA-256.
- **CA-17:** el estado final no confunde gates técnicos verdes con G7 independiente
  ni autoriza promoción o producción.

## Gates

| Gate | Alcance | Estado final |
|---|---|---|
| G0 | documentación, ADR, enlaces, UTF-8 y trazabilidad | Verde |
| G1 | Java 21, reactor, JUnit y ArchUnit | Verde |
| G2 | composición base/plugin e inspección WAR/migrador | Verde |
| G3 | PostgreSQL/Testcontainers, JPA y migraciones | Verde |
| G4 | Docker/Compose, health, OIDC y volúmenes | Verde |
| G5 | seguridad de servidor y demo Playwright responsive | Verde |
| G6 | guía, retrospectiva, siguiente Sprint y PDF | Verde |
| G7 | validación independiente heredada | Pendiente; requiere otra persona |

## Regla de parada

Una prueba ejecutada y fallida detiene el gate. Se corrige la causa y se repite el
alcance afectado. No se omiten pruebas, no se modifica una migración aplicada y no
se eliminan volúmenes para simular una actualización limpia.

## Resultado

G0-G6 están verdes y la demo de Sprint 6 está disponible sobre la imagen final.
El PDF obligatorio fue regenerado, renderizado por completo, revisado visualmente
y validado en metadatos, texto, páginas, tamaño y checksum. La evidencia
reproducible se conserva en
[J11-S6-07](../../evidence/J11-S6-07-validacion-demo-cierre.md) y el recorrido en el
[runbook de cierre](../../runbooks/demo-cierre-sprint-06.md).

G7 continúa pendiente. Ese estado demuestra el incremento técnico, pero no cierra
formalmente los Sprints afectados, no publica la guía `1.0`, no promueve imágenes
y no autoriza un despliegue de producción.
