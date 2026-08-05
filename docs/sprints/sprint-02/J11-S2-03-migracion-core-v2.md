# J11-S2-03 — Migración core V2 y evolución segura

- Estado: Completada
- Fecha de inicio: 2026-07-27
- Fecha de cierre: 2026-07-27
- Dependencia: `J11-S2-02` completada y verde

## Objetivo

Evolucionar el esquema `core` para persistir empresas, decisiones de activación y la asignación exclusiva de personalización sin modificar V1, demostrando creación desde cero, actualización desde V1, idempotencia, checksums y compatibilidad con el modelo aprobado.

## Alcance

- seleccionar y centralizar la versión de Testcontainers necesaria, con fuente, licencia y checksum documentados;
- agregar una migración V2 inmutable bajo `classpath:db/migration/core`;
- crear las tablas, claves, restricciones, índices y versión de concurrencia definidos por el ADR;
- persistir la relación uno a uno entre empresa y `PluginId` de personalización, sin convertir el catálogo físico en una tabla maestra;
- mantener claves foráneas únicamente dentro de `core`;
- actualizar la validación de readiness para exigir la versión esperada sin ejecutar migraciones desde el WAR;
- ejecutar pruebas SQL/Flyway sobre PostgreSQL real mediante Testcontainers;
- probar también el migrador one-shot y Compose sobre una base V1 preservada.

## Dependencia de prueba seleccionada

- Testcontainers Java `2.0.5`, centralizado mediante `org.testcontainers:testcontainers-bom`.
- Módulos `testcontainers-junit-jupiter` y `testcontainers-postgresql`, ambos con alcance `test` únicamente en `migrator`.
- Licencia MIT y fuente oficial `testcontainers/testcontainers-java`.
- Motivo: ejecutar Flyway y las restricciones SQL contra PostgreSQL real, efímero y aislado; no forma parte del WAR ni del migrator ejecutable.
- Gate explícito: `.\mvnw.cmd -B -Ppostgres-integration -Dlogixone.postgres.integration=true -pl migrator -am verify`; el perfil evita exigir un daemon Docker dentro de los stages de construcción de imágenes.

## Escenarios obligatorios

1. base vacía aplica V1 y V2 en orden;
2. base con V1 aplica únicamente V2;
3. segunda ejecución aplica cero migraciones;
4. V1 conserva su checksum histórico;
5. V2 alterada después de aplicarse falla por checksum;
6. restricciones rechazan empresa o activación inválida;
7. duplicar una decisión empresa/plugin falla de forma controlada;
8. desactivar conserva la fila y los datos asociados;
9. readiness queda `DOWN` con V2 pendiente y `UP` después del migrador;
10. ningún test usa `Flyway.clean` sobre una base no efímera.
11. una empresa no puede tener dos personalizaciones asignadas;
12. un mismo `PluginId` de personalización no puede asignarse a dos empresas;
13. el reemplazo concurrente no deja asignaciones duplicadas ni parciales;
14. readiness y diagnóstico ante asignación ausente o JAR incompatible cumplen exactamente la decisión de `J11-S2-01`.

## Fuera de alcance

- entidades y repositorios JPA;
- datos de usuarios o roles;
- migraciones de esquemas `plg_*`;
- borrado o transformación destructiva de datos V1;
- reparación automática de checksum.

## Criterios de aceptación

- **CA-01:** V1 permanece byte por byte sin cambios.
- **CA-02:** V2 refleja exactamente el modelo y restricciones aceptados en `J11-S2-01`.
- **CA-03:** base vacía y actualización desde V1 terminan en el mismo esquema lógico.
- **CA-04:** reejecución aplica cero migraciones y conserva checksums.
- **CA-05:** alteración controlada de V1 o V2 falla sin `repair` ni filtración de secretos.
- **CA-06:** unicidad y claves internas impiden estados duplicados o huérfanos.
- **CA-07:** SQL y nombres respetan propiedad exclusiva del esquema `core`.
- **CA-08:** Testcontainers usa PostgreSQL real y recursos efímeros aislados.
- **CA-09:** readiness exige la última versión esperada sin mutar la base.
- **CA-10:** migrator sigue siendo el único proceso que aplica cambios.
- **CA-11:** pruebas de migrator, integración, Compose y persistencia están verdes.
- **CA-12:** evidencia registra versión, licencia, descarga bajo `.tools`, comandos y checksums.
- **CA-13:** V2 representa la asignación obligatoria sin almacenar datos internos del descriptor físico.
- **CA-14:** restricciones de base garantizan unicidad tanto por empresa como por plugin de personalización.
- **CA-15:** actualización, rollback y concurrencia no pueden dejar una sustitución parcial.

## Gates

1. prueba del recurso y configuración del migrator;
2. integración Testcontainers en PostgreSQL real;
3. build del migrator;
4. Compose V1→V2 y base vacía;
5. health negativo/positivo;
6. `mvnw.cmd -B verify`.

## Resultado

- V1 permanece inmutable con SHA-256 `07a375f06f9eb9d6e6ec162e113ada35397348bfcd03486870faf28cc424da6` y checksum Flyway `-1098736951`.
- V2 crea `core.company` y `core.company_plugin_activation`; quedó congelada con SHA-256 `f5186a3817f7a31569c58551a9339911b29b44f7409e47ae470fc999afa5cc11` y checksum Flyway `-1309935940`.
- Nueve pruebas unitarias y siete escenarios Testcontainers sobre PostgreSQL 18.4 quedaron verdes.
- Base vacía, V1→V2, idempotencia, checksum, restricciones, concurrencia, readiness y rollback de aplicación fueron verificados en entornos aislados.
- El build limpio terminó con 14/14 módulos, 85 pruebas y 5 reglas ArchUnit verdes.
- Los 15 criterios de aceptación están cumplidos. La evidencia reproducible está en [J11-S2-03 — Migración `core` V2](../../evidence/J11-S2-03-migracion-core-v2.md).

## Siguiente historia permitida

`J11-S2-04` queda habilitada: V2 está inmutable, reproducible y verde.
