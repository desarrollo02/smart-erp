# ADR-0003 — Persistencia y migraciones

- Estado: Aceptado
- Fecha: 2026-07-23
- Historia: `J11-S1-01`

## Contexto

Los plugins necesitan persistencia sin compartir un modelo JPA global. Las migraciones deben ser reproducibles, auditables y seguras para despliegues donde una versión anterior de la aplicación pueda necesitar recuperarse.

## Decisión

### Propiedad de datos

- Se utilizará inicialmente una instancia PostgreSQL por entorno.
- El kernel será dueño del esquema `core`.
- Cada plugin persistente será dueño de un esquema `plg_<plugin_id>`, con identificadores en minúsculas y `snake_case`.
- Cada propietario mantendrá su propia unidad de persistencia y sus entidades.
- El nombre previsto de una unidad de persistencia será `logixone-<plugin-id>-pu`; la del kernel será `logixone-core-pu`.
- Las unidades podrán utilizar el mismo datasource JTA administrado por WildFly, pero no compartirán entidades ni asociaciones JPA.
- Ningún plugin ejecutará SQL directo sobre el esquema privado de otro plugin.
- Las referencias entre dominios se almacenarán como identificadores sin claves foráneas cruzadas entre esquemas de plugins.

Las restricciones se aplicarán inicialmente con estructura de módulos, ArchUnit, revisión de migraciones y pruebas de integración. Roles PostgreSQL separados por plugin se evaluarán cuando exista suficiente evidencia operativa para asumir ese coste.

### Migraciones

- Se adopta Flyway con migraciones SQL versionadas como fuente de verdad del esquema.
- Un módulo o contenedor `migrator` ejecutará las migraciones antes de iniciar o declarar lista la aplicación.
- El kernel y cada plugin tendrán ubicación de migraciones independiente y una tabla de historial en su propio esquema.
- Se utilizará una instancia lógica de Flyway por esquema con ciclo de vida independiente.
- El orden entre propietarios respetará el grafo de dependencias de plugins.
- Una migración aplicada es inmutable; la versión, descripción y checksum deben conservarse.
- Los cambios posteriores se realizan con una nueva migración hacia adelante.
- `clean` queda prohibido fuera de bases efímeras creadas específicamente para pruebas.

### JPA y creación del esquema

- JPA/Hibernate no creará ni actualizará esquemas compartidos.
- La generación automática de DDL estará deshabilitada.
- Se permite validación sin mutación después de aplicar migraciones.
- Un fallo de migración o validación impide que readiness sea exitoso.

### Cambios destructivos y recuperación

- Un cambio destructivo requiere aprobación explícita, respaldo probado y procedimiento de recuperación.
- Se preferirá el patrón expandir–migrar–contraer en varias versiones.
- Las migraciones deben mantener compatibilidad suficiente con el artefacto anterior cuando el plan de rollback dependa de volver a su digest.
- Desactivar o retirar un plugin conserva su esquema y sus datos.
- Los datos del legado se incorporarán mediante adaptadores de lectura o procesos de importación versionados; no habrá escrituras dobles sin una decisión y pruebas específicas.

## Alternativas consideradas

### Un único modelo JPA y esquema público

Se descarta porque permite relaciones y consultas cruzadas que eliminan los límites de plugins.

### Una base de datos física por plugin

Se pospone porque eleva considerablemente el coste operativo antes de demostrar una necesidad de aislamiento físico.

### Actualización automática con Hibernate

Se descarta porque no proporciona un historial de despliegue suficientemente controlado ni una estrategia segura para producción.

### Ejecutar migraciones dentro del arranque web

Se descarta porque mezcla despliegue y mutación de datos, complica réplicas concurrentes y reduce la claridad de recuperación.

## Consecuencias

- La infraestructura necesita una fase de migración anterior al servicio web.
- Cada plugin persistente debe entregar migraciones junto con su implementación.
- Las consultas que crucen dominios se resolverán mediante contratos, proyecciones o eventos, no mediante joins privados improvisados.
- La eliminación física de un plugin no equivale a eliminar su información.
- El major exacto de PostgreSQL, el driver JDBC y los digests quedan como decisión de implementación obligatoria en `J11-S1-03`.

## Verificación

- Aplicar todas las migraciones sobre PostgreSQL vacío.
- Ejecutarlas nuevamente sin cambios y comprobar que no se reaplican.
- Reiniciar aplicación y base conservando el volumen.
- Detectar cambios de checksum.
- Demostrar que JPA no crea ni actualiza tablas.
- Probar que un plugin no importa entidades ni accede al esquema de otro.

## Resolución de implementación para Sprint 1

`J11-S1-03` fijó Flyway `12.8.1` y pgJDBC `42.7.10`. El contenedor one-shot lee URL y usuario desde configuración externa y la contraseña exclusivamente desde un archivo de secreto. La primera ubicación es `classpath:db/migration/core`, el esquema predeterminado es `core` y la tabla es `core.flyway_schema_history`.

La configuración obliga `cleanDisabled=true`, `validateMigrationNaming=true`, `validateOnMigrate=true`, `failOnMissingLocations=true` y `outOfOrder=false`. La imagen ejecuta como `10001:10001` sobre el JRE Temurin 21 fijado por digest. El descubrimiento de migraciones de plugins queda diferido hasta que J11-S1-04 defina el descriptor común; no se adelanta ese contrato en la infraestructura.

## Fuente

- [Flyway versioned migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations)
- [Flyway FAQ: esquemas con ciclos de vida independientes](https://documentation.red-gate.com/flyway/reference/usage/frequently-asked-questions)
