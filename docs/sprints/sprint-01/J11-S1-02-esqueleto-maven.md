# J11-S1-02 — Esqueleto Maven reproducible

- Fecha: 2026-07-23
- Estado: Completado
- Tipo: Build y estructura

## Objetivo

Crear el reactor Maven multimódulo del proyecto, fijar Java y Maven mediante configuración verificable y demostrar un build limpio antes de introducir contratos o lógica de producción.

## Estado inicial

- `J11-S1-01` está completado.
- La raíz del proyecto contiene únicamente `AGENTS.md` y `docs/`.
- No existe POM, Maven Wrapper, código Java, Dockerfile ni infraestructura.
- Los ADR aceptados fijan Java 21, Jakarta EE 11, WildFly 41.0.0.Final, Maven 3.9.16 y Maven Wrapper 3.3.4.

## Alcance

- POM padre y reactor multimódulo.
- Maven Wrapper con Maven 3.9.16 y checksum de distribución.
- BOM interno con Jakarta EE 11.
- Módulos vacíos que materialicen los límites aprobados.
- Reglas Maven Enforcer para Java, Maven y convergencia de dependencias.
- Archivos básicos reproducibles de editor y control de artefactos.
- Build completo con `verify`.

No se implementarán todavía contratos de plugins, CDI, persistencia, UI, Docker ni lógica empresarial.

## Coordenadas iniciales

- `groupId`: `py.com.logixone`
- versión del reactor: `0.1.0-SNAPSHOT`
- empaquetado final previsto: WAR

## Módulos previstos

1. `platform-bom`
2. `plugin-api`
3. `kernel-api`
4. `kernel-domain`
5. `kernel-application`
6. `kernel-infrastructure-jakarta`
7. `web-shell`
8. `migrator`
9. `plugins/reference-plugin`
10. `distribution/logixone-war`
11. `tests/architecture-tests`
12. `tests/integration-tests`
13. `tests/e2e-tests`

## Criterios de aceptación

- `mvnw` y `mvnw.cmd` ejecutan Maven 3.9.16.
- La descarga Maven del Wrapper tiene checksum fijado.
- El compilador usa `--release 21` y Enforcer rechaza otro major de Java.
- Jakarta EE API 11.0.0 está centralizada y solo puede consumirse como `provided`.
- Los 13 módulos forman un reactor válido y respetan el orden arquitectónico.
- El WAR vacío se construye sin exigir `web.xml`.
- `mvnw verify` termina correctamente desde el staging y desde la ruta definitiva.
- No se agregan clases de producción ni dependencias no justificadas.
- Todos los pasos, fallos y pruebas quedan documentados.

## Pasos ejecutados

1. Se leyó completamente `AGENTS.md`.
2. Se verificó que `J11-S1-01` está cerrado y que la raíz aún no contiene código.
3. El primer parche de apertura fue rechazado por un separador de hunk inválido; no produjo cambios parciales.
4. Se corrigió el formato del parche y se abrió esta historia antes de crear archivos de build.
5. Se diagnosticó Maven 3.9.15 sobre Java 8 como toolchain predeterminado y se confirmó que no había JDK 21 instalado.
6. La primera descarga temporal de Temurin 21.0.11 alcanzó el timeout antes de completar y no se utilizó.
7. Se verificó el tamaño oficial, se reanudó la descarga y se validó el SHA-256 antes de extraer el JDK temporal.
8. Se creó un POM raíz mínimo y se validó con Java 21.
9. Se generó Maven Wrapper 3.3.4 para Maven 3.9.16 y se probó desde un caché vacío.
10. Se verificó la distribución Maven contra el SHA-512 oficial de Apache y se fijó su SHA-256 en el Wrapper.
11. Se crearon y validaron el BOM, `plugin-api` y `kernel-api`.
12. Se agregaron dominio, aplicación, infraestructura Jakarta, shell y migrador; su reactor quedó verde.
13. Se completaron los 13 módulos, incluido el WAR y los módulos reservados de prueba; el reactor completo quedó verde.
14. Enforcer rechazó correctamente Java 17 y Maven 3.9.15.
15. Se verificó Jakarta EE 11 como `provided` y se inspeccionó el contenido del WAR.
16. Dos builds limpios consecutivos generaron un WAR binariamente idéntico.
17. La documentación y la estructura preparada pasaron diez controles integrados antes de instalarse.
18. Se instalaron 28 archivos declarados sin copiar `target/`, JDK ni caches temporales.
19. `mvnw.cmd verify` se ejecutó desde `C:\cosme\LogixoneJakarta11` con 14/14 proyectos correctos y el mismo SHA-256 del WAR.
20. La validación integrada en destino pasó 12/12 controles, con cero enlaces rotos y cero diferencias en los archivos instalados.

## Validaciones

| Gate | Resultado |
|---|---|
| POM mínimo | `BUILD SUCCESS` con Java 21 |
| Wrapper limpio | Maven 3.9.16, Java 21.0.11 y checksum fijado |
| BOM y APIs | 4/4 proyectos correctos |
| Kernel y adaptadores | 8/8 proyectos correctos |
| Reactor completo | 14/14 proyectos correctos |
| Rechazo Java 17 | Correcto, salida 1 esperada |
| Rechazo Maven 3.9.15 | Correcto, salida 1 esperada |
| Jakarta EE | versión 11.0.0 y alcance `provided` |
| WAR | generado sin `web.xml` y sin plugin de referencia |
| Reproducibilidad | dos SHA-256 idénticos |
| Ruta definitiva | `BUILD SUCCESS`, 14/14 proyectos, WAR idéntico |
| Integración final | 12/12 controles correctos |

## Archivos creados

- `pom.xml`, `.gitignore`, `.gitattributes` y `.editorconfig`.
- `.mvn/wrapper/maven-wrapper.properties`, `mvnw` y `mvnw.cmd`.
- Un `pom.xml` para cada uno de los 13 módulos.
- `docs/evidence/J11-S1-02-validacion-build.md`.
- `docs/runbooks/build-local.md`.
- Índices y arquitectura actualizados.

## Riesgos y pendientes

- La primera ejecución del Wrapper necesita acceso a Maven Central.
- Las versiones concretas de plugins Maven deben resolverse y fijarse sin introducir Maven 4.
- Los módulos vacíos prueban estructura y build, no comportamiento empresarial.
- Los avisos de JAR vacío son deliberados y desaparecerán cuando cada historia agregue código probado.
- El JDK verificado y los cachés se almacenan localmente en `.tools/`, fuera de Git. Cada ambiente debe aportar Java 21 o usar la imagen de build que se definirá en `J11-S1-03`.

## Adenda posterior

Por directiva del usuario, todos los archivos descargados y cachés creados durante esta historia fueron trasladados desde `C:\tmp` a `.tools/` dentro del proyecto. La operación y su build de verificación se documentan en [J11-S1-02 — Adenda: descargas dentro del proyecto](J11-S1-02-descargas-proyecto.md).

## Siguiente paso permitido

La historia está cerrada. El siguiente paso permitido es iniciar `J11-S1-03` para Docker e infraestructura como código.
