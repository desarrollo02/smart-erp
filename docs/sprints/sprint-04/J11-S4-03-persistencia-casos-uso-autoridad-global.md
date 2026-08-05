# J11-S4-03 — Persistencia JPA/JTA y casos de uso de autoridad global

- Estado: Completada; validada en `J11-S4-08`
- Fecha: 2026-07-28
- Dependencias: `J11-S4-01`, `J11-S4-02`, ADR-0009 y migración `core` V4
- Política de pruebas: matriz automatizada diferida a `J11-S4-08`

## Objetivo

Materializar el modelo neutral de autoridad global sobre las tablas privadas de
`core` V4 y ofrecer casos de uso transaccionales para roles globales, asignaciones,
permisos y estado de usuarios. Ninguna operación podrá dejar la instancia sin al
menos un administrador global efectivo.

## Alcance

- entidades JPA privadas para `system_role`, `system_role_permission` y
  `app_user_system_role`;
- repositorio JPA que implementa el puerto neutral de autoridad global;
- bloqueo transaccional serializado para proteger el último administrador;
- alta y cambio de estado de roles globales;
- asignación y revocación de roles globales;
- concesión y revocación de permisos globales conocidos;
- cambio de estado de usuario protegido por la autoridad global;
- resolución de permisos globales efectivos;
- adaptador JTA para el bootstrap one-shot y las operaciones administrativas;
- auditoría estructurada con actor, operación, resultado e identificadores técnicos.

## Límites

- no se agrega REST, ruta `/admin/*`, backing bean ni pantalla;
- no se modifica V4 ni se usa generación automática de esquema;
- no se administran contraseñas ni roles de Keycloak;
- no se permite autoridad global derivada de una empresa;
- no se habilita el bootstrap por defecto.

## Criterios de aceptación

- **CA-01:** la unidad JPA declara explícitamente las entidades privadas de V4 y
  mantiene `hibernate.hbm2ddl.auto=validate`.
- **CA-02:** el repositorio traduce entidades a dominio y no filtra tipos Jakarta al
  dominio o a la aplicación.
- **CA-03:** altas repetidas son idempotentes o producen un conflicto tipado.
- **CA-04:** los cambios versionados detectan versiones obsoletas.
- **CA-05:** desactivar un usuario o rol, revocar una asignación o revocar el permiso
  de administración se rechaza si eliminaría al último administrador efectivo.
- **CA-06:** las mutaciones destructivas se serializan dentro de una transacción JTA
  para impedir dos revocaciones concurrentes incompatibles.
- **CA-07:** solo se conceden permisos incluidos en el catálogo global conocido.
- **CA-08:** el bootstrap completo se ejecuta dentro de una única transacción y queda
  operativo únicamente cuando la declaración explícita lo habilita.
- **CA-09:** toda mutación registra actor técnico, operación, resultado e IDs sin
  guardar claims, tokens ni secretos.
- **CA-10:** no aparece una superficie web o anónima nueva.

## Decisión de concurrencia

PostgreSQL usará un `pg_advisory_xact_lock` de clave fija, adquirido por el
repositorio dentro de la transacción JTA antes de leer el snapshot y ejecutar una
mutación que pueda reducir autoridad. El lock se libera automáticamente al cerrar
la transacción. Esta serialización complementa, no reemplaza, el `@Version` de
usuarios y roles.

## Matriz de pruebas ejecutada en J11-S4-08

| Nivel | Comprobación | Estado |
|---|---|---|
| JUnit | comandos, altas, idempotencia, conflictos y catálogo de permisos | Verde |
| JUnit | política del último administrador en cada mutación destructiva | Verde |
| JUnit | resolución efectiva y contextos inválidos | Verde |
| JPA/PostgreSQL | mapeo V4, constraints y traducción de conflictos | Verde |
| JTA/PostgreSQL | commit, rollback y bloqueo concurrente | Verde |
| Integración | bootstrap deshabilitado, habilitado e idempotente | Verde |
| ArchUnit | límites aplicación/dominio/Jakarta | Verde |
| Maven | `mvn verify` acumulado | Verde |

La matriz se completó sin fallos ni omisiones en `J11-S4-08`.

## Documentación afectada

- arquitectura general;
- guía de implementación;
- runbooks de migración y Compose;
- backlog y estado de Sprint 4;
- evidencia específica de la historia.

## Resultado del corte

Se implementaron las entidades, el repositorio, el lock transaccional, los casos de
uso, la resolución efectiva, el límite JTA y la auditoría. El código principal
compiló con Java 21 en siete módulos. La evidencia y los límites de lo todavía no
validado están en
[J11-S4-03](../../evidence/J11-S4-03-persistencia-casos-uso-autoridad-global.md).

La historia quedó completada y validada. Las rutas administrativas agregadas en
historias posteriores también superaron el gate acumulado.
