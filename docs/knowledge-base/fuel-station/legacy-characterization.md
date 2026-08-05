# Estaciones de servicio: caracterización y factibilidad del plugin

- Fecha de análisis: 2026-08-02
- Estado: decisiones incorporadas al roadmap; implementación no iniciada
- Fuente principal: `C:\cosme\multienvios\miaterra`, commit
  `55a56963f00329edd2da57b53a1a94da129cc819`
- Fuente complementaria: `C:\cosme\felsina\ingeniolafelsina`, commit
  `412b3cd978757b1b8a389f2007060a90f5c7322b`
- Uso de las fuentes: solo lectura; no se copiaron clases, entidades ni XHTML

## Objetivo

Determinar si la operación de una estación de servicio de combustibles constituye
un dominio propio y cómo debe integrarse con catálogo, inventario, compras, ventas,
POS, documentos, tesorería y SIFEN sin duplicar sus fuentes de verdad.

## Evidencia encontrada en Multienvíos

El legado contiene una capacidad de **consumo de combustible para flota**, no una
estación de servicio completa:

- catálogo simple de tipo de combustible;
- grupos de artículos para lubricantes y combustibles;
- registro de salida vinculado con empresa, sucursal, motivo, operador, sector,
  entregado por, retirado por, vehículo y solicitud;
- referencia opcional a compra y remisión;
- detalle de artículos y cantidades;
- kilometraje y horas de uso para analizar consumo;
- historial de cargas asociado al vehículo.

`FlwRegistroCombustibleControlador` hereda un controlador genérico de
entradas/salidas de stock y completa numerosas entidades de otros dominios. La UI
consulta personas, vehículos, solicitudes, compras, motivos y artículos desde un
contenedor global. El comportamiento confirma la necesidad de registrar despachos
internos, pero también evidencia el acoplamiento que Logixone debe evitar.

## Evidencia encontrada en Ingenio La Felsina

La fuente conserva un catálogo `StwTipoCombustible` y referencias del tipo usado
por vehículos. No se encontró un módulo operativo de estación con tanques,
surtidores o turnos. La fuente sirve para caracterizar compatibilidad de vehículos
y consumo, no para copiar una solución de playa.

## Capacidades no cubiertas por los legados

No se observaron agregados completos para:

- estación, isla o playa de expendio;
- tanques, compartimientos, capacidad segura y producto almacenado;
- máquinas expendedoras, surtidores, picos/mangueras y números de serie;
- lecturas totalizadoras de apertura/cierre;
- turnos de playa y asignación de operador;
- recepción de cisterna, medición antes/después y diferencias;
- medición manual o automática de tanque, agua y temperatura;
- transacciones de despacho importadas desde controlador de surtidor;
- conciliación de inventario húmedo contra ventas y stock contable;
- cambios de precio efectivos y consistencia volumen × precio;
- verificaciones metrológicas, precintos, habilitaciones e incidentes;
- integración segura con hardware o protocolos de fabricantes.

Estas ausencias impiden considerar al legado una implementación de estación de
servicio. El nuevo plugin se diseñará por requisitos y pruebas de caracterización.

## Referencias oficiales verificadas

La revisión del 2026-08-02 encontró obligaciones que afectan qué evidencia conviene
conservar, sin convertir al ERP en autoridad certificadora:

- el [MIC solicita para habilitación](https://www.mic.gov.py/habilitacion-de-estaciones-de-servicios/)
  descripción de tanques con capacidad/producto, máquinas de expendio, picos,
  seguridad, DIA, verificación final y georreferenciación;
- el [procedimiento INTN MLE-PT-02 Rev.05](https://intn.gov.py/wp-content/uploads/2026/03/MLE-PT-02-Rev.05-2026-03-04-Procedimiento-Tecnico-Verificacion-subs-de-surtidores-de-combustibles.pdf),
  fechado 2026-03-04, verifica máquinas expendedoras, identificación/serie,
  producto, correspondencia entre volumen y precio, certificados, marcas y
  precintos; un pico reprobado puede quedar inhabilitado;
- MADES informó que la [PNA 40 002 19 es obligatoria](https://www.mades.gov.py/2019/08/16/mades-adopta-normas-de-intn-como-requisitos-obligatorios-para-instalacion-y-operacion-de-estaciones-de-servicios-y-afines/)
  para instalación/operación y destaca tanques de doble pared, monitoreo continuo
  y prevención de filtraciones;
- MIC/SEDECO comunicaron que la
  [Resolución SDCU 851/2025](https://www.mic.gov.py/socializan-resolucion-que-obliga-a-las-estaciones-de-servicio-a-dar-informacion-clara-y-real-sobre-combustibles/)
  exige informar claramente tipos y precios ofrecidos.

Las normas pueden cambiar. La implementación deberá verificar fuentes oficiales,
versiones, vigencia y checksums aplicables. Guardar un certificado en el sistema no
demuestra por sí mismo cumplimiento ni habilita equipos.

## Frontera propuesta

### Propiedad de `fuel_station`

- identidad y configuración operativa de la estación;
- tanques, surtidores, picos y sus estados operativos;
- lecturas totalizadoras y mediciones de tanque;
- recepciones físicas y despachos de combustible;
- turnos de playa y asignación del actor operativo;
- conciliación de inventario húmedo y diferencias;
- evidencia de inspección, verificación, precintos e incidentes;
- adaptadores públicos para importar datos de controladores de playa.

### Propiedad que permanece fuera

| Dato/capacidad | Propietario |
|---|---|
| combustible como producto, unidad, impuesto y lista de precio | `commercial_catalog` |
| stock contable, depósito y movimiento valorizable | `inventory` |
| orden/recepción comercial del proveedor | `purchasing` |
| cliente, vehículo de tercero o cuenta comercial | `business_partners` o plugin futuro correspondiente |
| carrito, cobro rápido y sesión de caja | `point_of_sale` |
| factura/ticket canónico y numeración | `commercial_documents` |
| firma, CDC, transmisión y respuesta fiscal | `sifen` |
| caja, medios de pago y conciliación financiera | `treasury` |
| deuda, cupo y cobranza de clientes corporativos | `accounts_receivable` |
| costo y asiento | plugin de costo futuro o `accounting`, mediante eventos |
| empleado | `human_resources`; `fuel_station` usa `ActorId`/referencia pública |

El volumen operativo de tanque y surtidor no sustituye el stock contable. El cierre
calcula una conciliación explicable entre apertura, recepciones, despachos,
ajustes/ensayos y cierre medido; después publica hechos para que `inventory`
registre movimientos idempotentes.

## Decisiones

| Código | Decisión |
|---|---|
| FS-D01 | Crear `fuel_station` como plugin funcional reutilizable después de `point_of_sale`; ADR-0033 desplazó su orden de 11 a 12 y ADR-0034 lo desplaza actualmente a 13. |
| FS-D02 | El plugin será dueño de playa, tanques, surtidores/picos, mediciones, turnos, recepciones, despachos y conciliación. |
| FS-D03 | Combustibles, impuestos y precios continúan en `commercial_catalog`; se conservan IDs y snapshots efectivos. |
| FS-D04 | `inventory` mantiene stock contable; `fuel_station` mantiene inventario húmedo operativo y publica diferencias/movimientos por contrato. |
| FS-D05 | POS, documentos, tesorería y cuentas por cobrar mantienen venta, factura, cobro y crédito. |
| FS-D06 | La primera versión admite carga manual e importación de archivos/API; no controla remotamente surtidores. |
| FS-D07 | Protocolos de fabricantes se implementarán como adaptadores técnicos versionados, nunca dentro del dominio ni del kernel. |
| FS-D08 | Habilitaciones, verificaciones y precintos se registran como evidencia versionada; el ERP no declara cumplimiento automáticamente. |
| FS-D09 | El primer alcance cubre combustibles líquidos. GLP, GNV, hidrógeno y carga eléctrica requieren decisiones posteriores. |
| FS-D10 | Un posible `fuel_station_paraguay` solo se evaluará si reportes/reglas oficiales ejecutables justifican un adaptador nacional. |

## Modelo conceptual mínimo

- `service_station`: código empresarial, nombre, estado, ubicación pública y zona
  horaria;
- `forecourt`: playa/isla operativa dentro de una estación;
- `fuel_tank`: capacidad, producto público, estado y límites operativos;
- `fuel_dispenser` y `fuel_nozzle`: máquina, pico, serie, producto y estado;
- `forecourt_shift`: apertura, responsables, estado y cierre;
- `dispenser_totalizer_reading`: lectura monotónica, origen e instante;
- `tank_measurement`: volumen, agua, temperatura, método e instante;
- `fuel_delivery`: recepción de cisterna y mediciones por tanque;
- `fuel_dispense`: volumen, precio efectivo, importe, pico, turno y referencia POS;
- `wet_stock_reconciliation`: ecuación, tolerancias, diferencias y aprobación;
- `metrology_verification`: organismo, instrumento, versión, resultado, vigencia,
  certificados y precintos como referencias de evidencia;
- `forecourt_incident`: fuga, derrame, equipo fuera de servicio u otra clase
  cerrada, sin reemplazar protocolos externos de emergencia.

Las mediciones e importes usan decimales explícitos y unidad identificada. No se
usa `double`, EAV ni JSON como única fuente operativa.

## Invariantes

1. tanque y pico pertenecen siempre a la misma empresa/estación de su operación;
2. un pico activo dispensa solamente el producto configurado y vigente;
3. lecturas totalizadoras no retroceden salvo evento correctivo autorizado;
4. cada despacho importado es idempotente por dispositivo, secuencia y fecha;
5. un pico reprobado o fuera de servicio no acepta nuevos despachos operativos;
6. cambio de precio conserva vigencia y snapshot usado en cada despacho;
7. recepción, despacho, ensayo metrológico y ajuste tienen clases separadas;
8. cierre de turno congela lecturas y requiere explicar diferencias fuera de
   tolerancia;
9. ningún evento escribe directamente tablas de catálogo, inventario, POS o
   tesorería;
10. desactivar el plugin conserva mediciones, conciliaciones y evidencia.

## Pruebas futuras

- totalizador concurrente, duplicado, fuera de orden y reinicio del importador;
- recepción distribuida entre varios tanques y conciliación posterior;
- cambio de precio durante un turno sin reescribir despachos previos;
- pico inhabilitado, certificado vencido y permiso denegado;
- cierre con diferencias dentro/fuera de tolerancia;
- outbox/inbox con `inventory` y POS, incluidos duplicados y cuarentena;
- caída del adaptador de hardware sin perder el último cursor confirmado;
- aislamiento empresarial y ausencia de JPA/SQL cruzado;
- UI en 375, 720 y 1280 px sin depender de una tabla ancha;
- trazabilidad desde despacho hasta venta, documento y cobro solo por IDs públicos.

## Límites

El análisis no implementa módulos, migraciones, hardware ni cumplimiento. No
autoriza iniciar `fuel_station` antes de cerrar Sprint 8 y completar en orden los
plugins 4 a 12.
