# Evidencia J11-S10-00 — Gobierno y planificación de floorplans

- Fecha: 2026-08-14
- Estado: Completada; caracterización aceptada
- Tipo de corte: exclusivamente documental
- Baseline funcional inspeccionado: J11-S9-07
- Pendiente heredado: validación independiente de Sprint 9

## Resultado

Se creó el
[inventario de pantallas, tareas y métricas](../sprints/sprint-10/inventario-tareas-metricas-floorplans.md)
antes de modificar `plugin-api` o el shell. El inventario deriva de los
descriptores, contratos, handlers, dominio, pruebas Playwright y capturas reales
del baseline, no de un mock.

El resultado registra:

- 23 pantallas clasificadas sin categoría abierta;
- 6 tareas piloto con actor, objetivo, precondiciones, recorrido y resultado;
- acciones válidas por estado, actor, permiso y riesgo;
- 9 campos técnicos visibles de Inventario que deben salir de la captura manual;
- identidades, versiones, idempotencia, auditoría y referencias que deben seguir
  internas;
- inventario de selectores, fuente, propietario, clase y ruta de administración;
- baseline de activaciones, foco, campos técnicos, errores y desborde vertical;
- umbrales para comparar el renderer compatible v1 con el candidato v2;
- matriz responsive 375/599/600/720/839/840/1280, teclado, escaneo y
  accesibilidad;
- estrategia aditiva para `PluginApiVersion 0.4.x` y contratos de pantalla v1/v2.

## Fuentes revisadas

- `plugin-api`: `ScreenDefinition`, `ScreenElementDefinition`, tipos y versión;
- `ShellScreenRegistry`: rutas y `ScreenId` realmente soportados;
- contratos y handlers de `inventory` y `purchasing`;
- agregados `PurchaseRequest`, `PurchaseOrder`, `GoodsReceipt` y
  `SupplierReturn`;
- permisos públicos de Inventario y Compras;
- `InventoryVisualIT` y `PurchasingVisualIT`;
- evidencia y capturas de J11-S8-06 y J11-S9-06;
- ADR-0047 y la épica de floorplans.

## Medición de capturas preexistentes

Las capturas Playwright son `fullPage`. Se releyeron sus dimensiones con
`System.Drawing.Image` sin editar los archivos:

| Captura | Dimensión | DV con viewport 900 |
|---|---:|---:|
| movimiento Inventario expandido | 1280×1738 | 838 px |
| solicitud enviada expandida/media/compacta | 1280×1117 / 720×900 / 375×1061 | 217 / 0 / 161 px |
| orden emitida expandida/media/compacta | 1280×1117 / 720×900 / 375×987 | 217 / 0 / 87 px |
| recepción confirmada compacta | 375×900 | 0 px |
| devolución confirmada media | 720×900 | 0 px |

Los anchos no capturados no reciben valores inventados. J11-S10-05 debe repetir
renderer v1 y v2 con el mismo fixture y completar la matriz.

## Guardas conservadas

- Ocultar un campo o una acción no sustituye autorización del servidor.
- Recepción y devolución de stock conservan el permiso técnico público de
  movimiento de compra.
- No se cambia dominio, persistencia, migraciones, rutas ni composición.
- `sales` permanece fuera de Sprint 10.
- La validación independiente pendiente no se representa como completada.

## Validación del corte

Se ejecutan para este corte:

```powershell
git diff --check
.\.tools\python\3.13.14\python.exe tools\validate_docs.py
```

No corresponde Maven: no se modificó código, POM, contrato, migración,
composición ni runtime. J11-S10-01 vuelve al flujo incremental completo con
materialización del índice y prueba mínima inmediata.

## Siguiente trabajo habilitado

J11-S10-01 puede evolucionar el contrato Java puro de forma aditiva, con pruebas
de compatibilidad v1/v2 y rechazo seguro de versiones desconocidas. No se inicia
el renderer ni los pilotos antes de que esa prueba esté verde.
