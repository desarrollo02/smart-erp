# Sprint 6 - Primer plugin productivo `business_partners`

- Estado: G0-G6 de J11-S6-07 verdes; G7 independiente pendiente
- Fecha de planificación: 2026-07-29
- Dependencia técnica: gates G0-G6 de Sprint 5 verdes
- Pendiente transversal: validación independiente G7 de la guía candidata
- ADR rectores: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md),
  [ADR-0012](../../adr/0012-composicion-unica-y-migraciones-de-plugins.md) y
  [ADR-0013](../../adr/0013-eventos-integracion-outbox-por-plugin.md)

## Objetivo

Construir `business_partners` como primer plugin ERP productivo y como referencia
de calidad para los siguientes dominios. Debe administrar clientes, proveedores,
contactos y direcciones sin trasladar acoplamientos del sistema legado ni invadir
responsabilidades del kernel.

La primera actividad es caracterizar el comportamiento necesario usando el legado
como fuente de conocimiento de solo lectura. No se copia código ni se diseña la
persistencia antes de acordar requisitos, invariantes, casos de uso y datos.

## Secuencia propuesta

| Orden | Historia | Resultado esperado |
|---:|---|---|
| 1 | [J11-S6-00](J11-S6-00-gobierno-planificacion.md) | gobierno, alcance, riesgos, criterios y matriz de pruebas |
| 2 | [J11-S6-01](J11-S6-01-caracterizacion-business-partners.md) | caracterización del legado, glosario, casos de uso e invariantes |
| 3 | [J11-S6-02](J11-S6-02-dominio-contratos-business-partners.md) | dominio neutral y contratos públicos versionados |
| 4 | [J11-S6-03](J11-S6-03-persistencia-business-partners.md) | esquema `plg_business_partners`, migraciones y repositorios |
| 5 | [J11-S6-04](J11-S6-04-aplicacion-seguridad-business-partners.md) | comandos, consultas, permisos, auditoría y autorización |
| 6 | [J11-S6-05](J11-S6-05-interfaz-business-partners.md) | pantallas JSF Material Design 3 responsive y slots públicos |
| 7 | [J11-S6-06](J11-S6-06-integracion-composicion-business-partners.md) | integración, documentación para implementadores y composición física |
| 8 | [J11-S6-07](J11-S6-07-validacion-demo-cierre.md) | validación integral, demo visual, retrospectiva y PDF de cierre |

Cada historia de código vuelve al flujo incremental normal: prueba pequeña después
de cada cambio y `mvn verify` al completar un corte coherente. No se aplica una
nueva excepción de pruebas.

## Límites

- `business_partners` no contiene facturación, ventas, compras, inventario ni
  contabilidad.
- El kernel no conoce entidades, DTO internos ni tablas del plugin.
- El esquema pertenece exclusivamente a `plg_business_partners`.
- Otros plugins referencian participantes por identificadores y contratos públicos.
- No se implementa outbox hasta que exista un evento, productor y consumidor reales.
- Las pantallas exponen slots públicos para futuras personalizaciones, pero la
  personalización exclusiva de una empresa se desarrolla al final de su conjunto
  funcional, cuando exista un pedido concreto.
- El manual SIFEN se usa después para modelar documentos comerciales; no determina
  por sí solo el agregado de participantes de este Sprint.

## Definition of Done del Sprint

- pruebas unitarias, ArchUnit, PostgreSQL/Testcontainers y `mvn verify` verdes;
- composición WAR/migrator consistente con y sin el plugin;
- autorización positiva y negativa en servidor;
- migraciones idempotentes y datos preservados al recrear contenedores;
- demo real navegable en 375, 720 y 1280 px;
- guía de implementación actualizada;
- PDF obligatorio regenerado, renderizado y revisado;
- evidencia y retrospectiva completas.

## Estado del siguiente paso

`J11-S6-00` y `J11-S6-01` están completas. El responsable de producto confirmó
BP-D01 a BP-D10 sin cambios el 2026-07-29.

`J11-S6-02` completó dominio neutral y contratos públicos. `J11-S6-03` agregó V1,
ocho tablas privadas, JPA y repositorios. `J11-S6-04` completó casos de uso,
búsqueda paginada, cuatro permisos, autorización neutral y auditoría `core` V6.
`J11-S6-05` agregó `plugin-api` 0.4.0, interacción neutral, menú, pantalla JSF
Material Design 3, slots públicos y Playwright verde en siete anchos. `J11-S6-06`
incorporó el perfil único `with-business-partners-demo`, construyó WAR/migrador e
imágenes verificadas, demostró idempotencia y conservación de los volúmenes y dejó
una nueva demo visual disponible. `J11-S6-07` repitió reactor, ArchUnit,
PostgreSQL/Testcontainers, Docker/Compose, health, OIDC y Playwright sobre la imagen
final. La demo oficial quedó verde con 35 capturas responsive. El PDF de G6 fue
regenerado y verificado en sus 44 páginas. Resta la validación humana independiente
G7; por ello el Sprint sigue formalmente abierto y no autoriza promoción.

## Retrospectiva y siguiente incremento

La retrospectiva completa está en la
[evidencia J11-S6-07](../../evidence/J11-S6-07-validacion-demo-cierre.md). El
siguiente incremento autorizado es la planificación de
[Sprint 7](../sprint-07/README.md) para `commercial_catalog`, comenzando por
caracterización y decisiones antes de persistencia o UI.
