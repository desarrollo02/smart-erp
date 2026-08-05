# J11-S2-02 — Evidencia del modelo neutral multiempresa

- Fecha: 2026-07-27
- Estado: Verde; historia completada
- Tipo de cambio: contratos Java puros, dominio, aplicación, arquitectura y documentación

## Objetivo verificado

Materializar ADR-0005 sin adelantar persistencia ni adaptadores: identidad empresarial, categoría de plugin, activación deseada y efectiva, personalización obligatoria, aislamiento por empresa, puertos de aplicación y políticas deterministas.

## Ambiente

```text
Apache Maven 3.9.16
Java 21.0.11 Eclipse Adoptium
Maven home: .tools/maven-wrapper-home/wrapper/dists/apache-maven-3.9.16/...
Java home: .tools/jdk/jdk-21.0.11+10
Windows 11 amd64
Locale: es_PY
Codificación: UTF-8
```

Todos los comandos Maven exitosos usaron el Wrapper y definieron `JAVA_HOME` y `MAVEN_USER_HOME` hacia las herramientas locales verificadas dentro de `.tools/`.

## Implementación

### Contrato de plugins

- Se agregó `PluginKind.FUNCTIONAL/CUSTOMIZATION` como campo obligatorio de `PluginDescriptor`.
- `PluginApiVersion.CURRENT` pasó de `0.1.0` a `0.2.0`; el cambio de constructor es deliberadamente incompatible y no tiene valor por defecto.
- El plugin de referencia declara categoría funcional y compatibilidad `[0.2.0,0.3.0)`.
- El catálogo rechaza dependencias funcional→personalización y personalización→personalización.
- El orden topológico preserva dependencias y sitúa todas las personalizaciones después de todos los plugins funcionales.

La versión Maven del proyecto continúa siendo `0.1.0-SNAPSHOT`; es distinta de la versión semántica del contrato público expresada por `PluginApiVersion`.

### API y dominio del kernel

- `CompanyId` encapsula UUID, es inmutable, comparable y valida texto canónico en minúsculas.
- `CompanyContext` es un puerto de lectura sin setters, Jakarta, HTTP ni `ThreadLocal` manual.
- `Company` exige personalización y versión no negativa; `CompanyStatus` distingue intención `INACTIVE/ACTIVE`.
- `PluginActivationDecision` conserva el estado deseado por empresa/plugin y no confunde fila ausente con habilitación.
- `CompanyPluginResolver` deriva la composición efectiva desde una empresa, sus decisiones y el catálogo físico.
- La resolución pone en cuarentena solo a la empresa con personalización ausente, incorrecta, compartida o incompatible.
- `PluginActivationPolicy` valida dependencias requeridas al habilitar y dependientes al deshabilitar; las opcionales no bloquean.
- Se materializó el conjunto mínimo de códigos estables fijado por ADR-0005, aunque los conflictos transaccionales y overlays se consumirán en historias posteriores.

### Aplicación y límites

- Se agregaron puertos neutrales `CompanyIdGenerator`, `CompanyRepository` y `PluginActivationRepository`.
- Los comandos de alta, ciclo de vida, activación y reemplazo exigen valores explícitos y versión esperada cuando aplica.
- `CompanyPluginQueryService` consulta únicamente el `CompanyId` solicitado, ignora decisiones ajenas y usa `PluginRegistry` como catálogo físico autorizado.
- ArchUnit restringe las capas neutrales a sus dependencias permitidas y prohíbe Jakarta, `java.sql`, `javax.sql` y PostgreSQL.

## Secuencia de gates

| Gate | Comando o control | Resultado final |
|---|---|---|
| G0 documental | UTF-8 estricto, caracteres de reemplazo y enlaces Markdown locales | 61 archivos, 145 enlaces, 0 errores |
| API de plugins | `.\mvnw.cmd -B -pl plugin-api test` | 12 pruebas, verde |
| API del kernel | `.\mvnw.cmd -B -pl kernel-api -am test` | 3 pruebas, verde |
| Dominio | `.\mvnw.cmd -B -pl kernel-domain -am test` | 23 pruebas, verde |
| Aplicación | `.\mvnw.cmd -B -pl kernel-application -am test` | 15 pruebas, verde |
| Arquitectura | `.\mvnw.cmd -B -pl tests/architecture-tests -am test` | 5 reglas, verde |
| Repositorio | `.\mvnw.cmd -B clean verify` | 14/14 módulos, `BUILD SUCCESS`, 27.938 s |
| WAR con plugin | `.\mvnw.cmd -B -Pwith-reference-plugin -pl distribution/logixone-war -am clean package` | 9/9 módulos, `BUILD SUCCESS`, 28.239 s |
| Restauración default | `.\mvnw.cmd -B -pl distribution/logixone-war -am clean package` | 8/8 módulos, `BUILD SUCCESS`, 23.498 s |

El conteo de los 24 reportes Surefire/ArchUnit vigentes produjo:

```text
tests=83 failures=0 errors=0 skipped=0
```

Distribución por módulo:

| Módulo | Pruebas |
|---|---:|
| `plugin-api` | 12 |
| `kernel-api` | 3 |
| `kernel-domain` | 23 |
| `kernel-application` | 15 |
| `kernel-infrastructure-jakarta` | 12 |
| `web-shell` | 3 |
| `migrator` | 8 |
| `reference-plugin` | 2 |
| ArchUnit | 5 |
| **Total** | **83** |

## Composición exacta del WAR

El contenido se inspeccionó con `jar tf distribution/logixone-war/target/logixone.war` usando el JDK 21 local.

| Variante | `reference-plugin` | `plugin-api` | `kernel-api` |
|---|---:|---:|---:|
| Predeterminada | 0 | 1 | 1 |
| `with-reference-plugin` | 1 | 1 | 1 |

Tras verificar el perfil opcional se reconstruyó la variante predeterminada. El WAR que queda en el workspace contiene cero `reference-plugin` y una copia de cada API compartida.

El G0 se ejecutó después de crear esta evidencia y revisar los índices: los 61 archivos Markdown se decodificaron como UTF-8 estricto, no contienen caracteres de reemplazo y sus 145 enlaces locales resuelven correctamente.

## Fallos encontrados y correcciones

1. El primer intento de Wrapper se ejecutó sin las variables locales y no llegó a iniciar Maven: el entorno global exponía Java 8 y `MAVEN_USER_HOME` no estaba preparado. Se fijaron `JAVA_HOME`, `MAVEN_USER_HOME` y `Path` hacia `.tools/`; Maven 3.9.16 confirmó Java 21.0.11.
2. La primera prueba de `kernel-api` no compiló porque el módulo nuevo no declaraba JUnit 5. Se agregó la dependencia de test al POM y la repetición terminó con 3 pruebas verdes.
3. Una inspección inicial supuso un nombre incorrecto para la prueba ArchUnit. Se enumeraron los archivos y se usó `ModuleBoundariesArchitectureTest.java`; no se modificó código por ese error de diagnóstico.
4. La primera compilación de `CompanyCommandTest` llamó una fábrica inexistente `CompanyId.from(UUID)`. Se corrigió para usar el constructor público y se repitió el gate de aplicación completo: 15 pruebas verdes.

Ninguna prueba fue omitida, deshabilitada o relajada para obtener el resultado verde.

## Archivos de implementación

- `plugin-api`: nuevo `PluginKind`, descriptor y versión de API actualizados, más pruebas de contrato.
- `kernel-api`: `CompanyId`, `CompanyContext`, dependencia JUnit y pruebas.
- `kernel-domain`: paquete `company`, políticas/resolución/diagnósticos, reglas de catálogo y pruebas.
- `kernel-application`: puertos, comandos, resultado/servicio de consulta y pruebas.
- `kernel-infrastructure-jakarta`, `reference-plugin` y fixtures consumidores: migrados al constructor `PluginDescriptor` 0.2.0.
- `tests/architecture-tests`: límites de `kernel-api` y prohibiciones JDBC/PostgreSQL.
- historia, Sprint, arquitectura, estrategia, épica, índices y esta evidencia.

La carpeta no contiene metadata Git; por ello no fue posible producir un diff verificable mediante Git. El alcance se auditó mediante enumeración de fuentes, búsquedas de usos del descriptor y gates Maven desde limpio.

## Pruebas no ejecutadas

No se ejecutaron PostgreSQL, Testcontainers, Flyway V2, JPA/JTA, Docker, Compose, WildFly ni Playwright. Esta historia excluye SQL, persistencia, runtime y UI. Esos gates comienzan secuencialmente en `J11-S2-03`, `J11-S2-04` y `J11-S2-07`; ejecutarlos aquí habría adelantado historias bloqueadas.

## Conclusión

Los 16 criterios de `J11-S2-02` están demostrados. El último baseline integral está verde, el WAR default fue restaurado y `J11-S2-03` queda habilitada para crear y verificar la migración aditiva `core` V2.
