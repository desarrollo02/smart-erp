# J11-S1-01 — Baseline y decisiones arquitectónicas

- Fecha: 2026-07-23
- Estado: Completado
- Tipo: Arquitectura

## Objetivo

Fijar el baseline técnico y las decisiones que gobernarán la creación del ERP modular antes de escribir código de producción.

## Estado inicial

- El proyecto nuevo contiene únicamente instrucciones operativas y documentación.
- No existe todavía POM, código Java, Dockerfile, Compose ni base de datos del proyecto.
- El legado `C:\cosme\multienvios\miaterra` permanece como base de conocimiento de solo lectura.
- `J11-S1-00` está completado y habilita esta historia.

## Criterios de aceptación

- El baseline de Java, Jakarta EE, WildFly y Maven queda fijado con fuentes oficiales.
- El modelo de composición y activación de plugins queda aceptado.
- La propiedad de datos y la estrategia de migraciones quedan aceptadas.
- La construcción Docker, configuración y promoción por digest quedan aceptadas.
- Existe una vista general de arquitectura y módulos previstos.
- Existe una matriz de pruebas obligatorias por tipo de cambio.
- Las decisiones abiertas se indican expresamente y tienen una historia responsable.
- La documentación y sus enlaces pasan la validación estructural.

## Pasos ejecutados

1. Se leyeron completamente `AGENTS.md`, `docs/README.md` y el cierre de `J11-S1-00`.
2. Se confirmó que no existe código ni infraestructura que pueda adelantarse accidentalmente.
3. Se revisaron fuentes oficiales de Jakarta EE, WildFly, Maven, Docker y Flyway.
4. Se abrió este registro antes de redactar las decisiones.
5. Se aceptaron cuatro ADR independientes: plataforma, plugins, persistencia y Docker/IaC.
6. Se validaron los ADR con siete controles estructurales y de contenido, todos correctos.
7. Se creó la vista general de arquitectura, incluidos módulos, dependencias, arranque, flujo de operación y decisiones diferidas.
8. Se creó la estrategia de pruebas con gates G0 a G5 y matriz por tipo de cambio.
9. Se validaron arquitectura, estrategia y enlaces: nueve controles correctos y cero enlaces locales rotos en 17 documentos.
10. Se ejecutó una validación integrada previa a instalación: once controles correctos sobre 18 documentos.
11. Se instalaron 13 archivos nuevos o actualizados en `C:\cosme\LogixoneJakarta11\docs`.
12. Se repitió la validación desde la ruta definitiva: once controles correctos, cero enlaces rotos y hashes coincidentes para los 13 archivos instalados.
13. El primer gate de cierre formal obtuvo 11/12: una aserción buscaba `` `reference-plugin` `` aunque la arquitectura lo expresa como la ruta `reference-plugin/` dentro de un bloque de texto.
14. Se corrigió únicamente la aserción para comprobar la representación real y se repitió el gate completo con 12/12 controles correctos.

## Fuentes oficiales revisadas

- [Jakarta EE Platform 11](https://jakarta.ee/specifications/platform/11/)
- [WildFly 41 is released](https://www.wildfly.org/news/2026/07/16/WildFly-41-is-released/)
- [WildFly Images](https://docs.wildfly.org/wildfly-container/)
- [WildFly Quickstarts](https://docs.wildfly.org/quickstart/)
- [Apache Maven release history](https://maven.apache.org/docs/history)
- [Apache Maven Wrapper](https://maven.apache.org/tools/wrapper/index.html)
- [Docker build best practices](https://docs.docker.com/build/building/best-practices/)
- [Flyway versioned migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations)

## Archivos de la historia

- `docs/adr/0001-baseline-plataforma.md`
- `docs/adr/0002-arquitectura-plugins.md`
- `docs/adr/0003-persistencia-migraciones.md`
- `docs/adr/0004-docker-iac-promocion-digest.md`
- `docs/architecture/overview.md`
- `docs/architecture/test-strategy.md`
- `docs/evidence/J11-S1-01-validacion-documental.md`
- `docs/sprints/sprint-01/README.md`
- `docs/sprints/sprint-01/J11-S1-01-baseline-arquitectonico.md`
- Índices actualizados en `docs/`, `docs/adr/`, `docs/architecture/` y `docs/evidence/`.

## Decisiones aceptadas

- Java 21, Jakarta EE 11, WildFly 41.0.0.Final, Maven 3.9.16 y Maven Wrapper 3.3.4.
- WAR modular con plugins JAR físicamente seleccionados durante el build.
- Activación lógica de plugins por empresa sin carga dinámica de clases.
- `plugin-api` y `kernel-api` neutrales respecto de Jakarta e infraestructura.
- PostgreSQL con esquema `core` y un esquema `plg_<plugin_id>` por plugin.
- Flyway en un migrador anterior al arranque de la aplicación.
- Una imagen OCI construida una vez y promovida por digest entre ambientes.
- Gates de prueba obligatorios y detención inmediata ante fallos.

## Validaciones

| Corte | Resultado |
|---|---|
| Apertura y trazabilidad | 6/6 controles correctos |
| ADR | 7/7 controles correctos |
| Arquitectura y pruebas | 9/9 controles correctos |
| Enlaces Markdown en staging | 17 documentos, 0 rotos |
| Integración previa a instalación | 11/11 controles correctos; 18 documentos |
| Ruta definitiva | 11/11 controles correctos; 0 enlaces rotos; 0 diferencias de hash |
| Cierre formal, primer intento | 11/12; fallo de la aserción `ArchitectureModulesDefined` |
| Cierre formal corregido | 12/12 controles correctos |

No se ejecutaron pruebas Maven, Java, Docker, Compose o PostgreSQL porque la historia no creó build, código ni infraestructura. Los gates correspondientes comienzan en `J11-S1-02` y `J11-S1-03`.

## Riesgos y decisiones abiertas

- Las versiones exactas de plugins Maven se fijarán en `J11-S1-02`.
- El major de PostgreSQL, driver JDBC y digests de imágenes se fijarán y probarán en `J11-S1-03`.
- Los contratos Java concretos se implementarán en `J11-S1-04`.
- La biblioteca visual adicional y el proveedor de identidad no están autorizados implícitamente; tienen historias futuras.
- El uso de una instancia PostgreSQL compartida entrega aislamiento lógico, no físico; las pruebas arquitectónicas son obligatorias.

## Siguiente paso permitido

La historia está cerrada. El siguiente paso permitido es iniciar `J11-S1-02` y crear el esqueleto Maven reproducible conforme a estos ADR.
