# PBI-2026-08-20-001 - Gestionar presupuestos, pedidos y compromisos de venta

## Clasificación

- Tipo: Historia
- Módulo: `sales`
- Rutas: `/sales/quotes`, `/sales/orders`, `/sales/commitments`, `/sales/terms`
- Estado: Listo
- Prioridad: Pendiente de Product Owner
- Estimación: Pendiente del equipo
- Creado: 2026-08-20

## Necesidad e historia

El legado mezcla pedido, factura, cobro, remisión, entrega y transporte. Como
vendedor, quiero emitir presupuestos y confirmar pedidos con precios y
disponibilidad verificables, para comprometer ventas sin perder trazabilidad ni
prometer stock inexistente.

## Contexto confirmado

- Las tablas antiguas de presupuesto están vacías; el modelo activo contiene
  1.021 pedidos y 2.637 líneas.
- Producto confirmó presupuesto/pedido V1, pedido directo o derivado, reserva al
  confirmar y exclusión de factura, pagos, cuentas por cobrar, logística y POS.
- SA-D01 a SA-D10 fueron aceptadas sin cambios.
- La solución será una reimplementación neutral; no se copiará el modelo legado.

## Alcance y reglas

- Agregados separados, aceptación idempotente y vínculo de origen.
- Condiciones comerciales por empresa, líneas de catálogo, snapshots y totales.
- Reserva por línea inventariable; servicios no reservan.
- Sólo borradores se editan estructuralmente.
- Excepciones de precio/descuento requieren permiso y motivo.
- Cancelar libera reservas remanentes sin borrar historia.
- Cerrar no significa facturar, cobrar o entregar.

Quedan fuera documentos/SIFEN, deuda/crédito/cobro, logística/entrega, POS y
migración masiva del legado.

## Criterios de aceptación

### CA-01 - Separar presupuesto y pedido

Dado un presupuesto emitido, cuando se acepta, entonces se crea una sola vez un
pedido nuevo y ambos conservan identidad, estado e historia propios.

#### Ejemplo verificado con datos reales

- Ambiente: Miaterra local de desarrollo
- Consultado: 2026-08-20, America/Asuncion
- Identificador: `PED-001-431`
- Datos enmascarados: cliente, usuario, IDs e importe
- Origen: `public.vtw_presupuestos_*` y `public.vtw_pedidos_*`
- Tipo de resultado: Resultado esperado, no ejecutado

| Campo | Valor |
|---|---|
| presupuesto antiguo | 0 cabeceras / 0 líneas |
| pedido observado | 1 cabecera / 1 línea |

El legado concentra la preventa en pedido; el resultado esperado separa ambos y
conserva un vínculo inmutable, corrigiendo exactamente la superposición observada.

### CA-02 - Confirmar con reserva atómica

Dado un pedido con líneas inventariables, cuando se confirma, entonces todas sus
reservas se crean idempotentemente o el pedido permanece borrador.

#### Ejemplo verificado con datos reales

- Ambiente: Miaterra local de desarrollo
- Consultado: 2026-08-20, America/Asuncion
- Identificador: `PED-001-431`
- Datos enmascarados: cliente, artículo, ubicación, IDs e importe
- Origen: `vtw_pedidos_detalle.cantidad`, `cantidad_facturada`
- Tipo de resultado: Resultado esperado, no ejecutado

| Campo | Valor |
|---|---:|
| cantidad | 1,000 |
| facturada | 0 |

La cantidad real será la entrada de la reserva; el legado no conserva una reserva
pública verificable y no se ejecutó una escritura para fabricar el resultado.

### CA-03 - Cancelar con compensación e historia

Dado un pedido confirmado con reserva remanente, cuando se cancela, entonces se
libera el remanente y se conservan documento, líneas, motivo, actor y referencias.

#### Ejemplo verificado con datos reales

- Ambiente: Miaterra local de desarrollo
- Consultado: 2026-08-20, America/Asuncion
- Identificador: distribución de estados
- Datos enmascarados: actores e IDs
- Origen: `vtw_pedidos_cabecera.estado` y controlador de anulación
- Tipo de resultado: Resultado esperado, no ejecutado

| Campo | Valor |
|---|---:|
| anulados `A` | 73 |
| desanulación legada | vuelve a estado nulo |

El criterio reemplaza la pérdida de semántica observada por una cancelación
terminal y compensatoria.

### CA-04 - Preservar snapshots comerciales

Dado un documento emitido/confirmado, cuando cambian cliente, artículo, precio,
moneda o condición, entonces el histórico mantiene los valores usados.

#### Ejemplo verificado con datos reales

- Ambiente: Miaterra local de desarrollo
- Consultado: 2026-08-20, America/Asuncion
- Identificador: `PED-001-431`
- Datos enmascarados: referencias, cliente e importe
- Origen: `vtw_pedidos_cabecera.id_moneda`, `id_lista_precio`, `id_condicion_venta`
- Tipo de resultado: Resultado esperado, no ejecutado

| Campo | Valor |
|---|---|
| moneda | referencia presente |
| condición | referencia presente |
| lista | ausente |

La combinación real demuestra que una FK mutable no basta; el documento nuevo
guardará códigos, nombres, versiones y valores cotizados.

### CA-05 - Usar ciclos explícitos

Dado cualquier documento de Ventas, cuando se crea o transiciona, entonces su
estado es explícito y autorizado, nunca nulo.

#### Ejemplo verificado con datos reales

- Ambiente: Miaterra local de desarrollo
- Consultado: 2026-08-20, America/Asuncion
- Identificador: distribución completa de estados
- Datos enmascarados: no aplica
- Origen: `vtw_pedidos_cabecera.estado`
- Tipo de resultado: Resultado observado

| Estado | Registros |
|---|---:|
| `F` | 543 |
| nulo | 399 |
| `A` | 73 |
| `P` | 6 |

La distribución prueba directamente el estado nulo y la mezcla con facturación
que los ciclos aceptados eliminan.

### CA-06 - Mantener fuera documentos, dinero y logística

Dado un pedido confirmado, cuando otro proceso factura, cobra, despacha o entrega,
entonces `sales` recibe sólo referencias/hechos públicos y no posee esos objetos.

#### Ejemplo verificado con datos reales

- Ambiente: Miaterra local de desarrollo
- Consultado: 2026-08-20, America/Asuncion
- Identificador: `PED-001-420`
- Datos enmascarados: cliente, artículo, IDs e importe
- Origen: `vtw_pedidos_cabecera.estado`, `vtw_pedidos_detalle.cantidad_facturada`
- Tipo de resultado: Resultado esperado, no ejecutado

| Campo | Valor |
|---|---:|
| estado | `F` |
| cantidad | 1,000 |
| facturada | 1,000 |

El caso demuestra el acoplamiento actual; el resultado esperado mantiene el
compromiso y el documento fiscal en propietarios diferentes.

### CA-07 - Integrar sólo por APIs públicas

Dado `sales` presente, cuando resuelve cliente, precio, moneda o inventario,
entonces usa APIs públicas y no clases, DTO, JPA o SQL internos.

#### Ejemplo verificado con datos reales

- Ambiente: Miaterra local de desarrollo
- Consultado: 2026-08-20, America/Asuncion
- Identificador: metadatos de tablas de pedido
- Datos enmascarados: nombres/destinos de relaciones
- Origen: `information_schema.table_constraints`
- Tipo de resultado: Resultado esperado, no ejecutado

| Campo | Valor |
|---|---:|
| FKs en cabecera | 21 |
| FKs en detalle | 3 |

La cantidad real de cruces justifica el criterio; ArchUnit y el esquema privado
deberán demostrar cero accesos cruzados.

### CA-08 - Autorizar y adaptar las cuatro rutas

Dado un actor empresarial autorizado, cuando usa cualquiera de las rutas,
entonces el servidor revalida permiso/empresa/plugin/versión y la pantalla opera
en 375, 720 y 1280 px sin overflow horizontal normal.

#### Ejemplo verificado con datos reales

- Ambiente: Miaterra local de desarrollo
- Consultado: 2026-08-20, America/Asuncion
- Identificador: `PED-001-431`
- Datos enmascarados: cliente, artículo, usuario e importe
- Origen: una cabecera y línea de `vtw_pedidos_*`
- Tipo de resultado: Resultado esperado, no ejecutado

| Elemento | Valor |
|---|---|
| cabecera | fecha, moneda y condición presentes |
| líneas | 1 |
| cumplimiento | cantidad 1; facturada 0 |

El conjunto mínimo real permitirá probar lista, alta y detalle en los tres anchos
sin fabricar un documento funcional.

## Referencia e impacto preliminar

- Fuente: Miaterra, commit `ca5bdd74395182f69a9f876be7eb72f9ded0b2a7`.
- Nivel: reimplementación basada en comportamiento.
- Módulos previstos: `sales-api` y `sales`; esquema `plg_sales`.
- Integración: APIs de Socios, Catálogo, Referencia e Inventario.
- UI: contribuciones neutrales y shell; permisos por capacidad.

## Archivo de base de datos a ejecutar

| Archivo previsto | Propósito | Momento |
|---|---|---|
| `plugins/sales/src/main/resources/db/migration/sales/V1__initialize_sales_schema.sql` | crear esquema privado, presupuestos, pedidos, líneas, origen, condiciones, reservas, historia, idempotencia y restricciones | J11-S11-03 mediante migrador |

## Verificación esperada

JUnit, PostgreSQL/Testcontainers, migración limpia/repetida, concurrencia,
idempotencia, JPA/JTA, rollback multilínea, ArchUnit, composición presente/ausente,
Compose/health/OIDC, seguridad negativa, Playwright 375/720/1280, manuales, demo,
fotografía, PDF y decisión de instalador.

## Riesgos

- `InventoryReservations` exige clave exacta y vencimiento;
- condiciones comerciales requieren historia e inactivación;
- migrar estados nulos o `F` exige reglas posteriores, no inferencias;
- Docker estaba detenido y se validará en la historia aplicable.

## Definition of Ready

- [x] Problema, actor, valor, rutas, alcance y exclusiones definidos.
- [x] Ocho criterios con ejemplo real inmediato y protegido.
- [x] Dependencias y archivo SQL previsto identificados.
- [x] Base fuente verificada; destino aún no posee objetos `sales`.
- [x] Sin preguntas funcionales bloqueantes; estimable por el equipo.

## Historial

### 2026-08-20

- Creado tras inspección estática, consulta local de sólo lectura y aceptación de SA-D01 a SA-D10.
