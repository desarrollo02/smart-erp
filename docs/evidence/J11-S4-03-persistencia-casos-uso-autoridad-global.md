# Evidencia de J11-S4-03 — Persistencia JPA/JTA y casos de uso globales

- Fecha: 2026-07-28
- Estado: implementada pendiente de pruebas
- Historia: [J11-S4-03](../sprints/sprint-04/J11-S4-03-persistencia-casos-uso-autoridad-global.md)

## Resultado

La autoridad global de V4 ya tiene persistencia privada y límite transaccional:

- `SystemRoleEntity` mapea el rol global y conserva `@Version`;
- `SystemRolePermissionEntity` y su clave mapean concesiones globales;
- `AppUserSystemRoleEntity` y su clave mapean asignaciones a usuarios locales;
- `persistence.xml` declara las cinco clases y conserva DDL `none` más `validate`;
- `JpaSystemAuthorityRepository` implementa el puerto neutral sin exponer Jakarta,
  Hibernate, JDBC o tipos PostgreSQL fuera de infraestructura.

V4 no fue modificada. Los nombres de tablas, columnas, constraints y claves
coinciden con la migración aditiva ya versionada.

## Casos de uso

`SystemAuthorityAdministrationService` incorpora operaciones tipadas para:

- registrar y cambiar estado de roles globales;
- asignar y revocar roles globales;
- conceder y revocar permisos globales conocidos;
- cambiar el estado de un usuario sin invalidar la autoridad global.

`SystemAuthorityQueryService` resuelve permisos actuales desde usuario, roles,
asignaciones y concesiones persistidas. `TransactionalSystemAuthorityUseCases`
aporta la frontera JTA y también implementa el puerto del bootstrap one-shot. El
cambio de estado de usuario del adaptador de seguridad existente delega en esta
frontera para no eludir la protección del último administrador.

## Concurrencia y último administrador

Antes de una mutación global, el repositorio adquiere
`pg_advisory_xact_lock(7100110400)`. PostgreSQL lo conserva hasta finalizar la
transacción JTA, por lo que dos operaciones que reduzcan autoridad no pueden
evaluar simultáneamente el mismo snapshot anterior.

Después del lock y antes de escribir, desactivar un usuario o rol, revocar una
asignación o revocar un permiso construye el estado propuesto completo y aplica
`SystemAuthoritySafetyPolicy`. El resultado se rechaza con
`SYSTEM_LAST_ADMINISTRATOR_REQUIRED` si ya no quedaría un usuario activo, con rol
activo y `kernel.system_administration.manage`. Un contexto referencial incoherente
falla cerrado con `SYSTEM_AUTHORITY_CONTEXT_INVALID`.

El lock complementa el `@Version` de usuarios y roles; no lo reemplaza. Asignaciones
y concesiones mantienen claves compuestas y constraints de V4.

## Auditoría y seguridad

El evento global ahora distingue bootstrap, alta/estado de rol, estado de usuario,
asignación/revocación y concesión/revocación. Registra resultado, actor local,
IDs técnicos, permiso, código y versiones cuando aplican. No registra issuer,
subject, nombre visible, token, cookie, password ni secreto.

No se agregó REST, recurso Jakarta, backing bean, XHTML ni ruta `/admin/*`. El
bootstrap permanece `false` por defecto y solo puede ejecutarse desde la declaración
externa exacta ya documentada.

## Compilación principal

Se corrigió una incompatibilidad del script oficial `mvnw.cmd` con PowerShell 5.1:
una carpeta normal devuelve `Target=null` y el Wrapper intentaba indexarlo. El
cambio distingue carpeta normal de enlace sin alterar URL, versión o checksum de
Maven.

Comando final:

```powershell
$env:JAVA_HOME='C:\cosme\LogixoneJakarta11\.tools\jdk\jdk-21.0.11+10'
$env:MAVEN_USER_HOME='C:\cosme\LogixoneJakarta11\.tools\maven-wrapper-home'
.\mvnw.cmd -pl kernel-infrastructure-jakarta -am `
  "-Dmaven.repo.local=C:\cosme\LogixoneJakarta11\.tools\maven-repository" `
  "-Dmaven.test.skip=true" compile
```

Resultado final: `BUILD SUCCESS`, siete de siete módulos. La compilación previa con
la distribución ya resuelta por el Wrapper también fue verde y recompiló 105
fuentes de aplicación y 51 de infraestructura.

También se empaquetó la candidata visual sin compilar ni ejecutar tests:

```powershell
.\mvnw.cmd -pl distribution/logixone-war -am `
  -Pwith-screen-customization-plugins `
  "-Dmaven.test.skip=true" package
```

Resultado: `BUILD SUCCESS`, doce de doce módulos. El WAR final conserva
`kernel-infrastructure-jakarta`, el plugin de referencia y las personalizaciones A
y B. Su SHA-256 local es
`A5790F7070BEF0017B79543C425D887CA52B10E96205B58D28F32417FA9AA7E3`.

Dos intentos anteriores no compilaron código: el primero encontró el defecto de
inicio del Wrapper y el segundo detectó Java 8 mediante Maven Enforcer. Se configuró
el JDK 21 validado dentro de `.tools/` y se repitió correctamente.

Una selección inicial `-pl distribution` falló porque esa carpeta no es un módulo;
se corrigió a `distribution/logixone-war`. Un empaquetado base verde retiró los
plugins del WAR generado según su contrato y se repitió con el perfil visual para
preservar la candidata A/B.

## Revisión estática

- documentos y fuentes revisados: `invalid_utf8=0`;
- enlaces Markdown locales: `broken_links=0`;
- `persistence.xml`: XML bien formado;
- imports Jakarta/Hibernate/PostgreSQL en módulos puros: `0`;
- superficies `@Path`, `@WebServlet` o `@Named` nuevas en este corte: `0`;
- V4 permaneció inmutable con SHA-256
  `8C35EF550FFC0949915758389781B25F9243A1E49AEC8AC2AFC16F26CB46B67A`;
- el JAR de infraestructura contiene `persistence.xml`, las entidades V4, el
  repositorio JPA y la frontera JTA.

## Pruebas pendientes

Por la excepción autorizada de Sprint 4 no se ejecutaron ni compilaron JUnit,
ArchUnit, Testcontainers, PostgreSQL, JPA runtime, JTA/rollback, concurrencia,
Docker/Compose, health ni Playwright. Permanecen pendientes CA-01 a CA-10 y gates
G2–G7; la compilación principal no los sustituye.

No se observó una prueba fallida. La historia queda **Implementada pendiente de
pruebas**, no terminada ni verde.

## Siguiente paso

`J11-S4-04`: crear la frontera web administrativa confiable, exigir OIDC y permisos
globales en servidor, reducir denegaciones para evitar enumeración y no trasladar
reglas de negocio a JSF.
