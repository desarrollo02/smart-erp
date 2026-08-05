# ADR-0001 — Baseline de plataforma

- Estado: Aceptado
- Fecha: 2026-07-23
- Historia: `J11-S1-01`

## Contexto

Logixone Jakarta 11 es un ERP greenfield. El legado se consulta para recuperar conocimiento de negocio, pero sus restricciones de Java 8, Java EE/`javax.*`, WAR monolítico y dependencias internas no deben convertirse en el baseline nuevo.

La plataforma necesita una combinación soportada, reproducible y conservadora para desarrollo, pruebas y producción.

## Decisión

Se adopta el siguiente baseline:

| Componente | Decisión |
|---|---|
| Java | Java 21 LTS; compilación con `--release 21` |
| Jakarta EE | Jakarta EE Platform 11; API `jakarta.platform:jakarta.jakartaee-api:11.0.0` con alcance `provided` |
| Servidor | WildFly estándar `41.0.0.Final`, no WildFly Preview |
| Build | Apache Maven `3.9.16`, ejecutado mediante Maven Wrapper `3.3.4` |
| Empaquetado | Monolito modular distribuido inicialmente como un WAR |
| Base de datos | PostgreSQL; el major y su digest se fijarán en `J11-S1-03` después de la prueba de compatibilidad |
| Interfaz inicial | Jakarta Faces 4.1 para el shell mínimo; ninguna biblioteca visual adicional queda aprobada aún |

El código de dominio y aplicación se mantendrá independiente del servidor y de Jakarta cuando no necesite capacidades de contenedor. Las dependencias y versiones se centralizarán en el POM padre y en el BOM interno.

El Maven Wrapper incluirá la URL de distribución y su checksum SHA-256. Se utilizarán `mvnw.cmd` en Windows y `./mvnw` en entornos POSIX; no se aceptará que el build dependa de una instalación Maven no declarada.

## Motivos

- Jakarta EE 11 es la plataforma final solicitada y requiere Java SE 17 o superior.
- WildFly 41 declara compatibilidad Jakarta EE 11 Platform, Web y Core sobre Java 17 y Java 21.
- Java 21 combina certificación del servidor, soporte de imagen actual y ciclo LTS.
- La imagen oficial de WildFly 41 ya no mantiene una variante JDK 17; la variante JDK 21 es la base coherente.
- Maven 3.9.16 es la versión estable actual; Maven 4 permanece en estado previo a GA y no se usará para el baseline.

## Alternativas consideradas

### Java 17

Es compatible, pero no aporta ventaja frente a Java 21 y ya no tiene una imagen WildFly 41 mantenida equivalente.

### Java 25

WildFly 41 lo ejecuta y prueba, pero la compatibilidad Jakarta EE 11 declarada se concentra en Java 17 y 21. Se prioriza el baseline certificado y conservador.

### Jakarta EE 10 o WildFly EE 10

Se descartan porque el proyecto es nuevo y su objetivo explícito es Jakarta EE 11.

### WildFly Preview

Se descarta porque no ofrece una ventaja necesaria para el alcance inicial y sus tecnologías pueden tener estabilidad menor.

### Maven 4

Se reconsiderará cuando exista una versión GA compatible con todo el toolchain. No se introducirá una versión candidata en el baseline productivo.

## Consecuencias

- Todo módulo Java debe compilar con Java 21.
- Quedan prohibidas las dependencias `javax.*`.
- No se compilará contra clases internas de WildFly ni Hibernate salvo en un adaptador explícito y justificado.
- Las APIs proporcionadas por el servidor no se empaquetan dentro del WAR.
- Un cambio de versión mayor de Java, Jakarta EE, WildFly o Maven necesita reemplazar o enmendar este ADR.
- Las versiones exactas de plugins Maven y bibliotecas de pruebas se fijarán en el BOM durante `J11-S1-02`.

## Verificación

`J11-S1-02` debe demostrar:

- Java efectivo 21.
- Maven efectivo 3.9.16 mediante Wrapper.
- dependencia Jakarta EE 11 con alcance `provided`.
- build limpio y reproducible en Windows y en el contenedor de build.

## Fuentes

- [Jakarta EE Platform 11](https://jakarta.ee/specifications/platform/11/)
- [WildFly 41 is released](https://www.wildfly.org/news/2026/07/16/WildFly-41-is-released/)
- [WildFly Quickstarts](https://docs.wildfly.org/quickstart/)
- [Maven releases history](https://maven.apache.org/docs/history)
- [Maven Wrapper](https://maven.apache.org/tools/wrapper/index.html)

