# J11-S1-00 — Gobierno documental

- Fecha: 2026-07-23
- Estado: Completado
- Tipo: Preparación del proyecto

## Objetivo

Crear `docs/` como ubicación única de la documentación del proyecto y establecer una regla verificable para documentar todos los pasos posteriores.

## Estado inicial

- `C:\cosme\LogixoneJakarta11` existe.
- La raíz contiene `AGENTS.md`.
- La carpeta `docs/` no existía.
- El proyecto legado permanece sin modificaciones.

## Criterios de aceptación

- Existe `docs/README.md` como índice principal.
- Existen espacios definidos para ADR, arquitectura, conocimiento, backlog, Sprints, evidencias y runbooks.
- Está definido qué información debe registrar cada paso.
- El propio cambio de gobierno documental queda documentado.
- La estructura y los enlaces locales principales se validan correctamente.

## Pasos ejecutados

1. Se comprobó que el proyecto nuevo existe y que `docs/` no estaba creado.
2. Se definió la estructura documental inicial.
3. Se creó el índice y las reglas de trazabilidad.
4. Se crearon índices descriptivos para cada categoría documental.
5. Se validó la estructura preparada antes de instalarla: cinco controles correctos, nueve documentos Markdown y ningún archivo obligatorio ausente.
6. Se instaló la estructura en `C:\cosme\LogixoneJakarta11\docs`.
7. Se repitió la validación desde la ubicación definitiva con resultado correcto.

## Archivos creados

- `docs/README.md`
- `docs/adr/README.md`
- `docs/architecture/README.md`
- `docs/knowledge-base/README.md`
- `docs/backlog/README.md`
- `docs/sprints/README.md`
- `docs/sprints/sprint-01/J11-S1-00-gobierno-documental.md`
- `docs/evidence/README.md`
- `docs/runbooks/README.md`

## Validación

Validación ejecutada el 2026-07-23 mediante PowerShell y lectura UTF-8.

Comprobaciones realizadas:

- `docs/` existe como directorio.
- Los nueve documentos Markdown obligatorios existen.
- `docs/README.md` declara la fuente única de documentación.
- El índice exige trazabilidad para cada paso.
- El enlace al documento `J11-S1-00` tiene un destino existente.

Resultado: cinco de cinco controles correctos, nueve documentos encontrados y cero documentos obligatorios ausentes.

Comandos base utilizados para la comprobación:

```powershell
Test-Path -LiteralPath 'C:\cosme\LogixoneJakarta11\docs' -PathType Container
Get-ChildItem -LiteralPath 'C:\cosme\LogixoneJakarta11\docs' -Recurse -File -Filter '*.md'
Get-Content -LiteralPath 'C:\cosme\LogixoneJakarta11\docs\README.md' -Raw -Encoding UTF8
```

## Siguiente paso permitido

La tarea documental está cerrada. El siguiente paso permitido es iniciar `J11-S1-01`.
