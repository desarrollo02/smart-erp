# Épica — Documentos comerciales canónicos e integración SIFEN

- Estado: Planificada
- Fecha de incorporación: 2026-07-28
- Prioridad actual: posterior al cierre operativo del kernel, salvo repriorización explícita
- Decisiones relacionadas:
  [ADR-0010](../adr/0010-modelo-canonico-documentos-referencia-sifen.md) y
  [ADR-0031](../adr/0031-facturacion-masiva-en-documentos-comerciales.md)

## Objetivo

Implementar factura, nota de crédito, nota de débito, nota de remisión y sus
relaciones mediante un modelo comercial canónico, persistente y auditable, usando
la estructura de SIFEN para descubrir datos y cardinalidades sin convertir una
versión del XML fiscal en el dominio interno del ERP.

## Alcance funcional inicial

- cabecera común del documento y numeración controlada;
- snapshots históricos de emisor, receptor, direcciones y conceptos;
- ítems, ajustes, impuestos, pagos, crédito, cuotas y totales;
- extensiones tipadas para factura, nota de crédito/débito y remisión;
- referencias entre documentos y ciclos correctivos;
- transporte, origen, destino, vehículos y transportistas cuando corresponda;
- estados comercial, fiscal y logístico independientes;
- preparación, aprobación y ejecución idempotente de lotes de facturas, con
  resultado y recuperación por documento;
- adaptador SIFEN versionado, artefactos firmados y eventos fiscales inmutables.

## Invariantes

1. El modelo canónico no usa nombres de nodos XML como lenguaje ubicuo del ERP.
2. Los documentos emitidos conservan snapshots y no cambian al editar maestros.
3. XML/JSON puede conservarse como evidencia, pero no es la única persistencia operativa.
4. No se usa una tabla universal con cientos de columnas opcionales ni EAV.
5. CDC, timbrado y numeración no reemplazan el UUID interno; los formatos que
   contienen ceros significativos se conservan como texto canónico.
6. Facturación, notas, remisiones y adaptación fiscal pertenecen a plugins
   funcionales, nunca al kernel.
7. Los límites entre plugins usan contratos e identificadores públicos, no
   entidades JPA ni lectura de tablas privadas.
8. Antes de implementar se verifican el manual, XSD, catálogos y servicios SIFEN
   oficiales vigentes, con versión y checksum registrados.
9. El lote comercial que origina facturas no comparte identidad, estado ni
   reintentos con los lotes técnicos de transmisión SIFEN.
10. Repetir una solicitud o reanudar después de una caída no puede duplicar una
    factura ni su numeración.

## Fuera de alcance de esta incorporación

Esta épica no implementa todavía tablas, entidades, pantallas, cálculos, firma,
transmisión ni certificación SIFEN. El manual v150 entregado es una fuente histórica
para planificar la persistencia; no demuestra cumplimiento fiscal vigente.

## Criterios de aceptación de la futura épica

- requisitos y casos de uso caracterizados por cada tipo de documento;
- agregado canónico, límites de plugin y contratos aprobados mediante ADR o adenda;
- migraciones versionadas y restricciones verificadas sobre PostgreSQL;
- precisión, redondeo, numeración, concurrencia, inmutabilidad y relaciones documentales probados;
- mapeo contra la versión SIFEN vigente probado de extremo a extremo;
- XML firmado, envíos, respuestas y eventos recuperables y auditables;
- autorización por empresa y permisos aplicada en servicios y UI;
- facturación masiva probada con idempotencia, concurrencia, éxito parcial,
  reinicio, reintentos y numeración atómica;
- documentación para implementadores, operación, respaldo, rollback y retención
  actualizada en el mismo incremento.

## Preparación disponible

El relevamiento inicial se conserva en
[SIFEN v150 como referencia estructural](../knowledge-base/sifen-v150-estructura-documentos.md).
La capacidad de volumen se desarrolla en la
[épica de facturación masiva](epica-facturacion-masiva.md) y permanece dentro de
`commercial_documents`.
