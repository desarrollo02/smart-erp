# J11-S1-02 — Validación del esqueleto Maven

- Fecha: 2026-07-23
- Estado: Completado
- Ambiente: Windows 11, PowerShell, staging y ruta definitiva
- Alcance: Wrapper, reactor Maven, dependencias, WAR y reproducibilidad

## Toolchain observado

El equipo tenía como valores predeterminados Java 8u202 y Maven 3.9.15. También había JDK 17 y JDK 25, pero no JDK 21. No se cambió `JAVA_HOME` global.

Para probar el baseline se descargó temporalmente Eclipse Temurin 21.0.11+10 desde Adoptium:

- archivo: `OpenJDK21U-jdk_x64_windows_hotspot_21.0.11_10.zip`;
- tamaño oficial: 205.073.954 bytes;
- SHA-256 oficial y verificado: `d3625e7cadf23787ea540229544b6e2ab494b3b54da1801879e583e1dfee0a64`;
- ubicación inicial de ejecución: `C:\tmp\logixone-jdk21\jdk-21.0.11+10`.

Por directiva posterior del usuario, el JDK, los archivos originales y todos los cachés fueron trasladados después del cierre a `C:\cosme\LogixoneJakarta11\.tools`. La evidencia del traslado está en [J11-S1-02 — Relocalización de descargas](J11-S1-02-descargas-proyecto.md).

La primera descarga alcanzó el timeout de 300 segundos con 200.464.576 bytes y no se utilizó. Se consultó nuevamente el tamaño oficial, se reanudaron los 4.609.378 bytes restantes y solo se extrajo después de verificar tamaño y SHA-256.

## Wrapper

- Maven Wrapper: 3.3.4, tipo `only-script`.
- Maven fijado: 3.9.16.
- La distribución ZIP se verificó contra el SHA-512 publicado por Apache.
- SHA-256 fijado en `maven-wrapper.properties`: `5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce`.
- La prueba se repitió con un caché vacío y reportó Maven 3.9.16 sobre Java 21.0.11.

## Resultados por corte

| Corte | Comando o procedimiento | Resultado |
|---|---|---|
| POM mínimo | Maven 3.9.15 de bootstrap, JDK 21, `mvn -N validate` | Correcto |
| Wrapper desde caché vacío | `mvnw.cmd --version` | Maven 3.9.16 y Java 21.0.11 |
| BOM y APIs | `mvnw.cmd -pl platform-bom,plugin-api,kernel-api -am verify` | 4/4 proyectos correctos |
| Kernel y adaptadores | `mvnw.cmd -pl kernel-infrastructure-jakarta,web-shell,migrator -am verify` | 8/8 proyectos correctos |
| Reactor completo | `mvnw.cmd verify` | 14/14 proyectos correctos; 13 módulos y padre |
| Java incorrecto | `mvnw.cmd -N validate` con JDK 17 | Rechazado por Enforcer, esperado |
| Maven incorrecto | Maven 3.9.15 con JDK 21, `validate` | Rechazado por Enforcer, esperado |
| Jakarta EE | `maven-dependency-plugin:3.11.0:tree` | `jakarta.jakartaee-api:11.0.0:provided` |
| WAR | inspección con `jar tf` | kernel y shell presentes; plugin de referencia y `web.xml` ausentes |
| Reproducibilidad | dos ejecuciones consecutivas de `mvnw.cmd clean verify` | mismo tamaño y SHA-256 |
| Build en destino | `mvnw.cmd verify` desde `C:\cosme\LogixoneJakarta11` | 14/14 proyectos correctos |
| Integración final | estructura, hashes, documentación y WAR | 12/12 controles correctos |

## Artefacto reproducible

- Ruta en staging: `distribution/logixone-war/target/logixone.war`.
- Tamaño en ambas construcciones: 7.280 bytes.
- SHA-256 en ambas construcciones: `85C9BC9F5E2D0926C59E0362A0E88AB37D7BC3D7D71B8DF73D653860D4E86200`.

## Observaciones

- Los JAR vacíos generan avisos esperados; la historia prohíbe adelantar clases o contratos.
- No existen archivos Java de producción ni pruebas unitarias todavía.
- El plugin de referencia es un módulo del reactor, pero no está incluido en el WAR. Su composición presente/ausente pertenece a `J11-S1-05`.
- No se ejecutaron Docker, Compose ni PostgreSQL porque corresponden a `J11-S1-03`.

## Validación final

- Se instalaron únicamente 28 archivos declarados.
- No se copiaron `target/`, repositorios Maven, JDK ni caches del Wrapper.
- Los 28 archivos instalados coincidieron por SHA-256 con el staging validado.
- Existen 13 módulos y 14 POM contando el padre.
- Los 21 documentos no tienen enlaces locales rotos.
- El build en destino produjo `logixone.war` de 7.280 bytes con SHA-256 `85C9BC9F5E2D0926C59E0362A0E88AB37D7BC3D7D71B8DF73D653860D4E86200`.
- El plugin de referencia no apareció accidentalmente en el WAR.

Resultado: `J11-S1-02` cumple sus criterios de aceptación y habilita `J11-S1-03`.
