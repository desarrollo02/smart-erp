# J11-S2-04 — Persistencia JPA/JTA del kernel

- Fecha: 2026-07-27
- Estado: Verde; 16 de 16 criterios de aceptación satisfechos
- Ambiente: Windows 11 amd64, Java Temurin 21.0.11+10, Maven Wrapper 3.9.16, Docker Engine 29.6.2, Compose 5.3.1 y WildFly 41
- PostgreSQL probado: 18.4 real, imagen fijada por digest

## Resultado

El kernel ya persiste empresas y decisiones de activación mediante puertos neutrales y adaptadores JPA internos. WildFly administra el datasource `java:/jdbc/LogixoneCoreDS`, el pool y las transacciones JTA; la unidad `logixone-core-pu` únicamente valida el esquema `core` V2 creado previamente por el migrador.

La asignación de personalización continúa formando parte de `Company`: es obligatoria, exclusiva y versionada. No se creó una relación JPA entre entidades para representar la activación; `PluginActivationEntity` conserva únicamente el UUID empresarial dentro de su clave compuesta, evitando propagar un grafo administrado o filtrar entidades por los puertos.

## Diseño implementado

- `CompanyRepository` y `PluginActivationRepository` siguen en `kernel-application` y retornan modelos de dominio.
- `CompanyEntity`, `PluginActivationEntity`, sus mapeos y los repositorios `Jpa*` viven exclusivamente en `kernel-infrastructure-jakarta`.
- `@Version` traduce escritores obsoletos a `PersistenceConflictException` con código estable; las restricciones PostgreSQL de personalización y clave compuesta también se traducen a conflictos tipados.
- Todas las consultas de activación reciben `CompanyId`; una empresa diferente obtiene una colección vacía y no ve la personalización de otra empresa.
- La persistencia normal usa JTA; una unidad `RESOURCE_LOCAL` separada existe solo bajo `src/test/resources` para probar repositorios directamente con Testcontainers.
- El health técnico reutiliza el datasource administrado. La aplicación ya no abre conexiones mediante `DriverManager` ni recibe la contraseña de PostgreSQL.

## Runtime reproducible

`infra/wildfly/configure-runtime.cli` registra pgJDBC como módulo WildFly y crea un datasource JTA con:

- JNDI `java:/jdbc/LogixoneCoreDS`;
- URL y usuario como expresiones de entorno;
- credencial referenciada mediante una expresión cuyo valor se carga desde el secreto montado al arrancar;
- pool mínimo/inicial 2, máximo 20;
- `READ_COMMITTED`, validación `SELECT 1` y sorter PostgreSQL;
- `LOGIXONE_TX_NODE_ID` configurable y único por instancia.

La versión de pgJDBC se obtiene de `postgresql.jdbc.version` en el POM padre durante el build. El WAR no contiene pgJDBC, Hibernate ni clases WildFly. El entrypoint rechaza secreto ausente, vacío o multilínea antes de iniciar el servidor.

## Dependencias y licencias

| Componente | Uso | Licencia | SHA-256 local verificado | Fuente |
|---|---|---|---|---|
| Hibernate ORM `7.4.5.Final` | proveedor de prueba; el runtime lo aporta WildFly | Apache License 2.0 | `E894C0AE560F0031883A2F5C819E1EF1EE3E16B2CBCA31E6301A004876A67DC7` | [Hibernate ORM 7.4](https://hibernate.org/orm/releases/7.4/) y [licencia oficial](https://hibernate.org/community/license/) |
| pgJDBC `42.7.10` | pruebas y módulo del servidor; alcance `provided` en la aplicación | BSD 2-Clause | `CAB1CD67CFA25C25DE4348E532298028288A877BA01C77D1619FE45416193387` | [licencia oficial pgJDBC](https://jdbc.postgresql.org/license/) |
| Jackson Annotations `2.21` | compatibilidad exclusiva de pruebas entre Flyway 12/Jackson 3 y Testcontainers | Apache License 2.0 | `53CA085F4A150F703F49E1AABD935BD03B43E1EA3D55D135438292AF22CEF56B` | [repositorio oficial Jackson Annotations](https://github.com/FasterXML/jackson-annotations) |

No se añadió una dependencia runtime nueva al WAR. Jackson Annotations y Hibernate declarados por el proyecto tienen alcance `test`; Jakarta EE y pgJDBC mantienen alcance `provided` donde corresponde.

## Pruebas PostgreSQL y JPA

Comando integral:

```powershell
.\mvnw.cmd -B "-Dlogixone.postgres.integration=true" clean verify
```

Resultado: código 0, 14 de 14 módulos y 99 pruebas verdes. El total incluye:

- 86 pruebas del baseline normal, entre ellas 6 reglas ArchUnit;
- 7 escenarios del migrador sobre PostgreSQL 18.4;
- 6 escenarios JPA de repositorio sobre PostgreSQL 18.4.

Los seis escenarios JPA demostraron bootstrap con migraciones previas y `validate`, CRUD e idempotencia de empresa, aislamiento entre empresas, unicidad y rollback de personalización, conflicto optimista y activación con empresa obligatoria.

ArchUnit agregó la regla de que toda clase `@Entity` resida exclusivamente en el paquete de persistencia de infraestructura. Las reglas existentes conservaron Jakarta/JPA/JDBC fuera de APIs, dominio y aplicación.

## Prueba JTA dentro de WildFly

El arnés se construyó únicamente mediante el perfil opt-in y se copió temporalmente al contenedor efímero; no forma parte del reactor ni del WAR normal:

```powershell
.\mvnw.cmd -B -Pjta-runtime-harness `
  -pl tests/runtime-persistence-harness -am package

.\mvnw.cmd -B -pl tests/integration-tests `
  "-Dlogixone.base-uri=http://127.0.0.1:18084" `
  "-Dlogixone.jta-probe=true" verify
```

Resultado: código 0 y 4 de 4 pruebas runtime verdes. Dos comprueban liveness/readiness y dos demuestran que una transacción JTA confirma empresa más activación conjuntamente, o revierte ambas filas ante una excepción runtime.

Los logs de arranque confirmaron `LogixoneCoreDS` enlazado, unidad JPA en fases 1 y 2, Hibernate ORM 7.4.5.Final provisto por WildFly, PostgreSQL 18.4, `DataSourceConnectionProvider`, esquema validado y aislamiento `READ_COMMITTED`.

## WAR, imagen y Compose

Se construyeron ambas variantes desde limpio:

```powershell
.\mvnw.cmd -B -Pwith-reference-plugin `
  -pl distribution/logixone-war -am clean package
.\mvnw.cmd -B -pl distribution/logixone-war -am clean package
```

La variante con perfil contiene exactamente un `reference-plugin`. El WAR normal final contiene seis JAR propios y cero coincidencias para `reference-plugin`, pgJDBC, Hibernate, WildFly o el arnés JTA.

Ambos Dockerfiles pasaron `docker buildx build --check` sin advertencias. La imagen normal `logixone/app:j11-s2-04` se construyó con identificador/digest local `sha256:702fed9e8591c1ff5e55a314a3be7817f19655625c57b796bc8e1950b80f5f2d`.

La composición aislada `logixone-s204` quedó saludable. Después de ejecutar `down` sin `--volumes` y volver a crear los contenedores con el mismo volumen, la empresa y su activación siguieron presentes, V1/V2 no se reaplicaron y readiness volvió a `200 UP`. Una recreación posterior usando la imagen final conservó nuevamente esos datos.

Tras conservar esta evidencia se verificaron las etiquetas exactas y se retiró únicamente el entorno efímero con `down --volumes --remove-orphans`. La comprobación posterior informó cero contenedores, volúmenes y redes con la etiqueta `com.docker.compose.project=logixone-s204`; esos datos sintéticos ya no son recuperables y ninguna composición ajena fue afectada.

## Fallos detectados y correcciones

1. Flyway 12/Jackson 3 encontró una versión transitiva 2.20 de annotations desde Testcontainers. Se centralizó `2.21` solo para tests y Enforcer volvió a confirmar convergencia.
2. Un CLI multilinea no conservó argumentos durante el build de WildFly. Las operaciones se expresaron como comandos atómicos de una línea y el CLI devolvió `success`.
3. Al reemplazar el entrypoint heredado se perdió el `CMD` de WildFly. Se declaró explícitamente el comando de arranque.
4. Un bean CDI con scope normal no podía ser proxificado sin constructor vacío. El probe sin estado pasó a `@Dependent`.
5. La versión del driver estaba repetida como argumento Docker. El build ahora la lee de la propiedad central del POM y falla si está ausente.

Ninguna prueba fue omitida o relajada para lograr el cierre.

## Criterios y continuidad

CA-01 a CA-16 están satisfechos. `J11-S2-05` queda habilitada para conectar estos repositorios a los casos de uso y guardas de activación, manteniendo fuera de alcance los endpoints administrativos públicos.

La guía para implementadores no cambia aún su recorrido empresarial: esta historia materializa infraestructura interna. El capítulo operativo de datasource, secreto, migración y conservación de volúmenes se actualizó en los runbooks; la validación independiente completa de la guía permanece en `J11-S2-08`.
