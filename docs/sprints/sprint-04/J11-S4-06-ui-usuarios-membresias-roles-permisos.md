# J11-S4-06 — UI de usuarios, membresías, roles y permisos

- Estado: Completada; validada en `J11-S4-08`
- Sprint: 4
- Fecha: 2026-07-28
- Dependencias: `J11-S4-01` a `J11-S4-05` completadas
- ADR rector: [ADR-0009](../../adr/0009-autoridad-administrativa-global-kernel.md)

## Objetivo

Entregar pantallas Jakarta Faces, Material Design 3 y responsive para administrar
usuarios locales, membresías, roles y permisos empresariales, además de la autoridad
global del kernel, sin confundir ambos planos ni delegar autorización a Keycloak.

## Alcance funcional

### Seguridad empresarial

1. Listar y registrar usuarios locales por subject OIDC del issuer configurado.
2. Activar e inactivar usuarios con versión optimista y protección del último
   administrador global efectivo.
3. Seleccionar una empresa y administrar membresías de usuarios.
4. Registrar, activar e inactivar roles propios de esa empresa.
5. Asignar y desasignar roles de la misma empresa a sus membresías.
6. Conceder y revocar permisos funcionales de roles empresariales.
7. Ofrecer como nuevas concesiones únicamente permisos actualmente efectivos en
   la composición de plugins de la empresa; las concesiones históricas no efectivas
   permanecen visibles y revocables.

### Autoridad global

1. Listar, registrar, activar e inactivar roles globales.
2. Asignar y desasignar roles globales a usuarios locales.
3. Conceder y revocar exclusivamente permisos de `SystemPermission` conocidos.
4. Reutilizar `SystemAuthoritySafetyPolicy` y el lock transaccional para impedir
   que una mutación deje la instancia sin administrador global efectivo.

## Límites de seguridad

- `/admin/security.xhtml` exige `SECURITY_MANAGE`.
- `/admin/system-authority.xhtml` exige `SYSTEM_ADMINISTRATION_MANAGE`.
- Cada comando vuelve a autorizar el permiso exacto en el servidor.
- Un rol empresarial siempre contiene `CompanyId`; un rol global nunca lo contiene.
- Los roles de Keycloak autentican acceso al contenedor, pero no conceden permisos
  empresariales ni globales dentro del kernel.
- El issuer usado para un usuario nuevo proviene de la configuración OIDC validada;
  la UI no permite ingresar un issuer arbitrario.
- IDs, versiones, subject, códigos y permisos enviados por Faces son candidatos no
  confiables y se validan de nuevo en aplicación/dominio.
- No se administran contraseñas, MFA, realms, clientes ni usuarios internos de
  Keycloak.
- No se eliminan usuarios, membresías o roles. Las bajas lógicas conservan historia.
- Desasignar roles y revocar permisos son operaciones tipadas sobre claves exactas,
  no endpoints de borrado genérico.
- Una denegación o ID manipulado no expone SQL, stacktrace ni datos ajenos.

## Contratos requeridos

- Puertos neutrales distintos para seguridad empresarial y autoridad global.
- Proyecciones administrativas inmutables sin entidades JPA.
- Listados deterministas de usuarios, membresías, roles, asignaciones y grants.
- Nuevos comandos neutrales `UnassignRoleCommand` y `RevokePermissionCommand` para
  completar el ciclo empresarial.
- Auditoría de mutaciones con actor `AppUserId`, recurso técnico, resultado y
  correlación generada por el servidor.
- Beans request-scoped delgados, sin JPA ni reglas de negocio.

## Experiencia visual

- Material Design 3 sobre Jakarta Faces.
- Diseño usable a 375, 720 y 1280 px.
- Identificadores técnicos con corte de línea seguro y etiquetas persistentes.
- Selectores dependientes de empresa sin conservar autoridad en sesión.
- Confirmación explícita al inactivar usuario/membresía/rol, desasignar rol o
  revocar permiso.
- Conflictos de versión y protección del último administrador se presentan como
  estados recuperables.

## Criterios de aceptación

- **CA-01:** abrir o mutar seguridad empresarial sin `SECURITY_MANAGE` es denegado.
- **CA-02:** abrir o mutar autoridad global sin
  `SYSTEM_ADMINISTRATION_MANAGE` es denegado.
- **CA-03:** un usuario nuevo usa el issuer configurado, nace inactivo y no expone
  credenciales.
- **CA-04:** inactivar usuario respeta versión y no elimina membresías o roles.
- **CA-05:** toda membresía referencia usuario y empresa existentes y nace inactiva.
- **CA-06:** un rol empresarial no puede cruzarse a otra empresa.
- **CA-07:** asignación y desasignación exigen membresía y rol de la misma empresa.
- **CA-08:** una concesión nueva sólo acepta un permiso efectivo de la empresa.
- **CA-09:** una concesión histórica no efectiva queda identificada y puede
  revocarse sin inventar disponibilidad.
- **CA-10:** roles, asignaciones y permisos globales no contienen `CompanyId`.
- **CA-11:** revocar autoridad nunca puede eliminar al último administrador global.
- **CA-12:** cada acción vuelve a autorizarse y audita actor/correlación.
- **CA-13:** versiones obsoletas no sobrescriben usuarios, membresías ni roles.
- **CA-14:** los backing beans no importan JPA ni infraestructura.
- **CA-15:** las pantallas son utilizables a 375, 720 y 1280 px.
- **CA-16:** no existe gestión de Keycloak, REST administrativo ni borrado genérico.

## Migraciones

No se requiere migración nueva. Se reutilizan las tablas V3 de usuarios, membresías,
roles y permisos empresariales y las tablas V4 de autoridad global. V1–V4 permanecen
inmutables.

## Documentación afectada

- esta historia y evidencia técnica;
- índice de Sprint 4 y backlog activo;
- guía de implementación;
- runbooks de shell y Compose;
- navegación del panel administrativo.

## Resultado implementado

- Se añadieron las rutas protegidas `/admin/security.xhtml` y
  `/admin/system-authority.xhtml`, enlazadas desde la landing únicamente cuando el
  actor posee el permiso global correspondiente.
- La seguridad empresarial dispone de alta y estado de usuarios, membresías por
  empresa, roles empresariales, asignaciones y permisos funcionales efectivos.
- La autoridad global dispone de roles de instancia, asignaciones a usuarios y
  concesiones de `SystemPermission`, sin `CompanyId`.
- Las desasignaciones y revocaciones usan comandos tipados exactos; no se agregó
  borrado genérico ni administración de Keycloak.
- Cada acción relee la autoridad global actual, valida los candidatos recibidos y
  registra actor local con correlación del servidor.
- Las pantallas usan Jakarta Faces, Material Design 3 y composición responsive; la
  validación visual real a 375/720/1280 queda reservada para `J11-S4-08`.

La compilación de aplicación y web, el test-compile y el empaquetado de los doce
módulos terminaron correctamente con pruebas omitidas. El WAR contiene ambas rutas.
La evidencia reproducible se encuentra en
[J11-S4-06](../../evidence/J11-S4-06-ui-usuarios-membresias-roles-permisos.md).

## Matriz de pruebas ejecutada en J11-S4-08

Por la excepción aprobada, las pruebas se acumularon y ejecutaron en `J11-S4-08`:

- unitarias de consultas, comandos, revocación y mensajes;
- aislamiento estricto entre empresas;
- último administrador global y concurrencia optimista;
- PostgreSQL/JPA/JTA y rollback de asignaciones/grants;
- autorización positiva y negativa por ruta y acción;
- manipulación de IDs, versión, permission y subject;
- ArchUnit de dependencias web/aplicación;
- Playwright a 375/720/1280 px y regresión de las demos A/B.

El gate acumulado terminó verde sin fallos ni omisiones. La historia está
completada; esto no equivale a aptitud para producción.
