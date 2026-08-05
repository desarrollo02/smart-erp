# J11-S1-01 — Validación documental y arquitectónica

- Fecha: 2026-07-23
- Estado: Completado
- Ambiente: Windows, PowerShell, staging local controlado y ruta definitiva
- Alcance: documentación y coherencia arquitectónica

## Elementos validados

- Apertura trazable de la historia y secuencia del Sprint.
- Cuatro ADR aceptados y estructurados.
- Baseline exacto de plataforma.
- Neutralidad del API de plugins.
- Propiedad de datos por plugin.
- Promoción de imágenes por digest.
- Vista general de arquitectura y dependencias permitidas.
- Matriz de gates G0 a G5.
- Enlaces Markdown locales.

## Resultados por corte

| Corte | Controles | Resultado |
|---|---:|---|
| Apertura de historia | 6 | 6 correctos |
| ADR | 7 | 7 correctos |
| Arquitectura y estrategia de pruebas | 9 | 9 correctos |
| Enlaces locales en staging | 17 documentos revisados | 0 enlaces rotos |
| Integración previa a instalación | 11 | 11 correctos sobre 18 documentos |
| Validación integrada en ruta definitiva | 11 | 11 correctos |
| Cierre formal, primer intento | 12 | 11 correctos y 1 aserción de prueba incorrecta |
| Cierre formal corregido | 12 | 12 correctos |

Se instalaron 13 archivos nuevos o actualizados. Sus hashes SHA-256 en destino coincidieron con el staging validado; no hubo diferencias.

## Procedimiento reproducible

La validación documental utiliza lectura UTF-8, `Test-Path` y búsqueda de enlaces Markdown relativos. Las comprobaciones de contenido verifican estados, secciones obligatorias y decisiones críticas.

Comandos base:

```powershell
Get-ChildItem -LiteralPath '<raiz>\docs' -Recurse -File -Filter '*.md'
Get-Content -LiteralPath '<documento>' -Raw -Encoding UTF8
Test-Path -LiteralPath '<destino-de-enlace>'
```

Para cada enlace local se resuelve una ruta absoluta a partir del directorio del documento y se exige que el destino exista.

## Pruebas no ejecutadas

No se ejecutaron Maven, Java, Docker, Compose ni pruebas de base de datos porque `J11-S1-01` no crea todavía build, código o infraestructura. Sus gates comienzan en las historias `J11-S1-02` y `J11-S1-03`.

## Fallos

El primer gate de cierre formal falló en `ArchitectureModulesDefined`. La prueba buscaba literalmente `` `reference-plugin` ``, mientras que el documento representa el módulo correctamente como `reference-plugin/` dentro del árbol de directorios. No existía un defecto arquitectónico ni un enlace roto.

Se corrigió la aserción para buscar la ruta realmente documentada y se repitieron los doce controles, no solamente el control fallido. Resultado final: 12/12 correctos, 18 documentos, cero enlaces rotos y cero diferencias de hash.

## Conclusión

`J11-S1-01` cumple sus criterios documentales y arquitectónicos. Queda habilitado `J11-S1-02`; todavía no están habilitadas las historias de Docker, contratos de plugins ni runtime.
