# J11-S9-08 - Evidencia del instalador Windows interno

- Fecha: 2026-08-14
- Estado: paquete creado y validado internamente; cierre formal pendiente
- Decisión de producto: `SÍ`
- Versión: `0.9.0-internal.1`
- Canal: `INTERNAL_UNSIGNED`
- Plataforma diagnosticada: Windows 11 Pro 25H2 x64, build 26200
- ADR: [ADR-0026](../adr/0026-instalador-windows-bootstrapper-nativo.md)

## Decisión registrada

El responsable de producto respondió explícitamente `SÍ` a
`¿Crearemos un nuevo instalador Windows para este Sprint?` el 2026-08-14. La
razón registrada es producir una edición interna que corresponda al baseline de
Compras congelado en J11-S9-07. Esta decisión habilitó la sustitución acotada de
`current`; no autorizó distribución externa ni instalación sobre una máquina
bloqueada.

## Baseline congelado

| Componente | Identidad exacta |
|---|---|
| Perfil físico | `with-purchasing-demo` |
| Aplicación | `logixone/app:j11-s9-07-closing` |
| Digest aplicación | `sha256:60f5de23f43e13991da30ef95be698c64f91862e38b9e75269cf13fd6d58d49a` |
| Migrador | `logixone/migrator:j11-s9-07-closing` |
| Digest migrador | `sha256:5e1d1db7de7a03451e368f60c021f341054c2b8de093a3d0f0b1c382b8e8fb95` |
| Proyecto Compose | `logixone` |
| Política de datos | `PRESERVE_VOLUMES` |

## Artefacto promovido

| Control | Resultado |
|---|---|
| Directorio | `installer/windows/current/` |
| Ejecutable | `Logixone-Setup-0.9.0-internal.1.exe` |
| Tamaño EXE | 104448 bytes |
| SHA-256 EXE | `E7E2036D130AE4D8A10E821C18B9558279E71E6E15CBA8A0323155A83E83509A` |
| Firma | `NotSigned` |
| Distribución externa | bloqueada: `externalDistributionAllowed=false` |
| Archivos | 8 declarados y 8 presentes |
| Tamaño total | 1815224 bytes |
| Payload | 1734 entradas; 1600148 bytes; SHA-256 `6AFB2C0F6F77A1908CC9FF23D776E01E0A5C157943EAD0B69536483D06FF1238` |
| `SHA256SUMS.txt` | 0 faltantes y 0 diferencias |
| Residuos en `build` | 0 |

El candidato se construyó primero en
`.tools/tmp/validation/J11-S9-08-profile/installer/windows/current`. Los ocho
hashes del candidato coincidieron con los ocho archivos promovidos. La edición
anterior `0.8.0-internal.1`, SHA-256
`E97E8C31240AA263E24E4FC86B93C92880F56CCC4CD2F52DD77528FD1F2BC37A`,
fue retirada únicamente de `current` después de la promoción satisfactoria; su
evidencia histórica y fuentes permanecen.

## Cambios de compatibilidad del constructor

La primera prueba con el manifiesto de Sprint 9 falló de forma cerrada porque el
loader exigía literalmente `J11-S8-08`. La revisión detectó además que el motor
usaba `with-inventory-demo` al reconstruir imágenes ausentes. Se corrigió para:

1. validar `J11-S<sprint>-08` contra el Sprint declarado;
2. cargar `baseline.mavenProfile` desde el manifiesto;
3. aceptar sólo un nombre de perfil seguro;
4. usar ese perfil tanto para aplicación como para migrador;
5. probar el manifiesto real de Sprint 9.

La matriz determinista pasó de 54 a 58 aserciones y terminó verde.

## Preflight real de solo lectura

| Comprobación | Resultado |
|---|---|
| Windows | PASS - Windows 11 Pro 25H2 build 26200 |
| Arquitectura | PASS - x64 |
| RAM | WARNING - 15.7 GiB; 8 mínimo, 16 recomendado |
| Disco | WARNING - 31.5 GiB; 5 para reparación, 60 recomendado |
| Virtualización/SLAT | PASS |
| WSL | PASS - 2.6.3.0 |
| Docker Engine | PASS - 29.6.2 |
| Docker Compose | PASS - 5.3.1 |
| Puertos 18080/8180 | BLOCKER - ocupados por la demo de cierre no adoptable por este perfil |
| Red/TLS | no ejecutada por `--no-network`; informada, no simulada |
| Reinicio pendiente | PASS - no detectado |
| Permisos | usuario estándar; no se solicitó UAC |
| Instalación previa | detectada |
| Resultado | `BLOQUEADA`, código 2 |

Se repitió con un destino inexistente bajo `.tools/tmp`: antes y después continuó
ausente. No se ejecutó `--execute`, no se modificó la instalación previa y no se
detuvieron contenedores ni servicios del usuario.

## Pruebas e integridad

| Control | Resultado |
|---|---|
| `build-bootstrapper.ps1 -Test` | `PREFLIGHT_TESTS_OK assertions=58` |
| `--ui-smoke` sobre `current` | `UI_SMOKE_OK`, código 0 |
| Identidad CLI | `0.9.0-internal.1`, `J11-S9-08` |
| Digests en `BUILD-INFO.json` | iguales al manifiesto y a Docker local |
| Archivos declarados/presentes | 8/8 |
| Comparación candidato/promovido | 0 diferencias |
| Entradas prohibidas en payload | 0 |
| Firma | `NotSigned` |

Las pruebas deterministas cubren compatible, advertencias, bloqueo, puertos,
Docker ausente, instalación previa, volúmenes huérfanos, consentimiento, plan
alterado, siete fases, cancelación, rechazo UAC simulado, hash inválido y ZIP
traversal. No se ejecutó una instalación real de esta edición porque el preflight
bloqueó correctamente la máquina.

## Gates automatizados del corte preparado

La materialización reproducible `.tools/tmp/validation/J11-S9-08-final/` se creó
desde el índice Git preparado, sin `.git` ni `.tools`, y fue validada mediante el
Wrapper canónico de la raíz:

| Gate | Resultado |
|---|---|
| `mvnw.cmd -B -f .../pom.xml -Pwith-purchasing-demo verify` | `BUILD SUCCESS`; 28 módulos en 02:58 |
| Surefire | 145 reportes; 535 pruebas; 0 fallos, 0 errores, 0 omitidas |
| ArchUnit | 5 reportes; 34 pruebas verdes |
| Bootstrapper desde el corte | `PREFLIGHT_TESTS_OK assertions=58` |
| UI smoke del EXE promovido | `UI_SMOKE_OK` |
| Documentación | 365 Markdown; 0 enlaces rotos, errores de codificación, mojibake o secretos |
| Integridad de `current` | 8/8 archivos; 0 faltantes, extras o hashes distintos; 0 entradas prohibidas |

Los `target/` y reportes se generaron sólo dentro de esa materialización. No se
utilizaron ni modificaron procesos, servidor de aplicaciones o toolchains del IDE
del usuario.

## PDF obligatorios

| Artefacto | Páginas | Tamaño | SHA-256 | Verificación |
|---|---:|---:|---|---|
| `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` | 112 | 444336 bytes | `4AD127ECD85A584A19FFCCEC886F8F3A06B9802AEDE6AD93E65E254F37A9D2A7` | 112 páginas renderizadas y revisadas; portada, índice, diagrama, tablas, cortes, caracteres y cierre correctos |
| `docs/output/pdf/manuales-modulos/07-manual-compras.pdf` | 15 | 280116 bytes | `F92FF8E7BE29852B5E3DC9CFD040F54950A4B9D7A8E979E7BE8A121A565F7BC4` | 15 páginas renderizadas y revisadas; portada, recorridos y cierre correctos |

Ambos PDF se reabrieron con `pypdf` y `pdfplumber`: texto extraíble en todas las
páginas, sin páginas vacías ni caracteres de reemplazo, sin cifrado, acciones de
apertura, acciones adicionales, formularios ni JavaScript. La guía de estructura
publicada coincide byte a byte con el candidato visualmente revisado.

## Pendientes bloqueantes

- VM Windows limpia compatible y VM/máquina incompatible;
- actualización real desde `0.8.0-internal.1` y reparación con datos;
- rechazo UAC, cancelación y puerto ajeno en ambientes independientes;
- instalación, migración, health y persistencia de esta edición en VM compatible;
- Authenticode válido para cualquier entrega externa;
- validación independiente G7 de la guía candidata.

Hasta resolverlos, Sprint 9 permanece abierto y el EXE sirve únicamente para
evaluación interna controlada.
