# J11-S2-05 — Casos de uso y guardas de activación

- Estado: Completada
- Fecha de inicio: 2026-07-27
- Fecha de cierre: 2026-07-27
- Dependencia: `J11-S2-04` completada y verde

## Objetivo

Implementar los casos de uso transaccionales que administran empresas, activaciones y su personalización obligatoria, y una guarda neutral que impida ejecutar operaciones de un plugin no efectivo para la empresa correspondiente.

## Alcance

- servicios de aplicación para ciclo de vida de empresa y activación;
- asignación inicial y reemplazo transaccional de la personalización empresarial;
- validación contra catálogo físico y estado persistido;
- transacciones, idempotencia, concurrencia y rollback;
- guarda neutral de operaciones de plugin;
- puertos de auditoría o eventos aprobados por el ADR;
- pruebas unitarias y de integración con dos empresas.

## Casos de uso mínimos

- registrar una empresa con la personalización exigida por las reglas aceptadas;
- consultar su estado sin enumerar datos de otra empresa;
- cambiar el ciclo de vida permitido;
- consultar activación deseada y efectiva;
- activar un plugin presente;
- desactivar un plugin activo;
- consultar la personalización asignada y efectiva;
- reemplazar la personalización por otra presente y compatible;
- verificar acceso antes de una operación protegida.

## Reglas de aplicación

- todos los comandos validan empresa y plugin antes de persistir;
- repetir una transición ya satisfecha produce el resultado idempotente definido por el ADR;
- activar exige dependencias requeridas activas en la misma empresa;
- desactivar rechaza dependientes activos y no realiza cambios parciales;
- el flujo común rechaza desactivar la personalización obligatoria;
- activar una empresa exige exactamente una personalización asignada, presente, compatible y no asignada a otra empresa;
- reemplazar valida completamente el nuevo plugin antes de cambiar la asignación y conserva el anterior ante cualquier fallo;
- una empresa inactiva deniega cualquier plugin aunque la decisión deseada permanezca guardada;
- un plugin ausente del catálogo físico se deniega sin eliminar su fila;
- la guarda se aplica en la capa de aplicación antes de invocar lógica funcional;
- ocultar una contribución nunca sustituye la guarda;
- logs y eventos usan IDs técnicos y códigos estables, sin datos comerciales innecesarios.

## Frontera de seguridad

Los casos de uso se implementan sin endpoint público. Los comandos administrativos reciben una empresa explícita desde un llamador confiable de pruebas o futuro adaptador autorizado. Las operaciones funcionales consumen el puerto de contexto aprobado, pero Sprint 2 no decide todavía autenticación ni usuario.

## Fuera de alcance

- REST de administración, UI o carga inicial interactiva;
- login, usuario, roles o asignación de permisos;
- reglas empresariales de un plugin real;
- autoactivación de dependencias;
- edición libre de pantallas o reemplazo de recursos internos;
- borrado físico de activaciones o datos.

## Criterios de aceptación

- **CA-01:** cada caso de uso tiene entrada, resultado y errores neutrales explícitos.
- **CA-02:** empresa inexistente o inactiva se deniega de forma segura.
- **CA-03:** plugin ausente no puede activarse ni ejecutarse.
- **CA-04:** activar con dependencias requeridas satisfechas termina en estado efectivo activo.
- **CA-05:** dependencia requerida inactiva rechaza activación sin escritura parcial.
- **CA-06:** dependiente activo rechaza desactivación sin escritura parcial.
- **CA-07:** operaciones repetidas cumplen la idempotencia del ADR.
- **CA-08:** conflicto concurrente se transforma en resultado estable y no filtra detalles JPA/SQL.
- **CA-09:** la guarda impide ejecutar el callback o puerto funcional cuando deniega.
- **CA-10:** dos empresas producen decisiones y resultados independientes.
- **CA-11:** autorización futura puede envolver los casos de uso sin cambiar el dominio.
- **CA-12:** unitarias, integración PostgreSQL, JTA, ArchUnit y `mvn verify` quedan verdes.
- **CA-13:** una empresa no puede quedar operativa sin exactamente una personalización válida.
- **CA-14:** el mismo plugin de personalización no puede utilizarse para registrar o activar una segunda empresa.
- **CA-15:** la desactivación común de la personalización se rechaza sin cambiar estado.
- **CA-16:** el reemplazo válido es atómico, idempotente y auditable.
- **CA-17:** un reemplazo ausente, incompatible o concurrente conserva íntegramente la personalización anterior.

## Gates

1. unitarias de aplicación con puertos falsos;
2. integración con repositorios reales y rollback;
3. casos negativos de empresa/plugin/dependencias;
4. concurrencia e idempotencia;
5. ArchUnit y `mvnw.cmd -B verify`.

## Siguiente historia permitida

`J11-S2-06` cuando la guarda y todas las transiciones estén verdes.

## Resultado y cierre

Los 17 criterios quedaron satisfechos. Los casos de uso neutrales administran el alta y ciclo de vida empresarial, activación funcional, reemplazo atómico de personalización, resultados idempotentes, conflictos estables y auditoría técnica. La guarda consulta un `CompanyContext` confiable y deniega antes de invocar el callback cuando el plugin no es efectivo.

La fachada CDI aplica una transacción JTA a cada comando sin publicar REST administrativo. PostgreSQL y WildFly demostraron aislamiento entre dos empresas, commit, rollback por fallo de auditoría y conservación íntegra de la asignación anterior ante rechazos.

Evidencia: [J11-S2-05 — Casos de uso y guardas de activación](../../evidence/J11-S2-05-casos-uso-guardas-activacion.md).
