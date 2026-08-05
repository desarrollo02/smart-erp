# Caracterización del legado para `inventory`

- Fecha de inspección: 2026-07-31
- Proyecto observado: `C:\cosme\multienvios\miaterra\fuente\tag`
- Modo de trabajo: solo lectura
- Estado: caracterización completa; IN-D01 a IN-D10 confirmadas sin cambios el 2026-07-31
- Alcance futuro: tercer plugin productivo de Logixone

## Propósito

Transformar el comportamiento útil de existencias y movimientos del legado en
lenguaje, casos de uso, invariantes y decisiones neutrales antes de diseñar el
plugin. Este documento no aprueba todavía entidades, tablas, contratos Java ni
pantallas y no autoriza copiar código `javax.*`, SQL, controladores o XHTML.

El paquete legado llamado `stock` concentra responsabilidades que en Logixone
pertenecerán a varios plugins. La caracterización separa:

1. depósitos, ubicaciones y políticas de almacenamiento;
2. saldos físicos, disponibles y reservados;
3. entradas, salidas, transferencias y ajustes trazables;
4. reservas, conteos y correcciones;
5. compras, remisiones, producción, vehículos, rutas, costos y contabilidad.

Solo los cuatro primeros grupos son candidatos para `inventory`. El quinto queda
fuera y deberá integrarse mediante contratos públicos cuando exista su plugin.

## Fuentes contrastadas

Las rutas siguientes pertenecen al proyecto legado y se consultaron sin
modificarlas:

| Área | Fuente principal | Evidencia observada |
|---|---|---|
| menú y permisos | `WEB-INF/menuStock.xhtml`, `ConsultaPermisosVentasControlador` y `ApplicationConstant` | un único módulo agrupa maestros, movimientos, documentos, logística y reportes con permisos por forma |
| existencia | `StwExistenciaArt.java` | una fila única por empresa, sucursal y artículo; saldo disponible, bloqueado, mínimo, máximo y fecha de inventario |
| entrada/salida | `StwEntsalCab.java`, `StwEntsalDet.java`, `StwEntSalEJB.java` y pantalla `StwCOEntSal.xhtml` | cabecera/detalle por motivo, cantidades, unidad, costo, lote, vencimiento y múltiples referencias cruzadas |
| motivos | `StwMotivoEntSal.java` | código y un indicador `E`/`S` determinan el signo del movimiento |
| historial | `StwMovimientoArt.java` y `StwMovArtEjb.java` | filas de agregar/descontar para E/S y dos filas para una transferencia; el servicio permite modificar y eliminar |
| depósitos | `BswDeposito.java` | depósito asociado a sucursal, ciudad y dirección, usado también por remisiones |
| ubicación | `StwUbicArticulos.java` y `StwCOUbicArticulosControlador.java` | bloque, estante, fila y columna por artículo/sucursal; la opción de menú observada está deshabilitada |
| inventario físico | `StwInventCab.java`, `StwInventDet.java` y `StwCOControlInventario.xhtml` | cabecera por empresa/sucursal, cantidades real/física/bloqueada y fechas; la vista observada reutiliza componentes inconsistentes |
| ajuste por inventario | `StwEntSalEJB.actualizaPorInventario` | el motivo especial reemplaza directamente el saldo actual por la cantidad contada |
| reservas | `StwReservasArticulos.java` | número, fecha, cantidad, estado activo, artículo, usuario, sucursal y empresa; no se halló servicio o pantalla operativa que complete su ciclo |
| unidades | `StwUnidadesMedida.java` y cantidades `cantidad`/`cantidadUb` | unidad y multiplicador global; el detalle conserva una cantidad presentada y otra de unidad base |
| lote/vencimiento | `StwEntsalDet.java` | lote y vencimiento existen en el movimiento, pero no forman parte de la clave del saldo observado |
| stock negativo | `StwEntSalEJB.verificaStockNegativo` | una salida se rechaza si el concepto maneja stock y el saldo resultante es negativo |
| catálogo | `StwArticulos.java` | `indManejaStock` mezcla la habilitación de inventario con el maestro comercial |
| consultas | existencia, movimiento y reportes bajo `controlStock/` | consultas por artículo/sucursal, mínimos, comparativos y movimientos por motivo |

También se buscaron usos desde ventas, compras, remisiones, producción, taller,
agricultura y cuentas. Esos usos demuestran consumidores futuros; no transfieren
su lógica al plugin de inventario.

## Lenguaje neutral

| Término legado | Término candidato | Interpretación |
|---|---|---|
| sucursal usada como stock | depósito | unidad operativa que contiene ubicaciones dentro de una empresa |
| bloque/estante/fila/columna | ubicación | posición identificable dentro de un depósito |
| artículo que maneja stock | concepto inventariable | producto del catálogo inscrito explícitamente en inventario |
| existencia disponible | saldo físico | cantidad contabilizada físicamente para una clave de stock |
| cantidad bloqueada | cantidad reservada | parte del saldo físico no disponible para nuevas promesas |
| disponible | disponibilidad | `físico - reservado`, sin incluir cantidades futuras o en tránsito |
| entrada/salida | movimiento | cambio inmutable de cantidad con tipo, motivo, origen e identidad propia |
| motivo E/S | motivo | explicación controlada; no reemplaza al tipo técnico del movimiento |
| nota E/S | documento de movimiento | comando y registro interno de uno o más renglones |
| inventario | conteo físico | proceso controlado que compara cantidad teórica y contada |
| ajuste de inventario | ajuste | movimiento explícito por diferencia; no sobrescritura silenciosa del saldo |
| reserva activa | reserva de stock | compromiso temporal con ciclo, vencimiento y fuente identificable |
| cantidad UB | cantidad base | cantidad normalizada a la unidad base del ítem |
| lote/fecha vencimiento | dimensión de trazabilidad | clave opcional según política del concepto inventariable |
| transferencia | traslado | una operación atómica con débito de origen y crédito de destino |

## Comportamiento observado y requisito neutral

| ID | Observación del legado | Requisito neutral candidato | Tratamiento |
|---|---|---|---|
| IN-O01 | el saldo se identifica por empresa, sucursal y artículo | saldo aislado por empresa, depósito, ubicación, ítem y dimensiones de trazabilidad | ampliar; IN-D01/IN-D03 |
| IN-O02 | `cantDispon` se modifica directamente | libro de movimientos inmutable y proyección de saldo transaccional | corregir; IN-D04 |
| IN-O03 | `cantBloqueo` no posee trazabilidad propia | reservas explícitas cuya suma explica lo reservado | corregir; IN-D07 |
| IN-O04 | mínimos y máximos comparten la fila de saldo | política de reposición separada y diferida | excluir del primer corte |
| IN-O05 | el signo depende de un texto `E`/`S` del motivo | tipo de movimiento estable y motivo controlado independiente | endurecer; IN-D04 |
| IN-O06 | una transferencia crea una salida y una entrada | una transferencia atómica conserva ambos extremos y una identidad común | conservar y endurecer |
| IN-O07 | el servicio de historial puede actualizar o borrar | nunca modificar o borrar un movimiento contabilizado; revertir con otro movimiento | rechazar; IN-D04/IN-D09 |
| IN-O08 | inventario reemplaza el saldo directamente | conteo cerrado genera ajuste por diferencia y conserva antes/después | corregir; IN-D09 |
| IN-O09 | lote y vencimiento existen solo en el detalle | lote, serie, vencimiento y estado forman la clave cuando la política lo exige | ampliar; IN-D03 |
| IN-O10 | ubicaciones se guardan como texto ligado al artículo | ubicación maestra estable, independiente del artículo, dentro de un depósito | corregir; IN-D01 |
| IN-O11 | depósito se relaciona con sucursal y dirección | depósito propio de inventario; cualquier sede externa se referencia solo por contrato | separar; IN-D01 |
| IN-O12 | reserva tiene solo activo/inactivo | ciclo explícito, cantidades original/consumida/liberada y expiración idempotente | ampliar; IN-D07 |
| IN-O13 | se impide stock negativo en la ruta principal | validar saldo físico y disponible dentro de la misma transacción y bajo concurrencia | conservar y endurecer; IN-D06 |
| IN-O14 | cantidad y cantidad base conviven sin snapshot claro del factor | resolver conversión pública y guardar unidad, factor, cantidad base y versión usada | corregir; IN-D05 |
| IN-O15 | costo, moneda y proveedor están en el movimiento | inventario inicial administra cantidades; valoración y contabilidad son externas | separar; IN-D08 |
| IN-O16 | la cabecera conoce compra, remisión, pedido, cliente, proveedor y taller | referencia de origen neutral, sin JPA ni DTO de otros plugins | corregir; IN-D04/IN-D10 |
| IN-O17 | números se generan mediante `MAX + 1` | identidad opaca y secuencia transaccional; idempotencia separada del número visible | corregir |
| IN-O18 | catálogo decide `indManejaStock` | inscripción local de productos del catálogo como inventariables | separar; IN-D02 |
| IN-O19 | permisos dependen de pantallas/forma | permisos por capacidad y autorización nuevamente en aplicación | corregir |
| IN-O20 | una pantalla de control de inventario está incompleta/inconsistente | nuevos recorridos orientados a tarea, responsive y probados; no portar la vista | rechazar portación |

## Deudas que no deben heredarse

- El saldo mutable no explica por sí solo cómo se llegó a una cantidad.
- Movimientos contabilizados pueden modificarse o eliminarse mediante el EJB.
- Un inventario especial sustituye el saldo, perdiendo una corrección explícita.
- La clave de existencia no incluye depósito real, ubicación, lote, serie,
  vencimiento o condición.
- Lote y vencimiento se registran, pero no se concilian contra saldos separados.
- La reserva no documenta fuente, expiración, consumo parcial ni liberación.
- El generador `MAX + 1` no es seguro bajo concurrencia.
- Cabecera y detalle tienen relaciones JPA directas hacia compras, remisiones,
  ventas, personas, proveedores, tesorería, producción y taller.
- El motivo mezcla clasificación funcional con el signo técnico del movimiento.
- Unidad y multiplicador global no bastan para conversiones específicas por ítem.
- Existen cantidades con escala 2 y costos con escalas diferentes sin política
  de precisión única.
- La transferencia se materializa como filas independientes sin invariante
  visible que garantice atomicidad o igualdad entre origen y destino.
- La pantalla de inventario observada reutiliza beans y campos ajenos, y la opción
  de ubicaciones encontrada está deshabilitada.
- Los permisos del módulo mezclan stock, catálogo, compras, documentos,
  vehículos, rutas y reportes.
- Consultas nativas y referencias a IDs de tablas ajenas no pueden convertirse en
  contratos del nuevo ERP.

## Frontera candidata del plugin

`inventory` sería propietario de:

- depósitos, ubicaciones, estado operativo y ubicación general controlada;
- inscripción de productos del catálogo como conceptos inventariables;
- políticas de lote, serie, vencimiento y condición de stock;
- libro inmutable de movimientos y proyecciones de saldo;
- cantidades físicas, reservadas y disponibles;
- entradas, salidas, transferencias, ajustes y reversiones;
- motivos controlados y referencias neutrales de origen;
- reservas, consumo, liberación y expiración;
- conteos físicos, bloqueo del alcance, diferencias y ajustes de cierre;
- autorización, versión optimista, idempotencia y auditoría funcional;
- consultas y comandos públicos mínimos para consumidores autorizados.

Quedan fuera:

| Responsabilidad | Propietario futuro |
|---|---|
| identidad, nombre, tipo, unidad base y conversiones del ítem | `commercial_catalog` por API pública |
| orden de compra, recepción contra proveedor y costo de compra | `purchasing` |
| pedido, promesa comercial y documento de venta | `sales` y documentos comerciales |
| remisión, expedición, vehículo, ruta y entrega | `logistics` |
| fórmulas, consumo de componentes y producción terminada | `manufacturing` |
| costo promedio, FIFO, valoración y asientos | `costing`/`accounting` futuros |
| mínimos, máximos y propuestas de reposición | planificación/reposición futura |
| representación fiscal y SIFEN | plugin fiscal/documental correspondiente |
| sede o sucursal corporativa | contrato público futuro; inventario no lee sus tablas |

No existirán relaciones JPA, SQL directo ni claves foráneas hacia esquemas de
otros plugins.

## Actores y permisos candidatos

| Permiso | Capacidad |
|---|---|
| `inventory:read` | consultar depósitos, ubicaciones, saldos y movimientos |
| `inventory:manage_storage` | crear, modificar e inactivar depósitos/ubicaciones |
| `inventory:manage_items` | inscribir y configurar conceptos inventariables |
| `inventory:post_movements` | contabilizar entradas, salidas y transferencias manuales |
| `inventory:manage_reservations` | crear, consumir, liberar y expirar reservas |
| `inventory:count` | iniciar y capturar conteos físicos |
| `inventory:adjust` | aprobar ajustes, cerrar conteos y revertir movimientos |

La UI ocultará acciones no autorizadas, pero cada consulta y mutación validará en
el servidor empresa confiable, plugin activo, permiso, versión e idempotencia.

## Casos de uso neutrales candidatos

| ID | Caso de uso | Resultado esperado |
|---|---|---|
| IN-UC-01 | administrar depósitos | depósito aislado por empresa, con código único y ciclo activo/inactivo |
| IN-UC-02 | administrar ubicaciones | posición válida y única dentro del depósito, incluida la ubicación general |
| IN-UC-03 | inscribir concepto inventariable | referencia pública válida de un `PRODUCT`, unidad base y políticas confirmadas |
| IN-UC-04 | consultar disponibilidad | físico, reservado y disponible por clave y agregados autorizados |
| IN-UC-05 | consultar libro de movimientos | página ordenada y filtrable con origen, motivo, actor y saldo resultante |
| IN-UC-06 | registrar entrada | movimiento inmutable que incrementa saldo y conserva snapshot de catálogo/unidad |
| IN-UC-07 | registrar salida | movimiento que descuenta sin producir físico o disponible negativo |
| IN-UC-08 | transferir | débito y crédito atómicos, mismas dimensiones y cantidad base |
| IN-UC-09 | ajustar | diferencia explícita, motivo obligatorio y permiso reforzado |
| IN-UC-10 | revertir | movimiento compensatorio enlazado; el original permanece intacto |
| IN-UC-11 | crear reserva | asignación que reduce disponible, no físico, con fuente y vencimiento |
| IN-UC-12 | consumir reserva | salida referenciada que reduce físico y reservado de forma consistente |
| IN-UC-13 | liberar/expirar reserva | cantidad vuelve a disponible una sola vez, conservando causa y actor |
| IN-UC-14 | iniciar conteo | alcance válido queda temporalmente cerrado a movimientos ordinarios |
| IN-UC-15 | capturar y revisar conteo | cantidad física se compara con saldo teórico sin modificarlo todavía |
| IN-UC-16 | cerrar/cancelar conteo | ajuste inmutable o cancelación auditada y liberación del alcance |
| IN-UC-17 | resolver comando idempotente | un reintento retorna el mismo resultado sin duplicar efectos |
| IN-UC-18 | inactivar depósito/ubicación/concepto | impide nuevas operaciones si quedan saldos o compromisos incompatibles |

## Invariantes candidatas

| ID | Invariante |
|---|---|
| IN-I01 | toda entidad operativa pertenece a una empresa no nula |
| IN-I02 | depósito y ubicación tienen IDs opacos y códigos únicos normalizados dentro de su alcance |
| IN-I03 | todo saldo y movimiento usa depósito y ubicación; nunca una ubicación nula |
| IN-I04 | una ubicación pertenece a un solo depósito y a la misma empresa |
| IN-I05 | solo un `PRODUCT` inscrito puede recibir movimientos; un `SERVICE` se rechaza |
| IN-I06 | el saldo físico es la suma de movimientos contabilizados para la misma clave |
| IN-I07 | reservado es la suma pendiente de asignaciones activas para la misma clave |
| IN-I08 | disponible es `físico - reservado` y nunca es negativo |
| IN-I09 | físico nunca es negativo en el primer contrato del plugin |
| IN-I10 | un movimiento contabilizado no se edita ni se elimina |
| IN-I11 | una reversión referencia un único movimiento y no puede aplicarse dos veces |
| IN-I12 | una transferencia confirma origen y destino dentro de una transacción y por igual cantidad base |
| IN-I13 | todo comando externo tiene clave idempotente única por empresa y fuente |
| IN-I14 | toda cantidad se normaliza a la unidad base con factor positivo y snapshot de versión |
| IN-I15 | lote, serie, vencimiento y condición cumplen la política vigente del concepto |
| IN-I16 | una serie identifica una unidad física y no puede estar en dos ubicaciones activas |
| IN-I17 | una reserva no puede consumir o liberar más que su cantidad pendiente |
| IN-I18 | expirar, liberar o consumir la misma cantidad más de una vez no produce efecto duplicado |
| IN-I19 | un conteo cerrado no se reabre; toda corrección posterior crea otro proceso/movimiento |
| IN-I20 | el cierre de conteo genera ajustes explícitos y conserva teórico, contado y diferencia |
| IN-I21 | una mutación requiere empresa confiable, permiso, plugin activo y versión esperada |
| IN-I22 | contratos públicos no exponen entidades, tablas, DTO internos ni detalles JPA |

## Snapshot de catálogo que debe conservar inventario

Cada renglón contabilizado preservará el dato utilizado en ese momento, aunque el
catálogo cambie después:

| Dato | Motivo |
|---|---|
| `CatalogItemId` | identidad pública estable |
| código y nombre visible | explicación histórica y soporte |
| tipo y versión del ítem | demostrar que era un producto válido al contabilizar |
| unidad presentada | reproducir la entrada del usuario o consumidor |
| unidad base | clave de cantidad normalizada |
| factor aplicado | reproducir la conversión sin consultar el estado actual |
| cantidad presentada y base | conciliación y auditoría |

Categoría, marca, precio, impuestos, proveedor y cuentas contables no forman parte
del snapshot inicial de inventario.

## Decisiones para confirmación de producto

### IN-D01 - Depósito y ubicación

Alternativas:

1. usar únicamente una sede/sucursal como dimensión de saldo;
2. depósito obligatorio y ubicación opcional;
3. depósito y ubicación obligatorios, con una ubicación de sistema `GENERAL` para
   operación sin detalle físico.

**Recomendación:** alternativa 3. Cada depósito pertenece a una empresa y posee
ubicaciones de un nivel con código, nombre, tipo y estado. `GENERAL` se crea con el
depósito, no puede borrarse y evita nulos. Una futura sede se enlazará por ID
público opcional cuando exista ese contrato, nunca por JPA.

Impacto: toda clave de saldo y todo movimiento incluyen ambos IDs; la UI simple
preselecciona `GENERAL`, mientras empresas con organización física pueden usar
ubicaciones específicas.

Estado: **aceptada sin cambios el 2026-07-31**.

### IN-D02 - Conceptos habilitados para inventario

Alternativas:

1. todo `PRODUCT` activo del catálogo maneja stock automáticamente;
2. agregar un alcance `INVENTORY` al contrato del catálogo;
3. inscribir explícitamente en `inventory` un `PRODUCT` resuelto mediante
   `CatalogItemDirectory`.

**Recomendación:** alternativa 3. Compra/venta siguen siendo alcances comerciales;
inventario posee su propia inscripción y políticas. Se rechaza `SERVICE`. La
inactivación del catálogo impide nuevas operaciones ordinarias, pero no borra
saldos ni historia y permite correcciones autorizadas.

Impacto: `inventory` depende únicamente de `commercial-catalog-api`, conserva el
ID y snapshot requerido y no obliga a modificar las tablas privadas del catálogo.

Estado: **aceptada sin cambios el 2026-07-31**.

### IN-D03 - Lote, serie, vencimiento y condición

Alternativas:

1. excluir todas las dimensiones del primer modelo;
2. exigir lote, serie y vencimiento para todo producto;
3. política por concepto: seguimiento `NONE`, `LOT` o `SERIAL`, vencimiento
   `NONE`, `OPTIONAL` o `REQUIRED`, y condición obligatoria controlada.

**Recomendación:** alternativa 3. La condición inicial será `AVAILABLE`,
`QUARANTINED` o `DAMAGED`; reservado no es condición. Una serie representa una
unidad, lote/serie se normalizan y la transferencia conserva dimensiones. La UI
solo solicita los campos exigidos por la política.

Impacto: estas dimensiones integran la clave del saldo desde V1 y evitan una
migración estructural destructiva posterior.

Estado: **aceptada sin cambios el 2026-07-31**.

### IN-D04 - Tipos, motivo, origen e idempotencia

Alternativas:

1. copiar motivos E/S y permitir editar el comprobante;
2. guardar únicamente deltas sin tipo ni fuente;
3. libro inmutable con `RECEIPT`, `ISSUE`, `TRANSFER`, `ADJUSTMENT` y `REVERSAL`,
   motivo obligatorio, referencia neutral e idempotencia.

**Recomendación:** alternativa 3. Una referencia de origen contiene sistema,
tipo e ID opacos; no importa clases externas. Todo comando posee `commandId`; un
consumidor externo además envía una clave única por empresa/fuente. La corrección
se hace con reversión o nuevo ajuste, nunca editando/borrando.

Impacto: permite reintentos seguros, auditoría y futura integración con compras,
ventas y logística sin acoplamiento.

Estado: **aceptada sin cambios el 2026-07-31**.

### IN-D05 - Unidad, conversión y precisión

Alternativas:

1. aceptar únicamente la unidad base;
2. convertir con un multiplicador global dentro de inventario;
3. aceptar unidad presentada, resolver `CatalogUnitConversions` y persistir el
   snapshot de conversión.

**Recomendación:** alternativa 3. Los saldos usan unidad base; cantidades se
guardan como decimal de hasta 6 posiciones y factores de hasta 12. Valores que
excedan la precisión se rechazan en el límite, no se redondean silenciosamente;
redondeo visual no cambia el dato. Series requieren cantidad entera unitaria.

Impacto: exige que el catálogo responda una conversión vigente, pero el movimiento
continúa reproducible si esa conversión cambia después.

Estado: **aceptada sin cambios el 2026-07-31**.

### IN-D06 - Stock negativo

Alternativas:

1. permitir negativo globalmente;
2. configurar permiso por empresa o depósito;
3. prohibir saldo físico y disponible negativos en V1.

**Recomendación:** alternativa 3. Salidas, transferencias, reservas y ajustes se
validan bajo bloqueo/concurrencia dentro de la misma transacción. La corrección de
datos no evade la invariante. Una excepción futura requerirá caso real, nueva
decisión y versión del contrato.

Impacto: comportamiento predecible y conciliable; operaciones deben registrar la
entrada faltante antes de consumirla.

Estado: **aceptada sin cambios el 2026-07-31**.

### IN-D07 - Ciclo de reservas

Alternativas:

1. solo una cantidad bloqueada en el saldo;
2. reserva activa/inactiva sin vencimiento;
3. reserva explícita con asignaciones y estados `ACTIVE`,
   `PARTIALLY_CONSUMED`, `CONSUMED`, `RELEASED` y `EXPIRED`.

**Recomendación:** alternativa 3. Fuente e instante de expiración son obligatorios.
La reserva asigna claves de stock concretas, reduce disponible y permite consumo o
liberación parcial. La expiración es un comando idempotente. El tiempo de vida por
defecto será configuración empresarial, pero cada reserva conserva el valor
efectivo recibido.

Impacto: ventas futuras podrá prometer sin leer tablas y los reintentos no liberan
ni consumen dos veces.

Estado: **aceptada sin cambios el 2026-07-31**.

### IN-D08 - Costos y valoración

Alternativas:

1. copiar costo último/promedio y moneda dentro de inventario;
2. guardar un costo informativo sin método contable;
3. V1 administra cantidades, no calcula ni persiste valoración monetaria.

**Recomendación:** alternativa 3. Costos de compra, promedio, FIFO, moneda,
variaciones y asientos pertenecen a futuros plugins de compras/costeo/contabilidad.
Estos consumidores usarán contratos o eventos cuando existan; no habrá campos
monetarios “por si acaso”.

Impacto: el primer plugin no muestra valor de inventario; reduce acoplamiento y
evita que un dato informativo se convierta en fuente contable accidental.

Estado: **aceptada sin cambios el 2026-07-31**.

### IN-D09 - Conteos, ajustes y correcciones

Alternativas:

1. sobrescribir el saldo con la cantidad contada;
2. permitir conteo mientras continúan todos los movimientos;
3. proceso `DRAFT` → `COUNTING` → `REVIEW` → `POSTED`, con cierre temporal del
   alcance, más `CANCELLED`.

**Recomendación:** alternativa 3. Al iniciar, el alcance de claves queda bloqueado
para movimientos ordinarios; se captura conteo ciego y se revisa diferencia. El
cierre genera ajustes inmutables y libera el alcance. Un conteo publicado no se
reabre; se corrige con otro conteo o movimiento autorizado.

Impacto: la pausa queda limitada a depósito/ubicación/ítems seleccionados y la
diferencia es reproducible sin reemplazo silencioso del saldo.

Estado: **aceptada sin cambios el 2026-07-31**.

### IN-D10 - Contratos, snapshots y eventos

Alternativas:

1. exponer repositorios/entidades de inventario;
2. crear desde ahora numerosos eventos y outbox sin consumidores;
3. `inventory-api` Java puro con consultas/comandos mínimos; documentar eventos
   futuros y materializarlos cuando exista el primer consumidor real.

**Recomendación:** alternativa 3. El API inicial publicará IDs opacos,
disponibilidad, reservas y comandos de movimiento autorizados. Internamente se
conserva el snapshot de catálogo definido arriba. Compras, ventas y logística no
leen tablas. Los eventos de movimiento/reserva se versionarán e incorporarán al
outbox únicamente junto con productor y consumidor aprobados, conforme al ADR de
eventos.

Impacto: mantiene un contrato pequeño hoy y evita consolidar mensajes hipotéticos,
sin cerrar la evolución asíncrona futura.

Estado: **aceptada sin cambios el 2026-07-31**.

## Matriz de pruebas propuesta para las historias de implementación

| Nivel | Cobertura mínima |
|---|---|
| dominio | cantidades, claves, políticas, estados, transferencias, reservas, conteos y reversiones |
| contratos | IDs, validaciones, compatibilidad y ausencia de Jakarta/JPA en `inventory-api` |
| ArchUnit | ningún acceso a internos/JPA/tablas de catálogo y kernel sin dependencia de inventario |
| PostgreSQL | restricciones, aislamiento por empresa, libro/proyección y migraciones inmutables |
| JPA/JTA | movimiento y saldo atómicos; transferencia de dos extremos; rollback completo |
| concurrencia | dos salidas/reservas simultáneas no producen negativo ni sobre-reserva |
| idempotencia | reintentos de movimiento, reversión, consumo y expiración producen un solo efecto |
| seguridad | empresa ajena, plugin inactivo y permiso ausente son rechazados en servidor |
| composición | WAR y migrador con y sin `inventory`, dependencia de catálogo validada |
| Docker | migración, arranque, health, recreación y persistencia en volumen |
| Playwright | tareas y errores en 375, 720 y 1280 px, teclado y sin overflow normal |
| demo | datos ficticios, saldos, entrada/salida, transferencia, reserva/liberación y denegación |
| instalador Windows | preflight, consentimiento/UAC, instalación limpia, actualización, health y persistencia |

No se ejecuta esta matriz en `J11-S8-01` porque no se modificó código ni runtime.

## Riesgos y mitigaciones

| Riesgo | Mitigación propuesta |
|---|---|
| saldo desincronizado del libro | transacción única, restricciones y prueba de conciliación |
| doble contabilización por reintento | clave idempotente única y resultado persistido |
| carrera de dos salidas | bloqueo optimista/pesimista acotado y condición de saldo en transacción |
| explosión de claves por lote/serie | índices por empresa/ítem/depósito y políticas solo cuando aplican |
| conteo operativo demasiado amplio | cierre por alcance seleccionable, no por empresa completa |
| catálogo inactivo con saldo | conservar historia; bloquear operación ordinaria y permitir corrección autorizada |
| API pública demasiado grande | consultas/comandos por capacidad, DTO inmutable y versionado |
| acoplamiento prematuro a compras/ventas | referencia neutral e interfaces públicas, sin clases ni tablas externas |
| costo usado como contabilidad | excluir moneda/costo de V1 y documentar propietario futuro |
| migración sucia del legado | staging, mapeo, conciliación y reporte; nunca modificar el origen |

## Migración futura

Sprint 8 no migra datos del legado. Un proyecto posterior deberá:

- mapear empresa/sucursal/depósito a depósitos y ubicaciones aprobadas;
- conciliar `StwExistenciaArt` contra movimientos disponibles antes de importar;
- explicar diferencias o cargarlas como saldos iniciales con origen auditado;
- decidir tratamiento de `cantBloqueo` sin reserva trazable;
- normalizar unidad base, cantidad, factor, lotes, series y vencimientos;
- separar filas de movimientos que pertenecen a compras, remisiones o producción;
- detectar duplicados y transferencias incompletas;
- mapear IDs de artículo mediante el registro `legacy_id` → `CatalogItemId`;
- no importar costos como valoración contable sin proyecto específico;
- producir conteos, errores, checksum y conciliación de entrada/salida;
- ejecutar fuera del esquema legado y sin escribir en el origen.

## Condición para avanzar

El responsable de producto confirmó IN-D01 a IN-D10 sin cambios el 2026-07-31.
Esto autoriza `J11-S8-02` para modelar dominio y contratos públicos. JPA,
migraciones, aplicación, UI y composición permanecen reservados a sus historias
posteriores.
