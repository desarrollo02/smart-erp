# Inventory

Plugin funcional `inventory@1.1.0`.

Desde J11-S8-05 contiene el descriptor CDI/SPI, el dominio neutral de depósitos,
ubicaciones, inscripción de productos, saldos, movimientos, reservas y conteos.
Declara una dependencia requerida de `commercial_catalog` 1.x y solo consume su
API pública. Su esquema privado `plg_inventory` tiene V1–V2, unidad JPA en modo
`validate`, snapshots históricos y siete repositorios acotados por empresa. La
aplicación publica ocho permisos, revalida empresa y autorización, audita sus
resultados y ejecuta las mutaciones mediante una frontera CDI/JTA. Tres menús y
contratos de pantalla neutrales exponen depósitos, existencias y conteos; sus
handlers consultan y mutan los casos de uso sin aportar XHTML, CSS o JavaScript.

Todavía no contiene eventos ni composición física. El WAR, el migrador, los datos
de demo y la validación visual navegable pertenecen a J11-S8-06.
Consulte el
[contrato del plugin](docs/plugin-contract.md) y
[ADR-0023](../../docs/adr/0023-modelo-inventory-y-contratos-publicos.md) junto con
[ADR-0024](../../docs/adr/0024-persistencia-privada-inventory.md).

J11-S9-04 amplió el contrato público a 1.1 con el movimiento por identidad de
catálogo para Compras. No cambió tablas privadas ni expuso `InventoryItemId` al
consumidor.
