# ADR-0049 — Modelo `sales` y contratos públicos

- Estado: Aceptado
- Fecha: 2026-08-20

## Decisión

Crear `sales-api` como Java puro y `sales` como plugin funcional separado. Cotización y pedido son agregados distintos; un pedido puede ser directo o derivado de una cotización aceptada. Los documentos conservan snapshots de cliente, catálogo, unidad, precio, impuesto, moneda y condición comercial.

La cotización consulta disponibilidad pero nunca reserva. Confirmar un pedido exige una reserva pública de `inventory` por cada línea administrada como stock; cancelar expone esas reservas para su liberación transaccional por la futura capa de aplicación. Toda transición usa versión esperada.

`sales` depende únicamente de los contratos públicos 1.x de `business_partners`, `commercial_catalog`, `reference_data` e `inventory`. Este corte no aporta persistencia, permisos, menús ni pantallas.

## Consecuencias

- La API pública no filtra entidades, DTO internos ni Jakarta.
- Los documentos históricos no cambian cuando cambian los maestros.
- Persistencia, idempotencia durable, autorización y coordinación JTA se incorporarán en historias posteriores.
