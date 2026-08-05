# Evidencia de J11-S4-02 — Migración `core` V4 y bootstrap global

- Fecha: 2026-07-28
- Estado: implementada pendiente de pruebas
- Historia: [J11-S4-02](../sprints/sprint-04/J11-S4-02-migracion-core-v4-bootstrap-global.md)

## Resultado

Se agregó una migración aditiva V4 con tres tablas privadas de `core`:

- `system_role` para rol global versionado;
- `system_role_permission` para permisos administrativos;
- `app_user_system_role` para asignaciones a usuarios locales.

No contienen `company_id`. Las FKs hacia `app_user` y `system_role` usan
`ON DELETE RESTRICT`; estados, códigos, nombres, versiones y timestamps tienen
checks. Readiness pasó de versión esperada 3 a 4 sin agregar ejecución de Flyway a
la aplicación.

## Inmutabilidad de migraciones

Los SHA-256 observados después de agregar V4 son:

| Recurso | SHA-256 |
|---|---|
| V1 | `07A375F06F9EBB9D6E6EC162E113ADA35397348BFCD03486870FAF28CC424DA6` |
| V2 | `F5186A3817F7A31569C58551A9339911B29B44F7409E47AE470FC999AFA5CC11` |
| V3 | `6C34C64C0739F4988287C7B9DBA5A0DFF2808C976B30A0B2C066F382F7961170` |
| V4 | `8C35EF550FFC0949915758389781B25F9243A1E49AEC8AC2AFC16F26CB46B67A` |

V1–V3 coinciden con sus identidades documentadas anteriores. No se ejecutó
Flyway/PostgreSQL, por lo que todavía no existe checksum runtime observado para V4.

## Bootstrap neutral

`kernel-application.security.system` incorpora:

- `BootstrapSystemAuthorityCommand`;
- `SystemAuthorityBootstrapState`;
- `SystemAuthorityBootstrapService`;
- `SystemAuthorityRepository` y `SystemAuthorityBootstrapPort`;
- `SystemRoleIdGenerator`;
- evento y puerto de auditoría global.

El servicio reutiliza `AppUser` por identidad OIDC exacta o lo crea activo, crea un
rol global activo cuando no existe, asigna el rol y materializa los permisos
declarados. Exige `kernel.system_administration.manage`, evalúa
`SystemAuthoritySafetyPolicy`, devuelve `UNCHANGED` para un estado compatible y
rechaza identidad, rol, asignación, permisos o contexto incompatibles.

La auditoría registra solo `AppUserId`, `SystemRoleId`, operación, resultado, código
y tiempo. No registra issuer, subject, display name, token, cookie o secreto.

## Frontera Jakarta y configuración

- `UuidSystemRoleIdGenerator` aporta UUID técnico;
- `StructuredSystemAuthorityAudit` emite el evento seguro;
- `ConfiguredSystemAuthorityBootstrap` consume configuración externa opt-in;
- el adaptador usa `Instance<SystemAuthorityBootstrapPort>` para permitir el
  baseline deshabilitado sin inventar persistencia;
- si se habilita antes de `J11-S4-03`, falla con `AdapterUnavailable`;
- no existe recurso REST, bean Faces, ruta `/admin/*` ni lectura SQL directa.

Compose y su ejemplo declaran `LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_*` con
`ENABLED=false`. Los cinco permisos conocidos aparecen como ejemplo no sensible;
subject y nombre real permanecen vacíos y locales.

## Compilación realizada

Comando final para infraestructura y dependencias:

```powershell
.\mvnw.cmd -B -pl kernel-infrastructure-jakarta -am `
  "-Dmaven.test.skip=true" compile
```

Resultado: `BUILD SUCCESS`, siete de siete módulos, 97 fuentes de aplicación y 44
de infraestructura compiladas.

Después de ajustar validación del nombre opcional se repitió:

```powershell
.\mvnw.cmd -B -pl kernel-application -am `
  "-Dmaven.test.skip=true" compile
```

Resultado: `BUILD SUCCESS`, cinco de cinco módulos.

Una invocación anterior sin comillas hizo que PowerShell entregara
`.test.skip=true` como fase y Maven terminó antes del reactor. Se corrigió el
comando; no fue una prueba ni un defecto de código.

## Pruebas pendientes

No se ejecutaron ni compilaron JUnit, ArchUnit, Testcontainers, PostgreSQL,
Docker/Compose, health runtime, JPA o JTA. Permanecen pendientes los escenarios de
la historia y los gates G2–G7. En particular, el nuevo puerto no puede habilitarse
operativamente hasta que `J11-S4-03` materialice su adaptador JPA/JTA.

## Siguiente paso

`J11-S4-03`: mapear V4 con entidades privadas, implementar repositorio y límite JTA,
y exponer casos de uso transaccionales que conserven la protección del último
administrador.
