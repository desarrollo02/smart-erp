# Evidencia — Análisis de facturación masiva

- Fecha: 2026-08-02
- Tipo de cambio: documental y arquitectónico
- Resultado: capacidad asignada a `commercial_documents`; no se creó un plugin
  adicional ni se inició código

> Nota vigente: esta evidencia conserva el primer corte. Producto confirmó después
> planes, prorrateo y consumo medido; ADR-0033 agregó `recurring_billing` sin
> transferirle la factura ni el lote de emisión.

## Fuentes locales revisadas

| Fuente | Commit limpio | Elementos relevantes |
|---|---|---|
| `C:\cosme\multienvios\miaterra` | `55a56963f00329edd2da57b53a1a94da129cc819` | generación de prefacturas, filtro por período, agrupación, selección y facturación por lote |
| `C:\cosme\felsina\ingeniolafelsina` | `412b3cd978757b1b8a389f2007060a90f5c7322b` | planilla salarial que origina un comprobante de compra mediante acoplamiento directo |

Las fuentes se consultaron en modo solo lectura y conservaron `git status --short`
vacío. No se copiaron clases ni pantallas.

## Archivos caracterizados

- Multienvíos:
  `TswGenerarPreFacturasControlador.java`, `TswGenerarPreFacturasEJB.java`,
  `VtwCoFacturacionMasivaControlador.java`, `TswGenerarPreFacturas.xhtml` y sus
  fragmentos de filtros, grillas y diálogos;
- Ingenio La Felsina:
  `RhwPlanillaSalarioControlador.java` y las acciones de generación expuestas en
  `RhwPlanillaSalario.xhtml`.

## Fuente oficial vigente revisada

Se consultó el portal oficial e-Kuatia/DNIT el 2026-08-02. La página de
documentación declara Manual Técnico versión 150 y notas técnicas 001–027. La Nota
Técnica 27 está fechada 2026-03-09. La guía de mejores prácticas de octubre de 2024
documenta el envío asíncrono de hasta 50 DE por lote, mismo RUC y tipo documental,
mensaje comprimido máximo de 1.000 KB, protocolo de recepción y consulta posterior.

- [Documentación técnica](https://ekuatia.set.gov.py/web/e-kuatia/documentacion-tecnica)
- [Guía oficial de envío de DE](https://ekuatia.set.gov.py/documents/20123/420592/Gu%C3%ADa%2Bde%2BMejores%2BPr%C3%A1cticas%2Bpara%2Bla%2BGesti%C3%B3n%2Bdel%2BEnv%C3%ADo%2Bde%2BDE.pdf/38fe5830-98c0-2241-9895-671f86f1225f?t=1729866823709)
- [Nota Técnica 27](https://ekuatia.set.gov.py/documents/20123/420595/NT_E_KUATIA_027_MT_V150.pdf/e5376c97-64cf-3fe0-e962-b6f22c8c207a?t=1773076266295)

No se descargaron artefactos regulatorios ni se afirmó certificación. La historia
de implementación deberá almacenarlos dentro de `.tools/`, verificar checksums y
registrar las versiones efectivamente probadas.

## Hallazgos

- el período facturado y la fecha común de emisión son datos diferentes;
- vista previa, edición previa y agrupación por responsable de facturación son
  necesidades reales;
- cada factura necesita resultado e idempotencia propios;
- el bucle de UI, `MAX + 1`, impuesto embebido y dependencias directas observados no
  son patrones trasladables;
- un dominio distinto puede originar cargos, pero debe hacerlo mediante contratos;
- un lote comercial puede generar muchos lotes fiscales y no comparte su estado;
- el caso inicial no demuestra un dominio suficiente para crear `bulk_billing` o
  `recurring_billing`; la decisión posterior de producto aporta los requisitos
  autónomos que faltaban para el segundo.

## Resultado documental

- [ADR-0031](../adr/0031-facturacion-masiva-en-documentos-comerciales.md) aceptado;
- [caracterización](../knowledge-base/commercial-documents/facturacion-masiva-legacy-characterization.md) creada;
- [épica](../backlog/epica-facturacion-masiva.md) planificada;
- ADR-0010, roadmap, arquitectura y guías continúan como fuentes rectoras;
- el orden de plugins no cambia.

## Pruebas y límites

No se modificaron Java, POM, descriptores, migraciones, Compose ni UI ejecutable.
Por ello no corresponde Maven, Docker ni Playwright. G0 se ejecutó al terminar el
cambio sobre 262 archivos Markdown y 1.070 enlaces
locales: cero enlaces rotos, errores de codificación UTF-8, patrones de texto
corrupto o coincidencias con secretos locales. Sprint 8 y J11-S8-C02 siguen siendo
el trabajo activo; facturación masiva permanece planificada.
