# Evidencia J11-S6-03 - Persistencia de `business_partners`

- Fecha: 2026-07-29
- Estado: Verde
- Alcance: V1, restricciones PostgreSQL, unidad JPA, repositorios y arquitectura
- Entorno: Windows, Java 21.0.11, Maven Wrapper 3.9.16, PostgreSQL 18.4 por digest

## Cambios demostrados

- el descriptor declara una única migración para `plg_business_partners`;
- V1 crea ocho tablas y un historial Flyway independiente;
- siete entidades JPA privadas validan el esquema con DDL deshabilitado;
- el repositorio realiza round-trip del agregado y siempre exige empresa;
- códigos físicos son únicos por ámbito e identificaciones duplicadas son
  candidatos informativos;
- reemplazar un primario no produce carrera de unicidad;
- la versión obsoleta produce `VERSION_CONFLICT`;
- doce asignaciones concurrentes de secuencia producen exactamente 1 a 12;
- no existe operación de borrado físico.

## Pruebas pequeñas ejecutadas

| Corte | Resultado |
|---|---|
| descriptor | 3/3 verdes |
| recurso V1 | 1/1 verde |
| snapshot del agregado | 5/5 pruebas de agregado verdes |
| mapeo JPA y contrato sin delete | 3/3 verdes |
| ArchUnit | 12/12 reglas verdes |

## Gate PostgreSQL del módulo

Comando:

```powershell
.\mvnw.cmd -B -pl plugins/business-partners -am verify `
  "-Dlogixone.postgres.integration=true"
```

Resultado:

- reactor parcial: 5 módulos, `BUILD SUCCESS`;
- unitarias del plugin: 20, 0 fallos, 0 errores, 0 omitidas;
- integración: 13, 0 fallos, 0 errores, 0 omitidas;
- Flyway: primera ejecución 1 migración; segunda ejecución 0;
- PostgreSQL: dos contenedores efímeros basados en
  `postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0`;
- tiempo observado final: 1 min 36 s.

## Gate de arquitectura

Comando:

```powershell
.\mvnw.cmd -B -pl tests/architecture-tests -am `
  "-Dtest=ModuleBoundariesArchitectureTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Resultado: 16 módulos construidos; 12/12 reglas verdes. API, dominio y puertos del
plugin permanecen sin Jakarta/JDBC/Hibernate; las entidades residen únicamente en
`businesspartners.infrastructure.persistence`.

## Gate de reactor

Comando:

```powershell
.\mvnw.cmd -B verify
```

Resultado: 20/20 módulos, 217 pruebas, 0 fallos, 0 errores y 0 omitidas;
`BUILD SUCCESS` en 49,9 s. El módulo arquitectónico ejecutó sus 14 pruebas.

## Gate documental

El escaneo estricto de UTF-8 y enlaces locales recorrió 158 archivos Markdown y
580 enlaces locales: 0 enlaces rotos y 0 errores de codificación.

## Inspección de artefactos

- el JAR del plugin contiene exactamente la V1, `META-INF/persistence.xml` y su
  proveedor `PluginDefinition`;
- el JAR `business-partners-api` contiene 0 entradas Jakarta y 0 clases del
  dominio interno;
- el WAR base contiene 0 JAR de `business-partners`, conforme al diferimiento de
  composición hasta J11-S6-06.

## Límites conservados

- el WAR y migrador todavía no componen físicamente `business_partners`;
- no hay comandos, consultas de pantalla, permisos, auditoría funcional ni UI;
- no existe outbox sin consumidor real;
- la validación independiente transversal de la guía continúa pendiente;
- no se regenera PDF porque el Sprint 6 aún no está cerrando.
