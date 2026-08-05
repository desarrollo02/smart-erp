# J11-S8-C04 — Evidencia de gobierno Git y ramas por Sprint

- Fecha: 2026-08-05
- Rama de trabajo: `chore/J11-S8-C04-adopcion-git`
- Rama de integración: `sprint/08-cierre`
- Baseline de adopción: `166c5e1bd6b86c34998cab3e7d9338d2873ab0a9`
- Estado: rama publicada y validación local verde; PR, checks remotos y protecciones pendientes

## Resultado local

- `main` permanece congelada en el commit inicial publicado.
- `sprint/08-cierre` se creó exactamente desde ese commit, se publicó y sigue a
  `origin/sprint/08-cierre`.
- La adopción se implementó en la rama corta
  `chore/J11-S8-C04-adopcion-git`, publicada en `origin` con el commit inicial de
  adopción `0af3ade`.
- No existe ni se creó una rama de Sprint 9.

## Higiene no destructiva

El índice contenía 465 archivos bajo `tmp/` y un bytecode bajo
`tools/__pycache__/`: 466 entradas generadas en total. Se retiraron sólo del
índice y se agregaron `tmp/`, `**/__pycache__/` y `*.pyc` a `.gitignore`.

Las 465 copias locales de `tmp/`, aproximadamente 158,5 MiB, se comprobaron antes
y después de la operación y permanecieron físicamente en el equipo. Los PDF y
evidencias canónicos de `docs/` siguen versionados; no se reescribió historia.

## Automatización preparada

`.github/workflows/pull-request-validation.yml` valida Pull Requests hacia
`main` y `sprint/**` mediante dos jobs:

1. `governance`: dirección permitida de ramas, documentación, temporales
   rastreados, whitespace y búsqueda conservadora de secretos;
2. `maven-verify`: Temurin `21.0.11+10` y `mvnw -B verify` con cachés y temporales
   dentro de `.tools/`.

Las acciones están fijadas por SHA completo y el workflow sólo concede
`contents: read`. La ejecución remota se registrará después de abrir el PR.

## Hallazgos corregidos durante la validación

### Fin de línea de migraciones SQL

Una materialización limpia para Windows convirtió migraciones SQL a CRLF y expuso
seis fallos de checksum en `CoreMigrationResourceTest`. Se agregó
`*.sql text eol=lf` a `.gitattributes`. La comparación posterior confirmó 6/6
checksums idénticos entre el índice y una exportación limpia; la prueba focal
quedó 6/6 verde.

### Movimiento atómico del generador de plugins

Windows produjo un `AccessDeniedException` transitorio al promover el directorio
de staging del generador. `PluginScaffoldGenerator` ahora conserva el movimiento
atómico preferido, retrocede al movimiento normal y reintenta de forma acotada
sólo el bloqueo transitorio, sin reemplazar un destino existente. Dos ejecuciones
consecutivas del módulo quedaron verdes: 22 pruebas de `plugin-api` y 9 de
`plugin-scaffold` en cada ejecución.

## Pruebas ejecutadas

| Prueba | Resultado |
|---|---|
| comparación SHA-256 de seis migraciones `core` en exportación limpia | 6/6 idénticas |
| `CoreMigrationResourceTest` en exportación limpia | 6/6 verdes |
| `mvnw.cmd -B -f <export>/pom.xml -pl tools/plugin-scaffold -am test`, ejecución 1 | 31/31 verdes |
| mismo comando, ejecución 2 | 31/31 verdes |
| `mvnw.cmd -B -f <export>/pom.xml clean verify` | 26/26 módulos, 478 pruebas, cero fallos, errores u omitidas; `BUILD SUCCESS` en 4:33 |
| `tools/validate_docs.py` | 298 Markdown, cero enlaces rotos, errores UTF-8, mojibake o filtraciones de secretos |

La exportación se ubicó temporalmente bajo `.tools/tmp/`, conforme a la política
del proyecto. Se utilizó porque IntelliJ mantenía abiertos directorios `target`
del árbol principal; el resultado verifica exactamente el contenido preparado en
el índice sin depender de esos artefactos locales.

## Publicación y pendiente remoto

- `chore/J11-S8-C04-adopcion-git` quedó publicada y sigue su rama homónima de
  `origin`;
- abrir el [PR hacia `sprint/08-cierre`](https://github.com/desarrollo02/smart-erp/compare/sprint%2F08-cierre...chore%2FJ11-S8-C04-adopcion-git?expand=1);
- comprobar los dos jobs del workflow en GitHub;
- proteger `main` y `sprint/*` sin force push ni borrado, con PR obligatorio;
- registrar URL, resultado de checks y configuración observada.

La automatización intentó abrir la comparación mediante el único navegador
integrado disponible, pero su webview no llegó a adjuntarse y no existía una
sesión alternativa. No se reutilizaron ni extrajeron credenciales Git para llamar
la API de GitHub por otro canal. La rama publicada permanece recuperable y el
enlace anterior deja la operación remota preparada.

Sprint 8 continúa abierto. Esta evidencia no autoriza crear Sprint 9, integrar el
cierre en `main` ni crear el tag `sprint-08` antes de completar los demás gates.
