# Backup y restauración controlada de PostgreSQL

- Fecha de actualización: 2026-07-28
- Historia de la última ejecución completa: `J11-S1-03`
- Alcance: backup lógico, validación y restauración de prueba sin modificar la base principal
- Estado: procedimiento probado; su ejecución se registra en la [evidencia de cierre](../evidence/J11-S1-03-cierre.md)

## Objetivo

Crear un backup lógico reproducible de `logixone`, validarlo y restaurarlo en `logixone_restore_probe`. La base temporal demuestra recuperabilidad sin sobrescribir la única copia operativa ni eliminar `logixone_postgres-data`.

## Prerrequisitos

- Docker Engine y Compose operativos.
- `infra/compose/compose.env.local` y el secreto local preparados según [Validación y operación de Compose](compose.md).
- PostgreSQL de la composición saludable.
- Ejecución desde la raíz del proyecto.
- Espacio disponible bajo `.tools/tmp/`.

No imprimir el secreto, no incluir el volcado en Git y no ejecutar `docker compose down --volumes`.

### Preparación para `core` V3

`J11-S3-02` agregó la migración aditiva V3 y `J11-S3-08` la validó sobre PostgreSQL
real. Antes de aplicarla sobre cualquier volumen compartido nuevo:

1. ejecutar este procedimiento completo contra el estado V2;
2. conservar el dump bajo `.tools/tmp/` con un nombre que identifique `pre-v3`, fecha y ambiente;
3. registrar tamaño y SHA-256 sin copiar credenciales ni datos del dump a la evidencia;
4. restaurar en la base temporal y confirmar V1/V2 antes de ejecutar el migrador V3;
5. no usar `Flyway clean`, `repair` ni borrar el volumen como mecanismo de rollback.

V3 no elimina ni transforma filas V2. Volver al artefacto anterior debe conservar las nuevas tablas; restaurar el backup solo se considera ante corrupción o un procedimiento de recuperación aprobado, no para “desinstalar” la migración.

## 1. Arrancar PostgreSQL sin alterar el volumen

```powershell
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml up -d --wait --wait-timeout 120 postgres
$postgres = (docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml ps -q postgres).Trim()
```

Resultado esperado: `$postgres` contiene un identificador y el servicio aparece `healthy`.

## 2. Crear y validar el backup

```powershell
New-Item -ItemType Directory -Force -Path .tools/tmp | Out-Null
docker exec $postgres pg_dump -U logixone -d logixone -Fc -f /tmp/logixone-core-v2.dump
docker exec $postgres pg_restore --list /tmp/logixone-core-v2.dump | Out-Null
docker cp "${postgres}:/tmp/logixone-core-v2.dump" .tools/tmp/logixone-core-v2.dump
Get-Item -LiteralPath .tools/tmp/logixone-core-v2.dump | Select-Object Length
Get-FileHash -Algorithm SHA256 -LiteralPath .tools/tmp/logixone-core-v2.dump
```

Resultado esperado: todos los comandos terminan con código 0, el archivo tiene longitud mayor que cero y queda identificado por SHA-256. El checksum permite detectar alteraciones; no sustituye cifrado ni control de acceso.

## 3. Restaurar en una base temporal

La base temporal tiene un nombre fijo y separado. Si existe por una prueba previa incompleta, se elimina únicamente esa base antes de recrearla.

```powershell
docker exec $postgres dropdb --if-exists -U logixone logixone_restore_probe
docker exec $postgres createdb -U logixone logixone_restore_probe
docker exec $postgres pg_restore -U logixone -d logixone_restore_probe /tmp/logixone-core-v2.dump
docker exec $postgres psql -U logixone -d logixone_restore_probe -v ON_ERROR_STOP=1 -Atc "select property_key || '=' || property_value from core.system_metadata order by property_key;"
docker exec $postgres psql -U logixone -d logixone_restore_probe -v ON_ERROR_STOP=1 -Atc "select version || '|' || checksum || '|' || success from core.flyway_schema_history order by installed_rank;"
docker exec $postgres psql -U logixone -d logixone_restore_probe -v ON_ERROR_STOP=1 -Atc "select 'company=' || count(*) from core.company union all select 'activation=' || count(*) from core.company_plugin_activation;"
```

Resultado esperado: la metadata restaurada coincide con la base fuente y V1/V2 aparecen exitosas con los mismos checksums. También deben existir `core.company` y `core.company_plugin_activation`, con la misma cantidad de filas que en la fuente.

## 4. Limpiar únicamente artefactos de prueba

```powershell
docker exec $postgres dropdb --if-exists -U logixone logixone_restore_probe
Remove-Item -LiteralPath .tools/tmp/logixone-core-v2.dump -Force
docker compose --env-file infra/compose/compose.env.local -f infra/compose/compose.yaml down
```

La eliminación se limita a la base `logixone_restore_probe` y al archivo exacto de prueba. `down` conserva el volumen nombrado y el archivo temporal dentro del contenedor desaparece al retirarlo.

## Recuperación real

Ante pérdida o corrupción, no restaurar directamente sobre la única base recuperable:

1. conservar el volumen afectado y reunir logs;
2. verificar tamaño, checksum, procedencia y compatibilidad del backup;
3. crear una base o volumen de recuperación aislado;
4. copiar el backup al contenedor con `docker cp` y ejecutar `pg_restore --list`;
5. restaurar con `pg_restore`, consultar metadata y ejecutar el migrador;
6. arrancar la aplicación y ejecutar health y smoke;
7. promover el estado recuperado solo con evidencia y autorización operativa.

## Diagnóstico y reversión

- Si `pg_dump` o `pg_restore --list` falla, no continuar: conservar el origen y generar un backup válido.
- Si la restauración falla, reunir logs y eliminar solo `logixone_restore_probe`; la base `logixone` no fue modificada.
- Si la metadata no coincide, tratar el backup como no recuperable hasta explicar la diferencia.
- Si el archivo local contiene datos reales, aplicar controles de acceso, cifrado y retención definidos por el ambiente antes de sacarlo del host controlado.
