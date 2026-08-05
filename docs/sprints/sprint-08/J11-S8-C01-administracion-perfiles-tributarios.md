# J11-S8-C01 - Corrección: administración visual de perfiles tributarios

- Estado: Implementada y validada técnicamente; recongelación, PDF e instalador pendientes
- Sprint: 8
- Fecha de decisión de producto: 2026-08-01
- Tipo: corrección funcional y de experiencia de usuario
- Plugin afectado: `commercial_catalog`
- Permiso rector: `commercial_catalog.definitions.manage`

## Motivo

La revisión visual posterior a la demo confirmó que la ficha de un artículo sólo
permitía asignar el perfil tributario ficticio cargado por el fixture. El modelo y
los casos de uso admitían múltiples perfiles por empresa, pero no existía un
recorrido visual para consultarlos o registrarlos. La misma brecha puede repetirse
en otros selectores construidos desde definiciones controladas.

El responsable de producto aceptó corregir el recorrido el 2026-08-01. La decisión
reabre el baseline congelado por J11-S8-07: las imágenes y el instalador generados
antes de esta corrección no representan el nuevo baseline y no pueden promoverse.

## Alcance del corte

- aportar un menú y una pantalla neutral de perfiles tributarios;
- listar perfiles de la empresa activa, con tratamiento interno, vigencia, estado
  y versión;
- registrar más de un perfil mediante el caso de uso real ya auditado;
- exigir `commercial_catalog.definitions.manage` para abrir la administración y
  para registrar;
- mantener la asignación del artículo bajo `commercial_catalog.items.manage`;
- explicar desde la ficha que los perfiles nuevos se administran en su menú;
- agregar datos de demostración inequívocamente ficticios para gravado general,
  reducido y exento;
- conservar la separación entre perfil interno y correspondencia fiscal/SIFEN.

## Fuera de alcance

- afirmar que los datos de demostración son una configuración fiscal certificada;
- guardar una tasa oficial o códigos SIFEN en el catálogo comercial;
- resolver exenciones por cliente, jurisdicción, régimen o tipo de operación;
- editar, versionar o inactivar perfiles existentes desde la UI en este primer
  corte;
- administrar todavía unidades, categorías, marcas, etiquetas o variantes en la
  misma pantalla.

Los demás selectores controlados quedan incluidos en la historia de continuidad
de definiciones maestras. Este corte establece el patrón vertical empezando por el
hallazgo tributario que bloquea la comprensión de la demo.

## Criterios de aceptación

- **CA-01:** el descriptor aporta `/catalog/tax-profiles` sólo a quien posee
  `commercial_catalog.definitions.manage`.
- **CA-02:** el shell renderiza el nuevo contrato sin XHTML, CSS, JavaScript ni EL
  aportados por el plugin.
- **CA-03:** el directorio queda aislado por empresa y permite filtrar por texto y
  estado.
- **CA-04:** el alta exige código, nombre, tratamiento interno, descripción y
  vigencia inicial; la vigencia final es opcional y posterior.
- **CA-05:** cada carga y alta revalida empresa, plugin y permiso en el servidor.
- **CA-06:** una denegación, código repetido, vigencia inválida o dato mal formado
  produce un mensaje comprensible sin revelar SQL ni datos internos.
- **CA-07:** un perfil registrado aparece inmediatamente en el directorio y queda
  disponible en los selectores del artículo mediante la consulta real.
- **CA-08:** la ficha del perfil distingue identidad interna, tratamiento,
  vigencia, estado y versión, y declara que no equivale a una regla SIFEN.
- **CA-09:** la pantalla funciona en 375, 720 y 1280 px, con teclado, labels, foco
  visible y sin overflow horizontal normal.
- **CA-10:** pruebas de plugin, renderer, arquitectura, reactor, composición y
  Playwright quedan verdes antes de recongelar el baseline.
- **CA-11:** demo, manuales, fotografía de plugins, PDF y evidencias de Sprint 8 se
  actualizan cuando corresponda.
- **CA-12:** después de recongelar, el instalador Windows se regenera y se repiten
  sus gates afectados; la edición anterior queda obsoleta y no entregable.

## Continuidad de definiciones maestras

La siguiente evolución del patrón debe cubrir unidades, categorías, marcas,
etiquetas y familias de variantes. Los selectores mostrarán una vía de
administración sólo a usuarios autorizados; no se habilitará texto libre ni alta
silenciosa desde operaciones cotidianas.
