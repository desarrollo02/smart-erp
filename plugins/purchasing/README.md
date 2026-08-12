# Purchasing

Plugin funcional `purchasing@1.1.0`.

J11-S9-02 incorporó el descriptor CDI/SPI y un dominio neutral para solicitudes,
órdenes, recepciones y devoluciones. Declara dependencias requeridas 1.x de
`business_partners`, `commercial_catalog` y `reference_data`, e Inventario desde
1.1, pero
consume exclusivamente sus contratos públicos.

J11-S9-03 agrega el esquema privado `plg_purchasing`, V1 con nueve tablas, una
unidad JPA en `validate` y repositorios empresariales para los cuatro agregados.
J11-S9-04 agrega doce permisos, casos de uso auditados, adaptadores CDI, frontera
JTA, integración pública con Inventario y V2 con dos ledgers de idempotencia e
importación. V1 conserva unidad presentada/base, factor y trazabilidad física.

J11-S9-05 agrega cinco menús y pantallas neutrales: solicitudes, órdenes,
recepciones, devoluciones y seguimiento. Los handlers publican
`ScreenInteraction`; el shell conserva Jakarta Faces, XHTML, Material Design 3 y
responsive. También incorpora directorios paginados y fuentes gobernadas para
todos los selectores. J11-S9-06 agrega la composición física
`with-purchasing-demo` al WAR y migrador, búsquedas exactas y el recorrido E2E
completo. Maven, ArchUnit, PostgreSQL/Testcontainers, migraciones, health, OIDC y
Playwright están verdes; sólo la validación independiente permanece pendiente.

Consulte [ADR-0041](../../docs/adr/0041-modelo-purchasing-y-contratos-publicos.md)
y [ADR-0042](../../docs/adr/0042-persistencia-privada-purchasing.md), junto con
[ADR-0043](../../docs/adr/0043-aplicacion-jta-idempotencia-purchasing.md) y
[ADR-0044](../../docs/adr/0044-recorridos-visuales-purchasing.md).

El detalle técnico mantenible se encuentra en
[docs/plugin-contract.md](docs/plugin-contract.md).
