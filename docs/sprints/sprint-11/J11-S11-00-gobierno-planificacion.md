# J11-S11-00 - Gobierno y planificación de `sales`

- Estado: Completada documentalmente
- Sprint: 11
- Fecha: 2026-08-20
- Tipo: gobierno y planificación
- ADR: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)

## Objetivo y decisiones

Abrir ERP 5 sin ocultar Sprints abiertos ni adelantar plugins. Producto decidió
continuar con `sales`, usar Miaterra sólo en lectura, incluir presupuesto/pedido,
reservar al confirmar y excluir documentos, finanzas, logística y POS.

## Secuencia y criterios

J11-S11-00/01 preceden API, persistencia, aplicación, UI, composición, cierre e
instalador. Se conserva ADR-0011; Sprint 10 no se presenta como aceptado
independientemente; no se creó código; el legado no se modificó; las historias y
gates están enumerados.

## Validación

No correspondían Maven, Docker o Playwright. Se verificaron trazabilidad,
precedencia, alcance y límites documentales.
