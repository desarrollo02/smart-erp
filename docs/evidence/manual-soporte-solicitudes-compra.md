# Evidencia de elaboración — Manual de soporte de Solicitudes de compra

## Identificación

- Fecha de verificación: 20 de agosto de 2026.
- Pantalla: **Trabajo > Solicitudes de compra**.
- Ruta: `/faces/app/view.xhtml?route=%2Fpurchasing%2Frequests`.
- Alcance: una pantalla con modos lista, alta y detalle.
- Fuente canónica: `docs/user-guide/modules/solicitudes-compra-soporte.md`.

## Investigación funcional y técnica

- Se revisaron el contrato de pantalla, el manejador Jakarta Faces, el servicio de aplicación, el dominio, los repositorios, las entidades, las migraciones y los permisos del plugin `purchasing`.
- Se documentaron los permisos `purchasing.view`, `purchasing.requests.create`, `purchasing.requests.submit` y `purchasing.requests.approve`.
- Se describieron lista y filtros, alta, líneas, envío, aprobación, rechazo, cancelación, clonación, concurrencia, idempotencia y auditoría.
- Se incluyeron cuatro capturas reales obtenidas por Playwright sobre la aplicación local con datos ficticios: lista expandida, lista compacta, alta expandida y detalle enviado expandido.

## Consulta local de sólo lectura

- Ambiente consultado: candidata aislada de PostgreSQL 18.4, base `logixone`.
- Todas las consultas se ejecutaron dentro de `BEGIN TRANSACTION READ ONLY`; no se ejecutaron DML, DDL, migraciones ni procedimientos con efecto.
- Resultado observado: tres solicitudes, todas Aprobadas y con una línea.
- Ejemplo principal: `SC-3A8949E1`, fecha 2026-08-20, versión final 2, con secuencia Crear 0, Enviar 1 y Aprobar 2.
- La decisión independiente se comprobó sin publicar los dos UUID internos de actores.
- Se verificaron 12 tablas, cero vistas y cero secuencias en `plg_purchasing`.
- Para la pantalla se documentaron 57 columnas de cuatro objetos: `purchase_request`, `purchase_request_line`, `purchasing_operation` y `core.audit_event`, además de dos servicios externos.
- Se verificaron dos triggers: inmutabilidad de líneas fuera de Borrador y auditoría append-only.
- El manual contiene 14 bloques titulados **Ejemplo verificado con datos reales**.

## Artefactos

- Markdown: `docs/user-guide/modules/solicitudes-compra-soporte.md`.
- Web responsive: `docs/user-guide/modules/web/solicitudes-compra-soporte.html`.
- PDF: `docs/output/pdf/manual-soporte-solicitudes-compra.pdf`.
- Generador reproducible: `tools/generate_purchase_request_support_manual.py`.
- Capturas: `docs/user-guide/modules/assets/purchasing-requests/`.

## Validación del PDF y la web

- PDF A4 de 19 páginas, 588094 bytes, sin cifrado, formularios ni JavaScript.
- SHA-256: `4AF2EBBA452AF7F4CA188368F0DB1BC6A04B6B9FA9DBED08C82E081EF760AC73`.
- El PDF se reabrió con `pypdf`: 19 páginas y ninguna página sin texto extraíble.
- Las 19 páginas se renderizaron a PNG con Poppler y se revisaron visualmente: portada, índice, encabezados, pies, tablas, diagramas, cortes y las cuatro capturas.
- La tabla de cobertura se transformó a una disposición vertical para evitar texto ilegible y los encabezados se mantuvieron junto a sus imágenes o ejemplos.
- Markdown, HTML y texto extraído del PDF: cero caracteres de sustitución y cero marcadores comunes de texto mal codificado.
- HTML: cuatro imágenes con texto alternativo y diseño responsive.
- El generador pasó validación sintáctica mediante `ast.parse` sin crear archivos fuera del proyecto.
- `git diff --check` no informó errores atribuibles a estos artefactos; mostró únicamente advertencias de finales de línea en archivos ajenos ya modificados.

## Límites

- No se repitió la suite Maven ni Playwright: las capturas proceden de la evidencia E2E local existente del corte y este cambio sólo agrega documentación derivada.
- La validación independiente por otra persona permanece pendiente.
