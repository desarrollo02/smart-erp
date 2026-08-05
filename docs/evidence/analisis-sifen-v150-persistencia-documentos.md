# Evidencia — Análisis del manual SIFEN v150 para persistencia de documentos

- Fecha: 2026-07-28
- Tipo de cambio: documental y arquitectónico
- Fuente local revisada: `C:\Users\sdiaz\Desktop\Manual+Técnico+Versión+150.pdf`
- Resultado: decisión aceptada y trabajo futuro incorporado al backlog

## Identificación de la fuente

| Dato | Valor observado |
|---|---|
| Título PDF | Manual Técnico de Sistema de Facturación Electrónica Nacional |
| Autor | Equipo de Proyecto SIFEN |
| Versión interna | 150 |
| Fecha interna | 2019-09-10 |
| Páginas | 217 |
| Tamaño | 5.204.470 bytes |
| SHA-256 | `976CD88C05C31041EE86DB1667E1B426E2D9DDF675D941973D5918CBDC5427C6` |
| Cifrado | no |

El PDF original no se copió al repositorio ni fue modificado. El propio documento
advierte que puede cambiar; por eso se clasifica como referencia histórica y no
como baseline regulatorio vigente.

## Procedimiento de revisión

1. se comprobó existencia, tamaño, metadatos, cantidad de páginas y checksum;
2. se extrajo texto para localizar índice, tipos de documento, grupos, ítems,
   pagos, transporte, totales, firma y eventos;
3. se revisaron especialmente las páginas PDF 57, 59, 62, 64, 66–80, 86–87,
   103–104 y 113;
4. se renderizaron páginas seleccionadas con Poppler a imágenes y se inspeccionó
   visualmente portada, tablas de grupos y secciones de factura, notas, remisión,
   ítems, totales y eventos;
5. se comparó la estructura encontrada con las reglas de modularidad, persistencia
   e inmutabilidad del proyecto.

Poppler informó advertencias de sustitución de la fuente `Symbol`; las páginas
seleccionadas conservaron tablas y texto suficientemente legibles para este análisis
estructural. Las imágenes temporales se eliminaron después de la inspección.

## Hallazgos verificables

- el DE agrupa identidad/versión, operación, timbrado, datos generales, emisor,
  receptor, tipo específico, pagos, ítems, transporte, totales, referencias, firma
  y campos externos a firma;
- factura, nota de crédito/débito y remisión comparten estructuras, pero tienen
  grupos específicos y no usan pagos, totales o transporte del mismo modo;
- los participantes, direcciones, conceptos e importes emitidos requieren snapshots históricos;
- ítems, cuotas, referencias, actividades y datos logísticos presentan cardinalidades repetibles;
- el capítulo de eventos exige distinguir el artefacto firmado, su evolución fiscal
  y el ciclo de vida comercial interno;
- la estructura sirve para diseñar entidades y relaciones, pero copiar el XSD al
  dominio produciría acoplamiento a versión y nulabilidad excesiva.

## Resultado documental

- regla metodológica incorporada a `AGENTS.md`;
- [ADR-0010](../adr/0010-modelo-canonico-documentos-referencia-sifen.md) aceptado;
- [análisis detallado](../knowledge-base/sifen-v150-estructura-documentos.md) creado;
- [épica de documentos y SIFEN](../backlog/epica-documentos-comerciales-y-sifen.md) planificada;
- arquitectura y guía de implementación actualizadas.

## Pruebas y límites

No se modificó código ni esquema y no se ejecutaron pruebas Maven: esta decisión
solo prepara trabajo futuro. Tampoco se consultó ni certificó la versión SIFEN
oficial vigente. La primera historia de implementación deberá verificar fuentes
oficiales actuales, registrar checksums y ejecutar toda la matriz definida en
ADR-0010.

## Actualización posterior

El 2026-08-02 el análisis de facturación masiva sí verificó el portal oficial:
Manual Técnico 150 vigente, notas acumulativas hasta NT-027 y guía de envíos
asíncronos. La [evidencia posterior](analisis-facturacion-masiva.md) no certifica
una implementación y mantiene pendiente congelar por checksum todos los artefactos
aplicables al iniciar el trabajo fiscal.
