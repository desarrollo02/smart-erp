# Épica — Taller automotriz comercial

- Estado: Planificada; AW-D01 a AW-D10 aceptadas el 2026-08-12
- Plugin: `automotive_workshop`
- Clasificación: funcional vertical, reutilizable y opcional por empresa
- Orden interno: F2
- Prerrequisitos: F1 `fleet_maintenance`, `sales` y `commercial_documents`
- Decisión: [ADR-0046](../adr/0046-familia-mantenimiento-flota-taller-automotriz.md)
- Fuente: [caracterización del Taller legado](../knowledge-base/vehicle-maintenance/legacy-characterization.md)

## Objetivo

Permitir que una empresa reciba vehículos de clientes, obtenga autorización para
diagnosticar y reparar, coordine presupuesto/pedido, siga el trabajo técnico y
entregue el vehículo con trazabilidad comercial, sin duplicar la orden de F1 ni
las fuentes de verdad de Ventas, Documentos, Tesorería o Cuentas por Cobrar.

## Valor de negocio

- controla el vehículo desde recepción hasta entrega;
- registra condición inicial y reduce disputas sobre daños/accesorios;
- distingue autorización de diagnóstico, presupuesto y ampliaciones;
- mantiene al cliente informado sobre espera, aprobación y avance;
- convierte trabajo técnico en venta/documento mediante contratos públicos;
- conserva evidencia de alcance aceptado y entrega;
- permite operar F1 como taller interno cuando F2 no está contratado.

## Audiencias

| Actor | Necesidad |
|---|---|
| recepcionista | identificar cliente/vehículo y registrar condición de ingreso |
| asesor de servicio | coordinar diagnóstico, presupuesto, autorización y comunicación |
| cliente/propietario | aprobar alcance y recibir información segura |
| jefe de taller | vincular visita/autorización con la OT técnica de F1 |
| ventas | administrar presupuesto, pedido, precios y descuentos canónicos |
| caja/cobranzas | operar documentos, pagos y deuda en sus módulos propietarios |
| auditoría/soporte | reconstruir consentimiento, versiones, comunicaciones y entrega |

## Capacidades

### Recepción

- cita o ingreso, cliente, propietario autorizado y `VehicleId`;
- kilometraje declarado/aceptado, combustible aproximado y accesorios;
- síntoma, daños visibles, fotografías y documentos;
- autorización acotada de diagnóstico;
- custodia, ubicación lógica y estado de la visita.

### Presupuesto y autorización

- solicitud de diagnóstico técnico a F1;
- correlación con presupuesto/pedido de `sales`;
- autorización/rechazo del cliente con versión, alcance, límite monetario,
  excepciones, canal, instante y evidencia;
- ampliación versionada cuando el diagnóstico descubre trabajo adicional;
- vencimiento y revocación de enlace sin convertirlo en sesión permanente.

### Trabajo y comunicación

- creación/referencia idempotente de una única OT técnica en F1;
- estado comercial derivado de eventos públicos, sin modificar la OT;
- comunicaciones de recepción, presupuesto, espera, avance y terminación;
- bloqueo comprensible cuando falta autorización o presupuesto vigente;
- operación segura con canales externos no disponibles.

### Entrega

- inspección de salida, documentos/evidencias y aceptación;
- correlación con factura/nota de `commercial_documents`;
- referencia opcional a cobro de Tesorería y deuda de Cuentas por Cobrar;
- fecha, receptor autorizado, observación y cierre comercial;
- reclamo posterior como referencia pública futura, sin reabrir silenciosamente
  la OT o documento.

## Propiedad e integraciones

| Dato o proceso | Propietario |
|---|---|
| vehículo | `logistics` |
| cliente/propietario/contacto | `business_partners` |
| diagnóstico, tareas, checklist, repuestos y OT | `fleet_maintenance` |
| artículo, servicio, precio e impuesto | `commercial_catalog` |
| presupuesto/pedido/descuento | `sales` |
| stock/consumo | `inventory`, solicitado desde F1 |
| factura/notas | `commercial_documents` |
| cobro | `treasury` |
| deuda | `accounts_receivable` |
| recepción, autorizaciones, comunicación y entrega | `automotive_workshop` |

F2 guarda IDs opacos y snapshots de consentimiento/entrega. No comparte JPA ni
consulta esquemas privados.

## Modelo conceptual

- `CustomerVehicleVisitId`, `VehicleIntakeId`, `IntakeEvidenceId`;
- `DiagnosticAuthorizationId`, `RepairAuthorizationId`;
- `CustomerCommunicationId`, `VehicleDeliveryId`;
- correlaciones con `VehicleId`, `BusinessPartnerId`, `SalesQuoteId`,
  `SalesOrderId`, `WorkOrderId` y `CommercialDocumentId`;
- estado, versión, idempotencia, canal y auditoría;
- snapshots mínimos de vehículo, cliente, alcance autorizado y receptor.

El futuro `automotive-workshop-api` será Java puro. F1 no dependerá de él.

## Estados iniciales

Visita:

`SCHEDULED`, `RECEIVED`, `DIAGNOSING`, `AWAITING_APPROVAL`, `AUTHORIZED`,
`IN_SERVICE`, `READY_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`.

Autorización:

`PENDING`, `APPROVED`, `REJECTED`, `EXPIRED`, `REVOKED`, `SUPERSEDED`.

El estado comercial se deriva de hechos confirmados por sus propietarios. F2 no
marca una factura como emitida, una OT como completada o un pago como recibido
sin el resultado público correspondiente.

## Pantallas previstas

1. Agenda/recepción y alta contextual mediante rutas del propietario.
2. Ficha de ingreso con condición y evidencias.
3. Autorización de diagnóstico.
4. Seguimiento del presupuesto/pedido de Ventas.
5. Autorización de reparación y ampliaciones.
6. Seguimiento, comunicación y entrega.

La vista del cliente será autenticada o usará una concesión de un solo propósito,
expirable y revocable. No expondrá datos de otras visitas, empresas, precios
internos, técnicos, inventario o eventos privados. Todas las pantallas internas
serán JSF 4.1, Material Design 3 y responsive en 375/720/1280 px.

## Permisos iniciales

- `automotive_workshop.view`;
- `automotive_workshop.intake.manage`;
- `automotive_workshop.diagnostics.request`;
- `automotive_workshop.estimates.coordinate`;
- `automotive_workshop.authorizations.manage`;
- `automotive_workshop.communications.manage`;
- `automotive_workshop.delivery.manage`;
- `automotive_workshop.commercial.view`;
- `automotive_workshop.export`.

Los permisos de Ventas, Documentos, Tesorería y Cuentas por Cobrar no se conceden
por tener un permiso de F2.

## Decisiones aceptadas AW-D01 a AW-D10

Las decisiones están detalladas en ADR-0046 y quedan aceptadas sin cambios: F2
separado; orden posterior a F1/Ventas/Documentos; vehículo y cliente externos;
recepción/autorización propias; una sola OT técnica en F1; presupuesto/factura en
sus dominios; pago/deuda externos; snapshots; aprobación segura y activación
opcional.

## Mapa de historias

| Orden | Historia | Entregable |
|---:|---|---|
| 1 | AW-00 | políticas reales de recepción, diagnóstico, autorización y entrega |
| 2 | AW-01 | API Java pura, estados y correlaciones con F1/Ventas |
| 3 | AW-02 | esquema privado, snapshots, historia e idempotencia |
| 4 | AW-03 | aplicación, permisos y adaptadores con Ventas/Documentos |
| 5 | AW-04 | UI responsive interna y autorización externa segura |
| 6 | AW-05 | entrega, comunicaciones y referencias de pago/deuda |
| 7 | AW-06 | composición, seguridad, integración, demo y regresión F1 |
| 8 | AW-07 | matriz integral, manuales, PDF, cierre y decisión de instalador |

## Criterios de aceptación

- **AW-CE01:** empresa, cliente, propietario y vehículo se revalidan en servidor.
- **AW-CE02:** una visita guarda condición/evidencia sin modificar el vehículo.
- **AW-CE03:** diagnóstico requiere autorización vigente según política.
- **AW-CE04:** presupuesto/pedido pertenecen a Ventas y se correlacionan por ID.
- **AW-CE05:** aprobación identifica versión y alcance exactos; una versión
  reemplazada no autoriza trabajo adicional.
- **AW-CE06:** un reintento no crea otra visita, autorización, OT o documento.
- **AW-CE07:** F2 muestra el estado confirmado por F1/Ventas/Documentos sin
  reescribirlo.
- **AW-CE08:** enlace vencido/revocado, actor ajeno o empresa ajena son rechazados.
- **AW-CE09:** entregar conserva receptor, instante, condición y referencias.
- **AW-CE10:** F2 no posee factura, pago, deuda o asiento.
- **AW-CE11:** F1 continúa operando con F2 ausente o inactivo.
- **AW-CE12:** logs y auditoría minimizan datos personales y no registran tokens.
- **AW-CE13:** interfaz interna/externa cumple accesibilidad y responsive.
- **AW-CE14:** PostgreSQL, JTA, outbox/inbox, seguridad negativa, composición,
  Docker/Compose, health y Playwright quedan verdes antes del cierre.

## Fuera de alcance V1

- marketplace de talleres o mecánicos;
- aseguradoras, siniestros y peritaje integral;
- financiación propia, crédito al cliente o cobranza dentro de F2;
- control remoto del vehículo;
- marketing/CRM general;
- conversión automática de chats en autorización sin confirmación válida;
- duplicar presupuestos, facturas o stock para operar sin sus plugins propietarios.
