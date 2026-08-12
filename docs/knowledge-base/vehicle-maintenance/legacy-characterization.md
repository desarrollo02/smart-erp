# Taller y mantenimiento vehicular — caracterización del legado

- Estado: análisis documental y estático; no autoriza copia ni implementación
- Fecha de revisión: 2026-08-12
- Fuente preferente: `C:\cosme\mega\miaterra\fuente\tag`
- Revisión observada: `6f6dda310`
- Rama observada: `17661_logixone_master-correccines_para_el_viernes_06_08_2026`
- Acceso: solo lectura
- Decisión destino: [ADR-0046](../../adr/0046-familia-mantenimiento-flota-taller-automotriz.md)

## Objetivo

Convertir el comportamiento vigente del Taller legado en requisitos, límites y
pruebas de caracterización para dos plugins futuros de Smart ERP. Este documento
no convierte clases, páginas o tablas legadas en contratos del producto nuevo.

## Fuentes revisadas

- `docs/taller/TawManualUsuario.md`, versión 1.0 del 2026-08-10;
- `docs/taller/taw-orden-trabajo.md`;
- `docs/taller/taw-control-aceite-gasoil.md`;
- `src/main/webapp/WEB-INF/menuTaller.xhtml`;
- controladores bajo `py.com.ping.taller.cdi`;
- entidades bajo `py.com.ping.taller.jpa` y las entidades de OT bajo
  `py.com.ping.stock.jpa`;
- caso `GenerarSolicitudTallerDesdePreventaUseCase` bajo
  `py.com.ping.erp.taller.solicitud`;
- vista y migraciones `vw_taller_operacion_activos`,
  `taw_regla_mantenimiento` y `taw_orden_trabajo_items_valores`.

La fuente declara 22 pantallas activas agrupadas en Definiciones, Movimientos y
Consultas. La inspección fue estática; no se ejecutó el legado ni se modificó su
base de datos.

## Capacidades observadas

### Preparación

- máquinas/vehículos, grupos y marcas;
- repuestos e insumos;
- departamentos, sectores, tipos y funcionarios;
- actividades con costo/precio;
- ítems de verificación booleanos, numéricos, rangos, texto u opciones;
- tipos de orden que componen actividades y checklists;
- seguros y referencias auxiliares.

### Solicitud

La solicitud registra número, fecha/hora, tipo de orden, moneda, vehículo o
maquinaria, departamento, solicitante, observación, estado, adjuntos, respuestas
de checklist y servicios/repuestos estimados. Puede vincular preventa o contrato
y ofrece alta rápida de vehículo.

### Orden de trabajo

La OT permite planificar, asignar, iniciar, pausar, continuar, finalizar y
consultar. Registra sector, vehículo/máquina, cliente, solicitud, preventa,
contrato, personal, checklist, servicios/repuestos, costos, descuentos,
movimientos de stock, compras, adjuntos, historial y reglas de mantenimiento
futuro.

Los estados observados son `PE`, `CU`, `PA`, `FI` y valores históricos `P`, `R`,
`C`. El legado bloquea cambios sensibles cuando la orden está finalizada,
realizada o cancelada, pero conserva varios códigos equivalentes que deben
normalizarse durante una migración.

### Programación preventiva

`taw_regla_mantenimiento` asocia una OT y un tipo de orden con regla `KM` o
`FECHA`, intervalo, fecha/kilometraje proyectado y marca de generación. El diseño
nuevo debe versionar el plan y deduplicar por ciclo; una marca booleana aislada
no alcanza para reintentos, múltiples vehículos o cambios de plan.

### Ejecución y seguimiento

- OT móvil y acceso externo mediante token;
- personal y cronómetro por cambios de estado;
- operaciones activas de Taller o Remisión, con autoactualización y vencimiento;
- modo TV de solo lectura;
- dashboard con cantidad, estado, costo, duración, repuestos y rankings por
  vehículo, técnico, tipo y repuesto.

### Stock, compra y costo

Una línea de OT puede crear una salida de stock o una orden de compra. El legado
distingue costo interno bruto de total comercial con descuento y reutiliza una
misma OT tanto para mantenimiento propio como para trabajo vendido. El nuevo
diseño debe separar:

- necesidad/consumo técnico en F1;
- movimiento y costo de stock en Inventario;
- compra o servicio tercerizado en Compras;
- presupuesto, precio y descuento en Ventas;
- factura/notas en Documentos Comerciales.

### Combustible y aceite

El menú de Taller incluye cargas de combustible/aceite, resúmenes de consumo y
vales originados en Transporte. La misma operación calcula diferencias de
kilometraje, litros por distancia y costos. Esta capacidad es evidencia útil,
pero no pertenece al futuro F1: combustible permanece en Logística, Telemetría o
`fuel_station`; sólo un lubricante consumido durante una OT usa el flujo de
repuesto de Inventario.

## Tablas y acoplamientos observados

| Elemento legado | Uso observado | Propietario futuro |
|---|---|---|
| `srw_orden_trabajo` | cabecera, estado, cliente, vehículo, proveedor, sector y referencias comerciales | F1 conserva OT técnica; IDs externos por contrato |
| `srw_orden_trabajo_servicio` | servicios/repuestos, cantidad y costo/precio | F1 conserva tarea/requerimiento; Catálogo e Inventario conservan ítem/stock |
| `srw_orden_trabajo_evento` | cambios de estado y cronómetro | F1, historia append-only |
| `srw_orden_trabajo_archivos` | evidencia adjunta | F1, con política de seguridad/retención |
| `stw_operadores_ot` | personal asignado | F1 usa `ActorId`; RR. HH. conserva empleado |
| `taw_items` | definiciones configurables de checklist | F1, tipo cerrado y versionado |
| `taw_tipo_orden_items` | composición de ítems por tipo de orden | F1, plantilla/versiones |
| `taw_orden_trabajo_items_valores` | respuestas de checklist | F1, resultado inmutable contextual |
| `taw_regla_mantenimiento` | proyección por kilómetros/fecha | F1, plan/versión/disparo idempotente |
| `taw_solicitudes_remisiones` | cruce solicitud/remisión | evento/ID público entre F1 y Logística |
| `vw_taller_operacion_activos` | unión operacional Taller/Remisión | proyección por eventos, sin vista SQL cruzada |

La entidad de OT mantiene relaciones JPA directas con empresa, sucursal, usuarios,
cliente, proveedor, tipo de orden, reclamo, pedido, contrato, sector, vehículo,
máquina, solicitudes, compras, movimientos de stock y personal. Es el principal
acoplamiento que no debe reproducirse.

## Reglas que deben conservarse como requisitos

1. Una solicitud identifica vehículo, solicitante, área y necesidad antes de
   convertirse en trabajo.
2. Una OT sólo inicia después de existir y cumplir los datos requeridos.
3. Pausar/continuar conserva intervalos de tiempo y actor responsable.
4. Una OT no se completa sin resolver requerimientos obligatorios, checklist y
   operaciones pendientes definidas por política.
5. El mismo técnico no se asigna dos veces a una OT.
6. Un repuesto no produce dos salidas por reintento.
7. Una salida requiere cantidad positiva, existencia suficiente y almacén
   autorizado; Inventario toma la decisión final.
8. Compra y consumo conservan correlación con la OT sin cambiar su propietario.
9. Una orden cerrada no se edita silenciosamente.
10. El historial conserva estado, instante, actor y comentario/motivo.
11. La programación preventiva no duplica la siguiente OT.
12. Un ranking es indicativo; cantidad, utilización, costo y duración deben
    interpretarse juntos.

## Comportamientos que no se trasladan

- asociaciones JPA y consultas SQL entre dominios;
- herencia de controladores de Stock, RR. HH. o Transporte;
- alta rápida que cree maestros externos sin contrato y permiso del propietario;
- plantillas HTML o JSON ejecutable para checklists;
- token permanente compartido por WhatsApp para editar una OT;
- códigos de estado duplicados o ambiguos;
- modificación directa de stock, compra, factura, vale o liquidación;
- combustible como submódulo de Taller;
- una sola cifra que mezcle costo técnico con precio/descuento comercial;
- generación preventiva basada únicamente en un booleano `generado`.

## Frontera propuesta

| Capacidad | Plugin propietario |
|---|---|
| vehículo, categoría y disponibilidad pública | `logistics` |
| dispositivo, posición, odómetro/horas observados | `vehicle_telemetry` |
| plan, solicitud, defecto, checklist y OT técnica | `fleet_maintenance` |
| recepción y autorización comercial del cliente | `automotive_workshop` |
| persona/organización propietaria o cliente | `business_partners` |
| artículo, servicio, unidad y precio maestro | `commercial_catalog` |
| reserva, salida, devolución y costo de stock | `inventory` |
| compra de repuesto o servicio externo | `purchasing` |
| presupuesto y pedido | `sales` |
| factura, notas y snapshots documentales | `commercial_documents` |
| pago y deuda | `treasury` / `accounts_receivable` |
| empleado | `human_resources` |
| coordinación configurable | `business_process_management` opcional |

## Pantallas candidatas

### F1

1. Dashboard de mantenimiento.
2. Historial y estado técnico por vehículo.
3. Planes e intervalos preventivos.
4. Solicitudes, fallas e inspecciones.
5. Directorio de órdenes de trabajo.
6. Ficha de OT con tareas, checklist, personal, repuestos y evidencia.
7. Ejecución móvil autenticada.
8. Agenda y puestos/sectores del taller.
9. Costos, indisponibilidad y reincidencias.

### F2

1. Recepción del vehículo del cliente.
2. Autorización de diagnóstico.
3. Seguimiento del presupuesto de Ventas.
4. Autorización de reparación y ampliaciones.
5. Estado/comunicación del servicio.
6. Entrega, aceptación y referencias documentales.

## Pruebas de caracterización propuestas

- crear solicitud completa e impedir empresa/vehículo ajenos;
- convertir una solicitud una sola vez pese a reintento;
- ejecutar el ciclo completo y rechazar transiciones inválidas;
- reconstruir tiempo activo a través de pausa/continuación/reinicio;
- impedir doble asignación de técnico;
- validar cada tipo de checklist y bloquear ítems obligatorios incompletos;
- reservar/consumir/devolver repuesto sin duplicar el movimiento;
- conservar OT recuperable cuando Inventario o Compras rechazan una operación;
- generar una sola OT preventiva por plan/versión/umbral;
- ignorar o cuarentenar lectura duplicada, tardía o no plausible;
- operar sin Telemetría mediante lectura manual autorizada;
- publicar indisponibilidad sin escribir tablas de Logística;
- cerrar OT y demostrar inmutabilidad/reapertura explícita;
- recibir vehículo de tercero y vincular cliente/propietario autorizados;
- aprobar presupuesto/reparación sin convertir el enlace en permiso permanente;
- emitir documento por contrato público sin que F2 posea factura o deuda;
- funcionar con F2 o BPM ausentes/inactivos;
- verificar UI y ausencia de overflow en 375, 720 y 1280 px.

## Dudas para FM-00/AW-00

La decisión de producto ya fija los límites FM-D01–FM-D12 y AW-D01–AW-D10. Las
historias iniciales todavía deben relevar datos concretos sin cambiar esos
límites:

- categorías y cantidad real de vehículos;
- políticas por fecha, kilómetros, horas, condición o fabricante;
- talleres, puestos, turnos, especialidades y terceros utilizados;
- responsables de autorización y segregación de funciones;
- evidencia, tamaño, retención y privacidad de fotografías/documentos;
- monedas y política de costos;
- fuentes reales de odómetro/horómetro y tolerancias;
- alcance de garantía, siniestros, campañas/recalls y neumáticos;
- política comercial de diagnóstico, presupuesto, anticipos y entrega;
- licencia y subconjunto VMRS que eventualmente se contratará.
