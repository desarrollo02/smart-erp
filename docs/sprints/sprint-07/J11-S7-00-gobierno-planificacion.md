# J11-S7-00 - Gobierno y planificación de `commercial_catalog`

- Estado: Completada; caracterización y decisiones CC-D01 a CC-D10 confirmadas
- Sprint: 7
- Fecha: 2026-07-30
- Dependencia: G0-G6 de `J11-S6-07` verdes
- ADR rector: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)

## Objetivo

Convertir el segundo plugin del roadmap en un backlog verificable antes de leer
tablas concretas, diseñar el dominio o crear código. La historia debe acordar el
lenguaje, las fronteras y las decisiones que evitarán mezclar catálogo con
inventario, ventas, documentos o SIFEN.

## Actividades

1. revisar el legado de solo lectura para identificar comportamiento y vocabulario;
2. documentar casos de uso sin copiar código ni modelo de datos;
3. separar producto, servicio, variante, clasificación, marca, unidad, impuesto,
   lista de precio, vigencia y moneda;
4. decidir si unidad e impuesto son valores propios, catálogos públicos o
   referencias versionadas;
5. definir qué datos deben quedar como snapshots en consumidores futuros;
6. acordar estados, identidad, códigos, duplicados, vigencias y concurrencia;
7. identificar contratos que necesitarán inventario, compras, ventas y documentos;
8. preparar criterios de seguridad, auditoría, responsive, migración y demo.

## Decisiones que deben presentarse al responsable de producto

- `CC-D01`: producto y servicio como un agregado común o tipos separados;
- `CC-D02`: política de códigos y códigos de barras;
- `CC-D03`: variantes y atributos en la primera versión;
- `CC-D04`: unidad base, conversiones y precisión;
- `CC-D05`: clasificación tributaria interna frente a catálogos fiscales externos;
- `CC-D06`: propiedad y vigencia de listas/precios;
- `CC-D07`: moneda, redondeo y precios con/sin impuestos;
- `CC-D08`: alcance de categorías, marcas y etiquetas;
- `CC-D09`: inactivación, reemplazo y preservación histórica;
- `CC-D10`: contratos públicos y snapshots requeridos por consumidores.

Ninguna decisión se considera aceptada por silencio. Las alternativas, impacto y
recomendación deben registrarse antes de `J11-S7-02`.

## Criterios de aceptación

- **CA-01:** la caracterización distingue comportamiento observado de decisión
  nueva del producto.
- **CA-02:** catálogo no contiene stock, pedidos, documentos ni integración fiscal.
- **CA-03:** se documentan casos de uso, invariantes y datos con ejemplos ficticios.
- **CA-04:** CC-D01 a CC-D10 tienen alternativas, recomendación y estado explícito.
- **CA-05:** se identifican contratos públicos sin exponer entidades o DTO internos.
- **CA-06:** se define una matriz de pruebas unitarias, ArchUnit, PostgreSQL,
  composición, seguridad, Playwright y Docker.
- **CA-07:** cada pantalla planificada incluye 375, 720 y 1280 px desde su historia.
- **CA-08:** el cierre mantiene demo visual, guía para implementadores y PDF.
- **CA-09:** el trabajo no modifica el proyecto legado.
- **CA-10:** cualquier nueva decisión arquitectónica se registra mediante ADR.

## Gates del Sprint

| Gate | Resultado requerido |
|---|---|
| G0 | documentación, decisiones, enlaces y trazabilidad |
| G1 | dominio/API, unitarias y ArchUnit |
| G2 | esquema, JPA y PostgreSQL/Testcontainers |
| G3 | aplicación, seguridad y auditoría |
| G4 | composición base/demo, WAR y migrador |
| G5 | Docker/Compose, health, OIDC y persistencia |
| G6 | Playwright, demo 375/720/1280 y accesibilidad |
| G7 | guía, retrospectiva, PDF y validación independiente aplicable |

## Regla de inicio

`J11-S7-01` podía comenzar cuando G6 documental de Sprint 6 estuviera verde; esa
condición se cumplió y la caracterización quedó documentada. El responsable de
producto confirmó CC-D01 a CC-D10 sin cambios el 2026-07-30 y aclaró que el
catálogo sería otro módulo/plugin. J11-S7-02 quedó autorizada y se completó verde.

## Resultado

Gobierno, frontera, secuencia, gates, decisiones y condición de parada quedaron
definidos. [J11-S7-01](J11-S7-01-caracterizacion-commercial-catalog.md) convirtió
la inspección del legado en requisitos neutrales y
[J11-S7-02](J11-S7-02-dominio-contratos-commercial-catalog.md) creó API y dominio.
El siguiente paso autorizado es J11-S7-03 para persistencia privada.
