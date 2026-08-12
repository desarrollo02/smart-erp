# Evidencia — ADR-0046 planificación de mantenimiento y taller

- Fecha: 2026-08-12
- Tipo: decisión y planificación documental
- Estado: aceptado por producto; implementación no autorizada
- Decisión: [ADR-0046](../adr/0046-familia-mantenimiento-flota-taller-automotriz.md)
- Fuente: [caracterización del legado](../knowledge-base/vehicle-maintenance/legacy-characterization.md)

## Confirmación de producto

Producto aprobó todos los puntos presentados el 2026-08-12. La aprobación se
registra como aceptación sin cambios de FM-D01 a FM-D12 y AW-D01 a AW-D10.

## Resultado documental

- se planifica la familia vertical Flota con F1 `fleet_maintenance` y F2
  `automotive_workshop`;
- el catálogo global planificado pasa de treinta y uno a treinta y tres plugins
  reutilizables;
- ERP 1–19 mantiene numeración y precedencia;
- F1 comienza después de estabilizar `logistics-api`; Telemetría es opcional;
- F2 comienza después de F1, Ventas y Documentos Comerciales;
- se separan costo/ejecución técnica de precio/venta/facturación;
- `J11-S9-06` continúa siendo el siguiente trabajo autorizado;
- no se agregaron módulos, POM, descriptores, migraciones, pantallas o código.

## Evidencia revisada

- fuente preferente `C:\cosme\mega\miaterra\fuente\tag` en revisión
  `6f6dda310`;
- manual de usuario de Taller con 22 pantallas;
- documentación específica de OT y combustible/aceite;
- menú, controladores y entidades de Taller/Stock;
- tablas de OT, servicios, eventos, adjuntos, personal, checklist y reglas de
  mantenimiento;
- acoplamientos con vehículos, clientes, contratos, compras, stock, tesorería y
  transporte convertidos en límites y contratos propuestos.

## Verificación aplicable

| Comprobación | Resultado |
|---|---|
| revisión estática del legado | ejecutada en solo lectura |
| coherencia F1/F2 y propietarios | revisada contra ADR-0011/0034/0045 |
| conteo del catálogo | 31 → 33; ERP 1–19 sin cambios |
| siguiente iteración | J11-S9-06 permanece autorizada |
| pruebas Maven/ArchUnit/PostgreSQL | no aplican: no hubo código ni composición |
| pruebas funcionales/Playwright | pendientes de historias FM/AW futuras |
| PDF del roadmap | regenerado y verificado en evidencia específica |

La revisión documental no se describe como implementación ni como gate técnico
verde de los futuros plugins.
