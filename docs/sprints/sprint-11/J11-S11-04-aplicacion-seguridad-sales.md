# J11-S11-04 — Aplicación, seguridad y reservas de `sales`

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Fecha: 2026-08-21
- Decisión: [ADR-0051](../../adr/0051-aplicacion-jta-reservas-sales.md)

## Alcance implementado

- once permisos y contexto empresarial autorizado;
- condiciones: alta, revisión e inactivación;
- presupuestos: alta, emisión, aceptación, rechazo, vencimiento y cancelación;
- pedidos directos o derivados, confirmación, cancelación y cierre;
- precio normal y excepción autorizada con motivo;
- ledger idempotente, auditoría e historia de transiciones;
- reserva exacta por línea y liberación del remanente al cancelar;
- `inventory-api` 1.3.0 con reserva por identidad pública de catálogo;
- frontera CDI/JTA con rollback explícito ante resultados fallidos;
- consulta pública mínima de presupuesto y pedido.

## Criterios

1. La denegación ocurre antes de leer referencias o generar UUID.
2. Las referencias se resuelven por APIs públicas.
3. Un reintento idéntico no repite efectos; otra huella se rechaza.
4. Las líneas de servicio no reservan y las de stock son atómicas.
5. Ventas nunca recibe `InventoryItemId`.
6. Cancelar libera sin borrar historia o referencias.
7. Cada transición conserva actor, instante, estados y motivo.
8. Cerrar no representa factura, cobro, despacho o entrega.
9. Un resultado fallido marca rollback JTA.
10. UI, composición física y Playwright quedan fuera.

## Validación

El corte coherente final se materializó desde el índice en
`.tools/tmp/validation/J11-S11-04-final-v4` y se validó el 2026-08-21:

- pruebas de módulo: verdes; `inventory` 66 pruebas y `sales` 14 pruebas;
- `mvnw.cmd -f .tools/tmp/validation/J11-S11-04-final-v3/pom.xml -pl plugins/sales -am verify -Dlogixone.postgres.integration=true`: verde; Flyway V1 validada y 3 escenarios sobre PostgreSQL 18.4 mediante Testcontainers, incluido ledger e historial persistentes; `v4` sólo agrega las reglas ArchUnit y esta evidencia;
- `mvnw.cmd -f .tools/tmp/validation/J11-S11-04-final-v4/pom.xml -pl tests/architecture-tests -am test`: verde; 26 módulos y 37 pruebas ArchUnit/composición, incluidas las fronteras de API, dominio, aplicación y persistencia de Sales;
- `mvnw.cmd -f .tools/tmp/validation/J11-S11-04-final-v4/pom.xml verify`: verde; reactor completo de 30 módulos, WAR incluido.

La primera ejecución PostgreSQL quedó bloqueada porque el servicio Docker estaba
detenido. Se inició Docker Desktop, se repitió exactamente el gate y terminó
verde. Al ampliar la cobertura, un fixture abreviado infringió correctamente la
restricción de huella SHA-256; se corrigió a los 64 caracteres que produce la
aplicación y el gate final quedó verde. Ningún fallo se clasificó como prueba
diferida. La prueba runtime de la
transacción JTA distribuida entre `sales` e `inventory`, junto con composición,
OIDC y Playwright, corresponde a `J11-S11-06`. La validación independiente
acumulada permanece pendiente conforme a la continuidad autorizada.
