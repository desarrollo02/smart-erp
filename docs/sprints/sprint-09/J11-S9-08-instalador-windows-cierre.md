# J11-S9-08 - Instalador Windows y último gate técnico

- Estado: Implementada y validada internamente; cierre formal pendiente de G7,
  Authenticode y matriz Windows independiente
- Sprint: 9
- Fecha: 2026-08-14
- Tipo: distribución, diagnóstico, instalación y cierre
- Épica: [Instalador Windows reproducible](../../backlog/epica-instalador-windows-reproducible.md)
- Procedimiento: [metodología de cierre](../../runbooks/metodologia-instalador-windows-cierre-sprint.md)

## Decisión de producto

- Pregunta: `¿Crearemos un nuevo instalador Windows para este Sprint?`
- Respuesta: **SÍ**.
- Fecha: 2026-08-14.
- Responsable: responsable de producto, mediante respuesta explícita en la tarea
  de cierre asistida por Codex.
- Razón: disponer de una edición instalable interna que represente exactamente el
  baseline de Compras congelado en J11-S9-07.

## Objetivo

Regenerar el bootstrapper Windows interno después de completar G0–G6 de Sprint 9,
ligarlo a las imágenes verificadas de Compras y sustituir únicamente los ocho
derivados declarados de `installer/windows/current`.

## Baseline congelado

| Componente | Identidad exacta |
|---|---|
| Perfil Maven | `with-purchasing-demo` |
| Aplicación | `logixone/app:j11-s9-07-closing` |
| Digest aplicación | `sha256:60f5de23f43e13991da30ef95be698c64f91862e38b9e75269cf13fd6d58d49a` |
| Migrador | `logixone/migrator:j11-s9-07-closing` |
| Digest migrador | `sha256:5e1d1db7de7a03451e368f60c021f341054c2b8de093a3d0f0b1c382b8e8fb95` |
| Política de datos | `PRESERVE_VOLUMES` |

## Resultado

La edición `0.9.0-internal.1` se construyó desde una materialización del índice,
verificó ambos digests locales y promovió ocho archivos sin residuos en `build`.
La adaptación eliminó dos acoplamientos del Sprint anterior: la historia de cierre
se valida contra el Sprint declarado y el perfil Maven se lee del manifiesto para
reconstruir aplicación y migrador de forma coherente.

El preflight real clasificó esta máquina como `BLOQUEADA` porque la demo de cierre
ocupaba 18080 y 8180. No escribió el destino, no solicitó UAC y no se ejecutó la
instalación. Compilación, 58 aserciones deterministas, smoke de UI, integridad del
payload y promoción quedaron verdes.

## Estado de aceptación

| Alcance | Resultado |
|---|---|
| Identidad, baseline, manifiesto y digests | Verde |
| Preflight previo a cambios/UAC | Verde; bloqueo comprensible y sin escritura |
| Pruebas deterministas de consentimiento, UAC, cancelación y paquete | Verde |
| `current`, hashes, payload, terceros y restricción interna | Verde |
| Instalación/reparación local de esta edición | No ejecutada: máquina bloqueada |
| VM limpia, incompatible y actualización desde edición anterior | Pendiente independiente |
| Authenticode | `NotSigned`; distribución externa bloqueada |
| G7 independiente | Pendiente |

## Cierre

J11-S9-08 creó correctamente el instalador solicitado, pero no cierra Sprint 9.
La edición es sólo `INTERNAL_UNSIGNED` y no puede entregarse a una empresa. El
dictamen independiente G7, Authenticode y la matriz Windows real conservan el
estado abierto del Sprint.

La evidencia detallada está en
[J11-S9-08](../../evidence/J11-S9-08-instalador-windows-cierre.md) y el recorrido
seguro en el [runbook de demo](../../runbooks/demo-instalador-windows-sprint-09.md).
