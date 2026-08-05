# Evidencia J11-S8-05 - Interfaz neutral de `inventory`

- Fecha: 2026-07-31
- Estado: Verde
- Historia: [J11-S8-05](../sprints/sprint-08/J11-S8-05-interfaz-inventory.md)
- ADR: [ADR-0025](../adr/0025-recorridos-visuales-inventory.md)
- Entorno: Windows, Java 21.0.11 y Maven Wrapper 3.9.16

## Resultado funcional

El descriptor de `inventory` publica tres menús protegidos por `inventory.view` y
tres pantallas neutrales:

1. `inventory:stock` en `/inventory`;
2. `inventory:warehouses` en `/inventory/warehouses`;
3. `inventory:counts` en `/inventory/counts`.

Existencias permite buscar e inscribir productos, consultar disponibilidad,
actualizar snapshots, cambiar el ciclo de vida, contabilizar movimientos y
administrar reservas. Depósitos permite buscar, crear, abrir, renombrar, agregar
ubicaciones e inactivar. Conteos permite buscar, preparar alcance, capturar líneas,
revisar, contabilizar o cancelar.

Los handlers obtienen la empresa del contexto confiable y revalidan plugin,
permiso, recurso y versión. Cada mutación usa su permiso específico;
`post_count` exige `inventory.adjustments.post`. Los errores de entrada se
convierten a mensajes estables sin revelar SQL, stacktrace ni datos sensibles.

## Arquitectura visual

El plugin aporta contratos neutrales, proyecciones y handlers; no aporta XHTML,
CSS, JavaScript ni EL. `ShellScreenRegistry` y `ShellTextCatalog` describen las
tres presentaciones y el XHTML único del shell conserva Material Design 3,
adaptación de tabla a lista, formularios compactos, labels, foco y breakpoints.

La validación visual navegable en 375, 599, 600, 720, 839, 840 y 1280 px necesita
la composición física real. Se ejecutará en J11-S8-06 junto al fixture y
Playwright; no se declara anticipadamente como probada en esta historia.

## Pruebas focalizadas

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools\jdk\jdk-21.0.11+10').Path
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl plugins/inventory -am test
.\mvnw.cmd -pl web-shell -am test
```

Resultados:

- `inventory`: 56/56 pruebas verdes;
- consultas de directorio: 3/3;
- handlers de depósitos, existencias y conteos: 12/12;
- descriptor de inventario: 2/2;
- `web-shell`: 22/22, incluidas 3/3 pruebas del renderer de inventario;
- cero fallos, errores u omisiones.

## PostgreSQL

```powershell
.\mvnw.cmd -pl plugins/inventory -am -Ppostgres-integration `
  "-Dlogixone.postgres.integration=true" `
  "-Dtest=__none__" `
  "-Dsurefire.failIfNoSpecifiedTests=false" `
  "-Dit.test=InventoryJpaValidationPostgreSqlIT" verify
```

Resultado: 5/5 escenarios verdes sobre PostgreSQL 18.4 mediante Testcontainers.
El escenario nuevo comprueba directorios empresariales de depósitos, artículos y
conteos contra las tablas reales de `plg_inventory`; los seis módulos del corte
quedaron en `BUILD SUCCESS`.

## Arquitectura y reactor integral

```powershell
.\mvnw.cmd -pl tests/architecture-tests -am test
.\mvnw.cmd verify
```

Resultados:

- arquitectura: 24/24 pruebas verdes, incluidas 20 reglas ArchUnit;
- gate arquitectónico: 20/20 módulos construidos;
- reactor completo: 24/24 módulos en `BUILD SUCCESS`;
- 104 reportes y 369/369 pruebas, con cero fallos, errores u omisiones;
- el WAR base continúa sin composición física de `inventory`, como exige el
  alcance de J11-S8-05.

## Documentación revisada

Se actualizaron ADR-0025 y su índice, historia/estado de Sprint, backlog, contrato
del plugin, vista arquitectónica, estrategia de pruebas, guía de implementación,
manual técnico, manual de usuario y esta evidencia.

La validación estricta final produjo:

```text
MARKDOWN_FILES=228
UTF8_ERRORS=0
MOJIBAKE_FILES=0
LOCAL_LINKS=856
BROKEN_LINKS=0
```

La guía de Visual Studio Code fue revisada y no cambia: esta historia no agregó
JDK, extensiones, variables, perfiles o comandos de montaje. El PDF de estructura,
la fotografía de dependencias y el instalador Windows son artefactos obligatorios
del cierre de Sprint, no de esta historia; Sprint 8 continúa abierto.

## Continuidad autorizada

J11-S8-05 queda completada y habilita J11-S8-06. El siguiente corte debe incorporar
`commercial_catalog` e `inventory` al mismo plugin set físico, ejecutar sus
migraciones, activar dependencias/menús por empresa, preparar datos ficticios,
desplegar y validar con Playwright. Hasta entonces no existe una nueva demo visual
de Inventario y no se promueve ninguna imagen.
