# J11-S9-06 — Integración, composición y demo candidata de `purchasing`

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Fecha: 2026-08-12
- Dependencia: J11-S9-05 implementada y validada automáticamente
- Rama local: `sprint/09-purchasing`
- Evidencia: [J11-S9-06](../../evidence/J11-S9-06-integracion-composicion-purchasing.md)
- Demo: [guion reproducible](../../runbooks/demo-purchasing-j11-s9-06.md)
- Manual: [Compras](../../user-guide/modules/compras.md)

## Objetivo

Componer físicamente Compras en el WAR y el migrador, desplegar el corte con
Docker/Compose y demostrar los recorridos críticos mediante la interfaz real de
Jakarta Faces, incluyendo autorización, persistencia, integración con plugins
proveedores y comportamiento responsive.

## Alcance implementado

- perfil Maven explícito `with-purchasing-demo` en el conjunto físico de plugins;
- soporte del perfil en las imágenes de aplicación y migrador;
- composición de `reference_data`, `business_partners`, `commercial_catalog`,
  `inventory` y `purchasing`, sin relaciones JPA ni acceso cruzado a tablas;
- consultas de Compras dentro de una transacción JTA normal, evitando ejecutar
  Hibernate fuera del contexto transaccional del servidor;
- búsqueda paginada por texto y resolución exacta de UUID para proveedores,
  artículos, monedas, solicitudes, órdenes, recepciones y depósitos;
- protocolo semántico de POST para selectores del shell: preserva valores entre
  búsquedas, recalcula dependencias y revalida cada opción en el servidor;
- comparación de números comerciales sin sensibilidad a mayúsculas/minúsculas;
- contexto empresarial verificable en la interfaz, sin exponer información
  sensible;
- acceso estable desde la raíz `/logixone/`, que redirige a la vista Faces
  protegida por OIDC sin exponer una pantalla pública alternativa;
- E2E real que aprovisiona catálogos ficticios, crea una solicitud, exige un
  aprobador distinto, emite una orden, confirma recepción y devolución, revisa
  seguimiento y prueba la denegación al desactivar el plugin;
- evidencia visual en 375, 720 y 1280 px, más límites 599/600/839/840 px.

## Criterios de aceptación

- **CA-01:** el perfil físico incluye Compras y todas sus dependencias públicas.
- **CA-02:** el mismo perfil construye WAR y migrador; las migraciones V1–V2 se
  validan de forma idempotente.
- **CA-03:** la aplicación desplegada queda `healthy` y readiness informa todos
  los checks técnicos en `UP`.
- **CA-04:** la matriz OIDC acepta emisor/firma/audiencia/expiración válidos y
  rechaza audiencia, emisor o expiración inválidos.
- **CA-05:** una solicitud no puede ser aprobada por quien la creó.
- **CA-06:** la recepción y devolución de stock usan únicamente contratos
  públicos de Inventario y conservan cantidades coherentes.
- **CA-07:** una devolución confirmada vuelve a abrir la cantidad pendiente del
  proveedor: pedida − recibida + devuelta − cerrada.
- **CA-08:** un plugin desactivado no aporta menú ni permite abrir su ruta; al
  restaurarlo vuelve a estar disponible.
- **CA-09:** las pantallas no presentan overflow horizontal normal en compacto,
  medio, expandido ni en los límites del sistema responsive.
- **CA-10:** los valores de selectores se revalidan en servidor y no se confía en
  UUID enviados por el navegador.

Todos los criterios automatizables quedaron verdes. La prueba independiente por
otra persona permanece diferida hasta la candidata comercializable.

## Correcciones guiadas por pruebas

La ejecución no se dio por válida ante los primeros fallos. El ciclo de prueba y
corrección detectó y resolvió:

1. búsquedas AJAX no deterministas dentro de componentes repetidos;
2. aceptación potencial de una referencia obtenida mediante búsqueda difusa;
3. preparación de datos E2E en una empresa distinta a la del aprobador;
4. ausencia de unidad y perfil tributario en el catálogo de prueba;
5. búsquedas sensibles a mayúsculas en números comerciales;
6. pérdida de una selección al buscar en otro selector dependiente;
7. validación prematura de JSF sobre opciones que debían reconstruirse;
8. una expectativa E2E incorrecta: devolver dos unidades reabre dos unidades
   pendientes y no reduce el pendiente;
9. entrada raíz inexistente, contraste insuficiente en metadatos de la barra y
   coincidencia ambigua entre `Habilitar` y `Deshabilitar` en Playwright.

Cada falla automatizada bloqueó el avance hasta que la causa quedó corregida y la
prueba relevante volvió a verde.

## Resultado

J11-S9-06 queda implementada y validada automáticamente. La imagen verificada
`logixone/app:j11-s9-06-purchasing-demo-r5` está desplegada localmente en
`http://localhost:18080/logixone/`. Sprint 9 continúa abierto: J11-S9-07 debe
ejecutar el gate acumulado de candidata comercializable, actualizar la fotografía
de plugins, regenerar y revisar el PDF obligatorio de cierre y reunir la
validación independiente pendiente. J11-S9-08 conserva la decisión explícita del
instalador Windows.
