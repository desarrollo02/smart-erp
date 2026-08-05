# Construcción local con Maven Wrapper

- Fecha de revisión: 2026-08-01
- Estado: Vigente

## Prerrequisitos

- JDK 21.
- Acceso HTTPS a Maven Central en la primera ejecución.
- No se requiere una instalación global de Maven.

El build exige Maven 3.9.16 y Java 21. Cualquier otro major de Java o versión Maven es rechazado por Maven Enforcer.

## Windows

Las herramientas descargadas para el proyecto se almacenan en `.tools/` y no se
versionan. Desde la raíz se ejecuta directamente el Wrapper:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd verify
```

`mvnw.cmd` selecciona automáticamente `.tools/jdk/jdk-21.0.11+10` y fija
`MAVEN_USER_HOME` en `.tools/maven-wrapper-home`; también dirige los temporales a
`.tools/tmp`. Esto ocurre aun cuando Windows tenga Java 8 configurado globalmente.
El resultado de `--version` debe indicar Maven 3.9.16, Java 21 y ambos runtimes
bajo `.tools`. No preparar esas variables manualmente en cada terminal: si el
Wrapper no las selecciona, existe una regresión que debe corregirse antes de
continuar.

`.mvn/maven.config` dirige automáticamente las dependencias a `.tools/maven-repository`. No eliminar esa opción ni redirigirlas al perfil del usuario.

## POSIX

```bash
export JAVA_HOME=/ruta/al/jdk-21
export MAVEN_USER_HOME="$PWD/.tools/maven-wrapper-home"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw --version
./mvnw verify
```

Si el checkout todavía no conserva el bit ejecutable, usar `sh ./mvnw` y corregir el permiso al inicializar Git; no mantener scripts alternativos.

El JDK almacenado actualmente es para Windows. Cada binario adicional requerido por otro sistema operativo debe descargarse y verificarse dentro de `.tools/` en ese equipo.

## Cambio dirigido

Después de un cambio coherente en un módulo:

```powershell
.\mvnw.cmd -pl <modulo> -am test
```

Antes de cerrar el corte:

```powershell
.\mvnw.cmd verify
```

## Build limpio

```powershell
.\mvnw.cmd clean verify
```

Artefacto esperado:

```text
distribution/logixone-war/target/logixone.war
```

## Variantes de composición del WAR y migrador

`distribution/logixone-plugin-set` es la única fuente de selección. Construir ambos
artefactos en el mismo reactor y con el mismo perfil:

La distribución predeterminada no contiene el plugin de referencia:

```powershell
.\mvnw.cmd -pl migrator,distribution/logixone-war -am clean package
```

La variante de prueba lo incorpora físicamente:

```powershell
.\mvnw.cmd -Pwith-reference-plugin `
  -pl migrator,distribution/logixone-war -am clean package
```

La variante de personalización incorpora el funcional y las dos personalizaciones A/B:

```powershell
.\mvnw.cmd -Pwith-screen-customization-plugins `
  -pl migrator,distribution/logixone-war -am clean package
```

Usar `clean` al alternar variantes para que el directorio expandido del WAR no
conserve una dependencia anterior. El WAR incluye los JAR y el migrador sombrea
sus clases, recursos SQL y proveedores `ServiceLoader`. Los perfiles solo
seleccionan presencia física; no asignan una personalización ni activan plugins por
empresa.

## Integración HTTP contra una aplicación ejecutándose

Después de iniciar una composición saludable y conocer el puerto publicado:

```powershell
.\mvnw.cmd -B -pl tests/integration-tests `
  "-Dlogixone.base-uri=http://127.0.0.1:<puerto>" verify
```

La propiedad activa el perfil `runtime-integration`; Failsafe ejecuta los tests `*IT` con REST Assured. Sin la propiedad, el build normal compila esas pruebas pero no intenta conectarse a un servicio externo. REST Assured `6.0.0` es una dependencia de test Apache-2.0 con versión centralizada y se almacena en `.tools/maven-repository`.

## Diagnóstico

- Error de versión Java en Windows: comprobar que exista
  `.tools/jdk/jdk-21.0.11+10/bin/java.exe` y ejecutar `mvnw.cmd --version`. Si el
  archivo existe y el Wrapper no informa Java 21, detenerse y corregir el Wrapper;
  no repetir un ajuste manual de `JAVA_HOME`.
- Error de versión Maven: usar el Wrapper, no `mvn` global.
- Error de checksum del Wrapper: no desactivar la validación; revisar descarga, proxy y valor oficial.
- Dependencia divergente: corregir el BOM o el POM padre; no excluir la regla Enforcer.

## Recuperación

El build no modifica datos externos. Para limpiar artefactos generados usar el lifecycle declarado:

```powershell
.\mvnw.cmd clean
```

No borrar manualmente la raíz del proyecto ni caches compartidos.
