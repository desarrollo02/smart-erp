# J11-S1-05 — Kernel, descubrimiento CDI y plugin de referencia

- Fecha de inicio: 2026-07-24
- Estado: Completada
- Tipo: Aplicación, infraestructura Jakarta y composición
- Dependencia: `J11-S1-04` completada

## Objetivo

Construir un registro inmutable del catálogo validado, descubrir mediante CDI las definiciones de plugins físicamente presentes y demostrar con un plugin de referencia que la misma base de kernel funciona con el JAR incluido o retirado de la distribución.

## Alcance

- Registro de aplicación independiente de Jakarta.
- Excepción operativa con diagnósticos tipados cuando el catálogo es inválido.
- Adaptador CDI confinado a `kernel-infrastructure-jakarta`.
- Inicialización del registro al arrancar el contexto de aplicación.
- Plugin de referencia como bean CDI con descriptor, capacidad, permiso y menú.
- Log estructurado de plugins descubiertos, sin información sensible.
- Composición y arranque del WAR con el plugin presente y ausente.
- Caso negativo del mismo camino de bootstrap ante un catálogo inválido.

## Fuera de alcance

- Activación persistida por empresa.
- Ejecución de lógica empresarial, pantalla o endpoint del plugin.
- Endpoints semánticos de liveness y readiness, reservados a `J11-S1-06`.
- Descubrimiento y ejecución de migraciones aportadas por plugins.

## Decisiones de implementación

- `kernel-application` crea y expone un `PluginRegistry` inmutable; no importa CDI ni Jakarta.
- El adaptador usa `Instance<PluginDefinition>` para que una distribución sin plugins produzca un catálogo vacío válido.
- La observación de `@Initialized(ApplicationScoped.class)` ejecuta exactamente el mismo bootstrap probado en el caso negativo.
- Un catálogo inválido lanza `InvalidPluginCatalogException`; no se publica un orden parcial.
- El plugin de referencia usa el identificador `reference_plugin` y no incorpora persistencia en esta historia.
- La clase CDI del plugin reside en su propio módulo y el kernel no la referencia ni transitiva ni directamente.

## Criterios de aceptación

- **CA-01:** el registro vive en `kernel-application`, es inmutable y permite consulta por identidad.
- **CA-02:** cero definiciones producen un registro válido y vacío.
- **CA-03:** un catálogo inválido conserva diagnósticos tipados y detiene el bootstrap.
- **CA-04:** `kernel-application` no depende de Jakarta ni de implementaciones de plugins.
- **CA-05:** el adaptador CDI descubre `PluginDefinition` mediante `Instance`.
- **CA-06:** el bootstrap ocurre al inicializar el contexto de aplicación y registra cantidad e identidades sin secretos.
- **CA-07:** el plugin de referencia es un bean CDI y entrega un descriptor válido con contribuciones mínimas.
- **CA-08:** el WAR predeterminado compila y arranca con cero plugins.
- **CA-09:** el WAR con `-Pwith-reference-plugin` contiene el JAR, arranca y descubre exactamente `reference_plugin`.
- **CA-10:** ArchUnit impide Jakarta en aplicación/dominio neutral y dependencias desde plugins hacia implementaciones del kernel.
- **CA-11:** pruebas específicas, composición presente/ausente y `mvnw.cmd verify` quedan verdes.
- **CA-12:** historia, arquitectura, ADR, operación y evidencia se actualizan en el mismo cambio.

## Secuencia y gates

1. Implementar el registro; `mvnw.cmd -pl kernel-application -am test`.
2. Implementar plugin y adaptador CDI; probar ambos módulos.
3. Ejecutar ArchUnit y construir los dos WAR desde limpio.
4. Construir y arrancar en WildFly las variantes ausente/presente; inspeccionar despliegue, health operativo y logs.
5. Ejecutar `mvnw.cmd clean verify` y documentar el cierre.

No se inicia un corte posterior mientras exista una prueba relevante fallando.

## Estado inicial verificado

- `J11-S1-04` está verde con 28 pruebas.
- El perfil Maven de composición ya produce WAR con y sin el JAR vacío.
- `kernel-application`, `kernel-infrastructure-jakarta` y el plugin de referencia todavía no contienen código.
- Docker CLI está disponible, pero Docker Engine permanece detenido al iniciar la historia.
- La carpeta no contiene metadata Git.

## Resultados

- Registro neutral e inmutable con 4 pruebas propias.
- Adaptador CDI con 4 pruebas, incluido catálogo vacío y caso negativo.
- Plugin de referencia con 2 pruebas y contribuciones mínimas.
- Cuatro reglas ArchUnit verdes.
- Imágenes y stacks Compose con plugin presente/ausente verificados en WildFly 41.
- Gate final: 14/14 módulos y 39 pruebas verdes; cero fallos, errores u omitidas.
- Evidencia completa: [J11-S1-05 — Kernel, CDI y plugin de referencia](../../evidence/J11-S1-05-kernel-cdi-plugin-referencia.md).

Los doce criterios quedan cumplidos. El siguiente paso permitido es `J11-S1-06`.
