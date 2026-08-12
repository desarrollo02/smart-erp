# Evidencia — J11-S9-02 dominio y contratos de `purchasing`

- Fecha: 2026-08-11
- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Rama local: `sprint/09-purchasing`
- Historia: [J11-S9-02](../sprints/sprint-09/J11-S9-02-dominio-contratos-purchasing.md)
- ADR: [ADR-0041](../adr/0041-modelo-purchasing-y-contratos-publicos.md)

## Resultado implementado

`purchasing-api@1.0.0` publica identidades, estados, referencias, directorio y
comandos tipados de importación de documentos abiertos. `purchasing@1.0.0`
contiene descriptor CDI/SPI y los agregados `PurchaseRequest`, `PurchaseOrder`,
`GoodsReceipt` y `SupplierReturn`.

El código conserva las fronteras aprobadas: Java puro en API/dominio, dependencias
sólo hacia contratos públicos, ciclos separados, cantidades append-only,
snapshots, versión esperada e inventario obligatorio sólo para `STOCK`.

## Pruebas automatizadas ejecutadas

- contrato API: UUID canónicos, procedencia, documentos abiertos y líneas;
- solicitud: envío, separación de aprobación, terminales y concurrencia;
- orden: asignación/directa, recepción parcial, sobre-recepción, devolución,
  reapertura y cierre corto;
- recepción/devolución: ubicación, movimiento y confirmación inmutable;
- descriptor: identidad, versión, dependencias y ausencia de contribuciones
  adelantadas;
- ArchUnit: Java puro, APIs públicas y prohibición de implementaciones ajenas.

El 2026-08-11 la aclaración de producto dejó explícito que sólo se difiere la
validación de otra persona. Sobre el corte materializado
`.tools/tmp/validation/J11-S9-05-automated` quedaron verdes el módulo Compras y
sus dependencias, 32 pruebas ArchUnit y el `mvn verify` completo de 28 módulos.
PostgreSQL y la evidencia detallada se registran en J11-S9-03/J11-S9-05.

## Revisión estática

Se revisan antes de entregar:

1. archivos esperados, paquetes e imports prohibidos;
2. POM raíz, dependency management y dependencias de arquitectura;
3. proveedor SPI y descriptor sin migraciones/UI;
4. enlaces, UTF-8, espacios finales y `git diff --check`;
5. ausencia de SQL, JPA, `javax.*`, secretos y código legado copiado.

Resultado de la pasada:

```text
BRANCH=sprint/09-purchasing
JAVA_FILES=39
API_MAIN_FILES=23
PLUGIN_MAIN_FILES=10
TEST_FILES=6
TEST_METHODS=12
ARCH_RULES_ADDED=3
XML_WELL_FORMED=5
SPI_PROVIDERS=1
SQL_FILES=0
LOCAL_LINKS_OK=15_DOCS
NEUTRAL_FORBIDDEN_IMPORTS=0
LEGACY_CLASS_REFERENCES=0
SQL_JPA_SECRET_PATTERNS=0
UTF8_AND_TRAILING_WHITESPACE_OK
GIT_DIFF_CHECK=OK
```

Esta revisión estática se conserva como evidencia histórica de la implementación;
ya no sustituye a Maven o ArchUnit, que fueron ejecutados posteriormente y
quedaron verdes. La validación independiente continúa pendiente.
