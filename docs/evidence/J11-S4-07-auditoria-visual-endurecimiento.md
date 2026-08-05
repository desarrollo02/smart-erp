# Evidencia de J11-S4-07 — Auditoría visual y endurecimiento

- Fecha: 2026-07-28
- Estado: implementada pendiente de pruebas
- Historia: [J11-S4-07](../sprints/sprint-04/J11-S4-07-auditoria-visual-endurecimiento.md)

## Resultado

La auditoría técnica dejó de ser únicamente una emisión a logs. V5 incorpora el
almacén append-only `core.audit_event`; los cinco adaptadores existentes persisten
un sobre común y siguen emitiendo el log estructurado operativo.

Se agregó `/admin/audit.xhtml` con:

- autorización exacta `kernel.audit.view`;
- páginas fijas de 25 registros, máximo neutral de 50;
- orden reciente determinista;
- filtros cerrados por categoría, resultado, ventana temporal, empresa exacta y
  correlación exacta;
- identificadores técnicos, códigos y versiones, sin identidad externa ni datos
  comerciales;
- aviso explícito de que el historial consultable comienza en V5.

`AdminAuthorizationFilter` aplica ahora `no-store`, `nosniff`, `DENY`, política de
referencia, política de capacidades y CSP tanto en respuestas permitidas como
denegadas de la zona administrativa.

## Migración V5

Recurso:
`migrator/src/main/resources/db/migration/core/V5__add_technical_audit_event.sql`.

- tamaño: `3569` bytes;
- SHA-256: `0AACBA3999424DBB00337D7DF39936E9D702E1E2DF8D413A80817E5C8A52D625`;
- V1–V4 no fueron modificadas;
- readiness exige ahora versión `5`;
- el recurso quedó dentro de `migrator-0.1.0-SNAPSHOT.jar`.

La tabla no tiene FKs hacia recursos actuales para conservar historia. Un trigger
rechaza `UPDATE` y `DELETE`; no existe operación web de modificación, eliminación o
exportación. Los eventos anteriores no se importan desde logs.

## Transacciones y privacidad

Las auditorías de mutaciones usan la misma transacción JTA que la operación. Un
fallo de persistencia propaga el error y fuerza rollback. Los adaptadores de acceso
empresarial y autoridad global ahora usan transacciones cortas para confirmar
decisiones permitidas y denegadas.

El sobre persistido admite exclusivamente IDs locales, categoría, operación,
resultado, código, versiones, correlación e instante. No contiene issuer, subject
OIDC, nombre, token, cookie, contraseña, secreto, claims, SQL, stacktrace o dato
comercial.

## Compilación y empaquetado

Se reutilizó el JDK 21.0.11+10 validado bajo `.tools/jdk/`:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk/jdk-21.0.11+10').Path
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl kernel-infrastructure-jakarta -am compile -DskipTests
.\mvnw.cmd -pl web-shell -am compile -DskipTests
.\mvnw.cmd -Pwith-screen-customization-plugins `
  -pl distribution/logixone-war -am package -DskipTests
```

Los cortes terminaron `BUILD SUCCESS`: 7/7, 6/6 y 12/12 módulos. El empaquetado
compiló seis fuentes de prueba de infraestructura y una fuente web; Surefire indicó
`Tests are skipped` y ninguna prueba fue ejecutada.

WAR:

- ruta: `distribution/logixone-war/target/logixone.war`;
- tamaño: `609004` bytes;
- SHA-256: `95BC18DACA7D4A5CFA5D70C6FCA7BD26586E71F9A6C7F9B930A324EE3A3414FA`;
- páginas administrativas incluidas: `index.xhtml`, `companies.xhtml`,
  `plugins.xhtml`, `security.xhtml`, `system-authority.xhtml` y `audit.xhtml`.

## Revisión estática

- `audit.xhtml` se reabrió como XML: válido;
- referencias JPA, infraestructura o plugins concretos desde Java de
  `web-shell`: `0`;
- imports Jakarta en `plugin-api`, `kernel-api`, `kernel-domain` o
  `kernel-application`: `0`;
- imports `javax.*` prohibidos en aplicación, infraestructura y web: `0`; se
  excluyó el uso legítimo preexistente de `javax.sql.DataSource` de Java SE;
- el contrato de consulta es neutral y el bean web no importa entidades;
- el WAR contiene la nueva página y el JAR migrador contiene V5.

## Pruebas pendientes

No se ejecutaron JUnit, ArchUnit, PostgreSQL/Testcontainers, migración V1→V5,
JPA/JTA runtime, OIDC/WildFly, cabeceras HTTP reales, Docker/Compose, seguridad
negativa ni Playwright. CA-01 a CA-15 y los gates G2–G7 permanecen pendientes para
`J11-S4-08`.

La compilación y la inspección estática no certifican la sintaxis PostgreSQL del
trigger, el commit/rollback conjunto, la persistencia de denegaciones, la consulta
real, CSP en WildFly ni la responsividad. La demo administrativa aún no se anuncia
como validada.

## Siguiente paso

`J11-S4-08`: ejecutar toda la matriz acumulada, corregir cualquier fallo, preparar
datos ficticios, validar la demo administrativa a 375/720/1280, completar el
recorrido independiente, regenerar el PDF y cerrar Sprint 4. Sólo después comienza
la planificación del primer plugin productivo.
