# J11-S1-05 — Evidencia de kernel, CDI y plugin de referencia

- Fecha: 2026-07-24
- Entorno: Windows, Java Temurin 21.0.11+10, Maven Wrapper 3.9.16, Docker Engine 29.6.2, WildFly 41
- Estado final: Verde

## Resultado funcional

- El registro neutral admite cero plugins y ordena un catálogo válido.
- Los errores conservan diagnósticos tipados y detienen el bootstrap.
- CDI descubre definiciones físicamente presentes sin introducir Jakarta en aplicación o dominio.
- WildFly registró cero plugins en la imagen predeterminada y exactamente `reference_plugin@1.0.0` en la variante con perfil.
- Ambas variantes pasaron migración, despliegue, health y HTTP 200 mediante Compose aislado.

## Pruebas por incremento

| Incremento | Comando o control | Resultado |
|---|---|---|
| Registro de aplicación | `mvnw.cmd -B -pl kernel-application -am test` | 4 pruebas nuevas; 21 acumuladas, cero fallos |
| CDI y plugin | `mvnw.cmd -B -pl kernel-infrastructure-jakarta,plugins/reference-plugin -am test` | 4 pruebas CDI y 2 del plugin; cero fallos |
| Arquitectura | `mvnw.cmd -B -pl tests/architecture-tests -am test` | 4 reglas ArchUnit, cero fallos |
| WAR ausente | build limpio sin perfil e inspección con `jar tf` | Adaptador CDI presente; JAR del plugin ausente |
| WAR presente | build limpio con `-Pwith-reference-plugin` | Un JAR del plugin y su bean CDI presentes; API Jakarta no empaquetada |
| Dockerfile | `docker buildx build --check` | Sin advertencias |
| Perfil inválido | builder con `LOGIXONE_MAVEN_PROFILE=forbidden` | Rechazado deliberadamente; paso interno terminó con código 64 |
| Gate final | `mvnw.cmd -B clean verify` | 14/14 módulos; 39 pruebas, cero fallos, errores u omitidas |

## Imágenes verificadas

| Variante | Imagen local | ID | SHA-256 del WAR |
|---|---|---|---|
| Sin plugin | `logixone/app:j11-s1-05-absent` | `sha256:a7f18d08f9fbc4115db2d56535786ec556a23a02811bf79b48bee24c6e4c30bc` | `f977882e35676a1325195185df2fed5a44b735c2a4dd2c261e591eb0182c7cb4` |
| Referencia | `logixone/app:j11-s1-05-reference` | `sha256:1efb2b718ffffc01bb14e052f8641198c06f2461fe42c47fa06a8e6a3fb00295` | `b6b9a5b5a73e214c10816799cc832bafdb65505d087696f575c8ebef4662f3da` |

Ambas imágenes ejecutan como `uid=1000(jboss)`, contienen el WAR esperado y no contienen `/workspace`. Los IDs son identidades locales de imágenes cargadas por BuildKit; no se presentan como digests publicados en un registro.

## Prueba real en WildFly y Compose

Se usaron dos proyectos aislados, `logixone-s105-absent` y `logixone-s105-reference`, con puerto HTTP asignado dinámicamente y volumen PostgreSQL propio.

### Sin plugin

- PostgreSQL: `running`, `healthy`.
- Migrador: `exited`, código `0`.
- Aplicación: `running`, `healthy`.
- Log: `event=plugin_catalog_initialized plugin_count=0 plugins=`.
- Smoke: HTTP `200`.

### Con plugin de referencia

- PostgreSQL: `running`, `healthy`.
- Migrador: `exited`, código `0`.
- Aplicación: `running`, `healthy`.
- Log: `event=plugin_catalog_initialized plugin_count=1 plugins=reference_plugin@1.0.0`.
- WildFly: `WFLYSRV0010: Deployed "logixone.war"`.
- Smoke: HTTP `200`.

Después de cada prueba se ejecutó `docker compose down --volumes --remove-orphans` con el nombre exacto del proyecto. Se eliminaron solo sus contenedores, redes y volúmenes efímeros. La inspección final encontró cero contenedores y cero volúmenes `logixone-s105` restantes. Las dos imágenes locales se conservaron como evidencia reproducible.

## Fallos y correcciones

1. Docker Engine estaba detenido. Docker Desktop se inició con autorización y el Engine 29.6.2 quedó accesible.
2. El primer arnés de inspección del JAR usó `-notmatch` sobre una colección de PowerShell y produjo un falso negativo. No había defecto en el artefacto. Se corrigió la condición para contar coincidencias y la repetición confirmó el adaptador exactamente una vez.
3. `docker stop --time` funcionó, pero informó que la bandera está obsoleta. La segunda limpieza usó `--timeout`.
4. El perfil `forbidden` falló deliberadamente. Docker reportó código general 1 y mostró que el paso interno terminó con el código 64 definido por el Dockerfile.

## Cobertura de aceptación

| Criterio | Resultado |
|---|---|
| `CA-01` a `CA-04` | Registro inmutable, vacío válido, excepción tipada y neutralidad de aplicación probados. |
| `CA-05` a `CA-07` | Adaptador CDI, observador de arranque, log seguro y plugin mínimo probados. |
| `CA-08` y `CA-09` | Builds, imágenes y runtime presente/ausente verificados. |
| `CA-10` | Cuatro reglas ArchUnit verdes. |
| `CA-11` | Maven, Docker, Compose, migrador, health y smoke verdes. |
| `CA-12` | Historia, arquitectura, ADR, runbook y evidencia actualizados. |

## Riesgos y límites

- La carpeta continúa sin metadata Git; no hay diff ni historial verificable.
- El plugin de referencia demuestra descubrimiento y contribuciones, no lógica empresarial ni persistencia.
- La activación por empresa y los endpoints semánticos pertenecen a historias posteriores.
- Docker Desktop quedó en ejecución después de completar las pruebas.

## Conclusión

`J11-S1-05` queda completada. El siguiente paso permitido es `J11-S1-06`: aplicación mínima y endpoints de salud.
