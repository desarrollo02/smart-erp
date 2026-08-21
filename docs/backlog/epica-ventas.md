# Épica - Ventas `sales`

- Estado: aplicación y reservas validadas automáticamente; J11-S11-05 habilitada
- Orden: ERP 5
- Sprint: [Sprint 11](../sprints/sprint-11/README.md)
- ADR: [ADR-0011](../adr/0011-roadmap-dependencias-plugins-productivos.md)
- Caracterización: [legado actualizado](../knowledge-base/sales/legacy-characterization.md)
- PBI: [PBI-2026-08-20-001](sales/PBI-2026-08-20-001-presupuestos-pedidos-compromisos.md)

## Resultado de negocio

Cotizar y comprometer ventas con presupuestos/pedidos trazables, precios
históricos y reserva de stock al confirmar, sin incorporar factura, cobro,
despacho o entrega.

## Alcance inicial

- presupuesto con vigencia, emisión y decisión;
- pedido directo o derivado;
- cliente, catálogo, precio, moneda, condición y snapshots;
- excepciones de precio/descuento autorizadas;
- reserva idempotente al confirmar y liberación al cancelar;
- numeración, concurrencia, idempotencia, historia y auditoría;
- condiciones comerciales por empresa;
- cuatro pantallas responsive sobre floorplans 2.0;
- integración sólo por contratos públicos.

## Fuera de V1

Documentos/SIFEN, cuentas por cobrar, crédito, deuda, cobros, anticipos,
logística, entrega, POS, caja, contabilidad, ingresos, costos y comisiones.

## Dependencias

| API | Capacidad |
|---|---|
| `business-partners-api` | cliente activo `CLIENT` |
| `commercial-catalog-api` | ítem, unidad, tipo, precio e impuesto |
| `reference-data-api` | moneda habilitada y escala |
| `inventory-api` | disponibilidad y reservas |
| kernel | empresa, autorización, auditoría y activación |

## Historias

| Orden | Historia | Entregable |
|---:|---|---|
| 1 | J11-S11-00 | gobierno y planificación |
| 2 | J11-S11-01 | caracterización y decisiones |
| 3 | J11-S11-02 | `sales-api` y dominio |
| 4 | J11-S11-03 | persistencia y migraciones |
| 5 | J11-S11-04 | aplicación, seguridad y reservas |
| 6 | J11-S11-05 | UI Material Design 3 |
| 7 | J11-S11-06 | composición e integraciones |
| 8 | J11-S11-07 | matriz, demo, documentación, fotografía y PDF |
| 9 | J11-S11-08 | decisión de instalador Windows |

J11-S11-00/01 son documentales. Las historias de código exigirán prueba mínima
inmediata y gates proporcionales. Sólo la validación independiente puede diferirse.

## Criterios de la épica

- presupuesto/pedido separados y aceptación idempotente;
- pedido directo válido;
- confirmación con reservas atómicas y cancelación compensatoria;
- snapshots históricos y excepciones autorizadas;
- cierre separado de factura, cobro y entrega;
- revalidación de empresa, permiso, activación, versión e idempotencia;
- cuatro rutas accesibles en 375/720/1280;
- composición coherente con `sales` presente/ausente;
- `logistics` espera el Sprint visual guiado post-Ventas.

## Inicio de código

SA-D01 a SA-D10 están aceptadas. J11-S11-02 y J11-S11-03 están implementadas y
validadas automáticamente. J11-S11-04 implementó y validó automáticamente
aplicación, seguridad y reservas. J11-S11-05 queda habilitada como siguiente
historia; la validación independiente acumulada continúa pendiente.
