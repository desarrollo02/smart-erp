# Evidencia J11-S9-03 — Persistencia de `purchasing`

- Fecha: 2026-08-11
- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Historia: [J11-S9-03](../sprints/sprint-09/J11-S9-03-persistencia-purchasing.md)
- ADR: [ADR-0042](../adr/0042-persistencia-privada-purchasing.md)

## Resultado implementado

`purchasing` declara una V1 privada con nueve tablas. Solicitud, orden, recepción
y devolución conservan cabecera y líneas propias; las asignaciones solicitud→orden
son explícitas. No existe FK, consulta SQL ni relación JPA hacia tablas de otro
plugin.

La unidad `logixone-purchasing-pu` mantiene deshabilitada la generación DDL. Nueve
entidades, tres embebibles y cuatro adaptadores reconstruyen agregados completos,
siempre reciben empresa y no ofrecen borrado físico. Los documentos confirmados y
las líneas de solicitudes finalizadas quedan protegidos por dominio y trigger.

## Pruebas automatizadas ejecutadas

- recurso V1, número de tablas y ausencia de tipos/relaciones prohibidos;
- descriptor y contribución de migración;
- nueve mapeos JPA, cuatro `@Version` y contratos de repositorio;
- traducción de SQLSTATE a códigos estables;
- Flyway V1 idempotente sobre PostgreSQL real;
- aislamiento empresarial, FKs privadas y cantidades imposibles;
- movimiento obligatorio antes de confirmar stock;
- inmutabilidad posterior de recepción/devolución;
- validación JPA sin DDL y round-trip de cuatro agregados;
- límites ArchUnit de propietario JPA y dirección aplicación→infraestructura.

El corte materializado ejecutó 19 pruebas unitarias y 6 integraciones reales de
Compras sobre PostgreSQL 18.4/Testcontainers. Flyway aplicó V1–V2, volvió a migrar
idempotentemente y JPA validó/reconstruyó los cuatro agregados. La primera pasada
detectó un trigger compartido que intentaba leer `return_state` al operar sobre
`goods_receipt`; se separaron las funciones de recepción y devolución y la
repetición terminó sin fallos.

## Revisión estática

Se revisaron estructura, XML, SQL, referencias de esquemas, imports prohibidos,
codificación, enlaces Markdown y diferencias Git sin ejecutar código.

```text
XML_WELL_FORMED=5
MAIN_JAVA=34
TEST_JAVA=10
TEST_METHODS=21
JPA_ENTITIES=9
MIGRATION_TABLES=9
SQL_FUNCTIONS=4
SQL_TRIGGERS=8
ARCHITECTURE_DEPENDENCY_DUPLICATES=0
FORBIDDEN_SCHEMA_REFERENCES=0
DEPENDENCY_IMPLEMENTATION_IMPORTS=0
FORBIDDEN_TYPES=0
PUBLIC_DELETE_METHODS=0
DOMAIN_API_JPA=0
MISSING_PROJECT_IMPORTS=0
JAVA_BRACE_MISMATCHES=0
LOCAL_LINKS=390
BROKEN_LINKS=0
UTF8_ERRORS=0
TRAILING_WHITESPACE=0
GIT_DIFF_CHECK=OK
```

La revisión estática no equivale por sí sola a compilar o migrar. Después de esta
pasada sí se usaron Maven, Docker, PostgreSQL, JPA runtime, ArchUnit y
Testcontainers; los resultados automatizados quedaron verdes.

## Documentación revisada

Se actualizaron ADR, historia, Sprint, épica, arquitectura, manual técnico, guía
de implementación, contrato del plugin e índice documental. El manual de usuario
no cambia: J11-S9-03 no aporta pantallas, permisos ni un recorrido operativo. La
guía de Visual Studio Code tampoco cambia porque los comandos y el procedimiento
oficial permanecen iguales.

No corresponde regenerar todavía los manuales/PDF de cierre, la demo oficial ni
el instalador: pertenecen al baseline congelado de J11-S9-07/J11-S9-08.
