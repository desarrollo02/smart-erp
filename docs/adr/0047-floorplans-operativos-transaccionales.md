# ADR-0047 — Floorplans operativos y transaccionales

- Estado: Aceptada
- Fecha: 2026-08-13
- Decisión de producto: incorporar el incremento antes de `sales`
- Iteración planificada: Sprint 10
- Decisiones relacionadas: ADR-0007, ADR-0017, ADR-0018, ADR-0028 y ADR-0044

## Contexto

El shell productivo consolidó un floorplan seguro de `directory`, `create` y
`detail`. Ese patrón resolvió la mezcla inicial de búsqueda, alta y ficha y es
adecuado para maestros como socios, artículos, definiciones y depósitos.

Inventario y Compras demostraron, sin embargo, que una tarea operativa no es sólo
el mantenimiento de una entidad. Registrar un movimiento, preparar una orden,
capturar varias líneas, recibir mercadería o aprobar trabajo pendiente exige
priorizar velocidad, contexto, prevención de errores y acciones válidas para el
estado actual.

El contrato vigente sólo distingue `DISPLAY_TEXT`, `TEXT_INPUT`, `SELECT`,
`DATA_TABLE` y `ACTION`. No expresa propósito de pantalla, tipo semántico de dato,
líneas editables, resumen de totales, acción primaria, disponibilidad dinámica ni
visibilidad condicional. Como consecuencia, documentos y movimientos se
representan como formularios genéricos y pueden exponer datos técnicos como
identidad de origen o clave de idempotencia.

El responsable de producto confirmó el 2026-08-13 que las pantallas utilizadas
para comprar, vender y ejecutar movimientos deben adoptar un diseño que facilite
la operación y que este trabajo debe incorporarse al plan antes de construir
Ventas.

## Decisión

1. El shell seguirá siendo dueño exclusivo de Jakarta Faces, XHTML, Material
   Design 3, tokens, responsive, accesibilidad, foco y renderers. Los plugins no
   aportarán markup, CSS, JavaScript ni EL.
2. El contrato neutral declarará el propósito de cada pantalla mediante una
   taxonomía cerrada equivalente a:
   - `MASTER_DATA`: directorio, alta y ficha actuales;
   - `WORKLIST`: bandeja de elementos que una persona debe procesar;
   - `TRANSACTION_EDITOR`: documento operativo con cabecera, líneas, resumen y
     acciones de finalización;
   - `GUIDED_OPERATION`: captura secuencial cuando el orden de validación importa;
   - `INQUIRY`: consulta y seguimiento sin mutación.
3. El contrato evolucionará con tipos semánticos neutrales para fecha, cantidad,
   dinero, estado, referencia buscable, líneas y resumen. No incorporará tipos de
   Jakarta ni de una biblioteca visual.
4. Las acciones declararán función semántica y disponibilidad dinámica. El shell
   mostrará sólo acciones aplicables al estado, permiso y actor actuales,
   distinguirá primaria, secundaria y destructiva, y explicará un bloqueo cuando
   sea útil sin sustituir la autorización del servidor.
5. El renderer admitirá visibilidad y obligatoriedad condicional. Un movimiento
   de entrada no pedirá origen; una transferencia pedirá origen y destino; lote,
   serie y vencimiento aparecerán sólo cuando el artículo los requiera.
6. Versiones optimistas, identidades técnicas e idempotencia permanecerán en el
   contrato de aplicación, estado seguro, logs y auditoría. No serán datos que un
   operador deba inventar o transcribir.
7. El editor transaccional expandido usará cabecera compacta, líneas como área
   principal, resumen contextual y barra persistente de acciones. Medio y
   compacto aplicarán una adaptación explícita, no una reducción literal del
   escritorio.
8. Los pilotos obligatorios serán:
   - movimiento de existencias, para validar captura breve y condicional;
   - orden de compra, para validar cabecera, múltiples líneas y finalización;
   - recepción/devolución, para validar captura guiada;
   - solicitudes/aprobaciones, para validar `WORKLIST` y separación de funciones.
9. Sprint 10 implementará la fundación y los pilotos después del gate técnico y
   la decisión de instalador de Sprint 9. `sales` comenzará después de que el
   floorplan operativo esté validado automáticamente.
10. Esta iteración es transversal y no altera ADR-0011: `purchasing` continúa como
    plugin ERP 4 y `sales` como plugin ERP 5.

## Alternativas descartadas

- **Dar XHTML propio a cada plugin:** rompe el gobierno del shell, la seguridad,
  el tema, las personalizaciones y la validación responsive.
- **Cambiar sólo colores, márgenes o tipografía:** no corrige jerarquía, captura de
  líneas, acciones inválidas ni datos técnicos expuestos.
- **Usar un asistente para toda operación:** agrega pasos a tareas frecuentes. El
  flujo guiado se reserva para tareas largas, poco familiares o con dependencias
  secuenciales.
- **Esperar hasta terminar Ventas:** obligaría a construir y luego migrar otro
  módulo operativo sobre el contrato insuficiente.
- **Rediseñar maestros existentes:** el floorplan actual conserva buen ajuste para
  administración y consulta; el cambio se limita a tareas operativas.

## Consecuencias

- `plugin-api` necesitará una versión nueva y una estrategia explícita de
  compatibilidad para pantallas existentes.
- El shell incorporará renderers cerrados adicionales y rechazará con seguridad
  combinaciones o versiones que no soporte.
- Los contratos visuales de Inventario y Compras avanzarán de versión, sin cambiar
  sus modelos, tablas ni contratos públicos de dominio.
- Las personalizaciones existentes deberán conservar compatibilidad o migrarse por
  versión; no podrán seleccionar clases CSS ni estructura XHTML.
- Sprint 9 no se reabre para introducir el cambio. Su siguiente gate permanece
  J11-S9-07 y su decisión de instalador J11-S9-08.
- Sprint 10 será una iteración de plataforma visual con pilotos reales. Ventas se
  planificará a partir de Sprint 11 y reutilizará el contrato validado.

## Verificación

- pruebas unitarias de invariantes y compatibilidad de `plugin-api`;
- pruebas del renderer y rechazo de propósitos/tipos no soportados;
- ArchUnit para mantener `plugin-api` libre de Jakarta y dependencias visuales;
- pruebas de estado, permiso, actor, versión e idempotencia para acciones;
- Playwright en 375, 599, 600, 720, 839, 840 y 1280 px;
- teclado, foco, mensajes, contraste y `prefers-reduced-motion`;
- ausencia de datos técnicos editables y de acciones no aplicables;
- demo navegable de los pilotos sobre WildFly, OIDC, PostgreSQL y JTA;
- medición reproducible de pasos, cambios de foco y errores en tareas operativas.

## Referencias verificadas

- [Material Design 3 — Canonical layout examples](https://m3.material.io/foundations/layout/canonical-examples/overview)
- [SAP Fiori — Worklist Floorplan](https://experience.sap.com/fiori-design-web/work-list/)
- [SAP Fiori — Wizard Floorplan](https://experience.sap.com/fiori-design-web/wizard/)
