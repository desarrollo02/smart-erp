# J11-S6-02 - Dominio neutral y contratos públicos de `business_partners`

- Estado: Completada
- Sprint: 6
- Fecha: 2026-07-29
- Dependencia: [J11-S6-01](J11-S6-01-caracterizacion-business-partners.md) completada
- Decisión: [ADR-0014](../../adr/0014-modelo-participante-comercial-y-contrato-publico.md)

## Objetivo

Crear el primer corte Java puro del participante comercial y una API pública
versionada que otros plugins puedan consumir sin acceder a clases internas,
entidades o tablas de `business_partners`.

## Alcance

- módulo `business-partners-api` con identidad, enums, referencia mínima, versión y
  puerto público de lectura;
- módulo desplegable `business-partners` con descriptor CDI/SPI y dominio neutral;
- agregado con empresa, identidad, código, nombres, estado y versión;
- roles cliente/proveedor coexistentes y de estado independiente;
- identificaciones, canales, direcciones y contactos nominales como hijos neutrales;
- invariantes aceptadas BP-D01 a BP-D10;
- pruebas unitarias y límites ArchUnit.

## Fuera de alcance

- esquema `plg_business_partners`, Flyway, JPA, repositorios o Testcontainers;
- comandos de aplicación, búsqueda paginada, autorización o auditoría;
- menús, permisos, endpoints, Jakarta Faces, Material Design o slots de pantalla;
- composición física del WAR/migrador e imagen Docker;
- migración del legado, integración DNIT, eventos y outbox.

## Criterios de aceptación

- **CA-01:** API pública e implementación son módulos físicos separados.
- **CA-02:** `business-partners-api` usa Java estándar y `kernel-api` únicamente
  para `CompanyId`; no usa Jakarta, JPA, JDBC ni infraestructura.
- **CA-03:** `BusinessPartnerId` es UUID canónico y la API pública declara versión
  semántica `1.0.0`.
- **CA-04:** el puerto público consulta por empresa e ID y devuelve una proyección
  mínima inmutable sin datos sensibles ni tipos internos.
- **CA-05:** el agregado permite cero roles, cliente, proveedor o ambos.
- **CA-06:** participante y roles tienen estado independiente; no existe baja
  física en el dominio.
- **CA-07:** códigos se normalizan de forma determinista y no existe `MAX + 1`.
- **CA-08:** nombres, identificaciones, canales, direcciones y contactos aplican
  invariantes neutrales aceptadas.
- **CA-09:** existe como máximo un canal o dirección primaria activa por categoría
  y finalidad dentro del agregado.
- **CA-10:** las mutaciones usan versión esperada y rechazan sobrescritura
  concurrente.
- **CA-11:** el descriptor es `business_partners@1.0.0`, no inventa dependencias,
  migraciones, eventos, permisos, menús o pantallas.
- **CA-12:** pruebas del módulo, ArchUnit y `mvn verify` quedan verdes.
- **CA-13:** documentación, guía de implementadores y evidencia quedan actualizadas.

## Secuencia de implementación

1. agregar API pública y probar identidad, versión y proyección;
2. agregar esqueleto de plugin y probar descriptor CDI/SPI;
3. implementar valores básicos y agregado con pruebas incrementales;
4. agregar hijos neutrales y primarios únicos;
5. reforzar límites ArchUnit;
6. ejecutar `verify`, documentar resultados y cerrar la historia.

## Estado inicial

No existen módulos, clases o artefactos productivos de `business_partners`. La
composición base contiene cero plugins y los fixtures de referencia continúan
siendo exclusivamente técnicos.

## Resultado implementado

- `business-partners-api` expone siete tipos públicos y sólo depende de
  `kernel-api` para `CompanyId`.
- `business-partners` fue creado con el generador oficial y registra una única
  `PluginDefinition` por CDI y `ServiceLoader`.
- El descriptor es `business_partners@1.0.0`, compatible con `plugin-api` 0.3.x y
  sin contribuciones inventadas.
- `BusinessPartner` conserva empresa, ID opaco, código, tipo, nombres, estado,
  versión, roles y detalles neutrales.
- Los roles cliente/proveedor pueden estar ausentes, coexistir y cambiar de estado
  independientemente.
- Las identificaciones conservan valor presentado y clave candidata; detectarlas
  no fusiona ni bloquea universalmente.
- Direcciones y canales reemplazan explícitamente el primario de la misma
  categoría/finalidad; los contactos nominales siguen siendo hijos livianos.
- No existe método de baja física, JPA, SQL, migración, endpoint o UI.

## Criterios verificados

| Criterio | Resultado |
|---|---|
| CA-01/CA-02 | dos módulos físicos; API Java puro con única dependencia empresarial `CompanyId` |
| CA-03/CA-04 | UUID canónico, contrato `1.0.0`, puerto empresarial y referencia mínima inmutable |
| CA-05/CA-06 | cero/uno/dos roles, estados independientes e inactivación sin borrado |
| CA-07/CA-08 | NFKC, mayúsculas invariantes, nombres y detalles validados |
| CA-09 | primario único por categoría/finalidad probado en agregado y contacto |
| CA-10 | versión esperada y excepción tipada ante edición obsoleta |
| CA-11 | descriptor vacío de contribuciones, CDI/SPI equivalente |
| CA-12 | módulo, 14 pruebas arquitectónicas y reactor completo verdes |
| CA-13 | ADR, contrato, guía, arquitectura y evidencia actualizados |

## Pruebas ejecutadas

| Gate | Resultado |
|---|---|
| API | 5 pruebas propias y 6 de `kernel-api` verdes |
| plugin/dominio | 15 pruebas propias verdes |
| ArchUnit | 14 pruebas, incluidas 12 reglas de límites, verdes |
| reactor | 20 módulos, 212 pruebas, 0 fallos, 0 errores, 0 omitidas |
| artefactos | API sin Jakarta/internos; plugin con un CDI/SPI y cero migraciones; WAR base sin ambos JAR |

Los comandos, tiempos, fallos corregidos y conteos están en la
[evidencia J11-S6-02](../../evidence/J11-S6-02-dominio-contratos-business-partners.md).
PostgreSQL, Docker y Playwright no aplican: esta historia no modifica datos,
composición runtime o interfaz.

## Resultado

`J11-S6-02` queda completada. El único siguiente trabajo funcional autorizado es
`J11-S6-03`: diseñar y probar el esquema privado `plg_business_partners`, sus
migraciones inmutables y repositorios, sin cambiar la API pública aceptada por
comodidad de persistencia.
