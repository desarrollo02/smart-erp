# J11-S2-04 — Persistencia JPA/JTA del kernel

- Estado: Completada
- Fecha de inicio: 2026-07-27
- Fecha de cierre: 2026-07-27
- Dependencia: `J11-S2-03` completada y verde

## Objetivo

Implementar el adaptador de persistencia del kernel sobre PostgreSQL con datasource JTA administrado por WildFly y unidad `logixone-core-pu`, incluyendo la asignación exclusiva de personalización y manteniendo entidades y consultas fuera de los contratos neutrales.

## Alcance

- configurar un datasource JTA mediante infraestructura reproducible y variables externas existentes;
- declarar `logixone-core-pu` con Jakarta Persistence provisto por WildFly;
- mapear entidades internas de empresa, activación y asignación de personalización al esquema `core` V2;
- implementar los puertos de repositorio definidos por aplicación;
- delimitar transacciones en servicios de aplicación/adaptadores Jakarta;
- validar el esquema sin crear, actualizar o eliminar DDL;
- probar consultas y mutaciones con PostgreSQL real mediante Testcontainers;
- validar en WildFly inyección, JTA, persistencia y rollback.

## Estrategia de implementación seleccionada

- JNDI estable: `java:/jdbc/LogixoneCoreDS`.
- Unidad JTA estable: `logixone-core-pu` con Jakarta Persistence 3.2 provista por WildFly.
- Proveedor del runtime: Hibernate ORM 7.4.5.Final ya incluido en la imagen WildFly 41 fijada por digest; no se empaqueta en el WAR.
- El driver PostgreSQL se instala como módulo del servidor y el datasource queda configurado reproduciblemente durante la construcción de la imagen. Al iniciar, el entrypoint lee la contraseña desde el secreto por archivo y la entrega al servidor solo como variable de proceso.
- La configuración estándar de JPA declara generación `none` y Hibernate `validate`.
- Las pruebas directas de repositorio usan el mismo proveedor con alcance `test`, PostgreSQL 18.4 real y una unidad `RESOURCE_LOCAL` exclusiva del test.
- La validación JTA se realizará en WildFly mediante un arnés de integración activado por perfil y ausente del WAR normal.

## Reglas de diseño

- ninguna entidad JPA cruza fuera de `kernel-infrastructure-jakarta`;
- los repositorios devuelven modelos neutrales, no entidades administradas;
- toda consulta de activación incluye `CompanyId` de forma obligatoria;
- toda consulta de personalización parte de `CompanyId` y devuelve como máximo la asignación de esa empresa;
- no existe consulta “global” reutilizada accidentalmente por una operación empresarial;
- una transacción fallida no deja activaciones parciales;
- reemplazar una personalización bloquea o versiona la asignación según el ADR y nunca deja dos propietarias ni una escritura parcial;
- la concurrencia se resuelve según el ADR y produce un conflicto tipado;
- secretos y URL no se incorporan al WAR, POM ni logs.

## Fuera de alcance

- repositorios o entidades de plugins;
- caché distribuida o segundo nivel;
- multitenancy de Hibernate por esquema/base;
- endpoints REST, UI o contexto derivado de request;
- auditoría con usuario autenticado.

## Criterios de aceptación

- **CA-01:** datasource y unidad de persistencia tienen nombres estables y documentación operativa.
- **CA-02:** APIs Jakarta permanecen `provided` y no se empaqueta Hibernate/WildFly en el WAR.
- **CA-03:** la configuración JPA prohíbe generación y solo valida el esquema migrado.
- **CA-04:** entidades pertenecen exclusivamente a infraestructura y esquema `core`.
- **CA-05:** repositorios implementan puertos neutrales sin filtrar entidades.
- **CA-06:** CRUD permitido por el ADR funciona sobre PostgreSQL real.
- **CA-07:** dos empresas con el mismo `PluginId` conservan decisiones independientes.
- **CA-08:** lectura con empresa equivocada no devuelve ni modifica datos.
- **CA-09:** conflicto concurrente y rollback no dejan estado parcial.
- **CA-10:** WildFly demuestra datasource, JPA y JTA reales, no solo un proveedor embebido de pruebas.
- **CA-11:** ArchUnit prohíbe JPA en API, dominio y aplicación.
- **CA-12:** pruebas de módulo, Testcontainers, WAR, Compose y `mvn verify` quedan verdes.
- **CA-13:** el repositorio de asignaciones implementa la relación uno a uno sin filtrar entidades JPA.
- **CA-14:** consultar desde una empresa distinta no revela el `PluginId` de personalización ajeno.
- **CA-15:** asignación duplicada y reutilización del mismo plugin por otra empresa se rechazan de forma estable.
- **CA-16:** reemplazo concurrente, rollback e idempotencia preservan exactamente una asignación válida según el ADR.

## Gates

1. POM/modelo efectivo y dependencia `provided`;
2. pruebas del adaptador con Testcontainers;
3. ArchUnit;
4. WAR inspeccionado;
5. despliegue WildFly y rollback real;
6. `mvnw.cmd -B verify`.

## Siguiente historia permitida

`J11-S2-05` cuando persistencia, JTA y aislamiento estén demostrados.

## Cierre

Los 16 criterios quedaron satisfechos el 2026-07-27. La evidencia incluye repositorios sobre PostgreSQL 18.4 real, aislamiento y conflictos tipados, validación JPA sin DDL, commit/rollback JTA dentro de WildFly, ambas composiciones del WAR, recreación con volumen persistente, checks Docker/Compose y el `clean verify` integral.

Evidencia: [J11-S2-04 — Persistencia JPA/JTA](../../evidence/J11-S2-04-persistencia-jpa-jta.md).
