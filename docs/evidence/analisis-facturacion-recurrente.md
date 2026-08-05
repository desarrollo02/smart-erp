# Evidencia — Incorporación de facturación recurrente

- Fecha: 2026-08-02
- Tipo de cambio: documental y arquitectónico
- Resultado: `recurring_billing` agregado como plugin funcional futuro número 8

> Nota vigente: ADR-0034 insertó posteriormente `vehicle_telemetry` como orden 7;
> `recurring_billing` ocupa ahora el orden 9 dentro de diecinueve reutilizables.

## Decisión confirmada

Producto solicitó incluir planes recurrentes, prorrateo y consumo medido como
dominio independiente dentro del plan de facturación masiva. Esto satisface la
condición que ADR-0031 había definido para proponer el plugin.

## Resultado

- `recurring_billing` posee planes, suscripciones, ciclos, prorrateo, consumo
  facturable, correcciones, tarificación y corridas de cargos;
- `commercial_documents` conserva lote de emisión, factura, numeración,
  idempotencia documental y correcciones;
- `sifen` conserva firma, CDC, transmisión y lotes técnicos;
- el roadmap queda en dieciocho reutilizables y `18 + N` con personalizaciones;
- el orden nuevo es 8, después de documentos y antes de SIFEN;
- la implementación no se adelanta al trabajo activo ni al orden 7.

## Artefactos

- [ADR-0033](../adr/0033-dominio-facturacion-recurrente.md);
- [análisis del dominio](../knowledge-base/commercial-documents/recurring-billing-domain-analysis.md);
- [épica](../backlog/epica-facturacion-recurrente.md);
- ADR-0031, facturación masiva, roadmap, arquitectura y guías actualizados.

## Pruebas y límites

No se modificaron Java, POM, descriptor, migraciones, Compose ni UI. No corresponde
ejecutar Maven, Docker o Playwright. Este cambio se valida mediante G0 documental;
Sprint 8 y J11-S8-C02 continúan como trabajo activo.

G0 se ejecutó con `tmp/validate_docs.py` usando el runtime Python local validado:

- 270 archivos Markdown revisados;
- 1.142 enlaces locales inventariados;
- 0 enlaces rotos;
- 0 errores de codificación UTF-8;
- 0 archivos con mojibake;
- 0 posibles secretos.
