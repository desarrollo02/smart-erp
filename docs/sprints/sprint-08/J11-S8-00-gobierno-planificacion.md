# J11-S8-00 - Gobierno y planificación de `inventory`

> Nota posterior: ADR-0029 sustituyó el gate automático del punto 9. En el cierre
> se pregunta si se creará un instalador; sólo con respuesta `SÍ` se genera y
> prueba como último gate.

- Estado: Completada; IN-D01 a IN-D10 confirmadas sin cambios el 2026-07-31
- Sprint: 8
- Fecha: 2026-07-31
- Dependencia: G0-G6 de `J11-S7-07` verdes
- ADR rector: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)

## Objetivo

Convertir el tercer plugin del roadmap en un backlog verificable antes de leer
tablas concretas, diseñar agregados o crear código. La historia debe separar
existencia, movimiento, reserva, depósito y ubicación de catálogo, compras,
ventas, logística, documentos y contabilidad.

## Actividades

1. inspeccionar el legado de solo lectura y registrar comportamiento, no código;
2. describir entradas, salidas, transferencias, ajustes, reservas y liberaciones;
3. identificar dimensiones necesarias: empresa, depósito, ubicación, artículo,
   unidad, lote, serie y estado cuando correspondan;
4. decidir políticas de stock negativo, concurrencia e idempotencia;
5. definir qué datos de catálogo se resuelven en línea y cuáles se conservan como
   snapshot del movimiento;
6. separar cantidad física, disponible, reservada y en tránsito;
7. diseñar contratos públicos mínimos hacia/desde catálogo y consumidores futuros;
8. preparar seguridad, auditoría, migración, responsive, demo y recuperación;
9. congelar el baseline, preguntar si se creará un instalador Windows y, con
   respuesta `SÍ`, generarlo/probarlo como último gate.

## Decisiones que debe confirmar producto

- `IN-D01`: jerarquía depósito/ubicación y obligatoriedad de ubicación;
- `IN-D02`: conceptos de catálogo habilitados para inventario;
- `IN-D03`: dimensiones iniciales de lote, serie, vencimiento y estado;
- `IN-D04`: tipos de movimiento, motivo, documento origen e idempotencia;
- `IN-D05`: unidad de stock, conversiones y precisión;
- `IN-D06`: política de stock negativo por empresa/depósito;
- `IN-D07`: ciclo de vida y expiración de reservas;
- `IN-D08`: alcance de costos y separación de valoración contable;
- `IN-D09`: conteos, ajustes, cierres y correcciones sin borrar historia;
- `IN-D10`: contratos públicos, snapshots y eventos para compras/ventas/logística.

Ninguna decisión se consideró aceptada por silencio. El responsable de producto
confirmó las diez recomendaciones sin cambios el 2026-07-31 y autorizó
`J11-S8-02`.

## Criterios de aceptación

- **CA-01:** comportamiento legado y decisión nueva quedan separados.
- **CA-02:** inventario no contiene pedidos, facturas, expedición ni asientos.
- **CA-03:** se documentan casos, invariantes, concurrencia y ejemplos ficticios.
- **CA-04:** IN-D01 a IN-D10 tienen alternativas y estado explícito.
- **CA-05:** toda referencia a catálogo usa API pública e identificadores.
- **CA-06:** la matriz cubre unitarias, ArchUnit, PostgreSQL, composición,
  concurrencia, seguridad, Docker y Playwright.
- **CA-07:** cada pantalla incluye 375, 720 y 1280 px desde su historia.
- **CA-08:** el cierre conserva demo visual, guía, retrospectiva, PDF e instalador Windows verificado.
- **CA-09:** el proyecto legado permanece sin modificaciones.
- **CA-10:** cualquier nueva decisión arquitectónica se registra en un ADR.

## Regla de inicio

`J11-S8-01` completó caracterización y propuestas; la confirmación explícita
autoriza comenzar modelado neutral y contratos en `J11-S8-02`. G7 independiente
sigue bloqueando promoción, publicación `1.0` y producción.

La decisión transversal del 2026-07-31 agrega J11-S8-08 después de la validación
funcional. El instalador no autoriza adelantar persistencia, aplicación o UI fuera
de la historia correspondiente.

J11-S8-02 quedó verde el 2026-07-31 con ADR-0023, API pública, dominio neutral,
ArchUnit y reactor completo. El siguiente alcance autorizado es J11-S8-03.
