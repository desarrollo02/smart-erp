# Purchasing API

Contrato público Java puro `1.1.0` del plugin `purchasing`.

Publica identidades opacas, referencias mínimas de solicitudes y órdenes, y dos
comandos tipados e idempotentes para importar solicitudes u órdenes abiertas. El
contrato sólo depende de `kernel-api` para `CompanyId`; no expone Jakarta, JPA,
tablas, entidades, repositorios ni clases privadas de Compras o de otros plugins.

Los comandos de importación conservan sistema, registro y lote de origen. Una
solicitud con precio esperado declara además su moneda. Son una
frontera pública para un adaptador autorizado de `legacy_migration`; no permiten
escribir directamente el esquema privado ni importan recepciones, devoluciones,
facturas o pagos históricos.
