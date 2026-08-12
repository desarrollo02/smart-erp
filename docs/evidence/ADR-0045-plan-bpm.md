# Evidencia — ADR-0045 planificación de BPM

- Fecha: 2026-08-11
- Tipo: análisis y planificación documental
- Estado: completada; implementación no iniciada
- Decisión: [ADR-0045](../adr/0045-plugin-gestion-procesos-negocio-bpm.md)
- Épica: [Gestión de procesos de negocio](../backlog/epica-gestion-procesos-negocio-bpm.md)

## Autorización registrada

Producto solicitó analizar un plugin BPM para que cada empresa configure,
supervise y mejore sus procesos. Después de revisar viabilidad, confirmó agregarlo
a la planificación.

## Resultado

- `business_process_management` queda planificado como funcional transversal,
  opcional y activable por empresa;
- no renumera ERP 1–19 ni cambia el siguiente incremento autorizado;
- el catálogo global planificado pasa de treinta a treinta y un reutilizables;
- BPM coordina, pero cada plugin funcional conserva datos, invariantes y permisos;
- BPMN 2.0.2 orienta el modelado con un subconjunto cerrado;
- DMN 1.5 queda como extensión posterior;
- el primer piloto propuesto es aprobación de solicitudes de Compras;
- BPM-D01 a BPM-D12 deben resolverse antes del código;
- no se seleccionó motor, biblioteca visual o transporte;
- no se modificaron POM, código, migraciones, composición, Docker o UI.

## Fuentes revisadas

- arquitectura modular y activación: ADR-0002 y ADR-0005;
- roadmap y dependencias: ADR-0011;
- eventos/outbox/inbox: ADR-0013 y su contrato operativo;
- autorización y auditoría: ADR-0016;
- modelo de aprobación actual de `purchasing`;
- OMG BPMN 2.0.2 y DMN formal 1.5;
- Jakarta Batch 2.1 como capacidad batch, no como motor BPM.

## Validación documental ejecutada

- enlaces Markdown locales: `OK` en los trece archivos incorporados o
  actualizados por la planificación;
- UTF-8 sin caracteres de reemplazo: `OK`;
- ausencia de espacios finales: `OK`;
- consistencia de conteo treinta → treinta y uno y secuencia ERP 1–19 sin cambios:
  revisada mediante búsqueda estática;
- presencia en índices ADR, backlog, arquitectura, Sprint 9 y guías: `OK`;
- `git diff --check`: `OK`.

No corresponde ejecutar Maven, PostgreSQL, ArchUnit, Compose o Playwright porque
este cambio agrega exclusivamente planificación y no modifica artefactos
ejecutables.
