# J11-S1-03 - Cierre formal de Docker e infraestructura como código

- Fecha de inicio: 2026-07-23
- Estado: completado
- Incremento: séptimo de `J11-S1-03`
- Historia: [Docker e infraestructura como código](../sprints/sprint-01/J11-S1-03-docker-iac.md)

## Objetivo

Auditar los 21 criterios de aceptación, completar las brechas operativas, repetir G0, G2 y G4 y cerrar `J11-S1-03` únicamente si toda la evidencia es reproducible, no contiene secretos y conserva el baseline verde.

## Auditoría inicial de criterios

| Criterio | Estado inicial | Evidencia principal |
|---|---|---|
| `CA-01` | Demostrado | [Diagnóstico](J11-S1-03-diagnostico-docker.md) y [Engine/bases](J11-S1-03-engine-imagenes-base.md) |
| `CA-02` | Demostrado | [Dockerfile multi-stage](J11-S1-03-dockerfile-multistage.md) |
| `CA-03` | Demostrado | [Migrador](J11-S1-03-migrator-one-shot.md) y [persistencia/smoke](J11-S1-03-persistencia-smoke.md) |
| `CA-04` | Demostrado | [Dockerfile multi-stage](J11-S1-03-dockerfile-multistage.md) |
| `CA-05` | Demostrado | [Dockerfile multi-stage](J11-S1-03-dockerfile-multistage.md) |
| `CA-06` | Demostrado | [Engine/bases](J11-S1-03-engine-imagenes-base.md) |
| `CA-07` | Demostrado | [Engine/bases](J11-S1-03-engine-imagenes-base.md) |
| `CA-08` | Demostrado | [Dockerfile multi-stage](J11-S1-03-dockerfile-multistage.md) |
| `CA-09` | Demostrado | [Compose estático](J11-S1-03-compose-estatico.md) y [persistencia/smoke](J11-S1-03-persistencia-smoke.md) |
| `CA-10` | Demostrado | [Persistencia/smoke](J11-S1-03-persistencia-smoke.md) |
| `CA-11` | Demostrado | [Migrador](J11-S1-03-migrator-one-shot.md) |
| `CA-12` | Demostrado | [Migrador](J11-S1-03-migrator-one-shot.md) |
| `CA-13` | Demostrado | [Migrador](J11-S1-03-migrator-one-shot.md) |
| `CA-14` | Demostrado | [Persistencia/smoke](J11-S1-03-persistencia-smoke.md) |
| `CA-15` | Demostrado | [Persistencia/smoke](J11-S1-03-persistencia-smoke.md) |
| `CA-16` | Demostrado | [Compose estático](J11-S1-03-compose-estatico.md) y [persistencia/smoke](J11-S1-03-persistencia-smoke.md) |
| `CA-17` | Demostrado | [Dockerfile multi-stage](J11-S1-03-dockerfile-multistage.md) y [persistencia/smoke](J11-S1-03-persistencia-smoke.md) |
| `CA-18` | Demostrado | [Persistencia/smoke](J11-S1-03-persistencia-smoke.md) |
| `CA-19` | Demostrado | Evidencias incrementales `J11-S1-03-*` |
| `CA-20` | Pendiente de gate final | Ejecutar G0, G2 y G4 en este incremento |
| `CA-21` | Pendiente | Completar y probar backup/restauración y recuperación básica |

## Plan de cierre

1. documentar un runbook concreto de backup y restauración PostgreSQL;
2. probar un backup lógico y restaurarlo en una base temporal sin modificar `logixone`;
3. ejecutar G2 con `mvnw.cmd clean verify` y Java 21;
4. ejecutar G4 con imágenes referenciadas por digest, migración idempotente, health y smoke;
5. ejecutar la auditoría final de seguridad, identidades y estado persistente;
6. ejecutar G0 sobre toda la documentación;
7. retirar recursos efímeros sin eliminar `logixone_postgres-data`;
8. cerrar los 21 criterios, la historia y el tablero Scrum solo si todos quedan verdes.

## Reglas de seguridad

- No leer ni registrar el contenido del secreto.
- No ejecutar `docker compose down --volumes`.
- No restaurar sobre la única base recuperable; usar una base temporal independiente.
- El backup de prueba se almacena bajo `.tools/tmp/`, se valida por tamaño y SHA-256 y se elimina al terminar.
- Cualquier fallo detiene el gate correspondiente hasta documentar y corregir su causa.

## Registro de ejecución

### Corte 1 — apertura, auditoría y runbook de recuperación

- G0 de apertura: 69 documentos Markdown, 84 enlaces locales, cero archivos UTF-8 inválidos y cero enlaces rotos.
- La auditoría mantuvo `CA-01` a `CA-19` demostrados y confirmó que las brechas de cierre eran `CA-20` y `CA-21`.
- Se añadió [Backup y restauración controlada de PostgreSQL](../runbooks/postgresql-backup-restore.md), con backup lógico, validación, restauración aislada, limpieza y reversión.
- La promoción inicial de cinco documentos se completó. El comando usado solo para presentar sus metadatos falló después porque `Get-Item` recibió una colección anidada; una verificación independiente confirmó que los cinco archivos existían y G0 quedó verde. No hubo pérdida ni alteración parcial de contenido.

El siguiente corte probará el runbook contra el volumen preservado. Los demás resultados se agregarán con comando o procedimiento, código de salida, identidad y corrección de fallos.

### Corte 2 — correcciones previas a la recuperación

1. La validación previa encontró que `infra/compose/compose.env.local` no existía. Docker 29.6.2, `linux/amd64`, el secreto local y `logixone_postgres-data` sí estaban disponibles. No se inició ningún contenedor en ese intento.
2. El primer script para copiar la plantilla local no llegó a ejecutarse por un error de sintaxis PowerShell al combinar un comando y una comparación dentro de paréntesis. Se separaron las instrucciones y la copia terminó correctamente.
3. `compose.env.local` se creó desde el ejemplo no sensible y no contiene un valor secreto. `git check-ignore` no pudo confirmar la exclusión porque esta carpeta todavía no es un repositorio Git; la regla `*.local` sí está declarada en `.gitignore`.
4. PostgreSQL arrancó saludable con el volumen preservado y la imagen fijada por digest.
5. La primera consulta de metadata falló antes del backup porque el procedimiento asumía `key/value`. El catálogo confirmó `property_key/property_value`; no se creó backup ni base temporal. El runbook se corrigió antes de repetir la prueba.

### Corte 3 — backup y restauración controlada

La repetición corregida terminó con código 0:

| Control | Resultado |
|---|---|
| Formato | backup lógico custom de `pg_dump` (`-Fc`) |
| Tamaño | 3.951 bytes |
| SHA-256 | `5DDEC46018F0786F3AA4E7719AB79FB31B93FDEDA20E161F918A9EDA948CE7E7` |
| Índice `pg_restore --list` | 23 líneas; código 0 |
| Base de restauración | `logixone_restore_probe`, separada de `logixone` |
| Metadata fuente/restaurada | coincidencia exacta; `schema_owner=core` |
| Flyway fuente/restaurado | coincidencia exacta; baseline exitoso y V1 `-1098736951`, `success=true` |
| Limpieza | base temporal ausente (`count=0`) y archivo `.tools/tmp/logixone-j11-s1-03.dump` eliminado |
| Estado principal | metadata y Flyway intactos |
| Persistencia | `logixone_postgres-data` existente |

Este corte demuestra backup, validación, restauración aislada, comprobación funcional y reversión segura de los artefactos de prueba. `CA-21` queda demostrado, sujeto a la repetición final de G0 junto con los demás gates.

### Corte 4 — G2, build limpio

Comando: `mvnw.cmd -B clean verify` con `JAVA_HOME=.tools/jdk/jdk-21.0.11+10`.

- Código de salida: 0; `BUILD SUCCESS`.
- Reactor: 14 de 14 módulos exitosos.
- Migrator: 8 pruebas, cero fallos, cero errores y cero omitidas.
- Maven Enforcer: versión de Maven, Java 21, convergencia y duplicados aprobados.
- Tiempo informado por Maven: 9,086 s.
- WAR: SHA-256 `23C935CEB30AB75CCDD72A9CF96658E6A0B63B50B4A552AE908D7B7F1488BD39`.
- Migrador ejecutable: SHA-256 `56343D2FD82E42FD53F341BC32155A7122D20C30AC342FC7C9653A778A846091`.

Ambos checksums coinciden con el sexto incremento. G2 queda verde y no se reconstruyeron imágenes Docker.

### Corte 5 — G4, recreación y persistencia

El arranque usó `--pull never` y las imágenes promovidas, sin rebuild:

- app: `logixone/app@sha256:c812ebebc6fc430d152922536bc57fe520661969e76032bbe58ea71407b77b34`;
- migrator: `logixone/migrator@sha256:8ade1b95b630bb7f7416ec52c734d44e7fedacf781846089f3e9ba1a1e643847`;
- PostgreSQL: `docker.io/library/postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0`.

El primer arranque dejó PostgreSQL y app saludables y el migrador `712b1f960f35` terminó con código 0. Flyway validó dos migraciones, confirmó versión 1 y reportó `migrations_executed=0`. `/` respondió `200` y `/logixone/` respondió `403` como smoke de transporte esperado.

La prueba de recreación produjo identidades nuevas:

| Recurso | Antes | Después |
|---|---|---|
| app | `ead8f9cd6dc1` | `bcebd295574e`, luego `953503b265b4` |
| PostgreSQL | `36467b0aca62` | `f4f5f413c870` |
| Volumen | `logixone_postgres-data` | el mismo volumen preservado |

Después de recrear PostgreSQL, el migrador one-shot terminó con código 0, `migrations_executed=0` y `schema_version=1`. El estado final siguió siendo `schema_owner=core`, baseline exitoso y V1 con checksum `-1098736951`; el smoke repitió `200/403`.

El primer arnés de smoke intentó usar `HttpClientHandler`, tipo no cargado en PowerShell 5.1, y se detuvo sin ejecutar solicitudes. Se sustituyó por `curl.exe` y el smoke pasó. Durante la recreación, otro arnés interpretó el progreso normal de `docker compose run`, escrito en stderr, como excepción PowerShell; app quedó detenida y PostgreSQL saludable. Se inspeccionó el estado y se repitieron solo migrator y app con una captura compatible; ambos terminaron verdes.

### Corte 6 — seguridad, identidad y paridad

La auditoría terminó 13 de 13:

1. Compose válido.
2. PostgreSQL/app saludables y migrator con exit 0.
3. Tres identidades de imagen exactas por digest.
4. App y migrator con usuarios runtime no root (`jboss` y `10001:10001`).
5. Tres variables sensibles y todas mediante sufijo `_FILE`.
6. Tres montajes del secreto en solo lectura.
7. Únicamente app publica puerto.
8. Binding exacto `127.0.0.1:18080`.
9. App en `backend`/`edge`; datos y migrator solo en `backend`.
10. Sin `/workspace` ni `.tools` como destino runtime.
11. Volumen `logixone_postgres-data` montado RW en `/var/lib/postgresql`.
12. Logs sin `ERROR`, `SEVERE`, `Exception` ni asignaciones `password=`.
13. Matrices development/test con los mismos digests; solo varía el puerto externo.

### Corte 7 — limpieza y G0 final

`docker compose down`, sin `--volumes`, terminó con código 0. El estado final fue:

- contenedores Compose: 0;
- redes Compose: 0;
- volumen `logixone_postgres-data`: preservado;
- backup `.tools/tmp/logixone-j11-s1-03.dump`: ausente;
- base temporal `logixone_restore_probe`: eliminada antes de la parada.

G0 sobre la documentación final contabilizó 70 archivos Markdown y 91 enlaces locales: cero UTF-8 inválidos y cero enlaces rotos. La primera medición verde posterior a la promoción encontró 91 en lugar de los 89 anticipados porque el cierre añadió dos referencias cruzadas; se corrigió este conteo y se repitió G0.

## Matriz final de aceptación

| Criterios | Resultado final |
|---|---|
| `CA-01` a `CA-08` | Cumplidos: diagnóstico, reproducibilidad, Dockerfile multi-stage, bases por digest y runtime mínimo. |
| `CA-09` a `CA-17` | Cumplidos: Compose, secreto por archivo, migrador one-shot, fallo seguro, health, persistencia, redes y puertos. |
| `CA-18` | Cumplido: promoción development/test por las mismas referencias inmutables, sin rebuild. |
| `CA-19` | Cumplido: evidencias incrementales, incidencias, correcciones, identidades y resultados reproducibles. |
| `CA-20` | Cumplido: G0, G2 y G4 verdes en el séptimo incremento. |
| `CA-21` | Cumplido: runbooks de build, arranque, diagnóstico, parada, recreación, backup y recuperación probada. |

Resultado: 21 de 21 criterios cumplidos y Definition of Done aprobada.

## Archivos creados o modificados en el séptimo incremento

- `docs/runbooks/postgresql-backup-restore.md`
- `docs/runbooks/README.md`
- `docs/runbooks/compose.md`
- `docs/evidence/J11-S1-03-cierre.md`
- `docs/evidence/README.md`
- `docs/sprints/sprint-01/J11-S1-03-docker-iac.md`
- `docs/sprints/sprint-01/README.md`
- `docs/README.md`
- `infra/compose/compose.env.local`, archivo operativo local no sensible creado desde el ejemplo y cubierto por `*.local` en `.gitignore`.

## Decisión y siguiente paso

`J11-S1-03` queda completada. El siguiente paso permitido es iniciar `J11-S1-04`, contratos de plugins y validaciones. No se adelantó trabajo de esa historia.
