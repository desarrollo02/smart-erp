# J11-S11-01 - Caracterización de `sales`

- Estado: Completada; SA-D01 a SA-D10 aceptadas sin cambios
- Sprint: 11
- Fecha: 2026-08-20
- Tipo: conocimiento del legado y requisitos
- Evidencia: [caracterización](../../knowledge-base/sales/legacy-characterization.md)

## Objetivo y método

Separar presupuesto, pedido, reserva y compromiso de factura, finanzas y
logística antes de diseñar código. Se revisó Miaterra en el commit
`ca5bdd74395182f69a9f876be7eb72f9ded0b2a7` y PostgreSQL local en transacción de
sólo lectura. Se consultaron metadatos, conteos, estados y cinco registros; se
protegieron personas, IDs e importes. Docker destino no estaba disponible.

## Hallazgos

- tablas de presupuesto vacías; 1.021 pedidos y 2.637 líneas en el modelo activo;
- 399 estados nulos y 543 estados mezclados con facturación;
- 21 FKs en cabecera y un trigger cruzado agrícola en detalle;
- APIs destino suficientes para cliente, precio, moneda y reserva.

SA-D01 a SA-D10 fijan agregados/ciclos separados, reserva al confirmar, snapshots,
condiciones empresariales, excepciones autorizadas, concurrencia/idempotencia,
cuatro rutas y APIs públicas.

## Validación y resultado

No se ejecutaron Maven, Docker o Playwright. La consulta JDBC usó herramientas
validadas bajo `.tools/`, terminó con rollback y eliminó el auxiliar temporal tras
verificar su ruta. J11-S11-02 queda listo para una solicitud explícita; no se
crearon módulos, migraciones, permisos o pantallas.
