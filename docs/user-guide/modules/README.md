# Manuales de usuario por módulo

Este directorio contiene un manual independiente por cada módulo navegable del baseline candidato de Sprint 9. Cada manual explica el vocabulario funcional, las pantallas, los datos, los permisos, los recorridos operativos y las tablas afectadas.

| Orden | Módulo | Pantallas | Fuente | Ayuda web | PDF |
|---:|---|---:|---|---|---|
| 01 | Administración segura del kernel | 6 | `administracion-kernel.md` | `web/administracion-kernel.html` | `../../output/pdf/manuales-modulos/01-manual-administracion-kernel.pdf` |
| 02 | Datos de referencia | 1 | `datos-referencia.md` | `web/datos-referencia.html` | `../../output/pdf/manuales-modulos/02-manual-datos-referencia.pdf` |
| 03 | Socios comerciales | 2 | `socios-comerciales.md` | `web/socios-comerciales.html` | `../../output/pdf/manuales-modulos/03-manual-socios-comerciales.pdf` |
| 04 | Catálogo comercial | 5 | `catalogo-comercial.md` | `web/catalogo-comercial.html` | `../../output/pdf/manuales-modulos/04-manual-catalogo-comercial.pdf` |
| 05 | Inventario | 3 | `inventario.md` | `web/inventario.html` | `../../output/pdf/manuales-modulos/05-manual-inventario.pdf` |
| 06 | Panel de demostración | 1 | `panel-demostracion.md` | `web/panel-demostracion.html` | `../../output/pdf/manuales-modulos/06-manual-panel-demostracion.pdf` |
| 07 | Compras | 5 | `compras.md` | `web/compras.html` | `../../output/pdf/manuales-modulos/07-manual-compras.pdf` |

El manual `07` documenta las pantallas reales definidas por J11-S9-05 y compuestas
por J11-S9-06. Sus pruebas automatizadas de módulo, PostgreSQL, arquitectura,
runtime y Playwright están verdes; la validación independiente y el cierre de
Sprint 9 permanecen pendientes, por lo que el manual no presenta el módulo como
productivo. La fuente Markdown y su ayuda web ya reflejan J11-S9-06; el PDF 07
continúa siendo el artefacto derivado de J11-S9-05 y se regenerará, renderizará y
revisará visualmente dentro del cierre J11-S9-07.

El manual 01 incorpora una captura real y un ejemplo guiado por cada una de sus
seis pantallas. Sus diagramas fueron contrastados el 2026-08-11 con los metadatos
de PostgreSQL local mediante consultas de solo lectura; las imágenes usan
exclusivamente la demo ficticia y no contienen credenciales.

El orden sigue las dependencias de aprendizaje: primero acceso y administración,
luego datos base y maestros comerciales, después catálogo, inventario y compras,
manteniendo el panel técnico como referencia de composición.

## Convenciones de los diagramas

- `C`: la pantalla crea filas.
- `R`: la pantalla consulta filas.
- `U`: la pantalla modifica filas existentes.
- `D`: la pantalla elimina filas. En este baseline no hay borrado físico desde estas pantallas.
- `EXT`: dato obtenido mediante un contrato o servicio, sin acceso directo a una tabla privada de otro módulo.
- `PK`, `FK` y `UK`: clave primaria, clave foránea y restricción de unicidad.

Los PDF y las páginas web son artefactos derivados. Las fuentes mantenidas son estos archivos y el generador reproducible `tools/generate_module_user_manuals.ps1`.
