# J11-S4-01 — Contratos y modelo neutral de autoridad global

- Estado: Completada; validada en `J11-S4-08`
- Fecha: 2026-07-28
- Dependencia: `J11-S4-00` completada documentalmente

## Objetivo

Modelar en Java puro la autoridad administrativa global del kernel, sus permisos,
roles, asignaciones, concesiones y políticas fail-closed antes de agregar tablas,
JPA, CDI o pantallas.

## Alcance

- identificador neutral y validado de permiso global;
- identidad, código, estado y versión de rol global;
- asignación de roles globales a usuarios locales;
- concesión de permisos globales a roles;
- resolución de permisos efectivos para un usuario;
- política que exige al menos un administrador global efectivo en un estado
  propuesto;
- diagnósticos cerrados para contextos inconsistentes;
- documentación de contratos e invariantes.

## Fuera de alcance

- migración `core` V4;
- repositorios, JPA, JTA o CDI;
- comandos administrativos y bootstrap;
- integración OIDC adicional;
- ruta `/admin/*` o pantallas JSF;
- ejecutar pruebas automatizadas en este corte.

## Diseño

`SystemPermission` pertenece a `kernel-api` porque es un contrato neutral que
utilizarán aplicación y adaptadores. Expone únicamente códigos estables conocidos
por el kernel y conserva validación sintáctica para evolución compatible.

El modelo de roles globales permanece en `kernel-domain.security.system`. No
contiene `CompanyId`: mezclarlo con roles empresariales debe resultar imposible por
tipos. `EffectiveSystemPermissionPolicy` intersecta concesiones con el vocabulario
actualmente disponible. Concesiones históricas desconocidas pueden persistir sin
volverse efectivas.

`SystemAuthoritySafetyPolicy` recibe el estado completo que un caso de uso desea
persistir. Falla cerrado si hay referencias inconsistentes y exige al menos un
usuario activo, asignado a un rol global activo que conceda
`kernel.system_administration.manage`. Los servicios futuros deberán construir el
estado posterior a una mutación y ejecutar esta política antes de escribir.

## Criterios de aceptación

- **CA-01:** `SystemPermission` rechaza valores no canónicos y contiene los cinco
  permisos iniciales de ADR-0009.
- **CA-02:** IDs y códigos de rol global son opacos, comparables y validados.
- **CA-03:** un rol global no contiene empresa.
- **CA-04:** nombres de presentación rechazan blancos, controles y exceso de longitud.
- **CA-05:** versiones negativas se rechazan.
- **CA-06:** asignaciones relacionan `AppUserId` y `SystemRoleId` sin `CompanyId`.
- **CA-07:** concesiones relacionan rol y permiso global tipado.
- **CA-08:** usuario inactivo no obtiene permisos efectivos.
- **CA-09:** rol inactivo no aporta permisos.
- **CA-10:** referencias cruzadas ausentes o incompatibles producen denegación cerrada.
- **CA-11:** permisos efectivos se intersectan con el vocabulario disponible.
- **CA-12:** la política de seguridad exige al menos un administrador efectivo.
- **CA-13:** la política rechaza usuarios, roles, asignaciones o concesiones
  estructuralmente inconsistentes.
- **CA-14:** los módulos puros no importan Jakarta, JPA, Keycloak ni JSF.
- **CA-15:** documentación y guía describen el contrato y enlazan su evidencia de validación.

## Matriz de pruebas ejecutada en J11-S4-08

- unitarias de valores nulos, UUID canónico, regex, nombres y versiones;
- unitarias de resolución con usuario/rol activo e inactivo;
- referencias ausentes, duplicados incompatibles y permisos no disponibles;
- cero, uno y varios administradores efectivos;
- simulación de revocar asignación, permiso, rol o usuario del último administrador;
- ArchUnit para límites de `kernel-api` y `kernel-domain`;
- `mvnw.cmd -pl kernel-domain -am test`;
- `mvnw.cmd verify` en el gate acumulado.

La matriz se ejecutó en `J11-S4-08` mediante pruebas unitarias, ArchUnit y el reactor
completo. Todos los alcances quedaron verdes sin omisiones.

## Resultado

El contrato y modelo neutral quedaron implementados, documentados y validados. La
evidencia específica está en
[J11-S4-01](../../evidence/J11-S4-01-modelo-autoridad-global.md) y el gate acumulado
en [J11-S4-08](../../evidence/J11-S4-08-validacion-demo-cierre.md).

## Siguiente paso condicionado

La historia está completada. `J11-S4-02` a `J11-S4-07` también quedaron validadas;
el cierre de Sprint conserva pendiente únicamente el recorrido independiente.
