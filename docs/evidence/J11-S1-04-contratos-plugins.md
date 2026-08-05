# J11-S1-04 — Evidencia de contratos de plugins y validaciones

- Fecha: 2026-07-24
- Entorno: Windows, Java Temurin 21.0.11+10, Maven Wrapper 3.9.16
- Estado final: Verde

## Alcance demostrado

- SPI y descriptor Java puro e inmutable.
- SemVer 2.0.0 y rangos de compatibilidad explícitos.
- Dependencias requeridas y opcionales.
- Capacidades, permisos, menú y migraciones.
- Diagnósticos estables y orden topológico determinista.
- Límites ArchUnit.
- Composición física del WAR con plugin presente y ausente.

## Dependencia incorporada

Se agregó `com.tngtech.archunit:archunit-junit5:1.4.2` únicamente con alcance `test` y versión centralizada en el POM padre. ArchUnit se distribuye bajo Apache-2.0. La dependencia es necesaria para convertir en pruebas ejecutables la prohibición de Jakarta e infraestructura en `plugin-api`, `kernel-api` y `kernel-domain`, y la dirección kernel–plugins.

Todos sus artefactos se almacenaron en `.tools/maven-repository` mediante la configuración existente; no se descargaron binarios fuera del proyecto.

## Pruebas por incremento

| Incremento | Comando o control | Resultado |
|---|---|---|
| Contratos iniciales | `mvnw.cmd -B -pl plugin-api -am test` | 9 pruebas, cero fallos; después del SPI neutral: 10 pruebas, cero fallos |
| Catálogo, primer intento | `mvnw.cmd -B -pl kernel-domain -am test` | Falló compilación: referencia `List::sort` sin comparador |
| Catálogo, corrección | mismo comando después de proporcionar `Comparator.naturalOrder()` | 16 pruebas acumuladas, cero fallos |
| Límites arquitectónicos | `mvnw.cmd -B -pl tests/architecture-tests -am test` | 3 reglas ArchUnit, cero fallos |
| WAR sin plugin | `mvnw.cmd -B -pl distribution/logixone-war -am clean package` e inspección con `jar tf` | Build 8/8; `reference-plugin` ausente |
| WAR con plugin | `mvnw.cmd -B -Pwith-reference-plugin -pl distribution/logixone-war -am clean package` e inspección con `jar tf` | Build 9/9; `reference-plugin-0.1.0-SNAPSHOT.jar` presente |
| Gate final | `mvnw.cmd -B clean verify` | 14/14 módulos; 28 pruebas, cero fallos, errores u omitidas |
| Estado final del WAR | inspección de `distribution/logixone-war/target/logixone.war` | Variante predeterminada restaurada; plugin ausente |
| Imports prohibidos | búsqueda de `javax`, Jakarta, Hibernate, JBoss y PrimeFaces en contratos y dominio | Cero coincidencias |

La advertencia de SLF4J durante ArchUnit indica que no hay proveedor de logging para la herramienta de prueba y usa NOP. No afecta resultados y no se agregó una implementación de logging innecesaria a producción.

## Cobertura de criterios

| Criterio | Evidencia |
|---|---|
| `CA-01` | ArchUnit y búsqueda estática confirman Java estándar en `plugin-api`. |
| `CA-02` | `PluginDescriptorTest` comprueba copia defensiva e inmutabilidad. |
| `CA-03` | Pruebas de identidad, SemVer, rangos, menú y migraciones inválidas. |
| `CA-04` a `CA-09` | `PluginCatalogResolverTest`: duplicados, ausencias, versiones, autorreferencias, ciclos, contribuciones y orden. |
| `CA-10` | Tres reglas ArchUnit verdes. |
| `CA-11` | Gate final 14/14 y 28 pruebas verdes. |
| `CA-12` | Arquitectura, ADR, runbook, historia, evidencia e índice actualizados. |

## Fallo y corrección

El primer gate de `kernel-domain` se detuvo durante compilación porque una referencia de método intentó invocar `List.sort` sin el comparador obligatorio. No se avanzó al siguiente incremento. Se cambió únicamente esa invocación para usar el orden natural de `PluginId` y se repitió el gate completo con resultado verde.

## Riesgos y límites

- La carpeta no contiene metadata Git; no es posible presentar estado, diff o historial verificable.
- Docker Engine estaba detenido al iniciar la historia. No se modificaron Docker, Compose, persistencia ni despliegue, por lo que no se repitió G4.
- El plugin de referencia continúa vacío. CDI, registro runtime y activación empresarial no forman parte de esta historia.

## Conclusión

`J11-S1-04` queda verde. El siguiente paso permitido es `J11-S1-05`: kernel, descubrimiento CDI y plugin de referencia funcional.
