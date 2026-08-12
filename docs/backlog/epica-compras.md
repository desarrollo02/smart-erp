# Épica - Compras `purchasing`

- Estado: activa; J11-S9-06 implementada y validada automáticamente, validación independiente pendiente; J11-S9-07 habilitada
- Orden del roadmap: 4
- Incremento inicial: [Sprint 9](../sprints/sprint-09/README.md)
- ADR rector: [ADR-0011](../adr/0011-roadmap-dependencias-plugins-productivos.md)
- Caracterización: [legado actualizado](../knowledge-base/purchasing/legacy-characterization.md)

## Resultado de negocio

Permitir que una empresa solicite, apruebe y ordene compras; registre recepciones
parciales o completas; y devuelva cantidades a un proveedor, manteniendo historia,
permisos y trazabilidad sin acoplarse a las tablas de socios, catálogo, referencia
o inventario.

## Audiencias

- solicitante: declara una necesidad y consulta su avance;
- aprobador: acepta o rechaza solicitudes con motivo y separación de funciones;
- comprador: selecciona proveedor, prepara y emite órdenes;
- receptor: confirma cantidades y destino físico;
- supervisor: cierra faltantes, cancela o autoriza correcciones/devoluciones;
- soporte/auditoría: consulta estados, actores, versiones y referencias externas.

## Alcance inicial

- solicitudes de compra con líneas de catálogo o descripción libre;
- aprobación/rechazo, cancelación y clonación segura;
- órdenes locales directas o derivadas de solicitudes;
- proveedor, moneda, unidad, precio y condiciones históricas;
- recepción parcial/final contra orden;
- devolución a proveedor contra recepción;
- cumplimiento por línea: ordenado, recibido, devuelto, pendiente y cerrado;
- permisos, empresa, activación, auditoría, concurrencia e idempotencia;
- búsqueda paginada y revalidada de proveedores, ítems, monedas y destinos;
- integración sólo por APIs públicas.

## Fuera del primer alcance

- importaciones, embarques, nacionalización y costos asociados;
- comprobante fiscal del proveedor, factura, nota de crédito y SIFEN;
- deuda, cuotas, vencimientos, anticipos, rendiciones, pagos y retenciones;
- cuentas bancarias del proveedor, caja, orden de pago y conciliación;
- asientos, provisión, períodos y valoración de inventario;
- planificación de reposición, licitaciones, cotizaciones múltiples o workflow
  configurable de varios niveles.

## Dependencias requeridas candidatas

| Plugin/API | Capacidad consumida |
|---|---|
| `business_partners` | proveedor activo y rol `SUPPLIER` |
| `commercial_catalog` | ítem, tipo, unidad base y conversión |
| `reference_data` | moneda habilitada, escala y publicación |
| `inventory` | destino y movimiento idempotente de entrada/salida |
| kernel | empresa, identidad, autorización, auditoría y activación |

Las dependencias fueron confirmadas con PU-D10. No se permiten
relaciones JPA, SQL, DTO internos ni claves foráneas entre esquemas privados.
`legacy_migration` será un consumidor técnico opcional de la API pública de
Compras; `purchasing` no depende de su implementación y continúa operando cuando
el módulo de migración se retira.

`business_process_management` será otro consumidor transversal opcional. Un
futuro piloto podrá coordinar la aprobación de solicitudes mediante eventos y
comandos públicos, pero Compras conservará sus estados, separación solicitante/
aprobador, autorización e invariantes. `purchasing` seguirá operando con BPM
ausente o inactivo y no importará su implementación.

## Mapa de historias

| Orden | Historia | Entregable |
|---:|---|---|
| 1 | J11-S9-00 | gobierno, alcance, excepción de pruebas y riesgos |
| 2 | J11-S9-01 | caracterización y decisiones PU-D01 a PU-D10 |
| 3 | J11-S9-02 | `purchasing-api`, dominio neutral y contratos; validada automáticamente |
| 4 | J11-S9-03 | esquema privado, migraciones y repositorios; validada automáticamente con PostgreSQL |
| 5 | J11-S9-04 | aplicación, permisos, auditoría, JTA e idempotencia; validada automáticamente en el corte no compuesto |
| 6 | J11-S9-05 | UI neutral/Material Design responsive; módulo y shell validados automáticamente, Playwright al componer |
| 7 | J11-S9-06 | composición, integraciones y demo candidata; validada automáticamente |
| 8 | J11-S9-07 | matriz acumulada, demo oficial, PDF y cierre |
| 9 | J11-S9-08 | decisión explícita y, sólo con `SÍ`, instalador Windows |

J11-S9-02 a J11-S9-06 están implementadas y validadas automáticamente; sólo la
validación independiente permanece diferida. J11-S9-06 completó composición,
runtime, OIDC y Playwright. J11-S9-07 no se considera cumplida hasta repetir la
matriz sobre el baseline congelado, completar sus entregables de cierre y reunir
la aceptación independiente; sin esos gates no existe versión comercializable.

## Criterios de aceptación de la épica

- Una solicitud enviada y una orden emitida no se reescriben silenciosamente.
- Una solicitud puede existir sin proveedor; una orden emitida requiere uno.
- Solicitante y aprobador están separados en V1.
- Cantidades asignadas no superan lo aprobado; recepciones no superan lo ordenado;
  devoluciones no superan el neto recibido.
- Las líneas de stock producen movimientos de inventario idempotentes; servicios
  y no-stock no alteran existencias.
- Ninguna confirmación deja recepción/devolución e inventario en estados parciales.
- Los snapshots históricos permanecen aunque cambien los maestros.
- Factura, deuda, pago, retención, costo y asiento no son estados de la orden.
- Toda mutación revalida empresa, permiso, plugin activo, versión y selección.
- Las pantallas funcionan en 375, 720 y 1280 px, con teclado, foco visible,
  mensajes comprensibles y sin overflow horizontal normal.
- El plugin presente/ausente conserva una composición y migrador coherentes.
- La versión sólo se denomina comercializable después de ejecutar la matriz
  acumulada y resolver todo fallo.
- Si la oferta comercializable incluye migración Oracle, órdenes/solicitudes
  abiertas ingresan mediante un contrato público tipado, idempotente y con
  procedencia; nunca mediante SQL al esquema de Compras.
- El piloto BPM futuro usa eventos y comandos públicos y prueba Compras con BPM
  presente, ausente e inactivo; no forma parte de J11-S9-06.

## Riesgos principales

- ampliar `business-partners-api` para datos históricos que hoy no expone;
- definir atomicidad JTA entre recepción/devolución y `InventoryMovements`;
- impedir dobles recepciones bajo concurrencia y reintentos;
- distinguir unidad presentada, unidad base y factor sin redondeo silencioso;
- evitar que impuestos o pagos entren prematuramente al agregado de orden;
- migrar estados y relaciones cruzadas del legado sin copiar su acoplamiento;
- mantener visible la validación independiente pendiente y no confundirla con los
  gates automatizados obligatorios.

## Condición de inicio de código

Producto aceptó PU-D01 a PU-D10 sin cambios y autorizó la rama local
`sprint/09-purchasing` el 2026-08-11. J11-S9-02 implementó API y dominio; J11-S9-03
implementó nueve tablas privadas, V1, JPA y cuatro repositorios. J11-S9-04 agregó
doce permisos, aplicación auditada, V2, contratos CDI/JTA e integración pública
con Inventario. J11-S9-05 agregó menús, pantallas, búsquedas paginadas, selectores
gobernados y el manual PDF 07. J11-S9-06 compuso WAR/migrador y dejó verdes Maven,
ArchUnit, PostgreSQL, Docker/Compose, migraciones, health, OIDC y Playwright.
J11-S9-07 queda habilitada para el gate acumulado y los entregables de cierre. La
validación independiente por otra persona permanece pendiente.
