# J11-S2-03 — Migración `core` V2 y evolución segura

- Fecha: 2026-07-27
- Estado: Completada
- Ambiente: Windows 11 `amd64`, Java Temurin 21.0.11+10, Maven 3.9.16, Docker Engine 29.6.2 y Compose 5.3.1 sobre `linux/amd64`
- PostgreSQL: 18.4 Bookworm fijado por digest
- Codificación: UTF-8

## Resultado

La migración aditiva V2 quedó implementada y congelada. Crea `core.company` y `core.company_plugin_activation`, conserva V1 byte por byte, mantiene todas las claves foráneas dentro de `core` y no materializa el catálogo físico de plugins como tabla maestra.

Se demostraron tres trayectorias reales en PostgreSQL 18.4:

1. una base vacía aplica V1 y V2, y una repetición aplica cero migraciones;
2. una base histórica en V1 aplica únicamente V2; la aplicación nueva pasa de readiness `503 DOWN` a `200 UP` sin reiniciarse;
3. la aplicación histórica `j11-s1-06` sigue saludable sobre una base V2, por lo que el rollback del WAR no elimina ni exige revertir datos.

## Archivos de implementación

- `pom.xml`: versión `2.0.5` de Testcontainers y BOM centralizado.
- `migrator/pom.xml`: módulos de PostgreSQL/JUnit con alcance `test` y gate explícito `postgres-integration`.
- `migrator/src/main/resources/db/migration/core/V2__add_companies_and_plugin_activation.sql`: migración V2.
- `migrator/src/test/java/py/com/logixone/migrator/CoreMigrationResourceTest.java`: identidad inmutable de V1 y estructura estática de V2.
- `migrator/src/test/java/py/com/logixone/migrator/CoreMigrationPostgreSqlIT.java`: siete escenarios contra PostgreSQL real.
- `kernel-infrastructure-jakarta/src/main/java/py/com/logixone/kernel/infrastructure/jakarta/health/DriverManagerHealthQueries.java`: readiness exige V2 aplicada.
- `kernel-infrastructure-jakarta/src/test/java/py/com/logixone/kernel/infrastructure/jakarta/health/CoreDatabaseProbeTest.java`: contrato de versión y ausencia de DML en el probe.

## Identidad de las migraciones

| Recurso | SHA-256 del archivo | Checksum Flyway observado |
|---|---|---:|
| `V1__initialize_core_schema.sql` | `07a375f06f9eb9d6e6ec162e113ada35397348bfcd03486870faf28cc424da6` | `-1098736951` |
| `V2__add_companies_and_plugin_activation.sql` | `f5186a3817f7a31569c58551a9339911b29b44f7409e47ae470fc999afa5cc11` | `-1309935940` |

`CoreMigrationResourceTest` fija explícitamente el SHA-256 histórico de V1. Las pruebas PostgreSQL alteran copias temporales, nunca los recursos versionados, y comprueban que cambiar V1 o V2 aplicada produce `FlywayValidateException`. No se ejecutó `repair` ni `Flyway.clean`.

## Modelo físico V2

`core.company` contiene UUID, estado `INACTIVE`/`ACTIVE`, `customization_plugin_id` obligatorio y único, versión optimista y marcas temporales coherentes. La unicidad expresa que cada implementación de personalización pertenece a una sola empresa; la columna única y no nula expresa que cada empresa persistida tiene exactamente una asignación.

`core.company_plugin_activation` conserva una fila por clave empresa/plugin con intención `DISABLED`/`ENABLED`, versión y marcas temporales. Desactivar actualiza la intención y no elimina la fila. Su única clave foránea apunta a `core.company` con `ON DELETE RESTRICT`; no existen claves hacia tablas `plg_*` ni hacia el catálogo físico.

Los identificadores persistidos siguen el mismo formato de `PluginId`. Las restricciones rechazan estados, identificadores y versiones inválidos, empresas inexistentes, decisiones duplicadas y personalizaciones compartidas.

## Dependencia Testcontainers

Se seleccionó Testcontainers Java `2.0.5`, versión publicada en las [fuentes oficiales](https://java.testcontainers.org/). Se usaron el [módulo PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/) y la [integración JUnit 5](https://java.testcontainers.org/test_framework_integration/junit_5/). El proyecto oficial declara licencia MIT en su [repositorio](https://github.com/testcontainers/testcontainers-java).

Todos los POM y JAR descargados quedaron en `.tools/maven-repository`; los once archivos encontrados coincidieron con el SHA-1 publicado junto al artefacto:

| Artefacto | Bytes | SHA-1 |
|---|---:|---|
| `testcontainers-2.0.5.jar` | 17789146 | `7e5013c78fd678ba8958db23c6883274019f2f2f` |
| `testcontainers-2.0.5.pom` | 2451 | `ef6c31d0b7722bf92c86716e29aeb072f0c53ec1` |
| `testcontainers-bom-2.0.5.pom` | 12236 | `0a6cdc7911641569b58d61bf9bc4ae0084dcff98` |
| `testcontainers-database-commons-2.0.5.jar` | 15029 | `bc1eeea337c461c108507f596122b3e6fdb62122` |
| `testcontainers-database-commons-2.0.5.pom` | 1547 | `09e71be65812af78d77b91f30fcc33628b767083` |
| `testcontainers-jdbc-2.0.5.jar` | 30070 | `8b93b3febba94df8a3bd67c9fde8f63c8ddf2177` |
| `testcontainers-jdbc-2.0.5.pom` | 1540 | `52ae0d093e2a30ddb4ead44a39bce690d40c507d` |
| `testcontainers-junit-jupiter-2.0.5.jar` | 14917 | `89bd1ef45ec60b17b323c747da1e6f2af6187543` |
| `testcontainers-junit-jupiter-2.0.5.pom` | 1551 | `9cae545d407dbfc914b1602490b21e69da2acf2b` |
| `testcontainers-postgresql-2.0.5.jar` | 14864 | `a7a9f748bb0e043098d3fc36d4929538ee0f6de8` |
| `testcontainers-postgresql-2.0.5.pom` | 1548 | `cd37159d5103c366445d30230fa92a0d76a52b2d` |

Testcontainers solo está en el classpath de prueba de `migrator`; no forma parte del JAR ejecutable ni del WAR. El perfil requiere opt-in porque el builder Docker ejecuta `verify` dentro de un contenedor sin anidar un daemon:

```powershell
.\mvnw.cmd -B -Ppostgres-integration `
  "-Dlogixone.postgres.integration=true" -pl migrator -am verify
```

Resultado final: 9 pruebas unitarias y 7 pruebas PostgreSQL reales, sin fallos, errores u omitidas.

## Matriz PostgreSQL real

| Escenario | Resultado |
|---|---|
| Base vacía | 2 migraciones; versión 2; segunda ejecución 0 |
| Base detenida en V1 | el migrador ejecuta únicamente V2 |
| Convergencia | firma de columnas, restricciones e índices idéntica entre instalación limpia y actualización |
| Checksum | mutar una copia de V1 o V2 falla en validación |
| Restricciones | estados, IDs, versiones y referencias inválidas rechazadas |
| Unicidad | personalización compartida y decisión empresa/plugin duplicada rechazadas |
| Desactivación | conserva fila y aumenta versión |
| Concurrencia | dos sustituciones por la misma personalización producen exactamente un ganador; no dejan escritura parcial |

Cada escenario usa una base efímera distinta dentro del contenedor fijado como:

```text
postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0
```

## Readiness y migración V1→V2

El probe del WAR solo consulta. Exige que exista V2 exitosa y que `schema_owner=core`; no contiene `INSERT`, `UPDATE` ni `DELETE`. El migrator sigue siendo el único proceso que modifica el esquema.

En el proyecto aislado `logixone-s2-03-upgrade`, puerto 18083:

1. la imagen histórica `logixone/migrator:j11-s1-03` creó solo V1;
2. la aplicación nueva arrancó deliberadamente sin ejecutar el nuevo migrador;
3. liveness respondió 200 y readiness 503, con solo `migrations=DOWN`;
4. `logixone/migrator:j11-s2-03` ejecutó una migración y dejó versión 2;
5. una segunda ejecución informó `migrations_executed=0 schema_version=2`;
6. la misma instancia de app recuperó readiness 200 y pasó 2/2 pruebas REST Assured.

El estado final mantuvo V1 con checksum `-1098736951`, agregó V2 con `-1309935940`, y no dejó filas de empresa o activación usadas por pruebas.

## Compose desde base vacía y rollback

En `logixone-s2-03-empty`, puerto 18084, el arranque completo produjo:

```text
event=migration_succeeded schema=core migrations_executed=2 schema_version=2
```

Las dos tablas existieron, `app` quedó `healthy` con cero reinicios, 2/2 pruebas REST pasaron y la repetición del migrator produjo cero cambios.

En `logixone-s2-03-rollback`, puerto 18085, el migrator nuevo llevó una base vacía a V2 y se arrancó la imagen histórica de aplicación `logixone/app:j11-s1-06`, ID `sha256:681b4f16abd47511eaa6f8b8571d6d91dd2a8da1c8d7be8b554ced9dc0fd2074`. La aplicación histórica quedó `healthy`, con cero reinicios, y pasó 2/2 pruebas REST. V1 y V2 permanecieron intactas; no se hizo downgrade de datos.

Los tres proyectos aislados, sus redes y sus volúmenes de prueba se identificaron por label antes de ejecutar `down --volumes --remove-orphans`. Después de la limpieza no quedó ningún recurso con esos nombres. No se operó sobre el proyecto Compose normal `logixone`.

## Imágenes construidas

Ambos Dockerfiles pasaron `docker buildx build --check` sin advertencias. Las imágenes fueron construidas para `linux/amd64`:

| Imagen | ID/digest local | Usuario | Artefacto interno SHA-256 |
|---|---|---|---|
| `logixone/app:j11-s2-03` | `sha256:7b37f5c236a65dcaa2899f941a3d5dede44b7d1d05b3eb6c4b2638859cc5c62e` | `jboss` | WAR `c6f8dc3406cf1ad15edeb29c64d41e6f9d2c5bd9f5f13ed7587d7a11dfc8454c` |
| `logixone/migrator:j11-s2-03` | `sha256:96dccbc278d087a2088596ead879927a85afe412d56bdd6aa90ce5379b01e86a` | `10001:10001` | JAR `6c0d3cc59fdc8a5540c582ce2113114e5062b20d4815284290e4a0026f086324` |

El build normal de la aplicación ejecutó el reactor completo sin activar Testcontainers. El gate PostgreSQL se ejecutó separadamente en el host con Docker disponible.

## Fallos encontrados y corregidos

1. Testcontainers rechazó inicialmente la referencia PostgreSQL fijada por digest porque su nombre canónico no coincidía con el esperado por el módulo. Se declaró explícitamente como sustituto compatible de `postgres`; la imagen y el digest no cambiaron.
2. Las dos primeras aserciones de conteo incluyeron la fila de creación de esquema administrada por Flyway, cuya versión es nula. Se corrigieron para contar solo migraciones versionadas con `version IS NOT NULL`; los siete escenarios reales quedaron verdes.
3. Un comando de inspección de Docker usó una plantilla con comillas mal interpretadas por PowerShell. No modificó recursos; se repitió con salida JSON antes de la limpieza destructiva.

No se omitió ni relajó ninguna prueba para obtener el resultado verde.

## Gates de cierre

| Gate | Resultado final |
|---|---|
| Recurso/migrator | 9 unitarias verdes; V1 y V2 identificadas |
| PostgreSQL real | 7/7 escenarios Testcontainers verdes |
| Infraestructura health | 13 pruebas verdes en el corte del módulo |
| Build limpio | 14/14 módulos, 85 pruebas y 5 reglas ArchUnit verdes |
| REST runtime | 2/2 verdes en actualización, base vacía y rollback |
| Docker/Compose | checks estáticos, dos imágenes y tres trayectorias aisladas verdes |
| G0 documental | 61 Markdown, 149 enlaces locales, 5 ADR indexados y 0 errores |

## Criterios de aceptación

| Criterio | Evidencia |
|---|---|
| CA-01 | SHA-256 de V1 congelado y prueba automática; no fue editada. |
| CA-02 | V2 coincide con las tablas, cardinalidades, estados y versiones de ADR-0005. |
| CA-03 | Testcontainers compara firmas de instalación limpia y V1→V2. |
| CA-04 | Testcontainers y Compose confirman cero migraciones en la repetición. |
| CA-05 | Copias mutadas de V1 y V2 fallan por checksum, sin `repair`. |
| CA-06 | PK, FK, `NOT NULL`, `UNIQUE` y `CHECK` rechazan estados huérfanos o duplicados. |
| CA-07 | Todo pertenece a `core`; no hay FK ni tabla del catálogo físico. |
| CA-08 | Siete escenarios usan PostgreSQL 18.4 efímero fijado por digest. |
| CA-09 | V1 pendiente produce readiness 503; V2 aplicada recupera 200 sin reinicio. |
| CA-10 | El WAR solo consulta; migrator es el único escritor de esquema. |
| CA-11 | Maven, Testcontainers, REST, imágenes y Compose están verdes. |
| CA-12 | Versión, MIT, fuentes, ubicación, comandos, hashes y correcciones están registrados. |
| CA-13 | `customization_plugin_id` es obligatorio sin FK al catálogo ni datos internos de descriptor. |
| CA-14 | Columna única impide compartir plugin; una empresa solo puede almacenar una asignación. |
| CA-15 | Concurrencia deja un ganador y rollback de app sobre V2 permanece saludable sin borrar datos. |

## Alcance no ejecutado

JPA/JTA y sus entidades/repositorios comienzan en `J11-S2-04`; Playwright sigue fuera de alcance porque aún no existe UI navegable. El diagnóstico empresarial de personalización ausente o incompatible continúa siendo una regla neutral ya probada en `J11-S2-02`: afecta a esa empresa, no al readiness global. Una base sin empresas es un bootstrap válido y permaneció globalmente `UP`.

## Cierre

Los 15 criterios están satisfechos. V2 queda inmutable y reproducible; `J11-S2-04` puede iniciar la persistencia JPA/JTA sobre este esquema, sin generar DDL.
