# J11-S7-01 - Caracterización de `commercial_catalog`

- Estado: Completada; CC-D01 a CC-D10 confirmadas sin cambios el 2026-07-30
- Sprint: 7
- Fecha: 2026-07-30
- Tipo: conocimiento del legado y requisitos
- Dependencia: [J11-S7-00](J11-S7-00-gobierno-planificacion.md)
- Evidencia principal: [caracterización del legado](../../knowledge-base/commercial-catalog/legacy-characterization.md)

## Objetivo

Entender el comportamiento útil de artículos, productos, servicios, unidades,
clasificaciones, impuestos y precios del legado y convertirlo en lenguaje, casos
de uso, invariantes y decisiones neutrales antes de diseñar el segundo plugin
productivo.

## Estado inicial

Sprint 6 dejó técnico y visualmente verde `business_partners`. Sprint 7 estaba
planificado para `commercial_catalog`, pero no existían requisitos aceptados,
dominio, API, módulo Maven, esquema, migración ni pantalla.

El legado fue consultado exclusivamente en
`C:\cosme\multienvios\miaterra`. No se modificó y no se copió código.

## Trabajo realizado

1. Se localizaron el maestro de artículos, sus pantallas/controladores y los
   catálogos de unidad, marca, familia, grupo, línea e IVA.
2. Se contrastaron listas configurables, precios efectivos, conversiones por
   artículo, importación masiva y consumo de precios en ventas.
3. Se identificó que `StwArticulos` concentra relaciones de catálogo, inventario,
   compras, ventas, contabilidad, producción, taller y transporte.
4. Se separaron identidad/descripción, definiciones y precios de existencias,
   costos, documentos y reglas sectoriales.
5. Se documentaron dieciocho observaciones, quince casos de uso, dieciocho
   invariantes y los snapshots que deberán conservar futuros consumidores.
6. Se registraron deudas que no deben copiarse: `MAX + 1`, baja física, estados
   heterogéneos, precios fijos duplicados, código de barras único y JPA cruzada.
7. Se prepararon alternativas, impacto y recomendación para CC-D01 a CC-D10.

## Hallazgos determinantes

- Producto y servicio aparecen en un mismo maestro; `COMPRA`, `VENTA` o ambos es
  una dimensión diferente del tipo de ítem.
- `indManejaStock` permite que los procesos distingan conceptos sin existencia,
  pero la existencia pertenece al futuro plugin `inventory`.
- El código ya se aísla por empresa, aunque subsiste un generador `MAX + 1` no
  seguro y otra función específica del legado.
- Una sola columna de barcode no cubre identificadores por empaque o tipo.
- Talle, color y modelo muestran una necesidad real de variantes, pero el
  generador acoplado no debe copiarse.
- Las conversiones por ítem poseen factor positivo y defaults por operación; es
  comportamiento útil que requiere precisión y snapshot.
- Las listas de precio por empresa y su orden son más estables que los campos
  `precioBase`, `precio2` y `precio3`.
- Elegibilidad por cliente y condición de venta pertenece a ventas, no al catálogo.
- La tabla IVA combina clasificación, tasas y vigencia. El nuevo dominio debe
  separar perfil interno y mapeos fiscales/SIFEN.
- Inactivar listas conserva precios anteriores; la misma conservación debe regir
  para ítems, unidades, clasificaciones y reemplazos.

## Alcance funcional candidato

Si las decisiones son aceptadas, `commercial_catalog` administrará:

- ítems producto/servicio con ID estable, código empresarial y alcance compra/venta;
- identificadores alternativos;
- categorías, marca y fundamento acotado de variantes;
- unidades base y conversiones por ítem;
- perfiles tributarios internos efectivos;
- listas y entradas de precio con moneda, modo tributario y vigencia;
- búsqueda, consulta, alta, modificación, inactivación, reactivación y reemplazo;
- referencias y cotizaciones públicas mínimas para otros plugins.

No administrará existencias, depósitos, movimientos, costos, proveedores,
promociones, condiciones por cliente, pedidos, documentos, cuentas contables,
producción ni integración SIFEN.

## Casos de uso candidatos

| Rango | Contenido |
|---|---|
| CC-UC-01 a CC-UC-04 | buscar, consultar, registrar y modificar ítems |
| CC-UC-05 a CC-UC-09 | identificadores, clasificación, variantes, unidades y perfil tributario |
| CC-UC-10 a CC-UC-11 | listas y entradas de precio |
| CC-UC-12 a CC-UC-13 | ciclo de vida y reemplazo |
| CC-UC-14 a CC-UC-15 | referencia pública y cotización determinista |

El detalle está en la
[base de conocimiento](../../knowledge-base/commercial-catalog/legacy-characterization.md#casos-de-uso-neutrales-candidatos).

## Recomendaciones presentadas a producto

| Decisión | Recomendación resumida | Estado |
|---|---|---|
| CC-D01 | agregado común con tipo inmutable `PRODUCT`/`SERVICE` | Aceptada |
| CC-D02 | código empresarial más múltiples identificadores tipados | Aceptada |
| CC-D03 | fundamento relacional de variantes, sin generador masivo inicial | Aceptada |
| CC-D04 | unidad base y conversiones específicas por ítem/propósito | Aceptada |
| CC-D05 | perfil tributario interno versionado y mapeo fiscal separado | Aceptada |
| CC-D06 | listas/precios en catálogo; elegibilidad y prioridad en ventas | Aceptada |
| CC-D07 | moneda ISO, modo tributario y redondeo fijados por lista | Aceptada |
| CC-D08 | categorías jerárquicas, marca y etiquetas controladas | Aceptada |
| CC-D09 | inactivación y reemplazo opcional; sin baja física normal | Aceptada |
| CC-D10 | contratos pequeños y snapshots propiedad de consumidores | Aceptada |

Las alternativas e impactos completos están en
[CC-D01 a CC-D10](../../knowledge-base/commercial-catalog/legacy-characterization.md#decisiones-para-confirmación-de-producto).
El responsable de producto confirmó las diez recomendaciones sin cambios el
2026-07-30 y ratificó que el catálogo será otro módulo/plugin independiente.

## Criterios de aceptación

- **CA-01:** fuentes del legado identificadas y consultadas en modo de solo lectura.
  **Cumplido.**
- **CA-02:** existe glosario neutral y no se trasladan nombres como contratos.
  **Cumplido.**
- **CA-03:** alta, consulta, modificación, búsqueda, clasificación, precios y ciclo
  de vida están caracterizados. **Cumplido.**
- **CA-04:** catálogo se separa de stock, costos, compras, ventas, documentos y
  SIFEN. **Cumplido.**
- **CA-05:** casos de uso, permisos, invariantes, snapshots, riesgos y migración
  futura están documentados. **Cumplido.**
- **CA-06:** CC-D01 a CC-D10 tienen alternativas, recomendación e impacto.
  **Cumplido; confirmación pendiente.**
- **CA-07:** producto acepta o modifica CC-D01 a CC-D10.
  **Cumplido; aceptadas sin cambios el 2026-07-30.**
- **CA-08:** la aceptación autoriza explícitamente `J11-S7-02`.
  **Cumplido.**

## Pruebas y validación

No se modificó código; no corresponden Maven, PostgreSQL, Docker o Playwright. El
gate aplicable es documental: archivos, trazabilidad, enlaces locales, UTF-8,
mojibake y secretos. Su resultado se conserva en la
[evidencia de J11-S7-01](../../evidence/J11-S7-01-caracterizacion-commercial-catalog.md).

## Resultado

La caracterización y su aceptación quedan completas. `J11-S7-02` está autorizada
para crear API y dominio como módulos separados. Persistencia, aplicación y UI no
se adelantan.
