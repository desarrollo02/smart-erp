# J11-S6-03 - Persistencia de `business_partners`

- Estado: Completada
- Sprint: 6
- Fecha de inicio: 2026-07-29
- Gate principal: G2 datos
- ADR: [ADR-0015](../../adr/0015-persistencia-privada-business-partners.md)

## Objetivo

Crear el esquema privado, la migración Flyway, los mapeos JPA y los repositorios
del primer plugin productivo, preservando los límites definidos por el dominio y
sin adelantar aplicación, seguridad o interfaz.

## Alcance

- contribución de migración en el descriptor;
- V1 relacional bajo `plg_business_partners`;
- unidad JPA independiente con validación y DDL deshabilitado;
- entidades privadas y adaptación dominio-persistencia;
- repositorio siempre acotado por empresa;
- secuencia transaccional disponible para códigos configurables;
- conflictos estables de unicidad y versión;
- pruebas unitarias y PostgreSQL/Testcontainers.

## Fuera de alcance

- comandos, consultas paginadas y autorización;
- auditoría funcional y eventos de integración;
- permisos, menús, endpoints y pantallas;
- importación del legado;
- composición física final del plugin en WAR/migrador, reservada para
  `J11-S6-06`.

## Criterios de aceptación

- **CA-01:** el descriptor declara una única migración para
  `plg_business_partners`.
- **CA-02:** Flyway crea las ocho tablas acordadas sobre PostgreSQL vacío y una
  segunda ejecución aplica cero migraciones.
- **CA-03:** ningún objeto del plugin referencia tablas o entidades privadas de
  otro propietario.
- **CA-04:** códigos generales y de rol respetan su ámbito de unicidad; las
  identificaciones duplicadas siguen siendo almacenables para advertencia.
- **CA-05:** claves e índices preservan el aislamiento por empresa y el único
  principal activo por categoría/finalidad.
- **CA-06:** JPA valida el esquema y no intenta crearlo o actualizarlo.
- **CA-07:** un agregado completo realiza round-trip sin perder roles,
  identificaciones, direcciones, canales ni contactos.
- **CA-08:** una actualización obsoleta falla con conflicto de versión estable.
- **CA-09:** el repositorio no ofrece baja física y la inactivación conserva
  filas.
- **CA-10:** dominio, API y puertos no dependen de Jakarta, Hibernate, JDBC o
  PostgreSQL.

## Matriz de prueba

| Nivel | Evidencia requerida |
|---|---|
| descriptor | propietario y ubicación exactos |
| recurso SQL | nombres, tablas y ausencia de referencias cruzadas |
| dominio | restauración de snapshot sin cambiar versión ni invariantes |
| JPA | metamodelo, `@Version`, esquema y unidad independiente |
| PostgreSQL | vacío, idempotencia, restricciones, índices y aislamiento |
| repositorio | alta, lectura, actualización, duplicados y concurrencia obsoleta |
| arquitectura | neutralidad y entidades dentro de infraestructura del propietario |
| reactor | `mvn verify` verde |

## Resultado

Se creó la V1 con ocho tablas privadas, la unidad JPA independiente, siete
entidades, los puertos y adaptadores de agregado/secuencia. El gate del módulo
ejecutó 20 pruebas unitarias y 13 escenarios PostgreSQL/Testcontainers sin fallos.
ArchUnit conserva neutralidad y propiedad de entidades. La evidencia reproducible
está en
[`docs/evidence/J11-S6-03-persistencia-business-partners.md`](../../evidence/J11-S6-03-persistencia-business-partners.md).

La historia no agrega aplicación, seguridad, UI ni composición final. El siguiente
corte autorizado es `J11-S6-04`.
