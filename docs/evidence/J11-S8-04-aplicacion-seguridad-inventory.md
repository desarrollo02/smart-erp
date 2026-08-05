# Evidencia J11-S8-04 - Aplicación y seguridad de `inventory`

- Fecha: 2026-07-31
- Estado: Verde
- Historia: [J11-S8-04](../sprints/sprint-08/J11-S8-04-aplicacion-seguridad-inventory.md)

## Cortes ejecutados

| Corte | Resultado |
|---|---|
| dominio de saldo agregado | 3/3 pruebas verdes |
| estructura y refresh de catálogo | 11/11 pruebas verdes |
| movimientos e idempotencia | 6/6 pruebas verdes |
| reservas y recibos inmutables | 5/5 pruebas verdes |
| conteos y ajustes | 7/7 pruebas verdes |
| CDI, permisos y rollback JTA | 6/6 pruebas verdes |
| módulo completo | 41/41 pruebas verdes |
| PostgreSQL V1→V2 y JPA | 10/10 pruebas verdes |
| reactor completo | 24/24 módulos; 351/351 pruebas verdes |
| arquitectura | 24/24 pruebas verdes, incluidas 20 reglas ArchUnit |

Los comandos finales fueron:

```powershell
mvnw.cmd -pl plugins/inventory -am test
mvnw.cmd -pl plugins/inventory -am -Ppostgres-integration `
  "-Dit.test=InventoryMigrationPostgreSqlIT,InventoryJpaValidationPostgreSqlIT" verify
mvnw.cmd verify
```

Se usó Java 21 desde `.tools/jdk/jdk-21.0.11+10` y PostgreSQL 18.4 mediante
Testcontainers con la imagen fijada por digest del proyecto.

## Resultado consolidado

```text
FULL_VERIFY_REPORTS=99
FULL_VERIFY_TESTS=351
FAILURES=0
ERRORS=0
SKIPPED=0

INVENTORY_UNIT_REPORTS=16
INVENTORY_UNIT_TESTS=41
INVENTORY_INTEGRATION_TESTS=10
INVENTORY_API_JAKARTA=0
BASE_WAR_INVENTORY=0
```

## Comprobaciones funcionales

- autorización exacta antes de acceso al estado de negocio;
- rechazo de sustitución de empresa en contratos CDI;
- conversiones contrastadas contra `CatalogUnitConversions`;
- reintento exacto sin duplicar efectos;
- conflicto estable cuando una clave idempotente cambia su intención;
- rechazo de stock insuficiente y de alcance bloqueado por conteo;
- consumo de reserva con saldo, reserva, movimiento y recibo coherentes;
- cierre de conteo con ajuste append-only y reintento estable;
- rollback JTA explícito para resultados fallidos;
- V2 inmutable, FK empresarial y unicidad de operación validadas en PostgreSQL.

## Incidencia corregida

La primera prueba del nuevo delta agregado detectó que la validación usada sólo
admitía cantidades no negativas. Se sustituyó por la normalización firmada ya
existente y se repitió inmediatamente la prueba hasta quedar verde. Ninguna prueba
fallida fue ignorada o relajada.

Después de retirar imports y una variable sin uso se recompilaron los seis módulos
del corte y se repitieron `InventoryCountServiceTest` e
`InventoryApplicationAdaptersTest`: 5/5 pruebas verdes. Finalmente se repitió
`mvnw.cmd verify` sobre el estado entregado: 24/24 módulos y 351/351 pruebas verdes.

## Revisión documental

Se actualizaron la historia/estado de Sprint, backlog, contrato del plugin, vista
arquitectónica, estrategia de pruebas, guía de implementación, manual técnico y
manual de usuario. Los enlaces relativos de los once documentos afectados y una
búsqueda de caracteres UTF-8 dañados quedaron verdes.

La guía de Visual Studio Code fue revisada y no cambia: J11-S8-04 no agregó JDK,
extensiones, variables, perfiles, comandos de montaje ni rutas de ejecución. La
ficha `implementation-guide/VALIDATION.md` permanece intencionalmente congelada en
la validación independiente de J11-S7-07; este incremento no suplanta ese gate.
Tampoco corresponde regenerar el PDF o el instalador, porque son artefactos del
cierre de Sprint y Sprint 8 continúa abierto.

## Revisión de alcance

No cambiaron Docker, Compose, OIDC, rutas HTTP ni UI. Por eso no corresponden aún
Playwright, capturas responsive o demo de inventario. Los manuales dejan explícito
que los permisos y servicios están disponibles para integración, pero la tarea de
usuario aparecerá recién en J11-S8-05 y la demo nueva en J11-S8-06.
