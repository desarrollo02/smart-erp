# Evidencia J11-S10-07 — Decisión de instalador Windows

- Fecha: 2026-08-20
- Estado: decisión `NO` registrada; sin construcción ni promoción
- Historia: [J11-S10-07](../sprints/sprint-10/J11-S10-07-decision-instalador-windows.md)
- Responsable: responsable de producto

## Registro

El responsable de producto respondió explícitamente **NO** a
`¿Crearemos un nuevo instalador Windows para este Sprint?` el 2026-08-20. No se
indicó una motivación adicional. La respuesta cierra la decisión de Sprint 10 y
no autoriza cambios sobre la distribución Windows existente.

## Controles de no modificación

| Control | Resultado |
|---|---|
| Diagnóstico o preflight | no ejecutado; la respuesta `NO` no lo requiere |
| Construcción o descarga | no ejecutada |
| Promoción a `current` | no ejecutada |
| Archivos en `installer/windows/current` | 8 |
| Tamaño total conservado | 1815224 bytes |
| Edición conservada | `0.9.0-internal.1`, baseline Sprint 9 |
| EXE conservado | 104448 bytes; SHA-256 `E7E2036D130AE4D8A10E821C18B9558279E71E6E15CBA8A0323155A83E83509A` |
| Cambios Git bajo `current` | ninguno |

`installer/windows/current` no representa Sprint 10 y no debe entregarse como
instalador de este baseline. Su estado `NotSigned`, restricciones internas y
evidencia histórica de Sprint 9 permanecen sin cambios.

## Guía de estructura actualizada

La guía derivada se regeneró para que portada, estado, demo y continuidad
registren la decisión `NO`:

| Artefacto | Páginas | Bytes | SHA-256 |
|---|---:|---:|---|
| `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` | 122 | 468751 | `970020750D767758BBC1AAE26BE40DA0281B0CC44250A32380104D5E65E56C99` |

Poppler rasterizó las 122 páginas y se revisaron las cinco hojas de contacto,
portada y cierre a tamaño completo. `pypdf` confirmó texto extraíble, cero
páginas vacías, cero caracteres de reemplazo y ausencia de cifrado. La primera
revisión reveló dos referencias residuales a J11-S10-06 en el cierre; se corrigió
el generador y se repitieron autoría, rasterización y revisión completas antes de
aceptar el PDF final.

## Pendiente formal

La decisión completa J11-S10-07, pero no sustituye la validación independiente
acumulada. Sprint 10 permanece abierto únicamente por ese gate humano. No se
promueven imágenes, no se publica la guía `1.0` y no se despliega a producción.
