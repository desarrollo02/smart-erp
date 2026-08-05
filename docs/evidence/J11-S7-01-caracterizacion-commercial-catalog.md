# Evidencia J11-S7-01 - Caracterización de `commercial_catalog`

- Fecha: 2026-07-30
- Estado: Completa; aceptación de producto registrada el 2026-07-30
- Historia: [J11-S7-01](../sprints/sprint-07/J11-S7-01-caracterizacion-commercial-catalog.md)
- Resultado: [base de conocimiento](../knowledge-base/commercial-catalog/legacy-characterization.md)

## Alcance de la inspección

Se consultó el árbol `C:\cosme\multienvios\miaterra\fuente\tag` y dos scripts
relacionados bajo `C:\cosme\multienvios\miaterra\scripts`. No se escribió ningún
archivo en el proyecto legado.

La búsqueda partió de rutas y símbolos relacionados con artículo, producto,
servicio, catálogo, precio, marca, categoría, unidad, IVA, moneda y lista. Luego
se redujo a fuentes que muestran comportamiento del maestro, definiciones,
conversiones y precios o que demuestran dependencias hacia otros dominios.

## Fuentes contrastadas

| Área | Evidencia observada |
|---|---|
| maestro | `StwArticulos` y su restricción código/empresa |
| aplicación/UI | `StwCOArticulosControlador`, pantalla y parciales |
| deuda histórica | `StwArticulosControlador` y eliminación física |
| definiciones | unidades, conversiones, familia, grupo, línea, marca e IVA |
| precios | `StwListaPrecio`, `StwPreciosFijos` y documentación funcional |
| consumo | caso de uso de Facturación V2 y búsquedas en ventas/compras/stock |
| importación | caso de uso de importación masiva y scripts de listas por empresa |

## Evidencias negativas relevantes

- el maestro importa entidades de muchos dominios y posee colecciones JPA cruzadas;
- existe `MAX(cod_articulo) + 1`;
- una pantalla antigua elimina físicamente artículos;
- precio base y precios 2/3 coexisten con listas configurables;
- estados activos usan convenciones incompatibles;
- barcode está limitado a un campo único;
- tipo compra/venta se confunde fácilmente con producto/servicio;
- el precio mezcla alcances que pertenecen a ventas, sucursales y devoluciones;
- flags sectoriales hacen que el maestro conozca producción, taller y transporte;
- datos fiscales internos no están separados de su futura representación oficial.

Estos hallazgos justifican modelar desde casos de uso e invariantes, no portar las
clases o tablas.

## Archivos creados o modificados

- `docs/knowledge-base/commercial-catalog/legacy-characterization.md`;
- `docs/sprints/sprint-07/J11-S7-00-gobierno-planificacion.md`;
- `docs/sprints/sprint-07/J11-S7-01-caracterizacion-commercial-catalog.md`;
- `docs/sprints/sprint-07/README.md`;
- este documento e índices relacionados;
- guía de implementación y backlog, únicamente para reflejar el estado.

No se creó Java, POM, SQL, migración, XHTML, CSS, imagen, volumen o dato.

## Matriz de validación aplicable

| Gate | Resultado |
|---|---|
| legado sin modificaciones | cumplido por procedimiento de solo lectura |
| fuente → observación → requisito | cumplido en CC-O01 a CC-O18 |
| separación entre dominios | cumplida en la frontera y matriz de propietarios |
| casos e invariantes | CC-UC-01 a CC-UC-15 y CC-I01 a CC-I18 documentados |
| decisiones | alternativas y recomendación listas; producto pendiente |
| código/pruebas automatizadas | no aplica; no hubo cambios de código |
| documentación | 177 Markdown, 0 enlaces rotos, 0 errores UTF-8, 0 mojibake y 0 filtraciones de secretos |

## Gate documental G0

El validador recorrió todos los Markdown mantenidos bajo el repositorio, resolvió
enlaces locales desde cada documento, decodificó UTF-8 de forma estricta y buscó
secuencias de texto dañado y patrones de secretos.

```text
MARKDOWN_FILES=177
BROKEN_LINKS=0
ENCODING_ERRORS=0
MOJIBAKE_FILES=0
SECRET_LEAKS=0
```

## Condición de continuidad

El responsable de producto confirmó CC-D01 a CC-D10 sin cambios el 2026-07-30 y
ratificó que `commercial_catalog` será otro módulo/plugin funcional. Se autoriza
`J11-S7-02` para API pública y dominio neutral; JPA, migración V1 y UI siguen
reservados a `J11-S7-03` y posteriores.

G7 independiente de la guía general continúa pendiente y no autoriza promoción ni
producción, aunque no bloquea esta caracterización.
