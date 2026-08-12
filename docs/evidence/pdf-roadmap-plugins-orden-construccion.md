# Evidencia - PDF de plugins y orden de construcción

- Fecha: 2026-08-12
- Artefacto: [`00-roadmap-plugins-y-orden-construccion.pdf`](../output/pdf/00-roadmap-plugins-y-orden-construccion.pdf)
- Fuente web: [roadmap de plugins](../user-guide/roadmap-plugins-y-orden-construccion.html)
- Generador: [`generate_plugin_roadmap_pdf.ps1`](../../tools/generate_plugin_roadmap_pdf.ps1)
- Render de QA: [`render_plugin_roadmap_pdf_for_qa.ps1`](../../tools/render_plugin_roadmap_pdf_for_qa.ps1)
- Estado: verificado

## Cobertura

- 33 plugins reutilizables planificados;
- fundación compartida R0 y secuencia ERP 1-19;
- tres plugins de operaciones del proveedor;
- familia cooperativa C1-C6;
- `legacy_migration` y `business_process_management` como transversales sin
  número ERP;
- familia Flota F1 `fleet_maintenance` y F2 `automotive_workshop`, con ejecución
  técnica separada de la recepción/venta comercial;
- personalización final por empresa, fuera del conteo reutilizable;
- estado actual y J11-S9-06 como siguiente paso autorizado.

## Validación ejecutada

| Comprobación | Resultado |
|---|---|
| generación reproducible | dos ejecuciones produjeron exactamente 45.334 bytes y el mismo SHA-256 |
| reapertura | correcta con `Windows.Data.Pdf.PdfDocument` |
| páginas | 8 |
| render visual | 8/8 páginas a PNG, ancho 1400 px |
| revisión visual | portada, encabezados, pies, tablas, tarjetas y llamadas sin cortes, solapamientos o páginas vacías |
| estructura PDF | encabezado `%PDF-1.4`, catálogo, árbol de páginas, `startxref` y `%%EOF` presentes |
| preservación en Git | `*.pdf binary`; blob preparado idéntico al archivo validado |
| metadatos | título, autor, asunto, palabras clave, creador y fecha presentes |
| texto/contenido | título, metadato 33, F1 antes de F2, los 33 identificadores y J11-S9-06 recuperados; ninguno falta |
| tamaño | 45.334 bytes |
| SHA-256 | `9F7A0180BA338A829AD8BD8EDD598FBABDBA8D4613F1459EF60FB052EC455AB8` |

El PDF es un artefacto derivado. Los ADR, épicas y guías Markdown versionados
continúan siendo las fuentes canónicas del orden y del estado de implementación.
