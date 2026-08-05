# J11-S5-01 - Composición única y migraciones de plugins

- Estado: Completada
- Fecha: 2026-07-29
- Dependencia: `J11-S5-00`
- ADR: [ADR-0012](../../adr/0012-composicion-unica-y-migraciones-de-plugins.md)

## Objetivo

Garantizar que el WAR y el migrador incorporen exactamente la misma selección
física y que el proceso one-shot valide el catálogo y migre cada esquema de plugin
en orden topológico antes de arrancar la aplicación.

## Cambio coherente

1. crear `distribution/logixone-plugin-set` como selección física única;
2. hacer que WAR y migrador dependan del plugin set;
3. registrar cada fixture mediante el SPI Java de `PluginDefinition`;
4. construir un plan `core` + plugins validado y determinista;
5. ejecutar una instancia Flyway por esquema con historial independiente;
6. agregar una migración fixture a `reference-plugin`;
7. alinear el Dockerfile del migrador con el perfil de la aplicación;
8. actualizar arquitectura, runbooks y guía para implementadores.

## Criterios de aceptación

- **CA-01:** la composición base incluye cero implementaciones de plugins.
- **CA-02:** `with-reference-plugin` incluye exactamente `reference-plugin` en WAR
  y migrador.
- **CA-03:** `with-screen-customization-plugins` incluye los tres fixtures en ambos.
- **CA-04:** el catálogo inválido se rechaza antes de migrar un esquema de plugin.
- **CA-05:** `core` se ejecuta primero y los plugins siguen el orden topológico.
- **CA-06:** una contribución con esquema diferente a `PluginId.schemaName()` falla.
- **CA-07:** varias ubicaciones del mismo plugin se ejecutan con un historial único.
- **CA-08:** `reference-plugin` crea `plg_reference_plugin` y su historial.
- **CA-09:** una segunda ejecución aplica cero migraciones.
- **CA-10:** checksum cambiado y ubicación ausente producen fallo seguro.
- **CA-11:** no se empaqueta Jakarta EE dentro del ejecutable del migrador.
- **CA-12:** logs no muestran URL, usuario, contraseña ni contenido SQL sensible.
- **CA-13:** retirar el plugin no elimina su esquema ni sus datos.

## Pruebas previstas

- unitarias del plan, propiedad, agrupación y orden;
- pruebas de comando y salida segura;
- pruebas del descriptor y recurso SQL del fixture;
- PostgreSQL/Testcontainers para vacío, idempotencia y checksum;
- builds Maven de las tres composiciones e inspección de WAR/JAR;
- ArchUnit y `mvn verify` del corte;
- build de ambas imágenes y prueba Compose antes del cierre de Sprint.

## Documentación afectada

- ADR e índice;
- arquitectura general y estrategia de pruebas;
- runbooks de build, migrador y Docker;
- guía de implementación;
- evidencia de `J11-S5-01`.

## Límites

No incorpora el dominio `business_partners`, no crea carga dinámica, no migra por
empresa y no borra esquemas al retirar un plugin.

## Resultado final

El código, SPI, SQL fixture y composición Maven quedaron implementados y
validados. Las pruebas unitarias, 12 controles arquitectónicos, `mvn verify` con
181 pruebas y las variantes físicas base/referencia/A-B están verdes.

PostgreSQL/Testcontainers ejecutó 12 escenarios sin fallos. Compose confirmó la
aplicación inicial e idempotente de `core` V1–V5 y
`plg_reference_plugin` V1, health real y persistencia después de recrear
PostgreSQL. La distribución base posterior inició con `plugin_count=0` y el
migrador ejecutó únicamente `core`, mientras el esquema y el marcador del plugin
retirado permanecieron intactos. Los contenedores se apagaron sin `--volumes` y
los volúmenes nombrados quedaron conservados.

`J11-S5-02` queda habilitada. Este cierre no cierra Sprint 5 ni sustituye la demo,
la validación integral y el PDF obligatorios de `J11-S5-04`.

Evidencia: [J11-S5-01](../../evidence/J11-S5-01-migraciones-plugins-composicion.md).
