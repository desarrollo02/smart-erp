# WIN-I09 — Selección de plugins con dependencias resueltas

- Estado: Planificada; no implementada en `0.8.0-internal.1`
- Fecha de incorporación: 2026-08-04
- Épica: [Instalador Windows reproducible](epica-instalador-windows-reproducible.md)
- Decisiones rectoras: [ADR-0002](../adr/0002-arquitectura-plugins.md) y
  [ADR-0012](../adr/0012-composicion-unica-y-migraciones-de-plugins.md)

## Objetivo

Permitir que la persona que instala Logixone vea los plugins disponibles para el
baseline y seleccione cuáles estarán físicamente presentes. Cuando un plugin
requiera uno o varios plugins, el instalador debe incorporar recursivamente todas
las dependencias requeridas y explicar por qué fueron seleccionadas.

La selección física no activa automáticamente un plugin para todas las empresas.
La activación empresarial continúa siendo responsabilidad del kernel después del
despliegue.

## Reglas funcionales

1. El instalador muestra identificador, nombre, versión, descripción, clasificación,
   tamaño estimado, compatibilidad y dependencias requeridas/opcionales de cada
   plugin disponible en el baseline.
2. Los componentes técnicos obligatorios del kernel y la distribución se muestran
   como instalados y no son deseleccionables.
3. Al seleccionar un plugin se calcula el cierre transitivo de dependencias
   `REQUIRED`. Cada dependencia agregada automáticamente queda seleccionada y
   rotulada, por ejemplo, `Requerido por inventory`.
4. Si varios plugins comparten una dependencia, se instala una sola versión
   compatible y se muestran todos sus consumidores.
5. Una dependencia agregada automáticamente no se puede deseleccionar mientras
   exista un consumidor seleccionado. La interfaz debe ofrecer deseleccionar los
   consumidores afectados mediante una confirmación explícita.
6. Las dependencias `OPTIONAL` se explican y pueden sugerirse, pero no se agregan
   automáticamente salvo que una capacidad seleccionada las convierta en
   requeridas mediante un contrato versionado.
7. Dependencia ausente, ciclo, identificador duplicado o rangos de versión
   incompatibles bloquean el plan antes del consentimiento, UAC, descarga,
   migración o modificación del equipo.
8. El resumen previo al consentimiento distingue selección directa, selección
   automática, componentes obligatorios, plugins no seleccionados y cualquier
   diferencia respecto de una instalación existente.
9. Aplicación y migrador se construyen o adquieren con el mismo conjunto cerrado y
   la misma huella de composición. No se copian, eliminan ni cargan JAR en caliente.
10. Retirar físicamente un plugin exige nueva composición y redespliegue; no borra
    tablas, migraciones ni datos existentes.

## Fuente de verdad y plan resultante

El catálogo mostrado por el instalador debe derivarse automáticamente de los POM,
`PluginDescriptor` y perfiles reales del baseline. No se mantendrá manualmente un
segundo grafo de dependencias que pueda divergir del kernel.

El plan resuelto debe conservar, como mínimo:

- baseline y versión del instalador;
- plugins elegidos directamente;
- dependencias incorporadas automáticamente y sus razones;
- versiones y rangos resueltos;
- orden topológico de migración/composición;
- huella SHA-256 del conjunto;
- pareja de digests de aplicación y migrador;
- diferencias de instalación, actualización o reparación.

## Ejemplos de comportamiento

- Seleccionar `inventory` incorpora `commercial_catalog` y `reference_data` si
  continúan siendo dependencias requeridas en los descriptores vigentes.
- Seleccionar simultáneamente `inventory` y otro plugin que necesite
  `reference_data` mantiene una sola selección de `reference_data` y muestra ambos
  motivos.
- Intentar quitar `commercial_catalog` mientras `inventory` sigue seleccionado no
  produce una composición inválida: se bloquea o se ofrece quitar también los
  consumidores afectados.

Los nombres anteriores ilustran el baseline actual. Las reglas se aplican al
grafo real de la versión instalada y no se hardcodean para esos plugins.

## Criterios de aceptación

- **CA-01:** la lista se deriva del catálogo físico verificable del baseline.
- **CA-02:** seleccionar un plugin selecciona recursivamente todas sus dependencias
  requeridas.
- **CA-03:** la interfaz identifica qué fue elegido por la persona y qué fue
  agregado automáticamente, con la razón correspondiente.
- **CA-04:** no es posible confirmar una composición con dependencia ausente,
  ciclo, duplicado o rango incompatible.
- **CA-05:** deseleccionar una dependencia requerida conserva una composición
  válida o cancela la operación sin cambios.
- **CA-06:** las dependencias opcionales no se convierten silenciosamente en
  obligatorias.
- **CA-07:** WAR y migrador contienen exactamente el conjunto y versiones
  registrados por la huella del plan.
- **CA-08:** la migración se ejecuta en orden topológico y su segunda ejecución es
  idempotente.
- **CA-09:** actualizar, reparar o reducir la composición conserva esquemas,
  migraciones, configuración y datos existentes.
- **CA-10:** el flujo se verifica con dependencias lineales, ramificadas,
  compartidas, ausentes, cíclicas, duplicadas e incompatibles.
- **CA-11:** la UI es navegable por teclado, anuncia selecciones automáticas y no
  comunica estados únicamente mediante color.
- **CA-12:** el preflight y la vista previa no modifican el equipo y la huella
  aceptada coincide con la composición finalmente desplegada.

## Fuera de alcance

- carga dinámica, hot install, OSGi o classloaders personalizados;
- borrar automáticamente datos al retirar un plugin;
- activar plugins para todas las empresas desde el instalador;
- resolver dependencias descargando plugins no pertenecientes al catálogo firmado
  del baseline;
- instalar versiones distintas de un mismo plugin para empresas diferentes dentro
  de una misma distribución.

## Pruebas obligatorias

1. selección sin dependencias;
2. cadena de tres o más dependencias;
3. grafo ramificado y dependencia compartida;
4. deselección de dependencia con consumidores activos;
5. dependencia opcional aceptada y omitida;
6. dependencia ausente, ciclo, duplicado y conflicto de versiones;
7. manifiesto o huella manipulados;
8. igualdad de JAR entre WAR y migrador;
9. migración PostgreSQL inicial e idempotente;
10. actualización/reparación y reducción de composición conservando datos;
11. recorrido visual y accesible del selector, resumen y bloqueo.
