# J11-S1-04 — Contratos de plugins y validaciones

- Fecha de inicio: 2026-07-24
- Estado: Completada
- Tipo: Arquitectura, contratos y dominio del kernel
- Dependencia: `J11-S1-03` completada

## Objetivo

Definir el contrato Java puro e inmutable que describe un plugin y construir una resolución determinista del catálogo que rechace descriptores incompatibles antes de que el runtime o el migrador intenten utilizarlos.

## Alcance

- Identidad estable del plugin.
- SPI neutral para exponer el descriptor sin acoplarlo a CDI.
- Versiones semánticas y rangos de compatibilidad.
- Dependencias requeridas y opcionales.
- Capacidades, permisos, menú y migraciones.
- Validación de duplicados, ausencias, incompatibilidades, autorreferencias y ciclos.
- Orden topológico determinista con dependencias antes que dependientes.
- Pruebas unitarias y arquitectónicas que mantengan `plugin-api` libre de Jakarta e infraestructura.

## Fuera de alcance

- Descubrimiento CDI de plugins desplegados.
- Activación persistida por empresa.
- Implementación funcional del plugin de referencia.
- Endpoints, navegación o UI ejecutable.
- Ejecución de migraciones aportadas por plugins.

Estos puntos pertenecen a `J11-S1-05` y posteriores.

## Decisiones de contrato

- Los identificadores de plugin usan `snake_case` en minúsculas para conservar una correspondencia no ambigua con el esquema `plg_<plugin_id>`.
- Las versiones cumplen SemVer 2.0.0 y su precedencia ignora metadata de build.
- La compatibilidad se expresa como intervalo explícito `[mínimo inclusivo, máximo exclusivo)`. Ambos límites son obligatorios en Sprint 1.
- Una dependencia opcional ausente no invalida el catálogo; si está presente, su versión debe ser compatible y participa del orden del grafo.
- El resultado de validación contiene códigos de diagnóstico estables y ordenados; no se depende del texto de una excepción para tomar decisiones operativas.
- Los contratos viven en `plugin-api`; las reglas del catálogo y del grafo viven en `kernel-domain`.
- Se usa ArchUnit `1.4.2`, versión estable publicada el 2026-04-18, únicamente con alcance de prueba. La dependencia es Apache-2.0 y materializa los límites arquitectónicos exigidos por el baseline.
- El perfil Maven `with-reference-plugin` prueba únicamente la composición física del WAR. El plugin de referencia permanece sin implementación hasta `J11-S1-05`; sin el perfil, la distribución no lo incluye.

## Criterios de aceptación

- **CA-01:** `plugin-api` contiene únicamente Java estándar en producción.
- **CA-02:** los descriptores son inmutables y realizan copias defensivas de colecciones.
- **CA-03:** identidad, versiones, rangos, dependencias y contribuciones rechazan valores estructuralmente inválidos.
- **CA-04:** se rechazan identificadores de plugin duplicados.
- **CA-05:** se rechazan dependencias requeridas ausentes y se permiten opcionales ausentes.
- **CA-06:** se rechazan versiones de dependencia y del API de plugins fuera de rango.
- **CA-07:** se rechazan autorreferencias, dependencias repetidas y ciclos.
- **CA-08:** un catálogo válido produce orden topológico estable, independiente del orden de entrada.
- **CA-09:** capacidades, permisos, menús y migraciones duplicados generan diagnósticos deterministas.
- **CA-10:** las pruebas ArchUnit impiden Jakarta e infraestructura en contratos y dominio neutral.
- **CA-11:** las pruebas específicas y `mvnw.cmd verify` quedan verdes.
- **CA-12:** la historia, evidencia y estado del Sprint se actualizan en el mismo cambio.

## Secuencia de implementación y pruebas

1. Crear valores y descriptor en `plugin-api`; ejecutar `mvnw.cmd -pl plugin-api -am test`.
2. Crear resolución del catálogo en `kernel-domain`; ejecutar `mvnw.cmd -pl kernel-domain -am test`.
3. Activar ArchUnit; ejecutar `mvnw.cmd -pl tests/architecture-tests -am test`.
4. Construir e inspeccionar el WAR sin perfil y con `-Pwith-reference-plugin`.
5. Ejecutar `mvnw.cmd verify` y registrar resultados.

No se inicia el corte siguiente mientras exista una prueba relevante fallando.

## Estado inicial verificado

- El reactor de 14 módulos finaliza correctamente.
- El migrador aporta las únicas ocho pruebas existentes.
- `plugin-api`, `kernel-domain` y `architecture-tests` no tienen todavía código o pruebas propias.
- Docker Compose valida estáticamente; Docker Engine no está disponible durante el inicio de esta historia.
- La carpeta de trabajo no contiene metadata Git, por lo que el alcance de archivos se controlará mediante inventario explícito.

## Resultados

- `plugin-api`: 11 clases de producción y 10 pruebas verdes.
- `kernel-domain`: resolución determinista con 7 pruebas verdes.
- Arquitectura: 3 reglas ArchUnit verdes.
- Composición: WAR construido e inspeccionado con el plugin de referencia presente y ausente.
- Gate final: 14 de 14 módulos y 28 pruebas verdes; cero fallos, errores u omitidas.
- Evidencia completa: [J11-S1-04 — Contratos de plugins y validaciones](../../evidence/J11-S1-04-contratos-plugins.md).

Los doce criterios de aceptación quedan cumplidos. El siguiente paso permitido es `J11-S1-05`.
