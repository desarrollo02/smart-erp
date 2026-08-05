# Evidencia J11-S3-03 — Persistencia y casos de uso de seguridad

- Fecha: 2026-07-28
- Estado: Completada; validación acumulada en [J11-S3-08](J11-S3-08-validacion-demo-cierre.md)
- Tipo de entrega: modelo de presentación, aplicación neutral, persistencia JPA y límites JTA
- Historia: [J11-S3-03](../sprints/sprint-03/J11-S3-03-persistencia-casos-uso-seguridad.md)

## Resultado

La candidata ya puede representar y empaquetar el recorrido interno entre identidad local, membresía, rol y permiso:

```text
ExternalIdentity
  -> AppUserRepository
  -> CompanyMembershipRepository
  -> CompanyAuthorizationRepository
  -> políticas neutrales
  -> selección empresarial / permisos efectivos
```

Todavía no existe el adaptador OIDC que construye `ExternalIdentity` desde un principal validado; corresponde a `J11-S3-04` y `J11-S3-05`.

## Ajuste del modelo neutral

`AppUser` ahora conserva un nombre de presentación opcional y `CompanyRole` uno obligatorio, ambos con el límite de 160 caracteres y las mismas reglas de limpieza de V3. Esos valores son mutables y no participan en la identidad:

- el usuario continúa identificado por UUID local y `(issuer, subject)` externo;
- el rol continúa identificado por UUID y código único dentro de la empresa;
- correo, username, nombre visible o claims no se usan como claves de autorización.

Se agregó `USER_NOT_REGISTERED` como diagnóstico neutral para una identidad OIDC válida que no tenga usuario local.

## Mapeo JPA privado

La unidad `logixone-core-pu` enumera 11 clases de persistencia: las tres existentes de V2 y ocho clases nuevas para V3. Las nuevas estructuras son:

| Tabla V3 | Entidad | Clave |
|---|---|---|
| `core.app_user` | `AppUserEntity` | UUID simple |
| `core.company_membership` | `CompanyMembershipEntity` | `CompanyMembershipKey` |
| `core.security_role` | `SecurityRoleEntity` | UUID simple |
| `core.role_permission` | `RolePermissionEntity` | `RolePermissionKey` |
| `core.membership_role` | `MembershipRoleEntity` | `MembershipRoleKey` |

Las entidades pertenecen únicamente a `kernel-infrastructure-jakarta`. No contienen asociaciones JPA a plugins y convierten hacia/desde records neutrales.

Los adaptadores agregados son:

- `JpaAppUserRepository`, con resolución exacta por issuer/subject y versión optimista;
- `JpaCompanyMembershipRepository`, con consulta por usuario/empresa y versión optimista;
- `JpaCompanyAuthorizationRepository`, con roles, asignaciones y concesiones siempre acotados por empresa;
- `UuidAppUserIdGenerator` y `UuidRoleIdGenerator`;
- `PostgreSqlSecurityConflictMapper`, que traduce SQLSTATE y restricciones conocidas a `SecurityPersistenceCode`.

Los puertos no exponen `EntityManager`, entidades, `PersistenceException`, Hibernate, JDBC ni `PSQLException`.

## Casos de uso

`SecurityAdministrationService` implementa:

1. alta local de usuario como `INACTIVE`;
2. cambio versionado de estado de usuario;
3. alta de membresía como `INACTIVE`;
4. cambio versionado de estado de membresía;
5. alta de rol empresarial como `INACTIVE`;
6. cambio versionado de estado de rol;
7. asignación de un rol únicamente a una membresía de la misma empresa;
8. concesión de un `ContributionId` sin exigir que el plugin esté actualmente presente.

Los resultados esperados son `CHANGED`, `UNCHANGED` o `REJECTED`. Un cambio que ya coincide se audita como `UNCHANGED` y no llama a una mutación de entidad, por lo que no debe incrementar la versión. La comprobación runtime queda pendiente.

`SecurityQueryService` resuelve empresas y permisos leyendo repositorios en cada llamada. No mantiene una caché de membresías o roles: inactivar una membresía o rol debe afectar la siguiente consulta. Los permisos persistidos se intersectan con las contribuciones públicas vigentes, por lo que un código desconocido se conserva históricamente pero no se vuelve efectivo.

## Bootstrap cerrado

`SecurityBootstrapPort` es un puerto interno implementado por `TransactionalSecurityUseCases`. No existe referencia al puerto o al comando de bootstrap desde `web-shell` ni desde la distribución, y no se agregó REST, Faces ni autoaprovisionamiento posterior al login.

La declaración recibe externamente:

- issuer/subject exactos y nombre visible opcional;
- empresa activa y personalización esperada;
- código/nombre del rol inicial;
- conjunto no vacío de permisos públicos.

Si no existe el usuario, crea usuario activo, membresía activa, rol activo si hace falta, asignación y concesiones. Si la identidad ya existe, exige que toda la declaración inicial sea compatible; una repetición idéntica devuelve `UNCHANGED` y una declaración parcial o incompatible devuelve un código de bootstrap y no escribe nuevas filas.

La futura lectura de configuración externa, el secreto local y el orden posterior a migraciones pertenecen a `J11-S3-04`. No hay datos reales ni identidades semilla versionados.

## Transacciones y auditoría

`TransactionalSecurityUseCases` aplica `@Transactional(rollbackOn = RuntimeException.class)` a las ocho operaciones administrativas y al bootstrap. Una excepción propia de persistencia o un fallo del puerto obligatorio de auditoría sale del límite transaccional y debe revertir todas las escrituras del caso de uso.

`StructuredSecurityAudit` registra únicamente operación, resultado, IDs técnicos, código, versiones, instante y actor local. No registra issuer, subject, nombre visible, claims, passwords, tokens, cookies ni SQL.

`TransactionalSecurityQueries` usa una transacción de soporte y reconstruye la decisión desde el estado actual.

## Configuración de esquema

Se conservan estas propiedades:

```text
jakarta.persistence.schema-generation.database.action=none
jakarta.persistence.schema-generation.scripts.action=none
hibernate.hbm2ddl.auto=validate
```

`ManagedDataSourceHealthQueries.EXPECTED_CORE_SCHEMA_VERSION` cambió de `2` a `3`. La aplicación candidata no debe considerarse disponible sobre una base que todavía no tenga V3.

## Auditoría estática de límites

```text
JPA_ENTITIES_TOTAL=7
PERSISTENCE_XML_CLASSES=11
APPLICATION_SECURITY_SOURCES=30
INFRASTRUCTURE_SECURITY_SOURCES=3
FORBIDDEN_IMPORTS_IN_API_DOMAIN_APPLICATION=0
BOOTSTRAP_REFERENCES_IN_WEB_SHELL_OR_DISTRIBUTION=0
EXPECTED_CORE_SCHEMA_VERSION=3
DDL_DATABASE_ACTION=none
HIBERNATE_HBM2DDL=validate
```

Los imports prohibidos buscados fueron `jakarta.*`, `javax.*` y `org.keycloak.*` dentro de `kernel-api`, `kernel-domain` y `kernel-application`.

## Empaquetado

Se utilizó JDK 21.0.11 y Maven Wrapper con sus cachés bajo `.tools/`. Todos los comandos omitieron la ejecución de pruebas conforme a la excepción temporal del Sprint 3; Maven sí compiló las fuentes de prueba existentes.

### Modelo y aplicación

```powershell
mvnw.cmd -B -DskipTests -pl kernel-application -am package
```

Resultado final del incremento de aplicación:

- 5 de 5 módulos correctos;
- 72 fuentes principales de `kernel-application` compiladas;
- 7 fuentes de prueba compiladas, no ejecutadas;
- tiempo Maven: 5.326 s.

### Infraestructura Jakarta

```powershell
mvnw.cmd -B -DskipTests -pl kernel-infrastructure-jakarta -am package
```

Resultado final:

- 7 de 7 módulos correctos;
- 36 fuentes principales de infraestructura compiladas;
- 6 fuentes de prueba compiladas, no ejecutadas;
- tiempo Maven: 10.664 s.

### Reactor completo

```powershell
mvnw.cmd -B -DskipTests package
```

Resultado final:

- 16 de 16 módulos correctos;
- WAR ensamblado;
- pruebas omitidas;
- tiempo Maven: 14.092 s.

Artefactos del corte:

| Artefacto | Tamaño | SHA-256 |
|---|---:|---|
| `kernel-application-0.1.0-SNAPSHOT.jar` | 122,610 bytes | `F88D67659E9F231FFAF1A8ECCC197605070BB454CD6C2CD6D51FCEF22E3726A9` |
| `kernel-infrastructure-jakarta-0.1.0-SNAPSHOT.jar` | 63,481 bytes | `8CFC2C99B505023B8E34429439BF776875FEFA548907F173998AADE7595B2C8C` |
| `logixone.war` | 261,219 bytes | `941EB0D4BDF7E914F6DD7AFD49708C9EE502EB9EBC2853DB180952E2532BE6F6` |

El empaquetado correcto demuestra compatibilidad de compilación y ensamblado, no comportamiento JPA/JTA.

## Cobertura provisional de criterios

| Criterio | Evidencia actual | Estado |
|---|---|---|
| CA-01 | entidades y claves bajo infraestructura Jakarta | Cubierto estáticamente |
| CA-02 | 0 imports Jakarta/Javax/Keycloak en capas neutrales | Cubierto estáticamente |
| CA-03 | lookup previo y código estable para identidad duplicada | Runtime pendiente |
| CA-04 | ramas `UNCHANGED` no ejecutan `save` | Runtime pendiente |
| CA-05 | versiones JPA y rollback JTA declarado | Concurrencia pendiente |
| CA-06 | validación previa de empresa del rol y FK compuesta | Runtime pendiente |
| CA-07 | consultas sin caché y políticas filtran estados | Runtime pendiente |
| CA-08 | concesión acepta código público y política intersecta disponibilidad | Runtime pendiente |
| CA-09 | preflight completo y resultado bootstrap idempotente/incompatible | Runtime pendiente |
| CA-10 | auditoría dentro de límite JTA con rollback de runtime exception | Runtime pendiente |
| CA-11 | excepciones técnicas traducidas a `SecurityPersistenceException` | Runtime pendiente |
| CA-12 | auditoría sin claims ni datos de presentación | Cubierto estáticamente |
| CA-13 | DDL `none`, Hibernate `validate` | Cubierto estáticamente |
| CA-14 | matriz acumulada trazada | Pendiente en S3-08 |

## Pruebas aplazadas y deuda explícita

No se ejecutaron JUnit, ArchUnit, Testcontainers, PostgreSQL, Flyway, JPA runtime, JTA runtime, Docker, Compose ni smoke tests. Ninguno de esos gates se presenta como aprobado.

Antes de ejecutar el gate acumulado deben actualizarse y ampliarse, como mínimo:

- `CoreDatabaseProbeTest`, que todavía espera readiness de versión 2 y fallará con el nuevo requisito V3;
- `CoreMigrationPostgreSqlIT`, que todavía contiene escenarios y conteos centrados en V1/V2;
- `JpaEntityMappingTest`, para cubrir las cinco entidades V3 y sus claves;
- `PostgreSqlRepositoryIT`, para repositorios, idempotencia, versiones, unicidades y cruces de empresa;
- pruebas neutrales para comandos, resultados, consultas, permisos desconocidos y bootstrap;
- arnés WildFly/JTA para commit, rollback por conflicto y rollback por fallo de auditoría.

Estas son validaciones no ejecutadas, no resultados fallidos observados, salvo que la aserción histórica de readiness V2 es conocida como incompatible con el nuevo valor fuente V3.

## Gate documental G0

Se recorrieron los Markdown del repositorio excluyendo `.git`, `.tools`, `target` y temporales. Cada archivo se decodificó con UTF-8 estricto, se buscaron caracteres de reemplazo y marcadores de mojibake, y se resolvieron los enlaces locales desde su documento de origen.

```text
MARKDOWN_FILES=85
BAD_FILES=0
LOCAL_LINKS=244
BROKEN_LINKS=0
```

También se buscaron referencias obsoletas a JPA pendiente, aplicación que exigiera V2 o historia S3-03 pendiente. Solo permaneció `1.0-rc4` como entrada histórica válida de la guía. G0 queda cubierto para esta historia.

## Archivos del cambio

- modelo neutral en `kernel-domain/.../security/`;
- aplicación, comandos, auditoría y puertos en `kernel-application/.../security/`;
- entidades, claves, repositorios y generadores en `kernel-infrastructure-jakarta/.../persistence/`;
- límites JTA y auditoría estructurada en `kernel-infrastructure-jakarta/.../security/`;
- `kernel-infrastructure-jakarta/src/main/resources/META-INF/persistence.xml`;
- `kernel-infrastructure-jakarta/src/test/resources/META-INF/persistence.xml`;
- `ManagedDataSourceHealthQueries.java`;
- historia, Sprint, arquitectura, estrategia de pruebas, guía, validación, runbook e índices documentales.

## Conclusión y siguiente paso

`J11-S3-03` queda **Implementada pendiente de validación**. El siguiente trabajo autorizado es `J11-S3-04`: declarar Keycloak, configurar `elytron-oidc-client` y conectar el bootstrap externo después de migraciones sin publicar datos o secretos reales.
