# J11-S7-03 - Persistencia de `commercial_catalog`

- Estado: Completada
- Sprint: 7
- Fecha de inicio: 2026-07-30
- Gate principal: G2 datos
- ADR: [ADR-0020](../../adr/0020-persistencia-privada-commercial-catalog.md)

## Objetivo

Crear esquema privado, migración Flyway, mapeos JPA y repositorios empresariales
para ítems y precios sin adelantar aplicación, seguridad o interfaz.

## Alcance

- contribución de migración en el descriptor;
- V1 relacional bajo `plg_commercial_catalog`;
- definiciones controladas privadas para unidades, clasificación, tributación y
  variantes;
- unidad JPA independiente con `validate` y DDL deshabilitado;
- snapshots y reconstrucción de agregados completos;
- repositorios siempre acotados por empresa;
- conflictos estables de unicidad, referencia, solapamiento y versión;
- pruebas unitarias y PostgreSQL/Testcontainers.

## Fuera de alcance

- comandos, consultas de aplicación, permisos y auditoría;
- menú, endpoints, JSF, Material Design y Playwright;
- carga del legado, inventario, ventas, documentos y SIFEN;
- composición física del WAR/migrador, reservada a J11-S7-06.

## Criterios de aceptación

- **CA-01:** el descriptor declara una migración para `plg_commercial_catalog`.
- **CA-02:** Flyway crea veinte tablas y la segunda ejecución aplica cero cambios.
- **CA-03:** no existen referencias a tablas o entidades de otro propietario.
- **CA-04:** todas las claves operativas preservan `company_id` y su aislamiento.
- **CA-05:** código e identificadores activos respetan unicidad empresarial.
- **CA-06:** unidad/factor/default, clasificación, perfil y variante respetan sus
  cardinalidades y propietario.
- **CA-07:** listas fijan moneda/impuesto/redondeo y PostgreSQL impide vigencias
  activas ambiguas, incluso bajo concurrencia.
- **CA-08:** JPA valida el esquema sin crearlo o actualizarlo.
- **CA-09:** ítem y lista realizan round-trip completo sin pérdida.
- **CA-10:** actualización obsoleta produce conflicto estable.
- **CA-11:** repositorios no ofrecen borrado físico.
- **CA-12:** dominio/API/puertos permanecen libres de Jakarta/JDBC/Hibernate.
- **CA-13:** módulo, PostgreSQL, ArchUnit, reactor y documentación quedan verdes.

## Secuencia

1. registrar ADR y snapshots neutrales;
2. crear y validar V1/descriptor;
3. mapear entidades y repositorios;
4. ejecutar PostgreSQL/Testcontainers;
5. cerrar arquitectura, reactor y evidencia.

## Resultado

Los trece criterios quedaron satisfechos. La evidencia reproducible, comandos,
incidencias corregidas y límites deliberadamente pendientes están en
[Evidencia J11-S7-03](../../evidence/J11-S7-03-persistencia-commercial-catalog.md).

J11-S7-04 puede agregar comandos, consultas, permisos, autorización y auditoría
sobre estos puertos. La persistencia no habilita todavía el plugin en el WAR ni
crea una superficie operativa o visual.
