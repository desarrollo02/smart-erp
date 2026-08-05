# Evidencia J11-S5-01 - Composición única y migraciones de plugins

- Fecha: 2026-07-29
- Estado: Completada; unitarias, arquitectura, PostgreSQL, imágenes y Compose verdes
- ADR: [ADR-0012](../adr/0012-composicion-unica-y-migraciones-de-plugins.md)

## Resultado implementado

- `logixone-plugin-set` centraliza los perfiles físicos.
- WAR y migrador dependen del mismo set.
- El migrador descubre `PluginDefinition` mediante `ServiceLoader`, valida el
  catálogo con `PluginRegistry` y ejecuta `core` seguido de plugins en orden
  topológico.
- Cada plugin solo puede migrar `PluginId.schemaName()` y conserva un historial
  Flyway independiente.
- Los tres fixtures registran proveedor SPI; `reference-plugin` aporta una
  migración SQL técnica en `plg_reference_plugin`.
- El Dockerfile del migrador acepta los mismos tres perfiles cerrados que el de la
  aplicación.

## Validaciones ejecutadas

| Control | Resultado |
|---|---|
| Build inicial con Java global | bloqueado correctamente por Enforcer: Java 8 no cumple `[21,22)` |
| Build base con JDK 21 local | 10/10 módulos verdes; 159 pruebas ascendentes en el alcance |
| Migrator + reference-plugin | 18 pruebas de migrator y 2 del fixture verdes; módulos ascendentes verdes |
| Descubrimiento con `with-reference-plugin` | verde; `ServiceLoader` construyó un plan válido sin Jakarta runtime |
| Composición `with-screen-customization-plugins` | 13/13 módulos empaquetados |
| WAR de composición A/B | exactamente 3 JAR: funcional y personalizaciones A/B |
| Migrador sombreado A/B | exactamente 3 proveedores `PluginDefinition`; 0 entradas `jakarta/` |
| ArchUnit | 10 reglas de límites + 2 pruebas de composición, todas verdes |
| `mvn verify` base | 17/17 módulos y 181 pruebas verdes |
| Restauración base | WAR con 0 plugins; migrador con 0 proveedores y 0 entradas `jakarta/` |
| PostgreSQL/Testcontainers | 12 escenarios, 0 fallos, 0 errores, 0 omitidos |
| `docker buildx --check` | aplicación y migrador sin advertencias |
| Compose con personalizaciones A/B | PostgreSQL, Keycloak y aplicación saludables; migrador terminó 0 |
| Idempotencia en Compose | `core` V5 y `plg_reference_plugin` V1 aplicaron 0 cambios en reejecución |
| Recreación de PostgreSQL | mismo volumen y marcador técnico conservado |
| Retirada física | aplicación base con `plugin_count=0`; esquema, historial y marcador del plugin conservados |

Dos aserciones iniciales esperaban `IllegalArgumentException`; las reglas existentes
del kernel devolvieron el diagnóstico más específico `InvalidPluginCatalogException`
para esquema ajeno y ubicación duplicada. Se corrigieron las aserciones y el mismo
gate quedó verde. Una inserción inicial de `MigrationContribution` ocupó por error
la posición de dependencias del constructor; el compilador lo rechazó, se corrigió
el orden y se repitió el gate verde.

## Artefactos inspeccionados

La variante A/B fue empaquetada con pruebas omitidas únicamente para inspección de
composición, después de ejecutar las unitarias por separado:

| Artefacto | Tamaño | SHA-256 |
|---|---:|---|
| `distribution/logixone-war/target/logixone.war` | 611247 bytes | `6067982B4B1C52431CA855921ED4C70A9F45FB7FCDC3D9E426AEDE15BA0065D9` |
| `migrator/target/migrator-0.1.0-SNAPSHOT-executable.jar` | 5257643 bytes | `3904332720C24DB98A72B9A06A361088E1FE9A87F4A24E3C26EABF8285006508` |

Estos hashes identifican artefactos locales de diagnóstico, no una pareja aprobada
para promoción.

La variante base restaurada quedó identificada así:

| Artefacto base | Tamaño | SHA-256 |
|---|---:|---|
| `distribution/logixone-war/target/logixone.war` | 603811 bytes | `BC9C506E5FC7E2FFED00FD69DF654B6C2C359C2E53CE3F8EF6D519DFE6A294B1` |
| `migrator/target/migrator-0.1.0-SNAPSHOT-executable.jar` | 5250395 bytes | `DB268ECDCAA3DB2095802D8471084432F17FC0346900A654A6EF8D62D6BA464E` |

## PostgreSQL y Testcontainers

El motor Docker 29.6.2 estuvo disponible al reanudar el gate. Se ejecutó:

```powershell
.\mvnw.cmd -B -Ppostgres-integration `
  "-Dlogixone.postgres.integration=true" -pl migrator -am verify
```

`CoreMigrationPostgreSqlIT` terminó con 12 pruebas verdes. El nuevo escenario creó
`core` primero, migró el esquema técnico del plugin y confirmó que la segunda
ejecución no reaplica migraciones. Los casos existentes conservaron validación de
checksum, bases vacías/anteriores y restricciones.

## Imágenes verificadas

Aplicación y migrador se construyeron desde el mismo baseline y perfil. Cada etapa
builder ejecutó sus pruebas dentro de Linux.

| Variante | Imagen | Tamaño | ID OCI local |
|---|---|---:|---|
| A/B | `logixone/app:j11-s5-01-customized` | 500126463 bytes | `sha256:29ea12fe2dc1be0063844450f3cf01a8d1d5c0b62a40633b692b05f21d1bfa6b` |
| A/B | `logixone/migrator:j11-s5-01-customized` | 104569197 bytes | `sha256:1bd19f8e007b202aa1206f164597e37ebd74ee337773a7ee5d37925c082915a1` |
| base | `logixone/app:j11-s5-01-base` | 500119356 bytes | `sha256:1a6c5c98323692c6f8fe7588b16c6cb4985a6de4185112a6274e4c6de88b96b8` |
| base | `logixone/migrator:j11-s5-01-base` | 104563551 bytes | `sha256:b7aee39e8bc43be3b0fce1a48ec1aeac12f1b28f1317ba4832875bc46ffe7681` |

Son IDs de imágenes locales de validación, no referencias autorizadas para
promoción.

## Compose, idempotencia y retirada

La pila aislada usó el proyecto `logixone-s5-01` y puertos 18085/18185. La primera
prueba con sólo `reference_plugin` aplicó correctamente `core` V1–V5 y
`plg_reference_plugin` V1, pero la aplicación rechazó el aprovisionamiento de demo
con `CUSTOMIZATION_NOT_PRESENT`: el ambiente exigía `reference_custom_a`. El gate
se mantuvo fallido hasta construir aplicación y migrador con el mismo perfil
completo `with-screen-customization-plugins`.

Con esa pareja, Compose terminó verde y la reejecución informó:

```text
event=migration_succeeded owner=kernel schema=core migrations_executed=0 schema_version=5
event=migration_succeeded owner=reference_plugin schema=plg_reference_plugin migrations_executed=0 schema_version=1
```

Liveness y readiness respondieron `UP`. Se insertó el marcador técnico
`s5-volume-marker`, se recreó PostgreSQL y el conteo siguió en 1. Después se
reconstruyó y levantó la pareja base sin plugins, desactivando sólo el
aprovisionamiento de demo incompatible. La aplicación registró
`plugin_count=0`, el migrador informó únicamente `core` V5 con cero cambios y el
historial/esquema/marcador de `plg_reference_plugin` continuaron presentes.

El volumen PostgreSQL conservado es `logixone-s5-01_postgres-data`, creado el
2026-07-29T13:25:55Z. Al finalizar se ejecutó `docker compose down` sin
`--volumes`; tanto ese volumen como `logixone-s5-01_keycloak-data` permanecen.

## Conclusión

Los criterios CA-01 a CA-13 quedaron demostrados. J11-S5-01 se cierra y habilita
la documentación previa de J11-S5-02. Sprint 5 continúa abierto: todavía requiere
la plantilla, la decisión de eventos/outbox y el cierre integral con demo visual,
guía y PDF.
