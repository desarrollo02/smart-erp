# J11-S9-04 — Aplicación, permisos, JTA e idempotencia de `purchasing`

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Fecha: 2026-08-11
- Dependencia: J11-S9-03 implementada y validada automáticamente
- Decisión: [ADR-0043](../../adr/0043-aplicacion-jta-idempotencia-purchasing.md)
- Evidencia: [J11-S9-04](../../evidence/J11-S9-04-aplicacion-purchasing.md)

## Objetivo

Implementar casos de uso autorizados y auditados para solicitudes, órdenes,
recepciones, devoluciones e importación abierta; garantizar reintentos seguros y
una sola transacción JTA al confirmar stock con Inventario.

## Alcance implementado

- doce permisos declarados por el plugin;
- contexto de operación revalidado por empresa, plugin y permiso;
- auditoría técnica para cambios, reintentos, consultas y rechazos;
- casos de uso de solicitud: crear, reemplazar líneas, clonar con identidades
  nuevas, enviar, aprobar, rechazar y cancelar;
- casos de uso de orden: crear directa/asignada, emitir, cancelar y cerrar
  faltantes;
- recepción y devolución con integración pública a Inventario;
- snapshot de unidad presentada/base, factor, versión y trazabilidad física;
- V2 con ledger general de operaciones y ledger de importación;
- importación tipada de solicitudes y órdenes abiertas con procedencia;
- consultas públicas mínimas de solicitud y orden;
- adaptadores CDI y frontera JTA con rollback explícito ante resultado fallido.

## Criterios de aceptación

- **CA-01:** una operación sin permiso se rechaza antes de leer o generar identidad.
- **CA-02:** cada mutación exige empresa actual y permiso exacto.
- **CA-03:** un reintento con misma clave y huella no repite la mutación.
- **CA-04:** reutilizar clave o procedencia con otro comando produce conflicto.
- **CA-05:** proveedor, moneda, ítem y conversión se resuelven sólo por API pública.
- **CA-06:** una línea de stock usa identidad pública de catálogo; Inventario
  resuelve su identidad privada.
- **CA-07:** recepción/devolución, orden y movimiento quedan dentro de una frontera
  JTA y todo fallo marca rollback.
- **CA-08:** servicios y no-stock no publican movimiento físico.
- **CA-09:** las importaciones conservan procedencia y no escriben SQL privado.
- **CA-10:** no se agregan pantallas, menús, composición física ni eventos.
- **CA-11:** pruebas y gates quedan enumerados sin afirmar estado verde.

Todos los criterios están implementados; las pruebas de módulo, PostgreSQL,
arquitectura y regresión completa fueron ejecutadas. La validación independiente
permanece pendiente.

## Pruebas automatizadas ejecutadas

El corte `.tools/tmp/validation/J11-S9-05-automated` ejecutó:

```powershell
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl plugins/purchasing -am test
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl plugins/purchasing -am verify "-Dlogixone.postgres.integration=true"
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl tests/architecture-tests -am test
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml verify
```

Los gates terminaron verdes. La prueba runtime JTA entre el WAR compuesto e
Inventario se ejecutará con J11-S9-06, cuando Compras ingrese a la composición;
no se sustituye por una prueba manual.

## Resultado

La aplicación de Compras está implementada y validada automáticamente en el corte
disponible. Sprint 9 continúa abierto: falta componer la capacidad, ejecutar sus
gates runtime/Playwright y recibir validación independiente antes de considerar
una candidata comercializable o productiva.
