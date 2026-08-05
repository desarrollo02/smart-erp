# J11-S7-07 - Validación integral, demo visual y cierre técnico de Sprint 7

- Estado: Implementada y validada técnicamente; G7 independiente pendiente
- Sprint: 7
- Fecha: 2026-07-31
- Dependencias: `J11-S7-00` a `J11-S7-06` completas
- Pendiente transversal: validación independiente G7 de la guía candidata
- ADR rectores: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md),
  [ADR-0012](../../adr/0012-composicion-unica-y-migraciones-de-plugins.md),
  [ADR-0019](../../adr/0019-modelo-catalogo-comercial-y-contratos-publicos.md),
  [ADR-0020](../../adr/0020-persistencia-privada-commercial-catalog.md),
  [ADR-0021](../../adr/0021-aplicacion-autorizacion-auditoria-commercial-catalog.md) y
  [ADR-0022](../../adr/0022-recorridos-visuales-commercial-catalog.md)

## Objetivo

Validar el baseline completo de `commercial_catalog` junto con
`business_partners`, ejecutar la demo oficial sobre imágenes verificadas y dejar
reproducibles la operación, la evidencia, la retrospectiva, el siguiente trabajo y
el PDF obligatorio, además de documentar la estructura efectiva de plugins. El
corte debe demostrar composición real sin adelantar
inventario, ventas, documentos ni SIFEN.

## Alcance

- reactor base y composición `with-commercial-catalog-demo` con Java 21;
- límites ArchUnit y ausencia de dependencias internas entre plugins;
- WAR y migrador derivados del mismo perfil físico;
- PostgreSQL/Testcontainers, JPA, migraciones, checksum e idempotencia;
- Dockerfiles, imágenes verificadas, Compose, health, OIDC y volúmenes;
- autorización positiva/negativa, empresa activa y plugin activo;
- menú fusionado de Socios, Artículos y Listas de precios;
- directorio, alta, ficha, clasificación, identificador, lista y precio reales;
- demo JSF Material Design 3 en 375, 720 y 1280 px y límites responsive;
- desactivación/denegación/reactivación sin eliminar tablas ni datos;
- guía, retrospectiva, Sprint 8 y PDF de estructura del repositorio.
- fotografía de plugins y dependencias con gráfico y alternativa textual.

## Criterios de aceptación

- **CA-01:** la documentación no tiene enlaces rotos, errores UTF-8, mojibake ni
  filtraciones de secretos.
- **CA-02:** `clean verify` termina verde tanto sin perfil como con
  `with-commercial-catalog-demo`.
- **CA-03:** ArchUnit conserva fronteras de kernel, API y plugins.
- **CA-04:** la variante base no contiene implementaciones de plugins; la variante
  de demo contiene `business_partners`, `commercial_catalog`, el plugin funcional
  de referencia y las dos personalizaciones de referencia.
- **CA-05:** PostgreSQL valida esquema privado, invariantes, aislamiento,
  concurrencia, JPA, checksum e idempotencia.
- **CA-06:** aplicación y migrador se construyen en modo `verified` desde la misma
  selección física y el WAR local coincide por SHA-256 con el contenido de la
  imagen.
- **CA-07:** ejecutar dos veces el migrador informa cero cambios y recrear sólo
  `app` conserva los datos de ambos plugins.
- **CA-08:** liveness/readiness y la matriz OIDC positiva/negativa terminan verdes.
- **CA-09:** Playwright recorre shell, administración, socios y catálogo contra la
  imagen final, sin mocks ni SQL directo.
- **CA-10:** los menús de plugins activos se fusionan por contrato y desaparecen al
  desactivar su propietario para la empresa.
- **CA-11:** 375, 720 y 1280 px, además de 599/600/839/840, no presentan overflow
  horizontal normal ni pérdida de acciones esenciales.
- **CA-12:** la demo termina con ambos plugins activos y conserva los registros
  creados por los casos de uso.
- **CA-13:** el runbook incluye preparación, recorrido, resultados, límites,
  restauración y uso exclusivo de datos ficticios.
- **CA-14:** retrospectiva, backlog y planificación identifican `inventory` como
  siguiente plugin sin comenzar su modelo antes de aprobar decisiones.
- **CA-15:** el PDF identifica Sprint 7, se renderiza por completo y queda
  verificado en metadatos, texto, páginas, tamaño y checksum.
- **CA-16:** el estado final no presenta G7 pendiente como cierre formal, promoción
  de imágenes, publicación de la guía `1.0` o autorización de producción.
- **CA-17:** el Sprint conserva una fotografía derivada de descriptores y POM con
  gráfico, tabla accesible, perfiles físicos, cambios y siguiente dependencia.

## Gates

| Gate | Alcance | Estado final |
|---|---|---|
| G0 | documentación, ADR, enlaces, UTF-8 y trazabilidad | Verde |
| G1 | Java 21, reactor, JUnit y ArchUnit | Verde |
| G2 | composición base/demo e inspección WAR/migrador | Verde |
| G3 | PostgreSQL/Testcontainers, JPA y migraciones | Verde |
| G4 | Docker/Compose, health, OIDC y volúmenes | Verde |
| G5 | seguridad de servidor y demo Playwright responsive | Verde |
| G6 | guía, retrospectiva, siguiente Sprint y PDF | Verde |
| G7 | validación independiente heredada | Pendiente; requiere otra persona |

## Resultado

G0-G6 están verdes. La demo visual oficial está disponible sobre la imagen final y
su recorrido se conserva en el
[runbook de cierre](../../runbooks/demo-cierre-sprint-07.md). Los resultados
reproducibles, digests, conteos y capturas están en la
[evidencia J11-S7-07](../../evidence/J11-S7-07-validacion-demo-cierre.md).
La [estructura de plugins y dependencias](estructura-plugins-y-dependencias.md)
documenta por separado el corte físico y funcional.

G7 continúa pendiente. Por ello el Sprint queda técnicamente preparado pero no
formalmente cerrado; las imágenes no se promueven, la guía permanece candidata y
no se autoriza producción.
