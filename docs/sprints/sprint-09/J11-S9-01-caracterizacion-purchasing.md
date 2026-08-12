# J11-S9-01 - Caracterización de `purchasing`

- Estado: Completada; PU-D01 a PU-D10 aceptadas sin cambios por producto
- Sprint: 9
- Fecha: 2026-08-11
- Tipo: conocimiento del legado y requisitos
- Dependencia: [J11-S9-00](J11-S9-00-gobierno-planificacion.md)
- Evidencia principal: [caracterización del legado](../../knowledge-base/purchasing/legacy-characterization.md)

## Objetivo

Entender el comportamiento útil de solicitudes, órdenes, recepciones y procesos
de compra del legado actualizado, separar sus responsabilidades y convertirlas en
casos, invariantes y decisiones antes de crear el plugin.

## Fuente observada

- ruta: `C:\cosme\mega\miaterra\fuente\tag`;
- commit: `7fa64a7313940527a1b16856fbbccbad38f7c916`;
- última revisión: 2026-08-10T20:47:41-03:00;
- modo: solo lectura;
- base de datos: no consultada.

No se modificó ni se copió código legado.

## Trabajo realizado

1. se leyeron las instrucciones locales del proyecto legado;
2. se localizaron menús, permisos, pantallas, controladores, entidades y
   documentación de solicitudes, órdenes, Compras V2 y recepción pendiente;
3. se contrastaron los modelos `TswSolicitudCompra` y `StwOrdenCompra`;
4. se revisaron estado, aprobador, solicitante, proveedor, moneda, cantidades
   previstas/finales, precios, totales e historial;
5. se identificaron cruces directos con comprobantes, pagos, rendiciones,
   contabilidad, inventario, taller, agricultura, ventas e importación;
6. se revisaron los contratos públicos existentes de socios, catálogo, referencia
   e inventario;
7. se separaron solicitud, orden, recepción, devolución, factura y proceso
   financiero;
8. se documentaron 25 observaciones, 20 casos de uso, 26 invariantes, snapshots,
   dependencias, riesgos, preguntas de datos y matriz de pruebas;
9. se prepararon alternativas y recomendaciones PU-D01 a PU-D10.

## Hallazgos determinantes

- El legado contiene modelos superpuestos para solicitud y orden.
- La solicitud simple exige proveedor demasiado temprano.
- La orden mezcla estados operativos con anticipo, pago y recepción.
- Cantidad prevista y cantidad final comparten línea mutable.
- La recepción se origina desde la factura pendiente, no desde la orden.
- La recepción actualiza inventario y datos monetarios del artículo en la misma
  familia funcional.
- Existen relaciones JPA directas hacia muchos dominios.
- No se halló un flujo completo y claro de devolución a proveedor.
- No existe una tolerancia contractual visible para sobre-recepción.
- Los APIs actuales permiten resolver proveedor, ítem, moneda y movimiento, pero
  cualquier dato adicional debe ampliarse en el propietario, nunca por SQL.

## Frontera propuesta

`purchasing` poseerá solicitud, orden, recepción, devolución, snapshots, estados,
asignaciones, secuencias, idempotencia y auditoría. Consumirá:

- proveedor por `business-partners-api`;
- ítem/unidad por `commercial-catalog-api`;
- moneda por `reference-data-api`;
- entrada/salida física por `inventory-api`.

Factura, deuda, pago, retención, cuenta bancaria, asiento, costo e importación no
forman parte de V1.

## Decisiones presentadas

| Decisión | Recomendación resumida | Estado |
|---|---|---|
| PU-D01 | compra local; solicitud sin proveedor y orden con proveedor | Aceptada sin cambios |
| PU-D02 | líneas de catálogo o libres, clasificadas `STOCK`/`NON_STOCK`/`SERVICE` | Aceptada sin cambios |
| PU-D03 | ciclos separados y una aprobación simple con separación de funciones | Aceptada sin cambios |
| PU-D04 | asignaciones parciales entre solicitudes y órdenes | Aceptada sin cambios |
| PU-D05 | moneda validada; precio esperado; fiscalidad/tipo de cambio fuera de V1 | Aceptada sin cambios |
| PU-D06 | recepción append-only parcial, sin sobre-recepción en V1 | Aceptada sin cambios |
| PU-D07 | devolución propia y compensatoria contra recepción | Aceptada sin cambios |
| PU-D08 | IDs públicos y snapshots mínimos, sin leer tablas privadas | Aceptada sin cambios |
| PU-D09 | versión, idempotencia, historia append-only y compensaciones | Aceptada sin cambios |
| PU-D10 | APIs Java puras y llamada pública síncrona a inventario | Aceptada sin cambios |

## Criterios de aceptación

- **CA-01:** comportamiento legado y propuesta nueva están separados.
  **Cumplido.**
- **CA-02:** factura, deuda, pago, retención y asiento están fuera.
  **Cumplido.**
- **CA-03:** se documentan casos, invariantes, ciclos y snapshots. **Cumplido.**
- **CA-04:** PU-D01 a PU-D10 tienen alternativas y recomendación. **Cumplido y
  aceptado sin cambios por producto el 2026-08-11.**
- **CA-05:** todas las dependencias usan APIs públicas. **Cumplido como regla de
  diseño.**
- **CA-06:** se registran preguntas que requieren base de datos. **Cumplido; no se
  consultó sin autorización.**
- **CA-07:** se define matriz acumulada de pruebas. **Cumplido; ejecución
  pendiente por decisión de producto.**
- **CA-08:** el legado permanece sin modificaciones. **Cumplido.**
- **CA-09:** no se adelantan módulos, persistencia ni UI. **Cumplido.**
- **CA-10:** el resultado enlaza épica y Sprint. **Cumplido.**

## Pruebas y validación

No se ejecutaron Maven, PostgreSQL, Docker o Playwright. La historia no cambia
runtime y producto decidió acumular las pruebas. Se realizó inspección estática y
trazabilidad documental; esto no constituye un resultado automatizado verde.

## Resultado

Producto aceptó PU-D01 a PU-D10 sin cambios el 2026-08-11. J11-S9-02 queda
habilitada para crear `purchasing-api` y el dominio neutral. Persistencia,
migraciones, permisos, menús y pantallas permanecen fuera de esta historia.
