# Evidencia - PDF de plugins y orden de construcción

- Fecha: 2026-08-15
- Artefacto: [`00-roadmap-plugins-y-orden-construccion.pdf`](../output/pdf/00-roadmap-plugins-y-orden-construccion.pdf)
- Fuente web: [roadmap de plugins](../user-guide/roadmap-plugins-y-orden-construccion.html)
- Generador: [`generate_plugin_roadmap_pdf.ps1`](../../tools/generate_plugin_roadmap_pdf.ps1)
- Render de QA: [`render_plugin_roadmap_pdf_for_qa.ps1`](../../tools/render_plugin_roadmap_pdf_for_qa.ps1)
- Estado: verificado

## Cobertura

- 34 plugins reutilizables planificados;
- fundación compartida R0 y secuencia ERP 1-19;
- tres plugins de operaciones del proveedor;
- familia cooperativa C1-C6;
- `legacy_migration` y `business_process_management` como transversales sin
  número ERP;
- familia Flota F1 `fleet_maintenance` y F2 `automotive_workshop`, con ejecución
  técnica separada de la recepción/venta comercial;
- vertical `real_estate`, fuente legado `miaterra_master`, RE-00 y decisiones
  RE-D01 a RE-D12 antes del código;
- personalización final por empresa, fuera del conteo reutilizable;
- estado actual y Sprint 10 como siguiente paso autorizado.

## Validación ejecutada

| Comprobación | Resultado |
|---|---|
| generación reproducible | dos ejecuciones produjeron exactamente 50.229 bytes y el mismo SHA-256 |
| materialización | generador modificado copiado por hash sobre `git archive HEAD` en `.tools/tmp/validation/ADR-0048-pdf-roadmap-03` |
| reapertura | correcta con Poppler `pdfinfo` y `Windows.Data.Pdf.PdfDocument` |
| páginas | 9 |
| render visual | 9/9 páginas con Poppler a 144 dpi y 9/9 con Windows a PNG de ancho 1400 px |
| revisión visual | portada, encabezados, pies, tablas, tarjetas y llamadas sin cortes, solapamientos o páginas vacías |
| estructura PDF | encabezado `%PDF-1.4`, catálogo, árbol de páginas, `startxref` y `%%EOF` presentes |
| consistencia final | el PDF del repositorio coincide en tamaño y checksum con las dos ejecuciones deterministas |
| metadatos | título, autor, asunto, palabras clave, creador y fecha presentes |
| texto/contenido | título, metadato 34, F1 antes de F2, `real_estate`, `miaterra_master`, ADR-0048 y Sprint 10 recuperados |
| tamaño | 50.229 bytes |
| SHA-256 | `CF2D64A93954A9A0D0B45C618932003B0D7BEBF4BD004B71B21004E010E0819F` |

El PDF es un artefacto derivado. Los ADR, épicas y guías Markdown versionados
continúan siendo las fuentes canónicas del orden y del estado de implementación.
