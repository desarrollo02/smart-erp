# J11-S7-02 — Dominio neutral y contratos públicos de `commercial_catalog`

- Estado: Completada
- Sprint: 7
- Fecha: 2026-07-30
- Dependencia: [J11-S7-01](J11-S7-01-caracterizacion-commercial-catalog.md) completada
- Decisión: [ADR-0019](../../adr/0019-modelo-catalogo-comercial-y-contratos-publicos.md)

## Objetivo

Crear `commercial_catalog` como otro plugin funcional, físicamente separado de
`business_partners`, con una API Java pura versionada y un primer dominio neutral
que materialice CC-D01 a CC-D10 sin anticipar persistencia ni UI.

## Alcance

- módulo `commercial-catalog-api` con referencia, búsqueda, conversión y cotización;
- módulo `commercial-catalog` con descriptor CDI/SPI neutral;
- agregado `CatalogItem` con código, tipo, alcances, identificadores, unidad,
  clasificación, perfil tributario, variante, ciclo de vida y versión;
- lista de precios con moneda, modo tributario, redondeo, entradas y vigencias;
- pruebas unitarias y reglas ArchUnit.

## Fuera de alcance

- Flyway, esquema `plg_commercial_catalog`, JPA y repositorios;
- permisos, comandos transaccionales, auditoría y endpoints;
- menús, Jakarta Faces, Material Design, slots y Playwright;
- inventario, costos, pedidos, facturas, promociones y SIFEN;
- composición física del WAR/migrador, Docker y migración del legado.

## Criterios de aceptación

- **CA-01:** API e implementación son módulos Maven físicos diferentes.
- **CA-02:** la API usa Java estándar y `CompanyId`; no usa Jakarta ni internos.
- **CA-03:** contrato `1.0.0`, UUID canónicos y referencias empresariales mínimas.
- **CA-04:** existen contratos por propósito para búsqueda, conversión y cotización.
- **CA-05:** `CatalogItem` comparte agregado y mantiene tipo inmutable.
- **CA-06:** códigos, identificadores, unidades y conversiones aplican CC-D02/D04.
- **CA-07:** variante, clasificación y perfil tributario no incorporan SIFEN.
- **CA-08:** inactivación/reemplazo preservan historia y usan versión optimista.
- **CA-09:** lista fija moneda, impuesto y redondeo; entradas usan `BigDecimal`.
- **CA-10:** descriptor `commercial_catalog@1.0.0` no inventa contribuciones futuras.
- **CA-11:** ArchUnit y pruebas de ambos módulos quedan verdes.
- **CA-12:** reactor completo y documentación quedan actualizados.

## Secuencia

1. registrar ADR y módulos en el reactor;
2. implementar y probar API pública;
3. implementar y probar descriptor/dominio;
4. reforzar límites ArchUnit;
5. ejecutar reactor y registrar evidencia.

## Resultado

Los módulos `commercial-catalog-api` y `commercial-catalog` quedaron separados y
verdes. La API pública `1.0.0` expone contratos por propósito; el descriptor no
adelanta contribuciones y el dominio materializa CC-D01 a CC-D10 sin JPA ni UI.

Los 12 criterios quedaron cumplidos. El reactor terminó 22/22 con 266 pruebas, las
15 reglas dirigidas de límites quedaron verdes y la inspección confirmó que el WAR
base no incorpora el plugin. Consulte la
[evidencia reproducible](../../evidence/J11-S7-02-dominio-contratos-commercial-catalog.md).

## Continuidad

Se habilita `J11-S7-03`: esquema privado `plg_commercial_catalog`, migración V1,
JPA y repositorios. No se habilitan todavía permisos, UI ni composición física.
