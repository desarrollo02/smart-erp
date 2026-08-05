# Evidencia J11-S6-04 - Aplicación y seguridad de `business_partners`

- Fecha: 2026-07-29
- Resultado: Verde
- Alcance: aplicación neutral, permisos, autorización, auditoría y PostgreSQL

## Capacidades verificadas

- alta con código manual o secuencia transaccional;
- cambio versionado de nombre y código;
- identificaciones con advertencia no bloqueante de duplicado;
- direcciones, canales y contactos;
- rol cliente/proveedor y estado independiente;
- ciclo de vida sin borrado físico;
- búsqueda paginada por texto/identificación, rol y estado;
- detalle aislado por empresa y directorio público mínimo;
- permisos `view`, `manage`, `roles.manage` y `lifecycle.manage`;
- revalidación actual entregada como prueba neutral de una sola operación;
- auditoría técnica central sin nombres, documentos, direcciones o canales.

## Ejecuciones

### Prueba específica del plugin

```powershell
.\mvnw.cmd -B -pl plugins/business-partners -am test
```

Resultado: 26 pruebas del módulo, 0 fallos, 0 errores y 0 omitidas. Seis escenarios
de aplicación cubren alta, autorización negativa, concurrencia, duplicados, permisos
separados y aislamiento empresarial.

### PostgreSQL del plugin

```powershell
.\mvnw.cmd -B -pl plugins/business-partners -am verify `
  '-Dlogixone.postgres.integration=true'
```

Resultado: 14 escenarios PostgreSQL/Testcontainers, 0 fallos. Incluye 10 de JPA y
repositorios y 4 de migración. La búsqueda se ejecutó sobre PostgreSQL 18.4 con
filtro por identificación/rol/estado, paginación e aislamiento por empresa.

### Migraciones centrales

```powershell
.\mvnw.cmd -B -pl migrator -am verify `
  '-Dlogixone.postgres.integration=true'
```

Resultado: 12 escenarios PostgreSQL/Testcontainers, 0 fallos. Una base vacía aplica
V1–V6; bases V1, V2, V3, V4 y V5 convergen al mismo esquema; una segunda ejecución
aplica cero; los checksums modificados son rechazados.

- Recurso: `migrator/src/main/resources/db/migration/core/V6__extend_audit_for_plugin_operations.sql`
- SHA-256: `ac4f1128e6ed31618376d213bc801b29c77b0dba99f3aec7c49b1dd10b4bee35`

### Persistencia central de auditoría

```powershell
.\mvnw.cmd -B -pl kernel-infrastructure-jakarta -am verify `
  '-Dlogixone.postgres.integration=true'
```

Resultado: 13 escenarios de `PostgreSqlRepositoryIT`, 0 fallos. El nuevo escenario
persiste y vuelve a consultar un evento `PLUGIN_OPERATION`, incluyendo plugin,
permiso, tipo e identificador técnico del recurso y versiones anterior/nueva, sin
incorporar datos comerciales al sobre de auditoría.

### Límites arquitectónicos

```powershell
.\mvnw.cmd -B -pl tests/architecture-tests -am test
```

Resultado: 15 pruebas, incluidas 13 reglas `ModuleBoundariesArchitectureTest`, sin
fallos. La aplicación completa de `business_partners` queda libre de Jakarta y no
depende de su infraestructura; los plugins no dependen de implementaciones del
kernel.

### Reactor completo

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools\jdk\jdk-21.0.11+10').Path
.\mvnw.cmd -B verify
```

Resultado: 20 módulos verdes en 47,244 s; 229 pruebas unitarias, 0 fallos, 0
errores y 0 omitidas. El WAR base conserva cero JAR de `business-partners`, porque
su composición física corresponde a J11-S6-06.

### Documentación

```powershell
& 'C:\Users\sdiaz\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' `
  'tmp\validate_docs.py'
```

Resultado: 161 archivos Markdown; 0 enlaces locales rotos, 0 errores UTF-8, 0
archivos con mojibake y 0 coincidencias con secretos locales. Los ejemplos que
enseñan a buscar caracteres dañados usan escapes Unicode para no producir falsos
positivos en el propio detector.

## Alcance no ejecutado

- No corresponde Playwright: J11-S6-04 no crea pantallas.
- No corresponde demo visual: la primera visualización del plugin es J11-S6-05.
- La composición Docker de V6 y del plugin se ejecutará en J11-S6-06.
- El Sprint continúa abierto; demo de cierre y PDF corresponden a J11-S6-07.
