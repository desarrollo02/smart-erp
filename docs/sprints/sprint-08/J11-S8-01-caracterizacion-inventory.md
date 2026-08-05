# J11-S8-01 - Caracterización de `inventory`

- Estado: Completada; IN-D01 a IN-D10 confirmadas sin cambios el 2026-07-31
- Sprint: 8
- Fecha: 2026-07-31
- Tipo: conocimiento del legado y requisitos
- Dependencia: [J11-S8-00](J11-S8-00-gobierno-planificacion.md)
- Evidencia principal: [caracterización del legado](../../knowledge-base/inventory/legacy-characterization.md)

## Objetivo

Entender el comportamiento útil de depósitos, ubicaciones, existencias,
movimientos, reservas y conteos del legado y convertirlo en lenguaje, casos de
uso, invariantes y decisiones neutrales antes de diseñar el tercer plugin
productivo.

## Estado inicial

Sprint 7 dejó técnico y visualmente verde `commercial_catalog`. Sprint 8 estaba
planificado para `inventory`, pero no existían requisitos aceptados, dominio, API,
módulo Maven, esquema, migración, permiso, menú ni pantalla.

El legado se consultó exclusivamente en modo de solo lectura bajo
`C:\cosme\multienvios\miaterra\fuente\tag`. No se modificó y no se copió código.

## Trabajo realizado

1. Se leyeron las reglas locales del proyecto legado antes de inspeccionarlo.
2. Se localizaron menú, permisos, entidades, EJB, controladores y pantallas de
   existencia, entrada/salida, historial, ubicación, depósito, reservas y conteo.
3. Se contrastó `StwExistenciaArt` con `StwEntSalEJB` y `StwMovimientoArt` para
   distinguir saldo mutable, efecto operativo e historial observado.
4. Se verificaron transferencias, rechazo de negativos, unidad base, lote,
   vencimiento, inventario físico y referencias hacia dominios externos.
5. Se separaron compras, remisiones, ventas, producción, logística, costos y
   contabilidad del alcance de `inventory`.
6. Se revisaron los contratos públicos vigentes de `commercial-catalog-api` para
   evitar proponer entidades o SQL cruzados.
7. Se documentaron veinte observaciones, dieciocho casos de uso, veintidós
   invariantes, snapshot de catálogo, permisos, riesgos y migración futura.
8. Se prepararon alternativas, impacto y recomendación para IN-D01 a IN-D10.

## Hallazgos determinantes

- La existencia legado tiene una sola fila por empresa, sucursal y artículo; no
  representa depósito/ubicación/lote/serie como clave de saldo.
- El servicio actual modifica directamente `cantDispon`; el historial asociado
  posee operaciones de actualizar y eliminar.
- El motivo especial de inventario reemplaza el saldo en vez de generar una
  diferencia explicada por un ajuste.
- El rechazo de stock negativo es comportamiento útil, pero necesita protección
  transaccional e idempotencia bajo concurrencia.
- Una transferencia crea débito y crédito; el nuevo dominio debe hacerlos
  atómicos y conservar una única identidad de traslado.
- Lote y vencimiento existen en detalles, pero no se concilian contra saldos por
  esas dimensiones.
- La reserva encontrada no posee fuente, consumo parcial, liberación, vencimiento
  ni servicio operativo completo.
- Ubicación es texto ligado a artículo/sucursal; debe convertirse en maestro del
  depósito, independiente del concepto.
- Unidad presentada y unidad base aparecen separadas, pero no queda un snapshot
  contractual claro del factor aplicado.
- La cabecera de entrada/salida importa directamente compras, proveedores,
  remisiones, pedidos, personas, taller, producción y tesorería.
- El menú llamado Stock combina catálogo, compras, documentos, flota, rutas y
  reportes. Esa agrupación visual no define la frontera del nuevo plugin.
- La pantalla de control de inventario observada contiene componentes
  inconsistentes; no debe portarse.

## Alcance funcional candidato

Si las decisiones son aceptadas, `inventory` administrará:

- depósitos, ubicaciones y conceptos inventariables por empresa;
- políticas de lote, serie, vencimiento y condición;
- libro inmutable y proyección de físico/reservado/disponible;
- entrada, salida, transferencia, ajuste y reversión;
- reservas con asignaciones, consumo, liberación y expiración;
- conteos físicos y ajustes de cierre;
- permisos, auditoría, versión, concurrencia e idempotencia;
- API pública mínima basada en IDs y proyecciones neutrales.

No administrará catálogo comercial, compra, venta, remisión, logística,
producción, costo, valoración, asiento, reposición ni SIFEN.

## Casos e invariantes candidatos

| Rango | Contenido |
|---|---|
| IN-UC-01 a IN-UC-05 | depósitos, ubicaciones, inscripción, disponibilidad e historial |
| IN-UC-06 a IN-UC-10 | entrada, salida, traslado, ajuste y reversión |
| IN-UC-11 a IN-UC-13 | reserva, consumo, liberación y expiración |
| IN-UC-14 a IN-UC-16 | inicio, captura, cierre o cancelación de conteos |
| IN-UC-17 a IN-UC-18 | idempotencia e inactivación segura |
| IN-I01 a IN-I22 | empresa, claves, cantidades, historia, concurrencia, reservas, conteos y contratos |

El detalle completo está en la
[base de conocimiento](../../knowledge-base/inventory/legacy-characterization.md#casos-de-uso-neutrales-candidatos).

## Recomendaciones presentadas a producto

| Decisión | Recomendación resumida | Estado |
|---|---|---|
| IN-D01 | depósito y ubicación obligatorios; ubicación `GENERAL` controlada | Aceptada |
| IN-D02 | inscripción local explícita de productos del catálogo | Aceptada |
| IN-D03 | políticas `NONE`/`LOT`/`SERIAL`, vencimiento y condición | Aceptada |
| IN-D04 | libro inmutable, tipos estables, origen neutral e idempotencia | Aceptada |
| IN-D05 | saldo en unidad base y snapshot de conversión pública | Aceptada |
| IN-D06 | prohibir físico y disponible negativos en V1 | Aceptada |
| IN-D07 | reserva asignada con ciclo, consumo parcial y expiración | Aceptada |
| IN-D08 | V1 administra cantidades; valoración monetaria queda fuera | Aceptada |
| IN-D09 | conteo con cierre temporal acotado y ajuste inmutable | Aceptada |
| IN-D10 | API Java pura mínima; eventos al existir consumidor real | Aceptada |

Las alternativas e impactos completos están en
[IN-D01 a IN-D10](../../knowledge-base/inventory/legacy-characterization.md#decisiones-para-confirmación-de-producto).

## Criterios de aceptación

- **CA-01:** comportamiento legado y decisión nueva quedan separados.
  **Cumplido.**
- **CA-02:** inventario no contiene pedidos, facturas, expedición ni asientos.
  **Cumplido.**
- **CA-03:** se documentan casos, invariantes, concurrencia y ejemplos de efecto.
  **Cumplido.**
- **CA-04:** IN-D01 a IN-D10 tienen alternativas, impacto, recomendación y estado.
  **Cumplido; aceptadas sin cambios el 2026-07-31.**
- **CA-05:** toda referencia a catálogo usa API pública e identificadores.
  **Cumplido.**
- **CA-06:** la matriz cubre dominio, contratos, ArchUnit, PostgreSQL,
  composición, concurrencia, seguridad, Docker, Playwright e instalador Windows.
  **Cumplido.**
- **CA-07:** cada pantalla futura incluye 375, 720 y 1280 px. **Cumplido como
  criterio; no existe UI en esta historia.**
- **CA-08:** el cierre conserva demo, guía, retrospectiva, PDF e instalador
  Windows verificado. **No aplica aún; Sprint 8 permanece abierto.**
- **CA-09:** el proyecto legado permanece sin modificaciones. **Cumplido por
  procedimiento de solo lectura.**
- **CA-10:** cualquier nueva decisión arquitectónica se registra en un ADR.
  **Cumplido; esta historia propone decisiones de producto, no altera el
  baseline.**

## Pruebas y validación

No se modificó código, runtime ni persistencia; no corresponden Maven,
PostgreSQL, Docker o Playwright. El gate aplicable es documental: trazabilidad,
enlaces locales, UTF-8, texto dañado y secretos. Su resultado se conserva en la
[evidencia de J11-S8-01](../../evidence/J11-S8-01-caracterizacion-inventory.md).

## Resultado

La caracterización técnica y la aceptación de producto están completas. La
confirmación de IN-D01 a IN-D10 autoriza `J11-S8-02` para crear el dominio neutral
y los contratos públicos versionados. Persistencia, migraciones, aplicación, UI y
composición no se adelantan.
