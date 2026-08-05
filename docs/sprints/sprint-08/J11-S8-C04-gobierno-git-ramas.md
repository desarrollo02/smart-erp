# J11-S8-C04 — Gobierno Git y ramas por Sprint

- Estado: Implementada y validada localmente; publicación del PR y protecciones remotas pendientes
- Fecha de decisión: 2026-08-05
- Responsable de la decisión: responsable de producto
- Sprint: 8, cierre reabierto
- Tipo: gobierno técnico y mantenimiento no destructivo del repositorio

## Objetivo

Adoptar un flujo Git liviano dirigido por Sprint que preserve el último baseline
aceptado, obligue a integrar cada historia mediante Pull Request y permita cerrar
Sprint 8 sin iniciar prematuramente `purchasing`. Detener además el crecimiento de
temporales y cachés en el índice sin borrar archivos locales ni reescribir el
historial ya publicado.

## Decisión aprobada

El flujo canónico es:

```text
story/* | fix/* | chore/* -> sprint/NN-descripcion -> main -> tag sprint-NN
hotfix/* -------------------------------------------> main -> Sprint activo
```

- `main` es la única rama permanente y recibe únicamente cierres de Sprint o
  hotfixes aprobados.
- Existe una sola rama `sprint/*` activa porque el roadmap no permite adelantar el
  Sprint siguiente para compensar pendientes del actual.
- No se mantienen `develop` ni `release/*`; agregarían líneas de integración sin
  un propietario o gate diferente.
- Las historias y correcciones usan squash merge hacia el Sprint. El Sprint usa
  merge commit hacia `main` para conservar el límite de cierre.
- Los tags `sprint-NN-rc.N` fijan candidatos inmutables y `sprint-NN` identifica el
  cierre formal. `vX.Y.Z` exige aprobación expresa de versión de producto.
- Un hotfix parte del tag productivo exacto o de `main` y después se incorpora a
  la rama de Sprint activa.

## Excepción de adopción inicial

El repositorio se inicializó y publicó el 2026-08-05 con el commit
`166c5e1bd6b86c34998cab3e7d9338d2873ab0a9`. Ese corte contiene Sprint 8 todavía
abierto, por lo que `main` no representa retrospectivamente un Sprint cerrado.
No se reescribirá la historia para simular un baseline anterior.

La adopción comienza creando `sprint/08-cierre` desde ese commit y congelando los
pushes directos a `main`. Sólo después de completar los gates pendientes se
integrará el Sprint, se creará `sprint-08` y podrá nacer
`sprint/09-purchasing` desde el nuevo `main`.

## Plan de adopción

| Orden | Acción | Resultado esperado |
|---:|---|---|
| 1 | crear `sprint/08-cierre` desde `166c5e1` | línea única para completar el Sprint abierto |
| 2 | crear CI reproducible para checks documentales, Maven y gates aplicables | nombres de checks reales antes de exigirlos |
| 3 | proteger `main` y `sprint/*` | sin push directo, force push ni borrado; PR obligatorio |
| 4 | integrar cada pendiente mediante `fix/*` o `chore/*` | un commit coherente por historia y evidencia asociada |
| 5 | corregir el seguimiento de temporales y cachés | archivos locales conservados, índice limpio y `.gitignore` actualizado |
| 6 | ejecutar la recongelación completa de Sprint 8 | gates, demo, topología, manuales, PDF, G7 e instalador resueltos |
| 7 | fusionar `sprint/08-cierre` en `main` con merge commit | baseline formal trazable |
| 8 | crear el tag anotado `sprint-08` y eliminar la rama integrada | cierre inmutable sin ramas obsoletas |
| 9 | crear `sprint/09-purchasing` desde `main` | siguiente Sprint habilitado en el orden autorizado |

## Dependencias de automatización aprobadas

El workflow inicial fija sus acciones por SHA completo y les concede únicamente
lectura de contenido:

| Acción | Versión | SHA | Licencia | Necesidad |
|---|---|---|---|---|
| `actions/checkout` | 7.0.0 | `9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0` | MIT | obtener exactamente el merge del Pull Request sin persistir credenciales |
| `actions/setup-java` | 5.6.0 | `03ad4de0992f5dab5e18fcb136590ce7c4a0ac95` | MIT | preparar Temurin `21.0.11+10`, igual al JDK validado localmente |

Las versiones y SHA se verificaron el 2026-08-05 contra los repositorios oficiales
de ambas acciones. Maven Wrapper conserva su distribución y checksum existentes;
el workflow dirige el home del Wrapper, repositorio Maven y temporales hacia
`.tools/` dentro del workspace efímero.

## Higiene del repositorio incluida

El diagnóstico inicial encontró 465 archivos rastreados bajo `tmp/`, con
aproximadamente 158,5 MiB, y dos archivos `__pycache__/*.pyc`. Son resultados de
render, hojas de contacto y cachés, no fuentes canónicas. El cambio operativo debe:

1. agregar `tmp/`, `**/__pycache__/` y `*.pyc` a `.gitignore`;
2. retirarlos únicamente del índice, conservando las copias locales;
3. mantener evidencias oficiales en `docs/evidence/` y PDF finales en
   `docs/output/pdf/`;
4. comprobar que generadores y validadores reproducen los artefactos sin depender
   de temporales versionados;
5. no ejecutar una reescritura del commit ya publicado.

Esta limpieza detiene crecimiento futuro, pero no reduce el peso del historial ya
publicado. Cualquier compactación histórica exigiría otra decisión explícita y no
forma parte de esta historia.

## Protecciones y políticas de Pull Request

### `main`

- Pull Request obligatorio;
- origen permitido: `sprint/*` o `hotfix/*`;
- rama actualizada, conversaciones resueltas y aprobación independiente;
- checks obligatorios sólo después de que sus workflows existan;
- force push y borrado deshabilitados.

### `sprint/*`

- Pull Request desde `story/*`, `fix/*` o `chore/*`;
- prueba mínima, `mvn verify` y gates adicionales según el alcance;
- evidencia y documentación en el mismo cambio;
- force push y borrado deshabilitados mientras la rama esté activa.

## Criterios de aceptación

- **CA-01:** la política coincide en `AGENTS.md`, manual técnico y plan de Sprint.
- **CA-02:** `sprint/08-cierre` parte exactamente de `166c5e1` y sigue el remoto.
- **CA-03:** `main` y `sprint/*` tienen protecciones verificadas sin checks
  inexistentes que bloqueen todo el flujo.
- **CA-04:** una historia de prueba puede abrir PR hacia el Sprint y no directamente
  hacia `main`.
- **CA-05:** el cierre sólo puede llegar a `main` desde `sprint/*` y conserva un
  merge commit identificable.
- **CA-06:** temporales y cachés dejan de estar rastreados sin borrarse localmente y
  sin reescribir historia.
- **CA-07:** los documentos canónicos y evidencias oficiales permanecen
  versionados y sus enlaces locales siguen válidos.
- **CA-08:** no existe rama de Sprint 9 antes del cierre formal y tag de Sprint 8.
- **CA-09:** se registra evidencia de configuración remota, checks, ramas, merge y
  tag sin incluir tokens ni secretos.

## Pruebas y evidencia previstas

- validación UTF-8, enlaces Markdown y búsqueda de secretos;
- `git status`, `git branch -a -vv` y `git check-ignore` sobre casos representativos;
- inspección de reglas de protección y checks en GitHub;
- ejecución del workflow en un Pull Request de adopción;
- pruebas Maven, arquitectura, Docker o Playwright únicamente cuando el cambio
  operativo afecte código, build o runtime;
- evidencia final en `docs/evidence/J11-S8-C04-gobierno-git-ramas.md`.

La ejecución local del 2026-08-05 quedó registrada en la
[evidencia de adopción](../../evidence/J11-S8-C04-gobierno-git-ramas.md). El
workflow remoto y las protecciones continúan como pendientes verificables hasta
publicar esta rama y completar el Pull Request de adopción.

## Pendientes y siguiente paso permitido

La autorización operativa se recibió el 2026-08-05. `sprint/08-cierre` fue creada
y publicada desde el commit inicial; la rama corta
`chore/J11-S8-C04-adopcion-git` contiene la adopción validada. El siguiente paso
permitido es publicar esta rama, abrir su PR hacia `sprint/08-cierre`, verificar
los checks y configurar las protecciones. No se autoriza iniciar Sprint 9.
