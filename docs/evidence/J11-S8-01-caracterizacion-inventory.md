# Evidencia J11-S8-01 - Caracterización de `inventory`

- Fecha: 2026-07-31
- Estado: Completa; IN-D01 a IN-D10 aceptadas sin cambios el 2026-07-31
- Historia: [J11-S8-01](../sprints/sprint-08/J11-S8-01-caracterizacion-inventory.md)
- Resultado: [base de conocimiento](../knowledge-base/inventory/legacy-characterization.md)

## Alcance de la inspección

Se consultó el árbol `C:\cosme\multienvios\miaterra\fuente\tag` después de leer
su `AGENTS.md`. No se escribió ningún archivo en el proyecto legado.

La búsqueda partió de stock, inventario, existencia, depósito, ubicación,
entrada/salida, movimiento, reserva, lote, serie, vencimiento, ajuste,
transferencia, unidad y permisos. Luego se redujo a fuentes que demuestran
comportamiento y dependencias de dominio.

## Fuentes contrastadas

| Área | Evidencia observada |
|---|---|
| navegación/seguridad | menú Stock, permisos por forma y constantes |
| saldo | `StwExistenciaArt` por empresa/sucursal/artículo |
| contabilización | `StwEntsalCab`, `StwEntsalDet` y `StwEntSalEJB` |
| historial/traslado | `StwMovimientoArt` y `StwMovArtEjb` |
| almacenamiento | `BswDeposito`, `StwUbicArticulos` y su controlador |
| conteo | `StwInventCab`, `StwInventDet` y vista de control de inventario |
| reserva | `StwReservasArticulos` y ausencia de consumo/expiración operativos |
| unidad/trazabilidad | unidades, cantidad base, lote y vencimiento |
| frontera externa | relaciones hacia compras, ventas, remisiones, personas, producción y taller |
| contrato actual | `commercial-catalog-api` para referencia y conversión públicas |

## Evidencias negativas relevantes

- el saldo se muta directamente y no constituye un libro reproducible;
- el servicio del historial permite actualizar y borrar;
- el motivo inventario reemplaza saldo sin ajuste por diferencia;
- la clave de saldo omite depósito, ubicación y trazabilidad;
- una reserva no posee ciclo de vida suficiente;
- lote/vencimiento no forman la clave de existencia;
- el número visible se genera con `MAX + 1`;
- cabecera/detalle importan entidades de muchos dominios;
- ubicación está ligada al artículo y su opción observada está deshabilitada;
- la vista de control de inventario contiene referencias inconsistentes.

Estos hallazgos justifican modelar desde casos de uso e invariantes, no portar las
clases, tablas o pantallas.

## Archivos creados o modificados

- `docs/knowledge-base/inventory/legacy-characterization.md`;
- `docs/sprints/sprint-08/J11-S8-01-caracterizacion-inventory.md`;
- `docs/sprints/sprint-08/README.md`;
- este documento e índices relacionados;
- backlog, guía de implementación y manual técnico, solo para reflejar estado.

No se creó Java, POM, SQL, migración, XHTML, CSS, imagen, volumen o dato.

## Matriz de validación aplicable

| Gate | Resultado |
|---|---|
| legado sin modificaciones | cumplido por procedimiento de solo lectura |
| fuente → observación → requisito | cumplido en IN-O01 a IN-O20 |
| separación entre dominios | cumplida en la frontera y matriz de propietarios |
| casos e invariantes | IN-UC-01 a IN-UC-18 e IN-I01 a IN-I22 documentados |
| decisiones | alternativas e impacto documentados; diez recomendaciones aceptadas |
| código/pruebas automatizadas | no aplica; no hubo cambios de código |
| documentación | 214 Markdown, 0 enlaces rotos, 0 errores UTF-8, 0 mojibake y 0 filtraciones de secretos |

## Revisión de documentación transversal

| Documento | Resultado |
|---|---|
| guía de implementación | actualizada para indicar caracterización y bloqueo de modelado |
| manual técnico | actualizado con frontera y condición de continuidad |
| manual de usuario | revisado; sin cambio porque no existe nueva capacidad visible |
| guía Visual Studio Code | revisada; sin cambio porque no cambió build, extensión ni ejecución |
| estructura de plugins del Sprint | no se genera todavía; es entregable de cierre y no existe plugin implementado |
| PDF de estructura del repositorio | no se regenera; Sprint 8 no está cerrando |

## Gate documental G0

El validador recorrió todos los Markdown mantenidos bajo el repositorio, resolvió
enlaces locales desde cada documento, decodificó UTF-8 de forma estricta y buscó
secuencias de texto dañado y patrones de secretos.

```text
MARKDOWN_FILES=214
BROKEN_LINKS=0
ENCODING_ERRORS=0
MOJIBAKE_FILES=0
SECRET_LEAKS=0
```

## Condición de continuidad

Producto confirmó IN-D01 a IN-D10 sin cambios el 2026-07-31. Se autoriza
`J11-S8-02` para `inventory-api` y dominio neutral; esquema, migración, aplicación
y UI continúan reservados a J11-S8-03 y posteriores. G7 independiente de la guía
general sigue pendiente y no autoriza promoción ni producción.
