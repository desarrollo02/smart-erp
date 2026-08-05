# Importar publicaciones completas de datos de referencia

- Verificado: 2026-08-05
- Alcance: UN M49 e ISO 4217 List One observadas para J11-S8-C07
- Red durante la generación: no permitida

El script `tools/generate_reference_data_publication.ps1` transforma los originales
validados conservados en `.tools/downloads/reference-data/`. Antes de generar SQL
verifica ruta, tamaño, SHA-256, fecha de publicación, estructura, cardinalidad,
unicidad y los 13 valores de unidad menor `N.A.`.

Desde la raíz del repositorio:

```powershell
powershell -ExecutionPolicy Bypass -File tools/generate_reference_data_publication.ps1
```

El resultado queda en
`.tools/tmp/reference-data/V4__publish_full_reference_data.sql`. La revisión lo
compara byte por byte con la migración versionada. El script no sobrescribe código
fuente ni accede a Internet.

Resultado esperado para los originales observados:

```json
{"sha256":"72cb35a11073cb232129074551af0d0e8181eb8f4a4e52f1492c2883a287d3c0","countries":248,"currencies":178,"not_applicable_minor_units":13,"source_currency_rows":277}
```

Verifique además que el SHA-256 de ambos archivos coincida:

```powershell
Get-FileHash -Algorithm SHA256 `
  .tools/tmp/reference-data/V4__publish_full_reference_data.sql, `
  plugins/reference-data/src/main/resources/db/migration/reference_data/V4__publish_full_reference_data.sql
```

Si cambia tamaño, checksum, estructura, cardinalidad, unicidad o fecha SIX, el
script termina con error antes de crear una migración aceptable. No corrija el
original ni relaje los valores esperados sin revisar la nueva edición, sus
diferencias, licencia y procedencia.

PowerShell, Git y el sistema operativo son prerrequisitos de plataforma. Maven,
Java, dependencias, originales y temporales continúan gobernados bajo `.tools/`.
