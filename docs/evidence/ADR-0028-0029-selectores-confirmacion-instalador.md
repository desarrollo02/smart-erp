# Evidencia - Gobierno de selectores y confirmación del instalador

- Fecha: 2026-08-01
- Alcance: análisis y planificación documental
- Estado: verde en G0; implementación de J11-S8-C02 pendiente

## Inspección realizada

Se inspeccionaron contratos de pantalla, handlers de opciones, XHTML administrativo,
dominio, casos de uso y migraciones de kernel, `business_partners`,
`commercial_catalog`, `inventory` y fixtures. Se excluyeron `target/` y artefactos
generados.

Resultado:

- 18 selectores lógicos nativos del shell/kernel;
- 4 selectores de `business_partners`;
- 20 selectores de `commercial_catalog`;
- 27 selectores de `inventory`;
- 69 selectores lógicos en total;
- tres controles físicos del renderer genérico que no se cuentan nuevamente como
  fuentes de datos;
- cero selectores funcionales en los fixtures de referencia/personalización.

Las brechas se clasificaron sin alterar código: unidades, categorías, marcas,
etiquetas, variantes, ciclo completo de perfiles tributarios, tipos de
identificación, canales, países/monedas y acceso contextual común. Estados cerrados
no se reclasificaron como datos editables.

## Decisiones registradas

- ADR-0028 define cinco clases de fuente y exige administración para catálogos
  empresariales.
- ADR-0029 obliga a preguntar `SÍ` o `NO` antes de crear el instalador de cada
  cierre.
- J11-S8-C02 queda planificada antes de recongelar Sprint 8 o iniciar `purchasing`.
- No se modificó ni eliminó `installer/windows/current`.

## Validación

G0 final recorrió 252 archivos Markdown y 993 enlaces locales:

- errores UTF-8: 0;
- enlaces locales rotos: 0;
- referencias contradictorias vigentes a instalador obligatorio: 0; las menciones
  restantes son históricas o condicionales a respuesta `SÍ`.

No se ejecutaron Maven, Docker, PostgreSQL ni Playwright porque este corte no
modifica Java, XHTML, CSS, POM, migraciones, imágenes, Compose ni artefactos del
instalador. La implementación y sus pruebas pertenecen a J11-S8-C02.

