# Reference data

- Artifact Maven: `reference-data`
- Plugin ID: `reference_data`
- Tipo: `FUNCTIONAL`
- Versión inicial: `1.0.0`
- Compatibilidad Plugin API: `[0.4.0,0.5.0)`
- Contrato público: `reference-data-api@1.0.0`

Fundación R0 para países, monedas y procedencia normativa compartida. El kernel no
posee estos maestros y cada consumidor usa la API pública, nunca el esquema o las
clases internas del plugin.

## Corte inicial

- esquema privado `plg_reference_data` V1 con cinco tablas;
- publicaciones inmutables con autoridad, URI, fecha, SHA-256, completitud y
  cantidad;
- `BOOTSTRAP_SUBSET` con Paraguay (`PY`/`PRY`/`600`), guaraní
  (`PYG`/`600`, 0 decimales) y dólar estadounidense (`USD`/`840`, 2 decimales);
- políticas de habilitación por empresa; ausencia de política significa habilitado;
- permiso `reference_data.view` y pantalla de sólo lectura `/reference-data`;
- capacidades `reference_data.directory` y `reference_data.provenance`;
- descubrimiento CDI y `ServiceLoader` para WAR/migrador.

El corte no contiene una publicación mundial completa, importador automático,
tasas de cambio ni edición de códigos desde el navegador. No accede a Internet en
runtime.

## Consumo

`business_partners` y `commercial_catalog` declaran dependencia requerida
`reference_data [1.0.0,2.0.0)` y consumen únicamente `reference-data-api`. Ambos
vuelven a resolver empresa y código dentro de sus transacciones de alta.

Consulte [plugin-contract.md](docs/plugin-contract.md) antes de ampliar catálogos,
políticas o publicaciones.

## Prueba local

```powershell
.\mvnw.cmd -B -pl plugins/reference-data -am test
.\mvnw.cmd -B -pl plugins/reference-data -am `
  "-Dlogixone.postgres.integration=true" verify
.\mvnw.cmd -B -pl tests/architecture-tests -am test
```
