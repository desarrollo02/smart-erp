# Evidencia de J11-S5-02 — Plantilla mínima de plugin productivo

- Fecha: 2026-07-29
- Estado: verde
- Historia: [J11-S5-02](../sprints/sprint-05/J11-S5-02-plantilla-plugin-productivo.md)

## Resultado implementado

Se agregó `tools/plugin-scaffold`, un generador Java 21 versionado dentro del
reactor. El ejecutable valida entradas, renderiza UTF-8 con finales LF en un área
temporal y mueve el árbol completo al destino sólo al finalizar. Genera siete
archivos y nunca sobrescribe un destino existente.

El esqueleto funcional declara una única definición vacía. El de personalización
exige plugin objetivo y rango de versiones y genera una dependencia `REQUIRED`.
No se generan entidades, migraciones, pantallas ni reglas empresariales sin
requisitos.

## Pruebas incrementales

| Corte | Comando | Resultado |
|---|---|---|
| herramienta | `.\mvnw.cmd -B -pl tools/plugin-scaffold -am test` | 9 pruebas del generador y CLI verdes; 16 de `plugin-api` verdes |
| arquitectura | `.\mvnw.cmd -B -pl tests/architecture-tests -am test` | 13 reglas verdes; la herramienta sólo usa Java y contratos públicos |
| empaquetado | `.\mvnw.cmd -B -pl tools/plugin-scaffold -am package` | JAR ejecutable creado sin advertencia de manifiesto duplicado |
| reactor base | `.\mvnw.cmd -B verify` | 18 módulos, 191 pruebas, 0 fallos, 0 errores, 0 omitidas |
| variante A/B | `.\mvnw.cmd -B -Pwith-screen-customization-plugins verify` | 18 módulos verdes; WAR y migrador contienen los mismos tres plugins |
| limpieza final | Java 21 + `.\mvnw.cmd -B clean verify` | 18 módulos y 191 pruebas verdes; temporales eliminados; WAR/migrador base con cero plugins |

Las pruebas del generador cubren determinismo, identidades, rutas, colisión,
limpieza segura, plugin funcional, personalización y errores de CLI. Además
compilan con Java 21 la definición y su prueba generadas, y verifican que
`ServiceLoader` descubra exactamente un descriptor.

La inspección del JAR ejecutable encontró cero entradas `jakarta` y cero clases de
kernel. La composición base tampoco empaqueta la herramienta en WAR ni migrador.

## Prueba de composición con un módulo generado

La CLI generó temporalmente `cli-probe` y emitió:

```text
event=plugin_scaffold_created plugin_id=cli_probe artifact_id=cli-probe kind=FUNCTIONAL file_count=7
```

Un reactor temporal reutilizó el `logixone-plugin-set` canónico con sólo ese
plugin. El `verify` terminó con 11 módulos verdes y 2 pruebas del módulo generado.
La inspección posterior encontró:

- exactamente `cli-probe` en el WAR y ningún plugin de referencia;
- exactamente
  `py.com.logixone.plugins.cliprobe.CliProbePluginDefinition` en el proveedor del
  migrador.

El corte temporal vivió únicamente bajo `tools/plugin-scaffold/target`. El
`clean verify` final lo eliminó y restauró los artefactos base canónicos sin
plugins.

## Docker, Compose y conservación

Buildx validó ambos Dockerfiles sin advertencias. Se construyeron:

| Imagen | ID | Tamaño |
|---|---|---:|
| `logixone/app:j11-s5-02-customized` | `sha256:54441d13d12c06dbd22118114f9e81451d70e9689ab45739434837819ead4953` | 500126463 bytes |
| `logixone/migrator:j11-s5-02-customized` | `sha256:bbad86783ea5fa7e1b39b48143809752e7963e72650fe0e5be08d20371096701` | 104569197 bytes |

El proyecto Compose aislado `logixone-s5-01` reutilizó los volúmenes de la
historia anterior. El migrador informó cero migraciones pendientes para `core` y
`reference_plugin`; la aplicación inició con tres plugins y readiness `UP`. La
consulta de control devolvió un marcador persistente. El apagado se realizó sin
`--volumes`, por lo que permanecen:

- `logixone-s5-01_postgres-data`;
- `logixone-s5-01_keycloak-data`.

## Fallo encontrado y corrección

El primer Compose de `J11-S5-01` intentó usar la imagen de referencia con un
ambiente que exigía `reference_custom_a`. El kernel rechazó correctamente el
catálogo con `CUSTOMIZATION_NOT_PRESENT`. Se construyó la pareja WAR/migrador con
la misma composición A/B y se repitió el gate; no se relajó la validación ni se
ignoró el fallo.

La primera invocación del `clean verify` final tomó Java 8 del sistema y Maven
Enforcer la rechazó antes de compilar porque el baseline exige `[21,22)`. Se fijó
`JAVA_HOME` al JDK 21 validado en `.tools/jdk/jdk-21.0.11+10` y se repitió el mismo
gate: 18 módulos y 191 pruebas quedaron verdes. La inspección final confirmó que
los dos directorios temporales ya no existen, el WAR base tiene cero JAR de plugin
y el migrador base no contiene proveedor `PluginDefinition`.

## Documentación

- [Runbook del generador](../runbooks/plugin-scaffold.md).
- Guía para implementadores actualizada a `1.0-rc25`.
- Arquitectura y estrategia de pruebas actualizadas.

## Pendientes

- `J11-S5-03`: decidir y, sólo si está justificado, definir el contrato mínimo de
  eventos/outbox.
- `J11-S5-04`: validación integral, demo visual responsive, retrospectiva y PDF
  obligatorio del cierre.
- La validación humana independiente de la guía continúa pendiente y bloquea la
  publicación `1.0`, pero no invalida estos gates técnicos.
