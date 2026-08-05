# Evidencia J11-S8-03 - Persistencia de `inventory`

- Fecha: 2026-07-31
- Estado: Verde
- Historia: [J11-S8-03](../sprints/sprint-08/J11-S8-03-persistencia-inventory.md)
- ADR: [ADR-0024](../adr/0024-persistencia-privada-inventory.md)

## Resultado implementado

`inventory` declara la migración inmutable V1 del esquema privado
`plg_inventory`. Nueve tablas separan depósitos, ubicaciones, inscripciones de
catálogo, saldos, movimientos y líneas, reservas, conteos y líneas. No existe FK,
consulta SQL ni relación JPA hacia tablas de otro plugin.

La unidad `logixone-inventory-pu` valida el esquema y mantiene deshabilitada la
generación de DDL. Nueve entidades JPA y seis adaptadores reconstruyen agregados
desde snapshots completos, siempre reciben `CompanyId`, no ofrecen borrado físico
y traducen conflictos de integridad o versión a códigos estables.

## Invariantes comprobadas

- cantidades `NUMERIC(30,6)` y factores `NUMERIC(30,12)`;
- dimensiones opcionales comparadas con `UNIQUE NULLS NOT DISTINCT`;
- saldo físico, reservado y disponible no negativos;
- una serie positiva no puede ocupar dos buckets;
- idempotencia y fuente únicas por empresa;
- un movimiento admite una única reversión directa;
- libro de movimientos append-only con snapshots históricos;
- conteos activos solapados serializados con advisory lock transaccional;
- aislamiento empresarial en claves, búsquedas y referencias privadas;
- concurrencia optimista mediante `@Version`.

## Pruebas incrementales ejecutadas

| Corte | Comando | Resultado |
|---|---|---|
| migración y descriptor | `mvnw.cmd -pl plugins/inventory -am test` | recursos y contrato de migración verdes |
| PostgreSQL real | `mvnw.cmd -pl plugins/inventory -am verify -Dlogixone.postgres.integration=true` | 6/6 módulos; 19 unitarias y 9 de integración verdes |
| arquitectura dirigida | `mvnw.cmd -pl tests/architecture-tests -am test` | 20 reglas de límites verdes |
| reactor final | `mvnw.cmd verify` | 24/24 módulos y 329 pruebas verdes |

Todos los comandos se ejecutaron con Java 21 desde
`.tools/jdk/jdk-21.0.11+10`. La integración utilizó PostgreSQL 18.4 mediante
Testcontainers con la imagen fijada por digest declarada por el proyecto.

## Hallazgos corregidos durante la validación

Las primeras pasadas hicieron visibles cuatro defectos reales y se corrigió su
causa antes de continuar:

1. la contribución de migración estaba ubicada en el argumento incorrecto del
   descriptor y no compilaba;
2. el trigger de conteos dependía del `search_path`; quedó cualificado con
   `plg_inventory.stock_count`;
3. una aserción de cantidad confundía escala decimal con valor numérico; se
   validó el valor conservando la precisión de PostgreSQL;
4. la lista explícita de propietarios JPA de ArchUnit todavía no incluía
   `inventory`; se agregó el nuevo paquete y una regla que impide que aplicación
   dependa de infraestructura.

No se omitió, desactivó ni relajó una prueba. Cada fallo bloqueó el siguiente
cambio hasta quedar verde.

## Resultado consolidado

```text
REPORTS=93
TESTS=329
FAILURES=0
ERRORS=0
SKIPPED=0

INVENTORY_SUREFIRE_REPORTS=10
INVENTORY_SUREFIRE_TESTS=19
INVENTORY_FAILSAFE_REPORTS=2
INVENTORY_FAILSAFE_TESTS=9
```

Docker/Compose, OIDC y Playwright no se ejecutaron: la historia no compone el
plugin en el WAR, no cambia endpoints ni agrega interfaz. Esos gates pertenecen a
J11-S8-05 a J11-S8-07.

## Inspección de artefactos

```text
API_ENTRIES=34
API_JAKARTA=0
PLUGIN_ENTRIES=81
JPA_ENTITIES=9
SPI=1
MIGRATIONS=1
PERSISTENCE_XML=1
BASE_WAR_INVENTORY=0
```

El JAR público continúa libre de Jakarta. El JAR funcional contiene exactamente
una V1, una unidad de persistencia y nueve entidades. El WAR base no incorpora
inventario, coherente con la composición física reservada a J11-S8-06.

## Revisión documental y operativa

Se actualizaron ADR, historia, Sprint, backlog, arquitectura, estrategia de
pruebas, contrato del plugin, manual técnico y guía de implementación. Se revisó
el manual de usuario y no cambió porque todavía no existe un recorrido visible.
Se revisó la guía de Visual Studio Code y no cambió porque los comandos oficiales,
extensiones y procedimiento de montaje permanecen iguales.

La validación final de Markdown, codificación, enlaces locales y secretos produjo:

```text
MARKDOWN_FILES=223
UTF8_ERRORS=0
MOJIBAKE_FILES=0
LOCAL_LINKS=840
BROKEN_LINKS=0
SECRET_PATTERNS=0
```

No corresponde regenerar todavía la fotografía de plugins, los manuales/PDF de
cierre, la demo visual ni el instalador Windows: son entregables del baseline
congelado al finalizar Sprint 8, no de esta historia intermedia.

## Conclusión

J11-S8-03 queda verde. J11-S8-04 puede implementar casos de uso, permisos,
auditoría y demarcación JTA sobre estos puertos. Interfaz y demo de inventario aún
no están disponibles.
