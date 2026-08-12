# J11-S9-02 — Dominio neutral y contratos públicos de `purchasing`

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Sprint: 9
- Fecha: 2026-08-11
- Dependencia: [J11-S9-01](J11-S9-01-caracterizacion-purchasing.md) completada y PU-D01 a PU-D10 aceptadas
- Decisión: [ADR-0041](../../adr/0041-modelo-purchasing-y-contratos-publicos.md)

## Objetivo

Crear `purchasing` como cuarto plugin funcional, físicamente separado de sus
predecesores, con API Java pura `1.0.0` y dominio neutral. El corte materializa las
decisiones aceptadas sin adelantar persistencia, aplicación, UI o composición.

## Alcance implementado

- módulos Maven `purchasing-api` y `purchasing` registrados en el reactor;
- identidades opacas de solicitud, orden, recepción, devolución y líneas;
- contratos `PurchasingDirectory` y `PurchasingImports`;
- importación tipada e idempotente de solicitudes u órdenes abiertas;
- snapshots de proveedor, artículo/unidad y moneda mediante contratos públicos;
- solicitud sin proveedor, líneas `STOCK`/`NON_STOCK`/`SERVICE` y aprobación con
  separación solicitante/aprobador;
- orden directa o con asignaciones parciales, cantidades inmutables y cierre de
  faltantes explícito;
- recepción parcial sin sobre-recepción;
- devolución compensatoria que no edita la recepción;
- destino/origen y movimiento de inventario obligatorios para líneas `STOCK`;
- descriptor CDI/SPI con cuatro dependencias funcionales requeridas 1.x;
- pruebas unitarias y reglas ArchUnit escritas para el gate acumulado.

## Fuera de alcance

- Flyway, `plg_purchasing`, JPA, repositorios o secuencias;
- casos de uso transaccionales, permisos, auditoría e idempotency ledger;
- menús, pantallas, renderers, selectores y Playwright;
- WAR, migrador, perfiles físicos o datos de demo;
- importación histórica de recepciones, devoluciones, comprobantes o pagos;
- eventos/outbox sin consumidor real.

## Criterios de aceptación

- **CA-01:** API e implementación son módulos Maven distintos. **Cumplido y probado.**
- **CA-02:** `purchasing-api` usa sólo Java y `CompanyId`. **Cumplido por ArchUnit.**
- **CA-03:** el descriptor declara versión `1.0.0`, cuatro dependencias requeridas
  1.x y ninguna migración/UI. **Cumplido y probado.**
- **CA-04:** solicitud y orden conservan ciclos separados. **Implementado; prueba
  pendiente.**
- **CA-05:** solicitante y aprobador son distintos. **Implementado; prueba
  pendiente.**
- **CA-06:** asignaciones no exceden la orden y cantidades directas exigen motivo.
  **Cumplido y probado.**
- **CA-07:** recepción no excede pendiente y devolución no excede neto recibido.
  **Cumplido y probado.**
- **CA-08:** `STOCK` exige catálogo, ubicación y movimiento; servicio/no-stock no
  altera inventario. **Cumplido y probado.**
- **CA-09:** todo cambio usa versión esperada y un registro confirmado no se
  reescribe. **Cumplido y probado.**
- **CA-10:** importación conserva procedencia y admite sólo documentos abiertos.
  **Cumplido y probado.**
- **CA-11:** no existen JPA, SQL, migraciones, permisos, menús, pantallas o
  eventos. **Cumplido por revisión estática y gate automatizado.**
- **CA-12:** documentación, ADR y evidencia describen límites y deuda de pruebas.
  **Cumplido.**

## Pruebas automatizadas ejecutadas

Se escribieron pruebas para identidades, comandos de importación, aprobación,
concurrencia, asignaciones, recepción, devolución, movimientos y descriptor, más
reglas ArchUnit de API/dominio y prohibición de acceder a implementaciones ajenas.

La aclaración de producto exige ejecutar las pruebas automatizadas y diferir sólo
la validación de otra persona. El corte reproducible
`.tools/tmp/validation/J11-S9-05-automated` ejecutó:

```powershell
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl plugins/purchasing -am test
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml -pl tests/architecture-tests -am test
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-05-automated\pom.xml verify
```

Resultados: módulo Compras y dependencias verdes, 32 pruebas de arquitectura
verdes y reactor completo de 28 módulos verde.

## Resultado

El código y la documentación del alcance están implementados y validados
automáticamente. La validación independiente sigue pendiente y el Sprint no se
considera comercializable. J11-S9-03 quedó habilitada para diseñar esquema
privado, migraciones y repositorios sin adelantar aplicación o UI.
