# Evidencia J11-S9-05 — Interfaz de `purchasing`

- Fecha: 2026-08-11
- Rama local: `sprint/09-purchasing`
- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Pruebas automatizadas ejecutadas: módulo, shell, PostgreSQL/Testcontainers, ArchUnit y `mvn verify`

## Evidencia estática

- `PurchasingScreenContract` define cinco rutas y cinco contratos con dos slots
  seguros por pantalla.
- `PurchasingPluginDefinition` aporta cinco menús, cinco pantallas y requiere
  `business_partners@1.1` e `inventory@1.1`.
- Existen handlers independientes para solicitudes, órdenes, recepciones,
  devoluciones y seguimiento.
- `PurchasingSelectorSources` cubre los selectores cerrados, empresariales,
  normativos y operativos.
- `JpaPurchasingDirectoryRepository` filtra siempre por empresa y pagina los
  cuatro directorios sin acceder a otro esquema.
- `ShellScreenRegistry` registra las cinco rutas y sus regiones; `ShellTextCatalog`
  contiene etiqueta y ayuda para cada control.
- La búsqueda pública de proveedores devuelve solo referencias mínimas; el
  directorio de almacenamiento revalida empresa y permiso de Inventario.
- `commercial-catalog-api@1.1.0` lleva el alcance `PURCHASE` al repositorio para
  calcular total y página sin filtrado posterior en el handler.
- Las pruebas de descriptor, versión y cobertura de selectores fueron actualizadas
  y ejecutadas.

## Manual 07

- Fuente: `docs/user-guide/modules/compras.md`.
- Ayuda web: `docs/user-guide/modules/web/compras.html`.
- PDF: `docs/output/pdf/manuales-modulos/07-manual-compras.pdf`.
- Cobertura: 5 pantallas, 5 bosquejos y 5 diagramas de tablas.
- Páginas: 15.
- Tamaño: 277.824 bytes.
- SHA-256: `9B2F918F46554C0EE609B1DC125258C2E409D08C9B7066F59CCDCA15D5A1C68D`.
- Estructura PDF: encabezado `%PDF-1.4`, EOF presente, 15 objetos de página,
  título, árbol estructural y mapas `ToUnicode` presentes.
- Revisión visual: las 15 páginas fueron renderizadas y revisadas en la hoja de
  contacto; portada, tablas, bosquejos, diagramas, pies y cortes son legibles, no
  hay páginas vacías ni contenido desbordado visible.
- La tabla de permisos explicita además `inventory.view`,
  `inventory.movements.purchase.post` y los permisos que habilitan las rutas
  Administrar de catálogos relacionados.
- Metadatos de tablas/triggers: derivados de migraciones V1–V2 y contrastados en
  PostgreSQL 18.4 efímero de Testcontainers. No se consultó ninguna base de datos
  del usuario ni un servicio configurado manualmente.

La política del navegador integrado impidió abrir una URL `file:` para una
segunda comprobación interactiva del texto. No se intentó eludir esa política.
La verificabilidad textual conserva como fuente canónica el HTML UTF-8 y el PDF
incluye estructura y mapas Unicode; una extracción con herramienta gobernada bajo
`.tools/` se acumula con el gate documental final.

## Revisión estática ejecutada

- comparación de los 96 identificadores de contrato con el catálogo de textos:
  cero identificadores ausentes;
- inspección de rutas, regiones, permisos y fuentes de selector;
- inspección de referencias cruzadas: solo APIs e identificadores públicos;
- `git diff --check` sin errores antes del cierre documental;
- generación reproducible y render de las 15 páginas del manual.

Estas comprobaciones estáticas se complementaron con compilación, pruebas de
módulo, integración PostgreSQL y arquitectura. Playwright de Compras requiere la
composición de J11-S9-06 y continúa como gate automatizado, no como prueba humana.

## Ejecución automatizada y pendientes reales

- una primera compilación detectó dos ternarios inferidos como `Object` en los
  handlers de solicitudes y órdenes; se tiparon como
  `PurchasingOperationResult<T>` y la repetición quedó verde;
- `web-shell`: 59 pruebas verdes;
- `purchasing`: 19 unitarias y 6 PostgreSQL/Testcontainers verdes;
- `commercial-catalog`: 106 pruebas verdes; `inventory`: 71; y
  `business-partners`: 74, incluyendo sus perfiles PostgreSQL/Testcontainers;
- ArchUnit: 32 pruebas verdes en reactor de 24 módulos;
- `mvn verify`: 28 módulos verdes, incluido el WAR actual;
- Docker/Compose runtime, health/OIDC, seguridad desplegada y Playwright
  375/720/1280 de Compras: gate de J11-S9-06 cuando las rutas sean navegables;
- prueba de aceptación y validación independiente por otra persona: pendientes.
