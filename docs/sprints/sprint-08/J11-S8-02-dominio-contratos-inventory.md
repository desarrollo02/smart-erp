# J11-S8-02 — Dominio neutral y contratos públicos de `inventory`

- Estado: Completada
- Sprint: 8
- Fecha: 2026-07-31
- Dependencia: [J11-S8-01](J11-S8-01-caracterizacion-inventory.md) completada
- Decisión: [ADR-0023](../../adr/0023-modelo-inventory-y-contratos-publicos.md)

## Objetivo

Crear `inventory` como tercer plugin funcional, físicamente separado de
`commercial_catalog`, con API Java pura `1.0.0` y dominio neutral que materialice
IN-D01 a IN-D10 sin anticipar persistencia, aplicación o UI.

## Alcance

- módulo `inventory-api` con identidades, disponibilidad, movimientos y reservas;
- módulo `inventory` con descriptor CDI/SPI y dependencia requerida del catálogo;
- depósitos, ubicaciones y ubicación de sistema `GENERAL`;
- inscripción explícita de productos y políticas de trazabilidad;
- balance no negativo, libro inmutable, transferencias y reversiones;
- reservas con consumo/liberación/expiración;
- conteos con alcance, ciclo y ajuste por diferencia;
- pruebas unitarias y reglas ArchUnit.

## Fuera de alcance

- Flyway, esquema `plg_inventory`, JPA y repositorios;
- servicios transaccionales, permisos, auditoría y endpoints;
- menú, Jakarta Faces, Material Design, slots y Playwright;
- composición en WAR/migrador, Docker y datos de demo;
- compras, ventas, documentos, SIFEN, costos, valoración y contabilidad;
- eventos/outbox sin un consumidor real aprobado.

## Criterios de aceptación

- **CA-01:** API e implementación son módulos Maven físicos diferentes.
- **CA-02:** el API usa solo Java estándar, su paquete y `CompanyId`.
- **CA-03:** contrato `1.0.0`, UUID canónicos y consultas/comandos por propósito.
- **CA-04:** descriptor `inventory@1.0.0` exige `commercial_catalog` 1.x y no
  inventa contribuciones futuras.
- **CA-05:** depósito y ubicación son obligatorios; `GENERAL` nace con el depósito
  y no puede inactivarse.
- **CA-06:** solo un `PRODUCT` activo se inscribe y lote/serie/vencimiento cumplen
  su política; una serie mueve cantidad base uno.
- **CA-07:** cantidades/factores respetan escalas 6/12 y conservan el snapshot de
  conversión y versión.
- **CA-08:** físico, reservado y disponible nunca son negativos.
- **CA-09:** movimientos contabilizados son inmutables y una transferencia conserva
  dimensiones y cantidad entre dos ubicaciones distintas.
- **CA-10:** reserva conserva fuente, vencimiento, remanente y ciclo explícito.
- **CA-11:** conteo bloquea su alcance y genera ajustes, no sobrescribe saldos.
- **CA-12:** no existen dinero, JPA, SQL, migraciones, UI, composición ni eventos.
- **CA-13:** pruebas de ambos módulos, ArchUnit, reactor y documentación quedan
  verdes.

## Resultado

Los módulos, contratos y dominio quedaron implementados y verdes. Las 13
condiciones de aceptación están cumplidas: 12 pruebas propias del plugin, 4 del
API, 19 reglas dirigidas de arquitectura, 24/24 módulos y 321 pruebas acumuladas
sin fallos. El JAR no contiene migraciones y el WAR base no incorpora inventario.

La documentación validó 220 archivos Markdown y 831 enlaces locales sin errores
de UTF-8, mojibake o enlaces rotos. Consulte la
[evidencia reproducible](../../evidence/J11-S8-02-dominio-contratos-inventory.md).

## Continuidad

Se habilita J11-S8-03 para diseñar esquema privado, migraciones y repositorios. No
se habilitan todavía aplicación, permisos, UI ni composición física.
