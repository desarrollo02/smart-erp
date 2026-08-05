# J11-S8-04 - Aplicación, seguridad y transacciones de `inventory`

- Estado: Completada
- Sprint: 8
- Fecha: 2026-07-31
- Dependencia: [J11-S8-03](J11-S8-03-persistencia-inventory.md)
- ADR aplicables: [ADR-0023](../../adr/0023-modelo-inventory-y-contratos-publicos.md) y [ADR-0024](../../adr/0024-persistencia-privada-inventory.md)
- Evidencia: [J11-S8-04](../../evidence/J11-S8-04-aplicacion-seguridad-inventory.md)

## Objetivo

Convertir el dominio y los repositorios verdes de inventario en casos de uso
autorizados, auditables, idempotentes y atómicos, sin adelantar Jakarta Faces ni
la composición física del plugin.

## Resultado implementado

- siete permisos separados para consulta, estructura, artículos, movimientos,
  reservas, conteos y ajustes;
- comandos de depósitos, ubicaciones e inscripción local de productos resueltos
  exclusivamente mediante `CatalogItemDirectory`;
- consulta exacta de almacén, artículo, disponibilidad, movimiento, reserva y
  conteo;
- entradas, salidas, transferencias, ajustes y reversiones append-only con
  conversión pública reproducible e idempotencia por empresa y fuente;
- reservas con consumo parcial, liberación y expiración según reloj del servidor;
- recibo inmutable V2 para impedir efectos repetidos de consumir, liberar o
  expirar una reserva;
- conteos con teórico estable al iniciar, cierre del alcance, captura, revisión,
  cancelación y ajuste inmutable al contabilizar;
- adaptadores CDI de `InventoryAvailability`, `InventoryMovements` e
  `InventoryReservations` con empresa actual y permiso exacto revalidados;
- fachada JTA que marca rollback ante todo resultado fallido de una mutación.

## Criterios de aceptación

- **CA-01:** una operación sin permiso se rechaza antes de leer catálogo,
  repositorios o generar identidad. **Cumplido.**
- **CA-02:** una empresa no puede sustituir el `CompanyId` autorizado.
  **Cumplido.**
- **CA-03:** una salida, reserva o ajuste no puede dejar físico, reservado o
  disponible negativos. **Cumplido.**
- **CA-04:** un movimiento valida artículo, trazabilidad, almacén, ubicación,
  bloqueo de conteo y conversión vigente. **Cumplido.**
- **CA-05:** un reintento exacto no duplica movimiento, reserva, consumo,
  liberación, expiración ni cierre de conteo. **Cumplido para operaciones con
  efecto físico o reservado; las transiciones de conteo usan versión y estado
  resultante para reconocer el reintento exacto.**
- **CA-06:** consumir una reserva reduce físico y reservado y agrega una salida
  inmutable ligada a la reserva. **Cumplido.**
- **CA-07:** iniciar un conteo rechaza un teórico que cambió antes del bloqueo y
  contabilizarlo genera diferencias, nunca reemplazo silencioso. **Cumplido.**
- **CA-08:** los rechazos y cambios generan auditoría técnica sin cantidades,
  nombres ni datos privados. **Cumplido.**
- **CA-09:** cualquier fallo de mutación deja la transacción JTA marcada para
  rollback. **Cumplido.**
- **CA-10:** el API público continúa libre de Jakarta y el WAR base continúa sin
  incorporar `inventory`. **Cumplido.**
- **CA-11:** Flyway V1→V2, JPA `validate`, aislamiento, restricciones y libro de
  operaciones de reserva quedan verdes sobre PostgreSQL. **Cumplido.**
- **CA-12:** `mvnw.cmd verify` y ArchUnit quedan verdes. **Cumplido.**

## Permisos publicados

| Permiso | Responsabilidad |
|---|---|
| `inventory.view` | consultas exactas y futuras pantallas de lectura |
| `inventory.storage.manage` | depósitos y ubicaciones |
| `inventory.items.manage` | inscripción, refresh e inactivación de artículos |
| `inventory.movements.post` | entrada, salida y transferencia ordinarias |
| `inventory.reservations.manage` | reserva, consumo, liberación y expiración |
| `inventory.counts.manage` | ciclo de conteo salvo contabilización |
| `inventory.adjustments.post` | ajustes, reversiones y cierre contabilizado |

Ocultar un botón no sustituye estas guardas. El kernel debe comprobar empresa,
plugin activo y concesión antes de emitir `AuthorizedCompanyOperation`.

## Persistencia añadida

V1 permanece inmutable. V2 agrega `stock_reservation_operation`, propiedad de
`plg_inventory`, con clave primaria `(company_id, idempotency_key)`, FK privada a
la reserva y snapshot del resultado. El esquema pasa de nueve a diez tablas y la
unidad JPA de nueve a diez entidades.

## Validación

- módulo y dependencias: 41 pruebas unitarias de `inventory`, todas verdes;
- PostgreSQL/Testcontainers: 10 escenarios de migración/JPA, todos verdes;
- reactor: 24/24 módulos y 351 pruebas, sin fallos, errores ni omitidas;
- arquitectura: 24 pruebas, incluidas 20 reglas ArchUnit, verdes;
- `inventory-api`: cero clases o dependencias `jakarta.*` empaquetadas;
- WAR base: cero entradas de `inventory`, como exige la composición diferida.

## Fuera de alcance

No se agregaron menú, pantalla, renderer, fixture visual, perfil físico, imagen ni
demo nueva. `J11-S8-05` queda habilitada para publicar los contratos de pantalla y
la UI JSF Material Design 3 responsive. `J11-S8-06` compondrá el plugin y producirá
la siguiente demo visual; Sprint 8 continúa abierto.
