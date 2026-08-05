# Evidencia J11-S7-03 - Persistencia de `commercial_catalog`

- Fecha: 2026-07-30
- Estado: Verde
- Historia: [J11-S7-03](../sprints/sprint-07/J11-S7-03-persistencia-commercial-catalog.md)
- ADR: [ADR-0020](../adr/0020-persistencia-privada-commercial-catalog.md)
- Entorno: Windows, Java 21.0.11, Maven Wrapper 3.9.16 y PostgreSQL 18.4 por digest

## Resultado

El descriptor declara V1 para `plg_commercial_catalog`. Flyway crea veinte tablas
privadas; once entidades JPA validan las raíces y detalles operativos con DDL
deshabilitado. Los repositorios reconstruyen ítems y listas completos, conservan
empresa, historia y versión optimista, no exponen borrado y asignan números con una
secuencia atómica por empresa y ámbito.

PostgreSQL protege unicidad de códigos/identificadores, FKs privadas, defaults de
unidad y vigencias tributarias/de precio. Los solapamientos usan advisory locks
transaccionales para cerrar la carrera entre escrituras concurrentes.

## Pruebas incrementales

| Corte | Resultado |
|---|---|
| snapshots de persistencia | 2/2 verdes |
| descriptor y recurso V1 | 3 pruebas verdes |
| mapeo JPA y contratos sin borrado | 3/3 verdes |
| traducción de conflictos SQL | 6/6 códigos estables verdes |
| validación Hibernate inicial | 1/1 verde contra PostgreSQL |
| round-trip, aislamiento, versión y precios | 4/4 verdes contra PostgreSQL |
| secuencia concurrente | 12 asignaciones únicas `1..12`, separación empresarial verde |
| límites ArchUnit | 16/16 reglas verdes |

## Gate PostgreSQL del módulo

```powershell
.\mvnw.cmd -B -pl plugins/commercial-catalog -am verify `
  "-Dlogixone.postgres.integration=true"
```

Resultado: 5/5 módulos del alcance y `BUILD SUCCESS`; 23 pruebas unitarias del
plugin y 10 pruebas de integración, sin fallos, errores u omisiones. Se usaron dos
contenedores efímeros basados en
`postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0`.
La primera migración aplicó V1 y la segunda ejecución aplicó cero cambios.

Los escenarios cubrieron veinte tablas, ausencia de referencias cruzadas y tipos
flotantes, empresas con IDs/códigos iguales, identificadores, unidades/defaults,
perfiles tributarios, vigencias adyacentes/solapadas, Hibernate `validate`,
round-trip de ítem/lista, historial al inactivar, escritura obsoleta y secuencia
concurrente.

```text
CATALOG_FAILSAFE_REPORTS=2
CATALOG_FAILSAFE_TESTS=10
CATALOG_FAILSAFE_FAILURES=0
CATALOG_FAILSAFE_ERRORS=0
CATALOG_FAILSAFE_SKIPPED=0
```

## Gate de arquitectura

```powershell
.\mvnw.cmd -B -pl tests/architecture-tests -am test `
  "-Dtest=ModuleBoundariesArchitectureTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false"
```

Resultado: 18 módulos del alcance y 16/16 reglas verdes. API, dominio y aplicación
del catálogo no dependen de Jakarta, Hibernate, JDBC o PostgreSQL; la aplicación
no alcanza infraestructura y las entidades JPA sólo pueden residir en el paquete
privado de persistencia del plugin.

## Gate de reactor

```powershell
.\mvnw.cmd -B verify
```

Resultado: 22/22 módulos, 279 pruebas Surefire, 0 fallos, 0 errores, 0 omitidas y
`BUILD SUCCESS` en 1 min 10 s.

```text
SUREFIRE_REPORTS=73
SUREFIRE_TESTS=279
SUREFIRE_FAILURES=0
SUREFIRE_ERRORS=0
SUREFIRE_SKIPPED=0
```

## Inspección de artefactos

```text
PLUGIN_ENTRIES=77
PLUGIN_MIGRATIONS=1
PLUGIN_PERSISTENCE_XML=1
PLUGIN_SPI=1
JPA_ENTITY_CLASSES=11
BASE_WAR_COMMERCIAL_CATALOG=0
```

El JAR contiene exactamente el archivo V1 además de su directorio, la unidad JPA
y un proveedor SPI. El WAR base conserva cero entradas del catálogo porque la
composición física sigue reservada a J11-S7-06.

## Incidencias detectadas y corregidas

- La primera ejecución PostgreSQL reveló que las funciones de los triggers
  resolvían tablas sin calificar fuera del `search_path` de Flyway. Se calificaron
  con `plg_commercial_catalog` y los cinco escenarios de migración quedaron verdes.
- La primera prueba de repositorios bajo JPQL estricto detectó el alias reservado
  `entry`. Se cambió por `record` y se repitió el mismo gate: 5/5 escenarios JPA
  verdes.
- La sincronización de finalidades de unidad se reemplazó por diff por clave para
  evitar retirar y volver a persistir la misma identidad dentro de un contexto JPA.

No se omitió, relajó o desactivó ninguna prueba. Una ejecución fallida bloqueó el
avance hasta corregir su causa y repetir el gate.

## Gate documental

El escaneo estricto recorrió todos los Markdown mantenidos, validó UTF-8, buscó
marcadores de texto dañado y resolvió enlaces locales:

```text
MARKDOWN_FILES=188
BAD_FILES=0
LOCAL_LINKS=688
BROKEN_LINKS=0
```

## Límites conservados

- no existen comandos, permisos, auditoría funcional, menú, endpoint o JSF;
- el plugin no se compone aún en WAR/migrador ni puede activarse operativamente;
- no se adelantaron inventario, ventas, documentos, SIFEN u outbox;
- Docker/Compose y Playwright no aplican a este corte sin distribución/UI;
- no se regenera el PDF porque el Sprint 7 todavía no está cerrando;
- la validación independiente transversal de la guía candidata sigue pendiente.

## Conclusión

J11-S7-03 queda verde y autoriza iniciar J11-S7-04. La siguiente historia debe
usar estos puertos mediante casos de uso autorizados; no debe exponer repositorios
ni resolver la falta de aplicación mediante SQL directo.
