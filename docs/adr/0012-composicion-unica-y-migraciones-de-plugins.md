# ADR-0012 - Composición física única y migraciones de plugins

- Estado: Aceptado
- Fecha: 2026-07-29
- Historia: `J11-S5-01`
- Decisiones relacionadas: ADR-0002, ADR-0003 y ADR-0011

## Contexto

El WAR descubre plugins presentes mediante CDI, pero el ejecutable one-shot
`migrator` solo conoce actualmente las migraciones del esquema `core`. Mantener una
lista de plugins para el WAR y otra lista separada para el migrador permitiría
construir combinaciones incompatibles: código funcional presente sin su esquema o
migraciones presentes para un binario que no puede usarlo.

Cada plugin ya declara `PluginDescriptor`, dependencias y
`MigrationContribution` en `plugin-api`. Falta un mecanismo neutral para que el
proceso Java independiente del servidor descubra esos mismos descriptores, valide
el catálogo y aplique los esquemas `plg_*` en orden reproducible.

El responsable de producto decidió el 2026-07-29 continuar con estas fundaciones
dejando pendiente la validación independiente de Sprint 4. Los gates técnicos ya
ejecutados de Sprint 4 conservan su resultado verde; la decisión no cierra el
Sprint ni autoriza promoción o producción.

## Decisión

### Una sola selección física

La distribución tendrá un módulo `distribution/logixone-plugin-set` como fuente
única de selección física. Sus perfiles Maven compondrán los mismos JAR de plugins
para:

- `distribution/logixone-war`, que los incorpora bajo `WEB-INF/lib`;
- `migrator`, que los incorpora en su ejecutable sombreado.

Aplicación y migrador se deben construir en el mismo reactor y con el mismo perfil.
Agregar o retirar un plugin sigue requiriendo reconstrucción y redespliegue; no se
introduce carga dinámica.

### Dos adaptadores, un contrato

- WildFly continúa descubriendo `PluginDefinition` mediante CDI.
- El proceso one-shot descubre esas mismas definiciones mediante
  `ServiceLoader<PluginDefinition>` y el archivo estándar
  `META-INF/services/py.com.logixone.plugin.api.PluginDefinition` de cada plugin.
- El descriptor continúa siendo la única fuente de identidad, versión,
  dependencias y contribuciones. No habrá un segundo manifiesto con datos
  duplicados.
- Toda definición debe ser pública, concreta y construible sin argumentos para el
  adaptador `ServiceLoader`.

`plugin-api` permanece Java puro. `ServiceLoader` forma parte de Java SE y no agrega
una dependencia de Jakarta o del servidor.

### Plan de migración

El migrador ejecutará en este orden:

1. esquema `core`;
2. catálogo físico completo validado con las mismas reglas del kernel;
3. plugins en el orden topológico de sus dependencias;
4. dentro de cada plugin, todas sus ubicaciones declaradas en una sola instancia
   lógica de Flyway para su esquema.

Cada contribución debe usar exactamente `PluginId.schemaName()`, es decir,
`plg_<plugin_id>`. Un plugin puede no tener migraciones; en ese caso participa de la
validación del catálogo pero no crea un esquema vacío.

Cada esquema usa su propia tabla `flyway_schema_history`, validación de nombres y
checksum, `outOfOrder=false`, `cleanDisabled=true` y fallo ante ubicaciones
ausentes. Un error de catálogo, propiedad o Flyway detiene el proceso y evita que
Compose arranque la aplicación.

### Presencia física y activación empresarial

Las migraciones dependen de la presencia física en la distribución, no de la
activación para una empresa. Si un plugin está presente, sus migraciones se aplican
aunque todavía no esté activo; así puede habilitarse después sin mutar el esquema
durante una petición web. Desactivar o retirar físicamente un plugin no borra su
esquema, historial ni datos.

### Continuidad autorizada

Esta decisión reemplaza únicamente la condición temporal de ADR-0011 que impedía
iniciar cualquier habilitador antes del dictamen independiente de `J11-S4-08`.
Sprint 5 puede implementar y probar fundaciones transversales. Sprint 4 permanece
abierto y no se puede publicar la guía `1.0`, promover imágenes ni desplegar a
producción hasta completar la validación independiente.

Los cambios nuevos de Sprint 5 vuelven al flujo incremental normal: una prueba
ejecutada y fallida bloquea el avance. La decisión no convierte resultados verdes
ya obtenidos en pendientes ni difiere automáticamente las pruebas nuevas.

## Alternativas descartadas

### Mantener perfiles separados en WAR y migrador

Se descarta porque dos listas editables pueden divergir sin que Maven detecte el
error.

### Leer descriptores CDI desde el migrador

Se descarta porque acoplaría el proceso one-shot a Jakarta CDI y a un contenedor que
no necesita.

### Copiar migraciones de todos los plugins al módulo migrator

Se descarta porque quitaría la propiedad al plugin, generaría colisiones y haría
posible aplicar migraciones de capacidades ausentes.

### Ejecutar migraciones al arrancar el WAR

Se mantiene descartado por ADR-0003: mezcla despliegue con mutación de datos y es
inseguro con múltiples réplicas.

## Consecuencias

- Cada nuevo plugin debe registrar su `PluginDefinition` tanto para CDI como para
  `ServiceLoader`.
- Los perfiles de composición se mantienen una sola vez en el plugin set.
- El migrador incorpora los contratos y JAR de la misma composición que el WAR.
- Un catálogo inválido falla antes de tocar esquemas de plugins; `core` se mantiene
  como primer propietario por compatibilidad con el baseline existente.
- La imagen de aplicación y la imagen del migrador siguen siendo artefactos
  distintos, pero deben registrar el mismo perfil y promoverse como una pareja de
  digests.

## Verificación obligatoria

1. construir composición base, con referencia y con personalizaciones;
2. comprobar que WAR y migrador contienen el mismo conjunto de plugins;
3. probar catálogo vacío, dependencias, ciclos, duplicados y esquema ajeno;
4. aplicar `core` y `plg_reference_plugin` sobre PostgreSQL vacío;
5. repetir y comprobar idempotencia;
6. modificar una migración aplicada en una base efímera y comprobar rechazo por
   checksum;
7. retirar el plugin, recrear contenedores y demostrar conservación del esquema;
8. ejecutar `mvn verify`, ArchUnit, Docker/Compose, health y smoke tests del corte.

## Referencias

- [ADR-0002 - Arquitectura de plugins](0002-arquitectura-plugins.md)
- [ADR-0003 - Persistencia y migraciones](0003-persistencia-migraciones.md)
- [ADR-0011 - Roadmap de plugins productivos](0011-roadmap-dependencias-plugins-productivos.md)
- [Sprint 5](../sprints/sprint-05/README.md)

