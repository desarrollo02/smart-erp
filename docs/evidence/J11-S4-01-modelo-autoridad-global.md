# Evidencia de J11-S4-01 — Modelo neutral de autoridad global

- Fecha: 2026-07-28
- Estado: implementada pendiente de pruebas
- Historia: [J11-S4-01](../sprints/sprint-04/J11-S4-01-modelo-autoridad-global.md)

## Resultado

Se agregó el contrato Java puro para separar autoridad administrativa global de
roles empresariales y claims de Keycloak.

`kernel-api` publica `SystemPermission` con cinco códigos iniciales:

- `kernel.company.manage`;
- `kernel.plugin.manage`;
- `kernel.security.manage`;
- `kernel.audit.view`;
- `kernel.system_administration.manage`.

`kernel-domain.security.system` incorpora identidad y código de rol global, estado,
rol versionado, asignación a `AppUserId`, concesión de permisos, resolución efectiva
y política de seguridad del último administrador.

## Invariantes materializados

- ningún tipo de rol global contiene `CompanyId`;
- usuario inactivo no obtiene permisos;
- rol inactivo no aporta concesiones;
- asignaciones de otro usuario o referencias ausentes fallan cerradas;
- permisos persistidos solo son efectivos si permanecen en el vocabulario disponible;
- un estado deseado completo solo es seguro si conserva al menos un usuario activo
  con rol activo y `kernel.system_administration.manage`;
- duplicados incompatibles de usuario o rol producen `INVALID_CONTEXT`.

## Revisión estática manual

Sin ejecutar compilación ni pruebas se revisaron constructores, nulos, regex,
comparabilidad, colecciones inmutables, imports y rutas fail-closed.

Durante esa revisión se corrigieron dos defectos antes de cerrar la implementación:

1. las constantes de `SystemPermission` se habían declarado antes del patrón usado
   por su constructor; el patrón ahora se inicializa primero;
2. la política del último administrador podía devolver `SAFE` antes de validar una
   asignación posterior inválida; ahora valida la colección completa y decide al final.

No queda un defecto conocido dentro del alcance. Esta afirmación no sustituye la
compilación ni la matriz automatizada pendiente.

## Archivos de código creados

- `kernel-api/.../security/SystemPermission.java`;
- `kernel-domain/.../security/system/SystemRoleId.java`;
- `SystemRoleCode.java`;
- `SystemRoleStatus.java`;
- `SystemRole.java`;
- `AppUserSystemRoleAssignment.java`;
- `SystemRolePermissionGrant.java`;
- `SystemSecurityDiagnosticCode.java`;
- `EffectiveSystemPermissionResolution.java`;
- `EffectiveSystemPermissionPolicy.java`;
- `SystemAuthoritySafetyStatus.java`;
- `SystemAuthoritySafetyPolicy.java`.

También se actualizaron ADR-0009, arquitectura, guía `1.0-rc12`, ficha de validación,
historia, Sprint e índices.

## Pruebas pendientes

Por decisión de producto no se ejecutaron Maven, JUnit ni ArchUnit. Permanecen
pendientes:

- validación de constructores y valores canónicos;
- combinaciones de usuario/rol activo e inactivo;
- referencias ausentes y duplicados incompatibles;
- intersección de permisos disponibles;
- cero, uno y varios administradores;
- simulación de cada revocación del último administrador;
- límites arquitectónicos de módulos puros;
- compilación de reactor y `mvn verify` acumulado.

Una ejecución futura fallida bloqueará el avance y exigirá corregir la causa.

## Siguiente paso

`J11-S4-02`: diseñar y agregar la migración aditiva `core` V4, puertos de bootstrap
global y declaración externa del primer administrador. La historia deberá quedar
implementada con únicamente sus pruebas pendientes.
