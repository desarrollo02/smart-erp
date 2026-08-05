# J11-S3-01 — Evidencia del modelo neutral de identidad y autorización

- Fecha: 2026-07-28
- Estado: Completada; validación acumulada en [J11-S3-08](J11-S3-08-validacion-demo-cierre.md)
- Tipo de cambio: contratos y políticas Java puras, puertos de aplicación y documentación
- Dependencia: ADR-0006 y `J11-S3-00` completada

## Objetivo ejecutado

Se materializó el modelo neutral que permitirá vincular una identidad OIDC validada con un usuario local, resolver sus membresías empresariales y calcular permisos sin introducir todavía Jakarta, Keycloak, HTTP, JWT, JPA, SQL o UI.

## Superficie incorporada

### `kernel-api` — 3 fuentes

- `AppUserId`: UUID canónico, opaco, comparable e inmutable.
- `AuthenticatedActor`: referencia pública mínima al usuario local; no expone claims.
- `AuthenticatedCompanyContext`: actor y empresa ya validados, compatible con `CompanyContext`.

### `kernel-domain` — 17 fuentes

- `ExternalIdentity`: issuer OIDC canónico y subject exacto, con límites y sin usar correo/username como identidad.
- `AppUser`, `CompanyMembership` y `CompanyRole`: estados cerrados, versiones no negativas e IDs tipados.
- `RoleCode`, `RoleId`, `MembershipRoleAssignment` y `RolePermissionGrant`: autorización empresarial explícita.
- `CompanyAccessPolicy`: resolución determinista para cero, una o varias membresías.
- `CompanySelectionResolution`: resultado inmutable `SELECTED`, `SELECTION_REQUIRED` o `DENIED`.
- `EffectivePermissionPolicy`: validación fail-closed e intersección con permisos actualmente disponibles.
- `EffectivePermissionResolution` y `SecurityDiagnosticCode`: salida tipada, estable y sin detalles de infraestructura.

### `kernel-application` — 4 fuentes

- `AppUserIdGenerator`;
- `AppUserRepository`;
- `CompanyMembershipRepository`;
- `CompanyAuthorizationRepository`.

Los puertos son contratos. Sus adaptadores JPA/JTA pertenecen a `J11-S3-03` y no se adelantaron.

## Reglas implementadas

1. Issuer debe ser una URL HTTP/HTTPS absoluta y canónica, sin user-info, query ni fragmento.
2. Subject conserva comparación exacta y rechaza vacío, bordes en blanco, controles y longitud excesiva.
3. Usuario, membresía y rol son inactivos o activos; `null` y versiones negativas se rechazan.
4. Una colección de membresías de otro usuario produce denegación por contexto inválido.
5. Cero membresías activas deniega sin exponer empresas.
6. Una membresía activa se selecciona automáticamente.
7. Varias membresías activas exigen elección y se devuelven ordenadas y sin duplicados.
8. Una empresa solicitada solo se selecciona si está en las membresías activas.
9. Roles, asignaciones o concesiones cruzados entre empresas producen denegación completa.
10. Solo roles activos asignados aportan permisos.
11. Una concesión histórica solo es efectiva si su `ContributionId` aparece en los permisos disponibles de plugins efectivos.
12. Resultados y colecciones son inmutables y deterministas.

## Límites arquitectónicos

El control estático examinó las 24 fuentes nuevas:

```text
NEW_JAVA_FILES=24
FORBIDDEN_IMPORTS=0
kernel-api=3
kernel-domain=17
kernel-application=4
```

Se buscaron imports de Jakarta, `javax`, Keycloak, Hibernate, WildFly, JDBC y `javax.sql`. No se encontró ninguno. El WAR empaquetado contiene `WAR_KEYCLOAK_ENTRIES=0`.

## Compilaciones ejecutadas

Todas usaron Maven Wrapper 3.9.16, JDK `21.0.11+10`, repositorio local `.tools/maven-repository` y `-DskipTests`.

### Corte 1 — API

```powershell
mvnw.cmd -B -DskipTests -pl kernel-api -am package
```

Resultado: 2 de 2 módulos `SUCCESS`; 5 fuentes principales compiladas; Surefire informó `Tests are skipped`.

### Corte 2 — dominio

```powershell
mvnw.cmd -B -DskipTests -pl kernel-domain -am package
```

Resultado: 4 de 4 módulos `SUCCESS`; `kernel-domain` compiló 31 fuentes principales; pruebas omitidas.

### Corte 3 — aplicación

```powershell
mvnw.cmd -B -DskipTests -pl kernel-application -am package
```

Resultado: 5 de 5 módulos `SUCCESS`; `kernel-application` compiló 46 fuentes principales; pruebas omitidas.

### Corte 4 — reactor completo

```powershell
mvnw.cmd -B -DskipTests package
```

Resultado: 16 de 16 módulos `SUCCESS`, WAR ensamblado, 0 pruebas ejecutadas, tiempo Maven `23.236 s`.

La inspección de JAR confirmó las tres clases públicas de API, las 17 fuentes de dominio con sus clases compiladas y los cuatro puertos de aplicación.

## Incidencias

1. El primer intento invocó el Wrapper sin `MAVEN_USER_HOME`; el script híbrido encontró un home Maven nulo y terminó antes de Maven. No modificó archivos.
2. Al fijar el home local, Maven inició con el Java 8 global. Enforcer rechazó correctamente la versión antes de compilar.
3. Se fijaron `MAVEN_USER_HOME=.tools/maven-wrapper-home` y `JAVA_HOME=.tools/jdk/jdk-21.0.11+10`; los cortes posteriores quedaron verdes.

Los dos fallos fueron de entorno previo a compilación. No se relajó Enforcer ni se cambió Wrapper/POM.

## Pruebas diferidas

No se ejecutaron JUnit, ArchUnit, Testcontainers, Docker, OIDC ni Playwright. Tampoco se agregaron todavía los casos automatizados: se incorporarán a la matriz acumulada de `J11-S3-08` conforme a la decisión de producto.

La compilación verde demuestra sintaxis, tipos, dependencias y ensamblado; no demuestra todavía todos los comportamientos de los 14 criterios. Por eso el estado no es `Completada`.

## G0 documental

Después de actualizar historia, arquitectura, estrategia, guía, índices y esta evidencia se obtuvo:

```text
MARKDOWN_FILES=83
BAD_FILES=0
LOCAL_LINKS=231
BROKEN_LINKS=0
```

`BAD_FILES=0` combina UTF-8 estricto, ausencia de caracteres de reemplazo y patrones de texto dañado.

## Cobertura provisional de aceptación

| Criterio | Estado antes del gate acumulado |
|---|---|
| `CA-01` | Implementado: superficie pública mínima en `kernel-api.security`. |
| `CA-02` | Implementado: validación canónica de issuer/subject. |
| `CA-03` | Implementado: igualdad solo por issuer/subject. |
| `CA-04` | Implementado estructuralmente: enums cerrados, no nulos y versiones válidas; pruebas pendientes. |
| `CA-05` | Implementado: membresía tipada sin JPA. |
| `CA-06` | Implementado: política rechaza cruce de empresa/rol. |
| `CA-07` | Implementado: concesiones usan `ContributionId` público. |
| `CA-08` | Implementado: cero/una/múltiples membresías. |
| `CA-09` | Implementado: empresa solicitada sin membresía deniega. |
| `CA-10` | Implementado: intersección de concesiones y permisos disponibles. |
| `CA-11` | Implementado: copias inmutables y orden determinista. |
| `CA-12` | Control estático: 0 imports prohibidos. |
| `CA-13` | Arquitectura y guía `1.0-rc3` actualizadas. |
| `CA-14` | Casos requeridos identificados; ejecución diferida a `J11-S3-08`. |

## Siguiente paso

La excepción temporal autoriza iniciar `J11-S3-02`: migración aditiva `core` V3. Esta historia permanece `Implementada pendiente de validación` hasta que `J11-S3-08` ejecute JUnit y ArchUnit y resuelva cualquier hallazgo.
