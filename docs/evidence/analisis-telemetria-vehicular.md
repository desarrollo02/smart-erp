# Evidencia — Incorporación de telemetría vehicular

- Fecha: 2026-08-03
- Tipo de cambio: documental y arquitectónico
- Resultado: `vehicle_telemetry` agregado como plugin funcional futuro número 7

## Decisión confirmada

Producto solicitó incorporar al plan telemetría y seguimiento GPS para vehículos
de cualquier categoría, incluida la posibilidad de pausar o finalizar el
seguimiento.

## Resultado

- `logistics` conserva vehículo, clasificación, conductor, ruta, viaje y despacho;
- `vehicle_telemetry` posee dispositivos, asignaciones, observaciones, posición,
  recorridos, geocercas, alertas y tracking lifecycle;
- los proveedores GPS se aíslan en adaptadores técnicos versionados;
- el ciclo inicial es `ACTIVE`, `PAUSED`, `STOPPED` y conserva historia;
- inmovilización, apagado y comandos físicos remotos quedan fuera;
- el roadmap pasa a diecinueve reutilizables y `19 + N` con personalizaciones;
- el plugin ocupa el orden 7, después de logística;
- no se inicia código durante Sprint 8 ni antes de estabilizar `logistics-api`.

## Artefactos

- [ADR-0034](../adr/0034-plugin-telemetria-vehicular.md);
- [caracterización](../knowledge-base/vehicle-telemetry/legacy-characterization.md);
- [épica](../backlog/epica-telemetria-vehicular.md);
- roadmap, arquitectura, selector inventory, guías e índices actualizados.

## Pruebas y límites

No se modificaron Java, POM, descriptor, migraciones, Compose ni UI. El gate
aplicable es G0 documental; Maven, Docker y Playwright no corresponden a este
cambio de planificación. Sprint 8 y J11-S8-C02 continúan como trabajo activo.

G0 se ejecutó con `tmp/validate_docs.py` usando el runtime Python local validado:

- 274 archivos Markdown revisados;
- 0 enlaces locales rotos;
- 0 errores de codificación UTF-8;
- 0 archivos con mojibake;
- 0 filtraciones de secretos locales.
