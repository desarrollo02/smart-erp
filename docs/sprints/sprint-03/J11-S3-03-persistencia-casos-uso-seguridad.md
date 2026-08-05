# J11-S3-03 — Persistencia y casos de uso de seguridad

- Estado: Completada
- Dependencia: `J11-S3-02` implementada para la candidata
- Evidencia: [persistencia y casos de uso de seguridad](../../evidence/J11-S3-03-persistencia-casos-uso-seguridad.md)

## Objetivo

Implementar adaptadores JPA/JTA y casos de uso transaccionales para resolver y administrar usuarios, membresías, roles, permisos y bootstrap sin filtrar detalles de persistencia.

## Alcance

- entidades JPA privadas del kernel alineadas con V3;
- repositorios para identidad, usuario, membresía, rol y concesiones;
- resolución de usuario por `(issuer, subject)`;
- altas, cambios de estado y asignaciones con versión optimista;
- consultas de empresas disponibles y permisos concedidos;
- bootstrap one-shot idempotente a través de un puerto cerrado;
- auditoría obligatoria y rollback atómico;
- resultados y diagnósticos tipados en aplicación.

## Fuera de alcance

- acceder a tablas de Keycloak o plugins;
- guardar tokens, cookies o passwords;
- adaptar `SecurityContext` o sesión HTTP;
- UI administrativa completa;
- endpoint anónimo de bootstrap.

## Criterios de aceptación

- **CA-01:** entidades y repositorios permanecen privados de `kernel-infrastructure-jakarta`.
- **CA-02:** dominio y aplicación no dependen de JPA, CDI o clases de Keycloak.
- **CA-03:** identidad externa duplicada se rechaza de forma determinista.
- **CA-04:** cambios idempotentes devuelven `UNCHANGED` sin aumentar versión.
- **CA-05:** conflictos optimistas no escriben estado parcial.
- **CA-06:** asignar un rol de otra empresa se rechaza antes de confirmar.
- **CA-07:** revocar membresía o rol modifica inmediatamente la consulta efectiva.
- **CA-08:** una concesión para un permiso desconocido puede conservarse, pero no se vuelve efectiva sin contribución pública vigente.
- **CA-09:** bootstrap idéntico es idempotente y bootstrap incompatible falla cerrado.
- **CA-10:** fallo de auditoría obligatoria revierte la transacción completa.
- **CA-11:** errores JPA/SQL no atraviesan los puertos de aplicación.
- **CA-12:** no se registran claims, passwords, tokens ni datos personales innecesarios.
- **CA-13:** la unidad de persistencia continúa en `validate`.
- **CA-14:** pruebas de repositorio, JTA, concurrencia y rollback quedan definidas para G3 acumulado.

## Gates

- G1: módulos compilables/empaquetables para la candidata.
- G2/G3 diferidos: unitarias, Testcontainers y runtime JTA en `J11-S3-08`.
- G0 documental inmediato.

## Estado provisional aplicado

Se usó `Implementada pendiente de validación` hasta que G2 y G3 quedaron verdes.

## Resultado provisional

Se mapearon las cinco tablas V3 mediante cinco entidades nuevas y tres claves embebidas privadas de infraestructura; la unidad suma siete entidades al incluir las dos de V2. Los adaptadores implementan resolución exacta de `(issuer, subject)`, membresías, roles, asignaciones y concesiones; traducen conflictos JPA/PostgreSQL a códigos propios de aplicación y conservan concurrencia optimista para usuario, membresía y rol.

La capa neutral agregó comandos y resultados `CHANGED`, `UNCHANGED` y `REJECTED`, administración de estados, asignación empresarial segura, concesión histórica de permisos y consultas que releen la base para reflejar revocaciones. Los nombres visibles quedaron representados como atributos mutables y nunca forman parte de la identidad.

El bootstrap inicial quedó detrás de `SecurityBootstrapPort`, sin REST ni Faces. Valida empresa activa y personalización exacta, crea usuario/membresía/rol/asignación/concesiones dentro de una única transacción y solo acepta como idempotente una declaración existente compatible. La integración de ese puerto con configuración externa y el orden de arranque corresponde a `J11-S3-04`.

`persistence.xml` continúa con generación de esquema `none` y `hibernate.hbm2ddl.auto=validate`; readiness exige ahora migración `core` V3. En el corte provisional el reactor empaquetó 16 de 16 módulos con pruebas omitidas. Todavía no se habían ejecutado PostgreSQL, JPA runtime, JTA ni la suite automatizada, por lo que en ese momento la historia no estaba completada.

## Validación acumulada

`J11-S3-08` dejó verdes repositorios PostgreSQL, JPA `validate`, idempotencia,
concurrencia, aislamiento, bootstrap y 4 pruebas JTA runtime con autolimpieza. G2/G3
quedaron verdes. Evidencia:
[gates G2–G6](../../evidence/J11-S3-08-validacion-demo-cierre.md).

## Siguiente paso

La historia está completada. El Sprint continúa con G7 de `J11-S3-08`.
