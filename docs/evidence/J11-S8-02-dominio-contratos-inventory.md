# Evidencia J11-S8-02 - Dominio y contratos de `inventory`

- Fecha: 2026-07-31
- Estado: Verde
- Historia: [J11-S8-02](../sprints/sprint-08/J11-S8-02-dominio-contratos-inventory.md)
- ADR: [ADR-0023](../adr/0023-modelo-inventory-y-contratos-publicos.md)

## Resultado implementado

`inventory-api` publica un contrato Java puro `1.0.0`; `inventory` contiene la
definición CDI/SPI y el dominio privado. El descriptor declara una dependencia
funcional requerida de `commercial_catalog` 1.x y todavía no aporta migraciones,
capacidades, permisos, menú o pantallas.

El dominio cubre ubicación `GENERAL`, inscripción local de productos, políticas de
lote/serie/vencimiento, cantidades base, balance no negativo, movimientos
inmutables, transferencias, reservas parciales y conteos con ajustes explícitos.

## Pruebas incrementales ejecutadas

| Corte | Comando | Resultado |
|---|---|---|
| intento inicial | `mvnw.cmd -B -pl plugins/inventory-api -am test` | detenido por Java 8 heredado; no compiló código |
| API pública con JDK validado | mismo comando con `.tools/jdk/jdk-21.0.11+10` | 4 propias, verdes |
| descriptor, depósitos e inscripción | `mvnw.cmd -B -pl plugins/inventory -am test` | 4 propias, verdes |
| dominio completo | mismo comando | 12 propias, verdes |
| fronteras | test dirigido `ModuleBoundariesArchitectureTest` | 19 reglas verdes |
| reactor | `mvnw.cmd -B verify` | 24/24 módulos y 321 pruebas verdes |

El primer intento falló correctamente en Maven Enforcer porque `JAVA_HOME`
apuntaba a Java 8. Se reutilizó el JDK 21 ya validado dentro de `.tools/`; no se
omitió ni relajó ninguna regla.

## Límites comprobados

- `inventory-api` solo usa Java, su paquete y `CompanyId`;
- el dominio solo usa contratos públicos de inventario, empresa y catálogo;
- API y dominio no usan Jakarta, `javax`, Hibernate, JDBC, PostgreSQL o PrimeFaces;
- inventario no importa dominio, aplicación o infraestructura de catálogo;
- no existen JPA, migraciones, SQL, permisos, menús, pantallas o eventos.

## Resultado consolidado

```text
REPORTS=89
TESTS=321
FAILURES=0
ERRORS=0
SKIPPED=0
```

PostgreSQL/Testcontainers, Docker/Compose y Playwright no se ejecutaron porque esta
historia no agrega SQL, JPA, distribución, endpoint o UI. Esos gates corresponden
a J11-S8-03 y J11-S8-05 a S8-07.

## Inspección de artefactos

```text
API_ENTRIES=34
API_JAKARTA=0
PLUGIN_ENTRIES=28
SPI=1
MIGRATIONS=0
BASE_WAR_INVENTORY=0
```

El JAR público no contiene Jakarta. El JAR funcional registra exactamente un
proveedor SPI y `beans.xml`, sin migraciones. El WAR base no incorpora
`inventory-api` ni `inventory`; la composición física sigue reservada a S8-06.

## Validación documental y seguridad

La pasada final produjo:

```text
MARKDOWN_FILES=220
UTF8_ERRORS=0
MOJIBAKE_FILES=0
LOCAL_LINKS=831
BROKEN_LINKS=0
SECRET_PATTERNS=0
```

La primera invocación del detector de mojibake usó caracteres literales que
PowerShell interpretó como comandos. Esa pasada no se aceptó; se repitió con
escapes Unicode ASCII y quedó verde. No se omitió ninguna validación.

## Conclusión

J11-S8-02 queda verde sin adelantar persistencia, aplicación o UI. J11-S8-03 puede
diseñar el esquema privado y los repositorios desde este dominio aceptado.
